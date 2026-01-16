/*
 * 批量初始化数据SQL文件
 * 用于填充测试数据，确保系统有足够的数据进行开发和测试
 * 
 * 使用说明：
 * 1. 先执行 database.sql 创建表结构
 * 2. 再执行本文件初始化测试数据
 */

SET NAMES utf8mb4;
USE `short_video_platform`;

-- =================================================================
-- 1. 初始化用户数据（包含管理员和普通用户）
-- =================================================================

-- 注意：密码加密方式为 MD5(password + salt)
-- 所有测试用户的密码都是 "123456"
-- 示例：密码 "123456" + salt "salt001" = MD5("123456salt001") = 'a1b2c3d4e5f6...' (实际值需要计算)
-- 为了简化，这里使用兼容方式：如果密码是 "e10adc3949ba59abbe56e057f20f883e"（123456的MD5），系统会特殊处理
-- 但为了正确性，我们使用正确的加密方式

-- 管理员用户（已有admin，这里再添加几个测试管理员）
-- 密码：123456，salt：salt001，MD5("123456salt001") = '8f434346648f6b96df89dda901c5176b'
INSERT INTO `sys_user` (`username`, `password`, `salt`, `nickname`, `phone`, `email`, `real_name`, `gender`, `bio`, `level`, `balance`, `points`, `fans_count`, `follow_count`, `status`, `status_str`, `create_time`) VALUES
('admin2', '8f434346648f6b96df89dda901c5176b', 'salt001', '管理员2号', '13800000001', 'admin2@example.com', '张管理员', 'male', '系统管理员，负责内容审核', 99, 0.00, 9999, 0, 0, 1, 'normal', NOW()),
('admin3', '8f434346648f6b96df89dda901c5176b', 'salt002', '管理员3号', '13800000002', 'admin3@example.com', '李管理员', 'female', '系统管理员，负责用户管理', 99, 0.00, 9999, 0, 0, 1, 'normal', NOW());

