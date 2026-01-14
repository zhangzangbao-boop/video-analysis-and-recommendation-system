package com.video.server.service.impl;

import com.video.server.dto.AdminStatsDTO;
import com.video.server.service.AdminStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * 管理后台统计服务实现类
 */
@Service
@RequiredArgsConstructor
public class AdminStatsServiceImpl implements AdminStatsService {
    
    @Override
    public AdminStatsDTO getStats(String dateRange) {
        AdminStatsDTO stats = new AdminStatsDTO();
        
        // 模拟数据（实际应该从数据库统计）
        // 根据时间范围返回不同的数据
        switch (dateRange) {
            case "week":
                stats.setTotalViews(892300L);
                stats.setDailyActiveUsers(45200L);
                stats.setNewCreators(320L);
                stats.setTotalRevenue(new BigDecimal("125800.00"));
                break;
            case "month":
                stats.setTotalViews(3580000L);
                stats.setDailyActiveUsers(128000L);
                stats.setNewCreators(1050L);
                stats.setTotalRevenue(new BigDecimal("480000.00"));
                break;
            case "year":
                stats.setTotalViews(45000000L);
                stats.setDailyActiveUsers(890000L);
                stats.setNewCreators(12000L);
                stats.setTotalRevenue(new BigDecimal("5600000.00"));
                break;
            default:
                stats.setTotalViews(892300L);
                stats.setDailyActiveUsers(45200L);
                stats.setNewCreators(320L);
                stats.setTotalRevenue(new BigDecimal("125800.00"));
        }
        
        // 设置同比增长率
        Map<String, Double> trends = new HashMap<>();
        trends.put("totalViews", 12.5);
        trends.put("dailyActiveUsers", 5.2);
        trends.put("newCreators", -2.1);
        trends.put("totalRevenue", 8.4);
        stats.setTrends(trends);
        
        // 设置混合图表数据
        AdminStatsDTO.MixChartData mixChartData = new AdminStatsDTO.MixChartData();
        if ("week".equals(dateRange)) {
            mixChartData.setXAxis(Arrays.asList("周一", "周二", "周三", "周四", "周五", "周六", "周日"));
            mixChartData.setUserData(Arrays.asList(20L, 49L, 70L, 23L, 25L, 76L, 135L));
            mixChartData.setViewData(Arrays.asList(2000L, 4900L, 7000L, 2300L, 2500L, 7600L, 13500L));
        } else if ("month".equals(dateRange)) {
            mixChartData.setXAxis(Arrays.asList("1日", "5日", "10日", "15日", "20日", "25日", "30日"));
            mixChartData.setUserData(Arrays.asList(150L, 230L, 224L, 218L, 135L, 147L, 260L));
            mixChartData.setViewData(Arrays.asList(15000L, 23000L, 22400L, 21800L, 13500L, 14700L, 26000L));
        } else {
            mixChartData.setXAxis(Arrays.asList("1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月"));
            mixChartData.setUserData(Arrays.asList(2000L, 3500L, 4000L, 3800L, 5000L, 6000L, 7500L, 8000L, 7000L, 6500L, 9000L, 10000L));
            mixChartData.setViewData(Arrays.asList(200000L, 350000L, 400000L, 380000L, 500000L, 600000L, 750000L, 800000L, 700000L, 650000L, 900000L, 1000000L));
        }
        stats.setMixChartData(mixChartData);
        
        // 设置条形图数据
        AdminStatsDTO.BarChartData barChartData = new AdminStatsDTO.BarChartData();
        if ("week".equals(dateRange)) {
            barChartData.setYAxis(Arrays.asList("Vlog", "猫咪", "Python", "Vue3", "健身", "旅行", "探店", "测评", "LOL", "职场"));
            barChartData.setData(Arrays.asList(320L, 302L, 301L, 334L, 390L, 450L, 420L, 480L, 500L, 550L));
        } else if ("month".equals(dateRange)) {
            barChartData.setYAxis(Arrays.asList("美妆", "穿搭", "数码", "情感", "剧情", "科普", "新闻", "音乐", "舞蹈", "生活"));
            barChartData.setData(Arrays.asList(1200L, 1300L, 1400L, 1500L, 1600L, 1800L, 2100L, 2400L, 2800L, 3000L));
        } else {
            barChartData.setYAxis(Arrays.asList("年度大赏", "春节", "世界杯", "双11", "毕业季", "开学", "暑假", "寒假", "国庆", "五一"));
            barChartData.setData(Arrays.asList(50000L, 52000L, 55000L, 60000L, 65000L, 70000L, 80000L, 85000L, 90000L, 100000L));
        }
        stats.setBarChartData(barChartData);
        
        // 设置优秀创作者列表
        List<AdminStatsDTO.CreatorDTO> creatorList = new ArrayList<>();
        AdminStatsDTO.CreatorDTO creator1 = new AdminStatsDTO.CreatorDTO();
        creator1.setName("极客阿辉");
        creator1.setAvatar("https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png");
        creator1.setScore(98.2);
        creatorList.add(creator1);
        
        AdminStatsDTO.CreatorDTO creator2 = new AdminStatsDTO.CreatorDTO();
        creator2.setName("美妆小皇后");
        creator2.setAvatar("https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png");
        creator2.setScore(95.6);
        creatorList.add(creator2);
        
        AdminStatsDTO.CreatorDTO creator3 = new AdminStatsDTO.CreatorDTO();
        creator3.setName("旅行日记");
        creator3.setAvatar("https://cube.elemecdn.com/9/c2/f0ee8a3c7c9638a54940382568c9dpng.png");
        creator3.setScore(92.1);
        creatorList.add(creator3);
        
        AdminStatsDTO.CreatorDTO creator4 = new AdminStatsDTO.CreatorDTO();
        creator4.setName("萌宠集中营");
        creator4.setAvatar("https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png");
        creator4.setScore(89.5);
        creatorList.add(creator4);
        
        AdminStatsDTO.CreatorDTO creator5 = new AdminStatsDTO.CreatorDTO();
        creator5.setName("Java教学");
        creator5.setAvatar("https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png");
        creator5.setScore(88.3);
        creatorList.add(creator5);
        
        stats.setCreatorList(creatorList);
        
        return stats;
    }
}
