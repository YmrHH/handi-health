import request from './request'
import type { ApiResponse } from './types'

export interface ReportBoardData {
  latestMonth: string
  latestNewPatients: number
  latestAlerts: number
  latestFollowups: number
  latestHighRisk: number
  latestMidRisk: number
  latestLowRisk: number
  latestAuc: number
  latestF1: number
  alertChangeRate: number
  highRiskChangeRate: number
  months: string[]
  alertsArr: number[]
  followArr: number[]
  highArr: number[]
  otherArr: number[]
  aucArr: number[]
  f1Arr: number[]
  sourceNames: string[]
  sourceCounts: number[]
  // 病种分析数据
  diseaseAnalysis?: DiseaseAnalysisData[]
}

// 康养计划效果
export interface CarePlanEffectiveness {
  planName: string // 康养计划名称
  planType: string // 计划类型（如：饮食控制、运动康复、用药管理、中医调理等）
  patientCount: number // 使用该计划的患者数
  effectiveness: number // 效果评分 (0-100)
  stabilityImprovement: number // 稳定性提升百分比
  avgImprovementDays: number // 平均改善天数
}

// 病情加重因素
export interface DeteriorationFactor {
  factorName: string // 因素名称
  impactLevel: 'high' | 'medium' | 'low' // 影响程度
  patientCount: number // 受影响患者数
  description: string // 因素描述
}

// 患者画像特征
export interface PatientProfile {
  ageRange: string // 年龄范围，如 "50-65岁"
  diseaseStage: string // 病情阶段，如 "早期"、"中期"、"晚期"
  constitution: string // 体质特点，如 "气虚质"、"痰湿质"、"阴虚质"
  comorbidities: string[] // 合并症列表，如 ["高血压", "高血脂"]
  lifestyle: string[] // 生活方式特征，如 ["久坐", "高盐饮食", "缺乏运动"]
  typicalCount: number // 符合该画像的典型患者数量
}

// 病种分析数据
export interface DiseaseAnalysisData {
  disease: string // 病种名称
  patientCount: number // 患者数量
  stableRate: number // 病情稳定率 (0-1)
  improvementRate: number // 病情改善率 (0-1)
  deteriorationRate: number // 病情恶化率 (0-1)
  // 康养计划效果分析
  carePlanEffectiveness: CarePlanEffectiveness[]
  // 病情加重因素
  deteriorationFactors: DeteriorationFactor[]
  // 医生建议
  recommendations: string[]
  // 典型患者画像（最常见的患者特征组合）
  typicalProfiles?: PatientProfile[]
}

export interface ReportGroup {
  id: string
  title: string
  open: boolean
  reports: ReportItem[]
}

export interface ReportItem {
  id: string
  name: string
  desc: string
  columns: string[]
  rows: any[]
}

export interface ReportExportParams {
  reportId: string
  timeRange: string
  dept: string
  format: string
}

export interface ReportPreviewParams {
  reportId: string
  timeRange: string
  dept: string
}

export const reportApi = {
  // 获取报告看板数据
  async getBoard(): Promise<ApiResponse<ReportBoardData>> {
    try {
      const response = await request.get<any>('/api/report/board')
      // request.ts 会把非统一格式包成 { success: true, data: xxx }
      // 这里统一解包，确保页面拿到的是后端真实结构
      const data = (response as any)?.data ?? response
      return {
        success: true,
        data: data as any
      }
    } catch (error: any) {
      return { success: false, message: error.message || '加载失败' }
    }
  },

  // 获取报表预览数据（随筛选条件变化）
  async previewReport(params: ReportPreviewParams): Promise<ApiResponse<ReportItem>> {
    try {
      const qs = new URLSearchParams()
      qs.append('reportId', params.reportId)
      qs.append('timeRange', params.timeRange)
      qs.append('dept', params.dept)
      const response = await request.get<any>(`/api/report/preview?${qs.toString()}`)
      const data = (response as any)?.data ?? response
      return { success: true, data: data as any }
    } catch (error: any) {
      return { success: false, message: error.message || '加载失败' }
    }
  },

  // 获取报表类型列表
  async getReportTypes(): Promise<ApiResponse<ReportGroup[]>> {
    try {
      const response = await request.get<any>('/api/report/types')
      const data = (response as any)?.data ?? response
      return {
        success: true,
        data: Array.isArray(data) ? data : []
      }
    } catch (error: any) {
      return { success: false, message: error.message || '加载失败', data: [] }
    }
  },

  // 导出报表（后端生成文件 + 返回下载地址）
  async exportReport(params: ReportExportParams): Promise<void> {
    const response = await request.post<any>('/api/report/export', {
      reportId: params.reportId,
      timeRange: params.timeRange,
      dept: params.dept,
      format: params.format
    })
    const payload = (response as any)?.data ?? response
    const downloadUrl = payload?.downloadUrl as string | undefined
    const fileName = (payload?.fileName as string | undefined) || `${params.reportId}.csv`
    if (!downloadUrl) {
      throw new Error('后端未返回下载地址')
    }

    // axios baseURL 可能为空字符串，因此优先使用相对路径即可
    const a = document.createElement('a')
    a.href = downloadUrl
    a.download = fileName
    a.style.display = 'none'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
  }
}

