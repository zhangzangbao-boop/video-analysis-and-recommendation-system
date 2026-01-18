#!/bin/bash

# Flume与数据生成器集成脚本
# 用于启动整个数据管道：数据生成器 → 日志文件 → Flume → HDFS

echo "==========================================="
echo "短视频推荐系统 - Flume与Hadoop集成设置"
echo "==========================================="

# 创建必要的目录
echo "创建日志目录..."
mkdir -p logs
mkdir -p flume/checkpoint
mkdir -p flume/data

# 检查Java是否已安装
if ! command -v java &> /dev/null; then
    echo "错误: Java未安装，请先安装Java"
    exit 1
fi

# 检查Maven是否已安装
if ! command -v mvn &> /dev/null; then
    echo "警告: Maven未安装，跳过编译步骤"
else
    echo "编译项目..."
    mvn clean compile -q
    if [ $? -ne 0 ]; then
        echo "编译失败，请检查代码"
        exit 1
    fi
    echo "编译完成"
fi

# 检查Hadoop是否运行
echo "检查Hadoop服务状态..."
if hdfs dfsadmin -report &> /dev/null; then
    echo "Hadoop服务正常运行"
else
    echo "警告: 无法连接到Hadoop，请确保Hadoop服务已启动"
fi

# 提供运行选项
echo ""
echo "可用的运行选项:"
echo "1. 启动Flume Agent (SpoolDir模式)"
echo "2. 启动Flume Agent (Exec模式)" 
echo "3. 启动数据生成器"
echo "4. 启动Flume和数据生成器 (推荐)"
echo "5. 退出"

read -p "请选择 (1-5): " choice

case $choice in
    1)
        echo "启动Flume Agent (SpoolDir模式)..."
        if [ -z "$FLUME_HOME" ]; then
            echo "错误: 请设置FLUME_HOME环境变量"
            exit 1
        fi
        
        $FLUME_HOME/bin/flume-ng agent \
          --conf $FLUME_HOME/conf \
          --conf-file ./conf/flume-conf.properties \
          -Dflume.root.logger=INFO,console \
          -n agent1
        ;;
    2)
        echo "启动Flume Agent (Exec模式)..."
        if [ -z "$FLUME_HOME" ]; then
            echo "错误: 请设置FLUME_HOME环境变量"
            exit 1
        fi
        
        $FLUME_HOME/bin/flume-ng agent \
          --conf $FLUME_HOME/conf \
          --conf-file ./conf/flume-exec-conf.properties \
          -Dflume.root.logger=INFO,console \
          -n agent2
        ;;
    3)
        echo "启动数据生成器..."
        scala com.shortvideo.recommendation.datagenerator.DataGeneratorApp
        ;;
    4)
        echo "启动完整的数据管道..."
        echo "请先确保Flume服务已启动，然后运行数据生成器"
        echo ""
        echo "1. 在第一个终端启动Flume: ./scripts/setup-and-run.sh (选择选项1)"
        echo "2. 在第二个终端启动数据生成器: ./scripts/setup-and-run.sh (选择选项3)"
        ;;
    5)
        echo "退出"
        exit 0
        ;;
    *)
        echo "无效选择"
        exit 1
        ;;
esac