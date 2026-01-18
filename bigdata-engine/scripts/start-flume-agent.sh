#!/bin/bash

# 启动Flume Agent脚本
# 用于监听日志文件并将其写入Hadoop

FLUME_HOME="/opt/flume"  # 请根据实际情况修改为您的Flume安装路径
CONFIG_FILE="./conf/flume-conf.properties"
AGENT_NAME="agent1"

echo "正在启动Flume Agent..."

# 检查配置文件是否存在
if [ ! -f "$CONFIG_FILE" ]; then
    echo "错误: 配置文件 $CONFIG_FILE 不存在"
    exit 1
fi

# 检查Flume是否已安装
if [ ! -x "$FLUME_HOME/bin/flume-ng" ]; then
    echo "错误: 无法找到Flume执行文件，请检查FLUME_HOME路径"
    exit 1
fi

echo "使用配置文件: $CONFIG_FILE"
echo "启动Agent: $AGENT_NAME"

# 启动Flume Agent
$FLUME_HOME/bin/flume-ng agent \
  --conf $FLUME_HOME/conf \
  --conf-file $CONFIG_FILE \
  -Dflume.root.logger=INFO,console \
  -Dorg.apache.flume.log.level=INFO \
  -n $AGENT_NAME

echo "Flume Agent已启动"