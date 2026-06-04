<template>
  <div class="home-container">
    <!-- 欢迎横幅 -->
    <el-card class="welcome-banner" shadow="never">
      <h1>欢迎来到体育用品商城</h1>
      <p>专业装备，助力你的每一次突破</p>
      <el-button type="primary" size="large" @click="goToProducts">
        立即选购
      </el-button>
    </el-card>

    <!-- 商品展示区域 (后续可替换为真实API数据) -->
    <div class="section-title">
      <h2>热门推荐</h2>
    </div>
    <el-row :gutter="20">
      <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="item in mockProducts" :key="item.id">
        <el-card class="product-card" shadow="hover">
          <div class="product-image">
            <el-icon :size="60" color="#909399"><ShoppingBag /></el-icon>
          </div>
          <div class="product-info">
            <h3>{{ item.name }}</h3>
            <p class="price">¥ {{ item.price.toFixed(2) }}</p>
            <el-button type="warning" plain size="small" @click="handleAddToCart(item)">
              加入购物车
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { ElMessage } from 'element-plus'
import { ShoppingBag } from '@element-plus/icons-vue'

const router = useRouter()
const store = useStore()

// 模拟商品数据（后续替换为后端API请求）
const mockProducts = ref([
  { id: 1, name: '专业跑步鞋', price: 599 },
  { id: 2, name: '透气运动T恤', price: 129 },
  { id: 3, name: '碳纤维羽毛球拍', price: 880 },
  { id: 4, name: '加厚瑜伽垫', price: 89 }
])

// 跳转到商品列表页
const goToProducts = () => {
  router.push('/products')
}

// 加入购物车
const handleAddToCart = (item) => {
  store.dispatch('addToCart', { id: item.id, name: item.name, price: item.price })
  ElMessage.success(`已将 ${item.name} 加入购物车！`)
}
</script>

<style scoped>
.home-container {
  padding: 20px;
}

.welcome-banner {
  text-align: center;
  background: linear-gradient(135deg, #409EFF, #79bbff);
  color: white;
  margin-bottom: 30px;
  border: none;
}

.welcome-banner h1 {
  font-size: 32px;
  margin-bottom: 10px;
}

.welcome-banner p {
  font-size: 16px;
  margin-bottom: 20px;
  opacity: 0.9;
}

/* 覆盖 Element Plus 卡片内部背景色，使其与渐变融合 */
.welcome-banner :deep(.el-card__body) {
  background: transparent; 
}

.section-title {
  margin-bottom: 20px;
  border-left: 4px solid #409EFF;
  padding-left: 10px;
}

.product-card {
  margin-bottom: 20px;
  text-align: center;
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

.product-info h3 {
  font-size: 16px;
  margin-bottom: 10px;
}

.product-info .price {
  color: #f56c6c;
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 15px;
}
</style>
