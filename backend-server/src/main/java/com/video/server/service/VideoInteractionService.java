package com.video.server.service;

import com.video.server.dto.VideoDTO; // 引入 DTO
import java.util.List;

/**
 * 视频互动服务接口
 */
public interface VideoInteractionService {

    boolean likeVideo(Long userId, Long videoId);

    boolean unlikeVideo(Long userId, Long videoId);

    boolean isLiked(Long userId, Long videoId);

    List<VideoDTO> getLikedVideos(Long userId, Integer limit);

    boolean collectVideo(Long userId, Long videoId);

    boolean uncollectVideo(Long userId, Long videoId);

    boolean isCollected(Long userId, Long videoId);

    /**
     * 【新增】获取收藏视频列表
     */
    List<VideoDTO> getCollectedVideos(Long userId, Integer limit);
}