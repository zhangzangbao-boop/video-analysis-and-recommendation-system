package com.video.server.service.impl;

import com.video.server.constant.VideoStatus;
import com.video.server.dto.BatchVideoRequest;
import com.video.server.dto.VideoAuditRequest;
import com.video.server.service.VideoAuditService;
import com.video.server.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 视频审核服务实现类
 */
@Service
@RequiredArgsConstructor
public class VideoAuditServiceImpl implements VideoAuditService {
    
    private final VideoService videoService;
    
    // RedisTemplate 设为可选依赖，如果Redis不可用则跳过缓存操作
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String HOT_VIDEO_LIST_KEY = "hot:video:list";
    
    @Override
    public void auditVideo(VideoAuditRequest request) {
        String status;
        if ("pass".equalsIgnoreCase(request.getAction())) {
            status = VideoStatus.PASSED.name();
            // 如果Redis可用，删除热门视频缓存
            if (redisTemplate != null) {
                redisTemplate.delete(HOT_VIDEO_LIST_KEY);
            }
        } else if ("reject".equalsIgnoreCase(request.getAction())) {
            status = VideoStatus.REJECTED.name();
        } else {
            throw new IllegalArgumentException("无效的审核操作: " + request.getAction());
        }
        
        // 更新视频状态
        videoService.updateStatus(request.getVideoId(), status);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchOperateVideos(BatchVideoRequest request) {
        List<Long> videoIds = request.getVideoIds();
        if (videoIds == null || videoIds.isEmpty()) {
            throw new IllegalArgumentException("视频ID列表不能为空");
        }
        
        String action = request.getAction();
        if ("pass".equalsIgnoreCase(action)) {
            // 批量通过
            String status = VideoStatus.PASSED.name();
            for (Long videoId : videoIds) {
                videoService.updateStatus(videoId, status);
            }
            // 如果Redis可用，删除热门视频缓存
            if (redisTemplate != null) {
                redisTemplate.delete(HOT_VIDEO_LIST_KEY);
            }
        } else if ("delete".equalsIgnoreCase(action)) {
            // 批量下架
            for (Long videoId : videoIds) {
                videoService.deleteVideo(videoId);
            }
        } else {
            throw new IllegalArgumentException("无效的批量操作类型: " + action);
        }
    }
}
