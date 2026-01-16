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
    
    /**
     * 根据条件查询用户行为记录（分页，用于离线分析）
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param lastId 上次最后一条ID（用于增量提取）
     * @param limit 限制数量
     * @return 行为记录列表
     */
    List<UserBehavior> selectByCondition(@Param("startTime") java.time.LocalDateTime startTime,
                                          @Param("endTime") java.time.LocalDateTime endTime,
                                          @Param("lastId") Long lastId,
                                          @Param("limit") Integer limit);
    
    /**
     * 统计符合条件的用户行为记录数量
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 总数
     */
    Long countByCondition(@Param("startTime") java.time.LocalDateTime startTime,
                          @Param("endTime") java.time.LocalDateTime endTime);
}
