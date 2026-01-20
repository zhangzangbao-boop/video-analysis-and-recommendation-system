package com.shortvideo.recommendation.realtime.recommend

import com.shortvideo.recommendation.common.entity.{Recommendation, UserBehavior}
import com.shortvideo.recommendation.common.utils.RedisUtil
import com.shortvideo.recommendation.realtime.model.VideoInfoRow

import org.apache.spark.rdd.RDD

/**
 * 实时推荐生成器（最小可跑版本）
 *
 * 策略：
 * - 候选集：优先取 Redis 的实时热门 rec:video:hot（Top M），如果没有则用 video_info 的离线热度排序兜底
 * - 过滤：过滤掉本批次用户已经互动过的视频（user 本批次行为里的 videoId）
 * - 输出：每个用户 TopN 写入 Redis：user_recs:{userId} (ZSet)
 */
object RealtimeRecommender {

  private val REDIS_HOT_KEY = "rec:video:hot"

  def generateAndSave(
                       behaviors: RDD[UserBehavior],
                       videoMap: Map[Long, VideoInfoRow],
                       topN: Int = 20,
                       hotCandidateSize: Int = 200
                     ): Unit = {
    if (behaviors.isEmpty()) return

    // 1) 本批次每个用户已经互动过的视频集合（用于过滤）
    val userSeen: Map[Long, Set[Long]] =
      behaviors
        .map(b => (b.userId, b.videoId))
        .groupByKey()
        .mapValues(_.toSet)
        .collectAsMap()
        .toMap

    if (userSeen.isEmpty) return

    // 2) 构造候选集（优先 Redis 热门）
    val hotCandidates: List[(Long, Double)] = loadHotCandidatesFromRedis(hotCandidateSize) match {
      case Some(list) if list.nonEmpty =>
        list
          .flatMap { case (vidStr, score) => toLong(vidStr).map(_ -> score) }
          .filter { case (vid, _) => videoMap.get(vid).exists(_.isRecommendable) }

      case _ =>
        // 兜底：video_info 离线热度排序
        videoMap.valuesIterator
          .filter(_.isRecommendable)
          .toList
          .sortBy(v => -v.offlineHotScore)
          .take(hotCandidateSize)
          .map(v => v.id -> v.offlineHotScore)
    }

    if (hotCandidates.isEmpty) return

    // 3) 为每个用户生成 TopN 推荐并写入 Redis
    userSeen.foreach { case (userId, seenSet) =>
      val recs = hotCandidates
        .iterator
        .filterNot { case (vid, _) => seenSet.contains(vid) }
        .take(topN)
        .map { case (vid, score) =>
          Recommendation(
            userId = userId,
            videoId = vid,
            score = score,
            source = "realtime"
          )
        }
        .toList

      if (recs.nonEmpty) {
        RedisUtil.saveUserRecs(userId, recs)
      }
    }
  }

  private def loadHotCandidatesFromRedis(topM: Int): Option[List[(String, Double)]] = {
    try {
      val list = RedisUtil.zrevrangeWithScores(REDIS_HOT_KEY, 0, topM - 1)
      Some(list)
    } catch {
      case _: Exception => None
    }
  }

  private def toLong(s: String): Option[Long] = {
    try Some(s.toLong) catch { case _: Exception => None }
  }
}

