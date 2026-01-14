package com.video.server.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体类
 */
@Data
public class User {
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 用户名/账号
     */
    private String username;
    
    /**
     * 密码（加密后）
     */
    private String password;
    
    /**
     * 加密盐值
     */
    private String salt;
    
    /**
     * 手机号
     */
    private String phone;
    
    /**
     * 昵称
     */
    private String nickname;
    
    /**
     * 头像地址
     */
    private String avatarUrl;
    
    /**
     * 用户等级：1-3
     */
    private Integer level;
    
    /**
     * 账户余额
     */
    private java.math.BigDecimal balance;
    
    /**
     * 积分
     */
    private Integer points;
    
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
     * 个人简介
     */
    private String bio;
    
    /**
     * 状态：1-正常，0-冻结/封禁，2-禁言
     */
    private Integer status;
    
    /**
     * 状态字符串：normal/frozen/muted
     */
    private String statusStr;
    
    /**
     * 是否删除：0-未删除，1-已删除
     */
    private Integer isDeleted;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 最后登录时间
     */
    private LocalDateTime lastLogin;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
