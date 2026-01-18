package com.shortvideo.recommendation.offline.ods

import org.apache.spark.sql.SparkSession

/**
 * ODS 层作业：原始日志挂载
 * 作用：将 Flume 采集到 HDFS 的 JSON 日志映射为 Hive ODS 表，并维护日期分区。
 * 频率：每日执行 (T+1)
 */
object OdsLogToHdfs {

  def main(args: Array[String]): Unit = {
    // 启用 Hive 支持
    val spark = SparkSession.builder()
      .appName("OdsLogLayer")
      .master("local[*]")
      .enableHiveSupport() // 关键：开启 Hive 元数据支持
      .config("spark.sql.warehouse.dir", "hdfs://localhost:9000/user/hive/warehouse")
      .getOrCreate()

    // 1. 创建数仓数据库
    spark.sql("CREATE DATABASE IF NOT EXISTS short_video_dw")
    spark.sql("USE short_video_dw")

    println("[INFO] 开始构建 ODS 层表结构...")

    // 2. 创建 ODS 外部表 (指向 Flume 的输出目录)
    // 注意：Flume 输出的是 JSON 文本，这里使用 textfile 格式读取，后续在 DWD 解析
    // 假设 Flume 路径结构: /short-video/behavior/logs/2026-01-15/...
    spark.sql(
      """
        |CREATE EXTERNAL TABLE IF NOT EXISTS ods_behavior_log_json (
        |  json_line STRING COMMENT '原始JSON日志行'
        |)
        |COMMENT '用户行为原始日志表(ODS)'
        |PARTITIONED BY (dt STRING)
        |STORED AS TEXTFILE
        |LOCATION 'hdfs://localhost:9000/short-video/behavior/logs/'
      """.stripMargin)

    // 3. 修复分区 (MSCK REPAIR TABLE)
    // 作用：让 Hive 自动发现 HDFS 目录中新增的日期分区 (如 dt=2026-01-16)
    // 注意：Flume 的目录结构最好调整为 /.../logs/dt=2026-01-16/，或者手动添加分区
    // 如果 Flume 是按 /logs/2026-01-16/ 目录生成的，需要使用 ALTER TABLE ADD PARTITION 指令

    println("[INFO] 正在加载新分区...")

    // 方案 A: 如果 Flume 路径是标准的 Hive 格式 (logs/dt=2026-01-16)
    // spark.sql("MSCK REPAIR TABLE ods_behavior_log_json")

    // 方案 B: 如果 Flume 路径是简单日期 (logs/2026-01-16)，则需要手动挂载
    // 这里演示自动挂载当前日期的逻辑
    val today = java.time.LocalDate.now().toString
    val partitionPath = s"hdfs://localhost:9000/short-video/behavior/logs/$today"

    // 检查路径是否存在，存在则挂载
    try {
      spark.sql(s"ALTER TABLE ods_behavior_log_json ADD IF NOT EXISTS PARTITION (dt='$today') LOCATION '$partitionPath'")
      println(s"[SUCCESS] 分区 dt=$today 已挂载")
    } catch {
      case e: Exception => println(s"[WARN] 分区挂载失败 (可能是目录不存在): ${e.getMessage}")
    }

    spark.stop()
  }
}