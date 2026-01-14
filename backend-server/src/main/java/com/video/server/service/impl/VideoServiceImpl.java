package com.video.server.service.impl;

import com.video.server.constant.VideoStatus;
import com.video.server.dto.PageResponse;
import com.video.server.dto.VideoListRequest;
import com.video.server.entity.Video;
import com.video.server.mapper.VideoMapper;
import com.video.server.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 视频服务实现类
 */
@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {
    
    private final VideoMapper videoMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String HOT_VIDEO_LIST_KEY = "hot:video:list";
    private static final long CACHE_EXPIRE_HOURS = 1;
    
    @Override
    public List<Video> getHotVideoList() {
        // 先从 Redis 获取缓存
        @SuppressWarnings("unchecked")
        List<Video> cachedList = (List<Video>) redisTemplate.opsForValue().get(HOT_VIDEO_LIST_KEY);
        
        if (cachedList != null && !cachedList.isEmpty()) {
            return cachedList;
        }
        
        // 缓存不存在，从数据库查询
        List<Video> videoList = videoMapper.selectByStatusOrderByPlayCountDesc("PASSED", 20);
        
        // 存入 Redis，设置过期时间为1小时
        if (videoList != null && !videoList.isEmpty()) {
            redisTemplate.opsForValue().set(HOT_VIDEO_LIST_KEY, videoList, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        }
        
        return videoList;
    }
    
    @Override
    public PageResponse<Video> getVideoList(VideoListRequest request) {
        // 转换状态：前端传的是 pending/published/removed，需要转换为数据库状态
        String status = convertStatus(request.getStatus());
        
        // 计算偏移量
        int offset = (request.getPage() - 1) * request.getPageSize();
        
        // 查询列表
        List<Video> list = videoMapper.selectByCondition(
            request.getKeyword(), 
            status, 
            offset, 
            request.getPageSize()
        );
        
        // 查询总数
        Long total = videoMapper.countByCondition(request.getKeyword(), status);
        
        return new PageResponse<>(list, total, request.getPage(), request.getPageSize());
    }
    
    @Override
    public Video getVideoById(Long videoId) {
        return videoMapper.selectById(videoId);
    }
    
    @Override
    public List<Video> getRecommendVideoList(Long userId, Integer limit) {
        // 简化版推荐：直接返回热门视频
        // 实际应该根据用户画像和协同过滤算法推荐
        if (limit == null || limit <= 0) {
            limit = 10;
        }
        return videoMapper.selectByStatusOrderByPlayCountDesc("PASSED", limit);
    }
    
    @Override
    public void incrementLikeCount(Long videoId) {
        videoMapper.incrementLikeCount(videoId);
    }
    
    @Override
    public void updateStatus(Long videoId, String status) {
        videoMapper.updateStatusById(videoId, status);
    }
    
    @Override
    public void deleteVideo(Long videoId) {
        videoMapper.deleteById(videoId);
    }
    
    @Override
    public void setHot(Long videoId, Boolean isHot) {
        int hotStatus = isHot ? 1 : 0;
        videoMapper.updateHotStatus(videoId, hotStatus);
        // 如果设置为热门，清除缓存
        if (isHot) {
            redisTemplate.delete(HOT_VIDEO_LIST_KEY);
        }
    }
    
    /**
     * 转换状态：前端状态 -> 数据库状态
     */
    private String convertStatus(String frontendStatus) {
        if (frontendStatus == null || "all".equals(frontendStatus)) {
            return null;
        }
        switch (frontendStatus) {
            case "pending":
                return VideoStatus.PENDING.name();
            case "published":
                return VideoStatus.PASSED.name();
            case "removed":
                return VideoStatus.REJECTED.name();
            default:
                return null;
        }
    }
}
