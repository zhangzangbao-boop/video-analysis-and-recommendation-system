package com.video.server.service;

import com.video.server.dto.PageResponse;
import com.video.server.dto.VideoListRequest;
import com.video.server.dto.VideoUploadRequest;
import com.video.server.entity.Video;
import com.video.server.entity.VideoCategory;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface VideoService {

    /**
     * 上传并发布视频
     */
    void uploadAndPublish(MultipartFile file, MultipartFile coverFile, String title, String description, Integer categoryId, String tags, Long userId);

    /**
     * 上传视频（旧接口兼容）
     */
    void uploadVideo(VideoUploadRequest request, Long userId);

    /**
     * 获取热门视频列表
     */
    List<Video> getHotVideoList(); // 改名以匹配实现类，或者直接用 getHotVideos

    List<Video> getHotVideos();

    /**
     * 获取推荐视频列表
     */
    List<Video> getRecommendVideos(Long userId, Integer limit);
    List<Video> getRecommendVideoList(Long userId, Integer limit);

    /**
     * 根据ID获取视频详情
     */
    Video getVideoById(Long id);

    /**
     * 搜索视频
     */
    List<Video> searchVideos(String keyword, Integer categoryId, Integer page, Integer pageSize);
    
    /**
     * 统计搜索结果总数
     */
    Long countSearchVideos(String keyword, Integer categoryId);

    /**
     * 获取视频列表（后台管理用）
     */
    PageResponse<Video> getVideoList(VideoListRequest request);

    /**
     * 获取我的作品（分页）
     */
    PageResponse<Video> getMyVideos(Long userId, int page, int limit);

    /**
     * 【新增】获取用户发布的作品 (List形式，供前端直接调用)
     */
    List<Video> getUserPublishedVideos(Long userId, Integer limit);

    /**
     * 增加点赞数
     */
    void incrementLikeCount(Long videoId);

    /**
     * 更新视频状态
     */
    void updateStatus(Long videoId, String status);

    /**
     * 审核视频
     */
    void auditVideoResult(Long videoId, String status, String reason);

    /**
     * 删除视频
     */
    void deleteVideo(Long videoId);

    /**
     * 设置热门
     */
    void setHot(Long videoId, Boolean isHot);

    /**
     * 获取分类列表
     */
    List<VideoCategory> getCategories();

    /**
     * 【新增】更新视频信息（编辑功能）
     */
    void updateVideo(Long videoId, String title, String description, Integer categoryId, String tags, String coverUrl);
    
    /**
     * 获取所有视频的总播放量（仅统计已发布且未删除的视频）
     */
    Long getTotalPlayCount();
    
    /**
     * 更新视频审核消息
     */
    void updateAuditMessage(Long videoId, String auditMsg);
}