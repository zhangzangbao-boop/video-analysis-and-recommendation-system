package com.video.server.controller.admin;

import com.video.server.dto.ApiResponse;
import com.video.server.dto.OperationLogListRequest;
import com.video.server.dto.PageResponse;
import com.video.server.entity.OperationLog;
import com.video.server.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 操作日志控制器（管理后台）
 */
@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
public class AdminLogsController {
    
    private final OperationLogService operationLogService;
    
    /**
     * 获取日志列表
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<PageResponse<OperationLog>>> getLogList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        
        OperationLogListRequest request = new OperationLogListRequest();
        request.setKeyword(keyword);
        request.setStartDate(startDate != null ? startDate.toString() : null);
        request.setEndDate(endDate != null ? endDate.toString() : null);
        request.setPage(page);
        request.setPageSize(pageSize);
        
        PageResponse<OperationLog> response = operationLogService.getLogList(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
