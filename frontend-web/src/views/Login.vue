<template>
  <div class="guest-container">
    <el-header class="main-header">
      <div class="header-left-section">
        <h2 class="logo-text">短视频推荐系统</h2>

        <div class="horizontal-nav">
          <div class="nav-item active">
            <i class="el-icon-video-camera"></i>
            <span class="nav-text">视频首页</span>
            <div class="nav-indicator"></div>
          </div>
        </div>
      </div>

      <div class="header-right-section">
        <div class="header-search-wrapper">
          <div class="header-search-input-wrapper">
            <i class="header-search-icon el-icon-search"></i>
            <input
                v-model="searchKeyword"
                type="text"
                placeholder="搜索视频..."
                class="header-search-input"
                @keyup.enter="handleSearch"
            />
          </div>
          <button class="header-search-button" @click="handleSearch">
            <i class="el-icon-search"></i>
          </button>
        </div>

        <div class="header-user-info">
          <div class="user-info-guest">
            <span
                class="guest-text"
                @click="handleGuestSecretClick"
                title="当前身份：游客"
                style="cursor: pointer; user-select: none;"
            >
              游客
            </span>

            <el-button
                size="small"
                round
                class="login-btn-small"
                @click="showUserLoginDialog = true"
            >
              登录
            </el-button>
          </div>
        </div>
      </div>
    </el-header>

    <div class="main-content-full">
      <el-main class="content-area-full">
        <div class="guest-video-list">
          <!-- 提示信息 -->
          <div class="login-prompt-section">
            <el-empty description="登录后解锁更多精彩视频">
              <el-button type="primary" @click="showUserLoginDialog = true">去登录</el-button>
            </el-empty>
          </div>

          <!-- 真实视频预览列表 -->
          <div v-loading="loadingVideos" class="preview-video-section">
            <h3 class="section-title">
              <i class="el-icon-video-camera"></i> 热门视频预览
            </h3>
            <div v-if="previewVideos.length === 0 && !loadingVideos" class="no-videos-tip">
              <p>暂无视频，登录后查看更多精彩内容</p>
            </div>
            <div v-else class="preview-video-grid">
              <div 
                v-for="video in previewVideos" 
                :key="video.id" 
                class="preview-video-card"
                @click="previewVideo(video)"
              >
                <div class="preview-cover">
                  <img :src="video.coverUrl || ''" :alt="video.title" />
                  <div class="play-overlay">
                    <i class="el-icon-video-play"></i>
                  </div>
                  <div class="duration-badge" v-if="video.duration">
                    {{ formatDuration(video.duration) }}
                  </div>
                </div>
                <div class="preview-info">
                  <h4 class="preview-title" :title="video.title">{{ video.title || '无标题' }}</h4>
                  <div class="preview-meta">
                    <span class="preview-author">UP主: {{ video.authorId ? `用户${video.authorId}` : '未知' }}</span>
                    <div class="preview-stats">
                      <span><i class="el-icon-view"></i> {{ formatNumber(video.playCount || 0) }}</span>
                      <span><i class="el-icon-star-on"></i> {{ formatNumber(video.likeCount || 0) }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 登录按钮下方也显示视频 -->
          <div class="login-button-section">
            <el-button 
              type="primary" 
              size="large" 
              round
              icon="el-icon-user"
              @click="showUserLoginDialog = true"
              class="main-login-btn"
            >
              立即登录，解锁更多精彩内容
            </el-button>
          </div>
        </div>
      </el-main>
    </div>

    <el-dialog
        title="用户登录"
        :visible.sync="showUserLoginDialog"
        width="360px"
        center
        append-to-body
    >
      <el-form :model="loginForm" :rules="rules" ref="userLoginForm">
        <el-form-item prop="username">
          <el-input v-model="loginForm.username" prefix-icon="el-icon-user" placeholder="账号"></el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="loginForm.password" prefix-icon="el-icon-lock" type="password" placeholder="密码" show-password></el-input>
        </el-form-item>
        <el-button type="primary" style="width: 100%;" @click="handleUserLogin" :loading="loading">登 录</el-button>
      </el-form>
    </el-dialog>

    <el-dialog
        title="管理员后台"
        :visible.sync="showAdminLoginDialog"
        width="360px"
        center
        append-to-body
        custom-class="admin-dialog"
    >
      <el-form :model="adminForm" :rules="rules" ref="adminLoginForm">
        <el-form-item prop="username">
          <el-input v-model="adminForm.username" prefix-icon="el-icon-s-custom" placeholder="管理员账号"></el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="adminForm.password" prefix-icon="el-icon-key" type="password" placeholder="管理员密码" show-password></el-input>
        </el-form-item>
        <el-button type="danger" style="width: 100%;" @click="handleAdminLogin" :loading="loading">进入后台</el-button>
      </el-form>
    </el-dialog>

  </div>
</template>

<script>
// 1. 引入 request 用于直接调用登录接口 (解决 api/user.js 缺失 login 的问题)
import request from '@/utils/request'
// 2. 引入 userVideoApi 用于获取用户信息 (确保头部导航显示正确)
import { userVideoApi } from '@/api/user'

export default {
  name: 'UserLogin',

  data() {
    return {
      searchKeyword: '',
      clickCount: 0,
      lastClickTime: 0,
      loading: false,
      loadingVideos: false,
      previewVideos: [],

      // 弹窗控制
      showUserLoginDialog: false,
      showAdminLoginDialog: false,

      // 表单数据
      loginForm: { username: '', password: '' },
      adminForm: { username: '', password: '' },

      rules: {
        username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
      }
    };
  },
  mounted() {
    // 加载热门视频预览
    this.loadPreviewVideos();
  },
  methods: {
    // 【新增】加载预览视频
    async loadPreviewVideos() {
      this.loadingVideos = true;
      try {
        // 调用热门视频接口（不需要登录）
        const res = await userVideoApi.getHotVideos();
        if (res && res.data && res.data.length > 0) {
          // 只显示前8个视频作为预览
          this.previewVideos = res.data.slice(0, 8);
        }
      } catch (error) {
        console.error('加载预览视频失败:', error);
        // 静默失败，不影响登录功能
      } finally {
        this.loadingVideos = false;
      }
    },

    // 【新增】预览视频（弹窗播放）
    previewVideo(video) {
      if (!video.videoUrl) {
        this.$message.warning('视频暂不可用，请登录后查看');
        this.showUserLoginDialog = true;
        return;
      }

      // 使用 Element UI 的 MessageBox 显示视频预览
      this.$msgbox({
        title: video.title || '视频预览',
        message: this.$createElement('div', {
          style: { padding: '20px', textAlign: 'center' }
        }, [
          this.$createElement('p', {
            style: { color: '#666', marginBottom: '15px', fontSize: '14px' }
          }, video.description || '暂无简介'),
          this.$createElement('video', {
            attrs: {
              controls: true,
              src: video.videoUrl,
              autoplay: false
            },
            style: {
              maxWidth: '100%',
              maxHeight: '500px',
              borderRadius: '8px',
              marginTop: '15px',
              backgroundColor: '#000'
            }
          }, '您的浏览器不支持视频播放'),
          this.$createElement('p', {
            style: { 
              color: '#999', 
              fontSize: '12px', 
              marginTop: '15px',
              padding: '10px',
              backgroundColor: '#f5f5f5',
              borderRadius: '4px'
            }
          }, '💡 提示：登录后可观看完整视频、点赞、评论等更多功能')
        ]),
        showCancelButton: false,
        confirmButtonText: '关闭',
        customClass: 'video-preview-dialog',
        beforeClose: (action, instance, done) => {
          // 关闭时暂停视频播放
          const videoElement = instance.$el.querySelector('video');
          if (videoElement) {
            videoElement.pause();
          }
          done();
        }
      });
    },

    // 【新增】格式化时长
    formatDuration(seconds) {
      if (!seconds) return '0:00';
      const mins = Math.floor(seconds / 60);
      const secs = Math.floor(seconds % 60);
      return `${mins}:${secs.toString().padStart(2, '0')}`;
    },

    // 【新增】格式化数字
    formatNumber(num) {
      if (!num) return '0';
      if (num >= 10000) {
        return (num / 10000).toFixed(1) + 'w';
      } else if (num >= 1000) {
        return (num / 1000).toFixed(1) + 'k';
      }
      return num.toString();
    },

    handleSearch() {
      this.$message.warning('请先登录后搜索');
      this.showUserLoginDialog = true;
    },

    // --- 核心：游客文字 5连击 ---
    handleGuestSecretClick() {
      const now = new Date().getTime();
      if (now - this.lastClickTime > 1000) {
        this.clickCount = 0;
      }
      this.clickCount++;
      this.lastClickTime = now;

      if (this.clickCount >= 5) {
        this.$message.success('进入管理员通道');
        this.clickCount = 0;
        this.showAdminLoginDialog = true;
      }
    },

    // --- 普通用户登录逻辑 (修复版) ---
    handleUserLogin() {
      this.$refs.userLoginForm.validate(async valid => {
        if (valid) {
          this.loading = true;
          try {
            // 1. 调用真实登录接口 (使用 auth 路径)
            const res = await request({
              url: '/api/auth/login',
              method: 'post',
              data: this.loginForm
            });

            // 2. 获取 Token
            // 假设后端返回结构为 { code: 200, data: { token: '...' } } 或直接返回 data
            // request.js 拦截器返回的是 response.data
            const token = res.data ? res.data.token : res.token;

            if (!token) {
              throw new Error('登录失败：未获取到Token');
            }

            // 3. 【关键修复】Token 必须同时存入 localStorage (供 request.js 使用) 和 sessionStorage
            localStorage.setItem('userToken', token);
            sessionStorage.setItem('userToken', token);

            // 4. 立即获取用户信息 (为了 UserMain 显示头像和用户名)
            // 因为 request.js 此时已经能从 localStorage 读到 token 了，所以可以直接调接口
            try {
              const userRes = await userVideoApi.getCurrentUser();
              const user = userRes.data;

              if (user) {
                // 存储用户信息到本地
                localStorage.setItem('username', user.nickname || user.username);
                localStorage.setItem('userAvatar', user.avatarUrl || '');
                localStorage.setItem('userId', user.id);
                localStorage.setItem('userRole', 'user');

                // 为了保险，sessionStorage 也存一份
                sessionStorage.setItem('username', user.nickname || user.username);
                sessionStorage.setItem('userAvatar', user.avatarUrl || '');
                sessionStorage.setItem('userId', user.id);
                sessionStorage.setItem('userRole', 'user');

                this.$message.success('欢迎回来，' + (user.nickname || user.username));
              }
            } catch (err) {
              console.warn('获取用户信息失败，但登录已成功', err);
              // 降级处理：使用登录表单的用户名
              localStorage.setItem('username', this.loginForm.username);
            }

            this.showUserLoginDialog = false;
            // 跳转到 UserMain 视频首页
            this.$router.push('/main/video');

          } catch (e) {
            console.error(e);
            // 错误提示已由 request.js 拦截器处理，这里只需处理 loading
          } finally {
            this.loading = false;
          }
        }
      });
    },

    // --- 管理员登录逻辑 ---
    handleAdminLogin() {
      this.$refs.adminLoginForm.validate(async valid => {
        if (valid) {
          this.loading = true;
          try {
            // 真实调用
            const res = await request({
              url: '/api/auth/login',
              method: 'post',
              data: this.adminForm
            });

            const token = res.data ? res.data.token : res.token;

            // 管理员 Token 存储
            sessionStorage.setItem('userToken', token); // 路由守卫放行
            sessionStorage.setItem('userRole', 'admin');

            // 同时也存 localStorage 以防 request.js 需要
            localStorage.setItem('userToken', token);
            localStorage.setItem('userRole', 'admin');

            this.$message.success('管理员登录成功');
            this.showAdminLoginDialog = false;
            this.$router.push('/admin/stats');

          } catch (e) {
            console.error(e);
          } finally {
            this.loading = false;
          }
        }
      });
    }
  }
};
</script>

<style scoped>
/* --- 全局容器 --- */
.guest-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

/* --- 顶部导航栏 (保持 UserMain 的紫色风格) --- */
.main-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 30px;
  height: 75px !important;
  box-shadow: 0 4px 20px rgba(102, 126, 234, 0.3);
  position: relative;
  z-index: 1000;
}

