<template>
  <div class="admin-login-container">
    <div class="login-box">
      <div class="login-header">
        <i class="el-icon-s-platform" style="font-size: 48px; color: #F56C6C; margin-bottom: 10px;"></i>
        <div class="title">后台管理系统</div>
        <div class="subtitle">Administrator Access Only</div>
      </div>

      <el-form :model="loginForm" :rules="rules" ref="loginForm" class="login-form">
        <el-form-item prop="username">
          <el-input v-model="loginForm.username" prefix-icon="el-icon-user-solid" placeholder="管理员账号"></el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input
              v-model="loginForm.password"
              prefix-icon="el-icon-key"
              type="password"
              placeholder="管理员密码"
              show-password
              @keyup.enter.native="handleAdminLogin"
          ></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="danger" class="login-btn" @click="handleAdminLogin" :loading="loading">
            进入后台
          </el-button>

          <div style="margin-top: 15px; text-align: center;">
            <el-link type="info" @click="$router.push('/')">返回首页</el-link>
          </div>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script>
// import { login } from '@/api/user'; // 使用真实的登录接口

export default {
  name: 'AdminLogin',
  data() {
    return {
      loginForm: { username: '', password: '' },
      rules: {
        username: [{ required: true, message: '请输入管理员账号', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
      },
      loading: false
    }
  },
  methods: {
    handleAdminLogin() {
      this.$refs.loginForm.validate(async valid => {
        if (valid) {
          this.loading = true;
          try {
            // 模拟登录请求 (实际请替换为 API 调用)
            // const res = await login(this.loginForm);

            // --- 模拟后端验证逻辑 ---
            if (this.loginForm.username !== 'admin') {
              throw new Error('非管理员账号禁止登录');
            }

            const mockRes = {
              token: 'admin-token-' + Date.now(),
              user: { username: 'admin', id: 999, role: 'admin' }
            };
            // ---------------------

            // 存储 Session (必须存 role: admin)
            sessionStorage.setItem('userToken', mockRes.token);
            sessionStorage.setItem('username', mockRes.user.username);
            sessionStorage.setItem('userId', mockRes.user.id);
            sessionStorage.setItem('userRole', mockRes.user.role);

            this.$message.success('管理员登录成功');

            // 跳转到后台首页
            this.$router.push('/admin/stats');

          } catch (error) {
            this.$message.error(error.message || '登录失败');
          } finally {
            this.loading = false;
          }
        }
      });
    }
  }
}
</script>

<style scoped>
.admin-login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  /* 使用深色背景区分 */
  background: #2b3248;
}
.login-box {
  width: 380px;
  padding: 40px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}
.login-header { text-align: center; margin-bottom: 30px; }
.title { font-size: 22px; font-weight: bold; color: #333; }
.subtitle { color: #999; font-size: 12px; margin-top: 5px; }
.login-btn { width: 100%; font-size: 16px; padding: 12px; }
</style>