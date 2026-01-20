package com.shortvideo.recommendation.common.config

import com.shortvideo.recommendation.common.utils.ConfigUtils

/**
 * Redis配置
 */
case class RedisConfig(
                        host: String,
                        port: Int,
                        password: String,
                        database: Int,
                        timeout: Int,
                        maxTotal: Int,
                        maxIdle: Int,
                        minIdle: Int
                      )

object RedisConfig {

  def apply(): RedisConfig = {
    RedisConfig(
      host = ConfigUtils.getString("redis.host", "localhost"),
      port = ConfigUtils.getInt("redis.port", 6379),
      password = ConfigUtils.getString("redis.password", ""),
      database = ConfigUtils.getInt("redis.database", 0),
      timeout = ConfigUtils.getInt("redis.timeout", 2000),
      maxTotal = ConfigUtils.getInt("redis.max.total", 50),
      maxIdle = ConfigUtils.getInt("redis.max.idle", 10),
      minIdle = ConfigUtils.getInt("redis.min.idle", 5)
    )
  }

  /**
   * 创建Redis配置（别名方法，保持API一致性）
   */
  def createConfig(): RedisConfig = {
    RedisConfig()
  }
}