
<template>
  <div class="admin-orders">
    <!-- 操作栏 -->
    <el-card shadow="never" class="toolbar-card">
      <el-row :gutter="16" align="middle">
        <el-col :xs="24" :sm="8" :md="6">
          <el-input v-model="searchKeyword" placeholder="搜索订单号..." clearable @keyup.enter="handleSearch" />
        </el-col>
        <el-col :xs="24" :sm="6" :md="4">
          <el-select v-model="selectedStatus" placeholder="订单状态" clearable @change="handleSearch">
            <el-option label="全部状态" value="" />
            <el-option label="待确认" value="0" />
            <el-option label="已确认" value="1" />
            <el-option label="已完成" value="2" />
            <el-option label="已取消" value="3" />
          </el-select>
        </el-col>
        <el-col :xs="24" :sm="4" :md="3">
          <el-select v-model="selectedPayment" placeholder="付款状态" clearable @change="handleSearch">
            <el-option label="全部" value="" />
            <el-option label="未付款" value="0" />
            <el-option label="已付款" value="1" />
          </el-select>
        </el-col>
        <el-col :xs="24" :sm="6" :md="5">
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon> 搜索
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 订单表格 -->
    <el-card shadow="never">
      <el-table :data="orders" stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="orderId" label="订单号" width="80" />
        <el-table-column prop="customerName" label="客户" width="120">
          <template #default="{ row }">
            <span>{{ row.customerName || row.customerCode }}</span>
          </template>
        </el-table-column>
        <el-table-column label="商品" min-width="200">
          <template #default="{ row }">
            <span>{{ (row.items || []).map(i => i.productName || i.productCode).join('、') }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="金额" width="100">
          <template #default="{ row }">
            <span style="color: #f56c6c; font-weight: 500;">¥ {{ Number(row.totalAmount || 0).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="付款" width="80">
          <template #default="{ row }">
            <el-tag :type="row.paymentStatus === 1 ? 'success' : 'warning'" size="small">
              {{ row.paymentStatus === 1 ? '已付' : '未付' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发货" width="80">
          <template #default="{ row }">
            <el-tag :type="row.shippingStatus === 1 ? 'primary' : row.shippingStatus === 2 ? 'success' : 'info'" size="small">
              {{ row.shippingStatus === 1 ? '已发' : row.shippingStatus === 2 ? '签收' : '未发' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.orderStatus)" size="small">{{ statusText(row.orderStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="orderDate" label="下单时间" width="160" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="handleViewDetail(row)">详情</el-button>
            <el-button v-if="row.orderStatus === 0" text type="success" size="small" @click="handleConfirm(row)">确认</el-button>
            <el-button v-if="row.orderStatus === 1 && row.paymentStatus === 1 && row.shippingStatus === 0" text type="success" size="small" @click="handleShip(row)">发货</el-button>
            <el-button v-if="row.orderStatus === 0 || row.orderStatus === 1" text type="warning" size="small" @click="handleCancel(row)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div style="display:flex; justify-content:center; margin-top:20px;" v-if="total > 0">
        <el-pagination
          v-model:current-page="pageNum"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="fetchOrders"
        />
      </div>
    </el-card>

    <!-- 订单详情对话框 -->
    <el-dialog v-model="detailVisible" title="订单详情" width="600px">
      <div v-if="currentOrder" class="order-detail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="订单号">{{ currentOrder.orderId }}</el-descriptions-item>
          <el-descriptions-item label="客户">{{ currentOrder.customerName || currentOrder.customerCode }}</el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ currentOrder.orderDate }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">
            <el-tag :type="statusTagType(currentOrder.orderStatus)" size="small">{{ statusText(currentOrder.orderStatus) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="付款状态">
            <el-tag :type="currentOrder.paymentStatus === 1 ? 'success' : 'warning'" size="small">
              {{ currentOrder.paymentStatus === 1 ? '已付款' : '未付款' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="发货状态">
            <el-tag :type="currentOrder.shippingStatus === 1 ? 'primary' : currentOrder.shippingStatus === 2 ? 'success' : 'info'" size="small">
              {{ currentOrder.shippingStatus === 1 ? '已发货' : currentOrder.shippingStatus === 2 ? '已签收' : '未发货' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="收货人">{{ currentOrder.recipientName }}</el-descriptions-item>
          <el-descriptions-item label="收货电话">{{ currentOrder.recipientPhone }}</el-descriptions-item>
          <el-descriptions-item label="收货地址" :span="2">{{ currentOrder.recipientAddress }}</el-descriptions-item>
          <el-descriptions-item label="运费">¥ {{ Number(currentOrder.shippingFee || 0).toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="支付方式">{{ currentOrder.paymentMethod || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ currentOrder.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
        <h4 style="margin: 16px 0 8px;">商品明细</h4>
        <el-table :data="currentOrder.items || []" size="small" stripe>
          <el-table-column prop="productName" label="商品" />
          <el-table-column prop="unitPrice" label="单价" width="100">
            <template #default="{ row }">¥ {{ Number(row.unitPrice || 0).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="80" />
          <el-table-column label="小计" width="100">
            <template #default="{ row }">
              <span style="color: #f56c6c;">¥ {{ Number(row.totalAmount || 0).toFixed(2) }}</span>
            </template>
          </el-table-column>
        </el-table>
        <div class="detail-total">
          合计：<strong>¥ {{ Number(currentOrder.totalAmount || 0).toFixed(2) }}</strong>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getOrderList, getOrderById, confirmOrder, shipOrder, cancelOrder } from '@/api/order'

const searchKeyword = ref('')
const selectedStatus = ref('')
const selectedPayment = ref('')
const detailVisible = ref(false)
const currentOrder = ref(null)

const orders = ref([])
const total = ref(0)
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)

// 加载订单数据
const fetchOrders = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value
    }
    if (searchKeyword.value) {
      params.orderId = searchKeyword.value
    }
    if (selectedStatus.value !== '') {
      params.orderStatus = Number(selectedStatus.value)
    }
    if (selectedPayment.value !== '') {
      params.paymentStatus = Number(selectedPayment.value)
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

const handleSearch = () => {
  pageNum.value = 1
  fetchOrders()
}

const handleReset = () => {
  searchKeyword.value = ''
  selectedStatus.value = ''
  selectedPayment.value = ''
  pageNum.value = 1
  fetchOrders()
}

const statusText = (orderStatus) => {
  const map = { 0: '待确认', 1: '已确认', 2: '已完成', 3: '已取消' }
  return map[orderStatus] || '未知'
}

const statusTagType = (orderStatus) => {
  const map = { 0: 'warning', 1: 'primary', 2: 'success', 3: 'info' }
  return map[orderStatus] || 'info'
}

const handleViewDetail = async (row) => {
  try {
    const detail = await getOrderById(row.orderId)
    currentOrder.value = detail
    detailVisible.value = true
  } catch (e) {
    // 如果详情获取失败，用列表数据展示
    currentOrder.value = row
    detailVisible.value = true
  }
}

const handleConfirm = (row) => {
  ElMessageBox.confirm(`确认订单 ${row.orderId}？确认后顾客即可付款`, '确认订单', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'info'
  }).then(async () => {
    await confirmOrder(row.orderId)
    ElMessage.success('订单已确认')
    fetchOrders()
  }).catch(() => {})
}

const handleShip = (row) => {
  ElMessageBox.confirm(`确认对订单 ${row.orderId} 发货？`, '确认发货', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'info'
  }).then(async () => {
    await shipOrder(row.orderId)
    ElMessage.success('发货成功')
    fetchOrders()
  }).catch(() => {})
}

const handleCancel = (row) => {
  ElMessageBox.confirm(`确认取消订单 ${row.orderId}？取消后如果已扣库存将自动回滚`, '取消订单', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await cancelOrder(row.orderId)
    ElMessage.success('订单已取消')
    fetchOrders()
  }).catch(() => {})
}
</script>

<style scoped>
.admin-orders {
  padding: 0;
}

.toolbar-card {
  margin-bottom: 16px;
}

.order-detail h4 {
  color: #303133;
}

.detail-total {
  text-align: right;
  margin-top: 16px;
  font-size: 16px;
  color: #606266;
}

.detail-total strong {
  color: #f56c6c;
  font-size: 20px;
}
</style>
