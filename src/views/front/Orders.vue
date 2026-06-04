
<template>
  <div class="orders-container">
    <el-card shadow="never">
      <template #header>
        <div class="orders-header">
          <h2><el-icon><List /></el-icon> 我的订单</h2>
        </div>
      </template>

      <!-- 订单筛选 -->
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="全部订单" name="all" />
        <el-tab-pane label="待付款" name="pending" />
        <el-tab-pane label="已付款" name="paid" />
        <el-tab-pane label="已发货" name="shipped" />
        <el-tab-pane label="已完成" name="completed" />
      </el-tabs>

      <!-- 订单列表 -->
      <div v-if="filteredOrders.length > 0" class="order-list">
        <el-card v-for="order in filteredOrders" :key="order.id" shadow="hover" class="order-card">
          <div class="order-top">
            <div class="order-meta">
              <span class="order-no">订单号：{{ order.orderNo }}</span>
              <span class="order-time">{{ order.createTime }}</span>
            </div>
            <el-tag :type="statusTagType(order.status)" size="small">{{ statusText(order.status) }}</el-tag>
          </div>
          <el-divider />
          <div class="order-items">
            <div v-for="item in order.items" :key="item.id" class="order-item">
              <div class="item-icon">
                <el-icon :size="28" color="#909399"><ShoppingBag /></el-icon>
              </div>
              <div class="item-info">
                <span class="item-name">{{ item.name }}</span>
                <span class="item-qty">x{{ item.quantity }}</span>
              </div>
              <span class="item-price">¥ {{ (item.price * item.quantity).toFixed(2) }}</span>
            </div>
          </div>
          <el-divider />
          <div class="order-bottom">
            <span class="order-total">
              共 {{ order.items.reduce((s, i) => s + i.quantity, 0) }} 件商品，合计：
              <strong>¥ {{ order.totalAmount.toFixed(2) }}</strong>
            </span>
            <div class="order-actions">
              <el-button v-if="order.status === 'pending'" type="primary" size="small" @click="handlePay(order)">
                去付款
              </el-button>
              <el-button v-if="order.status === 'shipped'" type="success" size="small" @click="handleConfirm(order)">
                确认收货
              </el-button>
              <el-button size="small" @click="handleViewDetail(order)">查看详情</el-button>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 空状态 -->
      <el-empty v-else description="暂无订单" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { List, ShoppingBag } from '@element-plus/icons-vue'

const activeTab = ref('all')

// 模拟订单数据
const orders = ref([
  {
    id: 1,
    orderNo: 'SP20260601001',
    createTime: '2026-06-01 14:30:00',
    status: 'completed',
    totalAmount: 728,
    items: [
      { id: 1, name: '专业跑步鞋', price: 599, quantity: 1 },
      { id: 2, name: '透气运动T恤', price: 129, quantity: 1 }
    ]
  },
  {
    id: 2,
    orderNo: 'SP20260602001',
    createTime: '2026-06-02 09:15:00',
    status: 'shipped',
    totalAmount: 880,
    items: [
      { id: 3, name: '碳纤维羽毛球拍', price: 880, quantity: 1 }
    ]
  },
  {
    id: 3,
    orderNo: 'SP20260603001',
    createTime: '2026-06-03 16:45:00',
    status: 'paid',
    totalAmount: 259,
    items: [
      { id: 9, name: '哑铃套装 20kg', price: 259, quantity: 1 }
    ]
  },
  {
    id: 4,
    orderNo: 'SP20260604001',
    createTime: '2026-06-04 10:20:00',
    status: 'pending',
    totalAmount: 459,
    items: [
      { id: 11, name: '冲锋衣', price: 459, quantity: 1 }
    ]
  }
])

const filteredOrders = computed(() => {
  if (activeTab.value === 'all') return orders.value
  return orders.value.filter(o => o.status === activeTab.value)
})

const statusText = (status) => {
  const map = { pending: '待付款', paid: '已付款', shipped: '已发货', completed: '已完成' }
  return map[status] || status
}

const statusTagType = (status) => {
  const map = { pending: 'warning', paid: 'primary', shipped: '', completed: 'success' }
  return map[status] || 'info'
}

const handleTabChange = () => {}

const handlePay = (order) => {
  ElMessage.info(`订单 ${order.orderNo} 付款功能需要后端API支持`)
}

const handleConfirm = (order) => {
  ElMessage.success(`订单 ${order.orderNo} 已确认收货`)
  order.status = 'completed'
}

const handleViewDetail = (order) => {
  ElMessage.info(`订单详情功能开发中`)
}
</script>

<style scoped>
.orders-container {
  padding: 10px 0;
}

.orders-header h2 {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  margin: 0;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.order-card {
  border-radius: 8px;
}

.order-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.order-meta {
  display: flex;
  gap: 20px;
  font-size: 14px;
  color: #606266;
}

.order-no {
  font-weight: 500;
}

.order-time {
  color: #909399;
}

.order-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.order-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.item-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  border-radius: 4px;
  flex-shrink: 0;
}

.item-info {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
}

.item-name {
  font-size: 14px;
  color: #303133;
}

.item-qty {
  font-size: 13px;
  color: #909399;
}

.item-price {
  font-size: 14px;
  color: #f56c6c;
  font-weight: 500;
}

.order-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.order-total {
  font-size: 14px;
  color: #606266;
}

.order-total strong {
  color: #f56c6c;
  font-size: 18px;
}

.order-actions {
  display: flex;
  gap: 8px;
}
</style>
