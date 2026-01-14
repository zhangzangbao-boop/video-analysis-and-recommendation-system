package com.video.server.dto;

import lombok.Data;

/**
 * 用户行为请求DTO
 */
@Data
public class UserBehaviorRequest {
    
    /**
     * 行为类型（like, view, collect, share等）
     */
    private String actionType;
    
    /**
     * 视频ID
     */
    private Long videoId;
    
    /**
     * 用户ID
     */
    private Long userId;
}
