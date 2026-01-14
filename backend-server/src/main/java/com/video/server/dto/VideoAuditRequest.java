package com.video.server.dto;

import lombok.Data;

/**
 * 视频审核请求DTO
 */
@Data
public class VideoAuditRequest {
    
    /**
     * 视频ID
     */
    private Long videoId;
    
    /**
     * 审核操作（pass/reject）
     */
    private String action;
    
    /**
     * 审核原因
     */
    private String reason;
}
