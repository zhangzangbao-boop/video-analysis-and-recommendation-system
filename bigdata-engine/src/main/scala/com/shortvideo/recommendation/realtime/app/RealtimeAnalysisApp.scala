package com.shortvideo.recommendation.realtime.app

import com.shortvideo.recommendation.common.config.{KafkaConfig, RedisConfig, SparkConfig}
import com.shortvideo.recommendation.common.utils.{RedisUtil, SparkUtil}
import com.shortvideo.recommendation.realtime.BehaviorParser
import com.shortvideo.recommendation.realtime.hot.RealtimeHotVideoAggregator
import com.shortvideo.recommendation.realtime.mysql.VideoInfoMySQLLoader
import com.shortvideo.recommendation.realtime.recommend.RealtimeRecommender
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe

/**
 * 实时分析应用主入口
 * 功能：
 * 1. 从Kafka读取用户行为数据
 * 2. 解析为UserBehavior实体
 * 3. 实时统计热门视频
 * 4. 实时统计用户行为并更新兴趣标签
 * 5. 将结果存储到Redis
 */
object RealtimeAnalysisApp {

  def main(args: Array[String]): Unit = {
    println("=" * 60)
    println("实时分析应用启动中...")
    println("=" * 60)

    try {
      // 1. 加载配置
      val sparkConfig = SparkConfig.createStreamingConfig()
      val kafkaConfig = KafkaConfig.createConsumerConfig()
      val redisConfig = RedisConfig.createConfig()

      // 2. 初始化Redis连接池
      println("[初始化] Redis连接池...")
      RedisUtil.initPool(redisConfig)

      // 3. 创建 StreamingContext
      // 注意：batchDuration 在 SparkUtil 中默认为 10 秒
      println("[初始化] 创建StreamingContext...")
      val ssc = SparkUtil.createStreamingContext(sparkConfig.appName)

      // 3.1 启动时从 MySQL 加载 video_info，并广播（避免每个批次都读数据库）
      println("[初始化] 从MySQL加载 video_info...")
      val videoList = VideoInfoMySQLLoader.loadAll()
      if (videoList.isEmpty) {
        println("[WARN] video_info 为空，实时推荐将无法生成（请确认MySQL表中有可推荐数据）")
      }
      val videoMapBc = ssc.sparkContext.broadcast(videoList.map(v => v.id -> v).toMap)

      // 4. 构造 Kafka 参数
      val kafkaParams = Map[String, Object](
        "bootstrap.servers"  -> kafkaConfig.bootstrapServers,
        "key.deserializer"   -> classOf[StringDeserializer],
        "value.deserializer" -> classOf[StringDeserializer],
        "group.id"           -> kafkaConfig.groupId,
        "auto.offset.reset"  -> kafkaConfig.autoOffsetReset,
        "enable.auto.commit" -> (kafkaConfig.enableAutoCommit: java.lang.Boolean)
      )

      // 5. 定义要订阅的 Topic
      val topics = Array("shortvideo_user_behavior")

      // 6. 创建 Kafka 直连流
      println("[初始化] 创建Kafka流...")
      val rawKafkaStream = KafkaUtils.createDirectStream[String, String](
        ssc,
        PreferConsistent,
        Subscribe[String, String](topics, kafkaParams)
      )

      // 7. 解析为UserBehavior实体流
      println("[处理] 解析用户行为数据...")
      val behaviorStream = BehaviorParser.parse(rawKafkaStream)

      // 7.1 实时热门视频榜（写入 Redis: rec:video:hot）
      println("[处理] 启动实时热门视频统计（写入 rec:video:hot）...")
      RealtimeHotVideoAggregator.start(behaviorStream, topLimit = 1000, expireSeconds = 7 * 24 * 60 * 60)

      // 8. 实时推荐（最小可跑版本：热门候选 + 过滤已看 + 写入Redis）
      // 可通过启动参数传入 topN：args(0)=inputPath 已被 ALS 用到，这里实时模块先固定为20
      println("[处理] 启动实时推荐生成并写入Redis...")
      behaviorStream.foreachRDD { rdd =>
        if (!rdd.isEmpty()) {
          val videoMap = videoMapBc.value
          RealtimeRecommender.generateAndSave(rdd, videoMap, topN = 20, hotCandidateSize = 200)
        }
      }


      // 10. 监控输出（可选）
      behaviorStream.foreachRDD { rdd =>
        if (!rdd.isEmpty()) {
          val count = rdd.count()
          println(s"[监控] 批次处理完成：${count} 条有效行为数据")
          
          // 每10个批次打印一次Top热门视频（可选）
          // val topVideos = RealtimeHotVideoProcessor.getTopHotVideos(10)
          // println(s"[监控] Top 10热门视频: ${topVideos.take(5).mkString(", ")}")
        }
      }

      println("=" * 60)
      println("实时分析应用启动成功！")
      println("正在从Kafka消费数据...")
      println("=" * 60)

      // 11. 启动流处理
      ssc.start()
      ssc.awaitTermination()

    } catch {
      case e: Exception =>
        println(s"[ERROR] 应用启动失败: ${e.getMessage}")
        e.printStackTrace()
        System.exit(1)
    } finally {
      // 关闭Redis连接池
      RedisUtil.closePool()
    }
  }
}