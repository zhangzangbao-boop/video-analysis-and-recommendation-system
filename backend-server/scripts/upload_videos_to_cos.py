#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
视频上传到腾讯云COS脚本
从JSON文件读取视频URL，下载视频和封面，上传到腾讯云COS，并生成更新后的SQL脚本

前置要求:
    pip install qcloud-python-sdk cos-python-sdk-v5 requests

使用方法:
    python upload_videos_to_cos.py <json_file_path> [--config <config_file>]
    
示例:
    python upload_videos_to_cos.py "f:/视频json文件/视频json文件/videos_pexels_food.json"
"""

import json
import os
import sys
import argparse
import glob
import tempfile
import requests
from datetime import datetime
from urllib.parse import urlparse
import yaml

try:
    from qcloud_cos import CosConfig
    from qcloud_cos import CosS3Client
except ImportError:
    print("错误: 未安装腾讯云COS SDK")
    print("请运行: pip install cos-python-sdk-v5")
    sys.exit(1)


def load_config_from_yml(yml_path='backend-server/src/main/resources/application.yml'):
    """
    从application.yml读取腾讯云COS配置
    """
    try:
        with open(yml_path, 'r', encoding='utf-8') as f:
            content = f.read()
            # 简单的YAML解析（只解析tencent.cos部分）
            config = {}
            in_tencent_section = False
            in_cos_section = False
            current_key = None
            
            for line in content.split('\n'):
                line = line.strip()
                if line.startswith('tencent:'):
                    in_tencent_section = True
                elif in_tencent_section and line.startswith('cos:'):
                    in_cos_section = True
                elif in_cos_section:
                    if ':' in line and not line.startswith('#'):
                        key, value = line.split(':', 1)
                        key = key.strip()
                        value = value.strip().strip('"').strip("'")
                        if key:
                            config[key] = value
                    elif not line or line.startswith('#'):
                        continue
                    else:
                        break
            
            return config
    except Exception as e:
        print(f"警告: 无法读取配置文件 {yml_path}: {e}")
        return {}


def download_file(url, output_path, max_retries=3):
    """
    下载文件到本地
    """
    for attempt in range(max_retries):
        try:
            response = requests.get(url, stream=True, timeout=30)
            response.raise_for_status()
            
            os.makedirs(os.path.dirname(output_path), exist_ok=True)
            
            with open(output_path, 'wb') as f:
                for chunk in response.iter_content(chunk_size=8192):
                    if chunk:
                        f.write(chunk)
            
            return True
        except Exception as e:
            if attempt < max_retries - 1:
                print(f"  下载失败，重试 {attempt + 1}/{max_retries}: {e}")
            else:
                print(f"  下载失败: {e}")
                return False
    return False


def upload_to_cos(client, bucket_name, local_file_path, cos_file_path, domain):
    """
    上传文件到腾讯云COS
    """
    try:
        # 上传文件
        with open(local_file_path, 'rb') as fp:
            response = client.put_object(
                Bucket=bucket_name,
                Body=fp,
                Key=cos_file_path,
                StorageClass='STANDARD',
                EnableMD5=False
            )
        
        # 返回完整的访问URL
        cos_url = f"{domain.rstrip('/')}/{cos_file_path}"
        return cos_url
    except Exception as e:
        print(f"  上传失败: {e}")
        return None


def get_file_extension(url):
    """
    从URL获取文件扩展名
    """
    parsed = urlparse(url)
    path = parsed.path
    if '.' in path:
        return os.path.splitext(path)[1]
    return '.mp4'  # 默认扩展名


def process_videos(json_file_path, cos_config, output_sql_path=None):
    """
    处理视频：下载并上传到COS
    """
    # 读取JSON文件
    try:
        with open(json_file_path, 'r', encoding='utf-8') as f:
            videos = json.load(f)
    except Exception as e:
        print(f"错误: 无法读取文件 '{json_file_path}': {e}")
        return []
    
    if not isinstance(videos, list):
        print(f"错误: JSON文件格式不正确，应为数组")
        return []
    
    # 初始化COS客户端
    secret_id = cos_config.get('secret-id', '')
    secret_key = cos_config.get('secret-key', '')
    region = cos_config.get('region', 'ap-guangzhou')
    bucket_name = cos_config.get('bucket-name', '')
    domain = cos_config.get('domain', '')
    video_folder = cos_config.get('video-folder', 'video/short_video/')
    cover_folder = cos_config.get('cover-folder', 'video/cover/')
    
    if not all([secret_id, secret_key, region, bucket_name, domain]):
        print("错误: 腾讯云COS配置不完整")
        print("请检查 application.yml 中的 tencent.cos 配置")
        return []
    
    config = CosConfig(Region=region, SecretId=secret_id, SecretKey=secret_key)
    client = CosS3Client(config)
    
    # 创建临时目录
    temp_dir = tempfile.mkdtemp(prefix='video_upload_')
    print(f"临时目录: {temp_dir}")
    
    # 生成SQL更新语句
    sql_statements = []
    sql_statements.append(f"-- 从文件 {os.path.basename(json_file_path)} 上传的视频数据")
    sql_statements.append(f"-- 生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    sql_statements.append("")
    sql_statements.append("USE `short_video_platform`;")
    sql_statements.append("")
    
    success_count = 0
    fail_count = 0
    
    for idx, video in enumerate(videos, 1):
        video_id = video.get('id')
        if not video_id:
            print(f"视频 {idx}: 跳过（无ID）")
            fail_count += 1
            continue
        
        print(f"\n处理视频 {idx}/{len(videos)}: ID={video_id}, 标题={video.get('title', 'N/A')}")
        
        # 下载视频
        video_url = video.get('videoUrl', '')
        if not video_url:
            print(f"  跳过：无视频URL")
            fail_count += 1
            continue
        
        video_ext = get_file_extension(video_url)
        video_local_path = os.path.join(temp_dir, f"video_{video_id}{video_ext}")
        
        print(f"  下载视频: {video_url}")
        if not download_file(video_url, video_local_path):
            print(f"  视频下载失败，跳过")
            fail_count += 1
            continue
        
        # 上传视频到COS
        video_cos_path = f"{video_folder.rstrip('/')}/video_{video_id}{video_ext}"
        print(f"  上传视频到COS: {video_cos_path}")
        cos_video_url = upload_to_cos(client, bucket_name, video_local_path, video_cos_path, domain)
        
        if not cos_video_url:
            print(f"  视频上传失败，跳过")
            fail_count += 1
            continue
        
        # 下载封面
        cover_url = video.get('coverUrl', '')
        cos_cover_url = None
        
        if cover_url:
            cover_ext = get_file_extension(cover_url)
            if not cover_ext or cover_ext == '.mp4':
                cover_ext = '.jpg'  # 默认图片扩展名
            
            cover_local_path = os.path.join(temp_dir, f"cover_{video_id}{cover_ext}")
            
            print(f"  下载封面: {cover_url}")
            if download_file(cover_url, cover_local_path):
                # 上传封面到COS
                cover_cos_path = f"{cover_folder.rstrip('/')}/cover_{video_id}{cover_ext}"
                print(f"  上传封面到COS: {cover_cos_path}")
                cos_cover_url = upload_to_cos(client, bucket_name, cover_local_path, cover_cos_path, domain)
        
        if not cos_cover_url:
            # 如果封面上传失败，使用原URL（可选）
            cos_cover_url = cover_url
            print(f"  警告: 封面上传失败，使用原URL")
        
        # 生成SQL UPDATE语句
        title = video.get('title', '').replace("'", "''")
        description = video.get('description', '').replace("'", "''")
        tags = video.get('tags', '').replace("'", "''")
        duration = video.get('duration', 0)
        
        sql = f"""UPDATE `video_info` 
