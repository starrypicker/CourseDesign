<template>
  <div class="admin-products">
    <el-card shadow="never" class="toolbar-card">
      <el-row :gutter="16" align="middle">
        <el-col :xs="24" :sm="8" :md="6"><el-input v-model="searchKeyword" placeholder="搜索商品名称..." clearable @keyup.enter="handleSearch" /></el-col>
        <el-col :xs="24" :sm="6" :md="4"><el-select v-model="selectedCategory" placeholder="厂家筛选" clearable @change="handleSearch"><el-option label="全部厂家" value="" /><el-option v-for="m in manufacturerOptions" :key="m.manufacturerCode" :label="m.manufacturerName" :value="m.manufacturerCode" /></el-select></el-col>
        <el-col :xs="24" :sm="4" :md="3"><el-button type="primary" @click="handleSearch">搜索</el-button><el-button @click="handleReset">重置</el-button></el-col>
        <el-col :xs="24" :sm="6" :md="11" style="text-align:right;">
          <el-button type="success" plain @click="handleBatchImport"><el-icon><Upload /></el-icon>一键导入图片</el-button>
          <el-button type="primary" @click="handleAdd"><el-icon><Plus /></el-icon>添加商品</el-button>
        </el-col>
      </el-row>
    </el-card>
    <el-card shadow="never">
      <el-table :data="products" stripe style="width:100%" v-loading="loading">
        <el-table-column prop="productCode" label="编码" width="80" />
        <el-table-column label="商品信息" min-width="200"><template #default="{ row }"><div style="display:flex;align-items:center;gap:10px;cursor:pointer;" @click="handleViewDetail(row)"><div style="width:40px;height:40px;background:#f5f7fa;border-radius:4px;display:flex;align-items:center;justify-content:center;overflow:hidden;"><img v-if="getProductImage(row)" :src="getProductImage(row)" :alt="row.productName" style="width:100%;height:100%;object-fit:cover;" /><el-icon v-else :size="20" color="#909399"><ShoppingBag /></el-icon></div><span style="color:#409EFF;">{{ row.productName }}</span></div></template></el-table-column>
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
    <!-- 商品详情弹窗 -->
    <ProductDetailDialog v-model:visible="detailVisible" :product="detailProduct" :show-add-cart="false" />

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
        <el-form-item label="商品图片">
          <div class="image-upload-area">
            <img v-if="productForm.image" :src="productForm.image" class="image-preview" />
            <el-upload
              :show-file-list="false"
              :before-upload="handleBeforeUpload"
              :http-request="handleImageUpload"
              accept="image/*"
            >
              <el-button type="primary" plain size="small">
                <el-icon><Upload /></el-icon>{{ editingId ? '更换图片' : '上传图片' }}
              </el-button>
            </el-upload>
            <el-button v-if="productForm.image" type="danger" plain size="small" @click="productForm.image = ''">清除图片</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="handleSubmit">确定</el-button></template>
    </el-dialog>

    <!-- 批量导入图片弹窗 -->
    <el-dialog v-model="batchVisible" title="一键导入商品图片" width="550px" destroy-on-close>
      <div class="batch-import-info">
        <el-alert type="info" :closable="false" show-icon>
          <template #title>
            选择商品图片文件，文件名需与商品编码一致<br/>
            例如：<strong>P001.jpg</strong> 对应商品编码 <strong>P001</strong>
          </template>
        </el-alert>
      </div>
      <el-upload
        v-model:file-list="batchFiles"
        :auto-upload="false"
        multiple
        accept="image/*"
        drag
        style="margin-top:16px;"
      >
        <el-icon :size="48" color="#409EFF"><UploadFilled /></el-icon>
        <div class="el-upload__text">将图片文件拖到此处，或<em>点击选择</em></div>
        <template #tip>
          <div class="el-upload__tip">支持 jpg/png/gif/webp 格式，每张不超过5MB</div>
        </template>
      </el-upload>
      <template #footer>
        <el-button @click="batchVisible = false">取消</el-button>
        <el-button type="primary" @click="handleBatchUpload" :loading="batchUploading">
          <el-icon><Upload /></el-icon>开始导入
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, ShoppingBag, Upload, UploadFilled } from '@element-plus/icons-vue'
import { getProductList, addProduct, updateProduct, deleteProduct, uploadProductImage, batchUploadProductImages } from '@/api/product'
import { getManufacturerList } from '@/api/manufacturer'
import ProductDetailDialog from '@/components/ProductDetailDialog.vue'
import { getProductImage } from '@/utils/constants'

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

const detailVisible = ref(false)
const detailProduct = ref(null)
const handleViewDetail = (row) => { detailProduct.value = row; detailVisible.value = true }

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

