package com.shortvideo.recommendation.realtime.service

import com.shortvideo.recommendation.common.entity.Recommendation
import com.shortvideo.recommendation.common.utils.RedisUtil
import com.shortvideo.recommendation.realtime.entity.HotVideo

import scala.collection.JavaConverters._
import com.shortvideo.recommendation.common.utils.RedisUtils

/**
 * 实时推荐服务类
 * 提供给Spring Boot应用访问实时推荐结果的接口
 */
object RealtimeRecommendationService {

  /**
   * 获取用户的实时推荐
   * @param userId 用户ID
   * @param count 推荐数量
   * @return 推荐结果列表
   */
  def getUserRealtimeRecommendations(userId: Long, count: Int = 20): List[Recommendation] = {
    val key = s"realtime_rec:$userId"
    
    val videoScores = RedisUtil.zrevrangeWithScores(key, 0, count - 1)
    
    videoScores.map { case (videoIdStr, score) =>
      Recommendation(
        userId = userId,
        videoId = videoIdStr.toLong,
        score = score,
        source = "realtime",
        timestamp = System.currentTimeMillis()
      )
    }.toList
  }

  /**
   * 获取用户的协同过滤实时推荐
   * @param userId 用户ID
   * @param count 推荐数量
   * @return 推荐结果列表
   */
  def getUserCollaborativeRecommendations(userId: Long, count: Int = 20): List[Recommendation] = {
    val key = s"realtime_rec:$userId"
    
    val videoScores = RedisUtil.zrevrangeWithScores(key, 0, count - 1)
    
    videoScores.map { case (videoIdStr, score) =>
      Recommendation(
        userId = userId,
        videoId = videoIdStr.toLong,
        score = score,
        source = "realtime_collaborative",
        timestamp = System.currentTimeMillis()
      )
    }.toList
  }

  /**
   * 获取用户的基于内容的实时推荐
   * @param userId 用户ID
   * @param count 推荐数量
   * @return 推荐结果列表
   */
  def getUserContentBasedRecommendations(userId: Long, count: Int = 20): List[Recommendation] = {
    val key = s"realtime_content_rec:$userId"
    
    val videoScores = RedisUtil.zrevrangeWithScores(key, 0, count - 1)
    
    videoScores.map { case (videoIdStr, score) =>
      Recommendation(
        userId = userId,
        videoId = videoIdStr.toLong,
        score = score,
        source = "realtime_content_based",
        timestamp = System.currentTimeMillis()
      )
    }.toList
  }

  /**
   * 获取实时热门视频
   * @param count 视频数量
   * @return 热门视频列表
   */
  def getRealtimeHotVideos(count: Int = 50): List[HotVideo] = {
    val key = "hot_videos:realtime"
    
    val videoScores = RedisUtil.zrevrangeWithScores(key, 0, count - 1)
    
    videoScores.map { case (videoIdStr, hotScore) =>
      val detailKey = s"hot_video_detail:$videoIdStr"
      val detailData = RedisUtil.hgetAll(detailKey)
      
      HotVideo(
        videoId = videoIdStr.toLong,
        hotScore = hotScore,
        behaviorCount = detailData.get("behaviorCount").map(_.toLong).getOrElse(0L),
        likeCount = detailData.get("likeCount").map(_.toLong).getOrElse(0L),
        commentCount = detailData.get("commentCount").map(_.toLong).getOrElse(0L),
        shareCount = detailData.get("shareCount").map(_.toLong).getOrElse(0L),
        updateTime = detailData.get("updateTime").map(_.toLong).getOrElse(System.currentTimeMillis())
      )
    }.toList
  }

  /**
   * 获取用户最近的行为统计
   * @param userId 用户ID
   * @return 行为统计Map
   */
  def getUserBehaviorStats(userId: Long): Map[String, Long] = {
    val key = s"user_behavior:$userId"
    RedisUtil.hgetAll(key).asScala.mapValues(_.toLong).toMap
  }

  /**
   * 获取用户兴趣标签
   * @param userId 用户ID
   * @param count 标签数量
   * @return 兴趣标签列表
   */
  def getUserInterestTags(userId: Long, count: Int = 10): List[(String, Double)] = {
    val key = s"user_interest:$userId"
    RedisUtil.zrevrangeWithScores(key, 0, count - 1)
  }

  /**
   * 检查用户是否有实时推荐
   * @param userId 用户ID
   * @return 是否存在推荐
   */
  def hasRealtimeRecommendations(userId: Long): Boolean = {
    val key = s"realtime_rec:$userId"
    RedisUtil.zcard(key) > 0
  }

  /**
   * 获取实时推荐结果的刷新时间
   * @param userId 用户ID
   * @return 刷新时间戳
   */
  def getRecommendationRefreshTime(userId: Long): Option[Long] = {
    val key = s"realtime_rec_detail:$userId"
    val fields = RedisUtil.hkeys(key).asScala.toList
    if (fields.nonEmpty) {
      val timestampField = fields.find(_.endsWith(":timestamp"))
      timestampField.map(field => RedisUtil.hget(key, field).toLong)
    } else {
      None
    }
  }
}