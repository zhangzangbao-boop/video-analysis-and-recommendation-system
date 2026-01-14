package com.video.server.service.impl;

import com.video.server.dto.AdminCreateRequest;
import com.video.server.dto.AdminListRequest;
import com.video.server.dto.PageResponse;
import com.video.server.entity.Admin;
import com.video.server.exception.BusinessException;
import com.video.server.mapper.AdminMapper;
import com.video.server.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 管理员服务实现类
 */
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {
    
    private final AdminMapper adminMapper;
    
    // 默认重置密码
    private static final String DEFAULT_PASSWORD = "123456";
    
    @Override
    public PageResponse<Admin> getAdminList(AdminListRequest request) {
        // 计算偏移量
        int offset = (request.getPage() - 1) * request.getPageSize();
        
        // 查询列表（简化版，实际应该支持分页查询）
        List<Admin> list = adminMapper.selectByCondition(
            request.getKeyword(),
            request.getStatus()
        );
        
        // 手动分页（实际应该在SQL中分页）
        int start = offset;
        int end = Math.min(start + request.getPageSize(), list.size());
        List<Admin> pagedList = list.subList(Math.min(start, list.size()), end);
        
        // 统计总数
        long total = list.size();
        
        return new PageResponse<>(pagedList, total, request.getPage(), request.getPageSize());
    }
    
    @Override
    public Admin getAdminById(Long adminId) {
        return adminMapper.selectById(adminId);
    }
    
    @Override
    public Admin createAdmin(AdminCreateRequest request) {
        // 检查用户名是否已存在
        Admin existing = adminMapper.selectByUsername(request.getUsername());
        if (existing != null) {
            throw new BusinessException(400, "用户名已存在");
        }
        
        // 创建管理员
        Admin admin = new Admin();
        admin.setUsername(request.getUsername());
        admin.setRealName(request.getRealName());
        admin.setPhone(request.getPhone());
        admin.setEmail(request.getEmail());
        admin.setRoleId(request.getRoleId());
        admin.setStatus(1); // 默认正常状态
        admin.setIsDeleted(0);
        admin.setCreateTime(LocalDateTime.now());
        admin.setUpdateTime(LocalDateTime.now());
        
        // 加密密码
        String salt = UUID.randomUUID().toString().replace("-", "");
        String encryptedPassword = encryptPassword(request.getPassword(), salt);
        admin.setPassword(encryptedPassword);
        admin.setSalt(salt);
        
        adminMapper.insert(admin);
        return admin;
    }
    
    @Override
    public void updateAdmin(Long adminId, AdminCreateRequest request) {
        Admin admin = adminMapper.selectById(adminId);
        if (admin == null) {
            throw new BusinessException(404, "管理员不存在");
        }
        
        // 检查用户名是否被其他管理员使用
        Admin existing = adminMapper.selectByUsername(request.getUsername());
        if (existing != null && !existing.getId().equals(adminId)) {
            throw new BusinessException(400, "用户名已被使用");
        }
        
        // 更新信息
        admin.setUsername(request.getUsername());
        admin.setRealName(request.getRealName());
        admin.setPhone(request.getPhone());
        admin.setEmail(request.getEmail());
        admin.setRoleId(request.getRoleId());
        admin.setUpdateTime(LocalDateTime.now());
        
        // 如果提供了新密码，则更新密码
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            String salt = UUID.randomUUID().toString().replace("-", "");
            String encryptedPassword = encryptPassword(request.getPassword(), salt);
            admin.setPassword(encryptedPassword);
            admin.setSalt(salt);
        }
        
        adminMapper.updateById(admin);
    }
    
    @Override
    public void deleteAdmin(Long adminId) {
        adminMapper.deleteById(adminId);
    }
    
    @Override
    public void updateAdminStatus(Long adminId, Integer status) {
        adminMapper.updateStatusById(adminId, status);
    }
    
    @Override
    public void resetPassword(Long adminId) {
        // 生成新盐值
        String salt = UUID.randomUUID().toString().replace("-", "");
        // 加密密码
        String encryptedPassword = encryptPassword(DEFAULT_PASSWORD, salt);
        // 更新密码
        adminMapper.updatePasswordById(adminId, encryptedPassword, salt);
    }
    
    /**
     * 加密密码
     */
    private String encryptPassword(String password, String salt) {
        String saltedPassword = password + salt;
        return DigestUtils.md5DigestAsHex(saltedPassword.getBytes(StandardCharsets.UTF_8));
    }
}
