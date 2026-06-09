import request from '@/utils/request'

export function getOrderList(params) {
  return request.get('/order/list', { params })
}

export function getOrderById(orderId) {
  return request.get(`/order/${orderId}`)
}

export function createOrder(data) {
  return request.post('/order', data)
}

export function confirmOrder(orderId) {
  return request.put(`/order/confirm/${orderId}`)
}

export function shipOrder(orderId) {
  return request.put(`/order/ship/${orderId}`)
}

export function cancelOrder(orderId) {
  return request.put(`/order/cancel/${orderId}`)
}

export function payOrder(orderId, paymentMethod) {
  return request.put(`/order/pay/${orderId}`, null, { params: { paymentMethod } })
}

export function completeOrder(orderId) {
  return request.put(`/order/complete/${orderId}`)
}

export function getUnpaidOrders() {
  return request.get('/order/unpaid')
}

export function getUnshippedOrders() {
  return request.get('/order/unshipped')
}

export function getCompletedOrders() {
  return request.get('/order/completed')
}

export function getDashboardStats() {
  return request.get('/order/dashboard-stats')
}
