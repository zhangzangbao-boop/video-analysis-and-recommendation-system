package com.video.server.dto;

import lombok.Data;

/**
 * 修改密码请求DTO
 */
@Data
public class PasswordChangeRequest {
    
    /**
     * 旧密码
     */
    private String oldPassword;
    
    /**
     * 新密码
     */
    private String newPassword;
}
