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
 * 根据实际数据库中的video_info数据生成真实的用户行为
 */
class LogGenerator {
  private val random = new Random()

  // 从实际数据库中提取的用户ID列表
  private val userIdRange = Array(10003L, 10004L, 10005L, 10006L, 10007L, 10008L, 10011L, 10019L)
  
  // 从实际数据库中提取的视频ID列表（前50个标准视频 + 部分真实视频）
  private val videoIdRange = Array(
    // 标准视频ID: 10001-10050
    10001L, 10002L, 10003L, 10004L, 10005L, 10006L, 10007L, 10008L, 10009L, 10010L,
    10011L, 10012L, 10013L, 10014L, 10015L, 10016L, 10017L, 10018L, 10019L, 10020L,
    10021L, 10022L, 10023L, 10024L, 10025L, 10026L, 10027L, 10028L, 10029L, 10030L,
    10031L, 10032L, 10033L, 10034L, 10035L, 10036L, 10037L, 10038L, 10039L, 10040L,
    10041L, 10042L, 10043L, 10044L, 10045L, 10046L, 10047L, 10048L, 10049L, 10050L,
    // 部分真实视频ID（从SQL中提取）
    856084L, 2980977L, 3042473L, 3045061L, 3046247L, 3129957L, 3129977L, 3209829L,
    3248177L, 3253109L, 3253859L, 3869109L, 3946211L, 3969469L, 3969596L, 4039050L,
    4115295L, 4115300L, 4115476L, 4267889L
  )
  
  // 根据分类ID映射视频ID（便于按分类生成行为）
  // category_id: 1=搞笑, 2=生活, 3=科技, 4=游戏, 5=美食, 6=萌宠
  private val categoryVideoMap: Map[Int, Array[Long]] = Map(
    1 -> Array(10001L, 10002L, 10003L, 10004L, 10005L, 10006L, 10007L, 10008L, 10009L, 10010L, 856084L, 3042473L, 4115295L, 4115300L, 4115476L),
    2 -> Array(10011L, 10012L, 10013L, 10014L, 10015L, 10016L, 10017L, 10018L, 10019L, 10020L, 2980977L, 3045061L, 3969469L, 4267889L),
    3 -> Array(10021L, 10022L, 10023L, 10024L, 10025L, 10026L, 10027L, 10028L, 10029L, 10030L, 3046247L, 3129957L, 3129977L, 3209829L, 3248177L, 3253109L, 3253859L, 3946211L),
    4 -> Array(10031L, 10032L, 10033L, 10034L, 10035L, 10036L, 10037L, 10038L, 10039L, 10040L),
    5 -> Array(10041L, 10042L, 10043L, 10044L, 10045L),
    6 -> Array(10046L, 10047L, 10048L, 10049L, 10050L, 3869109L, 3969596L, 4039050L)
  )
  
