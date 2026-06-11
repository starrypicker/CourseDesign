import { createRouter, createWebHistory } from 'vue-router'
import FrontLayout from '@/layouts/FrontLayout.vue'
import AdminLayout from '@/layouts/AdminLayout.vue'

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
  {
    path: '/',
    component: FrontLayout,
    redirect: '/home',
    children: [
      { path: 'home', name: 'FrontHome', component: FrontHome, meta: { title: '首页', requiresAuth: false } },
      { path: 'products', name: 'Products', component: Products, meta: { title: '商品列表', requiresAuth: false } },
      { path: 'cart', name: 'Cart', component: Cart, meta: { title: '购物车', requiresAuth: true } },
      { path: 'orders', name: 'Orders', component: Orders, meta: { title: '我的订单', requiresAuth: true } },
      { path: 'profile', name: 'Profile', component: Profile, meta: { title: '个人中心', requiresAuth: true } }
    ]
  },
  {
    path: '/admin',
    component: AdminLayout,
    redirect: '/admin/dashboard',
    meta: { requiresAuth: true, role: 'admin' },
    children: [
      { path: 'dashboard', name: 'AdminDashboard', component: AdminDashboard, meta: { title: '仪表盘' } },
      { path: 'products', name: 'AdminProducts', component: AdminProducts, meta: { title: '商品管理' } },
      { path: 'orders', name: 'AdminOrders', component: AdminOrders, meta: { title: '订单管理' } },
      { path: 'users', name: 'AdminUsers', component: AdminUsers, meta: { title: '用户管理' } },
      { path: 'purchase', name: 'AdminPurchase', component: AdminPurchase, meta: { title: '进货管理' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/home' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() { return { top: 0 } }
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  let isAdmin = false
  try {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || 'null')
    isAdmin = userInfo?.role === 'admin'
  } catch (e) {}
  if (to.matched.some(record => record.meta.requiresAuth)) {
    if (!token) {
      next({ name: 'Login', query: { redirect: to.fullPath } })
    } else if (to.matched.some(record => record.meta.role === 'admin') && !isAdmin) {
      next({ name: 'FrontHome' })
    } else {
      next()
    }
  } else {
    next()
  }
})

export default router
