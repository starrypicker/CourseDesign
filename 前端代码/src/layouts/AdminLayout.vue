<template>
  <div class="admin-layout">
    <!-- 移动端遮罩 -->
    <transition name="fade">
      <div v-if="mobileSidebarOpen" class="sidebar-overlay" @click="mobileSidebarOpen = false" />
    </transition>
    <aside class="admin-sidebar" :class="{ collapsed: sidebarCollapsed, 'mobile-open': mobileSidebarOpen }">
      <div class="sidebar-logo">
        <el-icon :size="22"><Trophy /></el-icon>
        <span v-show="!sidebarCollapsed">体育用品管理</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        :collapse="sidebarCollapsed"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        @select="mobileSidebarOpen = false"
      >
        <el-menu-item index="/admin/dashboard"><el-icon><DataAnalysis /></el-icon><span>仪表盘</span></el-menu-item>
        <el-menu-item index="/admin/products"><el-icon><Goods /></el-icon><span>商品管理</span></el-menu-item>
        <el-menu-item index="/admin/orders"><el-icon><List /></el-icon><span>订单管理</span></el-menu-item>
        <el-menu-item index="/admin/users"><el-icon><User /></el-icon><span>顾客管理</span></el-menu-item>
        <el-menu-item index="/admin/purchase"><el-icon><Box /></el-icon><span>进货管理</span></el-menu-item>
        <el-menu-item index="/admin/manufacturers"><el-icon><OfficeBuilding /></el-icon><span>厂家管理</span></el-menu-item>
        <el-menu-item index="/admin/returns"><el-icon><RefreshLeft /></el-icon><span>退换货管理</span></el-menu-item>
        <el-menu-item index="/admin/chat">
          <el-icon><ChatDotSquare /></el-icon>
          <span>客服消息</span>
          <el-badge v-if="chatUnread > 0" :value="chatUnread" class="chat-badge" />
        </el-menu-item>
        <el-menu-item index="/admin/invoices"><el-icon><Document /></el-icon><span>发票管理</span></el-menu-item>
        <el-menu-item index="/admin/reports"><el-icon><DataLine /></el-icon><span>订单报表</span></el-menu-item>
        <el-menu-item index="/admin/low-stock"><el-icon><Warning /></el-icon><span>缺货管理</span></el-menu-item>
      </el-menu>
    </aside>
    <div class="admin-main" :class="{ expanded: sidebarCollapsed }">
      <header class="admin-header">
        <div class="header-left">
          <el-button class="collapse-btn" text @click="toggleSidebar">
            <el-icon :size="20"><component :is="sidebarCollapsed ? 'Expand' : 'Fold'" /></el-icon>
          </el-button>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/admin/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <!-- 移动端菜单按钮 -->
          <el-button class="mobile-sidebar-btn" text @click="mobileSidebarOpen = true">
            <el-icon :size="20"><Menu /></el-icon>
          </el-button>
          <el-dropdown @command="handleCommand" trigger="click">
            <span class="user-info"><el-icon><UserFilled /></el-icon><span class="desktop-only">管理员</span><el-icon class="el-icon--right"><ArrowDown /></el-icon></span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="home">回到前台</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>
      <main class="admin-content">
        <el-backtop :right="30" :bottom="60" target=".admin-content" />
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { DataAnalysis, Goods, List, User, Box, Trophy, UserFilled, ArrowDown, OfficeBuilding, RefreshLeft, ChatDotSquare, Document, DataLine, Warning, Menu } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const store = useStore()
const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta.title || '')
const chatUnread = ref(0)
const sidebarCollapsed = ref(false)
const mobileSidebarOpen = ref(false)
let unreadTimer = null

const fetchChatUnread = async () => {
  try {
    const { getUnreadCount } = await import('@/api/chat')
    const count = await getUnreadCount()
    chatUnread.value = count || 0
  } catch (e) {}
}

onMounted(() => {
  fetchChatUnread()
  unreadTimer = setInterval(fetchChatUnread, 30000)
  // 响应式检测
  checkScreenSize()
  window.addEventListener('resize', checkScreenSize)
})

onBeforeUnmount(() => {
  if (unreadTimer) { clearInterval(unreadTimer); unreadTimer = null }
  window.removeEventListener('resize', checkScreenSize)
})

const checkScreenSize = () => {
  if (window.innerWidth < 992) {
    sidebarCollapsed.value = true
  }
}

const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

const handleCommand = (command) => {
  if (command === 'home') router.push('/home')
  else if (command === 'logout') { store.dispatch('logout'); router.push('/login') }
}
</script>

<style scoped>
.admin-layout { display: flex; min-height: 100vh; }
.admin-sidebar {
  width: 220px; background-color: #304156; flex-shrink: 0;
  transition: width 0.3s ease;
  position: fixed; left: 0; top: 0; bottom: 0; z-index: 50;
  overflow-y: auto; overflow-x: hidden;
}
.admin-sidebar.collapsed { width: 64px; }
.sidebar-logo {
  height: 60px; display: flex; align-items: center; justify-content: center; gap: 8px;
  color: #fff; font-size: 16px; font-weight: bold;
  background-color: #263445; flex-shrink: 0;
  overflow: hidden; white-space: nowrap;
}
.admin-main {
  flex: 1; display: flex; flex-direction: column; overflow: hidden;
  margin-left: 220px; transition: margin-left 0.3s ease;
  min-width: 0;
}
.admin-main.expanded { margin-left: 64px; }
.admin-header {
  height: 60px; background: #fff; display: flex; align-items: center;
  justify-content: space-between; padding: 0 20px;
  box-shadow: 0 1px 4px rgba(0,21,41,0.08); flex-shrink: 0;
  position: sticky; top: 0; z-index: 20;
}
.header-left { display: flex; align-items: center; gap: 12px; }
.collapse-btn { padding: 6px; }
.header-right { display: flex; align-items: center; }
.mobile-sidebar-btn { display: none; }
.user-info {
  display: flex; align-items: center; gap: 4px; cursor: pointer;
  color: #606266; font-size: 14px; padding: 4px 8px; border-radius: 6px;
  transition: color 0.2s, background-color 0.2s;
}
.user-info:hover { color: #409EFF; background-color: #ecf5ff; }
.admin-content {
  flex: 1; padding: 20px; background-color: #f0f2f5; overflow-y: auto;
}
.chat-badge { margin-left: 8px; }
.chat-badge :deep(.el-badge__content) { margin-top: 0; }

/* 侧边栏遮罩 */
.sidebar-overlay {
  display: none;
  position: fixed; inset: 0; background: rgba(0,0,0,0.4); z-index: 45;
}
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

@media (max-width: 992px) {
  .admin-sidebar {
    transform: translateX(-100%);
    width: 220px;
    z-index: 55;
  }
  .admin-sidebar.mobile-open { transform: translateX(0); }
  .admin-sidebar.collapsed { width: 220px; }
  .sidebar-overlay { display: block; }
  .admin-main { margin-left: 0 !important; }
  .admin-main.expanded { margin-left: 0 !important; }
  .mobile-sidebar-btn { display: inline-flex; }
  .collapse-btn { display: none; }
  .desktop-only { display: none; }
}
</style>
