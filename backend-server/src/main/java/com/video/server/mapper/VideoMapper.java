package com.video.server.mapper;

import com.video.server.entity.Video;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 视频 Mapper 接口
 */
@Mapper
public interface VideoMapper extends BaseMapper<Video> {

    List<Video> selectByStatusOrderByPlayCountDesc(@Param("status") String status, @Param("limit") Integer limit);

    List<Video> selectByCondition(@Param("keyword") String keyword,
                                  @Param("status") String status,
                                  @Param("offset") Integer offset,
                                  @Param("limit") Integer limit);

    Long countByCondition(@Param("keyword") String keyword, @Param("status") String status);

    List<Video> selectByUserId(@Param("userId") Long userId,
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

    List<Video> selectByIds(@Param("videoIds") List<Long> videoIds);

    List<Video> selectByConditionForOffline(@Param("startTime") java.time.LocalDateTime startTime,
                                            @Param("endTime") java.time.LocalDateTime endTime,
                                            @Param("lastId") Long lastId,
                                            @Param("limit") Integer limit);

    Long countByConditionForOffline(@Param("startTime") java.time.LocalDateTime startTime,
                                    @Param("endTime") java.time.LocalDateTime endTime);
}