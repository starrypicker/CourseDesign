import request from '@/utils/request'

export function getInvoiceList(params) {
  return request.get('/invoice/list', { params })
}

export function getInvoiceById(id) {
  return request.get(`/invoice/${id}`)
}

export function getInvoiceByOrderId(orderId) {
  return request.get(`/invoice/by-order/${orderId}`)
}

export function requestInvoice(data) {
  return request.post('/invoice/request', data)
}

export function issueInvoice(id, invoiceNo) {
  return request.put(`/invoice/issue/${id}`, { invoiceNo })
}

export function cancelInvoice(id) {
  return request.put(`/invoice/cancel/${id}`)
}
