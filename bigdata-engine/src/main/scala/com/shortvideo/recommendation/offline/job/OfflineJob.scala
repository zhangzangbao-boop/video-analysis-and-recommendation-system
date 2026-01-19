package com.shortvideo.recommendation.offline.job

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import java.sql.{Connection, DriverManager, PreparedStatement}
import java.time.LocalDate

/**
 * 离线统计分析任务
 * 修正说明：
 * 1. 数据库名适配后端设计: short_video_platform
 * 2. 表名适配后端设计: movies -> video_info
 * 3. 算法逻辑严格遵守技术文档 (权重: 播放3/点赞5/收藏4/评论4)
 */
object OfflineJob {

  // [修正] 数据库名与 application.yml 保持一致
  private val JDBC_URL = "jdbc:mysql://localhost:3306/short_video_platform?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai"
  private val JDBC_USER = "root"
  private val JDBC_PASSWORD = "root" // 请确保密码正确，application.yml中是123456，此处需匹配环境

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("ShortVideoOfflineAnalysis")
      .master("local[*]")
      .getOrCreate()

    import spark.implicits._

    val today = LocalDate.now().toString
    val inputPath = "/short-video/behavior/*/*.log"

    try {
      println(s"[INFO] 开始离线统计任务 (基于 HDFS 日志: $inputPath)...")

      // 1. 读取并解析原始日志
      val rawDF = spark.read.json(inputPath)
      rawDF.createOrReplaceTempView("behavior_log")

      // ========================================
      // 任务 1: 更新热门视频 (Hot Video)
      // ========================================
      // [算法保持文档一致] 权重: 播放=3, 点赞=5, 收藏=4, 评论=4
      // [字段适配后端数据库] mid -> video_id
      val hotVideoDF = spark.sql(
        """
          |SELECT
          |  cast(mid as Long) as video_id,
          |  (sum(CASE WHEN behaviorType IN ('play', '1') THEN 1 ELSE 0 END) * 3 +
          |   sum(CASE WHEN behaviorType IN ('like', '2') THEN 1 ELSE 0 END) * 5 +
          |   sum(CASE WHEN behaviorType IN ('collect', '3') THEN 1 ELSE 0 END) * 4 +
          |   sum(CASE WHEN behaviorType IN ('comment', '4') THEN 1 ELSE 0 END) * 4) as hot_score
          |FROM behavior_log
          |WHERE mid IS NOT NULL
          |GROUP BY mid
          |ORDER BY hot_score DESC
          |LIMIT 100
        """.stripMargin)

      if (hotVideoDF.isEmpty) {
        println(s"[WARN] 未找到有效行为数据，跳过热门统计")
      } else {
        val hotVideoIds = hotVideoDF.select("video_id").as[Long].collect()
        updateHotVideosInMySQL(hotVideoIds)
      }

      // ========================================
      // 任务 2: 统计每日核心指标
      // ========================================
      val statsDF = spark.sql(
        s"""
           |SELECT
           |  count(DISTINCT userId) as dau,
           |  count(*) as total_interaction
           |FROM behavior_log
        """.stripMargin).collect()

      if (statsDF.nonEmpty) {
        val row = statsDF(0)
        // 简单打印日志，如果需要写入 sys_statistics_daily 请确保表结构一致
        println(s"  - 日期: $today")
        println(s"  - DAU: ${row.getAs[Long]("dau")}")
        println(s"  - 互动量: ${row.getAs[Long]("total_interaction")}")
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
  // MySQL 辅助方法
  // ---------------------------------------------------------

  def updateHotVideosInMySQL(hotVideoIds: Array[Long]): Unit = {
    var conn: Connection = null
    var stmt: PreparedStatement = null
    try {
      conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD)
      conn.setAutoCommit(false)

      // [修正] 表名适配: movies -> video_info
      // [修正] 字段适配: is_hot 字段
      val resetSql = "UPDATE video_info SET is_hot = 0 WHERE is_hot = 1"
      stmt = conn.prepareStatement(resetSql)
      stmt.executeUpdate()
      stmt.close()

      // [修正] 主键适配: mid -> id
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
      println(s"[INFO] 热门视频列表已更新 (Table: video_info)，数量: ${hotVideoIds.length}")
    } catch {
      case e: Exception =>
        if (conn != null) conn.rollback()
        println(s"[ERROR] 更新热门视频失败: ${e.getMessage}")
    } finally {
      if (stmt != null) stmt.close()
      if (conn != null) conn.close()
    }
  }
}