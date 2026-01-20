package com.video.server.mapper;

import com.video.server.dto.VideoCommentDTO;
import com.video.server.entity.VideoComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface VideoCommentMapper {

    int insert(VideoComment comment);

    VideoComment selectById(@Param("id") Long id);

    int deleteById(@Param("id") Long id);

    Long countByVideoId(@Param("videoId") Long videoId);

    // --- DTO查询 ---
    VideoCommentDTO selectDTOById(@Param("id") Long id);
    List<VideoCommentDTO> selectDTOByVideoId(@Param("videoId") Long videoId, @Param("limit") Integer limit);
    List<VideoCommentDTO> selectDTOByParentId(@Param("parentId") Long parentId, @Param("limit") Integer limit);

    // --- 【修复】加回基础查询供 User 使用 ---
    List<VideoComment> selectByUserId(@Param("userId") Long userId, @Param("limit") Integer limit);

    int updateLikeCount(@Param("id") Long id, @Param("delta") int delta);
}