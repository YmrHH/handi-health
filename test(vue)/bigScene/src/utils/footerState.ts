import { reactive } from 'vue'

export type FooterTone = 'success' | 'warning' | 'danger'

export type FooterState = {
  brand: string
  pageLabel: string
  statusText: string
  statusTone: FooterTone
  latencyMs: number | null
  securityText: string
  versionText: string
  updatedAt: string
  tickerItems: string[]
}

export const footerState = reactive<FooterState>({
  brand: '寒岐智护·慢性病随访健康预警管理平台',
  pageLabel: '总指挥舱',
  statusText: '运行状态：正常',
  statusTone: 'success',
  latencyMs: null,
  securityText: '高',
  versionText: '大屏端',
  updatedAt: '—',
  tickerItems: []
})

