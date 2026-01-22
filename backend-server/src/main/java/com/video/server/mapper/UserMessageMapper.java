package com.video.server.mapper;

import com.video.server.entity.UserMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户消息 Mapper 接口
 */
@Mapper
public interface UserMessageMapper extends BaseMapper<UserMessage> {
    
    /**
     * 查询用户的消息列表
     * @param userId 用户ID
     * @param type 消息类型（可选）
     * @param isRead 是否已读（可选）
     * @param limit 限制数量
     * @return 消息列表
     */
    List<UserMessage> selectByUserId(
            @Param("userId") Long userId,
            @Param("type") String type,
            @Param("isRead") Integer isRead,
            @Param("limit") Integer limit
    );
    
    /**
     * 统计用户未读消息数
     * @param userId 用户ID
     * @return 未读消息数
     */
    Long countUnreadByUserId(@Param("userId") Long userId);
    
    /**
     * 标记消息为已读
     * @param messageId 消息ID
     * @return 更新行数
     */
    int markAsRead(@Param("messageId") Long messageId);
    
    /**
     * 一键标记所有消息为已读
     * @param userId 用户ID
     * @return 更新行数
     */
    int markAllAsRead(@Param("userId") Long userId);
    
    /**
     * 根据类型和相关信息查询消息（用于去重）
     * @param userId 用户ID
     * @param type 消息类型
     * @param relatedUserId 相关用户ID
     * @param relatedVideoId 相关视频ID
     * @param relatedCommentId 相关评论ID
     * @return 消息
     */
    UserMessage selectByTypeAndRelated(
            @Param("userId") Long userId,
            @Param("type") String type,
            @Param("relatedUserId") Long relatedUserId,
            @Param("relatedVideoId") Long relatedVideoId,
            @Param("relatedCommentId") Long relatedCommentId
    );
}
