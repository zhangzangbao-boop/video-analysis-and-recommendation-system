package com.video.server.service.impl;

import com.video.server.dto.PageResponse;
import com.video.server.dto.UserCreateRequest;
import com.video.server.dto.UserListRequest;
import com.video.server.entity.User;
import com.video.server.exception.BusinessException;
import com.video.server.mapper.UserMapper;
import com.video.server.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
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
        try {
            // 计算偏移量
            int offset = (request.getPage() - 1) * request.getPageSize();
            
            // 查询列表
            List<User> list = userMapper.selectByCondition(
                request.getKeyword(),
                request.getStatus(),
                request.getLevel()
            );
            
            // 防止空指针
            if (list == null) {
                list = new java.util.ArrayList<>();
            }
            
            // 手动分页（实际应该在SQL中分页）
            long total = list.size();
            int start = Math.min(offset, list.size());
            int end = Math.min(start + request.getPageSize(), list.size());
            
            List<User> pagedList;
            if (start >= end || list.isEmpty()) {
                pagedList = new java.util.ArrayList<>();
            } else {
                pagedList = list.subList(start, end);
            }
            
            return new PageResponse<>(pagedList, total, request.getPage(), request.getPageSize());
        } catch (Exception e) {
            // 记录详细错误信息
            System.err.println("获取用户列表失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("获取用户列表失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public User getUserById(Long userId) {
        return userMapper.selectById(userId);
    }
    
    @Override
    public void updateUserStatus(Long userId, Integer status) {
        userMapper.updateStatusById(userId, status);
        // 同时更新status_str字段
        String statusStr = convertStatusToStr(status);
        userMapper.updateStatusStrById(userId, statusStr);
    }
    
    @Override
    public void updateUserStatusByStr(Long userId, String statusStr) {
        Integer status = convertStrToStatus(statusStr);
        updateUserStatus(userId, status);
    }
    
    /**
     * 将状态整数转换为字符串
     */
    private String convertStatusToStr(Integer status) {
        if (status == null) return "normal";
        switch (status) {
            case 0: return "frozen";
            case 2: return "muted";
            case 1:
            default: return "normal";
        }
    }
    
    /**
     * 将状态字符串转换为整数
     */
    private Integer convertStrToStatus(String statusStr) {
        if (statusStr == null) return 1;
        switch (statusStr.toLowerCase()) {
            case "frozen": return 0;
            case "muted": return 2;
            case "normal":
            default: return 1;
        }
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
    
    @Override
    public User createUser(UserCreateRequest request) {
        // 检查用户名是否已存在
        User existing = userMapper.selectByUsername(request.getUsername());
        if (existing != null) {
            throw new BusinessException(400, "用户名已存在");
        }
        
        // 创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPhone(request.getPhone());
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setEmail(request.getEmail());
        user.setStatus(1); // 默认正常状态
        user.setStatusStr("normal");
        user.setIsDeleted(0);
        user.setLevel(1); // 默认等级1
        user.setBalance(new java.math.BigDecimal("0.00"));
        user.setPoints(0);
        user.setFansCount(0);
        user.setFollowCount(0);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        
        // 加密密码
        String salt = UUID.randomUUID().toString().replace("-", "");
        String encryptedPassword = encryptPassword(request.getPassword(), salt);
        user.setPassword(encryptedPassword);
        user.setSalt(salt);
        
        userMapper.insert(user);
        return user;
    }
    
    /**
     * 加密密码
     */
    private String encryptPassword(String password, String salt) {
        String saltedPassword = password + salt;
        return DigestUtils.md5DigestAsHex(saltedPassword.getBytes(StandardCharsets.UTF_8));
    }
}
