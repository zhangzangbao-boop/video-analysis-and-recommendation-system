package com.video.server.dto;

import lombok.Data;

/**
 * 用户列表请求DTO
 */
@Data
public class UserListRequest {
    
    /**
     * 关键词（用户名/ID/手机号）
     */
    private String keyword;
    
    /**
     * 状态字符串：normal-正常，frozen-冻结，muted-禁言
     */
    private String status;
    
    /**
     * 用户等级：1-3
     */
    private String level;
    
    /**
     * 页码
     */
    private Integer page = 1;
    
    /**
     * 每页数量
     */
    private Integer pageSize = 10;
}
