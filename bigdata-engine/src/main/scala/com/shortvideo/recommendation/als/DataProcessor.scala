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
   * 
   * 支持两种格式：
   * 1. 字符串格式：play, like, collect, comment
   * 2. 整数格式：1=播放, 2=点赞, 3=收藏, 4=评论
   * 
   * 注意：其他行为类型（share, follow, unfollow等）不参与评分
   */
  private def getBehaviorWeight(behaviorType: Any): Float = {
    behaviorType match {
      // 字符串格式（当前日志格式）
      case "play" | "1" => 3.0f    // 播放 = 3.0
      case "like" | "2" => 5.0f    // 点赞 = 5.0
      case "collect" | "3" => 4.0f // 收藏 = 4.0
      case "comment" | "4" => 4.0f // 评论 = 4.0
      // 整数格式（文档标准格式）
      case i: Int => i match {
        case 1 => 3.0f  // 播放
        case 2 => 5.0f  // 点赞
        case 3 => 4.0f  // 收藏
        case 4 => 4.0f  // 评论
        case _ => 0.0f
      }
      case l: Long => l.toInt match {
        case 1 => 3.0f
        case 2 => 5.0f
        case 3 => 4.0f
        case 4 => 4.0f
        case _ => 0.0f
      }
      // 其他行为类型（share, follow, unfollow等）不参与评分
      case _ => 0.0f
    }
  }

  /**
   * 从 HDFS 读取并解析 JSON 行为日志
   * 文档流程步骤：READ_HDFS -> PARSE -> CONVERT
   * 
   * 支持的字段格式：
   * - videoId 或 mid (视频ID)
   * - userId (用户ID)
   * - behaviorType (行为类型，支持字符串和整数)
   * - behaviorTime 或 createDate (行为时间)
   */
  def readAndParseBehaviorLogs(spark: SparkSession, hdfsPath: String): Dataset[Rating] = {
    import spark.implicits._

    println(s"[INFO] 开始读取 HDFS JSON 数据: $hdfsPath")

    try {
      // 1. 读取 JSON 数据
      val rawDF = spark.read.json(hdfsPath)
      
      val rawCount = rawDF.count()
      println(s"[INFO] 读取到原始日志记录数: $rawCount")
      
      if (rawCount == 0) {
        println("[WARN] HDFS路径下没有数据文件")
        // 尝试列出可能的文件结构以帮助调试
        println(s"[DEBUG] 检查路径模式: $hdfsPath")
        try {
          val hadoopConf = spark.sparkContext.hadoopConfiguration
          val fs = org.apache.hadoop.fs.FileSystem.get(hadoopConf)
          
          // 处理通配符路径
          var basePath = hdfsPath
          if (hdfsPath.contains("*")) {
            basePath = hdfsPath.substring(0, hdfsPath.indexOf("*"))
            if (!basePath.endsWith("/")) {
              val lastSlash = basePath.lastIndexOf("/")
              basePath = basePath.substring(0, lastSlash + 1)
            }
          } else {
            basePath = basePath.replaceAll("/[^/]*\\.json$", "")
          }
          
          val path = new org.apache.hadoop.fs.Path(basePath)
          if (fs.exists(path)) {
            val statuses = fs.listStatus(path)
            println(s"[DEBUG] 父路径存在，子目录/文件数量: ${statuses.length}")
            statuses.take(10).foreach(status => {
              println(s"  - ${status.getPath.toString}")
            })
          } else {
            println(s"[DEBUG] 父路径不存在: $path")
          }
        } catch {
          case e: Exception => 
            println(s"[DEBUG] 尝试列出目录时出现异常: ${e.getMessage}")
            // 不中断处理，因为Spark可能仍然可以通过通配符找到数据
        }
        return spark.emptyDataset[Rating]
      }

      // 显示原始数据的架构，用于调试
      println("[DEBUG] 原始数据Schema:")
      rawDF.printSchema()

      // 显示前几行数据示例
      println("[DEBUG] 原始数据样本 (前5行):")
      rawDF.show(5, truncate = false)

      // 2. 注册 UDF 处理行为权重（支持字符串和整数）
      val behaviorToWeightUDF = udf((behaviorType: Any) => {
        behaviorType match {
          case s: String => getBehaviorWeight(s)
          case i: Int => getBehaviorWeight(i)
          case l: Long => getBehaviorWeight(l)
          case _ => 0.0f
        }
      })

      // 3. 数据转换与清洗
      // 支持多种字段名：videoId/mid, behaviorTime/createDate
      val ratings = rawDF
        // 确保关键字段存在（支持多种字段名）
        .filter(
          col("userId").isNotNull && 
          (col("videoId").isNotNull || col("mid").isNotNull) && 
          col("behaviorType").isNotNull
        )
        .select(
          col("userId").cast(LongType).as("userId"),
          // 支持 videoId 和 mid 两种字段名
          coalesce(col("videoId"), col("mid")).cast(LongType).as("movieId"),
          col("behaviorType"),
          // 支持 behaviorTime 和 createDate 两种字段名，如果都没有则使用当前时间
          coalesce(
            col("behaviorTime"),
            col("createDate"),
            current_timestamp().cast(StringType)
          ).as("behaviorTime")
        )
        // 转换 behaviorType -> rating
        .withColumn("rating", behaviorToWeightUDF(col("behaviorType")))
        // 过滤无权重的行为（只保留 play, like, collect, comment）
        .filter(col("rating") > 0)
        // 处理时间戳：尝试解析时间字符串，失败则使用当前时间
        .withColumn("timestamp", 
          when(
            col("behaviorTime").rlike("^\\d{4}-\\d{2}-\\d{2}"), // 日期格式字符串 (yyyy-MM-dd HH:mm:ss)
            unix_timestamp(col("behaviorTime"), "yyyy-MM-dd HH:mm:ss").cast(LongType) * lit(1000L)
          ).otherwise(
            when(
              col("behaviorTime").rlike("^\\d+$"), // 纯数字（可能是时间戳）
              col("behaviorTime").cast(LongType)
            ).otherwise(
              unix_timestamp().cast(LongType) * lit(1000L) // 默认使用当前时间
            )
          )
        )
        .select(
          col("userId"),
          col("movieId"),
          col("rating").cast(FloatType),
          col("timestamp").cast(LongType)
        )
        .as[Rating]

      val validCount = ratings.count()
      println(s"[INFO] 解析后的有效评分记录数: $validCount")
      
      if (validCount > 0) {
        // 输出统计信息
        val userCount = ratings.select("userId").distinct().count()
        val movieCount = ratings.select("movieId").distinct().count()
        println(s"[INFO] 数据统计:")
        println(s"  - 用户数: $userCount")
        println(s"  - 视频数: $movieCount")
        println(s"  - 评分记录数: $validCount")
      } else {
        println("[WARN] 解析后没有有效的评分记录，请检查数据格式是否符合预期:")
        println("  - 必需字段: userId (用户ID), videoId/mid (视频ID), behaviorType (行为类型)")
        println("  - 行为类型: play/1(播放=3.0), like/2(点赞=5.0), collect/3(收藏=4.0), comment/4(评论=4.0)")
        println("  - 示例JSON格式: {\"userId\": 123, \"videoId\": 456, \"behaviorType\": \"play\", \"behaviorTime\": \"2025-01-20 10:30:00\"}")
      }

      ratings

    } catch {
      case e: Exception =>
        println(s"[ERROR] 读取或解析数据失败: ${e.getMessage}")
        e.printStackTrace()
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
    
    val beforeCount = ratings.count()
    println(s"[INFO] 聚合前记录数: $beforeCount")

    val aggregated = ratings
      .groupByKey(r => (r.userId, r.movieId))
      .mapGroups { case ((uid, mid), iter) =>
        val maxRating = iter.map(_.rating).max
        // 保留最新的时间戳
        val maxTimestamp = iter.map(_.timestamp).max
        Rating(uid, mid, maxRating, maxTimestamp)
      }
    
    val afterCount = aggregated.count()
    println(s"[INFO] 聚合后记录数: $afterCount (去重: ${beforeCount - afterCount} 条)")
    
    aggregated
  }

  /**
   * 过滤数据质量
   * 文档流程步骤：FILTER
   * 移除行为过少的用户和视频，保证模型训练质量
   */
  def filterByQuality(
                       ratings: Dataset[Rating],
                       minUserBehaviors: Int = 5,
                       minMovieBehaviors: Int = 5
                     ): Dataset[Rating] = {
    import ratings.sparkSession.implicits._

    val beforeCount = ratings.count()
    println(s"[INFO] 开始数据质量过滤 (用户行为>=$minUserBehaviors, 视频互动>=$minMovieBehaviors)...")
    println(s"[INFO] 过滤前记录数: $beforeCount")

    // 1. 统计用户行为数
    val userCounts = ratings
      .groupBy("userId")
      .count()
      .filter(col("count") >= minUserBehaviors)
    val validUsers = userCounts.select("userId").as[Long].collect().toSet
    println(s"[INFO] 活跃用户数 (行为>=$minUserBehaviors): ${validUsers.size}")

    // 2. 统计视频互动数
    val movieCounts = ratings
      .groupBy("movieId")
      .count()
      .filter(col("count") >= minMovieBehaviors)
    val validMovies = movieCounts.select("movieId").as[Long].collect().toSet
    println(s"[INFO] 活跃视频数 (互动>=$minMovieBehaviors): ${validMovies.size}")

    // 3. 过滤：只保留活跃用户对活跃视频的评分
    val filtered = ratings.filter(r => validUsers.contains(r.userId) && validMovies.contains(r.movieId))

    val afterCount = filtered.count()
    println(s"[INFO] 质量过滤后的评分记录数: $afterCount (过滤: ${beforeCount - afterCount} 条)")
    
    if (afterCount > 0) {
      val finalUserCount = filtered.select("userId").distinct().count()
      val finalMovieCount = filtered.select("movieId").distinct().count()
      println(s"[INFO] 最终数据统计:")
      println(s"  - 用户数: $finalUserCount")
      println(s"  - 视频数: $finalMovieCount")
      println(s"  - 评分记录数: $afterCount")
    }

    filtered
  }
}