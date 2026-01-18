package com.shortvideo.recommendation.common.utils

import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.{Calendar, Date, TimeZone}
import com.shortvideo.recommendation.common.Constants

/**
 * 时间工具类
 */
object TimeUtil {

  /**
   * 获取当前时间戳
   */
  def currentTimestamp: Timestamp = new Timestamp(System.currentTimeMillis())

  /**
   * 获取当前时间字符串
   */
  def currentTimeString(format: String = Constants.TimeFormat.YYYY_MM_DD_HH_MM_SS): String = {
    val sdf = new SimpleDateFormat(format)
    sdf.format(new Date())
  }

  /**
   * 时间戳转字符串
   */
  def timestampToString(timestamp: Timestamp,
                        format: String = Constants.TimeFormat.YYYY_MM_DD_HH_MM_SS): String = {
    val sdf = new SimpleDateFormat(format)
    sdf.format(timestamp)
  }

  /**
   * 字符串转时间戳
   */
  def stringToTimestamp(timeStr: String,
                        format: String = Constants.TimeFormat.YYYY_MM_DD_HH_MM_SS): Timestamp = {
    val sdf = new SimpleDateFormat(format)
    val date = sdf.parse(timeStr)
    new Timestamp(date.getTime)
  }

  /**
   * 获取当天日期（yyyyMMdd格式）
   */
  def getTodayDate: String = {
    val sdf = new SimpleDateFormat(Constants.TimeFormat.YYYYMMDD)
    sdf.format(new Date())
  }

  /**
   * 获取昨天日期（yyyyMMdd格式）
   */
  def getYesterdayDate: String = {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, -1)
    val sdf = new SimpleDateFormat(Constants.TimeFormat.YYYYMMDD)
    sdf.format(calendar.getTime)
  }

  /**
   * 获取前天日期（yyyyMMdd格式）
   */
  def getDayBeforeYesterday: String = {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, -2)
    val sdf = new SimpleDateFormat(Constants.TimeFormat.YYYYMMDD)
    sdf.format(calendar.getTime)
  }

  /**
   * 获取当前小时（yyyyMMddHH格式）
   */
  def getCurrentHour: String = {
    val sdf = new SimpleDateFormat(Constants.TimeFormat.YYYYMMDDHH)
    sdf.format(new Date())
  }

  /**
   * 获取指定时间的小时
   */
  def getHourFromTimestamp(timestamp: Timestamp): String = {
    val sdf = new SimpleDateFormat(Constants.TimeFormat.YYYYMMDDHH)
    sdf.format(timestamp)
  }

  /**
   * 计算时间差（秒）
   */
  def getTimeDiffInSeconds(startTime: Timestamp, endTime: Timestamp): Long = {
    (endTime.getTime - startTime.getTime) / 1000
  }

  /**
   * 计算时间差（分钟）
   */
  def getTimeDiffInMinutes(startTime: Timestamp, endTime: Timestamp): Long = {
    (endTime.getTime - startTime.getTime) / (1000 * 60)
  }

  /**
   * 计算时间差（小时）
   */
  def getTimeDiffInHours(startTime: Timestamp, endTime: Timestamp): Long = {
    (endTime.getTime - startTime.getTime) / (1000 * 60 * 60)
  }

  /**
   * 添加天数
   */
  def addDays(timestamp: Timestamp, days: Int): Timestamp = {
    val calendar = Calendar.getInstance()
    calendar.setTime(timestamp)
    calendar.add(Calendar.DAY_OF_YEAR, days)
    new Timestamp(calendar.getTimeInMillis)
  }

  /**
   * 添加小时
   */
  def addHours(timestamp: Timestamp, hours: Int): Timestamp = {
    val calendar = Calendar.getInstance()
    calendar.setTime(timestamp)
    calendar.add(Calendar.HOUR_OF_DAY, hours)
    new Timestamp(calendar.getTimeInMillis)
  }

  /**
   * 获取一天的开始时间（00:00:00）
   */
  def getDayStart(timestamp: Timestamp): Timestamp = {
    val calendar = Calendar.getInstance()
    calendar.setTime(timestamp)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    new Timestamp(calendar.getTimeInMillis)
  }

  /**
   * 获取一天的结束时间（23:59:59）
   */
  def getDayEnd(timestamp: Timestamp): Timestamp = {
    val calendar = Calendar.getInstance()
    calendar.setTime(timestamp)
    calendar.set(Calendar.HOUR_OF_DAY, 23)
    calendar.set(Calendar.MINUTE, 59)
    calendar.set(Calendar.SECOND, 59)
    calendar.set(Calendar.MILLISECOND, 999)
    new Timestamp(calendar.getTimeInMillis)
  }

  /**
   * 格式化Unix时间戳
   */
  def formatUnixTimestamp(unixTimestamp: Long,
                          format: String = Constants.TimeFormat.YYYY_MM_DD_HH_MM_SS): String = {
    val sdf = new SimpleDateFormat(format)
    sdf.format(new Date(unixTimestamp * 1000))
  }

  /**
   * 获取UTC时间
   */
  def getUTCTime: Timestamp = {
    val sdf = new SimpleDateFormat(Constants.TimeFormat.ISO_8601)
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"))
    val dateStr = sdf.format(new Date())
    stringToTimestamp(dateStr, Constants.TimeFormat.ISO_8601)
  }

  /**
   * 判断是否是同一天
   */
  def isSameDay(timestamp1: Timestamp, timestamp2: Timestamp): Boolean = {
    val cal1 = Calendar.getInstance()
    val cal2 = Calendar.getInstance()
    cal1.setTime(timestamp1)
    cal2.setTime(timestamp2)

    cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
      cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
  }
}