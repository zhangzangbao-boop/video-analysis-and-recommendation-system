# 数据库脚本说明

## 主要文件

### database.sql（主文件）
**所有SQL语句已整合到此文件中，包含完整的表结构和初始化数据。**

此文件包含：
1. ✅ 所有表结构定义（用户、视频、互动、统计、通知等）
2. ✅ 所有字段说明和注释
3. ✅ 索引定义
4. ✅ 基础数据初始化
5. ✅ 废弃表说明

**建议：直接执行此文件即可完成数据库初始化。**

## 其他SQL文件（已整合，可删除或保留作为参考）

以下文件的内容已全部整合到 `database.sql` 中：

- `update_user_table.sql` - 用户表字段更新（已整合）
- `create_operation_log_table.sql` - 操作日志表（已整合）
- `create_missing_tables.sql` - 缺失的表（已整合）
- `admin_table.sql` - 管理员表（已废弃，使用RBAC模式）
- `user_behavior_table.sql` - 用户行为表（已废弃，使用video_interaction表）

## 执行方式

### 方式一：直接执行主文件（推荐）
```bash
mysql -u root -p < database.sql
```

### 方式二：在MySQL客户端执行
```sql
source /path/to/database.sql;
```

### 方式三：如果数据库已存在，可以只执行部分表
如果某些表已存在，可以：
1. 先备份现有数据
2. 注释掉不需要的表创建语句
3. 只执行新增的表或字段

## 表结构说明

### 核心表
- `sys_user` - 系统用户表（包含画像和资产字段）
- `sys_role` - 角色表
- `sys_user_role` - 用户角色关联表（RBAC模式）
- `video_info` - 视频信息表
- `video_interaction` - 视频互动记录表

### 扩展表
- `sys_user_follow` - 用户关注关系表
- `video_topic` - 热门话题表
- `video_topic_relation` - 视频话题关联表
- `sys_notice` - 系统通知表
- `sys_user_notice` - 用户通知阅读记录表
- `sys_operation_log` - 系统操作日志表
- `sys_login_log` - 用户登录审计日志表

### 统计表
- `sys_statistics_daily` - 每日核心数据统计表
- `sys_statistics_category` - 视频分类每日统计表

## 重要说明

### 1. RBAC模式
- **不使用 `sys_admin` 表**
- 管理员是 `sys_user` 表中拥有 `ROLE_ADMIN` 角色的用户
- 通过 `sys_user_role` 表关联角色

### 2. 用户行为记录
- **不使用 `user_behavior` 表**
- 使用 `video_interaction` 表，性能更优
- 类型：1-点赞, 2-收藏, 3-转发, 4-不感兴趣

### 3. 冗余字段
- `sys_user.fans_count` 和 `sys_user.follow_count` 是冗余字段
- 可通过 `sys_user_follow` 表统计得出
- 建议：通过定时任务或触发器更新，提高查询性能

## 字段标注说明

所有SQL语句都包含详细的注释：
- `COMMENT` - 字段说明
- `-- [NEW]` - 新增字段标记
- `-- 说明：` - 表或字段的详细说明
- `-- 注意：` - 重要注意事项

## 版本信息

- **Version**: 3.0 - 完整版
- **Date**: 2024-01-01
- **包含**: 所有表结构和字段
