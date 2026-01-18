# Flume与数据生成器集成脚本 (PowerShell版本)
# 用于启动整个数据管道：数据生成器 → 日志文件 → Flume → HDFS

Write-Host "===========================================" -ForegroundColor Cyan
Write-Host "短视频推荐系统 - Flume与Hadoop集成设置" -ForegroundColor Cyan
Write-Host "===========================================" -ForegroundColor Cyan

# 创建必要的目录
Write-Host "创建日志目录..." -ForegroundColor Green
New-Item -ItemType Directory -Path "logs" -Force
New-Item -ItemType Directory -Path "flume\checkpoint" -Force
New-Item -ItemType Directory -Path "flume\data" -Force

# 检查Java是否已安装
if (!(Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Host "错误: Java未安装，请先安装Java" -ForegroundColor Red
    exit 1
}

# 检查Maven是否已安装
if (!(Get-Command mvn -ErrorAction SilentlyContinue)) {
    Write-Host "警告: Maven未安装，跳过编译步骤" -ForegroundColor Yellow
} else {
    Write-Host "编译项目..." -ForegroundColor Green
    mvn clean compile
    if ($LASTEXITCODE -ne 0) {
        Write-Host "编译失败，请检查代码" -ForegroundColor Red
        exit 1
    }
    Write-Host "编译完成" -ForegroundColor Green
}

# 检查Hadoop是否运行
Write-Host "检查Hadoop服务状态..." -ForegroundColor Green
try {
    hdfs dfsadmin -report | Out-Null
    Write-Host "Hadoop服务正常运行" -ForegroundColor Green
} catch {
    Write-Host "警告: 无法连接到Hadoop，请确保Hadoop服务已启动" -ForegroundColor Yellow
}

# 提供运行选项
Write-Host "" -ForegroundColor White
Write-Host "可用的运行选项:" -ForegroundColor White
Write-Host "1. 启动数据生成器" -ForegroundColor White
Write-Host "2. 显示帮助信息" -ForegroundColor White
Write-Host "3. 退出" -ForegroundColor White

$choice = Read-Host "请选择 (1-3)"

switch ($choice) {
    1 {
        Write-Host "启动数据生成器..." -ForegroundColor Green
        # 检查scala是否可用
        if (Get-Command scala -ErrorAction SilentlyContinue) {
            scala com.shortvideo.recommendation.datagenerator.DataGeneratorApp
        } else {
            Write-Host "错误: 未找到Scala命令，请确保Scala已安装并添加到PATH" -ForegroundColor Red
        }
    }
    2 {
        Write-Host "" -ForegroundColor White
        Write-Host "使用说明:" -ForegroundColor Yellow
        Write-Host "1. 确保Hadoop HDFS服务正在运行" -ForegroundColor White
        Write-Host "2. 确保Flume服务正在运行 (参考FLUME_INTEGRATION.md)" -ForegroundColor White
        Write-Host "3. 运行数据生成器产生数据" -ForegroundColor White
        Write-Host "4. Flume将自动捕获日志文件并传输到HDFS" -ForegroundColor White
        Write-Host "" -ForegroundColor White
        Write-Host "Flume配置文件:" -ForegroundColor Yellow
        Write-Host " - conf/flume-conf.properties (SpoolDir模式)" -ForegroundColor White
        Write-Host " - conf/flume-exec-conf.properties (Exec模式)" -ForegroundColor White
    }
    3 {
        Write-Host "退出" -ForegroundColor Green
        exit 0
    }
    default {
        Write-Host "无效选择" -ForegroundColor Red
        exit 1
    }
}