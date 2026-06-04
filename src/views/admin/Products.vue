
<template>
  <div class="admin-products">
    <!-- 操作栏 -->
    <el-card shadow="never" class="toolbar-card">
      <el-row :gutter="16" align="middle">
        <el-col :xs="24" :sm="8" :md="6">
          <el-input v-model="searchKeyword" placeholder="搜索商品名称..." prefix-icon="Search" clearable />
        </el-col>
        <el-col :xs="24" :sm="6" :md="4">
          <el-select v-model="selectedCategory" placeholder="分类筛选" clearable>
            <el-option label="全部分类" value="" />
            <el-option v-for="cat in categories" :key="cat" :label="cat" :value="cat" />
          </el-select>
        </el-col>
        <el-col :xs="24" :sm="10" :md="14" style="text-align: right;">
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon> 添加商品
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 商品表格 -->
    <el-card shadow="never">
      <el-table :data="filteredProducts" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column label="商品信息" min-width="200">
          <template #default="{ row }">
            <div style="display: flex; align-items: center; gap: 10px;">
              <div style="width: 40px; height: 40px; background: #f5f7fa; border-radius: 4px; display: flex; align-items: center; justify-content: center;">
                <el-icon :size="20" color="#909399"><ShoppingBag /></el-icon>
              </div>
              <span>{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="price" label="价格" width="100">
          <template #default="{ row }">
            <span style="color: #f56c6c; font-weight: 500;">¥ {{ row.price.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="80" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'on' ? 'success' : 'info'" size="small">
              {{ row.status === 'on' ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button text :type="row.status === 'on' ? 'warning' : 'success'" size="small" @click="handleToggleStatus(row)">
              {{ row.status === 'on' ? '下架' : '上架' }}
            </el-button>
            <el-button text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 添加/编辑商品对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" destroy-on-close>
      <el-form :model="productForm" :rules="productRules" ref="productFormRef" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="productForm.name" placeholder="请输入商品名称" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="productForm.category" placeholder="请选择分类" style="width: 100%;">
            <el-option v-for="cat in categories" :key="cat" :label="cat" :value="cat" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="productForm.price" :min="0" :precision="2" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="库存" prop="stock">
          <el-input-number v-model="productForm.stock" :min="0" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="productForm.description" type="textarea" :rows="3" placeholder="请输入商品描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, ShoppingBag } from '@element-plus/icons-vue'

const searchKeyword = ref('')
const selectedCategory = ref('')
const dialogVisible = ref(false)
const dialogTitle = ref('添加商品')
const productFormRef = ref(null)
const editingId = ref(null)

const categories = ['鞋类', '服装', '球类', '健身', '户外', '游泳']

const products = ref([
  { id: 1, name: '专业跑步鞋', price: 599, category: '鞋类', description: '轻量缓震，适合长跑训练', stock: 50, status: 'on' },
  { id: 2, name: '透气运动T恤', price: 129, category: '服装', description: '速干透气面料，运动必备', stock: 120, status: 'on' },
  { id: 3, name: '碳纤维羽毛球拍', price: 880, category: '球类', description: '超轻碳纤维材质，进攻利器', stock: 30, status: 'on' },
  { id: 4, name: '加厚瑜伽垫', price: 89, category: '健身', description: '环保TPE材质，防滑加厚', stock: 200, status: 'on' },
  { id: 5, name: '户外登山包 50L', price: 369, category: '户外', description: '大容量防水，登山徒步首选', stock: 45, status: 'on' },
  { id: 6, name: '竞速泳镜', price: 158, category: '游泳', description: '防雾防紫外线，专业竞速', stock: 80, status: 'off' },
  { id: 7, name: '篮球鞋', price: 499, category: '鞋类', description: '高帮护踝，室内外通用', stock: 60, status: 'on' },
  { id: 8, name: '运动短裤', price: 79, category: '服装', description: '宽松舒适，速干透气', stock: 150, status: 'on' }
])

const filteredProducts = computed(() => {
  let result = products.value
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    result = result.filter(p => p.name.toLowerCase().includes(kw))
  }
  if (selectedCategory.value) {
    result = result.filter(p => p.category === selectedCategory.value)
  }
  return result
})

const productForm = reactive({
  name: '',
  category: '',
  price: 0,
  stock: 0,
  description: ''
})

const productRules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入库存', trigger: 'blur' }]
}

const resetForm = () => {
  productForm.name = ''
  productForm.category = ''
  productForm.price = 0
  productForm.stock = 0
  productForm.description = ''
}

const handleAdd = () => {
  editingId.value = null
  dialogTitle.value = '添加商品'
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  editingId.value = row.id
  dialogTitle.value = '编辑商品'
  productForm.name = row.name
  productForm.category = row.category
  productForm.price = row.price
  productForm.stock = row.stock
  productForm.description = row.description
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!productFormRef.value) return
  try {
    await productFormRef.value.validate()
    if (editingId.value) {
      const product = products.value.find(p => p.id === editingId.value)
      if (product) {
        Object.assign(product, {
          name: productForm.name,
          category: productForm.category,
          price: productForm.price,
          stock: productForm.stock,
          description: productForm.description
        })
      }
      ElMessage.success('商品修改成功')
    } else {
      const newId = Math.max(...products.value.map(p => p.id)) + 1
      products.value.push({
        id: newId,
        name: productForm.name,
        category: productForm.category,
        price: productForm.price,
        stock: productForm.stock,
        description: productForm.description,
        status: 'on'
      })
      ElMessage.success('商品添加成功')
    }
    dialogVisible.value = false
  } catch (e) {}
}

const handleToggleStatus = (row) => {
  row.status = row.status === 'on' ? 'off' : 'on'
  ElMessage.success(`商品已${row.status === 'on' ? '上架' : '下架'}`)
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除商品"${row.name}"吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    products.value = products.value.filter(p => p.id !== row.id)
    ElMessage.success('删除成功')
  }).catch(() => {})
}
</script>

<style scoped>
.admin-products {
  padding: 0;
}

.toolbar-card {
  margin-bottom: 16px;
}
</style>
