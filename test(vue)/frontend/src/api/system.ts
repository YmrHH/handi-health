import request from './request'
import type { ApiResponse } from './types'

export interface OrgDepartment {
  hospitalArea: string
  deptName: string
  leaderName: string
}

export interface OrgUserAccount {
  userName: string
  loginName: string
  deptName: string
  roleName: string
  status: string
}

export interface SystemOrgUserData {
  deptList: OrgDepartment[]
  userList: OrgUserAccount[]
}

export interface RuleRiskFollowup {
  riskLevel: string
  scoreRange: string
  defaultDay: number
  remark: string
}

export interface RuleAlertStrategy {
  ruleItem: string
  setting: string
}

export interface RuleSeasonFactor {
  factorName: string
  weightDesc: string
}

export interface RuleVisitPlan {
  planId: number
  disease: string
  visitFreq: string
  description: string
}

export interface SystemRulesData {
  riskRules: RuleRiskFollowup[]
  alertRules: RuleAlertStrategy[]
  seasonRules: RuleSeasonFactor[]
  visitRules: RuleVisitPlan[]
}

export interface DataDictionaryEntry {
  dictId: number
  dictType: string
  dictKey: string
  dictValue: string
  description: string
}

export interface OperationLogEntry {
  logTime: string
  userName: string
  action: string
  detail: string
}

export interface DoctorRecommendation {
  date: string
  doctor: string
  title: string
  description: string
  patients?: string[]
}

export interface DataAnalysis {
  date: string
  type: 'patient' | 'followup' | 'alert' | 'plan' | 'other'
  title: string
  summary: string
  metrics?: Array<{
    label: string
    value: string
  }>
}

export interface ModificationLog {
  id: number
  type: 'followup' | 'patient' | 'alert' | 'plan' | 'user' | 'other'
  operator: string
  time: string
  action: string
  target: string
  beforeValue?: string
  afterValue?: string
  description?: string
}

export interface SystemDataLogData {
  dictList?: DataDictionaryEntry[]
  logList?: OperationLogEntry[]
  doctorRecommendations?: DoctorRecommendation[]
  dataAnalysis?: DataAnalysis[]
  modificationLogs?: ModificationLog[]
}

export interface DoctorAdviceRow {
  id: number
  adviceDate?: string | null
  doctorName?: string
  title: string
  description?: string
  patients?: string[]
}

export interface DoctorAdviceListData {
  total: number
  rows: DoctorAdviceRow[]
  page: number
  pageSize: number
}

export const systemApi = {
  // 获取组织用户数据
  async getOrgUser(): Promise<ApiResponse<SystemOrgUserData>> {
    try {
      const response = await request.get<SystemOrgUserData>('/api/system/org-user')
      const data = (response as any)?.data ?? response
      return {
        success: true,
        data: data as any
      }
    } catch (error: any) {
      return { success: false, message: error.message || '加载失败' }
    }
  },

  // 获取规则配置数据
  async getRules(): Promise<ApiResponse<SystemRulesData>> {
    try {
      // 规则数据量较大，单次查询可能超过默认30s，提升超时时间
      const response = await request.get<SystemRulesData>('/api/system/rules', { timeout: 120000 })
      const data = (response as any)?.data ?? response
      return {
        success: true,
        data: data as any
      }
    } catch (error: any) {
      return { success: false, message: error.message || '加载失败' }
    }
  },

  // 获取数据日志数据
  async getDataLog(): Promise<ApiResponse<SystemDataLogData>> {
    try {
      // 数据与日志查询可能较慢，同样提升超时时间
      const response = await request.get<SystemDataLogData>('/api/system/data-log', { timeout: 120000 })
      const data = (response as any)?.data ?? response
      return {
        success: true,
        data: data as any
      }
    } catch (error: any) {
      return { success: false, message: error.message || '加载失败' }
    }
  },

  async getDoctorAdviceList(params: {
    page?: number
    pageSize?: number
    keyword?: string
    startDate?: string
    endDate?: string
  } = {}): Promise<ApiResponse<DoctorAdviceListData>> {
    try {
      const response = await request.get<any>('/api/system/doctor-advice', { params })
      const layer1: any = (response as any)?.data ?? response
      return {
        success: layer1?.success !== false,
        data: layer1 as any,
        message: layer1?.message || layer1?.error
      }
    } catch (error: any) {
      return { success: false, message: error.message || '加载失败' }
    }
  },

  async getDoctorAdviceDetail(id: number | string): Promise<ApiResponse<DoctorAdviceRow>> {
    try {
      const response = await request.get<any>('/api/system/doctor-advice/detail', { params: { id } })
      const layer1: any = (response as any)?.data ?? response
      const data: any = layer1?.data ?? layer1
      return {
        success: layer1?.success !== false,
        data: data as any,
        message: layer1?.message || layer1?.error
      }
    } catch (error: any) {
      return { success: false, message: error.message || '加载失败' }
    }
  }
}

