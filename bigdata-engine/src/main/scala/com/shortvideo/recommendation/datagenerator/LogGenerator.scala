package com.shortvideo.recommendation.datagenerator

import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.{Calendar, Date, Random}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.shortvideo.recommendation.common.entity.UserBehavior

import scala.collection.mutable.ListBuffer

/**
 * 模拟日志数据生成器
 */
class LogGenerator {
  private val random = new Random()

  // 模拟的用户ID范围 - 减少范围以提高重复概率
  private val userIdRange = (1 to 50).toArray  // 只使用50个用户，提高重复概率
  // 模拟的视频ID范围 - 减少范围以提高重复概率
  private val videoIdRange = (1 to 30).toArray  // 只使用30个视频，提高重复概率
  // 行为类型 - 只保留ALS模型需要的行为
  private val behaviorTypes = Array("play", "like", "collect", "comment")
  // 设备信息
  private val deviceInfos = Array("iPhone13", "Samsung Galaxy S21", "Huawei P40", "Xiaomi 11", "OPPO Reno6", "Vivo X60", "Android Phone", "iPhone12")
  // 网络类型
  private val networkTypes = Array("WiFi", "4G", "5G", "3G")
  // IP地址段
  private val ipSegments = Array("192.168", "10.0", "172.16")

  /**
   * 生成单条用户行为数据
   */
  def generateUserBehavior(): UserBehavior = {
    // 使用偏向分布，让某些用户和视频更频繁出现
    val userId = userIdRange(getBiasedIndex(userIdRange.length))
    val videoId = videoIdRange(getBiasedIndex(videoIdRange.length))
    val behaviorType = behaviorTypes(random.nextInt(behaviorTypes.length))

    // 根据行为类型设定不同的观看时长
    val duration = behaviorType match {
      case "play" => random.nextInt(180) + 10 // 播放行为：10-190秒
      case "like" => 5 // 点赞：5秒
      case "collect" => 8 // 收藏：8秒
      case "comment" => 25 // 评论：25秒
      case _ => random.nextInt(60) // 其他行为：0-60秒
    }

    val deviceInfo = deviceInfos(random.nextInt(deviceInfos.length))
    val networkType = networkTypes(random.nextInt(networkTypes.length))
    val ipAddress = generateIpAddress()
    val location = generateLocation()

    // 生成过去几天内的随机时间，但确保时间递增
    val currentTime = new Timestamp(System.currentTimeMillis() - random.nextInt(86400000 * 3)) // 过去3天内

    UserBehavior(
      userId = userId,
      videoId = videoId,
      behaviorType = behaviorType,
      behaviorTime = currentTime,
      duration = duration,
      deviceInfo = deviceInfo,
      networkType = networkType,
      ipAddress = ipAddress,
      location = location,
      extraInfo = s"""{"platform":"mobile","version":"1.0.${random.nextInt(100)}"}"""
    )
  }

  /**
   * 使用偏向分布，让某些索引更频繁被选中
   * 这样可以产生更多重复的用户行为和视频互动
   */
  private def getBiasedIndex(rangeLength: Int): Int = {
    // 30%的概率选择前10%的索引（热点用户/视频）
    if (random.nextDouble() < 0.3) {
      random.nextInt(math.max(1, (rangeLength * 0.1).toInt))
    } else {
      // 70%的概率选择全部范围
      random.nextInt(rangeLength)
    }
  }

  /**
   * 生成IP地址
   */
  private def generateIpAddress(): String = {
    val segment = ipSegments(random.nextInt(ipSegments.length))
    val thirdOctet = random.nextInt(255)
    val fourthOctet = random.nextInt(255)
    s"$segment.$thirdOctet.$fourthOctet"
  }

  /**
   * 生成地理位置
   */
  private def generateLocation(): String = {
    val provinces = Array("北京", "上海", "广东", "江苏", "浙江", "山东", "河南", "四川", "湖北", "湖南")
    val cities = Array("北京市", "上海市", "广州市", "深圳市", "南京市", "杭州市", "青岛市", "郑州市", "成都市", "武汉市")

    val province = provinces(random.nextInt(provinces.length))
    val city = cities(random.nextInt(cities.length))
    s"$province-$city"
  }

  /**
   * 生成指定数量的用户行为数据
   */
  def generateUserBehaviors(count: Int): List[UserBehavior] = {
    val behaviors = ListBuffer[UserBehavior]()
    for (_ <- 1 to count) {
      behaviors += generateUserBehavior()
    }
    behaviors.toList
  }

