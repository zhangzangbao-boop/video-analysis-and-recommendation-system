package com.video.server.service.impl;

import com.video.server.dto.UserBehaviorRequest;
import com.video.server.entity.UserBehavior;
import com.video.server.mapper.UserBehaviorMapper;
import com.video.server.service.UserBehaviorService;
import com.video.server.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户行为服务实现类
 */
@Service
@RequiredArgsConstructor
public class UserBehaviorServiceImpl implements UserBehaviorService {
    
    private final UserBehaviorMapper userBehaviorMapper;
    private final VideoService videoService;
    
    // KafkaTemplate 设为可选依赖，如果Kafka不可用则跳过消息发送
    @Autowired(required = false)
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    private static final String KAFKA_TOPIC = "user-behavior-topic";
    
    @Override
    public void recordBehavior(UserBehaviorRequest request) {
        // 1. 插入行为记录
        UserBehavior userBehavior = new UserBehavior();
        userBehavior.setUserId(request.getUserId());
        userBehavior.setVideoId(request.getVideoId());
        userBehavior.setActionType(request.getActionType());
        userBehavior.setCreateTime(LocalDateTime.now());
        userBehaviorMapper.insert(userBehavior);
        
        // 2. 如果是点赞行为，更新视频点赞数
        if ("like".equalsIgnoreCase(request.getActionType())) {
            videoService.incrementLikeCount(request.getVideoId());
        }
        
        // 3. 如果Kafka可用，发送行为数据到 Kafka
        if (kafkaTemplate != null) {
            try {
                kafkaTemplate.send(KAFKA_TOPIC, userBehavior);
            } catch (Exception e) {
                // Kafka发送失败不影响主流程，只记录日志
                System.err.println("Kafka消息发送失败: " + e.getMessage());
            }
        }
    }
}
