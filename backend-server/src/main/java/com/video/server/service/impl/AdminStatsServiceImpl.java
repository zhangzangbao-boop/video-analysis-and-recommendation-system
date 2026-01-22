package com.video.server.service.impl;

import com.video.server.dto.AdminStatsDTO;
import com.video.server.mapper.AdminStatsMapper;
import com.video.server.service.AdminStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理后台统计服务实现类
 */
@Service
@RequiredArgsConstructor
public class AdminStatsServiceImpl implements AdminStatsService {
    
    private final AdminStatsMapper adminStatsMapper;
    
    @Override
    public AdminStatsDTO getStats(String dateRange) {
        AdminStatsDTO stats = new AdminStatsDTO();
        
        // 计算时间范围
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime;
        LocalDateTime lastPeriodStartTime;
        LocalDateTime lastPeriodEndTime;
        String groupBy;
        
        switch (dateRange) {
            case "week":
                startTime = endTime.minusDays(7);
                lastPeriodStartTime = startTime.minusDays(7);
                lastPeriodEndTime = startTime;
                groupBy = "day";
                break;
            case "month":
                startTime = endTime.minusDays(30);
                lastPeriodStartTime = startTime.minusDays(30);
                lastPeriodEndTime = startTime;
                groupBy = "day";
                break;
            case "year":
                startTime = endTime.minusYears(1);
                lastPeriodStartTime = startTime.minusYears(1);
                lastPeriodEndTime = startTime;
                groupBy = "month";
                break;
            default:
                startTime = endTime.minusDays(7);
                lastPeriodStartTime = startTime.minusDays(7);
                lastPeriodEndTime = startTime;
                groupBy = "day";
        }
        
        // 1. 获取当前时间范围的统计数据
        Long totalViews = adminStatsMapper.getTotalViews(startTime, endTime);
        Long dailyActiveUsers = adminStatsMapper.getDailyActiveUsers(startTime, endTime);
        Long newCreators = adminStatsMapper.getNewCreators(startTime, endTime);
        
        // 2. 获取上一时间范围的统计数据（用于计算同比增长率）
        Long lastTotalViews = adminStatsMapper.getTotalViews(lastPeriodStartTime, lastPeriodEndTime);
        Long lastDailyActiveUsers = adminStatsMapper.getDailyActiveUsers(lastPeriodStartTime, lastPeriodEndTime);
        Long lastNewCreators = adminStatsMapper.getNewCreators(lastPeriodStartTime, lastPeriodEndTime);
        
        // 3. 计算同比增长率
        Map<String, Double> trends = new HashMap<>();
        trends.put("totalViews", calculateTrend(totalViews, lastTotalViews));
        trends.put("dailyActiveUsers", calculateTrend(dailyActiveUsers, lastDailyActiveUsers));
        trends.put("newCreators", calculateTrend(newCreators, lastNewCreators));
        trends.put("totalRevenue", 0.0); // 广告收入暂时设为0
        
        stats.setTotalViews(totalViews);
        stats.setDailyActiveUsers(dailyActiveUsers);
        stats.setNewCreators(newCreators);
        stats.setTotalRevenue(BigDecimal.ZERO); // 广告收入暂时设为0
        stats.setTrends(trends);
        
        // 4. 设置混合图表数据
        AdminStatsDTO.MixChartData mixChartData = new AdminStatsDTO.MixChartData();
        List<Map<String, Object>> viewsData = adminStatsMapper.getViewsByDate(startTime, endTime, groupBy);
        List<Map<String, Object>> usersData = adminStatsMapper.getNewUsersByDate(startTime, endTime, groupBy);
        
        List<String> xAxis = viewsData.stream()
                .map(item -> (String) item.get("label"))
                .collect(Collectors.toList());
        
        List<Long> viewDataList = viewsData.stream()
                .map(item -> ((Number) item.get("value")).longValue())
                .collect(Collectors.toList());
        
        // 合并用户数据到对应日期
        Map<String, Long> userDataMap = usersData.stream()
                .collect(Collectors.toMap(
                        item -> (String) item.get("label"),
                        item -> ((Number) item.get("value")).longValue(),
                        (v1, v2) -> v1
                ));
        
        List<Long> userDataList = xAxis.stream()
                .map(label -> userDataMap.getOrDefault(label, 0L))
                .collect(Collectors.toList());
        
        mixChartData.setXAxis(xAxis);
        mixChartData.setViewData(viewDataList);
        mixChartData.setUserData(userDataList);
        stats.setMixChartData(mixChartData);
        
        // 5. 设置条形图数据（热门标签）
        AdminStatsDTO.BarChartData barChartData = new AdminStatsDTO.BarChartData();
        List<Map<String, Object>> hotTags = adminStatsMapper.getHotTags(startTime, endTime, 10);
        
        List<String> tagNames = hotTags.stream()
                .map(item -> (String) item.get("name"))
                .collect(Collectors.toList());
        
        List<Long> tagCounts = hotTags.stream()
                .map(item -> ((Number) item.get("count")).longValue())
                .collect(Collectors.toList());
        
        barChartData.setYAxis(tagNames);
        barChartData.setData(tagCounts);
        stats.setBarChartData(barChartData);
        
        // 6. 设置优秀创作者列表
        List<Map<String, Object>> topCreators = adminStatsMapper.getTopCreators(startTime, endTime, 5);
        List<AdminStatsDTO.CreatorDTO> creatorList = topCreators.stream()
                .map(item -> {
                    AdminStatsDTO.CreatorDTO creator = new AdminStatsDTO.CreatorDTO();
                    creator.setName((String) item.get("name"));
                    creator.setAvatar(item.get("avatar") != null ? (String) item.get("avatar") : "");
                    creator.setScore(((Number) item.get("score")).doubleValue());
                    return creator;
                })
                .collect(Collectors.toList());
        
        stats.setCreatorList(creatorList);
        
        return stats;
    }
    
    /**
     * 计算同比增长率
     */
    private double calculateTrend(Long current, Long last) {
        if (last == null || last == 0) {
            return current != null && current > 0 ? 100.0 : 0.0;
        }
        if (current == null || current == 0) {
            return -100.0;
        }
        return ((current - last) * 100.0) / last;
    }
}
