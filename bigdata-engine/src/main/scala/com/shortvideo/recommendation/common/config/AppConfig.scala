package com.shortvideo.recommendation.common.config

import com.shortvideo.recommendation.common.utils.ConfigUtils

/**
 * 应用配置
 */
case class AppConfig(
                      appName: String,
                      env: String,
                      logLevel: String,
                      checkpointEnabled: Boolean,
                      checkpointPath: String
                    )

object AppConfig {

  def apply(): AppConfig = {
    AppConfig(
      appName = ConfigUtils.getString("app.name", "ShortVideoRecommendation"),
      env = ConfigUtils.getString("app.env", "dev"),
      logLevel = ConfigUtils.getString("app.logLevel", "WARN"),
      checkpointEnabled = ConfigUtils.getBoolean("app.checkpoint.enabled", true),
      checkpointPath = ConfigUtils.getString("app.checkpoint.path", "/tmp/checkpoint")
    )
  }
}