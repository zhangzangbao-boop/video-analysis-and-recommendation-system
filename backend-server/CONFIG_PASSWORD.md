# 数据库密码配置说明

## 问题

测试失败显示：`Access denied for user 'root'@'localhost' (using password: YES)`

这说明数据库密码配置不正确。

## 解决方案

### 1. 确认您的MySQL root密码

在MySQL客户端测试：
```bash
mysql -u root -p
# 输入您的密码
```

### 2. 更新配置文件

#### 方式一：更新主配置文件（推荐）
编辑 `backend-server/src/main/resources/application.yml`：
```yaml
spring:
  datasource:
    password: YOUR_ACTUAL_PASSWORD  # 改为您的实际密码
```

#### 方式二：更新测试配置文件
编辑 `backend-server/src/test/resources/application-test.yml`：
```yaml
spring:
  datasource:
    password: YOUR_ACTUAL_PASSWORD  # 改为您的实际密码
```

### 3. 当前配置的密码

根据您的修改，当前配置的密码是：`041206`

如果这不是您的实际密码，请修改为正确的密码。

### 4. 验证配置

运行配置测试：
```bash
mvn test -Dtest=DatabaseConfigTest
```

这会显示当前使用的数据库配置信息。

## 安全建议

1. **不要将包含真实密码的配置文件提交到Git**
2. 使用环境变量或配置文件外部化
3. 生产环境使用不同的密码

## 使用环境变量（可选）

可以设置环境变量来覆盖配置：

```bash
# Windows PowerShell
$env:DB_PASSWORD="your_password"

# Windows CMD
set DB_PASSWORD=your_password

# Linux/Mac
export DB_PASSWORD=your_password
```

然后在 `application.yml` 中使用：
```yaml
spring:
  datasource:
    password: ${DB_PASSWORD:041206}  # 默认值041206，可通过环境变量覆盖
```
