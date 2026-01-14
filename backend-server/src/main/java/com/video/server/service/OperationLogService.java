package com.video.server.service;

import com.video.server.dto.OperationLogListRequest;
import com.video.server.dto.PageResponse;
import com.video.server.entity.OperationLog;

/**
 * 操作日志服务接口
 */
public interface OperationLogService {
    
    /**
     * 获取日志列表（分页）
     * @param request 查询请求
     * @return 分页结果
     */
    PageResponse<OperationLog> getLogList(OperationLogListRequest request);
    
    /**
     * 记录操作日志
     * @param log 日志对象
     */
    void saveLog(OperationLog log);
}
