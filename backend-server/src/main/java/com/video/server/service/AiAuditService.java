package com.video.server.service;

import com.video.server.dto.AiAuditResult;

/**
 * AI审核服务接口
 */
public interface AiAuditService {
    
    /**
     * 审核视频内容（包括视频、封面、文本）
     * @param videoUrl 视频URL
     * @param coverUrl 封面URL
     * @param title 标题
     * @param description 描述
     * @param tags 标签
     * @return AI审核结果
     */
    AiAuditResult auditVideo(String videoUrl, String coverUrl, String title, String description, String tags);
    
    /**
     * 仅审核文本内容（标题、描述、标签）
     * @param title 标题
     * @param description 描述
     * @param tags 标签
     * @return AI审核结果
     */
    AiAuditResult auditText(String title, String description, String tags);
    
    /**
     * 审核图片（封面）
     * @param imageUrl 图片URL
     * @return AI审核结果
     */
    AiAuditResult auditImage(String imageUrl);
}
