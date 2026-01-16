package com.video.server.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户关注关系实体类
 */
@Data
public class UserFollow {
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 粉丝ID（谁关注）
     */
    private Long userId;
    
    /**
     * 被关注者ID（关注了谁）
     */
    private Long followUserId;
    
    /**
     * 关注时间
     */
    private LocalDateTime createTime;
}
