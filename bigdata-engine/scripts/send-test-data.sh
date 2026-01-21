#!/bin/bash
# 快速发送测试数据到Kafka的脚本

echo "=== 发送测试数据到Kafka ==="
echo ""

# 检查参数
if [ $# -eq 0 ]; then
    echo "用法: $0 <数量>"
    echo "示例: $0 100  # 发送100条测试数据"
    exit 1
fi

COUNT=$1

echo "准备发送 $COUNT 条测试数据到Kafka topic: shortvideo_user_behavior"
echo ""

# 使用sbt运行LogProducer
cd "$(dirname "$0")/.."
sbt "runMain com.shortvideo.recommendation.datagenerator.LogProducer fixed $COUNT"

echo ""
echo "数据发送完成！"
