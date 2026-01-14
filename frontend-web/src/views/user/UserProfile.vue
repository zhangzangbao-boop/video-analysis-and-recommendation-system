<template>
  <div class="profile-container">
    <!-- 用户信息卡片 - 视觉强化 -->
    <el-card class="profile-main-card">
      <!-- 上半部分：用户信息和统计 -->
      <div class="profile-header-enhanced">
        <!-- 左侧：大尺寸头像 -->
        <div class="avatar-section-enhanced">
          <div class="avatar-wrapper">
            <el-avatar 
              :size="120" 
              :src="userInfo.avatar"
              class="user-avatar-enhanced"
            >
              {{ userInfo.username.charAt(0) }}
            </el-avatar>
            <div class="avatar-badge">
              <i class="el-icon-star-on"></i>
              <span>活跃用户</span>
            </div>
          </div>
        </div>
        
        <!-- 中间：用户基本信息 -->
        <div class="user-info-enhanced">
          <div class="user-title-section">
            <h1 class="username-enhanced">{{ userInfo.username }}</h1>
            <el-tag type="success" size="small">在线</el-tag>
          </div>
          <div class="user-bio-enhanced">
            {{ userInfo.bio || '这个人很懒，还没有写简介哦~' }}
          </div>
          
          <!-- 用户统计数据 -->
          <div class="stats-cards">
            <div class="stat-card" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);">
              <div class="stat-icon">
                <i class="el-icon-view"></i>
              </div>
              <div class="stat-content">
                <div class="stat-number">1.2万</div>
                <div class="stat-label">累计播放</div>
              </div>
            </div>
            
            <div class="stat-card" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);">
              <div class="stat-icon">
                <i class="el-icon-star-on"></i>
              </div>
              <div class="stat-content">
                <div class="stat-number">{{ userStats.following || 128 }}</div>
                <div class="stat-label">关注</div>
              </div>
            </div>
            
            <div class="stat-card" style="background: linear-gradient(135deg, #5ee7df 0%, #b490ca 100%);">
              <div class="stat-icon">
                <i class="el-icon-user"></i>
              </div>
              <div class="stat-content">
                <div class="stat-number">{{ userStats.followers || 256 }}</div>
                <div class="stat-label">粉丝</div>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 右侧：快速操作 -->
        <div class="quick-actions">
          <el-button type="primary" icon="el-icon-edit" class="action-btn">编辑资料</el-button>
          <el-button icon="el-icon-setting" class="action-btn">账号设置</el-button>
          <el-button icon="el-icon-share" class="action-btn">分享主页</el-button>
        </div>
      </div>
    </el-card>
    
    <!-- 标签页区域 -->
    <div class="tabs-section">
      <!-- 标签页卡片 -->
      <el-card class="tabs-card">
        <!-- 自定义标签页样式 -->
        <div class="custom-tabs">
          <div 
            v-for="tab in tabs" 
            :key="tab.name"
            class="custom-tab-item"
            :class="{ active: activeTab === tab.name }"
            @click="activeTab = tab.name"
          >
            <i :class="tab.icon"></i>
            <span>{{ tab.label }}</span>
            <div class="tab-indicator" v-if="activeTab === tab.name"></div>
          </div>
        </div>
        
        <!-- 标签内容区域 -->
        <div class="tab-content">
          <!-- 播放历史 -->
          <div v-show="activeTab === 'history'" class="tab-pane enhanced">
            <div class="content-header">
              <h3><i class="el-icon-time"></i> 播放历史</h3>
              <p>记录您最近观看的视频内容</p>
            </div>
            <div class="history-grid">
              <div class="empty-state">
                <div class="empty-icon">
                  <i class="el-icon-video-play"></i>
                </div>
                <h4>暂无观看历史</h4>
                <p>快去发现精彩的短视频吧</p>
                <el-button type="primary" @click="$router.push('/main/video')">
                  去首页看看
                </el-button>
              </div>
            </div>
          </div>
          
          <!-- 点赞记录 -->
          <div v-show="activeTab === 'likes'" class="tab-pane enhanced">
            <div class="content-header">
              <h3><i class="el-icon-star-on"></i> 点赞记录</h3>
              <p>您点赞过的精彩内容</p>
            </div>
            <div class="likes-grid">
              <div class="empty-state">
                <div class="empty-icon">
                  <i class="el-icon-star-off"></i>
                </div>
                <h4>暂无点赞记录</h4>
                <p>看到喜欢的视频，点个赞吧</p>
              </div>
            </div>
          </div>
          
          <!-- 评论记录 -->
          <div v-show="activeTab === 'comments'" class="tab-pane enhanced">
            <div class="content-header">
              <h3><i class="el-icon-chat-dot-round"></i> 评论记录</h3>
              <p>您发表的评论和互动</p>
            </div>
            <div class="comments-list">
              <div class="empty-state">
                <div class="empty-icon">
                  <i class="el-icon-chat-dot-square"></i>
                </div>
                <h4>暂无评论记录</h4>
                <p>发表你的第一条评论</p>
              </div>
            </div>
          </div>
          
          <!-- 设置 -->
          <div v-show="activeTab === 'settings'" class="tab-pane enhanced">
            <div class="content-header">
              <h3><i class="el-icon-setting"></i> 账号设置</h3>
              <p>管理您的账号和隐私</p>
            </div>
            
            <div class="settings-enhanced">
              <div class="settings-grid">
                <!-- 基础设置 -->
                <div class="setting-card">
                  <div class="setting-card-header">
                    <i class="el-icon-user"></i>
                    <h4>基础信息</h4>
                  </div>
                  <div class="setting-form">
                    <el-form label-width="100px">
                      <el-form-item label="用户名">
                        <el-input v-model="userInfo.username"></el-input>
                      </el-form-item>
                      <el-form-item label="个人简介">
                        <el-input 
                          type="textarea" 
                          v-model="userInfo.bio"
                          placeholder="介绍一下自己吧~"
                        ></el-input>
                      </el-form-item>
                    </el-form>
                  </div>
                </div>
                
                <!-- 隐私设置 -->
                <div class="setting-card">
                  <div class="setting-card-header">
                    <i class="el-icon-lock"></i>
                    <h4>隐私设置</h4>
                  </div>
                  <div class="setting-form">
                    <div class="setting-item">
                      <div class="setting-label">消息通知</div>
                      <el-switch v-model="settings.notifications"></el-switch>
                    </div>
                    <div class="setting-item">
                      <div class="setting-label">隐私模式</div>
                      <el-switch v-model="settings.privacy"></el-switch>
                    </div>
                    <div class="setting-item">
                      <div class="setting-label">允许评论</div>
                      <el-switch v-model="settings.allowComments" active-value="true" inactive-value="false"></el-switch>
                    </div>
                  </div>
                </div>
                
                <!-- 主题设置 -->
                <div class="setting-card">
                  <div class="setting-card-header">
                    <i class="el-icon-picture-outline"></i>
                    <h4>主题设置</h4>
                  </div>
                  <div class="setting-form">
                    <div class="setting-item">
                      <div class="setting-label">主题颜色</div>
                      <el-color-picker v-model="settings.themeColor"></el-color-picker>
                    </div>
                    <div class="setting-item">
                      <div class="setting-label">字体大小</div>
                      <el-select v-model="settings.fontSize" size="small">
                        <el-option label="小" value="small"></el-option>
                        <el-option label="中" value="medium"></el-option>
                        <el-option label="大" value="large"></el-option>
                      </el-select>
                    </div>
                  </div>
                </div>
                
                <!-- 其他设置 -->
                <div class="setting-card">
                  <div class="setting-card-header">
                    <i class="el-icon-more"></i>
                    <h4>其他设置</h4>
                  </div>
                  <div class="setting-actions">
                    <el-button type="text" icon="el-icon-info">查看帮助中心</el-button>
                    <el-button type="text" icon="el-icon-question">意见反馈</el-button>
                    <el-button type="text" icon="el-icon-refresh">清除缓存</el-button>
                    <el-button type="text" icon="el-icon-download">数据导出</el-button>
                  </div>
                </div>
              </div>
              
              <!-- 保存按钮 -->
              <div class="settings-actions">
                <el-button type="primary" size="large" icon="el-icon-check">保存设置</el-button>
                <el-button type="default" size="large">恢复默认</el-button>
              </div>
            </div>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script>
