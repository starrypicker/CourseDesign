
<template>
  <div class="cart-container">
    <el-card shadow="never">
      <template #header>
        <div class="cart-header">
          <h2><el-icon><ShoppingCart /></el-icon> 我的购物车</h2>
          <el-button v-if="cart.length > 0" type="danger" plain size="small" @click="handleClearCart">
            清空购物车
          </el-button>
        </div>
      </template>

      <!-- 购物车列表 -->
      <el-table v-if="cart.length > 0" :data="cart" style="width: 100%" stripe>
        <el-table-column label="商品信息" min-width="250">
          <template #default="{ row }">
            <div class="cart-product-info">
              <div class="product-icon">
                <el-icon :size="36" color="#909399"><ShoppingBag /></el-icon>
              </div>
              <span class="product-name">{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="单价" width="120" align="center">
          <template #default="{ row }">
            <span class="price">¥ {{ row.price.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="数量" width="180" align="center">
          <template #default="{ row }">
            <el-input-number
              v-model="row.quantity"
              :min="1"
              :max="99"
              size="small"
              @change="(val) => handleQuantityChange(row.id, val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="小计" width="120" align="center">
          <template #default="{ row }">
            <span class="price">¥ {{ (row.price * row.quantity).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-button type="danger" link @click="handleRemove(row.id)">
              <el-icon><Delete /></el-icon> 删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 空状态 -->
      <el-empty v-else description="购物车是空的，快去选购吧！">
        <el-button type="primary" @click="$router.push('/products')">去购物</el-button>
      </el-empty>
    </el-card>

    <!-- 结算栏 -->
    <el-card v-if="cart.length > 0" shadow="never" class="checkout-card">
      <div class="checkout-bar">
        <div class="checkout-info">
          <span>共 <strong>{{ cartCount }}</strong> 件商品</span>
          <span class="checkout-total">
            合计：<strong>¥ {{ cartTotal.toFixed(2) }}</strong>
          </span>
        </div>
        <el-button type="primary" size="large" @click="handleCheckout">
          去结算
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ShoppingCart, ShoppingBag, Delete } from '@element-plus/icons-vue'

const store = useStore()
const router = useRouter()

const cart = computed(() => store.getters.cart)
const cartCount = computed(() => store.getters.cartCount)
const cartTotal = computed(() => store.getters.cartTotal)

const handleQuantityChange = (productId, quantity) => {
  store.dispatch('updateCartQuantity', { productId, quantity })
}

const handleRemove = (productId) => {
  store.dispatch('removeFromCart', productId)
  ElMessage.success('已从购物车移除')
}

const handleClearCart = () => {
  ElMessageBox.confirm('确定要清空购物车吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    store.dispatch('clearCart')
    ElMessage.success('购物车已清空')
  }).catch(() => {})
}

const handleCheckout = () => {
  ElMessage.info('结算功能需要后端API支持，敬请期待！')
}
</script>

<style scoped>
.cart-container {
  padding: 10px 0;
}

.cart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.cart-header h2 {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  margin: 0;
}

.cart-product-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.product-icon {
  width: 50px;
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  border-radius: 4px;
  flex-shrink: 0;
}

.product-name {
  font-size: 14px;
  color: #303133;
}

.price {
  color: #f56c6c;
  font-weight: bold;
}

.checkout-card {
  margin-top: 20px;
}

.checkout-bar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 30px;
}

.checkout-info {
  display: flex;
  align-items: center;
  gap: 20px;
  font-size: 14px;
  color: #606266;
}

.checkout-total strong {
  color: #f56c6c;
  font-size: 22px;
}
</style>
