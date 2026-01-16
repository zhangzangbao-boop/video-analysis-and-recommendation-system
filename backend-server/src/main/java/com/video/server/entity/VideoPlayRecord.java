package com.video.server.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 视频播放历史记录实体类
 */
@Data
public class VideoPlayRecord {
    
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
     * 观看时长（秒）
     */
    private Integer duration;
    
    /**
     * 播放进度（%）
     */
    private Integer progress;
    
    /**
     * 是否完播：0-否，1-是
     */
    private Integer isFinish;
    
    /**
     * 观看时间
     */
    private LocalDateTime createTime;
}
