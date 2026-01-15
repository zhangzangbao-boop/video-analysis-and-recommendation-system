package com.video.server.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置类
 * 只有在 RedisConnectionFactory 存在时才会创建 RedisTemplate
 * 这样在测试环境中（Redis 被禁用时）不会尝试创建此 bean
 */
@Configuration
@ConditionalOnBean(RedisConnectionFactory.class)
public class RedisConfig {
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 使用 String 序列化器作为 key 的序列化器
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // 使用 JSON 序列化器作为 value 的序列化器
        // 注意：Spring Boot 4.0 中推荐使用 RedisSerializationContext，这里使用兼容方式
        template.setValueSerializer(RedisSerializer.json());
        template.setHashValueSerializer(RedisSerializer.json());
        
        template.afterPropertiesSet();
        return template;
    }
}
