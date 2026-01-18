package com.shortvideo.recommendation.offline.dws

import org.apache.spark.sql.{SaveMode, SparkSession}
import org.apache.spark.sql.functions._

/**
 * DWS 层作业：用户-视频交互特征聚合
 * 作用：基于 DWD 明细，按 User-Video 维度聚合，计算隐式评分(Rating)
 * 输出：直接供 ALS 算法训练的宽表
 */
object DwsUserVideoFeature {

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("DwsFeatureLayer")
      .master("local[*]")
      .enableHiveSupport()
      .getOrCreate()

    import spark.implicits._
    spark.sql("USE short_video_dw")

    // 1. 创建 DWS 特征表
    spark.sql(
      """
        |CREATE TABLE IF NOT EXISTS dws_user_video_interaction (
        |  user_id BIGINT,
        |  video_id BIGINT,
        |  total_duration INT COMMENT '累计观看时长',
        |  play_count INT COMMENT '播放次数',
        |  is_like INT COMMENT '是否点赞',
        |  is_comment INT COMMENT '是否评论',
        |  is_share INT COMMENT '是否分享',
        |  final_score DOUBLE COMMENT '综合评分(用于ALS)',
        |  last_interact_time BIGINT COMMENT '最后交互时间'
        |)
        |COMMENT '用户视频交互特征聚合表(DWS)'
        |PARTITIONED BY (dt STRING)
        |STORED AS PARQUET
      """.stripMargin)

    val today = java.time.LocalDate.now().toString
    println(s"[INFO] 开始聚合计算特征: dt=$today")

    // 2. 读取 DWD 明细数据
    // 实际生产中可能需要读取过去 N 天的数据进行滑动窗口计算，这里演示读取当天
    val dwdDf = spark.sql(s"SELECT * FROM dwd_behavior_log WHERE dt='$today'")

    // 3. 聚合逻辑
    val aggDf = dwdDf
      .groupBy("user_id", "video_id")
      .agg(
        sum("duration").as("total_duration"),
        count("behavior_type").as("play_count"),
        max(when(col("behavior_type") === "like", 1).otherwise(0)).as("is_like"),
        max(when(col("behavior_type") === "comment", 1).otherwise(0)).as("is_comment"),
        max(when(col("behavior_type") === "share", 1).otherwise(0)).as("is_share"),
        max("behavior_time").as("last_interact_time")
      )

    // 4. 计算综合评分 (特征工程核心)
    // 评分公式: 播放(1分) + 点赞(5分) + 评论(3分) + 分享(4分)
    // 另外可以加入完播率或时长权重的逻辑，这里简化处理
    val featureDf = aggDf.withColumn("final_score",
      lit(1.0) * when(col("play_count") > 0, 1).otherwise(0) +
        lit(5.0) * col("is_like") +
        lit(3.0) * col("is_comment") +
        lit(4.0) * col("is_share")
    ).withColumn("dt", lit(today))

    // 5. 写入 DWS 表
    featureDf.write
      .mode(SaveMode.Overwrite)
      .partitionBy("dt")
      .format("parquet")
      .saveAsTable("dws_user_video_interaction")

    println(s"[SUCCESS] DWS 聚合完成，产出特征记录数: ${featureDf.count()}")

    // 6. (可选) 打印几条看看效果
    spark.sql(s"SELECT * FROM dws_user_video_interaction WHERE dt='$today' ORDER BY final_score DESC LIMIT 5").show()

    spark.stop()
  }
}