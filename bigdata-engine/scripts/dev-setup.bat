@echo off
REM 短视频推荐系统Windows开发环境设置脚本
echo ================================
echo  短视频推荐系统开发环境设置
echo ================================

REM 1. 创建本地目录结构
echo.
echo [1] 创建本地目录结构...
if not exist "C:\tmp\shortvideo" (
    mkdir "C:\tmp\shortvideo"
    echo  创建 C:\tmp\shortvideo
)

REM 创建子目录
for %%d in (ods dwd dws ads models logs checkpoint) do (
    if not exist "C:\tmp\shortvideo\%%d" (
        mkdir "C:\tmp\shortvideo\%%d"
        echo  创建 C:\tmp\shortvideo\%%d
    )
)

REM 创建日志子目录
for %%d in (spark kafka flume) do (
    if not exist "C:\tmp\shortvideo\logs\%%d" (
        mkdir "C:\tmp\shortvideo\logs\%%d"
        echo  创建 C:\tmp\shortvideo\logs\%%d
    )
)

echo [1] 本地目录结构创建完成！

REM 2. 检查Redis服务
echo.
echo [2] 检查Redis服务...

REM 方法1：检查Redis是否在运行
tasklist /FI "IMAGENAME eq redis-server.exe" 2>NUL | find /I /N "redis-server.exe">NUL
if "%ERRORLEVEL%"=="0" (
    echo  Redis服务正在运行
    goto :check_kafka
)

REM 方法2：尝试连接Redis
echo 正在连接Redis...
redis-cli ping >nul 2>&1
if "%ERRORLEVEL%"=="0" (
    echo  Redis服务可用
    goto :check_kafka
)

echo.
echo [!] Redis服务未运行，请手动启动Redis：
echo  方法1：使用安装的Redis
echo      - 打开Redis安装目录
echo      - 运行 redis-server.exe
echo.
echo  方法2：使用Docker（如果已安装）
echo      docker run -p 6379:6379 redis:6.2-alpine
echo.
echo  方法3：使用Windows服务
echo      net start redis
echo.
pause

:check_kafka
REM 3. 检查Kafka服务
echo.
echo [3] 检查Kafka服务...

REM 检查端口9092是否监听
netstat -an | findstr ":9092" >nul
if "%ERRORLEVEL%"=="0" (
    echo  Kafka服务正在运行（端口9092）
    goto :setup_complete
)

echo.
echo [!] Kafka服务未运行，请选择启动方式：
echo.
echo  方法1：使用Docker（推荐）
echo      docker run -p 9092:9092 ^
echo          -e KAFKA_BROKER_ID=1 ^
echo          -e KAFKA_ZOOKEEPER_CONNECT=localhost:2181 ^
echo          -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 ^
echo          -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 ^
echo          apache/kafka:2.8.0
echo.
echo  方法2：本地安装Kafka
echo      1. 下载Kafka：https://kafka.apache.org/downloads
echo      2. 解压到本地目录
echo      3. 启动Zookeeper：
echo         .\bin\windows\zookeeper-server-start.bat config\zookeeper.properties
echo      4. 启动Kafka：
echo         .\bin\windows\kafka-server-start.bat config\server.properties
echo.
echo  方法3：使用开发模式（跳过Kafka）
echo      修改application.conf中的kafka.bootstrap.servers为""
echo      或者使用MockKafka进行开发
echo.

:setup_complete
echo ================================
echo  开发环境设置完成！
echo ================================
echo.
echo 下一步操作：
echo  1. 确保Redis服务运行在 localhost:6379
echo  2. 确保Kafka服务运行在 localhost:9092（可选）
echo  3. 运行项目测试：
echo      mvn test
echo  4. 运行Common模块测试：
echo      mvn exec:java ^
echo          -Dexec.mainClass="com.shortvideo.recommendation.common.TestCommon"
echo.
pause