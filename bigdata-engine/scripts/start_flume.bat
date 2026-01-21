@echo off
setlocal enabledelayedexpansion

:: ========================================================
:: Flume 双通道启动脚本 (Windows)
:: 功能：
::   - Agent1: 离线分析通道 (logs目录 -> HDFS)
::   - Agent2: 实时分析通道 (generated_logs.json -> Kafka)
:: ========================================================

:: 1. 基础配置
cd /d %~dp0..
set "BASE_DIR=%cd%"
set "FLUME_CONF_DIR=%BASE_DIR%\bigdata-engine\conf"
set "FLUME_CONF_FILE=%FLUME_CONF_DIR%\flume-conf.properties"
set "LOG_DIR=%BASE_DIR%\bigdata-engine\logs"
set "GENERATED_LOGS=%BASE_DIR%\bigdata-engine\generated_logs.json"

echo ========================================================
echo    Flume 双通道启动脚本
echo    配置文件: %FLUME_CONF_FILE%
echo ========================================================
echo.
echo [Agent1] 离线分析通道: %LOG_DIR% -^> HDFS
echo [Agent2] 实时分析通道: %GENERATED_LOGS% -^> Kafka
echo.

:: 2. 检查配置文件是否存在
if not exist "%FLUME_CONF_FILE%" (
    echo [ERROR] Flume配置文件不存在: %FLUME_CONF_FILE%
    pause
    exit /b 1
)

:: 3. 确保日志目录存在
if not exist "%LOG_DIR%" (
    echo [INFO] 创建日志目录: %LOG_DIR%
    mkdir "%LOG_DIR%"
)

:: 4. 确保generated_logs.json文件存在
if not exist "%GENERATED_LOGS%" (
    echo [INFO] 创建generated_logs.json文件: %GENERATED_LOGS%
    type nul ^> "%GENERATED_LOGS%"
)

:: 5. 确保Flume数据目录存在（channel文件目录）
if not exist "%BASE_DIR%\flume\checkpoint_offline" mkdir "%BASE_DIR%\flume\checkpoint_offline"
if not exist "%BASE_DIR%\flume\data_offline" mkdir "%BASE_DIR%\flume\data_offline"
if not exist "%BASE_DIR%\flume\checkpoint_realtime" mkdir "%BASE_DIR%\flume\checkpoint_realtime"
if not exist "%BASE_DIR%\flume\data_realtime" mkdir "%BASE_DIR%\flume\data_realtime"

:: 6. 检查Hadoop/HDFS是否运行（Agent1需要）
echo [INFO] 检查HDFS连接（Agent1需要）...
hadoop fs -ls hdfs://localhost:9000/ 2>nul
if %errorlevel% neq 0 (
    echo [WARNING] 无法连接到HDFS，请确保：
    echo   1. Hadoop NameNode已启动
    echo   2. HDFS服务正在运行（端口9000）
    echo   3. 环境变量HADOOP_HOME已设置
    echo.
    echo 继续启动Flume...（如果HDFS未就绪，Agent1将失败）
    echo.
    timeout /t 3
)

:: 7. 检查HDFS目标目录是否存在（Agent1需要）
echo [INFO] 检查HDFS目录...
hadoop fs -mkdir -p hdfs://localhost:9000/short-video/behavior/logs 2>nul
if %errorlevel% equ 0 (
    echo [SUCCESS] HDFS目录已准备就绪
) else (
    echo [WARNING] 无法创建HDFS目录，但继续启动...
)

echo.
echo ========================================================
echo    启动 Flume Agent1 (离线分析通道)...
echo ========================================================
echo.

:: 8. 查找Flume命令
if defined FLUME_HOME (
    set "FLUME_CMD=%FLUME_HOME%\bin\flume-ng.cmd"
) else (
    echo [INFO] 未设置FLUME_HOME环境变量，尝试使用系统PATH中的flume-ng
    set "FLUME_CMD=flume-ng.cmd"
)

:: 9. 启动Agent1（离线分析通道）
start "Flume Agent1 - 离线分析" cmd /k %FLUME_CMD% agent --conf %FLUME_CONF_DIR% --conf-file %FLUME_CONF_FILE% --name agent1 -Dflume.root.logger=INFO,console

:: 等待一下确保Agent1启动
timeout /t 2 >nul

echo.
echo ========================================================
echo    启动 Flume Agent2 (实时分析通道)...
echo ========================================================
echo.

:: 10. 检查Kafka是否运行（Agent2需要）
echo [INFO] 检查Kafka连接（Agent2需要）...
kafka-topics.bat --list --bootstrap-server localhost:9092 2>nul
if %errorlevel% neq 0 (
    echo [WARNING] 无法连接到Kafka，请确保：
    echo   1. Kafka服务已启动
    echo   2. Kafka服务正在运行（端口9092）
    echo   3. 环境变量KAFKA_HOME已设置
    echo.
    echo 继续启动Flume...（如果Kafka未就绪，Agent2将失败）
    echo.
    timeout /t 3
)

:: 11. 启动Agent2（实时分析通道）
start "Flume Agent2 - 实时分析" cmd /k %FLUME_CMD% agent --conf %FLUME_CONF_DIR% --conf-file %FLUME_CONF_FILE% --name agent2 -Dflume.root.logger=INFO,console

echo.
echo ========================================================
echo    Flume双通道已启动！
echo ========================================================
echo.
echo [Agent1] 离线分析通道: 监听 %LOG_DIR% 目录，发送到HDFS
echo [Agent2] 实时分析通道: 监听 %GENERATED_LOGS% 文件，发送到Kafka
echo.
echo 提示：
echo   - 每个Agent在独立的命令行窗口中运行
echo   - 关闭对应的命令行窗口即可停止对应的Agent
echo   - 按Ctrl+C可以停止Agent
echo.
pause
