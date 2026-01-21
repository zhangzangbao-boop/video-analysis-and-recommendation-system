@echo off
setlocal

:: ========================================================
:: 检查推荐结果数据脚本
:: 用于诊断 recommendation_result 表的数据情况
:: ========================================================

echo ========================================================
echo    检查推荐结果数据
echo ========================================================
echo.

:: 数据库配置（请根据实际情况修改）
set DB_HOST=localhost
set DB_PORT=3306
set DB_NAME=short_video_platform
set DB_USER=root
set DB_PASSWORD=123456

echo [INFO] 数据库配置:
echo   - 主机: %DB_HOST%:%DB_PORT%
echo   - 数据库: %DB_NAME%
echo   - 用户: %DB_USER%
echo.

:: 检查 MySQL 连接
echo [INFO] 检查 MySQL 连接...
mysql -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASSWORD% -e "SELECT 1;" %DB_NAME% >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] MySQL 连接失败！
    echo 请检查：
    echo   1. MySQL 服务是否运行
    echo   2. 数据库配置是否正确
    echo   3. 用户名和密码是否正确
    pause
    exit /b 1
)
echo [SUCCESS] MySQL 连接成功
echo.

:: 检查表是否存在
echo [INFO] 检查 recommendation_result 表...
mysql -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASSWORD% -e "SHOW TABLES LIKE 'recommendation_result';" %DB_NAME% | findstr "recommendation_result" >nul
if %errorlevel% neq 0 (
    echo [ERROR] recommendation_result 表不存在！
    echo 请执行 database.sql 创建表结构
    pause
    exit /b 1
)
echo [SUCCESS] recommendation_result 表存在
echo.

:: 统计推荐结果数据
echo [INFO] 统计推荐结果数据...
echo.
echo ========================================================
echo    推荐结果统计
echo ========================================================
mysql -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASSWORD% %DB_NAME% -e "
SELECT 
    type AS '推荐类型',
    COUNT(*) AS '总记录数',
    COUNT(DISTINCT user_id) AS '用户数',
    COUNT(DISTINCT video_id) AS '视频数',
    MIN(create_time) AS '最早记录',
    MAX(create_time) AS '最新记录'
FROM recommendation_result
GROUP BY type;
"
echo.

:: 显示最近的推荐记录
echo [INFO] 最近的推荐记录（前10条）...
mysql -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASSWORD% %DB_NAME% -e "
SELECT 
    id,
    user_id,
    video_id,
    score,
    rank,
    type,
    model_id,
    create_time
FROM recommendation_result
ORDER BY create_time DESC
LIMIT 10;
"
echo.

:: 检查模型参数表
echo [INFO] 检查 model_params 表...
mysql -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASSWORD% %DB_NAME% -e "
SELECT 
    model_id,
    rank,
    reg_param,
    max_iter,
    rmse,
    status,
    training_time,
    model_path
FROM model_params
ORDER BY create_time DESC
LIMIT 5;
"
echo.

echo ========================================================
echo    检查完成
echo ========================================================
echo.
echo 如果 recommendation_result 表为空，请检查：
echo   1. ALSTrainer 是否成功运行
echo   2. 查看 ALSTrainer 的日志输出
echo   3. 检查 MySQL 连接配置是否正确
echo   4. 检查 HDFS 中是否有行为日志数据
echo.
pause
