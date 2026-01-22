#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
视频数据导入脚本
从JSON文件导入视频数据到MySQL数据库

使用方法:
    python import_videos_from_json.py <json_file_path> [--status <status>]

示例:
    python import_videos_from_json.py "f:/视频json文件/videos_pexels_food.json" --status 1
    # 注意：脚本现在会忽略 --user-id 参数，自动从定义的 ID 池中随机分配发布者
"""

import json
import os
import sys
import argparse
import glob
from datetime import datetime
import time
import random  # <--- [修改] 引入随机模块

# 分类映射：文件名前缀 -> category_id
# 建议根据你的数据库实际情况补充 anime, dance, fashion 等分类
CATEGORY_MAPPING = {
    'funny': 1,       # 搞笑
    'life': 2,        # 生活
    'tech': 3,        # 科技
    'game': 4,        # 游戏
    'food': 5,        # 美食
    'pet': 6,         # 萌宠
    'travel': 2,      # [新增] 旅行 -> 归为生活(示例)
    'sports': 2,      # [新增] 运动 -> 归为生活(示例)
    'music': 1,       # [新增] 音乐 -> 归为搞笑(示例，请根据实际情况修改)
    'konwledge': 3,   # [新增] 知识 -> 归为科技(示例)
    'films': 1,       # [新增] 影视
    'fashion': 2,     # [新增] 时尚
    'dance': 1,       # [新增] 舞蹈
    'anime': 1,       # [新增] 动漫
}

# [修改] 定义用户ID池 (10004 - 10022)
USER_ID_POOL = [
    10004, 10005, 10006, 10007, 10008, 10009, 10010, 10011, 10012,
    10013, 10014, 10015, 10016, 10017, 10018, 10019, 10020, 10021, 10022
]

# 默认用户ID (虽然会被随机逻辑覆盖，但保留作为参数默认值)
DEFAULT_USER_ID = 10000

# 默认状态：1-已发布，0-待审核
DEFAULT_STATUS = 1


def generate_snowflake_id():
    """
    生成雪花算法ID（简化版）
    使用时间戳 + 随机数的方式生成唯一ID
    """
    timestamp = int(time.time() * 1000)  # 毫秒时间戳
    random_part = int(time.time() * 1000000) % 1000000  # 随机部分
    return (timestamp << 20) | random_part


def get_category_id_from_filename(filename):
    """
    从文件名中提取分类ID
    例如: videos_pexels_food.json -> 5 (美食)
    """
    basename = os.path.basename(filename).lower()
    for prefix, category_id in CATEGORY_MAPPING.items():
        if prefix in basename:
            return category_id
    # 如果找不到，默认归类为 2 (生活) 或者返回 None 跳过
    # 这里修改为返回 2 (生活) 以防止新文件被跳过，你可以根据需要改为 return None
    return 2


def escape_sql_string(s):
    """
    转义SQL字符串中的特殊字符
    """
    if s is None:
        return 'NULL'
    return "'" + str(s).replace("'", "''").replace("\\", "\\\\") + "'"


def process_json_file(json_file_path, user_id_arg, status, output_file=None, use_external_url=False):
    """
    处理单个JSON文件，生成SQL INSERT语句
    """
    # 获取分类ID
    category_id = get_category_id_from_filename(json_file_path)
    if category_id is None:
        print(f"警告: 无法从文件名 '{json_file_path}' 中识别分类，跳过此文件")
        return []

    # 读取JSON文件
    try:
        with open(json_file_path, 'r', encoding='utf-8') as f:
            videos = json.load(f)
    except Exception as e:
        print(f"错误: 无法读取文件 '{json_file_path}': {e}")
        return []

    if not isinstance(videos, list):
        print(f"错误: JSON文件 '{json_file_path}' 格式不正确，应为数组")
        return []

    # 生成SQL语句
    sql_statements = []
    sql_statements.append(f"-- 从文件 {os.path.basename(json_file_path)} 导入的视频数据")
    sql_statements.append(f"-- 分类ID: {category_id}, 状态: {status}")
    sql_statements.append("")

    for video in videos:
        # [修改] 核心逻辑：从池中随机选择一个 User ID，忽略传入的 user_id_arg
        current_random_user_id = random.choice(USER_ID_POOL)

        # 使用JSON中的id作为视频ID（如果存在且有效），否则生成新的ID
        video_id = video.get('id')
        if video_id is None or video_id <= 0:
            video_id = generate_snowflake_id()

        # 提取视频信息
        title = video.get('title', '未命名视频')
        description = video.get('description', '精彩视频')
        video_url = video.get('videoUrl', '')
        cover_url = video.get('coverUrl', '')
        duration = video.get('duration', 0)
        tags = video.get('tags', '')

        # 如果使用外部URL，直接使用；否则提示需要上传到COS
        if not use_external_url and ('pexels.com' in video_url or 'pexels.com' in cover_url):
            sql_statements.append(f"-- 注意: 此视频URL来自Pexels，需要上传到腾讯云COS")
            sql_statements.append(f"-- 视频ID: {video_id}, 原URL: {video_url}")

        # 构建SQL INSERT语句
        # 注意: VALUES 中的 user_id 已经替换为 current_random_user_id
        sql = f"""INSERT INTO `video_info` (
    `id`, `user_id`, `title`, `description`, `video_url`, `cover_url`,
    `category_id`, `tags`, `duration`, `status`, `is_hot`,
    `view_count`, `like_count`, `comment_count`, `share_count`,
    `is_deleted`, `create_time`, `update_time`
) VALUES (
    {video_id},
    {current_random_user_id},
    {escape_sql_string(title)},
    {escape_sql_string(description)},
    {escape_sql_string(video_url)},
    {escape_sql_string(cover_url)},
    {category_id},
    {escape_sql_string(tags)},
    {duration},
    {status},
    0,
    0,
    0,
    0,
    0,
    0,
    NOW(),
    NOW()
);"""
        sql_statements.append(sql)

    sql_statements.append("")
    return sql_statements


def main():
    parser = argparse.ArgumentParser(description='从JSON文件导入视频数据到MySQL数据库')
    parser.add_argument('json_files', nargs='+', help='JSON文件路径（支持通配符）')
    parser.add_argument('--user-id', type=int, default=DEFAULT_USER_ID,
                       help=f'用户ID（注意：此参数现已被脚本内的随机逻辑覆盖）')
    parser.add_argument('--status', type=int, default=DEFAULT_STATUS, choices=[0, 1, 2],
                       help='视频状态：0-待审核, 1-已发布, 2-驳回（默认: 1）')
    parser.add_argument('--output', '-o', type=str, default=None,
                       help='输出SQL文件路径（默认: 输出到控制台）')
    parser.add_argument('--database', type=str, default='short_video_platform',
                       help='数据库名称（默认: short_video_platform）')
    parser.add_argument('--use-external-url', action='store_true',
                       help='直接使用外部URL（如Pexels），不提示上传到COS（默认: False）')

    args = parser.parse_args()

    # 展开通配符
    json_files = []
    for pattern in args.json_files:
        matched_files = glob.glob(pattern)
        if matched_files:
            json_files.extend(matched_files)
        else:
            if os.path.exists(pattern):
                json_files.append(pattern)
            else:
                print(f"警告: 文件或模式 '{pattern}' 不存在，跳过")

    if not json_files:
        print("错误: 没有找到任何JSON文件")
        sys.exit(1)

    # 处理所有JSON文件
    all_sql_statements = []
    all_sql_statements.append("-- ==================================================================")
    all_sql_statements.append("-- 视频数据导入SQL脚本")
    all_sql_statements.append(f"-- 生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    all_sql_statements.append(f"-- 视频状态: {args.status}")
    all_sql_statements.append(f"-- 说明: 发布者ID已随机化 (10004-10022)")
    all_sql_statements.append("-- ==================================================================")
    all_sql_statements.append("")
    all_sql_statements.append(f"USE `{args.database}`;")
    all_sql_statements.append("")
    all_sql_statements.append("SET NAMES utf8mb4;")
    all_sql_statements.append("")

    total_videos = 0
    for json_file in json_files:
        print(f"处理文件: {json_file}")
        # 调用处理函数
        sql_statements = process_json_file(json_file, args.user_id, args.status,
                                          use_external_url=args.use_external_url)
        if sql_statements:
            all_sql_statements.extend(sql_statements)
            # 统计视频数量（排除注释行）
            video_count = len([s for s in sql_statements if s.strip().startswith('INSERT')])
            total_videos += video_count
            print(f"  ✓ 成功处理 {video_count} 个视频")

    all_sql_statements.append("-- ==================================================================")
    all_sql_statements.append(f"-- 导入完成，共 {total_videos} 个视频")
    all_sql_statements.append("-- ==================================================================")

    # 输出SQL语句
    sql_content = '\n'.join(all_sql_statements)

    if args.output:
        # 输出到文件
        with open(args.output, 'w', encoding='utf-8') as f:
            f.write(sql_content)
        print(f"\n✓ SQL脚本已生成: {args.output}")
        print(f"  共 {total_videos} 个视频")
        print(f"\n执行方式:")
        print(f"  mysql -u root -p {args.database} < {args.output}")
    else:
        # 输出到控制台
        print("\n" + "="*70)
        print("生成的SQL语句:")
        print("="*70)
        print(sql_content)
        print("="*70)
        print(f"\n共 {total_videos} 个视频")


if __name__ == '__main__':
    main()