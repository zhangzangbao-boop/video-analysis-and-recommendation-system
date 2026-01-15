package com.shortvideo.recommendation.common.exception

/**
 * Kafka相关异常
 */
class KafkaException(message: String, cause: Throwable = null)
  extends RuntimeException(message, cause)

/**
 * Kafka连接异常
 */
class KafkaConnectionException(message: String, cause: Throwable = null)
  extends KafkaException(s"Kafka连接失败: $message", cause)

/**
 * Kafka消息发送异常
 */
class KafkaSendException(message: String, cause: Throwable = null)
  extends KafkaException(s"Kafka消息发送失败: $message", cause)

/**
 * Kafka消息消费异常
 */
class KafkaConsumeException(message: String, cause: Throwable = null)
  extends KafkaException(s"Kafka消息消费失败: $message", cause)