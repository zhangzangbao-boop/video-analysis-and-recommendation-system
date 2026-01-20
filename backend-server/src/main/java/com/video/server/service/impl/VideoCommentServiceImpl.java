package com.video.server.service.impl;

import com.video.server.dto.VideoCommentDTO;
import com.video.server.entity.Video;
import com.video.server.entity.VideoComment;
import com.video.server.mapper.VideoCommentMapper;
import com.video.server.mapper.VideoMapper;
import com.video.server.service.VideoCommentService;
import com.video.server.utils.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class VideoCommentServiceImpl implements VideoCommentService {

    private final VideoCommentMapper commentMapper;
    private final VideoMapper videoMapper;

    // 内存 Map 存储点赞状态 Key: commentId, Value: Set<userId>
    private static final Map<Long, Set<Long>> COMMENT_LIKE_MAP = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public VideoCommentDTO addComment(Long videoId, Long userId, String content, Long parentId, Long replyUserId) {
        VideoComment comment = new VideoComment();
        long id = IdGenerator.nextId();
        comment.setId(id);
        comment.setVideoId(videoId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setParentId(parentId != null ? parentId : 0L);
        comment.setReplyUserId(replyUserId);
        comment.setLikeCount(0);
        comment.setStatus(1);
        comment.setIsDeleted(0);
        comment.setCreateTime(LocalDateTime.now());

        commentMapper.insert(comment);

        Video video = videoMapper.selectById(videoId);
        if (video != null) {
            Long count = commentMapper.countByVideoId(videoId);
            video.setCommentCount(count);
            videoMapper.updateById(video);
        }

        VideoCommentDTO dto = commentMapper.selectDTOById(id);
        if (dto != null) dto.setIsLiked(false);
        return dto;
    }

    @Override
    public List<VideoCommentDTO> getCommentsByVideoId(Long videoId, Integer limit, Long currentUserId) {
        List<VideoCommentDTO> comments = commentMapper.selectDTOByVideoId(videoId, limit);
        for (VideoCommentDTO comment : comments) {
            fillLikeStatus(comment, currentUserId);
            List<VideoCommentDTO> replies = commentMapper.selectDTOByParentId(comment.getId(), 5);
            if (replies != null && !replies.isEmpty()) {
                for (VideoCommentDTO reply : replies) {
                    fillLikeStatus(reply, currentUserId);
                }
                comment.setReplies(replies);
            }
        }
        return comments;
    }

    @Override
    public List<VideoCommentDTO> getRepliesByParentId(Long parentId, Integer limit, Long currentUserId) {
        List<VideoCommentDTO> replies = commentMapper.selectDTOByParentId(parentId, limit);
        for (VideoCommentDTO reply : replies) {
            fillLikeStatus(reply, currentUserId);
        }
        return replies;
    }

    @Override
    public List<VideoComment> getCommentsByUserId(Long userId, Integer limit) {
        return commentMapper.selectByUserId(userId, limit);
    }

    @Override
    @Transactional
    public boolean deleteComment(Long commentId, Long userId) {
        VideoComment comment = commentMapper.selectById(commentId);
        if (comment == null || !comment.getUserId().equals(userId)) {
            return false;
        }
        commentMapper.deleteById(commentId);

        Long count = commentMapper.countByVideoId(comment.getVideoId());
        Video video = new Video();
        video.setId(comment.getVideoId());
        video.setCommentCount(count);
        videoMapper.updateById(video);

        COMMENT_LIKE_MAP.remove(commentId);
        return true;
    }

    @Override
    @Transactional
    public boolean likeComment(Long commentId, Long userId) {
        Set<Long> userSet = COMMENT_LIKE_MAP.computeIfAbsent(commentId, k -> ConcurrentHashMap.newKeySet());

        // 检查是否已点赞
        if (userSet.contains(userId)) {
            return false;
        }

        // 数据库 +1
        int rows = commentMapper.updateLikeCount(commentId, 1);
        if (rows > 0) {
            userSet.add(userId);
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public boolean unlikeComment(Long commentId, Long userId) {
        Set<Long> userSet = COMMENT_LIKE_MAP.get(commentId);

        // 【核心修改】放宽检查逻辑
        // 即使内存 Map 为空（如服务重启后），只要用户发起了取消请求，我们就尝试去数据库 -1
        // 这样可以避免"点赞状态丢失导致无法取消"的 Bug

        if (userSet != null) {
            userSet.remove(userId);
        }

        // 数据库 -1
        commentMapper.updateLikeCount(commentId, -1);

        // 只要执行了 DB 操作，就返回 true，确保前端状态同步
        return true;
    }

    private void fillLikeStatus(VideoCommentDTO dto, Long userId) {
        if (userId == null) {
            dto.setIsLiked(false);
            return;
        }
        Set<Long> userSet = COMMENT_LIKE_MAP.get(dto.getId());
        dto.setIsLiked(userSet != null && userSet.contains(userId));
    }
}