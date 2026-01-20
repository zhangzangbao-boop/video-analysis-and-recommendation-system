package com.video.server.dto;

import com.video.server.entity.VideoComment;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class VideoCommentDTO extends VideoComment {
    /**
     * 发送者昵称
     */
    private String nickname;

    /**
     * 发送者头像
     */
    private String avatarUrl;

    /**
     * 被回复者的昵称（如果是回复楼层）
     */
    private String replyNickname;

    /**
     * 子评论列表（楼中楼）
     * 修复刷新消失问题：后端直接返回嵌套结构
     */
    private List<VideoCommentDTO> replies;

    /**
     * 当前用户是否已点赞
     * 修复无限点赞问题：前端根据此状态控制交互
     */
    private Boolean isLiked;
}