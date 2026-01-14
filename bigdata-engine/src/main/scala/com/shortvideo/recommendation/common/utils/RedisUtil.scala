package com.shortvideo.recommendation.common.utils

import redis.clients.jedis.{Jedis, JedisPool, JedisPoolConfig}
import com.shortvideo.recommendation.common.Constants
import com.shortvideo.recommendation.common.config.RedisConfig
import scala.collection.JavaConverters._
import scala.util.{Try, Success, Failure}
import java.util.{Map => JMap}

/**
 * Redis工具类
 */
object RedisUtils {

  private var jedisPool: JedisPool = _

  /**
   * 初始化Redis连接池
   */
  def initPool(config: RedisConfig): Unit = {
    val poolConfig = new JedisPoolConfig()

    // 连接池配置
    poolConfig.setMaxTotal(config.maxTotal)
    poolConfig.setMaxIdle(config.maxIdle)
    poolConfig.setMinIdle(config.minIdle)
    poolConfig.setTestOnBorrow(true)
    poolConfig.setTestOnReturn(true)
    poolConfig.setTestWhileIdle(true)
    poolConfig.setTimeBetweenEvictionRunsMillis(30000)
    poolConfig.setNumTestsPerEvictionRun(config.maxTotal)
    poolConfig.setMinEvictableIdleTimeMillis(60000)

    // 创建连接池
    if (config.password.nonEmpty) {
      jedisPool = new JedisPool(poolConfig, config.host, config.port,
        config.timeout, config.password, config.database)
    } else {
      jedisPool = new JedisPool(poolConfig, config.host, config.port,
        config.timeout, null, config.database)
    }

    println(s"Redis连接池初始化成功: ${config.host}:${config.port}")
  }

  /**
   * 获取Redis连接
   */
  def getConnection: Option[Jedis] = {
    Try {
      if (jedisPool == null) {
        throw new IllegalStateException("Redis连接池未初始化，请先调用initPool方法")
      }
      jedisPool.getResource
    } match {
      case Success(jedis) => Some(jedis)
      case Failure(e) =>
        println(s"获取Redis连接失败: ${e.getMessage}")
        None
    }
  }

  /**
   * 关闭Redis连接
   */
  def closeConnection(jedis: Jedis): Unit = {
    if (jedis != null) {
      jedis.close()
    }
  }

  /**
   * 关闭连接池
   */
  def closePool(): Unit = {
    if (jedisPool != null && !jedisPool.isClosed) {
      jedisPool.close()
    }
  }

  // ============ 字符串操作 ============

  /**
   * 设置字符串值
   */
  def set(key: String, value: String, expireSeconds: Long= 0): Boolean = {
    getConnection match {
      case Some(jedis) =>
        try {
          if (expireSeconds > 0) {
            jedis.setex(key, expireSeconds,value)
          } else {
            jedis.set(key, value)
          }
          true
        } catch {
          case e: Exception =>
            println(s"Redis设置失败: key=$key, error=${e.getMessage}")
            false
        } finally {
          closeConnection(jedis)
        }
      case None => false
    }
  }

  /**
   * 获取字符串值
   */
  def get(key: String): Option[String] = {
    getConnection match {
      case Some(jedis) =>
        try {
          Option(jedis.get(key))
        } catch {
          case e: Exception =>
            println(s"Redis获取失败: key=$key, error=${e.getMessage}")
            None
        } finally {
          closeConnection(jedis)
        }
      case None => None
    }
  }

  // ============ Hash操作 ============

  /**
   * 设置Hash字段
   */
  def hset(key: String, field: String, value: String): Boolean = {
    getConnection match {
      case Some(jedis) =>
        try {
          jedis.hset(key, field, value) > 0
        } catch {
          case e: Exception =>
            println(s"Redis HSET失败: key=$key, field=$field, error=${e.getMessage}")
            false
        } finally {
          closeConnection(jedis)
        }
      case None => false
    }
  }

  /**
   * 批量设置Hash字段
   */
  def hmset(key: String, hash: Map[String, String]): Boolean = {
    getConnection match {
      case Some(jedis) =>
        try {
          jedis.hmset(key, hash.asJava)
          true
        } catch {
          case e: Exception =>
            println(s"Redis HMSET失败: key=$key, error=${e.getMessage}")
            false
        } finally {
          closeConnection(jedis)
        }
      case None => false
    }
  }

  /**
   * 获取Hash字段值
   */
  def hget(key: String, field: String): Option[String] = {
    getConnection match {
      case Some(jedis) =>
        try {
          Option(jedis.hget(key, field))
        } catch {
          case e: Exception =>
            println(s"Redis HGET失败: key=$key, field=$field, error=${e.getMessage}")
            None
        } finally {
          closeConnection(jedis)
        }
      case None => None
    }
  }

