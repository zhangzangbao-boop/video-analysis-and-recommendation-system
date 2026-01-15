package com.shortvideo.recommendation.common.exception

/**
 * Spark相关异常
 */
class SparkException(message: String, cause: Throwable = null)
  extends RuntimeException(message, cause)

/**
 * Spark初始化异常
 */
class SparkInitException(message: String, cause: Throwable = null)
  extends SparkException(s"Spark初始化失败: $message", cause)

/**
 * Spark作业执行异常
 */
class SparkJobException(message: String, cause: Throwable = null)
  extends SparkException(s"Spark作业执行失败: $message", cause)