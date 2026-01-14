package com.video.server.mapper;

import com.video.server.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户 Mapper 接口
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    
    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户信息
     */
    User selectByUsername(@Param("username") String username);
    
    /**
     * 根据条件查询用户列表
     * @param keyword 关键词（用户名/手机号/ID）
     * @param status 状态字符串（normal/frozen/muted）
     * @param level 用户等级
     * @return 用户列表
     */
    List<User> selectByCondition(@Param("keyword") String keyword, @Param("status") String status, @Param("level") String level);
    
    /**
     * 更新用户状态
     * @param userId 用户ID
     * @param status 状态
     * @return 更新行数
     */
    int updateStatusById(@Param("userId") Long userId, @Param("status") Integer status);
    
    /**
     * 重置密码
     * @param userId 用户ID
     * @param password 新密码（加密后）
     * @param salt 盐值
     * @return 更新行数
     */
    int updatePasswordById(@Param("userId") Long userId, @Param("password") String password, @Param("salt") String salt);
}
