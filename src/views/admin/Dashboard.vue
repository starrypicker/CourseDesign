
<template>
  <div class="dashboard-container">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-row">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card stat-sales">
          <div class="stat-icon">
            <el-icon :size="40"><Goods /></el-icon>
          </div>
          <div class="stat-info">
            <p class="stat-label">今日销售额</p>
            <h3 class="stat-value">¥ {{ stats.todaySales.toLocaleString() }}</h3>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card stat-orders">
          <div class="stat-icon">
            <el-icon :size="40"><List /></el-icon>
          </div>
          <div class="stat-info">
            <p class="stat-label">今日订单</p>
            <h3 class="stat-value">{{ stats.todayOrders }}</h3>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card stat-products">
          <div class="stat-icon">
            <el-icon :size="40"><ShoppingBag /></el-icon>
          </div>
          <div class="stat-info">
            <p class="stat-label">商品总数</p>
            <h3 class="stat-value">{{ stats.totalProducts }}</h3>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card stat-users">
          <div class="stat-icon">
            <el-icon :size="40"><User /></el-icon>
          </div>
          <div class="stat-info">
            <p class="stat-label">注册用户</p>
            <h3 class="stat-value">{{ stats.totalUsers }}</h3>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <!-- 最近订单 -->
      <el-col :xs="24" :md="14">
        <el-card shadow="never" class="recent-card">
          <template #header>
            <div class="card-header">
              <span>最近订单</span>
              <el-button text type="primary" @click="$router.push('/admin/orders')">查看全部</el-button>
            </div>
          </template>
          <el-table :data="recentOrders" stripe size="small">
            <el-table-column prop="orderNo" label="订单号" width="150" />
            <el-table-column prop="customer" label="客户" width="100" />
            <el-table-column prop="amount" label="金额" width="100">
              <template #default="{ row }">
                <span style="color: #f56c6c; font-weight: 500;">¥ {{ row.amount.toFixed(2) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="时间" />
          </el-table>
        </el-card>
      </el-col>

      <!-- 热销商品 -->
      <el-col :xs="24" :md="10">
        <el-card shadow="never" class="recent-card">
          <template #header>
            <div class="card-header">
              <span>热销商品</span>
              <el-button text type="primary" @click="$router.push('/admin/products')">查看全部</el-button>
            </div>
          </template>
          <div class="hot-products">
            <div v-for="(item, index) in hotProducts" :key="item.id" class="hot-item">
              <span class="hot-rank" :class="{ top: index < 3 }">{{ index + 1 }}</span>
              <span class="hot-name">{{ item.name }}</span>
              <span class="hot-sales">销量 {{ item.sales }}</span>
              <span class="hot-amount">¥ {{ item.price.toFixed(2) }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { Goods, List, ShoppingBag, User } from '@element-plus/icons-vue'

const stats = reactive({
  todaySales: 12680,
  todayOrders: 38,
  totalProducts: 156,
  totalUsers: 1024
})

const recentOrders = reactive([
  { orderNo: 'SP20260604010', customer: '张三', amount: 599, status: 'pending', createTime: '2026-06-04 15:30' },
  { orderNo: 'SP20260604009', customer: '李四', amount: 1288, status: 'paid', createTime: '2026-06-04 14:20' },
  { orderNo: 'SP20260604008', customer: '王五', amount: 369, status: 'shipped', createTime: '2026-06-04 13:15' },
  { orderNo: 'SP20260604007', customer: '赵六', amount: 259, status: 'completed', createTime: '2026-06-04 11:40' },
  { orderNo: 'SP20260604006', customer: '孙七', amount: 880, status: 'completed', createTime: '2026-06-04 10:05' }
])

const hotProducts = reactive([
  { id: 1, name: '专业跑步鞋', sales: 328, price: 599 },
  { id: 2, name: '透气运动T恤', sales: 296, price: 129 },
  { id: 3, name: '碳纤维羽毛球拍', sales: 185, price: 880 },
  { id: 4, name: '哑铃套装 20kg', sales: 162, price: 259 },
  { id: 5, name: '冲锋衣', sales: 148, price: 459 },
  { id: 6, name: '篮球鞋', sales: 135, price: 499 }
])

const statusText = (status) => {
  const map = { pending: '待付款', paid: '已付款', shipped: '已发货', completed: '已完成' }
  return map[status] || status
}

const statusTagType = (status) => {
  const map = { pending: 'warning', paid: 'primary', shipped: '', completed: 'success' }
  return map[status] || 'info'
}
</script>

<style scoped>
.dashboard-container {
  padding: 0;
}

.stat-row {
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 10px;
}

.stat-card .stat-icon {
  width: 70px;
  height: 70px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  flex-shrink: 0;
}

.stat-sales .stat-icon { background: rgba(64, 158, 255, 0.1); color: #409EFF; }
.stat-orders .stat-icon { background: rgba(230, 162, 60, 0.1); color: #E6A23C; }
.stat-products .stat-icon { background: rgba(103, 194, 58, 0.1); color: #67C23A; }
.stat-users .stat-icon { background: rgba(144, 147, 153, 0.1); color: #909399; }

.stat-info {
  flex: 1;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  margin: 0;
}

.recent-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 500;
}

.hot-products {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.hot-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.hot-item:last-child {
  border-bottom: none;
}

.hot-rank {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
  background: #f0f0f0;
  color: #909399;
  flex-shrink: 0;
}

.hot-rank.top {
  background: #409EFF;
  color: #fff;
}

.hot-name {
  flex: 1;
  font-size: 14px;
  color: #303133;
}

.hot-sales {
  font-size: 13px;
  color: #909399;
}

.hot-amount {
  font-size: 14px;
  color: #f56c6c;
  font-weight: 500;
}
</style>
