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
   */
  private def validateHDFSDirectory(spark: SparkSession, path: String): Boolean = {
    try {
      // 使用SparkSession的Hadoop配置，这样可以正确连接到HDFS
      val hadoopConf = spark.sparkContext.hadoopConfiguration
      
      // 如果路径包含通配符，我们只需检查根目录是否存在
      if (path.contains("*")) {
        var basePath = path.substring(0, path.indexOf("*"))
        // 确保路径以"/"结尾
        if (!basePath.endsWith("/")) {
          val lastSlash = basePath.lastIndexOf("/")
          basePath = basePath.substring(0, lastSlash + 1)
        }
        
        val fs = org.apache.hadoop.fs.FileSystem.get(hadoopConf)
        val hdfsPath = new org.apache.hadoop.fs.Path(basePath)
        
        if (!fs.exists(hdfsPath)) {
          println(s"[ERROR] HDFS基础路径不存在: $hdfsPath")
          println(s"[SUGGESTION] 请确认HDFS服务已启动并创建相应目录")
          return false
        }
        
        // 列出基础路径下的内容以提供更多诊断信息
        val statuses = fs.listStatus(hdfsPath)
        println(s"[INFO] 基础路径存在，包含 ${statuses.length} 个项目")
        if (statuses.length > 0) {
          println("[INFO] 路径下的前10个项目:")
          statuses.take(10).foreach(status => {
            val fileType = if (status.isDirectory) "[DIR]" else "[FILE]"
            println(s"  $fileType ${status.getPath.getName}")
          })
        }
        
        println(s"[INFO] 通配符路径的基础目录存在: $basePath")
        println(s"[INFO] 将尝试读取: $path")
        return true
      } else {
        // 对于非通配符路径，检查具体路径
        val fs = org.apache.hadoop.fs.FileSystem.get(hadoopConf)
        val hdfsPath = new org.apache.hadoop.fs.Path(path.replaceAll("/[^/]*\\.json$", ""))
        
        if (!fs.exists(hdfsPath)) {
          println(s"[ERROR] HDFS路径不存在: $hdfsPath")
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
          return false
        }

        println(s"[INFO] 找到 ${jsonFiles.length} 个JSON文件在路径: $hdfsPath")
        return true
      }
    } catch {
      case e: Exception =>
        println(s"[ERROR] 检查HDFS路径时出错: ${e.getMessage}")
        // 打印堆栈跟踪以帮助调试
        e.printStackTrace()
        println("[SUGGESTION] 请检查HDFS服务是否正常运行，以及网络连接是否正常")
        false
    }
  }

  def main(args: Array[String]): Unit = {
    println("=" * 80)
    println("Spark ALS 离线推荐训练 (文档标准版)")
    println("=" * 80)

    // 1. 初始化 Spark
    val spark = SparkSession.builder()
      .appName("ShortVideoALSRecommendation")
      // 生产环境通常由 spark-submit 指定 master，这里保留 local[*] 方便调试
      .master("local[*]")
      .config("spark.sql.shuffle.partitions", "100")
      // 配置 HDFS 相关设置，确保能正确连接到 HDFS
      .config("spark.hadoop.fs.defaultFS", "hdfs://localhost:9000")
      .config("spark.hadoop.dfs.client.use.datanode.hostname", "false")
      .config("spark.hadoop.dfs.replication", "1")
      // 配置时间解析策略，支持多种时间格式（包括带毫秒的格式）
      .config("spark.sql.legacy.timeParserPolicy", "LEGACY")
      .getOrCreate()
    
    // 设置 Hadoop 配置，确保使用 HDFS 而不是本地文件系统
    val hadoopConf = spark.sparkContext.hadoopConfiguration
    if (!hadoopConf.get("fs.defaultFS", "").startsWith("hdfs://")) {
      hadoopConf.set("fs.defaultFS", "hdfs://localhost:9000")
      println("[INFO] 已设置 HDFS 默认文件系统: hdfs://localhost:9000")
    }

    import spark.implicits._

    // 2. 配置参数 (严格对应文档 2.4)
    val today = LocalDate.now().toString
    
    // ALS 超参数 (文档指定) - 必须先定义，后面会使用
    val rank = 20          // 文档要求: 20
    val maxIter = 10       // 文档要求: 10
    val regParam = 0.1     // 文档要求: 0.1
    val topN = 20          // 文档要求: 20
    
    // 文档指定读取路径：/short-video/behavior/logs/%Y-%m-%d/*.json
    // 支持两种路径格式：
    // 1. 按日期分区：hdfs://localhost:9000/short-video/behavior/logs/2025-01-20/*.json
    // 2. 通配符匹配：hdfs://localhost:9000/short-video/behavior/logs/*/*.json (读取所有历史数据)
    val inputPath = if (args.length > 0 && args(0).nonEmpty) {
      args(0) // 允许通过命令行参数指定路径
    } else {
      // 默认使用通配符读取所有历史数据（按日期分区）
      "hdfs://localhost:9000/short-video/behavior/logs/*/*.json"
    }
    val modelPath = s"hdfs://localhost:9000/short-video/als-model/model-$today"

    println(s"[INFO] 配置参数:")
    println(s"  - HDFS 输入路径: $inputPath")
    println(s"  - 模型保存路径: $modelPath")
    println(s"  - ALS 参数: rank=$rank, maxIter=$maxIter, regParam=$regParam, topN=$topN")

    try {
      // 验证HDFS路径，但允许跳过验证（通过环境变量或配置）
      val skipValidation = sys.env.getOrElse("SKIP_HDFS_VALIDATION", "false").toLowerCase == "true"
      if (!skipValidation && !validateHDFSDirectory(spark, inputPath)) {
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
        
        // 提供可能的解决方案提示
        println("[SOLUTION] 请检查以下几点:")
        println("  1. HDFS服务是否已启动 (start-dfs.sh)")
        println("  2. 目标路径是否存在")
        println("  3. JSON文件是否存在于指定路径")
        println("  4. 文件格式是否正确 (包含userId, videoId/mid, behaviorType字段)")
        println("  5. 使用命令 'hdfs dfs -ls /short-video/behavior/logs/' 检查文件")
        println("  6. 如果使用默认通配符路径，尝试提供具体日期路径，例如:")
        println("     hdfs://localhost:9000/short-video/behavior/logs/2025-01-20/*.json")
        println("  7. 检查HDFS是否有足够的权限访问该路径")
        println("  8. 确认Spark和Hadoop版本兼容性")
        println("  9. 根据当前情况，请确保在日期子目录中包含JSON文件，例如:")
        println("     在 /short-video/behavior/logs/2026-01-20/ 目录下放置JSON日志文件")
        println("  10. 上传测试数据文件，如:")
        println("     hdfs dfs -put ./sample.json /short-video/behavior/logs/2026-01-20/")
        
        // 尝试检查具体目录是否存在
        try {
          val hadoopConf = spark.sparkContext.hadoopConfiguration
          val fs = org.apache.hadoop.fs.FileSystem.get(hadoopConf)
          
          // 检查根目录是否存在
          val basePath = new org.apache.hadoop.fs.Path("/short-video/behavior/logs/")
          if (!fs.exists(basePath)) {
            println(s"[ERROR] 根目录不存在: $basePath")
            println("[SUGGESTION] 您可能需要先创建HDFS目录:")
            println("  hdfs dfs -mkdir -p /short-video/behavior/logs/")
          } else {
            println(s"[INFO] 根目录存在: $basePath")
            
            // 列出根目录下的内容
            val statuses = fs.listStatus(basePath)
            if (statuses.length > 0) {
              println("[INFO] 根目录下的子目录/文件:")
              statuses.foreach(status => {
                val fileType = if (status.isDirectory()) "[DIR]" else "[FILE]"
                println(s"  - $fileType ${status.getPath.getName}")
                
                // 如果是目录，进一步检查其中是否有JSON文件
                if (status.isDirectory()) {
                  try {
                    val subStatuses = fs.listStatus(status.getPath())
                    val jsonFiles = subStatuses.filter(subStatus => 
                      subStatus.getPath.getName.toLowerCase.endsWith(".json"))
                    
                    if (jsonFiles.length > 0) {
                      println(s"    └── 该目录包含 ${jsonFiles.length} 个JSON文件")
                    } else {
                      println(s"    └── 该目录下没有JSON文件，这可能是问题所在")
                      println(s"    └── 通配符路径 $inputPath 期望在此类目录下找到 .json 文件")
                      
                      // 提供具体的解决命令
                      println(s"    └── 示例: 将JSON文件放入此目录，如 ${status.getPath.getName}/sample.json")
                    }
                  } catch {
                    case _: Exception => println("    └── 无法访问此子目录")
                  }
                }
              })
            } else {
              println("[INFO] 根目录下没有任何内容")
            }
          }
        } catch {
          case e: Exception => 
            println(s"[ERROR] 尝试访问HDFS时出错: ${e.getMessage}")
            println("[SUGGESTION] 可能是HDFS服务未启动或网络连接问题")
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