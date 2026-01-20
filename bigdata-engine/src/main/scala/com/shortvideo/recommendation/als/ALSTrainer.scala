package com.shortvideo.recommendation.als

import com.shortvideo.recommendation.als.storage.{HDFSStorage, MySQLWriter}
import org.apache.spark.ml.recommendation.ALS
import org.apache.spark.sql.SparkSession
import java.time.LocalDate
import org.apache.spark.sql.functions.{avg, min, max}

/**
 * Spark ALS 离线推荐训练主程序
 * 修正说明：严格按照《技术交接文档》2.4 节流程实现
 * 流程：读取HDFS日志 -> 解析 -> 聚合 -> 过滤 -> 训练 -> 评估 -> 存储
 */
object ALSTrainer {

  def main(args: Array[String]): Unit = {
    println("=" * 80)
    println("Spark ALS 离线推荐训练 (文档标准版)")
    println("=" * 80)

    // 1. 初始化 Spark
    val spark = SparkSession.builder()
      .appName("ShortVideoALSRecommendation")
      // 生产环境通常由 spark-submit 指定 master，这里保留 local[*] 方便调试
      .master("local[*]")
      .config("spark.sql.shuffle.partitions", "100")
      .getOrCreate()

    import spark.implicits._

    // 2. 配置参数 (严格对应文档 2.4)
    val today = LocalDate.now().toString
    
    // ALS 超参数 (文档指定) - 必须先定义，后面会使用
    val rank = 20          // 文档要求: 20
    val maxIter = 10       // 文档要求: 10
    val regParam = 0.1     // 文档要求: 0.1
    val topN = 20          // 文档要求: 20
    
    // 文档指定读取路径：/short-video/behavior/logs/%Y-%m-%d/*.json
    // 支持两种路径格式：
    // 1. 按日期分区：hdfs://localhost:9000/short-video/behavior/logs/2025-01-20/*.json
    // 2. 通配符匹配：hdfs://localhost:9000/short-video/behavior/logs/*/*.json (读取所有历史数据)
    val inputPath = if (args.length > 0 && args(0).nonEmpty) {
      args(0) // 允许通过命令行参数指定路径
    } else {
      // 默认使用通配符读取所有历史数据（按日期分区）
      "hdfs://localhost:9000/short-video/behavior/logs/*/*.json"
    }
    val modelPath = s"hdfs://localhost:9000/short-video/als-model/model-$today"

    println(s"[INFO] 配置参数:")
    println(s"  - HDFS 输入路径: $inputPath")
    println(s"  - 模型保存路径: $modelPath")
    println(s"  - ALS 参数: rank=$rank, maxIter=$maxIter, regParam=$regParam, topN=$topN")

    try {
      // ============================================
      // 3. 数据处理流程 (对应流程图 Step 2-6)
      // ============================================

      // Step 2 & 3 & 4: 读取 HDFS -> 解析 -> 转换评分
      val rawRatings = DataProcessor.readAndParseBehaviorLogs(spark, inputPath)

      if (rawRatings.isEmpty) {
        throw new RuntimeException(s"路径 $inputPath 下未找到有效行为数据！")
      }

      // Step 5: 聚合评分 (同一用户对同一视频取最高分)
      val aggregatedRatings = DataProcessor.aggregateRatings(rawRatings)

      // Step 6: 数据质量过滤 (用户行为>=5, 视频互动>=5)
      val trainingDataFull = DataProcessor.filterByQuality(aggregatedRatings, 5, 5)

      val count = trainingDataFull.count()
      if (count == 0) {
        throw new RuntimeException("经过去重和过滤后，训练数据集为空！")
      }
      
      // 输出详细统计信息
      val finalUserCount = trainingDataFull.select("userId").distinct().count()
      val finalMovieCount = trainingDataFull.select("movieId").distinct().count()
      val avgRating = trainingDataFull.select(avg("rating")).first().getDouble(0)
      val minRating = trainingDataFull.select(min("rating")).first().getFloat(0)
      val maxRating = trainingDataFull.select(max("rating")).first().getFloat(0)
      
      println(s"[INFO] ========== 训练数据统计 ==========")
      println(s"  - 用户数: $finalUserCount")
      println(s"  - 视频数: $finalMovieCount")
      println(s"  - 评分记录数: $count")
      println(s"  - 平均评分: ${avgRating.formatted("%.2f")}")
      println(s"  - 评分范围: [$minRating, $maxRating]")
      println(s"  - 数据密度: ${(count.toDouble / (finalUserCount * finalMovieCount) * 100).formatted("%.4f")}%")
      println(s"==========================================")

      // ============================================
      // 4. 划分数据集与训练 (对应流程图 Step 7-8)
      // ============================================
      // Step 7: 划分训练集和测试集 (80% 训练, 20% 测试)
      val Array(training, test) = trainingDataFull.randomSplit(Array(0.8, 0.2), seed = 1234L)
      
      val trainingCount = training.count()
      val testCount = test.count()
      println(s"[INFO] 数据集划分:")
      println(s"  - 训练集: $trainingCount 条 (${(trainingCount.toDouble / count * 100).formatted("%.1f")}%)")
      println(s"  - 测试集: $testCount 条 (${(testCount.toDouble / count * 100).formatted("%.1f")}%)")

      println(s"[INFO] 开始训练 ALS 模型 (rank=$rank, maxIter=$maxIter, regParam=$regParam)...")
      val als = new ALS()
        .setMaxIter(maxIter)
        .setRegParam(regParam)
        .setRank(rank)
        .setUserCol("userId")
        .setItemCol("movieId")
        .setRatingCol("rating")
        .setColdStartStrategy("drop")

      val model = als.fit(training)
      println("[INFO] 模型训练完成")

      // ============================================
      // 5. 评估与保存 (对应流程图 Step 9-13)
      // ============================================
      // Step 9: 模型评估
      val predictions = model.transform(test)
      val rmse = ModelEvaluator.evaluateRMSE(predictions)
      println(s"[INFO] 模型 RMSE: $rmse")

      // Step 10: 生成推荐列表 (Top-20)
      println(s"[INFO] 为所有用户生成 Top-$topN 推荐列表...")
      val userRecs = model.recommendForAllUsers(topN)
      
      val recUserCount = userRecs.count()
      println(s"[INFO] 推荐结果统计:")
      println(s"  - 获得推荐的用户数: $recUserCount")
      println(s"  - 每个用户推荐数: $topN")

      // Step 11: 保存模型到 HDFS
      println(s"[INFO] 保存模型到: $modelPath")
      model.write.overwrite().save(modelPath)

      // Step 12 & 13: 写入 MySQL (推荐结果 + 模型参数)
      // 写入推荐结果 type=OFFLINE
      println("[INFO] 开始保存模型参数和推荐结果到 MySQL...")
      val modelId = MySQLWriter.writeModelParamsToMySQL(modelPath, rank, regParam, maxIter, rmse)
      MySQLWriter.writeRecommendationsToMySQL(userRecs, "OFFLINE", modelId)

      // 输出最终统计信息
      println("=" * 80)
      println("[SUCCESS] 离线推荐流程执行完毕！")
      println("=" * 80)
      println("[INFO] 最终统计:")
      println(s"  - 模型ID: $modelId")
      println(s"  - 模型路径: $modelPath")
      println(s"  - 模型RMSE: ${if (rmse >= 0) rmse.formatted("%.4f") else "N/A"}")
      println(s"  - 推荐用户数: $recUserCount")
      println(s"  - 推荐总数: ${recUserCount * topN}")
      println("=" * 80)

    } catch {
      case e: Exception =>
        e.printStackTrace()
        println(s"[ERROR] 任务失败: ${e.getMessage}")
    } finally {
      spark.stop()
    }
  }
}