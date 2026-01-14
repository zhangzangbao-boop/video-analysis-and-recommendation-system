-- =================================================================
-- 废弃表说明
-- =================================================================

-- 注意：以下表已废弃，不应再使用
-- 1. user_behavior 表 - 功能与 video_interaction 表重复
--    建议：使用 video_interaction 表，它使用 tinyint 存储类型，性能更优
--    video_interaction.type: 1-点赞, 2-收藏, 3-转发, 4-不感兴趣

-- 2. sys_admin 表 - 系统采用 RBAC 模式
--    建议：管理员只是 sys_user 表中拥有 ROLE_ADMIN 角色的用户
--    通过 sys_user_role 表关联角色，不需要单独的管理员表

-- 如果存在这些表，可以删除或重命名
-- DROP TABLE IF EXISTS `user_behavior`;
-- DROP TABLE IF EXISTS `sys_admin`;