  /**
   * 生成JSON格式的日志数据
   */
  def generateJsonLog(userBehavior: UserBehavior): String = {
    val objectMapper = new ObjectMapper()
    objectMapper.registerModule(DefaultScalaModule)

    // 将UserBehavior转换为Map以便序列化
    val behaviorMap = Map(
      "userId" -> userBehavior.userId,
      "videoId" -> userBehavior.videoId,
      "behaviorType" -> userBehavior.behaviorType,
      "behaviorTime" -> userBehavior.behaviorTime.toString,
      "duration" -> userBehavior.duration,
      "deviceInfo" -> userBehavior.deviceInfo,
      "networkType" -> userBehavior.networkType,
      "ipAddress" -> userBehavior.ipAddress,
      "location" -> userBehavior.location,
      "extraInfo" -> userBehavior.extraInfo
    )

    objectMapper.writeValueAsString(behaviorMap)
  }

  /**
   * 生成高质量数据集，确保某些用户和视频有足够的行为记录
   * 专门为ALS模型训练设计，确保满足用户行为>=5和视频互动>=5的过滤条件
   */
  def generateHighQualityDataSet(totalRecords: Int): List[UserBehavior] = {
    val behaviors = ListBuffer[UserBehavior]()
    val numUsers = math.min(20, userIdRange.length)  // 使用较小的用户集
    val numVideos = math.min(15, videoIdRange.length)  // 使用较小的视频集

    // 首先为每个用户生成至少5个行为（确保满足用户行为阈值）
    for (userId <- 1 to numUsers) {
      for (i <- 1 to 5) {  // 每个用户至少5个行为
        val videoId = videoIdRange(random.nextInt(numVideos))
        val behaviorType = behaviorTypes(random.nextInt(behaviorTypes.length))
        val duration = behaviorType match {
          case "play" => random.nextInt(180) + 10
          case "like" => 5
          case "collect" => 8
          case "comment" => 25
          case _ => random.nextInt(60)
        }

        val deviceInfo = deviceInfos(random.nextInt(deviceInfos.length))
        val networkType = networkTypes(random.nextInt(networkTypes.length))
        val ipAddress = generateIpAddress()
        val location = generateLocation()
        val currentTime = new Timestamp(System.currentTimeMillis() - random.nextInt(86400000 * 3))

        behaviors += UserBehavior(
          userId = userId.toLong,
          videoId = videoId.toLong,
          behaviorType = behaviorType,
          behaviorTime = currentTime,
          duration = duration,
          deviceInfo = deviceInfo,
          networkType = networkType,
          ipAddress = ipAddress,
          location = location,
          extraInfo = s"""{"platform":"mobile","version":"1.0.${random.nextInt(100)}"}"""
        )
      }
    }

    // 为某些热门视频额外添加互动（确保满足视频互动阈值）
    val popularVideos = (1 to 5).map(v => videoIdRange(random.nextInt(numVideos))).toSet
    for (_ <- 1 to (totalRecords - behaviors.length)) {
      val userId = userIdRange(random.nextInt(numUsers))
      val videoId = if (random.nextDouble() < 0.4) {
        // 40%概率选择热门视频
        val popularVideoArray = popularVideos.toArray
        popularVideoArray(random.nextInt(popularVideoArray.length))
      } else {
        // 60%概率选择随机视频
        videoIdRange(random.nextInt(numVideos))
      }

      val behaviorType = behaviorTypes(random.nextInt(behaviorTypes.length))
      val duration = behaviorType match {
        case "play" => random.nextInt(180) + 10
        case "like" => 5
        case "collect" => 8
        case "comment" => 25
        case _ => random.nextInt(60)
      }

      val deviceInfo = deviceInfos(random.nextInt(deviceInfos.length))
      val networkType = networkTypes(random.nextInt(networkTypes.length))
      val ipAddress = generateIpAddress()
      val location = generateLocation()
      val currentTime = new Timestamp(System.currentTimeMillis() - random.nextInt(86400000 * 3))

      behaviors += UserBehavior(
        userId = userId.toLong,
        videoId = videoId.toLong,
        behaviorType = behaviorType,
        behaviorTime = currentTime,
        duration = duration,
        deviceInfo = deviceInfo,
        networkType = networkType,
        ipAddress = ipAddress,
        location = location,
        extraInfo = s"""{"platform":"mobile","version":"1.0.${random.nextInt(100)}"}"""
      )
    }

    behaviors.toList
  }
}

object LogGenerator {
  def apply(): LogGenerator = new LogGenerator()
}