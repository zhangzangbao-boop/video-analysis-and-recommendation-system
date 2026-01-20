package com.shortvideo.recommendation.als

import com.shortvideo.recommendation.als.storage.{HDFSStorage, MySQLWriter}
import org.apache.spark.ml.recommendation.ALS
import org.apache.spark.sql.SparkSession
import java.time.LocalDate
import org.apache.spark.sql.functions.{avg, min, max}
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.hadoop.conf.Configuration
import java.net.URI

/**
 * Spark ALS 离线推荐训练主程序
 * 修正说明：严格按照《技术交接文档》2.4 节流程实现
 * 流程：读取HDFS日志 -> 解析 -> 聚合 -> 过滤 -> 训练 -> 评估 -> 存储
 */
object ALSTrainer {

  /**
   * 验证HDFS路径是否存在并可读
   * 修复：使用 URI 来正确获取 HDFS 文件系统，而不是默认的本地文件系统
   */
  private def validateHDFSDirectory(spark: SparkSession, path: String): Boolean = {
    try {
      val hadoopConf = spark.sparkContext.hadoopConfiguration
      
      // 从路径中提取 HDFS URI (例如: hdfs://localhost:9000)
      val hdfsUri = if (path.startsWith("hdfs://")) {
        val uri = new URI(path)
        new URI(uri.getScheme, uri.getAuthority, null, null, null)
      } else {
        // 如果不是 hdfs:// 开头，尝试使用默认配置
        val fsDefaultName = hadoopConf.get("fs.defaultFS", "file:///")
        new URI(fsDefaultName)
      }
      
      println(s"[DEBUG] 使用 HDFS URI: $hdfsUri")
      
      // 使用 URI 获取正确的文件系统（HDFS 而不是本地文件系统）
      val fs = FileSystem.get(hdfsUri, hadoopConf)
      
      // 如果路径包含通配符，我们只需检查根目录是否存在
      if (path.contains("*")) {
        var basePath = path.substring(0, path.indexOf("*"))
        // 确保路径以"/"结尾
        if (!basePath.endsWith("/")) {
          val lastSlash = basePath.lastIndexOf("/")
          basePath = basePath.substring(0, lastSlash + 1)
        }
        
        val hdfsPath = new Path(basePath)
        
        if (!fs.exists(hdfsPath)) {
          println(s"[ERROR] HDFS基础路径不存在: $hdfsPath")
          println(s"[SUGGESTION] 请确认HDFS服务已启动并创建相应目录")
          fs.close()
          return false
        }
        
        println(s"[INFO] 通配符路径的基础目录存在: $basePath")
        println(s"[INFO] 将尝试读取: $path")
        fs.close()
        return true
      } else {
        // 对于非通配符路径，检查具体路径
        val hdfsPath = new Path(path.replaceAll("/[^/]*\\.json$", ""))
        
        if (!fs.exists(hdfsPath)) {
          println(s"[ERROR] HDFS路径不存在: $hdfsPath")
          fs.close()
          return false
        }

        // 检查是否有可读的JSON文件
        val jsonPattern = ".*\\.json$".r
        val statuses = fs.listStatus(hdfsPath)
        val jsonFiles = statuses.filter(status => 
          jsonPattern.findFirstIn(status.getPath.getName).isDefined ||
          status.getPath.getName.toLowerCase.endsWith(".json"))

        if (jsonFiles.isEmpty) {
          println(s"[ERROR] 在路径 $hdfsPath 下未找到JSON文件")
          val allFiles = statuses.map(_.getPath.getName).mkString(", ")
          if (allFiles.nonEmpty) {
            println(s"[INFO] 该目录下的文件有: $allFiles")
          }
          fs.close()
          return false
        }

        println(s"[INFO] 找到 ${jsonFiles.length} 个JSON文件在路径: $hdfsPath")
        fs.close()
        return true
      }
    } catch {
      case e: Exception =>
        println(s"[ERROR] 检查HDFS路径时出错: ${e.getMessage}")
        // 打印堆栈跟踪以帮助调试
        e.printStackTrace()
        println("[SUGGESTION] 请检查HDFS服务是否正常运行，以及网络连接是否正常")
        println("[SUGGESTION] 如果HDFS未启动，可以设置环境变量 SKIP_HDFS_VALIDATION=true 跳过验证")
        false
    }
  }

