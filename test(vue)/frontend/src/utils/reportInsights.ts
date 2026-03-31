import type { ReportTransformResult } from '@/utils/reportTransform'

function pct(v: number): string {
  if (!Number.isFinite(v)) return '0.0%'
  return `${(v * 100).toFixed(1)}%`
}

export function buildInsights(result: ReportTransformResult, metricLabelMap?: Record<string, string>): string[] {
  const rows = result.rows || []
  const cfg = result.config
  if (rows.length === 0) return []

  const metricKey = cfg.metricKeys[0] || 'visitCount'
  const metricLabel = (metricLabelMap && metricLabelMap[metricKey]) ? metricLabelMap[metricKey] : metricKey
  const values = rows.map((r) => Number(r.metrics[metricKey] ?? 0) || 0)
  const sum = values.reduce((a, b) => a + b, 0)
  const max = Math.max(...values)
  const min = Math.min(...values)
  const maxIdx = values.indexOf(max)
  const minIdx = values.indexOf(min)

  const topRow = rows[maxIdx]
  const bottomRow = rows[minIdx]

  const insights: string[] = []

  if (topRow) {
    const share = sum > 0 ? max / sum : 0
    insights.push(`“${topRow.label}” 为当前最高分组，${metricLabel} 为 ${Math.round(max)}，占比约 ${pct(share)}。`)
  }

  if (bottomRow && rows.length >= 3) {
    insights.push(`最低分组为 “${bottomRow.label}”，${metricLabel} 为 ${Math.round(min)}，与最高分组差值 ${Math.round(max - min)}。`)
  }

  if (cfg.dimensionKey === 'time' && rows.length >= 3) {
    const tail = values.slice(-3)
    const trend = tail[2] - tail[0]
    if (trend > 0) insights.push(`最近 3 个时间点整体呈上升（+${Math.round(trend)}）。`)
    else if (trend < 0) insights.push(`最近 3 个时间点整体呈下降（${Math.round(trend)}）。`)
    else insights.push(`最近 3 个时间点整体较平稳。`)
  }

  // 完成率提示（如果选择了 completionRate）
  if (cfg.metricKeys.includes('completionRate')) {
    const avg =
      rows.reduce((acc, r) => acc + (Number(r.metrics.completionRate ?? 0) || 0), 0) / Math.max(1, rows.length)
    insights.push(`分组平均完成率约为 ${pct(avg)}。`)
  }

  return insights.slice(0, 4)
}

