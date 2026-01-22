package com.video.server.mapper;

import com.video.server.dto.VideoDTO;
import com.video.server.entity.Video;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 视频 Mapper 接口
 */
@Mapper
public interface VideoMapper extends BaseMapper<Video> {

    /**
     * 根据ID查询视频（返回VideoDTO，包含作者昵称）
     */
    VideoDTO selectById(@Param("id") Long id);

    List<VideoDTO> selectByStatusOrderByPlayCountDesc(@Param("status") String status, @Param("limit") Integer limit);

    List<VideoDTO> selectByCondition(@Param("keyword") String keyword,
                                  @Param("status") String status,
                                  @Param("offset") Integer offset,
                                  @Param("limit") Integer limit);

    Long countByCondition(@Param("keyword") String keyword, @Param("status") String status);

    List<VideoDTO> selectByUserId(@Param("userId") Long userId,
                               @Param("offset") Integer offset,
                               @Param("limit") Integer limit);

    Long countByUserId(@Param("userId") Long userId);

    int incrementLikeCount(@Param("videoId") Long videoId);

    int updateStatusById(@Param("videoId") Long videoId, @Param("status") String status);

    int updateAuditResult(@Param("videoId") Long videoId,
                          @Param("status") String status,
                          @Param("msg") String msg);

    int deleteById(@Param("videoId") Long videoId);

    int updateHotStatus(@Param("videoId") Long videoId, @Param("isHot") Integer isHot);

    List<VideoDTO> selectByIds(@Param("videoIds") List<Long> videoIds);

    List<Video> selectByConditionForOffline(@Param("startTime") java.time.LocalDateTime startTime,
                                            @Param("endTime") java.time.LocalDateTime endTime,
                                            @Param("lastId") Long lastId,
                                            @Param("limit") Integer limit);

    Long countByConditionForOffline(@Param("startTime") java.time.LocalDateTime startTime,
                                    @Param("endTime") java.time.LocalDateTime endTime);

    /**
     * 【新增】查询用户发布的作品
     */
    List<Video> selectByAuthorId(@Param("authorId") Long authorId, @Param("limit") Integer limit);

    // ==========================================
    // 【核心修复】补全缺失的方法定义
    // ==========================================

    /**
     * 增加播放量
     */
    void incrementPlayCount(@Param("id") Long id);

    /**
     * 搜索视频 (前台搜索用)
     */
    List<VideoDTO> searchVideos(@Param("keyword") String keyword,
                             @Param("categoryId") Integer categoryId,
                             @Param("offset") Integer offset,
                             @Param("limit") Integer limit);
    // 添加接口
    List<Video> selectListByUserId(@Param("userId") Long userId);
    
    /**
     * 获取所有视频的总播放量（仅统计已发布且未删除的视频）
     */
    Long getTotalPlayCount();
}