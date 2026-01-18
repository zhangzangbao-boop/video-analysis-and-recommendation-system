package com.shortvideo.recommendation.offline.job

import org.apache.spark.sql.SparkSession
import java.sql.{Connection, DriverManager, PreparedStatement}
import java.time.LocalDate

/**
 * 离线统计分析任务
 * 修改说明：已升级为读取 DWD 层 Hive 表数据
 */
object OfflineJob {

  // MySQL 配置
  private val JDBC_URL = "jdbc:mysql://localhost:3306/short_video_platform?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai"
  private val JDBC_USER = "root"
  private val JDBC_PASSWORD = "root"

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("ShortVideoOfflineAnalysis")
      .master("local[*]")
      .enableHiveSupport() // 开启 Hive
      .getOrCreate()

    import spark.implicits._

    val today = LocalDate.now().toString

    try {
      spark.sql("USE short_video_dw")

      println(s"[INFO] 开始离线统计任务 (基于 DWD 表, dt=$today)...")

      // ========================================
      // 任务 1: 更新热门视频 (基于 DWD 清洗后的数据)
      // ========================================
      // 规则: 播放*1 + 点赞*5 + 评论*3 + 分享*4
      val hotVideoDF = spark.sql(
        s"""
           |SELECT
           |  video_id as videoId,
           |  (sum(CASE WHEN behavior_type='play' THEN 1 ELSE 0 END) * 1 +
           |   sum(CASE WHEN behavior_type='like' THEN 1 ELSE 0 END) * 5 +
           |   sum(CASE WHEN behavior_type='comment' THEN 1 ELSE 0 END) * 3 +
           |   sum(CASE WHEN behavior_type='share' THEN 1 ELSE 0 END) * 4) as hot_score
           |FROM dwd_behavior_log
           |WHERE dt = '$today'
           |GROUP BY video_id
           |ORDER BY hot_score DESC
           |LIMIT 100
        """.stripMargin)

      // 如果 DWD 表今天没数据，这里会是空的，不会报错
      if (hotVideoDF.isEmpty) {
        println(s"[WARN] DWD 表 dt=$today 无数据，跳过热门统计")
      } else {
        val hotVideoIds = hotVideoDF.select("videoId").as[Long].collect()
        updateHotVideosInMySQL(hotVideoIds)
      }

      // ========================================
      // 任务 2: 统计每日核心指标 (DAU)
      // ========================================
      println("[INFO] 开始统计每日指标...")

      // 使用 Hive SQL 快速统计
      val statsDF = spark.sql(
        s"""
           |SELECT
           |  count(DISTINCT user_id) as dau,
           |  count(*) as total_interaction
           |FROM dwd_behavior_log
           |WHERE dt = '$today'
        """.stripMargin).collect()

      if (statsDF.nonEmpty) {
        val row = statsDF(0)
        val dau = row.getAs[Long]("dau")
        val totalInteractions = row.getAs[Long]("total_interaction")

        println(s"  - 日期: $today")
        println(s"  - DAU: $dau")
        println(s"  - 互动量: $totalInteractions")

        saveDailyStats(today, "dau", dau.toDouble)
        saveDailyStats(today, "total_interaction", totalInteractions.toDouble)
      }

    } catch {
      case e: Exception =>
        e.printStackTrace()
        println(s"[ERROR] 离线分析任务失败: ${e.getMessage}")
    } finally {
      spark.stop()
    }
  }

  // ---------------------------------------------------------
  // 以下 MySQL 辅助方法保持不变
  // ---------------------------------------------------------

  def updateHotVideosInMySQL(hotVideoIds: Array[Long]): Unit = {
    // ... (保持之前的代码内容不变) ...
    var conn: Connection = null
    var stmt: PreparedStatement = null
    try {
      conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD)
      conn.setAutoCommit(false)
      val resetSql = "UPDATE video_info SET is_hot = 0 WHERE is_hot = 1"
      stmt = conn.prepareStatement(resetSql)
      stmt.executeUpdate()
      stmt.close()

      val updateSql = "UPDATE video_info SET is_hot = 1 WHERE id = ?"
      stmt = conn.prepareStatement(updateSql)
      var count = 0
      for (vid <- hotVideoIds) {
        stmt.setLong(1, vid)
        stmt.addBatch()
        count += 1
        if (count % 50 == 0) stmt.executeBatch()
      }
      stmt.executeBatch()
      conn.commit()
      println(s"[INFO] 热门视频列表已更新，标记数量: ${hotVideoIds.length}")
    } catch {
      case e: Exception =>
        if (conn != null) conn.rollback()
        println(s"[ERROR] 更新热门视频失败: ${e.getMessage}")
    } finally {
      if (stmt != null) stmt.close()
      if (conn != null) conn.close()
    }
  }

  def saveDailyStats(date: String, metricName: String, value: Double): Unit = {
    // ... (保持之前的代码内容不变) ...
    var conn: Connection = null
    var stmt: PreparedStatement = null
    try {
      conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD)
      val sql = "INSERT INTO sys_statistics_daily (stat_date, metric_name, metric_value) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE metric_value = VALUES(metric_value)"
      stmt = conn.prepareStatement(sql)
      stmt.setString(1, date)
      stmt.setString(2, metricName)
      stmt.setBigDecimal(3, new java.math.BigDecimal(value))
      stmt.executeUpdate()
    } catch {
      case e: Exception => println(s"[ERROR] 写入统计失败: $metricName - ${e.getMessage}")
    } finally {
      if (stmt != null) stmt.close()
      if (conn != null) conn.close()
    }
  }
}