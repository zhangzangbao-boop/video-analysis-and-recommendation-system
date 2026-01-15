# 服务启动说明

## 问题：ERR_CONNECTION_REFUSED

如果前端出现 `ERR_CONNECTION_REFUSED` 错误，说明后端服务没有运行。

## 启动后端服务

### 方式一：使用 Maven 启动（推荐）

```bash
cd backend-server
mvn spring-boot:run
```

### 方式二：使用 IDE 启动

1. 打开 `backend-server` 项目
2. 找到 `ServerApplication.java` 文件
3. 右键运行 `main` 方法

### 方式三：打包后启动

```bash
cd backend-server
mvn clean package
java -jar target/server-0.0.1-SNAPSHOT.jar
```

## 验证后端服务

后端服务启动成功后，应该运行在：`http://localhost:8080`

可以通过以下方式验证：

1. 浏览器访问：`http://localhost:8080/api/admin/user/list`
2. 或使用 curl：
   ```bash
   curl http://localhost:8080/api/admin/user/list
   ```

## 启动前端服务

```bash
cd frontend-web
npm install  # 如果还没安装依赖
npm run serve
```

前端服务通常运行在：`http://localhost:8081` 或 `http://localhost:8080`（如果8080被后端占用）

## 常见问题

### 1. 端口被占用

如果8080端口被占用，可以修改 `backend-server/src/main/resources/application.yml`：
```yaml
server:
  port: 8081  # 改为其他端口
```

同时修改前端 `frontend-web/src/utils/request.js` 中的 baseURL。

### 2. 数据库连接失败

确保 MySQL 服务已启动，并且数据库 `short_video_platform` 已创建。

执行数据库初始化：
```bash
mysql -u root -p < backend-server/src/main/resources/database.sql
```

### 3. Redis 连接失败

如果不需要 Redis，可以暂时注释掉相关配置。

## 检查服务状态

### 检查后端是否运行

```bash
# Windows
netstat -ano | findstr :8080

# Linux/Mac
lsof -i :8080
```

### 检查前端是否运行

```bash
# Windows
netstat -ano | findstr :8081

# Linux/Mac
lsof -i :8081
```
