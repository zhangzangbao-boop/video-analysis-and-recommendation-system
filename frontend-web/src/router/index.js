import Vue from 'vue'
import VueRouter from 'vue-router'

// --- 1. 引入公共页面 ---
import Login from '../views/Login.vue' // 使用你美化过的登录页

// --- 2. 引入队友的前台页面 (User) ---
import UserMain from '../views/user/UserMain.vue'
import VideoPlayer from '../views/user/VideoPlayer.vue'
import NavigationSystem from '../views/user/NavigationSystem.vue'
import UserProfile from '../views/user/UserProfile.vue'

// --- 3. 引入你的后台页面 (Admin) ---
import AdminLayout from '../views/admin/AdminLayout.vue'
import AdminStats from '../views/admin/AdminStats.vue'
import VideoManage from '../views/admin/VideoManage.vue'
import UserManage from '../views/admin/UserManage.vue'
import AdminLogs from '../views/admin/AdminLogs.vue'

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
      { path: 'video', name: 'UserVideo', component: VideoPlayer },
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
      { path: 'logs', name: 'AdminLogs', component: AdminLogs }
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
  const userToken = localStorage.getItem('userToken');   // 普通用户令牌

  // 1. 如果要去【管理后台】
  if (to.path.startsWith('/admin')) {
    if (adminToken) {
      next();
    } else {
      Vue.prototype.$message.warning('请先登录管理员账号');
      next('/login');
    }
  } 
  // 2. 如果要去【用户前台】
  else if (to.path.startsWith('/main')) {
    if (userToken || adminToken) { // 管理员也可以看前台，或者要求必须有 userToken
      next();
    } else {
      Vue.prototype.$message.warning('请先登录');
      next('/login');
    }
  } 
  // 3. 其他页面 (登录页等) 直接放行
  else {
    next();
  }
});

export default router