/* 左侧区域 */
.header-left-section {
  display: flex;
  align-items: center;
  gap: 40px;
}
.logo-text {
  margin: 0;
  font-size: 24px;
  font-weight: bold;
  letter-spacing: 1px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  white-space: nowrap;
}

/* 导航项 */
.horizontal-nav { display: flex; align-items: center; gap: 8px; }
.nav-item {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: default;
  padding: 10px 20px;
  border-radius: 8px;
  position: relative;
}
.nav-item.active { background: rgba(255, 255, 255, 0.15); }
.nav-item i { font-size: 18px; color: rgba(255, 255, 255, 0.9); }
.nav-text { font-size: 15px; font-weight: 500; color: white; }
.nav-indicator {
  position: absolute;
  bottom: 8px;
  left: 50%;
  transform: translateX(-50%);
  width: 30px;
  height: 3px;
  background: white;
  border-radius: 2px;
}

/* 右侧区域 */
.header-right-section { display: flex; align-items: center; gap: 20px; }

/* 搜索框 */
.header-search-wrapper { display: flex; align-items: center; gap: 8px; }
.header-search-input-wrapper {
  position: relative;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  padding: 8px 16px 8px 40px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  min-width: 280px;
}
.header-search-icon {
  position: absolute; left: 14px; top: 50%; transform: translateY(-50%);
  color: #9499a0; font-size: 16px;
}
.header-search-input {
  width: 100%; border: none; background: transparent; outline: none; font-size: 14px; color: #333;
}
.header-search-button {
  width: 40px; height: 40px; border: none;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%; cursor: pointer; color: white; font-size: 18px;
  transition: all 0.3s;
}
.header-search-button:hover { background: rgba(255, 255, 255, 0.3); }

