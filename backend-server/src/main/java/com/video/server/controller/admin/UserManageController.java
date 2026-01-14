package com.video.server.controller.admin;

import com.video.server.dto.ApiResponse;
import com.video.server.dto.PageResponse;
import com.video.server.dto.UserListRequest;
import com.video.server.entity.User;
import com.video.server.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理控制器（管理后台）
 */
@RestController
@RequestMapping("/api/admin/user")
@RequiredArgsConstructor
public class UserManageController {
    
    private final UserService userService;
    
    /**
     * 获取用户列表
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<PageResponse<User>>> getUserList(UserListRequest request) {
        PageResponse<User> response = userService.getUserList(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 获取用户详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<User>> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }
    
    /**
     * 更新用户状态
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateUserStatus(
            @PathVariable Long id,
            @RequestParam Integer status) {
        userService.updateUserStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success());
    }
    
    /**
     * 重置用户密码
     */
    @PutMapping("/{id}/password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@PathVariable Long id) {
        userService.resetPassword(id);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
