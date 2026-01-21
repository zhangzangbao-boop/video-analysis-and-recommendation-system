package com.shortvideo.recommendation.datagenerator

import java.io.{BufferedWriter, FileWriter}
import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}
import com.shortvideo.recommendation.common.entity.UserBehavior
import com.shortvideo.recommendation.common.utils.HDFSUtil

import scala.io.StdIn
import java.io.File
import scala.language.postfixOps

/**
 * 数据生成器主应用程序
 * 根据用户输入的时间或数据量来生成和传输数据
 */
object DataGeneratorApp {
  
  def main(args: Array[String]): Unit = {
    println("=== 短视频推荐系统 - 数据生成器 ===")
    println("请选择数据生成模式:")
    println("1. 按数量生成 (指定生成数据条数)")
    println("2. 按时间生成 (指定生成持续时间)")
    println("3. 实时生成 (持续生成直到停止)")
    println("4. 高质量数据生成 (专为ALS模型训练优化)")
    
    val choice = StdIn.readLine("请输入选择 (1/2/3/4): ").trim
    
    // 确保日志目录存在 (生成到bigdata-engine/logs下)
    val logDir = new File("bigdata-engine/logs")
    if (!logDir.exists()) {
      logDir.mkdirs()
      println(s"创建日志目录: ${logDir.getAbsolutePath}")
    }
    
    // 检查HDFS是否可用
    if (HDFSUtil.isHDFSAvailable()) {
      println("[INFO] HDFS连接正常，日志将同步到HDFS")
    } else {
      println("[WARNING] HDFS不可用，日志将仅保存到本地")
      println("[TIP] 请确保Hadoop HDFS服务已启动 (start-dfs.cmd)")
    }
    
    try {
      choice match {
        case "1" => generateByCount()
        case "2" => generateByTime()
        case "3" => generateRealTime()
        case "4" => generateHighQualityData()
        case _ => 
          println("无效选择，使用默认模式: 按数量生成")
          generateByCount()
      }
    } finally {
      // 关闭HDFS连接
      HDFSUtil.close()
    }
  }
  
  /**
   * 按指定数量生成数据
   */
  private def generateByCount(): Unit = {
    print("请输入要生成的数据条数: ")
    val countStr = StdIn.readLine().trim
    
    try {
      val count = countStr.toInt
      if (count <= 0) {
        println("数据条数必须大于0")
        return
      }
      
      println(s"开始生成 $count 条数据...")
      val generator = LogGenerator()
      val startTime = System.currentTimeMillis()
      
      val behaviors = generator.generateUserBehaviors(count)
      
      // 输出到控制台或文件
      outputBehaviors(behaviors, s"Generated $count records in ${System.currentTimeMillis() - startTime}ms")

    } catch {
      case _: NumberFormatException =>
        println("输入的不是有效数字")
      case ex: Exception =>
        println(s"生成数据时发生错误: ${ex.getMessage}")
    }
  }
  
  /**
   * 按指定时间生成数据
   */
  private def generateByTime(): Unit = {
    print("请输入生成持续时间 (秒): ")
    val timeStr = StdIn.readLine().trim
    
    try {
      val seconds = timeStr.toInt
      if (seconds <= 0) {
        println("时间必须大于0秒")
        return
      }
      
      println(s"开始生成数据，持续 $seconds 秒...")
      val generator = LogGenerator()
      val executor = Executors.newSingleThreadScheduledExecutor()
      
      var totalRecords = 0
      val startTime = System.currentTimeMillis()
      val endTime = startTime + (seconds * 1000)
      
      // 每秒生成一批数据
      val task = executor.scheduleAtFixedRate(new Runnable {
        override def run(): Unit = {
          if (System.currentTimeMillis() < endTime) {
            val batch = generator.generateUserBehaviors(10) // 每秒生成10条
            outputBehaviors(batch, s"Batch generated at ${new java.util.Date()}")
            totalRecords += batch.size
            
            // 显示进度
            val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
            println(f"已运行: $elapsed%.1f 秒, 已生成: $totalRecords 条数据")
          } else {
            executor.shutdown()
          }
        }
      }, 0, 1, TimeUnit.SECONDS)

      // 等待任务完成
      while (!executor.isTerminated) {
        Thread.sleep(1000)
      }
      
      val actualDuration = (System.currentTimeMillis() - startTime) / 1000.0
      println(s"数据生成完成! 总共生成了 $totalRecords 条记录，实际耗时: $actualDuration%.1f 秒")

    } catch {
      case _: NumberFormatException =>
        println("输入的不是有效数字")
      case ex: Exception =>
        println(s"生成数据时发生错误: ${ex.getMessage}")
    }
  }
  
