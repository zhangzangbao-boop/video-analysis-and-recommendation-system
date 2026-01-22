package com.video.server.service.impl;

import com.video.server.entity.User;
import com.video.server.entity.UserMessage;
import com.video.server.entity.Video;
import com.video.server.mapper.UserMapper;
import com.video.server.mapper.UserMessageMapper;
import com.video.server.mapper.VideoMapper;
import com.video.server.service.UserMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户消息服务实现类
 */
@Service
@RequiredArgsConstructor
public class UserMessageServiceImpl implements UserMessageService {
    
    private final UserMessageMapper messageMapper;
    private final UserMapper userMapper;
    private final VideoMapper videoMapper;
    
    @Override
    @Transactional
    public boolean createMessage(UserMessage message) {
        message.setCreateTime(LocalDateTime.now());
        message.setIsRead(0);
        return messageMapper.insert(message) > 0;
    }
    
    @Override
    public List<UserMessage> getMessages(Long userId, String type, Integer isRead, Integer limit) {
        return messageMapper.selectByUserId(userId, type, isRead, limit);
    }
    
    @Override
    public Long getUnreadCount(Long userId) {
        Long count = messageMapper.countUnreadByUserId(userId);
        return count != null ? count : 0L;
    }
    
    @Override
    @Transactional
    public boolean markAsRead(Long messageId) {
        return messageMapper.markAsRead(messageId) > 0;
    }
    
    @Override
    @Transactional
    public boolean markAllAsRead(Long userId) {
        return messageMapper.markAllAsRead(userId) > 0;
    }
    
    @Override
    @Transactional
    public boolean createLikeMessage(Long videoOwnerId, Long likerId, Long videoId) {
        // 不能给自己点赞的消息
        if (videoOwnerId.equals(likerId)) {
            return false;
        }
        
        // 检查是否已存在相同的消息（去重）
        UserMessage existing = messageMapper.selectByTypeAndRelated(
                videoOwnerId, "LIKE", likerId, videoId, null);
        if (existing != null) {
            return false; // 已存在，不重复创建
        }
        
        User liker = userMapper.selectById(likerId);
        Video video = videoMapper.selectById(videoId);
        
        UserMessage message = new UserMessage();
        message.setUserId(videoOwnerId);
        message.setType("LIKE");
        message.setTitle("收到新的点赞");
        message.setContent(String.format("%s 赞了你的视频《%s》", 
                liker != null ? liker.getNickname() : "用户", 
                video != null ? video.getTitle() : ""));
        message.setRelatedUserId(likerId);
        message.setRelatedVideoId(videoId);
        
        return createMessage(message);
    }
    
    @Override
    @Transactional
    public boolean createCommentMessage(Long videoOwnerId, Long commenterId, Long videoId, Long commentId, String content) {
        // 不能给自己评论的消息
        if (videoOwnerId.equals(commenterId)) {
            return false;
        }
        
        // 检查是否已存在相同的消息（去重）
        UserMessage existing = messageMapper.selectByTypeAndRelated(
                videoOwnerId, "COMMENT", commenterId, videoId, commentId);
        if (existing != null) {
            return false; // 已存在，不重复创建
        }
        
        User commenter = userMapper.selectById(commenterId);
        Video video = videoMapper.selectById(videoId);
        
        UserMessage message = new UserMessage();
        message.setUserId(videoOwnerId);
        message.setType("COMMENT");
        message.setTitle("收到新的评论");
        message.setContent(String.format("%s 评论了你的视频《%s》：%s", 
                commenter != null ? commenter.getNickname() : "用户", 
                video != null ? video.getTitle() : "",
                content.length() > 50 ? content.substring(0, 50) + "..." : content));
        message.setRelatedUserId(commenterId);
        message.setRelatedVideoId(videoId);
        message.setRelatedCommentId(commentId);
        
        return createMessage(message);
    }
    
    @Override
    @Transactional
    public boolean createCollectMessage(Long videoOwnerId, Long collectorId, Long videoId) {
        // 不能给自己收藏的消息
        if (videoOwnerId.equals(collectorId)) {
            return false;
        }
        
        // 检查是否已存在相同的消息（去重）
        UserMessage existing = messageMapper.selectByTypeAndRelated(
                videoOwnerId, "COLLECT", collectorId, videoId, null);
        if (existing != null) {
            return false; // 已存在，不重复创建
        }
        
        User collector = userMapper.selectById(collectorId);
        Video video = videoMapper.selectById(videoId);
        
        UserMessage message = new UserMessage();
        message.setUserId(videoOwnerId);
        message.setType("COLLECT");
        message.setTitle("收到新的收藏");
        message.setContent(String.format("%s 收藏了你的视频《%s》", 
                collector != null ? collector.getNickname() : "用户", 
                video != null ? video.getTitle() : ""));
        message.setRelatedUserId(collectorId);
        message.setRelatedVideoId(videoId);
        
        return createMessage(message);
    }
    
    @Override
    @Transactional
    public boolean createFollowMessage(Long followedUserId, Long followerId) {
        // 不能给自己关注的消息
        if (followedUserId.equals(followerId)) {
            return false;
        }
        
        // 检查是否已存在相同的消息（去重）
        UserMessage existing = messageMapper.selectByTypeAndRelated(
                followedUserId, "FOLLOW", followerId, null, null);
        if (existing != null) {
            return false; // 已存在，不重复创建
        }
        
        User follower = userMapper.selectById(followerId);
        
        UserMessage message = new UserMessage();
        message.setUserId(followedUserId);
        message.setType("FOLLOW");
        message.setTitle("收到新的关注");
        message.setContent(String.format("%s 关注了你", 
                follower != null ? follower.getNickname() : "用户"));
        message.setRelatedUserId(followerId);
        
        return createMessage(message);
    }
    
    @Override
    @Transactional
    public boolean createSystemMessage(Long userId, Long noticeId, String title, String content) {
        UserMessage message = new UserMessage();
        message.setUserId(userId);
        message.setType("SYSTEM");
        message.setTitle(title != null ? title : "系统通知");
        message.setContent(content);
        message.setRelatedNoticeId(noticeId);
        
        return createMessage(message);
    }
}
