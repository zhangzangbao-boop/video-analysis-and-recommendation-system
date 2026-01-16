package com.video.server.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 视频互动记录实体类
 */
@Data
public class VideoInteraction {
    
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
     * 类型：1-点赞，2-收藏，3-转发，4-不感兴趣
     */
    private Integer type;
    
    /**
     * 操作时间
     */
    private LocalDateTime createTime;
}
