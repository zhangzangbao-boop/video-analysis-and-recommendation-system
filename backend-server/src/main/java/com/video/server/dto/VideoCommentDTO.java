package com.video.server.dto;

import com.video.server.entity.VideoComment;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
}