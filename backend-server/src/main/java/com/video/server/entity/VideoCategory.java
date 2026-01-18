package com.video.server.entity;

import lombok.Data;

/**
 * 视频分类实体
 */
@Data
public class VideoCategory {
    /**
     * 分类ID
     */
    private Integer id;
    
    /**
     * 分类名称
     */
    private String name;
    
    /**
     * 分类编码
     */
    private String code;
    
    /**
     * 排序
     */
    private Integer sort;
}
