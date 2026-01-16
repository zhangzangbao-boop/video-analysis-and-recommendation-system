package com.video.server.dto;

import lombok.Data;

/**
 * 离线分析数据请求DTO
 */
@Data
public class OfflineDataRequest {
    
    /**
     * 表类型：user_behavior, video_play_record, video_interaction, video_info, sys_user, all
     */
    private String tableType;
    
    /**
     * 页码（从1开始）
     */
    private Integer page = 1;
    
    /**
     * 每页数量（默认1000，最大10000）
     */
    private Integer pageSize = 1000;
    
    /**
     * 开始时间（格式：yyyy-MM-dd HH:mm:ss）
     */
    private String startTime;
    
    /**
     * 结束时间（格式：yyyy-MM-dd HH:mm:ss）
     */
    private String endTime;
    
    /**
     * 上次最后一条ID（用于增量提取）
     */
    private Long lastId;
}
