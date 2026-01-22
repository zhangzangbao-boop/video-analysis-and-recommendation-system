package com.shortvideo.recommendation.als.storage

import java.sql.{Connection, DriverManager, PreparedStatement, Timestamp}
import org.apache.spark.sql.Row

object MySQLWriter {

  // [修正] 数据库名与 application.yml 保持一致
  // 使用统一的数据库配置
  import com.shortvideo.recommendation.common.config.DatabaseConfig
  private val JDBC_URL = DatabaseConfig.JDBC_URL
  private val JDBC_USER = DatabaseConfig.JDBC_USER
  private val JDBC_PASSWORD = DatabaseConfig.JDBC_PASSWORD

  /**
   * 写入推荐结果到 MySQL
   * 适配 table: recommendation_result
   */
  def writeRecommendationsToMySQL(recommendations: org.apache.spark.sql.Dataset[Row], recommendType: String, modelId: String = ""): Unit = {
    import recommendations.sparkSession.implicits._

    println(s"[INFO] 开始写入推荐结果 (类型: $recommendType, 模型ID: $modelId)...")
    
    // 先测试数据库连接
    if (!testConnection()) {
      println("[ERROR] 数据库连接失败，无法写入推荐结果")
      return
    }

    val totalUsers = recommendations.count()
    println(s"[INFO] 待写入推荐结果的用户数: $totalUsers")

    if (recommendType == "OFFLINE") {
      val deletedCount = deleteOldRecommendations(recommendType)
      println(s"[INFO] 已删除 $deletedCount 条旧的 $recommendType 推荐数据")
    }

    var totalWritten = 0L
    var partitionCount = 0
    
    recommendations.foreachPartition { partition: Iterator[Row] =>
      partitionCount += 1
      val written = writePartitionToMySQL(partition, recommendType, modelId, partitionCount)
      totalWritten += written
    }
    
    println(s"[SUCCESS] 推荐结果写入完成！")
    println(s"  - 处理分区数: $partitionCount")
    println(s"  - 写入记录数: $totalWritten")
    println(s"  - 推荐类型: $recommendType")
    println(s"  - 模型ID: $modelId")
  }
  
