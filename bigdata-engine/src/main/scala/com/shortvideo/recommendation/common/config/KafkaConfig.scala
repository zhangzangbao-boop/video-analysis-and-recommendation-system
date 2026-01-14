package com.shortvideo.recommendation.common.config

import com.shortvideo.recommendation.common.utils.ConfigUtils

/**
 * Kafka配置
 */
case class KafkaConfig(
                        bootstrapServers: String,
                        groupId: String,
                        autoOffsetReset: String,
                        enableAutoCommit: Boolean,
                        autoCommitIntervalMs: Int,
                        sessionTimeoutMs: Int,
                        maxPollRecords: Int,
                        maxPollIntervalMs: Int,
                        acks: String,
                        retries: Int,
                        batchSize: Int,
                        lingerMs: Int,
                        bufferMemory: Long,
                        compressionType: String,
                        securityEnabled: Boolean,
                        securityProtocol: String,
                        saslMechanism: String
                      )

object KafkaConfig {

  def apply(): KafkaConfig = {
    KafkaConfig(
      bootstrapServers = ConfigUtils.getString("kafka.bootstrap.servers", "localhost:9092"),
      groupId = ConfigUtils.getString("kafka.group.id", "shortvideo-recommendation"),
      autoOffsetReset = ConfigUtils.getString("kafka.auto.offset.reset", "latest"),
      enableAutoCommit = ConfigUtils.getBoolean("kafka.enable.auto.commit", true),
      autoCommitIntervalMs = ConfigUtils.getInt("kafka.auto.commit.interval.ms", 5000),
      sessionTimeoutMs = ConfigUtils.getInt("kafka.session.timeout.ms", 30000),
      maxPollRecords = ConfigUtils.getInt("kafka.max.poll.records", 500),
      maxPollIntervalMs = ConfigUtils.getInt("kafka.max.poll.interval.ms", 300000),
      acks = ConfigUtils.getString("kafka.producer.acks", "1"),
      retries = ConfigUtils.getInt("kafka.producer.retries", 3),
      batchSize = ConfigUtils.getInt("kafka.producer.batch.size", 16384),
      lingerMs = ConfigUtils.getInt("kafka.producer.linger.ms", 1),
      bufferMemory = ConfigUtils.getLong("kafka.producer.buffer.memory", 33554432L),
      compressionType = ConfigUtils.getString("kafka.producer.compression.type", "snappy"),
      securityEnabled = ConfigUtils.getBoolean("kafka.security.enabled", false),
      securityProtocol = ConfigUtils.getString("kafka.security.protocol", "SASL_PLAINTEXT"),
      saslMechanism = ConfigUtils.getString("kafka.sasl.mechanism", "PLAIN")
    )
  }

  /**
   * 创建生产者配置
   */
  def createProducerConfig(): KafkaConfig = {
    val config = KafkaConfig()
    config.copy(
      groupId = s"${config.groupId}-producer"
    )
  }

  /**
   * 创建消费者配置
   */
  def createConsumerConfig(): KafkaConfig = {
    val config = KafkaConfig()
    config.copy(
      enableAutoCommit = ConfigUtils.getBoolean("kafka.consumer.enable.auto.commit", false),
      autoOffsetReset = ConfigUtils.getString("kafka.consumer.auto.offset.reset", "earliest")
    )
  }
}