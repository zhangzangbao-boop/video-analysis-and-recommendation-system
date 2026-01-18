package com.shortvideo.recommendation.als

import org.apache.spark.ml.evaluation.RegressionEvaluator
import org.apache.spark.sql.Dataset

/**
 * 模型评估工具类
 */
object ModelEvaluator {

  /**
   * 计算 RMSE (均方根误差)
   *
   * @param predictions 包含 "rating" (真实值) 和 "prediction" (预测值) 列的 Dataset
   * @return RMSE 值
   */
  def evaluateRMSE(predictions: Dataset[_]): Double = {
    val evaluator = new RegressionEvaluator()
      .setMetricName("rmse")
      .setLabelCol("rating")
      .setPredictionCol("prediction")

    val rmse = evaluator.evaluate(predictions)

    // 如果结果是 NaN (通常因为测试集数据太少或冷启动)，返回 -1 或打印警告
    if (rmse.isNaN) {
      println("[WARN] 模型评估结果为 NaN，可能是由于数据稀疏导致")
      -1.0
    } else {
      rmse
    }
  }
}