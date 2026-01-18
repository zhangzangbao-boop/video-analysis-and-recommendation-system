package com.video.server.controller;

import com.video.server.dto.ApiResponse;
import com.video.server.entity.Video;
import com.video.server.entity.VideoCategory;
import com.video.server.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 视频控制器（用户端）
 */
@RestController
@RequestMapping("/api/v1/video")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
public class VideoController {
    
    private final VideoService videoService;
    
    /**
     * 获取热门视频列表
     */
    @GetMapping("/hot")
    public ResponseEntity<ApiResponse<List<Video>>> getHotVideoList() {
        List<Video> list = videoService.getHotVideoList();
        return ResponseEntity.ok(ApiResponse.success(list));
    }
    
    /**
     * 获取推荐视频列表
     */
    @GetMapping("/recommend")
    public ResponseEntity<ApiResponse<List<Video>>> getRecommendVideoList(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "10") Integer limit) {
        List<Video> list = videoService.getRecommendVideoList(userId, limit);
        return ResponseEntity.ok(ApiResponse.success(list));
    }
    
    /**
     * 获取视频详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Video>> getVideoById(@PathVariable Long id) {
        Video video = videoService.getVideoById(id);
        return ResponseEntity.ok(ApiResponse.success(video));
    }

    /**
     * 视频上传接口
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Void>> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "coverFile", required = false) MultipartFile coverFile,
            @RequestParam(value = "categoryId", required = false) Integer categoryId,
            @RequestParam(value = "tags", required = false) String tags,
            HttpServletRequest request // 用于获取当前登录用户
    ) {
        // 1. 从 Request 中获取用户 ID (假设 JwtInterceptor 已经将 userId 放入 attribute)
        // 或者从 header 解析 token
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            // 如果拦截器未配置，从 localStorage 中的 userId 获取（临时方案）
            // 实际应该从 JWT token 中解析
            String userIdStr = request.getHeader("X-User-Id");
            if (userIdStr != null && !userIdStr.isEmpty()) {
                try {
                    userId = Long.parseLong(userIdStr);
                } catch (NumberFormatException e) {
                    userId = 1L; // 默认用户ID，测试用
                }
            } else {
                userId = 1L; // 默认用户ID，测试用
            }
        }

        // 2. 调用 Service（传入封面文件）
        videoService.uploadAndPublish(file, coverFile, title, description, categoryId, tags, userId);

        return ResponseEntity.ok(ApiResponse.success());
    }
    
    /**
     * 获取视频分类列表
     */
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<VideoCategory>>> getCategories() {
        List<VideoCategory> categories = videoService.getCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }
}
