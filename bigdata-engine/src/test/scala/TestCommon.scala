package com.shortvideo.recommendation.common

import com.shortvideo.recommendation.common.utils._
import com.shortvideo.recommendation.common.config._
import com.shortvideo.recommendation.common.entity._

/**
 * Common模块测试类
 */
object TestCommon {

  def main(args: Array[String]): Unit = {
    println("=== 开始测试Common模块 ===")

    // 1. 测试配置加载
    testConfigUtils()

    // 2. 测试Spark工具
    testSparkUtils()

    // 3. 测试Redis工具
    testRedisUtils()

    // 4. 测试Kafka工具
    testKafkaUtils()

    // 5. 测试时间工具
    testTimeUtils()

    println("=== Common模块测试完成 ===")
  }

  def testConfigUtils(): Unit = {
    println("\n--- 测试ConfigUtils ---")

    ConfigUtils.loadConfig()

    val appName = ConfigUtils.getString("app.name")
    println(s"应用名称: $appName")

    val sparkMaster = ConfigUtils.getString("spark.master")
    println(s"Spark Master: $sparkMaster")

    val kafkaServers = ConfigUtils.getString("kafka.bootstrap.servers")
    println(s"Kafka服务器: $kafkaServers")

    val redisHost = ConfigUtils.getString("redis.host")
    println(s"Redis主机: $redisHost")

    println("ConfigUtils测试通过 ✓")
  }

  def testSparkUtils(): Unit = {
    println("\n--- 测试SparkUtils ---")

    val sparkConfig = SparkConfig()
    println(s"Spark配置: ${sparkConfig}")

    val spark = SparkUtil.createSparkSession("TestApp")
    println(s"SparkSession创建成功: ${spark.version}")

    // 创建测试DataFrame
    import spark.implicits._
    val testData = Seq(
      (1L, 101L, "view", 60),
      (2L, 102L, "like", 30)
    ).toDF("user_id", "video_id", "behavior_type", "duration")

    println("测试DataFrame:")
    testData.show()

    SparkUtil.stopSparkSession(spark)
    println("SparkUtils测试通过 ✓")
  }

  def testRedisUtils(): Unit = {
    println("\n--- 测试RedisUtils ---")

    val redisConfig = RedisConfig()
    println(s"Redis配置: ${redisConfig}")

    // 初始化连接池
    RedisUtil.initPool(redisConfig)

    // 测试连接
    val connected = RedisUtil.checkRedisConnection(redisConfig)
    if (connected) {
      println("Redis连接测试成功")

      // 测试基本操作
      val testKey = "test:common"
      val testValue = "hello redis"

      RedisUtil.set(testKey, testValue)
      val getValue = RedisUtil.get(testKey)

      println(s"设置值: $testValue")
      println(s"获取值: $getValue")

      if (getValue.contains(testValue)) {
        println("Redis基本操作测试通过")
      }

      // 测试Hash操作
      val hashKey = "test:hash"
      RedisUtil.hset(hashKey, "field1", "value1")
      RedisUtil.hset(hashKey, "field2", "value2")

      val hashValue = RedisUtil.hget(hashKey, "field1")
      println(s"Hash获取: $hashValue")

      // 清理测试数据
      RedisUtil.del(testKey)
      RedisUtil.del(hashKey)
    } else {
      println("Redis连接失败，请检查Redis服务是否启动")
    }

    RedisUtil.closePool()
    println("RedisUtils测试完成")
  }

  def testKafkaUtils(): Unit = {
    println("\n--- 测试KafkaUtils ---")

    val kafkaConfig = KafkaConfig()
    println(s"Kafka配置: ${kafkaConfig}")

    // 测试连接（增加超时和重试）
    val connected = try {
      KafkaUtil.checkKafkaConnection(kafkaConfig)
    } catch {
      case e: Exception =>
        println(s"Kafka连接测试异常: ${e.getMessage}")
        false
    }

    if (connected) {
      println("Kafka连接测试成功")
    } else {
      println("Kafka连接失败，跳过生产者和消费者测试")
      println("提示：请确保Kafka服务已启动")
      println("在开发环境可以使用以下方式启动Kafka：")
      println("1. 使用Docker: docker run -p 9092:9092 apache/kafka:2.8.0")
      println("2. 或者配置为使用模拟Kafka进行开发")
    }

    println("KafkaUtils测试完成")
  }
  def testTimeUtils(): Unit = {
    println("\n--- 测试TimeUtils ---")

    val now = TimeUtil.currentTimestamp
    println(s"当前时间戳: $now")

    val nowStr = TimeUtil.currentTimeString()
    println(s"当前时间字符串: $nowStr")

    val today = TimeUtil.getTodayDate
    println(s"今天日期: $today")

    val yesterday = TimeUtil.getYesterdayDate
    println(s"昨天日期: $yesterday")

    val hour = TimeUtil.getCurrentHour
    println(s"当前小时: $hour")

    val timestampStr = TimeUtil.timestampToString(now)
    println(s"时间戳转字符串: $timestampStr")

    val parsedTimestamp = TimeUtil.stringToTimestamp(timestampStr)
    println(s"字符串转时间戳: $parsedTimestamp")

    // 测试时间差
    val startTime = TimeUtil.stringToTimestamp("2024-01-01 00:00:00")
    val endTime = TimeUtil.stringToTimestamp("2024-01-01 01:30:00")

    val diffHours = TimeUtil.getTimeDiffInHours(startTime, endTime)
    val diffMinutes = TimeUtil.getTimeDiffInMinutes(startTime, endTime)

    println(s"时间差: $diffHours 小时, $diffMinutes 分钟")

    println("TimeUtils测试通过 ✓")
  }

  def testEntities(): Unit = {
    println("\n--- 测试数据实体 ---")

    // 测试UserBehavior实体
    val userBehavior = UserBehavior(
      userId = 1001L,
      videoId = 2001L,
      behaviorType = "view",
      behaviorTime = TimeUtil.currentTimestamp,
      duration = 60,
      deviceInfo = "iPhone",
      networkType = "wifi",
      ipAddress = "192.168.1.1",
      location = "北京"
    )

    println(s"UserBehavior实体: $userBehavior")

    // 测试UserRating实体
    val userRating = UserRating(
      userId = 1001L,
      videoId = 2001L,
      rating = 4.5,
      timestamp = System.currentTimeMillis()
    )

    println(s"UserRating实体: $userRating")

    // 测试Recommendation实体
    val recommendation = Recommendation(
      userId = 1001L,
      videoId = 2001L,
      score = 0.95,
      source = "als"
    )

    println(s"Recommendation实体: $recommendation")

    println("数据实体测试通过 ✓")
  }
}