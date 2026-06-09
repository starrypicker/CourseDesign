<template>
  <div class="admin-products">
    <el-card shadow="never" class="toolbar-card">
      <el-row :gutter="16" align="middle">
        <el-col :xs="24" :sm="8" :md="6"><el-input v-model="searchKeyword" placeholder="搜索商品名称..." clearable @keyup.enter="handleSearch" /></el-col>
        <el-col :xs="24" :sm="6" :md="4"><el-select v-model="selectedCategory" placeholder="厂家筛选" clearable @change="handleSearch"><el-option label="全部厂家" value="" /><el-option v-for="m in manufacturerOptions" :key="m.manufacturerCode" :label="m.manufacturerName" :value="m.manufacturerCode" /></el-select></el-col>
        <el-col :xs="24" :sm="4" :md="3"><el-button type="primary" @click="handleSearch">搜索</el-button><el-button @click="handleReset">重置</el-button></el-col>
        <el-col :xs="24" :sm="6" :md="11" style="text-align:right;"><el-button type="primary" @click="handleAdd"><el-icon><Plus /></el-icon>添加商品</el-button></el-col>
      </el-row>
    </el-card>
    <el-card shadow="never">
      <el-table :data="filteredProducts" stripe style="width:100%" v-loading="loading">
        <el-table-column prop="productCode" label="编码" width="80" />
        <el-table-column label="商品信息" min-width="200"><template #default="{ row }"><div style="display:flex;align-items:center;gap:10px;"><div style="width:40px;height:40px;background:#f5f7fa;border-radius:4px;display:flex;align-items:center;justify-content:center;"><el-icon :size="20" color="#909399"><ShoppingBag /></el-icon></div><span>{{ row.productName }}</span></div></template></el-table-column>
        <el-table-column prop="manufacturerName" label="厂家" width="100"><template #default="{ row }"><el-tag size="small">{{ row.manufacturerName || '-' }}</el-tag></template></el-table-column>
        <el-table-column prop="unitPrice" label="价格" width="100"><template #default="{ row }"><span style="color:#f56c6c;font-weight:500;">¥ {{ Number(row.unitPrice || 0).toFixed(2) }}</span></template></el-table-column>
        <el-table-column prop="stockQuantity" label="库存" width="80"><template #default="{ row }"><span :style="{ color: row.stockQuantity <= row.minStock ? '#f56c6c' : '' }">{{ row.stockQuantity }}</span></template></el-table-column>
        <el-table-column prop="status" label="状态" width="90"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '上架' : '下架' }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button text :type="row.status === 1 ? 'warning' : 'success'" size="small" @click="handleToggleStatus(row)">{{ row.status === 1 ? '下架' : '上架' }}</el-button>
            <el-button text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="display:flex;justify-content:center;margin-top:20px;" v-if="total > 0"><el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="total,prev,pager,next" @current-change="fetchProducts" /></div>
    </el-card>
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" destroy-on-close>
      <el-form :model="productForm" :rules="productRules" ref="productFormRef" label-width="80px">
        <el-form-item label="编码" prop="productCode" v-if="!editingId"><el-input v-model="productForm.productCode" placeholder="请输入商品编码" /></el-form-item>
        <el-form-item label="名称" prop="productName"><el-input v-model="productForm.productName" placeholder="请输入商品名称" /></el-form-item>
        <el-form-item label="厂家" prop="manufacturerCode"><el-select v-model="productForm.manufacturerCode" placeholder="请选择厂家" style="width:100%;"><el-option v-for="m in manufacturerOptions" :key="m.manufacturerCode" :label="m.manufacturerName" :value="m.manufacturerCode" /></el-select></el-form-item>
        <el-form-item label="价格" prop="unitPrice"><el-input-number v-model="productForm.unitPrice" :min="0" :precision="2" style="width:100%;" /></el-form-item>
        <el-form-item label="库存" prop="stockQuantity"><el-input-number v-model="productForm.stockQuantity" :min="0" style="width:100%;" /></el-form-item>
        <el-form-item label="最低库存" prop="minStock"><el-input-number v-model="productForm.minStock" :min="0" style="width:100%;" /></el-form-item>
        <el-form-item label="重量(kg)" prop="weight"><el-input-number v-model="productForm.weight" :min="0" :precision="2" style="width:100%;" /></el-form-item>
        <el-form-item label="描述" prop="productDesc"><el-input v-model="productForm.productDesc" type="textarea" :rows="3" placeholder="请输入商品描述" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="handleSubmit">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, ShoppingBag } from '@element-plus/icons-vue'
