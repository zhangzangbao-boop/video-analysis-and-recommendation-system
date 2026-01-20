package com.video.server.service;

import com.video.server.dto.VideoCommentDTO;
import com.video.server.entity.VideoComment;
import java.util.List;

public interface VideoCommentService {

    VideoCommentDTO addComment(Long videoId, Long userId, String content, Long parentId, Long replyUserId);

    // 修改：增加 currentUserId 参数用于判断点赞状态
    List<VideoCommentDTO> getCommentsByVideoId(Long videoId, Integer limit, Long currentUserId);

    List<VideoCommentDTO> getRepliesByParentId(Long parentId, Integer limit, Long currentUserId);

    List<VideoComment> getCommentsByUserId(Long userId, Integer limit);

    boolean deleteComment(Long commentId, Long userId);

    boolean likeComment(Long commentId, Long userId);

    // 新增：取消点赞
    boolean unlikeComment(Long commentId, Long userId);
}