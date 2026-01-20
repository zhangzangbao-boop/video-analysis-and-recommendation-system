package com.shortvideo.recommendation.realtime.model

import java.sql.Timestamp

/**
 * video_info 表对应的数据模型（实时模块使用）
 *
 * 表字段（用户提供）：
 * id, user_id, title, description, video_url, cover_url, category_id, tags, duration,
 * status, audit_msg, is_hot, view_count, like_count, comment_count, share_count,
 * create_time, update_time, is_deleted
 */
case class VideoInfoRow(
                         id: Long,
                         userId: Long,
                         title: String,
                         description: String,
                         videoUrl: String,
                         coverUrl: String,
                         categoryId: Long,
                         tags: String,
                         duration: Int,
                         status: Int,
                         auditMsg: String,
                         isHot: Int,
                         viewCount: Long,
                         likeCount: Long,
                         commentCount: Long,
                         shareCount: Long,
                         createTime: Timestamp,
                         updateTime: Timestamp,
                         isDeleted: Int
                       ) {

  /** 是否可推荐（上架且未删除） */
  def isRecommendable: Boolean = status == 1 && isDeleted == 0

  /** 简单热度分（用于没有实时热门时的兜底排序） */
  def offlineHotScore: Double = {
    // 可按你们业务调整权重
    viewCount * 1.0 + likeCount * 2.0 + commentCount * 3.0 + shareCount * 4.0
  }
}

