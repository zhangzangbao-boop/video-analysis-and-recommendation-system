package com.video.server.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 推荐结果 Mapper 接口
 */
@Mapper
public interface RecommendationResultMapper {
    
    /**
     * 根据用户ID获取推荐视频ID列表（按优先级排序）
     * 优先级：1. REALTIME > OFFLINE  2. score降序  3. rank升序
     * 
     * @param userId 用户ID
     * @param limit 返回数量限制
     * @param excludeVideoIds 排除的视频ID列表（用于刷新时排除已推送的视频）
     * @return 推荐视频ID列表
     */
    List<Long> selectVideoIdsByUserId(@Param("userId") Long userId, 
                                      @Param("limit") Integer limit,
                                      @Param("excludeVideoIds") List<Long> excludeVideoIds);
    
    /**
     * 获取随机推荐视频ID（当用户没有推荐结果时使用）
     * 
     * @param excludeVideoIds 排除的视频ID列表（用户已看过的）
     * @param limit 返回数量限制
     * @return 随机视频ID列表
     */
    List<Long> selectRandomVideoIds(@Param("excludeVideoIds") List<Long> excludeVideoIds, @Param("limit") Integer limit);
}
