<template>
  <div class="main-container">
    <!-- 顶部导航栏 -->
    <el-header class="main-header">
      <!-- 轮播背景 -->
      <div class="banner-carousel" ref="bannerCarousel" @mouseenter="pauseCarousel" @mouseleave="resumeCarousel">
        <div
          v-for="(banner, index) in bannerImages"
          :key="index"
          class="banner-slide"
          :class="{ active: currentBanner === index }"
          :style="{ backgroundImage: banner.includes('linear-gradient') ? banner : `url(${banner})` }"
        ></div>
        <!-- 轮播指示器 -->
        <div class="banner-indicators">
          <div
            v-for="(banner, index) in bannerImages"
            :key="`dot-${index}`"
            class="banner-dot"
            :class="{ active: currentBanner === index }"
            @click="goToBanner(index)"
          ></div>
        </div>
      </div>
      <!-- 导航栏主体内容 -->
      <div class="header-main-content">
        <!-- 左侧：标题 + 导航菜单 -->
        <div class="header-left-section">
        <div class="logo-wrapper" @click="$router.push('/main/video')">
          <img src="@/logo/logo.png" class="logo-img" alt="logo">
          <h2 class="logo-text">推刻</h2>
        </div>

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
        
        <!-- 右侧：搜索 + 消息通知 + 用户信息 -->
        <div class="header-right-section">
          <!-- 消息通知图标 -->
          <div v-if="isLogin" class="message-notification" ref="messageNotification" @click="toggleMessageDropdown">
            <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="message-badge">
              <i class="el-icon-bell" :class="{ 'has-unread': unreadCount > 0 }"></i>
            </el-badge>
            <!-- 消息下拉列表 -->
            <div v-if="showMessageDropdown" class="message-dropdown" ref="messageDropdown" @click.stop="handleDropdownClick">
              <div class="message-header">
                <span>消息通知</span>
                <div>
                  <el-button type="text" size="mini" @click="markAllAsRead" v-if="unreadCount > 0">一键已读</el-button>
                  <el-button type="text" size="mini" @click="goToMessageCenter">查看全部</el-button>
                  <el-button type="text" size="mini" icon="el-icon-close" @click="showMessageDropdown = false" style="padding: 0 5px;"></el-button>
                </div>
              </div>
              <div class="message-list" v-loading="loadingMessages">
                <div v-if="messages.length === 0 && !loadingMessages" class="empty-message">
                  <p>暂无消息</p>
                </div>
                <div 
                  v-for="msg in messages" 
                  :key="msg.id" 
                  class="message-item"
                  :class="{ 'unread': msg.isRead === 0 }"
                  @click="viewMessage(msg)"
                >
                  <div class="message-icon">
                    <i :class="getMessageIcon(msg.type)"></i>
                  </div>
                  <div class="message-content">
                    <div class="message-title">{{ msg.title }}</div>
                    <div class="message-text">{{ msg.content }}</div>
                    <div class="message-time">{{ formatTime(msg.createTime) }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          
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
      </div>
      
      <!-- 热门分类导航 -->
      <div class="category-nav-section" v-if="categories.length > 0">
        <div class="category-nav-container">
          <div 
            v-for="category in categories.slice(0, 10)" 
            :key="category.id"
            class="category-nav-item"
            :class="{ active: selectedCategoryId === category.id }"
            @click="handleCategoryClick(category)"
          >
            <span class="category-name">{{ category.name }}</span>
            <span v-if="category.videoCount" class="category-count">({{ category.videoCount }})</span>
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
import { userVideoApi } from '@/api/user'

export default {
  name: 'UserMain',
  data() {
    // 安全获取 banner 图片，如果不存在则使用渐变背景
    const getBannerImages = () => {
      const defaultGradients = [
        'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
        'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)'
      ]
      
      try {
        // 尝试从 web-front 目录加载图片文件
        const banner1 = require('@/web-front/bachalpsee-4334666_1920.jpg')
        const banner2 = require('@/web-front/mountain-climbing-2124113_1920.jpg')
        const banner3 = require('@/web-front/mountains-4695049_1920.jpg')
        return [banner1, banner2, banner3]
      } catch (e) {
        // 如果图片不存在，使用渐变背景作为降级方案
        console.warn('Banner 图片未找到，使用渐变背景作为降级方案', e)
        return defaultGradients
      }
    }
    
    return {
      activeMenu: '/main/video',
      searchKeyword: '',
      showSearchSuggestions: false,
      searchSuggestions: [],
      menuItems: [
        { path: '/main/video', title: '视频首页', icon: 'el-icon-video-camera' },
        { path: '/main/navigation', title: '导航系统', icon: 'el-icon-map-location' },
        { path: '/main/profile', title: '个人中心', icon: 'el-icon-user' }
      ],
      // 消息通知相关
      showMessageDropdown: false,
      unreadCount: 0,
      messages: [],
      loadingMessages: false,
      messageTimer: null,
      // 用户信息
      userInfo: null,
      // 轮播相关 - 优先使用图片文件，如果不存在则使用渐变背景
      bannerImages: getBannerImages(),
      currentBanner: 0,
      carouselInterval: null,
      isPaused: false,
      // 分类相关
      categories: [],
      selectedCategoryId: null
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
        // 优先显示nickname，如果没有则显示username
        if (this.userInfo && this.userInfo.nickname) {
          return this.userInfo.nickname;
        }
        // 从localStorage获取（登录时已保存nickname或username）
        return localStorage.getItem('username') || '用户';
      }
      return '游客';
    },
    userAvatar() {
      // 优先使用从后端获取的头像
      if (this.userInfo && this.userInfo.avatarUrl) {
        return this.userInfo.avatarUrl;
      }
      return localStorage.getItem('userAvatar') || '';
    }
  },
  created() {
    this.activeMenu = this.$route.path
    this.loadCategories()
    if (this.isLogin) {
      this.loadUserInfo()
      this.loadUnreadCount()
      this.loadMessages()
      // 每30秒刷新一次未读消息数
      this.messageTimer = setInterval(() => {
        this.loadUnreadCount()
      }, 30000)
    }
  },
  mounted() {
    // 在 mounted 后添加点击外部区域关闭消息下拉框的监听
    // 使用延迟执行，确保 DOM 已渲染
    this.$nextTick(() => {
      document.addEventListener('click', this.handleClickOutside)
    })
    // 启动轮播
    this.startCarousel()
  },
  beforeDestroy() {
    if (this.messageTimer) {
      clearInterval(this.messageTimer)
    }
    if (this.carouselInterval) {
      clearInterval(this.carouselInterval)
    }
    // 移除事件监听
    document.removeEventListener('click', this.handleClickOutside)
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
        // 1. 清除 localStorage (持久化存储)
        localStorage.removeItem('userToken');
        localStorage.removeItem('username');
        localStorage.removeItem('userId');
        localStorage.removeItem('userAvatar');
        localStorage.removeItem('userRole');

        // 2. 【新增】清除 sessionStorage (会话存储，路由守卫主要检查这里)
        sessionStorage.clear(); // 或者逐个移除: sessionStorage.removeItem('userToken')...

        this.$message.success('已退出登录');

        // 3. 【修改】跳转回登录页(即游客首页)
        this.$router.push('/login');
      }).catch(() => {
        // 取消操作
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
    },
    
    // 加载用户信息
    async loadUserInfo() {
      try {
        const res = await userVideoApi.getCurrentUser()
        if (res.code === 200 && res.data) {
          this.userInfo = res.data
          // 更新localStorage中的用户信息
          if (res.data.nickname) {
            localStorage.setItem('username', res.data.nickname)
          }
          if (res.data.avatarUrl) {
            localStorage.setItem('userAvatar', res.data.avatarUrl)
          }
        }
      } catch (error) {
        console.error('加载用户信息失败:', error)
      }
    },
    
    // 消息通知相关方法
    toggleMessageDropdown(event) {
      // 阻止事件冒泡，避免触发 handleClickOutside
      if (event) {
        event.stopPropagation()
        event.preventDefault()
      }
      const wasOpen = this.showMessageDropdown
      this.showMessageDropdown = !this.showMessageDropdown
      if (this.showMessageDropdown && !wasOpen) {
        // 只有在打开时才加载消息
        this.loadMessages()
      }
    },
    
    // 处理下拉框内部点击
    handleDropdownClick(event) {
      // 阻止事件冒泡到 document，避免触发 handleClickOutside
      event.stopPropagation()
    },
    
    async loadUnreadCount() {
      try {
        const res = await userVideoApi.getUnreadMessageCount()
        if (res.code === 200) {
          this.unreadCount = res.data || 0
        }
      } catch (error) {
        console.error('加载未读消息数失败:', error)
      }
    },
    
    async loadMessages() {
      this.loadingMessages = true
      try {
        const res = await userVideoApi.getMessageList({ limit: 10 })
        if (res.code === 200) {
          this.messages = res.data || []
        }
      } catch (error) {
        console.error('加载消息列表失败:', error)
      } finally {
        this.loadingMessages = false
      }
    },
    
    async markAllAsRead() {
      try {
        const res = await userVideoApi.markAllMessagesAsRead()
        if (res.code === 200) {
          this.$message.success('已全部标记为已读')
          this.unreadCount = 0
          this.messages.forEach(msg => msg.isRead = 1)
        }
      } catch (error) {
        console.error('标记已读失败:', error)
        this.$message.error('操作失败')
      }
    },
    
    async viewMessage(msg) {
      // 如果未读，标记为已读
      if (msg.isRead === 0) {
        try {
          await userVideoApi.markMessageAsRead(msg.id)
          msg.isRead = 1
          this.unreadCount = Math.max(0, this.unreadCount - 1)
        } catch (error) {
          console.error('标记已读失败:', error)
        }
      }
      
      // 跳转到消息详情或相关页面
      if (msg.type === 'LIKE' || msg.type === 'COLLECT' || msg.type === 'COMMENT') {
        if (msg.relatedVideoId) {
          this.$router.push(`/main/video/${msg.relatedVideoId}`)
        }
      } else if (msg.type === 'FOLLOW') {
        if (msg.relatedUserId) {
          // 可以跳转到用户主页
          this.$message.info('用户主页功能开发中')
        }
      } else if (msg.type === 'SYSTEM') {
        // 跳转到消息详情页
        this.$router.push(`/main/message/${msg.id}`)
      }
      
      this.showMessageDropdown = false
    },
    
    goToMessageCenter() {
      this.$router.push('/main/message')
      this.showMessageDropdown = false
    },
    
    getMessageIcon(type) {
      const iconMap = {
        'LIKE': 'el-icon-thumb',
        'COMMENT': 'el-icon-chat-dot-round',
        'COLLECT': 'el-icon-star-on',
        'FOLLOW': 'el-icon-user',
        'SYSTEM': 'el-icon-bell'
      }
      return iconMap[type] || 'el-icon-message'
    },
    
    formatTime(time) {
      if (!time) return ''
      const date = new Date(time)
      const now = new Date()
      const diff = now - date
      const minutes = Math.floor(diff / 60000)
      const hours = Math.floor(diff / 3600000)
      const days = Math.floor(diff / 86400000)
      
      if (minutes < 1) return '刚刚'
      if (minutes < 60) return `${minutes}分钟前`
      if (hours < 24) return `${hours}小时前`
      if (days < 7) return `${days}天前`
      return time.substring(0, 10)
    },
    
    // 处理点击外部区域关闭消息下拉框
    handleClickOutside(event) {
      // 如果下拉框未打开，直接返回
      if (!this.showMessageDropdown) {
        return
      }

      const messageNotification = this.$refs.messageNotification
      const messageDropdown = this.$refs.messageDropdown

      if (!messageNotification || !messageDropdown) {
        return
      }

      // 获取 DOM 元素
      const notificationEl = messageNotification.$el || messageNotification
      const dropdownEl = messageDropdown.$el || messageDropdown

      // 如果点击的不是消息通知区域和下拉框，则关闭下拉框
      if (notificationEl && dropdownEl) {
        const target = event.target
        if (!notificationEl.contains(target) && !dropdownEl.contains(target)) {
          this.showMessageDropdown = false
        }
      }
    },

    // 轮播相关方法
    startCarousel() {
      this.carouselInterval = setInterval(() => {
        if (!this.isPaused) {
          this.nextBanner()
        }
      }, 2000) // 2秒切换一次
    },

    nextBanner() {
      this.currentBanner = (this.currentBanner + 1) % this.bannerImages.length
    },

    goToBanner(index) {
      this.currentBanner = index
    },

    pauseCarousel() {
      this.isPaused = true
    },

    resumeCarousel() {
      this.isPaused = false
    },
    
    // 加载分类列表
    async loadCategories() {
      try {
        const res = await userVideoApi.getCategories()
        if (res.code === 200 && res.data) {
          this.categories = res.data
        }
      } catch (error) {
        console.error('加载分类列表失败:', error)
      }
    },
    
    // 处理分类点击
    handleCategoryClick(category) {
      this.selectedCategoryId = category.id
      // 跳转到导航系统页面并传递分类ID
      this.$router.push({
        path: '/main/navigation',
        query: { categoryId: category.id }
      })
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
  color: white;
  display: flex;
  flex-direction: column;
  padding: 0;
  position: relative;
  z-index: 1000;
  overflow: hidden; /* 恢复hidden，防止内容溢出 */
}

/* 导航栏主体 - 参考B站设计，增加高度以匹配logo */
.header-main-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 60px; /* 恢复合理的上下padding */
  height: 100px; /* 使用固定高度，确保布局稳定 */
  position: relative;
  z-index: 10;
  box-sizing: border-box;
}

