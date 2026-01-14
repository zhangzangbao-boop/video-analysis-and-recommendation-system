package com.video.server.controller.admin;

import com.video.server.dto.AdminStatsDTO;
import com.video.server.dto.ApiResponse;
import com.video.server.service.AdminStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 管理后台统计控制器
 */
@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {
    
    private final AdminStatsService adminStatsService;
    
    /**
     * 获取统计数据
     * @param dateRange 时间范围：week-近7天，month-近30天，year-全年
     * @return 统计数据
     */
    @GetMapping
    public ResponseEntity<ApiResponse<AdminStatsDTO>> getStats(
            @RequestParam(defaultValue = "week") String dateRange) {
        AdminStatsDTO stats = adminStatsService.getStats(dateRange);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
