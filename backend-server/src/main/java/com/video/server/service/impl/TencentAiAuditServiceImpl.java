package com.video.server.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.video.server.dto.AiAuditResult;
import com.video.server.service.AiAuditService;
import com.video.server.utils.SimpleContentAuditUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 腾讯云内容安全AI审核服务实现
 * 使用腾讯云内容安全API进行视频、图片、文本审核
 */
@Slf4j
@Service
public class TencentAiAuditServiceImpl implements AiAuditService {

    @Value("${tencent.cos.secret-id}")
    private String secretId;

    @Value("${tencent.cos.secret-key}")
    private String secretKey;

    @Value("${tencent.cms.region:ap-guangzhou}")
    private String region;

    private static final String CMS_ENDPOINT = "cms.tencentcloudapi.com";
    private static final String SERVICE = "cms";
    private static final String VERSION = "2019-03-21";
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();

    @Override
    public AiAuditResult auditVideo(String videoUrl, String coverUrl, String title, String description, String tags) {
        List<AiAuditResult> results = new ArrayList<>();
        
        // 1. 审核文本内容
        AiAuditResult textResult = auditText(title, description, tags);
        results.add(textResult);
        
        // 2. 审核封面图片（如果有）
        if (coverUrl != null && !coverUrl.trim().isEmpty()) {
            AiAuditResult imageResult = auditImage(coverUrl);
            results.add(imageResult);
        }
        
        // 3. 综合判断（取最高风险）
        return mergeResults(results);
    }

    @Override
    public AiAuditResult auditText(String title, String description, String tags) {
        try {
            // 先尝试调用腾讯云API
            if (secretId != null && secretKey != null && !secretId.isEmpty() && !secretKey.isEmpty()) {
                try {
                    // 构建文本内容
                    StringBuilder content = new StringBuilder();
                    if (title != null) content.append("标题：").append(title).append("\n");
                    if (description != null) content.append("描述：").append(description).append("\n");
                    if (tags != null) content.append("标签：").append(tags);
                    
                    // 调用腾讯云文本内容安全API
                    String response = callTextModeration(content.toString());
                    return parseTextModerationResponse(response, title, description, tags);
                } catch (Exception e) {
                    log.warn("腾讯云API调用失败，降级到关键词过滤: {}", e.getMessage());
                }
            }
            
            // 降级方案：使用简单关键词过滤
            SimpleContentAuditUtil.AuditResult simpleResult = SimpleContentAuditUtil.auditVideoContent(title, description, tags);
            return convertSimpleResult(simpleResult);
            
        } catch (Exception e) {
            log.error("文本审核失败", e);
            // 最终降级：返回需要人工审核
            return createFallbackResult("文本审核服务异常，建议人工审核");
        }
    }

    @Override
    public AiAuditResult auditImage(String imageUrl) {
        try {
            // 调用腾讯云图片内容安全API
            String response = callImageModeration(imageUrl);
            return parseImageModerationResponse(response);
            
        } catch (Exception e) {
            log.error("图片审核失败", e);
            return createFallbackResult("图片审核服务异常，建议人工审核");
        }
    }

    /**
     * 调用腾讯云文本内容安全API
     */
    private String callTextModeration(String content) throws IOException {
        JSONObject params = new JSONObject();
        params.put("Content", content);
        params.put("BizType", "default"); // 业务类型，使用默认
        
        return callTencentAPI("TextModeration", params.toJSONString());
    }

    /**
     * 调用腾讯云图片内容安全API
     */
    private String callImageModeration(String imageUrl) throws IOException {
        JSONObject params = new JSONObject();
        params.put("FileUrl", imageUrl); // 使用URL方式
        params.put("BizType", "default");
        
        return callTencentAPI("ImageModeration", params.toJSONString());
    }

