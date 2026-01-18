package com.video.server.service.impl;

import com.video.server.constant.VideoStatus;
import com.video.server.dto.PageResponse;
import com.video.server.dto.VideoListRequest;
import com.video.server.entity.Video;
import com.video.server.entity.VideoCategory;
import com.video.server.mapper.CategoryMapper;
import com.video.server.mapper.VideoMapper;
import com.video.server.service.VideoService;
import com.video.server.utils.TencentCosVideoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 视频服务实现类
 */
@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {
    
    private final VideoMapper videoMapper;
    private final CategoryMapper categoryMapper;
    private final TencentCosVideoUtil tencentCosVideoUtil;
    
    // RedisTemplate 设为可选依赖，如果Redis不可用则直接查询数据库
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String HOT_VIDEO_LIST_KEY = "hot:video:list";
    private static final long CACHE_EXPIRE_HOURS = 1;
    
    @Override
    public List<Video> getHotVideoList() {
        // 如果Redis可用，先从 Redis 获取缓存
        if (redisTemplate != null) {
            @SuppressWarnings("unchecked")
            List<Video> cachedList = (List<Video>) redisTemplate.opsForValue().get(HOT_VIDEO_LIST_KEY);
            
            if (cachedList != null && !cachedList.isEmpty()) {
                return cachedList;
            }
        }
        
        // 缓存不存在或Redis不可用，从数据库查询
        List<Video> videoList = videoMapper.selectByStatusOrderByPlayCountDesc("PASSED", 20);
        
        // 如果Redis可用，存入 Redis，设置过期时间为1小时
        if (redisTemplate != null && videoList != null && !videoList.isEmpty()) {
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
        if (limit == null || limit <= 0) {
            limit = 10;
        }
        
        // 如果用户未登录，直接返回热门视频
        if (userId == null) {
            return videoMapper.selectByStatusOrderByPlayCountDesc("PASSED", limit);
        }
        
        // 如果Redis可用，尝试从Redis获取推荐结果
        if (redisTemplate != null) {
            String recommendKey = "recommend:user:" + userId;
            @SuppressWarnings("unchecked")
            List<Long> recommendVideoIds = (List<Long>) redisTemplate.opsForValue().get(recommendKey);
            
            if (recommendVideoIds != null && !recommendVideoIds.isEmpty()) {
                // 限制数量
                if (recommendVideoIds.size() > limit) {
                    recommendVideoIds = recommendVideoIds.subList(0, limit);
                }
                
                // 根据ID列表查询视频详情
                List<Video> recommendVideos = videoMapper.selectByIds(recommendVideoIds);
                
                // 如果推荐结果不为空，返回推荐结果
                if (recommendVideos != null && !recommendVideos.isEmpty()) {
                    return recommendVideos;
                }
            }
        }
        
        // 降级策略：如果Redis中没有推荐结果，返回热门视频
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
        // 如果设置为热门且Redis可用，清除缓存
        if (isHot && redisTemplate != null) {
            redisTemplate.delete(HOT_VIDEO_LIST_KEY);
        }
    }
    
    @Override
    public void uploadAndPublish(MultipartFile file, MultipartFile coverFile, String title, String description, Integer categoryId, String tags, Long userId) {
        // 1. 校验文件
        if (file.isEmpty()) {
            throw new RuntimeException("视频文件不能为空");
        }

        // 2. 上传视频到腾讯云 COS
        String videoUrl = tencentCosVideoUtil.uploadVideo(file);
        
        // 3. 上传封面到腾讯云 COS（如果提供了封面文件）
        String coverUrl = "";
        if (coverFile != null && !coverFile.isEmpty()) {
            coverUrl = tencentCosVideoUtil.uploadCover(coverFile);
        }

        // 4. 构建 Video 实体对象
        Video video = new Video();
        // 生成视频ID（使用雪花算法）
        video.setId(com.video.server.utils.IdGenerator.nextId());
        video.setAuthorId(userId);
        video.setTitle(title);
        video.setDescription(description);
        video.setVideoUrl(videoUrl);
        video.setCoverUrl(coverUrl);
        video.setCategoryId(categoryId);
        video.setTags(tags);
        video.setDuration(0); // 如果无法获取时长，先置为0，或者使用 FFmpeg 获取

        // 设置初始状态
        video.setStatus(VideoStatus.PENDING); // 默认为待审核状态
        video.setIsHot(0);
        video.setPlayCount(0L);
        video.setLikeCount(0L);
        video.setCommentCount(0L);
        video.setShareCount(0L);
        video.setIsDeleted(0);
        video.setCreateTime(LocalDateTime.now());
        video.setUpdateTime(LocalDateTime.now());

        // 5. 保存到数据库
        videoMapper.insert(video);
    }
    
    @Override
    public List<VideoCategory> getCategories() {
        return categoryMapper.selectAll();
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