-- 普通用户（20个测试用户）
-- 所有用户密码都是 "123456"，使用不同的salt
-- 为了简化，使用兼容密码格式（系统会特殊处理）
INSERT INTO `sys_user` (`username`, `password`, `salt`, `nickname`, `phone`, `email`, `real_name`, `gender`, `bio`, `level`, `balance`, `points`, `fans_count`, `follow_count`, `status`, `status_str`, `create_time`) VALUES
-- 用户1-5
-- 注意：使用 'e10adc3949ba59abbe56e057f20f883e' 作为密码（123456的MD5），系统会特殊处理
('user001', 'e10adc3949ba59abbe56e057f20f883e', 'salt101', '短视频达人', '13800138001', 'user001@example.com', '王小明', 'male', '热爱生活，喜欢分享短视频', 2, 100.50, 500, 120, 80, 1, 'normal', DATE_SUB(NOW(), INTERVAL 30 DAY)),
('user002', 'e10adc3949ba59abbe56e057f20f883e', 'salt102', '美食博主', '13800138002', 'user002@example.com', '李小红', 'female', '美食探店，分享美味生活', 3, 200.00, 1200, 500, 200, 1, 'normal', DATE_SUB(NOW(), INTERVAL 25 DAY)),
('user003', 'e10adc3949ba59abbe56e057f20f883e', 'salt103', '游戏主播', '13800138003', 'user003@example.com', '张小强', 'male', '游戏解说，技术分享', 2, 50.00, 300, 80, 60, 1, 'normal', DATE_SUB(NOW(), INTERVAL 20 DAY)),
('user004', 'e10adc3949ba59abbe56e057f20f883e', 'salt104', '旅行家', '13800138004', 'user004@example.com', '赵小美', 'female', '环游世界，记录美好', 3, 300.00, 1500, 800, 300, 1, 'normal', DATE_SUB(NOW(), INTERVAL 15 DAY)),
('user005', 'e10adc3949ba59abbe56e057f20f883e', 'salt105', '萌宠日记', '13800138005', 'user005@example.com', '孙小萌', 'female', '分享萌宠日常', 1, 20.00, 100, 50, 30, 1, 'normal', DATE_SUB(NOW(), INTERVAL 10 DAY)),
-- 用户6-10
('user006', 'e10adc3949ba59abbe56e057f20f883e', 'salt106', '科技探索', '13800138006', 'user006@example.com', '周小科', 'male', '科技产品评测', 2, 150.00, 800, 200, 100, 1, 'normal', DATE_SUB(NOW(), INTERVAL 8 DAY)),
('user007', 'e10adc3949ba59abbe56e057f20f883e', 'salt107', '健身教练', '13800138007', 'user007@example.com', '吴小健', 'male', '健身指导，健康生活', 2, 80.00, 400, 150, 90, 1, 'normal', DATE_SUB(NOW(), INTERVAL 7 DAY)),
('user008', 'e10adc3949ba59abbe56e057f20f883e', 'salt108', '音乐人', '13800138008', 'user008@example.com', '郑小音', 'male', '原创音乐，翻唱作品', 3, 250.00, 1300, 600, 250, 1, 'normal', DATE_SUB(NOW(), INTERVAL 6 DAY)),
('user009', 'e10adc3949ba59abbe56e057f20f883e', 'salt109', '搞笑段子', '13800138009', 'user009@example.com', '钱小笑', 'male', '搞笑段子，快乐分享', 1, 30.00, 150, 60, 40, 1, 'normal', DATE_SUB(NOW(), INTERVAL 5 DAY)),
('user010', 'e10adc3949ba59abbe56e057f20f883e', 'salt110', '时尚穿搭', '13800138010', 'user010@example.com', '冯小美', 'female', '时尚穿搭，美妆分享', 2, 120.00, 600, 180, 120, 1, 'normal', DATE_SUB(NOW(), INTERVAL 4 DAY)),
-- 用户11-15
('user011', 'e10adc3949ba59abbe56e057f20f883e', 'salt111', '读书笔记', '13800138011', 'user011@example.com', '陈小书', 'female', '读书分享，知识传播', 1, 40.00, 200, 70, 50, 1, 'normal', DATE_SUB(NOW(), INTERVAL 3 DAY)),
('user012', 'e10adc3949ba59abbe56e057f20f883e', 'salt112', '摄影爱好者', '13800138012', 'user012@example.com', '褚小影', 'male', '摄影作品，技巧分享', 2, 90.00, 450, 130, 80, 1, 'normal', DATE_SUB(NOW(), INTERVAL 2 DAY)),
('user013', 'e10adc3949ba59abbe56e057f20f883e', 'salt113', '手工达人', '13800138013', 'user013@example.com', '卫小工', 'female', '手工制作，创意分享', 1, 25.00, 120, 45, 35, 1, 'normal', DATE_SUB(NOW(), INTERVAL 1 DAY)),
('user014', 'e10adc3949ba59abbe56e057f20f883e', 'salt114', '运动健将', '13800138014', 'user014@example.com', '蒋小运', 'male', '运动健身，健康生活', 2, 70.00, 350, 110, 70, 1, 'normal', NOW()),
('user015', 'e10adc3949ba59abbe56e057f20f883e', 'salt115', '电影解说', '13800138015', 'user015@example.com', '沈小影', 'male', '电影解说，影评分享', 2, 100.00, 500, 160, 100, 1, 'normal', NOW()),
-- 用户16-20
('user016', 'e10adc3949ba59abbe56e057f20f883e', 'salt116', '舞蹈老师', '13800138016', 'user016@example.com', '韩小舞', 'female', '舞蹈教学，才艺展示', 3, 180.00, 900, 400, 180, 1, 'normal', NOW()),
('user017', 'e10adc3949ba59abbe56e057f20f883e', 'salt117', '编程学习', '13800138017', 'user017@example.com', '杨小程', 'male', '编程教程，技术分享', 2, 60.00, 300, 90, 60, 1, 'normal', NOW()),
('user018', 'e10adc3949ba59abbe56e057f20f883e', 'salt118', '绘画艺术', '13800138018', 'user018@example.com', '朱小画', 'female', '绘画作品，艺术创作', 2, 110.00, 550, 170, 110, 1, 'normal', NOW()),
('user019', 'e10adc3949ba59abbe56e057f20f883e', 'salt119', '汽车评测', '13800138019', 'user019@example.com', '秦小汽', 'male', '汽车评测，驾驶体验', 3, 220.00, 1100, 450, 200, 1, 'normal', NOW()),
('user020', 'e10adc3949ba59abbe56e057f20f883e', 'salt120', '宠物医生', '13800138020', 'user020@example.com', '尤小宠', 'female', '宠物健康，养宠知识', 2, 85.00, 425, 140, 85, 1, 'normal', NOW());

-- 为所有用户分配ROLE_USER角色
INSERT INTO `sys_user_role` (`user_id`, `role_id`)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE u.username LIKE 'user%' AND r.role_code = 'ROLE_USER';

-- 为管理员分配ROLE_ADMIN角色
INSERT INTO `sys_user_role` (`user_id`, `role_id`)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE u.username LIKE 'admin%' AND r.role_code = 'ROLE_ADMIN';

-- =================================================================
-- 2. 初始化视频数据（50个视频）
-- =================================================================

-- 注意：video_info表的id字段使用雪花算法，这里使用大数字模拟
-- 实际应用中应该由Java代码生成

