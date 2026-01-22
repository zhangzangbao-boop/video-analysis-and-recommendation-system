package com.video.server.controller.admin;

import com.video.server.dto.ApiResponse;
import com.video.server.dto.BatchVideoRequest;
import com.video.server.dto.PageResponse;
import com.video.server.dto.VideoAuditRequest;
import com.video.server.dto.VideoListRequest;
import com.video.server.entity.Video;
import com.video.server.service.VideoAuditService;
import com.video.server.service.VideoService;
import com.video.server.service.AiAuditService;
import com.video.server.dto.AiAuditResult;
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
    private final AiAuditService aiAuditService;
    
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
    
    /**
     * AI分析视频（管理员手动触发）
     * @param id 视频ID
     * @return AI审核结果
     */
    @PostMapping("/{id}/ai-audit")
    public ResponseEntity<ApiResponse<AiAuditResult>> aiAuditVideo(@PathVariable Long id) {
        Video video = videoService.getVideoById(id);
        if (video == null) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.fail(400, "视频不存在"));
        }
        
        // 调用AI审核服务
        AiAuditResult result = aiAuditService.auditVideo(
            video.getVideoUrl(),
            video.getCoverUrl(),
            video.getTitle(),
            video.getDescription(),
            video.getTags()
        );
        
        // 更新视频的审核意见（保存AI分析结果）
        String auditMsg = buildAuditMessage(result);
        videoService.updateAuditMessage(id, auditMsg);
        
        return ResponseEntity.ok(ApiResponse.success(result));
    }
    
    /**
     * 构建审核消息
     */
    private String buildAuditMessage(AiAuditResult result) {
        StringBuilder msg = new StringBuilder();
        msg.append("[AI审核] ").append(result.getMessage());
        
        if (result.getViolations() != null && !result.getViolations().isEmpty()) {
            msg.append("\n违规详情：");
            for (AiAuditResult.ViolationDetail violation : result.getViolations()) {
                msg.append("\n- ").append(violation.getTypeName())
                   .append("（").append(violation.getLocation()).append("，置信度：")
                   .append(violation.getConfidence()).append("%）");
            }
        }
        
        msg.append("\n建议操作：").append(result.getSuggestion());
        return msg.toString();
    }
}
