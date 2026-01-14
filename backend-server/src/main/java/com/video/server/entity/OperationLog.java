package com.video.server.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志实体类
 */
@Data
public class OperationLog {
    
    /**
     * 日志ID
     */
    private Long id;
    
    /**
     * 操作人
     */
    private String operator;
    
    /**
     * 所属模块
     */
    private String module;
    
    /**
     * 操作内容
     */
    private String action;
    
    /**
     * 操作IP
     */
    private String ipAddress;
    
    /**
     * 状态：成功/失败
     */
    private String status;
    
    /**
     * 操作时间
     */
    private LocalDateTime createTime;
}
