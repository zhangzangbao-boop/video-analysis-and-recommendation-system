<template>
  <div class="main-container">
    <!-- 顶部导航 -->
    <el-header class="main-header">
      <div class="header-left">
        <h2>短视频推荐系统</h2>
      </div>
      <div class="header-right">
        <div v-if="isLogin" style="display: flex; align-items: center;">
          <span class="username">欢迎, {{ username }}</span>
          <el-button type="text" @click="handleLogout" style="color: rgba(255,255,255,0.8); margin-left: 15px;">退出</el-button>
        </div>
        <div v-else>
          <span class="username" style="margin-right: 15px;">当前身份：游客</span>
          <el-button size="small" round @click="$router.push('/login')">去登录</el-button>
        </div>
      </div>
    </el-header>

    <div class="main-content">
      <!-- 侧边菜单 -->
      <el-aside width="200px" class="side-menu">
        <el-menu
          :default-active="activeMenu"
          @select="handleMenuSelect"
          background-color=" #56586d"
          text-color="#fff"
          active-text-color="#0064fa"
          router
        >
          <el-menu-item index="/main/video">
            <i class="el-icon-video-camera"></i>
            <span slot="title">短视频播放器</span>
          </el-menu-item>
          <el-menu-item index="/main/navigation">
            <i class="el-icon-map-location"></i>
            <span slot="title">导航系统</span>
          </el-menu-item>
          <el-menu-item index="/main/profile">
            <i class="el-icon-user"></i>
            <span slot="title">个人中心</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <!-- 内容区域 -->
      <el-main class="content-area">
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
      activeMenu: '/main/video'
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
    }
  },
  created() {
    // 设置当前激活的菜单
    this.activeMenu = this.$route.path
  },
  watch: {
    $route(to) {
      this.activeMenu = to.path
    }
  },
  methods: {
    handleMenuSelect(index) {
      this.activeMenu = index
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
        // 刷新页面，状态变回游客
        location.reload(); 
      }).catch(() => {
        // 用户点击取消，什么都不做，直接关闭弹窗
      });
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
  background-color: #56586d;
  color: white;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
}

.header-left h2 {
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.username {
  font-size: 16px;
}

.main-content {
  flex: 1;
  display: flex;
}

.side-menu {
  background-color: #56586d;
}

.content-area {
  padding: 20px;
  background-color: #f0f2f5;
}

/* 侧边栏菜单项字体大小 */
.side-menu .el-menu-item {
  font-size: 18px !important; /* 默认14px，这里设为16px */
  height: 50px !important;     /* 增加高度，让字体有更多空间 */
  line-height: 50px !important; /* 垂直居中 */
}

/* 菜单项图标大小 */
.side-menu .el-menu-item .el-icon {
  font-size: 20px !important; /* 图标也相应放大 */
  margin-right: 8px;
}
</style>