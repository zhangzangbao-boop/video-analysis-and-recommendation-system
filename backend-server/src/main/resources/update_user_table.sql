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

-- 更新用户表，将status字段扩展为支持normal/frozen/muted
-- 注意：status字段保持tinyint类型，但前端使用status_str字段
-- status: 1=normal, 0=frozen, 2=muted

-- 添加统计字段的视图或扩展（这些可以通过JOIN查询获得）
-- fans_count, video_count, like_count 可以从相关表统计得出
