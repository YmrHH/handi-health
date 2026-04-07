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


