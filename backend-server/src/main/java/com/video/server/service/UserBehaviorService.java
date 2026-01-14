package com.video.server.service;

import com.video.server.dto.UserBehaviorRequest;

/**
 * 用户行为服务接口
 */
public interface UserBehaviorService {
    
    /**
     * 记录用户行为
     * @param request 用户行为请求
     */
    void recordBehavior(UserBehaviorRequest request);
}
