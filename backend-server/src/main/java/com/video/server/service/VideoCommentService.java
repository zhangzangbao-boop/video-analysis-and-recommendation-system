package com.video.server.service;

import com.video.server.entity.VideoComment;

import java.util.List;

/**
 * 视频评论服务接口
 */
public interface VideoCommentService {
    
    /**
     * 添加评论
     * @param videoId 视频ID
     * @param userId 用户ID
     * @param content 评论内容
     * @param parentId 父评论ID（可选，用于回复）
     * @param replyUserId 被回复者ID（可选）
     * @return 评论
     */
    VideoComment addComment(Long videoId, Long userId, String content, Long parentId, Long replyUserId);
    
    /**
     * 获取视频的评论列表
     * @param videoId 视频ID
     * @param limit 限制数量
     * @return 评论列表
     */
    List<VideoComment> getCommentsByVideoId(Long videoId, Integer limit);
    
    /**
     * 获取评论的回复列表
     * @param parentId 父评论ID
     * @param limit 限制数量
     * @return 回复列表
     */
    List<VideoComment> getRepliesByParentId(Long parentId, Integer limit);
    
    /**
     * 获取用户的评论列表
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 评论列表
     */
    List<VideoComment> getCommentsByUserId(Long userId, Integer limit);
    
    /**
     * 删除评论
     * @param commentId 评论ID
     * @param userId 用户ID（用于权限验证）
     * @return 是否删除成功
     */
    boolean deleteComment(Long commentId, Long userId);
}
