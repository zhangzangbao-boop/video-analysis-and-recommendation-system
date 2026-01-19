<template>
  <div class="main-container">
    <!-- 顶部导航栏 -->
    <el-header class="main-header">
      <!-- 左侧：标题 + 导航菜单 -->
      <div class="header-left-section">
        <h2 class="logo-text">短视频推荐系统</h2>
        
        <!-- 水平导航菜单 - 靠左 -->
        <div class="horizontal-nav">
          <div 
            v-for="menu in menuItems"
            :key="menu.path"
            :class="['nav-item', { active: activeMenu === menu.path }]"
            @click="handleMenuClick(menu)"
          >
            <i :class="menu.icon"></i>
            <span class="nav-text">{{ menu.title }}</span>
            <div class="nav-indicator" v-if="activeMenu === menu.path"></div>
          </div>
        </div>
      </div>
      
      <!-- 右侧：搜索 + 用户信息 -->
      <div class="header-right-section">
        <!-- 搜索框 -->
        <div class="header-search-wrapper">
          <div class="header-search-input-wrapper" @focus="showSearchSuggestions = true" @blur="hideSearchSuggestions">
            <i class="header-search-icon el-icon-search"></i>
            <input
              v-model="searchKeyword"
              type="text"
              placeholder="搜索视频..."
              class="header-search-input"
              @keyup.enter="handleSearch"
            />
            <!-- 搜索建议下拉 -->
            <div v-if="showSearchSuggestions && searchSuggestions.length > 0" class="search-suggestions-dropdown">
              <div 
                v-for="(suggestion, index) in searchSuggestions" 
                :key="index"
                class="suggestion-item"
                @mousedown="selectSuggestion(suggestion)"
              >
                <i class="el-icon-search"></i>
                <span>{{ suggestion }}</span>
              </div>
            </div>
          </div>
          <button class="header-search-button" @click="handleSearch">
            <i class="el-icon-search"></i>
          </button>
        </div>
        
        <!-- 用户信息 -->
        <div class="header-user-info">
          <div v-if="isLogin" class="user-info-logged">
            <el-avatar 
              :size="32" 
              :src="userAvatar || undefined"
              class="user-avatar-small"
            >
              <i class="el-icon-user-solid"></i>
            </el-avatar>
            <span class="username">{{ username }}</span>
            <el-button 
              type="text" 
              class="logout-btn"
              @click="handleLogout"
            >
              退出
            </el-button>
          </div>
          <div v-else class="user-info-guest">
            <span class="guest-text">游客</span>
            <el-button 
              size="small" 
              round 
              class="login-btn-small"
              @click="$router.push('/login')"
            >
              登录
            </el-button>
          </div>
        </div>
      </div>
    </el-header>

    <!-- 主内容区域 - 居中放大 -->
    <div class="main-content-full">
      <el-main class="content-area-full">
        <router-view></router-view>
      </el-main>
    </div>
  </div>
</template>