export default {
  name: 'UserProfile',
  data() {
    return {
      activeTab: 'history',
      tabs: [
        { name: 'history', label: '播放历史', icon: 'el-icon-time' },
        { name: 'likes', label: '点赞记录', icon: 'el-icon-star-on' },
        { name: 'comments', label: '评论记录', icon: 'el-icon-chat-dot-round' },
        { name: 'settings', label: '账号设置', icon: 'el-icon-setting' }
      ],
      userInfo: {
        username: localStorage.getItem('username') || '短视频爱好者',
        avatar: 'https://picsum.photos/120/120?random=1',
        bio: '热爱生活，喜欢分享短视频，记录美好时光。每天都会发现有趣的内容！'
      },
      userStats: {
        following: 128,
        followers: 256
      },
      settings: {
        notifications: true,
        privacy: false,
        themeColor: '#409EFF',
        allowComments: true,
        fontSize: 'medium'
      }
    }
  },
  
  methods: {
    // 保持原有功能，这里不需要增加新方法
  }
}
</script>

<style scoped>
/* 整体容器 */
.profile-container {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8f0 100%);
  min-height: 100vh;
}

/* 主要用户信息卡片 */
.profile-main-card {
  border-radius: 16px;
  margin-bottom: 30px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08);
  border: none;
}

