package com.video.server.controller;

import com.video.server.dto.ApiResponse;
import com.video.server.dto.PageResponse;
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

    @GetMapping("/hot")
    public ResponseEntity<ApiResponse<List<Video>>> getHotVideoList() {
        List<Video> list = videoService.getHotVideoList();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/recommend")
    public ResponseEntity<ApiResponse<List<Video>>> getRecommendVideoList(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "10") Integer limit) {
        List<Video> list = videoService.getRecommendVideoList(userId, limit);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    /**
     * 获取我的视频列表
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<PageResponse<Video>>> getMyVideos(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer limit,
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            String userIdStr = request.getHeader("X-User-Id");
            if (userIdStr != null && !userIdStr.isEmpty()) {
                try {
                    userId = Long.parseLong(userIdStr);
                } catch (Exception e) {
                    // ignore
                }
            }
        }

        // 调试日志：看看到底是哪个用户在查
        System.out.println(">>> 获取我的作品，当前查询 userId: " + userId);

        if (userId == null) {
            // 如果拿不到 ID，不要默认用 1L，否则用户 10003 也就看不到自己的数据了
            // 这里返回一个空的 PageResponse 或者报错，防止误导
            return ResponseEntity.ok(ApiResponse.success(new PageResponse<>(java.util.Collections.emptyList(), 0L, page, limit)));
        }

        PageResponse<Video> response = videoService.getMyVideos(userId, page, limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Void>> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "coverFile", required = false) MultipartFile coverFile,
            @RequestParam(value = "categoryId", required = false) Integer categoryId,
            @RequestParam(value = "tags", required = false) String tags,
            HttpServletRequest request
    ) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            String userIdStr = request.getHeader("X-User-Id");
            if (userIdStr != null && !userIdStr.isEmpty()) {
                try {
                    userId = Long.parseLong(userIdStr);
                } catch (NumberFormatException e) {
                    userId = 1L;
                }
            } else {
                userId = 1L;
            }
        }

        System.out.println(">>> 上传视频，归属 userId: " + userId);

        videoService.uploadAndPublish(file, coverFile, title, description, categoryId, tags, userId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<ApiResponse<Video>> getVideoById(@PathVariable Long id) {
        Video video = videoService.getVideoById(id);
        return ResponseEntity.ok(ApiResponse.success(video));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<VideoCategory>>> getCategories() {
        List<VideoCategory> categories = videoService.getCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    /**
     * 搜索视频
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<Video>>> searchVideos(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        com.video.server.dto.VideoListRequest request = new com.video.server.dto.VideoListRequest();
        request.setKeyword(keyword);
        request.setCategory(categoryId != null ? categoryId.toString() : null);
        request.setStatus("PASSED"); // 只搜索已发布的视频
        request.setPage(page);
        request.setPageSize(pageSize);
        PageResponse<Video> response = videoService.getVideoList(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 【新增】更新视频信息（编辑功能）
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updateVideo(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, Object> updateData,
            HttpServletRequest request) {
        // 验证视频是否存在
        Video video = videoService.getVideoById(id);
        if (video == null) {
            return ResponseEntity.ok(ApiResponse.fail(404, "视频不存在"));
        }
        
        // 提取更新字段
        String title = updateData.containsKey("title") ? (String) updateData.get("title") : null;
        String description = updateData.containsKey("description") ? (String) updateData.get("description") : null;
        Integer categoryId = updateData.containsKey("categoryId") ? 
            (updateData.get("categoryId") instanceof Integer ? (Integer) updateData.get("categoryId") : 
             Integer.valueOf(updateData.get("categoryId").toString())) : null;
        String tags = updateData.containsKey("tags") ? (String) updateData.get("tags") : null;
        String coverUrl = updateData.containsKey("coverUrl") ? (String) updateData.get("coverUrl") : null;
        
        // 调用服务层更新
        videoService.updateVideo(id, title, description, categoryId, tags, coverUrl);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMyVideo(@PathVariable Long id, HttpServletRequest request) {
        videoService.deleteVideo(id);
        return ResponseEntity.ok(ApiResponse.success());
    }
}