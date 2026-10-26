import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils/auth'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { public: true },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { public: true },
  },
  {
    path: '/simple',
    name: 'SimpleHome',
    component: () => import('@/views/SimpleHome.vue'),
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/Dashboard.vue'),
  },
  {
    path: '/food',
    name: 'Food',
    component: () => import('@/views/Food.vue'),
  },
  {
    path: '/exercise',
    name: 'Exercise',
    component: () => import('@/views/Exercise.vue'),
  },
  {
    path: '/plan',
    name: 'Plan',
    component: () => import('@/views/Plan.vue'),
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/Profile.vue'),
  },
  {
    path: '/ai',
    name: 'AI',
    component: () => import('@/views/Consult.vue'),
  },
  {
    path: '/consult',
    name: 'Consult',
    component: () => import('@/views/Consult.vue'),
  },
  {
    path: '/statistics',
    name: 'Statistics',
    component: () => import('@/views/Statistics.vue'),
  },
  {
    path: '/weight',
    name: 'Weight',
    component: () => import('@/views/Weight.vue'),
  },
  {
    path: '/messages',
    name: 'Messages',
    component: () => import('@/views/Messages.vue'),
  },
  {
    path: '/admin',
    name: 'AdminCenter',
    component: () => import('@/views/AdminCenter.vue'),
    meta: { requiresAdmin: true },
  },
  {
    path: '/',
    redirect: '/simple',
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = getToken()
  
  // 公开页面直接放行
  if (to.meta.public) {
    next()
    return
  }
  
  // 需要登录的页面
  if (!token) {
    next('/login')
    return
  }
  
  // 需要管理员权限的页面
  if (to.meta.requiresAdmin) {
    const userStore = useUserStore()
    // 缓存缺失时先放行，由页面自身兜底；正常流程登录后 userInfo 已在缓存
    if (userStore.userInfo && userStore.role !== 1) {
      next('/simple')
      return
    }
  }
  
  next()
})

export default router
