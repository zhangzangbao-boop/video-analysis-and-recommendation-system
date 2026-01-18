package com.shortvideo.recommendation.realtime.entity

import java.sql.Timestamp

/**
 * 实时用户行为实体
 * @param userId 用户ID
 * @param videoId 视频ID
 * @param behaviorType 行为类型
 * @param timestamp 时间戳
 * @param properties 额外属性
 */
case class RealtimeBehavior(
                             userId: Long,
                             videoId: Long,
                             behaviorType: String,
                             timestamp: Long,
                             properties: Map[String, String] = Map.empty
                           )

object RealtimeBehavior {
  def apply(userId: Long, videoId: Long, behaviorType: String, timestamp: Long): RealtimeBehavior = {
    new RealtimeBehavior(userId, videoId, behaviorType, timestamp, Map.empty)
  }
}