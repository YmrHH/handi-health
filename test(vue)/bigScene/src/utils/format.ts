export function safeNum(v: any, fallback = 0) {
  const n = Number(v)
  return Number.isFinite(n) ? n : fallback
}

export function percent(n: number, d: number, digits = 1) {
  if (!d) return '—'
  return `${((n / d) * 100).toFixed(digits)}%`
}

export function pad2(n: number) {
  return String(n).padStart(2, '0')
}

export function formatDateTime(d: Date) {
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())} ${pad2(d.getHours())}:${pad2(
    d.getMinutes()
  )}:${pad2(d.getSeconds())}`
}

