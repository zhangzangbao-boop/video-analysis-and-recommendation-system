package com.video.server.service.impl;

import com.video.server.dto.OfflineDataRequest;
import com.video.server.dto.OfflineDataResponse;
import com.video.server.entity.User;
import com.video.server.entity.UserBehavior;
import com.video.server.entity.Video;
import com.video.server.entity.VideoInteraction;
import com.video.server.entity.VideoPlayRecord;
import com.video.server.mapper.UserBehaviorMapper;
import com.video.server.mapper.UserMapper;
import com.video.server.mapper.VideoInteractionMapper;
import com.video.server.mapper.VideoMapper;
import com.video.server.mapper.VideoPlayRecordMapper;
import com.video.server.service.OfflineAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 离线分析服务实现类
 */
@Service
@RequiredArgsConstructor
public class OfflineAnalysisServiceImpl implements OfflineAnalysisService {
    
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_PAGE_SIZE = 10000;
    
    private final UserBehaviorMapper userBehaviorMapper;
    private final VideoPlayRecordMapper videoPlayRecordMapper;
    private final VideoInteractionMapper videoInteractionMapper;
    private final VideoMapper videoMapper;
    private final UserMapper userMapper;
    
    @Override
    public OfflineDataResponse getOfflineData(OfflineDataRequest request) {
        // 参数校验和默认值设置
        String tableType = request.getTableType();
        if (tableType == null || tableType.isEmpty()) {
            throw new IllegalArgumentException("tableType参数不能为空");
        }
        
        int page = request.getPage() != null && request.getPage() > 0 ? request.getPage() : 1;
        int pageSize = request.getPageSize() != null && request.getPageSize() > 0 
                ? Math.min(request.getPageSize(), MAX_PAGE_SIZE) : 1000;
        
        // 解析时间
        LocalDateTime startTime = null;
        LocalDateTime endTime = null;
        if (request.getStartTime() != null && !request.getStartTime().isEmpty()) {
            startTime = LocalDateTime.parse(request.getStartTime(), DATE_TIME_FORMATTER);
        }
        if (request.getEndTime() != null && !request.getEndTime().isEmpty()) {
            endTime = LocalDateTime.parse(request.getEndTime(), DATE_TIME_FORMATTER);
        }
        
        Long lastId = request.getLastId();
        
        // 根据表类型查询数据
        List<Object> records = new ArrayList<>();
        Long total = 0L;
        Long lastRecordId = null;
        
        if ("all".equalsIgnoreCase(tableType)) {
            // 查询所有表的数据
            List<Object> allRecords = new ArrayList<>();
            Long allTotal = 0L;
            
            // 用户行为
            List<UserBehavior> behaviors = userBehaviorMapper.selectByCondition(startTime, endTime, lastId, pageSize);
            allRecords.addAll(behaviors.stream().map(b -> (Object) b).collect(Collectors.toList()));
            allTotal += userBehaviorMapper.countByCondition(startTime, endTime);
            if (!behaviors.isEmpty()) {
                lastRecordId = behaviors.get(behaviors.size() - 1).getId();
            }
            
            // 播放记录
            List<VideoPlayRecord> playRecords = videoPlayRecordMapper.selectByCondition(startTime, endTime, lastId, pageSize);
            allRecords.addAll(playRecords.stream().map(r -> (Object) r).collect(Collectors.toList()));
            allTotal += videoPlayRecordMapper.countByCondition(startTime, endTime);
            
            // 互动记录
            List<VideoInteraction> interactions = videoInteractionMapper.selectByCondition(startTime, endTime, lastId, pageSize);
            allRecords.addAll(interactions.stream().map(i -> (Object) i).collect(Collectors.toList()));
            allTotal += videoInteractionMapper.countByCondition(startTime, endTime);
            
            // 视频信息
            List<Video> videos = videoMapper.selectByConditionForOffline(startTime, endTime, lastId, pageSize);
            allRecords.addAll(videos.stream().map(v -> (Object) v).collect(Collectors.toList()));
            allTotal += videoMapper.countByConditionForOffline(startTime, endTime);
            
            // 用户信息
            List<User> users = userMapper.selectByConditionForOffline(startTime, endTime, lastId, pageSize);
            allRecords.addAll(users.stream().map(u -> (Object) u).collect(Collectors.toList()));
            allTotal += userMapper.countByConditionForOffline(startTime, endTime);
            
            records = allRecords;
            total = allTotal;
        } else {
            // 查询指定表的数据
            switch (tableType.toLowerCase()) {
                case "user_behavior":
                    List<UserBehavior> behaviors = userBehaviorMapper.selectByCondition(startTime, endTime, lastId, pageSize);
                    records = behaviors.stream().map(b -> (Object) b).collect(Collectors.toList());
                    total = userBehaviorMapper.countByCondition(startTime, endTime);
                    if (!behaviors.isEmpty()) {
                        lastRecordId = behaviors.get(behaviors.size() - 1).getId();
                    }
                    break;
                case "video_play_record":
                    List<VideoPlayRecord> playRecords = videoPlayRecordMapper.selectByCondition(startTime, endTime, lastId, pageSize);
                    records = playRecords.stream().map(r -> (Object) r).collect(Collectors.toList());
                    total = videoPlayRecordMapper.countByCondition(startTime, endTime);
                    if (!playRecords.isEmpty()) {
                        lastRecordId = playRecords.get(playRecords.size() - 1).getId();
                    }
                    break;
                case "video_interaction":
                    List<VideoInteraction> interactions = videoInteractionMapper.selectByCondition(startTime, endTime, lastId, pageSize);
                    records = interactions.stream().map(i -> (Object) i).collect(Collectors.toList());
                    total = videoInteractionMapper.countByCondition(startTime, endTime);
                    if (!interactions.isEmpty()) {
                        lastRecordId = interactions.get(interactions.size() - 1).getId();
                    }
                    break;
                case "video_info":
                    List<Video> videos = videoMapper.selectByConditionForOffline(startTime, endTime, lastId, pageSize);
                    records = videos.stream().map(v -> (Object) v).collect(Collectors.toList());
                    total = videoMapper.countByConditionForOffline(startTime, endTime);
                    if (!videos.isEmpty()) {
                        lastRecordId = videos.get(videos.size() - 1).getId();
                    }
                    break;
                case "sys_user":
                    List<User> users = userMapper.selectByConditionForOffline(startTime, endTime, lastId, pageSize);
                    records = users.stream().map(u -> (Object) u).collect(Collectors.toList());
                    total = userMapper.countByConditionForOffline(startTime, endTime);
                    if (!users.isEmpty()) {
                        lastRecordId = users.get(users.size() - 1).getId();
                    }
                    break;
                default:
                    throw new IllegalArgumentException("不支持的表类型: " + tableType);
            }
        }
        
        // 判断是否还有更多数据
        boolean hasMore = records.size() >= pageSize;
        
        return OfflineDataResponse.builder()
                .tableType(tableType)
                .total(total)
                .page(page)
                .pageSize(pageSize)
                .hasMore(hasMore)
                .lastId(lastRecordId)
                .records(records)
                .build();
    }
}