    /**
     * 调用腾讯云API通用方法
     */
    private String callTencentAPI(String action, String payload) throws IOException {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonce = String.valueOf(new Random().nextInt(Integer.MAX_VALUE));
        
        // 构建请求参数
        Map<String, String> params = new LinkedHashMap<>();
        params.put("Action", action);
        params.put("Version", VERSION);
        params.put("Region", region);
        params.put("SecretId", secretId);
        params.put("Timestamp", timestamp);
        params.put("Nonce", nonce);
        params.put("SignatureMethod", "HmacSHA256");
        
        // 添加payload（对于POST请求）
        if (payload != null && !payload.isEmpty()) {
            params.put("RequestClient", "SDK_JAVA_3.1.562");
        }
        
        // 构建签名字符串
        StringBuilder signStr = new StringBuilder();
        signStr.append("POST").append("\n");
        signStr.append(CMS_ENDPOINT).append("\n");
        signStr.append("/").append("\n");
        
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String value = params.get(key);
            if (i > 0) signStr.append("&");
            signStr.append(key).append("=").append(value);
        }
        
        // 计算签名
        String signature = hmacSHA256(signStr.toString(), secretKey);
        params.put("Signature", signature);
        
        // 构建请求体
        StringBuilder requestBody = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String value = params.get(key);
            if (i > 0) requestBody.append("&");
            requestBody.append(key).append("=").append(java.net.URLEncoder.encode(value, "UTF-8"));
        }
        
        if (payload != null && !payload.isEmpty()) {
            // 对于JSON payload，需要特殊处理
            JSONObject jsonPayload = JSON.parseObject(payload);
            for (String key : jsonPayload.keySet()) {
                requestBody.append("&").append(key).append("=")
                    .append(java.net.URLEncoder.encode(jsonPayload.getString(key), "UTF-8"));
            }
        }
        
        // 发送HTTP请求
        RequestBody body = RequestBody.create(
            MediaType.parse("application/x-www-form-urlencoded"),
            requestBody.toString()
        );
        
        Request request = new Request.Builder()
            .url("https://" + CMS_ENDPOINT)
            .post(body)
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .build();
        
        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            return response.body().string();
        }
    }

    /**
     * HMAC-SHA256签名
     */
    private String hmacSHA256(String data, String key) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(
                key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256签名失败", e);
        }
    }

    /**
     * 解析文本审核响应
     */
    private AiAuditResult parseTextModerationResponse(String response, String title, String description, String tags) {
        try {
            JSONObject json = JSON.parseObject(response);
            
            // 检查是否有错误
            if (json.containsKey("Response") && json.getJSONObject("Response").containsKey("Error")) {
                JSONObject error = json.getJSONObject("Response").getJSONObject("Error");
                log.warn("腾讯云API返回错误: {}", error.getString("Message"));
                return createFallbackResult("审核服务暂时不可用，建议人工审核");
            }
            
            JSONObject result = json.getJSONObject("Response");
            if (result == null) {
                return createFallbackResult("审核结果解析失败，建议人工审核");
            }
            
            // 解析审核结果
            String label = result.getString("Label"); // Normal, Spam, Ad, Politics, Terrorism, Abuse, Porn, Illegal, etc.
            Integer score = result.getInteger("Score"); // 0-100
            String suggestion = result.getString("Suggestion"); // Pass, Review, Block
            
            AiAuditResult auditResult = new AiAuditResult();
            List<AiAuditResult.ViolationDetail> violations = new ArrayList<>();
            
            // 判断是否通过
            boolean passed = "Pass".equalsIgnoreCase(suggestion) || "Normal".equalsIgnoreCase(label);
            auditResult.setPassed(passed);
            
            // 设置建议操作
            if ("Block".equalsIgnoreCase(suggestion)) {
                auditResult.setSuggestion("REJECT");
            } else if ("Review".equalsIgnoreCase(suggestion)) {
                auditResult.setSuggestion("REVIEW");
            } else {
                auditResult.setSuggestion("PASS");
            }
            
            // 设置风险等级
            if (score != null) {
                if (score >= 80) {
                    auditResult.setRiskLevel("HIGH");
                } else if (score >= 50) {
                    auditResult.setRiskLevel("MEDIUM");
                } else {
                    auditResult.setRiskLevel("LOW");
                }
                auditResult.setRiskScore(score);
            } else {
                auditResult.setRiskLevel("LOW");
                auditResult.setRiskScore(0);
            }
            
            // 如果有违规，添加违规详情
            if (!passed && label != null && !"Normal".equalsIgnoreCase(label)) {
                AiAuditResult.ViolationDetail violation = new AiAuditResult.ViolationDetail();
                violation.setType(label.toLowerCase());
                violation.setTypeName(getViolationTypeName(label));
                violation.setConfidence(score);
                violation.setDescription(getViolationDescription(label));
                violation.setLocation("text");
                violations.add(violation);
            }
            
            auditResult.setViolations(violations);
            
            // 构建消息
            StringBuilder message = new StringBuilder();
            if (passed) {
                message.append("AI审核通过：文本内容正常");
            } else {
                message.append("AI检测到违规内容：").append(getViolationTypeName(label));
                if (score != null) {
                    message.append("（置信度：").append(score).append("%）");
                }
            }
            auditResult.setMessage(message.toString());
            
            return auditResult;
            
        } catch (Exception e) {
            log.error("解析文本审核响应失败", e);
            return createFallbackResult("审核结果解析失败，建议人工审核");
        }
    }

    /**
     * 解析图片审核响应
     */
    private AiAuditResult parseImageModerationResponse(String response) {
        try {
            JSONObject json = JSON.parseObject(response);
            JSONObject result = json.getJSONObject("Response");
            
            if (result == null) {
                return createFallbackResult("图片审核结果解析失败，建议人工审核");
            }
            
            // 解析审核结果（与文本类似）
            String label = result.getString("Label");
            Integer score = result.getInteger("Score");
            String suggestion = result.getString("Suggestion");
            
            AiAuditResult auditResult = new AiAuditResult();
            boolean passed = "Pass".equalsIgnoreCase(suggestion) || "Normal".equalsIgnoreCase(label);
            auditResult.setPassed(passed);
            
            if ("Block".equalsIgnoreCase(suggestion)) {
                auditResult.setSuggestion("REJECT");
            } else if ("Review".equalsIgnoreCase(suggestion)) {
                auditResult.setSuggestion("REVIEW");
            } else {
                auditResult.setSuggestion("PASS");
            }
            
            if (score != null) {
                auditResult.setRiskScore(score);
                if (score >= 80) {
                    auditResult.setRiskLevel("HIGH");
                } else if (score >= 50) {
                    auditResult.setRiskLevel("MEDIUM");
                } else {
                    auditResult.setRiskLevel("LOW");
                }
            }
            
            List<AiAuditResult.ViolationDetail> violations = new ArrayList<>();
            if (!passed && label != null && !"Normal".equalsIgnoreCase(label)) {
                AiAuditResult.ViolationDetail violation = new AiAuditResult.ViolationDetail();
                violation.setType(label.toLowerCase());
                violation.setTypeName(getViolationTypeName(label));
                violation.setConfidence(score);
                violation.setDescription(getViolationDescription(label));
                violation.setLocation("cover");
                violations.add(violation);
            }
            auditResult.setViolations(violations);
            
            StringBuilder message = new StringBuilder();
            if (passed) {
                message.append("AI审核通过：封面图片正常");
            } else {
                message.append("AI检测到违规内容：").append(getViolationTypeName(label));
            }
            auditResult.setMessage(message.toString());
            
            return auditResult;
            
        } catch (Exception e) {
            log.error("解析图片审核响应失败", e);
            return createFallbackResult("图片审核结果解析失败，建议人工审核");
        }
    }

    /**
     * 合并多个审核结果（取最高风险）
     */
    private AiAuditResult mergeResults(List<AiAuditResult> results) {
        if (results == null || results.isEmpty()) {
            return createFallbackResult("未进行任何审核");
        }
        
        AiAuditResult merged = new AiAuditResult();
        List<AiAuditResult.ViolationDetail> allViolations = new ArrayList<>();
        boolean allPassed = true;
        int maxRiskScore = 0;
        String highestRiskLevel = "LOW";
        String finalSuggestion = "PASS";
        
        for (AiAuditResult result : results) {
            if (!result.getPassed()) {
                allPassed = false;
            }
            if (result.getViolations() != null) {
                allViolations.addAll(result.getViolations());
            }
            if (result.getRiskScore() != null && result.getRiskScore() > maxRiskScore) {
                maxRiskScore = result.getRiskScore();
                highestRiskLevel = result.getRiskLevel();
            }
            // 优先级：REJECT > REVIEW > PASS
            if ("REJECT".equals(result.getSuggestion())) {
                finalSuggestion = "REJECT";
            } else if ("REVIEW".equals(result.getSuggestion()) && !"REJECT".equals(finalSuggestion)) {
                finalSuggestion = "REVIEW";
            }
        }
        
        merged.setPassed(allPassed);
        merged.setSuggestion(finalSuggestion);
        merged.setRiskLevel(highestRiskLevel);
        merged.setRiskScore(maxRiskScore);
        merged.setViolations(allViolations);
        
        // 构建综合消息
        StringBuilder message = new StringBuilder();
        if (allPassed) {
            message.append("AI综合审核通过：所有内容正常");
        } else {
            message.append("AI综合审核发现").append(allViolations.size()).append("处违规内容");
            if (!allViolations.isEmpty()) {
                message.append("：").append(allViolations.get(0).getTypeName());
            }
        }
        merged.setMessage(message.toString());
        
        return merged;
    }

    /**
     * 创建降级结果（当API调用失败时）
     */
    private AiAuditResult createFallbackResult(String message) {
        AiAuditResult result = new AiAuditResult();
        result.setPassed(false);
        result.setSuggestion("REVIEW");
        result.setRiskLevel("MEDIUM");
        result.setRiskScore(50);
        result.setMessage(message);
        result.setViolations(new ArrayList<>());
        return result;
    }

    /**
     * 获取违规类型中文名称
     */
    private String getViolationTypeName(String label) {
        if (label == null) return "未知";
        switch (label.toUpperCase()) {
            case "PORN": return "色情内容";
            case "VIOLENCE": return "暴力内容";
            case "POLITICS": return "政治敏感";
            case "TERRORISM": return "暴恐内容";
            case "ABUSE": return "辱骂内容";
            case "ILLEGAL": return "违法违规";
            case "SPAM": return "垃圾信息";
            case "AD": return "广告内容";
            default: return label;
        }
    }

    /**
     * 获取违规描述
     */
    private String getViolationDescription(String label) {
        return getViolationTypeName(label) + "，建议" + 
            ("PORN".equalsIgnoreCase(label) || "TERRORISM".equalsIgnoreCase(label) ? "驳回" : "人工审核");
    }
    
    /**
     * 将简单审核结果转换为AI审核结果
     */
    private AiAuditResult convertSimpleResult(SimpleContentAuditUtil.AuditResult simpleResult) {
        AiAuditResult result = new AiAuditResult();
        result.setPassed(simpleResult.isPassed());
        result.setSuggestion(simpleResult.getAction());
        result.setMessage(simpleResult.getReason());
        result.setRiskScore(simpleResult.getRiskScore() * 10); // 转换为0-100
        result.setViolations(new ArrayList<>());
        
        // 设置风险等级
        if (result.getRiskScore() >= 80) {
            result.setRiskLevel("HIGH");
        } else if (result.getRiskScore() >= 50) {
            result.setRiskLevel("MEDIUM");
        } else {
            result.setRiskLevel("LOW");
        }
        
        return result;
    }
}
