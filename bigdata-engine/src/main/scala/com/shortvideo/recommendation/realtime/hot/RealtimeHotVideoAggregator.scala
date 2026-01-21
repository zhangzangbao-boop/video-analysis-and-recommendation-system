package com.shortvideo.recommendation.realtime.hot

import com.shortvideo.recommendation.common.config.RedisConfig
import com.shortvideo.recommendation.common.entity.UserBehavior
import com.shortvideo.recommendation.common.utils.RedisUtil
import org.apache.spark.streaming.Seconds
import org.apache.spark.streaming.dstream.DStream
import scala.collection.JavaConverters._

/**
 * 实时热门视频统计（写入 Redis ZSet: rec:video:hot）
 *
 * 设计：
 * - Key: rec:video:hot
 * - member: videoId
 * - score: 热度分（按行为类型加权累计）
 *
 * 说明：
 * - 这是"最小可用"的热门榜实现：按 batch 聚合后累加到 ZSet
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
        val totalCount = rdd.count()
        println(s"[热门统计] 本批次需要更新 ${totalCount} 个视频的热度分")
        
        // 分区写 Redis，避免 collect 到 driver
        // 注意：在executor端执行，RedisUtil会自动懒加载初始化连接池
        rdd.foreachPartition { it =>
          // 确保在executor端初始化连接池（如果还未初始化）
          try {
            val jedisOpt = RedisUtil.getConnection
            jedisOpt match {
              case Some(jedis) =>
                try {
                  var count = 0
                  var totalScore = 0.0
                  it.foreach { case (videoId, scoreInc) =>
                    // Jedis 方法名为 zincrby（全小写）
                    val newScore = jedis.zincrby(HOT_KEY, scoreInc, videoId.toString)
                    count += 1
                    totalScore += scoreInc
                    if (count <= 5) {
                      println(s"[热门统计] 更新视频 $videoId 热度: +$scoreInc, 新总分: $newScore")
                    }
                  }

                  if (count > 0) {
                    println(s"[热门统计] 本分区更新了 $count 个视频，总热度增量: $totalScore")
                    
                    // 设置过期（每个分区都设置一次也无妨；也可做采样设置）
                    // expire返回Long: 1表示成功，0表示key不存在或设置失败
                    val expireResult = jedis.expire(HOT_KEY, expireSeconds)
                    if (expireResult == 1) {
                      println(s"[热门统计] 已设置Key过期时间: ${expireSeconds}秒")
                    } else {
                      println(s"[WARN] 设置Key过期时间失败 (返回码: $expireResult，可能key不存在)")
                    }

                    // 裁剪：只保留 topLimit（按分数从高到低），删除低分尾部
                    val size = jedis.zcard(HOT_KEY)
                    println(s"[热门统计] 当前热门视频总数: $size")
                    if (size > topLimit) {
                      // ZREMRANGEBYRANK 按 rank 从低到高，删除 [0, size-topLimit-1] 的低分部分
                      val removed = jedis.zremrangeByRank(HOT_KEY, 0, size - topLimit - 1)
                      println(s"[热门统计] 裁剪后保留Top $topLimit，删除了 $removed 个低分视频")
                    }
                    
                    // 获取Top 5热门视频（用于验证）
                    val top5 = jedis.zrevrangeWithScores(HOT_KEY, 0, 4)
                    if (top5 != null && !top5.isEmpty) {
                      println(s"[热门统计] Top 5热门视频:")
                      top5.asScala.zipWithIndex.foreach { case (tuple, idx) =>
                        println(s"  ${idx + 1}. 视频ID: ${tuple.getElement}, 热度分: ${tuple.getScore}")
                      }
                    }
                  } else {
                    println("[热门统计] 本分区没有需要更新的视频")
                  }
                } catch {
                  case e: Exception =>
                    println(s"[ERROR] 写入Redis热门视频数据失败: ${e.getMessage}")
                    e.printStackTrace()
                } finally {
                  RedisUtil.closeConnection(jedis)
                }
              case None =>
                println("[WARN] 无法获取Redis连接，跳过本分区的热门视频统计")
            }
          } catch {
            case e: Exception =>
              println(s"[ERROR] Executor端Redis连接初始化失败: ${e.getMessage}")
              e.printStackTrace()
          }
        }
      } else {
        println("[热门统计] 本批次没有需要更新的视频热度数据")
      }
    }
  }
}

