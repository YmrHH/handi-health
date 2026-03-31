// 通用响应类型
export interface ApiResponse<T = any> {
  success: boolean
  message?: string
  data?: T
}

// 分页响应
export interface PageResponse<T> {
  rows: T[]
  pageNo: number
  pageSize: number
  total: number
}

// 登录请求
export interface LoginRequest {
  phone: string
  password: string
}

// 注册请求
export interface RegisterRequest {
  name: string
  password: string
  confirmPassword: string
  phone: string
}

// 通用查询参数
export interface QueryParams {
  pageNo?: number
  pageSize?: number
  [key: string]: any
}

