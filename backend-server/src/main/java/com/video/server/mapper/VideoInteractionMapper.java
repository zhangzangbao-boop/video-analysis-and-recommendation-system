package com.video.server.mapper;

import com.video.server.entity.VideoInteraction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 视频互动记录 Mapper 接口
 */
@Mapper
public interface VideoInteractionMapper {
    
    /**
     * 根据条件查询互动记录（分页）
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param lastId 上次最后一条ID（用于增量提取）
     * @param limit 限制数量
     * @return 互动记录列表
     */
    List<VideoInteraction> selectByCondition(@Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime,
                                              @Param("lastId") Long lastId,
                                              @Param("limit") Integer limit);
    
    /**
     * 统计符合条件的互动记录数量
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 总数
     */
    Long countByCondition(@Param("startTime") LocalDateTime startTime,
                           @Param("endTime") LocalDateTime endTime);
    
    /**
     * 插入互动记录
     * @param interaction 互动记录
     * @return 影响行数
     */
    int insert(VideoInteraction interaction);
    
    /**
     * 删除互动记录
     * @param userId 用户ID
     * @param videoId 视频ID
     * @param type 类型
     * @return 影响行数
     */
    int delete(@Param("userId") Long userId, @Param("videoId") Long videoId, @Param("type") Integer type);
    
    /**
     * 查询用户对某个视频的互动记录
     * @param userId 用户ID
     * @param videoId 视频ID
     * @param type 类型
     * @return 互动记录
     */
    VideoInteraction selectByUserIdAndVideoIdAndType(@Param("userId") Long userId, 
                                                     @Param("videoId") Long videoId, 
                                                     @Param("type") Integer type);
    
    /**
     * 查询用户的点赞记录
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 互动记录列表
     */
    List<VideoInteraction> selectLikesByUserId(@Param("userId") Long userId, @Param("limit") Integer limit);
}