/* 响应式：移动端高度和布局 */
@media (max-width: 768px) {
  .header-main-content {
    height: 70px; /* 移动端固定高度 */
    padding: 10px 15px; /* 移动端合理的padding */
    box-sizing: border-box;
  }
  
  .banner-carousel {
    height: 70px; /* 移动端匹配高度 */
  }
  
  .logo-img {
    height: 35px; /* 移动端logo高度 */
  }
  
  .category-nav-section {
    padding: 8px 15px;
  }
  
  .category-nav-container {
    gap: 6px;
  }
  
  .category-nav-item {
    padding: 6px 12px;
    font-size: 12px;
  }
  
  .header-left-section {
    gap: 15px;
  }
  
  .horizontal-nav {
    gap: 4px;
  }
  
  .nav-item {
    padding: 6px 12px;
    font-size: 12px;
  }
  
  .nav-item i {
    font-size: 14px;
  }
  
  .nav-text {
    font-size: 12px;
  }
  
  .banner-indicators {
    bottom: 4px; /* 移动端指示器位置 */
  }
  
  .banner-dot {
    width: 6px;
    height: 6px;
  }
}

/* 轮播背景容器 - 参考B站，覆盖整个导航栏主体 */
.banner-carousel {
  position: absolute;
  top: 0;
  left: -20px; /* 左侧延伸20px，营造溢出感 */
  right: -20px; /* 右侧延伸20px，营造溢出感 */
  width: calc(100% + 40px); /* 总宽度比导航栏多40px */
  height: 100px; /* 匹配导航栏主体高度 */
  overflow: hidden;
  z-index: 1;
}

