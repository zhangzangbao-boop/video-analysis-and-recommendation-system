package com.shortvideo.recommendation.datagenerator

import java.io.{BufferedWriter, FileWriter}
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import scala.io.Source

/**
 * 模拟数据生成主程序
 * 支持多种输出方式：
 * 1. 输出到控制台
 * 2. 输出到本地文件
 * 3. 输出到Kafka
 * 4. 输出到HDFS
 */
object MockDataGenerator {
  
  def main(args: Array[String]): Unit = {
    if (args.length == 0) {
      println("短视频推荐系统 - 模拟数据生成器")
      println("=" * 50)
      println("用法:")
      println("  scala MockDataGenerator console <count>              # 在控制台输出模拟数据")
      println("  scala MockDataGenerator file <count> <filepath>      # 输出到本地文件")
      println("  scala MockDataGenerator kafka <count>                # 发送到Kafka")
      println("  scala MockDataGenerator hdfs <count>                 # 发送到HDFS")
      println("  scala MockDataGenerator highquality <count> <filepath> # 生成高质量数据集（专为ALS训练优化）")
      println("")
      println("示例:")
      println("  scala MockDataGenerator console 100                  # 控制台输出100条数据")
      println("  scala MockDataGenerator file 1000 ./mock_data.json   # 输出到本地文件")
      println("  scala MockDataGenerator kafka 500                    # 发送到Kafka")
      println("  scala MockDataGenerator hdfs 200                     # 发送到HDFS")
      println("  scala MockDataGenerator highquality 2000 ./high_quality_data.json # 生成高质量训练数据")
      return
    }
    
    val command = args(0)
    val generator = LogGenerator()
    
    command match {
      case "console" =>
        if (args.length < 2) {
          println("错误: 需要指定生成数量")
          return
        }
        val count = args(1).toInt
        generateToConsole(generator, count)
        
      case "file" =>
        if (args.length < 3) {
          println("错误: 需要指定生成数量和文件路径")
          return
        }
        val count = args(1).toInt
        val filePath = args(2)
        generateToFile(generator, count, filePath)
        
      case "kafka" =>
        if (args.length < 2) {
          println("错误: 需要指定生成数量")
          return
        }
        val count = args(1).toInt
        generateToKafka(generator, count)
        
      case "hdfs" =>
        if (args.length < 2) {
          println("错误: 需要指定生成数量")
          return
        }
        val count = args(1).toInt
        generateToHDFS(generator, count)
        
      case "highquality" =>
        if (args.length < 3) {
          println("错误: 需要指定生成数量和文件路径")
          return
        }
        val count = args(1).toInt
        val filePath = args(2)
        generateHighQualityData(generator, count, filePath)
        
      case _ =>
        println(s"未知命令: $command")
        println("请使用以下命令之一: console, file, kafka, hdfs, highquality")
    }
  }
  
  /**
   * 生成数据并输出到控制台
   */
  private def generateToConsole(generator: LogGenerator, count: Int): Unit = {
    println(s"[INFO] 开始生成 $count 条模拟数据到控制台...")
    
    for (i <- 1 to count) {
      val userBehavior = generator.generateUserBehavior()
      val jsonLog = generator.generateJsonLog(userBehavior)
      println(jsonLog)
      
      if (i % 100 == 0) {
        println(s"[PROGRESS] 已生成 $i 条数据...")
      }
    }
    
    println(s"[INFO] 成功生成 $count 条数据到控制台")
  }
  
  /**
   * 生成数据并保存到文件
   */
  private def generateToFile(generator: LogGenerator, count: Int, filePath: String): Unit = {
    println(s"[INFO] 开始生成 $count 条模拟数据到文件: $filePath")
    
    val writer = new BufferedWriter(new FileWriter(filePath))
    
    try {
      for (i <- 1 to count) {
        val userBehavior = generator.generateUserBehavior()
        val jsonLog = generator.generateJsonLog(userBehavior)
        writer.write(jsonLog)
        writer.newLine()
        
        if (i % 100 == 0) {
          println(s"[PROGRESS] 已生成 $i 条数据...")
        }
      }
    } finally {
      writer.close()
    }
    
    println(s"[INFO] 成功生成 $count 条数据到文件: $filePath")
  }
  
  /**
   * 生成高质量数据集，专门为ALS模型训练优化
   * 确保满足用户行为>=5和视频互动>=5的过滤条件
   */
  private def generateHighQualityData(generator: LogGenerator, count: Int, filePath: String): Unit = {
    println(s"[INFO] 开始生成 $count 条高质量训练数据到文件: $filePath")
    println(s"[INFO] 此数据集专门优化用于满足ALS模型训练的数据质量要求")
    println(s"[INFO] 确保每个用户至少有5个行为，某些视频至少有5次互动")
    
    val behaviors = generator.generateHighQualityDataSet(count)
    val writer = new BufferedWriter(new FileWriter(filePath))
    
    try {
      for ((userBehavior, i) <- behaviors.zipWithIndex) {
        val jsonLog = generator.generateJsonLog(userBehavior)
        writer.write(jsonLog)
        writer.newLine()
        
        if ((i + 1) % 100 == 0) {
          println(s"[PROGRESS] 已生成 ${i + 1} 条数据...")
        }
      }
    } finally {
      writer.close()
    }
    
    println(s"[INFO] 成功生成 ${behaviors.length} 条高质量训练数据到文件: $filePath")
    println(s"[INFO] 数据特点: 增强了用户行为重复度和视频互动频率，更适合ALS模型训练")
    
    // 输出统计信息
    val userStats = behaviors.groupBy(_.userId).mapValues(_.size).toMap
    val videoStats = behaviors.groupBy(_.videoId).mapValues(_.size).toMap
    val minUserBehaviors = userStats.values.min
    val minVideoBehaviors = videoStats.values.min
    val avgUserBehaviors = userStats.values.sum.toDouble / userStats.size
    val avgVideoBehaviors = videoStats.values.sum.toDouble / videoStats.size
    
    println(s"[STATS] 用户行为统计 - 最少: $minUserBehaviors, 平均: ${avgUserBehaviors.formatted("%.2f")}, 总用户: ${userStats.size}")
    println(s"[STATS] 视频互动统计 - 最少: $minVideoBehaviors, 平均: ${avgVideoBehaviors.formatted("%.2f")}, 总视频: ${videoStats.size}")
  }
  
  /**
   * 生成数据并发送到Kafka
   */
  private def generateToKafka(generator: LogGenerator, count: Int): Unit = {
    println(s"[INFO] 开始生成 $count 条模拟数据并发送到Kafka...")
    
    // 这里应该调用实际的Kafka生产者，暂时简化处理
    println("[WARN] Kafka输出功能暂未完全实现，仅作演示")
  }
  
  /**
   * 生成数据并发送到HDFS
   */
  private def generateToHDFS(generator: LogGenerator, count: Int): Unit = {
    println(s"[INFO] 开始生成 $count 条模拟数据并发送到HDFS...")
    
    // 这里应该调用实际的HDFS写入功能，暂时简化处理
    println("[WARN] HDFS输出功能暂未完全实现，仅作演示")
  }
  
  /**
   * 获取当前时间戳
   */
  private def getCurrentTimestamp: Timestamp = {
    new Timestamp(System.currentTimeMillis())
  }
}