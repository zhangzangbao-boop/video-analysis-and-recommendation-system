# ALS 离线分析部分修复说明

## ✅ 已完成的修复

### 1. ✅ 修复 DataProcessor - behaviorType 支持

**问题**：代码只支持字符串格式（"play", "like"等），文档要求支持整数格式（1,2,3,4）

**修复**：
- ✅ 支持字符串格式：`"play"`, `"like"`, `"collect"`, `"comment"`
- ✅ 支持整数格式：`1`(播放), `2`(点赞), `3`(收藏), `4`(评论)
- ✅ 向后兼容：同时支持两种格式，自动识别

**评分权重映射**（符合文档要求）：
- 播放 (play/1) → 3.0
- 点赞 (like/2) → 5.0
- 收藏 (collect/3) → 4.0
- 评论 (comment/4) → 4.0
- 其他行为（share, follow, unfollow等）→ 0.0（不参与评分）

### 2. ✅ 修复字段映射

**问题**：代码中使用 `mid` 和 `createDate`，但实际日志使用 `videoId` 和 `behaviorTime`

**修复**：
- ✅ 支持 `videoId` 和 `mid` 两种字段名（使用 `coalesce` 自动选择）
- ✅ 支持 `behaviorTime` 和 `createDate` 两种字段名
- ✅ 自动处理字段缺失情况

### 3. ✅ 添加统计信息输出

**新增统计信息**：
- ✅ 原始日志记录数
- ✅ 解析后的有效评分记录数
- ✅ 用户数、视频数统计
- ✅ 聚合前后的记录数对比
- ✅ 质量过滤前后的记录数对比
- ✅ 活跃用户数、活跃视频数
- ✅ 训练数据统计（用户数、视频数、评分数、平均评分、评分范围、数据密度）
- ✅ 数据集划分统计（训练集/测试集比例）
- ✅ 推荐结果统计（推荐用户数、推荐总数）
- ✅ 最终统计（模型ID、模型路径、RMSE、推荐总数）

### 4. ✅ 修复 HDFS 路径处理

**问题**：路径格式不符合文档要求，不支持日期分区

**修复**：
- ✅ 支持按日期分区的路径：`hdfs://localhost:9000/short-video/behavior/logs/*/*.json`
- ✅ 支持通配符匹配所有历史数据
- ✅ 支持通过命令行参数指定自定义路径
- ✅ 模型保存路径：`hdfs://localhost:9000/short-video/als-model/model-YYYY-MM-DD`

### 5. ✅ 改进时间戳处理

**修复**：
- ✅ 支持日期时间字符串格式：`yyyy-MM-dd HH:mm:ss`
- ✅ 支持毫秒时间戳格式
- ✅ 自动解析和转换
- ✅ 处理空值和异常情况

### 6. ✅ 改进错误处理和日志

**修复**：
- ✅ 详细的错误信息输出
- ✅ 异常堆栈跟踪
- ✅ 数据验证和空值检查
- ✅ 每个处理步骤的进度提示

## ⚠️ 需要确认的信息

### 1. MySQL 数据库连接配置

**当前配置**（`MySQLWriter.scala`）：
```scala
JDBC_URL = "jdbc:mysql://localhost:3306/short_video_platform"
JDBC_USER = "root"
JDBC_PASSWORD = "123456"  // ⚠️ 需要确认密码是否正确
```

**需要确认**：
- [ ] 数据库端口：3306 还是 3307？
- [ ] 数据库密码：`123456` 还是 `root`？
- [ ] 数据库名：`short_video_platform` 是否正确？

### 2. HDFS 路径结构

**当前配置**：
- 输入路径：`hdfs://localhost:9000/short-video/behavior/logs/*/*.json`
- 模型路径：`hdfs://localhost:9000/short-video/als-model/model-YYYY-MM-DD`

**需要确认**：
- [ ] HDFS 实际路径结构是否按日期分区？
- [ ] 日志文件扩展名是 `.json` 还是 `.log`？
- [ ] HDFS NameNode 地址和端口是否正确？

### 3. 行为类型处理

**当前实现**：
- ✅ 支持字符串格式（"play", "like", "collect", "comment"）
- ✅ 支持整数格式（1, 2, 3, 4）
- ✅ 其他行为类型（"share", "follow", "unfollow"）不参与评分

**需要确认**：
- [ ] 是否需要处理 "share" 行为？如果需要，评分权重应该是多少？
- [ ] 是否需要处理 "follow"/"unfollow" 行为？

### 4. 数据质量过滤阈值

**当前配置**：
- 用户最小行为数：5
- 视频最小互动数：5

**需要确认**：
- [ ] 这些阈值是否合适？是否需要调整？

## 📋 使用说明

### 运行 ALS 训练

```bash
# 方式1：使用默认路径（读取所有历史数据）
spark-submit \
  --class com.shortvideo.recommendation.als.ALSTrainer \
  --master local[*] \
  --driver-memory 2G \
  --executor-memory 2G \
  bigdata-engine/target/spark-example-1.0-jar-with-dependencies.jar

# 方式2：指定自定义路径
spark-submit \
  --class com.shortvideo.recommendation.als.ALSTrainer \
  --master local[*] \
  bigdata-engine/target/spark-example-1.0-jar-with-dependencies.jar \
  "hdfs://localhost:9000/short-video/behavior/logs/2025-01-20/*.json"
```

