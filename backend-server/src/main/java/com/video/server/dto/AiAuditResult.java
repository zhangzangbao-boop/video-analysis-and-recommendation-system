package com.video.server.dto;

import lombok.Data;
import java.util.List;

/**
 * AI审核结果
 */
@Data
public class AiAuditResult {
    
    /**
     * 审核是否通过
     */
    private Boolean passed;
    
    /**
     * 建议操作：PASS(通过), REJECT(驳回), REVIEW(人工审核)
     */
    private String suggestion;
    
    /**
     * 风险等级：LOW(低), MEDIUM(中), HIGH(高)
     */
    private String riskLevel;
    
    /**
     * 风险分数 0-100
     */
    private Integer riskScore;
    
    /**
     * 审核结果描述
     */
    private String message;
    
    /**
     * 违规详情列表
     */
    private List<ViolationDetail> violations;
    
    /**
     * 违规详情
     */
    @Data
    public static class ViolationDetail {
        /**
         * 违规类型：porn(色情), violence(暴力), politics(政治), terrorism(暴恐), etc.
         */
        private String type;
        
        /**
         * 违规类型中文名称
         */
        private String typeName;
        
        /**
         * 置信度 0-100
         */
        private Integer confidence;
        
        /**
         * 违规描述
         */
        private String description;
        
        /**
         * 违规位置：title(标题), description(描述), tags(标签), cover(封面), video(视频)
         */
        private String location;
    }
}
