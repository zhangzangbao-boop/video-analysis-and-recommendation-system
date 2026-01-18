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

  // 模拟的用户ID范围
  private val userIdRange = (1000 to 9999).toArray
  // 模拟的视频ID范围
  private val videoIdRange = (1 to 999).toArray
  // 行为类型
  private val behaviorTypes = Array("play", "like", "collect", "share", "comment", "follow", "unfollow")
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
    val userId = userIdRange(random.nextInt(userIdRange.length))
    val videoId = videoIdRange(random.nextInt(videoIdRange.length))
    val behaviorType = behaviorTypes(random.nextInt(behaviorTypes.length))

    // 根据行为类型设定不同的观看时长
    val duration = behaviorType match {
      case "play" => random.nextInt(180) + 10 // 播放行为：10-190秒
      case "like" => 5 // 点赞：5秒
      case "collect" => 8 // 收藏：8秒
      case "share" => 15 // 分享：15秒
      case "comment" => 25 // 评论：25秒
      case "follow" => 10 // 关注：10秒
      case "unfollow" => 5 // 取消关注：5秒
      case _ => random.nextInt(60) // 其他行为：0-60秒
    }

    val deviceInfo = deviceInfos(random.nextInt(deviceInfos.length))
    val networkType = networkTypes(random.nextInt(networkTypes.length))
    val ipAddress = generateIpAddress()
    val location = generateLocation()

    val currentTime = new Timestamp(System.currentTimeMillis() - random.nextInt(86400000)) // 过去24小时内

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
}

object LogGenerator {
  def apply(): LogGenerator = new LogGenerator()
}