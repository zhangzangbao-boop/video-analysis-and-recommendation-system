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
```bash
# Windows环境下的Kafka topic创建命令
bin\windows\kafka-topics.bat --create --topic shortvideo_user_behavior --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
bin\windows\kafka-topics.bat --create --topic shortvideo_content_exposure --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
bin\windows\kafka-topics.bat --create --topic shortvideo_user_interaction --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
bin\windows\kafka-topics.bat --create --topic shortvideo_recommend_result --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
```

#### 启动Hadoop
```bash
# 启动HDFS
sbin/start-dfs.sh

# 确认HDFS服务正常运行
hdfs dfsadmin -report
```

#### 启动Flume
```bash
# 使用SpoolDir配置启动Flume Agent (适用于批量文件处理)
bin/flume-ng agent -c conf -f conf/flume-conf.properties -n agent1 -Dflume.root.logger=INFO,console

# 或使用Exec配置启动Flume Agent (适用于实时日志流)
bin/flume-ng agent -c conf -f conf/flume-exec-conf.properties -n agent2 -Dflume.root.logger=INFO,console
```

F:\Project\video-analysis-and-recommendation-system\video-analysis-and-recommendation-system\bigdata-engine>set JAVA_OPTS=-Dflume.root.logger=INFO,console

F:\Project\video-analysis-and-recommendation-system\video-analysis-and-recommendation-system\bigdata-engine>F:\apache-flume-1.9.0-bin\bin\flume-ng.cmd agent --conf .\conf --conf-file .\conf\flume-conf.properties -n agent1


### 2. 运行数据生成器

#### 编译项目
```bash
mvn clean package -DskipTests
```

#### 运行DataGeneratorApp
```bash
# 运行数据生成器
java -cp target/spark-example-1.0-jar-with-dependencies.jar com.shortvideo.recommendation.datagenerator.DataGeneratorApp
```

#### 数据生成模式
数据生成器支持三种模式：

1. **按数量生成**: 指定生成的数据条数
2. **按时间生成**: 指定生成数据的持续时间
3. **实时生成**: 持续生成数据直到手动停止

生成的数据会输出到控制台，并同时保存到Flume监控的`logs`目录中，Flume会自动将其传输到HDFS。

### 3. 数据流向说明

1. [DataGeneratorApp](file:///F:/Project/video-analysis-and-recommendation-system/video-analysis-and-recommendation-system\bigdata-engine\src\main\scala\com\shortvideo\recommendation\datagenerator\DataGeneratorApp.scala#L12-L222)生成模拟用户行为数据
2. 数据以JSON格式保存到`logs/`目录
3. Flume监控该目录并将新文件传输到HDFS
4. Spark应用程序从HDFS读取数据进行处理和分析
5. 处理结果存储回HDFS、Redis或MySQL

### 4. 项目模块说明

- **als**: 协同过滤推荐算法模块，包含模型训练和评估
- **common**: 公共配置、实体类、工具类和常量定义
- **datagenerator**: 数据生成器模块，用于模拟用户行为数据
- **offline**: 离线处理模块，包含ODS/DWD/DWS层数据处理
- **realtime**: 实时处理模块，包含流式计算和实时推荐

## 环境要求

- Java 8+
- Scala 2.12+
- Maven 3.6+
- Kafka 2.8+
- Hadoop 3.x
- Redis
- MySQL

## 注意事项

1. 确保所有基础服务（Kafka、Hadoop）在运行DataGeneratorApp之前已启动
2. **特别注意**: 在运行项目前，请确保已创建项目所需的Kafka topics（`shortvideo_user_behavior`等）
3. 检查Flume配置文件中的路径是否与实际环境匹配
4. 确保HDFS目录有适当的权限
5. 在Windows环境下，可能需要配置Hadoop本地库