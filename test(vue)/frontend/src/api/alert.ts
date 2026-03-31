import request from './request'
import type { ApiResponse } from './types'

export interface AlertCenterRow {
  id: number
  severityText: string
  patientName: string
  alertType: string
  firstTime: string
  lastTime: string
  duration: string
  doctor: string
  statusText: string
  selected: boolean
}

export interface AlertCenterDetail {
  patient: string
  severityText: string
  alertType: string
  firstTime: string
  lastTime: string
  doctor: string
  statusText: string
  summary: string
}

export interface AlertCenterData {
  statusParam: string | null
  typeParam: string | null
  doctorParam: string | null
  rangeParam: string | null
  selectedAlertId: number | null
  typeList: string[]
  doctorList: string[]
  rows: AlertCenterRow[]
  detail: AlertCenterDetail | null
}

export interface FollowupWorkbenchFilter {
  status?: string
  doctor?: string
  startDate?: string
  endDate?: string
  q?: string
  page?: number
}

export interface FollowupWorkbenchRow {
  patientId: number
  patientIdNumber: number // 病案号
  riskId?: number // 风险分层ID
  name: string
  phone: string
  idCard: string
  symptom: string
  systemSyndrome: string
  visitDate: string
  visitTime: string
  status: string
  statusText: string
  doctor: string
  disease: string // 病种
  followupType: string // 随访类型
  riskLevel?: string // 风险等级
  followupRecordId?: number // 随访记录ID（用于已随访记录跳转到详情页）
}

export interface FollowupWorkbenchData {
  statusParam: string | null
  doctorParam: string | null
  startDate: string | null
  endDate: string | null
  keyword: string | null
  pageNo: number
  totalPages: number
  total: number
  doctorList: string[]
  rows: FollowupWorkbenchRow[]
}

export interface HardwareAlertRow {
  id: number
  severityText: string
  patientName: string | null
  deviceType: string
  deviceId: string | null
  alertType: string
  firstTime: string
  lastTime: string
  duration: string
  statusText: string
  selected: boolean
}

export interface HardwareAlertDetail {
  patientName: string | null
  severityText: string
  deviceType: string
  deviceId: string | null
  alertType: string
  location: string | null
  firstTime: string
  lastTime: string
  statusText: string
  summary: string
}

export interface HardwareAlertData {
  statusParam: string | null
  deviceTypeParam: string | null
  rangeParam: string | null
  selectedAlertId: number | null
  deviceTypeList: string[]
  rows: HardwareAlertRow[]
  detail: HardwareAlertDetail | null
}

export const alertApi = {
  // 获取告警中心数据
  async getAlerts(filter: {
    status?: string
    type?: string
    doctor?: string
    range?: string
    alertId?: string
  } = {}): Promise<ApiResponse<AlertCenterData>> {
    try {
      const params = new URLSearchParams()
      if (filter.status) params.append('status', filter.status)
      if (filter.type) params.append('type', filter.type)
      if (filter.doctor) params.append('doctor', filter.doctor)
      if (filter.range) params.append('range', filter.range)
      if (filter.alertId) params.append('alertId', filter.alertId)

      const response = await request.get<any>(`/api/alert/alerts?${params.toString()}`)
      const data = (response as any)?.data ?? response
      return {
        success: true,
        data: data as any
      }
    } catch (error: any) {
      return { success: false, message: error.message || '加载失败' }
    }
  },

  // 获取患者每日测量数据
  async getPatientDailyMeasurements(patientId: number, days: number = 7): Promise<ApiResponse<any>> {
    try {
      // 后端真实路由在 PatientController: /patient/daily-measurements
      // 兼容部署环境：dev 环境通常通过代理访问 /patient/...；部分环境可能统一挂到 /api 前缀
      let response: any
      try {
        response = await request.get<any>(`/patient/daily-measurements?patientId=${patientId}&days=${days}`)
      } catch (e: any) {
        response = await request.get<any>(`/api/patient/daily-measurements?patientId=${patientId}&days=${days}`)
      }

      const data = (response as any)?.data ?? response
      // 某些情况下（如前端 dev server 将 /patient 视为 SPA 路由），可能返回 index.html（字符串/HTML）且不会抛错。
      // 若返回不是数组，尝试再兜底请求另一条路径。
      if (!Array.isArray(data)) {
        try {
          const alt = await request.get<any>(`/api/patient/daily-measurements?patientId=${patientId}&days=${days}`)
          const altData = (alt as any)?.data ?? alt
          if (Array.isArray(altData)) return { success: true, data: altData as any }
        } catch {
          // ignore
        }
        try {
          const alt = await request.get<any>(`/patient/daily-measurements?patientId=${patientId}&days=${days}`)
          const altData = (alt as any)?.data ?? alt
          if (Array.isArray(altData)) return { success: true, data: altData as any }
        } catch {
          // ignore
        }
      }
      return { success: true, data: data as any }
    } catch (error: any) {
      return { success: false, message: error.message || '加载失败' }
    }
  },

  // 获取随访工作台数据
  async getFollowupWorkbench(filter: FollowupWorkbenchFilter = {}): Promise<ApiResponse<FollowupWorkbenchData>> {
    try {
      const params = new URLSearchParams()
      if (filter.status) params.append('status', filter.status)
      if (filter.doctor) params.append('doctor', filter.doctor)
      if (filter.startDate) params.append('startDate', filter.startDate)
      if (filter.endDate) params.append('endDate', filter.endDate)
      if (filter.q) params.append('q', filter.q)
      if (filter.page) params.append('page', filter.page.toString())

      const response = await request.get<any>(`/api/alert/followup?${params.toString()}`)
      const data = (response as any)?.data ?? response
      return {
        success: true,
        data: data as any
      }
    } catch (error: any) {
      return { success: false, message: error.message || '加载失败' }
    }
  },

  // 获取硬件异常数据
  async getHardwareAlerts(filter: {
    status?: string
    deviceType?: string
    range?: string
    alertId?: string
  } = {}): Promise<ApiResponse<HardwareAlertData>> {
    try {
      const params = new URLSearchParams()
      if (filter.status) params.append('status', filter.status)
      if (filter.deviceType) params.append('deviceType', filter.deviceType)
      if (filter.range) params.append('range', filter.range)
      if (filter.alertId) params.append('alertId', filter.alertId)

      const response = await request.get<any>(`/api/alert/hardware?${params.toString()}`)
      const data = (response as any)?.data ?? response
      return {
        success: true,
        data: data as any
      }
    } catch (error: any) {
      return { success: false, message: error.message || '加载失败' }
    }
  }

  ,
  // 更新告警状态（关闭/恢复）
  async updateAlertStatus(payload: { source: 'HEALTH' | 'DEVICE'; alertId: number; status: 'NEW' | 'PROCESSING' | 'CLOSED' }): Promise<ApiResponse<any>> {
    try {
      const response = await request.post<any>('/api/alert/status', payload)
      const data = (response as any)?.data ?? response
      return {
        success: (data as any)?.success !== false,
        data: data as any,
        message: (data as any)?.message
      }
    } catch (error: any) {
      return { success: false, message: error.message || '更新失败' }
    }
  }
}

