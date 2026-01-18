package com.shortvideo.recommendation.als

import com.shortvideo.recommendation.als.model.Rating
import org.apache.spark.sql.{Dataset, SparkSession}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._

object DataProcessor {

  /**
   * 行为类型对应的评分权重映射
   * LogGenerator 生成的是字符串，这里需要映射为权重
   */
  private def getBehaviorWeight(behaviorType: String): Float = behaviorType match {
    case "play" => 1.0f    // 播放权重低，因为可能是自动播放
    case "like" => 5.0f    // 点赞权重高
    case "collect" => 4.0f // 收藏
    case "share" => 4.0f   // 分享
    case "comment" => 3.0f // 评论
    case "follow" => 2.0f  // 关注
    case _ => 0.0f
  }

  /**
   * 从 HDFS 读取并解析 JSON 行为日志
   *
   * @param spark    SparkSession
   * @param hdfsPath HDFS 日志路径（支持通配符）
   * @return Dataset[Rating]
   */
  def readAndParseBehaviorLogs(spark: SparkSession, hdfsPath: String): Dataset[Rating] = {
    import spark.implicits._

    println(s"[INFO] 开始读取 HDFS JSON 数据: $hdfsPath")

    try {
      // 1. 读取 JSON 数据
      // LogGenerator 生成的字段: userId, videoId, behaviorType(String), behaviorTime(String), etc.
      val rawDF = spark.read.json(hdfsPath)

      // 2. 注册 UDF 处理行为权重
      val behaviorToWeightUDF = udf((typeStr: String) => getBehaviorWeight(typeStr))

      // 3. 数据转换与清洗
      val ratings = rawDF
        // 过滤必要的字段是否存在
        .filter(col("userId").isNotNull && col("videoId").isNotNull && col("behaviorType").isNotNull)
        .select(
          col("userId").cast(LongType),
          col("videoId").cast(LongType).as("movieId"), // 映射 videoId -> movieId
          col("behaviorType"),
          col("behaviorTime")
        )
        // 转换 behaviorType (String) -> rating (Float)
        .withColumn("rating", behaviorToWeightUDF(col("behaviorType")))
        // 转换 behaviorTime (String) -> timestamp (Long)
        // 假设格式为 "yyyy-MM-dd HH:mm:ss.SSS" 或 Spark 能自动识别的时间格式
        .withColumn("timestamp", unix_timestamp(col("behaviorTime")).cast(LongType))
        .filter(col("rating") > 0) // 过滤掉无权重的行为
        .select(
          col("userId"),
          col("movieId"),
          col("rating"),
          col("timestamp")
        )
        .as[Rating] // 转换为 Dataset[Rating]

      val validCount = ratings.count()
      println(s"[INFO] 解析后的有效评分记录数: $validCount")

      ratings

    } catch {
      case e: Exception =>
        println(s"[ERROR] 读取或解析数据失败: ${e.getMessage}")
        // 返回空集防止报错中断后续流程（视情况而定）
        spark.emptyDataset[Rating]
    }
  }

  /**
   * 聚合用户评分：同一用户对同一视频取最大权重
   */
  def aggregateRatings(ratings: Dataset[Rating]): Dataset[Rating] = {
    import ratings.sparkSession.implicits._

    ratings
      .groupByKey(r => (r.userId, r.movieId))
      .mapGroups { case ((uid, mid), iter) =>
        val maxRating = iter.map(_.rating).max
        Rating(uid, mid, maxRating, System.currentTimeMillis())
      }
  }

  /**
   * 过滤数据质量 (保持原有逻辑)
   */
  def filterByQuality(
                       ratings: Dataset[Rating],
                       minUserBehaviors: Int = 5,
                       minMovieBehaviors: Int = 5
                     ): Dataset[Rating] = {
    import ratings.sparkSession.implicits._

    println(s"[INFO] 开始数据质量过滤 (用户最少行为: $minUserBehaviors, 视频最少互动: $minMovieBehaviors)...")

    // 1. 统计用户行为数
    val userCounts = ratings
      .groupByKey(_.userId)
      .mapGroups { case (userId, iter) => (userId, iter.size) }

    // 2. 统计视频互动数
    val movieCounts = ratings
      .groupByKey(_.movieId)
      .mapGroups { case (movieId, iter) => (movieId, iter.size) }

    // 3. 筛选活跃ID
    val activeUsers = userCounts
      .filter(_._2 >= minUserBehaviors)
      .map(_._1)
      .collect().toSet

    val activeMovies = movieCounts
      .filter(_._2 >= minMovieBehaviors)
      .map(_._1)
      .collect().toSet

    println(s"[INFO] 活跃用户数: ${activeUsers.size}")
    println(s"[INFO] 活跃视频数: ${activeMovies.size}")

    // 4. 过滤数据
    val filtered = ratings
      .filter(r => activeUsers.contains(r.userId) && activeMovies.contains(r.movieId))

    println(s"[INFO] 质量过滤后的评分记录数: ${filtered.count()}")

    filtered
  }
}