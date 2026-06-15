<template>
  <div class="products-container">
    <el-card shadow="never" class="filter-card">
      <el-row :gutter="16" align="middle">
        <el-col :xs="24" :sm="12" :md="8">
          <el-input v-model="searchKeyword" placeholder="搜索商品名称..." :prefix-icon="Search" clearable @clear="handleSearch" @keyup.enter="handleSearch" />
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-select v-model="selectedCategory" placeholder="选择分类" clearable @change="handleSearch">
            <el-option label="全部分类" value="" />
            <el-option v-for="m in manufacturers" :key="m.manufacturerCode" :label="m.manufacturerName" :value="m.manufacturerCode" />
          </el-select>
        </el-col>
        <el-col :xs="24" :sm="12" :md="4"><el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon>搜索</el-button></el-col>
      </el-row>
    </el-card>
    <el-row :gutter="20" class="product-list">
      <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="item in products" :key="item.productCode">
        <el-card class="product-card" shadow="hover" @click="handleViewDetail(item)">
          <div class="product-image"><el-icon :size="60" color="#909399"><ShoppingBag /></el-icon></div>
          <div class="product-info">
            <h3 class="product-name">{{ item.productName }}</h3>
            <el-tag size="small" type="info">{{ item.manufacturerName || '未知厂家' }}</el-tag>
            <p class="product-desc">{{ item.productDesc }}</p>
            <div class="product-bottom">
              <span class="price">¥ {{ Number(item.unitPrice || 0).toFixed(2) }}</span>
              <el-button type="primary" size="small" @click.stop="handleViewDetail(item)">详情</el-button>
              <el-button type="warning" plain size="small" @click.stop="handleAddToCart(item)" :disabled="item.stockQuantity <= 0">
                <el-icon><ShoppingCart /></el-icon>{{ item.stockQuantity <= 0 ? '已售罄' : '加入购物车' }}
              </el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <el-empty v-if="!loading && products.length === 0" description="暂无商品" />
    <div class="pagination-wrapper" v-if="total > 0" style="display:flex;justify-content:center;margin-top:20px;">
      <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="fetchProducts" />
    </div>
    <el-dialog v-model="detailVisible" :title="detailProduct?.productName" width="500px" destroy-on-close>
      <div v-if="detailProduct" class="product-detail">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="商品编码">{{ detailProduct.productCode }}</el-descriptions-item>
          <el-descriptions-item label="商品名称">{{ detailProduct.productName }}</el-descriptions-item>
          <el-descriptions-item label="厂家">{{ detailProduct.manufacturerName || '未知' }}</el-descriptions-item>
          <el-descriptions-item label="单价">¥ {{ Number(detailProduct.unitPrice || 0).toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="库存">{{ detailProduct.stockQuantity }}{{ detailProduct.stockQuantity <= detailProduct.minStock ? ' (库存不足)' : '' }}</el-descriptions-item>
          <el-descriptions-item label="重量">{{ detailProduct.weight || 0 }} kg</el-descriptions-item>
          <el-descriptions-item label="描述">{{ detailProduct.productDesc || '暂无描述' }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="warning" @click="detailVisible = false; handleAddToCart(detailProduct)" :disabled="detailProduct?.stockQuantity <= 0">
          <el-icon><ShoppingCart /></el-icon>加入购物车
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useStore } from 'vuex'
import { ElMessage } from 'element-plus'
import { Search, ShoppingBag, ShoppingCart } from '@element-plus/icons-vue'
import { getProductList } from '@/api/product'
import { getManufacturerList } from '@/api/manufacturer'

const store = useStore()
const searchKeyword = ref('')
const selectedCategory = ref('')
const products = ref([])
const manufacturers = ref([])
const total = ref(0)
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(12)

const fetchProducts = async () => {
  loading.value = true
  try {
    const res = await getProductList({ productName: searchKeyword.value || undefined, manufacturerCode: selectedCategory.value || undefined, status: 1, pageNum: pageNum.value, pageSize: pageSize.value })
    products.value = res.rows || []
    total.value = res.total || 0
  } catch (e) { console.error('获取商品失败:', e) } finally { loading.value = false }
}

const fetchManufacturers = async () => {
  try { const res = await getManufacturerList(); manufacturers.value = res || [] } catch (e) { console.error('获取厂家列表失败:', e) }
}

onMounted(() => { fetchProducts(); fetchManufacturers() })

const detailVisible = ref(false)
const detailProduct = ref(null)

const handleSearch = () => { pageNum.value = 1; fetchProducts() }
const handleViewDetail = (item) => { detailProduct.value = item; detailVisible.value = true }
const handleAddToCart = (item) => {
  if (item.stockQuantity <= 0) {
    ElMessage.warning('该商品已售罄，无法加入购物车')
    return
  }
  store.dispatch('addToCart', { productCode: item.productCode, productName: item.productName, unitPrice: item.unitPrice, stockQuantity: item.stockQuantity, quantity: 1 })
  ElMessage.success(`已将 ${item.productName} 加入购物车！`)
}
</script>

<style scoped>
.products-container { padding: 10px 0; }
.filter-card { margin-bottom: 20px; }
.product-list { margin-top: 10px; }
.product-card { margin-bottom: 20px; transition: transform 0.3s; }
.product-card:hover { transform: translateY(-4px); }
.product-image { height: 160px; display: flex; align-items: center; justify-content: center; background-color: #f5f7fa; border-radius: 4px; margin-bottom: 15px; }
.product-info { text-align: left; }
.product-name { font-size: 16px; margin-bottom: 8px; color: #303133; }
.product-desc { font-size: 13px; color: #909399; margin: 8px 0; line-height: 1.4; }
.product-bottom { display: flex; align-items: center; justify-content: space-between; margin-top: 10px; }
.price { color: #f56c6c; font-size: 18px; font-weight: bold; }
</style>
