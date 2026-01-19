package com.video.server.mapper;

import com.video.server.entity.VideoComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 视频评论 Mapper 接口
 */
@Mapper
public interface VideoCommentMapper {
    
    /**
     * 插入评论
     * @param comment 评论
     * @return 影响行数
     */
    int insert(VideoComment comment);
    
    /**
     * 根据ID查询评论
     * @param id 评论ID
     * @return 评论
     */
    VideoComment selectById(@Param("id") Long id);
    
    /**
     * 查询视频的评论列表（顶级评论）
     * @param videoId 视频ID
     * @param limit 限制数量
     * @return 评论列表
     */
    List<VideoComment> selectByVideoId(@Param("videoId") Long videoId, @Param("limit") Integer limit);
    
    /**
     * 查询评论的回复列表
     * @param parentId 父评论ID
     * @param limit 限制数量
     * @return 回复列表
     */
    List<VideoComment> selectByParentId(@Param("parentId") Long parentId, @Param("limit") Integer limit);
    
    /**
     * 查询用户的评论列表
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 评论列表
     */
    List<VideoComment> selectByUserId(@Param("userId") Long userId, @Param("limit") Integer limit);
    
    /**
     * 删除评论（逻辑删除）
     * @param id 评论ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 更新评论点赞数
     * @param id 评论ID
     * @param delta 变化量（+1或-1）
     * @return 影响行数
     */
    int updateLikeCount(@Param("id") Long id, @Param("delta") Integer delta);
    
    /**
     * 统计视频的评论数
     * @param videoId 视频ID
     * @return 评论数
     */
    Long countByVideoId(@Param("videoId") Long videoId);
}
