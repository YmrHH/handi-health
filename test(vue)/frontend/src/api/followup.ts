import request from './request'
import type { ApiResponse } from './types'

export interface FollowupListFilter {
  startDate?: string
  endDate?: string
  riskLevel?: string
  followupType?: string
  resultStatus?: string
  staffName?: string
  patientName?: string
  pageNo?: number
}

export interface FollowupListRow {
  id: number
  patientId: number // 病案号
  followupDate: string
  patientName: string
  riskLevel: string
  followupType: string
  staffName: string
  contentSummary: string
  resultStatus: string
  nextPlanTime: string
}

export interface FollowupListStats {
  totalVisits: number
  patientCount: number
  completed: number
  noAnswer: number
  needFollow: number
  typeStats: {
    phone: number
    video: number
    home: number
  }
  riskStats: {
    high: number
    mid: number
    low: number
  }
}

export interface FollowupListData {
  rows: FollowupListRow[]
  pageNo: number
  totalPages: number
  total: number
  stats: FollowupListStats
}

export interface FollowupDetailRecord {
  id: number
  followupTime: string
  followupType: string
  staffName: string
  resultStatus: string
  riskLevel: string
  riskLevelText: string
  riskLevelCode: string
  nextPlanTime: string
  contentSummary: string
  symptomChange: string
  sbp: string
  dbp: string
  heartRate: string
  weight: string
  vitalMeasureTime: string
  medAdherence: string
  medPlanSummary: string
  adverseReaction: string
  tcmFace: string
  tcmTongueBody: string
  tcmTongueCoating: string
  tcmTongueImageUrl: string
  tcmPulseRate: string
  tcmPulseTypes: string
  tcmConclusion: string
  tcmRemark: string
  tcmEvaluationAdvice?: string
  labFpgValue: string
  labFpgStatus: string
  labFpgStatusCode: string
  labLdlcValue: string
  labLdlcStatus: string
  labLdlcStatusCode: string
  labTgValue: string
  labTgStatus: string
  labTgStatusCode: string
  labHcyValue: string
  labHcyStatus: string
  labHcyStatusCode: string
  labUaValue: string
  labUaStatus: string
  labUaStatusCode: string
  labSummary: string
  healthAdvice?: string
  summary: string
  nextFollowupType: string
  nextFollowupDate: string
  nextFollowupRemark: string
}

export interface FollowupDetailPatient {
  name: string
  gender: string
  age: number
  idCard: string
  phone: string
  disease: string
  syndrome: string
}

export interface FollowupDetailData {
  record: FollowupDetailRecord
  patient: FollowupDetailPatient
  tcmPulseTypeList: string[]
}

export interface FollowupTaskWorkbenchFilter {
  page?: number
  pageSize?: number
  status?: string
  staffId?: number | string
  doctorId?: number | string
  riskLevel?: string
  startAt?: string
  endAt?: string
}

export interface FollowupTaskWorkbenchData {
  total: number
  rows: any[]
}

// 随访人员相关接口
export interface StaffListFilter {
  staffName?: string
  department?: string
  status?: string
  pageNo?: number
  pageSize?: number
}

export interface StaffListRow {
  id?: number
  staffId?: number | string
  name: string
  department?: string
  phone?: string
  patientCount?: number
  totalFollowupCount?: number
  monthFollowupCount?: number
  status?: string
}

export interface StaffListStats {
  totalStaff: number
  activeStaff: number
  activeThisMonth: number
  totalFollowups: number
  monthFollowups: number
  avgFollowups: number
  totalPatients: number
  avgPatients: number
}

export interface StaffListData {
  rows: StaffListRow[]
  pageNo: number
  pageSize: number
  totalPages: number
  total: number
  stats: StaffListStats
}

export interface StaffDetailPatient {
  patientId: number
  patientName: string
  riskLevel?: string
  lastFollowupTime?: string
  followupCount?: number
}

