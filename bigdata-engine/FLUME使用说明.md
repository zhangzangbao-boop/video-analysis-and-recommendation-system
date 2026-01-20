# Flume日志同步使用说明

## 概述

Flume用于将本地日志文件（`bigdata-engine/logs`）自动同步到HDFS（`/short-video/behavior/logs`）。

## 前置条件

### 1. 环境准备

- ✅ Hadoop已安装并配置
- ✅ HDFS服务正在运行（NameNode端口9000）
- ✅ Flume已安装
- ✅ 环境变量已设置：
  - `HADOOP_HOME` - Hadoop安装目录
  - `FLUME_HOME` - Flume安装目录
  - PATH中包含 `%HADOOP_HOME%\bin` 和 `%FLUME_HOME%\bin`

### 2. 服务启动顺序

```
1. 启动Hadoop HDFS
   → start-dfs.cmd (Windows)
   → start-dfs.sh (Linux/Mac)

2. 验证HDFS连接
   → 运行 check_hdfs.bat

3. 启动Flume Agent
   → 运行 start_flume.bat
```

## 使用步骤

### 步骤1: 检查HDFS连接

```bash
cd bigdata-engine\scripts
check_hdfs.bat
```

这个脚本会：
- ✅ 检查Hadoop命令是否可用
- ✅ 测试HDFS连接
- ✅ 检查并创建目标目录 `/short-video/behavior/logs`
- ✅ 显示目录内容

**预期输出：**
```
[SUCCESS] HDFS连接正常
[INFO] 目录已存在: /short-video/behavior/logs
```

### 步骤2: 生成日志数据

```bash
# 从项目根目录运行
scala -cp "bigdata-engine/target/spark-example-1.0-jar-with-dependencies.jar" com.shortvideo.recommendation.datagenerator.DataGeneratorApp
```

选择生成模式（1/2/3），日志文件将生成到 `bigdata-engine\logs` 目录。

### 步骤3: 启动Flume Agent

```bash
cd bigdata-engine\scripts
start_flume.bat
```

这个脚本会：
- ✅ 检查配置文件
- ✅ 确保日志目录存在
- ✅ 创建Flume数据目录
- ✅ 测试HDFS连接
- ✅ 启动Flume Agent

**预期输出：**
```
[SUCCESS] HDFS目录已准备就绪
启动 Flume Agent...
```

### 步骤4: 验证同步

在Flume运行后，检查HDFS：

```bash
# 查看今日日志目录
hadoop fs -ls hdfs://localhost:9000/short-video/behavior/logs/

# 查看具体文件
hadoop fs -ls hdfs://localhost:9000/short-video/behavior/logs/2025-01-XX/

# 查看文件内容
hadoop fs -cat hdfs://localhost:9000/short-video/behavior/logs/2025-01-XX/behavior_log_*.json | head
```

## 目录结构

```
项目根目录/
├── bigdata-engine/
│   ├── logs/                          # 本地日志目录（被Flume监控）
│   │   ├── user_behavior_*.json      # 生成的日志文件
│   │   └── user_behavior_*.json.completed  # Flume处理后的标记文件
│   ├── conf/
│   │   └── flume-conf.properties     # Flume配置文件
│   └── scripts/
│       ├── start_flume.bat           # 启动Flume脚本
│       └── check_hdfs.bat            # HDFS检查脚本
└── ...

HDFS:
/short-video/behavior/logs/
└── YYYY-MM-DD/                       # 按日期分区
    ├── behavior_log_*.json           # 同步的日志文件
    └── ...
```

## 配置文件说明

### flume-conf.properties

| 配置项 | 说明 |
|--------|------|
| `agent1.sources.source1.spoolDir` | 监控的本地目录 |
| `agent1.sinks.sink_hdfs.hdfs.path` | HDFS目标路径（按日期分区） |
| `agent1.sinks.sink_hdfs.hdfs.rollSize` | 文件滚动大小（128MB） |
| `agent1.sinks.sink_hdfs.hdfs.rollInterval` | 文件滚动时间（600秒） |

## 常见问题排查

### 问题1: Flume启动失败

**症状：** `flume-ng.cmd` 命令未找到

**解决方案：**
1. 检查 `FLUME_HOME` 环境变量是否设置
2. 检查 `%FLUME_HOME%\bin` 是否在PATH中
3. 或在脚本中使用Flume的完整路径

### 问题2: 无法连接到HDFS

**症状：** `hadoop fs -ls` 失败

