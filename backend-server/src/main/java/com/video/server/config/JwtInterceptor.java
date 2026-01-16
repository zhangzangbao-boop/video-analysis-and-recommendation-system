package com.video.server.config;

import com.video.server.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * JWT 拦截器
 * 从请求头中提取Token并解析用户ID，存储到request属性中
 */
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {
    
    private final JwtUtil jwtUtil;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 从请求头中获取Token
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                // 解析Token获取用户ID
                Long userId = jwtUtil.parseToken(token);
                // 将用户ID存储到request属性中，供Controller使用
                request.setAttribute("userId", userId);
            } catch (Exception e) {
                // Token解析失败，但不阻止请求（某些接口可能不需要认证）
                // 如果需要强制认证，可以在这里返回false
            }
        }
        
        return true;
    }
}
