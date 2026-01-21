@echo off
setlocal enabledelayedexpansion

:: ========================================================
:: 将本地日志文件上传到HDFS
:: 功能：将 bigdata-engine\logs 目录下的JSON文件上传到HDFS
:: ========================================================

echo ========================================================
echo    上传本地日志到HDFS
echo ========================================================
echo.

:: 1. 检查Hadoop命令
where hadoop >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] hadoop命令未找到！
    pause
    exit /b 1
)

:: 2. 检查本地日志目录
set "LOCAL_LOG_DIR=bigdata-engine\logs"
if not exist "%LOCAL_LOG_DIR%" (
    echo [ERROR] 本地日志目录不存在: %LOCAL_LOG_DIR%
    pause
    exit /b 1
)

echo [INFO] 本地日志目录: %LOCAL_LOG_DIR%
echo.

:: 3. 检查HDFS连接
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

:: 4. 创建HDFS目录结构
echo [INFO] 创建HDFS目录结构...
hadoop fs -mkdir -p hdfs://localhost:9000/short-video/behavior/logs >nul 2>&1

:: 获取当前日期
for /f "tokens=*" %%i in ('powershell -Command "Get-Date -Format 'yyyy-MM-dd'"') do set date_str=%%i
if "%date_str%"=="" (
    :: 如果PowerShell不可用，使用date命令
    for /f "tokens=1-3 delims=/ " %%a in ('date /t') do (
        set month=%%a
        set day=%%b
        set year=%%c
    )
    set "date_str=%year%-%month%-%day%"
    set "date_str=%date_str: =0%"
)

set "HDFS_DATE_DIR=hdfs://localhost:9000/short-video/behavior/logs/%date_str%"
hadoop fs -mkdir -p "%HDFS_DATE_DIR%" >nul 2>&1
echo [INFO] HDFS日期目录: %HDFS_DATE_DIR%
echo.

:: 5. 上传JSON文件
echo [INFO] 开始上传JSON文件...
set /a count=0
set /a success=0
set /a failed=0

for %%f in ("%LOCAL_LOG_DIR%\*.json") do (
    set /a count+=1
    set "filename=%%~nxf"
    echo [INFO] 上传: !filename!
    
    hadoop fs -put "%%f" "%HDFS_DATE_DIR%/" >nul 2>&1
    if !errorlevel! equ 0 (
        set /a success+=1
        echo [SUCCESS] !filename! 上传成功
    ) else (
        set /a failed+=1
        echo [ERROR] !filename! 上传失败
    )
)

echo.
echo ========================================================
echo    上传完成
echo ========================================================
echo [INFO] 总计: %count% 个文件
echo [INFO] 成功: %success% 个文件
echo [INFO] 失败: %failed% 个文件
echo [INFO] HDFS路径: %HDFS_DATE_DIR%
echo.

:: 6. 验证上传结果
echo [INFO] 验证HDFS中的文件...
hadoop fs -ls "%HDFS_DATE_DIR%" | findstr "\.json$"
echo.

echo [SUCCESS] 上传完成！现在可以运行 ALSTrainer 了
pause
