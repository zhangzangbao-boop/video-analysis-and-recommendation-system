package com.video.server.service.impl;

import com.video.server.dto.LoginRequest;
import com.video.server.dto.LoginResponse;
import com.video.server.entity.Admin;
import com.video.server.entity.User;
import com.video.server.exception.BusinessException;
import com.video.server.mapper.AdminMapper;
import com.video.server.mapper.UserMapper;
import com.video.server.service.AuthService;
import com.video.server.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

/**
 * 认证服务实现类
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    
    private final UserMapper userMapper;
    private final AdminMapper adminMapper;
    private final JwtUtil jwtUtil;
    
    // 默认密码（用于测试，实际应该加密存储）
    private static final String DEFAULT_PASSWORD = "123456";
    // Token过期时间：7天
    private static final long TOKEN_EXPIRE_TIME = 7 * 24 * 60 * 60 * 1000L;
    
    @Override
    public LoginResponse login(LoginRequest request) {
        // 先尝试查询管理员
        Admin admin = adminMapper.selectByUsername(request.getUsername());
        if (admin != null) {
            // 检查管理员状态
            if (admin.getStatus() != null && admin.getStatus() == 0) {
                throw new BusinessException(403, "账号已被禁用");
            }
            
            // 验证密码
            if (!verifyPassword(request.getPassword(), admin.getPassword(), admin.getSalt())) {
                throw new BusinessException(401, "用户名或密码错误");
            }
            
            // 生成Token
            String token = jwtUtil.generateToken(admin.getId(), TOKEN_EXPIRE_TIME);
            return new LoginResponse(token, admin.getId(), admin.getUsername(), "admin");
        }
        
        // 如果不是管理员，查询普通用户
        User user = userMapper.selectByUsername(request.getUsername());
        if (user == null) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        
        // 检查用户状态
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(403, "账号已被冻结");
        }
        
        // 验证密码
        if (!verifyPassword(request.getPassword(), user.getPassword(), user.getSalt())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        
        // 生成Token
        String token = jwtUtil.generateToken(user.getId(), TOKEN_EXPIRE_TIME);
        return new LoginResponse(token, user.getId(), user.getUsername(), "user");
    }
    
    /**
     * 验证密码
     */
    private boolean verifyPassword(String inputPassword, String storedPassword, String salt) {
        // 如果密码是默认密码的MD5（用于测试）
        if ("e10adc3949ba59abbe56e057f20f883e".equals(storedPassword)) {
            return DEFAULT_PASSWORD.equals(inputPassword);
        }
        
        // 使用盐值加密后比较
        if (salt == null) {
            return false;
        }
        String saltedPassword = inputPassword + salt;
        String encryptedPassword = DigestUtils.md5DigestAsHex(saltedPassword.getBytes(StandardCharsets.UTF_8));
        return encryptedPassword.equals(storedPassword);
    }
}
