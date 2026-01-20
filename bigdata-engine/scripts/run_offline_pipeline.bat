@echo off
setlocal

:: ========================================================
:: 短视频推荐系统 - 离线分析全链路调度脚本 (Windows修正版)
:: 架构：Lambda 架构 (HDFS Raw Logs -> Spark -> MySQL)
:: 步骤：
:: 1. OfflineJob (热门统计 & DAU)
:: 2. ALSTrainer (ALS模型训练 & 推荐生成)
:: ========================================================

:: 1. 基础配置
cd /d %~dp0..
set "BASE_DIR=%cd%"
:: 确保这里的 jar 包名称和你 pom.xml 里的 artifactId/version 对应
set "JAR_PATH=%BASE_DIR%\target\bigdata-engine-1.0-jar-with-dependencies.jar"
set "LOG_DIR=%BASE_DIR%\logs\pipeline"

:: 配置 HDFS 相关环境变量（如果需要）
if "%HADOOP_HOME%"=="" (
    echo [WARN] HADOOP_HOME 未设置，将使用 Spark 内置的 Hadoop 客户端
)

:: 确保日志目录存在
if not exist "%BASE_DIR%\logs" mkdir "%BASE_DIR%\logs"
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

set "CURRENT_DATE=%date:~0,4%%date:~5,2%%date:~8,2%"
set "CURRENT_TIME=%time:~0,2%%time:~3,2%"
set "CURRENT_TIME=%CURRENT_TIME: =0%"
set "LOG_FILE=%LOG_DIR%\offline_job_%CURRENT_DATE%_%CURRENT_TIME%.log"

echo ========================================================
echo    开始执行离线分析数据管道 (Lambda架构版)
echo    日志文件: %LOG_FILE%
echo ========================================================

:: 2. 检查 JAR 包
if not exist "%JAR_PATH%" (
    echo [ERROR] 未找到 JAR 包: %JAR_PATH%
    echo 请先在 bigdata-engine 目录下运行: mvn clean package -DskipTests
    pause
    exit /b 1
)

:: 3. 按顺序执行任务链 (已移除 ODS/DWD/DWS)

:: 第一步：离线统计
call :RunSparkJob "com.shortvideo.recommendation.offline.job.OfflineJob" "1. [离线统计] 热门榜单 & DAU"
if %errorlevel% neq 0 goto :Fail

:: 第二步：ALS 模型训练
call :RunSparkJob "com.shortvideo.recommendation.als.ALSTrainer" "2. [离线推荐] ALS 模型训练"
if %errorlevel% neq 0 goto :Fail

echo.
echo ========================================================
echo    离线管道全部执行完毕!
echo ========================================================
pause
exit /b 0

:: ========================================================
:: 函数定义
:: ========================================================
:RunSparkJob
set "CLASS_NAME=%~1"
set "JOB_DESC=%~2"

echo.
echo --------------------------------------------------------
echo [STEP] 正在启动: %JOB_DESC%
echo        类名: %CLASS_NAME%

:: 这里的参数说明:
:: --master local[*] : 本地多线程运行
:: --driver-memory 2G : 确保内存足够
:: 配置 HDFS 相关参数
call spark-submit ^
  --class "%CLASS_NAME%" ^
  --master local[*] ^
  --driver-memory 2G ^
  --executor-memory 2G ^
  --conf "spark.hadoop.fs.defaultFS=hdfs://localhost:9000" ^
  --conf "spark.hadoop.dfs.client.use.datanode.hostname=false" ^
  --conf "spark.hadoop.dfs.replication=1" ^
  "%JAR_PATH%" >> "%LOG_FILE%" 2>&1

if %errorlevel% equ 0 (
    echo [SUCCESS] %JOB_DESC% 执行成功.
    exit /b 0
) else (
    echo [ERROR] %JOB_DESC% 执行失败!
    echo     请查看日志详情: %LOG_FILE%
    exit /b 1
)

:Fail
echo.
echo [FATAL] 任务链中断，请检查错误日志。
pause
exit /b 1