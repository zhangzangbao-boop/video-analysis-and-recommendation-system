package com.shortvideo.recommendation.als.storage

import java.sql.{Connection, DriverManager, PreparedStatement, Timestamp}
import org.apache.spark.sql.Row

/**
 * MySQL 写入工具类
 * 负责将 Spark ALS 生成的推荐结果写入 MySQL
 */
object MySQLWriter {

  // MySQL 连接配置
  // 请确保端口和密码与您的本地环境一致 (通常是 3306)
  private val JDBC_URL = "jdbc:mysql://localhost:3306/short_movie_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai"
  private val JDBC_USER = "root"
  private val JDBC_PASSWORD = "root"

  /**
   * 将推荐结果批量写入 MySQL (分布式并行写入)
   *
   * @param recommendations 推荐结果 Dataset
   * @param recommendType  推荐类型：OFFLINE 或 REALTIME
   * @param modelId        关联的模型ID
   */
  def writeRecommendationsToMySQL(recommendations: org.apache.spark.sql.Dataset[Row], recommendType: String, modelId: String = ""): Unit = {
    import recommendations.sparkSession.implicits._

    println(s"[INFO] 开始写入推荐结果 (类型: $recommendType)...")

    // 1. 先在 Driver 端执行删除旧数据操作 (避免并行删除产生锁竞争)
    if (recommendType == "OFFLINE") {
      deleteOldRecommendations(recommendType)
    }

    // 2. 使用 foreachPartition 分布式写入数据
    // 这将把任务分发到各个 Executor 并行执行，避免 Driver OOM
    recommendations.foreachPartition { partition: Iterator[Row] =>
      writePartitionToMySQL(partition, recommendType, modelId)
    }

    println("[INFO] 推荐结果写入任务已提交集群执行")
  }

  /**
   * 删除旧数据的辅助方法
   */
  private def deleteOldRecommendations(recommendType: String): Unit = {
    var connection: Connection = null
    var stmt: PreparedStatement = null
    try {
      connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD)
      // 简单策略：删除该类型的所有旧数据，或者按需删除
      // 这里为了演示安全，仅打印日志，实际生产中应根据日期或批次删除
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
   * 将一个分区的数据写入 MySQL (核心写入逻辑)
   */
  private def writePartitionToMySQL(partition: Iterator[Row], recommendType: String, modelId: String): Unit = {
    var connection: Connection = null
    var insertStmt: PreparedStatement = null

    try {
      // 每个分区建立独立的数据库连接
      connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD)
      connection.setAutoCommit(false)

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

      // 遍历分区内的每一行
      partition.foreach { row =>
        try {
          val userId = row.getAs[Long]("userId")
          // 获取推荐列表 (Array of Struct)
          val recs = row.getAs[Seq[Row]]("recommendations")

          if (recs != null) {
            recs.zipWithIndex.foreach { case (rec, index) =>
              val movieId = rec.getAs[Long]("movieId") // 注意：Struct 字段名需对应
              val score = rec.getAs[Float]("rating")
              val rank = index + 1

              insertStmt.setLong(1, userId)
              insertStmt.setLong(2, movieId)
              insertStmt.setDouble(3, score.toDouble)
              insertStmt.setInt(4, rank)
              insertStmt.setString(5, recommendType)
              insertStmt.setString(6, modelId)
              insertStmt.setTimestamp(7, currentTime)
              insertStmt.setTimestamp(8, currentTime)

              insertStmt.addBatch()
              count += 1

              // 批处理提交
              if (count % 1000 == 0) {
                insertStmt.executeBatch()
                connection.commit()
              }
            }
          }
        } catch {
          case e: Exception =>
          // 捕获单行异常，避免整个分区失败
          // e.printStackTrace()
        }
      }

      // 提交剩余数据
      insertStmt.executeBatch()
      connection.commit()

    } catch {
      case e: Exception =>
        if (connection != null) connection.rollback()
        println(s"[ERROR] 分区写入失败: ${e.getMessage}")
    } finally {
      if (insertStmt != null) insertStmt.close()
      if (connection != null) connection.close()
    }
  }

  /**
   * 写入模型参数 (保持不变，略作精简)
   */
  def writeModelParamsToMySQL(modelPath: String, rank: Int, regParam: Double, maxIter: Int, rmse: Double): String = {
    var connection: Connection = null
    var stmt: PreparedStatement = null
    val modelId = s"als-${System.currentTimeMillis()}"

    try {
      connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD)

      val sql = """
        INSERT INTO model_params (model_id, `rank`, reg_param, max_iter, training_time, model_path, rmse, status)
        VALUES (?, ?, ?, ?, NOW(), ?, ?, 'ACTIVE')
      """
      stmt = connection.prepareStatement(sql)
      stmt.setString(1, modelId)
      stmt.setInt(2, rank)
      stmt.setDouble(3, regParam)
      stmt.setInt(4, maxIter)
      stmt.setString(5, modelPath)
      stmt.setDouble(6, rmse)

      stmt.executeUpdate()
      println(s"[INFO] 模型参数已保存: $modelId")
      modelId
    } catch {
      case e: Exception =>
        e.printStackTrace()
        "unknown-model"
    } finally {
      if (stmt != null) stmt.close()
      if (connection != null) connection.close()
    }
  }
}