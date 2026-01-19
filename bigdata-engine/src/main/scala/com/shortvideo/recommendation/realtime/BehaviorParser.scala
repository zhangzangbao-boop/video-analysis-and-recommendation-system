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
            // 1. 解析成中间格式 UserBehaviorEvent
            val event = parseToEvent(value)

            // 2. 转换为业务实体 UserBehavior
            Some(convertToUserBehavior(event))
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
   * 将 JSON 字符串解析为 UserBehaviorEvent
   */
  private def parseToEvent(jsonStr: String): UserBehaviorEvent = {
    Json.parse(jsonStr).validate[UserBehaviorEvent] match {
      case JsSuccess(event, _) => event
      case JsError(errors) =>
        throw new KafkaException(s"JSON 解析失败: ${errors.mkString(", ")}, raw: $jsonStr")
    }
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