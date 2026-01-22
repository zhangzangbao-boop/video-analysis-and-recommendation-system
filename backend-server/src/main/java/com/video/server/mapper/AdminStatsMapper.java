package com.video.server.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 管理后台统计Mapper
 */
@Mapper
public interface AdminStatsMapper {
    
    /**
     * 统计指定时间范围内的总播放量
     */
    Long getTotalViews(@Param("startTime") LocalDateTime startTime, 
                      @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计指定时间范围内的日活跃用户数（去重）
     */
    Long getDailyActiveUsers(@Param("startTime") LocalDateTime startTime, 
                             @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计指定时间范围内的新增创作者数（新注册用户）
     */
    Long getNewCreators(@Param("startTime") LocalDateTime startTime, 
                        @Param("endTime") LocalDateTime endTime);
    
    /**
     * 按日期统计播放量（用于图表）
     */
    List<Map<String, Object>> getViewsByDate(@Param("startTime") LocalDateTime startTime, 
                                            @Param("endTime") LocalDateTime endTime,
                                            @Param("groupBy") String groupBy);
    
    /**
     * 按日期统计新增用户数（用于图表）
     */
    List<Map<String, Object>> getNewUsersByDate(@Param("startTime") LocalDateTime startTime, 
                                                 @Param("endTime") LocalDateTime endTime,
                                                 @Param("groupBy") String groupBy);
    
    /**
     * 统计热门标签（前10）
     */
    List<Map<String, Object>> getHotTags(@Param("startTime") LocalDateTime startTime, 
                                         @Param("endTime") LocalDateTime endTime,
                                         @Param("limit") Integer limit);
    
    /**
     * 获取优秀创作者列表（按视频数、播放量、点赞量综合评分）
     */
    List<Map<String, Object>> getTopCreators(@Param("startTime") LocalDateTime startTime, 
                                             @Param("endTime") LocalDateTime endTime,
                                             @Param("limit") Integer limit);
}
