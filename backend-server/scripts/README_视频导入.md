# 视频数据导入工具使用说明

## 概述

本工具用于将JSON格式的视频数据文件批量导入到MySQL数据库中。

## 文件说明

- `import_videos_from_json.py` - Python导入脚本（核心工具）
- `import_all_videos.bat` - Windows批处理脚本
- `import_all_videos.sh` - Linux/Mac Shell脚本

## 前置要求

1. **Python 3.x** - 必须安装Python 3.x
2. **MySQL数据库** - 数据库已创建并执行了 `database.sql`
3. **JSON文件** - 视频数据JSON文件

## JSON文件格式

JSON文件应为数组格式，每个视频对象包含以下字段：

```json
[
  {
    "id": 5794445,
    "title": "food创意瞬间 · 33s · 第1段",
    "description": "精彩视频",
    "videoUrl": "https://videos.pexels.com/video-files/...",
    "coverUrl": "https://images.pexels.com/videos/...",
    "duration": 33,
    "width": 1440,
    "height": 2560,
    "tags": "舞蹈,潮流,运动",
    "source": "pexels"
  }
]
```

## 分类映射

脚本会根据文件名自动识别视频分类：

| 文件名包含 | 分类ID | 分类名称 |
|-----------|--------|---------|
| `funny`   | 1      | 搞笑     |
| `life`    | 2      | 生活     |
| `tech`    | 3      | 科技     |
| `game`    | 4      | 游戏     |
| `food`    | 5      | 美食     |
| `pet`     | 6      | 萌宠     |

## 使用方法

### 方法一：使用批处理脚本（Windows）

1. 编辑 `import_all_videos.bat`，修改以下变量：
   ```bat
   set JSON_DIR=f:\视频json文件\视频json文件
   set USER_ID=10000
   set STATUS=1
   ```

2. 双击运行 `import_all_videos.bat`

3. 脚本会生成 `imported_videos.sql` 文件

4. 执行SQL文件导入数据：
   ```bash
   mysql -u root -p short_video_platform < backend-server\scripts\imported_videos.sql
   ```

### 方法二：使用Shell脚本（Linux/Mac）

1. 编辑 `import_all_videos.sh`，修改以下变量：
   ```bash
   JSON_DIR="f:/视频json文件/视频json文件"
   USER_ID=10000
   STATUS=1
   ```

2. 添加执行权限并运行：
   ```bash
   chmod +x import_all_videos.sh
   ./import_all_videos.sh
   ```

3. 执行SQL文件导入数据：
   ```bash
   mysql -u root -p short_video_platform < backend-server/scripts/imported_videos.sql
   ```

### 方法三：直接使用Python脚本

#### 导入单个文件

```bash
python backend-server/scripts/import_videos_from_json.py "f:/视频json文件/视频json文件/videos_pexels_food.json" --user-id 10000 --status 1 --output imported_videos.sql
```

#### 导入多个文件（使用通配符）

```bash
python backend-server/scripts/import_videos_from_json.py "f:/视频json文件/视频json文件/*.json" --user-id 10000 --status 1 --output imported_videos.sql
```

#### 导入多个文件（指定文件列表）

```bash
python backend-server/scripts/import_videos_from_json.py "file1.json" "file2.json" "file3.json" --user-id 10000 --status 1 --output imported_videos.sql
```

#### 参数说明

- `json_files` - JSON文件路径（支持通配符，必需）
- `--user-id` - 用户ID，默认为10000（admin用户）
- `--status` - 视频状态：0-待审核, 1-已发布, 2-驳回，默认为1
- `--output` / `-o` - 输出SQL文件路径，如果不指定则输出到控制台
- `--database` - 数据库名称，默认为 `short_video_platform`

## 示例

### 示例1：导入所有JSON文件并直接输出到控制台

```bash
python backend-server/scripts/import_videos_from_json.py "f:/视频json文件/视频json文件/*.json" --user-id 10000 --status 1
```

### 示例2：导入food分类的视频，状态设为待审核

```bash
python backend-server/scripts/import_videos_from_json.py "f:/视频json文件/视频json文件/videos_pexels_food.json" --user-id 10000 --status 0 --output food_videos.sql
```

### 示例3：导入后执行SQL

```bash
# 生成SQL文件
python backend-server/scripts/import_videos_from_json.py "f:/视频json文件/视频json文件/*.json" --user-id 10000 --status 1 --output imported_videos.sql

# 执行SQL导入数据
mysql -u root -p short_video_platform < imported_videos.sql
```

## 注意事项

1. **用户ID**: 默认使用10000（admin用户），如果您的admin用户ID不同，请修改 `--user-id` 参数
   - 可以通过以下SQL查询admin用户ID：
     ```sql
     SELECT id FROM sys_user WHERE username = 'admin';
     ```

2. **视频状态**: 
   - `0` - 待审核（需要管理员审核后才能发布）
   - `1` - 已发布（直接发布，用户可见）
   - `2` - 驳回（审核不通过）

3. **视频ID**: 
   - 如果JSON中的 `id` 字段存在且有效，将使用该ID
   - 否则会自动生成雪花算法ID

4. **分类识别**: 
   - 脚本根据文件名自动识别分类
   - 如果文件名不包含分类关键词，该文件将被跳过

5. **数据验证**: 
   - 导入前建议先查看生成的SQL文件
   - 确保视频URL和封面URL有效

## 故障排除

### 问题1：找不到Python

**错误信息**: `'python' 不是内部或外部命令`

**解决方法**: 
- 安装Python 3.x
- 或将Python添加到系统PATH环境变量
- 或使用完整路径：`C:\Python39\python.exe`

### 问题2：JSON文件格式错误

**错误信息**: `JSON文件格式不正确，应为数组`

**解决方法**: 
- 确保JSON文件是数组格式 `[...]`
- 检查JSON文件语法是否正确

### 问题3：无法识别分类

**警告信息**: `无法从文件名中识别分类`

**解决方法**: 
- 确保文件名包含分类关键词（funny, life, tech, game, food, pet）
- 或手动修改生成的SQL文件中的 `category_id`

### 问题4：数据库连接失败

**错误信息**: `Access denied for user 'root'@'localhost'`

**解决方法**: 
- 检查MySQL用户名和密码
- 确保数据库已创建：`CREATE DATABASE short_video_platform;`
- 确保已执行 `database.sql` 初始化表结构

## 数据验证

导入后，可以通过以下SQL验证数据：

```sql
-- 查看导入的视频总数
SELECT COUNT(*) as total_videos FROM video_info;

-- 按分类统计视频数量
SELECT 
    c.name as category_name,
    COUNT(v.id) as video_count
FROM video_info v
LEFT JOIN video_category c ON v.category_id = c.id
GROUP BY c.name
ORDER BY video_count DESC;

-- 查看最近导入的视频
SELECT id, title, category_id, status, create_time 
FROM video_info 
ORDER BY create_time DESC 
LIMIT 10;
```

## 技术支持

如有问题，请检查：
1. Python版本是否为3.x
2. JSON文件格式是否正确
3. 数据库连接配置是否正确
4. 表结构是否已正确创建
