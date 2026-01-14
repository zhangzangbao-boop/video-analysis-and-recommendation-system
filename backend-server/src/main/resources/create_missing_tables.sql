-- =================================================================
-- 创建缺失的表结构
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
    KEY `idx_topic_id` (`topic_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频话题关联表';

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
