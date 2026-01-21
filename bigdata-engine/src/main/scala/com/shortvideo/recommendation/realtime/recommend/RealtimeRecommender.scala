package com.shortvideo.recommendation.realtime.recommend

import com.shortvideo.recommendation.common.config.DatabaseConfig
import com.shortvideo.recommendation.common.entity.{Recommendation, UserBehavior}
import com.shortvideo.recommendation.common.utils.RedisUtil

import java.sql.{DriverManager, PreparedStatement, Timestamp}
import com.shortvideo.recommendation.realtime.model.VideoInfoRow

import org.apache.spark.rdd.RDD

/**
 * Real-time Recommender (Minimal Working Version)
 *
 * Strategy:
 * - Candidate set: Prioritize real-time popular videos from Redis rec:video:hot (Top M), 
 *   fallback to offline hot sorting from video_info if not available
 * - Filter: Filter out videos that users have already interacted with in this batch
 * - Output: Each user's TopN written to Redis: user_recs:{userId} (ZSet)
 */
object RealtimeRecommender {

  private val REDIS_HOT_KEY = "rec:video:hot"

  private val REALTIME_RECOMMEND_TYPE = "REALTIME"
  private val REALTIME_MODEL_ID = "realtime-hot"
  private val REALTIME_REASON = "hot_candidates_filtered"

