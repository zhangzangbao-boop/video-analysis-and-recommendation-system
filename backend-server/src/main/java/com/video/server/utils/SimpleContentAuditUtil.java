package com.video.server.utils;

import java.util.*;

/**
 * 简单的内容审核工具类
 * 基于关键词过滤，零成本实现
 */
public class SimpleContentAuditUtil {

    // 敏感词库（可以根据实际情况扩展）
    private static final Set<String> SENSITIVE_WORDS = new HashSet<>(Arrays.asList(
        // 政治敏感词
        "政治", "政府", "领导人",
        // 色情相关
        "色情", "性", "成人", "裸露",
        // 暴力相关
        "暴力", "血腥", "恐怖", "杀人",
        // 其他敏感词
        "赌博", "毒品", "诈骗"
    ));

    // 高风险词（直接驳回）
    private static final Set<String> HIGH_RISK_WORDS = new HashSet<>(Arrays.asList(
        "色情", "暴力", "恐怖", "赌博", "毒品"
    ));

    /**
     * 审核文本内容
     * @param text 要审核的文本（标题、描述、标签等）
     * @return 审核结果
     */
    public static AuditResult auditText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new AuditResult(true, "PASS", "内容为空，通过审核", 0);
        }

        String lowerText = text.toLowerCase();
        List<String> foundWords = new ArrayList<>();
        boolean hasHighRisk = false;

        // 检查是否包含敏感词
        for (String word : SENSITIVE_WORDS) {
            if (lowerText.contains(word.toLowerCase())) {
                foundWords.add(word);
                if (HIGH_RISK_WORDS.contains(word)) {
                    hasHighRisk = true;
                }
            }
        }

        if (foundWords.isEmpty()) {
            return new AuditResult(true, "PASS", "文本内容正常，建议通过", 0);
        }

        if (hasHighRisk) {
            return new AuditResult(false, "REJECT", 
                "检测到高风险敏感词: " + String.join(", ", foundWords) + "，建议驳回", 
                foundWords.size());
        } else {
            return new AuditResult(false, "REVIEW", 
                "检测到敏感词: " + String.join(", ", foundWords) + "，建议人工审核", 
                foundWords.size());
        }
    }

    /**
     * 综合审核视频信息（标题、描述、标签）
     * @param title 标题
     * @param description 描述
     * @param tags 标签
     * @return 综合审核结果
     */
    public static AuditResult auditVideoContent(String title, String description, String tags) {
        List<AuditResult> results = new ArrayList<>();
        
        // 审核标题
        if (title != null && !title.trim().isEmpty()) {
            results.add(auditText(title));
        }
        
        // 审核描述
        if (description != null && !description.trim().isEmpty()) {
            results.add(auditText(description));
        }
        
        // 审核标签
        if (tags != null && !tags.trim().isEmpty()) {
            // 标签可能是逗号分隔的多个标签
            String[] tagArray = tags.split("[,，]");
            for (String tag : tagArray) {
                if (!tag.trim().isEmpty()) {
                    results.add(auditText(tag.trim()));
                }
            }
        }

        // 综合判断
        if (results.isEmpty()) {
            return new AuditResult(true, "PASS", "AI审核通过：内容正常", 0);
        }

        // 检查是否有高风险
        for (AuditResult result : results) {
            if ("REJECT".equals(result.getAction())) {
                return new AuditResult(false, "REJECT", 
                    "AI审核建议驳回：" + result.getReason(), result.getRiskScore());
            }
        }

        // 检查是否需要人工审核
        for (AuditResult result : results) {
            if ("REVIEW".equals(result.getAction())) {
                return new AuditResult(false, "REVIEW", 
                    "AI审核建议人工审核：" + result.getReason(), result.getRiskScore());
            }
        }

        // 全部通过
        return new AuditResult(true, "PASS", "AI审核通过：未发现敏感内容", 0);
    }

    /**
     * 审核结果类
     */
    public static class AuditResult {
        private boolean passed;
        private String action; // PASS, REJECT, REVIEW
        private String reason;
        private int riskScore; // 风险分数 0-10

        public AuditResult(boolean passed, String action, String reason, int riskScore) {
            this.passed = passed;
            this.action = action;
            this.reason = reason;
            this.riskScore = riskScore;
        }

        public boolean isPassed() {
            return passed;
        }

        public String getAction() {
            return action;
        }

        public String getReason() {
            return reason;
        }

        public int getRiskScore() {
            return riskScore;
        }

        @Override
        public String toString() {
            return String.format("AuditResult{action=%s, reason='%s', riskScore=%d}", 
                action, reason, riskScore);
        }
    }
}
