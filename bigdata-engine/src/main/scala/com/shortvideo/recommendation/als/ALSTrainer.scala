package com.shortvideo.recommendation.als

import com.shortvideo.recommendation.als.storage.{HDFSStorage, MySQLWriter}
import org.apache.spark.ml.evaluation.RegressionEvaluator
import org.apache.spark.ml.recommendation.ALS
import org.apache.spark.sql.SparkSession

import java.time.LocalDate

/**
 * Spark ALS 离线推荐训练主程序
 * 修改说明：已升级为读取 DWS 层 Hive 表数据，不再直接读取 JSON
 */
object ALSTrainer {

  def main(args: Array[String]): Unit = {
    println("=" * 80)
    println("Spark ALS 离线推荐训练 (基于 DWS 数仓层)")
    println("=" * 80)

    // 1. 初始化 Spark (开启 Hive 支持)
    val spark = SparkSession.builder()
      .appName("ShortVideoALSRecommendation")
      .master("local[*]")
      .enableHiveSupport() // 关键：开启 Hive
      .config("spark.sql.shuffle.partitions", "100")
      .getOrCreate()

    // 引入隐式转换，用于 DataFrame 操作
    import spark.implicits._

    // 2. 配置参数
    val today = LocalDate.now().toString
    val modelPath = s"hdfs://localhost:9000/short-video/als-model/model-$today"

    // ALS 超参数
    val rank = 10
    val maxIter = 10
    val regParam = 0.1
    val topN = 20

    try {
      spark.sql("USE short_video_dw")

      // ============================================
      // 3. 读取训练数据 (从 DWS 宽表读取)
      // ============================================
      println(s"[INFO] 正在从 DWS 表加载数据 (dt=$today)...")

      // 直接使用 SQL 读取 DWS 层计算好的 final_score 作为 rating
      // 注意：如果今天的数据还没生成，可以尝试读取最近可用的分区，这里演示读取“今天”
      val ratings = spark.sql(
        s"""
           |SELECT
           |  user_id as userId,
           |  video_id as movieId,
           |  final_score as rating
           |FROM dws_user_video_interaction
           |WHERE dt = '$today'
        """.stripMargin).cache() // Cache 住，因为后面要用两次 (fit 和 count)

      val count = ratings.count()
      if (count == 0) {
        throw new RuntimeException(s"DWS 表分区 dt=$today 无数据！请先运行数仓调度任务。")
      }
      println(s"[INFO] 加载完成，训练样本数: $count")

      // ============================================
      // 4. 划分数据集与训练
      // ============================================
      val Array(training, test) = ratings.randomSplit(Array(0.8, 0.2), seed = 1234L)

      println("[INFO] 开始训练 ALS 模型...")
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
      // 5. 评估与保存 (保持原有逻辑)
      // ============================================
      val predictions = model.transform(test)
      val rmse = ModelEvaluator.evaluateRMSE(predictions) // 使用封装好的 Evaluator
      println(s"[INFO] 模型 RMSE: $rmse")

      // 保存模型
      model.write.overwrite().save(modelPath)

      // 生成并写入推荐结果
      println("[INFO] 生成 Top-N 推荐列表...")
      val userRecs = model.recommendForAllUsers(topN)

      // 写入 MySQL
      val modelId = MySQLWriter.writeModelParamsToMySQL(modelPath, rank, regParam, maxIter, rmse)
      MySQLWriter.writeRecommendationsToMySQL(userRecs, "OFFLINE", modelId)

      println("[SUCCESS] 离线推荐任务全部完成！")

    } catch {
      case e: Exception =>
        e.printStackTrace()
        println(s"[ERROR] 任务失败: ${e.getMessage}")
    } finally {
      spark.stop()
    }
  }
}