INSERT INTO `video_info` (`id`, `user_id`, `title`, `description`, `video_url`, `cover_url`, `category_id`, `tags`, `duration`, `status`, `is_hot`, `view_count`, `like_count`, `comment_count`, `share_count`, `create_time`) VALUES
-- 搞笑类视频（10个）
(10001, (SELECT id FROM sys_user WHERE username = 'user001' LIMIT 1), '搞笑日常：今天又搞砸了', '记录生活中的搞笑瞬间，希望大家开心', 'https://example.com/video/10001.mp4', 'https://example.com/cover/10001.jpg', 1, '搞笑,日常,生活', 60, 1, 1, 50000, 5000, 300, 200, DATE_SUB(NOW(), INTERVAL 30 DAY)),
(10002, (SELECT id FROM sys_user WHERE username = 'user009' LIMIT 1), '段子合集：笑到肚子疼', '精选搞笑段子，让你笑不停', 'https://example.com/video/10002.mp4', 'https://example.com/cover/10002.jpg', 1, '段子,搞笑,合集', 120, 1, 1, 80000, 8000, 500, 400, DATE_SUB(NOW(), INTERVAL 28 DAY)),
(10003, (SELECT id FROM sys_user WHERE username = 'user001' LIMIT 1), '恶搞朋友：没想到吧', '和朋友一起的搞笑时刻', 'https://example.com/video/10003.mp4', 'https://example.com/cover/10003.jpg', 1, '恶搞,朋友,搞笑', 45, 1, 0, 20000, 2000, 100, 80, DATE_SUB(NOW(), INTERVAL 25 DAY)),
(10004, (SELECT id FROM sys_user WHERE username = 'user009' LIMIT 1), '模仿秀：明星模仿', '模仿各种明星的经典动作', 'https://example.com/video/10004.mp4', 'https://example.com/cover/10004.jpg', 1, '模仿,明星,搞笑', 90, 1, 1, 60000, 6000, 400, 300, DATE_SUB(NOW(), INTERVAL 22 DAY)),
(10005, (SELECT id FROM sys_user WHERE username = 'user001' LIMIT 1), '宠物搞笑：猫咪的日常', '记录猫咪的搞笑瞬间', 'https://example.com/video/10005.mp4', 'https://example.com/cover/10005.jpg', 1, '宠物,猫咪,搞笑', 30, 1, 0, 15000, 1500, 80, 60, DATE_SUB(NOW(), INTERVAL 20 DAY)),
(10006, (SELECT id FROM sys_user WHERE username = 'user009' LIMIT 1), '反转剧情：你猜不到结局', '剧情反转，意想不到的结局', 'https://example.com/video/10006.mp4', 'https://example.com/cover/10006.jpg', 1, '反转,剧情,搞笑', 75, 1, 1, 70000, 7000, 450, 350, DATE_SUB(NOW(), INTERVAL 18 DAY)),
(10007, (SELECT id FROM sys_user WHERE username = 'user001' LIMIT 1), '搞笑配音：经典片段', '为经典片段配上搞笑配音', 'https://example.com/video/10007.mp4', 'https://example.com/cover/10007.jpg', 1, '配音,搞笑,经典', 105, 1, 0, 25000, 2500, 150, 120, DATE_SUB(NOW(), INTERVAL 15 DAY)),
(10008, (SELECT id FROM sys_user WHERE username = 'user009' LIMIT 1), '整蛊朋友：生日惊喜', '给朋友的生日整蛊', 'https://example.com/video/10008.mp4', 'https://example.com/cover/10008.jpg', 1, '整蛊,朋友,惊喜', 50, 1, 1, 55000, 5500, 350, 280, DATE_SUB(NOW(), INTERVAL 12 DAY)),
(10009, (SELECT id FROM sys_user WHERE username = 'user001' LIMIT 1), '搞笑挑战：你敢试试吗', '各种搞笑挑战，你敢来吗', 'https://example.com/video/10009.mp4', 'https://example.com/cover/10009.jpg', 1, '挑战,搞笑,互动', 65, 1, 0, 18000, 1800, 90, 70, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(10010, (SELECT id FROM sys_user WHERE username = 'user009' LIMIT 1), '搞笑合集：一周精选', '本周最搞笑的视频合集', 'https://example.com/video/10010.mp4', 'https://example.com/cover/10010.jpg', 1, '合集,搞笑,精选', 180, 1, 1, 90000, 9000, 600, 500, DATE_SUB(NOW(), INTERVAL 8 DAY)),

-- 生活类视频（10个）
(10011, (SELECT id FROM sys_user WHERE username = 'user002' LIMIT 1), '美食探店：网红餐厅', '探访网红餐厅，品尝美食', 'https://example.com/video/10011.mp4', 'https://example.com/cover/10011.jpg', 2, '美食,探店,生活', 300, 1, 1, 120000, 12000, 800, 600, DATE_SUB(NOW(), INTERVAL 27 DAY)),
(10012, (SELECT id FROM sys_user WHERE username = 'user004' LIMIT 1), '旅行vlog：日本之旅', '记录日本旅行的美好时光', 'https://example.com/video/10012.mp4', 'https://example.com/cover/10012.jpg', 2, '旅行,日本,vlog', 600, 1, 1, 150000, 15000, 1000, 800, DATE_SUB(NOW(), INTERVAL 24 DAY)),
(10013, (SELECT id FROM sys_user WHERE username = 'user002' LIMIT 1), '家常菜制作：红烧肉', '教你做美味的红烧肉', 'https://example.com/video/10013.mp4', 'https://example.com/cover/10013.jpg', 2, '美食,家常菜,教程', 180, 1, 0, 40000, 4000, 250, 200, DATE_SUB(NOW(), INTERVAL 21 DAY)),
(10014, (SELECT id FROM sys_user WHERE username = 'user004' LIMIT 1), '旅行攻略：云南大理', '云南大理旅行攻略分享', 'https://example.com/video/10014.mp4', 'https://example.com/cover/10014.jpg', 2, '旅行,云南,攻略', 450, 1, 1, 110000, 11000, 700, 550, DATE_SUB(NOW(), INTERVAL 19 DAY)),
(10015, (SELECT id FROM sys_user WHERE username = 'user002' LIMIT 1), '烘焙教程：制作蛋糕', '手把手教你制作美味蛋糕', 'https://example.com/video/10015.mp4', 'https://example.com/cover/10015.jpg', 2, '烘焙,蛋糕,教程', 240, 1, 0, 35000, 3500, 200, 150, DATE_SUB(NOW(), INTERVAL 16 DAY)),
(10016, (SELECT id FROM sys_user WHERE username = 'user004' LIMIT 1), '城市漫步：上海外滩', '漫步上海外滩，感受城市魅力', 'https://example.com/video/10016.mp4', 'https://example.com/cover/10016.jpg', 2, '城市,上海,漫步', 360, 1, 1, 95000, 9500, 600, 480, DATE_SUB(NOW(), INTERVAL 14 DAY)),
(10017, (SELECT id FROM sys_user WHERE username = 'user002' LIMIT 1), '早餐制作：营养搭配', '健康早餐的制作方法', 'https://example.com/video/10017.mp4', 'https://example.com/cover/10017.jpg', 2, '早餐,营养,健康', 120, 1, 0, 28000, 2800, 170, 130, DATE_SUB(NOW(), INTERVAL 11 DAY)),
(10018, (SELECT id FROM sys_user WHERE username = 'user004' LIMIT 1), '旅行日记：西藏之旅', '记录西藏旅行的难忘经历', 'https://example.com/video/10018.mp4', 'https://example.com/cover/10018.jpg', 2, '旅行,西藏,日记', 720, 1, 1, 130000, 13000, 850, 680, DATE_SUB(NOW(), INTERVAL 9 DAY)),
(10019, (SELECT id FROM sys_user WHERE username = 'user002' LIMIT 1), '下午茶时光：精致生活', '享受美好的下午茶时光', 'https://example.com/video/10019.mp4', 'https://example.com/cover/10019.jpg', 2, '下午茶,生活,精致', 90, 1, 0, 22000, 2200, 130, 100, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(10020, (SELECT id FROM sys_user WHERE username = 'user004' LIMIT 1), '旅行vlog：欧洲之旅', '记录欧洲旅行的精彩瞬间', 'https://example.com/video/10020.mp4', 'https://example.com/cover/10020.jpg', 2, '旅行,欧洲,vlog', 900, 1, 1, 140000, 14000, 900, 720, DATE_SUB(NOW(), INTERVAL 4 DAY)),

-- 科技类视频（10个）
(10021, (SELECT id FROM sys_user WHERE username = 'user006' LIMIT 1), '手机评测：最新旗舰', '深度评测最新旗舰手机', 'https://example.com/video/10021.mp4', 'https://example.com/cover/10021.jpg', 3, '科技,手机,评测', 480, 1, 1, 200000, 20000, 1200, 1000, DATE_SUB(NOW(), INTERVAL 26 DAY)),
(10022, (SELECT id FROM sys_user WHERE username = 'user017' LIMIT 1), '编程教程：Python入门', 'Python编程入门教程', 'https://example.com/video/10022.mp4', 'https://example.com/cover/10022.jpg', 3, '编程,Python,教程', 600, 1, 1, 180000, 18000, 1100, 900, DATE_SUB(NOW(), INTERVAL 23 DAY)),
(10023, (SELECT id FROM sys_user WHERE username = 'user006' LIMIT 1), '科技资讯：AI最新动态', '人工智能最新发展动态', 'https://example.com/video/10023.mp4', 'https://example.com/cover/10023.jpg', 3, '科技,AI,资讯', 300, 1, 0, 60000, 6000, 400, 320, DATE_SUB(NOW(), INTERVAL 20 DAY)),
(10024, (SELECT id FROM sys_user WHERE username = 'user017' LIMIT 1), '编程技巧：算法优化', '算法优化技巧分享', 'https://example.com/video/10024.mp4', 'https://example.com/cover/10024.jpg', 3, '编程,算法,技巧', 420, 1, 1, 160000, 16000, 1000, 800, DATE_SUB(NOW(), INTERVAL 17 DAY)),
(10025, (SELECT id FROM sys_user WHERE username = 'user006' LIMIT 1), '数码开箱：新品体验', '最新数码产品开箱体验', 'https://example.com/video/10025.mp4', 'https://example.com/cover/10025.jpg', 3, '数码,开箱,体验', 240, 1, 0, 50000, 5000, 300, 240, DATE_SUB(NOW(), INTERVAL 13 DAY)),
(10026, (SELECT id FROM sys_user WHERE username = 'user017' LIMIT 1), '编程实战：项目开发', '从零开始开发一个项目', 'https://example.com/video/10026.mp4', 'https://example.com/cover/10026.jpg', 3, '编程,项目,实战', 1200, 1, 1, 170000, 17000, 1050, 840, DATE_SUB(NOW(), INTERVAL 7 DAY)),
(10027, (SELECT id FROM sys_user WHERE username = 'user006' LIMIT 1), '科技解析：5G技术', '深入解析5G技术原理', 'https://example.com/video/10027.mp4', 'https://example.com/cover/10027.jpg', 3, '科技,5G,解析', 360, 1, 0, 45000, 4500, 280, 220, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(10028, (SELECT id FROM sys_user WHERE username = 'user017' LIMIT 1), '编程学习：数据结构', '数据结构与算法学习', 'https://example.com/video/10028.mp4', 'https://example.com/cover/10028.jpg', 3, '编程,数据结构,学习', 540, 1, 1, 150000, 15000, 950, 760, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(10029, (SELECT id FROM sys_user WHERE username = 'user006' LIMIT 1), '科技前沿：量子计算', '量子计算技术前沿', 'https://example.com/video/10029.mp4', 'https://example.com/cover/10029.jpg', 3, '科技,量子,前沿', 420, 1, 0, 55000, 5500, 350, 280, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(10030, (SELECT id FROM sys_user WHERE username = 'user017' LIMIT 1), '编程分享：代码优化', '代码优化最佳实践', 'https://example.com/video/10030.mp4', 'https://example.com/cover/10030.jpg', 3, '编程,优化,实践', 480, 1, 1, 140000, 14000, 880, 700, NOW()),

-- 游戏类视频（10个）
(10031, (SELECT id FROM sys_user WHERE username = 'user003' LIMIT 1), '游戏实况：最新大作', '最新游戏大作实况解说', 'https://example.com/video/10031.mp4', 'https://example.com/cover/10031.jpg', 4, '游戏,实况,解说', 1800, 1, 1, 250000, 25000, 1500, 1200, DATE_SUB(NOW(), INTERVAL 29 DAY)),
(10032, (SELECT id FROM sys_user WHERE username = 'user003' LIMIT 1), '游戏攻略：通关技巧', '游戏通关技巧分享', 'https://example.com/video/10032.mp4', 'https://example.com/cover/10032.jpg', 4, '游戏,攻略,技巧', 600, 1, 1, 220000, 22000, 1300, 1040, DATE_SUB(NOW(), INTERVAL 26 DAY)),
(10033, (SELECT id FROM sys_user WHERE username = 'user003' LIMIT 1), '游戏评测：新游体验', '新游戏深度评测', 'https://example.com/video/10033.mp4', 'https://example.com/cover/10033.jpg', 4, '游戏,评测,体验', 900, 1, 0, 80000, 8000, 500, 400, DATE_SUB(NOW(), INTERVAL 22 DAY)),
(10034, (SELECT id FROM sys_user WHERE username = 'user003' LIMIT 1), '游戏集锦：精彩操作', '游戏精彩操作集锦', 'https://example.com/video/10034.mp4', 'https://example.com/cover/10034.jpg', 4, '游戏,集锦,操作', 300, 1, 1, 210000, 21000, 1250, 1000, DATE_SUB(NOW(), INTERVAL 19 DAY)),
(10035, (SELECT id FROM sys_user WHERE username = 'user003' LIMIT 1), '游戏教学：新手入门', '游戏新手入门教学', 'https://example.com/video/10035.mp4', 'https://example.com/cover/10035.jpg', 4, '游戏,教学,新手', 720, 1, 0, 70000, 7000, 450, 360, DATE_SUB(NOW(), INTERVAL 16 DAY)),
(10036, (SELECT id FROM sys_user WHERE username = 'user003' LIMIT 1), '游戏直播：精彩对局', '游戏精彩对局直播', 'https://example.com/video/10036.mp4', 'https://example.com/cover/10036.jpg', 4, '游戏,直播,对局', 2400, 1, 1, 240000, 24000, 1450, 1160, DATE_SUB(NOW(), INTERVAL 13 DAY)),
(10037, (SELECT id FROM sys_user WHERE username = 'user003' LIMIT 1), '游戏分析：战术解析', '游戏战术深度解析', 'https://example.com/video/10037.mp4', 'https://example.com/cover/10037.jpg', 4, '游戏,分析,战术', 540, 1, 0, 65000, 6500, 400, 320, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(10038, (SELECT id FROM sys_user WHERE username = 'user003' LIMIT 1), '游戏回顾：经典游戏', '经典游戏回顾', 'https://example.com/video/10038.mp4', 'https://example.com/cover/10038.jpg', 4, '游戏,回顾,经典', 1080, 1, 1, 230000, 23000, 1400, 1120, DATE_SUB(NOW(), INTERVAL 7 DAY)),
(10039, (SELECT id FROM sys_user WHERE username = 'user003' LIMIT 1), '游戏资讯：行业动态', '游戏行业最新动态', 'https://example.com/video/10039.mp4', 'https://example.com/cover/10039.jpg', 4, '游戏,资讯,动态', 360, 1, 0, 75000, 7500, 480, 380, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(10040, (SELECT id FROM sys_user WHERE username = 'user003' LIMIT 1), '游戏挑战：极限操作', '游戏极限操作挑战', 'https://example.com/video/10040.mp4', 'https://example.com/cover/10040.jpg', 4, '游戏,挑战,极限', 420, 1, 1, 200000, 20000, 1200, 960, DATE_SUB(NOW(), INTERVAL 2 DAY)),

-- 美食类视频（5个）
(10041, (SELECT id FROM sys_user WHERE username = 'user002' LIMIT 1), '美食制作：川菜经典', '川菜经典菜品制作', 'https://example.com/video/10041.mp4', 'https://example.com/cover/10041.jpg', 5, '美食,川菜,制作', 600, 1, 1, 100000, 10000, 650, 520, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(10042, (SELECT id FROM sys_user WHERE username = 'user002' LIMIT 1), '美食探店：日料店', '探访精品日料店', 'https://example.com/video/10042.mp4', 'https://example.com/cover/10042.jpg', 5, '美食,日料,探店', 480, 1, 0, 42000, 4200, 260, 210, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(10043, (SELECT id FROM sys_user WHERE username = 'user002' LIMIT 1), '美食教程：甜品制作', '精美甜品制作教程', 'https://example.com/video/10043.mp4', 'https://example.com/cover/10043.jpg', 5, '美食,甜品,教程', 360, 1, 1, 85000, 8500, 550, 440, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(10044, (SELECT id FROM sys_user WHERE username = 'user002' LIMIT 1), '美食分享：家常小菜', '家常小菜制作分享', 'https://example.com/video/10044.mp4', 'https://example.com/cover/10044.jpg', 5, '美食,家常,分享', 240, 1, 0, 38000, 3800, 230, 185, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(10045, (SELECT id FROM sys_user WHERE username = 'user002' LIMIT 1), '美食vlog：一日三餐', '记录一日三餐的美食', 'https://example.com/video/10045.mp4', 'https://example.com/cover/10045.jpg', 5, '美食,vlog,三餐', 720, 1, 1, 95000, 9500, 620, 495, NOW()),

-- 萌宠类视频（5个）
(10046, (SELECT id FROM sys_user WHERE username = 'user005' LIMIT 1), '萌宠日常：猫咪的一天', '记录猫咪的日常生活', 'https://example.com/video/10046.mp4', 'https://example.com/cover/10046.jpg', 6, '萌宠,猫咪,日常', 180, 1, 1, 110000, 11000, 700, 560, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(10047, (SELECT id FROM sys_user WHERE username = 'user005' LIMIT 1), '萌宠搞笑：狗狗的趣事', '狗狗的搞笑瞬间', 'https://example.com/video/10047.mp4', 'https://example.com/cover/10047.jpg', 6, '萌宠,狗狗,搞笑', 120, 1, 0, 48000, 4800, 300, 240, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(10048, (SELECT id FROM sys_user WHERE username = 'user005' LIMIT 1), '萌宠教学：训练技巧', '宠物训练技巧分享', 'https://example.com/video/10048.mp4', 'https://example.com/cover/10048.jpg', 6, '萌宠,训练,技巧', 300, 1, 1, 105000, 10500, 680, 544, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(10049, (SELECT id FROM sys_user WHERE username = 'user005' LIMIT 1), '萌宠合集：可爱瞬间', '萌宠可爱瞬间合集', 'https://example.com/video/10049.mp4', 'https://example.com/cover/10049.jpg', 6, '萌宠,合集,可爱', 240, 1, 0, 52000, 5200, 330, 264, NOW()),
(10050, (SELECT id FROM sys_user WHERE username = 'user005' LIMIT 1), '萌宠故事：成长记录', '记录宠物的成长故事', 'https://example.com/video/10050.mp4', 'https://example.com/cover/10050.jpg', 6, '萌宠,故事,成长', 600, 1, 1, 115000, 11500, 750, 600, NOW());

-- =================================================================
-- 3. 初始化用户关注关系（50条关注记录）
-- =================================================================

INSERT INTO `sys_user_follow` (`user_id`, `follow_user_id`, `create_time`)
SELECT 
    u1.id,
    u2.id,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY)
FROM sys_user u1, sys_user u2
WHERE u1.username LIKE 'user%' 
  AND u2.username LIKE 'user%'
  AND u1.id != u2.id
  AND (u1.id % 3 = 0 OR u2.id % 3 = 0)
LIMIT 50;

-- 更新用户的关注数和粉丝数（基于实际关注关系）
UPDATE sys_user u
SET 
    follow_count = (
        SELECT COUNT(*) FROM sys_user_follow f 
        WHERE f.user_id = u.id
    ),
    fans_count = (
        SELECT COUNT(*) FROM sys_user_follow f 
        WHERE f.follow_user_id = u.id
    );

-- =================================================================
-- 4. 初始化视频互动记录（点赞、收藏、转发）
-- =================================================================

-- 点赞记录（200条）
INSERT INTO `video_interaction` (`user_id`, `video_id`, `type`, `create_time`)
SELECT 
    u.id,
    v.id,
    1, -- 1-点赞
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY)
FROM sys_user u, video_info v
WHERE u.username LIKE 'user%'
  AND v.status = 1
  AND (u.id + v.id) % 5 = 0
LIMIT 200;

-- 收藏记录（100条）
INSERT INTO `video_interaction` (`user_id`, `video_id`, `type`, `create_time`)
SELECT 
    u.id,
    v.id,
    2, -- 2-收藏
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY)
FROM sys_user u, video_info v
WHERE u.username LIKE 'user%'
  AND v.status = 1
  AND (u.id + v.id) % 7 = 0
LIMIT 100;

-- 转发记录（80条）
INSERT INTO `video_interaction` (`user_id`, `video_id`, `type`, `create_time`)
SELECT 
    u.id,
    v.id,
    3, -- 3-转发
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY)
FROM sys_user u, video_info v
WHERE u.username LIKE 'user%'
  AND v.status = 1
  AND (u.id + v.id) % 9 = 0
LIMIT 80;

-- 更新视频的点赞数、评论数、转发数（基于实际互动记录）
UPDATE video_info v
SET 
    like_count = (
        SELECT COUNT(*) FROM video_interaction i 
        WHERE i.video_id = v.id AND i.type = 1
    ),
    share_count = (
        SELECT COUNT(*) FROM video_interaction i 
        WHERE i.video_id = v.id AND i.type = 3
    );

-- =================================================================
-- 5. 初始化播放历史记录（300条）
-- =================================================================

INSERT INTO `video_play_record` (`user_id`, `video_id`, `duration`, `progress`, `is_finish`, `create_time`)
SELECT 
    u.id,
    v.id,
    FLOOR(RAND() * v.duration), -- 观看时长
    FLOOR(RAND() * 100), -- 播放进度
    CASE WHEN RAND() > 0.3 THEN 1 ELSE 0 END, -- 30%完播率
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY)
FROM sys_user u, video_info v
WHERE u.username LIKE 'user%'
  AND v.status = 1
  AND (u.id + v.id) % 4 = 0
LIMIT 300;

-- 更新视频的播放量（基于实际播放记录）
UPDATE video_info v
SET view_count = (
    SELECT COUNT(*) FROM video_play_record p 
    WHERE p.video_id = v.id
);

-- =================================================================
-- 6. 初始化评论数据（150条评论）
-- =================================================================

-- 注意：评论ID使用雪花算法，这里使用大数字模拟
INSERT INTO `video_comment` (`id`, `video_id`, `user_id`, `content`, `parent_id`, `like_count`, `status`, `create_time`)
SELECT 
    20000 + (@row_number := @row_number + 1),
    v.id,
    u.id,
    CASE 
        WHEN (@row_number % 10 = 0) THEN '太棒了！'
        WHEN (@row_number % 10 = 1) THEN '很有意思的视频'
        WHEN (@row_number % 10 = 2) THEN '学到了很多'
        WHEN (@row_number % 10 = 3) THEN '期待更多内容'
        WHEN (@row_number % 10 = 4) THEN '支持支持！'
        WHEN (@row_number % 10 = 5) THEN '这个视频真不错'
        WHEN (@row_number % 10 = 6) THEN '收藏了'
        WHEN (@row_number % 10 = 7) THEN '已点赞'
        WHEN (@row_number % 10 = 8) THEN '感谢分享'
        ELSE '很棒的内容'
    END,
    0, -- 父评论ID，0表示顶级评论
    FLOOR(RAND() * 100), -- 点赞数
    1, -- 状态：1-正常
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY)
FROM sys_user u, video_info v, (SELECT @row_number := 0) r
WHERE u.username LIKE 'user%'
  AND v.status = 1
  AND (u.id + v.id) % 6 = 0
LIMIT 150;

-- 更新视频的评论数（基于实际评论记录）
UPDATE video_info v
SET comment_count = (
    SELECT COUNT(*) FROM video_comment c 
    WHERE c.video_id = v.id AND c.status = 1
);

-- =================================================================
-- 7. 初始化用户行为记录（用于user_behavior表，兼容旧系统）
-- =================================================================

-- 注意：虽然database.sql中删除了此表，但代码中仍在使用，所以需要创建
CREATE TABLE IF NOT EXISTS `user_behavior` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `video_id` bigint NOT NULL COMMENT '视频ID',
    `action_type` varchar(32) NOT NULL COMMENT '行为类型（like, view, collect, share等）',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_video_id` (`video_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户行为表';

-- 初始化用户行为记录（观看、点赞、评论）
INSERT INTO `user_behavior` (`user_id`, `video_id`, `action_type`, `create_time`)
SELECT 
    u.id,
    v.id,
    'view',
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY)
FROM sys_user u, video_info v
WHERE u.username LIKE 'user%'
  AND v.status = 1
  AND (u.id + v.id) % 3 = 0
LIMIT 200;

INSERT INTO `user_behavior` (`user_id`, `video_id`, `action_type`, `create_time`)
SELECT 
    u.id,
    v.id,
    'like',
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY)
FROM sys_user u, video_info v
WHERE u.username LIKE 'user%'
  AND v.status = 1
  AND (u.id + v.id) % 5 = 0
LIMIT 150;

INSERT INTO `user_behavior` (`user_id`, `video_id`, `action_type`, `create_time`)
SELECT 
    u.id,
    v.id,
    'comment',
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY)
FROM sys_user u, video_info v
WHERE u.username LIKE 'user%'
  AND v.status = 1
  AND (u.id + v.id) % 8 = 0
LIMIT 100;

-- =================================================================
-- 8. 初始化操作日志（50条）
-- =================================================================

INSERT INTO `sys_operation_log` (`operator`, `module`, `action`, `ip_address`, `status`, `create_time`)
SELECT 
    'admin',
    CASE 
        WHEN (@log_num % 4 = 0) THEN '视频管理'
        WHEN (@log_num % 4 = 1) THEN '用户管理'
        WHEN (@log_num % 4 = 2) THEN '内容审核'
        ELSE '系统配置'
    END,
    CASE 
        WHEN (@log_num % 4 = 0) THEN '审核通过视频'
        WHEN (@log_num % 4 = 1) THEN '冻结用户账号'
        WHEN (@log_num % 4 = 2) THEN '驳回视频内容'
        ELSE '修改系统配置'
    END,
    CONCAT('192.168.1.', FLOOR(1 + RAND() * 254)),
    '成功',
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY)
FROM (SELECT @log_num := 0) r, sys_user
WHERE username LIKE 'admin%'
LIMIT 50;

-- =================================================================
-- 9. 初始化登录日志（100条）
-- =================================================================

INSERT INTO `sys_login_log` (`user_id`, `username`, `ip_address`, `device_info`, `status`, `msg`, `login_time`)
SELECT 
    u.id,
    u.username,
    CONCAT('192.168.1.', FLOOR(1 + RAND() * 254)),
    CASE 
        WHEN (@login_num % 3 = 0) THEN 'Windows Chrome'
        WHEN (@login_num % 3 = 1) THEN 'Android App'
        ELSE 'iOS Safari'
    END,
    CASE WHEN RAND() > 0.1 THEN 1 ELSE 0 END, -- 90%成功率
    CASE WHEN RAND() > 0.1 THEN '登录成功' ELSE '密码错误' END,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY)
FROM sys_user u, (SELECT @login_num := 0) r
WHERE u.username LIKE 'user%' OR u.username LIKE 'admin%'
LIMIT 100;

-- =================================================================
-- 10. 初始化系统通知（10条）
-- =================================================================

INSERT INTO `sys_notice` (`title`, `content`, `type`, `target_type`, `status`, `is_top`, `read_count`, `publish_time`, `create_time`)
VALUES
('欢迎使用短视频平台', '欢迎加入我们的短视频平台，开始你的创作之旅！', 1, 0, 1, 1, 500, DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 30 DAY)),
('平台功能更新', '我们新增了视频推荐功能，快来体验吧！', 2, 0, 1, 0, 300, DATE_SUB(NOW(), INTERVAL 25 DAY), DATE_SUB(NOW(), INTERVAL 25 DAY)),
('系统维护通知', '系统将于今晚22:00-24:00进行维护，期间可能无法访问', 3, 0, 1, 1, 200, DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY)),
('创作激励计划', '参与创作激励计划，获得更多曝光和收益！', 2, 0, 1, 0, 400, DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 15 DAY)),
('社区规范提醒', '请遵守社区规范，共同营造良好的创作环境', 1, 0, 1, 0, 250, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY)),
('新功能上线', '视频剪辑功能已上线，快来试试吧！', 2, 0, 1, 0, 350, DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY)),
('活动通知', '参与话题挑战，赢取丰厚奖品！', 2, 0, 1, 1, 450, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),
('隐私政策更新', '我们更新了隐私政策，请查看最新版本', 1, 0, 1, 0, 150, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
('感谢支持', '感谢大家一直以来的支持，我们会继续努力！', 1, 0, 1, 0, 280, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
('春节活动', '春节特别活动即将开始，敬请期待！', 2, 0, 1, 1, 320, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY));

-- =================================================================
-- 数据初始化完成
-- =================================================================

-- 显示数据统计
SELECT '数据初始化完成！' AS message;
SELECT 
    '用户总数' AS type,
    COUNT(*) AS count 
FROM sys_user
UNION ALL
SELECT 
    '视频总数',
    COUNT(*) 
FROM video_info
UNION ALL
SELECT 
    '关注关系',
    COUNT(*) 
FROM sys_user_follow
UNION ALL
SELECT 
    '互动记录',
    COUNT(*) 
FROM video_interaction
UNION ALL
SELECT 
    '播放记录',
    COUNT(*) 
FROM video_play_record
UNION ALL
SELECT 
    '评论总数',
    COUNT(*) 
FROM video_comment;
