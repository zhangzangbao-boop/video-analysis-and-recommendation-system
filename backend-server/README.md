# 短视频分析推荐系统 - 后端服务

## 项目概述

这是一个基于Spring Boot的短视频平台后端服务，提供用户管理、视频管理、管理员管理等功能。

## 技术栈

- **框架**: Spring Boot 2.7.18
- **数据库**: MySQL 8.0
- **ORM**: MyBatis
- **缓存**: Redis
- **消息队列**: Kafka
- **认证**: JWT (JSON Web Token)
- **Java版本**: JDK 8

## 数据库配置

### 1. 创建数据库

执行 `src/main/resources/database.sql` 创建数据库和表结构。

### 2. 创建管理员表

执行 `src/main/resources/admin_table.sql` 创建管理员表。

### 3. 配置数据库连接

编辑 `src/main/resources/application.yml`，修改数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/short_video_platform?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8
    username: root
    password: 你的密码
```

## 运行项目

### 前置条件

1. JDK 8+
2. Maven 3.6+
3. MySQL 8.0+
4. Redis（可选，用于缓存）
5. Kafka（可选，用于消息队列）

### 启动步骤

1. 确保MySQL数据库已创建并执行了SQL脚本
2. 确保Redis和Kafka已启动（如果使用）
3. 运行以下命令：

```bash
cd backend-server
mvn clean install
mvn spring-boot:run
```

或者使用IDE直接运行 `ServerApplication.java`

## API接口

### 认证接口

- `POST /api/auth/login` - 用户登录

### 用户管理接口（管理员）

- `GET /api/admin/user/list` - 获取用户列表
- `GET /api/admin/user/{id}` - 获取用户详情
- `PUT /api/admin/user/{id}/status` - 更新用户状态
- `PUT /api/admin/user/{id}/password` - 重置用户密码

### 管理员管理接口

- `GET /api/admin/admin/list` - 获取管理员列表
- `GET /api/admin/admin/{id}` - 获取管理员详情
- `POST /api/admin/admin` - 创建管理员
- `PUT /api/admin/admin/{id}` - 更新管理员
- `DELETE /api/admin/admin/{id}` - 删除管理员
- `PUT /api/admin/admin/{id}/status` - 更新管理员状态
- `PUT /api/admin/admin/{id}/password` - 重置管理员密码

### 视频管理接口（管理员）

- `GET /api/admin/video/list` - 获取视频列表
- `POST /api/admin/video/audit` - 审核视频
- `DELETE /api/admin/video/{id}` - 删除视频
- `PUT /api/admin/video/{id}/hot` - 设置/取消热门

## 默认账号

- **管理员账号**: admin
- **默认密码**: 123456

## 响应格式

所有接口统一返回格式：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {}
}
```

- `code`: 状态码，200表示成功
- `msg`: 提示信息
- `data`: 响应数据

## 跨域配置

已配置CORS，允许前端跨域访问。生产环境建议限制允许的域名。

## 注意事项

1. JWT密钥配置在 `application.yml` 中，生产环境请修改为安全的随机字符串
2. 数据库密码等敏感信息请妥善保管
3. Redis和Kafka为可选组件，如果未安装，相关功能可能无法使用
