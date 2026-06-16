<template>
  <div class="home-container">
    <!-- 欢迎横幅 -->
    <div class="welcome-banner">
      <div class="banner-content">
        <div class="banner-icon">
          <el-icon :size="80"><Trophy /></el-icon>
        </div>
        <h1>欢迎来到体育用品商城</h1>
        <p>专业装备，助力你的每一次突破</p>
        <el-button type="primary" size="large" round @click="goToProducts">
          立即选购 <el-icon><ArrowRight /></el-icon>
        </el-button>
      </div>
    </div>

    <!-- 热门推荐 -->
    <div class="section-header">
      <h2><span class="section-bar"></span>热门推荐</h2>
      <el-button text type="primary" @click="goToProducts">查看全部 <el-icon><ArrowRight /></el-icon></el-button>
    </div>

    <!-- 骨架屏 -->
    <el-row :gutter="20" v-if="loading">
      <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="i in 4" :key="i">
        <div class="product-card skeleton-card">
          <div class="skeleton" style="height:160px;border-radius:4px 4px 0 0;"></div>
          <div style="padding:16px;">
            <div class="skeleton" style="height:20px;width:70%;margin-bottom:8px;"></div>
            <div class="skeleton" style="height:24px;width:40%;margin-bottom:12px;"></div>
            <div class="skeleton" style="height:32px;"></div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20" v-else>
      <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="item in hotProducts" :key="item.productCode">
        <el-card class="product-card card-hover" shadow="hover" @click="handleViewDetail(item)">
          <div class="product-image">
            <img v-if="getProductImage(item)" :src="getProductImage(item)" :alt="item.productName" class="product-img" />
            <el-icon v-else :size="56" color="#909399"><ShoppingBag /></el-icon>
          </div>
          <div class="product-info">
            <h3>{{ item.productName }}</h3>
            <el-tag v-if="item.manufacturerName" size="small" type="info" class="manufacturer-tag">
              {{ item.manufacturerName }}
            </el-tag>
            <p class="price">¥ {{ formatPrice(item.unitPrice) }}</p>
            <div class="stock-info">
              <span v-if="item.stockQuantity <= 0" class="stock-sold-out">已售罄</span>
              <span v-else-if="item.stockQuantity <= item.minStock" class="stock-warning">库存紧张 ({{ item.stockQuantity }})</span>
              <span v-else class="stock-ok">库存充足</span>
            </div>
            <div class="product-actions">
              <el-button size="small" @click.stop="handleViewDetail(item)">详情</el-button>
              <el-button
                type="warning" plain size="small"
                @click.stop="handleAddToCart(item)"
                :disabled="item.stockQuantity <= 0"
              >
                <el-icon><ShoppingCart /></el-icon>{{ item.stockQuantity <= 0 ? '已售罄' : '加购' }}
              </el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <el-empty v-if="!loading && hotProducts.length === 0" description="暂无推荐商品" />

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
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { ElMessage } from 'element-plus'
import { ShoppingBag, ShoppingCart, Trophy, ArrowRight } from '@element-plus/icons-vue'
import { getProductList } from '@/api/product'
import ProductDetailDialog from '@/components/ProductDetailDialog.vue'
import { getProductImage } from '@/utils/constants'

const router = useRouter()
const store = useStore()
const hotProducts = ref([])
const loading = ref(true)
const detailVisible = ref(false)
const detailProduct = ref(null)

const fetchHotProducts = async () => {
  loading.value = true
  try {
    const res = await getProductList({ pageNum: 1, pageSize: 8, status: 1 })
    hotProducts.value = res.rows || []
  } catch (e) { console.error('获取热门商品失败:', e) } finally { loading.value = false }
}

onMounted(() => fetchHotProducts())

const formatPrice = (val) => Number(val || 0).toFixed(2)

const goToProducts = () => router.push('/products')

const handleViewDetail = (item) => {
  detailProduct.value = item
  detailVisible.value = true
}

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
.home-container { padding: 0; }

/* 横幅 */
.welcome-banner {
  background: linear-gradient(135deg, #409EFF 0%, #53a8ff 50%, #79bbff 100%);
  border-radius: 16px; padding: 60px 40px; margin-bottom: 30px;
  position: relative; overflow: hidden;
}
.welcome-banner::before {
  content: '';
  position: absolute; top: -50%; right: -10%;
  width: 300px; height: 300px;
  background: rgba(255,255,255,0.08); border-radius: 50%;
}
.welcome-banner::after {
  content: '';
  position: absolute; bottom: -30%; left: -5%;
  width: 200px; height: 200px;
  background: rgba(255,255,255,0.06); border-radius: 50%;
}
.banner-content {
  position: relative; z-index: 1;
  display: flex; flex-direction: column; align-items: center;
  color: #fff; text-align: center;
}
.banner-icon {
  width: 120px; height: 120px; border-radius: 50%;
  background: rgba(255,255,255,0.15); display: flex;
  align-items: center; justify-content: center; margin-bottom: 20px;
}
.banner-content h1 { font-size: 36px; margin-bottom: 8px; font-weight: 700; }
.banner-content p { font-size: 18px; margin-bottom: 24px; opacity: 0.9; }

/* 区域标题 */
.section-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 20px;
}
.section-header h2 {
  display: flex; align-items: center; gap: 10px;
  font-size: 20px; color: #303133; margin: 0;
}
.section-bar {
  display: inline-block; width: 4px; height: 20px;
  background: #409EFF; border-radius: 2px;
}

/* 商品卡片 */
.product-card {
  margin-bottom: 20px; cursor: pointer;
  border-radius: 12px; overflow: hidden;
}
.product-image {
  height: 160px; display: flex; align-items: center; justify-content: center;
  background: #f5f7fa; border-radius: 4px; margin-bottom: 15px; overflow: hidden;
}
.product-img {
  width: 100%; height: 100%; object-fit: cover;
}
.product-info h3 {
  font-size: 16px; margin-bottom: 8px; color: #303133;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.manufacturer-tag { margin-bottom: 8px; }
.product-info .price {
  color: #f56c6c; font-size: 20px; font-weight: bold; margin-bottom: 6px;
}
.stock-info { margin-bottom: 10px; font-size: 12px; }
.stock-warning { color: #E6A23C; }
.stock-sold-out { color: #f56c6c; font-weight: 500; }
.stock-ok { color: #67C23A; }
.product-actions { display: flex; gap: 8px; }
.skeleton-card { height: 320px; }
</style>
