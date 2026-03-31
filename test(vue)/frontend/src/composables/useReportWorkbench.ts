import { computed, ref, watch } from 'vue'
import { followupApi, type FollowupListRow } from '@/api/followup'
import { patientApi, type PatientSummaryRow } from '@/api/patient'
import { alertApi, type AlertCenterRow } from '@/api/alert'
import { getDomainSchema, type ReportDomain } from '@/utils/reportSchema'
import type { ChartType, TimeGrain } from '@/utils/reportSchema'
import type { ReportQueryConfig, ReportTransformResult } from '@/utils/reportTransform'
import { transformAlertReport, transformFollowupReport, transformPatientReport } from '@/utils/reportTransform'

export interface WorkbenchConfig {
  domain: ReportDomain
  name: string
  startDate: string
  endDate: string
  grain: TimeGrain
  dimensionKey: string
  metricKeys: string[]
  chartType: ChartType
  sortBy: 'default' | 'metricDesc' | 'metricAsc'
  topN: number | 'all'
  showLabels: boolean
}

function isoDate(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function defaultRangeDays(days: number) {
  const end = new Date()
  const start = new Date()
  start.setDate(end.getDate() - Math.max(1, days - 1))
  return { start: isoDate(start), end: isoDate(end) }
}

export function useReportWorkbench() {
  const range = defaultRangeDays(30)

  const config = ref<WorkbenchConfig>({
    domain: 'followup',
    name: '随访域 · 近30天 · 按时间趋势 · 指标：随访次数、完成数',
    startDate: range.start,
    endDate: range.end,
    grain: 'day',
    dimensionKey: 'time',
    metricKeys: ['visitCount', 'completedCount'],
    chartType: 'auto',
    sortBy: 'default',
    topN: 'all',
    showLabels: true
  })

  const schema = computed(() => getDomainSchema(config.value.domain))

  const loading = ref(false)
  const error = ref<string | null>(null)
  const rawRows = ref<FollowupListRow[]>([])
  const transform = ref<ReportTransformResult | null>(null)

  const lastGeneratedAt = computed(() => transform.value?.generatedAt || '')

  const canGenerate = computed(() => {
    if (!config.value.dimensionKey) return false
    if (!config.value.metricKeys || config.value.metricKeys.length === 0) return false
    if (schema.value.supportsTimeRange) {
      if (!config.value.startDate || !config.value.endDate) return false
    }
    return true
  })

  function recomputeName() {
    const domainLabel = schema.value.label
    const dimLabel = schema.value.dimensions.find((d) => d.key === config.value.dimensionKey)?.label || '维度'
    const metricLabels = config.value.metricKeys
      .map((k) => schema.value.metrics.find((m) => m.key === k)?.label || k)
      .join('、')
    const timePart = `${config.value.startDate} ~ ${config.value.endDate}`
    config.value.name = `${domainLabel} · ${timePart} · 按${dimLabel}分组 · 指标：${metricLabels}`
  }

  watch(
    () => [config.value.domain, config.value.startDate, config.value.endDate, config.value.dimensionKey, config.value.metricKeys.join(','), config.value.grain],
    () => recomputeName(),
    { immediate: true }
  )

  watch(
    () => config.value.domain,
    (domain) => {
      const s = getDomainSchema(domain)
      // 切换域后，自动对齐默认维度/指标/时间支持，避免出现空配置
      config.value = {
        ...config.value,
        domain,
        dimensionKey: s.defaultConfig.dimensionKey,
        metricKeys: [...s.defaultConfig.metricKeys],
        chartType: s.defaultConfig.chartType,
        grain: s.defaultConfig.grain,
        topN: s.defaultConfig.topN,
        sortBy: s.defaultConfig.sortBy,
        startDate: s.supportsTimeRange ? (config.value.startDate || defaultRangeDays(30).start) : '',
        endDate: s.supportsTimeRange ? (config.value.endDate || defaultRangeDays(30).end) : ''
      }
      transform.value = null
      rawRows.value = []
      error.value = null
    }
  )

  async function fetchFollowupRowsAll(params: { startDate: string; endDate: string }) {
    const cap = 5000
    const pageSizeGuess = 200
    const rows: FollowupListRow[] = []
    let pageNo = 1
    let pageCount = 0
    let capped = false

    while (true) {
      const resp = await followupApi.getList({
        startDate: params.startDate,
        endDate: params.endDate,
        pageNo
      })
      if (!resp.success) throw new Error(resp.message || '随访列表接口请求失败')
      const data: any = resp.data as any
      const list = (data?.rows || data?.list || []) as FollowupListRow[]
      const totalPages = Number(data?.totalPages || 0) || 0
      pageCount = Math.max(pageCount, pageNo)

      if (Array.isArray(list) && list.length) {
        rows.push(...list)
      }

      if (rows.length >= cap) {
        capped = true
        rows.length = cap
        break
      }

      if (totalPages && pageNo >= totalPages) break
      if (!totalPages && (!Array.isArray(list) || list.length < pageSizeGuess)) break
      pageNo += 1
      if (pageNo > 200) break
    }

    return { rows, pageCount, capped, cap }
  }

  async function fetchPatientRowsAll() {
    const cap = 5000
    const pageSize = 200
    const rows: PatientSummaryRow[] = []
    let pageNo = 1
    let pageCount = 0
    let capped = false

    while (true) {
      const resp = await patientApi.getPatientSummary({ pageNo, pageSize })
      if (!resp.success) throw new Error(resp.message || '患者汇总接口请求失败')
      const data: any = resp.data as any
      const list = (data?.rows || data?.list || []) as PatientSummaryRow[]
      const totalPages = Number(data?.totalPages || 0) || 0
      pageCount = Math.max(pageCount, pageNo)

      if (Array.isArray(list) && list.length) rows.push(...list)

      if (rows.length >= cap) {
        capped = true
        rows.length = cap
        break
      }

      if (totalPages && pageNo >= totalPages) break
      if (!totalPages && (!Array.isArray(list) || list.length < pageSize)) break
      pageNo += 1
      if (pageNo > 200) break
    }

    return { rows, pageCount, capped, cap }
  }

  async function fetchAlertRowsAll() {
    // 告警中心接口通常一次返回完整列表；我们仍然做 cap 截断
    const cap = 5000
    const resp = await alertApi.getAlerts({ range: 'ALL' })
    if (!resp.success) throw new Error(resp.message || '告警列表接口请求失败')
    const data: any = resp.data as any
    const list = (data?.rows || data?.list || []) as AlertCenterRow[]
    const rows = Array.isArray(list) ? list.slice(0, cap) : []
    const capped = Array.isArray(list) ? list.length > cap : false
    return { rows, pageCount: 1, capped, cap }
  }

  async function generate() {
    if (!canGenerate.value) return
    loading.value = true
    error.value = null
    transform.value = null
    rawRows.value = []

    try {
      const tcfg: ReportQueryConfig = {
        domain: config.value.domain,
        name: config.value.name,
        startDate: config.value.startDate,
        endDate: config.value.endDate,
        grain: config.value.grain,
        dimensionKey: config.value.dimensionKey,
        metricKeys: config.value.metricKeys,
        chartType: config.value.chartType,
        sortBy: config.value.sortBy,
        topN: config.value.topN,
        showLabels: config.value.showLabels
      }

      if (config.value.domain === 'followup') {
        if (!config.value.startDate || !config.value.endDate) throw new Error('请先选择时间范围')
        const fetched = await fetchFollowupRowsAll({ startDate: config.value.startDate, endDate: config.value.endDate })
        rawRows.value = fetched.rows as any
        transform.value = transformFollowupReport(tcfg, fetched.rows, {
          pageCount: fetched.pageCount,
          capped: fetched.capped,
          cap: fetched.cap
        })
      } else if (config.value.domain === 'patient') {
        const fetched = await fetchPatientRowsAll()
        rawRows.value = fetched.rows as any
        transform.value = transformPatientReport(tcfg, fetched.rows, {
          pageCount: fetched.pageCount,
          capped: fetched.capped,
          cap: fetched.cap
        })
      } else if (config.value.domain === 'alert') {
        // 告警域支持时间范围：优先前端用 firstTime 过滤（不依赖后端 range 能力）
        const fetched = await fetchAlertRowsAll()
        rawRows.value = fetched.rows as any
        transform.value = transformAlertReport(tcfg, fetched.rows, {
          pageCount: fetched.pageCount,
          capped: fetched.capped,
          cap: fetched.cap
        })
      }
    } catch (e: any) {
      error.value = e?.message || '生成报表失败'
    } finally {
      loading.value = false
    }
  }

  function reset() {
    const s = getDomainSchema(config.value.domain)
    const r = defaultRangeDays(30)
    config.value = {
      domain: config.value.domain,
      name: '',
      startDate: s.supportsTimeRange ? r.start : '',
      endDate: s.supportsTimeRange ? r.end : '',
      grain: s.defaultConfig.grain,
      dimensionKey: s.defaultConfig.dimensionKey,
      metricKeys: [...s.defaultConfig.metricKeys],
      chartType: s.defaultConfig.chartType,
      sortBy: s.defaultConfig.sortBy,
      topN: s.defaultConfig.topN,
      showLabels: true
    }
    transform.value = null
    rawRows.value = []
    error.value = null
  }

  return {
    schema,
    config,
    loading,
    error,
    rawRows,
    transform,
    lastGeneratedAt,
    canGenerate,
    generate,
    reset
  }
}

