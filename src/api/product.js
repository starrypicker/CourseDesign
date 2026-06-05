import request from '@/utils/request'

// 分页查询商品列表
export function getProductList(params) {
  return request.get('/product/list', { params })
}

// 根据编码获取商品详情
export function getProductByCode(productCode) {
  return request.get(`/product/${productCode}`)
}

// 添加商品
export function addProduct(data) {
  return request.post('/product', data)
}

// 更新商品
export function updateProduct(data) {
  return request.put('/product', data)
}

// 删除商品
export function deleteProduct(productCode) {
  return request.delete(`/product/${productCode}`)
}

// 获取低库存商品
export function getLowStockProducts() {
  return request.get('/product/low-stock')
}

// 补货
export function replenishStock(productCode, quantity) {
  return request.post('/product/replenish', null, { params: { productCode, quantity } })
}
