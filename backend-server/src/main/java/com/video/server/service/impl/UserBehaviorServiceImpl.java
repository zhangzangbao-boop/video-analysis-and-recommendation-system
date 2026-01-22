package com.video.server.service.impl;

import com.video.server.dto.UserBehaviorRequest;
import com.video.server.entity.UserBehavior;
import com.video.server.mapper.UserBehaviorMapper;
import com.video.server.service.UserBehaviorService;
import com.video.server.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.time.LocalDateTime;

/**
 * 用户行为服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserBehaviorServiceImpl implements UserBehaviorService {
    
    private final UserBehaviorMapper userBehaviorMapper;
    private final VideoService videoService;
    
    // KafkaTemplate 设为可选依赖，如果Kafka不可用则跳过消息发送
    @Autowired(required = false)
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    // 统一使用与实时分析应用相同的topic名称
    private static final String KAFKA_TOPIC = "shortvideo_user_behavior";
    
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
                log.info("发送用户行为数据到Kafka: userId={}, videoId={}, actionType={}", 
                        userBehavior.getUserId(), userBehavior.getVideoId(), userBehavior.getActionType());
                ListenableFuture<SendResult<String, Object>> future = kafkaTemplate.send(KAFKA_TOPIC, userBehavior);
                future.addCallback(new ListenableFutureCallback<SendResult<String, Object>>() {
                    @Override
                    public void onSuccess(SendResult<String, Object> result) {
                        log.info("Kafka消息发送成功: topic={}, offset={}", 
                                KAFKA_TOPIC, result.getRecordMetadata().offset());
                    }
                    
                    @Override
                    public void onFailure(Throwable ex) {
                        log.error("Kafka消息发送失败: topic={}, error={}", KAFKA_TOPIC, ex.getMessage(), ex);
                    }
                });
            } catch (Exception e) {
                // Kafka发送失败不影响主流程，只记录日志
                log.error("Kafka消息发送异常: topic={}, error={}", KAFKA_TOPIC, e.getMessage(), e);
            }
        } else {
            log.warn("KafkaTemplate未配置，跳过消息发送");
        }
    }
}
