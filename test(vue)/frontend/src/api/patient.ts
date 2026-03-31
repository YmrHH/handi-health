import request from './request'
import type { ApiResponse } from './types'

export interface RiskListFilter {
  level?: string
  disease?: string
  syndrome?: string
  doctor?: string
  q?: string
  pageNo?: number
  pageSize?: number
}

export interface RiskListRow {
  riskId: number
  riskLevel: string
  riskLevelText: string
  patientName: string
  age: number | null
  disease: string
  syndrome: string
  latestIndex: string
  lastVisitDate: string
  doctor: string
}

export interface RiskListData {
  levelParam: string | null
  diseaseParam: string | null
  syndromeParam: string | null
  doctorParam: string | null
  keyword: string | null
  diseaseList: string[]
  syndromeList: string[]
  doctorList: string[]
  highCount: number
  midCount: number
  lowCount: number
  newHighThisWeek: number
  pageNo: number
  pageSize: number
  total: number
  totalPages: number
  rows: RiskListRow[]
}

export interface ProfileEvent {
  date: string
  desc: string
}

export interface PatientProfileData {
  riskId: number
  patientId: number
  name: string
  age: number
  disease: string
  syndrome: string
  constitution?: string
  familyHistory?: string
  riskLevel: string
  riskText: string
  doctor: string
  lastAlertText: string
  events: ProfileEvent[]
  mainComplaint: string
  tongue: string
  pulse: string
  tcmSummary: string
  aiScore: string
  aiFocus: string
  aiAdvice: string
}

export interface PatientMetricsData {
  dates: string[]
  sbp: number[]
  dbp: number[]
  heartRate: number[]
  // 可选：血氧（SpO2），用于日常自测展示
  spo2?: number[]
  latestDate?: string
}

export interface DiseaseTypeOption {
  id: number
  code: string
  name: string
  value: string
  label: string
}

// 中医四诊数据
export interface TCMDiagnosisData {
  id?: number
  patientId: number
  doctorId?: number
  doctorName?: string
  diagnosisDate?: string
  mainComplaint: string
  tongueDescription: string
  tongueImageUrl?: string
  pulseDescription: string
  tcmSummary: string
  physicalExam?: string
  lifestyle?: string
  status?: string
  createdAt?: string
  updatedAt?: string
}

// 患者列表筛选条件
export interface PatientSummaryFilter {
  riskLevel?: string
  disease?: string
  syndrome?: string
  responsibleDoctor?: string
  q?: string
  pageNo?: number
  pageSize?: number
}

// 患者列表行数据
export interface PatientSummaryRow {
  patientId: number  // 病案号
  name: string
  idCard: string | null
  phone: string | null
  gender: string | null
  age: number | null
  disease: string | null
  syndrome: string | null
  riskId: number | null  // 风险分层ID（如果有）
  riskLevel: string | null
  latestIndex: string | null
  lastVisitDate: string | null
  responsibleDoctor: string | null
  followupCount: number
  lastFollowupTime: string | null
  activeAlertCount: number
  pendingTaskCount: number
  latestSbp: number | null
  latestDbp: number | null
  latestHeartRate: number | null
  latestWeight: number | null
}

// 患者列表数据
export interface PatientSummaryData {
  diseaseList: string[]
  syndromeList: string[]
  doctorList: string[]
  pageNo: number
  pageSize: number
  total: number
  totalPages: number
  rows: PatientSummaryRow[]
}

