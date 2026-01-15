import axios from 'axios'
import { Message } from 'element-ui'

// 创建axios实例
const service = axios.create({
  baseURL: process.env.VUE_APP_BASE_API || 'http://localhost:8090', // 后端API地址
  timeout: 10000 // 请求超时时间
})

// 请求拦截器
service.interceptors.request.use(
  config => {
    // 从localStorage获取token
    const adminToken = localStorage.getItem('adminToken')
    const userToken = localStorage.getItem('userToken')
    
    // 如果有token，添加到请求头
    if (adminToken) {
      config.headers['Authorization'] = 'Bearer ' + adminToken
    } else if (userToken) {
      config.headers['Authorization'] = 'Bearer ' + userToken
    }
    
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    const res = response.data
    
    // 如果后端返回的状态码不是200，则视为错误
    if (res.code && res.code !== 200) {
      const errorMsg = res.msg || res.message || '请求失败'
      Message.error(errorMsg)
      return Promise.reject(new Error(errorMsg))
    }
    
    return res
  },
  error => {
    console.error('响应错误:', error)
    
    // 根据错误类型提供更友好的提示
    let errorMessage = '网络错误'
    if (error.code === 'ECONNREFUSED' || error.message.includes('ERR_CONNECTION_REFUSED')) {
      errorMessage = '无法连接到服务器，请确保后端服务已启动（http://localhost:8090）'
    } else if (error.code === 'ECONNABORTED' || error.message.includes('timeout')) {
      errorMessage = '请求超时，请稍后重试'
    } else if (error.response) {
      // 服务器返回了错误状态码
      const status = error.response.status
      const responseData = error.response.data
      
      // 优先使用后端返回的错误信息
      if (responseData && (responseData.msg || responseData.message)) {
        errorMessage = responseData.msg || responseData.message
      } else if (status === 401) {
        errorMessage = '未授权，请重新登录'
      } else if (status === 403) {
        errorMessage = '没有权限访问'
      } else if (status === 404) {
        // 如果是API请求404，提供更详细的提示
        const url = error.config?.url || '未知资源'
        const baseURL = error.config?.baseURL || '未知'
        const fullUrl = baseURL + url
        if (url.includes('/api/')) {
          errorMessage = `API端点不存在: ${fullUrl}。请确保：1) 后端服务已启动在 ${baseURL}；2) 端点路径正确；3) 检查后端日志`
          console.error('404错误详情:', {
            url: fullUrl,
            method: error.config?.method,
            baseURL: baseURL,
            response: error.response?.data
          })
        } else {
          errorMessage = `资源不存在: ${url}`
        }
      } else if (status >= 500) {
        errorMessage = '服务器错误，请稍后重试'
      } else {
        errorMessage = `请求失败 (${status})`
      }
    } else if (error.message) {
      errorMessage = error.message
    }
    
    Message.error(errorMessage)
    return Promise.reject(error)
  }
)

export default service
