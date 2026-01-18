package com.shortvideo.recommendation.realtime.streaming

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.shortvideo.recommendation.common.config.{KafkaConfig, SparkConfig}
import com.shortvideo.recommendation.common.entity.{UserBehavior, Recommendation}
import com.shortvideo.recommendation.common.utils.{ConfigUtils, KafkaUtil, RedisUtil, SparkUtil}
import com.shortvideo.recommendation.realtime.entity.RealtimeBehavior
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.SparkConf
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, KafkaUtils, LocationStrategies}
import org.apache.spark.streaming.{Duration, Seconds, StreamingContext}
import org.apache.spark.sql.SparkSession

import scala.collection.JavaConverters._

/**
 * 实时推荐流处理
 * 基于实时用户行为生成推荐结果并存入Redis
 */
class RealtimeRecommendationStreaming {
  private val objectMapper = new ObjectMapper().registerModule(DefaultScalaModule)

  /**
   * 创建实时推荐流处理
   * @param sparkConf Spark配置
   * @param kafkaConfig Kafka配置
   * @param windowDuration 窗口持续时间
   * @return StreamingContext
   */
  def createStreamingContext(sparkConf: SparkConf, kafkaConfig: KafkaConfig, windowDuration: Duration = Seconds(30)): StreamingContext = {
    val ssc = new StreamingContext(sparkConf, windowDuration)

    // 创建Kafka流
    val kafkaParams = KafkaUtil.createConsumerProperties(kafkaConfig)
    kafkaParams.put("group.id", s"${kafkaConfig.groupId}_recommendation")
    kafkaParams.put("auto.offset.reset", "latest")
    kafkaParams.put("enable.auto.commit", "false")

    val topics = Array(
      ConfigUtils.getString("kafka.topics.user-behavior", "shortvideo_user_behavior"),
      ConfigUtils.getString("kafka.topics.content-exposure", "shortvideo_content_exposure")
    )

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

    // 生成实时推荐
    generateRealtimeRecommendations(userBehaviorStream)

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
   * 生成实时推荐
   */
  private def generateRealtimeRecommendations(userBehaviorStream: org.apache.spark.streaming.dstream.DStream[RealtimeBehavior]) = {
    // 按用户分组，计算用户最近的兴趣
    val userRecentBehaviors = userBehaviorStream
      .map(behavior => (behavior.userId, behavior))
      .groupByKeyAndWindow(windowDuration = Seconds(120), slideDuration = Seconds(30)) // 2分钟窗口，每30秒滑动

    // 为每个用户生成基于协同过滤的实时推荐
    userRecentBehaviors.foreachRDD(rdd => {
      if (!rdd.isEmpty()) {
        rdd.collect().foreach { case (userId, behaviors) =>
          val recommendations = generateRecommendationsForUser(userId, behaviors.toArray)
          storeRecommendationsToRedis(userId, recommendations)
        }
      }
    })

    // 基于内容的推荐：根据用户最近观看的视频类型推荐相似视频
    val contentBasedRecommendations = userBehaviorStream
      .filter(_.behaviorType == "view") // 只考虑观看行为
      .map(behavior => (behavior.userId, behavior.videoId))
      .groupByKeyAndWindow(windowDuration = Seconds(120), slideDuration = Seconds(30))

    contentBasedRecommendations.foreachRDD(rdd => {
      if (!rdd.isEmpty()) {
        rdd.collect().foreach { case (userId, videoIds) =>
          val contentBasedRecs = generateContentBasedRecommendations(userId, videoIds.toArray)
          storeContentBasedRecommendationsToRedis(userId, contentBasedRecs)
        }
      }
    })
  }

  /**
   * 为用户生成推荐
   */
  private def generateRecommendationsForUser(userId: Long, recentBehaviors: Array[RealtimeBehavior]): List[Recommendation] = {
    // 这里实现基于协同过滤的实时推荐算法
    // 简化版：根据用户最近的行为推荐相似视频
    
    val videoBehaviorMap = recentBehaviors.groupBy(_.videoId).mapValues(_.length)
    
    // 从Redis获取相似视频（假设预先计算好了）
    val recommendations = recentBehaviors.flatMap { behavior =>
      val similarVideos = RedisUtil.zrevrange(s"similar_videos:${behavior.videoId}", 0, 9)
      similarVideos.map { videoIdStr =>
        val videoId = videoIdStr.toLong
        // 根据行为类型给予不同的推荐分数
        val baseScore = behavior.behaviorType match {
          case "view" => 0.5
          case "like" => 0.8
          case "comment" => 1.0
          case "share" => 1.2
          case "collect" => 1.0
          case "follow" => 1.5
          case _ => 0.3
        }
        Recommendation(userId, videoId, baseScore * 0.8, "realtime_collaborative", System.currentTimeMillis())
      }
    }.toList.distinct.take(20)

    recommendations
  }

  /**
   * 生成基于内容的推荐
   */
  private def generateContentBasedRecommendations(userId: Long, recentVideoIds: Array[Long]): List[Recommendation] = {
    // 基于用户最近观看的视频，推荐相似类型的视频
    val recommendations = recentVideoIds.flatMap { videoId =>
      // 获取与最近观看视频相似的视频
      val similarVideos = RedisUtil.zrevrange(s"similar_videos:$videoId", 0, 14)
      similarVideos.map { similarVideoIdStr =>
        val similarVideoId = similarVideoIdStr.toLong
        Recommendation(userId, similarVideoId, 0.7, "realtime_content_based", System.currentTimeMillis())
      }
    }.toList.distinct.take(15)

    recommendations
  }

  /**
   * 将推荐结果存储到Redis
   */
  private def storeRecommendationsToRedis(userId: Long, recommendations: List[Recommendation]): Unit = {
    if (recommendations.nonEmpty) {
      val key = s"realtime_rec:$userId"
      
      // 使用ZSet存储推荐结果，视频ID为member，推荐分数为score
      val videoScores = recommendations.map(rec => rec.videoId.toString -> rec.score).toMap
      RedisUtil.deleteKey(key) // 清除旧推荐
      RedisUtil.zaddBatch(key, videoScores)
      RedisUtil.expire(key, 600) // 10分钟过期
      
      // 同时保存推荐详情
      val detailKey = s"realtime_rec_detail:$userId"
      val detailData = recommendations.map { rec =>
        s"${rec.videoId}:score" -> rec.score.toString ::
        s"${rec.videoId}:source" -> rec.source ::
        s"${rec.videoId}:timestamp" -> rec.timestamp.toString :: Nil
      }.flatten.toMap
      RedisUtil.hmset(detailKey, detailData)
      RedisUtil.expire(detailKey, 600)
    }
  }

  /**
   * 将基于内容的推荐结果存储到Redis
   */
  private def storeContentBasedRecommendationsToRedis(userId: Long, recommendations: List[Recommendation]): Unit = {
    if (recommendations.nonEmpty) {
      val key = s"realtime_content_rec:$userId"
      
      // 使用ZSet存储基于内容的推荐结果
      val videoScores = recommendations.map(rec => rec.videoId.toString -> rec.score).toMap
      RedisUtil.zaddBatch(key, videoScores)
      RedisUtil.expire(key, 600) // 10分钟过期
    }
  }
}