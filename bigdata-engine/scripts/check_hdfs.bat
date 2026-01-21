@echo off
setlocal

:: ========================================================
:: HDFS连接检查脚本
:: 功能：检查HDFS连接和目录状态
:: ========================================================

echo ========================================================
echo    HDFS连接和目录检查
echo ========================================================
echo.

:: 1. 检查Hadoop命令是否可用
where hadoop >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] hadoop命令未找到！
    echo 请确保：
    echo   1. Hadoop已安装
    echo   2. HADOOP_HOME环境变量已设置
    echo   3. %HADOOP_HOME%\bin已添加到PATH
    pause
    exit /b 1
)

echo [INFO] Hadoop命令可用
echo.

:: 2. 检查HDFS连接
echo [INFO] 测试HDFS连接...
hadoop fs -ls hdfs://localhost:9000/ 2>nul
if %errorlevel% equ 0 (
    echo [SUCCESS] HDFS连接正常
) else (
    echo [ERROR] 无法连接到HDFS！
    echo 请确保：
    echo   1. Hadoop NameNode已启动: start-dfs.cmd
    echo   2. HDFS服务正在运行（默认端口9000）
    echo   3. 网络连接正常
    pause
    exit /b 1
)

echo.

:: 3. 检查并创建目标目录
echo [INFO] 检查HDFS目录结构...
hadoop fs -test -e hdfs://localhost:9000/short-video/behavior/logs 2>nul
if %errorlevel% equ 0 (
    echo [SUCCESS] 目录已存在: /short-video/behavior/logs
    echo.
    echo [INFO] 目录内容:
    hadoop fs -ls hdfs://localhost:9000/short-video/behavior/logs 2>nul
    if %errorlevel% neq 0 (
        echo [INFO] 目录为空（正常，Flume会创建日期子目录）
    )
) else (
    echo [INFO] 创建目录: /short-video/behavior/logs
    hadoop fs -mkdir -p hdfs://localhost:9000/short-video/behavior/logs 2>nul
    if %errorlevel% equ 0 (
        echo [SUCCESS] 目录创建成功
    ) else (
        echo [ERROR] 目录创建失败
        echo 请检查HDFS权限设置
        pause
        exit /b 1
    )
)

echo.
echo [INFO] 检查日期子目录...
:: 获取当前日期 (格式: YYYY-MM-DD)
for /f "tokens=1-3 delims=-" %%a in ('powershell -Command "Get-Date -Format 'yyyy-MM-dd'"') do set date_str=%%a-%%b-%%c
if "%date_str%"=="" (
    :: 如果PowerShell不可用，使用date命令（需要根据系统区域设置调整）
    for /f "tokens=1-3 delims=/ " %%a in ('date /t') do (
        set month=%%a
        set day=%%b
        set year=%%c
    )
    set "date_str=%year%-%month%-%day%"
    set "date_str=%date_str: =0%"
)

if defined date_str (
    echo [INFO] 今日日期: %date_str%
    hadoop fs -test -e hdfs://localhost:9000/short-video/behavior/logs/%date_str% 2>nul
    if %errorlevel% equ 0 (
        echo [INFO] 今日目录已存在: /short-video/behavior/logs/%date_str%
        echo [INFO] 目录内容:
        hadoop fs -ls hdfs://localhost:9000/short-video/behavior/logs/%date_str% 2>nul
    ) else (
        echo [INFO] 今日目录将在Flume运行时自动创建
    )
) else (
    echo [WARNING] 无法获取日期，跳过日期目录检查
)

echo.
echo ========================================================
echo    检查总结
echo ========================================================
echo.
echo [INFO] HDFS连接状态: 正常
echo [INFO] 目标目录: hdfs://localhost:9000/short-video/behavior/logs
echo [INFO] 下一步: 运行 start_flume.bat 启动Flume Agent
echo.
echo ========================================================
echo    检查完成
echo ========================================================
pause
