package com.shortvideo.recommendation.common

import com.shortvideo.recommendation.common.utils.ConfigUtils

/**
 * 常量定义类
 */
object Constants {

  // 应用名称
  val APP_NAME = "ShortVideoRecommendation"

  // 环境变量
  object Env {
    val DEV = "dev"
    val TEST = "test"
    val PROD = "prod"
  }

  // Kafka配置
  object Kafka {
    val BOOTSTRAP_SERVERS = "kafka.bootstrap.servers"
    val GROUP_ID = "kafka.group.id"
    val AUTO_OFFSET_RESET = "kafka.auto.offset.reset"
    val ENABLE_AUTO_COMMIT = "kafka.enable.auto.commit"

    // Topic名称
    val TOPIC_USER_BEHAVIOR = "shortvideo_user_behavior"
    val TOPIC_CONTENT_EXPOSURE = "shortvideo_content_exposure"
    val TOPIC_USER_INTERACTION = "shortvideo_user_interaction"
    val TOPIC_RECOMMEND_RESULT = "shortvideo_recommend_result"

    // 序列化器
    val KEY_DESERIALIZER = "org.apache.kafka.common.serialization.StringDeserializer"
    val VALUE_DESERIALIZER = "org.apache.kafka.common.serialization.StringDeserializer"
  }

  // Spark配置
  object Spark {
    val MASTER = "spark.master"
    val APP_NAME = "spark.app.name"
    val EXECUTOR_MEMORY = "spark.executor.memory"
    val EXECUTOR_CORES = "spark.executor.cores"
    val DRIVER_MEMORY = "spark.driver.memory"
    val SQL_SHUFFLE_PARTITIONS = "spark.sql.shuffle.partitions"
    val STREAMING_BLOCK_INTERVAL = "spark.streaming.blockInterval"
    val STREAMING_KAFKA_MAX_RATE_PER_PARTITION = "spark.streaming.kafka.maxRatePerPartition"
  }

  // Redis配置
  object Redis {
    val HOST = "redis.host"
    val PORT = "redis.port"
    val PASSWORD = "redis.password"
    val DATABASE = "redis.database"
    val TIMEOUT = "redis.timeout"
    val MAX_TOTAL = "redis.max.total"
    val MAX_IDLE = "redis.max.idle"
    val MIN_IDLE = "redis.min.idle"

    // Key前缀
    val KEY_PREFIX_USER_RECOMMEND = "rec:user:"
    val KEY_PREFIX_VIDEO_SIMILAR = "rec:video:sim:"
    val KEY_PREFIX_VIDEO_HOT = "rec:video:hot"
    val KEY_PREFIX_USER_FEATURE = "feature:user:"
    val KEY_PREFIX_VIDEO_FEATURE = "feature:video:"

    // 过期时间（秒）
    val EXPIRE_ONE_DAY = 86400
    val EXPIRE_ONE_HOUR = 3600
    val EXPIRE_THIRTY_MINUTES = 1800
    val EXPIRE_TEN_MINUTES = 600
  }

  // HDFS路径
  object HDFS {
    // 从配置文件读取基础路径，如果没有配置则使用默认值
    // 默认使用本地文件系统路径，适合本地开发环境
    val BASE_PATH: String = {
      val configPath = ConfigUtils.getString("hdfs.base.path", "file:///tmp/shortvideo")
      configPath
    }

    // 数据层路径
    val ODS_PATH: String = {
      val path = ConfigUtils.getString("hdfs.ods.path", s"$BASE_PATH/ods")
      path
    }
    
    val DWD_PATH: String = {
      val path = ConfigUtils.getString("hdfs.dwd.path", s"$BASE_PATH/dwd")
      path
    }
    
    val DWS_PATH: String = {
      val path = ConfigUtils.getString("hdfs.dws.path", s"$BASE_PATH/dws")
      path
    }
    
    val ADS_PATH: String = {
      val path = ConfigUtils.getString("hdfs.ads.path", s"$BASE_PATH/ads")
      path
    }

    // 模型路径
    val MODEL_PATH: String = {
      val path = ConfigUtils.getString("hdfs.model.path", s"$BASE_PATH/models")
      path
    }

    // 日志路径
    val LOG_PATH: String = {
      val path = ConfigUtils.getString("hdfs.log.path", s"$BASE_PATH/logs")
      path
    }
  }

  // 用户行为类型
  object BehaviorType {
    val VIEW = "view"           // 观看
    val LIKE = "like"           // 点赞
    val COMMENT = "comment"     // 评论
    val SHARE = "share"         // 分享
    val COLLECT = "collect"     // 收藏
    val FOLLOW = "follow"       // 关注
    val UNFOLLOW = "unfollow"   // 取消关注

    // 行为权重（用于评分计算）
    val WEIGHTS = Map(
      VIEW -> 1.0,
      LIKE -> 2.0,
      COMMENT -> 3.0,
      SHARE -> 4.0,
      COLLECT -> 3.0,
      FOLLOW -> 5.0,
      UNFOLLOW -> -5.0
    )
  }

  // 时间格式
  object TimeFormat {
    val YYYYMMDD = "yyyyMMdd"
    val YYYY_MM_DD = "yyyy-MM-dd"
    val YYYYMMDDHH = "yyyyMMddHH"
    val YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss"
    val ISO_8601 = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
  }

  // 推荐相关常量
  object Recommendation {
    val TOP_N = 100                     // 每个用户的推荐数量
    val SIMILAR_VIDEOS_COUNT = 50       // 每个视频的相似视频数量
    val HOT_VIDEOS_COUNT = 1000         // 热门视频数量
    val ALS_RANK = 20                   // ALS模型rank参数
    val ALS_MAX_ITER = 10               // ALS模型最大迭代次数
    val ALS_REG_PARAM = 0.01            // ALS模型正则化参数
    val MIN_RATING = 0.1                // 最小评分
    val MAX_RATING = 5.0                // 最大评分
  }

  // 配置文件名
  object ConfigFile {
    val APPLICATION = "application.conf"
    val LOG4J = "log4j.properties"
  }
}
