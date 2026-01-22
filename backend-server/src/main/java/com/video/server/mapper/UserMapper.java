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

    // 【修改】确保参数与 Service 调用一致
    List<User> selectByCondition(@Param("keyword") String keyword,
                                 @Param("status") String status,
                                 @Param("level") String level);

    // 【新增】补充缺失的计数方法
    Long countByCondition(@Param("keyword") String keyword,
                          @Param("status") String status,
                          @Param("level") String level);

    /**
     * 更新用户状态
     * @param userId 用户ID
     * @param status 状态
     * @return 更新行数
     */
    int updateStatusById(@Param("userId") Long userId, @Param("status") Integer status);
    
    /**
     * 更新用户状态字符串
     * @param userId 用户ID
     * @param statusStr 状态字符串
     * @return 更新行数
     */
    int updateStatusStrById(@Param("userId") Long userId, @Param("statusStr") String statusStr);
    
    /**
     * 重置密码
     * @param userId 用户ID
     * @param password 新密码（加密后）
     * @param salt 盐值
     * @return 更新行数
     */
    int updatePasswordById(@Param("userId") Long userId, @Param("password") String password, @Param("salt") String salt);
    
    /**
     * 根据条件查询用户列表（分页，用于离线分析）
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param lastId 上次最后一条ID（用于增量提取）
     * @param limit 限制数量
     * @return 用户列表
     */
    List<User> selectByConditionForOffline(@Param("startTime") java.time.LocalDateTime startTime,
                                            @Param("endTime") java.time.LocalDateTime endTime,
                                            @Param("lastId") Long lastId,
                                            @Param("limit") Integer limit);
    
    /**
     * 统计符合条件的用户数量（用于离线分析）
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 总数
     */
    Long countByConditionForOffline(@Param("startTime") java.time.LocalDateTime startTime,
                                     @Param("endTime") java.time.LocalDateTime endTime);
    
    /**
     * 查询所有用户（未删除的）
     * @return 用户列表
     */
    List<User> selectAll();
}
