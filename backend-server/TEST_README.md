# 测试说明

## 测试类说明

### 1. DatabaseConnectionTest - 数据库连接测试
测试数据库连接和表结构：
- ✓ 数据源注入测试
- ✓ 数据库连接测试
- ✓ 数据库存在性测试
- ✓ sys_user表存在性测试
- ✓ 表字段完整性测试
- ✓ 查询功能测试

### 2. UserMapperTest - UserMapper测试
测试MyBatis Mapper层：
- ✓ UserMapper注入测试
- ✓ 查询所有用户
- ✓ 根据条件查询用户
- ✓ 根据ID查询用户
- ✓ 根据用户名查询用户

### 3. UserServiceTest - UserService测试
测试Service业务层：
- ✓ UserService注入测试
- ✓ 获取用户列表（分页）
- ✓ 带条件的用户列表查询
- ✓ 根据ID获取用户详情

## 运行测试

### 方式一：运行所有测试
```bash
cd backend-server
mvn test
```

### 方式二：运行特定测试类
```bash
# 运行数据库连接测试
mvn test -Dtest=DatabaseConnectionTest

# 运行UserMapper测试
mvn test -Dtest=UserMapperTest

# 运行UserService测试
mvn test -Dtest=UserServiceTest
```

### 方式三：在IDE中运行
1. 右键点击测试类
2. 选择 "Run 'TestClassName'"
3. 查看测试结果

## 测试前准备

### 1. 确保数据库已创建
```sql
CREATE DATABASE IF NOT EXISTS short_video_platform;
```

### 2. 执行数据库初始化脚本
```bash
mysql -u root -p short_video_platform < src/main/resources/database.sql
```

### 3. 检查数据库配置
确认 `application.yml` 或 `application-test.yml` 中的数据库配置正确：
- 数据库URL
- 用户名
- 密码

## 测试结果解读

### 成功标志
- 所有测试方法显示 ✓
- 控制台输出详细的测试信息
- 没有异常堆栈

### 失败原因

#### 1. 数据库连接失败
```
Access denied for user 'root'@'localhost'
```
**解决**：检查 `application.yml` 中的数据库密码是否正确

#### 2. 表不存在
```
Table 'short_video_platform.sys_user' doesn't exist
```
**解决**：执行 `database.sql` 创建表

#### 3. 字段不存在
```
Unknown column 'status_str' in 'field list'
```
**解决**：执行 `database.sql` 更新表结构

#### 4. 没有数据
```
⚠ 数据库中没有用户
```
**解决**：这是正常的，测试会跳过需要数据的测试

## 测试输出示例

```
✓ 数据源注入成功
✓ 数据库连接成功
  数据库产品: MySQL
  数据库版本: 8.0.20
  URL: jdbc:mysql://localhost:3306/short_video_platform
✓ 数据库存在: short_video_platform
✓ sys_user表存在
✓ sys_user表字段检查:
  总字段数: 20
  ✓ id
  ✓ username
  ✓ status_str
  ...
✓ 可以查询sys_user表
  未删除用户数: 1
```

## 注意事项

1. **测试会使用实际数据库**：确保测试数据库与生产数据库分离
2. **测试数据**：某些测试可能需要数据库中有数据
3. **事务回滚**：`UserMapperTest` 使用了 `@Transactional`，测试后会自动回滚
4. **密码安全**：不要将包含真实密码的配置文件提交到Git
