package com.video.server.dto;

import lombok.Data;

import java.util.List;

/**
 * 批量视频操作请求DTO
 */
@Data
public class BatchVideoRequest {
    
    /**
     * 视频ID列表
     */
    private List<Long> videoIds;
    
    /**
     * 操作类型：pass-批量通过，delete-批量下架
     */
    private String action;
    
    /**
     * 审核原因（可选）
     */
    private String reason;
}
