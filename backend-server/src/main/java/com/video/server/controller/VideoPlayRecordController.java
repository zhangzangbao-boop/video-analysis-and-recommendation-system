package com.video.server.controller;

import com.video.server.dto.ApiResponse;
import com.video.server.entity.Video;
import com.video.server.service.VideoPlayRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 视频播放记录控制器
 */
@RestController
@RequestMapping("/api/v1/video")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
public class VideoPlayRecordController {
    
    private final VideoPlayRecordService playRecordService;
    
    /**
     * 从请求中获取用户ID
     */
    private Long getUserIdFromRequest(HttpServletRequest request) {
        Object userIdObj = request.getAttribute("userId");
        if (userIdObj == null) {
            String userIdStr = request.getHeader("X-User-Id");
            if (userIdStr != null && !userIdStr.isEmpty()) {
                try {
                    return Long.parseLong(userIdStr);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        }
        return (Long) userIdObj;
    }
    
    /**
     * 记录播放历史
     */
    @PostMapping("/{videoId}/play")
    public ResponseEntity<ApiResponse<Void>> recordPlay(
            @PathVariable Long videoId,
            @RequestParam(required = false, defaultValue = "0") Integer duration,
            @RequestParam(required = false, defaultValue = "0") Integer progress,
            @RequestParam(required = false, defaultValue = "false") Boolean isFinish,
            HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return ResponseEntity.ok(ApiResponse.fail(401, "未登录"));
        }
        
        playRecordService.recordPlay(userId, videoId, duration, progress, isFinish);
        return ResponseEntity.ok(ApiResponse.success());
    }
    
    /**
     * 获取播放历史
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<Video>>> getPlayHistory(
            @RequestParam(defaultValue = "20") Integer limit,
            HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return ResponseEntity.ok(ApiResponse.fail(401, "未登录"));
        }
        
        List<Video> videos = playRecordService.getPlayHistory(userId, limit);
        return ResponseEntity.ok(ApiResponse.success(videos));
    }
}
