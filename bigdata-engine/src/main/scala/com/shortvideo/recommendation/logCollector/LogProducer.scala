package com.shortvideo.recommendation.logCollector
//模拟Kafka生产者
import com.shortvideo.recommendation.common.config.KafkaConfig
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}

import java.util.Properties

class LogProducer(config: KafkaConfig) {
  private val props = new Properties()
  props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapServers)
  props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
  props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")

  private val producer = new KafkaProducer[String, String](props)

  def send(topic: String, message: String): Unit = {
    val record = new ProducerRecord[String, String](topic, message)
    producer.send(record)
  }

  def close(): Unit = producer.close()
}