package com.video.server.controller;

import com.video.server.dto.ApiResponse;
import com.video.server.entity.Video;
import com.video.server.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 视频控制器（用户端）
 */
@RestController
@RequestMapping("/api/v1/video")
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
}
