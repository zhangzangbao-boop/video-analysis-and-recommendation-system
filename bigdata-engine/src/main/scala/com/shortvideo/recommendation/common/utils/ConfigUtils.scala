package com.shortvideo.recommendation.common.utils

import com.typesafe.config.{Config, ConfigFactory, ConfigException}
import scala.collection.JavaConverters._
import com.shortvideo.recommendation.common.Constants

/**
 * 配置工具类
 */
object ConfigUtils {

  private var config: Config = _
  private var configLoaded = false

  /**
   * 加载配置文件
   */
  def loadConfig(configFile: String = Constants.ConfigFile.APPLICATION): Unit = {
    try {
      println(s"正在加载配置文件: $configFile")

      // 方法1: 从classpath加载
      val classLoader = Thread.currentThread().getContextClassLoader
      val resourceUrl = classLoader.getResource(configFile)

      if (resourceUrl != null) {
        config = ConfigFactory.load(configFile)
        println(s"从classpath加载配置文件成功: $configFile")
      } else {
        // 方法2: 从文件系统加载
        val filePaths = List(
          s"conf/$configFile",
          s"src/main/resources/$configFile",
          s"$configFile"
        )

        var found = false
        for (filePath <- filePaths if !found) {
          val file = new java.io.File(filePath)
          if (file.exists()) {
            config = ConfigFactory.parseFile(file).resolve()
            println(s"从文件系统加载配置文件成功: ${file.getAbsolutePath}")
            found = true
          }
        }

        if (!found) {
          // 方法3: 创建默认配置
          println("配置文件未找到，创建默认配置")
          config = createDefaultConfig()
        }
      }

      configLoaded = true

      // 打印加载的配置（调试用）
      if (isDev) {
        println("=== 加载的配置摘要 ===")
        val keys = List(
          "app.name", "app.env",
          "spark.master", "spark.app.name",
          "kafka.bootstrap.servers",
          "redis.host", "redis.port"
        )

        keys.foreach { key =>
          try {
            if (config.hasPath(key)) {
              println(s"$key = ${config.getString(key)}")
            }
          } catch {
            case _: ConfigException => // 忽略
          }
        }
        println("====================")
      }

    } catch {
      case e: ConfigException.UnresolvedSubstitution =>
        println(s"配置文件包含未解析的变量: ${e.getMessage}")
        println("尝试加载不带变量的配置...")
        loadSimpleConfig(configFile)
      case e: Exception =>
        println(s"加载配置文件失败: ${e.getMessage}")
        println("使用默认配置...")
        config = createDefaultConfig()
        configLoaded = true
    }
  }

  /**
   * 加载简单配置（不解析变量）
   */
  private def loadSimpleConfig(configFile: String): Unit = {
    try {
      // 读取原始文件内容
      val source = scala.io.Source.fromResource(configFile)
      val lines = try source.mkString finally source.close()

      // 移除变量引用
      val cleanLines = lines.linesIterator
        .filterNot(_.contains("${"))
        .mkString("\n")

      config = ConfigFactory.parseString(cleanLines)
      configLoaded = true
      println("加载简化配置成功")
    } catch {
      case e: Exception =>
        println(s"加载简化配置失败: ${e.getMessage}")
        config = createDefaultConfig()
        configLoaded = true
    }
  }

  /**
   * 创建默认配置
   */
  private def createDefaultConfig(): Config = {
    val defaultConfig = s"""
      app {
        name = "ShortVideoRecommendation"
        env = "dev"
        logLevel = "WARN"
      }

      spark {
        master = "local[*]"
        app.name = "ShortVideoRecommendation"
        executor.memory = "2g"
        driver.memory = "2g"
        enableHiveSupport = false
      }

      kafka {
        bootstrap.servers = "localhost:9092"
        group.id = "shortvideo-recommendation"
      }

      redis {
        host = "localhost"
        port = 6379
      }

      hdfs {
        base.path = "file:///tmp/shortvideo"
      }
    """

    ConfigFactory.parseString(defaultConfig)
  }

