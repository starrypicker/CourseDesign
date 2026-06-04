
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
            <el-option v-for="cat in categories" :key="cat" :label="cat" :value="cat" />
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
      <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="item in filteredProducts" :key="item.id">
        <el-card class="product-card" shadow="hover">
          <div class="product-image">
            <el-icon :size="60" color="#909399"><ShoppingBag /></el-icon>
          </div>
          <div class="product-info">
            <h3 class="product-name">{{ item.name }}</h3>
            <el-tag size="small" type="info">{{ item.category }}</el-tag>
            <p class="product-desc">{{ item.description }}</p>
            <div class="product-bottom">
              <span class="price">¥ {{ item.price.toFixed(2) }}</span>
              <el-button type="warning" plain size="small" @click="handleAddToCart(item)">
                <el-icon><ShoppingCart /></el-icon>
                加入购物车
              </el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 空状态 -->
    <el-empty v-if="filteredProducts.length === 0" description="暂无商品" />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useStore } from 'vuex'
import { ElMessage } from 'element-plus'
import { Search, ShoppingBag, ShoppingCart } from '@element-plus/icons-vue'

const store = useStore()

const searchKeyword = ref('')
const selectedCategory = ref('')

// 模拟商品数据（后续替换为API请求）
const products = ref([
  { id: 1, name: '专业跑步鞋', price: 599, category: '鞋类', description: '轻量缓震，适合长跑训练', stock: 50 },
  { id: 2, name: '透气运动T恤', price: 129, category: '服装', description: '速干透气面料，运动必备', stock: 120 },
  { id: 3, name: '碳纤维羽毛球拍', price: 880, category: '球类', description: '超轻碳纤维材质，进攻利器', stock: 30 },
  { id: 4, name: '加厚瑜伽垫', price: 89, category: '健身', description: '环保TPE材质，防滑加厚', stock: 200 },
  { id: 5, name: '户外登山包 50L', price: 369, category: '户外', description: '大容量防水，登山徒步首选', stock: 45 },
  { id: 6, name: '竞速泳镜', price: 158, category: '游泳', description: '防雾防紫外线，专业竞速', stock: 80 },
  { id: 7, name: '篮球鞋', price: 499, category: '鞋类', description: '高帮护踝，室内外通用', stock: 60 },
  { id: 8, name: '运动短裤', price: 79, category: '服装', description: '宽松舒适，速干透气', stock: 150 },
  { id: 9, name: '哑铃套装 20kg', price: 259, category: '健身', description: '包胶静音，家用健身', stock: 35 },
  { id: 10, name: '网球拍', price: 320, category: '球类', description: '碳铝合金，初学者适用', stock: 40 },
  { id: 11, name: '冲锋衣', price: 459, category: '户外', description: '防风防雨透气，三合一设计', stock: 25 },
  { id: 12, name: '连体泳衣', price: 199, category: '游泳', description: '竞速修身，低水阻设计', stock: 55 }
])

const categories = computed(() => {
  const cats = [...new Set(products.value.map(p => p.category))]
  return cats
})

const filteredProducts = computed(() => {
  let result = products.value
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(p => p.name.toLowerCase().includes(keyword) || p.description.toLowerCase().includes(keyword))
  }
  if (selectedCategory.value) {
    result = result.filter(p => p.category === selectedCategory.value)
  }
  return result
})

const handleSearch = () => {
  // 搜索逻辑已通过 computed 自动处理
}

const handleAddToCart = (item) => {
  store.dispatch('addToCart', { id: item.id, name: item.name, price: item.price })
  ElMessage.success(`已将 ${item.name} 加入购物车！`)
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
