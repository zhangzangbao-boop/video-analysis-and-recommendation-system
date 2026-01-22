package com.video.server.service.impl;

import com.video.server.dto.VideoDTO; // 引入
import com.video.server.entity.VideoInteraction;
import com.video.server.mapper.VideoInteractionMapper;
import com.video.server.mapper.VideoMapper;
import com.video.server.service.VideoInteractionService;
import com.video.server.service.UserMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.video.server.entity.Video;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VideoInteractionServiceImpl implements VideoInteractionService {

    private final VideoInteractionMapper interactionMapper;
    private final VideoMapper videoMapper;
    private final UserMessageService messageService;

    // 互动类型常量
    private static final int TYPE_LIKE = 1;
    private static final int TYPE_COLLECT = 2;
    private static final int TYPE_SHARE = 3;

    @Override
    @Transactional
    public boolean likeVideo(Long userId, Long videoId) {
        VideoInteraction existing = interactionMapper.selectByUserIdAndVideoIdAndType(userId, videoId, TYPE_LIKE);
        if (existing != null) return false;

        VideoInteraction interaction = new VideoInteraction();
        interaction.setUserId(userId);
        interaction.setVideoId(videoId);
        interaction.setType(TYPE_LIKE);
        interaction.setCreateTime(LocalDateTime.now());
        interactionMapper.insert(interaction);

        videoMapper.incrementLikeCount(videoId);
        
        // 创建点赞消息通知
        Video video = videoMapper.selectById(videoId);
        if (video != null && video.getAuthorId() != null) {
            messageService.createLikeMessage(video.getAuthorId(), userId, videoId);
        }
        
        return true;
    }

    @Override
    @Transactional
    public boolean unlikeVideo(Long userId, Long videoId) {
        int deleted = interactionMapper.delete(userId, videoId, TYPE_LIKE);
        if (deleted > 0) {
            Video video = videoMapper.selectById(videoId);
            if (video != null && video.getLikeCount() != null && video.getLikeCount() > 0) {
                video.setLikeCount(video.getLikeCount() - 1);
                videoMapper.updateById(video);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean isLiked(Long userId, Long videoId) {
        VideoInteraction interaction = interactionMapper.selectByUserIdAndVideoIdAndType(userId, videoId, TYPE_LIKE);
        return interaction != null;
    }

    @Override
    public List<VideoDTO> getLikedVideos(Long userId, Integer limit) {
        return interactionMapper.selectLikedVideos(userId, limit);
    }

    @Override
    @Transactional
    public boolean collectVideo(Long userId, Long videoId) {
        VideoInteraction existing = interactionMapper.selectByUserIdAndVideoIdAndType(userId, videoId, TYPE_COLLECT);
        if (existing != null) return false;

        VideoInteraction interaction = new VideoInteraction();
        interaction.setUserId(userId);
        interaction.setVideoId(videoId);
        interaction.setType(TYPE_COLLECT);
        interaction.setCreateTime(LocalDateTime.now());
        interactionMapper.insert(interaction);
        
        // 创建收藏消息通知
        Video video = videoMapper.selectById(videoId);
        if (video != null && video.getAuthorId() != null) {
            messageService.createCollectMessage(video.getAuthorId(), userId, videoId);
        }
        
        return true;
    }

    @Override
    @Transactional
    public boolean uncollectVideo(Long userId, Long videoId) {
        int deleted = interactionMapper.delete(userId, videoId, TYPE_COLLECT);
        return deleted > 0;
    }

    @Override
    public boolean isCollected(Long userId, Long videoId) {
        VideoInteraction interaction = interactionMapper.selectByUserIdAndVideoIdAndType(userId, videoId, TYPE_COLLECT);
        return interaction != null;
    }

    /**
     * 【新增】实现获取收藏视频
     */
    @Override
    public List<VideoDTO> getCollectedVideos(Long userId, Integer limit) {
        return interactionMapper.selectCollectedVideos(userId, limit);
    }
}