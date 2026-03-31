import type { ReportTransformResult } from '@/utils/reportTransform'

function escapeCsvCell(v: any): string {
  const s = v == null ? '' : String(v)
  if (/[",\n\r]/.test(s)) return `"${s.replace(/"/g, '""')}"`
  return s
}

function formatCell(value: any, format?: 'int' | 'percent'): string {
  if (value == null) return ''
  const n = Number(value)
  if (format === 'percent') {
    if (!Number.isFinite(n)) return '0%'
    return `${(n * 100).toFixed(1)}%`
  }
  if (format === 'int') {
    if (!Number.isFinite(n)) return '0'
    return String(Math.round(n))
  }
  return String(value)
}

export function buildCsv(result: ReportTransformResult): string {
  const cols = result.columns
  const header = cols.map((c) => escapeCsvCell(c.label)).join(',')
  const lines: string[] = [header]
  for (const r of result.rows) {
    const rowCells = cols.map((c) => {
      if (c.key === 'label') return escapeCsvCell(r.label)
      const v = r.metrics[c.key]
      return escapeCsvCell(formatCell(v, c.format as any))
    })
    lines.push(rowCells.join(','))
  }
  return lines.join('\r\n')
}

export function downloadTextFile(fileName: string, content: string, mime = 'text/plain;charset=utf-8') {
  const blob = new Blob([content], { type: mime })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = fileName
  a.style.display = 'none'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

