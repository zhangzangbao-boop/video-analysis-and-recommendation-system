package com.shortvideo.recommendation.common.utils

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
import java.io.{BufferedWriter, OutputStreamWriter}
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * HDFS工具类
 * 用于直接写入HDFS，不依赖Spark
 */
object HDFSUtil {
  
  private var fs: FileSystem = _
  private val HDFS_URI = "hdfs://localhost:9000"
  private val BASE_PATH = "/short-video/behavior/logs"
  private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  
  /**
   * 初始化HDFS FileSystem
   */
  private def initFileSystem(): FileSystem = {
    if (fs == null || !fs.exists(new Path("/"))) {
      try {
        val conf = new Configuration()
        conf.set("fs.defaultFS", HDFS_URI)
        conf.setBoolean("fs.hdfs.impl.disable.cache", true)
        // 设置连接超时
        conf.setInt("ipc.client.connect.timeout", 10000)
        conf.setInt("ipc.client.connect.max.retries", 3)
        // 使用Spark的Hadoop配置（如果可用）
        try {
          // 尝试从Spark环境获取Hadoop配置
          val sparkConfClass = Class.forName("org.apache.spark.SparkConf")
          // 如果Spark可用，可以使用Spark的配置
        } catch {
          case _: ClassNotFoundException => // Spark不可用，使用默认配置
        }
        
        fs = FileSystem.get(new java.net.URI(HDFS_URI), conf)
        // 测试连接
        fs.exists(new Path("/"))
        println(s"[INFO] HDFS连接成功: $HDFS_URI")
      } catch {
        case ex: java.net.ConnectException =>
          println(s"[WARNING] HDFS连接失败: 无法连接到 $HDFS_URI")
          println(s"[TIP] 请确保Hadoop HDFS服务已启动: start-dfs.cmd")
          throw ex
        case ex: Exception =>
          println(s"[WARNING] HDFS连接失败: ${ex.getMessage}")
          println(s"[WARNING] 日志将仅保存到本地，不会同步到HDFS")
          throw ex
      }
    }
    fs
  }
  
  /**
   * 确保HDFS目录存在
   */
  private def ensureDirectoryExists(path: Path, fileSystem: FileSystem): Unit = {
    try {
      if (!fileSystem.exists(path)) {
        fileSystem.mkdirs(path)
        println(s"[INFO] 创建HDFS目录: ${path.toString}")
      }
    } catch {
      case ex: Exception =>
        println(s"[WARNING] 创建HDFS目录失败: ${ex.getMessage}")
        throw ex
    }
  }
  
  /**
   * 写入日志到HDFS
   * @param logEntry 日志内容（JSON字符串）
   * @param fileName 文件名（可选，如果不提供则自动生成）
   * @return 是否成功写入
   */
  def writeLogToHDFS(logEntry: String, fileName: Option[String] = None): Boolean = {
    try {
      val fileSystem = initFileSystem()
      
      // 获取当前日期
      val currentDate = LocalDate.now().format(DATE_FORMATTER)
      
      // 构建HDFS路径
      val datePath = new Path(s"$BASE_PATH/$currentDate")
      ensureDirectoryExists(datePath, fileSystem)
      
      // 生成文件名
      val finalFileName = fileName.getOrElse(s"user_behavior_${System.currentTimeMillis()}.json")
      val hdfsFilePath = new Path(datePath, finalFileName)
      
      // 追加写入（如果文件已存在）
      val outputStream = if (fileSystem.exists(hdfsFilePath)) {
        fileSystem.append(hdfsFilePath)
      } else {
        fileSystem.create(hdfsFilePath, true)
      }
      
      val writer = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"))
      try {
        writer.write(logEntry)
        writer.newLine()
        writer.flush()
      } finally {
        writer.close()
        outputStream.close()
      }
      
      println(s"[SUCCESS] 日志已同步到HDFS: ${hdfsFilePath.toString}")
      true
      
    } catch {
      case ex: Exception =>
        println(s"[WARNING] 写入HDFS失败: ${ex.getMessage}")
        // 不抛出异常，允许继续执行（仅保存到本地）
        false
    }
  }
  
  /**
   * 批量写入日志到HDFS（使用单个文件）
   * @param logEntries 日志内容列表
   * @param fileName 文件名
   * @return 是否成功写入
   */
  def writeBatchLogsToHDFS(logEntries: Seq[String], fileName: String): Boolean = {
    try {
      val fileSystem = initFileSystem()
      
      // 获取当前日期
      val currentDate = LocalDate.now().format(DATE_FORMATTER)
      
      // 构建HDFS路径
      val datePath = new Path(s"$BASE_PATH/$currentDate")
      ensureDirectoryExists(datePath, fileSystem)
      
      // 构建完整路径
      val hdfsFilePath = new Path(datePath, fileName)
      
      // 追加写入（如果文件已存在）
      val outputStream = if (fileSystem.exists(hdfsFilePath)) {
        fileSystem.append(hdfsFilePath)
      } else {
        fileSystem.create(hdfsFilePath, true)
      }
      
      val writer = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"))
      try {
        logEntries.foreach { logEntry =>
          writer.write(logEntry)
          writer.newLine()
        }
        writer.flush()
      } finally {
        writer.close()
        outputStream.close()
      }
      
      println(s"[SUCCESS] 批量日志已同步到HDFS: ${hdfsFilePath.toString} (${logEntries.size}条)")
      true
      
    } catch {
      case ex: Exception =>
        println(s"[WARNING] 批量写入HDFS失败: ${ex.getMessage}")
        false
    }
  }
  
  /**
   * 关闭HDFS连接
   */
  def close(): Unit = {
    if (fs != null) {
      try {
        fs.close()
        fs = null
        println("[INFO] HDFS连接已关闭")
      } catch {
        case ex: Exception =>
          println(s"[WARNING] 关闭HDFS连接失败: ${ex.getMessage}")
      }
    }
  }
  
  /**
   * 检查HDFS是否可用
   */
  def isHDFSAvailable(): Boolean = {
    try {
      val fileSystem = initFileSystem()
      fileSystem.exists(new Path("/"))
    } catch {
      case _: Exception => false
    }
  }
}
