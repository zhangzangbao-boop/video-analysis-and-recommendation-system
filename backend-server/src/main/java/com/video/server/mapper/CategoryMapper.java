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
     * 查询所有分类（按排序字段排序），包含每个分类的视频数量
     */
    @Select("SELECT " +
            "vc.id, " +
            "vc.name, " +
            "vc.code, " +
            "vc.sort, " +
            "COALESCE(COUNT(v.id), 0) AS videoCount " +
            "FROM video_category vc " +
            "LEFT JOIN video_info v ON vc.id = v.category_id AND v.status = 1 AND v.is_deleted = 0 " +
            "GROUP BY vc.id, vc.name, vc.code, vc.sort " +
            "ORDER BY vc.sort ASC, vc.id ASC")
    List<VideoCategory> selectAll();
}