  def generateAndSave(
                       behaviors: RDD[UserBehavior],
                       videoMap: Map[Long, VideoInfoRow],
                       topN: Int = 20,
                       hotCandidateSize: Int = 200
                     ): Unit = {
    println(s"[Recommendation Generation] Starting real-time recommendation generation, behavior data count: ${behaviors.count()}")
    
    if (behaviors.isEmpty()) {
      println("[Recommendation Generation] Behavior data is empty, skipping recommendation generation")
      return
    }

    // 1) Video collection that each user has interacted with in this batch (for filtering)
    println("[Recommendation Generation] Step 1: Collecting videos viewed by users...")
    val userSeen: Map[Long, Set[Long]] =
      behaviors
        .map(b => (b.userId, b.videoId))
        .groupByKey()
        .mapValues(_.toSet)
        .collectAsMap()
        .toMap

    println(s"[Recommendation Generation] Collected behavior data for ${userSeen.size} users")
    if (userSeen.isEmpty) {
      println("[Recommendation Generation] User behavior data is empty, skipping recommendation generation")
      return
    }

    // 2) Construct candidate set (prioritize Redis popular videos)
    println(s"[Recommendation Generation] Step 2: Constructing candidate set (target quantity: $hotCandidateSize)...")
    val hotCandidates: List[(Long, Double)] = loadHotCandidatesFromRedis(hotCandidateSize) match {
      case Some(list) if list.nonEmpty =>
        println(s"[Recommendation Generation] 从Redis加载了 ${list.size} 个热门视频（原始格式）")
        println(s"[诊断] Redis中的前10个视频ID（字符串）: ${list.take(10).map(_._1).mkString(", ")}")
        
        // 统计转换失败的情况
        var parseFailedCount = 0
        val parsed = list.flatMap { case (vidStr, score) => 
          toLong(vidStr) match {
            case Some(vidLong) => Some(vidLong -> score)
            case None =>
              parseFailedCount += 1
              if (parseFailedCount <= 5) {
                println(s"[诊断] 视频ID转换失败: '$vidStr' 无法转换为Long")
              }
              None
          }
        }
        println(s"[Recommendation Generation] 解析后的视频ID数量: ${parsed.size}（转换失败: $parseFailedCount）")
        if (parseFailedCount > 0) {
          println(s"[WARN] 有 $parseFailedCount 个视频ID无法转换为Long，已被过滤")
        }
        
        // 详细诊断过滤过程
        var notInMapCount = 0
        var notRecommendableCount = 0
        val filtered = parsed.filter { case (vid, _) => 
          videoMap.get(vid) match {
            case Some(videoInfo) =>
              val isRec = videoInfo.isRecommendable
              if (!isRec) {
                notRecommendableCount += 1
                if (notRecommendableCount <= 5) {
                  println(s"[诊断] 视频 $vid 在videoMap中，但isRecommendable=false (status=${videoInfo.status}, isDeleted=${videoInfo.isDeleted})")
                }
              }
              isRec
            case None =>
              notInMapCount += 1
              if (notInMapCount <= 5) {
                println(s"[诊断] 视频 $vid 不在videoMap中")
              }
              false
          }
        }
        println(s"[Recommendation Generation] 过滤后剩余 ${filtered.size} 个可推荐视频")
        println(s"[诊断] 过滤统计: 不在videoMap中=$notInMapCount, isRecommendable=false=$notRecommendableCount")
        
        if (filtered.isEmpty && parsed.nonEmpty) {
          println(s"[WARN] ====== 所有热门视频都被过滤掉了！======")
          println(s"[WARN] Redis中的前10个视频ID（Long）: ${parsed.take(10).map(_._1).mkString(", ")}")
          println(s"[WARN] videoMap中的前20个ID: ${videoMap.keys.take(20).toSeq.sorted.mkString(", ")}")
          println(s"[WARN] videoMap ID范围: ${if (videoMap.nonEmpty) s"${videoMap.keys.min}~${videoMap.keys.max}" else "空"}")
          println(s"[WARN] videoMap总数: ${videoMap.size}")
          println(s"[WARN] 将使用离线热门视频作为候选集")
        }
        filtered

      case _ =>
        println(s"[Recommendation Generation] Redis热门视频为空，使用离线热门排序作为候选集")
        Nil
    }
    
    // 如果Redis候选集为空，使用离线热门视频作为fallback
    val finalCandidates = if (hotCandidates.isEmpty) {
      println(s"[Recommendation Generation] Redis候选集为空，使用video_info中的离线热门视频作为候选集")
      val offlineCandidates = videoMap.valuesIterator
        .filter(_.isRecommendable)
        .toList
        .sortBy(v => -v.offlineHotScore)
        .take(hotCandidateSize)
        .map(v => v.id -> v.offlineHotScore)
      println(s"[Recommendation Generation] 从video_info加载了 ${offlineCandidates.size} 个候选视频")
      offlineCandidates
    } else {
      hotCandidates
    }

    if (finalCandidates.isEmpty) {
      println("[Recommendation Generation] 候选集为空，跳过推荐生成（可能原因：video_info中没有可推荐视频）")
      return
    }

    // 3) Generate TopN recommendations for each user and write to Redis
    println(s"[Recommendation Generation] Step 3: 为 ${userSeen.size} 个用户生成推荐（候选集大小: ${finalCandidates.size}）...")
    var totalRecsGenerated = 0
    var totalRecsWritten = 0
    
    userSeen.foreach { case (userId, seenSet) =>
      val recs = finalCandidates
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

      totalRecsGenerated += recs.size
      
      if (recs.nonEmpty) {
        try {
          RedisUtil.saveUserRecs(userId, recs)
          println(s"[INFO] Real-time recommendations for user $userId have been written to Redis: ${recs.size} items")
          totalRecsWritten += recs.size
        } catch {
          case e: Exception =>
            println(s"[ERROR] Failed to write real-time recommendations for user $userId to Redis: ${e.getMessage}")
            e.printStackTrace()
        }
        try {
          System.out.println(s"[Recommendation Generation] ====== Preparing to write to MySQL ======")
          System.out.println(s"[Recommendation Generation] UserID: $userId, Recommendation count: ${recs.size}")
          System.out.flush()
          saveRealtimeRecsToMySQL(userId, recs)
          System.out.println(s"[Recommendation Generation] ====== MySQL write completed ======")
          System.out.flush()
        } catch {
          case e: Exception =>
            System.err.println(s"[ERROR] ====== MySQL write exception ======")
            System.err.println(s"[ERROR] Real-time recommendations for user $userId failed to write to MySQL: ${e.getMessage}")
            System.err.println(s"[ERROR] Exception type: ${e.getClass.getName}")
            e.printStackTrace(System.err)
            System.err.flush()
        }
      } else {
        println(s"[WARN] No recommendable videos for user $userId (all candidate videos have been viewed)")
      }
    }
    
    println(s"[Recommendation Generation] Recommendation generation completed: Generated $totalRecsGenerated recommendations for ${userSeen.size} users, successfully wrote $totalRecsWritten items")
  }

