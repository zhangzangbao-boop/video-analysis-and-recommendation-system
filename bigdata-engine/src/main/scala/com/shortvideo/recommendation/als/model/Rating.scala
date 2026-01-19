package com.shortvideo.recommendation.als.model

case class Rating(
                   userId: Long,        // 用户ID
                   movieId: Long,       // 视频ID
                   rating: Float,       // 评分
                   timestamp: Long      // 时间戳
                 )
