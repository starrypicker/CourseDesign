import { createRouter, createWebHistory } from 'vue-router'

// 布局组件
import FrontLayout from '@/layouts/FrontLayout.vue'
import AdminLayout from '@/layouts/AdminLayout.vue'

// 页面组件（懒加载示例，可按需加载）
const Login = () => import('@/views/login/index.vue')
const FrontHome = () => import('@/views/front/Home.vue')
const Products = () => import('@/views/front/Products.vue')
const Cart = () => import('@/views/front/Cart.vue')
const Orders = () => import('@/views/front/Orders.vue')
const Profile = () => import('@/views/front/Profile.vue')
const AdminDashboard = () => import('@/views/admin/Dashboard.vue')
const AdminProducts = () => import('@/views/admin/Products.vue')
const AdminOrders = () => import('@/views/admin/Orders.vue')
const AdminUsers = () => import('@/views/admin/Users.vue')
const AdminPurchase = () => import('@/views/admin/Purchase.vue')

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { title: '登录 - 体育用品销售系统' }
  },
  // 顾客前台路由组
  {
    path: '/',
    component: FrontLayout,
    redirect: '/home',
    children: [
      {
        path: 'home',
        name: 'FrontHome',
        component: FrontHome,
        meta: { title: '首页', requiresAuth: false }
      },
      {
        path: 'products',
        name: 'Products',
        component: Products,
        meta: { title: '商品列表', requiresAuth: false }
      },
      {
        path: 'cart',
        name: 'Cart',
        component: Cart,
        meta: { title: '购物车', requiresAuth: true }      // 需要登录
      },
      {
        path: 'orders',
        name: 'Orders',
        component: Orders,
        meta: { title: '我的订单', requiresAuth: true }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: Profile,
        meta: { title: '个人中心', requiresAuth: true }
      }
    ]
  },
  // 管理员后台路由组
  {
    path: '/admin',
    component: AdminLayout,
    redirect: '/admin/dashboard',
    meta: { requiresAuth: true, role: 'admin' },           // 需要管理员角色
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: AdminDashboard,
        meta: { title: '仪表盘' }
      },
      {
        path: 'products',
        name: 'AdminProducts',
        component: AdminProducts,
        meta: { title: '商品管理' }
      },
      {
        path: 'orders',
        name: 'AdminOrders',
        component: AdminOrders,
        meta: { title: '订单管理' }
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: AdminUsers,
        meta: { title: '用户管理' }
      },
      {
        path: 'purchase',
        name: 'AdminPurchase',
        component: AdminPurchase,
        meta: { title: '进货管理' }
      }
    ]
  },
  // 404 重定向
  {
    path: '/:pathMatch(.*)*',
    redirect: '/home'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    // 页面切换时回到顶部
    return { top: 0 }
  }
})

// 全局前置守卫：权限验证
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  // 从 userInfo 中判断是否是管理员
  let isAdmin = false
  try {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || 'null')
    isAdmin = userInfo?.role === 'admin'
  } catch (e) {}

  // 如果目标路由需要登录
  if (to.matched.some(record => record.meta.requiresAuth)) {
    if (!token) {
      // 未登录，跳转到登录页，并携带 redirect 参数以便登录后回跳
      next({ name: 'Login', query: { redirect: to.fullPath } })
    } else if (to.matched.some(record => record.meta.role === 'admin') && !isAdmin) {
      // 已登录但不是管理员，尝试访问后台
      next({ name: 'FrontHome' })
    } else {
      next()
    }
  } else {
    // 不需要登录的页面直接放行
    next()
  }
})

export default router