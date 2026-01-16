package com.video.server.controller.admin;

import com.video.server.dto.ApiResponse;
import com.video.server.dto.OfflineDataRequest;
import com.video.server.dto.OfflineDataResponse;
import com.video.server.service.OfflineAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 离线分析控制器（管理员接口）
 */
@RestController
@RequestMapping("/api/admin/offline")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
public class OfflineAnalysisController {
    
    private final OfflineAnalysisService offlineAnalysisService;
    
    /**
     * 获取离线分析数据
     * @param request 请求参数
     * @return 数据响应
     */
    @GetMapping("/data")
    public ResponseEntity<ApiResponse<OfflineDataResponse>> getOfflineData(OfflineDataRequest request) {
        OfflineDataResponse response = offlineAnalysisService.getOfflineData(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
