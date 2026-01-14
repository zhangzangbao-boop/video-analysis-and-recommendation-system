package com.video.server.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 管理后台统计数据DTO
 */
@Data
public class AdminStatsDTO {
    
    /**
     * 总播放量
     */
    private Long totalViews;
    
    /**
     * 日活跃用户
     */
    private Long dailyActiveUsers;
    
    /**
     * 新增创作者
     */
    private Long newCreators;
    
    /**
     * 广告总收入
     */
    private BigDecimal totalRevenue;
    
    /**
     * 同比增长率（百分比）
     */
    private Map<String, Double> trends;
    
    /**
     * 流量与用户增长趋势数据
     */
    private MixChartData mixChartData;
    
    /**
     * 热门标签排行数据
     */
    private BarChartData barChartData;
    
    /**
     * 优秀创作者列表
     */
    private List<CreatorDTO> creatorList;
    
    @Data
    public static class MixChartData {
        private List<String> xAxis;
        private List<Long> userData;
        private List<Long> viewData;
    }
    
    @Data
    public static class BarChartData {
        private List<String> yAxis;
        private List<Long> data;
    }
    
    @Data
    public static class CreatorDTO {
        private String name;
        private String avatar;
        private Double score;
    }
}