const productForm = reactive({ productCode: '', productName: '', manufacturerCode: '', unitPrice: 0, stockQuantity: 0, minStock: 10, weight: 0, productDesc: '', image: '' })
const productRules = {
  productCode: [{ required: true, message: '请输入商品编码', trigger: 'blur' }],
  productName: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  manufacturerCode: [{ required: true, message: '请选择厂家', trigger: 'change' }],
  unitPrice: [{ required: true, message: '请输入价格', trigger: 'blur' }],
  stockQuantity: [{ required: true, message: '请输入库存', trigger: 'blur' }]
}

const resetForm = () => { productForm.productCode = ''; productForm.productName = ''; productForm.manufacturerCode = ''; productForm.unitPrice = 0; productForm.stockQuantity = 0; productForm.minStock = 10; productForm.weight = 0; productForm.productDesc = ''; productForm.image = '' }

const handleAdd = () => { editingId.value = null; dialogTitle.value = '添加商品'; resetForm(); dialogVisible.value = true }

const handleEdit = (row) => {
  editingId.value = row.productCode; dialogTitle.value = '编辑商品'
  productForm.productCode = row.productCode; productForm.productName = row.productName; productForm.manufacturerCode = row.manufacturerCode
  productForm.unitPrice = row.unitPrice; productForm.stockQuantity = row.stockQuantity; productForm.minStock = row.minStock
  productForm.weight = row.weight; productForm.productDesc = row.productDesc; productForm.image = row.image || ''
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!productFormRef.value) return
  try {
    await productFormRef.value.validate()
    const data = { productCode: productForm.productCode, productName: productForm.productName, manufacturerCode: productForm.manufacturerCode, unitPrice: productForm.unitPrice, stockQuantity: productForm.stockQuantity, minStock: productForm.minStock, weight: productForm.weight, productDesc: productForm.productDesc, image: productForm.image || undefined }
    if (editingId.value) { await updateProduct(data); ElMessage.success('商品修改成功') }
    else { await addProduct(data); ElMessage.success('商品添加成功') }
    dialogVisible.value = false; fetchProducts()
  } catch (e) {}
}

// 图片上传
const handleBeforeUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  if (!isImage) { ElMessage.error('只能上传图片文件'); return false }
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isLt5M) { ElMessage.error('图片大小不能超过5MB'); return false }
  return true
}

const handleImageUpload = async ({ file }) => {
  try {
    const res = await uploadProductImage(productForm.productCode || editingId.value, file)
    if (res && res.url) {
      productForm.image = res.url
      ElMessage.success('图片上传成功')
    }
  } catch (e) { console.error('图片上传失败:', e) }
}

// 批量导入图片
const batchVisible = ref(false)
const batchFiles = ref([])
const batchUploading = ref(false)

const handleBatchImport = () => {
  batchFiles.value = []
  batchVisible.value = true
}

const handleBatchUpload = async () => {
  if (batchFiles.value.length === 0) {
    ElMessage.warning('请先选择图片文件')
    return
  }
  batchUploading.value = true
  try {
    const rawFiles = batchFiles.value.map(f => f.raw).filter(Boolean)
    if (rawFiles.length === 0) { ElMessage.warning('没有可上传的文件'); return }
    const res = await batchUploadProductImages(rawFiles)
    if (res.fail > 0 && res.errors && res.errors.length > 0) {
      // 有失败的文件，显示详情
      const errorList = res.errors.slice(0, 5).join('<br/>')
      const more = res.errors.length > 5 ? `<br/>...还有 ${res.errors.length - 5} 个错误` : ''
      ElMessage({
        type: 'warning',
        dangerouslyUseHTMLString: true,
        message: `成功 ${res.success || 0} 张，失败 ${res.fail} 张<br/><br/>${errorList}${more}`,
        duration: 8000
      })
    } else {
      ElMessage.success(`全部导入成功！共 ${res.success || 0} 张`)
    }
    batchVisible.value = false
    fetchProducts()
  } catch (e) { console.error('批量导入失败:', e) }
  finally { batchUploading.value = false }
}

const handleToggleStatus = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  try {
    // 先获取最新商品数据，避免并发修改覆盖
    const { getProductByCode } = await import('@/api/product')
    const latest = await getProductByCode(row.productCode)
    if (!latest) { ElMessage.error('商品不存在'); return }
    await updateProduct({
      productCode: latest.productCode,
      productName: latest.productName,
      manufacturerCode: latest.manufacturerCode,
      unitPrice: latest.unitPrice,
      stockQuantity: latest.stockQuantity,
      minStock: latest.minStock,
      weight: latest.weight,
      productDesc: latest.productDesc,
      status: newStatus
    })
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
.image-upload-area { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.image-preview { width: 80px; height: 80px; object-fit: cover; border-radius: 6px; border: 1px solid #dcdfe6; }
.batch-import-info { margin-bottom: 8px; }
</style>
