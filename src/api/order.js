import request from '@/utils/request'

// 分页查询订单列表
export function getOrderList(params) {
  return request.get('/order/list', { params })
}

// 根据ID获取订单详情
export function getOrderById(orderId) {
  return request.get(`/order/${orderId}`)
}

// 创建订单
export function createOrder(data) {
  return request.post('/order', data)
}

// 确认订单
export function confirmOrder(orderId) {
  return request.put(`/order/confirm/${orderId}`)
}

// 发货
export function shipOrder(orderId) {
  return request.put(`/order/ship/${orderId}`)
}

// 取消订单
export function cancelOrder(orderId) {
  return request.put(`/order/cancel/${orderId}`)
}

// 付款
export function payOrder(orderId, paymentMethod) {
  return request.put(`/order/pay/${orderId}`, null, { params: { paymentMethod } })
}

// 完成订单（确认收货）
export function completeOrder(orderId) {
  return request.put(`/order/complete/${orderId}`)
}

// 获取未付款订单
export function getUnpaidOrders() {
  return request.get('/order/unpaid')
}

// 获取未发货订单
export function getUnshippedOrders() {
  return request.get('/order/unshipped')
}

// 获取已完成订单
export function getCompletedOrders() {
  return request.get('/order/completed')
}
