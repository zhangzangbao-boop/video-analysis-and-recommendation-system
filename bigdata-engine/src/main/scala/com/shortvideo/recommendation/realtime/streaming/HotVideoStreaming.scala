package com.shortvideo.recommendation.realtime.streaming

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.shortvideo.recommendation.common.config.{KafkaConfig, SparkConfig}
import com.shortvideo.recommendation.common.entity.UserBehavior
import com.shortvideo.recommendation.common.utils.{ConfigUtils, KafkaUtil, RedisUtil, SparkUtil}
import com.shortvideo.recommendation.realtime.entity.{HotVideo, RealtimeBehavior}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.SparkConf
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, KafkaUtils, LocationStrategies}
import org.apache.spark.streaming.{Duration, Seconds, StreamingContext}
import org.apache.spark.sql.SparkSession

import scala.collection.JavaConverters._

/**
 * 实时热门视频流处理
 * 从Kafka消费用户行为数据，计算实时热门视频并存入Redis
 */
class HotVideoStreaming {
  private val objectMapper = new ObjectMapper().registerModule(DefaultScalaModule)

  /**
   * 创建实时热门视频流处理
   * @param sparkConf Spark配置
   * @param kafkaConfig Kafka配置
   * @param windowDuration 窗口持续时间
   * @return StreamingContext
   */
  def createStreamingContext(sparkConf: SparkConf, kafkaConfig: KafkaConfig, windowDuration: Duration = Seconds(60)): StreamingContext = {
    val ssc = new StreamingContext(sparkConf, windowDuration)

    // 创建Kafka流
    val kafkaParams = KafkaUtil.createConsumerProperties(kafkaConfig)
    kafkaParams.put("group.id", s"${kafkaConfig.groupId}_hot_video")
    kafkaParams.put("auto.offset.reset", "latest")
    kafkaParams.put("enable.auto.commit", "false")

    val scalaKafkaParams = kafkaParams.asScala.toMap.asInstanceOf[Map[String, Object]]

    val topics = Array(ConfigUtils.getString("kafka.topics.user-behavior", "shortvideo_user_behavior"))

    val offsets= KafkaUtil.getOffsets(kafkaParams, topics)
    //打印一下 offsets 的大小
    println(s"offsets size: ${offsets.size}")

    val kafkaStream: InputDStream[ConsumerRecord[String, String]] = KafkaUtils.createDirectStream(
      ssc,
      LocationStrategies.PreferConsistent,
      ConsumerStrategies.Subscribe[String, String](
        topics.toIterable,
        scalaKafkaParams,
        offsets)
    )

    // 解析用户行为数据
    val userBehaviorStream = kafkaStream.map(record => {
      val jsonStr = record.value()
      parseUserBehavior(jsonStr)
    }).filter(_.isDefined).map(_.get)

    // 计算实时热门视频
    calculateHotVideos(userBehaviorStream)

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
   * 计算实时热门视频
   */
  private def calculateHotVideos(userBehaviorStream: org.apache.spark.streaming.dstream.DStream[RealtimeBehavior]) = {
    // 按视频ID和行为类型统计
    val videoBehaviorStats = userBehaviorStream
      .map(behavior => ((behavior.videoId, behavior.behaviorType), 1))
      .reduceByKey(_ + _)

    // 按视频ID汇总各种行为
    val videoStats = userBehaviorStream
      .map(behavior => {
        val score = behavior.behaviorType match {
          case "view" => 1.0
          case "like" => 5.0
          case "comment" => 3.0
          case "share" => 4.0
          case "collect" => 3.0
          case "follow" => 5.0
          case _ => 0.0
        }
        (behavior.videoId, (1L, if (behavior.behaviorType == "like") 1L else 0L, 
                          if (behavior.behaviorType == "comment") 1L else 0L, 
                          if (behavior.behaviorType == "share") 1L else 0L, score))
      })
      .reduceByKey((a, b) => (a._1 + b._1, a._2 + b._2, a._3 + b._3, a._4 + b._4, a._5 + b._5))

    // 更新热门视频到Redis
    videoStats.foreachRDD(rdd => {
      if (!rdd.isEmpty()) {
        val hotVideos = rdd.collect().map { case (videoId, (totalCount, likeCount, commentCount, shareCount, totalScore)) =>
          HotVideo(
            videoId = videoId,
            hotScore = totalScore,
            behaviorCount = totalCount,
            likeCount = likeCount,
            commentCount = commentCount,
            shareCount = shareCount
          )
        }.sortBy(-_.hotScore).take(50) // 取前50个热门视频

        // 存储到Redis
        storeHotVideosToRedis(hotVideos)
      }
    })
  }

  /**
   * 将热门视频存储到Redis
   */
  private def storeHotVideosToRedis(hotVideos: Array[HotVideo]): Unit = {
    val hotVideoKey = "hot_videos:realtime"
    
    // 使用ZSet存储热门视频，视频ID为member，热度分为score
    val videoScores = hotVideos.map(hv => hv.videoId.toString -> hv.hotScore).toMap
    RedisUtil.deleteKey(hotVideoKey) // 清除旧数据
    RedisUtil.zaddBatch(hotVideoKey, videoScores)
    RedisUtil.expire(hotVideoKey, 300) // 5分钟过期
    
    // 同时保存完整的热门视频信息
    hotVideos.foreach { hotVideo =>
      val detailKey = s"hot_video_detail:${hotVideo.videoId}"
      val detailData = Map(
        "hotScore" -> hotVideo.hotScore.toString,
        "behaviorCount" -> hotVideo.behaviorCount.toString,
        "likeCount" -> hotVideo.likeCount.toString,
        "commentCount" -> hotVideo.commentCount.toString,
        "shareCount" -> hotVideo.shareCount.toString,
        "updateTime" -> hotVideo.updateTime.toString
      )
      RedisUtil.hmset(detailKey, detailData)
      RedisUtil.expire(detailKey, 300) // 5分钟过期
    }
  }
}