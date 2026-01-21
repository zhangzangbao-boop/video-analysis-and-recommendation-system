@echo off
chcp 65001 > nul
title 发送Kafka测试数据（Maven构建版本）

:: 检查参数是否为空
if "%1"=="" (
    echo 用法: %0 ^<数据数量^>
    echo 示例: %0 50  # 发送50条测试数据
    pause > nul
    exit /b 1
)

set DATA_COUNT=%1
set PROJECT_DIR="%~dp0\.."
set JAR_PATH="target\bigdata-engine-1.0-jar-with-dependencies.jar"

:: 检查JAR文件是否存在
if not exist %JAR_PATH% (
    echo 错误：JAR包不存在于 %JAR_PATH%
    echo 请先在项目根目录执行 'mvn package' 命令构建项目
    pause > nul
    exit /b 1
)

echo === 发送测试数据到Kafka ===
echo 目标Topic: shortvideo_user_behavior
echo 发送数量: %DATA_COUNT%
echo.

:: 切换到项目根目录
cd /d %PROJECT_DIR% || (
    echo 错误：无法切换到项目目录 %PROJECT_DIR%
    pause > nul
    exit /b 1
)

:: 核心：使用Java命令运行LogProducer
echo 正在运行Java程序（首次运行可能会稍慢，耐心等待）...
echo.
java -cp %JAR_PATH% com.shortvideo.recommendation.datagenerator.LogProducer fixed %DATA_COUNT%

:: 执行结果判断
if %errorlevel% equ 0 (
    echo.
    echo ✅ 数据发送成功！
) else (
    echo.
    echo ❌ 数据发送失败！
    echo 常见原因：
    echo 1. Kafka服务未启动（检查localhost:9092是否可连接）
    echo 2. 主类名错误（确认LogProducer的包名是否正确）
    echo 3. 依赖问题（请检查JAR包完整性）
)
pause > nul