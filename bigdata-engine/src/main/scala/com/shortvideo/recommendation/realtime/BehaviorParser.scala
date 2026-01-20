package com.shortvideo.recommendation.realtime

import com.shortvideo.recommendation.common.entity.{UserBehavior, UserBehaviorEvent}
import com.shortvideo.recommendation.common.exception.{KafkaException, SparkException}
import com.shortvideo.recommendation.common.utils.TimeUtil
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.streaming.Seconds
import org.apache.spark.streaming.dstream.DStream
import play.api.libs.json._

import java.sql.Timestamp
import java.time.{Instant, ZoneId}

/**
 * 实时用户行为数据解析器
 * 职责：Kafka原始消息 → UserBehavior 实体
 */
object BehaviorParser {

  // JSON 隐式格式定义（使用 play-json）
  implicit val userBehaviorEventReads: Reads[UserBehaviorEvent] = Json.reads[UserBehaviorEvent]

  // 兼容“直接就是UserBehavior字段”的日志格式（你当前Kafka里的JSON就是这种）
  // 示例：{"behaviorTime":"2026-01-20 13:51:35.653","behaviorType":"share",...}
  private implicit val userBehaviorReads: Reads[UserBehavior] = new Reads[UserBehavior] {
    override def reads(json: JsValue): JsResult[UserBehavior] = {
      def str(path: String): Option[String] = (json \ path).asOpt[String]
      def long(path: String): Option[Long] = (json \ path).asOpt[Long].orElse((json \ path).asOpt[Int].map(_.toLong))
      def int(path: String): Option[Int] = (json \ path).asOpt[Int].orElse((json \ path).asOpt[Long].map(_.toInt))

      val userId = long("userId")
      val videoId = long("videoId")
      val behaviorType = str("behaviorType").orElse(str("behavior"))

      // behaviorTime: "yyyy-MM-dd HH:mm:ss.SSS" 或 "yyyy-MM-dd HH:mm:ss"
      val behaviorTimeStr = str("behaviorTime").orElse(str("createDate"))
      val behaviorTs: Option[Timestamp] = behaviorTimeStr.flatMap(parseTimestampString)

      if (userId.isEmpty || videoId.isEmpty || behaviorType.isEmpty || behaviorTs.isEmpty) {
        JsError("missing required fields for UserBehavior (userId, videoId, behaviorType/behavior, behaviorTime)")
      } else {
        JsSuccess(
          UserBehavior(
            userId = userId.get,
            videoId = videoId.get,
            behaviorType = behaviorType.get.toLowerCase,
            behaviorTime = behaviorTs.get,
            duration = int("duration").getOrElse(0),
            deviceInfo = str("deviceInfo").orElse(str("device")).getOrElse(""),
            networkType = str("networkType").orElse(str("network")).getOrElse(""),
            ipAddress = str("ipAddress").orElse(str("ip")).getOrElse(""),
            location = str("location").getOrElse(""),
            extraInfo = str("extraInfo").orElse(str("extra")).getOrElse("")
          )
        )
      }
    }
  }

  /**
   * 主解析入口：从原始 Kafka DStream 解析成 UserBehavior 流
   *
   * @param rawStream Kafka 原始输入流
   * @return 解析后的 UserBehavior DStream
   */
  def parse(rawStream: DStream[ConsumerRecord[String, String]]): DStream[UserBehavior] = {
    rawStream
      .map(record => (record.key(), record.value()))     // (key, value)
      .flatMap {
        case (_, value) =>
          try {
            // 兼容两种格式：
            // A) 标准事件格式 UserBehaviorEvent（包含 logId/timestamp/behavior/properties）
            // B) 直接实体格式 UserBehavior（包含 behaviorTime/behaviorType/duration/deviceInfo...）
            parseToUserBehavior(value)
          } catch {
            case e: Exception =>
              // 记录错误日志，但不抛异常打断流（生产环境建议用结构化日志）
              println(s"行为数据解析失败: ${e.getMessage}, raw: $value")
              None
          }
      }
      .filter(isValidBehavior)  // 过滤掉无效数据
  }

  /**
   * 将 JSON 字符串解析为 UserBehavior（兼容两种日志结构）
   */
  private def parseToUserBehavior(jsonStr: String): Option[UserBehavior] = {
    val js = Json.parse(jsonStr)

    // 先尝试 UserBehaviorEvent
    js.validate[UserBehaviorEvent] match {
      case JsSuccess(event, _) =>
        Some(convertToUserBehavior(event))
      case JsError(_) =>
        // 再尝试直接 UserBehavior
        js.validate[UserBehavior] match {
          case JsSuccess(behavior, _) => Some(behavior)
          case JsError(errors2) =>
            throw new KafkaException(s"JSON 解析失败: ${errors2.mkString(", ")}, raw: $jsonStr")
        }
    }
  }

  /**
   * 将 JSON 字符串解析为 UserBehaviorEvent
   */
  private def parseToEvent(jsonStr: String): UserBehaviorEvent = {
    Json.parse(jsonStr).validate[UserBehaviorEvent] match {
      case JsSuccess(event, _) => event
      case JsError(errors) =>
        throw new KafkaException(s"JSON 解析失败: ${errors.mkString(", ")}, raw: $jsonStr")
    }
  }

  private def parseTimestampString(s: String): Option[Timestamp] = {
    // 支持毫秒/无毫秒两种格式
    val patterns = List(
      "yyyy-MM-dd HH:mm:ss.SSS",
      "yyyy-MM-dd HH:mm:ss"
    )

    patterns.view.flatMap { p =>
      try {
        val fmt = new java.text.SimpleDateFormat(p)
        Some(new Timestamp(fmt.parse(s).getTime))
      } catch {
        case _: Exception => None
      }
    }.headOption
  }

  /**
   * 从 UserBehaviorEvent 转换为 UserBehavior 实体
   * 进行字段映射、默认值填充、时间转换等
   */
  private def convertToUserBehavior(event: UserBehaviorEvent): UserBehavior = {
    val ts = new Timestamp(event.timestamp)  // Kafka时间戳通常是毫秒级

    // 常见 properties 字段提取（根据实际日志格式调整）
    val props = event.properties

    UserBehavior(
      userId       = event.userId,
      videoId      = event.videoId,
      behaviorType = event.behavior.toLowerCase,  // 统一小写，便于后续处理
      behaviorTime = ts,
      duration     = props.get("duration").map(_.toInt).getOrElse(0),
      deviceInfo   = props.getOrElse("device", ""),
      networkType  = props.getOrElse("network", ""),
      ipAddress    = props.getOrElse("ip", ""),
      location     = props.getOrElse("location", ""),
      extraInfo    = props.getOrElse("extra", "")
    )
  }

  /**
   * 简单的数据有效性校验
   */
  private def isValidBehavior(behavior: UserBehavior): Boolean = {
    behavior.userId > 0 &&
      behavior.videoId > 0 &&
      behavior.behaviorType.nonEmpty &&
      behavior.behaviorTime != null &&
      behavior.behaviorTime.getTime > 0
  }

}