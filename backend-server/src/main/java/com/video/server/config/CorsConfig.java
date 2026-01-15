package com.video.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 跨域配置
 */
@Configuration
public class CorsConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        
        // 使用OriginPattern支持所有源（开发环境）
        // 注意：使用addAllowedOriginPattern时，allowCredentials必须为false
        // 由于我们使用JWT token（通过Authorization header传递），不需要Cookie，所以不需要credentials
        config.addAllowedOriginPattern("*");
        
        // 允许所有请求头（包括Authorization）
        config.addAllowedHeader("*");
        
        // 允许所有请求方法（GET, POST, PUT, DELETE, OPTIONS等）
        config.addAllowedMethod("*");
        
        // 允许携带凭证（如果需要Cookie，需要改为false并使用addAllowedOrigin指定具体源）
        // 由于使用JWT token，这里设为false即可
        config.setAllowCredentials(false);
        
        // 预检请求的有效期，单位为秒
        config.setMaxAge(3600L);
        
        // 允许暴露的响应头
        config.addExposedHeader("*");
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        
        return new CorsFilter(source);
    }
}