<script>
export default {
  name: 'UserMain',
  data() {
    return {
      activeMenu: '/main/video',
      searchKeyword: '',
      showSearchSuggestions: false,
      searchSuggestions: [],
      menuItems: [
        { path: '/main/video', title: '视频首页', icon: 'el-icon-video-camera' },
        { path: '/main/navigation', title: '导航系统', icon: 'el-icon-map-location' },
        { path: '/main/profile', title: '个人中心', icon: 'el-icon-user' }
      ]
    }
  },
  computed: {
    // 判断是否有 userToken（仅当通过登录页面登录时才有）
    isLogin() {
      return !!localStorage.getItem('userToken');
    },
    username() {
      // 只有登录用户才显示用户名
      if (this.isLogin) {
        return localStorage.getItem('username') || '用户';
      }
      return '游客';
    },
    userAvatar() {
      return localStorage.getItem('userAvatar') || '';
    }
  },
  created() {
    this.activeMenu = this.$route.path
  },
  watch: {
    $route(to) {
      this.activeMenu = to.path
    },
    searchKeyword(newVal) {
      // 简单的搜索建议（可以根据实际需求调用API）
      if (newVal && newVal.length > 0) {
        this.searchSuggestions = [
          `${newVal} 视频`,
          `${newVal} 相关`,
          `热门 ${newVal}`
        ].slice(0, 3);
      } else {
        this.searchSuggestions = [];
      }
    }
  },
  methods: {
    handleMenuClick(menu) {
      this.activeMenu = menu.path
      this.$router.push(menu.path)
    },
    handleLogout() {
      this.$confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        localStorage.removeItem('userToken');
        localStorage.removeItem('username');
        localStorage.removeItem('userId');
        localStorage.removeItem('userAvatar');
        // 刷新页面，状态变回游客
        location.reload(); 
      }).catch(() => {
        // 用户点击取消，什么都不做，直接关闭弹窗
      });
    },
    handleSearch() {
      if (this.searchKeyword.trim()) {
        // 跳转到导航系统页面并传递搜索关键词
        this.$router.push({
          path: '/main/navigation',
          query: { search: this.searchKeyword.trim() }
        });
      } else {
        this.$message.warning('请输入搜索关键词')
      }
    },
    hideSearchSuggestions() {
      // 延迟隐藏，允许点击建议项
      setTimeout(() => {
        this.showSearchSuggestions = false;
      }, 200);
    },
    selectSuggestion(suggestion) {
      this.searchKeyword = suggestion;
      this.showSearchSuggestions = false;
      this.handleSearch();
    }
  }
}
</script>

<style scoped>
.main-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.main-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 30px;
  height: 75px;
  box-shadow: 0 4px 20px rgba(102, 126, 234, 0.3);
  position: relative;
  z-index: 1000;
}

/* 左侧区域：标题 + 导航 */
.header-left-section {
  display: flex;
  align-items: center;
  gap: 40px;
  flex: 0 0 auto;
}

.logo-text {
  margin: 0;
  font-size: 24px;
  font-weight: bold;
  letter-spacing: 1px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  white-space: nowrap;
}

/* 水平导航栏 - 靠左 */
.horizontal-nav {
  display: flex;
  align-items: center;
  gap: 8px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 10px 20px;
  border-radius: 8px;
  transition: all 0.3s ease;
  position: relative;
  white-space: nowrap;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.1);
  transform: translateY(-2px);
}

.nav-item.active {
  background: rgba(255, 255, 255, 0.15);
}

.nav-item i {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.9);
}

.nav-text {
  font-size: 15px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.95);
}

.nav-indicator {
  position: absolute;
  bottom: 8px;
  left: 50%;
  transform: translateX(-50%);
  width: 20px;
  height: 3px;
  background: white;
  border-radius: 2px;
  animation: indicatorSlide 0.3s ease;
}

@keyframes indicatorSlide {
  from {
    width: 0;
    opacity: 0;
  }
  to {
    width: 30px;
    opacity: 1;
  }
}

/* 右侧区域：搜索 + 用户信息 */
.header-right-section {
  display: flex;
  align-items: center;
  gap: 20px;
  flex: 0 0 auto;
}

/* 搜索框样式 */
.header-search-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-search-input-wrapper {
  position: relative;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  padding: 8px 16px 8px 40px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  min-width: 280px;
}

.header-search-input-wrapper:focus-within {
  border-color: rgba(255, 255, 255, 0.6);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  background: white;
}

.header-search-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: #9499a0;
  font-size: 16px;
  z-index: 1;
}

.header-search-input {
  width: 100%;
  border: none;
  background: transparent;
  font-size: 14px;
  color: #18191c;
  outline: none;
  padding: 0;
}

.header-search-input::placeholder {
  color: #9499a0;
}

/* 搜索建议下拉 */
.search-suggestions-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  right: 0;
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  z-index: 1000;
  max-height: 300px;
  overflow-y: auto;
}

