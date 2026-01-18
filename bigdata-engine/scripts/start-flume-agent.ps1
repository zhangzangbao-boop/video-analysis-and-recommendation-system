# 启动Flume Agent脚本 (PowerShell版本)
# 用于监听日志文件并将其写入Hadoop

# 设置Flume安装路径 - 请根据实际情况修改
$FLUME_HOME = "C:\flume"  # 请根据实际情况修改为您的Flume安装路径
$CONFIG_FILE = ".\conf\flume-conf.properties"
$AGENT_NAME = "agent1"

Write-Host "正在启动Flume Agent..." -ForegroundColor Green

# 检查配置文件是否存在
if (!(Test-Path $CONFIG_FILE)) {
    Write-Host "错误: 配置文件 $CONFIG_FILE 不存在" -ForegroundColor Red
    exit 1
}

# 检查Flume是否已安装
$FLUME_CMD = Join-Path $FLUME_HOME "bin\flume-ng.cmd"
if (!(Test-Path $FLUME_CMD)) {
    Write-Host "错误: 无法找到Flume执行文件: $FLUME_CMD" -ForegroundColor Red
    Write-Host "请检查FLUME_HOME路径是否正确" -ForegroundColor Yellow
    exit 1
}

Write-Host "使用配置文件: $CONFIG_FILE" -ForegroundColor Cyan
Write-Host "启动Agent: $AGENT_NAME" -ForegroundColor Cyan

# 设置Java选项
$env:JAVA_OPTS = "-Dflume.root.logger=INFO,console -Dorg.apache.flume.log.level=INFO"

# 启动Flume Agent
Write-Host "启动Flume Agent..." -ForegroundColor Green
& $FLUME_CMD agent --conf "$FLUME_HOME\conf" --conf-file $CONFIG_FILE -n $AGENT_NAME

Write-Host "Flume Agent已启动" -ForegroundColor Green