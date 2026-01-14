package com.video.server.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员实体类
 */
@Data
public class Admin {
    
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
    
    /**
     * 状态：1-正常，0-禁用
     */
    private Integer status;
    
    /**
     * 是否删除：0-未删除，1-已删除
     */
    private Integer isDeleted;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
