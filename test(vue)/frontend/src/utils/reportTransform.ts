import type { ChartType, TimeGrain } from '@/utils/reportSchema'
import type { FollowupListRow } from '@/api/followup'
import type { PatientSummaryRow } from '@/api/patient'
import type { AlertCenterRow } from '@/api/alert'

export interface ReportQueryConfig {
  domain: 'followup' | 'patient' | 'alert'
  name: string
  startDate?: string
  endDate?: string
  grain: TimeGrain
  dimensionKey: string
  metricKeys: string[]
  chartType: ChartType
  sortBy: 'default' | 'metricDesc' | 'metricAsc'
  topN: number | 'all'
  showLabels: boolean
}

export interface ReportTransformRow {
  key: string
  label: string
  metrics: Record<string, number>
}

export interface ReportTransformResult {
  config: ReportQueryConfig
  generatedAt: string
  source: {
    recordCount: number
    pageCount: number
    capped: boolean
    cap: number
  }
  rows: ReportTransformRow[]
  columns: { key: string; label: string; format?: 'int' | 'percent' }[]
  chart: {
    suggestedType: Exclude<ChartType, 'auto'>
    categories: string[]
    series: { name: string; data: number[] }[]
  }
  metrics: Array<{ label: string; valueText: string; subText?: string }>
}

function pad2(n: number) {
  return n < 10 ? `0${n}` : String(n)
}

