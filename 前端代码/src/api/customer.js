import request from '@/utils/request'

export function getCustomerList(params) {
  return request.get('/customer/list', { params })
}

export function getCustomerByCode(customerCode) {
  return request.get(`/customer/${customerCode}`)
}

export function addCustomer(data) {
  return request.post('/customer', data)
}

export function updateCustomer(data) {
  return request.put('/customer', data)
}

export function deleteCustomer(customerCode) {
  return request.delete(`/customer/${customerCode}`)
}

export function changePassword(data) {
  return request.put('/customer/change-password', data)
}
