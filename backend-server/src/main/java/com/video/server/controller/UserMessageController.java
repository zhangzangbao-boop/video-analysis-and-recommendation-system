package com.video.server.controller;

import com.video.server.dto.ApiResponse;
import com.video.server.entity.UserMessage;
import com.video.server.service.UserMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 用户消息控制器
 */
@RestController
@RequestMapping("/api/v1/user/message")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
public class UserMessageController {
    
    private final UserMessageService messageService;
    
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
     * 获取用户的消息列表
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<UserMessage>>> getMessages(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer isRead,
            @RequestParam(defaultValue = "20") Integer limit,
            HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return ResponseEntity.ok(ApiResponse.fail(401, "未登录"));
        }
        
        List<UserMessage> messages = messageService.getMessages(userId, type, isRead, limit);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }
    
    /**
     * 获取未读消息数
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return ResponseEntity.ok(ApiResponse.success(0L));
        }
        
        Long count = messageService.getUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }
    
    /**
     * 标记消息为已读
     */
    @PutMapping("/{messageId}/read")
    public ResponseEntity<ApiResponse<Boolean>> markAsRead(
            @PathVariable Long messageId,
            HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return ResponseEntity.ok(ApiResponse.fail(401, "未登录"));
        }
        
        boolean success = messageService.markAsRead(messageId);
        return ResponseEntity.ok(ApiResponse.success(success));
    }
    
    /**
     * 一键标记所有消息为已读
     */
    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Boolean>> markAllAsRead(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return ResponseEntity.ok(ApiResponse.fail(401, "未登录"));
        }
        
        boolean success = messageService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success(success));
    }
}
