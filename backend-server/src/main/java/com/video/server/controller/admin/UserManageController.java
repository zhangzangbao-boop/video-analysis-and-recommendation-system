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
@CrossOrigin(origins = "*", maxAge = 3600)
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
     * @param id 用户ID
     * @param status 状态（1-正常，0-冻结，2-禁言）或状态字符串（normal/frozen/muted）
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateUserStatus(
            @PathVariable Long id,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String statusStr) {
        if (statusStr != null && !statusStr.isEmpty()) {
            // 如果传的是状态字符串，使用字符串更新方法
            userService.updateUserStatusByStr(id, statusStr);
        } else if (status != null) {
            // 如果传的是整数，使用整数更新方法
            userService.updateUserStatus(id, status);
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.fail(400, "状态参数不能为空"));
        }
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
    
    /**
     * 创建用户
     */
    @PostMapping
    public ResponseEntity<ApiResponse<User>> createUser(@RequestBody com.video.server.dto.UserCreateRequest request) {
        User user = userService.createUser(request);
        return ResponseEntity.ok(ApiResponse.success(user));
    }
}
