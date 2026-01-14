<template>
  <div class="main-container">
    <!-- 顶部导航 -->
    <el-header class="main-header">
      <div class="header-left">
        <h2>短视频推荐系统</h2>
      </div>
      <div class="header-right">
        <span class="username">欢迎, {{ username }}</span>
        <el-button type="text" @click="handleLogout" style="color: white;">退出登录</el-button>
      </div>
    </el-header>

    <div class="main-content">
      <!-- 侧边菜单 -->
      <el-aside width="200px" class="side-menu">
        <el-menu
          :default-active="activeMenu"
          @select="handleMenuSelect"
          background-color="#545c64"
          text-color="#fff"
          active-text-color="#ffd04b"
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
      username: localStorage.getItem('username') || '用户',
      activeMenu: '/main/video'
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
        localStorage.removeItem('userToken')
        localStorage.removeItem('username')
        this.$router.push('/login')
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
  background-color: #409EFF;
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
  background-color: #545c64;
}

.content-area {
  padding: 20px;
  background-color: #f0f2f5;
}
</style>