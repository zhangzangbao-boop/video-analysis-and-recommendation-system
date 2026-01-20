package com.video.server.mapper;

import com.video.server.dto.VideoCommentDTO;
import com.video.server.entity.VideoComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface VideoCommentMapper {

    // --- 基础方法 ---
    int insert(VideoComment comment);

    VideoComment selectById(@Param("id") Long id);

    int deleteById(@Param("id") Long id);

    Long countByVideoId(@Param("videoId") Long videoId);

    // --- DTO查询 (修复楼中楼和头像显示的关键) ---

    /**
     * 查询刚插入的单条详细信息
     */
    VideoCommentDTO selectDTOById(@Param("id") Long id);

    /**
     * 查询视频的一级评论
     */
    List<VideoCommentDTO> selectDTOByVideoId(@Param("videoId") Long videoId, @Param("limit") Integer limit);

    /**
     * 查询评论的子回复
     */
    List<VideoCommentDTO> selectDTOByParentId(@Param("parentId") Long parentId, @Param("limit") Integer limit);

    // --- 业务查询 ---

    List<VideoComment> selectByUserId(@Param("userId") Long userId, @Param("limit") Integer limit);

    /**
     * 更新点赞数
     */
    int updateLikeCount(@Param("id") Long id, @Param("delta") int delta);
}