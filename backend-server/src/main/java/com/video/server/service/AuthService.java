package com.video.server.service;

import com.video.server.dto.LoginRequest;
import com.video.server.dto.LoginResponse;

/**
 * 认证服务接口
 */
public interface AuthService {
    
    /**
     * 用户登录
     * @param request 登录请求
     * @return 登录响应（包含token）
     */
    LoginResponse login(LoginRequest request);
}
