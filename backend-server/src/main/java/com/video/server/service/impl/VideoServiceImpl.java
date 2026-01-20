package com.video.server.service.impl;

import com.video.server.constant.VideoStatus;
import com.video.server.dto.PageResponse;
import com.video.server.dto.VideoListRequest;
import com.video.server.dto.VideoUploadRequest;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {

    private final VideoMapper videoMapper;
    private final CategoryMapper categoryMapper;
    private final TencentCosVideoUtil tencentCosVideoUtil;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    private static final String HOT_VIDEO_LIST_KEY = "hot:video:list";
    private static final long CACHE_EXPIRE_HOURS = 1;

    @Override
    public List<Video> getHotVideoList() {
        if (redisTemplate != null) {
            @SuppressWarnings("unchecked")
            List<Video> cachedList = (List<Video>) redisTemplate.opsForValue().get(HOT_VIDEO_LIST_KEY);
            if (cachedList != null && !cachedList.isEmpty()) {
                return cachedList;
            }
        }
        List<Video> videoList = videoMapper.selectByStatusOrderByPlayCountDesc("PASSED", 20);
        if (redisTemplate != null && videoList != null && !videoList.isEmpty()) {
            redisTemplate.opsForValue().set(HOT_VIDEO_LIST_KEY, videoList, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        }
        return videoList;
    }

    @Override
    public List<Video> getHotVideos() {
        return getHotVideoList();
    }

    @Override
    public PageResponse<Video> getVideoList(VideoListRequest request) {
        String status = convertStatus(request.getStatus());
        int offset = (request.getPage() - 1) * request.getPageSize();
        List<Video> list = videoMapper.selectByCondition(request.getKeyword(), status, offset, request.getPageSize());
        Long total = videoMapper.countByCondition(request.getKeyword(), status);
        return new PageResponse<>(list, total, request.getPage(), request.getPageSize());
    }

    @Override
    public PageResponse<Video> getMyVideos(Long userId, int page, int limit) {
        int offset = (page - 1) * limit;
        List<Video> list = videoMapper.selectByUserId(userId, offset, limit);
        Long total = videoMapper.countByUserId(userId);
        return new PageResponse<>(list, total, page, limit);
    }

    @Override
    public Video getVideoById(Long videoId) {
        Video video = videoMapper.selectById(videoId);
        if (video != null) {
            videoMapper.incrementPlayCount(videoId);
        }
        return video;
    }

    @Override
    public List<Video> getRecommendVideoList(Long userId, Integer limit) {
        if (limit == null || limit <= 0) limit = 10;
        if (userId == null) {
            return videoMapper.selectByStatusOrderByPlayCountDesc("PASSED", limit);
        }
        if (redisTemplate != null) {
            String recommendKey = "recommend:user:" + userId;
            @SuppressWarnings("unchecked")
            List<Long> recommendVideoIds = (List<Long>) redisTemplate.opsForValue().get(recommendKey);
            if (recommendVideoIds != null && !recommendVideoIds.isEmpty()) {
                if (recommendVideoIds.size() > limit) recommendVideoIds = recommendVideoIds.subList(0, limit);
                List<Video> recommendVideos = videoMapper.selectByIds(recommendVideoIds);
                if (recommendVideos != null && !recommendVideos.isEmpty()) return recommendVideos;
            }
        }
        return videoMapper.selectByStatusOrderByPlayCountDesc("PASSED", limit);
    }

    @Override
    public List<Video> getRecommendVideos(Long userId, Integer limit) {
        return getRecommendVideoList(userId, limit);
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
    public void auditVideoResult(Long videoId, String status, String reason) {
        videoMapper.updateAuditResult(videoId, status, reason);
    }

    @Override
    public void deleteVideo(Long videoId) {
        videoMapper.deleteById(videoId);
    }

    @Override
    public void setHot(Long videoId, Boolean isHot) {
        int hotStatus = isHot ? 1 : 0;
        videoMapper.updateHotStatus(videoId, hotStatus);
        if (isHot && redisTemplate != null) {
            redisTemplate.delete(HOT_VIDEO_LIST_KEY);
        }
    }

    @Override
    public void uploadAndPublish(MultipartFile file, MultipartFile coverFile, String title, String description, Integer categoryId, String tags, Long userId) {
        if (file.isEmpty()) throw new RuntimeException("视频文件不能为空");
        String videoUrl = tencentCosVideoUtil.uploadVideo(file);
        String coverUrl = "";
        if (coverFile != null && !coverFile.isEmpty()) {
            coverUrl = tencentCosVideoUtil.uploadCover(coverFile);
        }
        Video video = new Video();
        video.setId(com.video.server.utils.IdGenerator.nextId());
        video.setAuthorId(userId);
        video.setTitle(title);
        video.setDescription(description);
        video.setVideoUrl(videoUrl);
        video.setCoverUrl(coverUrl);
        video.setCategoryId(categoryId);
        video.setTags(tags);
        video.setDuration(0);
        video.setStatus(VideoStatus.PENDING);
        video.setIsHot(0);
        video.setPlayCount(0L);
        video.setLikeCount(0L);
        video.setCommentCount(0L);
        video.setShareCount(0L);
        video.setIsDeleted(0);
        video.setCreateTime(LocalDateTime.now());
        video.setUpdateTime(LocalDateTime.now());
        videoMapper.insert(video);
    }

    @Override
    @Transactional
    public void uploadVideo(VideoUploadRequest request, Long userId) {
        Video video = new Video();
        video.setId(com.video.server.utils.IdGenerator.nextId());
        video.setAuthorId(userId);
        video.setTitle(request.getTitle());
        video.setDescription(request.getDescription());
        video.setVideoUrl(request.getVideoUrl());
        video.setCoverUrl(request.getCoverUrl());
        video.setCategoryId(request.getCategoryId());
        video.setTags(request.getTags());
        video.setDuration(request.getDuration() != null ? request.getDuration() : 0);
        video.setStatus(VideoStatus.PENDING);
        video.setIsHot(0);
        video.setPlayCount(0L);
        video.setLikeCount(0L);
        video.setCommentCount(0L);
        video.setShareCount(0L);
        video.setIsDeleted(0);
        video.setCreateTime(LocalDateTime.now());
        video.setUpdateTime(LocalDateTime.now());
        videoMapper.insert(video);
    }

    @Override
    public List<VideoCategory> getCategories() {
        return categoryMapper.selectAll();
    }

    @Override
    public List<Video> searchVideos(String keyword, Integer categoryId, Integer page, Integer pageSize) {
        int offset = (page - 1) * pageSize;
        return videoMapper.searchVideos(keyword, categoryId, offset, pageSize);
    }

    /**
     * 【新增】实现获取用户作品
     */
    @Override
    public List<Video> getUserPublishedVideos(Long userId, Integer limit) {
        return videoMapper.selectByAuthorId(userId, limit);
    }

    private String convertStatus(String frontendStatus) {
        if (frontendStatus == null || "all".equalsIgnoreCase(frontendStatus)) return null;
        if ("PENDING".equalsIgnoreCase(frontendStatus)) return VideoStatus.PENDING.name();
        if ("PASSED".equalsIgnoreCase(frontendStatus) || "PUBLISHED".equalsIgnoreCase(frontendStatus)) return VideoStatus.PASSED.name();
        if ("REJECTED".equalsIgnoreCase(frontendStatus) || "REMOVED".equalsIgnoreCase(frontendStatus)) return VideoStatus.REJECTED.name();
        switch (frontendStatus.toLowerCase()) {
            case "pending": return VideoStatus.PENDING.name();
            case "published": return VideoStatus.PASSED.name();
            case "removed": return VideoStatus.REJECTED.name();
            default: return null;
        }
    }
}