package com.shortvideo.recommendation.datagenerator

import java.util.concurrent.{Executors, TimeUnit}
import com.shortvideo.recommendation.common.config.{KafkaConfig, SparkConfig}
import com.shortvideo.recommendation.common.utils.{ConfigUtils, KafkaUtil}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.spark.sql.SparkSession

/**
 * 日志生产者 - 将模拟数据发送到Kafka
 */
class LogProducer(kafkaConfig: KafkaConfig, topic: String) {
  private val producer: KafkaProducer[String, String] = KafkaUtil.createProducer(kafkaConfig)
  
  /**
   * 发送单条日志消息到Kafka
   */
  def sendLog(logMessage: String): Unit = {
    try {
      val key = System.currentTimeMillis().toString
      val record = new ProducerRecord[String, String](topic, key, logMessage)
      producer.send(record)
      println(s"[INFO] Sent message to topic '$topic': $logMessage")
    } catch {
      case e: Exception =>
        println(s"[ERROR] Failed to send message: ${e.getMessage}")
        e.printStackTrace()
    }
  }
  
  /**
   * 批量发送日志消息到Kafka
   */
  def sendLogs(logs: List[String]): Unit = {
    logs.foreach(log => sendLog(log))
  }
  
  /**
   * 关闭生产者
   */
  def close(): Unit = {
    if (producer != null) {
      producer.flush()
      producer.close()
    }
  }
}

object LogProducer {
  
  /**
   * 持续发送模拟数据到Kafka
   */
  def continuousSend(durationMinutes: Int = 1): Unit = {
    val kafkaConfig = KafkaConfig()
    val topic = ConfigUtils.getString("kafka.topics.user-behavior", "shortvideo_user_behavior")
    
    val logGenerator = LogGenerator()
    val logProducer = new LogProducer(kafkaConfig, topic)
    
    val startTime = System.currentTimeMillis()
    val endTime = startTime + (durationMinutes * 60 * 1000) // 转换为毫秒
    
    println(s"[INFO] 开始持续发送模拟数据到Kafka，持续时间: $durationMinutes 分钟")
    
    try {
      while (System.currentTimeMillis() < endTime) {
        val userBehavior = logGenerator.generateUserBehavior()
        val jsonLog = logGenerator.generateJsonLog(userBehavior)
        
        logProducer.sendLog(jsonLog)
        
        // 随机延迟，模拟真实用户行为频率
        val delay = 100 + scala.util.Random.nextInt(1000) // 100-1100毫秒
        Thread.sleep(delay)
      }
    } catch {
      case e: InterruptedException =>
        println(s"[INFO] 发送过程被中断")
      case e: Exception =>
        println(s"[ERROR] 发送过程中出现错误: ${e.getMessage}")
        e.printStackTrace()
    } finally {
      logProducer.close()
      println(s"[INFO] 模拟数据发送完成")
    }
  }
  
  /**
   * 发送固定数量的消息
   */
  def sendFixedCount(count: Int): Unit = {
    val kafkaConfig = KafkaConfig()
    val topic = ConfigUtils.getString("kafka.topics.user-behavior", "shortvideo_user_behavior")
    
    val logGenerator = LogGenerator()
    val logProducer = new LogProducer(kafkaConfig, topic)
    
    println(s"[INFO] 开始发送 $count 条模拟数据到Kafka")
    
    try {
      for (i <- 1 to count) {
        val userBehavior = logGenerator.generateUserBehavior()
        val jsonLog = logGenerator.generateJsonLog(userBehavior)
        
        logProducer.sendLog(jsonLog)
        
        // 每发送100条打印一次进度
        if (i % 100 == 0) {
          println(s"[INFO] 已发送 $i 条消息")
        }
        
        // 添加短暂延迟以避免过快发送
        Thread.sleep(10)
      }
    } catch {
      case e: Exception =>
        println(s"[ERROR] 发送过程中出现错误: ${e.getMessage}")
        e.printStackTrace()
    } finally {
      logProducer.close()
      println(s"[INFO] 固定数量数据发送完成，总计: $count 条")
    }
  }
  
  /**
   * 发送数据到HDFS（直接方式）
   */
  def sendDataToHDFS(count: Int): Unit = {
    val logGenerator = LogGenerator()
    val spark = SparkSession.builder()
      .appName("TestDataToHDFS")
      .master("local[*]")
      .getOrCreate()
    
    try {
      // 生成高质量模拟数据
      val behaviors = logGenerator.generateHighQualityDataSet(count)
      
      // 转换为JSON字符串
      val jsonLogs = behaviors.map(behavior => logGenerator.generateJsonLog(behavior))
      
      // 获取HDFS路径配置
      import spark.implicits._
      val df = spark.createDataset(jsonLogs)
      
      // 生成日期分区路径
      val currentDate = java.time.LocalDate.now().toString
      val hdfsPath = s"hdfs://localhost:9000/short-video/behavior/logs/$currentDate/user_behavior_${System.currentTimeMillis()}.json"
      
      // 写入HDFS
      df.coalesce(1).write
        .mode("append")
        .text(hdfsPath)
      
      println(s"[INFO] 成功将 $count 条高质量数据写入HDFS路径: $hdfsPath")
      
    } catch {
      case e: Exception =>
        println(s"[ERROR] 写入HDFS失败: ${e.getMessage}")
        e.printStackTrace()
    } finally {
      spark.stop()
    }
  }
  
  def main(args: Array[String]): Unit = {
    if (args.length == 0) {
      println("用法:")
      println("  scala LogProducer fixed <count>                    # 发送固定数量消息到Kafka")
      println("  scala LogProducer continuous <minutes>             # 持续发送指定分钟数到Kafka")
      println("  scala LogProducer hdfs <count>                     # 发送高质量数据到HDFS")
      println("")
      println("示例:")
      println("  scala LogProducer fixed 1000                       # 发送1000条消息到Kafka")
      println("  scala LogProducer continuous 5                     # 持续发送5分钟到Kafka")
      println("  scala LogProducer hdfs 500                         # 发送500条高质量数据到HDFS")
      return
    }
    
    args(0) match {
      case "fixed" =>
        if (args.length < 2) {
          println("错误: 需要指定发送数量")
          return
        }
        val count = args(1).toInt
        sendFixedCount(count)
        
      case "continuous" =>
        if (args.length < 2) {
          println("错误: 需要指定持续时间（分钟）")
          return
        }
        val minutes = args(1).toInt
        continuousSend(minutes)
        
      case "hdfs" =>
        if (args.length < 2) {
          println("错误: 需要指定发送数量")
          return
        }
        val count = args(1).toInt
        sendDataToHDFS(count)
        
      case _ =>
        println(s"未知命令: ${args(0)}")
    }
  }
}