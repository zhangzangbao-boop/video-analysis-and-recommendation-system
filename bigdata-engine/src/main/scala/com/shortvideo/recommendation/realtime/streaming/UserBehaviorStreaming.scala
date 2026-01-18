package com.shortvideo.recommendation.realtime.streaming

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.shortvideo.recommendation.common.config.{KafkaConfig, SparkConfig}
import com.shortvideo.recommendation.common.entity.UserBehavior
import com.shortvideo.recommendation.common.utils.{ConfigUtils, KafkaUtil, RedisUtil, SparkUtil}
import com.shortvideo.recommendation.realtime.entity.RealtimeBehavior
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.SparkConf
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, KafkaUtils, LocationStrategies}
import org.apache.spark.streaming.{Duration, Seconds, StreamingContext}
import org.apache.spark.sql.SparkSession

import scala.collection.JavaConverters._

/**
 * 实时用户行为流处理
 * 从Kafka消费用户行为数据，进行实时分析和推荐
 */
class UserBehaviorStreaming {
  private val objectMapper = new ObjectMapper().registerModule(DefaultScalaModule)

  /**
   * 创建实时用户行为流处理
   * @param sparkConf Spark配置
   * @param kafkaConfig Kafka配置
   * @param windowDuration 窗口持续时间
   * @return StreamingContext
   */
  def createStreamingContext(sparkConf: SparkConf, kafkaConfig: KafkaConfig, windowDuration: Duration = Seconds(30)): StreamingContext = {
    val ssc = new StreamingContext(sparkConf, windowDuration)

    // 创建Kafka流
    val kafkaParams = KafkaUtil.createConsumerProperties(kafkaConfig)
    kafkaParams.put("group.id", s"${kafkaConfig.groupId}_realtime")
    kafkaParams.put("auto.offset.reset", "latest")
    kafkaParams.put("enable.auto.commit", "false")

    val topics = Array(ConfigUtils.getString("kafka.topics.user-behavior", "shortvideo_user_behavior"))

    val offsets = KafkaUtil.getOffsets(kafkaParams, topics)

    val kafkaStream: InputDStream[ConsumerRecord[String, String]] = KafkaUtils.createDirectStream[String, String](
      ssc,
      LocationStrategies.PreferConsistent,
      ConsumerStrategies.Subscribe[String, String](topics, kafkaParams)
    )

    // 解析用户行为数据
    val userBehaviorStream = kafkaStream.map(record => {
      val jsonStr = record.value()
      parseUserBehavior(jsonStr)
    }).filter(_.isDefined).map(_.get)

    // 实时处理用户行为
    processUserBehaviorStream(userBehaviorStream)

    ssc
  }

  /**
   * 解析用户行为JSON数据
   */
  private def parseUserBehavior(jsonStr: String): Option[RealtimeBehavior] = {
    try {
      val userBehavior = objectMapper.readValue(jsonStr, classOf[UserBehavior])
      val behaviorType = userBehavior.behaviorType
      val timestamp = userBehavior.behaviorTime.getTime

      Some(RealtimeBehavior(
        userId = userBehavior.userId,
        videoId = userBehavior.videoId,
        behaviorType = behaviorType,
        timestamp = timestamp
      ))
    } catch {
      case e: Exception =>
        println(s"解析用户行为JSON失败: $jsonStr, 错误: ${e.getMessage}")
        None
    }
  }

  /**
   * 处理用户行为流
   */
  private def processUserBehaviorStream(userBehaviorStream: org.apache.spark.streaming.dstream.DStream[RealtimeBehavior]) = {
    // 实时统计用户行为
    userBehaviorStream.foreachRDD(rdd => {
      if (!rdd.isEmpty()) {
        val spark = SparkUtil.createSparkSessionFromConfig(SparkConfig.createStreamingConfig())
        import spark.implicits._

        val behaviorDF = rdd.toDF()
        behaviorDF.createOrReplaceTempView("temp_realtime_behavior")

        // 更新用户行为统计到Redis
        rdd.collect().foreach(behavior => {
          updateUserBehaviorStats(behavior)
        })

        spark.stop()
      }
    })

    // 实时计算用户兴趣
    userBehaviorStream
      .map(behavior => (behavior.userId, behavior.videoId, behavior.behaviorType, behavior.timestamp))
      .foreachRDD(rdd => {
        if (!rdd.isEmpty()) {
          rdd.collect().foreach { case (userId, videoId, behaviorType, timestamp) =>
            updateUserInterest(userId, videoId, behaviorType)
          }
        }
      })
  }

  /**
   * 更新用户行为统计数据到Redis
   * 记录用户的各种行为类型及其计数，并设置相应的过期时间
   *
   * @param behavior 实时用户行为对象，包含用户ID、视频ID、行为类型等信息
   */
  private def updateUserBehaviorStats(behavior: RealtimeBehavior): Unit = {
    // 统计用户行为类型
    val userBehaviorKey = s"user_behavior:${behavior.userId}"
    val behaviorCountKey = s"behavior_count:${behavior.behaviorType}"
    
    RedisUtil.hincrBy(userBehaviorKey, behavior.behaviorType, 1)
    RedisUtil.expire(userBehaviorKey, 24 * 60 * 60) // 24小时过期
    
    RedisUtil.incrBy(behaviorCountKey, 1)
    RedisUtil.expire(behaviorCountKey, 60 * 60) // 1小时过期
  }

  /**
   * 更新用户兴趣标签
   */
  private def updateUserInterest(userId: Long, videoId: Long, behaviorType: String): Unit = {
    val userInterestKey = s"user_interest:$userId"
    
    // 根据行为类型给予不同权重
    val weight = behaviorType match {
      case "view" => 1
      case "like" => 3
      case "comment" => 5
      case "share" => 8
      case "collect" => 4
      case "follow" => 10
      case _ => 0
    }
    
    if (weight > 0) {
      // 将视频ID作为member，权重作为score存储到ZSet中
      RedisUtil.zadd(userInterestKey, weight.toDouble, videoId.toString)
      RedisUtil.expire(userInterestKey, 7 * 24 * 60 * 60) // 7天过期
    }
  }
}