### 预期输出

```
================================================================================
Spark ALS 离线推荐训练 (文档标准版)
================================================================================
[INFO] 配置参数:
  - HDFS 输入路径: hdfs://localhost:9000/short-video/behavior/logs/*/*.json
  - 模型保存路径: hdfs://localhost:9000/short-video/als-model/model-2025-01-20
  - ALS 参数: rank=20, maxIter=10, regParam=0.1, topN=20
[INFO] 开始读取 HDFS JSON 数据: ...
[INFO] 读取到原始日志记录数: 1000
[INFO] 解析后的有效评分记录数: 800
[INFO] 数据统计:
  - 用户数: 100
  - 视频数: 200
  - 评分记录数: 800
[INFO] 执行评分聚合 (取最高分)...
[INFO] 聚合前记录数: 800
[INFO] 聚合后记录数: 750 (去重: 50 条)
[INFO] 开始数据质量过滤 (用户行为>=5, 视频互动>=5)...
[INFO] 活跃用户数 (行为>=5): 80
[INFO] 活跃视频数 (互动>=5): 150
[INFO] 质量过滤后的评分记录数: 600 (过滤: 150 条)
[INFO] ========== 训练数据统计 ==========
  - 用户数: 80
  - 视频数: 150
  - 评分记录数: 600
  - 平均评分: 4.20
  - 评分范围: [3.0, 5.0]
  - 数据密度: 5.00%
==========================================
[INFO] 数据集划分:
  - 训练集: 480 条 (80.0%)
  - 测试集: 120 条 (20.0%)
[INFO] 开始训练 ALS 模型 (rank=20, maxIter=10, regParam=0.1)...
[INFO] 模型训练完成
[INFO] 模型 RMSE: 0.5234
[INFO] 为所有用户生成 Top-20 推荐列表...
[INFO] 推荐结果统计:
  - 获得推荐的用户数: 80
  - 每个用户推荐数: 20
[INFO] 保存模型到: hdfs://localhost:9000/short-video/als-model/model-2025-01-20
[INFO] 开始保存模型参数和推荐结果到 MySQL...
[INFO] 模型参数已保存: als-1737360000000
[INFO] 开始写入推荐结果 (类型: OFFLINE)...
[SUCCESS] 离线推荐流程执行完毕！
================================================================================
[INFO] 最终统计:
  - 模型ID: als-1737360000000
  - 模型路径: hdfs://localhost:9000/short-video/als-model/model-2025-01-20
  - 模型RMSE: 0.5234
  - 推荐用户数: 80
  - 推荐总数: 1600
================================================================================
```

## 🔍 验证步骤

### 1. 验证 HDFS 数据

```bash
# 检查 HDFS 日志文件
hadoop fs -ls hdfs://localhost:9000/short-video/behavior/logs/

# 查看具体日期目录
hadoop fs -ls hdfs://localhost:9000/short-video/behavior/logs/2025-01-20/

# 查看日志文件内容（前10行）
hadoop fs -cat hdfs://localhost:9000/short-video/behavior/logs/2025-01-20/*.json | head -10
```

### 2. 验证模型保存

```bash
# 检查模型是否保存到 HDFS
hadoop fs -ls hdfs://localhost:9000/short-video/als-model/

# 查看模型目录
hadoop fs -ls hdfs://localhost:9000/short-video/als-model/model-2025-01-20/
```

### 3. 验证 MySQL 数据

```sql
-- 检查模型参数
SELECT * FROM model_params ORDER BY create_time DESC LIMIT 5;

-- 检查推荐结果
SELECT COUNT(*) as total_recommendations, 
       COUNT(DISTINCT user_id) as user_count,
       COUNT(DISTINCT video_id) as video_count
FROM recommendation_result 
WHERE type = 'OFFLINE';

-- 查看推荐结果示例
SELECT * FROM recommendation_result 
WHERE type = 'OFFLINE' 
ORDER BY user_id, `rank` 
LIMIT 20;
```

## 📝 代码变更总结

### DataProcessor.scala
- ✅ 增强 `getBehaviorWeight` 方法，支持字符串和整数格式
- ✅ 修复字段映射（videoId/mid, behaviorTime/createDate）
- ✅ 改进时间戳处理逻辑
- ✅ 添加详细的统计信息输出
- ✅ 改进错误处理和日志

### ALSTrainer.scala
- ✅ 修复 HDFS 路径配置（支持日期分区）
- ✅ 添加详细的统计信息输出
- ✅ 改进配置参数显示
- ✅ 添加最终统计信息

### MySQLWriter.scala
- ✅ 字段名已正确（使用 `video_id`）
- ⚠️ 需要确认数据库连接配置（端口、密码）

## 🚀 下一步

1. **确认 MySQL 连接配置**：端口、密码、数据库名
2. **验证 HDFS 路径**：确认实际路径结构
3. **测试运行**：使用实际数据测试 ALS 训练流程
4. **性能优化**（可选）：根据数据量调整 Spark 参数
