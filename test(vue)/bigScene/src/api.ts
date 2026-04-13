import axios from 'axios'

// 仿照 frontend/src/api/request.ts 的做法：统一 axios 实例 + 头部鉴权
export const http = axios.create({
  baseURL: 'http://192.140.173.165:8081',
  timeout: 30000,
  withCredentials: true
})

http.interceptors.request.use((config) => {
  try {
    if (typeof sessionStorage !== 'undefined') {
      const token = sessionStorage.getItem('token')
      const userId = sessionStorage.getItem('userId') || sessionStorage.getItem('xUserId')
      config.headers = (config.headers || {}) as any
      if (token) {
        ;(config.headers as any)['Authorization'] = `Bearer ${token}`
      }
      if (userId) {
        ;(config.headers as any)['X-User-Id'] = String(userId)
      }
    }
  } catch {
    // ignore
  }
  return config
})

type CacheEntry<T> = {
  promise: Promise<T>
  createdAt: number
  ttlMs: number
}

const memCache = new Map<string, CacheEntry<any>>()

async function cached<T>(
  key: string,
  fetcher: () => Promise<T>,
  opts?: {
    ttlMs?: number
  }
) {
  const ttlMs = Math.max(0, Number(opts?.ttlMs ?? 0))
  const hit = memCache.get(key) as CacheEntry<T> | undefined
  const now = Date.now()
  if (hit) {
    if (!hit.ttlMs) return await hit.promise
    if (now - hit.createdAt <= hit.ttlMs) return await hit.promise
    memCache.delete(key)
  }
  const promise = fetcher().catch((err) => {
    memCache.delete(key)
    throw err
  })
  memCache.set(key, { promise, createdAt: now, ttlMs })
  return await promise
}

export function clearApiCache() {
  memCache.clear()
}

export async function fetchReportBoard() {
  return await cached('reportBoard', async () => {
    const res = await http.get('/api/report/board')
    return (res.data as any)?.data ?? res.data
  }, { ttlMs: 30_000 })
}

export async function fetchMonthSummary() {
  return await cached('monthSummary', async () => {
    const res = await http.get('/api/report/month-summary')
    return (res.data as any)?.data ?? res.data
  }, { ttlMs: 60_000 })
}

export async function fetchHomeStats() {
  return await cached('homeStats', async () => {
    const res = await http.get('/api/home/stats')
    return (res.data as any)?.data ?? res.data
  }, { ttlMs: 15_000 })
}

export async function fetchAlerts(range = 30) {
  return await cached(`alerts:${range}`, async () => {
    const res = await http.get(`/api/alert/alerts?status=&type=&range=${range}`)
    return (res.data as any)?.data ?? res.data
  }, { ttlMs: 10_000 })
}

export async function fetchHardwareAlerts(range = 30) {
  return await cached(`hardwareAlerts:${range}`, async () => {
    const res = await http.get(`/api/alert/hardware?status=&deviceType=&range=${range}`)
    return (res.data as any)?.data ?? res.data
  }, { ttlMs: 10_000 })
}

export async function fetchPatientRiskList(pageSize = 2000) {
  return await cached(`patientRiskList:page1:${pageSize}`, async () => {
    const res = await http.get(`/api/patient/risk-list?level=&pageNo=1&pageSize=${pageSize}`)
    return (res.data as any)?.data ?? res.data
  }, { ttlMs: 60_000 })
}

export async function fetchPatientSummary(pageSize = 200) {
  return await cached(`patientSummary:${pageSize}`, async () => {
    const res = await http.get(`/api/patient/summary?pageNo=1&pageSize=${pageSize}`)
    return (res.data as any)?.data ?? res.data
  }, { ttlMs: 60_000 })
}

/** 患者画像（大屏左侧排行二级聚合用） */
export async function fetchPatientProfile(riskId: number | string) {
  const id = String(riskId ?? '').trim()
  return await cached(`patientProfile:${id}`, async () => {
    const res = await http.get('/api/patient/profile', { params: { riskId: id } })
    return (res.data as any)?.data ?? res.data
  }, { ttlMs: 120_000 })
}

// 登录（与主系统保持同样接口）
export async function loginByPhone(phone: string, password: string) {
  const res = await http.post('/api/auth/loginByPhone', { phone, password })
  const data = (res as any)?.data ?? res
  return data as any
}

function pickRowsLike(input: any): any[] {
  if (Array.isArray(input)) return input
  const arr =
    input?.rows ??
    input?.list ??
    input?.records ??
    input?.items ??
    input?.data?.rows ??
    input?.data?.list ??
    input?.data?.records ??
    input?.result?.rows ??
    input?.result?.list ??
    input?.result?.records ??
    []
  return Array.isArray(arr) ? arr : []
}

function normStatus(raw: any): string {
  const s = String(raw ?? '').trim().toUpperCase()
  if (!s) return ''
  if (['PENDING', 'TODO', 'NEW', 'WAITING', 'ASSIGNED'].includes(s)) return 'PENDING'
  if (['IN_PROGRESS', 'PROCESSING', 'DOING'].includes(s)) return 'IN_PROGRESS'
  if (['DONE', 'COMPLETED', 'FINISHED', 'SUCCESS', 'SENT', 'ARRIVED'].includes(s)) return 'DONE'
  if (['OVERDUE', 'TIMEOUT', 'EXPIRED'].includes(s)) return 'OVERDUE'
  return s
}

