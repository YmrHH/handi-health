export type ReportDomain = 'followup' | 'patient' | 'alert'

export type TimeGrain = 'day' | 'week' | 'month'

export type ChartType = 'auto' | 'bar' | 'line' | 'horizontalBar' | 'pie'

export interface DimensionDef {
  key: string
  label: string
  kind: 'category' | 'time'
  supportsGrain?: boolean
}

export interface MetricDef {
  key: string
  label: string
  kind: 'count' | 'rate'
  format?: 'int' | 'percent'
}

export interface DomainSchema {
  domain: ReportDomain
  label: string
  supportsTimeRange: boolean
  timeFieldLabel?: string
  dimensions: DimensionDef[]
  metrics: MetricDef[]
  defaultConfig: {
    dimensionKey: string
    metricKeys: string[]
    chartType: ChartType
    grain: TimeGrain
    topN: number | 'all'
    sortBy: 'default' | 'metricDesc' | 'metricAsc'
  }
}

export const reportSchemas: Record<ReportDomain, DomainSchema> = {
  followup: {
    domain: 'followup',
    label: '随访域',
    supportsTimeRange: true,
    timeFieldLabel: '随访日期',
    dimensions: [
      { key: 'time', label: '时间', kind: 'time', supportsGrain: true },
      { key: 'followupType', label: '随访类型', kind: 'category' },
      { key: 'resultStatus', label: '随访结果', kind: 'category' },
      { key: 'riskLevel', label: '风险等级', kind: 'category' },
      { key: 'staffName', label: '随访人员', kind: 'category' }
    ],
    metrics: [
      { key: 'visitCount', label: '随访次数', kind: 'count', format: 'int' },
      { key: 'completedCount', label: '完成数', kind: 'count', format: 'int' },
      { key: 'noAnswerCount', label: '未接通数', kind: 'count', format: 'int' },
      { key: 'needFollowCount', label: '需复访数', kind: 'count', format: 'int' },
      { key: 'completionRate', label: '完成率', kind: 'rate', format: 'percent' }
    ],
    defaultConfig: {
      dimensionKey: 'time',
      metricKeys: ['visitCount', 'completedCount'],
      chartType: 'auto',
      grain: 'day',
      topN: 'all',
      sortBy: 'default'
    }
  },
  patient: {
    domain: 'patient',
    label: '患者域',
    supportsTimeRange: false,
    dimensions: [
      { key: 'riskLevel', label: '风险等级', kind: 'category' },
      { key: 'disease', label: '病种', kind: 'category' },
      { key: 'syndrome', label: '证型', kind: 'category' },
      { key: 'responsibleDoctor', label: '责任医生', kind: 'category' },
      { key: 'gender', label: '性别', kind: 'category' },
      { key: 'ageBand', label: '年龄段', kind: 'category' }
    ],
    metrics: [
      { key: 'patientCount', label: '患者数', kind: 'count', format: 'int' },
      { key: 'followupCountSum', label: '随访次数总和', kind: 'count', format: 'int' },
      { key: 'activeAlertCountSum', label: '处理中告警数', kind: 'count', format: 'int' },
      { key: 'pendingTaskCountSum', label: '待随访任务数', kind: 'count', format: 'int' },
      { key: 'avgLatestSbp', label: '平均收缩压', kind: 'count', format: 'int' },
      { key: 'avgLatestDbp', label: '平均舒张压', kind: 'count', format: 'int' }
    ],
    defaultConfig: {
      dimensionKey: 'riskLevel',
      metricKeys: ['patientCount', 'pendingTaskCountSum'],
      chartType: 'auto',
      grain: 'day',
      topN: 'all',
      sortBy: 'default'
    }
  },
  alert: {
    domain: 'alert',
    label: '告警域',
    supportsTimeRange: true,
    timeFieldLabel: '首次时间',
    dimensions: [
      { key: 'time', label: '时间', kind: 'time', supportsGrain: true },
      { key: 'alertType', label: '告警类型', kind: 'category' },
      { key: 'severityText', label: '告警级别', kind: 'category' },
      { key: 'statusText', label: '处置状态', kind: 'category' },
      { key: 'doctor', label: '责任医生', kind: 'category' }
    ],
    metrics: [
      { key: 'alertCount', label: '告警数', kind: 'count', format: 'int' },
      { key: 'newCount', label: '未处理数', kind: 'count', format: 'int' },
      { key: 'processingCount', label: '处理中数', kind: 'count', format: 'int' },
      { key: 'closedCount', label: '已关闭数', kind: 'count', format: 'int' },
      { key: 'handledRate', label: '处理率', kind: 'rate', format: 'percent' }
    ],
    defaultConfig: {
      dimensionKey: 'time',
      metricKeys: ['alertCount', 'closedCount'],
      chartType: 'auto',
      grain: 'day',
      topN: 'all',
      sortBy: 'default'
    }
  }
}

export function getDomainSchema(domain: ReportDomain): DomainSchema {
  return reportSchemas[domain]
}

