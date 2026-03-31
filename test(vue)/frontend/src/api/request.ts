import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import type { ApiResponse } from './types'
import { useAuthStore } from '@/stores/auth'
import { recordFrontendAuditLog } from '@/audit/frontendAudit'

// 简单的消息提示（可以后续替换为UI组件库）
function showMessage(message: string, type: 'success' | 'error' | 'warning' = 'error') {
  console[type === 'error' ? 'error' : type === 'warning' ? 'warn' : 'log'](message)
  // TODO: 可以后续替换为Element Plus的ElMessage或其他UI库
}

// 创建axios实例
const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 30000,
  withCredentials: true // 支持携带cookie（用于session认证）
})

// 请求拦截器
service.interceptors.request.use(
  (config: AxiosRequestConfig) => {
    // 添加 token：后端按 Authorization: Bearer <userId> 识别当前用户
    const authStore = useAuthStore()
    const token = authStore.token || sessionStorage.getItem('token')
    if (token) {
      config.headers = (config.headers || {}) as any
      ;(config.headers as any)['Authorization'] = `Bearer ${token}`
      ;(config.headers as any)['X-User-Id'] = String(token)

      // 兜底：确保 workbench tasks 请求携带 userId（后端支持从 query param 取 userId）
      // 用于解决某些代理/环境下 Authorization header 未被后端读取的问题。
      if (typeof config.url === 'string' && config.url.includes('/api/followup/tasks')) {
        const hasUserId = /(^|[?&])userId=/.test(config.url)
        if (!hasUserId) {
          const sep = config.url.includes('?') ? '&' : '?'
          config.url = `${config.url}${sep}userId=${encodeURIComponent(String(token))}`
        }
      }
    }

    ;(config as any).__auditStartAt = Date.now()
    ;(config as any).__auditRequestId = `${Date.now()}_${Math.random().toString(16).slice(2)}`
    return config
  },
  (error) => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    try {
      const authStore = useAuthStore()
      const cfg: any = response.config || {}
      const startAt = typeof cfg.__auditStartAt === 'number' ? cfg.__auditStartAt : undefined
      const durationMs = startAt != null ? Date.now() - startAt : undefined
      recordFrontendAuditLog({
        type: 'api',
        user: authStore.username,
        page: typeof window !== 'undefined' ? window.location.pathname : undefined,
        method: (cfg.method || 'GET').toString().toUpperCase(),
        url: cfg.url,
        status: response.status,
        success: true,
        durationMs,
        requestId: cfg.__auditRequestId
      })
    } catch {
      // ignore
    }

    const res = response.data

    // 如果后端返回的数据结构是 { success: boolean, data: T }（优先判断）
    if (res && typeof res === 'object' && typeof (res as any).success === 'boolean') {
      if (!(res as any).success) {
        const msg = (res as any).message || (res as any).msg || '请求失败'
        showMessage(msg, 'error')
        return Promise.reject(new Error(msg))
      }
      return res
    }

    // 兼容后端结构：{ code, msg, data }（code 为数字 0 表示成功）
    if (res && typeof res === 'object' && Object.prototype.hasOwnProperty.call(res as any, 'code')) {
      const code = (res as any).code
      // 只有当 code 是数字且不为 0 时才认为失败
      if (typeof code === 'number' && code !== 0) {
        const msg = (res as any).msg || (res as any).message || '请求失败'
        showMessage(msg, 'error')
        return Promise.reject(new Error(msg))
      }
      const data = Object.prototype.hasOwnProperty.call(res as any, 'data') ? (res as any).data : res
      return { success: true, data } as any
    }

    // 如果后端直接返回数据（不是统一格式）
    return { success: true, data: res }
  },
  (error) => {
    console.error('响应错误:', error)

    try {
      const authStore = useAuthStore()
      const cfg: any = error?.config || {}
      const startAt = typeof cfg.__auditStartAt === 'number' ? cfg.__auditStartAt : undefined
      const durationMs = startAt != null ? Date.now() - startAt : undefined
      recordFrontendAuditLog({
        type: 'api',
        user: authStore.username,
        page: typeof window !== 'undefined' ? window.location.pathname : undefined,
        method: (cfg.method || 'GET').toString().toUpperCase(),
        url: cfg.url,
        status: error?.response?.status,
        success: false,
        durationMs,
        requestId: cfg.__auditRequestId,
        detail: {
          message: error?.message,
          responseMessage: error?.response?.data?.message
        }
      })
    } catch {
      // ignore
    }

    let message = '请求失败'
    if (error.response) {
      switch (error.response.status) {
        case 401:
          message = '未授权，请重新登录'
          // 可以在这里跳转到登录页
          // router.push('/login')
          break
        case 403:
          message = '拒绝访问'
          break
        case 404:
          message = '请求地址不存在'
          break
        case 409:
          message = error.response.data?.message || '资源冲突'
          break
        case 500:
          message = '服务器内部错误'
          break
        default:
          message = error.response.data?.message || `错误代码: ${error.response.status}`
      }
    } else if (error.request) {
      message = '网络连接失败，请检查网络'
    }

    showMessage(message, 'error')

    // 关键：将后端 message 作为 error.message 传递给上层，避免只看到
    // "Request failed with status code XXX"
    const wrapped = Object.assign(new Error(message), {
      response: error?.response,
      config: error?.config,
      isAxiosError: error?.isAxiosError
    })
    return Promise.reject(wrapped)
  }
)

// 对外暴露统一的 request 接口，内部根据 USE_MOCK 决定是否真正发起网络请求
const request = {
  get<T = any>(url: string, config?: AxiosRequestConfig) {
    return service.get<T>(url, config)
  },
  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig) {
    return service.post<T>(url, data, config)
  }
}

export default request