/* 用户信息区 */
.header-user-info { display: flex; align-items: center; }
.user-info-guest { display: flex; align-items: center; gap: 12px; }
.guest-text { font-size: 14px; color: rgba(255, 255, 255, 0.8); transition: color 0.3s; }
.guest-text:hover { color: white; }
.login-btn-small {
  background: rgba(255, 255, 255, 0.2) !important;
  border: 1px solid rgba(255, 255, 255, 0.3) !important;
  color: white !important;
}
.login-btn-small:hover { background: rgba(255, 255, 255, 0.3) !important; }

/* 主内容区 */
.main-content-full {
  flex: 1;
  padding: 20px;
  display: flex;
  justify-content: center;
  overflow: auto;
}
.content-area-full { width: 100%; max-width: 1400px; }

/* 登录提示区域 */
.login-prompt-section {
  margin-bottom: 30px;
  text-align: center;
}

/* 预览视频区域 */
.preview-video-section {
  margin-bottom: 40px;
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-title i {
  color: #667eea;
}

.no-videos-tip {
  text-align: center;
  padding: 40px 0;
  color: #999;
}

.preview-video-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.preview-video-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
  cursor: pointer;
  transition: all 0.3s ease;
}

.preview-video-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.12);
}

.preview-cover {
  position: relative;
  width: 100%;
  padding-top: 56.25%; /* 16:9 比例 */
  background: #000;
  overflow: hidden;
}

