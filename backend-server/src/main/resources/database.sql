
/*
 Navicat Premium Data Transfer
 Source Database       : short_video_platform
 Target Server Type    : MySQL
 Target Server Version : 80020
 File Encoding         : 65001

 Date: 2024-01-01
 Project: 基于Spark的短视频智能分析系统
 Description: 包含用户、视频、交互、统计、智能推荐全量表结构 (已优化索引与ID策略)
 Version: 3.1 - 最终完整版（包含所有表结构+推荐模块+初始化数据）
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =================================================================
-- 1. 创建数据库
-- =================================================================
CREATE DATABASE IF NOT EXISTS `short_video_platform` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `short_video_platform`;

-- =================================================================
-- 2. 清理原有数据库表（按依赖关系逆序删除，包含新增的推荐相关表）
-- =================================================================
-- 说明：在执行创建表之前，先删除所有可能存在的表
-- 注意：此操作会清空所有数据，请谨慎使用！

-- 删除用户通知相关表
DROP TABLE IF EXISTS `sys_user_notice`;
DROP TABLE IF EXISTS `sys_notice`;

-- 删除推荐相关核心表【新增】
DROP TABLE IF EXISTS `recommendation_result`;
DROP TABLE IF EXISTS `model_params`;

-- 删除统计相关表
DROP TABLE IF EXISTS `sys_statistics_category`;
DROP TABLE IF EXISTS `sys_statistics_daily`;

-- 删除评论和互动相关表
DROP TABLE IF EXISTS `video_comment`;
DROP TABLE IF EXISTS `video_play_record`;
DROP TABLE IF EXISTS `video_interaction`;
DROP TABLE IF EXISTS `sys_user_follow`;

-- 删除视频相关表
DROP TABLE IF EXISTS `video_topic_relation`;
DROP TABLE IF EXISTS `video_topic`;
DROP TABLE IF EXISTS `video_info`;
DROP TABLE IF EXISTS `video_category`;

-- 删除日志相关表
DROP TABLE IF EXISTS `sys_operation_log`;
DROP TABLE IF EXISTS `sys_login_log`;

-- 删除用户权限相关表
DROP TABLE IF EXISTS `sys_user_role`;
DROP TABLE IF EXISTS `sys_role`;
DROP TABLE IF EXISTS `sys_user`;

-- 删除废弃的表（如果存在）
DROP TABLE IF EXISTS `user_behavior`;
DROP TABLE IF EXISTS `sys_admin`;

-- =================================================================
-- 3. 创建表结构 (原所有表 + 新增推荐模块2张核心表)
-- =================================================================

-- =================================================================
-- 模块一：用户权限与安全 (ID策略: 数据库自增)
-- =================================================================

-- ----------------------------
-- Table structure for sys_user (系统用户表 - 已扩展画像与资产字段)
-- 说明：采用RBAC模式，管理员是拥有ROLE_ADMIN角色的用户，不需要单独的sys_admin表
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

    -- [NEW] 用户画像字段
                            `real_name` varchar(64) DEFAULT NULL COMMENT '真实姓名',
                            `email` varchar(128) DEFAULT NULL COMMENT '电子邮箱',
                            `gender` varchar(10) DEFAULT NULL COMMENT '性别: male/female',
                            `bio` varchar(255) DEFAULT NULL COMMENT '个人简介/个性签名',

    -- [NEW] 用户资产字段
                            `level` tinyint DEFAULT 1 COMMENT '用户等级: 1-3',
                            `balance` decimal(10,2) DEFAULT 0.00 COMMENT '账户余额',
                            `points` int DEFAULT 0 COMMENT '积分',

    -- [NEW] 用户统计字段（冗余字段，可通过sys_user_follow表统计）
                            `fans_count` int DEFAULT 0 COMMENT '粉丝数',
                            `follow_count` int DEFAULT 0 COMMENT '关注数',

    -- 状态字段
                            `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 1-正常, 0-冻结/封禁, 2-禁言',
                            `status_str` varchar(20) DEFAULT 'normal' COMMENT '状态字符串: normal/frozen/muted',
                            `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除: 0-未删除, 1-已删除',

    -- 时间字段
                            `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
                            `last_login` datetime DEFAULT NULL COMMENT '最后登录时间',
                            `ip_address` varchar(64) DEFAULT NULL COMMENT 'IP地址',
                            `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

                            PRIMARY KEY (`id`),
                            UNIQUE KEY `uk_username` (`username`),
                            UNIQUE KEY `uk_phone` (`phone`),
                            KEY `idx_status` (`status`),
                            KEY `idx_level` (`level`)
) ENGINE=InnoDB AUTO_INCREMENT=10000 DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- ----------------------------
-- Table structure for sys_role (角色表)
-- 说明：RBAC模式的核心表，定义系统角色
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID (自增)',
                            `role_code` varchar(32) NOT NULL COMMENT '角色编码',
                            `role_name` varchar(32) NOT NULL COMMENT '角色名称',
                            `description` varchar(128) DEFAULT NULL COMMENT '描述',
                            `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- ----------------------------
-- Table structure for sys_user_role (用户角色关联表)
-- 说明：实现RBAC模式，通过此表关联用户和角色
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
                                 `id` bigint NOT NULL AUTO_INCREMENT,
                                 `user_id` bigint NOT NULL COMMENT '用户ID',
                                 `role_id` bigint NOT NULL COMMENT '角色ID',
                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `uk_user_role` (`user_id`,`role_id`),
                                 KEY `idx_user_id` (`user_id`),
                                 KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- ----------------------------
-- Table structure for sys_login_log (用户登录审计日志表)
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
                                 KEY `idx_user_time` (`user_id`,`login_time`),
                                 KEY `idx_login_time` (`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户登录审计日志';

-- ----------------------------
-- Table structure for sys_operation_log (系统操作日志表)
-- ----------------------------
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

-- =================================================================
-- 模块二：视频内容管理 (ID策略: 雪花算法 & 自增混合)
-- =================================================================

-- ----------------------------
-- Table structure for video_category (视频分类字典表 - 用自增ID)
-- ----------------------------
DROP TABLE IF EXISTS `video_category`;
CREATE TABLE `video_category` (
                                  `id` int NOT NULL AUTO_INCREMENT,
                                  `name` varchar(32) NOT NULL COMMENT '分类名称',
                                  `code` varchar(32) DEFAULT NULL COMMENT '编码',
                                  `sort` int DEFAULT '0' COMMENT '排序',
                                  PRIMARY KEY (`id`),
                                  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频分类字典表';

-- ----------------------------
-- Table structure for video_info (视频信息表 - 核心表用雪花算法)
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
                              `tags` varchar(255) DEFAULT NULL COMMENT '标签',
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
                              KEY `idx_hot` (`is_hot`),
                              KEY `idx_category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频信息表';

-- ----------------------------
-- Table structure for video_topic (热门话题表)
-- ----------------------------
DROP TABLE IF EXISTS `video_topic`;
CREATE TABLE `video_topic` (
                               `id` bigint NOT NULL AUTO_INCREMENT COMMENT '话题ID',
                               `topic_name` varchar(64) NOT NULL COMMENT '话题名称',
                               `topic_desc` varchar(255) DEFAULT NULL COMMENT '话题描述',
                               `cover_url` varchar(512) DEFAULT NULL COMMENT '话题封面',
                               `video_count` int DEFAULT 0 COMMENT '关联视频数',
                               `view_count` bigint DEFAULT 0 COMMENT '总播放量',
                               `is_hot` tinyint DEFAULT 0 COMMENT '是否热门: 0-否, 1-是',
                               `sort_order` int DEFAULT 0 COMMENT '排序',
                               `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                               `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                               PRIMARY KEY (`id`),
                               UNIQUE KEY `uk_topic_name` (`topic_name`),
                               KEY `idx_hot` (`is_hot`),
                               KEY `idx_sort` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='热门话题表';

-- ----------------------------
-- Table structure for video_topic_relation (视频话题关联表)
-- ----------------------------
DROP TABLE IF EXISTS `video_topic_relation`;
CREATE TABLE `video_topic_relation` (
                                        `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                        `video_id` bigint NOT NULL COMMENT '视频ID',
                                        `topic_id` bigint NOT NULL COMMENT '话题ID',
                                        `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '关联时间',
                                        PRIMARY KEY (`id`),
                                        UNIQUE KEY `uk_video_topic` (`video_id`, `topic_id`),
                                        KEY `idx_topic_id` (`topic_id`),
                                        KEY `idx_video_id` (`video_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频话题关联表';

-- =================================================================
-- 模块三：互动与评论 (ID策略: 雪花算法 & 自增混合)
-- =================================================================

-- ----------------------------
-- Table structure for sys_user_follow (用户关注关系表)
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_follow`;
CREATE TABLE `sys_user_follow` (
                                   `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                   `user_id` bigint NOT NULL COMMENT '粉丝ID (谁关注)',
                                   `follow_user_id` bigint NOT NULL COMMENT '被关注者ID (关注了谁)',
                                   `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
                                   PRIMARY KEY (`id`),
                                   UNIQUE KEY `uk_follow` (`user_id`, `follow_user_id`),
                                   KEY `idx_follow_user` (`follow_user_id`),
                                   KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户关注关系表';

-- ----------------------------
-- Table structure for video_interaction (视频互动记录表 - 流水表用自增)
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
                                     KEY `idx_video_type` (`video_id`,`type`),
                                     KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频互动记录表';

-- ----------------------------
-- Table structure for video_play_record (视频播放历史记录表 - 日志表用自增)
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
-- Table structure for video_comment (视频评论表 - 核心内容用雪花算法)
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
                                 KEY `idx_reply_user` (`reply_user_id`),
                                 KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频评论表';

-- =================================================================
-- 模块四：离线统计与看板 (ID策略: 自增)
-- =================================================================

-- ----------------------------
-- Table structure for sys_statistics_daily (每日核心数据统计表)
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
-- Table structure for sys_statistics_category (视频分类每日统计表)
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
-- 模块五：系统通知 (ID策略: 自增)
-- =================================================================

-- ----------------------------
-- Table structure for sys_notice (系统通知表)
-- ----------------------------
DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice` (
                              `id` bigint NOT NULL AUTO_INCREMENT COMMENT '通知ID',
                              `title` varchar(128) NOT NULL COMMENT '通知标题',
                              `content` text COMMENT '通知内容',
                              `type` tinyint DEFAULT 1 COMMENT '通知类型: 1-系统公告, 2-活动通知, 3-系统维护',
                              `target_type` tinyint DEFAULT 0 COMMENT '目标类型: 0-全部用户, 1-指定用户',
                              `target_user_id` bigint DEFAULT NULL COMMENT '目标用户ID (target_type=1时有效)',
                              `status` tinyint DEFAULT 1 COMMENT '状态: 0-草稿, 1-已发布, 2-已撤回',
                              `is_top` tinyint DEFAULT 0 COMMENT '是否置顶: 0-否, 1-是',
                              `read_count` int DEFAULT 0 COMMENT '阅读数',
                              `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
                              `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                              `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                              PRIMARY KEY (`id`),
                              KEY `idx_status_publish` (`status`, `publish_time`),
                              KEY `idx_target_user` (`target_user_id`),
                              KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统通知表';

-- ----------------------------
-- Table structure for sys_user_notice (用户通知阅读记录表)
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_notice`;
CREATE TABLE `sys_user_notice` (
                                   `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                   `user_id` bigint NOT NULL COMMENT '用户ID',
                                   `notice_id` bigint NOT NULL COMMENT '通知ID',
                                   `is_read` tinyint DEFAULT 0 COMMENT '是否已读: 0-未读, 1-已读',
                                   `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
                                   `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                   PRIMARY KEY (`id`),
                                   UNIQUE KEY `uk_user_notice` (`user_id`, `notice_id`),
                                   KEY `idx_user_read` (`user_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户通知阅读记录表';

-- ----------------------------
-- Table structure for user_message (用户消息表)
-- ----------------------------
DROP TABLE IF EXISTS `user_message`;
CREATE TABLE `user_message` (
                                `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID',
                                `user_id` bigint NOT NULL COMMENT '接收消息的用户ID',
                                `type` varchar(32) NOT NULL COMMENT '消息类型: LIKE-点赞, COMMENT-评论, COLLECT-收藏, FOLLOW-关注, SYSTEM-系统通知',
                                `title` varchar(128) DEFAULT NULL COMMENT '消息标题',
                                `content` text COMMENT '消息内容',
                                `related_user_id` bigint DEFAULT NULL COMMENT '相关用户ID（发送消息的用户，如点赞者、评论者等）',
                                `related_video_id` bigint DEFAULT NULL COMMENT '相关视频ID（如点赞的视频、评论的视频等）',
                                `related_comment_id` bigint DEFAULT NULL COMMENT '相关评论ID（如回复的评论）',
                                `related_notice_id` bigint DEFAULT NULL COMMENT '相关系统通知ID（如果是系统通知）',
                                `is_read` tinyint DEFAULT 0 COMMENT '是否已读: 0-未读, 1-已读',
                                `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
                                `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                PRIMARY KEY (`id`),
                                KEY `idx_user_type` (`user_id`, `type`),
                                KEY `idx_user_read` (`user_id`, `is_read`),
                                KEY `idx_create_time` (`create_time`),
                                KEY `idx_related_user` (`related_user_id`),
                                KEY `idx_related_video` (`related_video_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户消息表（点赞、评论、收藏、关注、系统通知等）';

-- =================================================================
-- ✅ 模块六：智能推荐核心模块 (新增！ID策略: 数据库自增)
-- =================================================================
-- ----------------------------
-- Table structure for recommendation_result (离线和实时推荐结果统一存储表)
-- 核心说明：承接Spark ALS协同过滤/实时行为推荐的最终结果，前端直接查询该表展示推荐列表
-- ----------------------------
DROP TABLE IF EXISTS `recommendation_result`;
CREATE TABLE `recommendation_result` (
                                         id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
                                         user_id BIGINT NOT NULL COMMENT '用户ID',
                                         video_id BIGINT NOT NULL COMMENT '视频ID(原脚本movie_id修正为video_id，与业务统一)',
                                         score DOUBLE NOT NULL COMMENT '推荐分数 (0-1)，分数越高推荐优先级越高',
                                         `rank` INT NOT NULL COMMENT '推荐排名 (1-20)，每个用户固定返回TOP20推荐结果',
                                         `type` VARCHAR(20) NOT NULL COMMENT '推荐类型: OFFLINE-离线推荐/REALTIME-实时推荐',
                                         reason VARCHAR(200) COMMENT '推荐理由（可选：如基于协同过滤/基于您的观看历史/热门推荐）',
                                         create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间/推荐结果生成时间',
                                         update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                         model_id VARCHAR(50) COMMENT '模型ID，关联model_params表，溯源推荐结果的生成模型',
    -- 索引深度优化：贴合业务查询场景
                                         UNIQUE KEY `uk_user_video_type` (user_id, video_id, `type`) COMMENT '用户+视频+推荐类型 唯一，防止重复推荐',
                                         INDEX idx_user_type (user_id, `type`) COMMENT '高频查询：查询指定用户的离线/实时推荐列表',
                                         INDEX idx_score (score DESC) COMMENT '按推荐分数降序排序，提升榜单查询速度',
                                         INDEX idx_create_time (create_time) COMMENT '按时间筛选最新推荐结果'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短视频推荐结果统一存储表';

-- ----------------------------
-- Table structure for model_params (ALS模型训练记录&参数表)
-- 核心说明：存储Spark ALS协同过滤模型的训练参数+评估指标，用于模型版本管理、效果溯源、参数调优
-- ----------------------------
DROP TABLE IF EXISTS `model_params`;
CREATE TABLE `model_params` (
                                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                model_id VARCHAR(50) NOT NULL COMMENT '模型唯一标识(如ALS_20260116_V1)',
                                `rank` INT NOT NULL COMMENT 'ALS隐因子数量，核心调优参数',
                                reg_param DOUBLE NOT NULL COMMENT 'ALS正则化参数，防止过拟合',
                                max_iter INT NOT NULL COMMENT 'ALS最大迭代次数',
                                training_time DATETIME COMMENT '模型训练完成时间',
                                model_path VARCHAR(200) COMMENT '模型存储路径(HDFS/本地路径)',
                                rmse DOUBLE COMMENT '模型评估指标-均方根误差，越小代表模型效果越好',
                                `status` VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '模型状态: ACTIVE-生效中/DEPRECATED-已废弃',
                                create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '模型创建时间',
    -- 索引优化
                                UNIQUE KEY uk_model_id (model_id) COMMENT '模型ID唯一，防止重复训练',
                                INDEX idx_status (`status`) COMMENT '筛选生效中的模型',
                                INDEX idx_create_time (create_time) COMMENT '按训练时间排序查看模型迭代记录'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ALS协同过滤模型训练参数记录表';

-- =================================================================
-- 5. 基础数据初始化 (完整保留原初始化数据)
-- =================================================================

-- ----------------------------
-- 初始化角色 (仅两类)
-- ----------------------------
INSERT INTO `sys_role` (`role_code`, `role_name`, `description`) VALUES
                                                                     ('ROLE_USER', '普通用户', '拥有观看、互动、上传权限'),
                                                                     ('ROLE_ADMIN', '管理员', '拥有内容审核、用户管理、系统配置权限');

-- ----------------------------
-- 初始化视频分类
-- ----------------------------
INSERT INTO `video_category` (`name`, `code`, `sort`) VALUES
                                                          ('搞笑', 'funny', 1),
                                                          ('生活', 'life', 2),
                                                          ('科技', 'tech', 3),
                                                          ('游戏', 'game', 4),
                                                          ('美食', 'food', 5),
                                                          ('萌宠', 'pet', 6),
                                                          ('影视', 'movie', 7),
                                                          ('音乐', 'music', 8),
                                                          ('舞蹈', 'dance', 9),
                                                          ('体育', 'sports', 10),
                                                          ('时尚', 'fashion', 11),
                                                          ('旅行', 'travel', 12),
                                                          ('知识', 'knowledge', 13),
                                                          ('动漫', 'anime', 14);
                                                          

-- ----------------------------
-- 初始化系统管理员
-- 说明：采用RBAC模式，管理员是sys_user表中拥有ROLE_ADMIN角色的用户
-- 密码：123456 (MD5: e10adc3949ba59abbe56e057f20f883e)
-- ----------------------------
INSERT INTO `sys_user` (`username`, `password`, `nickname`, `status`, `level`, `points`) VALUES
    ('admin', 'e10adc3949ba59abbe56e057f20f883e', '系统管理员', 1, 99, 9999);

-- 绑定管理员权限
INSERT INTO `sys_user_role` (`user_id`, `role_id`)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE u.username = 'admin' AND r.role_code = 'ROLE_ADMIN';

-- =================================================================
-- 6. 数据库迁移脚本
-- =================================================================
ALTER TABLE `sys_user`
    ADD COLUMN `ip_address` varchar(64) DEFAULT NULL COMMENT 'IP地址'
AFTER `last_login`;

SET FOREIGN_KEY_CHECKS = 1;