  /**
   * 获取所有Hash字段
   */
  def hgetAll(key: String): Map[String, String] = {
    getConnection match {
      case Some(jedis) =>
        try {
          jedis.hgetAll(key).asScala.toMap
        } catch {
          case e: Exception =>
            println(s"Redis HGETALL失败: key=$key, error=${e.getMessage}")
            Map.empty
        } finally {
          closeConnection(jedis)
        }
      case None => Map.empty
    }
  }

  // ============ ZSet操作 ============

  /**
   * 添加ZSet成员
   */
  def zadd(key: String, score: Double, member: String): Boolean = {
    getConnection match {
      case Some(jedis) =>
        try {
          jedis.zadd(key, score, member) > 0
        } catch {
          case e: Exception =>
            println(s"Redis ZADD失败: key=$key, member=$member, error=${e.getMessage}")
            false
        } finally {
          closeConnection(jedis)
        }
      case None => false
    }
  }

  /**
   * 批量添加ZSet成员
   */
  def zaddBatch(key: String, members: Map[String, Double]): Long = {
    getConnection match {
      case Some(jedis) =>
        try {

          // 使用Java Map转换
          val javaMap = new java.util.HashMap[String, java.lang.Double]()
          members.foreach { case (member, score) =>
            javaMap.put(member, new java.lang.Double(score))
          }
          jedis.zadd(key, javaMap)

        } catch {
          case e: Exception =>
            println(s"Redis ZADD批量失败: key=$key, error=${e.getMessage}")
            0L
        } finally {
          closeConnection(jedis)
        }
      case None => 0L
    }
  }

  /**
   * 获取ZSet排名
   */
  def zrevrange(key: String, start: Long, end: Long): List[String] = {
    getConnection match {
      case Some(jedis) =>
        try {
          jedis.zrevrange(key, start, end).asScala.toList
        } catch {
          case e: Exception =>
            println(s"Redis ZREVRANGE失败: key=$key, error=${e.getMessage}")
            List.empty
        } finally {
          closeConnection(jedis)
        }
      case None => List.empty
    }
  }

  /**
   * 获取ZSet成员分数
   */
  def zscore(key: String, member: String): Option[Double] = {
    getConnection match {
      case Some(jedis) =>
        try {
          Option(jedis.zscore(key, member)).map(_.doubleValue())
        } catch {
          case e: Exception =>
            println(s"Redis ZSCORE失败: key=$key, member=$member, error=${e.getMessage}")
            None
        } finally {
          closeConnection(jedis)
        }
      case None => None
    }
  }

  // ============ 列表操作 ============

  /**
   * 添加到列表头部
   */
  def lpush(key: String, values: String*): Long = {
    getConnection match {
      case Some(jedis) =>
        try {
          jedis.lpush(key, values: _*)
        } catch {
          case e: Exception =>
            println(s"Redis LPUSH失败: key=$key, error=${e.getMessage}")
            0L
        } finally {
          closeConnection(jedis)
        }
      case None => 0L
    }
  }

  /**
   * 获取列表范围
   */
  def lrange(key: String, start: Long, end: Long): List[String] = {
    getConnection match {
      case Some(jedis) =>
        try {
          jedis.lrange(key, start, end).asScala.toList
        } catch {
          case e: Exception =>
            println(s"Redis LRANGE失败: key=$key, error=${e.getMessage}")
            List.empty
        } finally {
          closeConnection(jedis)
        }
      case None => List.empty
    }
  }

  // ============ 通用操作 ============

  /**
   * 设置过期时间
   */
  def expire(key: String, seconds: Long): Boolean = {
    getConnection match {
      case Some(jedis) =>
        try {
          jedis.expire(key, seconds) > 0
        } catch {
          case e: Exception =>
            println(s"Redis EXPIRE失败: key=$key, error=${e.getMessage}")
            false
        } finally {
          closeConnection(jedis)
        }
      case None => false
    }
  }

  /**
   * 删除Key
   */
  def del(key: String): Long = {
    getConnection match {
      case Some(jedis) =>
        try {
          jedis.del(key)
        } catch {
          case e: Exception =>
            println(s"Redis DEL失败: key=$key, error=${e.getMessage}")
            0L
        } finally {
          closeConnection(jedis)
        }
      case None => 0L
    }
  }

  /**
   * 检查Key是否存在
   */
  def exists(key: String): Boolean = {
    getConnection match {
      case Some(jedis) =>
        try {
          jedis.exists(key)
        } catch {
          case e: Exception =>
            println(s"Redis EXISTS失败: key=$key, error=${e.getMessage}")
            false
        } finally {
          closeConnection(jedis)
        }
      case None => false
    }
  }

  /**
   * 检查Redis连接
   */
  def checkRedisConnection(config: RedisConfig): Boolean = {
    Try {
      initPool(config)
      getConnection match {
        case Some(jedis) =>
          try {
            val result = jedis.ping()
            println(s"Redis连接测试成功: $result")
            true
          } finally {
            closeConnection(jedis)
          }
        case None => false
      }
    } match {
      case Success(result) => result
      case Failure(e) =>
        println(s"Redis连接测试失败: ${e.getMessage}")
        false
    }
  }
}
