package com.shortvideo.recommendation.common.config

import com.shortvideo.recommendation.common.utils.ConfigUtils
import scala.collection.mutable

/**
 * Spark配置
 */
case class SparkConfig(
                        master: String,
                        appName: String,
                        executorMemory: String,
                        executorCores: Int,
                        driverMemory: String,
                        sqlShufflePartitions: Int,
                        streamingBlockInterval: Int,
                        streamingKafkaMaxRatePerPartition: Int,
                        enableHiveSupport: Boolean,
                        logLevel: String
                      ) {

  /**
   * 获取Spark配置Map
   */
  def getConfigMap: Map[String, String] = {
    val configs = mutable.Map[String, String]()

    configs += ("spark.executor.memory" -> executorMemory)
    configs += ("spark.executor.cores" -> executorCores.toString)
    configs += ("spark.driver.memory" -> driverMemory)
    configs += ("spark.sql.shuffle.partitions" -> sqlShufflePartitions.toString)
    configs += ("spark.streaming.blockInterval" -> s"${streamingBlockInterval}ms")
    configs += ("spark.streaming.kafka.maxRatePerPartition" -> streamingKafkaMaxRatePerPartition.toString)

    // 序列化配置
    configs += ("spark.serializer" -> "org.apache.spark.serializer.KryoSerializer")
    configs += ("spark.kryoserializer.buffer.max" -> "256m")

    // 动态分配配置
    configs += ("spark.dynamicAllocation.enabled" -> "true")
    configs += ("spark.dynamicAllocation.minExecutors" -> "1")
    configs += ("spark.dynamicAllocation.maxExecutors" -> "10")
    configs += ("spark.dynamicAllocation.initialExecutors" -> "1")

    // SQL优化配置
    configs += ("spark.sql.adaptive.enabled" -> "true")
    configs += ("spark.sql.adaptive.coalescePartitions.enabled" -> "true")
    configs += ("spark.sql.adaptive.skewJoin.enabled" -> "true")

    configs.toMap
  }
}

object SparkConfig {

  def apply(): SparkConfig = {
    SparkConfig(
      master = ConfigUtils.getString("spark.master", "local[*]"),
      appName = ConfigUtils.getString("spark.app.name", "ShortVideoRecommendation"),
      executorMemory = ConfigUtils.getString("spark.executor.memory", "2g"),
      executorCores = ConfigUtils.getInt("spark.executor.cores", 2),
      driverMemory = ConfigUtils.getString("spark.driver.memory", "2g"),
      sqlShufflePartitions = ConfigUtils.getInt("spark.sql.shuffle.partitions", 200),
      streamingBlockInterval = ConfigUtils.getInt("spark.streaming.blockInterval", 200),
      streamingKafkaMaxRatePerPartition = ConfigUtils.getInt("spark.streaming.kafka.maxRatePerPartition", 1000),
      enableHiveSupport = ConfigUtils.getBoolean("spark.enableHiveSupport", true),
      logLevel = ConfigUtils.getString("spark.logLevel", "WARN")
    )
  }

  /**
   * 创建批处理Spark配置
   */
  def createBatchConfig(): SparkConfig = {
    val config = SparkConfig()
    config.copy(
      appName = s"${config.appName}_Batch"
    )
  }

  /**
   * 创建流处理Spark配置
   */
  def createStreamingConfig(): SparkConfig = {
    val config = SparkConfig()
    config.copy(
      appName = s"${config.appName}_Streaming",
      streamingBlockInterval = ConfigUtils.getInt("spark.streaming.blockInterval", 100),
      streamingKafkaMaxRatePerPartition = ConfigUtils.getInt("spark.streaming.kafka.maxRatePerPartition", 500)
    )
  }
}