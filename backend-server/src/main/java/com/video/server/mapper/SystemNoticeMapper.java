package com.video.server.mapper;

import com.video.server.entity.SystemNotice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统通知 Mapper 接口
 */
@Mapper
public interface SystemNoticeMapper extends BaseMapper<SystemNotice> {
    
    /**
     * 查询已发布的通知列表
     * @param targetType 目标类型（可选）
     * @param targetUserId 目标用户ID（可选）
     * @param limit 限制数量
     * @return 通知列表
     */
    List<SystemNotice> selectPublishedNotices(
            @Param("targetType") Integer targetType,
            @Param("targetUserId") Long targetUserId,
            @Param("limit") Integer limit
    );
}
