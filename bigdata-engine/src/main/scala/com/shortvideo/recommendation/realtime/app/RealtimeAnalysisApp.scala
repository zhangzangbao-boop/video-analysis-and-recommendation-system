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
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import scala.collection.JavaConverters._

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

    var ssc: org.apache.spark.streaming.StreamingContext = null

    try {
      // 1. 加载配置
      val sparkConfig = SparkConfig.createStreamingConfig()
      val kafkaConfig = KafkaConfig.createConsumerConfig()
      val redisConfig = RedisConfig.createConfig()

      // 2. 初始化Redis连接池并测试连接
      println("[初始化] Redis连接池...")
      println(s"[初始化] Redis配置: ${redisConfig.host}:${redisConfig.port}, database=${redisConfig.database}")
      RedisUtil.initPool(redisConfig)
      
      // 测试Redis连接
      println("[初始化] 测试Redis连接...")
      val testResult = RedisUtil.getConnection match {
        case Some(jedis) =>
          try {
            val pong = jedis.ping()
            println(s"[初始化] Redis连接测试成功: $pong")
            // 测试写入
            val testKey = "test:connection"
            val testValue = System.currentTimeMillis().toString
            jedis.set(testKey, testValue)
            val readValue = jedis.get(testKey)
            if (readValue == testValue) {
              println(s"[初始化] Redis写入/读取测试成功")
              jedis.del(testKey)
              true
            } else {
              println(s"[WARN] Redis写入/读取测试失败: 写入=$testValue, 读取=$readValue")
              false
            }
          } catch {
            case e: Exception =>
              println(s"[ERROR] Redis连接测试失败: ${e.getMessage}")
              e.printStackTrace()
              false
          } finally {
            RedisUtil.closeConnection(jedis)
          }
        case None =>
          println("[ERROR] 无法获取Redis连接")
          false
      }
      
      if (!testResult) {
        println("[ERROR] Redis连接测试失败，应用将继续运行，但可能无法写入数据")
        println("[提示] 请检查：1. Redis服务是否启动 2. 配置是否正确 3. 网络是否连通")
      }

      // 3. 创建 StreamingContext
      // 注意：batchDuration 在 SparkUtil 中默认为 10 秒
      // 重要：Spark Streaming 开启 checkpoint 后，重启会从 checkpoint 恢复 Kafka offset；
      // 如果 Kafka 没有新消息，则不会触发新的推荐写入（Redis/MySQL）。
      // 若希望每次重启都从头消费，请清理 checkpoint 目录或更换 kafka.group.id。
      println("[初始化] 创建StreamingContext...")
      ssc = SparkUtil.createStreamingContext(sparkConfig.appName)

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

      println(s"[初始化] Kafka配置: ${kafkaConfig.bootstrapServers}, groupId=${kafkaConfig.groupId}, offsetReset=${kafkaConfig.autoOffsetReset}")

      // 5. 定义要订阅的 Topic
      val topics = Array("shortvideo_user_behavior")
      println(s"[初始化] 订阅Kafka Topic: ${topics.mkString(", ")}")
      println(s"[提示] 如果offset设置为latest，只会消费启动后的新消息。如需消费历史消息，请设置为earliest或清理consumer group")
      
      // 5.1 检查Kafka topic状态（可选，用于诊断）
      try {
        import org.apache.kafka.clients.consumer.KafkaConsumer
        import org.apache.kafka.common.TopicPartition
        import scala.collection.JavaConverters._
        
        val checkProps = new java.util.Properties()
        checkProps.put("bootstrap.servers", kafkaConfig.bootstrapServers)
        checkProps.put("key.deserializer", classOf[StringDeserializer].getName)
        checkProps.put("value.deserializer", classOf[StringDeserializer].getName)
        checkProps.put("group.id", s"${kafkaConfig.groupId}-check")
        
        val checkConsumer = new KafkaConsumer[String, String](checkProps)
        val partitions = checkConsumer.partitionsFor(topics(0))
        
        if (partitions != null && !partitions.isEmpty) {
          val topicPartitions = partitions.asScala.map(p => new TopicPartition(topics(0), p.partition())).asJava
          checkConsumer.assign(topicPartitions)
          checkConsumer.seekToEnd(topicPartitions)
          
          val endOffsets = checkConsumer.endOffsets(topicPartitions)
          val beginningOffsets = checkConsumer.beginningOffsets(topicPartitions)
          
          println(s"[诊断] Kafka Topic '${topics(0)}' 状态:")
          partitions.asScala.foreach { p =>
            val tp = new TopicPartition(topics(0), p.partition())
            val begin = beginningOffsets.get(tp)
            val end = endOffsets.get(tp)
            val total = end - begin
            println(s"  分区 ${p.partition()}: 起始offset=$begin, 结束offset=$end, 总消息数=$total")
          }
          
          checkConsumer.close()
        } else {
          println(s"[WARN] 无法获取Topic '${topics(0)}' 的分区信息，可能Topic不存在")
        }
      } catch {
        case e: Exception =>
          println(s"[WARN] 检查Kafka topic状态失败: ${e.getMessage}")
          println(s"[提示] 这可能是正常的，如果Kafka服务未启动或Topic不存在")
      }

      // 6. 创建 Kafka 直连流
      println("[初始化] 创建Kafka流...")
      val rawKafkaStream = KafkaUtils.createDirectStream[String, String](
        ssc,
        PreferConsistent,
        Subscribe[String, String](topics, kafkaParams)
      )

      // 7. 监控原始Kafka消息（调试用）
      // 注意：不能直接对ConsumerRecord使用take()，需要先提取可序列化的部分
      rawKafkaStream.foreachRDD { rdd =>
        val rawCount = rdd.count()
        if (rawCount > 0) {
          println(s"[DEBUG] 从Kafka接收到 ${rawCount} 条原始消息")
          // 先提取key和value（可序列化），然后再take
          val samples = rdd.map(record => (record.key(), record.value())).take(3)
          samples.foreach { case (key, value) =>
            println(s"[DEBUG] Kafka消息示例: key=$key, value=${value.take(200)}")
          }
        } else {
          println(s"[WARN] 本批次没有从Kafka接收到任何消息（可能原因：1. Kafka没有数据 2. offset设置为latest且没有新消息 3. Kafka连接问题）")
        }
      }

      // 7.1 解析为UserBehavior实体流
      println("[处理] 解析用户行为数据...")
      val behaviorStream = BehaviorParser.parse(rawKafkaStream)

      // 7.2 监控解析后的数据流（调试用）
      behaviorStream.foreachRDD { rdd =>
        val parsedCount = rdd.count()
        if (parsedCount > 0) {
          println(s"[DEBUG] 解析后有效行为数据: ${parsedCount} 条")
          // 打印前3条解析后的数据（调试用）
          rdd.take(3).foreach { behavior =>
            println(s"[DEBUG] 行为数据示例: userId=${behavior.userId}, videoId=${behavior.videoId}, type=${behavior.behaviorType}")
          }
        } else {
          println(s"[WARN] 本批次解析后没有有效行为数据（可能原因：1. JSON格式错误 2. 必填字段缺失 3. 数据校验失败）")
        }
      }

      // 7.3 实时热门视频榜（写入 Redis: rec:video:hot）
      println("[处理] 启动实时热门视频统计（写入 rec:video:hot）...")
      RealtimeHotVideoAggregator.start(behaviorStream, topLimit = 1000, expireSeconds = 7 * 24 * 60 * 60)

      // 8. 实时推荐（最小可跑版本：热门候选 + 过滤已看 + 写入Redis）
      // 可通过启动参数传入 topN：args(0)=inputPath 已被 ALS 用到，这里实时模块先固定为20
      System.out.println("[处理] 启动实时推荐生成并写入Redis...")
      System.out.flush()
      behaviorStream.foreachRDD { rdd =>
        System.out.println(s"[推荐调用] ====== 检查RDD是否为空 ======")
        System.out.flush()
        val isEmpty = rdd.isEmpty()
        System.out.println(s"[推荐调用] RDD是否为空: $isEmpty")
        System.out.flush()
        
        if (!isEmpty) {
          System.out.println(s"[推荐调用] RDD不为空，开始处理...")
          System.out.flush()
          val videoMap = videoMapBc.value
          System.out.println(s"[推荐调用] videoMap大小: ${videoMap.size}")
          System.out.flush()
          
          val count = rdd.count()
          System.out.println(s"[推荐调用] 行为数据条数: $count")
          System.out.flush()
          
          System.out.println(s"[推荐调用] ====== 调用 generateAndSave ======")
          System.out.flush()
          RealtimeRecommender.generateAndSave(rdd, videoMap, topN = 20, hotCandidateSize = 200)
          System.out.println(s"[推荐调用] ====== generateAndSave 调用完成 ======")
          System.out.flush()
        } else {
          System.out.println("[推荐调用] RDD为空，跳过实时推荐生成")
          System.out.flush()
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
        } else {
          println("[监控] 本批次没有有效行为数据")
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
      try {
        // 确保Spark Streaming上下文被正确停止
        if (ssc != null) {
          ssc.stop(stopSparkContext = true, stopGracefully = true)
        }
      } catch {
        case e: Exception =>
          println(s"[WARN] 停止Spark Streaming上下文时出错: ${e.getMessage}")
      } finally {
        // 关闭Redis连接池
        RedisUtil.closePool()
      }
    }
  }
}