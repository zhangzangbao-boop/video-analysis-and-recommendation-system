package com.shortvideo.recommendation.als

import com.shortvideo.recommendation.als.model.Rating
import org.apache.spark.sql.{Dataset, SparkSession}
import org.apache.spark.sql.functions._

/**
 * 数据预处理工具类
 * 负责从 HDFS 读取原始日志数据并转换为 Rating 格式
 */
object DataProcessor {

  /**
   * 行为类型对应的评分权重
   */
  private val BEHAVIOR_RATING = Map(
    1 -> 3.0f, // 播放
    2 -> 5.0f, // 点赞
    3 -> 4.0f, // 收藏
    4 -> 4.0f  // 评论
  )

  /**
   * 从 HDFS 读取并解析行为日志
   *
   * @param spark    SparkSession
   * @param hdfsPath HDFS 日志路径（支持通配符）
   * @return Dataset[Rating]
   */
  def readAndParseBehaviorLogs(spark: SparkSession, hdfsPath: String): Dataset[Rating] = {
    import spark.implicits._

    println(s"[INFO] 开始读取 HDFS 日志数据: $hdfsPath")

    // 读取 HDFS 上的 JSON 格式日志文件
    val rawLogs = spark.read.json(hdfsPath)

    // 统计原始日志数量
    val rawCount = rawLogs.count()
    println(s"[INFO] 读取到原始日志记录数: $rawCount")

    // 解析日志数据并转换为 Rating 格式
    // JSON格式：{"behaviorId":xxx,"userId":xxx,"mid":xxx,"behaviorType":xxx,"createDate":xxx}
    val ratings = rawLogs
      .select(
        col("userId").cast("long"),
        col("mid").cast("long").as("movieId"),
        col("behaviorType").cast("int"),
        col("createDate").cast("long").as("timestamp")
      )
      .filter(col("userId").isNotNull)
      .filter(col("movieId").isNotNull)
      .filter(col("behaviorType").isNotNull)
      .map(row => {
        try {
          val userId = row.getAs[Long]("userId")
          val movieId = row.getAs[Long]("movieId")
          val behaviorType = row.getAs[Int]("behaviorType")
          val timestamp = row.getAs[Long]("timestamp")

          // 根据行为类型获取评分权重
          val rating = BEHAVIOR_RATING.getOrElse(behaviorType, 1.0f)

          Rating(userId, movieId, rating, timestamp)
        } catch {
          case e: Exception =>
            println(s"[WARN] 解析日志失败: $row, 错误: ${e.getMessage}")
            // 返回无效的 Rating，后续会被过滤
            Rating(-1L, -1L, 0.0f, 0L)
        }
      })
      .filter(_.userId > 0)  // 过滤无效数据
      .filter(_.movieId > 0)

    // 统计有效的评分数量
    val validCount = ratings.count()
    println(s"[INFO] 解析后的有效评分记录数: $validCount")

    ratings
  }

