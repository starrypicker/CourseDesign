<template>
  <div class="products-container">
    <!-- 筛选区域 -->
    <el-card shadow="never" class="filter-card">
      <el-row :gutter="12" align="middle">
        <el-col :xs="24" :sm="8" :md="7">
          <el-input v-model="searchKeyword" placeholder="搜索商品名称..." :prefix-icon="Search" clearable @clear="handleSearch" @keyup.enter="handleSearch" />
        </el-col>
        <el-col :xs="12" :sm="6" :md="5">
          <el-select v-model="selectedCategory" placeholder="选择分类" clearable @change="handleSearch" style="width:100%">
            <el-option label="全部分类" value="" />
            <el-option v-for="m in manufacturers" :key="m.manufacturerCode" :label="m.manufacturerName" :value="m.manufacturerCode" />
          </el-select>
        </el-col>
        <el-col :xs="12" :sm="10" :md="6">
          <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon>搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 骨架屏 -->
    <el-row :gutter="20" v-if="loading">
      <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="i in 8" :key="'s'+i">
        <div class="product-card skeleton-card">
          <div class="skeleton" style="height:160px;border-radius:4px 4px 0 0;"></div>
          <div style="padding:15px;">
            <div class="skeleton" style="height:18px;width:60%;margin-bottom:8px;"></div>
            <div class="skeleton" style="height:14px;width:40%;margin-bottom:10px;"></div>
            <div class="skeleton" style="height:22px;width:35%;margin-bottom:10px;"></div>
            <div class="skeleton" style="height:32px;"></div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 商品列表 -->
    <el-row :gutter="20" class="product-list" v-else>
      <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="item in products" :key="item.productCode">
        <el-card class="product-card card-hover" shadow="hover" @click="handleViewDetail(item)">
          <div class="product-image">
            <img v-if="getProductImage(item)" :src="getProductImage(item)" :alt="item.productName" class="product-img" />
            <el-icon v-else :size="56" color="#909399"><ShoppingBag /></el-icon>
            <el-tag v-if="item.stockQuantity <= 0" type="danger" size="small" class="sold-out-tag">售罄</el-tag>
            <el-tag v-else-if="item.stockQuantity <= item.minStock" type="warning" size="small" class="sold-out-tag">库存紧张</el-tag>
          </div>
          <div class="product-info">
            <h3 class="product-name">{{ item.productName }}</h3>
            <el-tag size="small" type="info">{{ item.manufacturerName || '未知厂家' }}</el-tag>
            <p class="product-desc">{{ item.productDesc }}</p>
            <div class="product-bottom">
              <span class="price">¥ {{ formatPrice(item.unitPrice) }}</span>
              <el-button size="small" @click.stop="handleViewDetail(item)">详情</el-button>
              <el-button type="warning" plain size="small" @click.stop="handleAddToCart(item)" :disabled="item.stockQuantity <= 0">
                <el-icon><ShoppingCart /></el-icon>{{ item.stockQuantity <= 0 ? '已售罄' : '加购' }}
              </el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="!loading && products.length === 0" description="暂无商品">
      <el-button type="primary" @click="handleReset">刷新列表</el-button>
    </el-empty>

    <div class="pagination-wrapper" v-if="total > 0">
      <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="fetchProducts" />
    </div>

    <!-- 商品详情弹窗 -->
    <ProductDetailDialog
      v-model:visible="detailVisible"
      :product="detailProduct"
      @add-to-cart="onDetailAddToCart"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useStore } from 'vuex'
import { ElMessage } from 'element-plus'
import { Search, ShoppingBag, ShoppingCart } from '@element-plus/icons-vue'
import { getProductList } from '@/api/product'
import { getManufacturerList } from '@/api/manufacturer'
import ProductDetailDialog from '@/components/ProductDetailDialog.vue'
import { getProductImage } from '@/utils/constants'

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
    const res = await getProductList({
      productName: searchKeyword.value || undefined,
      manufacturerCode: selectedCategory.value || undefined,
      status: 1, pageNum: pageNum.value, pageSize: pageSize.value
    })
    products.value = res.rows || []
    total.value = res.total || 0
  } catch (e) { console.error('获取商品失败:', e) } finally { loading.value = false }
}

const fetchManufacturers = async () => {
  try { const res = await getManufacturerList(); manufacturers.value = res || [] } catch (e) {}
}

onMounted(() => { fetchProducts(); fetchManufacturers() })

const formatPrice = (val) => Number(val || 0).toFixed(2)

const detailVisible = ref(false)
const detailProduct = ref(null)

const handleSearch = () => { pageNum.value = 1; fetchProducts() }
const handleReset = () => { searchKeyword.value = ''; selectedCategory.value = ''; pageNum.value = 1; fetchProducts() }

const handleViewDetail = (item) => { detailProduct.value = item; detailVisible.value = true }

const handleAddToCart = (item) => {
  if (item.stockQuantity <= 0) {
    ElMessage.warning('该商品已售罄，无法加入购物车')
    return
  }
  store.dispatch('addToCart', {
    productCode: item.productCode, productName: item.productName,
    unitPrice: item.unitPrice, stockQuantity: item.stockQuantity, quantity: 1
  })
  ElMessage.success(`已将 ${item.productName} 加入购物车！`)
}

const onDetailAddToCart = () => {}
</script>

<style scoped>
.products-container { padding: 0; }
.filter-card { margin-bottom: 20px; }
.product-list { margin-top: 10px; }
.product-card {
  margin-bottom: 20px; cursor: pointer;
  border-radius: 12px; overflow: hidden;
}
.product-image {
  height: 160px; display: flex; align-items: center; justify-content: center;
  background-color: #f5f7fa; border-radius: 4px; margin-bottom: 15px;
  position: relative; overflow: hidden;
}
.product-img { width: 100%; height: 100%; object-fit: cover; }
.sold-out-tag {
  position: absolute; top: 8px; right: 8px;
}
.product-info { text-align: left; }
.product-name {
  font-size: 16px; margin-bottom: 8px; color: #303133;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.product-desc {
  font-size: 13px; color: #909399; margin: 8px 0; line-height: 1.4;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
  overflow: hidden; min-height: 36px;
}
.product-bottom {
  display: flex; align-items: center; justify-content: space-between;
  margin-top: 10px; gap: 4px;
}
.price { color: #f56c6c; font-size: 18px; font-weight: bold; }
.pagination-wrapper { display: flex; justify-content: center; margin-top: 20px; }
.skeleton-card { height: 360px; }
</style>
