package com.video.server.entity;

import com.video.server.constant.VideoStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 视频实体类
 */
@Data
public class Video {
    
    /**
     * 主键ID（自增）
     */
    private Long id;
    
    /**
     * 视频标题
     */
    private String title;
    
    /**
     * 视频简介
     */
    private String description;
    
    /**
     * 作者ID
     */
    private Long authorId;
    
    /**
     * 封面地址
     */
    private String coverUrl;
    
    /**
     * 视频地址
     */
    private String videoUrl;
    
    /**
     * 分类ID
     */
    private Integer categoryId;
    
    /**
     * 视频时长（秒）
     */
    private Integer duration;
    
    /**
     * 播放量
     */
    private Long playCount;
    
    /**
     * 点赞数
     */
    private Long likeCount;
    
    /**
     * 评论数
     */
    private Long commentCount;
    
    /**
     * 转发数
     */
    private Long shareCount;
    
    /**
     * 审核状态
     */
    private VideoStatus status;
    
    /**
     * 审核意见
     */
    private String auditMsg;
    
    /**
     * 是否热门：0-否，1-是
     */
    private Integer isHot;
    
    /**
     * 标签
     */
    private String tags;
    
    /**
     * 逻辑删除：0-否，1-是
     */
    private Integer isDeleted;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
