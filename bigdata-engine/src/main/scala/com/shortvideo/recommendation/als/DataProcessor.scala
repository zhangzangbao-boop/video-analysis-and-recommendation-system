package com.shortvideo.recommendation.als

import com.shortvideo.recommendation.als.model.Rating
import org.apache.spark.sql.{Dataset, SparkSession}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._

object DataProcessor {

  /**
   * 行为类型对应的评分权重映射
   * 依据文档 2.4 节及 4.3 节：
   * 播放=3.0, 点赞=5.0, 收藏=4.0, 评论=4.0
   */
  private def getBehaviorWeight(behaviorType: String): Float = behaviorType match {
    case "play" => 3.0f    // 文档要求：3.0
    case "like" => 5.0f    // 文档要求：5.0
    case "collect" => 4.0f // 文档要求：4.0
    case "comment" => 4.0f // 文档要求：4.0
    case _ => 0.0f
  }

  /**
   * 从 HDFS 读取并解析 JSON 行为日志
   * 文档流程步骤：READ_HDFS -> PARSE -> CONVERT
   */
  def readAndParseBehaviorLogs(spark: SparkSession, hdfsPath: String): Dataset[Rating] = {
    import spark.implicits._

    println(s"[INFO] 开始读取 HDFS JSON 数据: $hdfsPath")

    try {
      // 1. 读取 JSON 数据
      val rawDF = spark.read.json(hdfsPath)

      // 2. 注册 UDF 处理行为权重
      val behaviorToWeightUDF = udf((typeStr: String) => getBehaviorWeight(typeStr))

      // 3. 数据转换与清洗
      val ratings = rawDF
        // 确保关键字段存在
        .filter(col("userId").isNotNull && col("mid").isNotNull && col("behaviorType").isNotNull)
        .select(
          col("userId").cast(LongType),
          col("mid").cast(LongType).as("movieId"), // 文档 Log 示例中字段可能为 mid，需注意映射
          col("behaviorType"),
          col("createDate").as("behaviorTime")     // 假设日志字段名为 createDate
        )
        // 转换 behaviorType -> rating
        .withColumn("rating", behaviorToWeightUDF(col("behaviorType")))
        // 过滤无权重的行为
        .filter(col("rating") > 0)
        .select(
          col("userId"),
          col("movieId"),
          col("rating"),
          current_timestamp().cast(LongType).as("timestamp") // 简化时间处理
        )
        .as[Rating]

      val validCount = ratings.count()
      println(s"[INFO] 解析后的有效评分记录数: $validCount")

      ratings

    } catch {
      case e: Exception =>
        println(s"[ERROR] 读取或解析数据失败: ${e.getMessage}")
        spark.emptyDataset[Rating]
    }
  }

  /**
   * 聚合用户评分：同一用户对同一视频取最高分
   * 文档流程步骤：AGGREGATE
   */
  def aggregateRatings(ratings: Dataset[Rating]): Dataset[Rating] = {
    import ratings.sparkSession.implicits._
    println("[INFO] 执行评分聚合 (取最高分)...")

    ratings
      .groupByKey(r => (r.userId, r.movieId))
      .mapGroups { case ((uid, mid), iter) =>
        val maxRating = iter.map(_.rating).max
        Rating(uid, mid, maxRating, System.currentTimeMillis())
      }
  }

  /**
   * 过滤数据质量
   * 文档流程步骤：FILTER
   */
  def filterByQuality(
                       ratings: Dataset[Rating],
                       minUserBehaviors: Int = 5,
                       minMovieBehaviors: Int = 5
                     ): Dataset[Rating] = {
    import ratings.sparkSession.implicits._

    println(s"[INFO] 开始数据质量过滤 (用户行为>=$minUserBehaviors, 视频互动>=$minMovieBehaviors)...")

    // 1. 统计用户行为数
    val userCounts = ratings.groupBy("userId").count().filter(col("count") >= minUserBehaviors)
    val validUsers = userCounts.select("userId").as[Long].collect().toSet

    // 2. 统计视频互动数
    val movieCounts = ratings.groupBy("movieId").count().filter(col("count") >= minMovieBehaviors)
    val validMovies = movieCounts.select("movieId").as[Long].collect().toSet

    // 3. 过滤
    val filtered = ratings.filter(r => validUsers.contains(r.userId) && validMovies.contains(r.movieId))

    println(s"[INFO] 质量过滤后的评分记录数: ${filtered.count()}")
    filtered
  }
}