export async function fetchInterventionBoardData() {
  return await cached('interventionBoard', async () => {
    const [homeServiceRes, visitPlanRes, recommendRes, staffRes, followTaskRes] = await Promise.allSettled([
      http.get('/api/intervention/home-service'),
      http.get('/api/intervention/visit-plan'),
      http.get('/api/intervention/recommend/list'),
      http.get('/api/followup/staff/list?pageNo=1&pageSize=200'),
      http.get('/api/followup/tasks?page=1&pageSize=200')
    ])

    const homeRows = homeServiceRes.status === 'fulfilled' ? pickRowsLike((homeServiceRes.value.data as any)?.data ?? homeServiceRes.value.data) : []
    const planRows = visitPlanRes.status === 'fulfilled' ? pickRowsLike((visitPlanRes.value.data as any)?.data ?? visitPlanRes.value.data) : []
    const recRows = recommendRes.status === 'fulfilled' ? pickRowsLike((recommendRes.value.data as any)?.data ?? recommendRes.value.data) : []
    const staffRows = staffRes.status === 'fulfilled' ? pickRowsLike((staffRes.value.data as any)?.data ?? staffRes.value.data) : []
    const followRows = followTaskRes.status === 'fulfilled' ? pickRowsLike((followTaskRes.value.data as any)?.data ?? followTaskRes.value.data) : []

    const taskRows = [...homeRows, ...followRows]
    const total = taskRows.length
    const pending = taskRows.filter((r) => {
      const s = normStatus(r?.status ?? r?.statusText)
      return s === 'PENDING'
    }).length
    const inProgress = taskRows.filter((r) => normStatus(r?.status ?? r?.statusText) === 'IN_PROGRESS').length
    const done = taskRows.filter((r) => normStatus(r?.status ?? r?.statusText) === 'DONE').length
    const overdue = taskRows.filter((r) => normStatus(r?.status ?? r?.statusText) === 'OVERDUE').length

    const typeMap = new Map<string, number>()
    for (const row of taskRows) {
      const key = String(row?.serviceType ?? row?.followupType ?? row?.visitType ?? row?.triggerType ?? '其他任务').trim() || '其他任务'
      typeMap.set(key, (typeMap.get(key) || 0) + 1)
    }
    const typeDist = Array.from(typeMap.entries())
      .sort((a, b) => b[1] - a[1])
      .slice(0, 8)
      .map(([name, value]) => ({ name, value }))

    const hotMap = new Map<string, number>()
    for (const row of planRows) {
      const key = String(row?.visitType ?? row?.triggerType ?? row?.serviceType ?? '综合随访').trim() || '综合随访'
      hotMap.set(key, (hotMap.get(key) || 0) + 1)
    }
    const hotPlans = Array.from(hotMap.entries())
      .sort((a, b) => b[1] - a[1])
      .slice(0, 5)
      .map(([name, value]) => ({ name, value }))

    const areaMap = new Map<string, number>()
    for (const row of homeRows) {
      const address = String(row?.address ?? '').trim()
      const area = address ? address.replace(/^.*?(市|区|县)/, '$1') : '未知区域'
      areaMap.set(area, (areaMap.get(area) || 0) + 1)
    }
    const areaDist = Array.from(areaMap.entries())
      .sort((a, b) => b[1] - a[1])
      .slice(0, 6)
      .map(([name, value]) => ({ name, value }))

    const now = new Date()
    const trend = Array.from({ length: 7 }).map((_, idx) => {
      const d = new Date(now)
      d.setDate(now.getDate() - (6 - idx))
      const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
      const c = taskRows.filter((row) => String(row?.planDate ?? row?.planTime ?? row?.dueAt ?? row?.createdAt ?? '').includes(key)).length
      return { label: `${d.getMonth() + 1}/${d.getDate()}`, value: c }
    })

    const sent = recRows.filter((r) => normStatus(r?.status ?? r?.statusText) === 'DONE').length
    const recTotal = recRows.length
    const sendRate = recTotal > 0 ? Math.round((sent / recTotal) * 100) : 0

    const events = homeRows
      .slice(0, 12)
      .map((r: any, i: number) => ({
        id: r?.taskId ?? r?.id ?? `evt-${i}`,
        title: `${r?.patientName ?? '患者'} · ${r?.serviceType ?? r?.visitType ?? '随访任务'} · ${r?.statusText ?? r?.status ?? '待处理'}`,
        time: String(r?.planDate ?? r?.createdAt ?? '').slice(0, 16) || '实时'
      }))

    return {
      overview: { total, done, pending, inProgress, overdue },
      typeDist,
      hotPlans,
      areaDist,
      trend,
      reachRate: sendRate,
      staffCount: staffRows.length,
      events
    }
  }, { ttlMs: 20_000 })
}


