package com.video.server.controller;

import com.video.server.dto.ApiResponse;
import com.video.server.dto.VideoCommentDTO;
import com.video.server.service.VideoCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 视频评论控制器
 */
@RestController
@RequestMapping("/api/v1/video")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
public class VideoCommentController {

    private final VideoCommentService commentService;

    /**
     * 辅助方法：从请求中尝试获取当前登录用户ID
     * 如果未登录，返回 null
     */
    private Long getUserIdFromRequest(HttpServletRequest request) {
        // 1. 优先从拦截器设置的 request attribute 中取
        Object userIdObj = request.getAttribute("userId");
        if (userIdObj != null) {
            return (Long) userIdObj;
        }

        // 2. 尝试从 Header 中取 (兼容某些未经过拦截器的场景)
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

    /**
     * 发表评论/回复
     */
    @PostMapping("/{videoId}/comment")
    public ResponseEntity<ApiResponse<VideoCommentDTO>> addComment(
            @PathVariable Long videoId,
            @RequestParam String content,
            @RequestParam(required = false) Long parentId,
            @RequestParam(required = false) Long replyUserId,
            HttpServletRequest request) {

        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return ResponseEntity.ok(ApiResponse.fail(401, "未登录"));
        }

        VideoCommentDTO result = commentService.addComment(videoId, userId, content, parentId, replyUserId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 获取视频的一级评论列表
     * 修改：传入 userId 以便检查点赞状态
     */
    @GetMapping("/{videoId}/comment")
    public ResponseEntity<ApiResponse<List<VideoCommentDTO>>> getComments(
            @PathVariable Long videoId,
            @RequestParam(defaultValue = "20") Integer limit,
            HttpServletRequest request) {

        // 尝试获取当前用户ID (可能为 null)
        Long userId = getUserIdFromRequest(request);

        List<VideoCommentDTO> list = commentService.getCommentsByVideoId(videoId, limit, userId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    /**
     * 获取评论的子回复列表
     * 修改：传入 userId 以便检查点赞状态
     */
    @GetMapping("/comment/{parentId}/replies")
    public ResponseEntity<ApiResponse<List<VideoCommentDTO>>> getReplies(
            @PathVariable Long parentId,
            @RequestParam(defaultValue = "10") Integer limit,
            HttpServletRequest request) {

        Long userId = getUserIdFromRequest(request);

        List<VideoCommentDTO> list = commentService.getRepliesByParentId(parentId, limit, userId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    /**
     * 点赞评论
     */
    @PostMapping("/comment/{commentId}/like")
    public ResponseEntity<ApiResponse<Boolean>> likeComment(
            @PathVariable Long commentId,
            HttpServletRequest request) {

        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return ResponseEntity.ok(ApiResponse.fail(401, "未登录"));
        }

        boolean success = commentService.likeComment(commentId, userId);
        return ResponseEntity.ok(ApiResponse.success(success));
    }

    /**
     * 【新增】取消点赞评论
     */
    @DeleteMapping("/comment/{commentId}/like")
    public ResponseEntity<ApiResponse<Boolean>> unlikeComment(
            @PathVariable Long commentId,
            HttpServletRequest request) {

        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return ResponseEntity.ok(ApiResponse.fail(401, "未登录"));
        }

        boolean success = commentService.unlikeComment(commentId, userId);
        return ResponseEntity.ok(ApiResponse.success(success));
    }

    /**
     * 删除评论
     */
    @DeleteMapping("/comment/{commentId}")
    public ResponseEntity<ApiResponse<Boolean>> deleteComment(
            @PathVariable Long commentId,
            HttpServletRequest request) {

        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return ResponseEntity.ok(ApiResponse.fail(401, "未登录"));
        }

        boolean success = commentService.deleteComment(commentId, userId);
        return ResponseEntity.ok(ApiResponse.success(success));
    }
}