# 快速解决 HDFS 无数据问题

## 问题描述

运行 `ALSTrainer` 时出现错误：
```
[ERROR] 在路径 hdfs://localhost:9000/short-video/behavior/logs/*/*.json 下未找到任何数据
```

## 原因分析

1. **HDFS 中没有数据**：数据生成器生成的日志文件保存在本地 `bigdata-engine/logs` 目录，但未同步到 HDFS
2. **HDFS 服务未启动**：HDFS 服务未运行，无法访问 HDFS 路径

## 解决方案

### 方案 1：上传本地文件到 HDFS（推荐）

如果本地 `bigdata-engine/logs` 目录下有 JSON 文件，可以使用脚本上传到 HDFS：

```bash
# Windows
cd bigdata-engine\scripts
upload_local_logs_to_hdfs.bat

# 脚本会自动：
# 1. 检查 HDFS 连接
# 2. 创建日期目录（如 2025-01-20）
# 3. 上传所有 JSON 文件到 HDFS
```

### 方案 2：使用本地文件路径（测试用）

如果 HDFS 服务未启动，可以直接使用本地文件路径：

```bash
# Windows 绝对路径
spark-submit --class com.shortvideo.recommendation.als.ALSTrainer ^
  target\bigdata-engine-1.0-jar-with-dependencies.jar ^
  file:///D:/study/clone/video-analysis-and-recommendation-system/bigdata-engine/logs/*.json

# 或者使用相对路径（需要先 cd 到项目根目录）
spark-submit --class com.shortvideo.recommendation.als.ALSTrainer ^
  target\bigdata-engine-1.0-jar-with-dependencies.jar ^
  file:///./bigdata-engine/logs/*.json
```

### 方案 3：重新生成数据并同步到 HDFS

如果本地也没有数据，需要先运行数据生成器：

```bash
# 1. 运行数据生成器（会自动同步到 HDFS）
spark-submit --class com.shortvideo.recommendation.datagenerator.DataGeneratorApp ^
  target\bigdata-engine-1.0-jar-with-dependencies.jar

# 选择模式：
# 1. 按数量生成（例如：1000 条）
# 2. 按时间生成（例如：10 分钟）
# 3. 实时生成（持续生成）

# 2. 然后运行 ALSTrainer
spark-submit --class com.shortvideo.recommendation.als.ALSTrainer ^
  target\bigdata-engine-1.0-jar-with-dependencies.jar
```

## 检查步骤

### 1. 检查本地文件

```bash
# Windows
dir bigdata-engine\logs\*.json

# 应该能看到多个 JSON 文件
```

### 2. 检查 HDFS 连接

```bash
# 运行检查脚本
cd bigdata-engine\scripts
check_hdfs.bat

# 或者手动检查
hadoop fs -ls hdfs://localhost:9000/short-video/behavior/logs/
```

### 3. 检查 HDFS 中的数据

```bash
# 运行数据检查脚本
cd bigdata-engine\scripts
check_hdfs_data.bat

# 或者手动检查
hadoop fs -ls -R hdfs://localhost:9000/short-video/behavior/logs/
```

## 常见问题

### Q1: HDFS 服务未启动怎么办？

**A:** 启动 HDFS 服务：
```bash
# Windows (如果使用 Hadoop for Windows)
start-dfs.cmd

# Linux/Mac
start-dfs.sh
```

### Q2: 上传脚本失败怎么办？

**A:** 检查以下几点：
1. HDFS 服务是否运行
2. Hadoop 命令是否在 PATH 中
3. HDFS 权限是否正确

### Q3: 使用本地文件路径仍然失败？

**A:** 检查：
1. 文件路径是否正确（Windows 需要使用 `/` 而不是 `\`）
2. 文件路径是否使用 `file:///` 前缀
3. 文件是否存在且可读

## 推荐工作流程

1. **首次运行**：
   ```bash
   # 1. 运行数据生成器
   spark-submit --class com.shortvideo.recommendation.datagenerator.DataGeneratorApp ...
   
   # 2. 上传到 HDFS（如果自动同步失败）
   upload_local_logs_to_hdfs.bat
   
   # 3. 运行 ALS 训练
   spark-submit --class com.shortvideo.recommendation.als.ALSTrainer ...
   ```

2. **日常运行**：
   ```bash
   # 直接运行离线分析管道（会自动处理）
   run_offline_pipeline.bat
   ```

## 注意事项

- 确保 HDFS 服务正常运行
- 确保本地日志目录有足够的 JSON 文件（至少需要一些数据才能训练模型）
- 如果数据量太少，过滤后可能为空，需要生成更多数据