  /**
   * 实时生成数据
   */
  private def generateRealTime(): Unit = {
    println("开始实时生成数据... (按 Ctrl+C 停止)")
    println("生成频率: 每秒5条记录")
    
    val generator = LogGenerator()
    val executor = Executors.newSingleThreadScheduledExecutor()
    
    var totalRecords = 0
    val startTime = System.currentTimeMillis()
    
    val task = executor.scheduleAtFixedRate(new Runnable {
      override def run(): Unit = {
        try {
          val batch = generator.generateUserBehaviors(5) // 每秒生成5条
          outputBehaviors(batch, "")
          totalRecords += batch.size
          
          // 每10秒显示一次统计
          val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000
          if (elapsedSeconds % 10 == 0) {
            println(s"[${new java.util.Date()}] 已生成总计: $totalRecords 条记录")
          }
        } catch {
          case ex: Exception =>
            println(s"生成数据时发生错误: ${ex.getMessage}")
        }
      }
    }, 0, 1, TimeUnit.SECONDS)
    
    // 添加关闭钩子
    Runtime.getRuntime.addShutdownHook(new Thread(() => {
      executor.shutdownNow()
      val totalDuration = (System.currentTimeMillis() - startTime) / 1000.0
      println(s"\n程序已停止。总共生成了 $totalRecords 条记录，运行时间: $totalDuration%.1f 秒")
    }))
    
    // 等待直到被中断
    try {
      while (!executor.isShutdown) {
        Thread.sleep(1000)
      }
    } catch {
      case _: InterruptedException =>
        executor.shutdownNow()
    }
  }
  
  /**
   * 生成高质量数据集，专为ALS模型训练优化
   */
  private def generateHighQualityData(): Unit = {
    print("请输入要生成的高质量数据条数: ")
    val countStr = StdIn.readLine().trim
    
    try {
      val count = countStr.toInt
      if (count <= 0) {
        println("数据条数必须大于0")
        return
      }
      
      println(s"开始生成 $count 条高质量数据...")
      val generator = LogGenerator()
      val startTime = System.currentTimeMillis()
      
      val behaviors = generator.generateHighQualityDataSet(count)
      
      // 输出到2控制台或文件
      outputBehaviors(behaviors, s"Generated $count high-quality records in ${System.currentTimeMillis() - startTime}ms")
      
      // 输出统计信息
      val userStats = behaviors.groupBy(_.userId).mapValues(_.size).toMap
      val videoStats = behaviors.groupBy(_.videoId).mapValues(_.size).toMap
      val minUserBehaviors = userStats.values.min
      val minVideoBehaviors = videoStats.values.min
      val avgUserBehaviors = userStats.values.sum.toDouble / userStats.size
      val avgVideoBehaviors = videoStats.values.sum.toDouble / videoStats.size
      
      println(s"[STATS] 用户行为统计 - 最少: $minUserBehaviors, 平均: ${avgUserBehaviors.formatted("%.2f")}, 总用户: ${userStats.size}")
      println(s"[STATS] 视频互动统计 - 最少: $minVideoBehaviors, 平均: ${avgVideoBehaviors.formatted("%.2f")}, 总视频: ${videoStats.size}")
      println(s"[INFO] 数据特点: 增强了用户行为重复度和视频互动频率，更适合ALS模型训练")

    } catch {
      case _: NumberFormatException =>
        println("输入的不是有效数字")
      case ex: Exception =>
        println(s"生成数据时发生错误: ${ex.getMessage}")
    }
  }
  
