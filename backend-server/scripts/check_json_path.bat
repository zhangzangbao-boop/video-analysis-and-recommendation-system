@echo off
chcp 65001 >nul
REM 检查JSON文件路径的辅助脚本

echo ============================================
echo JSON文件路径检查工具
echo ============================================
echo.

REM 常见的JSON文件路径
set PATH1=F:\视频json文件\视频json文件\videos_pexels_food.json
set PATH2=F:\视频json文件\视频json文件\videos_pexels_funny.json
set PATH3=F:\视频json文件\视频json文件\videos_pexels_game.json
set PATH4=F:\视频json文件\视频json文件\videos_pexels_life.json
set PATH5=F:\视频json文件\视频json文件\videos_pexels_pet.json
set PATH6=F:\视频json文件\视频json文件\videos_pexels_tech.json
set DIR_PATH=F:\视频json文件\视频json文件

echo 正在检查常见路径...
echo.

REM 检查目录
if exist "%DIR_PATH%" (
    echo [OK] 目录存在: %DIR_PATH%
    echo.
    echo 目录下的JSON文件:
    dir "%DIR_PATH%\*.json" /b 2>nul
    if errorlevel 1 (
        echo [警告] 目录下没有找到JSON文件
    )
    echo.
) else (
    echo [错误] 目录不存在: %DIR_PATH%
    echo.
)

REM 检查各个文件
echo 检查单个文件:
if exist "%PATH1%" echo [OK] %PATH1%
if not exist "%PATH1%" echo [不存在] %PATH1%

if exist "%PATH2%" echo [OK] %PATH2%
if not exist "%PATH2%" echo [不存在] %PATH2%

if exist "%PATH3%" echo [OK] %PATH3%
if not exist "%PATH3%" echo [不存在] %PATH3%

if exist "%PATH4%" echo [OK] %PATH4%
if not exist "%PATH4%" echo [不存在] %PATH4%

if exist "%PATH5%" echo [OK] %PATH5%
if not exist "%PATH5%" echo [不存在] %PATH5%

if exist "%PATH6%" echo [OK] %PATH6%
if not exist "%PATH6%" echo [不存在] %PATH6%

echo.
echo ============================================
echo 提示:
echo 如果文件不存在，请:
echo 1. 检查文件路径是否正确
echo 2. 确认文件是否在F盘
echo 3. 尝试使用完整路径
echo ============================================
echo.
pause
