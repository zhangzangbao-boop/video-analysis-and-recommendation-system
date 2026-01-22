package com.video.server.service;

import com.video.server.entity.UserMessage;

import java.util.List;

/**
 * 用户消息服务接口
 */
public interface UserMessageService {
    
    /**
     * 创建消息
     * @param message 消息对象
     * @return 是否成功
     */
    boolean createMessage(UserMessage message);
    
    /**
     * 获取用户的消息列表
     * @param userId 用户ID
     * @param type 消息类型（可选）
     * @param isRead 是否已读（可选）
     * @param limit 限制数量
     * @return 消息列表
     */
    List<UserMessage> getMessages(Long userId, String type, Integer isRead, Integer limit);
    
    /**
     * 获取未读消息数
     * @param userId 用户ID
     * @return 未读消息数
     */
    Long getUnreadCount(Long userId);
    
    /**
     * 标记消息为已读
     * @param messageId 消息ID
     * @return 是否成功
     */
    boolean markAsRead(Long messageId);
    
    /**
     * 一键标记所有消息为已读
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean markAllAsRead(Long userId);
    
    /**
     * 创建点赞消息
     * @param videoOwnerId 视频作者ID
     * @param likerId 点赞者ID
     * @param videoId 视频ID
     * @return 是否成功
     */
    boolean createLikeMessage(Long videoOwnerId, Long likerId, Long videoId);
    
    /**
     * 创建评论消息
     * @param videoOwnerId 视频作者ID
     * @param commenterId 评论者ID
     * @param videoId 视频ID
     * @param commentId 评论ID
     * @param content 评论内容
     * @return 是否成功
     */
    boolean createCommentMessage(Long videoOwnerId, Long commenterId, Long videoId, Long commentId, String content);
    
    /**
     * 创建收藏消息
     * @param videoOwnerId 视频作者ID
     * @param collectorId 收藏者ID
     * @param videoId 视频ID
     * @return 是否成功
     */
    boolean createCollectMessage(Long videoOwnerId, Long collectorId, Long videoId);
    
    /**
     * 创建关注消息
     * @param followedUserId 被关注者ID
     * @param followerId 关注者ID
     * @return 是否成功
     */
    boolean createFollowMessage(Long followedUserId, Long followerId);
    
    /**
     * 创建系统通知消息
     * @param userId 用户ID（null表示全部用户）
     * @param noticeId 系统通知ID
     * @param title 标题
     * @param content 内容
     * @return 是否成功
     */
    boolean createSystemMessage(Long userId, Long noticeId, String title, String content);
}
