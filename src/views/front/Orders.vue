
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
        <el-card v-for="order in filteredOrders" :key="order.orderId" shadow="hover" class="order-card">
          <div class="order-top">
            <div class="order-meta">
              <span class="order-no">订单号：{{ order.orderId }}</span>
              <span class="order-time">{{ order.orderDate }}</span>
            </div>
            <el-tag :type="statusTagType(order.orderStatus)" size="small">{{ statusText(order) }}</el-tag>
          </div>
          <el-divider />
          <div class="order-items">
            <div v-for="item in (order.items || [])" :key="item.itemId" class="order-item">
              <div class="item-icon">
                <el-icon :size="28" color="#909399"><ShoppingBag /></el-icon>
              </div>
              <div class="item-info">
                <span class="item-name">{{ item.productName || item.productCode }}</span>
                <span class="item-qty">x{{ item.quantity }}</span>
              </div>
              <span class="item-price">¥ {{ Number(item.totalAmount).toFixed(2) }}</span>
            </div>
          </div>
          <el-divider />
          <div class="order-bottom">
            <span class="order-total">
              共 {{ (order.items || []).reduce((s, i) => s + i.quantity, 0) }} 件商品，合计：
              <strong>¥ {{ Number(order.totalAmount).toFixed(2) }}</strong>
            </span>
            <div class="order-actions">
              <el-button v-if="order.paymentStatus === 0" type="primary" size="small" @click="handlePay(order)">
                去付款
              </el-button>
              <el-button v-if="order.shippingStatus === 1" type="success" size="small" @click="handleConfirm(order)">
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

    <!-- 订单详情弹窗 -->
    <el-dialog v-model="detailVisible" title="订单详情" width="600px">
      <template v-if="detailOrder">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ detailOrder.orderId }}</el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ detailOrder.orderDate }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">
            <el-tag :type="statusTagType(detailOrder.orderStatus)" size="small">{{ statusText(detailOrder) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="付款状态">{{ detailOrder.paymentStatus === 1 ? '已付款' : '未付款' }}</el-descriptions-item>
          <el-descriptions-item label="发货状态">{{ detailOrder.shippingStatus === 0 ? '未发货' : detailOrder.shippingStatus === 1 ? '已发货' : '已签收' }}</el-descriptions-item>
          <el-descriptions-item label="支付方式">{{ detailOrder.paymentMethod || '-' }}</el-descriptions-item>
          <el-descriptions-item label="收件人">{{ detailOrder.recipientName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="收件电话">{{ detailOrder.recipientPhone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="收件地址" :span="2">{{ detailOrder.recipientAddress || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detailOrder.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
        <h4 style="margin: 16px 0 8px">商品明细</h4>
        <el-table :data="detailOrder.items || []" border size="small">
          <el-table-column prop="productCode" label="商品编码" width="100" />
          <el-table-column prop="productName" label="商品名称" />
          <el-table-column prop="quantity" label="数量" width="80" />
          <el-table-column prop="unitPrice" label="单价" width="100">
            <template #default="{ row }">¥ {{ Number(row.unitPrice).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="totalAmount" label="小计" width="100">
            <template #default="{ row }">¥ {{ Number(row.totalAmount).toFixed(2) }}</template>
          </el-table-column>
        </el-table>
        <div style="text-align: right; margin-top: 12px; font-size: 16px">
          合计：<strong style="color: #f56c6c">¥ {{ Number(detailOrder.totalAmount).toFixed(2) }}</strong>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useStore } from 'vuex'
import { ElMessage, ElMessageBox } from 'element-plus'
import { List, ShoppingBag } from '@element-plus/icons-vue'
import { getOrderList, payOrder, completeOrder, getOrderById } from '@/api/order'

const store = useStore()
const activeTab = ref('all')
const orders = ref([])
const total = ref(0)
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)

// 加载订单数据
const fetchOrders = async () => {
  loading.value = true
  try {
    const userInfo = store.getters.userInfo
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value
    }
    // 如果是顾客登录，只查自己的订单
    if (userInfo && userInfo.customerCode) {
      params.customerCode = userInfo.customerCode
    }
    // 根据tab筛选
    if (activeTab.value === 'pending') {
      params.paymentStatus = 0
    } else if (activeTab.value === 'paid') {
      params.paymentStatus = 1
      params.shippingStatus = 0
    } else if (activeTab.value === 'shipped') {
      params.shippingStatus = 1
    } else if (activeTab.value === 'completed') {
      params.orderStatus = 2
    }
    const res = await getOrderList(params)
    orders.value = res.rows || []
    total.value = res.total || 0
  } catch (e) {
    console.error('获取订单失败:', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => fetchOrders())

const filteredOrders = computed(() => orders.value)

// 综合状态文字：根据 paymentStatus + shippingStatus + orderStatus 判断
const statusText = (order) => {
  if (order.orderStatus === 3) return '已取消'
  if (order.orderStatus === 2) return '已完成'
  if (order.shippingStatus === 2) return '已签收'
  if (order.shippingStatus === 1) return '已发货'
  if (order.paymentStatus === 1) return '已付款'
  if (order.orderStatus === 0) return '待确认'
  return '待付款'
}

const statusTagType = (orderStatus) => {
  if (orderStatus === 3) return 'info'
  if (orderStatus === 2) return 'success'
  if (orderStatus === 0) return 'warning'
  return 'primary'
}

const handleTabChange = () => {
  pageNum.value = 1
  fetchOrders()
}

const handlePay = async (order) => {
  try {
    await ElMessageBox.confirm('确认支付该订单？', '支付确认', {
      confirmButtonText: '确认支付',
      cancelButtonText: '取消',
      type: 'info'
    })
    await payOrder(order.orderId, 'online')
    ElMessage.success('支付成功！')
    fetchOrders()
  } catch (e) {
    if (e !== 'cancel') console.error('支付失败:', e)
  }
}

const handleConfirm = async (order) => {
  try {
    await ElMessageBox.confirm('确认已收到货物？', '收货确认', {
      confirmButtonText: '确认收货',
      cancelButtonText: '取消',
      type: 'info'
    })
    await completeOrder(order.orderId)
    ElMessage.success('已确认收货！')
    fetchOrders()
  } catch (e) {
    if (e !== 'cancel') console.error('确认收货失败:', e)
  }
}

const handleViewDetail = async (order) => {
  try {
    const detail = await getOrderById(order.orderId)
    detailOrder.value = detail
    detailVisible.value = true
  } catch (e) {
    console.error('获取订单详情失败:', e)
  }
}

const detailVisible = ref(false)
const detailOrder = ref(null)
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
