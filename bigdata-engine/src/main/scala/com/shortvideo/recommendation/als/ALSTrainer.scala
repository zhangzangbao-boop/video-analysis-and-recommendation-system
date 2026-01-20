package com.shortvideo.recommendation.als

import com.shortvideo.recommendation.als.storage.{HDFSStorage, MySQLWriter}
import org.apache.spark.ml.recommendation.ALS
import org.apache.spark.sql.SparkSession
import java.time.LocalDate

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
      .config("spark.hadoop.fs.defaultFS", "hdfs://localhost:9000")
      .getOrCreate()

    import spark.implicits._

    // 2. 配置参数 (严格对应文档 2.4)
    val today = LocalDate.now().toString
    // 文档指定读取路径：/short-video/behavior/*/*.log
    // 这里为了演示方便，读取所有历史数据，实际生产中可能限制日期
    val inputPath = "/short-video/behavior/*/*.log"
    val modelPath = s"hdfs://localhost:9000/short-video/als-model/model-$today"

    // ALS 超参数 (文档指定)
    val rank = 20          // 文档要求: 20
    val maxIter = 10       // 文档要求: 10
    val regParam = 0.1     // 文档要求: 0.1
    val topN = 20          // 文档要求: 20

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
      println(s"[INFO] 最终训练样本数: $count")

      // ============================================
      // 4. 划分数据集与训练 (对应流程图 Step 7-8)
      // ============================================
      // Step 7: 划分训练集和测试集 (80% 训练, 20% 测试)
      val Array(training, test) = trainingDataFull.randomSplit(Array(0.8, 0.2), seed = 1234L)

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

      // Step 11: 保存模型到 HDFS
      println(s"[INFO] 保存模型到: $modelPath")
      model.write.overwrite().save(modelPath)

      // Step 12 & 13: 写入 MySQL (推荐结果 + 模型参数)
      // 写入推荐结果 type=OFFLINE
      val modelId = MySQLWriter.writeModelParamsToMySQL(modelPath, rank, regParam, maxIter, rmse)
      MySQLWriter.writeRecommendationsToMySQL(userRecs, "OFFLINE", modelId)

      println("[SUCCESS] 离线推荐流程执行完毕！")

    } catch {
      case e: Exception =>
        e.printStackTrace()
        println(s"[ERROR] 任务失败: ${e.getMessage}")
    } finally {
      spark.stop()
    }
  }
}