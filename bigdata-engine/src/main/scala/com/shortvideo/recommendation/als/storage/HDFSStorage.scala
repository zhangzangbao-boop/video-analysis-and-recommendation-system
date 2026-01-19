package com.shortvideo.recommendation.als.storage

import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.spark.sql.SparkSession

/**
 * HDFS 存储工具类
 * 修正说明：
 * 1. 移除了 setModelMetadata 方法 (元数据现已按文档要求存入 MySQL)
 * 2. 保留文件系统基础操作，用于模型保存前的路径清理
 */
object HDFSStorage {

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
   * 删除 HDFS 路径
   * (在 ALSTrainer 中保存新模型前，可选择调用此方法清理旧路径，
   * 虽然 Spark write.overwrite() 通常能处理，但显式清理更安全)
   *
   * @param spark SparkSession
   * @param path  HDFS 路径
   */
  def deletePath(spark: SparkSession, path: String): Unit = {
    val fs = FileSystem.get(spark.sparkContext.hadoopConfiguration)
    val hdfsPath = new Path(path)
    if (fs.exists(hdfsPath)) {
      fs.delete(hdfsPath, true)
      println(s"[INFO] 已清理旧的 HDFS 路径: $path")
    }
  }

  /**
   * 列出 HDFS 目录下的所有文件 (保留作为运维工具方法)
   */
  def listFiles(spark: SparkSession, path: String): Array[String] = {
    val fs = FileSystem.get(spark.sparkContext.hadoopConfiguration)
    val hdfsPath = new Path(path)
    if (fs.exists(hdfsPath)) {
      val status = fs.listStatus(hdfsPath)
      status.map(_.getPath.toString)
    } else {
      Array.empty[String]
    }
  }
}