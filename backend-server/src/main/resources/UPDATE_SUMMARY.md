# 数据库更新说明

## ⚠️ 重要更新

**所有SQL语句已整合到 `database.sql` 文件中！**

**建议：直接执行 `database.sql` 文件即可完成所有数据库初始化。**

其他SQL文件（`update_user_table.sql`、`create_operation_log_table.sql`、`create_missing_tables.sql`）的内容已全部整合到主文件中，可以删除或保留作为参考。

## 执行方式

### 推荐方式：执行主文件
```bash
mysql -u root -p < database.sql
```

### 如果数据库已存在
如果某些表已存在，可以：
1. 先备份现有数据
2. 注释掉不需要的表创建语句
3. 只执行新增的表或字段

## 旧版执行顺序（已整合，仅供参考）

以下内容已整合到 `database.sql` 中：
1. ~~`update_user_table.sql`~~ - 更新用户表（已整合）
2. ~~`create_operation_log_table.sql`~~ - 创建操作日志表（已整合）
3. ~~`create_missing_tables.sql`~~ - 创建缺失的表（已整合）

## 1. 用户表更新 (update_user_table.sql)

执行以下SQL脚本更新用户表结构，添加画像字段：
```sql
ALTER TABLE `sys_user` 
ADD COLUMN `level` tinyint DEFAULT 1 COMMENT '用户等级: 1-3' AFTER `avatar_url`,
ADD COLUMN `balance` decimal(10,2) DEFAULT 0.00 COMMENT '账户余额' AFTER `level`,
ADD COLUMN `points` int DEFAULT 0 COMMENT '积分' AFTER `balance`,
ADD COLUMN `real_name` varchar(64) DEFAULT NULL COMMENT '真实姓名' AFTER `nickname`,
ADD COLUMN `email` varchar(128) DEFAULT NULL COMMENT '电子邮箱' AFTER `real_name`,
ADD COLUMN `gender` varchar(10) DEFAULT NULL COMMENT '性别: male/female' AFTER `email`,
ADD COLUMN `bio` varchar(255) DEFAULT NULL COMMENT '个人简介/个性签名' AFTER `gender`,
ADD COLUMN `fans_count` int DEFAULT 0 COMMENT '粉丝数' AFTER `bio`,
ADD COLUMN `follow_count` int DEFAULT 0 COMMENT '关注数' AFTER `fans_count`,
ADD COLUMN `last_login` datetime DEFAULT NULL COMMENT '最后登录时间' AFTER `create_time`,
ADD COLUMN `status_str` varchar(20) DEFAULT 'normal' COMMENT '状态字符串: normal/frozen/muted' AFTER `status`;
```

## 2. 操作日志表 (create_operation_log_table.sql)

创建业务操作日志表（区别于登录日志）：
```sql
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

## 3. 缺失的表结构 (create_missing_tables.sql)

### 3.1 用户关注关系表 (sys_user_follow)
用于存储用户之间的关注关系，支持粉丝数和关注数统计。

### 3.2 热门话题表 (video_topic)
存储话题信息，支持话题封面、统计等。

### 3.3 视频话题关联表 (video_topic_relation)
关联视频和话题的多对多关系。

### 3.4 系统通知表 (sys_notice)
存储系统通知和公告，支持全部用户或指定用户。

### 3.5 用户通知阅读记录表 (sys_user_notice)
记录用户是否已读通知。

## 4. 废弃表说明 (deprecate_tables.sql)

### 4.1 user_behavior 表（已废弃）
- **原因**：功能与 `video_interaction` 表重复
- **建议**：使用 `video_interaction` 表，它使用 `tinyint` 存储类型，性能更优
- **类型映射**：
  - `video_interaction.type`: 1-点赞, 2-收藏, 3-转发, 4-不感兴趣

### 4.2 sys_admin 表（已废弃）
- **原因**：系统采用 RBAC 模式
- **建议**：管理员只是 `sys_user` 表中拥有 `ROLE_ADMIN` 角色的用户
- **实现**：通过 `sys_user_role` 表关联角色

## 注意事项

1. **用户表的status字段**：
   - 保持 `tinyint` 类型（1=normal, 0=frozen, 2=muted）
   - 前端使用 `status_str` 字段（normal/frozen/muted）

2. **fans_count 和 follow_count**：
   - 是冗余字段，可以通过 `sys_user_follow` 表统计得出
   - 建议：通过定时任务或触发器更新，以提高查询性能

3. **视频表结构**：
   - 已包含所需字段（description, duration, comment_count, share_count等）

4. **所有新字段**：
   - 都设置了默认值，不会影响现有数据

5. **架构统一性**：
   - 管理员管理应使用 RBAC 模式，不要单独创建 `sys_admin` 表
   - 用户行为应使用 `video_interaction` 表，不要使用 `user_behavior` 表

## 详细架构说明

请参考 `ARCHITECTURE_NOTES.md` 文件了解详细的架构设计说明。
