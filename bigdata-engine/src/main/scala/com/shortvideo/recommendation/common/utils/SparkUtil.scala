package com.shortvideo.recommendation.common.utils

import org.apache.spark.sql.{SparkSession, DataFrame}
import org.apache.spark.streaming.{StreamingContext, Seconds}
import org.apache.spark.SparkConf
import com.shortvideo.recommendation.common.Constants
import com.shortvideo.recommendation.common.config.SparkConfig

/**
 * Spark工具类
 */
object SparkUtils {

  /**
   * 创建SparkSession（批处理）
   */
  def createSparkSession(appName: String = Constants.APP_NAME,
                         master: String = "local[*]"): SparkSession = {

    val sparkConf = new SparkConf()
      .setAppName(appName)
      .setMaster(master)
      .set("spark.sql.warehouse.dir", "/user/hive/warehouse")
      .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .set("spark.kryoserializer.buffer.max", "256m")
      .set("spark.sql.shuffle.partitions", "200")
      .set("spark.default.parallelism", "200")
      .set("spark.sql.adaptive.enabled", "true")
      .set("spark.sql.adaptive.coalescePartitions.enabled", "true")
      .set("spark.sql.adaptive.skewJoin.enabled", "true")
      .registerKryoClasses(Array(
        classOf[com.shortvideo.recommendation.common.entity.UserBehavior],
        classOf[com.shortvideo.recommendation.common.entity.UserRating],
        classOf[com.shortvideo.recommendation.common.entity.VideoInfo],
        classOf[com.shortvideo.recommendation.common.entity.Recommendation]
      ))

    // 设置日志级别
    if (master.contains("local")) {
      sparkConf.set("spark.driver.bindAddress", "127.0.0.1")
    }

    val spark = SparkSession.builder()
      .config(sparkConf)
      .enableHiveSupport()
      .getOrCreate()

    // 设置日志级别
    spark.sparkContext.setLogLevel("WARN")

    spark
  }

  /**
   * 创建StreamingContext
   */
  def createStreamingContext(appName: String = Constants.APP_NAME,
                             batchDuration: Int = 10): StreamingContext = {

    val sparkConf = new SparkConf()
      .setAppName(appName)
      .set("spark.streaming.backpressure.enabled", "true")
      .set("spark.streaming.kafka.maxRatePerPartition", "1000")
      .set("spark.streaming.stopGracefullyOnShutdown", "true")
      .set("spark.streaming.receiver.writeAheadLog.enable", "true")

    val ssc = new StreamingContext(sparkConf, Seconds(batchDuration))

    // 设置检查点
    val checkpointPath = s"${Constants.HDFS.LOG_PATH}/checkpoint/$appName"
    ssc.checkpoint(checkpointPath)

    ssc
  }

  /**
   * 从配置创建SparkSession
   */
  def createSparkSessionFromConfig(config: SparkConfig): SparkSession = {
    val sparkConf = new SparkConf()

    // 设置基础配置
    sparkConf.setAppName(config.appName)

    if (config.master.nonEmpty) {
      sparkConf.setMaster(config.master)
    }

    // 设置其他配置
    config.getConfigMap.foreach { case (key, value) =>
      sparkConf.set(key, value)
    }

    // 创建SparkSession
    val builder = SparkSession.builder()
      .config(sparkConf)

    if (config.enableHiveSupport) {
      builder.enableHiveSupport()
    }

    val spark = builder.getOrCreate()

    // 设置日志级别
    spark.sparkContext.setLogLevel(config.logLevel)

    spark
  }

  /**
   * 优雅关闭Spark应用
   */
  def stopSparkSession(spark: SparkSession): Unit = {
    if (spark != null) {
      spark.stop()
    }
  }

  /**
   * 优雅关闭StreamingContext
   */
  def stopStreamingContext(ssc: StreamingContext): Unit = {
    if (ssc != null) {
      ssc.stop(stopSparkContext = true, stopGracefully = true)
    }
  }

  /**
   * 读取HDFS数据
   */
  def readHdfsData(spark: SparkSession, path: String, format: String = "parquet"): DataFrame = {
    spark.read.format(format).load(path)
  }

  /**
   * 写入HDFS数据
   */
  def writeHdfsData(df: DataFrame, path: String, format: String = "parquet",
                    mode: String = "overwrite", partitionBy: Seq[String] = Seq.empty): Unit = {

    val writer = df.write
      .format(format)
      .mode(mode)

    if (partitionBy.nonEmpty) {
      writer.partitionBy(partitionBy: _*)
    }

    writer.save(path)
  }

  /**
   * 打印DataFrame Schema和信息
   */
  def printDataFrameInfo(df: DataFrame, name: String = "DataFrame"): Unit = {
    println(s"=== $name Schema ===")
    df.printSchema()

    println(s"=== $name Count ===")
    println(s"Count: ${df.count()}")

    if (df.count() > 0) {
      println(s"=== $name Sample Data ===")
      df.show(5, truncate = false)
    }
  }
}