  /**
   * 测试数据库连接
   */
  private def testConnection(): Boolean = {
    var connection: Connection = null
    try {
      Class.forName("com.mysql.cj.jdbc.Driver")
      connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD)
      println(s"[INFO] 数据库连接成功: $JDBC_URL")
      true
    } catch {
      case e: Exception =>
        println(s"[ERROR] 数据库连接失败: ${e.getMessage}")
        e.printStackTrace()
        false
    } finally {
      if (connection != null) connection.close()
    }
  }

  private def deleteOldRecommendations(recommendType: String): Int = {
    var connection: Connection = null
    var stmt: PreparedStatement = null
    try {
      Class.forName("com.mysql.cj.jdbc.Driver")
      connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD)
      stmt = connection.prepareStatement("DELETE FROM recommendation_result WHERE `type` = ?")
      stmt.setString(1, recommendType)
      val deletedCount = stmt.executeUpdate()
      connection.commit()
      deletedCount
    } catch {
      case e: Exception =>
        println(s"[ERROR] 清理旧数据失败: ${e.getMessage}")
        e.printStackTrace()
        0
    } finally {
      if (stmt != null) stmt.close()
      if (connection != null) connection.close()
    }
  }

  private def writePartitionToMySQL(partition: Iterator[Row], recommendType: String, modelId: String, partitionId: Int): Long = {
    var connection: Connection = null
    var insertStmt: PreparedStatement = null
    var totalWritten = 0L
    var errorCount = 0L

    try {
      Class.forName("com.mysql.cj.jdbc.Driver")
      connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD)
      connection.setAutoCommit(false)

      // [修正] 字段适配: movie_id -> video_id (依据 database.sql)
      // [修正] 包含 model_id 和 reason (与实时推荐保持一致)
      val sql =
        """
          |INSERT INTO recommendation_result
          |(user_id, video_id, score, `rank`, `type`, reason, model_id, create_time, update_time)
          |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
          |ON DUPLICATE KEY UPDATE
          |score = VALUES(score),
          |`rank` = VALUES(`rank`),
          |reason = VALUES(reason),
          |model_id = VALUES(model_id),
          |update_time = VALUES(update_time)
        """.stripMargin

      insertStmt = connection.prepareStatement(sql)
      val currentTime = new Timestamp(System.currentTimeMillis())
      var batchCount = 0
      
      // 根据推荐类型设置推荐理由
      val reason = if (recommendType == "OFFLINE") "基于协同过滤推荐" else "离线推荐"

      partition.foreach { row =>
        try {
          val userId = row.getAs[Number]("userId").longValue()
          val recs = row.getAs[Seq[Row]]("recommendations")

          if (recs != null && recs.nonEmpty) {
            recs.zipWithIndex.foreach { case (rec, index) =>
              try {
                // [注意] ALS 结果列名通常是 "movieId" (来自 ALSTrainer 的 setItemCol)
                val videoId = rec.getAs[Number](0).longValue()
                val score = rec.getAs[Float](1)
                val rank = index + 1

                insertStmt.setLong(1, userId)
                insertStmt.setLong(2, videoId)
                insertStmt.setDouble(3, score.toDouble)
                insertStmt.setInt(4, rank)
                insertStmt.setString(5, recommendType)
                insertStmt.setString(6, reason) // 写入推荐理由
                insertStmt.setString(7, modelId) // 写入模型ID
                insertStmt.setTimestamp(8, currentTime)
                insertStmt.setTimestamp(9, currentTime)

                insertStmt.addBatch()
                batchCount += 1
                totalWritten += 1

                if (batchCount >= 1000) {
                  val inserted = insertStmt.executeBatch()
                  connection.commit()
                  batchCount = 0
                }
              } catch {
                case e: Exception =>
                  errorCount += 1
                  println(s"[WARN] 分区 $partitionId: 写入单条推荐记录失败 (userId=$userId): ${e.getMessage}")
                  if (errorCount <= 5) e.printStackTrace() // 只打印前5个错误的堆栈
              }
            }
          }
        } catch {
          case e: Exception =>
            errorCount += 1
            println(s"[WARN] 分区 $partitionId: 处理用户推荐记录失败: ${e.getMessage}")
            if (errorCount <= 5) e.printStackTrace()
        }
      }

      if (batchCount > 0) {
        insertStmt.executeBatch()
        connection.commit()
      }

      if (totalWritten > 0 || errorCount > 0) {
        println(s"[INFO] 分区 $partitionId 写入完成: 成功=$totalWritten, 失败=$errorCount")
      }

    } catch {
      case e: Exception =>
        if (connection != null) connection.rollback()
        println(s"[ERROR] 分区 $partitionId 写入 MySQL 失败: ${e.getMessage}")
        e.printStackTrace()
    } finally {
      if (insertStmt != null) insertStmt.close()
      if (connection != null) connection.close()
    }
    
    totalWritten
  }

  // 写入模型参数 (表结构 database.sql 中有 model_params 表)
  def writeModelParamsToMySQL(modelPath: String, rank: Int, regParam: Double, maxIter: Int, rmse: Double): String = {
    var connection: Connection = null
    var stmt: PreparedStatement = null
    val modelId = s"als-${System.currentTimeMillis()}"

    try {
      println(s"[MySQL] ====== 开始写入模型参数到MySQL ======")
      println(s"[MySQL] 模型ID: $modelId")
      println(s"[MySQL] 模型路径: $modelPath")
      println(s"[MySQL] 参数: rank=$rank, regParam=$regParam, maxIter=$maxIter, rmse=$rmse")
      println(s"[MySQL] 数据库URL: $JDBC_URL")
      println(s"[MySQL] 数据库用户: $JDBC_USER")
      System.out.flush()
      
      println(s"[MySQL] 步骤1: 加载MySQL驱动...")
      Class.forName("com.mysql.cj.jdbc.Driver")
      println(s"[MySQL] 步骤2: 连接数据库...")
      System.out.flush()
      
      connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD)
      println(s"[MySQL] ✓ 数据库连接成功")
      System.out.flush()

      println(s"[MySQL] 步骤3: 更新旧模型状态为DEPRECATED...")
      val updateSql = "UPDATE model_params SET status = 'DEPRECATED' WHERE status = 'ACTIVE'"
      stmt = connection.prepareStatement(updateSql)
      val updatedCount = stmt.executeUpdate()
      stmt.close()
      println(s"[MySQL] 已更新 $updatedCount 条旧模型记录")
      System.out.flush()

      println(s"[MySQL] 步骤4: 插入新模型参数...")
      val insertSql = """
        INSERT INTO model_params
        (model_id, `rank`, reg_param, max_iter, training_time, model_path, rmse, status)
        VALUES (?, ?, ?, ?, NOW(), ?, ?, 'ACTIVE')
      """
      stmt = connection.prepareStatement(insertSql)
      stmt.setString(1, modelId)
      stmt.setInt(2, rank)
      stmt.setDouble(3, regParam)
      stmt.setInt(4, maxIter)
      stmt.setString(5, modelPath)
      stmt.setDouble(6, rmse)

      val insertedCount = stmt.executeUpdate()
      println(s"[MySQL] ✓ 模型参数插入成功，影响行数: $insertedCount")
      println(s"[MySQL] ✓✓✓ 模型参数已保存到MySQL: $modelId")
      System.out.flush()
      
      modelId

    } catch {
      case e: java.sql.SQLException =>
        System.err.println(s"[ERROR] ====== MySQL SQL异常 ======")
        System.err.println(s"[ERROR] 错误消息: ${e.getMessage}")
        System.err.println(s"[ERROR] SQL状态: ${e.getSQLState}")
        System.err.println(s"[ERROR] 错误码: ${e.getErrorCode}")
        System.err.println(s"[ERROR] 模型ID: $modelId")
        e.printStackTrace(System.err)
        System.err.flush()
        "unknown-model"
      case e: ClassNotFoundException =>
        System.err.println(s"[ERROR] ====== MySQL驱动类未找到 ======")
        System.err.println(s"[ERROR] 错误消息: ${e.getMessage}")
        System.err.println(s"[ERROR] 请检查是否添加了MySQL JDBC驱动依赖")
        e.printStackTrace(System.err)
        System.err.flush()
        "unknown-model"
      case e: Exception =>
        System.err.println(s"[ERROR] ====== 写入模型参数到MySQL失败 ======")
        System.err.println(s"[ERROR] 错误消息: ${e.getMessage}")
        System.err.println(s"[ERROR] 异常类型: ${e.getClass.getName}")
        System.err.println(s"[ERROR] 模型ID: $modelId")
        e.printStackTrace(System.err)
        System.err.flush()
        "unknown-model"
    } finally {
      try {
        if (stmt != null) {
          stmt.close()
          println(s"[MySQL] PreparedStatement已关闭")
        }
      } catch {
        case e: Exception =>
          System.err.println(s"[WARN] 关闭PreparedStatement失败: ${e.getMessage}")
      }
      try {
        if (connection != null) {
          connection.close()
          println(s"[MySQL] 数据库连接已关闭")
        }
      } catch {
        case e: Exception =>
          System.err.println(s"[WARN] 关闭数据库连接失败: ${e.getMessage}")
      }
      println(s"[MySQL] ====== 结束MySQL操作 ======")
      System.out.flush()
    }
  }
}