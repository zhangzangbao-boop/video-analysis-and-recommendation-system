package com.video.server.mapper;

import com.video.server.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 操作日志 Mapper 接口
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
    
    /**
     * 根据条件查询日志列表
     * @param keyword 关键词
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 日志列表
     */
    List<OperationLog> selectByCondition(
        @Param("keyword") String keyword,
        @Param("startDate") String startDate,
        @Param("endDate") String endDate,
        @Param("offset") Integer offset,
        @Param("limit") Integer limit
    );
    
    /**
     * 统计符合条件的日志数量
     * @param keyword 关键词
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 日志数量
     */
    Long countByCondition(
        @Param("keyword") String keyword,
        @Param("startDate") String startDate,
        @Param("endDate") String endDate
    );
    
    /**
     * 插入日志
     * @param log 日志对象
     * @return 插入行数
     */
    int insert(OperationLog log);
}
