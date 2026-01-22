package com.video.server.controller.admin;

import com.video.server.dto.ApiResponse;
import com.video.server.entity.SystemNotice;
import com.video.server.service.SystemNoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 系统通知管理控制器（管理员）
 */
@RestController
@RequestMapping("/api/v1/admin/notice")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
public class SystemNoticeController {
    
    private final SystemNoticeService noticeService;
    
    /**
     * 创建系统通知（草稿）
     */
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<SystemNotice>> createNotice(@RequestBody SystemNotice notice) {
        boolean success = noticeService.createNotice(notice);
        if (success) {
            return ResponseEntity.ok(ApiResponse.success(notice));
        }
        return ResponseEntity.ok(ApiResponse.fail(500, "创建通知失败"));
    }
    
    /**
     * 发布系统通知
     */
    @PostMapping("/publish")
    public ResponseEntity<ApiResponse<SystemNotice>> publishNotice(@RequestBody SystemNotice notice) {
        if (notice.getTitle() == null || notice.getTitle().trim().isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail(400, "通知标题不能为空"));
        }
        if (notice.getContent() == null || notice.getContent().trim().isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail(400, "通知内容不能为空"));
        }
        
        // 设置默认值
        if (notice.getType() == null) {
            notice.setType(1); // 默认系统公告
        }
        if (notice.getTargetType() == null) {
            notice.setTargetType(0); // 默认全部用户
        }
        
        boolean success = noticeService.publishNotice(notice);
        if (success) {
            return ResponseEntity.ok(ApiResponse.success(notice));
        }
        return ResponseEntity.ok(ApiResponse.fail(500, "发布通知失败"));
    }
    
    /**
     * 获取已发布的通知列表
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<SystemNotice>>> getPublishedNotices(
            @RequestParam(required = false) Long targetUserId,
            @RequestParam(defaultValue = "20") Integer limit) {
        List<SystemNotice> notices = noticeService.getPublishedNotices(targetUserId, limit);
        return ResponseEntity.ok(ApiResponse.success(notices));
    }
}
