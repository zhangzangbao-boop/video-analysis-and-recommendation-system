package com.video.server.controller;

import com.video.server.dto.UserBehaviorRequest;
import com.video.server.service.UserBehaviorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户行为控制器
 */
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserBehaviorController {
    
    private final UserBehaviorService userBehaviorService;
    
    /**
     * 记录用户行为
     * @param request 用户行为请求
     * @return 响应结果
     */
    @PostMapping("/behavior")
    public ResponseEntity<Void> recordBehavior(@RequestBody UserBehaviorRequest request) {
        userBehaviorService.recordBehavior(request);
        return ResponseEntity.ok().build();
    }
}