/* 增强版用户信息头部 */
.profile-header-enhanced {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 40px;
  align-items: center;
  padding: 30px;
}

/* 左侧头像区域 */
.avatar-section-enhanced {
  position: relative;
}

.avatar-wrapper {
  position: relative;
  text-align: center;
}

.user-avatar-enhanced {
  width: 140px !important;
  height: 140px !important;
  font-size: 48px;
  border: 6px solid white;
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
  margin-bottom: 10px;
}

.avatar-badge {
  background: linear-gradient(135deg, #ffd700 0%, #ffa500 100%);
  color: white;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: bold;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  box-shadow: 0 4px 10px rgba(255, 165, 0, 0.2);
}

/* 中间用户信息 */
.user-info-enhanced {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.user-title-section {
  display: flex;
  align-items: center;
  gap: 15px;
}

.username-enhanced {
  font-size: 32px;
  font-weight: bold;
  color: #333;
  margin: 0;
}

.user-bio-enhanced {
  font-size: 16px;
  color: #666;
  line-height: 1.6;
  margin: 0;
}

/* 统计卡片 */
.stats-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-top: 10px;
}

.stat-card {
  border-radius: 12px;
  padding: 20px;
  color: white;
  display: flex;
  align-items: center;
  gap: 15px;
  box-shadow: 0 6px 15px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s;
}

.stat-card:hover {
  transform: translateY(-5px);
}

.stat-icon {
  font-size: 28px;
  opacity: 0.9;
}

.stat-content {
  flex: 1;
}

.stat-number {
  font-size: 28px;
  font-weight: bold;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  opacity: 0.9;
}

/* 右侧快速操作 */
.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.action-btn {
  width: 150px;
  padding: 12px 20px;
  border-radius: 10px;
  font-weight: 500;
  transition: all 0.3s;
}

.action-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

/* 标签页区域 */
.tabs-section {
  margin-bottom: 30px;
}

.tabs-card {
  border-radius: 16px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08);
  border: none;
}

