import axios from 'axios'

// 仿照 frontend/src/api/request.ts 的做法：统一 axios 实例 + 头部鉴权
export const http = axios.create({
  baseURL: '',
  timeout: 30000,
  withCredentials: true
})

http.interceptors.request.use((config) => {
  try {
    if (typeof sessionStorage !== 'undefined') {
      // 如果主系统已经登录，会有 token；否则给大屏指定一个只读 userId（比如 1），由后端按需识别
      let token = sessionStorage.getItem('token')
      if (!token) {
        token = '1'
        sessionStorage.setItem('token', token)
      }
      config.headers = (config.headers || {}) as any
      ;(config.headers as any)['Authorization'] = `Bearer ${token}`
      ;(config.headers as any)['X-User-Id'] = String(token)
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

// 登录（与主系统保持同样接口）
export async function loginByPhone(phone: string, password: string) {
  const res = await http.post('/api/auth/loginByPhone', { phone, password })
  const data = (res as any)?.data ?? res
  return data as any
}


