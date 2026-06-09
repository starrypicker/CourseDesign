
<template>
  <div class="admin-purchase">
    <!-- 操作栏 -->
    <el-card shadow="never" class="toolbar-card">
      <el-row :gutter="16" align="middle">
        <el-col :xs="24" :sm="4" :md="3">
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon> 添加进货记录
          </el-button>
        </el-col>
        <el-col :xs="24" :sm="20" :md="21" style="text-align: right;">
          <el-button @click="fetchRecords">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 进货记录表格 -->
    <el-card shadow="never">
      <el-table :data="records" stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="记录ID" width="80" />
        <el-table-column prop="productName" label="商品名称" min-width="160" />
        <el-table-column prop="manufacturerName" label="厂家" width="120" />
        <el-table-column prop="quantity" label="进货数量" width="100" />
        <el-table-column prop="unitPrice" label="单价" width="100">
          <template #default="{ row }">
            ¥ {{ Number(row.unitPrice || 0).toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="总金额" width="120">
          <template #default="{ row }">
            <span style="color: #f56c6c; font-weight: 500;">¥ {{ Number(row.totalAmount || 0).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'warning' : row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 0 ? '待入库' : row.status === 1 ? '已入库' : '已取消' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" text type="success" size="small" @click="handleConfirm(row)">
              确认入库
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 添加进货记录对话框 -->
    <el-dialog v-model="dialogVisible" title="添加进货记录" width="500px" destroy-on-close>
      <el-form :model="purchaseForm" :rules="purchaseRules" ref="purchaseFormRef" label-width="80px">
        <el-form-item label="商品" prop="productCode">
          <el-select v-model="purchaseForm.productCode" placeholder="请选择商品" style="width: 100%;" filterable>
            <el-option v-for="p in productOptions" :key="p.productCode"
              :label="`${p.productName} (库存:${p.stockQuantity})`" :value="p.productCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="厂家" prop="manufacturerCode">
          <el-select v-model="purchaseForm.manufacturerCode" placeholder="请选择厂家" style="width: 100%;">
            <el-option v-for="m in manufacturerOptions" :key="m.manufacturerCode"
              :label="m.manufacturerName" :value="m.manufacturerCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="数量" prop="quantity">
          <el-input-number v-model="purchaseForm.quantity" :min="1" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="单价" prop="unitPrice">
          <el-input-number v-model="purchaseForm.unitPrice" :min="0" :precision="2" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="总金额">
          <span style="color: #f56c6c; font-weight: 500;">¥ {{ (purchaseForm.quantity * (purchaseForm.unitPrice || 0)).toFixed(2) }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { getPurchaseList, addPurchase, confirmPurchase } from '@/api/purchase'
import { getProductList } from '@/api/product'
import { getManufacturerList } from '@/api/manufacturer'

const records = ref([])
const productOptions = ref([])
const manufacturerOptions = ref([])
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const purchaseFormRef = ref(null)

const purchaseForm = reactive({
  productCode: '',
  manufacturerCode: '',
  quantity: 10,
  unitPrice: 0
})

const purchaseRules = {
  productCode: [{ required: true, message: '请选择商品', trigger: 'change' }],
  manufacturerCode: [{ required: true, message: '请选择厂家', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入数量', trigger: 'blur' }],
  unitPrice: [{ required: true, message: '请输入单价', trigger: 'blur' }]
}

const fetchRecords = async () => {
  loading.value = true
  try {
    const res = await getPurchaseList()
    records.value = res || []
  } catch (e) {
    console.error('获取进货记录失败:', e)
  } finally {
    loading.value = false
  }
}

const fetchOptions = async () => {
  try {
    const [products, manufacturers] = await Promise.all([
      getProductList({ pageNum: 1, pageSize: 100 }),
      getManufacturerList()
    ])
    productOptions.value = products.rows || []
    manufacturerOptions.value = manufacturers || []
  } catch (e) {
    console.error('获取选项失败:', e)
  }
}

onMounted(() => {
  fetchRecords()
  fetchOptions()
})

const handleAdd = () => {
  resetForm()
  dialogVisible.value = true
}

const resetForm = () => {
  purchaseForm.productCode = ''
  purchaseForm.manufacturerCode = ''
  purchaseForm.quantity = 10
  purchaseForm.unitPrice = 0
}

const handleSubmit = async () => {
  if (!purchaseFormRef.value) return
  try {
    await purchaseFormRef.value.validate()
    submitting.value = true
    await addPurchase({
      productCode: purchaseForm.productCode,
      manufacturerCode: purchaseForm.manufacturerCode,
      quantity: purchaseForm.quantity,
      unitPrice: purchaseForm.unitPrice,
      totalAmount: purchaseForm.quantity * purchaseForm.unitPrice
    })
    ElMessage.success('进货记录添加成功')
    dialogVisible.value = false
    fetchRecords()
  } catch (e) {
    if (e !== 'cancel') console.error('添加进货记录失败:', e)
  } finally {
    submitting.value = false
  }
}

const handleConfirm = (row) => {
  ElMessageBox.confirm(`确认将 ${row.quantity} 件"${row.productName}"入库？`, '确认入库', {
    confirmButtonText: '确定入库',
    cancelButtonText: '取消',
    type: 'info'
  }).then(async () => {
    await confirmPurchase(row.id)
    ElMessage.success('入库成功，库存已更新')
    fetchRecords()
  }).catch(() => {})
}
</script>

<style scoped>
.admin-purchase {
  padding: 0;
}

.toolbar-card {
  margin-bottom: 16px;
}
</style>
