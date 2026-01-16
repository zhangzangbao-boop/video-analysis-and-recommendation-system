package com.video.server.mapper;

import com.video.server.entity.UserBehavior;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户行为 Mapper 接口
 */
@Mapper
public interface UserBehaviorMapper extends BaseMapper<UserBehavior> {
    
    /**
     * 根据用户ID和行为类型查询
     * @param userId 用户ID
     * @param actionType 行为类型
     * @return 行为记录列表
     */
    List<UserBehavior> selectByUserIdAndActionType(@Param("userId") Long userId, @Param("actionType") String actionType);
}
