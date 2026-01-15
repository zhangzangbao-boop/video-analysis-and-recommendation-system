package com.video.server.dto;

import lombok.Data;

/**
 * 创建用户请求DTO
 */
@Data
public class UserCreateRequest {
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 密码
     */
    private String password;
    
    /**
     * 手机号
     */
    private String phone;
    
    /**
     * 昵称
     */
    private String nickname;
    
    /**
     * 邮箱
     */
    private String email;
}
