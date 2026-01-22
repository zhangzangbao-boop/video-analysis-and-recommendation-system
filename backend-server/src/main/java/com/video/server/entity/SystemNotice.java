package com.video.server.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 系统通知实体类
 */
@Data
public class SystemNotice {
    
    /**
     * 通知ID
     */
    private Long id;
    
    /**
     * 通知标题
     */
    private String title;
    
    /**
     * 通知内容
     */
    private String content;
    
    /**
     * 通知类型: 1-系统公告, 2-活动通知, 3-系统维护
     */
    private Integer type;
    
    /**
     * 目标类型: 0-全部用户, 1-指定用户
     */
    private Integer targetType;
    
    /**
     * 目标用户ID (target_type=1时有效)
     */
    private Long targetUserId;
    
    /**
     * 状态: 0-草稿, 1-已发布, 2-已撤回
     */
    private Integer status;
    
    /**
     * 是否置顶: 0-否, 1-是
     */
    private Integer isTop;
    
    /**
     * 阅读数
     */
    private Integer readCount;
    
    /**
     * 发布时间
     */
    private LocalDateTime publishTime;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
