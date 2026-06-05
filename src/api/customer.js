import request from '@/utils/request'

// 分页查询顾客列表
export function getCustomerList(params) {
  return request.get('/customer/list', { params })
}

// 根据编码获取顾客详情
export function getCustomerByCode(customerCode) {
  return request.get(`/customer/${customerCode}`)
}

// 添加顾客
export function addCustomer(data) {
  return request.post('/customer', data)
}

// 更新顾客
export function updateCustomer(data) {
  return request.put('/customer', data)
}

// 删除顾客
export function deleteCustomer(customerCode) {
  return request.delete(`/customer/${customerCode}`)
}

// 修改密码
export function changePassword(data) {
  return request.put('/customer/change-password', data)
}
