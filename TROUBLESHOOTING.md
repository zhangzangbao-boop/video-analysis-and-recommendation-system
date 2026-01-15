# 故障排查指南

## 500错误：系统异常，请稍后重试

### 问题诊断步骤

#### 1. 查看后端日志
后端控制台会显示详细的错误信息，包括：
- 异常类型
- 错误堆栈
- 具体错误位置

**重要**：请查看后端控制台的完整错误日志！

#### 2. 检查数据库表结构

最可能的原因是数据库表缺少新字段。执行以下SQL检查：

```sql
-- 连接到数据库
USE short_video_platform;

-- 检查sys_user表的字段
DESC sys_user;

-- 或者查看所有字段名
SELECT COLUMN_NAME 
FROM information_schema.columns 
WHERE table_schema = 'short_video_platform' 
  AND table_name = 'sys_user';
```

**必需字段列表**：
- `id`, `username`, `password`, `salt`, `phone`, `nickname`
- `real_name`, `email`, `gender`, `bio`
- `level`, `balance`, `points`
- `fans_count`, `follow_count`
- `status`, `status_str`
- `avatar_url`, `is_deleted`
- `create_time`, `last_login`, `update_time`

#### 3. 如果表结构不完整

执行完整的数据库初始化脚本：

```bash
mysql -u root -p short_video_platform < backend-server/src/main/resources/database.sql
```

**注意**：这会删除所有现有表和数据！如果有重要数据，请先备份。

#### 4. 常见错误原因

##### 错误1：字段不存在
```
Unknown column 'status_str' in 'field list'
```
**解决**：执行 `database.sql` 更新表结构

##### 错误2：表不存在
```
Table 'short_video_platform.sys_user' doesn't exist
```
**解决**：执行 `database.sql` 创建表

##### 错误3：数据库连接失败
```
Communications link failure
```
**解决**：
- 检查MySQL服务是否启动
- 检查 `application.yml` 中的数据库配置
- 检查数据库用户名和密码

##### 错误4：空指针异常
```
NullPointerException
```
**解决**：查看后端日志，找到具体的空指针位置

### 5. 测试数据库连接

在MySQL客户端执行：

```sql
-- 测试连接
SELECT 1;

-- 测试查询
SELECT COUNT(*) FROM sys_user;
```

### 6. 检查后端配置

确认 `application.yml` 中的配置正确：
- 数据库URL
- 用户名和密码
- 端口号

### 7. 重启后端服务

修改配置或数据库后，需要重启后端服务：
```bash
# 停止当前服务（Ctrl+C）
# 重新启动
mvn spring-boot:run
```

## 前端错误处理

前端现在会显示后端返回的详细错误信息。如果看到：
- "系统异常，请稍后重试" - 查看后端日志获取详细信息
- "无法连接到服务器" - 检查后端服务是否启动
- "请求超时" - 检查网络或增加超时时间

## 获取帮助

如果问题仍然存在，请提供：
1. 后端控制台的完整错误日志
2. 数据库表结构（执行 `DESC sys_user;`）
3. 浏览器控制台的网络请求详情
