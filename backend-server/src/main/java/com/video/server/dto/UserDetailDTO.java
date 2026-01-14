package com.video.server.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户详情DTO（包含统计信息）
 */
@Data
public class UserDetailDTO {
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 用户名/账号
     */
    private String username;
    
    /**
     * 手机号
     */
    private String phone;
    
    /**
     * 昵称
     */
    private String nickname;
    
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
    private BigDecimal balance;
    
    /**
     * 积分
     */
    private Integer points;
    
    /**
     * 状态字符串：normal/frozen/muted
     */
    private String status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 最后登录时间
     */
    private LocalDateTime lastLogin;
    
    /**
     * 粉丝数
     */
    private Long fansCount;
    
    /**
     * 发布作品数
     */
    private Long videoCount;
    
    /**
     * 获赞总量
     */
    private Long likeCount;
}
