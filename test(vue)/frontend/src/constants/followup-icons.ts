function withBase(pathFromPublicRoot: string) {
  // Vite：public/ 目录在运行时映射到 BASE_URL 下
  const base = (import.meta as any)?.env?.BASE_URL ?? '/'
  const b = String(base || '/')
  const prefix = b.endsWith('/') ? b : `${b}/`
  const p = pathFromPublicRoot.startsWith('/') ? pathFromPublicRoot.slice(1) : pathFromPublicRoot
  return `${prefix}${p}`
}

export const FOLLOWUP_ICONS = {
  // 注意：不要带 /public 前缀；public/ 在运行时映射到站点根
  dataManagement: withBase('icons/followup/01-data-dashboard.svg'),
  patientManagement: withBase('icons/followup/02-patient-record.svg'),
  warningManagement: withBase('icons/followup/03-alert-warning.svg'),
  followupManagement: withBase('icons/followup/04-followup-clipboard.svg'),
  systemManagement: withBase('icons/followup/05-system-shield-gear.svg'),
  platformLogo: withBase('icons/followup/06-platform-logo.svg'),
  followupTaskAssignTitle: withBase('icons/followup/07-page-followup-task-assign.svg')
} as const