  def main(args: Array[String]): Unit = {
    println("=" * 80)
    println("Spark ALS 离线推荐训练 (文档标准版)")
    println("=" * 80)

    // 0. 先确定输入路径（决定使用 HDFS 还是本地文件系统）
    // 支持两种路径格式：
    // 1. HDFS：hdfs://localhost:9000/short-video/behavior/logs/*/*.json
    // 2. 本地：file:///.../bigdata-engine/logs/*.json
    val today = LocalDate.now().toString
    val inputPath = if (args.length > 0 && args(0).nonEmpty) {
      args(0)
    } else {
      "hdfs://localhost:9000/short-video/behavior/logs/*/*.json"
    }
    val useHdfs = inputPath.startsWith("hdfs://")

    // 1. 初始化 Spark
    val sparkBuilder = SparkSession.builder()
      .appName("ShortVideoALSRecommendation")
      // 生产环境通常由 spark-submit 指定 master，这里保留 local[*] 方便调试
      .master("local[*]")
      .config("spark.sql.shuffle.partitions", "100")

    // 仅当 inputPath 是 hdfs:// 时，才强制设置 HDFS 默认文件系统
    if (useHdfs) {
      sparkBuilder
        // 配置 HDFS 相关设置，确保能正确连接到 HDFS
        .config("spark.hadoop.fs.defaultFS", "hdfs://localhost:9000")
        .config("spark.hadoop.dfs.client.use.datanode.hostname", "false")
        .config("spark.hadoop.dfs.replication", "1")
    } else {
      println(s"[INFO] 检测到使用本地文件系统输入: $inputPath")
      println("[INFO] 本次运行不会强制设置 spark.hadoop.fs.defaultFS 为 HDFS")
    }

    val spark = sparkBuilder.getOrCreate()
    
    // 设置 Hadoop 配置，确保使用 HDFS 而不是本地文件系统
    val hadoopConf = spark.sparkContext.hadoopConfiguration
    if (useHdfs && !hadoopConf.get("fs.defaultFS", "").startsWith("hdfs://")) {
      hadoopConf.set("fs.defaultFS", "hdfs://localhost:9000")
      println("[INFO] 已设置 HDFS 默认文件系统: hdfs://localhost:9000")
    }

    import spark.implicits._

    // 2. 配置参数 (严格对应文档 2.4)
    
    // ALS 超参数 (文档指定) - 必须先定义，后面会使用
    val rank = 20          // 文档要求: 20
    val maxIter = 10       // 文档要求: 10
    val regParam = 0.1     // 文档要求: 0.1
    val topN = 20          // 文档要求: 20
    
    val modelPath =
      if (useHdfs) s"hdfs://localhost:9000/short-video/als-model/model-$today"
      else s"file:///tmp/shortvideo/als-model/model-$today"

    println(s"[INFO] 配置参数:")
    println(s"  - HDFS 输入路径: $inputPath")
    println(s"  - 模型保存路径: $modelPath")
    println(s"  - ALS 参数: rank=$rank, maxIter=$maxIter, regParam=$regParam, topN=$topN")

    try {
      // 验证HDFS路径，但允许跳过验证（通过环境变量或配置）
      val skipValidation = sys.env.getOrElse("SKIP_HDFS_VALIDATION", "false").toLowerCase == "true"
      if (useHdfs && !skipValidation && !validateHDFSDirectory(spark, inputPath)) {
        println(s"[ERROR] HDFS路径验证失败，请确保HDFS服务运行且路径正确！")
        println(s"[INFO] 如需跳过验证，设置环境变量 SKIP_HDFS_VALIDATION=true")
        return
      }

      // ============================================
      // 3. 数据处理流程 (对应流程图 Step 2-6)
      // ============================================

      // Step 2 & 3 & 4: 读取 HDFS -> 解析 -> 转换评分
      val rawRatings = DataProcessor.readAndParseBehaviorLogs(spark, inputPath)

      if (rawRatings.isEmpty) {
        // 添加更详细的诊断信息
        println(s"[ERROR] 在路径 $inputPath 下未找到任何数据")
        
        // 检查本地是否有数据文件
        val localLogDir = new java.io.File("bigdata-engine/logs")
        val localFiles = if (localLogDir.exists()) {
          localLogDir.listFiles((_, name) => name.endsWith(".json"))
        } else {
          Array.empty[java.io.File]
        }
        
        if (localFiles.nonEmpty) {
          println(s"[INFO] 检测到本地有 ${localFiles.length} 个JSON文件在 bigdata-engine/logs 目录")
          println("[SOLUTION] 请执行以下步骤:")
          println("  1. 运行脚本上传本地文件到HDFS:")
          println("     bigdata-engine\\scripts\\upload_local_logs_to_hdfs.bat")
          println("  2. 或者使用本地文件路径运行（用于测试）:")
          println("     spark-submit --class com.shortvideo.recommendation.als.ALSTrainer \\")
          println("       target\\bigdata-engine-1.0-jar-with-dependencies.jar \\")
          println("       file:///D:/study/clone/video-analysis-and-recommendation-system/bigdata-engine/logs/*.json")
        } else {
          println("[SOLUTION] 请检查以下几点:")
          println("  1. HDFS服务是否已启动 (start-dfs.sh 或 start-dfs.cmd)")
          println("  2. 目标路径是否存在")
          println("  3. JSON文件是否存在于指定路径")
          println("  4. 文件格式是否正确 (包含userId, videoId/mid, behaviorType字段)")
          println("  5. 使用命令 'hdfs dfs -ls /short-video/behavior/logs/' 检查文件")
          println("  6. 如果使用默认通配符路径，尝试提供具体日期路径，例如:")
          println("     hdfs://localhost:9000/short-video/behavior/logs/2025-01-20/*.json")
          println("  7. 或者先运行数据生成器生成数据:")
          println("     spark-submit --class com.shortvideo.recommendation.datagenerator.DataGeneratorApp \\")
          println("       target\\bigdata-engine-1.0-jar-with-dependencies.jar")
        }
        
        throw new RuntimeException(s"路径 $inputPath 下未找到有效行为数据！")
      }

      // Step 5: 聚合评分 (同一用户对同一视频取最高分)
      val aggregatedRatings = DataProcessor.aggregateRatings(rawRatings)

      // Step 6: 数据质量过滤 (用户行为>=5, 视频互动>=5)
      val trainingDataFull = DataProcessor.filterByQuality(aggregatedRatings, 5, 5)

      val count = trainingDataFull.count()
      if (count == 0) {
        throw new RuntimeException("经过去重和过滤后，训练数据集为空！")
      }
      
      // 输出详细统计信息
      val finalUserCount = trainingDataFull.select("userId").distinct().count()
      val finalMovieCount = trainingDataFull.select("movieId").distinct().count()
      val avgRating = trainingDataFull.select(avg("rating")).first().getDouble(0)
      val minRating = trainingDataFull.select(min("rating")).first().getFloat(0)
      val maxRating = trainingDataFull.select(max("rating")).first().getFloat(0)
      
      println(s"[INFO] ========== 训练数据统计 ==========")
      println(s"  - 用户数: $finalUserCount")
      println(s"  - 视频数: $finalMovieCount")
      println(s"  - 评分记录数: $count")
      println(s"  - 平均评分: ${avgRating.formatted("%.2f")}")
      println(s"  - 评分范围: [$minRating, $maxRating]")
      println(s"  - 数据密度: ${(count.toDouble / (finalUserCount * finalMovieCount) * 100).formatted("%.4f")}%")
      println(s"==========================================")

      // ============================================
      // 4. 划分数据集与训练 (对应流程图 Step 7-8)
      // ============================================
      // Step 7: 划分训练集和测试集 (80% 训练, 20% 测试)
      val Array(training, test) = trainingDataFull.randomSplit(Array(0.8, 0.2), seed = 1234L)
      
      val trainingCount = training.count()
      val testCount = test.count()
      println(s"[INFO] 数据集划分:")
      println(s"  - 训练集: $trainingCount 条 (${(trainingCount.toDouble / count * 100).formatted("%.1f")}%)")
      println(s"  - 测试集: $testCount 条 (${(testCount.toDouble / count * 100).formatted("%.1f")}%)")

      println(s"[INFO] 开始训练 ALS 模型 (rank=$rank, maxIter=$maxIter, regParam=$regParam)...")
      val als = new ALS()
        .setMaxIter(maxIter)
        .setRegParam(regParam)
        .setRank(rank)
        .setUserCol("userId")
        .setItemCol("movieId")
        .setRatingCol("rating")
        .setColdStartStrategy("drop")

      val model = als.fit(training)
      println("[INFO] 模型训练完成")

      // ============================================
      // 5. 评估与保存 (对应流程图 Step 9-13)
      // ============================================
      // Step 9: 模型评估
      val predictions = model.transform(test)
      val rmse = ModelEvaluator.evaluateRMSE(predictions)
      println(s"[INFO] 模型 RMSE: $rmse")

      // Step 10: 生成推荐列表 (Top-20)
      println(s"[INFO] 为所有用户生成 Top-$topN 推荐列表...")
      val userRecs = model.recommendForAllUsers(topN)
      
      val recUserCount = userRecs.count()
      println(s"[INFO] 推荐结果统计:")
      println(s"  - 获得推荐的用户数: $recUserCount")
      println(s"  - 每个用户推荐数: $topN")

      // Step 11: 保存模型到 HDFS
      println(s"[INFO] 保存模型到: $modelPath")
      model.write.overwrite().save(modelPath)

      // Step 12 & 13: 写入 MySQL (推荐结果 + 模型参数)
      // 写入推荐结果 type=OFFLINE
      println("[INFO] 开始保存模型参数和推荐结果到 MySQL...")
      val modelId = MySQLWriter.writeModelParamsToMySQL(modelPath, rank, regParam, maxIter, rmse)
      MySQLWriter.writeRecommendationsToMySQL(userRecs, "OFFLINE", modelId)

      // 输出最终统计信息
      println("=" * 80)
      println("[SUCCESS] 离线推荐流程执行完毕！")
      println("=" * 80)
      println("[INFO] 最终统计:")
      println(s"  - 模型ID: $modelId")
      println(s"  - 模型路径: $modelPath")
      println(s"  - 模型RMSE: ${if (rmse >= 0) rmse.formatted("%.4f") else "N/A"}")
      println(s"  - 推荐用户数: $recUserCount")
      println(s"  - 推荐总数: ${recUserCount * topN}")
      println("=" * 80)

    } catch {
      case e: Exception =>
        e.printStackTrace()
        println(s"[ERROR] 任务失败: ${e.getMessage}")
    } finally {
      spark.stop()
    }
  }
}