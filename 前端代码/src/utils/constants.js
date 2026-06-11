// API 基础路径
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

// 请求超时时间（毫秒）
export const REQUEST_TIMEOUT = 10000

// 默认分页大小
export const DEFAULT_PAGE_SIZE = 10

// 用户角色常量
export const ROLES = {
  CUSTOMER: 'customer',
  ADMIN: 'admin'
}