/* 响应式：移动端轮播背景高度 */
@media (max-width: 768px) {
  .banner-carousel {
    height: 120px; /* 移动端导航栏主体高度 */
  }
}

/* 热门分类导航区域 */
.category-nav-section {
  width: 100%;
  background: rgba(0, 0, 0, 0.15);
  backdrop-filter: blur(10px);
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  padding: 10px 60px; /* 减少padding */
  position: relative;
  z-index: 10;
  min-height: 50px; /* 确保有足够高度 */
  box-sizing: border-box;
}

.category-nav-container {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  max-width: 1400px;
  margin: 0 auto;
}

.category-nav-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.1);
  color: white;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.category-nav-item:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.category-nav-item.active {
  background: rgba(255, 255, 255, 0.25);
  border-color: rgba(255, 255, 255, 0.4);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.category-name {
  font-size: 14px;
}

.category-count {
  font-size: 12px;
  opacity: 0.8;
}

/* 单个轮播图片 */
.banner-slide {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  transition: opacity 0.5s ease-in-out;
  /* 图片覆盖整个容器，保持宽高比，从顶部开始显示 */
  background-size: cover;
  background-position: center top;
  background-repeat: no-repeat;
  /* 稍微拉宽图片，营造更宽的视觉效果 */
  transform: scaleX(1.05);
}

.banner-slide.active {
  opacity: 1;
}

/* 轮播指示器 - 定位在导航栏主体区域的底部 */
.banner-indicators {
  position: absolute;
  bottom: 8px; /* 距离轮播容器底部8px */
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 6px;
  z-index: 2;
}

.banner-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  transition: all 0.3s ease;
}

