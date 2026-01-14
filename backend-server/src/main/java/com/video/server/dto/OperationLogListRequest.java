package com.video.server.dto;

import lombok.Data;

/**
 * 操作日志列表请求DTO
 */
@Data
public class OperationLogListRequest {
    
    /**
     * 关键词（操作人或内容）
     */
    private String keyword;
    
    /**
     * 开始日期
     */
    private String startDate;
    
    /**
     * 结束日期
     */
    private String endDate;
    
    /**
     * 页码
     */
    private Integer page = 1;
    
    /**
     * 每页数量
     */
    private Integer pageSize = 10;
}
