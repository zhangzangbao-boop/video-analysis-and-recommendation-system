Spark 数据处理模块

# 短视频推荐系统 - 大数据引擎

## 项目架构说明

短视频推荐系统的大数据引擎部分采用Lambda架构，主要包含以下组件：

### 技术栈
- **Spark**: 使用Spark Core、SQL、Streaming和MLlib进行数据处理和机器学习
- **Kafka**: 作为实时数据流的消息中间件
- **Hadoop/HDFS**: 用于分布式存储原始数据和处理结果
- **Flume**: 用于日志收集和传输
- **Redis**: 用于缓存推荐结果和热点数据
- **MySQL**: 存储元数据和最终推荐结果
- **Scala**: 主要开发语言

### 架构层次
1. **数据生成层**: 通过[DataGeneratorApp](file:///F:/Project/video-analysis-and-recommendation-system/video-analysis-and-recommendation-system\bigdata-engine\src\main\scala\com\shortvideo\recommendation\datagenerator\DataGeneratorApp.scala#L12-L222)模拟用户行为数据
2. **数据采集层**: Flume负责从日志目录采集数据并传输到HDFS
3. **离线处理层**: Spark处理历史数据，包括ODS/DWD/DWS层的数据清洗和特征工程
4. **实时处理层**: Spark Streaming处理实时数据流
5. **推荐算法层**: ALS协同过滤算法进行个性化推荐
6. **存储层**: HDFS分布式存储、Redis缓存、MySQL持久化存储

## 关键配置

### Kafka配置
- 服务器地址: `localhost:9092`
- 消费组ID: `shortvideo-recommendation`
- 主要Topic:
  - `shortvideo_user_behavior`: 用户行为数据
  - `shortvideo_content_exposure`: 内容曝光数据
  - `shortvideo_user_interaction`: 用户交互数据
  - `shortvideo_recommend_result`: 推荐结果数据

### HDFS配置
- HDFS URI: `hdfs://localhost:9000`
- 数据存储路径: `/short-video/behavior/logs`

### Flume配置
项目提供了两种Flume配置模式：
1. **SpoolDir模式** (`flume-conf.properties`): 监听目录中的新文件
2. **Exec模式** (`flume-exec-conf.properties`): 实时监控日志文件变化

## 运行步骤

### 1. 启动基础服务

#### 启动Kafka
```bash
# 启动Zookeeper (如果尚未启动)
bin/zookeeper-server-start.sh config/zookeeper.properties

# 启动Kafka服务器
bin/kafka-server-start.sh config/server.properties
```

#### 创建必要的Kafka Topics
```bash
# 创建项目所需的Kafka topics
bin/kafka-topics.sh --create --topic shortvideo_user_behavior --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
bin/kafka-topics.sh --create --topic shortvideo_content_exposure --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
bin/kafka-topics.sh --create --topic shortvideo_user_interaction --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
bin/kafka-topics.sh --create --topic shortvideo_recommend_result --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
```

对于Windows环境，使用以下命令：
```powershell
# 启动Zookeeper
.\bin\windows\zookeeper-server-start.bat .\config\zookeeper.properties

# 启动Kafka服务器
.\bin\windows\kafka-server-start.bat .\config\server.properties

# 创建Topics (使用PowerShell)
.\bin\windows\kafka-topics.bat --create --topic shortvideo_user_behavior --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
.\bin\windows\kafka-topics.bat --create --topic shortvideo_content_exposure --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
.\bin\windows\kafka-topics.bat --create --topic shortvideo_user_interaction --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
.\bin\windows\kafka-topics.bat --create --topic shortvideo_recommend_result --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
```

#### 启动Hadoop HDFS
```bash
# 启动HDFS
./sbin/start-dfs.sh
```

#### 启动MySQL和Redis
```bash
# 启动MySQL (确保已创建short_video_platform数据库)
sudo service mysql start

# 启动Redis
redis-server
```

### 2. 编译项目
```bash
# 编译Scala代码
mvn clean compile scala:compile

# 打包项目 (包含所有依赖)
mvn assembly:single
```

### 3. 运行数据生成器
```bash
# 方式1: 运行数据生成器主程序
java -cp target/spark-example-1.0-jar-with-dependencies.jar com.shortvideo.recommendation.datagenerator.DataGeneratorApp

# 方式2: 直接发送数据到HDFS
java -cp target/spark-example-1.0-jar-with-dependencies.jar com.shortvideo.recommendation.datagenerator.LogProducer sendToHDFS 1000
```

### 4. 运行离线分析流程

#### 使用运行脚本 (推荐)
我们提供了一个统一的运行脚本来自动化整个离线分析流程：

**Windows PowerShell:**
```powershell
# 设置PowerShell执行策略（如果需要）
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser

# 运行所有步骤
.\run-offline-analysis.ps1

# 运行指定步骤
.\run-offline-analysis.ps1 -Steps "dwd,dws"

# 查看帮助
.\run-offline-analysis.ps1 -Help
```

**Linux/Mac Bash:**
```bash
# 给脚本添加执行权限
chmod +x run-offline-analysis.sh

# 运行所有步骤
./run-offline-analysis.sh

# 运行指定步骤
./run-offline-analysis.sh -s "dwd,dws,als"

# 查看帮助
./run-offline-analysis.sh --help
```

#### 手动运行各个组件
如果需要单独运行各个组件：

**DWD层数据清洗:**
```bash
java -cp target/spark-example-1.0-jar-with-dependencies.jar com.shortvideo.recommendation.offline.dwd.DwdLogClean
```

**DWS层特征聚合:**
```bash
java -cp target/spark-example-1.0-jar-with-dependencies.jar com.shortvideo.recommendation.offline.dws.DwsUserVideoFeature
```

**ALS推荐模型训练:**
```bash
java -cp target/spark-example-1.0-jar-with-dependencies.jar com.shortvideo.recommendation.als.ALSTrainer
```

**离线统计分析:**
```bash
java -cp target/spark-example-1.0-jar-with-dependencies.jar com.shortvideo.recommendation.offline.job.OfflineJob
```

### 5. 启动Flume进行日志收集
```bash
# 使用SpoolDir配置
flume-ng agent -c conf -f conf/flume-conf.properties -n a1 -Dflume.root.logger=INFO,console

# 使用Exec配置
flume-ng agent -c conf -f conf/flume-exec-conf.properties -n a1 -Dflume.root.logger=INFO,console
```

### 6. 运行实时分析组件
```bash
# 启动实时推荐流处理
java -cp target/spark-example-1.0-jar-with-dependencies.jar com.shortvideo.recommendation.realtime.job.StreamingJob
```

## 项目模块说明

### 离线分析模块
离线分析模块包含以下组件：

1. **DWD层 (Data Warehouse Detail)**: [DwdLogClean](file:///F:/Project/video-analysis-and-recommendation-system/video-analysis-and-recommendation-system\bigdata-engine\src\main\scala\com\shortvideo\recommendation\offline\dwd\DwdLogClean.scala#L12-L103)
   - 解析原始日志数据
   - 数据清洗和格式标准化
   - 按日期分区存储

2. **DWS层 (Data Warehouse Summary)**: [DwsUserVideoFeature](file:///F:/Project/video-analysis-and-recommendation-system/video-analysis-and-recommendation-system\bigdata-engine\src\main\scala\com\shortvideo\recommendation\offline\dws\DwsUserVideoFeature.scala#L12-L84)
   - 用户-视频交互特征聚合
   - 计算综合评分作为ALS模型输入
   - 生成推荐模型训练数据

3. **ALS推荐模型**: [ALSTrainer](file:///F:/Project/video-analysis-and-recommendation-system/video-analysis-and-recommendation-system\bigdata-engine\src\main\scala\com\shortvideo\recommendation\als\ALSTrainer.scala#L12-L113)
   - 使用协同过滤算法训练推荐模型
   - 生成用户-视频推荐矩阵
   - 持久化推荐结果到MySQL和Redis

4. **离线统计分析**: [OfflineJob](file:///F:/Project/video-analysis-and-recommendation-system/video-analysis-and-recommendation-system\bigdata-engine\src\main\scala\com\shortvideo\recommendation\offline\job\OfflineJob.scala#L12-L154)
   - 热门视频统计
   - 核心业务指标计算
   - 数据可视化报表生成

### 实时分析模块
实时分析模块包含以下组件，专注于实时数据处理并将结果直接存储到Redis供Spring Boot应用使用：

1. **实时用户行为流处理**: [UserBehaviorStreaming](file:///F:/Project/video-analysis-and-recommendation-system/video-analysis-and-recommendation-system\bigdata-engine\src/main/scala/com/shortvideo/recommendation/realtime/streaming/UserBehaviorStreaming.scala)
   - 从Kafka消费用户行为数据流
   - 实时统计用户行为类型和频次
   - 更新用户兴趣标签到Redis
   - 使用滑动窗口进行实时行为分析
   - 结果直接存储到Redis以供Spring Boot应用访问

2. **实时热门视频计算**: [HotVideoStreaming](file:///F:/Project/video-analysis-and-recommendation-system/video-analysis-and-recommendation-system\bigdata-engine\src/main/scala/com/shortvideo/recommendation/realtime/streaming/HotVideoStreaming.scala)
   - 基于用户实时行为计算视频热度
   - 按行为类型分配不同权重（观看*1 + 点赞*5 + 评论*3 + 分享*4等）
   - 实时更新热门视频排行榜
   - 将热门视频及详细统计信息存储到Redis的ZSet中
   - 支持快速检索Top N热门视频

3. **实时推荐流处理**: [RealtimeRecommendationStreaming](file:///F:/Project/video-analysis-and-recommendation-system/video-analysis-and-recommendation-system\bigdata-engine\src/main/scala/com/shortvideo/recommendation/realtime/streaming/RealtimeRecommendationStreaming.scala)
   - 基于协同过滤算法生成实时推荐
   - 基于内容的推荐算法
   - 使用滑动窗口分析用户近期行为
   - 计算用户兴趣偏好并生成个性化推荐
   - 将推荐结果以ZSet形式存储到Redis中

4. **实时分析主任务**: [StreamingJob](file:///F:/Project/video-analysis-and-recommendation-system/video-analysis-and-recommendation-system\bigdata-engine\src/main/scala/com/shortvideo/recommendation/realtime/job/StreamingJob.scala)
   - 统筹管理所有实时流处理组件
   - 配置Spark Streaming参数和批处理间隔
   - 初始化Kafka消费者和Redis连接池
   - 协调多个数据流的处理逻辑
   - 管理资源生命周期和异常处理

5. **实时推荐服务接口**: [RealtimeRecommendationService](file:///F:/Project/video-analysis-and-recommendation-system/video-analysis-and-recommendation-system\bigdata-engine\src/main/scala/com/shortvideo/recommendation/realtime/service/RealtimeRecommendationService.scala)
   - 提供给Spring Boot应用的API接口
   - 获取用户实时推荐结果
   - 查询实时热门视频
   - 访问用户行为统计和兴趣标签
   - 检查推荐结果的有效性和刷新时间

#### 实时分析模块架构特点

**数据流处理**:
- 采用Apache Kafka作为消息队列，确保数据流的可靠传输
- 使用Spark Streaming的Direct API实现精确一次语义
- 实现滑动窗口处理，平衡实时性和准确性

**存储策略**:
- 所有实时分析结果直接存储到Redis
- 使用Redis的ZSet数据结构存储排序列表（如热门视频、推荐结果）
- 设置合理的过期时间以控制内存使用
- 使用Hash结构存储详细的统计信息

**性能优化**:
- 配置合理的批处理间隔（30秒或60秒）
- 限制每个分区的最大处理速率
- 使用Redis连接池提高访问效率
- 实现数据压缩和序列化优化

**Spring Boot集成**:
- 通过Redis作为中间存储，实现与Spring Boot应用的解耦
- 提供清晰的API接口访问实时推荐结果
- 支持多种推荐类型（协同过滤、基于内容）
- 实现推荐结果的快速检索和缓存

## 注意事项

1. 确保所有基础服务（Kafka、Hadoop、MySQL、Redis）在运行任何组件之前已启动
2. **特别注意**: 在运行项目前，请确保已创建项目所需的Kafka topics（`shortvideo_user_behavior`等）
3. 检查Flume配置文件中的路径是否与实际环境匹配
4. 确保HDFS目录有适当的权限
5. 在Windows环境下，可能需要配置Hadoop本地库
6. MySQL连接参数（用户名、密码）在配置文件中设置，默认为root/root
7. 确保有足够的内存资源运行Spark作业
8. 实时分析模块的结果会直接存储到Redis，Spring Boot应用可通过Redis获取最新推荐