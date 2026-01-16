package com.shortvideo.recommendation.als


import com.shortvideo.recommendation.als.DataProcessor._
import com.shortvideo.recommendation.als.storage.{HDFSStorage, MySQLWriter}
import org.apache.spark.ml.evaluation.RegressionEvaluator
import org.apache.spark.ml.recommendation.ALS
import org.apache.spark.sql.SparkSession

/**
 * Spark ALS 离线推荐系统训练主程序
 * 核心业务流程：
 * 1. 初始化Spark运行环境
 * 2. 读取HDFS上的短视频用户行为日志
 * 3. 数据预处理（解析、聚合、过滤低质量数据）
 * 4. 训练ALS协同过滤推荐模型
 * 5. 评估模型效果（RMSE指标）
 * 6. 生成用户Top-N推荐列表
 * 7. 保存模型到HDFS、保存推荐结果/模型参数到MySQL
 * 适用场景：短视频平台离线推荐模型训练（每日/定时执行）
 */
object ALSTrainer {

  def main(args: Array[String]): Unit = {
    // 打印启动日志：便于日志检索和可视化训练开始
    println("=" * 80)
    println("Spark ALS 推荐系统训练开始")
    println("=" * 80)

    // ============================================
    // 1. 初始化 Spark Session（Spark应用的核心入口）
    // ============================================
    val spark = SparkSession.builder()
      .appName("ShortVideoALSRecommendation")  // 应用名称：便于YARN/Spark UI识别
      .master("local[*]")                      // 运行模式：本地模式（生产环境需改为"yarn"）
      .config("spark.sql.shuffle.partitions", "100")  // 洗牌分区数：平衡并行度和资源占用
      .config("spark.sql.adaptive.enabled", "true")   // 开启自适应执行：优化查询性能
      .config("spark.sql.warehouse.dir", "D:\\spark_temp")  // Spark SQL仓库目录（Windows本地路径）
      .config("spark.worker.cleanup.enabled", "false")  // 禁用Worker清理：避免Windows文件锁问题
      .getOrCreate()  // 创建/获取SparkSession（单例模式）

    // 导入Spark隐式转换：支持Dataset/DataFrame的算子操作（如map、filter、join等）
    import spark.implicits._

    println("[INFO] Spark Session 初始化完成")

    // ============================================
    // 2. 配置核心参数（可抽离为配置文件，便于运维调整）
    // ============================================
    // 日志日期：取当天日期（minusDays(0)），生产环境可改为前一天（minusDays(1)）处理昨日数据
    val yesterday = java.time.LocalDate.now().minusDays(0).toString
    // HDFS日志路径：按日期分区读取，支持通配符匹配当日所有.log文件
    val hdfsPath = s"hdfs://localhost:9000/short-video/behavior/logs/$yesterday/*.log"
    // 模型保存路径：HDFS路径，添加时间戳避免模型覆盖
    val modelPath = s"hdfs://localhost:9000/short-video/als-model/model-${yesterday}"
    val topN = 20  // 每个用户推荐的视频数量（业务需求：推荐20个视频）

    // ALS模型核心超参数（调优重点）
    val rank = 20        // 隐因子数量：20维向量表征用户/视频特征（短视频场景经验值）
    val maxIter = 10     // 最大迭代次数：平衡训练效果和耗时
    val regParam = 0.1   // 正则化参数：防止模型过拟合

    // 打印配置参数：便于日志排查参数是否正确
    println(s"[INFO] 配置参数:")
    println(s"  - HDFS 路径: $hdfsPath")
    println(s"  - 模型路径: $modelPath")
    println(s"  - 推荐数量: $topN")
    println(s"  - Rank: $rank")
    println(s"  - MaxIter: $maxIter")
    println(s"  - RegParam: $regParam")

    try {
      // ============================================
      // 3. 数据预处理流水线（核心数据清洗逻辑）
      // ============================================
      // 步骤1：读取HDFS日志并解析为Rating格式（隐式行为→显式评分）
      val ratings = readAndParseBehaviorLogs(spark, hdfsPath)
      // 步骤2：聚合同一用户对同一视频的多次行为（保留最高评分）
      val aggregatedRatings = aggregateRatings(ratings)
      // 步骤3：过滤低质量数据（用户/视频行为数≥5次）
      val finalRatings = filterByQuality(aggregatedRatings, minUserBehaviors = 5, minMovieBehaviors = 5)

      // 打印最终训练数据量：监控数据规模
      println(s"[INFO] 最终用于训练的评分记录数: ${finalRatings.count()}")

      // ============================================
      // 4. 划分训练集和测试集（模型评估必备）
      // ============================================
      // randomSplit：按8:2比例划分训练集/测试集，seed=42保证结果可复现
      val Array(trainingData, testData) = finalRatings.randomSplit(Array(0.8, 0.2), seed = 42)
      println(s"[INFO] 训练集记录数: ${trainingData.count()}")
      println(s"[INFO] 测试集记录数: ${testData.count()}")

      // ============================================
      // 5. 配置并训练 ALS 模型（核心推荐算法）
      // ============================================
      println("[INFO] 开始训练 ALS 模型...")

      // 初始化ALS模型实例
      val als = new ALS()
        .setMaxIter(maxIter)                // 设置最大迭代次数
        .setRegParam(regParam)              // 设置正则化参数
        .setRank(rank)                      // 设置隐因子数量
        .setUserCol("userId")               // 指定用户ID字段名
        .setItemCol("movieId")              // 指定视频ID字段名（与Rating类字段一致）
        .setRatingCol("rating")             // 指定评分字段名
        .setColdStartStrategy("drop")       // 冷启动策略：丢弃无评分的用户/视频（避免NaN预测值）

      // 训练模型：使用训练集拟合ALS模型
      val model = als.fit(trainingData)

      println("[INFO] ALS 模型训练完成")

      // ============================================
      // 6. 模型评估（通过RMSE衡量预测精度）
      // ============================================
      println("[INFO] 开始模型评估...")

      // 用测试集生成预测结果（真实评分 vs 模型预测评分）
      val predictions = model.transform(testData)
      // 显示前10条预测结果：便于直观查看预测效果
      predictions.show(10, truncate = false)

      // 初始化回归评估器（用于计算RMSE）
      val evaluator = new RegressionEvaluator()
        .setMetricName("rmse")              // 评估指标：均方根误差（RMSE）
        .setLabelCol("rating")              // 真实标签列（原始评分）
        .setPredictionCol("prediction")     // 预测结果列（模型输出）

      // 计算RMSE：值越小，模型预测越准确
      val rmse = evaluator.evaluate(predictions)
      println(s"[INFO] 模型 RMSE = $rmse")

      // ============================================
      // 7. 生成推荐结果（为所有用户生成Top-N推荐）
      // ============================================
      println(s"[INFO] 开始生成 Top-$topN 推荐列表...")

      // 为所有用户生成Top-N推荐视频
      val userRecs = model.recommendForAllUsers(topN)
      // 打印推荐结果统计信息：监控推荐用户数
      println(s"[INFO] 推荐列表用户数: ${userRecs.count()}")
      // 打印推荐结果Schema：便于确认数据结构
      println(s"[INFO] 推荐结果 Schema: ${userRecs.schema.treeString}")

      // 显示前10条推荐结果：直观查看推荐格式
      userRecs.show(10, truncate = false)

      // ============================================
      // 8. 保存模型到 HDFS（持久化模型，便于线上服务加载）
      // ============================================
      println(s"[INFO] 保存模型到 HDFS: $modelPath")
      // 保存模型（overwrite：覆盖已有路径，避免报错）
      model.write.overwrite().save(modelPath)
      // 保存模型元数据（如rank、RMSE等）到HDFS：便于模型版本管理
      HDFSStorage.setModelMetadata(spark, modelPath, rank, regParam, maxIter, rmse)

      // ============================================
      // 8.1 保存模型参数到 MySQL（便于业务系统查询模型信息）
      // ============================================
      println("[INFO] 开始保存模型参数到 MySQL...")
      // 写入模型参数到MySQL，并返回模型ID（主键）
      val modelId = MySQLWriter.writeModelParamsToMySQL(modelPath, rank, regParam, maxIter, rmse)
      println(s"[INFO] 模型参数已保存到 MySQL，Model ID: $modelId")

      // ============================================
      // 9. 将推荐结果写入 MySQL（供线上推荐服务查询）
      // ============================================
      println("[INFO] 开始写入推荐结果到 MySQL...")
      // 写入推荐列表到MySQL，关联model_id便于追溯
      MySQLWriter.writeRecommendationsToMySQL(userRecs, "OFFLINE", modelId)
      println("[INFO] 推荐结果写入完成")

      // ============================================
      // 10. 打印训练统计信息（便于监控和复盘）
      // ============================================
      println("[INFO] 训练统计:")
      println(s"  - 总用户数: ${finalRatings.select("userId").distinct().count()}")
      println(s"  - 总视频数: ${finalRatings.select("movieId").distinct().count()}")
      println(s"  - 总评分数: ${finalRatings.count()}")
      println(s"  - 模型 RMSE: $rmse")
      println(s"  - 模型路径: $modelPath")
      println(s"  - 模型 ID: $modelId")

    } catch {
      // 异常处理：捕获训练过程中的所有异常，打印日志并终止程序
      case e: Exception =>
        println(s"[ERROR] 训练过程发生异常: ${e.getMessage}")
        e.printStackTrace()  // 打印异常堆栈：便于定位问题
        throw e              // 抛出异常：让Spark任务以失败状态结束（便于监控告警）
    } finally {
      // ============================================
      // 11. 关闭 Spark Session（释放资源）
      // ============================================
      spark.stop()
      println("[INFO] Spark Session 已关闭")
    }

    // 打印结束日志：标识训练流程完成
    println("=" * 80)
    println("Spark ALS 推荐系统训练完成")
    println("=" * 80)
  }
}