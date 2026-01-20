package com.video.server.dto;

import com.video.server.entity.Video;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 视频数据传输对象 (包含作者昵称等非数据库字段)
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class VideoDTO extends Video {

    /**
     * 作者昵称 (从用户表联表查询)
     */
    private String authorName;
}