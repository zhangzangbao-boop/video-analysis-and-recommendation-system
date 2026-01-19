package com.video.server.mapper;

import com.video.server.entity.UserFollow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户关注关系 Mapper 接口
 */
@Mapper
public interface UserFollowMapper extends BaseMapper<UserFollow> {
    
    /**
     * 查询关注关系
     * @param userId 粉丝ID
     * @param followUserId 被关注者ID
     * @return 关注关系
     */
    UserFollow selectByUserIdAndFollowUserId(@Param("userId") Long userId, @Param("followUserId") Long followUserId);
    
    /**
     * 删除关注关系
     * @param userId 粉丝ID
     * @param followUserId 被关注者ID
     * @return 删除行数
     */
    int deleteByUserIdAndFollowUserId(@Param("userId") Long userId, @Param("followUserId") Long followUserId);
    
    /**
     * 查询用户的关注列表（我关注的人）
     * @param userId 用户ID
     * @return 关注列表（包含被关注用户的信息）
     */
    List<com.video.server.entity.User> selectFollowingList(@Param("userId") Long userId);
    
    /**
     * 查询用户的粉丝列表（关注我的人）
     * @param userId 用户ID
     * @return 粉丝列表（包含粉丝用户的信息）
     */
    List<com.video.server.entity.User> selectFansList(@Param("userId") Long userId);
}
