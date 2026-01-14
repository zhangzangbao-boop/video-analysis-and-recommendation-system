package com.shortvideo.recommendation.common.entity

import java.sql.Timestamp

/**
 * 用户行为实体类
 * @param userId 用户ID
 * @param videoId 视频ID
 * @param behaviorType 行为类型
 * @param behaviorTime 行为时间
 * @param duration 观看时长（秒）
 * @param deviceInfo 设备信息
 * @param networkType 网络类型
 * @param ipAddress IP地址
 * @param location 地理位置
 * @param extraInfo 扩展信息
 */
case class UserBehavior(
                         userId: Long,
                         videoId: Long,
                         behaviorType: String,
                         behaviorTime: Timestamp,
                         duration: Int = 0,
                         deviceInfo: String = "",
                         networkType: String = "",
                         ipAddress: String = "",
                         location: String = "",
                         extraInfo: String = ""
                       )

/**
 * 用户行为评分实体（用于ALS模型）
 * @param userId 用户ID
 * @param videoId 视频ID
 * @param rating 评分
 * @param timestamp 时间戳
 */
case class UserRating(
                       userId: Long,
                       videoId: Long,
                       rating: Double,
                       timestamp: Long
                     )

/**
 * Kafka中的用户行为消息
 * @param logId 日志ID
 * @param userId 用户ID
 * @param videoId 视频ID
 * @param behavior 行为类型
 * @param timestamp 时间戳
 * @param properties 额外属性
 */
case class UserBehaviorEvent(
                              logId: String,
                              userId: Long,
                              videoId: Long,
                              behavior: String,
                              timestamp: Long,
                              properties: Map[String, String] = Map.empty
                            )