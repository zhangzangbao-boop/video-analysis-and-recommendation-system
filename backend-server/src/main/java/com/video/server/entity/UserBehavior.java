package com.video.server.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户行为实体类
 */
@Data
public class UserBehavior {
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 视频ID
     */
    private Long videoId;
    
    /**
     * 行为类型（like, view, collect, share等）
     */
    private String actionType;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
