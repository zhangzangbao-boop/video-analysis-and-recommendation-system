package com.video.server.mapper;

import com.video.server.dto.VideoDTO; // 引入 DTO
import com.video.server.entity.VideoInteraction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface VideoInteractionMapper {

    List<VideoInteraction> selectByCondition(@Param("startTime") LocalDateTime startTime,
                                             @Param("endTime") LocalDateTime endTime,
                                             @Param("lastId") Long lastId,
                                             @Param("limit") Integer limit);

    Long countByCondition(@Param("startTime") LocalDateTime startTime,
                          @Param("endTime") LocalDateTime endTime);

    int insert(VideoInteraction interaction);

    int delete(@Param("userId") Long userId, @Param("videoId") Long videoId, @Param("type") Integer type);

    VideoInteraction selectByUserIdAndVideoIdAndType(@Param("userId") Long userId,
                                                     @Param("videoId") Long videoId,
                                                     @Param("type") Integer type);

    /**
     * 查询用户的点赞记录 (Interaction实体)
     */
    List<VideoInteraction> selectLikesByUserId(@Param("userId") Long userId, @Param("limit") Integer limit);

    /**
     * 查询收藏视频列表 (修改返回类型为 VideoDTO)
     */
    List<VideoDTO> selectCollectedVideos(@Param("userId") Long userId, @Param("limit") Integer limit);

    /**
     * 查询点赞视频列表 (修改返回类型为 VideoDTO)
     */
    List<VideoDTO> selectLikedVideos(@Param("userId") Long userId, @Param("limit") Integer limit);
}
