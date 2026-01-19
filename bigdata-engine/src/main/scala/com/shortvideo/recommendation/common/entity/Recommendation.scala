package com.shortvideo.recommendation.common.entity

import org.apache.spark.mllib.recommendation.Rating

/**
 * 推荐结果
 * @param userId 用户ID
 * @param videoId 视频ID
 * @param score 推荐分数
 * @param source 推荐来源
 * @param timestamp 时间戳
 */
case class Recommendation(
                           userId: Long,
                           videoId: Long,
                           score: Double,
                           source: String = "als",  // als, hot, similar, realtime
                           timestamp: Long = System.currentTimeMillis()
                         )

/**
 * Spark MLlib的Rating封装
 */
case class MLRating(
                     userId: Long,
                     videoId: Long,
                     rating: Double
                   ) {
  def toSparkRating: Rating = Rating(userId.toInt, videoId.toInt, rating)
}

object MLRating {
  def fromSparkRating(rating:Rating): MLRating =
    MLRating(rating.user.toLong, rating.product.toLong, rating.rating)
}

/**
 * 用户推荐列表
 * @param userId 用户ID
 * @param recommendations 推荐列表
 * @param generateTime 生成时间
 */
case class UserRecommendation(
                               userId: Long,
                               recommendations: Array[Recommendation],
                               generateTime: Long = System.currentTimeMillis()
                             )

/**
 * 视频相似度
 * @param videoId 视频ID
 * @param similarVideos 相似视频列表
 * @param updateTime 更新时间
 */
case class VideoSimilarity(
                            videoId: Long,
                            similarVideos: Array[(Long, Double)],  // (videoId, similarity)
                            updateTime: Long = System.currentTimeMillis()
                          )