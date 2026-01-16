# 模拟数据生成器使用指南

## 概述

本项目提供了一套完整的模拟数据生成工具，用于向Hadoop生态系统发送模拟日志数据。主要包括以下几个组件：

- `LogGenerator`: 生成模拟用户行为数据
- `LogProducer`: 将数据发送到Kafka或HDFS
- `MockDataGenerator`: 主入口程序，支持多种输出方式

## 模拟数据结构

生成的用户行为数据包含以下字段：

| 字段名 | 类型 | 描述 |
|--------|------|------|
| userId | Long | 用户ID |
| videoId | Long | 视频ID |
| behaviorType | String | 行为类型 (play, like, collect, share, comment, follow, unfollow) |
| behaviorTime | String | 行为发生时间 |
| duration | Int | 观看时长（秒） |
| deviceInfo | String | 设备信息 |
| networkType | String | 网络类型 (WiFi, 4G, 5G, 3G) |
| ipAddress | String | IP地址 |
| location | String | 地理位置 |
| extraInfo | String | 扩展信息 |

## 使用方法

### 1. 编译项目

```bash
mvn clean compile
```

### 2. 运行模拟数据生成器

#### 控制台输出
```bash
# 输出100条模拟数据到控制台
scala com.shortvideo.recommendation.datagenerator.MockDataGenerator console 100
```

#### 文件输出
```bash
# 输出1000条模拟数据到本地文件
scala com.shortvideo.recommendation.datagenerator.MockDataGenerator file 1000 ./mock_data.json
```

#### 发送到Kafka
```bash
# 发送500条数据到Kafka主题
scala com.shortvideo.recommendation.datagenerator.MockDataGenerator kafka 500
```

#### 发送到HDFS
```bash
# 发送200条数据到HDFS
scala com.shortvideo.recommendation.datagenerator.MockDataGenerator hdfs 200
```

### 3. 使用专用生产者

#### 固定数量发送到Kafka
```bash
# 发送1000条消息到Kafka
scala com.shortvideo.recommendation.datagenerator.LogProducer fixed 1000
```

#### 持续发送到Kafka
```bash
# 持续发送10分钟的数据到Kafka
scala com.shortvideo.recommendation.datagenerator.LogProducer continuous 10
```

#### 发送到HDFS
```bash
# 发送500条数据到HDFS
scala com.shortvideo.recommendation.datagenerator.LogProducer hdfs 500
```

## 配置说明

模拟数据生成器使用以下配置参数，可在 `application.conf` 中修改：

```hocon
# HDFS配置
hdfs {
  hdfs.uri = "hdfs://localhost:9000"
  behavior.logs.path = ${hdfs.hdfs.uri}"/short-video/behavior/logs"
}

# Kafka配置
kafka {
  bootstrap.servers = "localhost:9092"
  topics {
    user-behavior = "shortvideo_user_behavior"
  }
}
```

## 数据特征

- 用户ID范围：1000-9999
- 视频ID范围：1-999
- 行为类型：播放、点赞、收藏、分享、评论、关注、取消关注
- 随机设备信息和网络类型
- 真实感地理位置
- 合理的观看时长分布

## 注意事项

1. 确保Kafka服务正在运行且能够连接
2. 确保HDFS服务正在运行且具有适当的写入权限
3. 根据实际需求调整数据生成速度，避免对系统造成过大压力
4. 在生产环境中使用时，请确保配置了正确的服务器地址

## 故障排除

如果遇到连接问题：

1. 检查Kafka服务是否启动：`bin/kafka-server-start.sh config/server.properties`
2. 检查HDFS服务是否启动：`start-dfs.sh`
3. 检查防火墙设置是否阻止了相应端口
4. 查看应用日志中的具体错误信息