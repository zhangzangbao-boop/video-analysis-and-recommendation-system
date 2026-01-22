package com.video.server.service.impl;

import com.video.server.constant.VideoStatus;
import com.video.server.dto.PageResponse;
import com.video.server.dto.VideoDTO;
import com.video.server.dto.VideoListRequest;
import com.video.server.dto.VideoUploadRequest;
import com.video.server.entity.Video;
import com.video.server.entity.VideoCategory;
import com.video.server.mapper.CategoryMapper;
import com.video.server.mapper.RecommendationResultMapper;
import com.video.server.mapper.VideoMapper;
import com.video.server.service.VideoService;
import com.video.server.utils.TencentCosVideoUtil;
import com.video.server.utils.SimpleContentAuditUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {

    private final VideoMapper videoMapper;
    private final CategoryMapper categoryMapper;
    private final TencentCosVideoUtil tencentCosVideoUtil;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired(required = false)
    private RecommendationResultMapper recommendationResultMapper;

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
        // 查询返回VideoDTO（包含authorName），然后转换为Video
        List<VideoDTO> videoDTOList = videoMapper.selectByStatusOrderByPlayCountDesc("PASSED", 20);
        List<Video> videoList = videoDTOList != null ? videoDTOList.stream()
            .map(dto -> {
                Video video = new Video();
                BeanUtils.copyProperties(dto, video);
                return video;
            })
            .collect(Collectors.toList()) : null;
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
        List<VideoDTO> dtoList = videoMapper.selectByCondition(request.getKeyword(), status, offset, request.getPageSize());
        List<Video> list = dtoList != null ? dtoList.stream()
            .map(dto -> {
                Video video = new Video();
                BeanUtils.copyProperties(dto, video);
                return video;
            })
            .collect(Collectors.toList()) : null;
        Long total = videoMapper.countByCondition(request.getKeyword(), status);
        return new PageResponse<>(list, total, request.getPage(), request.getPageSize());
    }

    @Override
    public PageResponse<Video> getMyVideos(Long userId, int page, int limit) {
        int offset = (page - 1) * limit;
        List<VideoDTO> dtoList = videoMapper.selectByUserId(userId, offset, limit);
        List<Video> list = dtoList != null ? dtoList.stream()
            .map(dto -> {
                Video video = new Video();
                BeanUtils.copyProperties(dto, video);
                return video;
            })
            .collect(Collectors.toList()) : null;
        Long total = videoMapper.countByUserId(userId);
        return new PageResponse<>(list, total, page, limit);
    }

    @Override
    public Video getVideoById(Long videoId) {
        VideoDTO videoDTO = videoMapper.selectById(videoId);
        Video video = null;
        if (videoDTO != null) {
            video = new Video();
            BeanUtils.copyProperties(videoDTO, video);
            videoMapper.incrementPlayCount(videoId);
        }
        return video;
    }

    @Override
    public List<Video> getRecommendVideoList(Long userId, Integer limit) {
        return getRecommendVideoList(userId, limit, null);
    }
    
    /**
     * 获取推荐视频列表（支持排除已推送的视频，用于刷新功能）
     * 混合推荐策略（隐藏逻辑）：
     * - 70% 从 recommendation_result 表获取（REALTIME优先，按score降序，rank升序）
     * - 20% 从热门视频获取（按播放量排序）
     * - 10% 随机推荐（增加多样性）
     * 
     * @param userId 用户ID
     * @param limit 返回数量限制
     * @param excludeVideoIds 排除的视频ID列表（用于刷新时排除已推送的视频）
     * @return 推荐视频列表
     */
    public List<Video> getRecommendVideoList(Long userId, Integer limit, List<Long> excludeVideoIds) {
        if (limit == null || limit <= 0) limit = 10;
        
        List<Video> finalRecommendVideos = new java.util.ArrayList<>();
        java.util.Set<Long> usedVideoIds = new java.util.HashSet<>();
        if (excludeVideoIds != null) {
            usedVideoIds.addAll(excludeVideoIds);
        }
        
        // ========== 策略1: 70% 从 recommendation_result 表获取 ==========
        int recommendCount = (int) Math.ceil(limit * 0.7);
        if (userId != null && recommendationResultMapper != null) {
            try {
                List<Long> recommendVideoIds = recommendationResultMapper.selectVideoIdsByUserId(
                    userId, recommendCount * 2, excludeVideoIds); // 多取一些，避免过滤后不够
                if (recommendVideoIds != null && !recommendVideoIds.isEmpty()) {
                    List<Long> filteredIds = recommendVideoIds.stream()
                        .filter(id -> !usedVideoIds.contains(id))
                        .limit(recommendCount)
                        .collect(java.util.stream.Collectors.toList());
                    
                    if (!filteredIds.isEmpty()) {
                        List<VideoDTO> recommendVideoDTOs = videoMapper.selectByIds(filteredIds);
                        List<Video> recommendVideos = recommendVideoDTOs != null ? recommendVideoDTOs.stream()
                            .map(dto -> {
                                Video video = new Video();
                                BeanUtils.copyProperties(dto, video);
                                return video;
                            })
                            .collect(Collectors.toList()) : null;
                        if (recommendVideos != null && !recommendVideos.isEmpty()) {
                            // 保持 recommendation_result 中的顺序
                            recommendVideos = recommendVideos.stream()
                                .sorted((v1, v2) -> {
                                    int idx1 = filteredIds.indexOf(v1.getId());
                                    int idx2 = filteredIds.indexOf(v2.getId());
                                    return Integer.compare(idx1, idx2);
                                })
                                .collect(java.util.stream.Collectors.toList());
                            
                            finalRecommendVideos.addAll(recommendVideos);
                            recommendVideos.forEach(v -> usedVideoIds.add(v.getId()));
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[WARN] 从recommendation_result获取推荐失败: " + e.getMessage());
            }
        }
        
        // ========== 策略2: 20% 从热门视频获取 ==========
        int hotCount = (int) Math.ceil(limit * 0.2);
        if (finalRecommendVideos.size() < limit) {
            try {
                List<VideoDTO> hotVideoDTOs = videoMapper.selectByStatusOrderByPlayCountDesc("PASSED", hotCount * 2);
                if (hotVideoDTOs != null && !hotVideoDTOs.isEmpty()) {
                    List<Video> hotVideos = hotVideoDTOs.stream()
                        .map(dto -> {
                            Video v = new Video();
                            BeanUtils.copyProperties(dto, v);
                            return v;
                        })
                        .collect(Collectors.toList());
                    List<Video> filteredHotVideos = hotVideos.stream()
                        .filter(v -> !usedVideoIds.contains(v.getId()))
                        .limit(hotCount)
                        .collect(java.util.stream.Collectors.toList());
                    
                    if (!filteredHotVideos.isEmpty()) {
                        finalRecommendVideos.addAll(filteredHotVideos);
                        filteredHotVideos.forEach(v -> usedVideoIds.add(v.getId()));
                    }
                }
            } catch (Exception e) {
                System.err.println("[WARN] 获取热门视频失败: " + e.getMessage());
            }
        }
        
        // ========== 策略3: 10% 随机推荐（增加多样性） ==========
        int randomCount = limit - finalRecommendVideos.size();
        if (randomCount > 0 && userId != null && recommendationResultMapper != null) {
            try {
                List<Long> randomVideoIds = recommendationResultMapper.selectRandomVideoIds(
                    new java.util.ArrayList<>(usedVideoIds), randomCount);
                if (randomVideoIds != null && !randomVideoIds.isEmpty()) {
                    List<VideoDTO> randomVideoDTOs = videoMapper.selectByIds(randomVideoIds);
                    List<Video> randomVideos = randomVideoDTOs != null ? randomVideoDTOs.stream()
                        .map(dto -> {
                            Video video = new Video();
                            BeanUtils.copyProperties(dto, video);
                            return video;
                        })
                        .collect(Collectors.toList()) : null;
                    if (randomVideos != null && !randomVideos.isEmpty()) {
                        finalRecommendVideos.addAll(randomVideos);
                    }
                }
            } catch (Exception e) {
                System.err.println("[WARN] 随机推荐失败: " + e.getMessage());
            }
        }
        
        // ========== 兜底方案：如果还不够，用热门视频补齐 ==========
        if (finalRecommendVideos.size() < limit) {
            try {
                int remainingCount = limit - finalRecommendVideos.size();
                List<VideoDTO> hotVideoDTOs = videoMapper.selectByStatusOrderByPlayCountDesc("PASSED", remainingCount * 2);
                if (hotVideoDTOs != null && !hotVideoDTOs.isEmpty()) {
                    List<Video> hotVideos = hotVideoDTOs.stream()
                        .map(dto -> {
                            Video v = new Video();
                            BeanUtils.copyProperties(dto, v);
                            return v;
                        })
                        .collect(Collectors.toList());
                    List<Video> filteredHotVideos = hotVideos.stream()
                        .filter(v -> !usedVideoIds.contains(v.getId()))
                        .limit(remainingCount)
                        .collect(java.util.stream.Collectors.toList());
                    
                    if (!filteredHotVideos.isEmpty()) {
                        finalRecommendVideos.addAll(filteredHotVideos);
                    }
                }
            } catch (Exception e) {
                System.err.println("[WARN] 兜底推荐失败: " + e.getMessage());
            }
        }
        
        // 打乱顺序，避免用户感知到推荐策略
        java.util.Collections.shuffle(finalRecommendVideos);
        
        return finalRecommendVideos.size() > limit ? 
            finalRecommendVideos.subList(0, limit) : finalRecommendVideos;
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
    public void updateAuditMessage(Long videoId, String auditMsg) {
        videoMapper.updateAuditMessage(videoId, auditMsg);
    }

    @Override
    public void uploadAndPublish(MultipartFile file, MultipartFile coverFile, String title, String description, Integer categoryId, String tags, Long userId) {
        if (file.isEmpty()) throw new RuntimeException("视频文件不能为空");
        String videoUrl = tencentCosVideoUtil.uploadVideo(file);
        String coverUrl = "";
        if (coverFile != null && !coverFile.isEmpty()) {
            coverUrl = tencentCosVideoUtil.uploadCover(coverFile);
        }
        
        // AI自动审核文本内容
        SimpleContentAuditUtil.AuditResult auditResult = SimpleContentAuditUtil.auditVideoContent(title, description, tags);
        
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
        
        // 保存AI审核结果到审核意见字段
        String auditMsg = "[AI审核] " + auditResult.getReason();
        video.setAuditMsg(auditMsg);
        
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
        // AI自动审核文本内容
        SimpleContentAuditUtil.AuditResult auditResult = SimpleContentAuditUtil.auditVideoContent(
            request.getTitle(), 
            request.getDescription(), 
            request.getTags()
        );
        
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
        
        // 保存AI审核结果到审核意见字段
        String auditMsg = "[AI审核] " + auditResult.getReason();
        video.setAuditMsg(auditMsg);
        
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

    /**
     * 【新增】更新视频信息（编辑功能）
     */
    @Override
    @Transactional
    public void updateVideo(Long videoId, String title, String description, Integer categoryId, String tags, String coverUrl) {
        VideoDTO videoDTO = videoMapper.selectById(videoId);
        if (videoDTO == null) {
            throw new RuntimeException("视频不存在");
        }
        Video video = new Video();
        BeanUtils.copyProperties(videoDTO, video);
        
        // 更新字段
        if (title != null) video.setTitle(title);
        if (description != null) video.setDescription(description);
        if (categoryId != null) video.setCategoryId(categoryId);
        if (tags != null) video.setTags(tags);
        if (coverUrl != null) video.setCoverUrl(coverUrl);
        video.setUpdateTime(LocalDateTime.now());
        
        // 执行更新
        videoMapper.updateById(video);
    }

    @Override
    public List<Video> searchVideos(String keyword, Integer categoryId, Integer page, Integer pageSize) {
        int offset = (page - 1) * pageSize;
        List<VideoDTO> dtoList = videoMapper.searchVideos(keyword, categoryId, offset, pageSize);
        return dtoList != null ? dtoList.stream()
            .map(dto -> {
                Video video = new Video();
                BeanUtils.copyProperties(dto, video);
                return video;
            })
            .collect(Collectors.toList()) : null;
    }
    
    @Override
    public Long countSearchVideos(String keyword, Integer categoryId) {
        return videoMapper.countSearchVideos(keyword, categoryId);
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
    
    @Override
    public Long getTotalPlayCount() {
        Long total = videoMapper.getTotalPlayCount();
        return total != null ? total : 0L;
    }
}