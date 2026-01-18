package com.shortvideo.recommendation.als.storage

import java.sql.{Connection, DriverManager, PreparedStatement, Timestamp}
import org.apache.spark.sql.Row

/**
 * MySQL 写入工具类
 * 负责将 Spark ALS 生成的推荐结果写入 MySQL
 */
object MySQLWriter {

  // ==========================================
  // MySQL 连接配置
  // ==========================================
  // [修改] 修正了端口为 3306，数据库名为 short_video_platform
  private val JDBC_URL = "jdbc:mysql://localhost:3306/short_video_platform?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai"
  private val JDBC_USER = "root"
  private val JDBC_PASSWORD = "root"

  /**
   * 将推荐结果批量写入 MySQL (分布式并行写入模式)
   *
   * @param recommendations 推荐结果 Dataset
   * @param recommendType  推荐类型：OFFLINE 或 REALTIME
   * @param modelId        关联的模型ID
   */
  def writeRecommendationsToMySQL(recommendations: org.apache.spark.sql.Dataset[Row], recommendType: String, modelId: String = ""): Unit = {
    import recommendations.sparkSession.implicits._

    println(s"[INFO] 开始写入推荐结果 (类型: $recommendType, 模型ID: $modelId)...")

    // 1. 如果是离线推荐，先在 Driver 端清理旧数据
    // 注意：这里只清理该类型的旧数据，避免全表删除
    if (recommendType == "OFFLINE") {
      deleteOldRecommendations(recommendType)
    }

    // 2. 使用 foreachPartition 分布式写入
    // 数据不回传 Driver，直接由各个 Executor 并行写入 MySQL
    recommendations.foreachPartition { partition: Iterator[Row] =>
      writePartitionToMySQL(partition, recommendType, modelId)
    }

    println("[INFO] 推荐结果写入任务已提交集群执行")
  }

  /**
   * 删除旧数据的辅助方法 (Driver 端执行)
   */
  private def deleteOldRecommendations(recommendType: String): Unit = {
    var connection: Connection = null
    var stmt: PreparedStatement = null
    try {
      connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD)

      println(s"[WARN] 准备清理旧的 $recommendType 推荐数据...")
      stmt = connection.prepareStatement("DELETE FROM recommendation_result WHERE `type` = ?")
      stmt.setString(1, recommendType)

      val count = stmt.executeUpdate()
      println(s"[INFO] 已清理 $count 条旧数据")

    } catch {
      case e: Exception =>
        println(s"[ERROR] 清理旧数据失败: ${e.getMessage}")
    } finally {
      if (stmt != null) stmt.close()
      if (connection != null) connection.close()
    }
  }

  /**
   * 将一个分区的数据写入 MySQL (Executor 端执行)
   * [修改] 增加了 modelId 参数
   */
  private def writePartitionToMySQL(partition: Iterator[Row], recommendType: String, modelId: String): Unit = {
    var connection: Connection = null
    var insertStmt: PreparedStatement = null

    try {
      // [关键] 显式加载驱动，防止 Executor 端找不到驱动
      Class.forName("com.mysql.cj.jdbc.Driver")

      connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD)
      connection.setAutoCommit(false) // 开启手动提交事务

      // 准备 SQL 语句 (支持 Insert Or Update)
      val sql =
        """
          |INSERT INTO recommendation_result
          |(user_id, movie_id, score, `rank`, `type`, model_id, create_time, update_time)
          |VALUES (?, ?, ?, ?, ?, ?, ?, ?)
          |ON DUPLICATE KEY UPDATE
          |score = VALUES(score),
          |`rank` = VALUES(`rank`),
          |model_id = VALUES(model_id),
          |update_time = VALUES(update_time)
        """.stripMargin

      insertStmt = connection.prepareStatement(sql)
      val currentTime = new Timestamp(System.currentTimeMillis())

      var count = 0
      var batchCount = 0

      // 遍历分区内的每一行数据
      partition.foreach { row =>
        try {
          // 获取 userId (兼容 Integer 和 Long)
          val userId = row.getAs[Any]("userId") match {
            case l: Long => l
            case i: Int => i.toLong
            case other => other.toString.toLong
          }

          // 获取推荐列表 Array[Struct]
          val recs = row.getAs[Seq[Row]]("recommendations")

          if (recs != null) {
            recs.zipWithIndex.foreach { case (rec, index) =>
              // 获取 movieId (兼容 Integer 和 Long)
              // 注意：Struct 中的字段顺序通常是 [movieId, rating]
              val movieId = rec.getAs[Any](0) match {
                case l: Long => l
                case i: Int => i.toLong
                case other => other.toString.toLong
              }

              val score = rec.getAs[Float](1) // rating
              val rank = index + 1

              // 填充参数
              insertStmt.setLong(1, userId)
              insertStmt.setLong(2, movieId)
              insertStmt.setDouble(3, score.toDouble)
              insertStmt.setInt(4, rank)
              insertStmt.setString(5, recommendType)
              insertStmt.setString(6, modelId)
              insertStmt.setTimestamp(7, currentTime)
              insertStmt.setTimestamp(8, currentTime)

              insertStmt.addBatch()
              batchCount += 1
              count += 1

              // 每 1000 条提交一次 batch
              if (batchCount >= 1000) {
                insertStmt.executeBatch()
                connection.commit()
                batchCount = 0
              }
            }
          }
        } catch {
          case e: Exception =>
          // 捕获单行处理异常，避免整个分区失败
          // 生产环境可记录到累加器或错误日志
        }
      }

      // 提交剩余的数据
      if (batchCount > 0) {
        insertStmt.executeBatch()
        connection.commit()
      }

    } catch {
      case e: Exception =>
        if (connection != null) connection.rollback()
        println(s"[ERROR] 分区写入 MySQL 失败: ${e.getMessage}")
    } finally {
      if (insertStmt != null) insertStmt.close()
      if (connection != null) connection.close()
    }
  }

  /**
   * 写入模型参数元数据
   */
  def writeModelParamsToMySQL(modelPath: String, rank: Int, regParam: Double, maxIter: Int, rmse: Double): String = {
    var connection: Connection = null
    var stmt: PreparedStatement = null
    // 生成唯一的模型ID
    val modelId = s"als-${System.currentTimeMillis()}"

    try {
      Class.forName("com.mysql.cj.jdbc.Driver")
      connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD)

      // 1. 将旧的 ACTIVE 模型状态更新为 DEPRECATED (可选)
      val updateSql = "UPDATE model_params SET status = 'DEPRECATED' WHERE status = 'ACTIVE'"
      stmt = connection.prepareStatement(updateSql)
      stmt.executeUpdate()
      stmt.close()

      // 2. 插入新模型记录
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

      stmt.executeUpdate()
      println(s"[INFO] 模型参数已保存: $modelId")

      modelId // 返回 ID 供后续使用

    } catch {
      case e: Exception =>
        e.printStackTrace()
        "unknown-model" // 出错返回默认值
    } finally {
      if (stmt != null) stmt.close()
      if (connection != null) connection.close()
    }
  }
}