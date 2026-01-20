package com.video.server.service.impl;

import com.video.server.entity.Video;
import com.video.server.entity.VideoPlayRecord;
import com.video.server.mapper.VideoMapper;
import com.video.server.mapper.VideoPlayRecordMapper;
import com.video.server.service.VideoPlayRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 视频播放记录服务实现
 */
@Service
@RequiredArgsConstructor
public class VideoPlayRecordServiceImpl implements VideoPlayRecordService {
    
    private final VideoPlayRecordMapper playRecordMapper;
    private final VideoMapper videoMapper;
    
    @Override
    @Transactional
    public void recordPlay(Long userId, Long videoId, Integer duration, Integer progress, Boolean isFinish) {
        // 查询是否已有记录
        VideoPlayRecord existing = playRecordMapper.selectByUserIdAndVideoId(userId, videoId);
        
        if (existing != null) {
            // 更新现有记录，同时更新播放时间
            existing.setDuration(duration);
            existing.setProgress(progress);
            existing.setIsFinish(isFinish ? 1 : 0);
            existing.setCreateTime(LocalDateTime.now()); // 更新播放时间为当前时间
            playRecordMapper.updateById(existing);
        } else {
            // 创建新记录
            VideoPlayRecord record = new VideoPlayRecord();
            record.setUserId(userId);
            record.setVideoId(videoId);
            record.setDuration(duration);
            record.setProgress(progress);
            record.setIsFinish(isFinish ? 1 : 0);
            record.setCreateTime(LocalDateTime.now());
            playRecordMapper.insert(record);
        }
        
        // 更新视频播放量（如果完播）
        if (isFinish != null && isFinish) {
            Video video = videoMapper.selectById(videoId);
            if (video != null) {
                video.setPlayCount((video.getPlayCount() != null ? video.getPlayCount() : 0L) + 1);
                videoMapper.updateById(video);
            }
        }
    }
    
    @Override
    public List<Video> getPlayHistory(Long userId, Integer limit) {
        // 获取播放历史，确保每个视频只返回最新的一条记录
        List<VideoPlayRecord> records = playRecordMapper.selectByUserIdDistinct(userId, limit);
        return records.stream()
                .map(record -> videoMapper.selectById(record.getVideoId()))
                .filter(video -> video != null)
                .distinct() // 再次去重，防止数据异常
                .collect(Collectors.toList());
    }

    /**
     * 【新增】实现删除单条
     */
    @Override
    @Transactional
    public void deleteRecord(Long userId, Long videoId) {
        playRecordMapper.deleteByUserIdAndVideoId(userId, videoId);
    }

    /**
     * 【新增】实现清空
     */
    @Override
    @Transactional
    public void clearHistory(Long userId) {
        playRecordMapper.deleteByUserId(userId);
    }
}
