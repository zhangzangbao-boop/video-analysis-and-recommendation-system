package com.video.server.service;

import com.video.server.dto.AdminStatsDTO;

/**
 * 管理后台统计服务接口
 */
public interface AdminStatsService {
    
    /**
     * 获取统计数据
     * @param dateRange 时间范围：week-近7天，month-近30天，year-全年
     * @return 统计数据
     */
    AdminStatsDTO getStats(String dateRange);
}
