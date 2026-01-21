package com.shortvideo.recommendation.datagenerator

import com.shortvideo.recommendation.common.config.DatabaseConfig

import java.sql.{Connection, DriverManager, ResultSet}

/**
 * 从数据库加载视频ID列表的工具类
 */
object VideoIdLoader {
  
  private val JDBC_URL = DatabaseConfig.JDBC_URL
  private val JDBC_USER = DatabaseConfig.JDBC_USER
  private val JDBC_PASSWORD = DatabaseConfig.JDBC_PASSWORD

  /**
   * 从数据库加载所有可推荐视频的ID列表
   * @return 视频ID数组
   */
  def loadVideoIds(): Array[Long] = {
    var conn: Connection = null
    var rs: ResultSet = null

    val videoIds = scala.collection.mutable.ArrayBuffer[Long]()

    try {
      println(s"[VideoIdLoader] 开始加载视频ID...")
      println(s"[VideoIdLoader] 数据库URL: $JDBC_URL")
      println(s"[VideoIdLoader] 数据库用户: $JDBC_USER")
      
      println(s"[VideoIdLoader] 加载MySQL驱动...")
      Class.forName("com.mysql.cj.jdbc.Driver")
      println(s"[VideoIdLoader] MySQL驱动加载成功")
      
      println(s"[VideoIdLoader] 正在连接数据库...")
      val props = new java.util.Properties()
      props.setProperty("user", JDBC_USER)
      props.setProperty("password", JDBC_PASSWORD)
      props.setProperty("connectTimeout", "5000") // 5秒连接超时
      props.setProperty("socketTimeout", "10000") // 10秒socket超时
      conn = DriverManager.getConnection(JDBC_URL, props)
      println(s"[VideoIdLoader] 数据库连接成功")

      // 只加载可推荐数据（status=1 且 is_deleted=0）
      val sql =
        """
          |SELECT id
          |FROM video_info
          |WHERE is_deleted = 0 AND status = 1
          |ORDER BY id
        """.stripMargin

      val stmt = conn.prepareStatement(sql)
      rs = stmt.executeQuery()

      while (rs.next()) {
        videoIds += rs.getLong("id")
      }

      println(s"[INFO] 从数据库加载了 ${videoIds.size} 个可推荐视频ID")
      if (videoIds.isEmpty) {
        println(s"[WARN] 数据库中没有可推荐视频，将使用默认的模拟ID范围")
      } else {
        println(s"[INFO] 视频ID范围: ${videoIds.min} ~ ${videoIds.max}")
      }
      
      videoIds.toArray
    } catch {
      case e: Exception =>
        println(s"[ERROR] 从数据库加载视频ID失败: ${e.getMessage}")
        e.printStackTrace()
        println(s"[WARN] 将使用默认的模拟ID范围 (1-30)")
        Array.empty[Long]
    } finally {
      try if (rs != null) rs.close() catch { case _: Exception => }
      try if (conn != null) conn.close() catch { case _: Exception => }
    }
  }
}
