package com.video.server.service.impl;

import com.video.server.dto.PageResponse;
import com.video.server.dto.UserListRequest;
import com.video.server.entity.User;
import com.video.server.mapper.UserMapper;
import com.video.server.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * 用户服务实现类
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    
    private final UserMapper userMapper;
    
    // 默认重置密码
    private static final String DEFAULT_PASSWORD = "123456";
    
    @Override
    public PageResponse<User> getUserList(UserListRequest request) {
        // 计算偏移量
        int offset = (request.getPage() - 1) * request.getPageSize();
        
        // 查询列表（简化版，实际应该支持分页查询）
        List<User> list = userMapper.selectByCondition(
            request.getKeyword(),
            request.getStatus(),
            request.getLevel()
        );
        
        // 手动分页（实际应该在SQL中分页）
        int start = offset;
        int end = Math.min(start + request.getPageSize(), list.size());
        List<User> pagedList = list.subList(Math.min(start, list.size()), end);
        
        // 统计总数
        long total = list.size();
        
        return new PageResponse<>(pagedList, total, request.getPage(), request.getPageSize());
    }
    
    @Override
    public User getUserById(Long userId) {
        return userMapper.selectById(userId);
    }
    
    @Override
    public void updateUserStatus(Long userId, Integer status) {
        userMapper.updateStatusById(userId, status);
    }
    
    @Override
    public void resetPassword(Long userId) {
        // 生成新盐值
        String salt = UUID.randomUUID().toString().replace("-", "");
        // 加密密码
        String encryptedPassword = encryptPassword(DEFAULT_PASSWORD, salt);
        // 更新密码
        userMapper.updatePasswordById(userId, encryptedPassword, salt);
    }
    
    /**
     * 加密密码
     */
    private String encryptPassword(String password, String salt) {
        String saltedPassword = password + salt;
        return DigestUtils.md5DigestAsHex(saltedPassword.getBytes(StandardCharsets.UTF_8));
    }
}
