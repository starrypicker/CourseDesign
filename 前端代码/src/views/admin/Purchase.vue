<template>
  <div class="admin-purchase">
    <el-card shadow="never" class="toolbar-card">
      <el-row :gutter="16" align="middle">
        <el-col :xs="24" :sm="4" :md="3"><el-button type="primary" @click="handleAdd"><el-icon><Plus /></el-icon>添加进货记录</el-button></el-col>
        <el-col :xs="12" :sm="4" :md="3"><el-select v-model="filterStatus" placeholder="状态筛选" clearable @change="fetchRecords"><el-option label="全部" value="" /><el-option label="待入库" :value="0" /><el-option label="已入库" :value="1" /><el-option label="已取消" :value="2" /></el-select></el-col>
        <el-col :xs="24" :sm="16" :md="18" style="text-align:right;"><el-button @click="fetchRecords"><el-icon><Refresh /></el-icon>刷新</el-button></el-col>
      </el-row>
    </el-card>
    <el-card shadow="never">
      <el-table :data="records" stripe style="width:100%" v-loading="loading">
        <el-table-column prop="id" label="记录ID" width="80" />
        <el-table-column prop="productName" label="商品名称" min-width="160">
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
        <el-table-column prop="manufacturerName" label="厂家" width="120" />
        <el-table-column prop="quantity" label="进货数量" width="100" />
        <el-table-column prop="unitPrice" label="单价" width="100"><template #default="{ row }">¥ {{ Number(row.unitPrice || 0).toFixed(2) }}</template></el-table-column>
        <el-table-column prop="totalAmount" label="总金额" width="120"><template #default="{ row }"><span style="color:#f56c6c;font-weight:500;">¥ {{ Number(row.totalAmount || 0).toFixed(2) }}</span></template></el-table-column>
        <el-table-column prop="status" label="状态" width="100"><template #default="{ row }"><el-tag :type="row.status === 0 ? 'warning' : row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 0 ? '待入库' : row.status === 1 ? '已入库' : '已取消' }}</el-tag></template></el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" text type="success" size="small" @click="handleConfirm(row)">确认入库</el-button>
            <el-button text type="warning" size="small" @click="handleContactManufacturer(row)">联系厂家</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="total > 0" style="display:flex;justify-content:center;margin-top:20px">
        <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total"
          layout="total, prev, pager, next" @current-change="fetchRecords" />
      </div>
    </el-card>
    <el-dialog v-model="dialogVisible" title="添加进货记录" width="500px" destroy-on-close>
      <el-form :model="purchaseForm" :rules="purchaseRules" ref="purchaseFormRef" label-width="80px">
        <el-form-item label="商品" prop="productCode"><el-select v-model="purchaseForm.productCode" placeholder="请选择商品" style="width:100%;" filterable><el-option v-for="p in productOptions" :key="p.productCode" :label="`${p.productName} (库存:${p.stockQuantity})`" :value="p.productCode" /></el-select></el-form-item>
        <el-form-item label="厂家" prop="manufacturerCode"><el-select v-model="purchaseForm.manufacturerCode" placeholder="请选择厂家" style="width:100%;"><el-option v-for="m in manufacturerOptions" :key="m.manufacturerCode" :label="m.manufacturerName" :value="m.manufacturerCode" /></el-select></el-form-item>
        <el-form-item label="数量" prop="quantity"><el-input-number v-model="purchaseForm.quantity" :min="1" style="width:100%;" /></el-form-item>
        <el-form-item label="单价" prop="unitPrice"><el-input-number v-model="purchaseForm.unitPrice" :min="0" :precision="2" style="width:100%;" /></el-form-item>
        <el-form-item label="总金额"><span style="color:#f56c6c;font-weight:500;">¥ {{ (purchaseForm.quantity * (purchaseForm.unitPrice || 0)).toFixed(2) }}</span></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button></template>
    </el-dialog>

    <!-- 商品详情弹窗 -->
    <ProductDetailDialog v-model:visible="productDetailVisible" :product="productDetail" :show-add-cart="false" />

    <!-- 联系厂家弹窗 -->
    <el-dialog v-model="manufacturerVisible" title="厂家联系信息" width="500px">
      <template v-if="currentManufacturer">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="厂家名称">{{ currentManufacturer.manufacturerName }}</el-descriptions-item>
          <el-descriptions-item label="厂家代码">{{ currentManufacturer.manufacturerCode }}</el-descriptions-item>
          <el-descriptions-item label="联系人">{{ currentManufacturer.contactPerson || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ currentManufacturer.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="地址">{{ currentManufacturer.address || '-' }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="currentPurchaseItem" style="margin-top:16px;padding:12px;background:#fdf6ec;border-radius:4px;">
          <p style="margin:0;color:#E6A23C;">📋 待进货商品：<strong>{{ currentPurchaseItem.productName }}</strong>，需进货 <strong>{{ currentPurchaseItem.quantity }}</strong> 件</p>
        </div>
      </template>
      <template #footer>
        <el-button @click="manufacturerVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, ShoppingBag } from '@element-plus/icons-vue'
import { getPurchaseList, addPurchase, confirmPurchase } from '@/api/purchase'
import { getProductList } from '@/api/product'
import { getManufacturerList, getManufacturerByCode } from '@/api/manufacturer'
import ProductDetailDialog from '@/components/ProductDetailDialog.vue'
import { getProductImage } from '@/utils/constants'

const records = ref([])
const productOptions = ref([])
const manufacturerOptions = ref([])
const loading = ref(false)
const submitting = ref(false)
const filterStatus = ref('')
const dialogVisible = ref(false)
const manufacturerVisible = ref(false)
const currentManufacturer = ref(null)
const currentPurchaseItem = ref(null)
const purchaseFormRef = ref(null)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const purchaseForm = reactive({ productCode: '', manufacturerCode: '', quantity: 10, unitPrice: 0 })
const purchaseRules = {
  productCode: [{ required: true, message: '请选择商品', trigger: 'change' }],
  manufacturerCode: [{ required: true, message: '请选择厂家', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入数量', trigger: 'blur' }],
  unitPrice: [{ required: true, message: '请输入单价', trigger: 'blur' }]
}

const fetchRecords = async () => {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (filterStatus.value !== '') params.status = filterStatus.value
    const res = await getPurchaseList(params)
    records.value = res.rows || []
    total.value = res.total || 0
  } catch (e) { console.error('获取进货记录失败:', e) } finally { loading.value = false }
}

const fetchOptions = async () => {
  try {
    const [products, manufacturers] = await Promise.all([getProductList({ pageNum: 1, pageSize: 200 }), getManufacturerList()])
    productOptions.value = products.rows || []; manufacturerOptions.value = manufacturers || []
  } catch (e) { console.error('获取选项失败:', e) }
}

onMounted(() => { fetchRecords(); fetchOptions() })

const handleAdd = () => { resetForm(); dialogVisible.value = true }
const resetForm = () => { purchaseForm.productCode = ''; purchaseForm.manufacturerCode = ''; purchaseForm.quantity = 10; purchaseForm.unitPrice = 0 }

const handleSubmit = async () => {
  if (!purchaseFormRef.value) return
  try {
    await purchaseFormRef.value.validate()
    submitting.value = true
    await addPurchase({ productCode: purchaseForm.productCode, manufacturerCode: purchaseForm.manufacturerCode, quantity: purchaseForm.quantity, unitPrice: purchaseForm.unitPrice, totalAmount: purchaseForm.quantity * purchaseForm.unitPrice })
    ElMessage.success('进货记录添加成功'); dialogVisible.value = false; pageNum.value = 1; fetchRecords()
  } catch (e) { if (e !== 'cancel') console.error('添加进货记录失败:', e) } finally { submitting.value = false }
}

const handleConfirm = (row) => {
  ElMessageBox.confirm(`确认将 ${row.quantity} 件"${row.productName}"入库？`, '确认入库', { confirmButtonText: '确定入库', cancelButtonText: '取消', type: 'info' })
    .then(async () => { await confirmPurchase(row.id); ElMessage.success('入库成功，库存已更新'); fetchRecords() }).catch(() => {})
}

const productDetailVisible = ref(false)
const productDetail = ref(null)
const handleViewProduct = (item) => {
  if (item && item.productCode) {
    productDetail.value = { productCode: item.productCode, productName: item.productName || item.productCode }
    productDetailVisible.value = true
  }
}

const handleContactManufacturer = async (row) => {
  try {
    currentPurchaseItem.value = row
    const manufacturer = await getManufacturerByCode(row.manufacturerCode)
    currentManufacturer.value = manufacturer
    manufacturerVisible.value = true
  } catch (e) {
    // 如果API失败，尝试从manufacturerOptions中查找
    const found = manufacturerOptions.value.find(m => m.manufacturerCode === row.manufacturerCode)
    currentManufacturer.value = found || { manufacturerName: row.manufacturerName || row.manufacturerCode, manufacturerCode: row.manufacturerCode }
    manufacturerVisible.value = true
  }
}
</script>

<style scoped>
.admin-purchase { padding: 0; }
.toolbar-card { margin-bottom: 16px; }
</style>