.preview-cover img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.play-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.preview-video-card:hover .play-overlay {
  opacity: 1;
}

.play-overlay i {
  font-size: 48px;
  color: white;
  text-shadow: 0 2px 8px rgba(0,0,0,0.5);
}

.duration-badge {
  position: absolute;
  bottom: 8px;
  right: 8px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
}

.preview-info {
  padding: 12px;
}

.preview-title {
  font-size: 15px;
  font-weight: 500;
  color: #333;
  margin: 0 0 8px 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  height: 42px;
}

.preview-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #999;
}

.preview-author {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-stats {
  display: flex;
  gap: 12px;
  margin-left: 10px;
}

.preview-stats span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.preview-stats i {
  font-size: 14px;
}

/* 登录按钮区域 */
.login-button-section {
  text-align: center;
  padding: 30px 0;
  margin-top: 20px;
}

.main-login-btn {
  padding: 15px 40px;
  font-size: 16px;
  font-weight: 600;
}

/* 弹窗微调 */
::v-deep .el-dialog {
  border-radius: 12px;
}
::v-deep .admin-dialog .el-dialog__header {
  background: #f56c6c;
  border-radius: 12px 12px 0 0;
}
::v-deep .admin-dialog .el-dialog__title {
  color: white;
}
::v-deep .admin-dialog .el-dialog__close {
  color: white;
}

/* 视频预览弹窗样式 */
::v-deep .video-preview-dialog {
  max-width: 800px;
}
::v-deep .video-preview-dialog .el-message-box__content {
  padding: 0;
}
::v-deep .video-preview-dialog video {
  width: 100%;
  max-height: 500px;
}
</style>