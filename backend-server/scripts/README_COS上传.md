# 视频上传到腾讯云COS使用说明

## ⚠️ 重要说明

**当前情况**: JSON文件中的视频URL是Pexels的链接，这些视频**还没有**上传到您的腾讯云COS。

**解决方案**: 使用本脚本下载Pexels视频并上传到腾讯云COS。

## 前置要求

### 1. 安装Python依赖

```bash
pip install cos-python-sdk-v5 requests
```

### 2. 配置腾讯云COS

确保 `backend-server/src/main/resources/application.yml` 中已配置腾讯云COS：

```yaml
tencent:
  cos:
    secret-id: 你的SecretId
    secret-key: 你的SecretKey
    region: ap-guangzhou
    bucket-name: 你的存储桶名称
    domain: https://你的存储桶域名
    video-folder: video/short_video/
    cover-folder: video/cover/
```

## 使用方法

### 方式一：上传单个JSON文件

```bash
python backend-server/scripts/upload_videos_to_cos.py "f:/视频json文件/视频json文件/videos_pexels_food.json"
```

### 方式二：批量上传所有JSON文件

```bash
python backend-server/scripts/upload_videos_to_cos.py "f:/视频json文件/视频json文件/*.json"
```

### 方式三：指定输出文件

```bash
python backend-server/scripts/upload_videos_to_cos.py "f:/视频json文件/视频json文件/*.json" --output my_uploaded_videos.sql
```

## 工作流程

1. **读取JSON文件** - 解析视频信息
2. **下载视频** - 从Pexels下载视频文件到临时目录
3. **下载封面** - 从Pexels下载封面图片到临时目录
4. **上传到COS** - 将视频和封面上传到腾讯云COS
5. **生成SQL** - 生成UPDATE语句更新数据库中的URL

## 输出结果

脚本会生成一个SQL文件（默认：`uploaded_videos_update.sql`），包含：

```sql
UPDATE `video_info` 
SET `video_url` = 'https://your-bucket.cos.ap-guangzhou.myqcloud.com/video/short_video/video_5794445.mp4',
    `cover_url` = 'https://your-bucket.cos.ap-guangzhou.myqcloud.com/video/cover/cover_5794445.jpg'
WHERE `id` = 5794445;
```

## 执行SQL更新数据库

```bash
mysql -u root -p short_video_platform < uploaded_videos_update.sql
```

## 完整流程示例

```bash
# 1. 上传视频到COS（会下载并上传）
python backend-server/scripts/upload_videos_to_cos.py "f:/视频json文件/视频json文件/*.json"

# 2. 更新数据库URL
mysql -u root -p short_video_platform < uploaded_videos_update.sql

# 3. 验证数据
mysql -u root -p short_video_platform -e "SELECT id, title, video_url FROM video_info LIMIT 5;"
```

## 注意事项

1. **网络要求**: 需要能够访问Pexels和腾讯云COS
2. **存储空间**: 确保腾讯云COS有足够的存储空间
3. **下载时间**: 视频文件较大，下载可能需要较长时间
4. **费用**: 上传到COS会产生存储和流量费用
5. **临时文件**: 脚本会在临时目录下载文件，处理完成后自动清理

## 故障排除

### 问题1: 无法下载视频

**错误**: `下载失败: Connection timeout`

**解决**: 
- 检查网络连接
- 确保可以访问Pexels
- 尝试手动访问视频URL验证

### 问题2: COS上传失败

**错误**: `上传失败: AccessDenied`

**解决**: 
- 检查 `secret-id` 和 `secret-key` 是否正确
- 检查存储桶权限设置
- 确认API密钥有上传权限

### 问题3: 配置文件读取失败

**错误**: `无法读取配置文件`

**解决**: 
- 检查 `application.yml` 路径是否正确
- 使用 `--config` 参数指定配置文件路径

## 替代方案

如果不想上传到COS，也可以：

1. **直接使用Pexels URL**（如果项目允许外部URL）
   - 修改导入脚本，直接使用Pexels的URL
   - 注意：依赖外部服务，可能不稳定

2. **手动上传**
   - 在腾讯云COS控制台手动上传视频
   - 手动更新数据库URL

## 相关文档

- [视频导入工具说明](./README_视频导入.md)
- [腾讯云COS配置说明](../../腾讯云COS配置说明.md)
