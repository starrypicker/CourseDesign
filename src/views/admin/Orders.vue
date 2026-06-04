
<template>
  <div class="admin-orders">
    <!-- 操作栏 -->
    <el-card shadow="never" class="toolbar-card">
      <el-row :gutter="16" align="middle">
        <el-col :xs="24" :sm="8" :md="6">
          <el-input v-model="searchKeyword" placeholder="搜索订单号/客户名..." prefix-icon="Search" clearable />
        </el-col>
        <el-col :xs="24" :sm="6" :md="4">
          <el-select v-model="selectedStatus" placeholder="订单状态" clearable>
            <el-option label="全部状态" value="" />
            <el-option label="待付款" value="pending" />
            <el-option label="已付款" value="paid" />
            <el-option label="已发货" value="shipped" />
            <el-option label="已完成" value="completed" />
          </el-select>
        </el-col>
      </el-row>
    </el-card>

    <!-- 订单表格 -->
    <el-card shadow="never">
      <el-table :data="filteredOrders" stripe style="width: 100%">
        <el-table-column prop="orderNo" label="订单号" width="150" />
        <el-table-column prop="customer" label="客户" width="100" />
        <el-table-column label="商品" min-width="200">
          <template #default="{ row }">
            <span>{{ row.items.map(i => i.name).join('、') }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="金额" width="100">
          <template #default="{ row }">
            <span style="color: #f56c6c; font-weight: 500;">¥ {{ row.totalAmount.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="handleViewDetail(row)">详情</el-button>
            <el-button v-if="row.status === 'paid'" text type="success" size="small" @click="handleShip(row)">发货</el-button>
            <el-button v-if="row.status === 'pending'" text type="warning" size="small" @click="handleCancel(row)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 订单详情对话框 -->
    <el-dialog v-model="detailVisible" title="订单详情" width="600px">
      <div v-if="currentOrder" class="order-detail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="订单号">{{ currentOrder.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="客户">{{ currentOrder.customer }}</el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ currentOrder.createTime }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagType(currentOrder.status)" size="small">{{ statusText(currentOrder.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="收货地址" :span="2">{{ currentOrder.address }}</el-descriptions-item>
        </el-descriptions>
        <h4 style="margin: 16px 0 8px;">商品明细</h4>
        <el-table :data="currentOrder.items" size="small" stripe>
          <el-table-column prop="name" label="商品" />
          <el-table-column prop="price" label="单价" width="100">
            <template #default="{ row }">¥ {{ row.price.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="80" />
          <el-table-column label="小计" width="100">
            <template #default="{ row }">
              <span style="color: #f56c6c;">¥ {{ (row.price * row.quantity).toFixed(2) }}</span>
            </template>
          </el-table-column>
        </el-table>
        <div class="detail-total">
          合计：<strong>¥ {{ currentOrder.totalAmount.toFixed(2) }}</strong>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const searchKeyword = ref('')
const selectedStatus = ref('')
const detailVisible = ref(false)
const currentOrder = ref(null)

const orders = ref([
  { id: 1, orderNo: 'SP20260604001', customer: '张三', totalAmount: 728, status: 'completed', createTime: '2026-06-04 10:20', address: '北京市朝阳区建国路88号', items: [{ name: '专业跑步鞋', price: 599, quantity: 1 }, { name: '透气运动T恤', price: 129, quantity: 1 }] },
  { id: 2, orderNo: 'SP20260604002', customer: '李四', totalAmount: 880, status: 'shipped', createTime: '2026-06-04 11:30', address: '上海市浦东新区陆家嘴路100号', items: [{ name: '碳纤维羽毛球拍', price: 880, quantity: 1 }] },
  { id: 3, orderNo: 'SP20260604003', customer: '王五', totalAmount: 259, status: 'paid', createTime: '2026-06-04 12:45', address: '广州市天河区天河路200号', items: [{ name: '哑铃套装 20kg', price: 259, quantity: 1 }] },
  { id: 4, orderNo: 'SP20260604004', customer: '赵六', totalAmount: 459, status: 'pending', createTime: '2026-06-04 13:10', address: '深圳市南山区科技路50号', items: [{ name: '冲锋衣', price: 459, quantity: 1 }] },
  { id: 5, orderNo: 'SP20260604005', customer: '孙七', totalAmount: 678, status: 'paid', createTime: '2026-06-04 14:25', address: '杭州市西湖区文三路300号', items: [{ name: '篮球鞋', price: 499, quantity: 1 }, { name: '运动短裤', price: 79, quantity: 1 }, { name: '透气运动T恤', price: 100, quantity: 1 }] },
  { id: 6, orderNo: 'SP20260604006', customer: '周八', totalAmount: 369, status: 'completed', createTime: '2026-06-04 09:50', address: '成都市武侯区人民路66号', items: [{ name: '户外登山包 50L', price: 369, quantity: 1 }] }
])

const filteredOrders = computed(() => {
  let result = orders.value
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    result = result.filter(o => o.orderNo.toLowerCase().includes(kw) || o.customer.includes(kw))
  }
  if (selectedStatus.value) {
    result = result.filter(o => o.status === selectedStatus.value)
  }
  return result
})

const statusText = (status) => {
  const map = { pending: '待付款', paid: '已付款', shipped: '已发货', completed: '已完成' }
  return map[status] || status
}

const statusTagType = (status) => {
  const map = { pending: 'warning', paid: 'primary', shipped: '', completed: 'success' }
  return map[status] || 'info'
}

const handleViewDetail = (row) => {
  currentOrder.value = row
  detailVisible.value = true
}

const handleShip = (row) => {
  ElMessageBox.confirm(`确认对订单 ${row.orderNo} 发货？`, '确认发货', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'info'
  }).then(() => {
    row.status = 'shipped'
    ElMessage.success('发货成功')
  }).catch(() => {})
}

const handleCancel = (row) => {
  ElMessageBox.confirm(`确认取消订单 ${row.orderNo}？`, '取消订单', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    orders.value = orders.value.filter(o => o.id !== row.id)
    ElMessage.success('订单已取消')
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
