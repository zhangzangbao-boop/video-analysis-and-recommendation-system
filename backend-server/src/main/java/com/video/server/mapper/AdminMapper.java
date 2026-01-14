package com.video.server.mapper;

import com.video.server.entity.Admin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 管理员 Mapper 接口
 */
@Mapper
public interface AdminMapper extends BaseMapper<Admin> {
    
    /**
     * 根据用户名查询管理员
     * @param username 用户名
     * @return 管理员信息
     */
    Admin selectByUsername(@Param("username") String username);
    
    /**
     * 根据条件查询管理员列表
     * @param keyword 关键词（用户名/手机号/ID）
     * @param status 状态
     * @return 管理员列表
     */
    List<Admin> selectByCondition(@Param("keyword") String keyword, @Param("status") Integer status);
    
    /**
     * 更新管理员状态
     * @param adminId 管理员ID
     * @param status 状态
     * @return 更新行数
     */
    int updateStatusById(@Param("adminId") Long adminId, @Param("status") Integer status);
    
    /**
     * 重置密码
     * @param adminId 管理员ID
     * @param password 新密码（加密后）
     * @param salt 盐值
     * @return 更新行数
     */
    int updatePasswordById(@Param("adminId") Long adminId, @Param("password") String password, @Param("salt") String salt);
}
