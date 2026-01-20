package com.video.server.controller;

import com.video.server.dto.ApiResponse;
import com.video.server.dto.VideoCommentDTO;
import com.video.server.service.VideoCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/v1/video")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
public class VideoCommentController {

    private final VideoCommentService commentService;

    // 获取用户ID的辅助方法
    private Long getUserIdFromRequest(HttpServletRequest request) {
        Object userIdObj = request.getAttribute("userId");
        if (userIdObj == null) {
            String userIdStr = request.getHeader("X-User-Id");
            try {
                return userIdStr != null ? Long.parseLong(userIdStr) : null;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return (Long) userIdObj;
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
        if (userId == null) return ResponseEntity.ok(ApiResponse.fail(401, "未登录"));

        VideoCommentDTO result = commentService.addComment(videoId, userId, content, parentId, replyUserId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 获取一级评论列表
     */
    @GetMapping("/{videoId}/comment")
    public ResponseEntity<ApiResponse<List<VideoCommentDTO>>> getComments(
            @PathVariable Long videoId,
            @RequestParam(defaultValue = "20") Integer limit) {
        List<VideoCommentDTO> list = commentService.getCommentsByVideoId(videoId, limit);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    /**
     * 获取子回复列表
     */
    @GetMapping("/comment/{parentId}/replies")
    public ResponseEntity<ApiResponse<List<VideoCommentDTO>>> getReplies(
            @PathVariable Long parentId,
            @RequestParam(defaultValue = "10") Integer limit) {
        List<VideoCommentDTO> list = commentService.getRepliesByParentId(parentId, limit);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    /**
     * 【修复】新增：点赞评论接口
     */
    @PostMapping("/comment/{commentId}/like")
    public ResponseEntity<ApiResponse<Boolean>> likeComment(
            @PathVariable Long commentId,
            HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.ok(ApiResponse.fail(401, "未登录"));

        boolean success = commentService.likeComment(commentId, userId);
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
        if (userId == null) return ResponseEntity.ok(ApiResponse.fail(401, "未登录"));

        boolean success = commentService.deleteComment(commentId, userId);
        return ResponseEntity.ok(ApiResponse.success(success));
    }
}