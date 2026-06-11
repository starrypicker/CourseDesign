import request from '@/utils/request'

export function getPurchaseList() {
  return request.get('/purchase/list')
}

export function getPurchaseById(id) {
  return request.get(`/purchase/${id}`)
}

export function addPurchase(data) {
  return request.post('/purchase', data)
}

export function confirmPurchase(id) {
  return request.put(`/purchase/confirm/${id}`)
}
