package com.shortvideo.recommendation.realtime.mysql

import com.shortvideo.recommendation.common.config.DatabaseConfig
import com.shortvideo.recommendation.realtime.model.VideoInfoRow

import java.sql.{Connection, DriverManager, ResultSet, Timestamp}
import scala.collection.mutable.ArrayBuffer

/**
 * 从 MySQL 的 video_info 表加载视频数据（实时模块启动时使用）
 */
object VideoInfoMySQLLoader {

  private val JDBC_URL = DatabaseConfig.JDBC_URL
  private val JDBC_USER = DatabaseConfig.JDBC_USER
  private val JDBC_PASSWORD = DatabaseConfig.JDBC_PASSWORD

  def loadAll(): Seq[VideoInfoRow] = {
    var conn: Connection = null
    var rs: ResultSet = null

    val buffer = ArrayBuffer[VideoInfoRow]()

    try {
      Class.forName("com.mysql.cj.jdbc.Driver")
      conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD)

      // 只加载可推荐数据（status=1 且 is_deleted=0）
      val sql =
        """
          |SELECT
          |  id, user_id, title, description, video_url, cover_url, category_id, tags, duration,
          |  status, audit_msg, is_hot, view_count, like_count, comment_count, share_count,
          |  create_time, update_time, is_deleted
          |FROM video_info
          |WHERE is_deleted = 0 AND status = 1
        """.stripMargin

      val stmt = conn.prepareStatement(sql)
      rs = stmt.executeQuery()

      while (rs.next()) {
        buffer += VideoInfoRow(
          id = rs.getLong("id"),
          userId = rs.getLong("user_id"),
          title = safeString(rs.getString("title")),
          description = safeString(rs.getString("description")),
          videoUrl = safeString(rs.getString("video_url")),
          coverUrl = safeString(rs.getString("cover_url")),
          categoryId = rs.getLong("category_id"),
          tags = safeString(rs.getString("tags")),
          duration = rs.getInt("duration"),
          status = rs.getInt("status"),
          auditMsg = safeString(rs.getString("audit_msg")),
          isHot = rs.getInt("is_hot"),
          viewCount = rs.getLong("view_count"),
          likeCount = rs.getLong("like_count"),
          commentCount = rs.getLong("comment_count"),
          shareCount = rs.getLong("share_count"),
          createTime = getTimestampOrNow(rs, "create_time"),
          updateTime = getTimestampOrNow(rs, "update_time"),
          isDeleted = rs.getInt("is_deleted")
        )
      }

      println(s"[INFO] video_info 加载完成，可推荐视频数=${buffer.size}")
      buffer.toSeq
    } catch {
      case e: Exception =>
        println(s"[ERROR] 读取 MySQL video_info 失败: ${e.getMessage}")
        e.printStackTrace()
        Seq.empty
    } finally {
      try if (rs != null) rs.close() catch { case _: Exception => }
      try if (conn != null) conn.close() catch { case _: Exception => }
    }
  }

  private def safeString(s: String): String = Option(s).getOrElse("")

  private def getTimestampOrNow(rs: ResultSet, col: String): Timestamp = {
    val ts = rs.getTimestamp(col)
    if (ts != null) ts else new Timestamp(System.currentTimeMillis())
  }
}

