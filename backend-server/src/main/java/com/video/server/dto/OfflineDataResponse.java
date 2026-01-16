package com.video.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 离线分析数据响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfflineDataResponse {
    
    /**
     * 表类型
     */
    private String tableType;
    
    /**
     * 总记录数
     */
    private Long total;
    
    /**
     * 当前页码
     */
    private Integer page;
    
    /**
     * 每页数量
     */
    private Integer pageSize;
    
    /**
     * 是否还有更多数据
     */
    private Boolean hasMore;
    
    /**
     * 最后一条记录的ID（用于下次增量提取）
     */
    private Long lastId;
    
    /**
     * 数据记录列表
     */
    private List<Object> records;
}
