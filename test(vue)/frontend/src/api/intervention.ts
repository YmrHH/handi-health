import request from './request'
import type { ApiResponse } from './types'

export interface InterventionRecommendFilter {
  bodyType?: string
  pattern?: string
  disease?: string
  season?: string
}

export interface InterventionRecommendRow {
  patientName: string
  bodyType: string
  pattern: string
  disease: string
  doctor: string
  recommendation: string
  riskLevel?: string // 风险等级
}

export interface InterventionRecommendData {
  bodyTypes: string[]
  patterns: string[]
  diseases: string[]
  bodyTypeParam: string | null
  patternParam: string | null
  diseaseParam: string | null
  seasonParam: string | null
  recommendList: InterventionRecommendRow[]
}

export interface VisitPlan {
  planId: number
  patientName: string
  disease: string
  visitType: string
  planDate: string
  doctor: string
  status: string
  statusText: string
  remark: string
}

export interface HomeServiceTask {
  taskId: number
  patientName: string
  address: string
  serviceType: string
  planDate: string
  assignee: string
  staffId?: number
  staffName?: string
  status: string
  statusText: string
  remark: string
}

export interface AssignStaffRequest {
  taskId: number
  staffId: number
}

export interface GenerateFollowupTasksRequest {
  patientId: number  // 必选：选择的患者ID
  serviceType: string  // 必选：服务类型
  planDate: string  // 必选：计划日期
  priority?: 'HIGH' | 'MEDIUM' | 'LOW'  // 可选：优先级，默认MEDIUM
  remark?: string  // 可选：备注
}

export interface GenerateFollowupTasksResponse {
  success: boolean
  task: HomeServiceTask  // 生成的单个任务
  message?: string
}

export interface UpdateVisitPlanStatusRequest {
  planId: number
  newStatus: string
}

export const interventionApi = {
  // 获取干预推荐数据
  async getRecommend(filter: InterventionRecommendFilter = {}): Promise<ApiResponse<InterventionRecommendData>> {
    try {
      const params = new URLSearchParams()
      if (filter.bodyType) params.append('bodyType', filter.bodyType)
      if (filter.pattern) params.append('pattern', filter.pattern)
      if (filter.disease) params.append('disease', filter.disease)
      if (filter.season) params.append('season', filter.season)

      const response = await request.get<any>(`/api/intervention/recommend?${params.toString()}`)
      const data = (response as any)?.data ?? response
      return {
        success: true,
        data: data as any
      }
    } catch (error: any) {
      return { success: false, message: error.message || '加载失败' }
    }
  },

  // 更新复查计划状态（下发/完成）
  async updateVisitPlanStatus(payload: UpdateVisitPlanStatusRequest): Promise<ApiResponse<any>> {
    try {
      const response = await request.post<any>('/api/intervention/visit-plan/update-status', payload)
      const data = (response as any)?.data ?? response
      return {
        success: data?.success !== false,
        data: data as any,
        message: data?.message || data?.msg
      }
    } catch (error: any) {
      return { success: false, message: error.message || '更新失败' }
    }
  },

  // 获取复诊计划数据
  async getVisitPlan(): Promise<ApiResponse<VisitPlan[]>> {
    try {
      const response = await request.get<any>('/api/intervention/visit-plan')
      const resData: any = response
      const payload = resData?.data ?? resData
      const plans = payload?.plans ?? payload
      const normalized: VisitPlan[] = Array.isArray(plans)
        ? (plans as any[]).map((row: any) => {
            const planId = Number(row?.planId ?? row?.id ?? 0)
            let planDate = String(row?.planDate ?? row?.visitDate ?? row?.dueAt ?? '')
            // 兼容后端可能返回 'YYYY-MM-DD HH:mm:ss'，浏览器 Date 解析不稳定
            if (planDate && planDate.includes(' ') && !planDate.includes('T')) {
              planDate = planDate.replace(' ', 'T')
            }
            const doctor = String(row?.doctor ?? row?.doctorName ?? '')
            const visitType = String(row?.visitType ?? row?.triggerType ?? '')
            const remark = String(row?.remark ?? '')

            return {
              planId,
              patientName: String(row?.patientName ?? ''),
              disease: String(row?.disease ?? ''),
              visitType,
              planDate,
              doctor,
              status: String(row?.status ?? ''),
              statusText: String(row?.statusText ?? ''),
              remark
            }
          })
        : ([] as any)
      return {
        success: true,
        data: normalized as any
      }
    } catch (error: any) {
      return { success: false, message: error.message || '加载失败' }
    }
  },

  // 获取居家服务数据
  async getHomeService(): Promise<ApiResponse<HomeServiceTask[]>> {
    try {
      const response = await request.get<any>('/api/intervention/home-service')
      const data = (response as any)?.data ?? response
      return {
        success: true,
        data: data as any
      }
    } catch (error: any) {
      return { success: false, message: error.message || '加载失败' }
    }
  },

  // 指派随访人员
  async assignStaff(assignRequest: AssignStaffRequest): Promise<ApiResponse> {
    try {
      const response = await request.post<any>('/api/intervention/home-service/assign', assignRequest)
      const data = (response as any)?.data ?? response
      return {
        success: data.success !== false,
        message: data.message || data.error
      }
    } catch (error: any) {
      return { success: false, message: error.message || '指派失败' }
    }
  },

  // 生成随访任务清单
  async generateFollowupTasks(generateRequest: GenerateFollowupTasksRequest): Promise<ApiResponse<GenerateFollowupTasksResponse>> {
    try {
      const response = await request.post<any>('/api/intervention/followup-tasks/generate', generateRequest)
      const resData: any = response
      const payload = resData?.data ?? resData
      const task = payload?.task ?? (Array.isArray(payload?.tasks) ? payload.tasks[0] : undefined)

      const normalized = {
        success: (payload?.success ?? resData?.success) !== false,
        task: task
      }

      return {
        success: normalized.success,
        data: normalized as any,
        message: payload?.message || payload?.msg || resData?.message
      }
    } catch (error: any) {
      return { success: false, message: error.message || '生成任务失败' }
    }
  }
}

// 生成随机地址
function generateRandomAddress(): string {
  const districts = ['南岗区', '道里区', '道外区', '香坊区', '平房区', '松北区']
  const streets = ['中山路', '和平路', '建设街', '学府路', '红旗大街', '长江路']
  const communities = ['阳光小区', '幸福家园', '和谐社区', '康乐花园', '温馨家园', '祥和苑']
  const buildings = ['1号楼', '2号楼', '3号楼', '5号楼', '8号楼', '12号楼']
  const units = ['1单元', '2单元', '3单元']
  const rooms = ['101室', '201室', '301室', '401室', '501室', '602室', '803室']
  
  const district = districts[Math.floor(Math.random() * districts.length)]
  const street = streets[Math.floor(Math.random() * streets.length)]
  const community = communities[Math.floor(Math.random() * communities.length)]
  const building = buildings[Math.floor(Math.random() * buildings.length)]
  const unit = units[Math.floor(Math.random() * units.length)]
  const room = rooms[Math.floor(Math.random() * rooms.length)]
  
  return `哈尔滨市${district}${street}${community}${building}${unit}${room}`
}

