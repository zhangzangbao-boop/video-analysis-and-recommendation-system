package com.video.server.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 视频评论实体类
 */
@Data
public class VideoComment {
    
    /**
     * 评论ID（雪花算法生成）
     */
    private Long id;
    
    /**
     * 视频ID
     */
    private Long videoId;
    
    /**
     * 评论者ID
     */
    private Long userId;
    
    /**
     * 评论内容
     */
    private String content;
    
    /**
     * 父评论ID（0表示顶级评论）
     */
    private Long parentId;
    
    /**
     * 被回复者ID
     */
    private Long replyUserId;
    
    /**
     * 点赞数
     */
    private Integer likeCount;
    
    /**
     * 状态：1-正常，0-待审
     */
    private Integer status;
    
    /**
     * 是否删除：0-否，1-是
     */
    private Integer isDeleted;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
