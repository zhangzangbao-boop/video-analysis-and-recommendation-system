package com.shortvideo.recommendation.offline.job

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import java.sql.{Connection, DriverManager, PreparedStatement, Date => SQLDate}
import java.time.{LocalDate, LocalDateTime}
import java.time.format.DateTimeFormatter

/**
 * 离线统计分析任务
 * 
 * 功能：
 * 1. 从 HDFS 读取行为日志（支持日期分区）
 * 2. 计算热门视频（基于行为权重：播放3/点赞5/收藏4/评论4）
 * 3. 统计每日核心指标（DAU、互动量等）
 * 4. 写入 MySQL（video_info.is_hot, sys_statistics_daily）
 * 
 * 依据文档：短视频推荐系统技术方案.md
 */
object OfflineJob {

  // MySQL 连接配置（与 MySQLWriter 保持一致）
  private val JDBC_URL = "jdbc:mysql://localhost:3306/short_video_platform?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai"
  private val JDBC_USER = "root"
  private val JDBC_PASSWORD = "123456"

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("ShortVideoOfflineAnalysis")
      .master("local[*]")
      .config("spark.sql.shuffle.partitions", "100")
      .config("spark.sql.adaptive.enabled", "true")
      .getOrCreate()

    import spark.implicits._

    // 支持通过命令行参数指定日期，默认今天
    val statDate = if (args.length > 0 && args(0).nonEmpty) {
      LocalDate.parse(args(0), DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    } else {
      LocalDate.now()
    }
    val statDateStr = statDate.toString

    // HDFS 路径：支持日期分区和通配符
    // 格式1: hdfs://localhost:9000/short-video/behavior/logs/2025-01-20/*.json (指定日期)
    // 格式2: hdfs://localhost:9000/short-video/behavior/logs/*/*.json (所有历史数据)
    val inputPath = if (args.length > 1 && args(1).nonEmpty) {
      args(1) // 允许通过命令行参数指定路径
    } else {
      s"hdfs://localhost:9000/short-video/behavior/logs/$statDateStr/*.json"
    }

    println("=" * 80)
    println("短视频推荐系统 - 离线统计分析任务")
    println("=" * 80)
    println(s"[INFO] 统计日期: $statDateStr")
    println(s"[INFO] HDFS 输入路径: $inputPath")

    try {
      // ========================================
      // 1. 读取并解析原始日志
      // ========================================
      println(s"[INFO] 开始读取 HDFS 日志数据...")
      
      val rawDF = spark.read.json(inputPath)
      val rawCount = rawDF.count()
      println(s"[INFO] 读取到原始日志记录数: $rawCount")

      if (rawCount == 0) {
        println("[WARN] HDFS路径下没有数据文件，任务结束")
        spark.stop()
        return
      }

      // 字段映射：支持 videoId/mid 和 behaviorTime/createDate
      // 注意：如果数据中没有 mid 字段，coalesce 会报错，需要先检查列是否存在
      val hasMid = rawDF.columns.contains("mid")
      val hasVideoId = rawDF.columns.contains("videoId")
      val hasCreateDate = rawDF.columns.contains("createDate")
      
      println(s"[INFO] 数据字段检查: videoId=$hasVideoId, mid=$hasMid, createDate=$hasCreateDate")
      
      // 根据实际存在的字段进行映射
      val processedDF = {
        var df = rawDF
        
        // 处理 video_id：优先使用 videoId，如果不存在则尝试 mid
        if (hasVideoId) {
          df = df.withColumn("video_id", col("videoId").cast("long"))
        } else if (hasMid) {
          df = df.withColumn("video_id", col("mid").cast("long"))
        } else {
          throw new IllegalArgumentException("数据中既没有 videoId 也没有 mid 字段")
        }
        
        // 处理 user_id
        df = df.withColumn("user_id", col("userId").cast("long"))
        
        // 处理 behavior_type_raw
        df = df.withColumn("behavior_type_raw", col("behaviorType"))
        
        // 处理 behavior_time：优先使用 behaviorTime，如果不存在则尝试 createDate
        if (rawDF.columns.contains("behaviorTime")) {
          df = df.withColumn("behavior_time", col("behaviorTime"))
        } else if (hasCreateDate) {
          df = df.withColumn("behavior_time", col("createDate"))
        } else {
          // 如果都没有，使用当前时间戳
          df = df.withColumn("behavior_time", lit(System.currentTimeMillis()))
        }
        
        // 过滤空值
        df.filter(col("video_id").isNotNull && col("user_id").isNotNull)
      }

      processedDF.createOrReplaceTempView("behavior_log")

      // ========================================
      // 2. 任务 1: 计算热门视频 (Hot Video)
      // ========================================
      println("[INFO] 开始计算热门视频...")
      
      // 行为权重映射（符合文档要求）：
      // 播放(play/1) = 3.0, 点赞(like/2) = 5.0, 收藏(collect/3) = 4.0, 评论(comment/4) = 4.0
      val hotVideoDF = spark.sql(
        """
          |SELECT
          |  video_id,
          |  (sum(CASE 
          |        WHEN behavior_type_raw IN ('play', '1', 1) THEN 1 
          |        ELSE 0 
          |      END) * 3.0 +
          |   sum(CASE 
          |        WHEN behavior_type_raw IN ('like', '2', 2) THEN 1 
          |        ELSE 0 
          |      END) * 5.0 +
          |   sum(CASE 
          |        WHEN behavior_type_raw IN ('collect', '3', 3) THEN 1 
          |        ELSE 0 
          |      END) * 4.0 +
          |   sum(CASE 
          |        WHEN behavior_type_raw IN ('comment', '4', 4) THEN 1 
          |        ELSE 0 
          |      END) * 4.0) as hot_score
          |FROM behavior_log
          |WHERE video_id IS NOT NULL
          |GROUP BY video_id
          |ORDER BY hot_score DESC
          |LIMIT 100
        """.stripMargin)

      val hotVideoCount = hotVideoDF.count()
      println(s"[INFO] 计算出热门视频数: $hotVideoCount")

      if (hotVideoCount > 0) {
        val hotVideoIds = hotVideoDF.select("video_id").as[Long].collect()
        updateHotVideosInMySQL(hotVideoIds)
        println(s"[SUCCESS] 热门视频列表已更新到 MySQL，数量: ${hotVideoIds.length}")
      } else {
        println("[WARN] 未找到有效行为数据，跳过热门统计")
      }

      // ========================================
      // 3. 任务 2: 统计每日核心指标
      // ========================================
      println("[INFO] 开始统计每日核心指标...")

      val statsDF = spark.sql(
        """
          |SELECT
          |  count(DISTINCT user_id) as dau,
          |  count(*) as total_interaction,
          |  sum(CASE WHEN behavior_type_raw IN ('play', '1', 1) THEN 1 ELSE 0 END) as play_count,
          |  sum(CASE WHEN behavior_type_raw IN ('like', '2', 2) THEN 1 ELSE 0 END) as like_count,
          |  sum(CASE WHEN behavior_type_raw IN ('collect', '3', 3) THEN 1 ELSE 0 END) as collect_count,
          |  count(DISTINCT video_id) as active_video_count
          |FROM behavior_log
        """.stripMargin)

      val statsRow = statsDF.collect()
      if (statsRow.nonEmpty) {
        val row = statsRow(0)
        val dau = row.getAs[Long]("dau")
        val totalInteraction = row.getAs[Long]("total_interaction")
        val playCount = row.getAs[Long]("play_count")
        val likeCount = row.getAs[Long]("like_count")
        val collectCount = row.getAs[Long]("collect_count")
        val activeVideoCount = row.getAs[Long]("active_video_count")

        println(s"[INFO] 统计结果:")
        println(s"  - DAU (日活用户): $dau")
        println(s"  - 总互动量: $totalInteraction")
        println(s"  - 播放次数: $playCount")
        println(s"  - 点赞次数: $likeCount")
        println(s"  - 收藏次数: $collectCount")
        println(s"  - 活跃视频数: $activeVideoCount")

        // 写入 sys_statistics_daily 表
        writeDailyStatisticsToMySQL(statDate, dau, totalInteraction, playCount, likeCount, collectCount, activeVideoCount)
        println(s"[SUCCESS] 每日统计已写入 MySQL")
      }

      println("=" * 80)
      println("[SUCCESS] 离线统计分析任务执行完成")
      println("=" * 80)

    } catch {
      case e: Exception =>
        e.printStackTrace()
        println(s"[ERROR] 离线分析任务失败: ${e.getMessage}")
        throw e
    } finally {
      spark.stop()
    }
  }