  /**
   * 获取配置值
   */
  def getString(key: String, defaultValue: String = ""): String = {
    ensureConfigLoaded()

    try {
      if (config.hasPath(key)) {
        config.getString(key)
      } else {
        // 尝试从环境变量获取
        val envKey = key.replace(".", "_").toUpperCase()
        val envValue = System.getenv(envKey)
        if (envValue != null && envValue.nonEmpty) {
          if (isDev) println(s"[DEBUG] 从环境变量获取: $envKey = $envValue")
          envValue
        } else {
          // 尝试从系统属性获取
          val sysKey = key.replace(".", "_")
          val sysValue = System.getProperty(sysKey)
          if (sysValue != null && sysValue.nonEmpty) {
            if (isDev) println(s"[DEBUG] 从系统属性获取: $sysKey = $sysValue")
            sysValue
          } else {
            if (isDev) println(s"[DEBUG] 使用默认值: $key = $defaultValue")
            defaultValue
          }
        }
      }
    } catch {
      case e: Exception =>
        if (isDev) println(s"[DEBUG] 获取配置失败: $key, error=${e.getMessage}")
        defaultValue
    }
  }

  // 其他getInt, getLong等方法保持不变...
  /**
   * 获取Int配置值
   */
  def getInt(key: String, defaultValue: Int = 0): Int = {
    if (config == null) {
      loadConfig()
    }

    if (config.hasPath(key)) {
      config.getInt(key)
    } else {
      println(s"配置项不存在: $key, 使用默认值: $defaultValue")
      defaultValue
    }
  }

  /**
   * 获取Long配置值
   */
  def getLong(key: String, defaultValue: Long = 0L): Long = {
    if (config == null) {
      loadConfig()
    }

    if (config.hasPath(key)) {
      config.getLong(key)
    } else {
      println(s"配置项不存在: $key, 使用默认值: $defaultValue")
      defaultValue
    }
  }

  /**
   * 获取Double配置值
   */
  def getDouble(key: String, defaultValue: Double = 0.0): Double = {
    if (config == null) {
      loadConfig()
    }

    if (config.hasPath(key)) {
      config.getDouble(key)
    } else {
      println(s"配置项不存在: $key, 使用默认值: $defaultValue")
      defaultValue
    }
  }

  /**
   * 获取Boolean配置值
   */
  def getBoolean(key: String, defaultValue: Boolean = false): Boolean = {
    if (config == null) {
      loadConfig()
    }

    if (config.hasPath(key)) {
      config.getBoolean(key)
    } else {
      println(s"配置项不存在: $key, 使用默认值: $defaultValue")
      defaultValue
    }
  }

  /**
   * 获取字符串列表
   */
  def getStringList(key: String, defaultValue: List[String] = List.empty): List[String] = {
    if (config == null) {
      loadConfig()
    }

    if (config.hasPath(key)) {
      config.getStringList(key).asScala.toList
    } else {
      println(s"配置项不存在: $key, 使用默认值: $defaultValue")
      defaultValue
    }
  }












  /**
   * 确保配置已加载
   */
  private def ensureConfigLoaded(): Unit = {
    if (!configLoaded) {
      loadConfig()
    }
  }

  /**
   * 获取当前环境
   */
  def getEnv: String = {
    val env = getString("app.env", Constants.Env.DEV)
    env
  }

  /**
   * 是否开发环境
   */
  def isDev: Boolean = getEnv == Constants.Env.DEV

  /**
   * 是否测试环境
   */
  def isTest: Boolean = getEnv == Constants.Env.TEST

  /**
   * 是否生产环境
   */
  def isProd: Boolean = getEnv == Constants.Env.PROD

  /**
   * 获取所有配置项（调试用）
   */
  def debugAllConfigs(): Unit = {
    ensureConfigLoaded()

    println("=== 当前所有配置项 ===")
    try {
      config.entrySet().asScala.foreach { entry =>
        println(s"${entry.getKey} = ${entry.getValue.unwrapped()}")
      }
    } catch {
      case e: Exception =>
        println(s"获取所有配置失败: ${e.getMessage}")
    }
    println("===================")
  }
}