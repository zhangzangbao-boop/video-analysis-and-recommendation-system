package com.shortvideo.recommendation.realtime.app

import com.shortvideo.recommendation.common.config.{KafkaConfig, SparkConfig}
import com.shortvideo.recommendation.common.utils.{KafkaUtil, SparkUtil}
import com.shortvideo.recommendation.realtime.BehaviorParser
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe

object RealtimeAnalysisApp {

  def main(args: Array[String]): Unit = {
    // 1. 加载配置
    val sparkConfig = SparkConfig.createStreamingConfig()
    val kafkaConfig = KafkaConfig.createConsumerConfig()

    // 2. 创建 StreamingContext
    // 注意：batchDuration 在 SparkUtil 中默认为 10 秒
    val ssc = SparkUtil.createStreamingContext(sparkConfig.appName)

    // 3. 构造 Kafka 参数
    // 我们从 KafkaConfig 对象中提取配置并映射为 Kafka 官方驱动要求的参数格式
    val kafkaParams = Map[String, Object](
      "bootstrap.servers"  -> kafkaConfig.bootstrapServers,
      "key.deserializer"   -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id"           -> kafkaConfig.groupId,
      "auto.offset.reset"  -> kafkaConfig.autoOffsetReset,
      "enable.auto.commit" -> (kafkaConfig.enableAutoCommit: java.lang.Boolean)
    )

    // 4. 定义要订阅的 Topic（需与 Flume Sink 中的配置保持一致）
    val topics = Array("user_behaviors")

    // 5. 创建 Kafka 直连流
    // 使用 Direct 方式，由 Spark 直接管理 Offset
    val rawKafkaStream = KafkaUtils.createDirectStream[String, String](
      ssc,
      PreferConsistent,
      Subscribe[String, String](topics, kafkaParams)
    )

    // 6. 调用解析器：原始 Kafka 消息 → UserBehavior 实体流
    // BehaviorParser.parse 内部封装了 JSON 解析、异常处理和数据清洗过滤
    val behaviorStream = BehaviorParser.parse(rawKafkaStream)

    // 7. 测试输出：打印解析后的实体（生产环境应替换为具体 Processor 逻辑）
    behaviorStream.foreachRDD { rdd =>
      if (!rdd.isEmpty()) {
        println(s"--- 批次处理开始：捕获到 ${rdd.count()} 条有效行为数据 ---")
        rdd.take(5).foreach(println) // 打印前5条
      }
    }

    // 8. 启动流处理
    ssc.start()
    ssc.awaitTermination()
  }
}