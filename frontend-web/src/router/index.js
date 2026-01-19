import Vue from 'vue'
import VueRouter from 'vue-router'

// --- 1. 引入公共页面 ---
import Login from '../views/Login.vue' // 使用你美化过的登录页

// --- 2. 引入队友的前台页面 (User) ---
import UserMain from '../views/user/UserMain.vue'
import VideoPlayer from '../views/user/VideoPlayer.vue'
import NavigationSystem from '../views/user/NavigationSystem.vue'
import UserProfile from '../views/user/UserProfile.vue'
import VideoHome from '../views/user/VideoHome.vue' // 新增视频首页

// --- 3. 引入你的后台页面 (Admin) ---
import AdminLayout from '../views/admin/AdminLayout.vue'
import AdminStats from '../views/admin/AdminStats.vue'
import VideoManage from '../views/admin/VideoManage.vue'
import UserManage from '../views/admin/UserManage.vue'
import AdminLogs from '../views/admin/AdminLogs.vue'
import SystemNotice from '../views/admin/SystemNotice.vue' // 新增

Vue.use(VueRouter)

const routes = [
  // --- 登录页 ---
  {
    path: '/login',
    name: 'Login',
    component: Login
  },

  // --- 队友的领地：用户前台 (嵌套路由) ---
  {
    path: '/main',
    component: UserMain, // 队友的主布局
    children: [
      { path: '', redirect: 'video' }, // 默认跳到视频页
      { path: 'video', name: 'VideoHome', component: VideoHome },
      { path: 'video/:id', name: 'VideoDetail', component: VideoPlayer }, // 视频详情页
      { path: 'navigation', name: 'UserNav', component: NavigationSystem },
      { path: 'profile', name: 'UserProfile', component: UserProfile }
    ]
  },

  // --- 你的领地：管理后台 (嵌套路由) ---
  {
    path: '/admin',
    component: AdminLayout, // 你的主布局
    children: [
      { path: '', name: 'AdminStats', component: AdminStats },
      { path: 'videos', name: 'VideoManage', component: VideoManage },
      { path: 'users', name: 'UserManage', component: UserManage },
      { path: 'logs', name: 'AdminLogs', component: AdminLogs },
      { path: 'notice', name: 'SystemNotice', component: SystemNotice } // 新增路由
    ]
  },

  // --- 根路径重定向 ---
  {
    path: '/',
    redirect: '/login' 
  }
]

const router = new VueRouter({
  mode: 'history',
  base: process.env.BASE_URL,
  routes
})

// --- 全局路由守卫 (双重安检) ---
router.beforeEach((to, from, next) => {
  const adminToken = localStorage.getItem('adminToken'); // 管理员令牌

  // 1. 如果要去【管理后台】
  if (to.path.startsWith('/admin')) {
    if (adminToken) {
      next();
    } else {
      Vue.prototype.$message.warning('请先登录管理员账号');
      next('/login');
    }
  } 
  // 2. 如果要去【用户前台】- 允许游客访问
  else if (to.path.startsWith('/main')) {
    next(); // 任何人（包括游客）都能访问
  } 
  // 3. 其他页面 (登录页等) 直接放行
  else {
    next();
  }
});

export default router