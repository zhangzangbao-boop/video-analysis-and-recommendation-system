package com.video.server.service;

import com.video.server.entity.Video;

import java.util.List;

/**
 * 视频互动服务接口
 */
public interface VideoInteractionService {
    
    /**
     * 点赞视频
     * @param userId 用户ID
     * @param videoId 视频ID
     * @return 是否点赞成功
     */
    boolean likeVideo(Long userId, Long videoId);
    
    /**
     * 取消点赞
     * @param userId 用户ID
     * @param videoId 视频ID
     * @return 是否取消成功
     */
    boolean unlikeVideo(Long userId, Long videoId);
    
    /**
     * 检查是否已点赞
     * @param userId 用户ID
     * @param videoId 视频ID
     * @return 是否已点赞
     */
    boolean isLiked(Long userId, Long videoId);
    
    /**
     * 获取用户的点赞列表
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 视频列表
     */
    List<Video> getLikedVideos(Long userId, Integer limit);
    
    /**
     * 收藏视频
     * @param userId 用户ID
     * @param videoId 视频ID
     * @return 是否收藏成功
     */
    boolean collectVideo(Long userId, Long videoId);
    
    /**
     * 取消收藏
     * @param userId 用户ID
     * @param videoId 视频ID
     * @return 是否取消成功
     */
    boolean uncollectVideo(Long userId, Long videoId);
}
