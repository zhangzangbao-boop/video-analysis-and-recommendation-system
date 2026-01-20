package com.video.server.service.impl;

import com.video.server.entity.Video;
import com.video.server.entity.VideoInteraction;
import com.video.server.mapper.VideoInteractionMapper;
import com.video.server.mapper.VideoMapper;
import com.video.server.service.VideoInteractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 视频互动服务实现
 */
@Service
@RequiredArgsConstructor
public class VideoInteractionServiceImpl implements VideoInteractionService {

    private final VideoInteractionMapper interactionMapper;
    private final VideoMapper videoMapper;

    // 互动类型常量
    private static final int TYPE_LIKE = 1;
    private static final int TYPE_COLLECT = 2;
    private static final int TYPE_SHARE = 3;

    @Override
    @Transactional
    public boolean likeVideo(Long userId, Long videoId) {
        // 检查是否已点赞
        VideoInteraction existing = interactionMapper.selectByUserIdAndVideoIdAndType(userId, videoId, TYPE_LIKE);
        if (existing != null) {
            return false; // 已点赞
        }

        // 创建点赞记录
        VideoInteraction interaction = new VideoInteraction();
        interaction.setUserId(userId);
        interaction.setVideoId(videoId);
        interaction.setType(TYPE_LIKE);
        interaction.setCreateTime(LocalDateTime.now());
        interactionMapper.insert(interaction);

        // 增加视频点赞数
        videoMapper.incrementLikeCount(videoId);

        return true;
    }

    @Override
    @Transactional
    public boolean unlikeVideo(Long userId, Long videoId) {
        int deleted = interactionMapper.delete(userId, videoId, TYPE_LIKE);
        if (deleted > 0) {
            // 减少视频点赞数
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
    public List<Video> getLikedVideos(Long userId, Integer limit) {
        List<VideoInteraction> interactions = interactionMapper.selectLikesByUserId(userId, limit);
        return interactions.stream()
                .map(interaction -> videoMapper.selectById(interaction.getVideoId()))
                .filter(video -> video != null)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public boolean collectVideo(Long userId, Long videoId) {
        // 检查是否已收藏
        VideoInteraction existing = interactionMapper.selectByUserIdAndVideoIdAndType(userId, videoId, TYPE_COLLECT);
        if (existing != null) {
            return false; // 已收藏
        }

        // 创建收藏记录
        VideoInteraction interaction = new VideoInteraction();
        interaction.setUserId(userId);
        interaction.setVideoId(videoId);
        interaction.setType(TYPE_COLLECT);
        interaction.setCreateTime(LocalDateTime.now());
        interactionMapper.insert(interaction);

        return true;
    }

    @Override
    @Transactional
    public boolean uncollectVideo(Long userId, Long videoId) {
        int deleted = interactionMapper.delete(userId, videoId, TYPE_COLLECT);
        return deleted > 0;
    }

    /**
     * 【新增】检查收藏状态实现
     */
    @Override
    public boolean isCollected(Long userId, Long videoId) {
        VideoInteraction interaction = interactionMapper.selectByUserIdAndVideoIdAndType(userId, videoId, TYPE_COLLECT);
        return interaction != null;
    }
}