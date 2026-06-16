<template>
  <div class="front-layout">
    <header class="front-header" :class="{ scrolled: isScrolled }">
      <div class="header-inner">
        <div class="header-left">
          <div class="logo" @click="$router.push('/home')">
            <el-icon :size="24" color="#409EFF"><Trophy /></el-icon>
            <span class="logo-text">体育用品商城</span>
          </div>
          <nav class="nav-links" :class="{ open: mobileMenuOpen }">
            <router-link to="/home" class="nav-item" @click="mobileMenuOpen = false">首页</router-link>
            <router-link to="/products" class="nav-item" @click="mobileMenuOpen = false">商品</router-link>
          </nav>
        </div>
        <div class="header-right">
          <router-link to="/cart" class="nav-item cart-link">
            <el-icon><ShoppingCart /></el-icon>
            购物车
            <el-badge v-if="cartCount > 0" :value="cartCount" class="cart-badge" />
          </router-link>
          <template v-if="isLoggedIn">
            <router-link to="/orders" class="nav-item desktop-only">订单</router-link>
            <router-link to="/returns" class="nav-item desktop-only">退换货</router-link>
            <router-link to="/invoices" class="nav-item desktop-only">发票</router-link>
            <router-link to="/chat" class="nav-item desktop-only">
              <el-icon><ChatDotSquare /></el-icon>客服
            </router-link>
            <router-link to="/profile" class="nav-item desktop-only">个人</router-link>
            <el-dropdown @command="handleCommand" trigger="click">
              <span class="user-dropdown">
                <el-icon><UserFilled /></el-icon>
                <span class="desktop-only">{{ userInfo?.customerName || '用户' }}</span>
                <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="isAdmin" command="admin">管理后台</el-dropdown-item>
                  <el-dropdown-item command="orders">我的订单</el-dropdown-item>
                  <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                  <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button type="primary" size="small" @click="$router.push('/login')">登录</el-button>
          </template>
          <!-- 移动端菜单按钮 -->
          <el-button class="mobile-menu-btn" text @click="mobileMenuOpen = !mobileMenuOpen">
            <el-icon :size="22"><component :is="mobileMenuOpen ? 'Close' : 'Menu'" /></el-icon>
          </el-button>
        </div>
      </div>
    </header>
    <!-- 移动端下拉导航 -->
    <transition name="slide-down">
      <div v-if="mobileMenuOpen && isLoggedIn" class="mobile-nav">
        <router-link to="/orders" class="mobile-nav-item" @click="mobileMenuOpen = false">
          <el-icon><List /></el-icon>我的订单
        </router-link>
        <router-link to="/returns" class="mobile-nav-item" @click="mobileMenuOpen = false">
          <el-icon><RefreshLeft /></el-icon>退换货
        </router-link>
        <router-link to="/invoices" class="mobile-nav-item" @click="mobileMenuOpen = false">
          <el-icon><Document /></el-icon>我的发票
        </router-link>
        <router-link to="/chat" class="mobile-nav-item" @click="mobileMenuOpen = false">
          <el-icon><ChatDotSquare /></el-icon>客服
        </router-link>
        <router-link to="/profile" class="mobile-nav-item" @click="mobileMenuOpen = false">
          <el-icon><UserFilled /></el-icon>个人中心
        </router-link>
      </div>
    </transition>
    <main class="front-main">
      <el-backtop :right="30" :bottom="60" />
      <router-view />
    </main>
    <footer class="front-footer">
      &copy; {{ currentYear }} 体育用品销售信息系统 — 专业装备，助力每一次突破
    </footer>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { Trophy, ShoppingCart, UserFilled, ArrowDown, ChatDotSquare, List, RefreshLeft, Document } from '@element-plus/icons-vue'

const router = useRouter()
const store = useStore()

const isLoggedIn = computed(() => store.getters.isLoggedIn)
const isAdmin = computed(() => store.getters.userInfo?.role === 'admin')
const userInfo = computed(() => store.getters.userInfo)
const cartCount = computed(() => store.getters.cartCount)
const currentYear = computed(() => new Date().getFullYear())

const isScrolled = ref(false)
const mobileMenuOpen = ref(false)

const handleScroll = () => {
  isScrolled.value = window.scrollY > 10
}

onMounted(() => window.addEventListener('scroll', handleScroll, { passive: true }))
onBeforeUnmount(() => window.removeEventListener('scroll', handleScroll))

const handleCommand = (command) => {
  if (command === 'admin') {
    router.push('/admin/dashboard')
  } else if (command === 'orders') {
    router.push('/orders')
  } else if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'logout') {
    store.dispatch('logout')
    router.push('/home')
  }
}
</script>

<style scoped>
.front-layout { display: flex; flex-direction: column; min-height: 100vh; }
.front-header {
  background: #fff;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  position: sticky;
  top: 0;
  z-index: 100;
  transition: box-shadow 0.3s ease;
}
.front-header.scrolled {
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
}
.header-inner {
  max-width: 1400px;
  margin: 0 auto;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}
.header-left { display: flex; align-items: center; gap: 30px; }
.logo { display: flex; align-items: center; gap: 8px; cursor: pointer; user-select: none; }
.logo-text { font-size: 18px; font-weight: bold; color: #303133; }
.logo:hover .logo-text { color: #409EFF; }
.logo:hover .logo-text { transition: color 0.3s; }
.nav-links { display: flex; gap: 4px; }
.nav-item {
  font-size: 14px; color: #606266; text-decoration: none;
  padding: 6px 10px; border-radius: 6px;
  transition: color 0.2s, background-color 0.2s;
  display: flex; align-items: center; gap: 4px;
  white-space: nowrap;
}
.nav-item:hover { color: #409EFF; background-color: #ecf5ff; }
.nav-item.router-link-active { color: #409EFF; font-weight: 500; background-color: #ecf5ff; }
.header-right { display: flex; align-items: center; gap: 8px; }
.cart-link { position: relative; }
.cart-badge { position: absolute; top: -6px; right: -10px; }
.user-dropdown {
  display: flex; align-items: center; gap: 4px; cursor: pointer;
  font-size: 14px; color: #606266; padding: 4px 8px; border-radius: 6px;
  transition: color 0.2s, background-color 0.2s;
}
.user-dropdown:hover { color: #409EFF; background-color: #ecf5ff; }
.front-main { flex: 1; padding: 20px; max-width: 1400px; width: 100%; margin: 0 auto; }
.front-footer {
  text-align: center; padding: 20px; background: #fff;
  color: var(--text-secondary); font-size: 13px;
  border-top: 1px solid #ebeef5;
}

/* 移动端菜单按钮 */
.mobile-menu-btn { display: none; }

/* 移动端下拉导航 */
.mobile-nav {
  display: none;
  flex-direction: column;
  background: #fff;
  padding: 8px 20px;
  border-bottom: 1px solid #ebeef5;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.mobile-nav-item {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 12px; font-size: 14px; color: #606266;
  text-decoration: none; border-radius: 6px;
  transition: background-color 0.2s;
}
.mobile-nav-item:hover { background: #f5f7fa; color: #409EFF; }
.slide-down-enter-active, .slide-down-leave-active { transition: all 0.25s ease; }
.slide-down-enter-from, .slide-down-leave-to { opacity: 0; transform: translateY(-8px); }

@media (max-width: 768px) {
  .desktop-only { display: none !important; }
  .mobile-menu-btn { display: inline-flex; }
  .mobile-nav { display: flex; }
  .header-inner { padding: 0 12px; }
  .front-main { padding: 12px; }
}
</style>
