package com.shortvideo.recommendation.common.entity

/**
 * 实时热门视频实体
 * @param videoId 视频ID
 * @param hotScore 热度分数
 * @param behaviorCount 行为总数
 * @param likeCount 点赞数
 * @param commentCount 评论数
 * @param shareCount 分享数
 * @param updateTime 更新时间
 */
case class HotVideo(
                     videoId: Long,
                     hotScore: Double,
                     behaviorCount: Long,
                     likeCount: Long,
                     commentCount: Long,
                     shareCount: Long,
                     updateTime: Long = System.currentTimeMillis()
                   )