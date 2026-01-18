@echo off
chcp 65001 >nul
REM 批量导入所有视频JSON文件的批处理脚本
REM 适用于Windows系统

echo ============================================
echo 视频数据批量导入工具
echo ============================================
echo.

REM 设置JSON文件路径（请根据实际情况修改）
REM 方式1: 指向单个文件
REM set JSON_PATH=F:\视频json文件\视频json文件\videos_pexels_food.json
REM 方式2: 指向目录（会导入目录下所有.json文件）
set JSON_PATH=F:\视频json文件\视频json文件\videos_pexels_food.json

REM 设置输出SQL文件路径
set OUTPUT_SQL=backend-server\scripts\imported_videos.sql

REM 设置用户ID（默认使用admin用户，ID为10000）
REM 查询admin用户ID: SELECT id FROM sys_user WHERE username = 'admin';
set USER_ID=10004

REM 设置视频状态（1=已发布，0=待审核）
set STATUS=1

REM 是否直接使用外部URL（如Pexels），不提示上传到COS
REM 设置为1表示使用外部URL，0表示提示需要上传到COS
set USE_EXTERNAL_URL=0

echo JSON文件路径: %JSON_PATH%
echo 输出SQL文件: %OUTPUT_SQL%
echo 用户ID: %USER_ID%
echo 视频状态: %STATUS%
echo 使用外部URL: %USE_EXTERNAL_URL%
echo.

REM 检查Python是否安装
python --version >nul 2>&1
if errorlevel 1 (
    echo 错误: 未找到Python，请先安装Python 3.x
    pause
    exit /b 1
)

REM 检查JSON文件/目录是否存在
echo 正在检查JSON文件路径...
if exist "%JSON_PATH%" (
    echo [OK] 文件存在: %JSON_PATH%
    set JSON_ARG=%JSON_PATH%
    goto :run_script
)

REM 尝试检查是否是目录
if exist "%JSON_PATH%\*.json" (
    echo [OK] 目录存在，将导入目录下所有JSON文件
    set JSON_ARG=%JSON_PATH%\*.json
    goto :run_script
)

REM 路径不存在，尝试查找可能的路径
echo [错误] JSON文件或目录不存在: %JSON_PATH%
echo.
echo 请检查以下可能的问题:
echo 1. 路径是否正确
echo 2. 文件是否真的存在
echo 3. 路径中的中文字符是否正确
echo.
echo 常见路径示例:
echo   F:\视频json文件\视频json文件\videos_pexels_food.json
echo   F:\视频json文件\视频json文件
echo.
pause
exit /b 1

:run_script

REM 构建Python命令
set PYTHON_CMD=python backend-server\scripts\import_videos_from_json.py "%JSON_ARG%" --user-id %USER_ID% --status %STATUS% --output %OUTPUT_SQL%

REM 如果使用外部URL，添加参数
if "%USE_EXTERNAL_URL%"=="1" (
    set PYTHON_CMD=%PYTHON_CMD% --use-external-url
)

REM 运行Python脚本
echo.
echo 正在生成SQL脚本...
echo 执行命令: %PYTHON_CMD%
echo.
%PYTHON_CMD%

if errorlevel 1 (
    echo.
    echo 错误: 生成SQL脚本失败
    pause
    exit /b 1
)

echo.
echo ============================================
echo SQL脚本生成完成！
echo ============================================
echo.
echo 下一步：执行SQL脚本导入数据
echo.
echo 方式1: 使用MySQL命令行
echo   mysql -u root -p short_video_platform ^< %OUTPUT_SQL%
echo.
echo 方式2: 在MySQL客户端中执行
echo   source %OUTPUT_SQL%
echo.
pause
