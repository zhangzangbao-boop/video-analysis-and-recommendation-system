-- 检查数据库表结构是否完整
-- 如果执行此脚本报错，说明表结构不完整，需要先执行 database.sql

-- 检查 sys_user 表是否存在所有必需字段
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'short_video_platform'
  AND TABLE_NAME = 'sys_user'
ORDER BY ORDINAL_POSITION;

-- 如果缺少字段，会看到字段列表不完整
-- 需要执行 database.sql 来创建/更新表结构