  private def loadHotCandidatesFromRedis(topM: Int): Option[List[(String, Double)]] = {
    try {
      val list = RedisUtil.zrevrangeWithScores(REDIS_HOT_KEY, 0, topM - 1)
      if (list.nonEmpty) {
        println(s"[INFO] Loaded popular candidate videos from Redis: ${list.size} items")
        Some(list)
      } else {
        println(s"[WARN] Redis popular video list is empty, will use offline hot sorting")
        None
      }
    } catch {
      case e: Exception =>
        println(s"[WARN] Failed to load popular candidate videos from Redis: ${e.getMessage}, will use offline hot sorting")
        e.printStackTrace()
        None
    }
  }

  private def toLong(s: String): Option[Long] = {
    try Some(s.toLong) catch { case _: Exception => None }
  }

  /**
   * Write real-time recommendation results to MySQL
   * Using INSERT ... ON DUPLICATE KEY UPDATE to ensure idempotency
   */
  private def saveRealtimeRecsToMySQL(userId: Long, recs: List[Recommendation]): Unit = {
    // Immediately output logs to ensure method call is visible
    System.out.println(s"========================================")
    System.out.println(s"[MySQL] ====== Starting MySQL Write ======")
    System.out.println(s"[MySQL] UserID: $userId")
    System.out.println(s"[MySQL] Recommendation count: ${recs.size}")
    System.out.println(s"[MySQL] Timestamp: ${System.currentTimeMillis()}")
    System.out.flush()
    
    var connection: java.sql.Connection = null
    var pstmt: PreparedStatement = null

    val sql =
      """
        |INSERT INTO recommendation_result
        |(user_id, video_id, score, `rank`, `type`, reason, model_id, create_time, update_time)
        |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        |ON DUPLICATE KEY UPDATE
        |score = VALUES(score),
        |`rank` = VALUES(`rank`),
        |reason = VALUES(reason),
        |model_id = VALUES(model_id),
        |update_time = VALUES(update_time)
      """.stripMargin

    try {
      System.out.println(s"[MySQL] Step 1: Loading MySQL driver...")
      System.out.flush()
      Class.forName("com.mysql.cj.jdbc.Driver")
      System.out.println(s"[MySQL] MySQL driver loaded successfully")
      
      System.out.println(s"[MySQL] Step 2: Preparing to connect to database...")
      System.out.println(s"[MySQL] Connection URL: ${DatabaseConfig.JDBC_URL}")
      System.out.println(s"[MySQL] Username: ${DatabaseConfig.JDBC_USER}")
      System.out.println(s"[MySQL] Password: ${if (DatabaseConfig.JDBC_PASSWORD.nonEmpty) "***" else "(empty)"}")
      System.out.flush()
      
      System.out.println(s"[MySQL] Step 3: Establishing database connection...")
      System.out.flush()
      connection = DriverManager.getConnection(DatabaseConfig.JDBC_URL, DatabaseConfig.JDBC_USER, DatabaseConfig.JDBC_PASSWORD)
      System.out.println(s"[MySQL] ✓ Database connection successful!")
      System.out.flush()
      
      System.out.println(s"[MySQL] Step 4: Preparing SQL statement...")
      System.out.flush()
      connection.setAutoCommit(false)
      pstmt = connection.prepareStatement(sql)
      System.out.println(s"[MySQL] ✓ SQL statement prepared successfully")
      System.out.flush()

      System.out.println(s"[MySQL] Step 5: Starting batch insert data...")
      System.out.flush()
      val currentTime = new Timestamp(System.currentTimeMillis())
      var batchCount = 0
      var totalInserted = 0

      recs.zipWithIndex.foreach { case (rec, index) =>
        val rank = index + 1
        pstmt.setLong(1, rec.userId)
        pstmt.setLong(2, rec.videoId)
        pstmt.setDouble(3, rec.score)
        pstmt.setInt(4, rank)
        pstmt.setString(5, REALTIME_RECOMMEND_TYPE)
        pstmt.setString(6, REALTIME_REASON)
        pstmt.setString(7, REALTIME_MODEL_ID)
        pstmt.setTimestamp(8, currentTime)
        pstmt.setTimestamp(9, currentTime)

        pstmt.addBatch()
        batchCount += 1

        if (batchCount >= 1000) {
          System.out.println(s"[MySQL] Executing batch insert (1000 records)...")
          System.out.flush()
          val results = pstmt.executeBatch()
          totalInserted += results.count(_ >= 0)
          connection.commit()
          batchCount = 0
          System.out.println(s"[MySQL] ✓ Batch commit 1000 records successful")
          System.out.flush()
        }
      }

      if (batchCount > 0) {
        System.out.println(s"[MySQL] Executing final batch insert ($batchCount records)...")
        System.out.flush()
        val results = pstmt.executeBatch()
        totalInserted += results.count(_ >= 0)
        connection.commit()
        System.out.println(s"[MySQL] ✓ Batch commit final $batchCount records successful")
        System.out.flush()
      }
      
      System.out.println(s"[MySQL] ========================================")
      System.out.println(s"[MySQL] ✓✓✓ MySQL write successful!")
      System.out.println(s"[MySQL] UserID: $userId")
      System.out.println(s"[MySQL] Processed records: ${recs.size} items")
      System.out.println(s"[MySQL] ========================================")
      System.out.flush()
      
    } catch {
      case e: java.sql.SQLException =>
        System.err.println(s"========================================")
        System.err.println(s"[ERROR] MySQL SQL exception!")
        System.err.println(s"[ERROR] UserID: $userId")
        System.err.println(s"[ERROR] Error message: ${e.getMessage}")
        System.err.println(s"[ERROR] SQL State: ${e.getSQLState}")
        System.err.println(s"[ERROR] Error code: ${e.getErrorCode}")
        System.err.println(s"[ERROR] Connection URL: ${DatabaseConfig.JDBC_URL}")
        System.err.println(s"[ERROR] Username: ${DatabaseConfig.JDBC_USER}")
        e.printStackTrace(System.err)
        System.err.flush()
        if (connection != null) {
          try {
            connection.rollback()
            System.out.println(s"[MySQL] Transaction rolled back")
            System.out.flush()
          } catch {
            case rollbackEx: Exception =>
              System.err.println(s"[ERROR] Rollback failed: ${rollbackEx.getMessage}")
              System.err.flush()
          }
        }
      case e: ClassNotFoundException =>
        System.err.println(s"========================================")
        System.err.println(s"[ERROR] MySQL driver class not found!")
        System.err.println(s"[ERROR] Error: ${e.getMessage}")
        System.err.println(s"[ERROR] Please check if MySQL JDBC driver dependency is added")
        e.printStackTrace(System.err)
        System.err.flush()
      case e: Exception =>
        System.err.println(s"========================================")
        System.err.println(s"[ERROR] MySQL write failed!")
        System.err.println(s"[ERROR] UserID: $userId")
        System.err.println(s"[ERROR] Exception type: ${e.getClass.getName}")
        System.err.println(s"[ERROR] Error message: ${e.getMessage}")
        e.printStackTrace(System.err)
        System.err.flush()
        if (connection != null) {
          try {
            connection.rollback()
            System.out.println(s"[MySQL] Transaction rolled back")
            System.out.flush()
          } catch {
            case rollbackEx: Exception =>
              System.err.println(s"[ERROR] Rollback failed: ${rollbackEx.getMessage}")
              System.err.flush()
          }
        }
    } finally {
      try {
        if (pstmt != null) {
          pstmt.close()
          System.out.println(s"[MySQL] PreparedStatement closed")
          System.out.flush()
        }
      } catch {
        case e: Exception =>
          System.err.println(s"[WARN] Closing PreparedStatement failed: ${e.getMessage}")
          System.err.flush()
      }
      try {
        if (connection != null) {
          connection.close()
          System.out.println(s"[MySQL] Database connection closed")
          System.out.flush()
        }
      } catch {
        case e: Exception =>
          System.err.println(s"[WARN] Closing database connection failed: ${e.getMessage}")
          System.err.flush()
      }
    }
  }
}