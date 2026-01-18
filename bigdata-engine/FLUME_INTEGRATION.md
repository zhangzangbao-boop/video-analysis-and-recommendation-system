# Flume与Hadoop集成说明

## 概述

本文档介绍如何使用Apache Flume监听日志文件并将它们写入Hadoop分布式文件系统(HDFS)。

## 系统架构

```
数据生成器 → 日志文件 → Flume Agent → HDFS
```

## 准备工作

### 1. 安装Flume
确保已安装Apache Flume，并设置`FLUME_HOME`环境变量。

### 2. Hadoop服务
确保Hadoop HDFS服务正在运行，并且有适当的写入权限。

### 3. 目录准备
确保以下目录存在并有写入权限：
- `logs/` - 用于存放待处理的日志文件
- HDFS上的目标路径：`/short-video/behavior/logs/`

## 配置说明

### SpoolDir配置 (推荐用于批量处理)

配置文件: `conf/flume-conf.properties`

- **Source**: 监控 `logs/` 目录中的新文件
- **Channel**: 内存通道用于缓冲数据
- **Sink**: 将数据写入HDFS指定路径

### Exec配置 (用于实时流处理)

配置文件: `conf/flume-exec-conf.properties`

- **Source**: 执行`tail -F`命令实时监控日志文件
- **Channel**: 文件通道确保数据可靠性
- **Sink**: 将数据写入HDFS指定路径

## 启动Flume Agent

### Linux/Mac系统
```bash
# SpoolDir模式
$FLUME_HOME/bin/flume-ng agent \
  --conf $FLUME_HOME/conf \
  --conf-file ./conf/flume-conf.properties \
  -Dflume.root.logger=INFO,console \
  -n agent1

# Exec模式
$FLUME_HOME/bin/flume-ng agent \
  --conf $FLUME_HOME/conf \
  --conf-file ./conf/flume-exec-conf.properties \
  -Dflume.root.logger=INFO,console \
  -n agent2
```

### Windows系统
```cmd
# 修改start-flume-agent.ps1中的FLUME_HOME路径，然后运行
powershell -ExecutionPolicy Bypass -File .\scripts\start-flume-agent.ps1
```

## 使用数据生成器

### 1. 生成数据到文件
```bash
# 编译项目
mvn clean compile

# 运行数据生成器
scala com.shortvideo.recommendation.datagenerator.DataGeneratorApp
```

### 2. 选择模式
- **按数量生成**: 指定生成特定数量的数据
- **按时间生成**: 指定生成持续时间
- **实时生成**: 持续生成直到手动停止

## 数据流向

1. 数据生成器将JSON格式的日志数据写入`logs/`目录
2. Flume的SpoolDir Source检测新文件并读取内容
3. 数据通过Channel传递给HDFS Sink
4. HDFS Sink将数据写入指定的HDFS路径

## HDFS存储结构

数据将以以下结构存储在HDFS中：
```
/short-video/behavior/logs/
├── 2026-01-16/
│   ├── user_behavior_12345.json
│   └── user_behavior_realtime_67890.json
├── 2026-01-17/
│   └── ...
```

## 验证数据传输

### 1. 检查HDFS中的文件
```bash
hdfs dfs -ls /short-video/behavior/logs/
```

### 2. 查看最近的文件内容
```bash
hdfs dfs -tail /short-video/behavior/logs/$(date +%Y-%m-%d)/user_behavior_*.json
```

## 故障排除

### 常见问题

1. **权限错误**: 确保Flume进程有HDFS写入权限
2. **连接错误**: 确认HDFS服务正在运行
3. **配置错误**: 检查配置文件中的路径是否正确

### 日志检查
- Flume日志通常位于 `$FLUME_HOME/logs/flume.log`
- 检查Hadoop日志以确认数据写入情况

## 性能调优

- **批次大小**: 调整`batchSize`参数以优化吞吐量
- **通道容量**: 根据数据量调整通道容量
- **滚动策略**: 根据存储需求调整文件滚动策略