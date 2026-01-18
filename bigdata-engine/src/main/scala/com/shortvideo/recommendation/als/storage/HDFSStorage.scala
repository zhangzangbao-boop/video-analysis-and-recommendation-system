package com.shortvideo.recommendation.als.storage

import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.spark.sql.SparkSession

/**
 * HDFS 存储工具类
 * 负责与 HDFS 的交互操作
 */
object HDFSStorage {

  /**
   * 保存模型元数据到 HDFS
   *
   * @param spark     SparkSession
   * @param modelPath 模型路径
   * @param rank      隐因子数量
   * @param regParam  正则化参数
   * @param maxIter   最大迭代次数
   * @param rmse      模型评估指标
   */
  def setModelMetadata(
                        spark: SparkSession,
                        modelPath: String,
                        rank: Int,
                        regParam: Double,
                        maxIter: Int,
                        rmse: Double
                      ): Unit = {
    println(s"[INFO] 保存模型元数据到 HDFS: $modelPath/metadata.json")

    val metadata = s"""
                      |{
                      |  "modelPath": "$modelPath",
                      |  "rank": $rank,
                      |  "regParam": $regParam,
                      |  "maxIter": $maxIter,
                      |  "rmse": $rmse,
                      |  "trainingTime": "${System.currentTimeMillis()}"
                      |}
     """.stripMargin

    val rdd = spark.sparkContext.parallelize(Seq(metadata))
    rdd.saveAsTextFile(s"$modelPath/metadata.json")

    println("[INFO] 模型元数据保存完成")
  }

  /**
   * 检查 HDFS 路径是否存在
   *
   * @param spark SparkSession
   * @param path  HDFS 路径
   * @return 是否存在
   */
  def pathExists(spark: SparkSession, path: String): Boolean = {
    val fs = FileSystem.get(spark.sparkContext.hadoopConfiguration)
    fs.exists(new Path(path))
  }

  /**
   * 列出 HDFS 目录下的所有文件
   *
   * @param spark SparkSession
   * @param path  HDFS 路径
   * @return 文件路径列表
   */
  def listFiles(spark: SparkSession, path: String): Array[String] = {
    val fs = FileSystem.get(spark.sparkContext.hadoopConfiguration)
    val status = fs.listStatus(new Path(path))
    status.map(_.getPath.toString)
  }

  /**
   * 删除 HDFS 路径
   *
   * @param spark SparkSession
   * @param path  HDFS 路径
   */
  def deletePath(spark: SparkSession, path: String): Unit = {
    val fs = FileSystem.get(spark.sparkContext.hadoopConfiguration)
    if (fs.exists(new Path(path))) {
      fs.delete(new Path(path), true)
      println(s"[INFO] 已删除 HDFS 路径: $path")
    }
  }
}