/* 自定义标签页样式 */
.custom-tabs {
  display: flex;
  border-bottom: 2px solid #f0f2f5;
  padding: 0 30px;
  margin-bottom: 30px;
}

.custom-tab-item {
  padding: 20px 25px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 500;
  color: #666;
  position: relative;
  transition: all 0.3s;
  border-bottom: 3px solid transparent;
}

.custom-tab-item:hover {
  color: #409EFF;
}

.custom-tab-item.active {
  color: #409EFF;
  font-weight: bold;
}

.tab-indicator {
  position: absolute;
  bottom: -2px;
  left: 0;
  right: 0;
  height: 3px;
  background: #409EFF;
  border-radius: 3px;
}

/* 标签内容区域 */
.tab-content {
  padding: 0 30px 30px;
}

.tab-pane.enhanced {
  animation: fadeIn 0.5s;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

/* 内容头部 */
.content-header {
  margin-bottom: 30px;
}

.content-header h3 {
  font-size: 24px;
  color: #333;
  margin: 0 0 10px 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.content-header p {
  color: #666;
  margin: 0;
  font-size: 14px;
}

/* 空状态样式 */
.empty-state {
  text-align: center;
  padding: 60px 20px;
}

.empty-icon {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
  font-size: 36px;
  color: white;
}

.empty-state h4 {
  font-size: 20px;
  color: #333;
  margin: 0 0 10px 0;
}

.empty-state p {
  color: #666;
  margin: 0 0 20px 0;
  font-size: 14px;
}

/* 设置页面增强版 */
.settings-enhanced {
  margin-top: 20px;
}

.settings-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 25px;
  margin-bottom: 30px;
}

.setting-card {
  background: #f8f9fa;
  border-radius: 12px;
  padding: 25px;
  transition: transform 0.3s;
}

.setting-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.05);
}

.setting-card-header {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 2px solid #e9ecef;
}

.setting-card-header i {
  font-size: 24px;
  color: #409EFF;
}

.setting-card-header h4 {
  margin: 0;
  color: #333;
  font-size: 18px;
}

/* 设置项 */
.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 0;
  border-bottom: 1px solid #eee;
}

.setting-item:last-child {
  border-bottom: none;
}

.setting-label {
  font-weight: 500;
  color: #333;
}

/* 设置操作按钮 */
.setting-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.setting-actions .el-button {
  justify-content: flex-start;
  padding: 10px 0;
}

/* 保存设置按钮 */
.settings-actions {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 40px;
  padding-top: 30px;
  border-top: 2px solid #f0f2f5;
}

.settings-actions .el-button {
  min-width: 150px;
  padding: 12px 30px;
  border-radius: 10px;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .profile-header-enhanced {
    grid-template-columns: 1fr;
    gap: 30px;
  }
  
  .quick-actions {
    flex-direction: row;
    justify-content: center;
  }
  
  .settings-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .profile-container {
    padding: 15px;
  }
  
  .profile-header-enhanced {
    padding: 20px;
  }
  
  .stats-cards {
    grid-template-columns: 1fr;
  }
  
  .custom-tabs {
    flex-direction: column;
    padding: 0;
  }
  
  .custom-tab-item {
    padding: 15px;
    justify-content: center;
  }
  
  .tab-content {
    padding: 0 15px 20px;
  }
  
  .username-enhanced {
    font-size: 24px;
  }
  
  .settings-actions {
    flex-direction: column;
  }
  
  .settings-actions .el-button {
    width: 100%;
  }
}

@media (max-width: 480px) {
  .user-avatar-enhanced {
    width: 100px !important;
    height: 100px !important;
  }
  
  .content-header h3 {
    font-size: 20px;
  }
  
  .setting-card {
    padding: 20px;
  }
}
</style>