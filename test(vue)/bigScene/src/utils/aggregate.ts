export function countBy<T>(list: T[], getKey: (it: T) => string) {
  const m: Record<string, number> = {}
  for (const it of list) {
    const k = (getKey(it) || '未填写').toString()
    m[k] = (m[k] || 0) + 1
  }
  return m
}

export function topEntries(map: Record<string, number>, topN: number) {
  return Object.entries(map)
    .sort((a, b) => b[1] - a[1])
    .slice(0, topN)
}

