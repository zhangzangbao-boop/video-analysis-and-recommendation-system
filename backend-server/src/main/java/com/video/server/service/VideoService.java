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

    List<Video> getHotVideoList();

    PageResponse<Video> getVideoList(VideoListRequest request);

    PageResponse<Video> getMyVideos(Long userId, int page, int limit);

    Video getVideoById(Long videoId);

    List<Video> getRecommendVideoList(Long userId, Integer limit);

    void incrementLikeCount(Long videoId);

    void updateStatus(Long videoId, String status);

    void auditVideoResult(Long videoId, String status, String reason);

    void deleteVideo(Long videoId);

    void setHot(Long videoId, Boolean isHot);

    void uploadAndPublish(MultipartFile file, MultipartFile coverFile, String title, String description, Integer categoryId, String tags, Long userId);

    List<VideoCategory> getCategories();
}