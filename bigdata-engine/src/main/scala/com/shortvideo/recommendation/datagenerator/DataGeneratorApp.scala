package com.shortvideo.recommendation.datagenerator

import java.io.{BufferedWriter, FileWriter}
import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}
import com.shortvideo.recommendation.common.entity.UserBehavior
import scala.io.StdIn
import java.io.File

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
    
    val choice = StdIn.readLine("请输入选择 (1/2/3): ").trim
    
    // 确保日志目录存在
    val logDir = new File("logs")
    if (!logDir.exists()) {
      logDir.mkdirs()
      println("创建日志目录: logs")
    }
    
    choice match {
      case "1" => generateByCount()
      case "2" => generateByTime()
      case "3" => generateRealTime()
      case _ => 
        println("无效选择，使用默认模式: 按数量生成")
        generateByCount()
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
   * 保存到Flume监控的目录
   */
  private def saveToFlumeDirectory(logEntry: String): Unit = {
    try {
      // 为每个日志条目创建唯一的临时文件，然后重命名以确保Flume能正确处理
      val timestamp = System.currentTimeMillis()
      val tempFileName = s"logs/temp_$timestamp.tmp"
      val finalFileName = s"logs/user_behavior_${timestamp}.json"
      
      // 先写入临时文件
      val tempWriter = new BufferedWriter(new FileWriter(tempFileName))
      tempWriter.write(logEntry)
      tempWriter.close()
      
      // 重命名文件，这样Flume的spooldir source能检测到新文件
      val tempFile = new java.io.File(tempFileName)
      val finalFile = new java.io.File(finalFileName)
      tempFile.renameTo(finalFile)
      
    } catch {
      case ex: Exception =>
        println(s"保存到Flume目录失败: ${ex.getMessage}")
    }
  }
}