function toISODate(d: Date): string {
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`
}

function startOfWeek(date: Date): Date {
  const d = new Date(date)
  const day = d.getDay() // 0=Sun
  const diff = (day === 0 ? -6 : 1 - day) // Monday start
  d.setDate(d.getDate() + diff)
  d.setHours(0, 0, 0, 0)
  return d
}

function startOfMonth(date: Date): Date {
  const d = new Date(date)
  d.setDate(1)
  d.setHours(0, 0, 0, 0)
  return d
}

function bucketTime(dateStr: string, grain: TimeGrain): { key: string; label: string } {
  const d = new Date(dateStr)
  if (Number.isNaN(d.getTime())) return { key: String(dateStr || '未知'), label: String(dateStr || '未知') }
  if (grain === 'day') {
    const k = toISODate(d)
    return { key: k, label: k }
  }
  if (grain === 'week') {
    const s = startOfWeek(d)
    const k = toISODate(s)
    return { key: k, label: `${k} 周` }
  }
  const s = startOfMonth(d)
  const k = `${s.getFullYear()}-${pad2(s.getMonth() + 1)}`
  return { key: k, label: k }
}

function normalizeCategory(value: any): string {
  const v = value == null ? '' : String(value).trim()
  return v ? v : '未填写'
}

function parseDateSafe(value: any): Date | null {
  if (value == null || value === '') return null
  const d = new Date(String(value))
  return Number.isNaN(d.getTime()) ? null : d
}

function inRange(d: Date, start?: string, end?: string): boolean {
  if (!start && !end) return true
  const ds = start ? new Date(`${start}T00:00:00`) : null
  const de = end ? new Date(`${end}T23:59:59`) : null
  if (ds && !Number.isNaN(ds.getTime()) && d < ds) return false
  if (de && !Number.isNaN(de.getTime()) && d > de) return false
  return true
}

function isCompleted(resultStatus: string): boolean {
  const v = String(resultStatus || '').trim()
  if (!v) return false
  if (v.includes('完成')) return true
  if (v === 'DONE' || v === 'COMPLETED') return true
  return false
}

function isNoAnswer(resultStatus: string): boolean {
  const v = String(resultStatus || '').trim()
  if (!v) return false
  if (v.includes('未接') || v.includes('未通') || v.includes('无人接听')) return true
  if (v === 'NO_ANSWER') return true
  return false
}

function isNeedFollow(resultStatus: string): boolean {
  const v = String(resultStatus || '').trim()
  if (!v) return false
  if (v.includes('复访') || v.includes('需复')) return true
  if (v === 'NEED_FOLLOW') return true
  return false
}

function metricOrder(metricKeys: string[]) {
  return metricKeys.length ? metricKeys[0] : 'visitCount'
}

function formatInt(n: number): string {
  if (!Number.isFinite(n)) return '0'
  return String(Math.round(n))
}

function formatPercent(v: number): string {
  if (!Number.isFinite(v)) return '0%'
  return `${(v * 100).toFixed(1)}%`
}

export function transformFollowupReport(
  config: ReportQueryConfig,
  sourceRows: FollowupListRow[],
  meta: { pageCount: number; capped: boolean; cap: number }
): ReportTransformResult {
  const buckets = new Map<string, ReportTransformRow>()

  for (const r of sourceRows) {
    let dimKey = '未知'
    let dimLabel = '未知'

    if (config.dimensionKey === 'time') {
      const b = bucketTime((r as any).followupDate, config.grain)
      dimKey = b.key
      dimLabel = b.label
    } else if (config.dimensionKey === 'followupType') {
      dimKey = normalizeCategory((r as any).followupType)
      dimLabel = dimKey
    } else if (config.dimensionKey === 'resultStatus') {
      dimKey = normalizeCategory((r as any).resultStatus)
      dimLabel = dimKey
    } else if (config.dimensionKey === 'riskLevel') {
      dimKey = normalizeCategory((r as any).riskLevel)
      dimLabel = dimKey
    } else if (config.dimensionKey === 'staffName') {
      dimKey = normalizeCategory((r as any).staffName)
      dimLabel = dimKey
    } else {
      dimKey = '全部'
      dimLabel = '全部'
    }

    const existing = buckets.get(dimKey)
    const row =
      existing ??
      ({
        key: dimKey,
        label: dimLabel,
        metrics: {
          visitCount: 0,
          completedCount: 0,
          noAnswerCount: 0,
          needFollowCount: 0,
          completionRate: 0
        }
      } as ReportTransformRow)

    row.metrics.visitCount += 1
    const rs = String((r as any).resultStatus || '')
    if (isCompleted(rs)) row.metrics.completedCount += 1
    if (isNoAnswer(rs)) row.metrics.noAnswerCount += 1
    if (isNeedFollow(rs)) row.metrics.needFollowCount += 1
    buckets.set(dimKey, row)
  }

  const rows: ReportTransformRow[] = Array.from(buckets.values()).map((r) => {
    const total = r.metrics.visitCount || 0
    const completed = r.metrics.completedCount || 0
    const rate = total > 0 ? completed / total : 0
    return {
      ...r,
      metrics: {
        ...r.metrics,
        completionRate: rate
      }
    }
  })

  const orderMetric = metricOrder(config.metricKeys)
  if (config.sortBy === 'metricDesc') {
    rows.sort((a, b) => (b.metrics[orderMetric] ?? 0) - (a.metrics[orderMetric] ?? 0))
  } else if (config.sortBy === 'metricAsc') {
    rows.sort((a, b) => (a.metrics[orderMetric] ?? 0) - (b.metrics[orderMetric] ?? 0))
  } else if (config.dimensionKey === 'time') {
    rows.sort((a, b) => String(a.key).localeCompare(String(b.key)))
  } else {
    rows.sort((a, b) => String(a.label).localeCompare(String(b.label)))
  }

  const finalRows =
    config.topN === 'all' ? rows : rows.slice(0, Math.max(1, Math.min(Number(config.topN) || 5, 50)))

  const suggestedType: Exclude<ChartType, 'auto'> =
    config.chartType !== 'auto'
      ? (config.chartType as any)
      : config.dimensionKey === 'time'
        ? 'line'
        : 'bar'

  const categories = finalRows.map((r) => r.label)
  const series = config.metricKeys.map((mk) => ({
    name: mk,
    data: finalRows.map((r) => Number(r.metrics[mk] ?? 0) || 0)
  }))

  const totalVisits = sourceRows.length
  const completedTotal = sourceRows.reduce((acc, r) => acc + (isCompleted(String((r as any).resultStatus || '')) ? 1 : 0), 0)
  const completionRate = totalVisits > 0 ? completedTotal / totalVisits : 0
  const top1 = finalRows[0]
  const top1Metric = top1 ? (top1.metrics[orderMetric] ?? 0) : 0

  const columns = [
    { key: 'label', label: config.dimensionKey === 'time' ? '时间' : '分组' },
    ...config.metricKeys.map((k) => {
      const isRate = k === 'completionRate'
      return { key: k, label: k, format: isRate ? 'percent' : 'int' as any }
    })
  ]

  return {
    config,
    generatedAt: new Date().toISOString(),
    source: {
      recordCount: sourceRows.length,
      pageCount: meta.pageCount,
      capped: meta.capped,
      cap: meta.cap
    },
    rows: finalRows,
    columns,
    chart: {
      suggestedType,
      categories,
      series: config.metricKeys.map((k) => ({
        name: k,
        data: finalRows.map((r) => Number(r.metrics[k] ?? 0) || 0)
      }))
    },
    metrics: [
      { label: '记录数', valueText: formatInt(totalVisits) },
      { label: '完成数', valueText: formatInt(completedTotal) },
      { label: '完成率', valueText: formatPercent(completionRate), subText: `${formatInt(completedTotal)}/${formatInt(totalVisits)}` },
      top1 ? { label: 'Top 分组', valueText: String(top1.label), subText: `${orderMetric}: ${formatInt(Number(top1Metric) || 0)}` } : { label: 'Top 分组', valueText: '—' }
    ]
  }
}

function ageToBand(age: any): string {
  const n = Number(age)
  if (!Number.isFinite(n)) return '未知'
  if (n < 40) return '<40'
  if (n < 50) return '40-49'
  if (n < 60) return '50-59'
  if (n < 70) return '60-69'
  if (n < 80) return '70-79'
  return '80+'
}

export function transformPatientReport(
  config: ReportQueryConfig,
  sourceRows: PatientSummaryRow[],
  meta: { pageCount: number; capped: boolean; cap: number }
): ReportTransformResult {
  const buckets = new Map<string, ReportTransformRow>()

  for (const r of sourceRows) {
    let dimKey = '全部'
    let dimLabel = '全部'

    if (config.dimensionKey === 'riskLevel') {
      dimKey = normalizeCategory((r as any).riskLevel)
      dimLabel = dimKey
    } else if (config.dimensionKey === 'disease') {
      dimKey = normalizeCategory((r as any).disease)
      dimLabel = dimKey
    } else if (config.dimensionKey === 'syndrome') {
      dimKey = normalizeCategory((r as any).syndrome)
      dimLabel = dimKey
    } else if (config.dimensionKey === 'responsibleDoctor') {
      dimKey = normalizeCategory((r as any).responsibleDoctor)
      dimLabel = dimKey
    } else if (config.dimensionKey === 'gender') {
      dimKey = normalizeCategory((r as any).gender)
      dimLabel = dimKey
    } else if (config.dimensionKey === 'ageBand') {
      dimKey = ageToBand((r as any).age)
      dimLabel = dimKey
    }

    const existing = buckets.get(dimKey)
    const row =
      existing ??
      ({
        key: dimKey,
        label: dimLabel,
        metrics: {
          patientCount: 0,
          followupCountSum: 0,
          activeAlertCountSum: 0,
          pendingTaskCountSum: 0,
          avgLatestSbp: 0,
          avgLatestDbp: 0,
          __sbpN: 0,
          __dbpN: 0
        } as any
      } as ReportTransformRow)

    row.metrics.patientCount += 1
    row.metrics.followupCountSum += Number((r as any).followupCount ?? 0) || 0
    row.metrics.activeAlertCountSum += Number((r as any).activeAlertCount ?? 0) || 0
    row.metrics.pendingTaskCountSum += Number((r as any).pendingTaskCount ?? 0) || 0

    const sbp = (r as any).latestSbp
    const dbp = (r as any).latestDbp
    const sbpN = Number(sbp)
    const dbpN = Number(dbp)
    if (Number.isFinite(sbpN)) {
      ;(row.metrics as any).__sbpN += 1
      row.metrics.avgLatestSbp += sbpN
    }
    if (Number.isFinite(dbpN)) {
      ;(row.metrics as any).__dbpN += 1
      row.metrics.avgLatestDbp += dbpN
    }

    buckets.set(dimKey, row)
  }

  const rows: ReportTransformRow[] = Array.from(buckets.values()).map((r) => {
    const sbpN = Number((r.metrics as any).__sbpN ?? 0) || 0
    const dbpN = Number((r.metrics as any).__dbpN ?? 0) || 0
    const avgSbp = sbpN > 0 ? (Number(r.metrics.avgLatestSbp) || 0) / sbpN : 0
    const avgDbp = dbpN > 0 ? (Number(r.metrics.avgLatestDbp) || 0) / dbpN : 0
    const metrics = { ...r.metrics, avgLatestSbp: avgSbp, avgLatestDbp: avgDbp }
    delete (metrics as any).__sbpN
    delete (metrics as any).__dbpN
    return { ...r, metrics }
  })

  const orderMetric = metricOrder(config.metricKeys)
  if (config.sortBy === 'metricDesc') rows.sort((a, b) => (b.metrics[orderMetric] ?? 0) - (a.metrics[orderMetric] ?? 0))
  else if (config.sortBy === 'metricAsc') rows.sort((a, b) => (a.metrics[orderMetric] ?? 0) - (b.metrics[orderMetric] ?? 0))
  else rows.sort((a, b) => String(a.label).localeCompare(String(b.label)))

  const finalRows = config.topN === 'all' ? rows : rows.slice(0, Math.max(1, Math.min(Number(config.topN) || 5, 50)))
  const suggestedType: Exclude<ChartType, 'auto'> = config.chartType !== 'auto' ? (config.chartType as any) : 'bar'

  const total = sourceRows.length
  const sumMain = sourceRows.reduce((acc, r) => acc + (Number((r as any)[config.metricKeys[0]] ?? 0) || 0), 0)
  const top1 = finalRows[0]

  return {
    config,
    generatedAt: new Date().toISOString(),
    source: { recordCount: sourceRows.length, pageCount: meta.pageCount, capped: meta.capped, cap: meta.cap },
    rows: finalRows,
    columns: [
      { key: 'label', label: '分组' },
      ...config.metricKeys.map((k) => ({ key: k, label: k, format: k.startsWith('avg') ? 'int' : 'int' as any }))
    ],
    chart: {
      suggestedType,
      categories: finalRows.map((r) => r.label),
      series: config.metricKeys.map((k) => ({ name: k, data: finalRows.map((r) => Number(r.metrics[k] ?? 0) || 0) }))
    },
    metrics: [
      { label: '患者数', valueText: formatInt(total) },
      { label: '随访次数总和', valueText: formatInt(sourceRows.reduce((a, r) => a + (Number((r as any).followupCount ?? 0) || 0), 0)) },
      { label: '处理中告警数', valueText: formatInt(sourceRows.reduce((a, r) => a + (Number((r as any).activeAlertCount ?? 0) || 0), 0)) },
      top1 ? { label: 'Top 分组', valueText: String(top1.label), subText: `${orderMetric}: ${formatInt(Number(top1.metrics[orderMetric] ?? 0) || 0)}` } : { label: 'Top 分组', valueText: '—' }
    ]
  }
}

function isStatusNew(statusText: string): boolean {
  const v = String(statusText || '').trim().toUpperCase()
  return v === 'NEW' || v.includes('未处理')
}
function isStatusProcessing(statusText: string): boolean {
  const v = String(statusText || '').trim().toUpperCase()
  return v === 'PROCESSING' || v.includes('处理中')
}
function isStatusClosed(statusText: string): boolean {
  const v = String(statusText || '').trim().toUpperCase()
  return v === 'CLOSED' || v.includes('关闭') || v.includes('已处理')
}

export function transformAlertReport(
  config: ReportQueryConfig,
  sourceRows: AlertCenterRow[],
  meta: { pageCount: number; capped: boolean; cap: number }
): ReportTransformResult {
  // 先按日期范围过滤（告警接口不一定支持自定义起止，前端保证“不造假”）
  const filtered = sourceRows.filter((r) => {
    const d = parseDateSafe((r as any).firstTime) || parseDateSafe((r as any).lastTime)
    if (!d) return true
    return inRange(d, config.startDate, config.endDate)
  })

  const buckets = new Map<string, ReportTransformRow>()

  for (const r of filtered) {
    let dimKey = '全部'
    let dimLabel = '全部'

    if (config.dimensionKey === 'time') {
      const b = bucketTime(String((r as any).firstTime || (r as any).lastTime || ''), config.grain)
      dimKey = b.key
      dimLabel = b.label
    } else if (config.dimensionKey === 'alertType') {
      dimKey = normalizeCategory((r as any).alertType)
      dimLabel = dimKey
    } else if (config.dimensionKey === 'severityText') {
      dimKey = normalizeCategory((r as any).severityText)
      dimLabel = dimKey
    } else if (config.dimensionKey === 'statusText') {
      dimKey = normalizeCategory((r as any).statusText)
      dimLabel = dimKey
    } else if (config.dimensionKey === 'doctor') {
      dimKey = normalizeCategory((r as any).doctor)
      dimLabel = dimKey
    }

    const existing = buckets.get(dimKey)
    const row =
      existing ??
      ({
        key: dimKey,
        label: dimLabel,
        metrics: { alertCount: 0, newCount: 0, processingCount: 0, closedCount: 0, handledRate: 0 }
      } as ReportTransformRow)

    row.metrics.alertCount += 1
    const st = String((r as any).statusText || '')
    if (isStatusNew(st)) row.metrics.newCount += 1
    if (isStatusProcessing(st)) row.metrics.processingCount += 1
    if (isStatusClosed(st)) row.metrics.closedCount += 1
    buckets.set(dimKey, row)
  }

  const rows: ReportTransformRow[] = Array.from(buckets.values()).map((r) => {
    const total = r.metrics.alertCount || 0
    const closed = r.metrics.closedCount || 0
    const rate = total > 0 ? closed / total : 0
    return { ...r, metrics: { ...r.metrics, handledRate: rate } }
  })

  const orderMetric = metricOrder(config.metricKeys)
  if (config.sortBy === 'metricDesc') rows.sort((a, b) => (b.metrics[orderMetric] ?? 0) - (a.metrics[orderMetric] ?? 0))
  else if (config.sortBy === 'metricAsc') rows.sort((a, b) => (a.metrics[orderMetric] ?? 0) - (b.metrics[orderMetric] ?? 0))
  else if (config.dimensionKey === 'time') rows.sort((a, b) => String(a.key).localeCompare(String(b.key)))
  else rows.sort((a, b) => String(a.label).localeCompare(String(b.label)))

  const finalRows = config.topN === 'all' ? rows : rows.slice(0, Math.max(1, Math.min(Number(config.topN) || 5, 50)))

  const suggestedType: Exclude<ChartType, 'auto'> =
    config.chartType !== 'auto' ? (config.chartType as any) : config.dimensionKey === 'time' ? 'line' : 'bar'

  const totalAlerts = filtered.length
  const closedTotal = filtered.reduce((acc, r) => acc + (isStatusClosed(String((r as any).statusText || '')) ? 1 : 0), 0)
  const handledRate = totalAlerts > 0 ? closedTotal / totalAlerts : 0
  const top1 = finalRows[0]

  return {
    config,
    generatedAt: new Date().toISOString(),
    source: { recordCount: filtered.length, pageCount: meta.pageCount, capped: meta.capped, cap: meta.cap },
    rows: finalRows,
    columns: [
      { key: 'label', label: config.dimensionKey === 'time' ? '时间' : '分组' },
      ...config.metricKeys.map((k) => ({ key: k, label: k, format: k === 'handledRate' ? 'percent' : 'int' as any }))
    ],
    chart: {
      suggestedType,
      categories: finalRows.map((r) => r.label),
      series: config.metricKeys.map((k) => ({ name: k, data: finalRows.map((r) => Number(r.metrics[k] ?? 0) || 0) }))
    },
    metrics: [
      { label: '告警数', valueText: formatInt(totalAlerts) },
      { label: '已关闭', valueText: formatInt(closedTotal) },
      { label: '处理率', valueText: formatPercent(handledRate), subText: `${formatInt(closedTotal)}/${formatInt(totalAlerts)}` },
      top1 ? { label: 'Top 分组', valueText: String(top1.label), subText: `${orderMetric}: ${formatInt(Number(top1.metrics[orderMetric] ?? 0) || 0)}` } : { label: 'Top 分组', valueText: '—' }
    ]
  }
}

