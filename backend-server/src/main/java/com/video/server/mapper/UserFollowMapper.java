package com.video.server.mapper;

import com.video.server.entity.UserFollow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
}
