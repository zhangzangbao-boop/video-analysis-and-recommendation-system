package com.video.server.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 基础 Mapper 接口
 * @param <T> 实体类型
 */
public interface BaseMapper<T> {
    
    /**
     * 根据 ID 查询
     */
    T selectById(@Param("id") Long id);
    
    /**
     * 插入记录
     */
    int insert(T entity);
    
    /**
     * 根据 ID 更新
     */
    int updateById(T entity);
    
    /**
     * 根据 ID 删除
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 查询所有
     */
    List<T> selectAll();
}
