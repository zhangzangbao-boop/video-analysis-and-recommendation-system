package com.video.server.mapper;

import com.video.server.entity.VideoPlayRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 视频播放记录 Mapper 接口
 */
@Mapper
public interface VideoPlayRecordMapper {
    
    /**
     * 根据条件查询播放记录（分页）
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param lastId 上次最后一条ID（用于增量提取）
     * @param limit 限制数量
     * @return 播放记录列表
     */
    List<VideoPlayRecord> selectByCondition(@Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime,
                                              @Param("lastId") Long lastId,
                                              @Param("limit") Integer limit);
    
    /**
     * 统计符合条件的播放记录数量
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 总数
     */
    Long countByCondition(@Param("startTime") LocalDateTime startTime,
                          @Param("endTime") LocalDateTime endTime);
    
    /**
     * 插入播放记录
     * @param record 播放记录
     * @return 影响行数
     */
    int insert(VideoPlayRecord record);
    
    /**
     * 查询用户的播放历史（按时间倒序）
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 播放记录列表
     */
    List<VideoPlayRecord> selectByUserId(@Param("userId") Long userId, @Param("limit") Integer limit);
    
    /**
     * 查询用户的播放历史（每个视频只返回最新的一条记录，按时间倒序）
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 播放记录列表
     */
    List<VideoPlayRecord> selectByUserIdDistinct(@Param("userId") Long userId, @Param("limit") Integer limit);
    
    /**
     * 查询用户对某个视频的播放记录
     * @param userId 用户ID
     * @param videoId 视频ID
     * @return 播放记录
     */
    VideoPlayRecord selectByUserIdAndVideoId(@Param("userId") Long userId, @Param("videoId") Long videoId);

    /**
     * 【新增】删除单条历史
     */
    int deleteByUserIdAndVideoId(@Param("userId") Long userId, @Param("videoId") Long videoId);

    /**
     * 【新增】清空用户历史
     */
    int deleteByUserId(@Param("userId") Long userId);

    /**
     * 更新播放记录
     * @param record 播放记录
     * @return 影响行数
     */
    int updateById(VideoPlayRecord record);
}
