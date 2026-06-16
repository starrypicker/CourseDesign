<template>
  <div class="admin-low-stock">
    <el-card shadow="never" class="toolbar-card">
      <el-row :gutter="16" align="middle">
        <el-col :xs="24" :sm="8"><h2 style="margin:0;"><el-icon><Warning /></el-icon> 缺货商品管理</h2></el-col>
        <el-col :xs="24" :sm="16" style="text-align:right;">
          <el-button @click="fetchData"><el-icon><Refresh /></el-icon>刷新</el-button>
          <el-button type="warning" @click="handleCheckAll">一键检查库存</el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 低库存商品 -->
    <el-card shadow="never" style="margin-top:16px;">
      <template #header>
        <div class="card-header">
          <span><el-tag type="danger" size="large">{{ filteredLowStock.length }}</el-tag> 件商品库存不足</span>
          <span style="color:#909399;font-size:13px;">库存低于最低库存线（minStock）</span>
        </div>
      </template>
      <el-table :data="filteredLowStock" stripe v-loading="loading" style="width:100%">
        <el-table-column prop="productCode" label="商品编码" width="100" />
        <el-table-column prop="productName" label="商品名称" min-width="180">
          <template #default="{ row }">
            <div style="display:flex;align-items:center;gap:8px;">
              <div style="width:32px;height:32px;background:#f5f7fa;border-radius:4px;overflow:hidden;flex-shrink:0;">
                <img v-if="getProductImage(row)" :src="getProductImage(row)" style="width:100%;height:100%;object-fit:cover;" />
                <el-icon v-else :size="16" color="#909399" style="display:flex;align-items:center;justify-content:center;width:100%;height:100%;"><ShoppingBag /></el-icon>
              </div>
              <span style="color:#409EFF;cursor:pointer;" @click="handleViewProduct(row)">{{ row.productName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="manufacturerName" label="生产厂家" width="160" />
        <el-table-column label="当前库存" width="120">
          <template #default="{ row }">
            <span :style="{ color: row.stockQuantity <= 0 ? '#f56c6c' : '#E6A23C', fontWeight: 'bold' }">
              {{ row.stockQuantity }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="minStock" label="最低库存线" width="110" />
        <el-table-column prop="unitPrice" label="单价" width="100">
          <template #default="{ row }">¥ {{ Number(row.unitPrice || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="建议补货量" width="120">
          <template #default="{ row }">
            <span style="color:#409EFF;font-weight:bold;">
              {{ Math.max(0, (row.minStock || 10) * 3 - (row.stockQuantity || 0)) }} 件
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="handleQuickReplenish(row)">
              <el-icon><Box /></el-icon>快速补货
            </el-button>
            <el-button text type="success" size="small" @click="handleCreatePurchase(row)">
              <el-icon><Plus /></el-icon>创建进货单
            </el-button>
            <el-button text type="warning" size="small" @click="handleContactManufacturer(row)">
              <el-icon><Phone /></el-icon>联系厂家
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && filteredLowStock.length === 0" description="🎉 所有商品库存充足" />
    </el-card>

    <!-- 缺货订单 -->
    <el-card shadow="never" style="margin-top:20px;">
      <template #header>
        <div class="card-header">
          <span><el-tag type="danger" size="large">{{ outOfStockOrders.length }}</el-tag> 笔缺货订单</span>
          <span style="color:#909399;font-size:13px;">订单中有商品库存不足，标记为缺货（canSupply=0）</span>
        </div>
      </template>
      <el-table :data="outOfStockOrders" stripe v-loading="loadingOrders" style="width:100%">
        <el-table-column prop="orderId" label="订单号" width="90" />
        <el-table-column prop="customerName" label="客户" width="160">
          <template #default="{ row }">{{ row.customerName || row.customerCode }}</template>
        </el-table-column>
        <el-table-column label="缺货商品" min-width="220">
          <template #default="{ row }">{{ (row.items || []).map(i => i.productName || i.productCode).join('、') }}</template>
        </el-table-column>
        <el-table-column label="金额" width="110">
          <template #default="{ row }"><span style="color:#f56c6c;font-weight:500;">¥ {{ Number(row.totalAmount || 0).toFixed(2) }}</span></template>
        </el-table-column>
        <el-table-column prop="orderDate" label="下单时间" width="170" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button text type="success" size="small" @click="handleMarkSuppliable(row)">
              标记可供货
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loadingOrders && outOfStockOrders.length === 0" description="暂无缺货订单" />
    </el-card>

    <!-- 商品详情弹窗 -->
    <ProductDetailDialog v-model:visible="productDetailVisible" :product="productDetail" :show-add-cart="false" />

    <!-- 快速补货弹窗 -->
    <el-dialog v-model="replenishVisible" title="快速补货" width="400px" destroy-on-close>
      <el-form :model="replenishForm" ref="replenishFormRef" label-width="90px">
        <el-form-item label="商品名称">
          <span style="font-weight:bold;">{{ replenishForm.productName }}</span>
        </el-form-item>
        <el-form-item label="当前库存">
          <span :style="{ color: '#f56c6c', fontWeight: 'bold' }">{{ replenishForm.currentStock }} 件</span>
        </el-form-item>
        <el-form-item label="补货数量" prop="quantity">
          <el-input-number v-model="replenishForm.quantity" :min="1" :max="99999" style="width:100%;" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="replenishVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmReplenish" :loading="submitting">确认补货</el-button>
      </template>
    </el-dialog>

    <!-- 厂家信息弹窗 -->
    <el-dialog v-model="manufacturerVisible" title="厂家联系信息" width="500px">
      <template v-if="currentManufacturer">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="厂家名称">{{ currentManufacturer.manufacturerName }}</el-descriptions-item>
          <el-descriptions-item label="厂家代码">{{ currentManufacturer.manufacturerCode }}</el-descriptions-item>
          <el-descriptions-item label="联系人">{{ currentManufacturer.contactPerson || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ currentManufacturer.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="地址">{{ currentManufacturer.address || '-' }}</el-descriptions-item>
        </el-descriptions>
      </template>
      <template #footer>
        <el-button @click="manufacturerVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Warning, Refresh, Box, Plus, Phone } from '@element-plus/icons-vue'
import { getLowStockProducts, replenishStock } from '@/api/product'
import { getOutOfStockOrders, markAsSuppliable } from '@/api/order'
import { getManufacturerByCode } from '@/api/manufacturer'
import { addPurchase } from '@/api/purchase'
import { useRouter } from 'vue-router'
import ProductDetailDialog from '@/components/ProductDetailDialog.vue'
import { getProductImage } from '@/utils/constants'

const router = useRouter()
const lowStockProducts = ref([])
const outOfStockOrders = ref([])
const loading = ref(false)
const loadingOrders = ref(false)
const submitting = ref(false)

const replenishVisible = ref(false)
const manufacturerVisible = ref(false)
const currentManufacturer = ref(null)
const replenishFormRef = ref(null)
const replenishForm = reactive({ productCode: '', productName: '', currentStock: 0, quantity: 0 })

const productDetailVisible = ref(false)
const productDetail = ref(null)
const handleViewProduct = (item) => {
  if (item && item.productCode) {
    productDetail.value = { productCode: item.productCode, productName: item.productName || item.productCode }
    productDetailVisible.value = true
  }
}

// 前端二次过滤：确保只显示真正低于最低库存的商品
const filteredLowStock = computed(() =>
  lowStockProducts.value.filter(item => item.stockQuantity < item.minStock)
)

const fetchData = async () => {
  loading.value = true
  loadingOrders.value = true
  try {
    const [products, orders] = await Promise.all([
      getLowStockProducts(),
      getOutOfStockOrders()
    ])
    lowStockProducts.value = products || []
    outOfStockOrders.value = orders || []
  } catch (e) {
    console.error('获取缺货数据失败:', e)
  } finally {
    loading.value = false
    loadingOrders.value = false
  }
}

onMounted(() => fetchData())

const handleCheckAll = () => {
  ElMessage.info('正在检查库存...')
  fetchData()
}

const handleQuickReplenish = (row) => {
  const suggested = Math.max(0, (row.minStock || 10) * 3 - (row.stockQuantity || 0))
  replenishForm.productCode = row.productCode
  replenishForm.productName = row.productName
  replenishForm.currentStock = row.stockQuantity || 0
  replenishForm.quantity = suggested || 10
  replenishVisible.value = true
}

const handleConfirmReplenish = async () => {
  if (replenishForm.quantity <= 0) {
    ElMessage.warning('请输入有效的补货数量')
    return
  }
  submitting.value = true
  try {
    await replenishStock(replenishForm.productCode, replenishForm.quantity)
    ElMessage.success(`补货成功，已增加 ${replenishForm.quantity} 件库存`)
    replenishVisible.value = false
    fetchData()
  } catch (e) {
    console.error('补货失败:', e)
  } finally {
    submitting.value = false
  }
}

const handleCreatePurchase = async (row) => {
  const suggested = Math.max(0, (row.minStock || 10) * 3 - (row.stockQuantity || 0))
  try {
    await ElMessageBox.confirm(
      `确认创建进货单？商品：${row.productName}，建议进货 ${suggested || 10} 件`,
      '创建进货单',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'info' }
    )
    await addPurchase({
      productCode: row.productCode,
      manufacturerCode: row.manufacturerCode,
      quantity: suggested || 10,
      unitPrice: row.unitPrice,
      totalAmount: (row.unitPrice || 0) * (suggested || 10)
    })
    ElMessage.success('进货记录已创建，请在进货管理中确认入库')
    router.push('/admin/purchase')
  } catch (e) {
    if (e !== 'cancel') console.error('创建进货单失败:', e)
  }
}

const handleContactManufacturer = async (row) => {
  try {
    const manufacturer = await getManufacturerByCode(row.manufacturerCode)
    currentManufacturer.value = manufacturer
    manufacturerVisible.value = true
  } catch (e) {
    currentManufacturer.value = { manufacturerName: row.manufacturerName || row.manufacturerCode, manufacturerCode: row.manufacturerCode }
    manufacturerVisible.value = true
  }
}

const handleMarkSuppliable = (row) => {
  ElMessageBox.confirm(
    `确认标记订单 ${row.orderId} 为可供货？将扣减对应商品库存`,
    '标记可供货',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'info' }
  )
    .then(async () => {
      await markAsSuppliable(row.orderId)
      ElMessage.success('已标记为可供货')
      fetchData()
    })
    .catch(() => {})
}
</script>

<style scoped>
.admin-low-stock { padding: 0; }
.toolbar-card { margin-bottom: 0; }
.card-header { display: flex; align-items: center; gap: 12px; }
</style>
