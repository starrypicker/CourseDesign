<template>
  <div class="dashboard-container">
    <el-row :gutter="20" class="stat-row">
      <el-col :xs="24" :sm="12" :md="6"><el-card shadow="hover" class="stat-card stat-sales clickable-stat" @click="$router.push('/admin/orders')"><div class="stat-icon"><el-icon :size="40"><Goods /></el-icon></div><div class="stat-info"><p class="stat-label">今日销售额</p><h3 class="stat-value">¥ {{ formatNumber(stats.todaySales) }}</h3></div></el-card></el-col>
      <el-col :xs="24" :sm="12" :md="6"><el-card shadow="hover" class="stat-card stat-orders clickable-stat" @click="$router.push('/admin/orders')"><div class="stat-icon"><el-icon :size="40"><List /></el-icon></div><div class="stat-info"><p class="stat-label">今日订单</p><h3 class="stat-value">{{ stats.todayOrders }}</h3></div></el-card></el-col>
      <el-col :xs="24" :sm="12" :md="6"><el-card shadow="hover" class="stat-card stat-month-sales clickable-stat" @click="$router.push('/admin/reports')"><div class="stat-icon"><el-icon :size="40"><TrendCharts /></el-icon></div><div class="stat-info"><p class="stat-label">本月销售额</p><h3 class="stat-value">¥ {{ formatNumber(stats.monthSales) }}</h3></div></el-card></el-col>
      <el-col :xs="24" :sm="12" :md="6"><el-card shadow="hover" class="stat-card stat-month-orders clickable-stat" @click="$router.push('/admin/reports')"><div class="stat-icon"><el-icon :size="40"><DataAnalysis /></el-icon></div><div class="stat-info"><p class="stat-label">本月订单</p><h3 class="stat-value">{{ stats.monthOrders }}</h3></div></el-card></el-col>
    </el-row>
    <el-row :gutter="20" class="stat-row">
      <el-col :xs="24" :sm="12" :md="6"><el-card shadow="hover" class="stat-card stat-products clickable-stat" @click="$router.push('/admin/products')"><div class="stat-icon"><el-icon :size="40"><ShoppingBag /></el-icon></div><div class="stat-info"><p class="stat-label">商品总数</p><h3 class="stat-value">{{ stats.totalProducts }}</h3></div></el-card></el-col>
      <el-col :xs="24" :sm="12" :md="6"><el-card shadow="hover" class="stat-card stat-users clickable-stat" @click="$router.push('/admin/users')"><div class="stat-icon"><el-icon :size="40"><User /></el-icon></div><div class="stat-info"><p class="stat-label">注册用户</p><h3 class="stat-value">{{ stats.totalCustomers }}</h3></div></el-card></el-col>
      <el-col :xs="24" :sm="12" :md="6"><el-card shadow="hover" class="stat-card stat-pending clickable-stat" @click="$router.push('/admin/orders')"><div class="stat-icon"><el-icon :size="40"><Clock /></el-icon></div><div class="stat-info"><p class="stat-label">待处理订单</p><h3 class="stat-value" style="color:#E6A23C;">{{ stats.pendingOrders }}</h3></div></el-card></el-col>
      <el-col :xs="24" :sm="12" :md="6"><el-card shadow="hover" class="stat-card stat-warning clickable-stat" @click="$router.push('/admin/low-stock')"><div class="stat-icon"><el-icon :size="40"><Warning /></el-icon></div><div class="stat-info"><p class="stat-label">库存预警</p><h3 class="stat-value" :style="{ color: stats.lowStockCount > 0 ? '#f56c6c' : '#67C23A' }">{{ stats.lowStockCount }}</h3></div></el-card></el-col>
    </el-row>
    <el-row :gutter="20">
      <el-col :xs="24" :md="14">
        <el-card shadow="never" class="recent-card">
          <template #header><div class="card-header"><span>最近订单</span><el-button text type="primary" @click="$router.push('/admin/orders')">查看全部</el-button></div></template>
          <el-table :data="recentOrders" stripe size="small">
            <el-table-column prop="orderId" label="订单号" width="80" />
            <el-table-column prop="customerName" label="客户" width="100"><template #default="{ row }"><span>{{ row.customerName || row.customerCode }}</span></template></el-table-column>
            <el-table-column prop="totalAmount" label="金额" width="100"><template #default="{ row }"><span style="color:#f56c6c;font-weight:500;">¥ {{ formatNumber(row.totalAmount) }}</span></template></el-table-column>
            <el-table-column label="状态" width="90"><template #default="{ row }"><el-tag :type="statusTagType(row.orderStatus)" size="small">{{ statusText(row.orderStatus) }}</el-tag></template></el-table-column>
            <el-table-column prop="orderDate" label="时间" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="10">
        <el-card shadow="never" class="recent-card" v-if="actualLowStock.length > 0">
          <template #header><div class="card-header"><span style="color:#f56c6c;">⚠ 库存预警</span><el-button text type="danger" @click="$router.push('/admin/products')">去补货</el-button></div></template>
          <div class="hot-products"><div v-for="item in actualLowStock" :key="item.productCode" class="hot-item"><span class="hot-rank" style="background:#f56c6c;color:#fff;">!</span><span class="hot-name">{{ item.productName }}</span><span class="hot-sales" style="color:#f56c6c;">库存 {{ item.stockQuantity }}/{{ item.minStock }}</span></div></div>
        </el-card>
        <el-card shadow="never" class="recent-card" style="margin-top:20px;">
          <template #header><div class="card-header"><span>商品列表</span><el-button text type="primary" @click="$router.push('/admin/products')">查看全部</el-button></div></template>
          <div class="hot-products"><div v-for="(item, index) in hotProducts" :key="item.productCode" class="hot-item clickable" @click="handleViewProduct(item)"><span class="hot-rank" :class="{ top: index < 3 }">{{ index + 1 }}</span><span class="hot-name link">{{ item.productName }}</span><span class="hot-sales">库存 {{ item.stockQuantity }}</span><span class="hot-amount">¥ {{ formatNumber(item.unitPrice) }}</span></div></div>
        </el-card>
        <el-card shadow="never" class="recent-card" style="margin-top:20px;" v-if="salesRanking.length > 0">
          <template #header><div class="card-header"><span>🏆 销量排行</span><span style="font-size:12px;color:#909399;">累计售出 {{ stats.totalSold }} 件</span></div></template>
          <div class="hot-products"><div v-for="(item, index) in salesRanking" :key="item.productCode" class="hot-item clickable" @click="handleViewProduct(item)"><span class="hot-rank" :class="{ top: index < 3 }">{{ index + 1 }}</span><span class="hot-name link">{{ item.productName }}</span><span class="hot-sales">售出 {{ item.totalSold }} 件</span><span class="hot-amount">¥ {{ formatNumber(item.totalRevenue) }}</span></div></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 商品详情弹窗 -->
    <ProductDetailDialog v-model:visible="productDetailVisible" :product="productDetail" :show-add-cart="false" />
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { Goods, List, ShoppingBag, User, TrendCharts, DataAnalysis, Clock, Warning } from '@element-plus/icons-vue'
import { getProductList, getLowStockProducts } from '@/api/product'
import { getOrderList, getDashboardStats } from '@/api/order'
import ProductDetailDialog from '@/components/ProductDetailDialog.vue'
import { getProductImage } from '@/utils/constants'

