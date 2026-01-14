-- 管理员表
DROP TABLE IF EXISTS `sys_admin`;
CREATE TABLE `sys_admin` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username` varchar(64) NOT NULL COMMENT '用户名/账号',
    `password` varchar(128) NOT NULL COMMENT '加密后的密码',
    `salt` varchar(32) DEFAULT NULL COMMENT '加密盐值',
    `real_name` varchar(64) DEFAULT NULL COMMENT '真实姓名',
    `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
    `email` varchar(128) DEFAULT NULL COMMENT '邮箱',
    `role_id` bigint DEFAULT NULL COMMENT '角色ID',
    `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 1-正常, 0-禁用',
    `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除: 0-未删除, 1-已删除',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';

-- 插入默认管理员账号（密码：123456）
INSERT INTO `sys_admin` (`username`, `password`, `salt`, `real_name`, `status`, `is_deleted`) 
VALUES ('admin', 'e10adc3949ba59abbe56e057f20f883e', 'default', '系统管理员', 1, 0);
