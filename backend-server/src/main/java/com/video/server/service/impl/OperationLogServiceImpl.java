package com.video.server.service.impl;

import com.video.server.dto.OperationLogListRequest;
import com.video.server.dto.PageResponse;
import com.video.server.entity.OperationLog;
import com.video.server.mapper.OperationLogMapper;
import com.video.server.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 操作日志服务实现类
 */
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {
    
    private final OperationLogMapper operationLogMapper;
    
    @Override
    public PageResponse<OperationLog> getLogList(OperationLogListRequest request) {
        // 计算偏移量
        int offset = (request.getPage() - 1) * request.getPageSize();
        
        // 查询列表
        List<OperationLog> list = operationLogMapper.selectByCondition(
            request.getKeyword(),
            request.getStartDate(),
            request.getEndDate(),
            offset,
            request.getPageSize()
        );
        
        // 统计总数
        long total = operationLogMapper.countByCondition(
            request.getKeyword(),
            request.getStartDate(),
            request.getEndDate()
        );
        
        return new PageResponse<>(list, total, request.getPage(), request.getPageSize());
    }
    
    @Override
    public void saveLog(OperationLog log) {
        operationLogMapper.insert(log);
    }
}
