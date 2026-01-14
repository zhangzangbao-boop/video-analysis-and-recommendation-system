<template>
  <el-container class="admin-layout">
    <el-aside width="200px" style="background-color: #304156">
      <div class="logo">管理后台系统</div>
      <el-menu 
        :default-active="$route.path" 
        background-color="#304156" 
        text-color="#bfcbd9" 
        active-text-color="#409EFF" 
        router 
        style="border: none;"
      >
        <el-menu-item index="/admin"><i class="el-icon-s-data"></i><span slot="title">数据看板</span></el-menu-item>
        <el-menu-item index="/admin/videos"><i class="el-icon-video-camera"></i><span slot="title">视频管理</span></el-menu-item>
        <el-menu-item index="/admin/users"><i class="el-icon-user"></i><span slot="title">用户管理</span></el-menu-item>
        <el-menu-item index="/admin/logs"><i class="el-icon-document"></i><span slot="title">操作日志</span></el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="admin-header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/admin' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ $route.name === 'AdminStats' ? '数据看板' : pageName }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        
        <div class="header-right">
          <el-tooltip content="全屏模式" placement="bottom">
            <i class="el-icon-full-screen icon-btn" @click="toggleFullScreen"></i>
          </el-tooltip>

          <el-popover
            placement="bottom"
            width="300"
            trigger="click"
            popper-class="notification-popover"
          >
            <div class="notification-list">
              <div class="notify-header">
                <span>系统通知 (3)</span>
                <el-button type="text" size="mini">全部已读</el-button>
              </div>
              <div class="notify-item">
                <div class="notify-title warning"><i class="el-icon-warning"></i> 系统告警</div>
                <div class="notify-content">检测到服务器 CPU 占用率超过 80%，请及时排查。</div>
                <div class="notify-time">10分钟前</div>
              </div>
              <div class="notify-item">
                <div class="notify-title success"><i class="el-icon-user"></i> 新用户注册</div>
                <div class="notify-content">今日新增注册用户突破 500 人。</div>
                <div class="notify-time">30分钟前</div>
              </div>
              <div class="notify-item" style="border-bottom: none;">
                <div class="notify-title info"><i class="el-icon-video-camera"></i> 审核提醒</div>
                <div class="notify-content">您有 12 个视频待审核，请尽快处理。</div>
                <div class="notify-time">1小时前</div>
              </div>
              <div class="notify-footer">查看全部通知</div>
            </div>
            
            <div slot="reference" class="icon-btn" style="margin-right: 25px; line-height: initial; display: flex; align-items: center; height: 100%;">
              <el-badge :value="3" class="item" :max="99" style="line-height: 20px;">
                <i class="el-icon-bell" style="font-size: 22px;"></i>
              </el-badge>
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
        <transition name="fade-transform" mode="out-in">
          <router-view></router-view>
        </transition>
      </el-main>
    </el-container>

    <el-dialog title="修改管理员密码" :visible.sync="pwdDialogVisible" width="400px">
      <el-form :model="pwdForm" :rules="pwdRules" ref="pwdForm" label-width="80px">
        <el-form-item label="旧密码" prop="oldPass"><el-input v-model="pwdForm.oldPass" type="password" show-password></el-input></el-form-item>
        <el-form-item label="新密码" prop="newPass"><el-input v-model="pwdForm.newPass" type="password" show-password></el-input></el-form-item>
        <el-form-item label="确认密码" prop="checkPass"><el-input v-model="pwdForm.checkPass" type="password" show-password></el-input></el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="pwdDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitPwdChange">确 定</el-button>
      </span>
    </el-dialog>
  </el-container>
</template>

<script>
export default {
  name: 'AdminLayout',
  data() {
    var validatePass2 = (rule, value, callback) => {
      if (value !== this.pwdForm.newPass) callback(new Error('两次输入密码不一致!'));
      else callback();
    };
    return {
      pwdDialogVisible: false,
      pwdForm: { oldPass: '', newPass: '', checkPass: '' },
      pwdRules: {
        oldPass: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
        newPass: [{ required: true, message: '请输入新密码', trigger: 'blur' }, { min: 6, message: '长度至少6位', trigger: 'blur' }],
        checkPass: [{ required: true, message: '请再次输入新密码', trigger: 'blur' }, { validator: validatePass2, trigger: 'blur' }]
      }
    };
  },
  computed: {
    pageName() {
      const map = {
        'VideoManage': '视频管理',
        'UserManage': '用户管理',
        'AdminLogs': '操作日志'
      };
      return map[this.$route.name] || '当前页面';
    }
  },
  methods: {
    toggleFullScreen() {
      if (!document.fullscreenElement) {
        document.documentElement.requestFullscreen();
      } else {
        if (document.exitFullscreen) document.exitFullscreen();
      }
    },
    handleCommand(command) {
      if (command === 'logout') this.handleLogout();
      else if (command === 'password') {
        this.pwdDialogVisible = true;
        if(this.$refs.pwdForm) this.$refs.pwdForm.resetFields(); 
      }
    },
    handleLogout() {
      this.$confirm('确定要退出管理后台吗?', '提示', { type: 'warning' }).then(() => {
        localStorage.removeItem('adminToken');
        this.$router.push('/login');
        this.$message.success('已安全退出');
      }).catch(() => {});
    },
    submitPwdChange() {
      this.$refs.pwdForm.validate((valid) => {
        if (valid) {
          this.pwdDialogVisible = false;
          this.$message.success('密码修改成功，请重新登录');
          this.handleLogout();
        }
      });
    }
  }
}
</script>

<style scoped>
.admin-layout { height: 100vh; }
.logo { height: 60px; line-height: 60px; text-align: center; color: #fff; font-weight: bold; font-size: 18px; background-color: #2b3a4d; }
.admin-header { background-color: #fff; line-height: 60px; border-bottom: 1px solid #e6e6e6; display: flex; justify-content: space-between; padding: 0 20px; }
.header-left { display: flex; align-items: center; }
.header-right { display: flex; align-items: center; }

/* 顶部图标按钮通用样式 */
.icon-btn {
  font-size: 20px;
  cursor: pointer;
  margin-right: 20px;
  color: #5a5e66;
  transition: color 0.3s;
}
.icon-btn:hover { color: #409EFF; }

.el-dropdown-link:hover { color: #409EFF; }

/* 页面切换动画 */
.fade-transform-leave-active,
.fade-transform-enter-active { transition: all 0.5s; }
.fade-transform-enter { opacity: 0; transform: translateX(-30px); }
.fade-transform-leave-to { opacity: 0; transform: translateX(30px); }

/* --- 通知列表样式 --- */
.notification-list {
  padding: 0;
}
.notify-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 15px;
  border-bottom: 1px solid #eee;
  font-size: 14px;
}
.notify-item {
  padding: 12px 15px;
  border-bottom: 1px solid #f0f0f0;
  transition: background 0.2s;
}
.notify-item:hover { background: #f9f9f9; }
.notify-title { font-size: 14px; margin-bottom: 5px; font-weight: bold; display: flex; align-items: center; }
.notify-title.warning { color: #E6A23C; }
.notify-title.success { color: #67C23A; }
.notify-title.info { color: #409EFF; }
.notify-title i { margin-right: 5px; }
.notify-content { font-size: 12px; color: #666; line-height: 1.4; }
.notify-time { font-size: 12px; color: #999; margin-top: 5px; text-align: right; }
.notify-footer {
  text-align: center;
  padding: 10px;
  color: #409EFF;
  cursor: pointer;
  font-size: 13px;
  border-top: 1px solid #eee;
}
</style>