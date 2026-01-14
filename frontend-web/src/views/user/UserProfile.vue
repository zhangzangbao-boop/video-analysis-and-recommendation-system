<template>
  <div class="profile-container">
    <el-card>
      <div slot="header">
        <span>个人中心</span>
      </div>
      
      <div class="profile-content">
        <!-- 上半栏：背景图和头像信息 -->
        <div class="profile-header-with-bg">
          <!-- 背景图片区域 -->
          <div class="profile-background"></div>
          
          <!-- 内容区域 -->
          <div class="profile-header-content">
            <!-- 左侧：放大头像 -->
            <div class="avatar-section-large">
              <el-avatar 
                :size="120" 
                :src="userInfo.avatar"
                class="user-avatar-large"
              >
                {{ userInfo.username.charAt(0) }}
              </el-avatar>
            </div>
            
            <!-- 右侧：用户信息和统计 -->
            <div class="user-info-section">
              <div class="user-basic-info">
                <h2 class="username-large">{{ userInfo.username }}</h2>
                <p class="user-bio">{{ userInfo.bio || '暂无简介' }}</p>
              </div>
              
              <div class="stats-section-horizontal">
                <div class="stat-item-horizontal">
                  <div class="stat-number-horizontal">{{ userStats.following || 128 }}</div>
                  <div class="stat-label-horizontal">关注</div>
                </div>
                <div class="stat-item-horizontal">
                  <div class="stat-number-horizontal">{{ userStats.followers || 256 }}</div>
                  <div class="stat-label-horizontal">粉丝</div>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 下半栏：标签页 -->
        <el-tabs v-model="activeTab" class="profile-tabs">
          <!-- 播放历史 -->
          <el-tab-pane label="播放历史" name="history">
            <div class="history-content">
              <p>播放历史内容...</p>
            </div>
          </el-tab-pane>
          
          <!-- 点赞记录 -->
          <el-tab-pane label="点赞记录" name="likes">
            <div class="likes-content">
              <p>点赞记录内容...</p>
            </div>
          </el-tab-pane>
          
          <!-- 评论记录 -->
          <el-tab-pane label="评论记录" name="comments">
            <div class="comments-content">
              <p>评论记录内容...</p>
            </div>
          </el-tab-pane>
          
          <!-- 设置 -->
          <el-tab-pane label="设置" name="settings">
            <div class="settings">
              <el-form label-width="100px">
                <el-form-item label="消息通知">
                  <el-switch v-model="settings.notifications"></el-switch>
                </el-form-item>
                <el-form-item label="隐私模式">
                  <el-switch v-model="settings.privacy"></el-switch>
                </el-form-item>
                <el-form-item label="主题颜色">
                  <el-color-picker v-model="settings.themeColor"></el-color-picker>
                </el-form-item>
              </el-form>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'UserProfile',
  data() {
    return {
      activeTab: 'history',
      userInfo: {
        username: localStorage.getItem('username') || '用户',
        avatar: '',
        bio: '热爱生活，喜欢分享短视频，记录美好时光'
      },
      userStats: {
        following: 128,
        followers: 256
      },
      settings: {
        notifications: true,
        privacy: false,
        themeColor: '#409EFF'
      }
    }
  }
}
</script>

<style scoped>
.profile-container {
  padding: 20px;
}

/* 上半栏背景布局 */
.profile-header-with-bg {
  position: relative;
  margin-bottom: 30px;
  padding-bottom: 30px;
  border-bottom: 1px solid #eee;
  border-radius: 8px;
  overflow: hidden;
  min-height: 200px;
}

/* 背景图片区域 */
.profile-background {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 180px; /* 背景图片的高度 */
  background-image: url('@/assets/image1.jpg'); /* 这里是图片路径 */
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  z-index: 1;
}

/* 渐变遮罩层，让文字更清晰 */
.profile-background::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 100%;
  background: linear-gradient(to bottom, rgba(255,255,255,0.8) 0%, rgba(255,255,255,0.5) 100%);
  z-index: 2;
}

/* 内容区域，在背景之上 */
.profile-header-content {
  position: relative;
  display: flex;
  align-items: center;
  gap: 40px;
  padding: 30px;
  z-index: 3; /* 确保内容在背景和遮罩层之上 */
}

/* 左侧放大头像 */
.avatar-section-large {
  flex-shrink: 0;
}

.user-avatar-large {
  width: 140px !important;
  height: 140px !important;
  font-size: 48px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border: 4px solid white;
}

/* 右侧用户信息 */
.user-info-section {
  flex: 1;
}

.user-basic-info {
  margin-bottom: 25px;
}

.username-large {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin: 0 0 10px 0;
}

.user-bio {
  font-size: 16px;
  color: #666;
  line-height: 1.5;
  margin: 0;
  max-width: 500px;
}

/* 水平排列的统计信息 */
.stats-section-horizontal {
  display: flex;
  gap: 40px;
}

.stat-item-horizontal {
  text-align: left;
}

.stat-number-horizontal {
  font-size: 28px;
  font-weight: bold;
  color: #409EFF;
  margin-bottom: 5px;
}

.stat-label-horizontal {
  font-size: 14px;
  color: #999;
}

/* 标签页区域 */
.profile-tabs {
  margin-top: 20px;
}

.history-content,
.likes-content,
.comments-content,
.settings {
  padding: 20px;
  min-height: 200px;
}

.settings {
  max-width: 600px;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .profile-header-content {
    flex-direction: column;
    text-align: center;
    gap: 20px;
  }
  
  .stats-section-horizontal {
    justify-content: center;
  }
  
  .user-avatar-large {
    width: 120px !important;
    height: 120px !important;
  }
  
  .username-large {
    font-size: 24px;
  }
}
</style>