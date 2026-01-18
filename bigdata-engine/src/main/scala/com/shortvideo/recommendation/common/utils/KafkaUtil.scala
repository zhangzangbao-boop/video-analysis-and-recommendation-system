package com.shortvideo.recommendation.common.utils


import java.util.Properties
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}

import scala.collection.JavaConverters._
import com.shortvideo.recommendation.common.Constants
import com.shortvideo.recommendation.common.config.KafkaConfig
import org.apache.kafka.common.TopicPartition

import java.{lang, util}
import scala.sys.props

/**
 * Kafka工具类
 */
object KafkaUtil {

  /**
   * 创建Kafka生产者配置
   */
  def createProducerProperties(config: KafkaConfig): Properties = {
    val props = new Properties()

    props.put("bootstrap.servers", config.bootstrapServers)
    props.put("key.serializer", classOf[StringSerializer].getName)
    props.put("value.serializer", classOf[StringSerializer].getName)
    props.put("acks", config.acks)
    props.put("retries", config.retries.toString)
    props.put("batch.size", config.batchSize.toString)
    props.put("linger.ms", config.lingerMs.toString)
    props.put("buffer.memory", config.bufferMemory.toString)
    props.put("compression.type", config.compressionType)

    // 安全配置（如果有）
    if (config.securityEnabled) {
      props.put("security.protocol", config.securityProtocol)
      props.put("sasl.mechanism", config.saslMechanism)
    }

    props
  }

  /**
   * 创建Kafka消费者配置
   */
  def createConsumerProperties(config: KafkaConfig): Properties = {
    val props = new Properties()

    props.put("bootstrap.servers", config.bootstrapServers)
    props.put("key.deserializer", classOf[StringDeserializer].getName)
    props.put("value.deserializer", classOf[StringDeserializer].getName)
    props.put("group.id", config.groupId)
    props.put("auto.offset.reset", config.autoOffsetReset)
    props.put("enable.auto.commit", config.enableAutoCommit.toString)
    props.put("auto.commit.interval.ms", config.autoCommitIntervalMs.toString)
    props.put("session.timeout.ms", config.sessionTimeoutMs.toString)
    props.put("max.poll.records", config.maxPollRecords.toString)
    props.put("max.poll.interval.ms", config.maxPollIntervalMs.toString)

    // 安全配置（如果有）
    if (config.securityEnabled) {
      props.put("security.protocol", config.securityProtocol)
      props.put("sasl.mechanism", config.saslMechanism)
    }

    props
  }

  def getOffsets(kafkaParams: Properties, topics: Array[String]): Map[TopicPartition, Long] = {

    val consumer =KafkaUtil.createConsumer(KafkaConfig.apply(), topics.toList)
    val offsets = consumer.assignment().asScala.map { partition =>
      val offset = consumer.position(partition)
      (partition, offset)
    }.toMap
    return offsets
  }

  /**
   * 创建Kafka生产者
   */
  def createProducer(config: KafkaConfig): KafkaProducer[String, String] = {
    val props = createProducerProperties(config)
    new KafkaProducer[String, String](props)
  }

  /**
   * 创建Kafka消费者
   */
  def createConsumer(config: KafkaConfig, topics: List[String]): KafkaConsumer[String, String] = {
    val props = createConsumerProperties(config)
    val consumer = new KafkaConsumer[String, String](props)
    consumer.subscribe(topics.asJava)
    consumer
  }

  /**
   * 发送消息到Kafka
   */
  def sendMessage(producer: KafkaProducer[String, String],
                  topic: String,
                  key: String,
                  message: String): Unit = {

    val record = new ProducerRecord[String, String](topic, key, message)
    producer.send(record)
  }

  /**
   * 批量发送消息到Kafka
   */
  def sendMessages(producer: KafkaProducer[String, String],
                   topic: String,
                   messages: List[(String, String)]): Unit = {

    messages.foreach { case (key, message) =>
      sendMessage(producer, topic, key, message)
    }

    producer.flush()
  }

  /**
   * 从Kafka消费消息
   */
  def consumeMessages(consumer: KafkaConsumer[String, String],
                      pollTimeout: Long = 1000): List[String] = {

    val records = consumer.poll(java.time.Duration.ofMillis(pollTimeout))

    records.asScala.map(_.value()).toList
  }

  /**
   * 关闭Kafka生产者
   */
  def closeProducer(producer: KafkaProducer[String, String]): Unit = {
    if (producer != null) {
      producer.close()
    }
  }

  /**
   * 关闭Kafka消费者
   */
  def closeConsumer(consumer: KafkaConsumer[String, String]): Unit = {
    if (consumer != null) {
      consumer.close()
    }
  }

  /**
   * 获取Kafka Topic列表
   */
  def listTopics(config: KafkaConfig): Set[String] = {
    val props = createConsumerProperties(config)
    val consumer = new KafkaConsumer[String, String](props)

    try {
      val topics = consumer.listTopics()
      topics.keySet().asScala.toSet
    } finally {
      closeConsumer(consumer)
    }
  }

  /**
   * 检查Kafka连接
   */
  def checkKafkaConnection(config: KafkaConfig): Boolean = {
    try {
      val topics = listTopics(config)
      println(s"Successfully connected to Kafka. Available topics: ${topics.mkString(", ")}")
      true
    } catch {
      case e: Exception =>
        println(s"Failed to connect to Kafka: ${e.getMessage}")
        false
    }
  }
}