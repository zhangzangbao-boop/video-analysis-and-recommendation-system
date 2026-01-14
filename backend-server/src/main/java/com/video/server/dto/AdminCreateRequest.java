package com.video.server.dto;

import lombok.Data;

/**
 * 创建管理员请求DTO
 */
@Data
public class AdminCreateRequest {
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 密码
     */
    private String password;
    
    /**
     * 真实姓名
     */
    private String realName;
    
    /**
     * 手机号
     */
    private String phone;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 角色ID
     */
    private Long roleId;
}
