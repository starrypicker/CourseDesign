<template>
  <div class="cart-container">
    <el-card shadow="never">
      <template #header>
        <div class="cart-header">
          <h2><el-icon><ShoppingCart /></el-icon> 我的购物车</h2>
          <el-button v-if="cart.length > 0" type="danger" plain size="small" @click="handleClearCart">清空购物车</el-button>
        </div>
      </template>
      <el-table v-if="cart.length > 0" :data="cart" style="width:100%" stripe>
        <el-table-column label="商品信息" min-width="250">
          <template #default="{ row }"><div class="cart-product-info"><div class="product-icon"><el-icon :size="36" color="#909399"><ShoppingBag /></el-icon></div><span class="product-name">{{ row.productName }}</span></div></template>
        </el-table-column>
        <el-table-column label="单价" width="120" align="center"><template #default="{ row }"><span class="price">¥ {{ Number(row.unitPrice || 0).toFixed(2) }}</span></template></el-table-column>
        <el-table-column label="数量" width="180" align="center">
          <template #default="{ row }"><el-input-number v-model="row.quantity" :min="1" :max="row.stockQuantity || 999" size="small" @change="(val) => handleQuantityChange(row.productCode, val)" /></template>
        </el-table-column>
        <el-table-column label="小计" width="120" align="center"><template #default="{ row }"><span class="price">¥ {{ (Number(row.unitPrice || 0) * row.quantity).toFixed(2) }}</span></template></el-table-column>
        <el-table-column label="操作" width="100" align="center"><template #default="{ row }"><el-button type="danger" link @click="handleRemove(row.productCode)"><el-icon><Delete /></el-icon>删除</el-button></template></el-table-column>
      </el-table>
      <el-empty v-else description="购物车是空的，快去选购吧！"><el-button type="primary" @click="$router.push('/products')">去购物</el-button></el-empty>
    </el-card>
    <el-card v-if="cart.length > 0" shadow="never" class="checkout-card">
      <div class="checkout-bar">
        <div class="checkout-info">
          <span>共 <strong>{{ cartCount }}</strong> 件商品</span>
          <span class="checkout-total">合计：<strong>¥ {{ cartTotal.toFixed(2) }}</strong></span>
          <div class="payment-select">
            <span class="payment-label">支付方式：</span>
            <el-select v-model="paymentMethod" size="small" style="width:130px">
              <el-option v-for="pm in PAYMENT_METHODS" :key="pm.value" :label="pm.label" :value="pm.value" />
            </el-select>
          </div>
        </div>
        <el-button type="primary" size="large" @click="handleCheckout">去结算</el-button>
      </div>
    </el-card>

    <!-- 邮寄信息确认弹窗 -->
    <el-dialog v-model="confirmVisible" title="确认订单信息" width="620px" destroy-on-close>
      <div class="confirm-order" v-if="confirmVisible">
        <h4 style="margin:0 0 12px;color:#303133;">📦 邮寄信息</h4>
        <el-form :model="shippingForm" :rules="shippingRules" ref="shippingFormRef" label-width="90px">
          <el-form-item label="收件人" prop="recipientName">
            <el-input v-model="shippingForm.recipientName" placeholder="请输入收件人姓名" />
          </el-form-item>
          <el-form-item label="联系电话" prop="recipientPhone">
            <el-input v-model="shippingForm.recipientPhone" placeholder="请输入收件人电话" />
          </el-form-item>
          <el-form-item label="收件地址" prop="recipientAddress">
            <el-input v-model="shippingForm.recipientAddress" type="textarea" :rows="2" placeholder="请输入收件地址" />
          </el-form-item>
          <el-form-item label="运输要求">
            <el-input v-model="shippingForm.shippingRequirement" placeholder="运输特殊要求（选填）" />
          </el-form-item>
        </el-form>

        <el-divider />

        <h4 style="margin:0 0 12px;color:#303133;">🛒 商品明细</h4>
        <el-table :data="cart" size="small" stripe style="width:100%">
          <el-table-column prop="productName" label="商品" />
          <el-table-column prop="unitPrice" label="单价" width="100">
            <template #default="{ row }">¥ {{ Number(row.unitPrice || 0).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="80" />
          <el-table-column label="小计" width="110">
            <template #default="{ row }"><span style="color:#f56c6c;">¥ {{ (Number(row.unitPrice || 0) * row.quantity).toFixed(2) }}</span></template>
          </el-table-column>
        </el-table>

        <div class="order-summary">
          <span>共 <strong>{{ cartCount }}</strong> 件商品，支付方式：<strong>{{ PAYMENT_METHODS.find(m => m.value === paymentMethod)?.label || paymentMethod }}</strong></span>
          <span class="summary-total">合计：<strong>¥ {{ cartTotal.toFixed(2) }}</strong></span>
        </div>
      </div>
      <template #footer>
        <el-button @click="confirmVisible = false">返回修改</el-button>
        <el-button type="primary" @click="handleConfirmOrder" :loading="submitting">确认下单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, reactive } from 'vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ShoppingCart, ShoppingBag, Delete } from '@element-plus/icons-vue'
import { PAYMENT_METHODS } from '@/utils/constants'

const store = useStore()
const router = useRouter()
const cart = computed(() => store.getters.cart)
const cartCount = computed(() => store.getters.cartCount)
const cartTotal = computed(() => store.getters.cartTotal)
const paymentMethod = ref('wechat')

