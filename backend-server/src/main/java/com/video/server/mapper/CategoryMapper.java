package com.video.server.mapper;

import com.video.server.entity.VideoCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 视频分类Mapper
 */
@Mapper
public interface CategoryMapper {
    
    /**
     * 查询所有分类（按排序字段排序）
     */
    @Select("SELECT id, name, code, sort FROM video_category ORDER BY sort ASC, id ASC")
    List<VideoCategory> selectAll();
}
