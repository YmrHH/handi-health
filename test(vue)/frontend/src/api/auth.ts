import request from './request'
import type { ApiResponse, LoginRequest, RegisterRequest } from './types'

export const authApi = {
  // 获取当前登录用户信息（用于刷新后重新校验登录态）
  async me(): Promise<ApiResponse> {
    try {
      const response: any = await request.get('/api/auth/me')
      const resData = response?.data ?? response
      return {
        success: resData?.success !== false,
        message: resData?.message,
        data: resData?.data
      }
    } catch (error: any) {
      return { success: false, message: error.message || '获取用户信息失败' }
    }
  },

  // 登录
  async login(data: LoginRequest): Promise<ApiResponse> {
    try {
      const response: any = await request.post('/api/auth/loginByPhone', {
        phone: data.phone,
        password: data.password
      })
      
      // 后端直接返回 { success, userId, role, name, ... }，不包装在 data 中
      // 响应拦截器可能已处理，需兼容两种情况
      const resData = response?.data ?? response
      
      return {
        success: resData?.success !== false,
        message: resData?.message,
        data: {
          userId: resData?.userId ?? resData?.id,
          role: resData?.role,
          name: resData?.name,
          username: resData?.username
        }
      }
    } catch (error: any) {
      return { success: false, message: error.message || '登录失败，请检查网络连接' }
    }
  },

  // 注册
  async register(data: RegisterRequest): Promise<ApiResponse> {
    try {
      const response = await request.post('/api/auth/register', {
        name: data.name,
        password: data.password,
        confirmPassword: data.confirmPassword,
        phone: data.phone
      })
      
      return {
        success: response.success || false,
        message: response.message,
        data: response.data
      }
    } catch (error: any) {
      return { success: false, message: error.message || '注册失败，请检查网络连接' }
    }
  },

  // 登出
  async logout(): Promise<ApiResponse> {
    try {
      const response = await request.get('/api/auth/logout')
      return {
        success: response.success || false,
        message: response.message
      }
    } catch (error: any) {
      return { success: false, message: error.message || '登出失败' }
    }
  }
}

