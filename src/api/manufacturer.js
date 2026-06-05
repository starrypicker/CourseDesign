import request from '@/utils/request'

// 获取厂家列表
export function getManufacturerList() {
  return request.get('/manufacturer/list')
}

// 根据编码获取厂家详情
export function getManufacturerByCode(manufacturerCode) {
  return request.get(`/manufacturer/${manufacturerCode}`)
}

// 添加厂家
export function addManufacturer(data) {
  return request.post('/manufacturer', data)
}

// 更新厂家
export function updateManufacturer(data) {
  return request.put('/manufacturer', data)
}

// 删除厂家
export function deleteManufacturer(manufacturerCode) {
  return request.delete(`/manufacturer/${manufacturerCode}`)
}