  // ---------------------------------------------------------
  // MySQL 辅助方法
  // ---------------------------------------------------------

  /**
   * 更新热门视频列表到 MySQL
   * 表: video_info
   * 字段: is_hot (0-否, 1-是)
   */
  def updateHotVideosInMySQL(hotVideoIds: Array[Long]): Unit = {
    var conn: Connection = null
    var stmt: PreparedStatement = null
    try {
      conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD)
      conn.setAutoCommit(false)

      // 1. 重置所有视频的 is_hot 标志
      val resetSql = "UPDATE video_info SET is_hot = 0 WHERE is_hot = 1"
      stmt = conn.prepareStatement(resetSql)
      val resetCount = stmt.executeUpdate()
      stmt.close()
      println(s"[INFO] 已重置 $resetCount 个视频的热门标志")

      // 2. 批量更新热门视频
      val updateSql = "UPDATE video_info SET is_hot = 1 WHERE id = ?"
      stmt = conn.prepareStatement(updateSql)
      var count = 0
      for (videoId <- hotVideoIds) {
        stmt.setLong(1, videoId)
        stmt.addBatch()
        count += 1
        if (count % 50 == 0) {
          stmt.executeBatch()
        }
      }
      val updateCount = stmt.executeBatch().sum
      conn.commit()
      println(s"[INFO] 已更新 $updateCount 个视频为热门")
    } catch {
      case e: Exception =>
        if (conn != null) conn.rollback()
        println(s"[ERROR] 更新热门视频失败: ${e.getMessage}")
        e.printStackTrace()
        throw e
    } finally {
      if (stmt != null) stmt.close()
      if (conn != null) conn.close()
    }
  }

  /**
   * 写入每日统计指标到 MySQL
   * 表: sys_statistics_daily
   * 字段: stat_date, metric_name, metric_value
   */
  def writeDailyStatisticsToMySQL(
    statDate: LocalDate,
    dau: Long,
    totalInteraction: Long,
    playCount: Long,
    likeCount: Long,
    collectCount: Long,
    activeVideoCount: Long
  ): Unit = {
    var conn: Connection = null
    var stmt: PreparedStatement = null
    try {
      conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD)
      conn.setAutoCommit(false)

      // 使用 INSERT ... ON DUPLICATE KEY UPDATE 避免重复
      val insertSql = """
        |INSERT INTO sys_statistics_daily (stat_date, metric_name, metric_value, create_time)
        |VALUES (?, ?, ?, NOW())
        |ON DUPLICATE KEY UPDATE
        |  metric_value = VALUES(metric_value),
        |  create_time = NOW()
      """.stripMargin

      stmt = conn.prepareStatement(insertSql)
      val sqlDate = SQLDate.valueOf(statDate)

      // 写入各项指标
      val metrics = Seq(
        ("dau", dau.toDouble),
        ("total_interaction", totalInteraction.toDouble),
        ("play_count", playCount.toDouble),
        ("like_count", likeCount.toDouble),
        ("collect_count", collectCount.toDouble),
        ("active_video_count", activeVideoCount.toDouble)
      )

      for ((metricName, metricValue) <- metrics) {
        stmt.setDate(1, sqlDate)
        stmt.setString(2, metricName)
        stmt.setBigDecimal(3, java.math.BigDecimal.valueOf(metricValue))
        stmt.addBatch()
      }

      val updateCount = stmt.executeBatch().sum
      conn.commit()
      println(s"[INFO] 已写入 $updateCount 条统计指标到 sys_statistics_daily")
    } catch {
      case e: Exception =>
        if (conn != null) conn.rollback()
        println(s"[ERROR] 写入每日统计失败: ${e.getMessage}")
        e.printStackTrace()
        throw e
    } finally {
      if (stmt != null) stmt.close()
      if (conn != null) conn.close()
    }
  }
}