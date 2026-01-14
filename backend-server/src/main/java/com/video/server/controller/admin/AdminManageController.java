package com.video.server.controller.admin;

import com.video.server.dto.AdminCreateRequest;
import com.video.server.dto.AdminListRequest;
import com.video.server.dto.ApiResponse;
import com.video.server.dto.PageResponse;
import com.video.server.entity.Admin;
import com.video.server.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员管理控制器
 */
@RestController
@RequestMapping("/api/admin/admin")
@RequiredArgsConstructor
public class AdminManageController {
    
    private final AdminService adminService;
    
    /**
     * 获取管理员列表
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<PageResponse<Admin>>> getAdminList(AdminListRequest request) {
        PageResponse<Admin> response = adminService.getAdminList(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 获取管理员详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Admin>> getAdminById(@PathVariable Long id) {
        Admin admin = adminService.getAdminById(id);
        return ResponseEntity.ok(ApiResponse.success(admin));
    }
    
    /**
     * 创建管理员
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Admin>> createAdmin(@RequestBody AdminCreateRequest request) {
        Admin admin = adminService.createAdmin(request);
        return ResponseEntity.ok(ApiResponse.success(admin));
    }
    
    /**
     * 更新管理员
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updateAdmin(
            @PathVariable Long id,
            @RequestBody AdminCreateRequest request) {
        adminService.updateAdmin(id, request);
        return ResponseEntity.ok(ApiResponse.success());
    }
    
    /**
     * 删除管理员
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAdmin(@PathVariable Long id) {
        adminService.deleteAdmin(id);
        return ResponseEntity.ok(ApiResponse.success());
    }
    
    /**
     * 更新管理员状态
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateAdminStatus(
            @PathVariable Long id,
            @RequestParam Integer status) {
        adminService.updateAdminStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success());
    }
    
    /**
     * 重置管理员密码
     */
    @PutMapping("/{id}/password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@PathVariable Long id) {
        adminService.resetPassword(id);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
