package com.video.server.service.impl;

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

/**
 * 视频评论服务实现
 */
@Service
@RequiredArgsConstructor
public class VideoCommentServiceImpl implements VideoCommentService {
    
    private final VideoCommentMapper commentMapper;
    private final VideoMapper videoMapper;
    
    @Override
    @Transactional
    public VideoComment addComment(Long videoId, Long userId, String content, Long parentId, Long replyUserId) {
        VideoComment comment = new VideoComment();
        comment.setId(IdGenerator.nextId());
        comment.setVideoId(videoId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setParentId(parentId != null ? parentId : 0L);
        comment.setReplyUserId(replyUserId);
        comment.setLikeCount(0);
        comment.setStatus(1); // 1-正常
        comment.setIsDeleted(0);
        comment.setCreateTime(LocalDateTime.now());
        
        commentMapper.insert(comment);
        
        // 更新视频评论数
        Video video = videoMapper.selectById(videoId);
        if (video != null) {
            Long commentCount = commentMapper.countByVideoId(videoId);
            video.setCommentCount(commentCount);
            videoMapper.updateById(video);
        }
        
        return comment;
    }
    
    @Override
    public List<VideoComment> getCommentsByVideoId(Long videoId, Integer limit) {
        return commentMapper.selectByVideoId(videoId, limit);
    }
    
    @Override
    public List<VideoComment> getRepliesByParentId(Long parentId, Integer limit) {
        return commentMapper.selectByParentId(parentId, limit);
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
            return false; // 评论不存在或无权删除
        }
        
        commentMapper.deleteById(commentId);
        
        // 更新视频评论数
        Video video = videoMapper.selectById(comment.getVideoId());
        if (video != null) {
            Long commentCount = commentMapper.countByVideoId(comment.getVideoId());
            video.setCommentCount(commentCount);
            videoMapper.updateById(video);
        }
        
        return true;
    }
}
