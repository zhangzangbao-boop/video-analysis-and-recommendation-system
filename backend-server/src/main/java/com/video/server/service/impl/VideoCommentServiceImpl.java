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

@Service
@RequiredArgsConstructor
public class VideoCommentServiceImpl implements VideoCommentService {

    private final VideoCommentMapper commentMapper;
    private final VideoMapper videoMapper;

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

        return commentMapper.selectDTOById(id);
    }

    @Override
    public List<VideoCommentDTO> getCommentsByVideoId(Long videoId, Integer limit) {
        return commentMapper.selectDTOByVideoId(videoId, limit);
    }

    @Override
    public List<VideoCommentDTO> getRepliesByParentId(Long parentId, Integer limit) {
        return commentMapper.selectDTOByParentId(parentId, limit);
    }

    /**
     * 【修复】实现加回的方法
     */
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

        return true;
    }

    @Override
    @Transactional
    public boolean likeComment(Long commentId, Long userId) {
        int rows = commentMapper.updateLikeCount(commentId, 1);
        return rows > 0;
    }
}