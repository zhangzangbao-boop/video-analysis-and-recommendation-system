# 腾讯云COS配置验证清单

## ✅ 当前配置状态

您的配置已经正确填写：

```yaml
tencent:
  cos:
    secret-id: AKIDvhTH1ay97shLm01px22zouvUv9Cav7AD
    secret-key: 1sW0MacQ0bEj3DHDxtnW3Riq3AhCVfsz
    region: ap-guangzhou
    bucket-name: shortvideosystem-1396185685
    domain: https://shortvideosystem-1396185685.cos.ap-guangzhou.myqcloud.com
    access-control: PublicRead
    video-folder: video/short_video/
    cover-folder: video/cover/
```

## 📋 验证步骤

### 1. 确认存储桶存在（最重要！）

1. 登录 [腾讯云控制台](https://console.cloud.tencent.com/)
2. 进入 [对象存储控制台](https://console.cloud.tencent.com/cos)
3. 查找存储桶：`shortvideosystem-1396185685`
4. **如果不存在**，需要创建：
   - 点击"创建存储桶"
   - 名称：`shortvideosystem-1396185685`
   - 地域：`ap-guangzhou`（广州）
   - 访问权限：**公有读私有写**

### 2. 验证存储桶配置

在存储桶详情页检查：
- ✅ 存储桶名称：`shortvideosystem-1396185685`
- ✅ 所属地域：`ap-guangzhou`
- ✅ 访问权限：公有读私有写
- ✅ 访问域名：`https://shortvideosystem-1396185685.cos.ap-guangzhou.myqcloud.com`

### 3. 验证API密钥权限

1. 进入 [访问管理 - API密钥管理](https://console.cloud.tencent.com/cam/capi)
2. 确认 SecretId `AKIDvhTH1ay97shLm01px22zouvUv9Cav7AD` 存在
3. 确认该密钥有COS的读写权限

### 4. 重启后端服务（必须！）

配置修改后，**必须重启后端服务**才能生效：

```bash
# 停止当前服务（Ctrl+C）
# 然后重新启动
cd backend-server
mvn spring-boot:run
```

### 5. 检查启动日志

启动后端服务时，查看日志中是否有：
- ✅ COS客户端初始化成功的日志
- ❌ 如果有错误，查看具体错误信息

### 6. 测试上传功能

1. 启动前端服务：
   ```bash
   cd frontend-web
   npm run serve
   ```

2. 访问上传页面：
   - 登录后访问：`http://localhost:8080/main/profile`
   - 切换到"上传视频"标签页

3. 尝试上传一个小视频文件测试

4. 查看后端日志，确认：
   - ✅ 文件上传到COS成功
   - ✅ 返回的视频URL格式正确

## 🔍 常见问题排查

### 问题1：存储桶不存在

**错误信息：** `NoSuchBucket`

**解决方案：**
1. 在腾讯云控制台创建存储桶
2. 确保名称完全一致：`shortvideosystem-1396185685`
3. 确保地域一致：`ap-guangzhou`
4. 重启后端服务

### 问题2：权限不足

**错误信息：** `AccessDenied`

**解决方案：**
1. 检查存储桶访问权限是否为"公有读私有写"
2. 检查API密钥是否有COS读写权限
3. 在访问管理中为密钥添加COS权限

### 问题3：地域不匹配

**错误信息：** 连接超时或找不到存储桶

**解决方案：**
1. 确认存储桶实际地域
2. 更新 `application.yml` 中的 `region` 配置
3. 重启后端服务

### 问题4：配置未生效

**症状：** 仍然报错"存储桶不存在"

**解决方案：**
1. 确认已重启后端服务
2. 检查 `application.yml` 文件是否保存
3. 查看后端启动日志，确认COS客户端是否初始化

## ✅ 配置验证清单

完成以下检查项：

- [ ] 存储桶 `shortvideosystem-1396185685` 在腾讯云控制台存在
- [ ] 存储桶地域为 `ap-guangzhou`
- [ ] 存储桶访问权限为"公有读私有写"
- [ ] API密钥存在且有COS权限
- [ ] `application.yml` 配置已保存
- [ ] 后端服务已重启
- [ ] 后端启动日志显示COS客户端初始化成功
- [ ] 测试上传功能正常

## 🎯 下一步

配置验证完成后：

1. **测试视频上传**
   - 访问用户个人资料页面
   - 切换到"上传视频"标签页
   - 上传一个测试视频

2. **检查上传结果**
   - 查看后端日志，确认上传成功
   - 在腾讯云COS控制台查看文件是否已上传
   - 检查返回的视频URL是否可以访问

3. **测试视频播放**
   - 上传的视频需要管理员审核通过后才会显示
   - 可以在管理后台审核视频
   - 审核通过后，视频会出现在推荐列表中

## 📝 注意事项

1. **存储桶名称必须完全一致**（区分大小写）
2. **地域必须匹配**存储桶的实际地域
3. **配置修改后必须重启后端服务**
4. **上传的视频默认状态为待审核**，需要管理员审核通过
5. **文件大小限制**：当前配置为500MB（可在 `application.yml` 中调整）

---

如果所有检查项都完成，配置应该可以正常工作了！🎉
