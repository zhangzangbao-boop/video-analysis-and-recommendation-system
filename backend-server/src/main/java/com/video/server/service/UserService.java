package com.video.server.service;

import com.video.server.dto.PageResponse;
import com.video.server.dto.UserListRequest;
import com.video.server.entity.User;

/**
 * 用户服务接口
 */
public interface UserService {
    
    /**
     * 获取用户列表（分页）
     * @param request 查询请求
     * @return 分页结果
     */
    PageResponse<User> getUserList(UserListRequest request);
    
    /**
     * 根据ID获取用户详情
     * @param userId 用户ID
     * @return 用户信息
     */
    User getUserById(Long userId);
    
    /**
     * 更新用户状态
     * @param userId 用户ID
     * @param status 状态（1-正常，0-冻结）
     */
    void updateUserStatus(Long userId, Integer status);
    
    /**
     * 重置用户密码
     * @param userId 用户ID
     */
    void resetPassword(Long userId);
}