  /**
   * 输出行为数据
   */
  private def outputBehaviors(behaviors: List[UserBehavior], description: String): Unit = {
    val generator = LogGenerator()

    // 打印描述信息
    if (description.nonEmpty) {
      println(description)
    }
    
    // 输出每条记录的JSON格式
    behaviors.foreach { behavior =>
      val jsonLog = generator.generateJsonLog(behavior)
      println(jsonLog)
      
      // 保存到Flume监控的目录
      saveToFlumeDirectory(jsonLog)
    }
  }
  
  /**
   * 保存到本地日志目录
   * 同时保存到两个位置：
   * 1. logs目录（单条文件形式） - 供Flume spooldir source收集到HDFS，用于离线分析
   * 2. generated_logs.json（追加模式） - 供Flume exec source收集到Kafka，用于实时分析
   */
  private def saveToFlumeDirectory(logEntry: String): Unit = {
    try {
      // 1. 保存到logs目录（单条文件形式，供离线分析使用）
      saveToLogsDirectory(logEntry)
      
      // 2. 追加到generated_logs.json（供实时分析使用）
      saveToGeneratedLogsFile(logEntry)
      
    } catch {
      case ex: Exception =>
        println(s"保存日志失败: ${ex.getMessage}")
        ex.printStackTrace()
    }
  }
  
  /**
   * 保存到logs目录（单条文件形式）
   * 供Flume spooldir source收集，最终保存到HDFS用于离线分析
   */
  private def saveToLogsDirectory(logEntry: String): Unit = {
    try {
      // 确保目录存在
      val logDir = new File("bigdata-engine/logs")
      if (!logDir.exists()) {
        logDir.mkdirs()
      }
      
      // 为每个日志条目创建唯一的文件
      val timestamp = System.currentTimeMillis()
      val nanoTime = System.nanoTime() // 使用纳秒时间确保唯一性
      val tempFileName = s"bigdata-engine/logs/temp_${timestamp}_${nanoTime}.tmp"
      val finalFileName = s"bigdata-engine/logs/user_behavior_${timestamp}_${nanoTime}.json"
      
      // 先写入临时文件
      val tempWriter = new BufferedWriter(new FileWriter(tempFileName))
      tempWriter.write(logEntry)
      tempWriter.close()
      
      // 重命名文件，这样Flume的spooldir source能检测到新文件
      val tempFile = new java.io.File(tempFileName)
      val finalFile = new java.io.File(finalFileName)
      if (tempFile.renameTo(finalFile)) {
        // 静默保存，不打印日志以减少输出（可选：在debug模式下打印）
        // println(s"[INFO] 日志已保存到logs目录: ${finalFile.getName}")
      } else {
        println(s"[WARN] 文件重命名失败: ${finalFile.getAbsolutePath}")
      }
    } catch {
      case ex: Exception =>
        println(s"[ERROR] 保存到logs目录失败: ${ex.getMessage}")
        throw ex
    }
  }
  
  /**
   * 追加到generated_logs.json文件
   * 供Flume exec source收集，可以发送到Kafka用于实时分析
   */
  private def saveToGeneratedLogsFile(logEntry: String): Unit = {
    var writer: BufferedWriter = null
    try {
      // 追加模式写入generated_logs.json
      writer = new BufferedWriter(new FileWriter("generated_logs.json", true)) // true表示追加模式
      writer.write(logEntry)
      writer.newLine() // 添加换行符以分隔记录
      writer.flush() // 立即刷新，确保数据及时写入
    } catch {
      case ex: Exception =>
        println(s"[ERROR] 追加到generated_logs.json失败: ${ex.getMessage}")
        throw ex
    } finally {
      if (writer != null) {
        try {
          writer.close()
        } catch {
          case ex: Exception =>
            println(s"[WARN] 关闭文件流失败: ${ex.getMessage}")
        }
      }
    }
  }
}