import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken } from './auth'

// 创建 axios 实例
const service = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers['Authorization'] = 'Bearer ' + token
    }
    return config
  },
  (error) => {
    console.log(error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response) => {
    const res = response.data
    
    if (res.code !== 200) {
      ElMessage.error(res.msg || '请求失败')
      
      // 401: Token 过期或无效
      if (res.code === 401) {
        // 可以在这里处理登出逻辑
        window.location.href = '/login'
      }
      
      return Promise.reject(new Error(res.msg || '请求失败'))
    } else {
      // 返回完整的响应，让调用者处理 data
      return res
    }
  },
  (error) => {
    console.log('err' + error)
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default service
