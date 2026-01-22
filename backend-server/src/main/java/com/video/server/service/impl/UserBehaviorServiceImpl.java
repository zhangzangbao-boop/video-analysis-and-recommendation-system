package com.video.server.service.impl;

import com.video.server.dto.UserBehaviorKafkaMessage;
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

        System.out.println("1\n1\n\n\n\n\n\n");


        // 3. 如果Kafka可用，发送行为数据到 Kafka
        if (kafkaTemplate != null) {
            try {
                // 转换为大数据模块期望的格式
                UserBehaviorKafkaMessage kafkaMessage = UserBehaviorKafkaMessage.builder()
                        .userId(userBehavior.getUserId())
                        .videoId(userBehavior.getVideoId())
                        .behaviorType(userBehavior.getActionType())  // actionType -> behaviorType
                        .behaviorTime(UserBehaviorKafkaMessage.formatTime(userBehavior.getCreateTime()))  // createTime -> behaviorTime (字符串格式)
                        .duration(0)
                        .deviceInfo("")
                        .networkType("")
                        .ipAddress("")
                        .location("")
                        .extraInfo("")
                        .build();
                
                ListenableFuture<SendResult<String, Object>> future = kafkaTemplate.send(KAFKA_TOPIC, kafkaMessage);
                future.addCallback(new ListenableFutureCallback<SendResult<String, Object>>() {
                    @Override
                    public void onSuccess(SendResult<String, Object> result) {
                        // 只记录成功信息，简化输出
                        log.info("✓ Kafka消息已发送: userId={}, videoId={}, behaviorType={}, offset={}", 
                                userBehavior.getUserId(), userBehavior.getVideoId(), 
                                userBehavior.getActionType(), result.getRecordMetadata().offset());
                    }
                    
                    @Override
                    public void onFailure(Throwable ex) {
                        log.error("✗ Kafka消息发送失败: userId={}, videoId={}, error={}", 
                                userBehavior.getUserId(), userBehavior.getVideoId(), ex.getMessage());
                    }
                });
            } catch (Exception e) {
                // Kafka发送失败不影响主流程，只记录错误
                log.error("✗ Kafka消息发送异常: userId={}, videoId={}, error={}", 
                        userBehavior.getUserId(), userBehavior.getVideoId(), e.getMessage());
            }
        }
    }
}