const confirmVisible = ref(false)
const submitting = ref(false)
const shippingFormRef = ref(null)
const shippingForm = reactive({
  recipientName: '',
  recipientPhone: '',
  recipientAddress: '',
  shippingRequirement: ''
})
const shippingRules = {
  recipientName: [{ required: true, message: '请输入收件人姓名', trigger: 'blur' }],
  recipientPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号', trigger: 'blur' }
  ],
  recipientAddress: [{ required: true, message: '请输入收件地址', trigger: 'blur' }]
}

const handleQuantityChange = (productCode, quantity) => store.dispatch('updateCartQuantity', { productCode, quantity })
const handleRemove = (productCode) => { store.dispatch('removeFromCart', productCode); ElMessage.success('已从购物车移除') }
const handleClearCart = () => {
  ElMessageBox.confirm('确定要清空购物车吗？', '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    .then(() => { store.dispatch('clearCart'); ElMessage.success('购物车已清空') }).catch(() => {})
}

const handleCheckout = async () => {
  const userInfo = store.getters.userInfo
  if (!userInfo) { ElMessage.warning('请先登录'); router.push('/login'); return }
  if (cart.value.length === 0) { ElMessage.warning('购物车是空的'); return }
  if (!userInfo.address || !userInfo.phone) {
    try {
      await ElMessageBox.confirm('您的收货地址或联系电话未完善，请先前往个人中心填写', '收货信息不完整', { confirmButtonText: '前往个人中心', cancelButtonText: '取消', type: 'warning' })
      router.push('/profile')
    } catch {}
    return
  }

  // 同步最新库存信息并验证
  await store.dispatch('syncCartStock')
  const outOfStockItems = []
  const insufficientItems = []
  cart.value.forEach(item => {
    const stock = item.stockQuantity
    if (stock !== undefined) {
      if (stock <= 0) {
        outOfStockItems.push(item)
      } else if (stock < item.quantity) {
        insufficientItems.push({ ...item, availableStock: stock })
      }
    }
  })

  // 移除已售罄的商品
  if (outOfStockItems.length > 0) {
    const names = outOfStockItems.map(i => i.productName).join('、')
    outOfStockItems.forEach(i => store.dispatch('removeFromCart', i.productCode))
    ElMessage.warning(`以下商品已售罄，已从购物车移除：${names}`)
  }

  // 处理库存不足的商品
  if (insufficientItems.length > 0) {
    const names = insufficientItems.map(i => `${i.productName}(仅剩${i.availableStock}件)`).join('、')
    insufficientItems.forEach(i => store.dispatch('updateCartQuantity', { productCode: i.productCode, quantity: i.availableStock }))
    ElMessage.warning(`以下商品库存不足，已自动调整数量：${names}`)
  }

  // 再次检查购物车是否为空
  if (cart.value.length === 0) { ElMessage.warning('购物车中所有商品均已售罄，请重新选购'); return }

  // 预填邮寄信息（从用户Profile）
  shippingForm.recipientName = userInfo.contactName || userInfo.customerName || ''
  shippingForm.recipientPhone = userInfo.phone || ''
  shippingForm.recipientAddress = userInfo.address || ''
  shippingForm.shippingRequirement = ''
  confirmVisible.value = true
}

const handleConfirmOrder = async () => {
  if (!shippingFormRef.value) return
  try {
    await shippingFormRef.value.validate()
  } catch { return }

  submitting.value = true
  try {
    const userInfo = store.getters.userInfo
    const { createOrder } = await import('@/api/order')
    const orderData = {
      customerCode: userInfo.customerCode,
      recipientName: shippingForm.recipientName,
      recipientAddress: shippingForm.recipientAddress,
      recipientPhone: shippingForm.recipientPhone,
      shippingRequirement: shippingForm.shippingRequirement || undefined,
      paymentMethod: paymentMethod.value,
      items: cart.value.map(item => ({ productCode: item.productCode, quantity: item.quantity }))
    }
    const orderId = await createOrder(orderData)
    if (orderId == null || orderId <= 0) {
      ElMessage.error('下单失败，请稍后重试')
      return
    }
    store.dispatch('clearCart')
    confirmVisible.value = false
    ElMessage.success(`下单成功！订单号：${orderId}`)
    router.push('/orders')
  } catch (e) { console.error('下单失败:', e) }
  finally { submitting.value = false }
}
</script>

<style scoped>
.cart-container { padding: 10px 0; }
.cart-header { display: flex; align-items: center; justify-content: space-between; }
.cart-header h2 { display: flex; align-items: center; gap: 8px; font-size: 18px; margin: 0; }
.cart-product-info { display: flex; align-items: center; gap: 12px; }
.product-icon { width: 50px; height: 50px; display: flex; align-items: center; justify-content: center; background: #f5f7fa; border-radius: 4px; flex-shrink: 0; }
.product-name { font-size: 14px; color: #303133; }
.price { color: #f56c6c; font-weight: bold; }
.checkout-card { margin-top: 20px; }
.checkout-bar { display: flex; align-items: center; justify-content: flex-end; gap: 30px; }
.checkout-info { display: flex; align-items: center; gap: 20px; font-size: 14px; color: #606266; flex-wrap: wrap; }
.payment-select { display: flex; align-items: center; gap: 6px; }
.payment-label { font-size: 13px; color: #909399; }
.checkout-total strong { color: #f56c6c; font-size: 22px; }
.order-summary { display: flex; align-items: center; justify-content: space-between; margin-top: 16px; font-size: 14px; color: #606266; }
.summary-total strong { color: #f56c6c; font-size: 20px; }
</style>
