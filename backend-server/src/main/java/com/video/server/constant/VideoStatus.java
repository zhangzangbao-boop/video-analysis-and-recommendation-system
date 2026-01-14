package com.video.server.constant;

/**
 * 视频审核状态枚举
 */
public enum VideoStatus {
    /**
     * 待审核
     */
    PENDING,
    
    /**
     * 审核通过
     */
    PASSED,
    
    /**
     * 审核驳回
     */
    REJECTED
}
