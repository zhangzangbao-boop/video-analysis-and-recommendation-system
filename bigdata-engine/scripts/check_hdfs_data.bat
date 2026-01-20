@echo off
setlocal

:: ========================================================
:: 检查HDFS中的数据脚本
:: 用于诊断HDFS中是否有行为日志数据
:: ========================================================

echo ========================================================
echo    检查HDFS中的数据
echo ========================================================
echo.

:: 1. 检查Hadoop命令是否可用
where hadoop >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] hadoop命令未找到！
    echo 请确保Hadoop已安装并配置到PATH
    pause
    exit /b 1
)

echo [INFO] Hadoop命令可用
echo.

:: 2. 检查HDFS连接
echo [INFO] 测试HDFS连接...
hadoop fs -ls hdfs://localhost:9000/ >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] 无法连接到HDFS！
    echo 请确保HDFS服务已启动
    pause
    exit /b 1
)
echo [SUCCESS] HDFS连接正常
echo.

:: 3. 检查数据目录
echo [INFO] 检查数据目录: /short-video/behavior/logs
hadoop fs -test -e hdfs://localhost:9000/short-video/behavior/logs >nul 2>&1
if %errorlevel% neq 0 (
    echo [WARNING] 数据目录不存在: /short-video/behavior/logs
    echo [INFO] 将创建目录...
    hadoop fs -mkdir -p hdfs://localhost:9000/short-video/behavior/logs
    if %errorlevel% equ 0 (
        echo [SUCCESS] 目录创建成功
    ) else (
        echo [ERROR] 目录创建失败
        pause
        exit /b 1
    )
) else (
    echo [SUCCESS] 数据目录存在
)
echo.

:: 4. 列出所有日期子目录
echo [INFO] 检查日期子目录...
hadoop fs -ls hdfs://localhost:9000/short-video/behavior/logs 2>nul
if %errorlevel% neq 0 (
    echo [INFO] 目录为空，没有日期子目录
) else (
    echo [INFO] 找到以下日期目录:
    hadoop fs -ls hdfs://localhost:9000/short-video/behavior/logs | findstr "^d"
)
echo.

:: 5. 统计JSON文件数量
echo [INFO] 统计JSON文件数量...
for /f "delims=" %%i in ('hadoop fs -ls -R hdfs://localhost:9000/short-video/behavior/logs 2^>nul ^| findstr "\.json$"') do (
    set /a json_count+=1
)
if defined json_count (
    echo [INFO] 找到 %json_count% 个JSON文件
) else (
    echo [WARNING] 未找到任何JSON文件
)
echo.

:: 6. 检查本地日志目录
echo [INFO] 检查本地日志目录: bigdata-engine\logs
if exist "bigdata-engine\logs" (
    echo [SUCCESS] 本地日志目录存在
    dir /b "bigdata-engine\logs\*.json" >nul 2>&1
    if %errorlevel% equ 0 (
        echo [INFO] 本地有JSON文件，可以使用以下命令同步到HDFS:
        echo   1. 运行 DataGeneratorApp 会自动同步
        echo   2. 或手动使用 HDFSUtil 同步
    ) else (
        echo [WARNING] 本地也没有JSON文件
    )
) else (
    echo [WARNING] 本地日志目录不存在
)
echo.

echo ========================================================
echo    检查总结
echo ========================================================
echo.
echo [INFO] 如果HDFS中没有数据，请执行以下步骤:
echo   1. 运行数据生成器生成数据:
echo      spark-submit --class com.shortvideo.recommendation.datagenerator.DataGeneratorApp ^
echo        target\bigdata-engine-1.0-jar-with-dependencies.jar
echo   2. 或者使用本地文件路径运行ALSTrainer（用于测试）
echo.
pause