.banner-dot.active {
  /* 简约的白色圆点指示器 */
  background: white;
  transform: scale(1.3);
  box-shadow: 0 0 8px rgba(255, 255, 255, 0.5);
}

/* 左侧区域 */
.header-left-section {
  display: flex;
  align-items: center;
  gap: 40px; /* 减少间距，避免元素过宽 */
  flex: 0 0 auto;
  position: relative;
  z-index: 10; /* 确保在背景图上方 */
}

/* --- 新增：Logo 容器 --- */
.logo-wrapper {
  display: flex;
  align-items: center;
  gap: 12px; /* 图片和文字之间的间距 */
  cursor: pointer;
  transition: opacity 0.3s;
}
.logo-wrapper:hover {
  opacity: 0.9;
}

/* --- 新增：Logo 图片 --- */
.logo-img {
  height: 45px; /* 调整logo高度，与导航栏协调 */
  width: auto;
  object-fit: contain;
}

/* Logo文字样式：白色 + 2px 黑色描边 */
.logo-text {
  margin: 0;
  font-size: 24px;
  font-weight: bold;
  letter-spacing: 1px;
  color: white;
  text-shadow: 
    -2px -2px 0 #000,
    2px -2px 0 #000,
    -2px 2px 0 #000,
    2px 2px 0 #000,
    0 -2px 0 #000,
    0 2px 0 #000,
    -2px 0 0 #000,
    2px 0 0 #000;
  white-space: nowrap;
  position: relative;
  z-index: 10;
}

