import axios from 'axios'
import { ElMessage } from 'element-plus'
import { API_BASE_URL, REQUEST_TIMEOUT } from './constants'

// 创建 Axios 实例
const service = axios.create({
  baseURL: API_BASE_URL,          // 基础 URL，从常量中读取
  timeout: REQUEST_TIMEOUT        // 请求超时时间
})

// -------------------- 请求拦截器 --------------------
service.interceptors.request.use(
  config => {
    // 从 localStorage 获取 token，并添加到请求头
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => {
    // 请求错误处理
    console.error('请求错误：', error)
    return Promise.reject(error)
  }
)

// -------------------- 响应拦截器 --------------------
service.interceptors.response.use(
  response => {
    const res = response.data

    // 根据后端约定的数据结构判断请求是否成功
    // 这里假设后端返回格式为 { code: 200, data: ..., message: 'success' }
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      // 如果 code 为特定值（如 401）可做特殊处理
      if (res.code === 401) {
        // token 过期，清除登录信息并跳转登录页
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        window.location.href = '/login'
      }
      return Promise.reject(new Error(res.message || 'Error'))
    } else {
      // 请求成功，直接返回 data
      return res.data
    }
  },
  error => {
    // 处理 HTTP 错误（如 404、500 等）
    const { response } = error
    if (response) {
      const { status, data } = response
      let message = '请求出错'
      switch (status) {
        case 400:
          message = '请求参数错误'
          break
        case 401:
          message = '登录已过期，请重新登录'
          localStorage.removeItem('token')
          localStorage.removeItem('userInfo')
          window.location.href = '/login'
          break
        case 403:
          message = '没有权限访问'
          break
        case 404:
          message = '请求的资源不存在'
          break
        case 500:
          message = '服务器内部错误'
          break
        default:
          message = data?.message || `连接错误 ${status}`
      }
      ElMessage.error(message)
    } else {
      ElMessage.error('网络异常，请检查您的网络连接')
    }
    return Promise.reject(error)
  }
)

export default service