SET `video_url` = '{cos_video_url}',
    `cover_url` = '{cos_cover_url}'
WHERE `id` = {video_id};

-- 如果视频不存在，使用以下INSERT语句:
-- INSERT INTO `video_info` (`id`, `user_id`, `title`, `description`, `video_url`, `cover_url`, `category_id`, `tags`, `duration`, `status`, `is_hot`, `view_count`, `like_count`, `comment_count`, `share_count`, `is_deleted`, `create_time`, `update_time`)
-- VALUES ({video_id}, 10000, '{title}', '{description}', '{cos_video_url}', '{cos_cover_url}', NULL, '{tags}', {duration}, 1, 0, 0, 0, 0, 0, 0, NOW(), NOW());
"""
        sql_statements.append(sql)
        
        print(f"  ✓ 成功: 视频URL已更新为 {cos_video_url}")
        success_count += 1
    
    # 清理临时文件
    import shutil
    try:
        shutil.rmtree(temp_dir)
        print(f"\n临时目录已清理: {temp_dir}")
    except:
        pass
    
    sql_statements.append("")
    sql_statements.append(f"-- 处理完成: 成功 {success_count} 个, 失败 {fail_count} 个")
    
    return sql_statements, success_count, fail_count


def main():
    parser = argparse.ArgumentParser(description='下载视频并上传到腾讯云COS')
    parser.add_argument('json_files', nargs='+', help='JSON文件路径（支持通配符）')
    parser.add_argument('--config', type=str, default='backend-server/src/main/resources/application.yml',
                       help='配置文件路径（默认: application.yml）')
    parser.add_argument('--output', '-o', type=str, default='uploaded_videos_update.sql',
                       help='输出SQL文件路径（默认: uploaded_videos_update.sql）')
    
    args = parser.parse_args()
    
    # 加载配置
    print("加载腾讯云COS配置...")
    cos_config = load_config_from_yml(args.config)
    
    if not cos_config.get('secret-id'):
        print("错误: 无法从配置文件中读取腾讯云COS配置")
        print("请确保 application.yml 中包含 tencent.cos 配置")
        sys.exit(1)
    
    print(f"  存储桶: {cos_config.get('bucket-name')}")
    print(f"  地域: {cos_config.get('region')}")
    print(f"  域名: {cos_config.get('domain')}")
    
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
    all_sql_statements.append("-- 视频上传到腾讯云COS后的URL更新SQL脚本")
    all_sql_statements.append(f"-- 生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    all_sql_statements.append("-- ==================================================================")
    all_sql_statements.append("")
    
    total_success = 0
    total_fail = 0
    
    for json_file in json_files:
        print(f"\n{'='*70}")
        print(f"处理文件: {json_file}")
        print(f"{'='*70}")
        
        sql_statements, success, fail = process_videos(json_file, cos_config)
        if sql_statements:
            all_sql_statements.extend(sql_statements)
            total_success += success
            total_fail += fail
    
    # 输出SQL文件
    sql_content = '\n'.join(all_sql_statements)
    
    with open(args.output, 'w', encoding='utf-8') as f:
        f.write(sql_content)
    
    print(f"\n{'='*70}")
    print(f"处理完成！")
    print(f"  成功: {total_success} 个视频")
    print(f"  失败: {total_fail} 个视频")
    print(f"  SQL文件: {args.output}")
    print(f"\n下一步: 执行SQL文件更新数据库")
    print(f"  mysql -u root -p short_video_platform < {args.output}")
    print(f"{'='*70}")


if __name__ == '__main__':
    main()
