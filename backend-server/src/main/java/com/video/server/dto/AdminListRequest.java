package com.video.server.dto;

import lombok.Data;

/**
 * 管理员列表请求DTO
 */
@Data
public class AdminListRequest {
    
    /**
     * 关键词（用户名/ID/手机号）
     */
    private String keyword;
    
    /**
     * 状态：1-正常，0-禁用
     */
    private Integer status;
    
    /**
     * 页码
     */
    private Integer page = 1;
    
    /**
     * 每页数量
     */
    private Integer pageSize = 10;
}
