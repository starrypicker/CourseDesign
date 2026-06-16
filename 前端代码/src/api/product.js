import request from '@/utils/request'

export function getProductList(params) {
  return request.get('/product/list', { params })
}

export function getProductByCode(productCode) {
  return request.get(`/product/${productCode}`)
}

export function addProduct(data) {
  return request.post('/product', data)
}

export function updateProduct(data) {
  return request.put('/product', data)
}

export function deleteProduct(productCode) {
  return request.delete(`/product/${productCode}`)
}

export function getLowStockProducts() {
  return request.get('/product/low-stock')
}

export function replenishStock(productCode, quantity) {
  return request.post('/product/replenish', null, { params: { productCode, quantity } })
}

export function uploadProductImage(productCode, file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post(`/product/${productCode}/upload-image`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function batchUploadProductImages(files) {
  const formData = new FormData()
  files.forEach(file => formData.append('files', file))
  return request.post('/product/batch-upload-images', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
