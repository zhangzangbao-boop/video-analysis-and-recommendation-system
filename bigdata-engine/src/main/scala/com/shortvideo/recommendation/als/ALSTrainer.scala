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

    // 1. 初始化 Spark (优化内存配置)
    val spark = SparkSession.builder()
      .appName("ShortVideoALSRecommendation")
      // 生产环境通常由 spark-submit 指定 master，这里保留 local[*] 方便调试
      .master("local[*]")
      // ========== 内存优化配置 ==========
      // 增加driver内存（ALS训练需要大量内存）
      .config("spark.driver.memory", "4g")
      .config("spark.driver.maxResultSize", "2g")
      // 增加executor内存（local模式下executor和driver共享内存）
      .config("spark.executor.memory", "4g")
      // 减少shuffle分区数以降低内存压力（根据数据量动态调整）
      .config("spark.sql.shuffle.partitions", "20")
      .config("spark.default.parallelism", "20")
      // 序列化配置（Kryo更节省内存）
      .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .config("spark.kryoserializer.buffer.max", "512m")
      // 内存管理配置
      .config("spark.memory.fraction", "0.8")
      .config("spark.memory.storageFraction", "0.3")
      // 垃圾回收优化
      .config("spark.executor.extraJavaOptions", "-XX:+UseG1GC -XX:MaxGCPauseMillis=200")
      .config("spark.driver.extraJavaOptions", "-XX:+UseG1GC -XX:MaxGCPauseMillis=200")
      // ========== HDFS 配置 ==========
      .config("spark.hadoop.fs.defaultFS", "hdfs://localhost:9000")
      .config("spark.hadoop.dfs.client.use.datanode.hostname", "false")
      .config("spark.hadoop.dfs.replication", "1")
      // ========== 其他配置 ==========
      .config("spark.sql.legacy.timeParserPolicy", "LEGACY")
      .config("spark.sql.adaptive.enabled", "true")
      .config("spark.sql.adaptive.coalescePartitions.enabled", "true")
      .getOrCreate()
    
    // 打印内存配置信息
    val driverMemory = spark.conf.get("spark.driver.memory", "未设置")
    val executorMemory = spark.conf.get("spark.executor.memory", "未设置")
    println(s"[INFO] Spark内存配置:")
    println(s"  - Driver内存: $driverMemory")
    println(s"  - Executor内存: $executorMemory")
    println(s"  - Shuffle分区数: ${spark.conf.get("spark.sql.shuffle.partitions")}")
    
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
      // 尝试获取今天的日期，如果失败则使用通配符读取所有历史数据（按日期分区）
      val todayPath = s"hdfs://localhost:9000/short-video/behavior/logs/$today/*.json"
      val yesterday = LocalDate.now().minusDays(1).toString
      val yesterdayPath = s"hdfs://localhost:9000/short-video/behavior/logs/$yesterday/*.json"
      
      // 检查今天的路径是否存在
      val hadoopConf = spark.sparkContext.hadoopConfiguration
      val fs = org.apache.hadoop.fs.FileSystem.get(hadoopConf)
      
      // 如果今天的路径存在且有数据，则使用今天的路径，否则使用通配符路径
      if (fs.exists(new Path(s"hdfs://localhost:9000/short-video/behavior/logs/$today"))) {
        println(s"[INFO] 使用今天的日期路径: $todayPath")
        todayPath
      } else if (fs.exists(new Path(s"hdfs://localhost:9000/short-video/behavior/logs/$yesterday"))) {
        println(s"[INFO] 使用昨天的日期路径: $yesterdayPath")
        yesterdayPath
      } else {
        // 如果今天的路径不存在，则使用通配符读取所有历史数据
        println(s"[INFO] 今天的路径不存在，使用通配符路径读取所有历史数据")
        "hdfs://localhost:9000/short-video/behavior/logs/*/*.json"
      }
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
        
        // 尝试查找存在的日期目录
        try {
          val hadoopConf = spark.sparkContext.hadoopConfiguration
          val fs = org.apache.hadoop.fs.FileSystem.get(hadoopConf)
          val basePath = new Path("hdfs://localhost:9000/short-video/behavior/logs/")
          
          if (fs.exists(basePath)) {
            val statuses = fs.listStatus(basePath)
            val dateDirs = statuses.filter(_.isDirectory()).map(_.getPath.getName)
              .filter(name => """^\d{4}-\d{2}-\d{2}$""".r.findFirstIn(name).isDefined)
              .sorted(Ordering.String.reverse)
              
            if (dateDirs.nonEmpty) {
              println("[INFO] 在HDFS中找到以下日期目录，可以尝试使用其中之一:")
              dateDirs.take(5).foreach(dir => {
                val datePath = s"hdfs://localhost:9000/short-video/behavior/logs/$dir/*.json"
                println(s"  - $datePath")
              })
            } else {
              println("[INFO] HDFS中没有找到按日期命名的目录")
            }
          } else {
            println("[INFO] HDFS根目录不存在，请先创建目录并上传数据")
          }
        } catch {
          case e: Exception => 
            println(s"[INFO] 尝试查找可用日期目录时出错: ${e.getMessage}")
        }
        
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
      println("[INFO] 开始聚合评分...")
      val aggregatedRatings = DataProcessor.aggregateRatings(rawRatings)
      println(s"[INFO] 聚合后评分记录数: ${aggregatedRatings.count()}")

      // Step 6: 数据质量过滤 (用户行为>=5, 视频互动>=5)
      println("[INFO] 开始数据质量过滤...")
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
      println("[INFO] 开始划分训练集和测试集...")
      val Array(training, test) = trainingDataFull.randomSplit(Array(0.8, 0.2), seed = 1234L)
      
      // 注意：不缓存数据集以节省内存，直接使用
      println("[INFO] 数据集划分完成，开始训练...")
      
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

      // Step 11: 保存模型到 HDFS (添加错误处理和重试)
      println(s"[INFO] 保存模型到: $modelPath")
      var modelSaved = false
      try {
        // 确保模型目录的父目录存在
        val hadoopConf = spark.sparkContext.hadoopConfiguration
        val fs = FileSystem.get(hadoopConf)
        val modelParentPath = new Path(modelPath).getParent
        if (!fs.exists(modelParentPath)) {
          fs.mkdirs(modelParentPath)
          println(s"[INFO] 创建模型目录: $modelParentPath")
        }
        
        model.write.overwrite().save(modelPath)
        modelSaved = true
        println(s"[SUCCESS] 模型已保存到: $modelPath")
      } catch {
        case e: Exception =>
          println(s"[ERROR] 保存模型到HDFS失败: ${e.getMessage}")
          e.printStackTrace()
          println(s"[WARN] 模型未保存，但将继续尝试保存到MySQL")
      }

      // Step 12 & 13: 写入 MySQL (推荐结果 + 模型参数)
      // 写入推荐结果 type=OFFLINE
      println("[INFO] 开始保存模型参数和推荐结果到 MySQL...")
      var modelId: String = ""
      try {
        modelId = MySQLWriter.writeModelParamsToMySQL(modelPath, rank, regParam, maxIter, rmse)
        println(s"[SUCCESS] 模型参数已保存到MySQL，模型ID: $modelId")
      } catch {
        case e: Exception =>
          println(s"[ERROR] 保存模型参数到MySQL失败: ${e.getMessage}")
          e.printStackTrace()
          // 如果模型已保存，至少记录模型路径
          if (modelSaved) {
            println(s"[WARN] 模型已保存到HDFS: $modelPath，但MySQL保存失败")
          }
      }
      
      try {
        MySQLWriter.writeRecommendationsToMySQL(userRecs, "OFFLINE", modelId)
        println(s"[SUCCESS] 推荐结果已保存到MySQL")
      } catch {
        case e: Exception =>
          println(s"[ERROR] 保存推荐结果到MySQL失败: ${e.getMessage}")
          e.printStackTrace()
          println(s"[WARN] 推荐结果未保存到MySQL，但模型训练已完成")
      }

      // 输出最终统计信息
      println("=" * 80)
      if (modelSaved && modelId.nonEmpty) {
        println("[SUCCESS] 离线推荐流程执行完毕！")
      } else if (modelSaved) {
        println("[PARTIAL SUCCESS] 模型已保存，但MySQL保存部分失败")
      } else {
        println("[WARNING] 训练完成，但保存过程出现问题")
      }
      println("=" * 80)
      println("[INFO] 最终统计:")
      println(s"  - 模型保存状态: ${if (modelSaved) "成功" else "失败"}")
      println(s"  - 模型路径: $modelPath")
      println(s"  - 模型ID: ${if (modelId.nonEmpty) modelId else "未保存"}")
      println(s"  - 模型RMSE: ${if (rmse >= 0) rmse.formatted("%.4f") else "N/A"}")
      println(s"  - 推荐用户数: $recUserCount")
      println(s"  - 推荐总数: ${recUserCount * topN}")
      println("=" * 80)
      
      // 如果模型保存失败，提供手动保存建议
      if (!modelSaved) {
        println("[TIP] 如果模型保存失败，可以:")
        println("  1. 检查HDFS服务是否正常运行")
        println("  2. 检查HDFS磁盘空间是否充足")
        println("  3. 检查是否有写入权限")
        println("  4. 尝试手动保存模型到本地:")
        val localModelPath = s"file:///tmp/als-model-$today"
        println("     model.write.overwrite().save(\"" + localModelPath + "\")")
      }

    } catch {
      case e: OutOfMemoryError =>
        println("=" * 80)
        println("[ERROR] ========== 内存不足错误 ==========")
        println("[ERROR] Java堆内存不足，程序无法继续执行")
        println("=" * 80)
        println("[SOLUTION] 解决方案:")
        println("  1. 增加JVM堆内存:")
        println("     - 如果使用sbt运行: 设置环境变量 JAVA_OPTS=\"-Xmx6g -Xms4g\"")
        println("     - 如果使用java运行: java -Xmx6g -Xms4g -cp ...")
        println("  2. 减少数据量:")
        println("     - 使用更具体的日期路径，而不是通配符")
        println("     - 例如: hdfs://localhost:9000/short-video/behavior/logs/2026-01-22/*.json")
        println("  3. 降低ALS参数:")
        println("     - 减少rank参数（当前: $rank，可尝试: 10）")
        println("     - 减少maxIter参数（当前: $maxIter，可尝试: 5）")
        println("  4. 减少shuffle分区数（已在代码中优化）")
        println("  5. 关闭其他占用内存的程序")
        println("=" * 80)
        e.printStackTrace()
        System.exit(1)
      case e: Exception =>
        println("=" * 80)
        println(s"[ERROR] 任务失败: ${e.getMessage}")
        println("=" * 80)
        e.printStackTrace()
        println("[INFO] 检查错误日志以获取更多信息")
    } finally {
      try {
        spark.stop()
        println("[INFO] Spark会话已关闭")
      } catch {
        case e: Exception =>
          println(s"[WARN] 关闭Spark会话时出错: ${e.getMessage}")
      }
    }
  }
}