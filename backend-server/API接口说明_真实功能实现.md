# API接口说明 - 真实功能实现

## 📋 概述

已实现真实的播放历史、点赞和评论功能，使用数据库表存储数据，不再使用虚假数据。

## 🎬 播放历史

### 记录播放历史
**接口**: `POST /api/v1/video/{videoId}/play`

**参数**:
- `videoId` (路径参数): 视频ID
- `duration` (可选): 观看时长（秒），默认0
- `progress` (可选): 播放进度（%），默认0
- `isFinish` (可选): 是否完播，默认false

**请求头**:
- `X-User-Id`: 用户ID（或通过JWT token获取）

**响应**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": null
}
```

**说明**: 
- 如果用户已观看过该视频，会更新现有记录
- 如果完播，会自动增加视频播放量

### 获取播放历史
**接口**: `GET /api/v1/video/history`

**参数**:
- `limit` (可选): 限制数量，默认20

**响应**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    {
      "id": 10001,
      "title": "视频标题",
      "videoUrl": "...",
      ...
    }
  ]
}
```

## 👍 点赞功能

### 点赞视频
**接口**: `POST /api/v1/video/{videoId}/like`

**参数**:
- `videoId` (路径参数): 视频ID

**响应**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": true
}
```

**说明**: 
- 如果已点赞，返回false
- 点赞成功后会更新视频的点赞数

### 取消点赞
**接口**: `DELETE /api/v1/video/{videoId}/like`

**响应**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": true
}
```

### 检查是否已点赞
**接口**: `GET /api/v1/video/{videoId}/like`

**响应**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": true
}
```

### 获取点赞列表
**接口**: `GET /api/v1/user/likes`

**参数**:
- `page` (可选): 页码
- `pageSize` (可选): 每页数量，默认20

**响应**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    {
      "id": 10001,
      "title": "视频标题",
      ...
    }
  ]
}
```

## 💬 评论功能

### 添加评论
**接口**: `POST /api/v1/video/{videoId}/comment`

**参数**:
- `videoId` (路径参数): 视频ID
- `content` (必需): 评论内容
- `parentId` (可选): 父评论ID（用于回复）
- `replyUserId` (可选): 被回复者ID

**响应**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": 20001,
    "videoId": 10001,
    "userId": 10000,
    "content": "评论内容",
    "parentId": 0,
    "likeCount": 0,
    "createTime": "2024-01-01T12:00:00"
  }
}
```

**说明**: 
- 添加评论后会自动更新视频的评论数
- `parentId`为0表示顶级评论

### 获取视频评论列表
**接口**: `GET /api/v1/video/{videoId}/comment`

**参数**:
- `videoId` (路径参数): 视频ID
- `limit` (可选): 限制数量，默认20

**响应**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    {
      "id": 20001,
      "videoId": 10001,
      "userId": 10000,
      "content": "评论内容",
      "parentId": 0,
      "likeCount": 5,
      "createTime": "2024-01-01T12:00:00"
    }
  ]
}
```

### 获取评论的回复列表
**接口**: `GET /api/v1/video/comment/{parentId}/replies`

**参数**:
- `parentId` (路径参数): 父评论ID
- `limit` (可选): 限制数量，默认10

**响应**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    {
      "id": 20002,
      "parentId": 20001,
      "content": "回复内容",
      ...
    }
  ]
}
```

### 删除评论
**接口**: `DELETE /api/v1/video/comment/{commentId}`

**参数**:
- `commentId` (路径参数): 评论ID

**响应**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": true
}
```

**说明**: 
- 只能删除自己的评论
- 删除后会自动更新视频的评论数

### 获取用户的评论列表
**接口**: `GET /api/v1/user/comments`

**参数**:
- `page` (可选): 页码
- `pageSize` (可选): 每页数量，默认20

**响应**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    {
      "id": 20001,
      "videoId": 10001,
      "content": "我的评论",
      ...
    }
  ]
}
```

## 📊 数据表说明

### video_play_record (播放历史表)
- `user_id`: 用户ID
- `video_id`: 视频ID
- `duration`: 观看时长（秒）
- `progress`: 播放进度（%）
- `is_finish`: 是否完播
- `create_time`: 观看时间

### video_interaction (互动记录表)
- `user_id`: 用户ID
- `video_id`: 视频ID
- `type`: 类型（1-点赞，2-收藏，3-转发）
- `create_time`: 操作时间

### video_comment (评论表)
- `id`: 评论ID（雪花算法）
- `video_id`: 视频ID
- `user_id`: 评论者ID
- `content`: 评论内容
- `parent_id`: 父评论ID（0表示顶级评论）
- `reply_user_id`: 被回复者ID
- `like_count`: 点赞数
- `status`: 状态（1-正常，0-待审）
- `is_deleted`: 是否删除
- `create_time`: 创建时间

## 🔄 自动更新

以下操作会自动更新相关统计：

1. **播放历史**:
   - 完播时自动增加视频播放量

2. **点赞**:
   - 点赞时增加视频点赞数
   - 取消点赞时减少视频点赞数

3. **评论**:
   - 添加评论时增加视频评论数
   - 删除评论时减少视频评论数

## ⚠️ 注意事项

1. 所有接口都需要用户登录（通过JWT token或X-User-Id请求头）
2. 未登录时部分接口会返回401错误
3. 数据存储在真实的数据库表中，不再是虚假数据
4. 所有操作都有事务保护，确保数据一致性
