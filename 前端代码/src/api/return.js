import request from '@/utils/request'

export function getReturnList(params) {
  return request.get('/return/list', { params })
}

export function getReturnById(id) {
  return request.get(`/return/${id}`)
}

export function createReturn(data) {
  return request.post('/return', data)
}

export function approveReturn(id, data) {
  return request.put(`/return/approve/${id}`, data)
}

export function rejectReturn(id, data) {
  return request.put(`/return/reject/${id}`, data)
}

export function completeReturn(id) {
  return request.put(`/return/complete/${id}`)
}
