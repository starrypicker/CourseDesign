
<template>
  <div class="products-container">
    <!-- 搜索和筛选栏 -->
    <el-card shadow="never" class="filter-card">
      <el-row :gutter="16" align="middle">
        <el-col :xs="24" :sm="12" :md="8">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索商品名称..."
            prefix-icon="Search"
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          />
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-select v-model="selectedCategory" placeholder="选择分类" clearable @change="handleSearch">
            <el-option label="全部分类" value="" />
            <el-option v-for="cat in categories" :key="cat.value" :label="cat.label" :value="cat.value" />
          </el-select>
        </el-col>
        <el-col :xs="24" :sm="12" :md="4">
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 商品列表 -->
    <el-row :gutter="20" class="product-list">
      <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="item in filteredProducts" :key="item.productCode">
        <el-card class="product-card" shadow="hover">
          <div class="product-image">
            <el-icon :size="60" color="#909399"><ShoppingBag /></el-icon>
          </div>
          <div class="product-info">
            <h3 class="product-name">{{ item.productName }}</h3>
            <el-tag size="small" type="info">{{ item.manufacturerName || '未知厂家' }}</el-tag>
            <p class="product-desc">{{ item.productDesc }}</p>
            <div class="product-bottom">
              <span class="price">¥ {{ Number(item.unitPrice).toFixed(2) }}</span>
              <el-button type="warning" plain size="small" @click="handleAddToCart(item)" :disabled="item.stockQuantity <= 0">
                <el-icon><ShoppingCart /></el-icon>
                {{ item.stockQuantity <= 0 ? '已售罄' : '加入购物车' }}
              </el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 空状态 -->
    <el-empty v-if="!loading && filteredProducts.length === 0" description="暂无商品" />
    <!-- 分页 -->
    <div class="pagination-wrapper" v-if="total > 0" style="display:flex;justify-content:center;margin-top:20px;">
      <el-pagination
        v-model:current-page="pageNum"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchProducts"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useStore } from 'vuex'
import { ElMessage } from 'element-plus'
import { Search, ShoppingBag, ShoppingCart } from '@element-plus/icons-vue'
import { getProductList } from '@/api/product'
import { getManufacturerList } from '@/api/manufacturer'

const store = useStore()

const searchKeyword = ref('')
const selectedCategory = ref('')  // 对应后端的厂家(manufacturerCode)
const products = ref([])
const manufacturers = ref([])
const total = ref(0)
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(12)

// 加载商品数据
const fetchProducts = async () => {
  loading.value = true
  try {
    const res = await getProductList({
      productName: searchKeyword.value || undefined,
      manufacturerCode: selectedCategory.value || undefined,
      status: 1,  // 只查上架商品
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    // res 是 PageResult: { total, rows, pageNum, pageSize }
    products.value = res.rows || []
    total.value = res.total || 0
  } catch (e) {
    console.error('获取商品失败:', e)
  } finally {
    loading.value = false
  }
}

// 加载厂家列表（用作分类筛选）
const fetchManufacturers = async () => {
  try {
    const res = await getManufacturerList()
    manufacturers.value = res || []
  } catch (e) {
    console.error('获取厂家列表失败:', e)
  }
}

onMounted(() => {
  fetchProducts()
  fetchManufacturers()
})

// 分类选项：从厂家列表生成
const categories = computed(() => {
  return manufacturers.value.map(m => ({
    label: m.manufacturerName,
    value: m.manufacturerCode
  }))
})

const filteredProducts = computed(() => products.value)

const handleSearch = () => {
  pageNum.value = 1
  fetchProducts()
}

const handleAddToCart = (item) => {
  // 字段映射：后端 productCode/productName/unitPrice
  store.dispatch('addToCart', {
    productCode: item.productCode,
    productName: item.productName,
    unitPrice: item.unitPrice,
    quantity: 1
  })
  ElMessage.success(`已将 ${item.productName} 加入购物车！`)
}
</script>

<style scoped>
.products-container {
  padding: 10px 0;
}

.filter-card {
  margin-bottom: 20px;
}

.product-list {
  margin-top: 10px;
}

.product-card {
  margin-bottom: 20px;
  transition: transform 0.3s;
}

.product-card:hover {
  transform: translateY(-4px);
}

.product-image {
  height: 160px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f5f7fa;
  border-radius: 4px;
  margin-bottom: 15px;
}

.product-info {
  text-align: left;
}

.product-name {
  font-size: 16px;
  margin-bottom: 8px;
  color: #303133;
}

.product-desc {
  font-size: 13px;
  color: #909399;
  margin: 8px 0;
  line-height: 1.4;
}

.product-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
}

.price {
  color: #f56c6c;
  font-size: 18px;
  font-weight: bold;
}
</style>