const stats = reactive({ todaySales: 0, todayOrders: 0, monthSales: 0, monthOrders: 0, pendingOrders: 0, lowStockCount: 0, totalProducts: 0, totalCustomers: 0, totalSold: 0 })
const recentOrders = ref([])
const hotProducts = ref([])
const lowStockProducts = ref([])
const salesRanking = ref([])

const productDetailVisible = ref(false)
const productDetail = ref(null)
const handleViewProduct = (item) => { productDetail.value = item; productDetailVisible.value = true }

// 前端二次过滤：确保库存预警列表只包含真正低于最低库存的商品
const actualLowStock = computed(() =>
  lowStockProducts.value.filter(item => item.stockQuantity < item.minStock)
)

const formatNumber = (val) => { if (val === null || val === undefined) return '0.00'; return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }

const fetchStats = async () => {
  try {
    const res = await getDashboardStats()
    if (res) { stats.todaySales = res.todaySales || 0; stats.todayOrders = res.todayOrders || 0; stats.monthSales = res.monthSales || 0; stats.monthOrders = res.monthOrders || 0; stats.pendingOrders = res.pendingOrders || 0; stats.lowStockCount = res.lowStockCount || 0; stats.totalProducts = res.totalProducts || 0; stats.totalCustomers = res.totalCustomers || 0; stats.totalSold = res.totalSold || 0; salesRanking.value = res.salesRanking || [] }
  } catch (e) { console.error('获取仪表盘数据失败:', e) }
}

