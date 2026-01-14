# 数据库更新说明

## 用户表更新

执行以下SQL脚本更新用户表结构：
```sql
-- 更新用户表，添加前端需要的字段
ALTER TABLE `sys_user` 
ADD COLUMN `level` tinyint DEFAULT 1 COMMENT '用户等级: 1-3' AFTER `avatar_url`,
ADD COLUMN `balance` decimal(10,2) DEFAULT 0.00 COMMENT '账户余额' AFTER `level`,
ADD COLUMN `points` int DEFAULT 0 COMMENT '积分' AFTER `balance`,
ADD COLUMN `real_name` varchar(64) DEFAULT NULL COMMENT '真实姓名' AFTER `nickname`,
ADD COLUMN `email` varchar(128) DEFAULT NULL COMMENT '电子邮箱' AFTER `real_name`,
ADD COLUMN `gender` varchar(10) DEFAULT NULL COMMENT '性别: male/female' AFTER `email`,
ADD COLUMN `bio` varchar(255) DEFAULT NULL COMMENT '个人简介' AFTER `gender`,
ADD COLUMN `last_login` datetime DEFAULT NULL COMMENT '最后登录时间' AFTER `create_time`,
ADD COLUMN `status_str` varchar(20) DEFAULT 'normal' COMMENT '状态字符串: normal/frozen/muted' AFTER `status`;
```

## 操作日志表

执行以下SQL脚本创建操作日志表：
```sql
-- 创建操作日志表
DROP TABLE IF EXISTS `sys_operation_log`;
CREATE TABLE `sys_operation_log` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `operator` varchar(64) NOT NULL COMMENT '操作人',
    `module` varchar(64) NOT NULL COMMENT '所属模块',
    `action` varchar(255) NOT NULL COMMENT '操作内容',
    `ip_address` varchar(64) DEFAULT NULL COMMENT '操作IP',
    `status` varchar(20) DEFAULT '成功' COMMENT '状态: 成功/失败',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_operator_time` (`operator`, `create_time`),
    KEY `idx_module_time` (`module`, `create_time`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统操作日志表';
```

## 注意事项

1. 用户表的status字段保持tinyint类型（1=normal, 0=frozen, 2=muted），但前端使用status_str字段
2. 视频表结构已包含所需字段（description, duration, comment_count, share_count等）
3. 所有新字段都设置了默认值，不会影响现有数据
