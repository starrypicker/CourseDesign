import axios from 'axios'
import { ElMessage } from 'element-plus'
import { API_BASE_URL, REQUEST_TIMEOUT } from './constants'
import router from '@/router'

const service = axios.create({
  baseURL: API_BASE_URL,
  timeout: REQUEST_TIMEOUT
})

service.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => {
    console.error('请求错误：', error)
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      if (res.code === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        router.push('/login')
      }
      return Promise.reject(new Error(res.message || 'Error'))
    } else {
      return res.data
    }
  },
  error => {
    const { response } = error
    if (response) {
      const { status, data } = response
      let message = '请求出错'
      switch (status) {
        case 400: message = '请求参数错误'; break
        case 401:
          message = '登录已过期，请重新登录'
          localStorage.removeItem('token')
          localStorage.removeItem('userInfo')
          router.push('/login')
          break
        case 403: message = '没有权限访问'; break
        case 404: message = '请求的资源不存在'; break
        case 500: message = '服务器内部错误'; break
        default: message = data?.message || `连接错误 ${status}`
      }
      ElMessage.error(message)
    } else {
      ElMessage.error('网络异常，请检查您的网络连接')
    }
    return Promise.reject(error)
  }
)

export default service
