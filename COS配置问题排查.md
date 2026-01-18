# 腾讯云COS配置问题排查指南

## 错误：NoSuchBucket（存储桶不存在）

如果遇到 `The specified bucket does not exist` 错误，请按以下步骤排查：

### 1. 检查存储桶是否存在

1. 登录 [腾讯云控制台](https://console.cloud.tencent.com/)
2. 进入 [对象存储控制台](https://console.cloud.tencent.com/cos)
3. 查看存储桶列表，确认存储桶 `shortvideosystem-1396185685` 是否存在

### 2. 检查存储桶名称配置

打开 `backend-server/src/main/resources/application.yml`，检查：

```yaml
tencent:
  cos:
    bucket-name: shortvideosystem-1396185685  # 必须与腾讯云控制台中的名称完全一致
```

**重要提示：**
- 存储桶名称必须与腾讯云控制台中的**完全一致**（区分大小写）
- 不要包含 `https://` 或域名部分，只需要存储桶名称

### 3. 检查存储桶地域配置

确认存储桶的地域与配置一致：

```yaml
tencent:
  cos:
    region: ap-guangzhou  # 必须与存储桶的实际地域一致
```

**常见地域代码：**
- `ap-guangzhou` - 广州
- `ap-beijing` - 北京
- `ap-shanghai` - 上海
- `ap-chengdu` - 成都

### 4. 检查API密钥权限

1. 进入 [访问管理 - API密钥管理](https://console.cloud.tencent.com/cam/capi)
2. 确认 SecretId 和 SecretKey 是否正确
3. 确认该密钥有访问COS的权限

### 5. 检查存储桶访问权限

1. 在COS控制台，进入存储桶详情
2. 检查"权限管理" -> "存储桶访问权限"
3. 建议设置为"公有读私有写"（PublicRead）

### 6. 验证配置

配置完成后，**必须重启后端服务**才能生效：

```bash
# 停止当前服务（Ctrl+C）
# 重新启动
cd backend-server
mvn spring-boot:run
```

### 7. 如果存储桶不存在

如果存储桶确实不存在，需要创建：

1. 进入 [对象存储控制台](https://console.cloud.tencent.com/cos)
2. 点击"创建存储桶"
3. 配置信息：
   - **名称**：`shortvideosystem-1396185685`（或您自定义的名称）
   - **地域**：选择 `ap-guangzhou`（广州）
   - **访问权限**：选择"公有读私有写"
4. 创建完成后，更新 `application.yml` 中的配置
5. 重启后端服务

### 8. 测试配置

创建测试文件验证配置：

```bash
# 在腾讯云控制台手动上传一个测试文件
# 然后检查是否能正常访问
```

### 9. 常见问题

#### 问题1：存储桶名称包含特殊字符
- **解决**：存储桶名称只能包含小写字母、数字和连字符（-）

#### 问题2：地域不匹配
- **解决**：确保 `region` 配置与存储桶实际地域一致

#### 问题3：API密钥权限不足
- **解决**：在访问管理中为密钥添加COS读写权限

#### 问题4：存储桶被删除
- **解决**：重新创建存储桶，或使用其他已存在的存储桶

### 10. 当前配置检查清单

- [ ] 存储桶 `shortvideosystem-1396185685` 在腾讯云控制台存在
- [ ] 存储桶地域为 `ap-guangzhou`
- [ ] `application.yml` 中的 `bucket-name` 与存储桶名称完全一致
- [ ] `application.yml` 中的 `region` 与存储桶地域一致
- [ ] `application.yml` 中的 `domain` 格式正确
- [ ] API密钥（SecretId/SecretKey）正确且有权限
- [ ] 后端服务已重启

### 11. 获取存储桶信息

如果存储桶存在但不确定配置，可以在COS控制台查看：

1. 进入存储桶详情页
2. 查看"基础配置"：
   - **存储桶名称**：复制到 `bucket-name`
   - **所属地域**：复制到 `region`
   - **访问域名**：复制到 `domain`（使用默认域名）

### 12. 联系支持

如果以上步骤都无法解决问题，请检查：
- 腾讯云账号是否正常
- 存储桶是否被暂停或删除
- 网络连接是否正常
- 查看后端日志获取详细错误信息

---

**配置示例（正确）：**

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
