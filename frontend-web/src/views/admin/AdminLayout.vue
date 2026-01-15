<template>
  <el-container class="admin-layout">
    <el-aside width="200px" style="background-color: #304156">
      <div class="logo">管理后台系统</div>
      <el-menu :default-active="$route.path" background-color="#304156" text-color="#bfcbd9" active-text-color="#409EFF" router style="border: none;">
        <el-menu-item index="/admin"><i class="el-icon-s-data"></i><span slot="title">数据看板</span></el-menu-item>
        <el-menu-item index="/admin/videos"><i class="el-icon-video-camera"></i><span slot="title">视频管理</span></el-menu-item>
        <el-menu-item index="/admin/users"><i class="el-icon-user"></i><span slot="title">用户管理</span></el-menu-item>
        <el-menu-item index="/admin/logs"><i class="el-icon-document"></i><span slot="title">操作日志</span></el-menu-item>
        <el-menu-item index="/admin/notice"><i class="el-icon-bell"></i><span slot="title">系统通知</span></el-menu-item> </el-menu>
    </el-aside>

    <el-container>
      <el-header class="admin-header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/admin' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ pageName }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-tooltip content="全屏模式" placement="bottom">
            <i class="el-icon-full-screen icon-btn" @click="toggleFullScreen"></i>
          </el-tooltip>

          <el-popover placement="bottom" width="300" trigger="click">
            <div class="notification-list">
              <div class="notify-header"><span>系统通知 (3)</span><el-button type="text" size="mini">全部已读</el-button></div>
              <div class="notify-item" v-for="i in 3" :key="i" style="padding: 10px; border-bottom: 1px solid #eee;">
                 <div style="font-weight: bold; font-size: 14px;">系统警告</div>
                 <div style="color: #666; font-size: 12px;">检测到服务器高负载，请注意查看。</div>
              </div>
              <div style="text-align: center; padding: 10px; color: #409EFF; cursor: pointer;">查看全部</div>
            </div>
            <div slot="reference" class="icon-btn" style="margin-right: 25px; line-height: initial; display: flex; align-items: center; height: 100%;">
              <el-badge :value="3" class="item" style="line-height: 20px;"><i class="el-icon-bell" style="font-size: 22px;"></i></el-badge>
            </div>
          </el-popover>

          <el-dropdown @command="handleCommand">
            <span class="el-dropdown-link" style="cursor: pointer; display: flex; align-items: center;">
              <el-avatar size="small" src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" style="margin-right: 10px;"></el-avatar>
              Admin <i class="el-icon-arrow-down el-icon--right"></i>
            </span>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="password" divided>修改密码</el-dropdown-item>
              <el-dropdown-item command="logout" style="color: #F56C6C;">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>
      </el-header>

      <el-main style="background-color: #f0f2f5; overflow-x: hidden;">
        <transition name="fade-transform" mode="out-in"><router-view></router-view></transition>
      </el-main>
    </el-container>
    
    <el-dialog title="修改管理员密码" :visible.sync="pwdDialogVisible" width="400px">
       <el-form :model="pwdForm" ref="pwdForm" label-width="80px">
        <el-form-item label="旧密码"><el-input v-model="pwdForm.oldPass"></el-input></el-form-item>
        <el-form-item label="新密码"><el-input v-model="pwdForm.newPass"></el-input></el-form-item>
       </el-form>
       <span slot="footer" class="dialog-footer">
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="pwdDialogVisible = false">确定</el-button>
      </span>
    </el-dialog>
  </el-container>
</template>

<script>
export default {
  name: 'AdminLayout',
  data() {
    return { pwdDialogVisible: false, pwdForm: { oldPass: '', newPass: '' } };
  },
  computed: {
    pageName() {
      const map = {
        'VideoManage': '视频管理', 'UserManage': '用户管理', 
        'AdminLogs': '操作日志', 'AdminStats': '数据看板',
        'SystemNotice': '系统通知' // 新增映射
      };
      return map[this.$route.name] || '当前页面';
    }
  },
  methods: {
    toggleFullScreen() {
      if (!document.fullscreenElement) document.documentElement.requestFullscreen();
      else if (document.exitFullscreen) document.exitFullscreen();
    },
    handleCommand(cmd) {
      if (cmd === 'logout') {
        localStorage.removeItem('adminToken');
        this.$router.push('/login');
      } else if (cmd === 'password') {
        this.pwdDialogVisible = true;
      }
    }
  }
}
</script>

<style scoped>
.admin-layout { height: 100vh; }
.logo { height: 60px; line-height: 60px; text-align: center; color: #fff; font-weight: bold; font-size: 18px; background-color: #2b3a4d; }
.admin-header { background-color: #fff; line-height: 60px; border-bottom: 1px solid #e6e6e6; display: flex; justify-content: space-between; padding: 0 20px; }
.header-left, .header-right { display: flex; align-items: center; }
.icon-btn { font-size: 20px; cursor: pointer; margin-right: 20px; color: #5a5e66; }
.fade-transform-leave-active, .fade-transform-enter-active { transition: all 0.5s; }
.fade-transform-enter { opacity: 0; transform: translateX(-30px); }
.fade-transform-leave-to { opacity: 0; transform: translateX(30px); }
.notification-list { padding: 0; }
.notify-header { display: flex; justify-content: space-between; padding: 10px 15px; border-bottom: 1px solid #eee; font-size: 14px; }
</style>