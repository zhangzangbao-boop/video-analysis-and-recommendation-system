package com.video.server.service;

import com.video.server.entity.Video;

import java.util.List;

/**
 * 视频互动服务接口
 */
public interface VideoInteractionService {

    /**
     * 点赞视频
     */
    boolean likeVideo(Long userId, Long videoId);

    /**
     * 取消点赞
     */
    boolean unlikeVideo(Long userId, Long videoId);

    /**
     * 检查是否已点赞
     */
    boolean isLiked(Long userId, Long videoId);

    /**
     * 获取用户的点赞列表
     */
    List<Video> getLikedVideos(Long userId, Integer limit);

    /**
     * 收藏视频
     */
    boolean collectVideo(Long userId, Long videoId);

    /**
     * 取消收藏
     */
    boolean uncollectVideo(Long userId, Long videoId);

    /**
     * 【新增】检查是否已收藏
     */
    boolean isCollected(Long userId, Long videoId);
}