export const patientApi = {
  // 病种库模糊查询
  async searchDiseaseTypes(payload: { q: string; limit?: number }): Promise<ApiResponse<{ rows: DiseaseTypeOption[] }>> {
    try {
      const params = new URLSearchParams()
      if (payload?.q) params.append('q', payload.q)
      if (payload?.limit) params.append('limit', payload.limit.toString())

      const response = await request.get<any>(`/api/disease-type/search?${params.toString()}`)
      const data = (response as any)?.data ?? response
      return {
        success: (data as any)?.success !== false,
        data: data as any,
        message: (data as any)?.message
      }
    } catch (error: any) {
      return { success: false, message: error.message || '查询失败' }
    }
  },

  // 新增病种
  async createDiseaseType(payload: { name: string }): Promise<ApiResponse<{ item: DiseaseTypeOption }>> {
    try {
      const response = await request.post<any>('/api/disease-type', payload)
      const data = (response as any)?.data ?? response
      return {
        success: (data as any)?.success !== false,
        data: data as any,
        message: (data as any)?.message
      }
    } catch (error: any) {
      return { success: false, message: error.message || '创建失败' }
    }
  },

  // 获取风险分层列表
  async getRiskList(filter: RiskListFilter = {}): Promise<ApiResponse<RiskListData>> {
    try {
      const params = new URLSearchParams()
      if (filter.level) params.append('level', filter.level)
      if (filter.disease) params.append('disease', filter.disease)
      if (filter.syndrome) params.append('syndrome', filter.syndrome)
      if (filter.doctor) params.append('doctor', filter.doctor)
      if (filter.q) params.append('q', filter.q)
      if (filter.pageNo) params.append('pageNo', filter.pageNo.toString())
      if (filter.pageSize) params.append('pageSize', filter.pageSize.toString())

      // 风险清单接口可能触发多表查询/远程数据库抖动，单独放宽超时
      const response = await request.get<any>(`/api/patient/risk-list?${params.toString()}`, { timeout: 120000 })
      const data = (response as any)?.data ?? response
      return {
        success: data?.success !== false,
        data: data as any,
        message: data?.message
      }
    } catch (error: any) {
      return { success: false, message: error.message || '加载失败' }
    }
  },

  // 医生创建患者账号（真实落库：写入 user 表，role=PATIENT）
  async createPatientByDoctor(payload: {
    doctorId: number | string
    doctorPassword: string
    name: string
    phone: string
    idCard?: string
    disease?: string
    syndrome?: string
    constitution?: string
    familyHistory?: string
    riskLevel: string
    address?: string
    age?: number | null
    sex?: string
  }): Promise<ApiResponse<any>> {
    try {
      const response = await request.post<any>('/api/auth/createUserByDoctor', {
        doctorId: payload.doctorId,
        doctorPassword: payload.doctorPassword,
        name: payload.name,
        phone: payload.phone,
        idCard: payload.idCard,
        disease: payload.disease,
        syndrome: payload.syndrome,
        constitution: payload.constitution,
        familyHistory: payload.familyHistory,
        role: 'PATIENT',
        riskLevel: payload.riskLevel,
        address: payload.address,
        age: payload.age,
        sex: payload.sex
      })
      const data = (response as any)?.data ?? response
      return {
        success: data?.success !== false,
        data: data as any,
        message: data?.message
      }
    } catch (error: any) {
      const backendMsg = error?.response?.data?.message || error?.response?.data?.msg
      return { success: false, message: backendMsg || error.message || '创建失败' }
    }
  },

  // 获取患者档案
  async getProfile(riskId: number): Promise<ApiResponse<PatientProfileData>> {
    try {
      const response = await request.get<any>(`/api/patient/profile?riskId=${riskId}`)
      const data = (response as any)?.data ?? response
      return {
        success: data?.success !== false,
        data: data as any,
        message: data?.message
      }
    } catch (error: any) {
      return { success: false, message: error.message || '加载失败' }
    }
  },

  // 获取患者指标趋势
  async getMetrics(riskId: number): Promise<ApiResponse<PatientMetricsData>> {
    try {
      const response = await request.get<any>(`/api/patient/metrics?riskId=${riskId}`)
      const data = (response as any)?.data ?? response
      return {
        success: data?.success !== false,
        data: data as any,
        message: data?.message
      }
    } catch (error: any) {
      return { success: false, message: error.message || '加载失败' }
    }
  },

  // 获取患者列表
  async getPatientSummary(filter: PatientSummaryFilter = {}): Promise<ApiResponse<PatientSummaryData>> {
    try {
      const params = new URLSearchParams()
      if (filter.riskLevel) params.append('riskLevel', filter.riskLevel)
      if (filter.disease) params.append('disease', filter.disease)
      if (filter.syndrome) params.append('syndrome', filter.syndrome)
      if (filter.responsibleDoctor) params.append('responsibleDoctor', filter.responsibleDoctor)
      if (filter.q !== undefined && filter.q !== null && filter.q !== '') {
        params.append('q', filter.q.trim())
      }
      if (filter.pageNo) params.append('pageNo', filter.pageNo.toString())
      if (filter.pageSize) params.append('pageSize', filter.pageSize.toString())

      const response = await request.get<any>(`/api/patient/summary?${params.toString()}`, { timeout: 120000 })
      const data = (response as any)?.data ?? response
      return {
        success: data?.success !== false,
        data: data as any,
        message: data?.message
      }
    } catch (error: any) {
      return { success: false, message: error.message || '加载失败' }
    }
  },

  // 创建新患者
  async createPatient(patient: {
    name: string
    gender?: string
    age?: number | null
    phone?: string
    idCard?: string
    disease?: string
    syndrome?: string
  }): Promise<ApiResponse<any>> {
    try {
      const response = await request.post<any>('/api/patient/create', patient)
      const data = (response as any)?.data ?? response
      return {
        success: data?.success !== false,
        data: data as any,
        message: data?.message
      }
    } catch (error: any) {
      return { success: false, message: error.message || '创建失败' }
    }
  },

  // 更新患者风险等级 + 责任医生（落库）
  async updateRiskOwner(payload: {
    patientId: number
    riskLevel: string
    responsibleDoctor: string
  }): Promise<ApiResponse<any>> {
    try {
      const response = await request.post<any>('/api/patient/update-risk-owner', payload)
      const data = (response as any)?.data ?? response
      return {
        success: data?.success !== false,
        data: data as any,
        message: data?.message
      }
    } catch (error: any) {
      const backendMsg = error?.response?.data?.message || error?.response?.data?.msg
      return { success: false, message: backendMsg || error.message || '更新失败' }
    }
  },

  async updateBasicInfo(payload: {
    patientId: number
    name: string
    gender: string
    age: number | null
    idCard: string
    phone: string
    address: string
    familyHistory: string
  }): Promise<ApiResponse<any>> {
    try {
      const response = await request.post<any>('/api/patient/update-basic-info', payload)
      const data = (response as any)?.data ?? response
      return { success: data?.success !== false, data: data as any, message: data?.message }
    } catch (error: any) {
      const backendMsg = error?.response?.data?.message || error?.response?.data?.msg
      return { success: false, message: backendMsg || error.message || '更新失败' }
    }
  },

  async updateDiagnosis(payload: {
    patientId: number
    disease: string
    syndrome: string
    constitution: string
  }): Promise<ApiResponse<any>> {
    try {
      const response = await request.post<any>('/api/patient/update-diagnosis', payload)
      const data = (response as any)?.data ?? response
      return { success: data?.success !== false, data: data as any, message: data?.message }
    } catch (error: any) {
      const backendMsg = error?.response?.data?.message || error?.response?.data?.msg
      return { success: false, message: backendMsg || error.message || '更新失败' }
    }
  },

  async updateLifestyle(payload: {
    patientId: number
    lifestyle: string
    pastHistory: string
    familyHistory: string
  }): Promise<ApiResponse<any>> {
    try {
      const response = await request.post<any>('/api/patient/update-lifestyle', payload)
      const data = (response as any)?.data ?? response
      return { success: data?.success !== false, data: data as any, message: data?.message }
    } catch (error: any) {
      const backendMsg = error?.response?.data?.message || error?.response?.data?.msg
      return { success: false, message: backendMsg || error.message || '更新失败' }
    }
  },

  // 为患者注册小程序账号
  async registerMiniProgramAccount(data: {
    phone: string
    password: string
    name: string
    idCard?: string
  }): Promise<ApiResponse<any>> {
    try {
      const response = await request.post<any>('/api/patient/register-miniprogram', data)
      const result = (response as any)?.data ?? response
      return {
        success: result?.success !== false,
        data: result as any,
        message: result?.message
      }
    } catch (error: any) {
      return { success: false, message: error.message || '注册小程序账号失败' }
    }
  },

  // 中医四诊相关接口
  async createTCMDiagnosis(data: TCMDiagnosisData): Promise<ApiResponse<TCMDiagnosisData>> {
    try {
      const response = await request.post<any>('/api/tcm-diagnosis/create', data)
      // 检查response是否已经是处理过的格式
      if (response && typeof response === 'object' && typeof response.success === 'boolean') {
        return {
          success: response.success,
          data: response.data as TCMDiagnosisData,
          message: response.message
        }
      }
      // 否则，尝试从response.data中获取数据
      const result = (response as any)?.data ?? response
      return {
        success: result?.success !== false,
        data: result?.data as TCMDiagnosisData,
        message: result?.message
      }
    } catch (error: any) {
      return { success: false, message: error.message || '创建失败' }
    }
  },

  async updateTCMDiagnosis(data: TCMDiagnosisData): Promise<ApiResponse<TCMDiagnosisData>> {
    try {
      const response = await request.post<any>('/api/tcm-diagnosis/update', data)
      // 检查response是否已经是处理过的格式
      if (response && typeof response === 'object' && typeof response.success === 'boolean') {
        return {
          success: response.success,
          data: response.data as TCMDiagnosisData,
          message: response.message
        }
      }
      // 否则，尝试从response.data中获取数据
      const result = (response as any)?.data ?? response
      return {
        success: result?.success !== false,
        data: result?.data as TCMDiagnosisData,
        message: result?.message
      }
    } catch (error: any) {
      return { success: false, message: error.message || '更新失败' }
    }
  },

  async getLatestTCMDiagnosis(patientId: number): Promise<ApiResponse<TCMDiagnosisData>> {
    try {
      const response = await request.get<any>(`/api/tcm-diagnosis/latest/${patientId}`)
      // 检查response是否已经是处理过的格式
      if (response && typeof response === 'object' && typeof response.success === 'boolean') {
        return {
          success: response.success,
          data: response.data as TCMDiagnosisData,
          message: response.message
        }
      }
      // 否则，尝试从response.data中获取数据
      const result = (response as any)?.data ?? response
      return {
        success: result?.success !== false,
        data: result?.data as TCMDiagnosisData,
        message: result?.message
      }
    } catch (error: any) {
      return { success: false, message: error.message || '查询失败' }
    }
  },

  async getTCMDiagnosisList(patientId: number, limit?: number): Promise<ApiResponse<TCMDiagnosisData[]>> {
    try {
      const params = new URLSearchParams()
      if (limit) params.append('limit', limit.toString())
      const response = await request.get<any>(`/api/tcm-diagnosis/list/${patientId}?${params.toString()}`)
      const result = (response as any)?.data ?? response
      return {
        success: result?.success !== false,
        data: result?.data as TCMDiagnosisData[],
        message: result?.message
      }
    } catch (error: any) {
      return { success: false, message: error.message || '查询失败' }
    }
  },

  async deleteTCMDiagnosis(id: number): Promise<ApiResponse<any>> {
    try {
      const response = await request.delete<any>(`/api/tcm-diagnosis/delete/${id}`)
      const result = (response as any)?.data ?? response
      return {
        success: result?.success !== false,
        message: result?.message
      }
    } catch (error: any) {
      return { success: false, message: error.message || '删除失败' }
    }
  }
}

