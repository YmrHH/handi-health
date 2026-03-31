export type FrontendAuditType = 'api' | 'route' | 'action'

export interface FrontendAuditLog {
  id: string
  type: FrontendAuditType
  time: string
  user?: string | null
  page?: string
  routeFrom?: string
  routeTo?: string
  action?: string
  method?: string
  url?: string
  status?: number
  success?: boolean
  durationMs?: number
  requestId?: string
  detail?: any
}

const STORAGE_KEY = 'frontend_audit_logs_v1'
const MAX_LOGS = 1000

function safeJsonParse<T>(raw: string | null, fallback: T): T {
  if (!raw) return fallback
  try {
    return JSON.parse(raw) as T
  } catch {
    return fallback
  }
}

function generateId(): string {
  return `${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 10)}`
}

export function listFrontendAuditLogs(): FrontendAuditLog[] {
  const logs = safeJsonParse<FrontendAuditLog[]>(localStorage.getItem(STORAGE_KEY), [])
  if (!Array.isArray(logs)) return []
  return logs
}

export function clearFrontendAuditLogs(): void {
  localStorage.removeItem(STORAGE_KEY)
}

export function exportFrontendAuditLogs(): string {
  const logs = listFrontendAuditLogs()
  return JSON.stringify(logs, null, 2)
}

export function recordFrontendAuditLog(partial: Omit<FrontendAuditLog, 'id' | 'time'> & { time?: string }): void {
  const now = new Date()
  const log: FrontendAuditLog = {
    id: generateId(),
    time: partial.time || now.toISOString(),
    ...partial
  }

  const logs = listFrontendAuditLogs()
  logs.unshift(log)
  if (logs.length > MAX_LOGS) {
    logs.length = MAX_LOGS
  }
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(logs))
  } catch {
    try {
      localStorage.removeItem(STORAGE_KEY)
      localStorage.setItem(STORAGE_KEY, JSON.stringify([log]))
    } catch {
      // ignore
    }
  }
}
