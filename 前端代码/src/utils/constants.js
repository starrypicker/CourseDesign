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

/**
 * 获取商品图片URL，不存在则返回空字符串
 * @param {Object|string} product - 商品对象或 image 字段值
 * @returns {string}
 */
export function getProductImage(product) {
  if (!product) return ''
  const image = typeof product === 'string' ? product : product.image
  if (!image) return ''
  if (image.startsWith('http') || image.startsWith('/')) return image
  return '/' + image
}