import { getProductList, addProduct, updateProduct, deleteProduct } from '@/api/product'
import { getManufacturerList } from '@/api/manufacturer'

const searchKeyword = ref('')
const selectedCategory = ref('')
const dialogVisible = ref(false)
const dialogTitle = ref('添加商品')
const productFormRef = ref(null)
const editingId = ref(null)
const products = ref([])
const manufacturerOptions = ref([])
const total = ref(0)
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)

const fetchProducts = async () => {
  loading.value = true
  try {
    const res = await getProductList({ productName: searchKeyword.value || undefined, manufacturerCode: selectedCategory.value || undefined, pageNum: pageNum.value, pageSize: pageSize.value })
    products.value = res.rows || []; total.value = res.total || 0
  } catch (e) { console.error('获取商品失败:', e) } finally { loading.value = false }
}

const fetchManufacturers = async () => {
  try { const res = await getManufacturerList(); manufacturerOptions.value = res || [] } catch (e) { console.error('获取厂家列表失败:', e) }
}

onMounted(() => { fetchProducts(); fetchManufacturers() })

const handleSearch = () => { pageNum.value = 1; fetchProducts() }
const handleReset = () => { searchKeyword.value = ''; selectedCategory.value = ''; pageNum.value = 1; fetchProducts() }
const filteredProducts = computed(() => products.value)

const productForm = reactive({ productCode: '', productName: '', manufacturerCode: '', unitPrice: 0, stockQuantity: 0, minStock: 10, weight: 0, productDesc: '' })
const productRules = {
  productCode: [{ required: true, message: '请输入商品编码', trigger: 'blur' }],
  productName: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  manufacturerCode: [{ required: true, message: '请选择厂家', trigger: 'change' }],
  unitPrice: [{ required: true, message: '请输入价格', trigger: 'blur' }],
  stockQuantity: [{ required: true, message: '请输入库存', trigger: 'blur' }]
}

const resetForm = () => { productForm.productCode = ''; productForm.productName = ''; productForm.manufacturerCode = ''; productForm.unitPrice = 0; productForm.stockQuantity = 0; productForm.minStock = 10; productForm.weight = 0; productForm.productDesc = '' }

const handleAdd = () => { editingId.value = null; dialogTitle.value = '添加商品'; resetForm(); dialogVisible.value = true }

const handleEdit = (row) => {
  editingId.value = row.productCode; dialogTitle.value = '编辑商品'
  productForm.productCode = row.productCode; productForm.productName = row.productName; productForm.manufacturerCode = row.manufacturerCode
  productForm.unitPrice = row.unitPrice; productForm.stockQuantity = row.stockQuantity; productForm.minStock = row.minStock
  productForm.weight = row.weight; productForm.productDesc = row.productDesc
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!productFormRef.value) return
  try {
    await productFormRef.value.validate()
    const data = { productCode: productForm.productCode, productName: productForm.productName, manufacturerCode: productForm.manufacturerCode, unitPrice: productForm.unitPrice, stockQuantity: productForm.stockQuantity, minStock: productForm.minStock, weight: productForm.weight, productDesc: productForm.productDesc }
    if (editingId.value) { await updateProduct(data); ElMessage.success('商品修改成功') }
    else { await addProduct(data); ElMessage.success('商品添加成功') }
    dialogVisible.value = false; fetchProducts()
  } catch (e) {}
}

const handleToggleStatus = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  try {
    await updateProduct({ productCode: row.productCode, productName: row.productName, manufacturerCode: row.manufacturerCode, unitPrice: row.unitPrice, stockQuantity: row.stockQuantity, minStock: row.minStock, weight: row.weight, productDesc: row.productDesc, status: newStatus })
    ElMessage.success(`商品已${newStatus === 1 ? '上架' : '下架'}`); fetchProducts()
  } catch (e) { console.error('状态切换失败:', e) }
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除商品"${row.productName}"吗？`, '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    .then(async () => { await deleteProduct(row.productCode); ElMessage.success('删除成功'); fetchProducts() }).catch(() => {})
}
</script>

<style scoped>
.admin-products { padding: 0; }
.toolbar-card { margin-bottom: 16px; }
</style>
