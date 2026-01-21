package com.shortvideo.recommendation.realtime.hot

import com.shortvideo.recommendation.common.entity.UserBehavior
import com.shortvideo.recommendation.common.utils.RedisUtil
import org.apache.spark.streaming.Seconds
import org.apache.spark.streaming.dstream.DStream

/**
 * 实时热门视频统计（写入 Redis ZSet: rec:video:hot）
 *
 * 设计：
 * - Key: rec:video:hot
 * - member: videoId
 * - score: 热度分（按行为类型加权累计）
 *
 * 说明：
 * - 这是“最小可用”的热门榜实现：按 batch 聚合后累加到 ZSet
 * - 后续可升级为窗口衰减（滑动窗口/时间衰减）以避免永远单调递增
 */
object RealtimeHotVideoAggregator {

  private val HOT_KEY = "rec:video:hot"

  // 行为权重（可按业务调整）
  private val WEIGHTS: Map[String, Double] = Map(
    "play" -> 3.0,
    "view" -> 1.0,
    "like" -> 5.0,
    "collect" -> 4.0,
    "comment" -> 4.0,
    "share" -> 4.0,
    "follow" -> 2.0,
    "unfollow" -> -2.0
  )

  /**
   * 启动热门统计
   *
   * @param behaviorStream 解析后的用户行为流
   * @param topLimit       ZSet 最大保留多少个视频（避免无限增长）
   * @param expireSeconds  Key 过期时间（秒）
   */
  def start(
             behaviorStream: DStream[UserBehavior],
             topLimit: Int = 1000,
             expireSeconds: Long = 7 * 24 * 60 * 60
           ): Unit = {

    // 每个 batch 聚合一次：对同一视频的热度分求和
    val batchScores = behaviorStream
      .map { b =>
        val w = WEIGHTS.getOrElse(b.behaviorType.toLowerCase, 0.0)
        (b.videoId, w)
      }
      .filter(_._2 != 0.0)
      .reduceByKey(_ + _)

    batchScores.foreachRDD { rdd =>
      if (!rdd.isEmpty()) {
        // 分区写 Redis，避免 collect 到 driver
        rdd.foreachPartition { it =>
          val jedisOpt = RedisUtil.getConnection
          jedisOpt match {
            case Some(jedis) =>
              try {
                it.foreach { case (videoId, scoreInc) =>
                  // Jedis 方法名为 zincrby（全小写）
                  jedis.zincrby(HOT_KEY, scoreInc, videoId.toString)
                }

                // 设置过期（每个分区都设置一次也无妨；也可做采样设置）
                jedis.expire(HOT_KEY, expireSeconds:Long)

                // 裁剪：只保留 topLimit（按分数从高到低），删除低分尾部
                val size = jedis.zcard(HOT_KEY)
                if (size > topLimit) {
                  // ZREMRANGEBYRANK 按 rank 从低到高，删除 [0, size-topLimit-1] 的低分部分
                  jedis.zremrangeByRank(HOT_KEY, 0, size - topLimit - 1)
                }
              } finally {
                RedisUtil.closeConnection(jedis)
              }
            case None => // Redis 连接获取失败，跳过本分区
          }
        }
      }
    }
  }
}

