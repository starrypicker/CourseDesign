<template>
  <div class="admin-reports">
    <el-card shadow="never">
      <template #header>
        <div class="page-header">
          <h2><el-icon><DataLine /></el-icon> 订单报表</h2>
        </div>
      </template>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- 已完成订单 -->
        <el-tab-pane label="已完成订单" name="completed">
          <el-table :data="completedOrders" stripe v-loading="loadingCompleted" style="width:100%">
            <el-table-column prop="orderId" label="订单号" width="90" />
            <el-table-column prop="customerName" label="客户" width="160">
              <template #default="{ row }">{{ row.customerName || row.customerCode }}</template>
            </el-table-column>
            <el-table-column label="商品" min-width="220">
              <template #default="{ row }">
                <span v-for="(item, idx) in (row.items || [])" :key="idx">
                  <span v-if="idx > 0">、</span>
                  <span style="color:#409EFF;cursor:pointer;" @click="handleViewProduct(item)">{{ item.productName || item.productCode }}</span>
                </span>
              </template>
            </el-table-column>
            <el-table-column label="金额" width="110">
              <template #default="{ row }"><span style="color:#f56c6c;font-weight:500;">¥ {{ Number(row.totalAmount || 0).toFixed(2) }}</span></template>
            </el-table-column>
            <el-table-column label="付款" width="80">
              <template #default="{ row }">
                <el-tag :type="row.paymentStatus === 1 ? 'success' : row.paymentStatus === 2 ? 'info' : 'warning'" size="small">
                  {{ row.paymentStatus === 1 ? '已付' : row.paymentStatus === 2 ? '退款' : '未付' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="发货" width="80">
              <template #default="{ row }">
                <el-tag :type="row.shippingStatus === 2 ? 'success' : row.shippingStatus === 1 ? 'primary' : 'info'" size="small">
                  {{ row.shippingStatus === 2 ? '签收' : row.shippingStatus === 1 ? '已发' : '未发' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="orderDate" label="下单时间" width="170" />
            <el-table-column prop="shippingDate" label="运输日期" width="170">
              <template #default="{ row }">{{ row.shippingDate || '-' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button text type="primary" size="small" @click="handleViewDetail(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!loadingCompleted && completedOrders.length === 0" description="暂无已完成订单" />
        </el-tab-pane>

        <!-- 未付款订单 -->
        <el-tab-pane label="未付款订单" name="unpaid">
          <div class="tab-summary">
            <el-tag type="warning" size="large">{{ unpaidOrders.length }} 笔未付款订单</el-tag>
            <span style="margin-left:12px;color:#f56c6c;font-weight:bold;">
              合计金额：¥ {{ unpaidTotal.toFixed(2) }}
            </span>
          </div>
          <el-table :data="unpaidOrders" stripe v-loading="loadingUnpaid" style="width:100%;margin-top:12px;">
            <el-table-column prop="orderId" label="订单号" width="90" />
            <el-table-column prop="customerName" label="客户" width="160">
              <template #default="{ row }">{{ row.customerName || row.customerCode }}</template>
            </el-table-column>
            <el-table-column label="商品" min-width="220">
              <template #default="{ row }">
                <span v-for="(item, idx) in (row.items || [])" :key="idx">
                  <span v-if="idx > 0">、</span>
                  <span style="color:#409EFF;cursor:pointer;" @click="handleViewProduct(item)">{{ item.productName || item.productCode }}</span>
                </span>
              </template>
            </el-table-column>
            <el-table-column label="金额" width="110">
              <template #default="{ row }"><span style="color:#f56c6c;font-weight:500;">¥ {{ Number(row.totalAmount || 0).toFixed(2) }}</span></template>
            </el-table-column>
            <el-table-column prop="orderDate" label="下单时间" width="170" />
            <el-table-column label="订单状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.orderStatus === 0 ? 'warning' : 'primary'" size="small">
                  {{ row.orderStatus === 0 ? '待确认' : '已确认' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button text type="primary" size="small" @click="handleViewDetail(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!loadingUnpaid && unpaidOrders.length === 0" description="暂无未付款订单" />
        </el-tab-pane>

        <!-- 未发货订单 -->
        <el-tab-pane label="未发货订单" name="unshipped">
          <div class="tab-summary">
            <el-tag type="danger" size="large">{{ unshippedOrders.length }} 笔未发货订单</el-tag>
            <span style="margin-left:12px;color:#f56c6c;font-weight:bold;">
              合计金额：¥ {{ unshippedTotal.toFixed(2) }}
            </span>
          </div>
          <el-table :data="unshippedOrders" stripe v-loading="loadingUnshipped" style="width:100%;margin-top:12px;">
            <el-table-column prop="orderId" label="订单号" width="90" />
            <el-table-column prop="customerName" label="客户" width="160">
              <template #default="{ row }">{{ row.customerName || row.customerCode }}</template>
            </el-table-column>
            <el-table-column label="商品" min-width="220">
              <template #default="{ row }">
                <span v-for="(item, idx) in (row.items || [])" :key="idx">
                  <span v-if="idx > 0">、</span>
                  <span style="color:#409EFF;cursor:pointer;" @click="handleViewProduct(item)">{{ item.productName || item.productCode }}</span>
                </span>
              </template>
            </el-table-column>
            <el-table-column label="金额" width="110">
              <template #default="{ row }"><span style="color:#f56c6c;font-weight:500;">¥ {{ Number(row.totalAmount || 0).toFixed(2) }}</span></template>
            </el-table-column>
            <el-table-column label="收件人" width="100">
              <template #default="{ row }">{{ row.recipientName || '-' }}</template>
            </el-table-column>
            <el-table-column label="收件地址" min-width="160" show-overflow-tooltip>
              <template #default="{ row }">{{ row.recipientAddress || '-' }}</template>
            </el-table-column>
            <el-table-column label="运输要求" width="120" show-overflow-tooltip>
              <template #default="{ row }">{{ row.shippingRequirement || '-' }}</template>
            </el-table-column>
            <el-table-column prop="orderDate" label="下单时间" width="170" />
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button text type="primary" size="small" @click="handleViewDetail(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!loadingUnshipped && unshippedOrders.length === 0" description="暂无未发货订单" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 商品详情弹窗 -->
    <ProductDetailDialog v-model:visible="productDetailVisible" :product="productDetail" :show-add-cart="false" />

    <!-- 订单详情弹窗 -->
    <el-dialog v-model="detailVisible" title="订单详情" width="650px">
      <div v-if="currentOrder" class="order-detail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="订单号">{{ currentOrder.orderId }}</el-descriptions-item>
          <el-descriptions-item label="客户">{{ currentOrder.customerName || currentOrder.customerCode }}</el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ currentOrder.orderDate }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">
            <el-tag :type="statusTagType(currentOrder.orderStatus)" size="small">{{ statusText(currentOrder.orderStatus) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="付款状态">
            <el-tag :type="currentOrder.paymentStatus === 1 ? 'success' : currentOrder.paymentStatus === 2 ? 'info' : 'warning'" size="small">
              {{ currentOrder.paymentStatus === 1 ? '已付款' : currentOrder.paymentStatus === 2 ? '已退款' : '未付款' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="发货状态">
            <el-tag :type="currentOrder.shippingStatus === 2 ? 'success' : currentOrder.shippingStatus === 1 ? 'primary' : 'info'" size="small">
              {{ currentOrder.shippingStatus === 2 ? '已签收' : currentOrder.shippingStatus === 1 ? '已发货' : '未发货' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="收货人">{{ currentOrder.recipientName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="收货电话">{{ currentOrder.recipientPhone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="收货地址" :span="2">{{ currentOrder.recipientAddress || '-' }}</el-descriptions-item>
          <el-descriptions-item label="运输要求">{{ currentOrder.shippingRequirement || '-' }}</el-descriptions-item>
          <el-descriptions-item label="运费">¥ {{ Number(currentOrder.shippingFee || 0).toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="支付方式">{{ currentOrder.paymentMethod || '-' }}</el-descriptions-item>
          <el-descriptions-item label="付款清单号" v-if="currentOrder.paymentListNo">{{ currentOrder.paymentListNo }}</el-descriptions-item>
          <el-descriptions-item label="支付时间" v-if="currentOrder.paymentTime">{{ currentOrder.paymentTime }}</el-descriptions-item>
          <el-descriptions-item label="运输日期" v-if="currentOrder.shippingDate">{{ currentOrder.shippingDate }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ currentOrder.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
        <h4 style="margin:16px 0 8px;">商品明细</h4>
        <el-table :data="currentOrder.items || []" size="small" stripe>
          <el-table-column prop="productName" label="商品" />
          <el-table-column prop="unitPrice" label="单价" width="100">
            <template #default="{ row }">¥ {{ Number(row.unitPrice || 0).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="80" />
          <el-table-column label="小计" width="100">
            <template #default="{ row }"><span style="color:#f56c6c;">¥ {{ Number(row.totalAmount || 0).toFixed(2) }}</span></template>
          </el-table-column>
        </el-table>
        <div class="detail-total">合计：<strong>¥ {{ Number(currentOrder.totalAmount || 0).toFixed(2) }}</strong></div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { DataLine } from '@element-plus/icons-vue'
import { getCompletedOrders, getUnpaidOrders, getUnshippedOrders, getOrderById } from '@/api/order'
import ProductDetailDialog from '@/components/ProductDetailDialog.vue'

const activeTab = ref('completed')
const completedOrders = ref([])
const unpaidOrders = ref([])
const unshippedOrders = ref([])
const loadingCompleted = ref(false)
const loadingUnpaid = ref(false)
const loadingUnshipped = ref(false)
const detailVisible = ref(false)
const currentOrder = ref(null)

const productDetailVisible = ref(false)
const productDetail = ref(null)
const handleViewProduct = (item) => {
  if (item && item.productCode) {
    productDetail.value = { productCode: item.productCode, productName: item.productName || item.productCode }
    productDetailVisible.value = true
  }
}

const unpaidTotal = computed(() =>
  unpaidOrders.value.reduce((sum, o) => sum + Number(o.totalAmount || 0), 0)
)
const unshippedTotal = computed(() =>
  unshippedOrders.value.reduce((sum, o) => sum + Number(o.totalAmount || 0), 0)
)

const fetchCompleted = async () => {
  loadingCompleted.value = true
  try { const res = await getCompletedOrders(); completedOrders.value = res || [] }
  catch (e) { console.error('获取已完成订单失败:', e) }
  finally { loadingCompleted.value = false }
}

const fetchUnpaid = async () => {
  loadingUnpaid.value = true
  try { const res = await getUnpaidOrders(); unpaidOrders.value = res || [] }
  catch (e) { console.error('获取未付款订单失败:', e) }
  finally { loadingUnpaid.value = false }
}

const fetchUnshipped = async () => {
  loadingUnshipped.value = true
  try { const res = await getUnshippedOrders(); unshippedOrders.value = res || [] }
  catch (e) { console.error('获取未发货订单失败:', e) }
  finally { loadingUnshipped.value = false }
}

const handleTabChange = (tab) => {
  if (tab === 'completed') fetchCompleted()
  else if (tab === 'unpaid') fetchUnpaid()
  else if (tab === 'unshipped') fetchUnshipped()
}

const handleViewDetail = async (row) => {
  try {
    const detail = await getOrderById(row.orderId)
    currentOrder.value = detail
    detailVisible.value = true
  } catch (e) { currentOrder.value = row; detailVisible.value = true }
}

const statusText = (orderStatus) => {
  const map = { 0: '待确认', 1: '已确认', 2: '已完成', 3: '已取消' }
  return map[orderStatus] || '未知'
}
const statusTagType = (orderStatus) => {
  const map = { 0: 'warning', 1: 'primary', 2: 'success', 3: 'info' }
  return map[orderStatus] || 'info'
}

onMounted(() => fetchCompleted())
</script>

<style scoped>
.admin-reports { padding: 0; }
.page-header { display: flex; align-items: center; justify-content: space-between; }
.page-header h2 { display: flex; align-items: center; gap: 8px; font-size: 18px; margin: 0; }
.tab-summary { padding: 12px 0; display: flex; align-items: center; }
.order-detail h4 { color: #303133; }
.detail-total { text-align: right; margin-top: 16px; font-size: 16px; color: #606266; }
.detail-total strong { color: #f56c6c; font-size: 20px; }
</style>