**解决方案：**
1. 确保Hadoop NameNode已启动：
   ```bash
   start-dfs.cmd
   ```
2. 检查HDFS端口9000是否被占用
3. 验证 `core-site.xml` 配置：
   ```xml
   <property>
     <name>fs.defaultFS</name>
     <value>hdfs://localhost:9000</value>
   </property>
   ```

### 问题3: 日志文件没有同步

**症状：** 本地有日志文件，但HDFS中没有

**排查步骤：**

1. **检查Flume是否运行：**
   ```bash
   # 查看Flume进程
   tasklist | findstr flume
   # 或
   jps | findstr Application
   ```

2. **检查日志文件格式：**
   - 确保文件扩展名是 `.json`
   - 确保文件不是 `.tmp`（会被忽略）
   - 确保文件不是 `.completed`（已处理）

3. **检查Flume日志：**
   - 查看Flume控制台输出
   - 查找错误信息，如：
     - `ChannelException` - Channel配置问题
     - `SinkException` - Sink配置问题
     - `IOException` - 文件或网络问题

4. **检查目录权限：**
   ```bash
   # 确保本地日志目录可读
   icacls bigdata-engine\logs
   
   # 确保HDFS目录可写
   hadoop fs -chmod 777 hdfs://localhost:9000/short-video/behavior/logs
   ```

5. **手动测试：**
   ```bash
   # 创建一个测试文件
   echo "test" > bigdata-engine\logs\test_%RANDOM%.json
   
   # 等待几秒，查看是否被处理（添加.completed后缀）
   dir bigdata-engine\logs
   ```

### 问题4: 文件被重命名为.completed但没有上传

**症状：** 本地文件有 `.completed` 后缀，但HDFS中没有

**可能原因：**
1. HDFS Sink配置错误
2. HDFS连接问题
3. Channel容量不足

**解决方案：**
1. 检查Flume日志中的错误信息
2. 增加Channel容量：
   ```properties
   agent1.channels.channel_memory.capacity = 50000
   ```
3. 检查网络连接和HDFS服务状态

### 问题5: Kafka Sink失败（不影响HDFS同步）

如果同时配置了Kafka Sink，确保：
1. Kafka服务正在运行（端口9092）
2. Topic `shortvideo_user_behavior` 已创建
3. Kafka服务正常

**注意：** Kafka Sink失败不会影响HDFS Sink的同步。

## 监控和维护

### 监控Flume运行状态

```bash
# 查看进程
tasklist | findstr flume

# 查看Flume日志（如果配置了日志文件）
# Flume默认输出到控制台，也可以重定向到文件
```

### 清理已处理文件

已处理的文件会添加 `.completed` 后缀，可以定期清理：

```bash
# 删除已处理文件（谨慎操作！）
del bigdata-engine\logs\*.completed
```

### 重启Flume

如果Flume异常退出：
1. 停止当前进程（Ctrl+C）
2. 检查错误日志
3. 修复问题
4. 重新运行 `start_flume.bat`

## 性能优化

1. **增加批处理大小：**
   ```properties
   agent1.sinks.sink_hdfs.hdfs.batchSize = 2000
   ```

2. **增加Channel容量：**
   ```properties
   agent1.channels.channel_memory.capacity = 100000
   ```

3. **调整文件滚动策略：**
   - 减小 `rollInterval` 更快生成文件
   - 增大 `rollSize` 生成更大的文件

## 故障恢复

如果Flume异常停止，未处理的文件仍然在 `bigdata-engine\logs` 目录中：

1. **检查未处理文件：**
   ```bash
   dir bigdata-engine\logs\*.json
   ```

2. **重启Flume：**
   - Flume会自动检测未处理文件（没有 `.completed` 后缀）
   - 按时间顺序处理旧文件

3. **验证同步：**
   ```bash
   hadoop fs -ls hdfs://localhost:9000/short-video/behavior/logs/
   ```

## 相关命令速查

```bash
# 启动Hadoop HDFS
start-dfs.cmd

# 检查HDFS连接
hadoop fs -ls hdfs://localhost:9000/

# 查看HDFS目录
hadoop fs -ls hdfs://localhost:9000/short-video/behavior/logs/

# 查看HDFS文件内容
hadoop fs -cat hdfs://localhost:9000/short-video/behavior/logs/2025-01-XX/behavior_log_*.json | head

# 启动Flume
cd bigdata-engine\scripts
start_flume.bat

# 检查Flume进程
tasklist | findstr flume
```
