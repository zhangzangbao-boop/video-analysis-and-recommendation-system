import Vue from 'vue'
import VueRouter from 'vue-router'

// 引入组件
import UserLogin from '../views/Login.vue' // 游客伪装首页 & 登录页
import AdminLogin from '../views/admin/AdminLogin.vue' // 独立的管理员登录页

import UserMain from '../views/user/UserMain.vue'
import VideoHome from '../views/user/VideoHome.vue'
import VideoPlayer from '../views/user/VideoPlayer.vue'
import NavigationSystem from '../views/user/NavigationSystem.vue'
import UserProfile from '../views/user/UserProfile.vue'

import AdminLayout from '../views/admin/AdminLayout.vue'
import AdminStats from '../views/admin/AdminStats.vue'
import VideoManage from '../views/admin/VideoManage.vue'
import UserManage from '../views/admin/UserManage.vue'
import AdminLogs from '../views/admin/AdminLogs.vue'
import SystemNotice from '../views/admin/SystemNotice.vue'

Vue.use(VueRouter)

const originalPush = VueRouter.prototype.push
VueRouter.prototype.push = function push(location) {
  return originalPush.call(this, location).catch(err => err)
}

const routes = [
  // --- 1. 伪装成首页的登录页 (游客入口) ---
  {
    path: '/login',
    name: 'UserLogin',
    component: UserLogin
  },

  // --- 2. 管理员专用登录 ---
  {
    path: '/admin-login',
    name: 'AdminLogin',
    component: AdminLogin
  },

  // --- 3. 用户主页面 (登录后才能进) ---
  {
    path: '/main',
    component: UserMain,
    meta: { requiresAuth: true }, // 【关键】未登录禁止访问，强制回 /login
    children: [
      { path: '', redirect: 'video' },
      { path: 'video', name: 'VideoHome', component: VideoHome },
      { path: 'video/:id', name: 'VideoDetail', component: VideoPlayer },
      { path: 'navigation', name: 'UserNav', component: NavigationSystem },
      { path: 'profile', name: 'UserProfile', component: UserProfile }
    ]
  },

  // --- 4. 管理后台 ---
  {
    path: '/admin',
    component: AdminLayout,
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      { path: '', redirect: 'stats' },
      { path: 'stats', name: 'AdminStats', component: AdminStats },
      { path: 'videos', name: 'VideoManage', component: VideoManage },
      { path: 'users', name: 'UserManage', component: UserManage },
      { path: 'logs', name: 'AdminLogs', component: AdminLogs },
      { path: 'notice', name: 'SystemNotice', component: SystemNotice }
    ]
  },

  // --- 根路径重定向 ---
  {
    path: '/',
    redirect: '/login' // 默认去伪装首页
  },

  // 捕获未知路由
  {
    path: '*',
    redirect: '/login'
  }
]

const router = new VueRouter({
  mode: 'history',
  base: process.env.BASE_URL,
  routes
})

// --- 全局路由守卫 ---
router.beforeEach((to, from, next) => {
  // 【修复】统一使用 sessionStorage (修复不同步问题)
  const token = sessionStorage.getItem('userToken') || sessionStorage.getItem('adminToken');
  const userRole = sessionStorage.getItem('userRole');

  // 1. 需要登录的页面
  if (to.matched.some(record => record.meta.requiresAuth)) {
    if (!token) {
      // 没登录去后台 -> AdminLogin
      if (to.path.startsWith('/admin')) {
        next('/admin-login');
      }
      // 没登录去用户页 -> Login (伪装首页)
      else {
        next('/login');
      }
      return;
    }

    // 2. 权限校验
    if (to.matched.some(record => record.meta.requiresAdmin)) {
      if (userRole !== 'admin') {
        Vue.prototype.$message.error('无权访问管理后台');
        next('/main/video');
        return;
      }
    }
  }

  // 3. 已登录防止重复进入登录页
  if (token && (to.path === '/login' || to.path === '/admin-login')) {
    if (userRole === 'admin') next('/admin/stats');
    else next('/main/video');
    return;
  }

  next();
})

export default router