import request from '@/utils/request'
import { WS_BASE_URL } from '@/utils/constants'

export function getChatHistory(targetCode) {
  return request.get('/chat/history', { params: { targetCode } })
}

export function getContacts() {
  return request.get('/chat/contacts')
}

export function markAsRead(senderCode) {
  return request.put(`/chat/read/${senderCode}`)
}

export function getUnreadCount() {
  return request.get('/chat/unread-count')
}

/** 创建 WebSocket 连接 */
export function createChatSocket(token) {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  // 优先使用环境变量配置的 WebSocket 地址，否则使用当前 host（开发时 Vite proxy 会转发）
  const host = WS_BASE_URL || window.location.host
  const url = `${protocol}//${host}/ws/chat?token=${encodeURIComponent(token)}`
  return new WebSocket(url)
}
