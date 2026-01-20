package com.video.server.controller;

import com.video.server.dto.ApiResponse;
import com.video.server.service.VideoInteractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 视频互动控制器
 */
@RestController
@RequestMapping("/api/v1/video")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
public class VideoInteractionController {

    private final VideoInteractionService interactionService;

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
     * 点赞视频
     */
    @PostMapping("/{videoId}/like")
    public ResponseEntity<ApiResponse<Boolean>> likeVideo(
            @PathVariable Long videoId,
            HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return ResponseEntity.ok(ApiResponse.fail(401, "未登录"));
        }

        boolean success = interactionService.likeVideo(userId, videoId);
        return ResponseEntity.ok(ApiResponse.success(success));
    }

    /**
     * 取消点赞
     */
    @DeleteMapping("/{videoId}/like")
    public ResponseEntity<ApiResponse<Boolean>> unlikeVideo(
            @PathVariable Long videoId,
            HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return ResponseEntity.ok(ApiResponse.fail(401, "未登录"));
        }

        boolean success = interactionService.unlikeVideo(userId, videoId);
        return ResponseEntity.ok(ApiResponse.success(success));
    }

    /**
     * 【修复】检查是否已点赞
     * 路径已修正为: /{videoId}/like/status
     */
    @GetMapping("/{videoId}/like/status")
    public ResponseEntity<ApiResponse<Boolean>> isLiked(
            @PathVariable Long videoId,
            HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return ResponseEntity.ok(ApiResponse.success(false));
        }

        boolean isLiked = interactionService.isLiked(userId, videoId);
        return ResponseEntity.ok(ApiResponse.success(isLiked));
    }

    /**
     * 收藏视频
     */
    @PostMapping("/{videoId}/collect")
    public ResponseEntity<ApiResponse<Boolean>> collectVideo(
            @PathVariable Long videoId,
            HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return ResponseEntity.ok(ApiResponse.fail(401, "未登录"));
        }

        boolean success = interactionService.collectVideo(userId, videoId);
        return ResponseEntity.ok(ApiResponse.success(success));
    }

    /**
     * 取消收藏
     */
    @DeleteMapping("/{videoId}/collect")
    public ResponseEntity<ApiResponse<Boolean>> uncollectVideo(
            @PathVariable Long videoId,
            HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return ResponseEntity.ok(ApiResponse.fail(401, "未登录"));
        }

        boolean success = interactionService.uncollectVideo(userId, videoId);
        return ResponseEntity.ok(ApiResponse.success(success));
    }

    /**
     * 【新增】检查是否已收藏
     * 路径: /{videoId}/collect/status
     */
    @GetMapping("/{videoId}/collect/status")
    public ResponseEntity<ApiResponse<Boolean>> isCollected(
            @PathVariable Long videoId,
            HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return ResponseEntity.ok(ApiResponse.success(false));
        }

        boolean isCollected = interactionService.isCollected(userId, videoId);
        return ResponseEntity.ok(ApiResponse.success(isCollected));
    }
}