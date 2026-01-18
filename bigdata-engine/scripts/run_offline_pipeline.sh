#!/bin/bash

# ========================================================
# 短视频推荐系统 - 离线分析全链路调度脚本
# 顺序：ODS -> DWD -> DWS -> 离线统计 & ALS模型训练
# ========================================================

# 1. 基础配置
# 获取脚本所在目录的上一级目录作为项目根目录 (即 bigdata-engine)
BASE_DIR=$(cd "$(dirname "$0")/.." && pwd)
# JAR包路径 (请确保 maven package 已执行)
JAR_PATH="$BASE_DIR/target/spark-example-1.0-jar-with-dependencies.jar"
# 日志目录
LOG_DIR="$BASE_DIR/logs/pipeline"
DATE=$(date +%Y-%m-%d)
# 当次运行的日志文件
LOG_FILE="$LOG_DIR/offline_job_$DATE.log"

# 创建日志目录
mkdir -p "$LOG_DIR"

echo "========================================================" | tee -a "$LOG_FILE"
echo "   开始执行离线分析数据管道" | tee -a "$LOG_FILE"
echo "   执行日期: $DATE" | tee -a "$LOG_FILE"
echo "   开始时间: $(date '+%Y-%m-%d %H:%M:%S')" | tee -a "$LOG_FILE"
echo "========================================================" | tee -a "$LOG_FILE"

# 2. 检查 JAR 包是否存在
if [ ! -f "$JAR_PATH" ]; then
    echo "[ERROR] 未找到 JAR 包: $JAR_PATH" | tee -a "$LOG_FILE"
    echo "请先在 bigdata-engine 目录下运行: mvn clean package -DskipTests" | tee -a "$LOG_FILE"
    exit 1
fi

# 3. 定义任务执行函数
run_spark_job() {
    CLASS_NAME=$1
    JOB_DESC=$2

    echo "" | tee -a "$LOG_FILE"
    echo "--------------------------------------------------------" | tee -a "$LOG_FILE"
    echo "[STEP] 正在启动: $JOB_DESC" | tee -a "$LOG_FILE"
    echo "       类名: $CLASS_NAME" | tee -a "$LOG_FILE"
    echo "       时间: $(date '+%Y-%m-%d %H:%M:%S')" | tee -a "$LOG_FILE"

    # 提交 Spark 任务
    # 注意：生产环境请修改 --master yarn 以及内存配置
    spark-submit \
        --class "$CLASS_NAME" \
        --master local[*] \
        --driver-memory 2G \
        --executor-memory 2G \
        "$JAR_PATH" >> "$LOG_FILE" 2>&1

    RET_CODE=$?

    if [ $RET_CODE -eq 0 ]; then
        echo "[SUCCESS] $JOB_DESC 执行成功." | tee -a "$LOG_FILE"
    else
        echo "[ERROR] $JOB_DESC 执行失败! 错误码: $RET_CODE" | tee -a "$LOG_FILE"
        echo "        请查看日志详情: $LOG_FILE" | tee -a "$LOG_FILE"
        # 遇到错误立即退出，阻断后续任务
        exit $RET_CODE
    fi
}

# ========================================================
# 4. 按顺序执行任务链
# ========================================================

# 第一步：ODS 层 - 原始日志接入 (挂载分区)
run_spark_job "com.shortvideo.recommendation.offline.ods.OdsLogToHdfs" "1. ODS 层 (日志接入)"

# 第二步：DWD 层 - 日志清洗 (JSON -> Parquet)
run_spark_job "com.shortvideo.recommendation.offline.dwd.DwdLogClean" "2. DWD 层 (数据清洗)"

# 第三步：DWS 层 - 特征聚合 (计算评分宽表)
run_spark_job "com.shortvideo.recommendation.offline.dws.DwsUserVideoFeature" "3. DWS 层 (特征聚合)"

# 第四步：业务统计 - 热门视频与看板 (依赖 DWD)
run_spark_job "com.shortvideo.recommendation.offline.job.OfflineJob" "4. 离线统计 (热门榜单 & DAU)"

# 第五步：算法训练 - ALS 模型 (依赖 DWS)
run_spark_job "com.shortvideo.recommendation.als.ALSTrainer" "5. ALS 模型训练 (生成推荐)"



# ========================================================
# 5. 结束
# ========================================================
echo "" | tee -a "$LOG_FILE"
echo "========================================================" | tee -a "$LOG_FILE"
echo "   离线管道执行完毕!" | tee -a "$LOG_FILE"
echo "   结束时间: $(date '+%Y-%m-%d %H:%M:%S')" | tee -a "$LOG_FILE"
echo "========================================================" | tee -a "$LOG_FILE"