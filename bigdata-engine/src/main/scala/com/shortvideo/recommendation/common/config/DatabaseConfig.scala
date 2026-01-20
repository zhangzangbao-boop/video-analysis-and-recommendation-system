package com.shortvideo.recommendation.common.config

/**
 * 数据库连接统一配置
 *
 * 说明：
 * - 按你的要求，将项目内散落的 url/user/password 抽取到 common 中统一维护
 * - 目前先以常量形式提供，后续也可以改为从 ConfigUtils/application.conf 读取
 */
object DatabaseConfig {
  // MySQL 连接配置（保持与现有代码一致）
  val JDBC_URL: String =
    "jdbc:mysql://localhost:3306/short_video_platform?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai"

  val JDBC_USER: String = "root"

  val JDBC_PASSWORD: String = "2468"
}

