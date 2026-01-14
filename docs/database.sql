/*
 Project: 基于Spark的短视频智能分析系统
 Description: 全量表结构 V2.0 (包含用户画像、操作审计、关注关系、话题与通知)
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 创建数据库
-- ----------------------------
CREATE DATABASE IF NOT EXISTS `short_video_platform` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `short_video_platform`;

-- =================================================================
-- 模块一：用户权限与安全
-- =================================================================

-- ----------------------------
-- Table structure for sys_user (已扩展画像与资产字段)
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID (自增)',
                            `username` varchar(64) NOT NULL COMMENT '用户名/账号',
                            `password` varchar(128) NOT NULL COMMENT '加密后的密码',
                            `salt` varchar(32) DEFAULT NULL COMMENT '加密盐值',
                            `phone` varchar(255) DEFAULT NULL COMMENT '手机号(AES加密存储)',
                            `nickname` varchar(64) DEFAULT NULL COMMENT '昵称',
                            `avatar_url` varchar(512) DEFAULT NULL COMMENT '头像地址(MinIO URL)',

    -- [NEW] 新增画像与资产字段
                            `real_name` varchar(64) DEFAULT NULL COMMENT '真实姓名',
                            `email` varchar(128) DEFAULT NULL COMMENT '邮箱',
                            `gender` tinyint DEFAULT '0' COMMENT '性别: 0-未知, 1-男, 2-女',
                            `level` int DEFAULT '1' COMMENT '用户等级',
                            `balance` decimal(10,2) DEFAULT '0.00' COMMENT '钱包余额',
                            `points` int DEFAULT '0' COMMENT '积分',
                            `fans_count` int DEFAULT '0' COMMENT '粉丝数',
                            `follow_count` int DEFAULT '0' COMMENT '关注数',
                            `signature` varchar(255) DEFAULT NULL COMMENT '个性签名(Bio)',

                            `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 1-正常, 0-冻结/封禁',
                            `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除: 0-未删除, 1-已删除',
                            `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
                            `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `uk_username` (`username`),
                            UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB AUTO_INCREMENT=10000 DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
                            `role_code` varchar(32) NOT NULL COMMENT '角色编码',
                            `role_name` varchar(32) NOT NULL COMMENT '角色名称',
                            `description` varchar(128) DEFAULT NULL COMMENT '描述',
                            `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
                                 `id` bigint NOT NULL AUTO_INCREMENT,
                                 `user_id` bigint NOT NULL COMMENT '用户ID',
                                 `role_id` bigint NOT NULL COMMENT '角色ID',
                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `uk_user_role` (`user_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- ----------------------------
-- Table structure for sys_login_log (登录日志)
-- ----------------------------
DROP TABLE IF EXISTS `sys_login_log`;
CREATE TABLE `sys_login_log` (
                                 `id` bigint NOT NULL AUTO_INCREMENT,
                                 `user_id` bigint DEFAULT NULL COMMENT '用户ID',
                                 `username` varchar(64) DEFAULT NULL COMMENT '尝试登录的用户名',
                                 `ip_address` varchar(64) DEFAULT NULL COMMENT '操作IP',
                                 `device_info` varchar(128) DEFAULT NULL COMMENT '设备信息',
                                 `status` tinyint NOT NULL COMMENT '登录状态: 1-成功, 0-失败',
                                 `msg` varchar(255) DEFAULT NULL COMMENT '结果描述',
                                 `login_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
                                 PRIMARY KEY (`id`),
                                 KEY `idx_user_time` (`user_id`,`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户登录审计日志';

-- ----------------------------
-- Table structure for sys_oper_log (操作/审计日志) [NEW]
-- ----------------------------
DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log` (
                                `id` bigint NOT NULL AUTO_INCREMENT,
                                `module` varchar(32) NOT NULL COMMENT '模块标题',
                                `business_type` int DEFAULT '0' COMMENT '业务类型(0其它 1新增 2修改 3删除)',
                                `method` varchar(128) DEFAULT NULL COMMENT '方法名称',
                                `operator_name` varchar(64) DEFAULT NULL COMMENT '操作人员',
                                `oper_url` varchar(255) DEFAULT NULL COMMENT '请求URL',
                                `oper_ip` varchar(128) DEFAULT NULL COMMENT '主机地址',
                                `oper_param` varchar(2000) DEFAULT NULL COMMENT '请求参数',
                                `status` int DEFAULT '0' COMMENT '操作状态(0正常 1异常)',
                                `error_msg` varchar(2000) DEFAULT NULL COMMENT '错误消息',
                                `oper_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
                                PRIMARY KEY (`id`),
                                KEY `idx_oper_time` (`oper_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志记录';

-- ----------------------------
-- Table structure for sys_notice (系统通知) [NEW]
-- ----------------------------
DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice` (
                              `id` bigint NOT NULL AUTO_INCREMENT,
                              `title` varchar(128) NOT NULL COMMENT '通知标题',
                              `content` text NOT NULL COMMENT '通知内容(支持HTML)',
                              `type` tinyint DEFAULT '1' COMMENT '类型: 1-系统通知, 2-活动通知, 3-维护公告',
                              `status` tinyint DEFAULT '1' COMMENT '状态: 1-正常, 0-关闭',
                              `create_by` varchar(64) DEFAULT NULL COMMENT '发布者',
                              `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                              PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统通知公告表';


-- =================================================================
-- 模块二：视频内容管理
-- =================================================================

-- ----------------------------
-- Table structure for video_category
-- ----------------------------
DROP TABLE IF EXISTS `video_category`;
CREATE TABLE `video_category` (
                                  `id` int NOT NULL AUTO_INCREMENT,
                                  `name` varchar(32) NOT NULL COMMENT '分类名称',
                                  `code` varchar(32) DEFAULT NULL COMMENT '编码',
                                  `sort` int DEFAULT '0' COMMENT '排序',
                                  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频分类字典表';

-- ----------------------------
-- Table structure for video_topic (话题/标签) [NEW]
-- ----------------------------
DROP TABLE IF EXISTS `video_topic`;
CREATE TABLE `video_topic` (
                               `id` bigint NOT NULL AUTO_INCREMENT,
                               `name` varchar(64) NOT NULL COMMENT '话题名称(如 #生活小技巧)',
                               `view_count` bigint DEFAULT '0' COMMENT '话题相关视频浏览量',
                               `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                               PRIMARY KEY (`id`),
                               UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频话题表';

-- ----------------------------
-- Table structure for video_info
-- ----------------------------
DROP TABLE IF EXISTS `video_info`;
CREATE TABLE `video_info` (
                              `id` bigint NOT NULL COMMENT '视频ID (Java端雪花算法生成)',
                              `user_id` bigint NOT NULL COMMENT '发布者ID',
                              `title` varchar(128) NOT NULL COMMENT '视频标题',
                              `description` varchar(512) DEFAULT NULL COMMENT '视频简介',
                              `video_url` varchar(512) NOT NULL COMMENT 'MinIO文件地址',
                              `cover_url` varchar(512) DEFAULT NULL COMMENT '封面地址',
                              `category_id` int DEFAULT NULL COMMENT '分类ID',
                              `tags` varchar(255) DEFAULT NULL COMMENT '标签/话题 (逗号分隔)',
                              `duration` int DEFAULT '0' COMMENT '时长(秒)',
                              `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态: 0-待审核, 1-已发布, 2-驳回',
                              `audit_msg` varchar(255) DEFAULT NULL COMMENT '审核意见',
                              `is_hot` tinyint DEFAULT '0' COMMENT '是否热门: 0-否, 1-是',
                              `view_count` bigint DEFAULT '0' COMMENT '播放量',
                              `like_count` bigint DEFAULT '0' COMMENT '点赞数',
                              `comment_count` bigint DEFAULT '0' COMMENT '评论数',
                              `share_count` bigint DEFAULT '0' COMMENT '转发数',
                              `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
                              `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                              `is_deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
                              PRIMARY KEY (`id`),
                              KEY `idx_user_id` (`user_id`),
                              KEY `idx_status_create` (`status`,`create_time`),
                              KEY `idx_hot` (`is_hot`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频信息表';


-- =================================================================
-- 模块三：互动与评论
-- =================================================================

-- ----------------------------
-- Table structure for sys_user_follow (用户关注) [NEW]
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_follow`;
CREATE TABLE `sys_user_follow` (
                                   `id` bigint NOT NULL AUTO_INCREMENT,
                                   `user_id` bigint NOT NULL COMMENT '粉丝ID (谁关注)',
                                   `follow_user_id` bigint NOT NULL COMMENT '被关注者ID (关注了谁)',
                                   `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                                   PRIMARY KEY (`id`),
                                   UNIQUE KEY `uk_follow` (`user_id`, `follow_user_id`),
                                   KEY `idx_follow_user` (`follow_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户关注关系表';

-- ----------------------------
-- Table structure for video_interaction
-- ----------------------------
DROP TABLE IF EXISTS `video_interaction`;
CREATE TABLE `video_interaction` (
                                     `id` bigint NOT NULL AUTO_INCREMENT,
                                     `user_id` bigint NOT NULL COMMENT '用户ID',
                                     `video_id` bigint NOT NULL COMMENT '视频ID',
                                     `type` tinyint NOT NULL COMMENT '类型: 1-点赞, 2-收藏, 3-转发, 4-不感兴趣',
                                     `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
                                     PRIMARY KEY (`id`),
                                     UNIQUE KEY `uk_user_video_type` (`user_id`,`video_id`,`type`),
                                     KEY `idx_video_type` (`video_id`,`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频互动记录表';

-- ----------------------------
-- Table structure for video_play_record
-- ----------------------------
DROP TABLE IF EXISTS `video_play_record`;
CREATE TABLE `video_play_record` (
                                     `id` bigint NOT NULL AUTO_INCREMENT,
                                     `user_id` bigint NOT NULL COMMENT '用户ID',
                                     `video_id` bigint NOT NULL COMMENT '视频ID',
                                     `duration` int DEFAULT '0' COMMENT '观看时长(秒)',
                                     `progress` int DEFAULT '0' COMMENT '播放进度(%)',
                                     `is_finish` tinyint DEFAULT '0' COMMENT '是否完播: 0-否, 1-是',
                                     `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '观看时间',
                                     PRIMARY KEY (`id`),
                                     KEY `idx_user_time` (`user_id`,`create_time`),
                                     KEY `idx_video_time` (`video_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频播放历史记录表';

-- ----------------------------
-- Table structure for video_comment
-- ----------------------------
DROP TABLE IF EXISTS `video_comment`;
CREATE TABLE `video_comment` (
                                 `id` bigint NOT NULL COMMENT '评论ID (Java端雪花算法生成)',
                                 `video_id` bigint NOT NULL COMMENT '视频ID',
                                 `user_id` bigint NOT NULL COMMENT '评论者ID',
                                 `content` varchar(1024) NOT NULL COMMENT '评论内容',
                                 `parent_id` bigint DEFAULT '0' COMMENT '父评论ID',
                                 `reply_user_id` bigint DEFAULT NULL COMMENT '被回复者ID',
                                 `like_count` int DEFAULT '0' COMMENT '点赞数',
                                 `status` tinyint DEFAULT '1' COMMENT '状态: 1-正常, 0-待审',
                                 `is_deleted` tinyint DEFAULT '0',
                                 `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                                 PRIMARY KEY (`id`),
                                 KEY `idx_video_status` (`video_id`,`status`),
                                 KEY `idx_reply_user` (`reply_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频评论表';


-- =================================================================
-- 模块四：离线统计与看板
-- =================================================================

-- ----------------------------
-- Table structure for sys_statistics_daily
-- ----------------------------
DROP TABLE IF EXISTS `sys_statistics_daily`;
CREATE TABLE `sys_statistics_daily` (
                                        `id` bigint NOT NULL AUTO_INCREMENT,
                                        `stat_date` date NOT NULL COMMENT '统计日期',
                                        `metric_name` varchar(64) NOT NULL COMMENT '指标: dau, video_new, etc.',
                                        `metric_value` decimal(20,2) NOT NULL DEFAULT '0.00' COMMENT '指标值',
                                        `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                                        PRIMARY KEY (`id`),
                                        UNIQUE KEY `uk_date_metric` (`stat_date`,`metric_name`),
                                        KEY `idx_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日核心数据统计表';

-- ----------------------------
-- Table structure for sys_statistics_category
-- ----------------------------
DROP TABLE IF EXISTS `sys_statistics_category`;
CREATE TABLE `sys_statistics_category` (
                                           `id` bigint NOT NULL AUTO_INCREMENT,
                                           `stat_date` date NOT NULL COMMENT '统计日期',
                                           `category_id` int NOT NULL COMMENT '分类ID',
                                           `video_count` int DEFAULT '0' COMMENT '视频存量',
                                           `play_count` bigint DEFAULT '0' COMMENT '今日播放量',
                                           `avg_duration` int DEFAULT '0' COMMENT '平均停留时长',
                                           `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                                           PRIMARY KEY (`id`),
                                           UNIQUE KEY `uk_date_cat` (`stat_date`,`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频分类每日统计表';


-- =================================================================
-- 5. 基础数据初始化
-- =================================================================

-- 初始化角色
INSERT INTO `sys_role` (`role_code`, `role_name`, `description`) VALUES
                                                                     ('ROLE_USER', '普通用户', '拥有观看、互动、上传权限'),
                                                                     ('ROLE_ADMIN', '管理员', '拥有内容审核、用户管理、系统配置权限');

-- 初始化视频分类
INSERT INTO `video_category` (`name`, `code`, `sort`) VALUES
                                                          ('搞笑', 'funny', 1),
                                                          ('生活', 'life', 2),
                                                          ('科技', 'tech', 3),
                                                          ('游戏', 'game', 4),
                                                          ('美食', 'food', 5),
                                                          ('萌宠', 'pet', 6);

-- 初始化话题
INSERT INTO `video_topic` (`name`, `view_count`) VALUES
                                                     ('#热门挑战', 1000), ('#日常生活', 500);

-- 初始化系统通知
INSERT INTO `sys_notice` (`title`, `content`, `type`, `create_by`) VALUES
    ('系统维护通知', '系统将于今晚24:00进行升级维护。', 1, 'admin');

-- 初始化系统管理员 (假设密码 'admin123' 的Hash)
INSERT INTO `sys_user` (`username`, `password`, `nickname`, `status`, `level`, `points`) VALUES
    ('admin', 'e10adc3949ba59abbe56e057f20f883e', '系统管理员', 1, 99, 9999);

-- 绑定管理员权限
INSERT INTO `sys_user_role` (`user_id`, `role_id`)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE u.username = 'admin' AND r.role_code = 'ROLE_ADMIN';

SET FOREIGN_KEY_CHECKS = 1;