#!/bin/bash
# 批量导入所有视频JSON文件的Shell脚本
# 适用于Linux/Mac系统

echo "============================================"
echo "视频数据批量导入工具"
echo "============================================"
echo ""

# 设置JSON文件目录（请根据实际情况修改）
JSON_DIR="f:/视频json文件/视频json文件"

# 设置输出SQL文件路径
OUTPUT_SQL="backend-server/scripts/imported_videos.sql"

# 设置用户ID（默认使用admin用户，ID为10000）
USER_ID=10000

# 设置视频状态（1=已发布，0=待审核）
STATUS=1

echo "JSON文件目录: $JSON_DIR"
echo "输出SQL文件: $OUTPUT_SQL"
echo "用户ID: $USER_ID"
echo "视频状态: $STATUS"
echo ""

# 检查Python是否安装
if ! command -v python3 &> /dev/null; then
    if ! command -v python &> /dev/null; then
        echo "错误: 未找到Python，请先安装Python 3.x"
        exit 1
    else
        PYTHON_CMD="python"
    fi
else
    PYTHON_CMD="python3"
fi

# 检查JSON目录是否存在
if [ ! -d "$JSON_DIR" ]; then
    echo "错误: JSON文件目录不存在: $JSON_DIR"
    echo "请修改脚本中的 JSON_DIR 变量"
    exit 1
fi

# 运行Python脚本
echo "正在生成SQL脚本..."
$PYTHON_CMD backend-server/scripts/import_videos_from_json.py "$JSON_DIR"/*.json --user-id $USER_ID --status $STATUS --output $OUTPUT_SQL

if [ $? -ne 0 ]; then
    echo ""
    echo "错误: 生成SQL脚本失败"
    exit 1
fi

echo ""
echo "============================================"
echo "SQL脚本生成完成！"
echo "============================================"
echo ""
echo "下一步：执行SQL脚本导入数据"
echo ""
echo "方式1: 使用MySQL命令行"
echo "  mysql -u root -p short_video_platform < $OUTPUT_SQL"
echo ""
echo "方式2: 在MySQL客户端中执行"
echo "  source $OUTPUT_SQL"
echo ""