  // 热门视频ID列表（is_hot=1的视频，会更频繁地被访问）
  private val hotVideoIds = Set(
    10001L, 10002L, 10004L, 10006L, 10008L, 10010L,  // 搞笑类热门
    10011L, 10012L, 10014L, 10016L, 10018L, 10020L,  // 生活类热门
    10021L, 10022L, 10024L, 10026L, 10028L, 10030L,  // 科技类热门
    10031L, 10032L, 10034L, 10036L, 10038L, 10040L,  // 游戏类热门
    10041L, 10043L, 10045L,                          // 美食类热门
    10046L, 10048L, 10050L                           // 萌宠类热门
  )
  
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
   * 基于实际数据库中的用户ID和视频ID，模拟真实的用户行为
   */
  def generateUserBehavior(): UserBehavior = {
    // 使用偏向分布，让某些用户和视频更频繁出现
    val userId = userIdRange(getBiasedIndex(userIdRange.length))
    
    // 视频ID选择逻辑：40%概率选择热门视频，60%概率选择普通视频
    val videoId = if (random.nextDouble() < 0.4 && hotVideoIds.nonEmpty) {
      // 选择热门视频
      val hotVideos = hotVideoIds.toArray
      hotVideos(random.nextInt(hotVideos.length))
    } else {
      // 选择普通视频，但要考虑分类分布
      // 搞笑和生活类视频更受欢迎，所以分配更高权重
      val category = selectCategoryWithWeight()
      val videos = categoryVideoMap.getOrElse(category, videoIdRange)
      videos(random.nextInt(videos.length))
    }
    
    val behaviorType = behaviorTypes(random.nextInt(behaviorTypes.length))

    // 根据行为类型设定不同的观看时长
    // 热门视频的观看时长通常更长
    val baseDuration = if (hotVideoIds.contains(videoId)) {
      random.nextInt(240) + 30 // 热门视频：30-270秒
    } else {
      random.nextInt(180) + 10 // 普通视频：10-190秒
    }
    
    val duration = behaviorType match {
      case "play" => baseDuration
      case "like" => 5 // 点赞：5秒
      case "collect" => 8 // 收藏：8秒
      case "comment" => 25 // 评论：25秒
      case _ => random.nextInt(60) // 其他行为：0-60秒
    }

    val deviceInfo = deviceInfos(random.nextInt(deviceInfos.length))
    val networkType = networkTypes(random.nextInt(networkTypes.length))
    val ipAddress = generateIpAddress()
    val location = generateLocation()

    // 生成过去几天内的随机时间
    val currentTime = new Timestamp(System.currentTimeMillis() - random.nextInt(86400000 * 7)) // 过去7天内

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
   * 根据权重选择分类（模拟不同分类的受欢迎程度）
   * 搞笑(1)和生活(2)类视频更受欢迎
   */
  private def selectCategoryWithWeight(): Int = {
    val rand = random.nextDouble()
    if (rand < 0.25) 1      // 25% 搞笑
    else if (rand < 0.45) 2 // 20% 生活
    else if (rand < 0.60) 3 // 15% 科技
    else if (rand < 0.75) 4 // 15% 游戏
    else if (rand < 0.90) 5 // 15% 美食
    else 6                  // 10% 萌宠
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
   * 使用实际数据库中的用户ID和视频ID
   */
  def generateHighQualityDataSet(totalRecords: Int): List[UserBehavior] = {
    val behaviors = ListBuffer[UserBehavior]()
    val numUsers = math.min(userIdRange.length, userIdRange.length)  // 使用所有实际用户
    val numVideos = math.min(30, videoIdRange.length)  // 使用前30个视频，提高重复概率

    // 首先为每个用户生成至少5个行为（确保满足用户行为阈值）
    for (userIdIndex <- 0 until numUsers) {
      val userId = userIdRange(userIdIndex)
      for (i <- 1 to 5) {  // 每个用户至少5个行为
        // 优先选择热门视频，提高数据质量
        val videoId = if (random.nextDouble() < 0.5 && hotVideoIds.nonEmpty) {
          val hotVideos = hotVideoIds.toArray
          hotVideos(random.nextInt(hotVideos.length))
        } else {
          videoIdRange(random.nextInt(numVideos))
        }
        
        val behaviorType = behaviorTypes(random.nextInt(behaviorTypes.length))
        val baseDuration = if (hotVideoIds.contains(videoId)) {
          random.nextInt(240) + 30
        } else {
          random.nextInt(180) + 10
        }
        
        val duration = behaviorType match {
          case "play" => baseDuration
          case "like" => 5
          case "collect" => 8
          case "comment" => 25
          case _ => random.nextInt(60)
        }

        val deviceInfo = deviceInfos(random.nextInt(deviceInfos.length))
        val networkType = networkTypes(random.nextInt(networkTypes.length))
        val ipAddress = generateIpAddress()
        val location = generateLocation()
        val currentTime = new Timestamp(System.currentTimeMillis() - random.nextInt(86400000 * 7))

        behaviors += UserBehavior(
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
    }

    // 为某些热门视频额外添加互动（确保满足视频互动阈值）
    val popularVideos = hotVideoIds.take(10).toArray  // 选择前10个热门视频
    for (_ <- 1 to (totalRecords - behaviors.length)) {
      val userId = userIdRange(random.nextInt(numUsers))
      val videoId = if (random.nextDouble() < 0.4 && popularVideos.nonEmpty) {
        // 40%概率选择热门视频
        popularVideos(random.nextInt(popularVideos.length))
      } else {
        // 60%概率选择随机视频，但要考虑分类权重
        val category = selectCategoryWithWeight()
        val videos = categoryVideoMap.getOrElse(category, videoIdRange)
        videos(random.nextInt(math.min(videos.length, numVideos)))
      }

      val behaviorType = behaviorTypes(random.nextInt(behaviorTypes.length))
      val baseDuration = if (hotVideoIds.contains(videoId)) {
        random.nextInt(240) + 30
      } else {
        random.nextInt(180) + 10
      }
      
      val duration = behaviorType match {
        case "play" => baseDuration
        case "like" => 5
        case "collect" => 8
        case "comment" => 25
        case _ => random.nextInt(60)
      }

      val deviceInfo = deviceInfos(random.nextInt(deviceInfos.length))
      val networkType = networkTypes(random.nextInt(networkTypes.length))
      val ipAddress = generateIpAddress()
      val location = generateLocation()
      val currentTime = new Timestamp(System.currentTimeMillis() - random.nextInt(86400000 * 7))

      behaviors += UserBehavior(
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

    behaviors.toList
  }
}

object LogGenerator {
  def apply(): LogGenerator = new LogGenerator()
}