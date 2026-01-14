package com.video.server.controller.admin;

import com.video.server.dto.ApiResponse;
import com.video.server.dto.BatchVideoRequest;
import com.video.server.dto.PageResponse;
import com.video.server.dto.VideoAuditRequest;
import com.video.server.dto.VideoListRequest;
import com.video.server.entity.Video;
import com.video.server.service.VideoAuditService;
import com.video.server.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 视频审核控制器（管理后台）
 */
@RestController
@RequestMapping("/api/admin/video")
@RequiredArgsConstructor
public class VideoAuditController {
    
    private final VideoAuditService videoAuditService;
    private final VideoService videoService;
    
    /**
     * 获取视频列表
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<PageResponse<Video>>> getVideoList(VideoListRequest request) {
        PageResponse<Video> response = videoService.getVideoList(request);
        return ResponseEntity.ok(ApiResponse.success(response));
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
     * 审核视频
     * @param request 审核请求
     * @return 操作结果
     */
    @PostMapping("/audit")
    public ResponseEntity<ApiResponse<Void>> auditVideo(@RequestBody VideoAuditRequest request) {
        videoAuditService.auditVideo(request);
        return ResponseEntity.ok(ApiResponse.success());
    }
    
    /**
     * 批量操作视频
     * @param request 批量操作请求
     * @return 操作结果
     */
    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<Void>> batchOperateVideos(@RequestBody BatchVideoRequest request) {
        videoAuditService.batchOperateVideos(request);
        return ResponseEntity.ok(ApiResponse.success());
    }
    
    /**
     * 删除/下架视频
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteVideo(@PathVariable Long id) {
        videoService.deleteVideo(id);
        return ResponseEntity.ok(ApiResponse.success());
    }
    
    /**
     * 设置/取消热门
     */
    @PutMapping("/{id}/hot")
    public ResponseEntity<ApiResponse<Void>> setHot(@PathVariable Long id, @RequestParam Boolean isHot) {
        videoService.setHot(id, isHot);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
