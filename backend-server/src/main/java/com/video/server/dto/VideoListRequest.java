package com.video.server.dto;

import lombok.Data;

/**
 * 视频列表请求DTO
 */
@Data
public class VideoListRequest {
    
    /**
     * 关键词（标题/ID）
     */
    private String keyword;
    
    /**
     * 分类
     */
    private String category;
    
    /**
     * 状态：all-全部，pending-待审核，published-已发布，removed-已下架
     */
    private String status;
    
    /**
     * 页码
     */
    private Integer page = 1;
    
    /**
     * 每页数量
     */
    private Integer pageSize = 10;
}
