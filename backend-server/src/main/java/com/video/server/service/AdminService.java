package com.video.server.service;

import com.video.server.dto.AdminCreateRequest;
import com.video.server.dto.AdminListRequest;
import com.video.server.dto.PageResponse;
import com.video.server.entity.Admin;

/**
 * 管理员服务接口
 */
public interface AdminService {
    
    /**
     * 获取管理员列表（分页）
     * @param request 查询请求
     * @return 分页结果
     */
    PageResponse<Admin> getAdminList(AdminListRequest request);
    
    /**
     * 根据ID获取管理员详情
     * @param adminId 管理员ID
     * @return 管理员信息
     */
    Admin getAdminById(Long adminId);
    
    /**
     * 创建管理员
     * @param request 创建请求
     * @return 管理员信息
     */
    Admin createAdmin(AdminCreateRequest request);
    
    /**
     * 更新管理员
     * @param adminId 管理员ID
     * @param request 更新请求
     */
    void updateAdmin(Long adminId, AdminCreateRequest request);
    
    /**
     * 删除管理员
     * @param adminId 管理员ID
     */
    void deleteAdmin(Long adminId);
    
    /**
     * 更新管理员状态
     * @param adminId 管理员ID
     * @param status 状态（1-正常，0-禁用）
     */
    void updateAdminStatus(Long adminId, Integer status);
    
    /**
     * 重置管理员密码
     * @param adminId 管理员ID
     */
    void resetPassword(Long adminId);
}
