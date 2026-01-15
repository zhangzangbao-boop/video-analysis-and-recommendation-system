package com.shortvideo.recommendation.common.exception

/**
 * Redis相关异常
 */
class RedisException(message: String, cause: Throwable = null)
  extends RuntimeException(message, cause)

/**
 * Redis连接异常
 */
class RedisConnectionException(message: String, cause: Throwable = null)
  extends RedisException(s"Redis连接失败: $message", cause)

/**
 * Redis操作异常
 */
class RedisOperationException(message: String, cause: Throwable = null)
  extends RedisException(s"Redis操作失败: $message", cause)