/* 响应式：移动端Logo文字 */
@media (max-width: 768px) {
  .logo-text {
    font-size: 18px;
  }
  .logo-img {
    height: 30px;
  }
}

/* 水平导航栏 - 靠左 */
.horizontal-nav {
  display: flex;
  align-items: center;
  gap: 8px; /* 减少间距 */
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  padding: 8px 16px; /* 减少padding，避免元素过大 */
  border-radius: 8px;
  transition: all 0.3s ease;
  position: relative;
  white-space: nowrap;
  /* 默认样式：半透明黑色背景 */
  background: rgba(0, 0, 0, 0.3);
  z-index: 10; /* 确保在背景图上方 */
  font-size: 14px; /* 确保字体大小合适 */
}

.nav-item:hover {
  /* hover效果：白色背景 + 上移2px */
  background: rgba(255, 255, 255, 0.8);
  transform: translateY(-2px);
}

.nav-item:hover .nav-text {
  /* hover时文字颜色变深 */
  color: #18191c;
  text-shadow: none;
}

.nav-item:hover i {
  /* hover时图标颜色变深 */
  color: #18191c;
}

.nav-item.active {
  background: rgba(255, 255, 255, 0.2);
}

.nav-item i {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.9);
  transition: all 0.3s ease;
}

.nav-text {
  font-size: 15px;
  font-weight: 500;
  /* 白色文字 + 2px 黑色描边，保证可读性 */
  color: white;
  text-shadow: 
    -2px -2px 0 #000,
    2px -2px 0 #000,
    -2px 2px 0 #000,
    2px 2px 0 #000,
    0 -2px 0 #000,
    0 2px 0 #000,
    -2px 0 0 #000,
    2px 0 0 #000;
  transition: all 0.3s ease;
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
  gap: 15px; /* 减少间距 */
  flex: 0 0 auto;
  position: relative;
  z-index: 10; /* 确保在背景图上方 */
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
  border-radius: 15px;
  padding: 8px 16px 8px 40px;
  border: 2px solid #ccc;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  width: 300px;
  min-width: 200px;
}