export interface StaffDetailFollowup {
  id: number
  followupTime: string
  patientName: string
  followupType: string
  resultStatus: string
  riskLevel?: string
}

export interface StaffDetailData {
  id?: number
  staffId?: number | string
  name: string
  department?: string
  position?: string
  phone?: string
  email?: string
  status?: string
  patientCount?: number
  totalFollowupCount?: number
  monthFollowupCount?: number
  completedCount?: number
  patients?: StaffDetailPatient[]
  recentFollowups?: StaffDetailFollowup[]
}

 function normalizeDepartment(...candidates: any[]): string | undefined {
   for (const c of candidates) {
     if (c == null) continue
     const v = String(c).trim()
     if (v) return v
   }
   return undefined
 }

 function normalizeStaffStatus(status: any): string {
   if (status == null) return '在职'
   if (typeof status === 'string') {
     const v = status.trim()
     if (v === '在职' || v === '离职') return v
     if (v === '1') return '在职'
     if (v === '0') return '离职'
     return v
   }
   if (typeof status === 'number') {
     return status === 1 ? '在职' : '离职'
   }
   return '在职'
 }

 function mapStaffListRow(row: any): StaffListRow {
   return {
     id: row?.id,
     staffId: row?.staffId ?? row?.jobNo ?? row?.username ?? row?.id,
     name: row?.name,
     department: normalizeDepartment(row?.department, row?.deptName, row?.address),
     phone: row?.phone ?? row?.mobile,
     patientCount: row?.patientCount,
     totalFollowupCount: row?.totalFollowupCount,
     monthFollowupCount: row?.monthFollowupCount,
     status: normalizeStaffStatus(row?.status)
   }
 }

 function mapStaffDetail(data: any): StaffDetailData {
   const role = data?.role == null ? undefined : String(data.role).trim().toUpperCase()
   const position =
     data?.position ??
     data?.postName ??
     (role === 'FOLLOW_UP' ? '随访人员' : undefined)

   return {
     id: data?.id,
     staffId: data?.staffId ?? data?.jobNo ?? data?.username,
     name: data?.name,
     department: normalizeDepartment(data?.department, data?.deptName, data?.address),
     position,
     phone: data?.phone ?? data?.mobile,
     email: data?.email,
     status: normalizeStaffStatus(data?.status),
     patientCount: data?.patientCount,
     totalFollowupCount: data?.totalFollowupCount,
     monthFollowupCount: data?.monthFollowupCount,
     completedCount: data?.completedCount,
     patients: data?.patients,
     recentFollowups: data?.recentFollowups
   }
 }

