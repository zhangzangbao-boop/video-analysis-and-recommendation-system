package com.video.server.service;

import com.video.server.dto.VideoCommentDTO;
import com.video.server.entity.VideoComment;
import java.util.List;

public interface VideoCommentService {

    /**
     * 发表评论/回复
     */
    VideoCommentDTO addComment(Long videoId, Long userId, String content, Long parentId, Long replyUserId);

    /**
     * 获取视频的一级评论 (DTO版)
     */
    List<VideoCommentDTO> getCommentsByVideoId(Long videoId, Integer limit);

    /**
     * 获取评论的子回复 (DTO版)
     */
    List<VideoCommentDTO> getRepliesByParentId(Long parentId, Integer limit);

    /**
     * 【修复】加回此方法，供 UserController 调用 (查看我的评论)
     */
    List<VideoComment> getCommentsByUserId(Long userId, Integer limit);

    /**
     * 删除评论
     */
    boolean deleteComment(Long commentId, Long userId);

    /**
     * 点赞评论
     */
    boolean likeComment(Long commentId, Long userId);
}