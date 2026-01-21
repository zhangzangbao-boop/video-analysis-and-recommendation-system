package com.video.server.controller.admin;

import com.video.server.dto.ApiResponse;
import com.video.server.dto.PageResponse;
import com.video.server.dto.UserCreateRequest;
import com.video.server.dto.UserListRequest;
import com.video.server.dto.UserActivityDTO;
import com.video.server.entity.User;
import com.video.server.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/user") // 【修改】去掉 /v1，与前端 admin.js 保持一致
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
public class UserManageController {

    private final UserService userService;

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<PageResponse<User>>> getUserList(UserListRequest request) {
        PageResponse<User> response = userService.getUserList(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<User>> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateUserStatus(@PathVariable Long id,
                                                              @RequestParam String status) {
        userService.updateUserStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/reset-password/{id}")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@PathVariable Long id) {
        userService.resetPassword(id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Void>> createUser(@RequestBody UserCreateRequest request) {
        userService.createUser(request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/{id}/activities")
    public ResponseEntity<ApiResponse<List<UserActivityDTO>>> getUserActivities(@PathVariable Long id) {
        List<UserActivityDTO> activities = userService.getUserActivities(id);
        return ResponseEntity.ok(ApiResponse.success(activities));
    }
}