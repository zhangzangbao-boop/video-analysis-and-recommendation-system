package com.shortvideo.recommendation.common.entity

/**
 * 视频基本信息
 * @param videoId 视频ID
 * @param title 标题
 * @param authorId 作者ID
 * @param category 分类
 * @param tags 标签列表
 * @param duration 视频时长（秒）
 * @param createTime 创建时间
 * @param status 状态（0-下架，1-上架）
 */
case class VideoInfo(
                      videoId: Long,
                      title: String,
                      authorId: Long,
                      category: String,
                      tags: Array[String] = Array.empty,
                      duration: Int,
                      createTime: java.sql.Timestamp,
                      status: Int = 1
                    )

/**
 * 视频热度信息
 * @param videoId 视频ID
 * @param viewCount 观看次数
 * @param likeCount 点赞次数
 * @param commentCount 评论次数
 * @param shareCount 分享次数
 * @param collectCount 收藏次数
 * @param hotScore 热度分数
 * @param updateTime 更新时间
 */
case class VideoHotness(
                         videoId: Long,
                         viewCount: Long = 0,
                         likeCount: Long = 0,
                         commentCount: Long = 0,
                         shareCount: Long = 0,
                         collectCount: Long = 0,
                         hotScore: Double = 0.0,
                         updateTime: java.sql.Timestamp
                       )