export const followupApi = {
  // 获取随访记录列表
  async getList(filter: FollowupListFilter = {}): Promise<ApiResponse<FollowupListData>> {
    try {
      const params = new URLSearchParams()
      if (filter.startDate) params.append('startDate', filter.startDate)
      if (filter.endDate) params.append('endDate', filter.endDate)
      if (filter.riskLevel) params.append('riskLevel', filter.riskLevel)
      if (filter.followupType) params.append('followupType', filter.followupType)
      if (filter.resultStatus) params.append('resultStatus', filter.resultStatus)
      if (filter.staffName) params.append('staffName', filter.staffName)
      if (filter.patientName) params.append('patientName', filter.patientName)
      if (filter.pageNo) params.append('pageNo', filter.pageNo.toString())

      const response = await request.get<any>(`/api/followup/list?${params.toString()}`)
      // request.ts 会把非统一格式包成 { success: true, data: xxx }
      const data: any = (response as any)?.data ?? response

      return {
        success: data.success !== false,
        data: data as any,
        message: data.error || data.message
      }
    } catch (error: any) {
      return { success: false, message: error.message || '加载失败' }
    }
  },

  // 获取随访工作台任务列表（用于“主动随访工作台”）
  async getWorkbenchTasks(filter: FollowupTaskWorkbenchFilter = {}): Promise<ApiResponse<FollowupTaskWorkbenchData>> {
    try {
      const params = new URLSearchParams()
      // 兜底：部分场景浏览器请求头 Authorization 可能未生效；后端支持从 query param 读取 userId
      const savedToken = sessionStorage.getItem('token')
      if (savedToken && savedToken.trim() !== '') params.append('userId', savedToken.trim())
      if (filter.page != null) params.append('page', String(filter.page))
      if (filter.pageSize != null) {
        const ps = Math.max(1, Math.min(Number(filter.pageSize) || 10, 200))
        params.append('pageSize', String(ps))
      }
      if (filter.status) params.append('status', filter.status)
      if (filter.staffId != null && String(filter.staffId).trim() !== '') params.append('staffId', String(filter.staffId))
      if (filter.doctorId != null && String(filter.doctorId).trim() !== '') params.append('doctorId', String(filter.doctorId))
      if (filter.riskLevel) params.append('riskLevel', filter.riskLevel)
      if (filter.startAt) params.append('startAt', filter.startAt)
      if (filter.endAt) params.append('endAt', filter.endAt)

      const response = await request.get<any>(`/api/followup/tasks?${params.toString()}`)
      const resData: any = response
      const finalData = resData?.data ?? resData
      const payload = finalData?.data ?? finalData

      return {
        success: resData?.success !== false && finalData?.success !== false,
        data: {
          total: Number(payload?.total ?? 0) || 0,
          rows: Array.isArray(payload?.rows) ? payload.rows : []
        },
        message: payload?.message || payload?.error || finalData?.message || resData?.message
      }
    } catch (error: any) {
      return { success: false, message: error.message || '加载失败' }
    }
  },

  // 获取随访详情
  async getDetail(id: number): Promise<ApiResponse<FollowupDetailData>> {
    try {
      // 详情接口字段多、后端查询可能较慢，单独放宽超时时间
      const response = await request.get<any>(`/api/followup/detail?id=${id}`, { timeout: 120000 })
      const data: any = (response as any)?.data ?? response
      return {
        success: data?.success !== false,
        data: data?.success === false ? undefined : (data as any),
        message: data?.message || data?.error
      }
    } catch (error: any) {
      return { success: false, message: error.message || '加载失败' }
    }
  },

  // 导入随访数据
  async importData(csvData: string): Promise<ApiResponse> {
    try {
      const params = new URLSearchParams()
      params.append('data', csvData)
      
      const response = await request.post<string>('/api/alert/followup-import', params.toString(), {
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded'
        }
      })

      // 解析JSON字符串响应
      let data: any
      if (typeof response === 'string') {
        data = JSON.parse(response)
      } else {
        data = response
      }

      return {
        success: data.success !== false,
        message: data.message
      }
    } catch (error: any) {
      return { success: false, message: error.message || '导入失败' }
    }
  },

  // 导出随访数据（演示环境：不调用后端，仅提示模拟导出）
  async exportData(filter: {
    status?: string
    doctor?: string
    startDate?: string
    endDate?: string
    q?: string
  } = {}): Promise<void> {
    const params = new URLSearchParams()
    if (filter.status) params.append('status', filter.status)
    if (filter.doctor) params.append('doctor', filter.doctor)
    if (filter.startDate) params.append('startDate', filter.startDate)
    if (filter.endDate) params.append('endDate', filter.endDate)
    if (filter.q) params.append('q', filter.q)

    // 演示模式：只在控制台输出参数，并给出提示，不发起真实下载
    console.log('导出随访数据（演示环境，不调用后端）：', params.toString())
    if (typeof window !== 'undefined' && typeof window.alert === 'function') {
      window.alert('当前为演示环境，已模拟导出随访数据（未连接后端，也不会产生真实文件）。')
    }
  },

  // 获取随访人员列表
  async getStaffList(filter: StaffListFilter = {}): Promise<ApiResponse<StaffListData>> {
    try {
      const params = new URLSearchParams()
      if (filter.staffName) params.append('staffName', filter.staffName)
      if (filter.department) params.append('department', filter.department)
      if (filter.status) {
        const raw = String(filter.status).trim()
        if (raw === '在职') {
          params.append('statusStr', '1')
        } else if (raw === '离职') {
          params.append('statusStr', '0')
        } else if (/^\d+$/.test(raw)) {
          params.append('statusStr', raw)
        }
      }
      if (filter.pageNo) params.append('pageNo', filter.pageNo.toString())
      if (filter.pageSize) params.append('pageSize', filter.pageSize.toString())

      const response = await request.get<any>(`/api/followup/staff/list?${params.toString()}`)
      // 响应拦截器已处理：可能返回 { success, data }，也可能直接返回后端结构
      const resData: any = response
      const finalData = resData?.data ?? resData
      const payload = finalData?.data ?? finalData

      const rowsRaw = payload?.rows || payload?.list || []
      const rows = Array.isArray(rowsRaw) ? rowsRaw.map(mapStaffListRow) : []

      const total = payload?.total ?? rows.length
      const pageNo = payload?.pageNo ?? filter.pageNo ?? 1
      const pageSize = payload?.pageSize ?? filter.pageSize ?? 10
      const totalPages = payload?.totalPages ?? Math.ceil((total || 0) / pageSize)

      return {
        success: resData?.success !== false && finalData?.success !== false,
        data: {
          rows,
          pageNo,
          pageSize,
          total,
          totalPages,
          stats: payload?.stats
        } as any,
        message: payload?.error || payload?.message || finalData?.message || resData?.message
      }
    } catch (error: any) {
      console.error('getStaffList error:', error)
      return { success: false, message: error.message || '加载失败' }
    }
  },

  // 获取随访人员详情
  async getStaffDetail(id: string): Promise<ApiResponse<StaffDetailData>> {
    try {
      const response = await request.get<any>(`/api/followup/staff/detail?id=${id}`, { timeout: 120000 })
      const resData: any = response
      const finalData = resData?.data ?? resData
      const payload = finalData?.data ?? finalData

      return {
        success: resData?.success !== false && finalData?.success !== false,
        data: (resData?.success === false || finalData?.success === false) ? undefined : mapStaffDetail(payload),
        message: payload?.message || payload?.error || finalData?.message || resData?.message
      }
    } catch (error: any) {
      return { success: false, message: error.message || '加载失败' }
    }
  },

  // 注册随访人员
  async registerStaff(params: {
    staffNo?: string
    name: string
    phone: string
    address?: string
    sex?: string
    age?: number
  }): Promise<ApiResponse<{ userId: number }>> {
    try {
      // 兼容“工号”字段：优先传 username/jobNo，便于后端落库到 user.username
      const payload: any = { ...params }
      const staffNo = (params as any)?.staffNo != null ? String((params as any).staffNo).trim() : ''
      if (staffNo) {
        payload.username = staffNo
        payload.jobNo = staffNo
        payload.staffId = staffNo
      }

      const response = await request.post<any>('/api/followup/staff', payload)
      const data: any = (response as any)?.data ?? response
      return {
        success: data?.success !== false,
        data: data?.success === false ? undefined : data,
        message: data?.message || data?.error
      }
    } catch (error: any) {
      return { success: false, message: error.message || '注册失败' }
    }
  },

  // 更新随访人员状态（1=在职，0=离职）
  async updateStaffStatus(params: { id: number | string; status: 0 | 1 }): Promise<ApiResponse> {
    try {
      const response = await request.post<any>('/api/followup/staff/status', params)
      const data: any = (response as any)?.data ?? response
      return {
        success: data?.success !== false,
        message: data?.message || data?.error
      }
    } catch (error: any) {
      return { success: false, message: error.message || '更新失败' }
    }
  }
}

