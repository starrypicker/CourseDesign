import request from '@/utils/request'

// 获取进货记录列表
export function getPurchaseList() {
  return request.get('/purchase/list')
}

// 根据ID获取进货记录详情
export function getPurchaseById(id) {
  return request.get(`/purchase/${id}`)
}

// 添加进货记录
export function addPurchase(data) {
  return request.post('/purchase', data)
}

// 确认入库
export function confirmPurchase(id) {
  return request.put(`/purchase/confirm/${id}`)
}
