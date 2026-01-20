@echo off
setlocal

:: ========================================================
:: Flume Agent 启动脚本 (Windows)
:: 功能：启动Flume Agent，将本地日志文件同步到HDFS
:: ========================================================

:: 1. 基础配置
cd /d %~dp0..
set "BASE_DIR=%cd%"
set "FLUME_CONF_DIR=%BASE_DIR%\bigdata-engine\conf"
set "FLUME_CONF_FILE=%FLUME_CONF_DIR%\flume-conf.properties"
set "LOG_DIR=%BASE_DIR%\bigdata-engine\logs"

echo ========================================================
echo    启动 Flume Agent (agent1)
echo    配置文件: %FLUME_CONF_FILE%
echo    监控目录: %LOG_DIR%
echo ========================================================
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

:: 4. 确保Flume数据目录存在（channel文件目录）
if not exist "D:\Project\flume_data\checkpoint" mkdir "D:\Project\flume_data\checkpoint"
if not exist "D:\Project\flume_data\data" mkdir "D:\Project\flume_data\data"

:: 5. 检查Hadoop/HDFS是否运行
echo [INFO] 检查HDFS连接...
hadoop fs -ls hdfs://localhost:9000/ 2>nul
if %errorlevel% neq 0 (
    echo [WARNING] 无法连接到HDFS，请确保：
    echo   1. Hadoop NameNode已启动
    echo   2. HDFS服务正在运行（端口9000）
    echo   3. 环境变量HADOOP_HOME已设置
    echo.
    echo 继续启动Flume...（如果HDFS未就绪，同步将失败）
    echo.
    timeout /t 3
)

:: 6. 检查HDFS目标目录是否存在，不存在则创建
echo [INFO] 检查HDFS目录...
hadoop fs -mkdir -p hdfs://localhost:9000/short-video/behavior/logs 2>nul
if %errorlevel% equ 0 (
    echo [SUCCESS] HDFS目录已准备就绪
) else (
    echo [WARNING] 无法创建HDFS目录，但继续启动...
)

echo.
echo ========================================================
echo    启动 Flume Agent...
echo ========================================================
echo.

:: 7. 启动Flume Agent
:: 注意：需要设置FLUME_HOME环境变量，或使用完整路径
if defined FLUME_HOME (
    set "FLUME_CMD=%FLUME_HOME%\bin\flume-ng.cmd"
) else (
    echo [INFO] 未设置FLUME_HOME环境变量，尝试使用系统PATH中的flume-ng
    set "FLUME_CMD=flume-ng.cmd"
)

%FLUME_CMD% agent ^
  --conf %FLUME_CONF_DIR% ^
  --conf-file %FLUME_CONF_FILE% ^
  --name agent1 ^
  -Dflume.root.logger=INFO,console

if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Flume启动失败！
    echo 请检查：
    echo   1. FLUME_HOME环境变量是否设置
    echo   2. Flume是否已安装
    echo   3. 配置文件语法是否正确
    echo   4. 是否有端口冲突
    pause
    exit /b 1
)

pause
