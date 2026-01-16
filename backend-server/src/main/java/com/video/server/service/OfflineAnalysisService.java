package com.video.server.service;

import com.video.server.dto.OfflineDataRequest;
import com.video.server.dto.OfflineDataResponse;

/**
 * 离线分析服务接口
 */
public interface OfflineAnalysisService {
    
    /**
     * 获取离线分析数据
     * @param request 请求参数
     * @return 数据响应
     */
    OfflineDataResponse getOfflineData(OfflineDataRequest request);
}
