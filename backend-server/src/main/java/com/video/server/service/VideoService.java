package com.video.server.service;

import com.video.server.dto.PageResponse;
import com.video.server.dto.VideoListRequest;
import com.video.server.entity.Video;
import com.video.server.entity.VideoCategory;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 视频服务接口
 */
public interface VideoService {
    
    /**
     * 获取热门视频列表
     * @return 热门视频列表
     */
    List<Video> getHotVideoList();
    
    /**
     * 获取视频列表（分页）
     * @param request 查询请求
     * @return 分页结果
     */
    PageResponse<Video> getVideoList(VideoListRequest request);
    
    /**
     * 根据ID获取视频详情
     * @param videoId 视频ID
     * @return 视频信息
     */
    Video getVideoById(Long videoId);
    
    /**
     * 获取推荐视频列表
     * @param userId 用户ID（可选）
     * @param limit 数量限制
     * @return 推荐视频列表
     */
    List<Video> getRecommendVideoList(Long userId, Integer limit);
    
    /**
     * 更新视频点赞数
     * @param videoId 视频ID
     */
    void incrementLikeCount(Long videoId);
    
    /**
     * 根据ID更新视频状态
     * @param videoId 视频ID
     * @param status 状态
     */
    void updateStatus(Long videoId, String status);
    
    /**
     * 删除/下架视频
     * @param videoId 视频ID
     */
    void deleteVideo(Long videoId);
    
    /**
     * 设置/取消热门
     * @param videoId 视频ID
     * @param isHot 是否热门
     */
    void setHot(Long videoId, Boolean isHot);
    
    /**
     * 上传并发布视频
     * @param file 视频文件
     * @param coverFile 封面文件（可选）
     * @param title 标题
     * @param description 简介
     * @param categoryId 分类ID
     * @param tags 标签
     * @param userId 发布者ID
     */
    void uploadAndPublish(MultipartFile file, MultipartFile coverFile, String title, String description, Integer categoryId, String tags, Long userId);
    
    /**
     * 获取视频分类列表
     * @return 分类列表
     */
    List<VideoCategory> getCategories();
}
