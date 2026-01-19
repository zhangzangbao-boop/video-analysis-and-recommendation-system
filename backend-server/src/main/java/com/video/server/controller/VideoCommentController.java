package com.video.server.controller;

import com.video.server.dto.ApiResponse;
import com.video.server.entity.VideoComment;
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
     * 添加评论
     */
    @PostMapping("/{videoId}/comment")
    public ResponseEntity<ApiResponse<VideoComment>> addComment(
            @PathVariable Long videoId,
            @RequestParam String content,
            @RequestParam(required = false) Long parentId,
            @RequestParam(required = false) Long replyUserId,
            HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return ResponseEntity.ok(ApiResponse.fail(401, "未登录"));
        }
        
        VideoComment comment = commentService.addComment(videoId, userId, content, parentId, replyUserId);
        return ResponseEntity.ok(ApiResponse.success(comment));
    }
    
    /**
     * 获取视频的评论列表
     */
    @GetMapping("/{videoId}/comment")
    public ResponseEntity<ApiResponse<List<VideoComment>>> getComments(
            @PathVariable Long videoId,
            @RequestParam(defaultValue = "20") Integer limit) {
        List<VideoComment> comments = commentService.getCommentsByVideoId(videoId, limit);
        return ResponseEntity.ok(ApiResponse.success(comments));
    }
    
    /**
     * 获取评论的回复列表
     */
    @GetMapping("/comment/{parentId}/replies")
    public ResponseEntity<ApiResponse<List<VideoComment>>> getReplies(
            @PathVariable Long parentId,
            @RequestParam(defaultValue = "10") Integer limit) {
        List<VideoComment> replies = commentService.getRepliesByParentId(parentId, limit);
        return ResponseEntity.ok(ApiResponse.success(replies));
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
