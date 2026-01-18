@echo off
setlocal

:: ========================================================
:: 短视频推荐系统 - 离线分析全链路调度脚本 (Windows版)
:: 顺序：ODS -> DWD -> DWS -> 离线统计 & ALS模型训练
:: ========================================================

:: 1. 基础配置
:: 获取当前脚本所在目录的上一级目录作为项目根目录
cd /d %~dp0..
set "BASE_DIR=%cd%"
set "JAR_PATH=%BASE_DIR%\target\spark-example-1.0-jar-with-dependencies.jar"
set "LOG_DIR=%BASE_DIR%\logs\pipeline"

:: 创建日志目录
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

:: 生成日志文件名 (格式: offline_job_YYYYMMDD_HHMM.log)
set "CURRENT_DATE=%date:~0,4%%date:~5,2%%date:~8,2%"
set "CURRENT_TIME=%time:~0,2%%time:~3,2%"
set "CURRENT_TIME=%CURRENT_TIME: =0%"
set "LOG_FILE=%LOG_DIR%\offline_job_%CURRENT_DATE%_%CURRENT_TIME%.log"

echo ========================================================
echo    开始执行离线分析数据管道
echo    日志文件: %LOG_FILE%
echo ========================================================

:: 2. 检查 JAR 包是否存在
if not exist "%JAR_PATH%" (
    echo [ERROR] 未找到 JAR 包: %JAR_PATH%
    echo 请先在 bigdata-engine 目录下运行: mvn clean package -DskipTests
    pause
    exit /b 1
)

:: 3. 按顺序执行任务链

:: 第一步：ODS 层
call :RunSparkJob "com.shortvideo.recommendation.offline.ods.OdsLogToHdfs" "1. ODS 层 (日志接入)"
if %errorlevel% neq 0 goto :Fail

:: 第二步：DWD 层
call :RunSparkJob "com.shortvideo.recommendation.offline.dwd.DwdLogClean" "2. DWD 层 (数据清洗)"
if %errorlevel% neq 0 goto :Fail

:: 第三步：DWS 层
call :RunSparkJob "com.shortvideo.recommendation.offline.dws.DwsUserVideoFeature" "3. DWS 层 (特征聚合)"
if %errorlevel% neq 0 goto :Fail

:: 第四步：离线统计
call :RunSparkJob "com.shortvideo.recommendation.offline.job.OfflineJob" "4. 离线统计 (热门榜单 & DAU)"
if %errorlevel% neq 0 goto :Fail

:: 第五步：ALS 模型训练
call :RunSparkJob "com.shortvideo.recommendation.als.ALSTrainer" "5. ALS 模型训练 (生成推荐)"
if %errorlevel% neq 0 goto :Fail

echo.
echo ========================================================
echo    离线管道全部执行完毕!
echo ========================================================
pause
exit /b 0

:: ========================================================
:: 函数定义: 执行 Spark 任务
:: ========================================================
:RunSparkJob
set "CLASS_NAME=%~1"
set "JOB_DESC=%~2"

echo.
echo --------------------------------------------------------
echo [STEP] 正在启动: %JOB_DESC%
echo        类名: %CLASS_NAME%

:: 提交任务 (Windows 下不需要 --master yarn，通常用 local[*])
call spark-submit ^
  --class "%CLASS_NAME%" ^
  --master local[*] ^
  --driver-memory 2G ^
  --executor-memory 2G ^
  "%JAR_PATH%" >> "%LOG_FILE%" 2>&1

if %errorlevel% equ 0 (
    echo [SUCCESS] %JOB_DESC% 执行成功.
    exit /b 0
) else (
    echo [ERROR] %JOB_DESC% 执行失败!
    echo         请查看日志详情: %LOG_FILE%
    exit /b 1
)

:Fail
echo.
echo [FATAL] 任务链中断，请检查错误日志。
pause
exit /b 1