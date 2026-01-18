# 腾讯云COS配置说明

为了让网站能够从腾讯云COS推荐真实的视频，需要完成以下配置：

## 1. 后端配置（application.yml）

在 `backend-server/src/main/resources/application.yml` 文件中已添加腾讯云COS配置，请按以下步骤配置：

### 1.1 获取腾讯云API密钥

1. 登录 [腾讯云控制台](https://console.cloud.tencent.com/)
2. 进入 [访问管理 - API密钥管理](https://console.cloud.tencent.com/cam/capi)
3. 创建或查看您的 SecretId 和 SecretKey

### 1.2 创建COS存储桶

1. 进入 [对象存储控制台](https://console.cloud.tencent.com/cos)
2. 点击"创建存储桶"
3. 选择地域（如：广州 ap-guangzhou）
4. 设置存储桶名称（如：my-video-bucket）
5. 设置访问权限为"公有读私有写"（重要！）

### 1.3 配置application.yml

修改 `backend-server/src/main/resources/application.yml` 中的以下配置：

```yaml
tencent:
  cos:
    secret-id: 你的SecretId          # 替换为实际的SecretId
    secret-key: 你的SecretKey        # 替换为实际的SecretKey
    region: ap-guangzhou             # 替换为你的存储桶地域
    bucket-name: my-video-bucket     # 替换为你的存储桶名称
    domain: https://my-video-bucket.cos.ap-guangzhou.myqcloud.com  # 替换为你的存储桶访问域名
```

**存储桶访问域名格式：**
```
https://{bucket-name}.cos.{region}.myqcloud.com
```

例如：
- 存储桶名：my-video-bucket
- 地域：ap-guangzhou（广州）
- 域名：https://my-video-bucket.cos.ap-guangzhou.myqcloud.com

## 2. 数据库配置

### 2.1 确保数据库中有视频数据

确保 `video_info` 表中已有视频记录，且：
- `video_url` 字段存储的是腾讯云COS的完整URL（如：`https://xxx.cos.ap-guangzhou.myqcloud.com/video/xxx.mp4`）
- `cover_url` 字段存储的是封面图片的完整URL（如：`https://xxx.cos.ap-guangzhou.myqcloud.com/cover/xxx.jpg`）
- `status` 字段为 `1`（PASSED，已审核通过）

### 2.2 上传视频到腾讯云COS

有两种方式：

#### 方式一：通过管理后台上传（推荐）

1. 在管理后台的视频管理页面
2. 上传视频和封面
3. 系统会自动上传到腾讯云COS并保存URL到数据库

#### 方式二：直接上传到COS

1. 在腾讯云COS控制台手动上传视频文件
2. 将文件URL手动插入到数据库

**示例SQL：**
```sql
INSERT INTO video_info (title, description, video_url, cover_url, author_id, duration, status, create_time) 
VALUES (
    '示例视频',
    '视频描述',
    'https://my-bucket.cos.ap-guangzhou.myqcloud.com/video/example.mp4',
    'https://my-bucket.cos.ap-guangzhou.myqcloud.com/cover/example.jpg',
    1,
    120,
    1,
    NOW()
);
```

## 3. 前端配置

前端已经配置完成，会自动调用后端API获取视频数据。

确保：
- 后端服务运行在 `http://localhost:8090`
- 前端开发服务器的 `VUE_APP_BASE_API` 配置正确（见 `.env` 文件）

## 4. 测试步骤

1. **配置完成后重启后端服务**
   ```bash
   cd backend-server
   mvn spring-boot:run
   ```

2. **启动前端开发服务器**
   ```bash
   cd frontend-web
   npm run serve
   ```

3. **访问前端页面**
   - 访问 `http://localhost:8080/main/video`
   - 应该能看到从腾讯云COS加载的真实视频

4. **检查网络请求**
   - 打开浏览器开发者工具
   - 查看 Network 标签
   - 应该能看到请求 `/api/v1/video/recommend` 和 `/api/v1/video/hot`
   - 返回的数据中的 `videoUrl` 和 `coverUrl` 应该是腾讯云COS的URL

## 5. 常见问题

### 5.1 视频无法播放

- **检查视频URL是否正确**：确保数据库中的 `video_url` 是完整的腾讯云COS URL
- **检查存储桶权限**：确保存储桶设置为"公有读私有写"
- **检查CORS配置**：在COS控制台配置CORS规则，允许前端域名访问

### 5.2 403 Forbidden 错误

- 检查 `secret-id` 和 `secret-key` 是否正确
- 检查存储桶权限设置
- 检查文件路径是否正确

### 5.3 404 Not Found 错误

- 检查视频文件是否真的存在于COS存储桶中
- 检查 `domain` 配置是否正确
- 检查 `video-folder` 和 `cover-folder` 配置

### 5.4 前端显示"暂无推荐视频"

- 检查数据库中是否有 `status=1`（已审核通过）的视频
- 检查后端API是否正常返回数据
- 查看浏览器控制台和网络请求的错误信息

## 6. CORS配置（重要！）

为了让前端能够直接访问COS上的视频，需要在腾讯云COS控制台配置CORS：

1. 进入COS控制台 -> 存储桶 -> 安全管理 -> 跨域访问CORS设置
2. 添加规则：
   - **允许的Origin**：`http://localhost:8080`（开发环境）或你的前端域名（生产环境）
   - **允许的方法**：GET, HEAD
   - **允许的Header**：`*`
   - **Expose-Headers**：`ETag, Content-Length`
   - **Max-Age**：600

## 7. 视频格式建议

- **推荐格式**：MP4 (H.264编码)
- **分辨率**：建议720p或1080p
- **文件大小**：建议单个视频不超过100MB（可在application.yml中调整 `multipart.max-file-size`）

## 完成检查清单

- [ ] 配置了腾讯云API密钥（secret-id, secret-key）
- [ ] 创建了COS存储桶并设置了正确的地域和权限
- [ ] 在application.yml中配置了所有COS参数
- [ ] 数据库中有视频数据，且video_url和cover_url指向腾讯云COS
- [ ] 配置了COS的CORS规则
- [ ] 重启了后端服务
- [ ] 前端能够正常加载和播放视频

配置完成后，您的网站就能从腾讯云COS推荐和播放真实的视频了！