  /**
   * 聚合同一用户对同一视频（物品）的多次行为评分
   * 核心逻辑：对(userId, movieId)维度的重复记录，保留最高评分作为最终评分
   * 解决问题：避免同一用户对同一视频的多次交互行为导致评分重复，保证(userId, movieId)唯一
   *
   * @param ratings 原始评分数据集，每条记录包含用户ID、视频ID、评分、时间戳
   *                类型：Dataset[Rating]（Rating需包含userId, movieId, rating, timestamp字段）
   * @return 聚合后的评分数据集，每个(userId, movieId)仅保留一条最高评分记录
   */
  def aggregateRatings(ratings: Dataset[Rating]): Dataset[Rating] = {
    // 导入SparkSession的隐式转换，支持Dataset的算子操作（如groupByKey、mapGroups等）
    import ratings.sparkSession.implicits._

    // 打印日志，便于跟踪数据处理进度
    println("[INFO] 开始聚合评分数据...")

    // 核心聚合逻辑：
    val aggregated = ratings
      // 1. 按(userId, movieId)二元组分组，这是聚合的核心维度
      //    目的：把同一用户对同一视频的所有评分记录归到同一组
      .groupByKey(r => (r.userId, r.movieId))
      // 2. 对每个分组内的记录进行处理（mapGroups是按组遍历的算子）
      //    入参：((userId, movieId), iter) -> 分组键 + 该组下的所有Rating迭代器
      .mapGroups { case ((userId, movieId), iter) =>
        // 3. 从分组内的所有评分中提取最大值（核心业务规则：取最高评分）
        //    注意：iter是迭代器，遍历后会耗尽，需确保仅调用一次max
        val maxRating = iter.map(_.rating).max
        // 4. 构造聚合后的Rating对象
        //    时间戳使用当前系统时间（也可优化为取评分最新的那条记录的时间戳）
        Rating(userId, movieId, maxRating, System.currentTimeMillis())
      }

    // 统计聚合后的记录数，打印日志便于验证聚合效果（比如对比原始数据看去重比例）
    val aggCount = aggregated.count()
    println(s"[INFO] 聚合后的评分记录数: $aggCount")

    // 返回聚合后的数据集
    aggregated
  }

  /**
   * 过滤数据质量
   * 移除行为过少的用户和视频
   *
   * @param ratings      Dataset[Rating]
   * @param minUserBehaviors 用户最小行为次数
   * @param minMovieBehaviors 视频最小被互动次数
   * @return Dataset[Rating]
   */
  def filterByQuality(
                       ratings: Dataset[Rating],
                       minUserBehaviors: Int = 5,
                       minMovieBehaviors: Int = 5
                     ): Dataset[Rating] = {
    // 导入隐式转换
    import ratings.sparkSession.implicits._

    // 打印日志：跟踪过滤进度
    println(s"[INFO] 开始数据质量过滤 (用户最小行为: $minUserBehaviors, 视频最小互动: $minMovieBehaviors)...")

    // 第一步：统计每个用户的行为次数
    val userCounts = ratings
      .groupByKey(_.userId)          // 按用户ID分组
      .mapGroups { case (userId, iter) =>
        (userId, iter.size)         // 计算每个用户的行为记录数
      }

    // 第二步：统计每个视频的被互动次数
    val movieCounts = ratings
      .groupByKey(_.movieId)         // 按视频ID分组
      .mapGroups { case (movieId, iter) =>
        (movieId, iter.size)        // 计算每个视频的被互动记录数
      }

    // 第三步：筛选活跃用户和活跃视频（满足最小行为次数要求）
    // collect()转为本地Set：便于后续过滤（小数据量场景适用，大数据建议用join）
    val activeUsers = userCounts
      .filter(_._2 >= minUserBehaviors)  // 保留行为≥minUserBehaviors的用户
      .map(_._1)                         // 提取用户ID
      .collect().toSet                   // 转为Set（提高contains查询效率）

    val activeMovies = movieCounts
      .filter(_._2 >= minMovieBehaviors) // 保留被互动≥minMovieBehaviors的视频
      .map(_._1)                         // 提取视频ID
      .collect().toSet                   // 转为Set

    // 打印日志：监控活跃用户/视频规模
    println(s"[INFO] 活跃用户数: ${activeUsers.size}")
    println(s"[INFO] 活跃视频数: ${activeMovies.size}")

    // 第四步：过滤数据：仅保留活跃用户对活跃视频的评分
    val filtered = ratings
      .filter(r => activeUsers.contains(r.userId) && activeMovies.contains(r.movieId))

    // 统计过滤后记录数：监控数据过滤比例
    val filteredCount = filtered.count()
    println(s"[INFO] 质量过滤后的评分记录数: $filteredCount")

    // 返回高质量评分数据集（可直接用于ALS模型训练）
    filtered
  }
}
