package com.video.server.controller.admin;

import com.video.server.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;

/**
 * 健康检查控制器
 */
@RestController
@RequestMapping("/api/admin/health")
@CrossOrigin(origins = "*", maxAge = 3600)
public class HealthController {
    
    /**
     * 健康检查
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
