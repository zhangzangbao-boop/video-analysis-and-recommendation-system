package com.shortvideo.recommendation.common.entity

/**
 * 用户实时兴趣特征
 * 用于计算实时推荐
 *
 * @param userId       用户ID
 * @param interestMap  兴趣特征 Map (movieId -> score)
 * @param lastUpdate   最后更新时间
 */
case class UserInterest(
                         userId: Long,
                         interestMap: Map[Long, Double],
                         lastUpdate: Long
                       )