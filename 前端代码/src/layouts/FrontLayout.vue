<template>
  <div class="front-layout">
    <header class="front-header">
      <div class="header-left">
        <div class="logo" @click="$router.push('/home')">
          <el-icon :size="22" color="#409EFF"><Trophy /></el-icon>
          <span class="logo-text">体育用品商城</span>
        </div>
        <nav class="nav-links">
          <router-link to="/home" class="nav-item">首页</router-link>
          <router-link to="/products" class="nav-item">商品</router-link>
        </nav>
      </div>
      <div class="header-right">
        <router-link to="/cart" class="nav-item cart-link">
          <el-icon><ShoppingCart /></el-icon>
          购物车
          <el-badge v-if="cartCount > 0" :value="cartCount" class="cart-badge" />
        </router-link>
        <template v-if="isLoggedIn">
          <router-link to="/orders" class="nav-item">订单</router-link>
          <router-link to="/profile" class="nav-item">个人</router-link>
          <el-dropdown @command="handleCommand">
            <span class="user-dropdown">
              <el-icon><UserFilled /></el-icon>
              {{ userInfo?.customerName || '用户' }}
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-if="isAdmin" command="admin">管理后台</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <el-button type="primary" size="small" @click="$router.push('/login')">登录</el-button>
        </template>
      </div>
    </header>
    <main class="front-main">
      <router-view />
    </main>
    <footer class="front-footer">体育用品销售信息系统 ©2026</footer>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { Trophy, ShoppingCart, UserFilled, ArrowDown } from '@element-plus/icons-vue'

const router = useRouter()
const store = useStore()

const isLoggedIn = computed(() => store.getters.isLoggedIn)
const isAdmin = computed(() => store.getters.userInfo?.role === 'admin')
const userInfo = computed(() => store.getters.userInfo)
const cartCount = computed(() => store.getters.cartCount)

const handleCommand = (command) => {
  if (command === 'admin') {
    router.push('/admin/dashboard')
  } else if (command === 'logout') {
    store.dispatch('logout')
    router.push('/home')
  }
}
</script>

<style scoped>
.front-layout { display: flex; flex-direction: column; min-height: 100vh; }
.front-header { background: #fff; padding: 0 40px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); display: flex; align-items: center; justify-content: space-between; height: 60px; position: sticky; top: 0; z-index: 100; }
.header-left { display: flex; align-items: center; gap: 30px; }
.logo { display: flex; align-items: center; gap: 6px; cursor: pointer; }
.logo-text { font-size: 18px; font-weight: bold; color: #303133; }
.nav-links { display: flex; gap: 20px; }
.nav-item { font-size: 14px; color: #606266; text-decoration: none; padding: 4px 8px; border-radius: 4px; transition: color 0.3s; display: flex; align-items: center; gap: 4px; }
.nav-item:hover { color: #409EFF; }
.nav-item.router-link-active { color: #409EFF; font-weight: 500; }
.header-right { display: flex; align-items: center; gap: 16px; }
.cart-link { position: relative; }
.cart-badge { position: absolute; top: -8px; right: -12px; }
.user-dropdown { display: flex; align-items: center; gap: 4px; cursor: pointer; font-size: 14px; color: #606266; }
.user-dropdown:hover { color: #409EFF; }
.front-main { flex: 1; padding: 20px; }
.front-footer { text-align: center; padding: 20px; color: var(--text-secondary); font-size: 14px; background: #fff; }
</style>
