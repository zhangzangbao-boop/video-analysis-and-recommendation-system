package com.video.server.service;

import com.video.server.dto.PageResponse;
import com.video.server.dto.UserCreateRequest;
import com.video.server.dto.UserListRequest;
import com.video.server.entity.User;
import com.video.server.dto.UserActivityDTO;
import java.util.List;

public interface UserService {

    /**
     * 获取用户列表
     */
    PageResponse<User> getUserList(UserListRequest request);

    /**
     * 获取用户详情
     */
    User getUserById(Long userId);

    /**
     * 根据用户名查询
     */
    User getUserByUsername(String username);

    /**
     * 更新用户状态
     * @param userId 用户ID
     * @param status 状态字符串 (normal, frozen, muted)
     */
    void updateUserStatus(Long userId, String status);

    /**
     * 重置密码
     */
    void resetPassword(Long userId);

    /**
     * 创建用户
     * (修改为 void 返回类型，匹配实现类)
     */
    void createUser(UserCreateRequest request);

    /**
     * 获取用户动态画像
     */
    List<UserActivityDTO> getUserActivities(Long userId);
}