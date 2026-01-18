package com.shortvideo.recommendation.offline.dwd

import org.apache.spark.sql.{SaveMode, SparkSession}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._

/**
 * DWD 层作业：日志清洗与明细化
 * 作用：解析 ODS 层 JSON 数据，过滤脏数据，写入 Parquet 格式的 DWD 表。
 */
object DwdLogClean {

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("DwdLogLayer")
      .master("local[*]")
      .enableHiveSupport()
      .getOrCreate()

    import spark.implicits._

    spark.sql("USE short_video_dw")

    // 1. 创建 DWD 表 (Parquet 格式)
    spark.sql(
      """
        |CREATE TABLE IF NOT EXISTS dwd_behavior_log (
        |  user_id BIGINT COMMENT '用户ID',
        |  video_id BIGINT COMMENT '视频ID',
        |  behavior_type STRING COMMENT '行为类型',
        |  behavior_time BIGINT COMMENT '行为时间戳',
        |  duration INT COMMENT '观看时长',
        |  device_info STRING COMMENT '设备信息',
        |  ip_address STRING COMMENT 'IP地址',
        |  province STRING COMMENT '省份'
        |)
        |COMMENT '用户行为明细表(DWD)'
        |PARTITIONED BY (dt STRING)
        |STORED AS PARQUET
      """.stripMargin)

    // 2. 读取 ODS 数据并解析
    // 假设我们要处理 "今天" 的分区
    val today = java.time.LocalDate.now().toString
    println(s"[INFO] 开始清洗处理分区: dt=$today")

    try {
      val odsDf = spark.sql(s"SELECT json_line FROM ods_behavior_log_json WHERE dt='$today'")

      // 定义 JSON Schema
      val schema = new StructType()
        .add("userId", LongType)
        .add("videoId", LongType)
        .add("behaviorType", StringType)
        .add("behaviorTime", StringType) // 假设原始是字符串时间或时间戳
        .add("duration", IntegerType)
        .add("deviceInfo", StringType)
        .add("ipAddress", StringType)
        .add("location", StringType)

      // 解析 JSON
      val parsedDf = odsDf.select(from_json(col("json_line"), schema).as("data")).select("data.*")

      // 3. 清洗逻辑
      val cleanDf = parsedDf
        .filter(col("userId").isNotNull && col("videoId").isNotNull) // 过滤脏数据
        .withColumn("dt", lit(today)) // 添加分区列
        // 简单处理 location -> province (实际可能需要复杂逻辑)
        .withColumn("province", split(col("location"), "-").getItem(0))
        // 处理时间: 假设 behaviorTime 是 "yyyy-MM-dd HH:mm:ss" 或 时间戳
        // 这里做一个兼容处理：如果是字符串转时间戳，如果是数字直接用
        .withColumn("behavior_time_long", unix_timestamp(col("behaviorTime"), "yyyy-MM-dd HH:mm:ss"))
        // 最终选出目标字段
        .select(
          col("userId").as("user_id"),
          col("videoId").as("video_id"),
          col("behaviorType").as("behavior_type"),
          coalesce(col("behavior_time_long"), col("behaviorTime").cast(LongType), lit(System.currentTimeMillis())).as("behavior_time"),
          col("duration"),
          col("deviceInfo").as("device_info"),
          col("ipAddress").as("ip_address"),
          col("province"),
          col("dt")
        )

      // 4. 写入 DWD 表
      cleanDf.write
        .mode(SaveMode.Overwrite)
        .partitionBy("dt")
        .format("parquet")
        .saveAsTable("dwd_behavior_log")

      println(s"[SUCCESS] DWD 清洗完成，写入条数: ${cleanDf.count()}")

    } catch {
      case e: Exception =>
        e.printStackTrace()
        println(s"[ERROR] DWD 清洗失败: ${e.getMessage}")
    }

    spark.stop()
  }
}