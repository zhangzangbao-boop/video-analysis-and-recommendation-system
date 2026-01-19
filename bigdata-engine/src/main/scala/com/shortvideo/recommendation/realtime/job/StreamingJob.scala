package com.shortvideo.recommendation.realtime.job

import com.shortvideo.recommendation.common.config.{KafkaConfig, RedisConfig, SparkConfig}
import com.shortvideo.recommendation.common.utils.{ConfigUtils, RedisUtil}
import com.shortvideo.recommendation.realtime.streaming.{HotVideoStreaming, RealtimeRecommendationStreaming, UserBehaviorStreaming}
import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}

/**
 * 实时分析主任务
 * 统筹管理实时用户行为分析、热门视频计算和实时推荐
 */
object StreamingJob {
  
  def main(args: Array[String]): Unit = {
    println("=" * 80)
    println("短视频推荐系统 - 实时分析主任务")
    println("=" * 80)

    // 初始化Redis连接
    try {
      RedisUtil.initPool(RedisConfig.apply())
      println("[INFO] Redis连接池初始化成功")
    } catch {
      case e: Exception =>
        println(s"[ERROR] Redis连接池初始化失败: ${e.getMessage}")
        e.printStackTrace()
        return
    }

    val sparkConf = new SparkConf()
      .setAppName("ShortVideoRealtimeAnalysis")
      .setMaster(ConfigUtils.getString("spark.master", "local[2]")) // 至少需要2个核来运行Streaming

    // 设置Spark Streaming相关配置
    val sparkConfig = SparkConfig()
    sparkConf.set("spark.streaming.kafka.maxRatePerPartition", sparkConfig.streamingKafkaMaxRatePerPartition.toString)
    sparkConf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")

    val kafkaConfig = KafkaConfig()

    // 创建StreamingContext
    val ssc = new StreamingContext(sparkConf, Seconds(30)) // 30秒批次间隔

    try {
      // 初始化各个流处理器
      val userBehaviorStreaming = new UserBehaviorStreaming()
      val hotVideoStreaming = new HotVideoStreaming()
      val realtimeRecommendationStreaming = new RealtimeRecommendationStreaming()

      // 创建各个数据流
      userBehaviorStreaming.createStreamingContext(sparkConf, kafkaConfig, Seconds(30))
      hotVideoStreaming.createStreamingContext(sparkConf, kafkaConfig, Seconds(60))
      realtimeRecommendationStreaming.createStreamingContext(sparkConf, kafkaConfig, Seconds(30))

      println("[INFO] 实时分析任务初始化完成")
      println("[INFO] 开始接收数据流...")

      // 启动流处理
      ssc.start()
      println("[INFO] 实时分析任务已启动")

      // 等待终止
      ssc.awaitTermination()

    } catch {
      case e: Exception =>
        println(s"[ERROR] 实时分析任务执行失败: ${e.getMessage}")
        e.printStackTrace()
    }finally {
        // 停止StreamingContext
        if (ssc != null) {
          ssc.stop(stopSparkContext = true, stopGracefully = true)
        }
        
        // 关闭Redis连接池
        try {
          RedisUtil.closePool()
          println("[INFO] Redis连接池已关闭")
        } catch {
          case e: Exception =>
            println(s"[ERROR] 关闭Redis连接池失败: ${e.getMessage}")
        }

    }
  }

}