# 系统架构说明

## 用户权限管理（RBAC模式）

### 管理员管理
- **不使用单独的 `sys_admin` 表**
- 管理员是 `sys_user` 表中拥有 `ROLE_ADMIN` 角色的用户
- 通过 `sys_user_role` 表关联用户和角色
- 角色定义在 `sys_role` 表中：
  - `ROLE_USER`: 普通用户
  - `ROLE_ADMIN`: 管理员

### 如何创建管理员
```sql
-- 1. 创建用户（如果不存在）
INSERT INTO sys_user (username, password, nickname, status) 
VALUES ('admin', '加密后的密码', '系统管理员', 1);

-- 2. 关联管理员角色
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id 
FROM sys_user u, sys_role r
WHERE u.username = 'admin' AND r.role_code = 'ROLE_ADMIN';
```

## 用户行为记录

### 使用 video_interaction 表
- **不使用 `user_behavior` 表**（已废弃）
- `video_interaction` 表使用 `tinyint` 存储类型，性能更优
- 类型定义：
  - 1: 点赞
  - 2: 收藏
  - 3: 转发
  - 4: 不感兴趣

### 迁移建议
如果现有系统使用了 `user_behavior` 表，可以迁移数据：
```sql
-- 迁移 user_behavior 到 video_interaction
INSERT INTO video_interaction (user_id, video_id, type, create_time)
SELECT 
    user_id,
    video_id,
    CASE action_type
        WHEN 'like' THEN 1
        WHEN 'collect' THEN 2
        WHEN 'share' THEN 3
        WHEN 'dislike' THEN 4
        ELSE 1
    END AS type,
    create_time
FROM user_behavior;
```

## 新增表结构

### 1. sys_user_follow（用户关注关系表）
- 存储用户之间的关注关系
- 用于统计粉丝数和关注数

### 2. video_topic（热门话题表）
- 存储话题信息
- 支持话题封面、统计等

### 3. video_topic_relation（视频话题关联表）
- 关联视频和话题的多对多关系

### 4. sys_notice（系统通知表）
- 存储系统通知和公告
- 支持全部用户或指定用户

### 5. sys_user_notice（用户通知阅读记录表）
- 记录用户是否已读通知

### 6. sys_operation_log（操作日志表）
- 记录管理员的操作日志
- 区别于 sys_login_log（登录日志）

## 用户画像字段

`sys_user` 表已扩展以下字段：
- `real_name`: 真实姓名
- `email`: 电子邮箱
- `gender`: 性别
- `level`: 用户等级
- `balance`: 账户余额
- `points`: 积分
- `bio`: 个性签名
- `fans_count`: 粉丝数（冗余字段，可通过 sys_user_follow 统计）
- `follow_count`: 关注数（冗余字段，可通过 sys_user_follow 统计）
- `last_login`: 最后登录时间
- `status_str`: 状态字符串（normal/frozen/muted）

## 注意事项

1. **fans_count 和 follow_count** 是冗余字段，可以通过 `sys_user_follow` 表统计得出
   - 建议：通过定时任务或触发器更新，以提高查询性能
   
2. **Admin 实体类** 如果仍在使用，建议逐步迁移到 RBAC 模式
   - 检查 `AdminManageController` 和相关 Service
   - 改为使用 `User` 实体 + 角色判断

3. **UserBehavior 实体** 如果仍在使用，建议迁移到 `VideoInteraction`
   - 检查 `UserBehaviorController` 和相关 Service
   - 改为使用 `VideoInteraction` 实体
