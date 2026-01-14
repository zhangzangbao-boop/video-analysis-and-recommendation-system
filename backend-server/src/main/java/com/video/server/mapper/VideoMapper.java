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
    
    /**
     * 根据状态查询视频列表，按播放量降序
     * @param status 审核状态
     * @param limit 限制数量
     * @return 视频列表
     */
    List<Video> selectByStatusOrderByPlayCountDesc(@Param("status") String status, @Param("limit") Integer limit);
    
    /**
     * 根据条件查询视频列表（分页）
     * @param keyword 关键词
     * @param status 状态
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 视频列表
     */
    List<Video> selectByCondition(@Param("keyword") String keyword, 
                                    @Param("status") String status,
                                    @Param("offset") Integer offset,
                                    @Param("limit") Integer limit);
    
    /**
     * 统计符合条件的视频数量
     * @param keyword 关键词
     * @param status 状态
     * @return 总数
     */
    Long countByCondition(@Param("keyword") String keyword, @Param("status") String status);
    
    /**
     * 更新视频点赞数
     * @param videoId 视频ID
     * @return 更新行数
     */
    int incrementLikeCount(@Param("videoId") Long videoId);
    
    /**
     * 根据ID更新状态
     * @param videoId 视频ID
     * @param status 状态
     * @return 更新行数
     */
    int updateStatusById(@Param("videoId") Long videoId, @Param("status") String status);
    
    /**
     * 逻辑删除视频
     * @param videoId 视频ID
     * @return 更新行数
     */
    int deleteById(@Param("videoId") Long videoId);
    
    /**
     * 设置/取消热门
     * @param videoId 视频ID
     * @param isHot 是否热门（1-是，0-否）
     * @return 更新行数
     */
    int updateHotStatus(@Param("videoId") Long videoId, @Param("isHot") Integer isHot);
}
