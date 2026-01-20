package com.video.server.dto;

import lombok.Data;

/**
 * 视频上传请求对象
 */
@Data
public class VideoUploadRequest {

    /**
     * 视频标题
     */
    private String title;

    /**
     * 视频简介
     */
    private String description;

    /**
     * 视频地址 (MinIO/COS URL)
     */
    private String videoUrl;

    /**
     * 封面地址
     */
    private String coverUrl;

    /**
     * 分类ID
     */
    private Integer categoryId;

    /**
     * 标签 (逗号分隔)
     */
    private String tags;

    /**
     * 视频时长 (秒)
     */
    private Integer duration;
}