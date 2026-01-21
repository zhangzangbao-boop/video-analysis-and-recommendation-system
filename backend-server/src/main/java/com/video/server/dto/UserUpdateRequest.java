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
     * 性别：male/female (注意：Controller中接收的是Integer，这里建议与实体类保持一致，或者在Controller做转换。
     * 之前的代码中你使用的是 Integer gender，但这里定义的是 String。
     * 为了匹配 UserProfile.vue 前端传来的数据 (editForm.gender 是数字)，建议改为 Integer)
     */
    private Integer gender; // 修改建议：前端传的是数字(0/1/2)，这里改为Integer更好，如果必须用String请确保前后端一致

    /**
     * 个人简介/个性签名
     */
    private String bio;

    /**
     * 手机号 (新增)
     */
    private String phone;
}