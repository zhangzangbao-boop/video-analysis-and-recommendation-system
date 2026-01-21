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
          <el-empty description="登录后解锁更多精彩视频">
            <el-button type="primary" @click="showUserLoginDialog = true">去登录</el-button>
          </el-empty>

          <div class="demo-card-grid">
            <div v-for="i in 8" :key="i" class="demo-card">
              <div class="demo-cover">
                <i class="el-icon-video-play" style="font-size: 40px; color: #fff;"></i>
              </div>
              <div class="demo-info">
                <div class="demo-title">游客预览视频 {{ i }}</div>
                <div class="demo-author">UP主: 演示用户</div>
              </div>
            </div>
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
  methods: {
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

/* 演示用的卡片样式 */
.demo-card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  margin-top: 20px;
  opacity: 0.5; /* 让背景看起来是未激活状态 */
  pointer-events: none; /* 禁止点击 */
}
.demo-card {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0,0,0,0.05);
}
.demo-cover {
  height: 160px;
  background: #e0e0e0;
  display: flex;
  align-items: center;
  justify-content: center;
}
.demo-info { padding: 12px; }
.demo-title { font-size: 15px; font-weight: 500; margin-bottom: 8px; }
.demo-author { font-size: 12px; color: #999; }

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
</style>