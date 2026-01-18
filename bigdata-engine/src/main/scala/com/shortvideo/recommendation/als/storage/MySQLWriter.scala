package com.shortvideo.recommendation.als.storage


import java.sql.{Connection, DriverManager, PreparedStatement, Timestamp}
import org.apache.spark.sql.Row

/**
 * MySQL 写入工具类
 * 负责将 Spark ALS 生成的推荐结果写入 MySQL
 */
object MySQLWriter {

  // MySQL 连接配置
  private val JDBC_URL = "jdbc:mysql://localhost:3307/short_movie_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai"
  private val JDBC_USER = "root"
  private val JDBC_PASSWORD = "root"

  /**
   * 将推荐结果批量写入 MySQL
   *
   * @param recommendations 推荐结果 Dataset，格式为 (userId, recommendations)
   *                       recommendations 是一个数组，包含 (movieId, rating)
   * @param recommendType  推荐类型：OFFLINE 或 REALTIME
   * @param modelId        关联的模型ID
   */
  def writeRecommendationsToMySQL(recommendations: org.apache.spark.sql.Dataset[Row], recommendType: String, modelId: String = ""): Unit = {
    import recommendations.sparkSession.implicits._

    println(s"[INFO] 开始写入推荐结果 (类型: $recommendType)...")
    println(s"[INFO] 推荐列表总用户数: ${recommendations.count()}")

    // 收集所有推荐数据到 Driver 端
    val allRecs = recommendations.collect()
    println(s"[INFO] 已收集 ${allRecs.length} 个用户的推荐数据")

    if (allRecs.isEmpty) {
      println("[WARN] 没有推荐数据需要写入")
      return
    }

    var connection: Connection = null
    var deleteStmt: PreparedStatement = null
    var insertStmt: PreparedStatement = null

    try {
      // 建立 JDBC 连接
      connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD)
      connection.setAutoCommit(false)

      // 删除旧的离线推荐（如果是离线推荐）
      if (recommendType == "OFFLINE") {
        val userIds = allRecs.map(_.getAs[Any]("userId")).map {
          case l: java.lang.Long => l.longValue()
          case i: java.lang.Integer => i.longValue()
        }
        val inClause = userIds.map(_ => "?").mkString(",")
        deleteStmt = connection.prepareStatement(
          s"DELETE FROM recommendation_result WHERE user_id IN ($inClause) AND `type` = ?"
        )
        userIds.zipWithIndex.foreach { case (userId, idx) =>
          deleteStmt.setLong(idx + 1, userId)
        }
        deleteStmt.setString(userIds.length + 1, recommendType)
        val deletedCount = deleteStmt.executeUpdate()
        println(s"[INFO] 已删除 $deletedCount 条旧的 $recommendType 推荐数据")
      }

      // 准备插入语句
      insertStmt = connection.prepareStatement(
        if (modelId.nonEmpty) {
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
        } else {
          """
            |INSERT INTO recommendation_result
            |(user_id, movie_id, score, `rank`, `type`, create_time, update_time)
            |VALUES (?, ?, ?, ?, ?, ?, ?)
            |ON DUPLICATE KEY UPDATE
            |score = VALUES(score),
            |`rank` = VALUES(`rank`),
            |update_time = VALUES(update_time)
          """.stripMargin
        }
      )

      val currentTime = new Timestamp(System.currentTimeMillis())

      // 批量插入所有推荐数据
      var totalCount = 0
      allRecs.foreach { row =>
        val userId = {
          val value = row.getAs[Any]("userId")
          value match {
            case l: java.lang.Long => l.longValue()
            case i: java.lang.Integer => i.longValue()
            case _ => throw new ClassCastException(s"Unexpected userId type: ${value.getClass.getName}")
          }
        }
        val recs = row.getAs[Seq[Row]]("recommendations")

        if (recs != null && recs.nonEmpty) {
          recs.zipWithIndex.foreach { case (rec, rank) =>
            val movieId = {
              val value = rec.getAs[Any](0)
              value match {
                case l: java.lang.Long => l.longValue()
                case i: java.lang.Integer => i.longValue()
                case _ => throw new ClassCastException(s"Unexpected movieId type: ${value.getClass.getName}")
              }
            }
            val score = rec.getAs[Float](1)

            insertStmt.setLong(1, userId)
            insertStmt.setLong(2, movieId)
            insertStmt.setDouble(3, score.toDouble)
            insertStmt.setInt(4, rank + 1)  // 排名从 1 开始
            insertStmt.setString(5, recommendType)
            if (modelId.nonEmpty) {
              insertStmt.setString(6, modelId)
              insertStmt.setTimestamp(7, currentTime)
              insertStmt.setTimestamp(8, currentTime)
            } else {
              insertStmt.setTimestamp(6, currentTime)
              insertStmt.setTimestamp(7, currentTime)
            }

            insertStmt.addBatch()
            totalCount += 1

            // 每 1000 条提交一次
            if (totalCount % 1000 == 0) {
              insertStmt.executeBatch()
              connection.commit()
            }
          }
        }
      }

      // 提交剩余数据
      insertStmt.executeBatch()
      connection.commit()

      println(s"[INFO] 推荐结果写入完成，共插入 $totalCount 条推荐记录")

    } catch {
      case e: Exception =>
        if (connection != null) {
          connection.rollback()
        }
        println(s"[ERROR] 写入 MySQL 失败: ${e.getMessage}")
        e.printStackTrace()
        throw e
    } finally {
      // 关闭资源
      if (deleteStmt != null) deleteStmt.close()
      if (insertStmt != null) insertStmt.close()
      if (connection != null) connection.close()
    }
  }

  /**
   * 将一个分区的数据写入 MySQL
   *
   * @param partition     分区数据迭代器
   * @param recommendType 推荐类型
   */
  private def writePartitionToMySQL(partition: Iterator[Row], recommendType: String): Unit = {
    var connection: Connection = null
    var insertStmt: PreparedStatement = null
    var deleteStmt: PreparedStatement = null

    try {
      // 建立 JDBC 连接
      connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD)
      connection.setAutoCommit(false)

      // 修复：补充 modelId 变量（原代码此处引用 modelId 但未定义，属于逻辑漏洞）
      val modelId = "" // 分区写入暂不关联模型ID，如需关联可改为方法参数

      // 准备插入语句
      insertStmt = connection.prepareStatement(
        if (modelId.nonEmpty) {
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
        } else {
          """
            |INSERT INTO recommendation_result
            |(user_id, movie_id, score, `rank`, `type`, create_time, update_time)
            |VALUES (?, ?, ?, ?, ?, ?, ?)
            |ON DUPLICATE KEY UPDATE
            |score = VALUES(score),
            |`rank` = VALUES(`rank`),
            |update_time = VALUES(update_time)
          """.stripMargin
        }
      )

      val currentTime = new Timestamp(System.currentTimeMillis())

      // 批量插入
      var count = 0
      var userCount = 0
      partition.foreach { row =>
        try {
          val userId = {
            val value = row.getAs[Any]("userId")
            value match {
              case l: java.lang.Long => l.longValue()
              case i: java.lang.Integer => i.longValue()
              case _ => throw new ClassCastException(s"Unexpected userId type: ${value.getClass.getName}")
            }
          }
          val recs = row.getAs[Seq[Row]]("recommendations")

          if (recs != null && recs.nonEmpty) {
            userCount += 1
            recs.zipWithIndex.foreach { case (rec, rank) =>
              val movieId = {
                val value = rec.getAs[Any](0)
                value match {
                  case l: java.lang.Long => l.longValue()
                  case i: java.lang.Integer => i.longValue()
                  case _ => throw new ClassCastException(s"Unexpected movieId type: ${value.getClass.getName}")
                }
              }
              val score = rec.getAs[Float](1)

              insertStmt.setLong(1, userId)
              insertStmt.setLong(2, movieId)
              insertStmt.setDouble(3, score.toDouble)
              insertStmt.setInt(4, rank + 1)  // 排名从 1 开始
              insertStmt.setString(5, recommendType)
              if (modelId.nonEmpty) {
                insertStmt.setString(6, modelId)
                insertStmt.setTimestamp(7, currentTime)
                insertStmt.setTimestamp(8, currentTime)
              } else {
                insertStmt.setTimestamp(6, currentTime)
                insertStmt.setTimestamp(7, currentTime)
              }

              insertStmt.addBatch()
              count += 1

              // 每 1000 条提交一次
              if (count % 1000 == 0) {
                insertStmt.executeBatch()
                connection.commit()
              }
            }
          } else {
            println(s"[WARN] 用户 $userId 的推荐列表为空")
          }
        } catch {
          case e: Exception =>
            println(s"[ERROR] 处理用户数据失败: $row, 错误: ${e.getMessage}")
            e.printStackTrace()
        }
      }

      // 提交剩余数据
      insertStmt.executeBatch()
      connection.commit()

      println(s"[INFO] 分区写入完成，共插入 $count 条推荐记录，处理用户数: $userCount")

    } catch {
      case e: Exception =>
        if (connection != null) {
          connection.rollback()
        }
        println(s"[ERROR] 写入 MySQL 失败: ${e.getMessage}")
        e.printStackTrace()
        throw e
    } finally {
      // 关闭资源
      if (insertStmt != null) insertStmt.close()
      if (deleteStmt != null) deleteStmt.close() // 补充关闭 deleteStmt
      if (connection != null) connection.close()
    }
  }

  /**
   * 将模型参数写入 MySQL
   *
   * @param modelPath  模型路径
   * @param rank       隐因子数量
   * @param regParam   正则化参数
   * @param maxIter    最大迭代次数
   * @param rmse       模型评估指标
   * @return           生成的模型ID
   */
  def writeModelParamsToMySQL(
                               modelPath: String,
                               rank: Int,
                               regParam: Double,
                               maxIter: Int,
                               rmse: Double
                             ): String = {
    var connection: Connection = null
    var stmt: PreparedStatement = null
    var modelId: String = null

    try {
      connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD)
      connection.setAutoCommit(false)

      // 将旧模型标记为 DEPRECATED
      stmt = connection.prepareStatement(
        "UPDATE model_params SET status = 'DEPRECATED' WHERE status = 'ACTIVE'"
      )
      stmt.executeUpdate()
      stmt.close()

      // 插入新模型参数
      modelId = s"als-model-${System.currentTimeMillis()}"
      stmt = connection.prepareStatement(
        """
          |INSERT INTO model_params
          |(model_id, `rank`, reg_param, max_iter, training_time, model_path, rmse, status, create_time)
          |VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE', NOW())
        """.stripMargin
      )

      stmt.setString(1, modelId)
      stmt.setInt(2, rank)
      stmt.setDouble(3, regParam)
      stmt.setInt(4, maxIter)
      stmt.setTimestamp(5, new Timestamp(System.currentTimeMillis()))
      stmt.setString(6, modelPath)
      stmt.setDouble(7, rmse)

      stmt.executeUpdate()
      connection.commit()

      println(s"[INFO] 模型参数已写入 MySQL，Model ID: $modelId")
      modelId // 返回生成的模型ID

    } catch {
      case e: Exception =>
        if (connection != null) {
          connection.rollback()
        }
        println(s"[ERROR] 写入模型参数失败: ${e.getMessage}")
        throw e
    } finally {
      if (stmt != null) stmt.close()
      if (connection != null) connection.close()
    }
  }
}