.suggestion-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.2s;
  color: #333;
}

.suggestion-item:hover {
  background: #f5f7fa;
}

.suggestion-item i {
  color: #9499a0;
  font-size: 16px;
}

.header-search-button {
  width: 40px;
  height: 40px;
  border: none;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 18px;
  backdrop-filter: blur(10px);
}

.header-search-button:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: scale(1.05);
}

/* 用户信息区域 */
.header-user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-info-logged {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 12px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 20px;
  backdrop-filter: blur(10px);
}

.user-avatar-small {
  border: 2px solid rgba(255, 255, 255, 0.3);
}

.username {
  font-size: 14px;
  font-weight: 500;
  color: white;
}

.logout-btn {
  color: rgba(255, 255, 255, 0.9) !important;
  padding: 0 8px !important;
  font-size: 13px !important;
}

.logout-btn:hover {
  color: white !important;
  background: rgba(255, 255, 255, 0.1) !important;
}

.user-info-guest {
  display: flex;
  align-items: center;
  gap: 12px;
}

.guest-text {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
}

.login-btn-small {
  background: rgba(255, 255, 255, 0.2) !important;
  border: 1px solid rgba(255, 255, 255, 0.3) !important;
  color: white !important;
  backdrop-filter: blur(10px);
}

.login-btn-small:hover {
  background: rgba(255, 255, 255, 0.3) !important;
}

/* 主内容区域 - 居中放大 */
.main-content-full {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8f0 100%);
  overflow: auto;
}

.content-area-full {
  width: 100%;
  max-width: 1400px;
  padding: 0;
  background: transparent;
  display: flex;
  flex-direction: column;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .header-search-input-wrapper {
    min-width: 200px;
  }
  
  .logo-text {
    font-size: 20px;
  }
  
  .nav-text {
    font-size: 14px;
  }
}

@media (max-width: 1024px) {
  .main-header {
    padding: 0 20px;
  }
  
  .header-left-section {
    gap: 20px;
  }
  
  .horizontal-nav {
    gap: 4px;
  }
  
  .nav-item {
    padding: 8px 12px;
  }
  
  .nav-item i {
    font-size: 16px;
  }
  
  .nav-text {
    font-size: 13px;
  }
  
  .header-search-input-wrapper {
    min-width: 180px;
  }
}

@media (max-width: 768px) {
  .main-header {
    flex-wrap: wrap;
    height: auto;
    padding: 15px;
    min-height: 75px;
  }
  
  .header-left-section {
    width: 100%;
    justify-content: space-between;
    margin-bottom: 15px;
  }
  
  .logo-text {
    font-size: 18px;
  }
  
  .horizontal-nav {
    gap: 4px;
  }
  
  .nav-item {
    padding: 8px 10px;
  }
  
  .nav-text {
    font-size: 12px;
  }
  
  .header-right-section {
    width: 100%;
    justify-content: space-between;
  }
  
  .header-search-input-wrapper {
    min-width: 150px;
    flex: 1;
  }
}

@media (max-width: 480px) {
  .main-header {
    padding: 12px;
  }
  
  .header-left-section {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .horizontal-nav {
    width: 100%;
    justify-content: flex-start;
  }
  
  .nav-item {
    padding: 6px 8px;
    flex: 1;
    justify-content: center;
  }
  
  .nav-item i {
    font-size: 14px;
  }
  
  .nav-text {
    font-size: 11px;
  }
  
  .header-right-section {
    flex-direction: column;
    gap: 10px;
    width: 100%;
  }
  
  .header-search-wrapper {
    width: 100%;
  }
  
  .header-search-input-wrapper {
    min-width: 0;
    flex: 1;
  }
  
  .header-user-info {
    width: 100%;
    justify-content: flex-end;
  }
  
  .main-content-full {
    padding: 10px;
  }
}
</style>