<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="$emit('update:visible', $event)"
    :title="product?.productName || '商品详情'"
    width="540px"
    destroy-on-close
    class="product-detail-dialog"
  >
    <div v-if="product" class="detail-body">
      <!-- 商品图标区域 -->
      <div class="detail-hero" :class="stockClass">
        <div class="hero-icon" :class="{ 'has-image': productImage }">
          <img v-if="productImage" :src="productImage" :alt="product?.productName" class="hero-image" @error="onImageError" />
          <el-icon v-else :size="64"><ShoppingBag /></el-icon>
        </div>
        <div class="hero-info">
          <h2 class="hero-name">{{ product.productName }}</h2>
          <el-tag :type="stockTagType" size="default" effect="dark">
            {{ stockLabel }}
          </el-tag>
          <p v-if="product.manufacturerName" class="hero-manufacturer">
            <el-icon><OfficeBuilding /></el-icon> {{ product.manufacturerName }}
          </p>
        </div>
      </div>

      <!-- 详情信息 -->
      <el-descriptions :column="2" border size="small" class="detail-table">
        <el-descriptions-item label="商品编码" :span="1">
          <el-tag type="info" size="small">{{ product.productCode }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="商品名称" :span="1">{{ product.productName }}</el-descriptions-item>
        <el-descriptions-item label="生产厂家" :span="1">
          {{ product.manufacturerName || '未知' }}
        </el-descriptions-item>
        <el-descriptions-item label="状态" :span="1">
          <el-tag :type="product.status === 1 ? 'success' : 'info'" size="small">
            {{ product.status === 1 ? '上架' : '下架' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="单价">
          <span class="price-highlight">¥ {{ formatPrice(product.unitPrice) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="库存">
          <span :style="{ color: stockColor, fontWeight: 'bold' }">
            {{ product.stockQuantity ?? 0 }}
          </span>
          <span v-if="product.minStock" style="color:#909399;font-size:12px;margin-left:4px;">
            / 最低 {{ product.minStock }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="重量" :span="1">
          {{ product.weight || 0 }} kg
        </el-descriptions-item>
        <el-descriptions-item label="最低库存" :span="1">
          {{ product.minStock || 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">
          <span class="detail-desc">{{ product.productDesc || '暂无描述' }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <!-- 库存状态进度条 -->
      <div class="stock-bar-wrapper" v-if="product.minStock && product.minStock > 0">
        <div class="stock-bar-label">
          <span>库存状态</span>
          <span class="stock-bar-pct">{{ stockPercentage }}%</span>
        </div>
        <el-progress
          :percentage="Math.min(stockPercentage, 100)"
          :color="progressColor"
          :stroke-width="8"
          :show-text="false"
        />
        <p class="stock-bar-hint">{{ stockHint }}</p>
      </div>
    </div>

    <div v-else class="detail-loading">
      <el-skeleton :rows="6" animated />
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="$emit('update:visible', false)">关闭</el-button>
        <el-button
          v-if="showAddCart"
          type="primary"
          @click="handleAddToCart"
          :disabled="!canAddToCart"
          :loading="addingToCart"
        >
          <el-icon><ShoppingCart /></el-icon>
          {{ canAddToCart ? '加入购物车' : '暂不可购买' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useStore } from 'vuex'
import { ElMessage } from 'element-plus'
import { ShoppingBag, ShoppingCart, OfficeBuilding } from '@element-plus/icons-vue'
import { getProductByCode } from '@/api/product'

const props = defineProps({
  visible: { type: Boolean, default: false },
  product: { type: Object, default: null },
  productCode: { type: String, default: '' },
  showAddCart: { type: Boolean, default: true }
})

const emit = defineEmits(['update:visible', 'add-to-cart'])

const store = useStore()
const addingToCart = ref(false)
const fetchedProduct = ref(null)

// 当弹窗打开且有 productCode 但无 product 时，自动获取最新数据
watch(() => props.visible, async (val) => {
  if (val && props.productCode && !props.product) {
    try {
      fetchedProduct.value = await getProductByCode(props.productCode)
    } catch { fetchedProduct.value = null }
  } else {
    fetchedProduct.value = null
  }
})

// 优先使用传入的 product，其次使用 fetchedProduct
const displayProduct = computed(() => props.product || fetchedProduct.value)

// 商品图片（处理图片加载失败）
const imageError = ref(false)
const productImage = computed(() => {
  if (imageError.value) return null
  const img = displayProduct.value?.image
  if (!img) return null
  // 如果已经是完整URL则直接使用，否则加前缀
  if (img.startsWith('http') || img.startsWith('/')) return img
  return '/' + img
})
const onImageError = () => { imageError.value = true }

// 弹窗打开时重置图片错误状态
watch(() => props.visible, (val) => {
  if (val) imageError.value = false
})

// 库存相关计算
const stockQuantity = computed(() => displayProduct.value?.stockQuantity ?? 0)
const minStock = computed(() => displayProduct.value?.minStock ?? 0)

const stockClass = computed(() => {
  if (stockQuantity.value <= 0) return 'stock-out'
  if (minStock.value > 0 && stockQuantity.value <= minStock.value) return 'stock-low'
  return 'stock-ok'
})

const stockTagType = computed(() => {
  if (stockQuantity.value <= 0) return 'danger'
  if (minStock.value > 0 && stockQuantity.value <= minStock.value) return 'warning'
  return 'success'
})

const stockLabel = computed(() => {
  if (stockQuantity.value <= 0) return '已售罄'
  if (minStock.value > 0 && stockQuantity.value <= minStock.value) return '库存紧张'
  return '库存充足'
})

const stockColor = computed(() => {
  if (stockQuantity.value <= 0) return '#f56c6c'
  if (minStock.value > 0 && stockQuantity.value <= minStock.value) return '#E6A23C'
  return '#67C23A'
})

const stockPercentage = computed(() => {
  if (minStock.value <= 0) return 100
  return Math.round((stockQuantity.value / (minStock.value * 3)) * 100)
})

const progressColor = computed(() => {
  if (stockPercentage.value <= 20) return '#f56c6c'
  if (stockPercentage.value <= 50) return '#E6A23C'
  return '#67C23A'
})

const stockHint = computed(() => {
  if (stockQuantity.value <= 0) return '该商品已售罄，请等待补货'
  if (minStock.value > 0 && stockQuantity.value <= minStock.value) return '库存低于最低线，建议尽快下单'
  return '库存充足，可放心购买'
})

const canAddToCart = computed(() => {
  return displayProduct.value && displayProduct.value.status === 1 && stockQuantity.value > 0
})

const formatPrice = (val) => {
  return Number(val || 0).toFixed(2)
}

const handleAddToCart = () => {
  const product = displayProduct.value
  if (!product || !canAddToCart.value) return

  addingToCart.value = true
  try {
    store.dispatch('addToCart', {
      productCode: product.productCode,
      productName: product.productName,
      unitPrice: product.unitPrice,
      stockQuantity: product.stockQuantity,
      quantity: 1
    })
    ElMessage.success(`已将 ${product.productName} 加入购物车！`)
    emit('add-to-cart', product)
  } finally {
    addingToCart.value = false
  }
}
</script>

<style scoped>
.product-detail-dialog :deep(.el-dialog__header) {
  border-bottom: 1px solid #ebeef5;
  padding: 16px 20px;
}
.product-detail-dialog :deep(.el-dialog__body) {
  padding: 0;
}
.product-detail-dialog :deep(.el-dialog__footer) {
  border-top: 1px solid #ebeef5;
  padding: 12px 20px;
}

.detail-body {
  padding: 0;
}

/* Hero 区域 */
.detail-hero {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 24px 20px;
  transition: background 0.3s;
}
.detail-hero.stock-ok {
  background: linear-gradient(135deg, #e8f5e9, #f1f8e9);
}
.detail-hero.stock-low {
  background: linear-gradient(135deg, #fff8e1, #fff3e0);
}
.detail-hero.stock-out {
  background: linear-gradient(135deg, #ffebee, #fce4ec);
}

.hero-icon {
  width: 100px;
  height: 100px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: transform 0.3s;
  overflow: hidden;
}
.hero-icon.has-image {
  width: 120px;
  height: 120px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}
.hero-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.stock-ok .hero-icon:not(.has-image) {
  background: rgba(103, 194, 58, 0.12);
  color: #67C23A;
}
.stock-low .hero-icon:not(.has-image) {
  background: rgba(230, 162, 60, 0.12);
  color: #E6A23C;
}
.stock-out .hero-icon:not(.has-image) {
  background: rgba(245, 108, 108, 0.12);
  color: #f56c6c;
}

.hero-info {
  flex: 1;
  min-width: 0;
}
.hero-name {
  margin: 0 0 8px 0;
  font-size: 20px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.hero-manufacturer {
  margin: 8px 0 0 0;
  font-size: 13px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 详情表格 */
.detail-table {
  margin: 0 20px 16px;
}
.detail-table :deep(.el-descriptions__label) {
  width: 80px;
}

.price-highlight {
  color: #f56c6c;
  font-weight: bold;
  font-size: 16px;
}
.detail-desc {
  color: #606266;
  line-height: 1.6;
}

/* 库存进度条 */
.stock-bar-wrapper {
  margin: 0 20px 20px;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 8px;
}
.stock-bar-label {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
  font-size: 13px;
  color: #606266;
}
.stock-bar-pct {
  font-weight: bold;
  color: #303133;
}
.stock-bar-hint {
  margin: 6px 0 0 0;
  font-size: 12px;
  color: #909399;
}

.detail-loading {
  padding: 40px 20px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
