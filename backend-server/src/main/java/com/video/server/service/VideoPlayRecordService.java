package com.video.server.service;

import com.video.server.entity.Video;

import java.util.List;

/**
 * 视频播放记录服务接口
 */
public interface VideoPlayRecordService {
    
    /**
     * 记录播放历史
     * @param userId 用户ID
     * @param videoId 视频ID
     * @param duration 观看时长（秒）
     * @param progress 播放进度（%）
     * @param isFinish 是否完播
     */
    void recordPlay(Long userId, Long videoId, Integer duration, Integer progress, Boolean isFinish);
    
    /**
     * 获取用户的播放历史
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 视频列表
     */
    List<Video> getPlayHistory(Long userId, Integer limit);
    /**
     * 【新增】删除单条记录
     */
    void deleteRecord(Long userId, Long videoId);

    /**
     * 【新增】清空历史
     */
    void clearHistory(Long userId);
}