const fetchRecentOrders = async () => {
  try { const res = await getOrderList({ pageNum: 1, pageSize: 5 }); recentOrders.value = res.rows || [] } catch (e) { console.error('获取最近订单失败:', e) }
}

const fetchProducts = async () => {
  try { const res = await getProductList({ pageNum: 1, pageSize: 6, status: 1 }); hotProducts.value = res.rows || [] } catch (e) { console.error('获取商品列表失败:', e) }
}

const fetchLowStock = async () => {
  try { const res = await getLowStockProducts(); lowStockProducts.value = res || [] } catch (e) { console.error('获取库存预警失败:', e) }
}

onMounted(() => { fetchStats(); fetchRecentOrders(); fetchProducts(); fetchLowStock() })

const statusText = (orderStatus) => { const map = { 0: '待确认', 1: '已确认', 2: '已完成', 3: '已取消' }; return map[orderStatus] || '未知' }
const statusTagType = (orderStatus) => { const map = { 0: 'warning', 1: 'primary', 2: 'success', 3: 'info' }; return map[orderStatus] || 'info' }
</script>

<style scoped>
.dashboard-container { padding: 0; }
.stat-row { margin-bottom: 20px; }
.stat-card { display: flex; align-items: center; padding: 20px; border-radius: 8px; margin-bottom: 10px; }
.stat-card .stat-icon { width: 70px; height: 70px; border-radius: 12px; display: flex; align-items: center; justify-content: center; margin-right: 16px; flex-shrink: 0; }
.stat-sales .stat-icon { background: rgba(64,158,255,0.1); color: #409EFF; }
.stat-orders .stat-icon { background: rgba(230,162,60,0.1); color: #E6A23C; }
.stat-products .stat-icon { background: rgba(103,194,58,0.1); color: #67C23A; }
.stat-users .stat-icon { background: rgba(144,147,153,0.1); color: #909399; }
.stat-month-sales .stat-icon { background: rgba(19,206,102,0.1); color: #13CE66; }
.stat-month-orders .stat-icon { background: rgba(64,158,255,0.1); color: #409EFF; }
.stat-pending .stat-icon { background: rgba(230,162,60,0.1); color: #E6A23C; }
.stat-warning .stat-icon { background: rgba(245,108,108,0.1); color: #f56c6c; }
.stat-info { flex: 1; }
.stat-label { font-size: 14px; color: #909399; margin-bottom: 4px; }
.stat-value { font-size: 24px; font-weight: bold; color: #303133; margin: 0; }
.recent-card { margin-bottom: 20px; }
.card-header { display: flex; align-items: center; justify-content: space-between; font-weight: 500; }
.hot-products { display: flex; flex-direction: column; gap: 12px; }
.hot-item { display: flex; align-items: center; gap: 12px; padding: 8px 0; border-bottom: 1px solid #f0f0f0; }
.hot-item:last-child { border-bottom: none; }
.hot-rank { width: 24px; height: 24px; border-radius: 4px; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: bold; background: #f0f0f0; color: #909399; flex-shrink: 0; }
.hot-rank.top { background: #409EFF; color: #fff; }
.hot-name { flex: 1; font-size: 14px; color: #303133; }
.hot-name.link { color: #409EFF; cursor: pointer; }
.hot-name.link:hover { text-decoration: underline; }
.hot-sales { font-size: 13px; color: #909399; }
.hot-amount { font-size: 14px; color: #f56c6c; font-weight: 500; }
.clickable-stat { cursor: pointer; transition: transform 0.2s, box-shadow 0.2s; }
.clickable-stat:hover { transform: translateY(-2px); box-shadow: 0 4px 16px rgba(0,0,0,0.1); }
.hot-item.clickable { cursor: pointer; }
.hot-item.clickable:hover { background: #f5f7fa; border-radius: 4px; padding-left: 4px; }
</style>
