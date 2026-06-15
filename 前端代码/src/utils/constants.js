// API 基础路径
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

// WebSocket 基础路径（生产环境可通过环境变量配置）
export const WS_BASE_URL = import.meta.env.VITE_WS_BASE_URL || ''

// 请求超时时间（毫秒）
export const REQUEST_TIMEOUT = 10000

// 默认分页大小
export const DEFAULT_PAGE_SIZE = 10

// 用户角色常量
export const ROLES = {
  CUSTOMER: 'customer',
  ADMIN: 'admin'
}

// 支付方式
export const PAYMENT_METHODS = [
  { value: 'wechat', label: '微信支付' },
  { value: 'alipay', label: '支付宝' },
  { value: 'bank_card', label: '银行卡' },
  { value: 'cod', label: '货到付款' }
]
