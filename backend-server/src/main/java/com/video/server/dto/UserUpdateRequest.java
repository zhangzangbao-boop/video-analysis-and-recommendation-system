package com.video.server.dto;

import lombok.Data;

/**
 * 更新用户信息请求DTO
 */
@Data
public class UserUpdateRequest {
    
    /**
     * 昵称
     */
    private String nickname;
    
    /**
     * 头像地址
     */
    private String avatarUrl;
    
    /**
     * 真实姓名
     */
    private String realName;
    
    /**
     * 电子邮箱
     */
    private String email;
    
    /**
     * 性别：male/female
     */
    private String gender;
    
    /**
     * 个人简介/个性签名
     */
    private String bio;
}
