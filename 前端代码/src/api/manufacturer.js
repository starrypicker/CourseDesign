import request from '@/utils/request'

export function getManufacturerList() {
  return request.get('/manufacturer/list')
}

export function getManufacturerByCode(manufacturerCode) {
  return request.get(`/manufacturer/${manufacturerCode}`)
}

export function addManufacturer(data) {
  return request.post('/manufacturer', data)
}

export function updateManufacturer(data) {
  return request.put('/manufacturer', data)
}

export function deleteManufacturer(manufacturerCode) {
  return request.delete(`/manufacturer/${manufacturerCode}`)
}