.header-search-input-wrapper:focus-within {
  border: 2px solid transparent;
  background: linear-gradient(white, white) padding-box,
              linear-gradient(45deg, #ff0000, #ff9900, #ffff00, #00ff00, #0099ff, #9900ff) border-box;
  background-size: 400% 400%;
  animation: rainbowBorder 2s ease-in-out infinite;
  box-shadow: 0 0 8px rgba(255, 0, 0, 0.3);
}

@keyframes rainbowBorder {
  0%, 100% { background-position: 0% 0%; }
  16.67% { background-position: 100% 0%; }
  33.33% { background-position: 100% 100%; }
  50% { background-position: 0% 100%; }
  66.67% { background-position: -100% 100%; }
  83.33% { background-position: -100% 0%; }
}

.header-search-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: #9499a0;
  font-size: 16px;
  z-index: 1;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.5s ease-in-out;
}

.header-search-input-wrapper:focus-within .header-search-icon {
  animation: iconRotate 0.5s ease-in-out, rainbowColor 0.2s ease-in-out infinite;
}

@keyframes iconRotate {
  from {
    transform: translateY(-50%) rotate(0deg);
  }
  to {
    transform: translateY(-50%) rotate(180deg);
  }
}

@keyframes rainbowColor {
  0%, 100% { color: #ff0000; }
  16.67% { color: #ff9900; }
  33.33% { color: #ffff00; }
  50% { color: #00ff00; }
  66.67% { color: #0099ff; }
  83.33% { color: #9900ff; }
}

.header-search-input {
  width: 100%;
  border: none;
  background: transparent;
  font-size: 14px;
  color: #18191c;
  outline: none;
  padding: 0;
  height: 24px;
  line-height: 24px;
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

/* 消息通知样式 */
.message-notification {
  position: relative;
  cursor: pointer;
  padding: 8px;
  border-radius: 50%;
  transition: all 0.3s;
  margin-right: 10px;
}

.message-notification:hover {
  background: rgba(255, 255, 255, 0.1);
}

.message-notification i {
  font-size: 22px;
  color: rgba(255, 255, 255, 0.9);
}

.message-notification i.has-unread {
  color: #ff6b6b;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

.message-badge {
  cursor: pointer;
}

.message-dropdown {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  width: 380px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  z-index: 2000;
  max-height: 500px;
  display: flex;
  flex-direction: column;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  border-bottom: 1px solid #f0f0f0;
  font-weight: 600;
  font-size: 16px;
}

.message-list {
  max-height: 400px;
  overflow-y: auto;
  padding: 8px;
}

.empty-message {
  text-align: center;
  padding: 40px 20px;
  color: #999;
}

.message-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
  border-left: 3px solid transparent;
}

.message-item:hover {
  background: #f8f9fa;
}

.message-item.unread {
  background: #f0f7ff;
  border-left-color: #409EFF;
  font-weight: 500;
}

.message-icon {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: #409EFF;
}

.message-content {
  flex: 1;
  min-width: 0;
}

.message-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-text {
  font-size: 13px;
  color: #666;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.message-time {
  font-size: 12px;
  color: #999;
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
  margin-top: 0; /* 确保没有额外的上边距 */
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
  .header-main-content {
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