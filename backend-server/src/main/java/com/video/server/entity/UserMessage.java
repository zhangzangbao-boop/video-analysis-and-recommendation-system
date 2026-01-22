package com.video.server.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户消息实体类
 */
@Data
public class UserMessage {
    
    /**
     * 消息ID
     */
    private Long id;
    
    /**
     * 接收消息的用户ID
     */
    private Long userId;
    
    /**
     * 消息类型: LIKE-点赞, COMMENT-评论, COLLECT-收藏, FOLLOW-关注, SYSTEM-系统通知
     */
    private String type;
    
    /**
     * 消息标题
     */
    private String title;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 相关用户ID（发送消息的用户，如点赞者、评论者等）
     */
    private Long relatedUserId;
    
    /**
     * 相关视频ID（如点赞的视频、评论的视频等）
     */
    private Long relatedVideoId;
    
    /**
     * 相关评论ID（如回复的评论）
     */
    private Long relatedCommentId;
    
    /**
     * 相关系统通知ID（如果是系统通知）
     */
    private Long relatedNoticeId;
    
    /**
     * 是否已读: 0-未读, 1-已读
     */
    private Integer isRead;
    
    /**
     * 阅读时间
     */
    private LocalDateTime readTime;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
