# Kafka测试数据发送指南

## 问题说明

如果看到以下警告：
```
[WARN] 本批次没有从Kafka接收到任何消息（可能原因：1. Kafka没有数据 2. offset设置为latest且没有新消息 3. Kafka连接问题）
```

说明Kafka topic `shortvideo_user_behavior` 中没有数据，需要先发送测试数据。

## 快速解决方案

### 方法1：使用LogProducer（推荐）

在项目根目录下运行：

**Windows:**
```bash
cd bigdata-engine
sbt "runMain com.shortvideo.recommendation.datagenerator.LogProducer fixed 100"
```

**Linux/Mac:**
```bash
cd bigdata-engine
sbt "runMain com.shortvideo.recommendation.datagenerator.LogProducer fixed 100"
```

或者使用提供的脚本：

**Windows:**
```bash
cd bigdata-engine/scripts
send-test-data.bat 100
```

**Linux/Mac:**
```bash
cd bigdata-engine/scripts
chmod +x send-test-data.sh
./send-test-data.sh 100
```

### 方法2：持续发送数据

如果需要持续发送数据（模拟实时场景）：

```bash
# 持续发送5分钟
sbt "runMain com.shortvideo.recommendation.datagenerator.LogProducer continuous 5"
```

### 方法3：使用Kafka命令行工具

如果安装了Kafka，可以直接使用命令行工具：

```bash
# 1. 启动Kafka producer
kafka-console-producer --broker-list localhost:9092 --topic shortvideo_user_behavior

# 2. 然后输入JSON格式的数据，例如：
{"userId":1,"videoId":1,"behaviorType":"view","behaviorTime":"2024-01-21 16:20:00","duration":30}

# 3. 按Ctrl+C退出
```

## 数据格式说明

发送到Kafka的数据必须是JSON格式，包含以下必填字段：

```json
{
  "userId": 1,
  "videoId": 1,
  "behaviorType": "view",
  "behaviorTime": "2024-01-21 16:20:00",
  "duration": 30
}
```

**字段说明：**
- `userId`: 用户ID（Long类型）
- `videoId`: 视频ID（Long类型）
- `behaviorType`: 行为类型（String），支持：view, play, like, collect, comment, share, follow, unfollow
- `behaviorTime`: 行为时间（String），格式：yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd HH:mm:ss.SSS
- `duration`: 观看时长（Int，可选，默认0）

## 验证数据是否发送成功

1. **检查Kafka topic状态**
   - 重新启动 `RealtimeAnalysisApp`，会显示topic状态诊断信息

2. **检查应用日志**
   - 应该看到：`[DEBUG] 从Kafka接收到 X 条原始消息`
   - 应该看到：`[热门统计] 本分区更新了 X 个视频`

3. **检查Redis数据**
   ```bash
   redis-cli
   > KEYS rec:video:hot
   > ZREVRANGE rec:video:hot 0 9 WITHSCORES
   ```

## 常见问题

### Q: 发送数据后仍然没有消息？
A: 检查以下几点：
1. Kafka服务是否正常运行
2. Topic名称是否正确（应该是 `shortvideo_user_behavior`）
3. Consumer group是否已存在且offset已消费完（尝试更换group.id）

### Q: 如何清理consumer group重新开始？
A: 
```bash
# 使用kafka-consumer-groups工具重置offset
kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group shortvideo-recommendation \
  --topic shortvideo_user_behavior \
  --reset-offsets --to-earliest --execute
```

### Q: 如何删除checkpoint重新开始？
A: 
- Windows: 删除 `D:\tmp\shortvideo\logs\checkpoint` 目录
- Linux/Mac: 删除 `/tmp/shortvideo/logs/checkpoint` 目录
