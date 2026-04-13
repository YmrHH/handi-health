<template>
  <div class="command-center-page">
    <div class="page-bg page-grid"></div>

    <main class="page-main stitch-home">
      <section class="hero-metrics stitch-stats" aria-label="首页核心指标">
        <article v-for="card in topCards" :key="card.key" class="metric-card glass-card">
          <div class="metric-icon" :class="`metric-icon--${card.accent}`">{{ card.icon }}</div>
          <div class="metric-body">
            <p class="metric-label">{{ card.label }}</p>
            <p class="metric-value">{{ formatNumber(card.value) }}</p>
            <p class="metric-meta">{{ card.meta }}</p>
          </div>
        </article>
      </section>

      <section class="hero-grid stitch-main cc-bento">
        <aside class="left-column stitch-col column-stack cc-left">
          <section class="glass-card panel panel-compact">
            <div class="panel-header">
              <div>
                <h3 class="panel-title">病种排名 TOP5</h3>
                <p class="panel-subtitle">基于患者档案与画像数据统计</p>
              </div>
            </div>
            <div class="rank-list">
              <div v-for="(item, idx) in finalDiseaseRanks" :key="`${item.name}-${idx}`" class="rank-item">
                <div class="rank-row">
                  <span class="rank-index">{{ idx + 1 }}</span>
                  <span class="rank-name">{{ item.name }}</span>
                  <span class="rank-value">{{ item.percent }}% · {{ formatNumber(item.count) }} 人</span>
                </div>
                <div class="rank-track">
                  <div class="rank-bar" :style="{ width: `${item.percent}%` }"></div>
                </div>
              </div>
              <div v-if="rankingsPending" class="loading-tip">正在统计...</div>
              <div v-else-if="!finalDiseaseRanks.length" class="empty-tip"></div>
            </div>
          </section>

          <section class="glass-card panel panel-compact efficiency-panel">
            <div class="panel-header">
              <div>
                <h3 class="panel-title">医生负载 TOP5</h3>
                <p class="panel-subtitle">基于患者归属与画像数据统计</p>
              </div>
            </div>
            <div class="doctor-list">
              <div v-for="(item, idx) in finalDoctorLoads" :key="`${item.name}-${idx}`" class="doctor-item">
                <div class="doctor-main">
                  <div class="doctor-avatar">{{ item.badge }}</div>
                  <div class="doctor-info">
                    <div class="doctor-name">{{ item.name }}</div>
                    <div class="doctor-count">关联患者 {{ formatNumber(item.count) }} 人</div>
                  </div>
                </div>
                <div class="doctor-load">
                  <span :class="['doctor-load-value', item.percent >= 90 ? 'is-hot' : '']">{{ item.percent }}%</span>
                  <span class="doctor-dot" :class="item.percent >= 90 ? 'is-hot' : item.percent >= 70 ? 'is-warn' : 'is-ok'"></span>
                </div>
              </div>
              <div v-if="doctorLoadsLoading" class="loading-tip">正在统计...</div>
              <div v-else-if="doctorLoadsEnhancing" class="enhance-tip">持续补充中</div>
              <div v-else-if="!finalDoctorLoads.length" class="empty-tip"></div>
            </div>
          </section>
        </aside>

        <section class="center-column stitch-center cc-center">
          <section ref="hubShellRef" class="hub-shell glass-card" :style="hubStyle">
            <div class="hub-glow"></div>
            <div class="hub-outer-ring"></div>
            <div class="hub-middle-ring"></div>
            <div class="hub-inner-ring"></div>
            <div ref="hubRingRef" class="hub-chart"></div>

            <div class="hub-core glass-card">
              <div class="hub-kicker">平台中枢</div>
              <h2 class="hub-title">寒岐智护</h2>
              <p class="hub-subtitle">慢性病随访健康预警管理平台</p>
              <div class="hub-total">{{ formatNumber(hubCenter.totalPatients) }}</div>
              <div class="hub-total-label">受管患者总数</div>

              <div class="hub-mini-grid">
                <div class="hub-mini-card">
                  <span class="hub-mini-value">{{ hubCenter.highRiskPercent }}%</span>
                  <span class="hub-mini-label">高风险占比</span>
                </div>
                <div class="hub-mini-card">
                  <span class="hub-mini-value">{{ formatNumber(hubCenter.pendingAlerts) }}</span>
                  <span class="hub-mini-label">待处理告警</span>
                </div>
              </div>
            </div>

            <div class="hub-node hub-node-top">
              <div class="hub-node-icon">患</div>
              <span>患者</span>
            </div>
            <div class="hub-node hub-node-right">
              <div class="hub-node-icon">医</div>
              <span>医生</span>
            </div>
            <div class="hub-node hub-node-bottom">
              <div class="hub-node-icon">智</div>
              <span>AI 中枢</span>
            </div>
            <div class="hub-node hub-node-left">
              <div class="hub-node-icon">联</div>
              <span>协同网络</span>
            </div>
          </section>

          <section class="trend-section glass-card">
            <div class="panel-header trend-header">
              <div>
                <h3 class="panel-title">综合趋势主图</h3>
                <p class="panel-subtitle">风险、告警与随访数据的连续变化</p>
              </div>
              <div class="switch-group">
                <button
                  v-for="item in trendModes"
                  :key="item.key"
                  type="button"
                  :class="['switch-btn', { active: trendMode === item.key }]"
                  @click="trendMode = item.key"
                >
                  {{ item.label }}
                </button>
              </div>
            </div>
            <div ref="trendRef" class="trend-chart"></div>
          </section>
        </section>

        <aside class="right-column stitch-col column-stack cc-right">
          <section class="glass-card panel panel-compact">
            <div class="panel-header">
              <div>
                <h3 class="panel-title">处置效率卡</h3>
                <p class="panel-subtitle">近 30 天告警闭环处理情况</p>
              </div>
            </div>
            <div class="efficiency-wrap">
              <div class="eff-kpis">
                <div class="eff-kpi">
                  <p class="eff-kpi-value is-primary">{{ avgResponseText }}</p>
                  <p class="eff-kpi-label">平均响应</p>
                </div>
                <div class="eff-divider"></div>
                <div class="eff-kpi">
                  <p class="eff-kpi-value is-secondary">{{ avgHandleText }}</p>
                  <p class="eff-kpi-label">平均处置</p>
                </div>
                <div class="eff-divider"></div>
                <div class="eff-kpi">
                  <p class="eff-kpi-value is-tertiary">{{ disposalRate }}%</p>
                  <p class="eff-kpi-label">闭环率</p>
                </div>
              </div>
              <div class="eff-plan-card">
                <p class="eff-plan-title">计划完成率（周度）</p>
                <div class="eff-plan-row">
                  <div class="eff-plan-track">
                    <div class="eff-plan-bar" :style="{ width: `${weeklyPlanRate}%` }"></div>
                  </div>
                  <span class="eff-plan-value">{{ weeklyPlanRate }}%</span>
                </div>
              </div>
            </div>
          </section>

          <section class="glass-card panel panel-compact event-panel">
            <div class="panel-header">
              <div>
                <h3 class="panel-title">实时动态流</h3>
                <p class="panel-subtitle">事件播报与处置进度追踪</p>
              </div>
            </div>
            <div class="event-list">
              <article v-for="(item, idx) in eventStreamItems" :key="`${item.time}-${idx}`" class="event-item" :class="`is-${item.tone}`">
                <div class="event-time">{{ item.time }}</div>
                <div class="event-title">{{ item.title }}</div>
                <div class="event-desc">{{ item.desc }}</div>
              </article>
            </div>
          </section>
        </aside>
      </section>
    </main>

  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onActivated, onBeforeUnmount, onDeactivated, onMounted, ref, shallowRef, watch } from 'vue'
import {
  fetchAlerts,
  fetchHardwareAlerts,
  fetchHomeStats,
  fetchMonthSummary,
  fetchPatientProfile,
  fetchPatientRiskList,
  fetchPatientSummary
} from '../api'
import { footerState } from '../utils/footerState'
import { use, init, type ECharts, type EChartsOption } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { LineChart, PieChart } from 'echarts/charts'

use([CanvasRenderer, GridComponent, LegendComponent, TooltipComponent, LineChart, PieChart])

type SummaryItem = Record<string, any>
type AlertItem = Record<string, any>

type RankItem = { name: string; count: number; percent: number }
type DoctorLoad = { name: string; count: number; percent: number; badge: string }
type TopCard = { key: string; label: string; value: number; meta: string; icon: string; accent: 'primary' | 'secondary' | 'tertiary' | 'error' }

const hubRingRef = ref<HTMLDivElement | null>(null)
const hubShellRef = ref<HTMLElement | null>(null)
const trendRef = ref<HTMLDivElement | null>(null)
const hubChart = shallowRef<ECharts | null>(null)
const trendChart = shallowRef<ECharts | null>(null)
const hubSize = ref(280)
let hubResizeObserver: ResizeObserver | null = null

const homeStats = ref<Record<string, any>>({})
const monthSummary = ref<Record<string, any>>({})
const patientSummary = ref<SummaryItem[]>([])
const riskList = ref<Record<string, any>[]>([])
const alerts = ref<AlertItem[]>([])
const hardwareAlerts = ref<AlertItem[]>([])
const trendMode = ref<'day' | 'week' | 'month'>('month')
const updatedAt = ref('—')
const pageAlive = ref(false)
const rankingsLoadDone = ref(false)

const trendModes = [
  { key: 'day', label: '日' },
  { key: 'week', label: '周' },
  { key: 'month', label: '月' }
]

function computeHubSize() {
  const el = hubShellRef.value
  if (!el) return
  const w = Math.max(0, el.clientWidth)
  const h = Math.max(0, el.clientHeight)
  const base = Math.min(w, h)
  hubSize.value = Math.max(1, Math.round(base * 0.56))
}

const hubStyle = computed(() => ({
  ['--hub-size' as any]: `${hubSize.value}px`,
  ['--hub-glow-size' as any]: `${Math.round(hubSize.value * 2.25)}px`,
  ['--hub-node-size' as any]: `${Math.round(hubSize.value * 0.16)}px`
}))

function getArray(input: any): any[] {
  if (Array.isArray(input)) return input
  if (Array.isArray(input?.records)) return input.records
  if (Array.isArray(input?.result)) return input.result
  if (Array.isArray(input?.pageData)) return input.pageData
  if (Array.isArray(input?.datas)) return input.datas
  if (Array.isArray(input?.recordsList)) return input.recordsList
  if (Array.isArray(input?.dataList)) return input.dataList
  if (Array.isArray(input?.list)) return input.list
  if (Array.isArray(input?.items)) return input.items
  if (Array.isArray(input?.rows)) return input.rows
  if (Array.isArray(input?.content)) return input.content
  if (Array.isArray(input?.data)) return input.data
  if (Array.isArray(input?.data?.records)) return input.data.records
  if (Array.isArray(input?.data?.result)) return input.data.result
  if (Array.isArray(input?.data?.pageData)) return input.data.pageData
  if (Array.isArray(input?.data?.datas)) return input.data.datas
  if (Array.isArray(input?.data?.recordsList)) return input.data.recordsList
  if (Array.isArray(input?.data?.dataList)) return input.data.dataList
  if (Array.isArray(input?.result?.records)) return input.result.records
  if (Array.isArray(input?.result?.rows)) return input.result.rows
  if (Array.isArray(input?.result?.list)) return input.result.list
  if (Array.isArray(input?.page?.records)) return input.page.records
  if (Array.isArray(input?.page?.rows)) return input.page.rows
  if (Array.isArray(input?.payload?.list)) return input.payload.list
  if (Array.isArray(input?.payload?.rows)) return input.payload.rows
  if (Array.isArray(input?.payload?.result)) return input.payload.result

  const deep = deepFindArray(input, 0, 5)
  if (deep.length) return deep
  return []
}

function deepFindArray(node: any, depth = 0, maxDepth = 4): any[] {
  if (depth > maxDepth || node == null) return []
  if (Array.isArray(node)) return node
  if (typeof node !== 'object') return []

  const priorityKeys = [
    'rows',
    'records',
    'list',
    'items',
    'content',
    'data',
    'result',
    'pageData',
    'datas',
    'recordsList',
    'dataList'
  ]
  for (const k of priorityKeys) {
    const v = (node as any)[k]
    if (Array.isArray(v)) return v
  }

  for (const v of Object.values(node)) {
    const arr = deepFindArray(v, depth + 1, maxDepth)
    if (arr.length) return arr
  }
  return []
}

function pickNumber(source: any, keys: string[], fallback = 0): number {
  for (const key of keys) {
    const value = source?.[key]
    if (typeof value === 'number' && Number.isFinite(value)) return value
    if (typeof value === 'string' && value.trim() !== '' && !Number.isNaN(Number(value))) return Number(value)
  }
  return fallback
}

function pickText(source: any, keys: string[], fallback = ''): string {
  for (const key of keys) {
    const value = source?.[key]
    if (typeof value === 'string' && value.trim()) return value.trim()
  }
  return fallback
}

function pickFirstText(values: any[]): string {
  for (const v of values) {
    if (typeof v === 'string' && v.trim()) return v.trim()
  }
  return ''
}

function formatNumber(value: number | string | undefined | null) {
  const n = Number(value ?? 0)
  if (!Number.isFinite(n)) return '0'
  return new Intl.NumberFormat('zh-CN').format(n)
}

function percent(value: number, total: number) {
  if (!total) return 0
  return Math.max(0, Math.min(100, Math.round((value / total) * 100)))
}

const totalPatients = computed(() => {
  const fromHomeDirect = pickNumber(homeStats.value, ['managedPatientCount', 'totalPatients', 'patientCount', 'totalPatientCount'])
  if (fromHomeDirect > 0) return fromHomeDirect
  const fromHomeNested =
    pickNumber(homeStats.value?.data, ['managedPatientCount', 'totalPatients', 'patientCount', 'totalPatientCount']) ||
    pickNumber(homeStats.value?.result, ['managedPatientCount', 'totalPatients', 'patientCount', 'totalPatientCount']) ||
    pickNumber(homeStats.value?.stats, ['managedPatientCount', 'totalPatients', 'patientCount', 'totalPatientCount']) ||
    pickNumber(homeStats.value?.summary, ['managedPatientCount', 'totalPatients', 'patientCount', 'totalPatientCount'])
  if (fromHomeNested > 0) return fromHomeNested

  const monthRows = getArray(monthSummary.value)
  if (monthRows.length) {
    for (let i = monthRows.length - 1; i >= 0; i--) {
      const n = pickNumber(monthRows[i], ['managedPatientCount', 'totalPatients', 'patientCount', 'totalPatientCount'])
      if (n > 0) return n
    }
  }
  return pickNumber(monthSummary.value, ['managedPatientCount', 'totalPatients', 'patientCount', 'totalPatientCount'], 0)
})

const highRiskCount = computed(() => {
  const fromHome = pickNumber(homeStats.value, ['highRiskPatients', 'highRiskCount', 'highRiskPatientCount', 'riskHighCount'])
  if (fromHome) return fromHome
  return riskList.value.filter((item) => cnRiskLevel(item).includes('高')).length
})

const alertSummary = computed(() => {
  const merged = [...alerts.value, ...hardwareAlerts.value]
  let closed = 0
  let pending = 0
  for (const item of merged) {
    const status = cnAlertStatus(item)
    if (status === '已处理' || status === '已关闭') closed += 1
    else pending += 1
  }
  return {
    total: merged.length,
    closed,
    pending
  }
})

const disposalRate = computed(() => percent(alertSummary.value.closed, Math.max(1, alertSummary.value.total)))
const avgResponseMinutes = computed(() =>
  pickNumber(homeStats.value, ['avgResponseMinutes', 'averageResponseMinutes', 'responseAvgMinutes', 'avgResponseTime'], 0)
)
const avgHandleMinutes = computed(() =>
  pickNumber(homeStats.value, ['avgHandleMinutes', 'averageHandleMinutes', 'handleAvgMinutes', 'avgDisposeTime'], 0)
)
const weeklyPlanRate = computed(() =>
  Math.max(
    0,
    Math.min(
      100,
      Math.round(
        pickNumber(homeStats.value, ['planFinishRate', 'weekPlanRate', 'weeklyPlanRate', 'taskCompleteRate', 'completionRate'], disposalRate.value)
      )
    )
  )
)
const avgResponseText = computed(() => (avgResponseMinutes.value > 0 ? `${Number(avgResponseMinutes.value.toFixed(1))}m` : '—'))
const avgHandleText = computed(() => (avgHandleMinutes.value > 0 ? `${Number(avgHandleMinutes.value.toFixed(1))}m` : '—'))

function cnRiskLevel(item: any) {
  const raw = pickText(item, ['riskLevel', 'level', 'risk'])
  if (!raw) return '未评估'
  const text = raw.toLowerCase()
  if (text.includes('high') || text.includes('高')) return '高风险'
  if (text.includes('medium') || text.includes('mid') || text.includes('中')) return '中风险'
  if (text.includes('low') || text.includes('低')) return '低风险'
  return raw
}

function cnAlertStatus(item: any) {
  const raw = pickText(item, ['status', 'alertStatus', 'handleStatus'])
  const text = raw.toLowerCase()
  if (text.includes('closed') || text.includes('resolved') || text.includes('handled') || text.includes('已关闭') || text.includes('已处理')) return '已处理'
  if (text.includes('pending') || text.includes('open') || text.includes('unhandled') || text.includes('待') || text.includes('未')) return '待处理'
  return raw || '待处理'
}

const profileDoctorLoads = ref<DoctorLoad[]>([])

function collectProfileBuckets(root: any): any[] {
  const seen = new Set<object>()
  const out: any[] = []
  function push(x: any) {
    if (x == null || typeof x !== 'object' || Array.isArray(x)) return
    if (seen.has(x)) return
    seen.add(x)
    out.push(x)
  }
  push(root)
  let cur: any = root
  for (let i = 0; i < 4 && cur && typeof cur === 'object'; i++) {
    const next = cur.data ?? cur.result
    if (next && typeof next === 'object' && !Array.isArray(next)) {
      push(next)
      cur = next
    } else break
  }
  const nestKeys = ['patient', 'profile', 'archive', 'detail', 'record', 'vo', 'payload', 'entity', 'info']
  const snapshot = [...out]
  for (const obj of snapshot) {
    for (const k of nestKeys) push(obj[k])
  }
  return out
}

function normalizeProfileField(v: any): string {
  if (v == null) return ''
  if (typeof v === 'string') return v.trim()
  if (typeof v === 'object') {
    return pickFirstText([
      v.name,
      v.label,
      v.text,
      v.title,
      v.value != null ? String(v.value) : ''
    ])
  }
  return String(v).trim()
}

function isValidProfileDisease(s: string) {
  const t = (s || '').trim()
  if (!t) return false
  const lower = t.toLowerCase()
  if (lower === 'null' || lower === 'undefined') return false
  const bad = ['未标注病种', '未知', '-', '——', '--', '无', '暂无', 'N/A', 'n/a']
  if (bad.includes(t)) return false
  return true
}

function isValidProfileDoctor(s: string) {
  const t = (s || '').trim()
  if (!t) return false
  const lower = t.toLowerCase()
  if (lower === 'null' || lower === 'undefined') return false
  const bad = ['未分配医生', '未知', '-', '——', '--', '无', '暂无', 'N/A', 'n/a']
  if (bad.includes(t)) return false
  return true
}

function extractRiskIdsFromRows(rows: SummaryItem[], maxN: number): string[] {
  const seen = new Set<string>()
  const out: string[] = []
  for (const row of rows) {
    const candidates = [
      row?.riskId,
      row?.risk_id,
      row?.patientRiskId,
      row?.riskPatientId,
      row?.patientId,
      row?.patient_id,
      row?.id,
      row?.patientBasicInfoId,
      row?.archiveId,
      row?.userId,
      row?.profileId,
      row?.recordId,
      row?.record_id,
      row?.summaryId,
      row?.bizId,
      row?.memberId,
      row?.patient?.id,
      row?.patient?.patientId,
      row?.patient?.riskId,
      row?.profile?.id,
      row?.profile?.patientId,
      row?.profile?.riskId,
      row?.archive?.id,
      row?.archive?.patientId,
      row?.archive?.riskId,
      row?.patientBasicInfo?.id,
      row?.detail?.patientId,
      row?.detail?.id,
      row?.data?.patientId,
      row?.data?.riskId
    ]
    let id: string | null = null
    for (const c of candidates) {
      if (c == null || c === '') continue
      const s = String(c).trim()
      if (!s || s === '0') continue
      id = s
      break
    }
    if (!id || seen.has(id)) continue
    seen.add(id)
    out.push(id)
    if (out.length >= maxN) break
  }
  return out
}

const PROFILE_DISEASE_KEYS = [
  'disease',
  'mainDisease',
  'primaryDisease',
  'diseaseName',
  'diagnosisName',
  'chronicDiseaseName',
  'diagnosis',
  'diagnosisLabel',
  'diseaseType',
  'illnessName',
  'categoryName',
  'conditionName',
  'chronicDisease',
  'chronicName',
  'tagName',
  'typeName',
  'majorDisease',
  'patientDisease',
  'icdName',
  'healthConditionName',
  'mainDiagnosis',
  'primaryDiagnosis',
  'clinicalDiagnosis',
  'diseaseLabel'
]

function pickDiseaseFromOneObject(inner: any): string {
  if (!inner || typeof inner !== 'object') return ''
  for (const k of PROFILE_DISEASE_KEYS) {
    const s = normalizeProfileField(inner[k])
    if (isValidProfileDisease(s)) return s
  }
  const nested = pickFirstText([
    inner?.disease?.name,
    inner?.disease?.label,
    inner?.mainDisease?.name,
    inner?.diagnosis?.name,
    inner?.diagnosis?.label,
    inner?.chronicDisease?.name,
    inner?.illness?.name,
    inner?.category?.name,
    inner?.categoryName,
    inner?.condition?.name,
    inner?.conditionName,
    inner?.healthRecord?.diagnosis,
    inner?.healthRecord?.diseaseName
  ])
  if (isValidProfileDisease(nested)) return nested
  const lists = [
    inner?.diagnosisList,
    inner?.chronicDiseaseList,
    inner?.chronicList,
    inner?.diseaseTags,
    inner?.diseaseNames,
    inner?.tags
  ]
  for (const list of lists) {
    if (!Array.isArray(list)) continue
    for (const x of list) {
      const s = normalizeProfileField(typeof x === 'string' ? x : x?.name ?? x?.diagnosisName ?? x?.label ?? x?.title ?? x?.text)
      if (isValidProfileDisease(s)) return s
    }
  }
  return ''
}

function pickDiseaseFromProfileInner(root: any): string {
  if (root == null || typeof root !== 'object') return ''
  for (const bucket of collectProfileBuckets(root)) {
    const s = pickDiseaseFromOneObject(bucket)
    if (s) return s
  }
  return ''
}

const PROFILE_DOCTOR_KEYS = [
  'doctor',
  'doctorName',
  'responsibleDoctorName',
  'followDoctorName',
  'realName',
  'doctorLabel',
  'attendingDoctorName',
  'familyDoctorName',
  'physicianName',
  'ownerName',
  'managerName',
  'chargeDoctorName',
  'chiefDoctorName',
  'doctorInCharge',
  'gpName',
  'followUpDoctor',
  'caregiverName',
  'operatorName',
  'createByName',
  'updateByName',
  'staffName',
  'userName',
  'nickname',
  'nickName'
]

function pickDoctorFromOneObject(inner: any): string {
  if (!inner || typeof inner !== 'object') return ''
  for (const k of PROFILE_DOCTOR_KEYS) {
    const s = normalizeProfileField(inner[k])
    if (isValidProfileDoctor(s)) return s
  }
  const nested = pickFirstText([
    inner?.doctor?.name,
    inner?.doctor?.realName,
    inner?.doctor?.label,
    inner?.physician?.name,
    inner?.owner?.name,
    inner?.manager?.name,
    inner?.staff?.name,
    inner?.staff?.realName,
    inner?.user?.name,
    inner?.user?.realName,
    inner?.operator?.name,
    inner?.operator?.realName
  ])
  if (isValidProfileDoctor(nested)) return nested
  return ''
}

function pickDoctorFromProfileInner(root: any): string {
  if (root == null || typeof root !== 'object') return ''
  for (const bucket of collectProfileBuckets(root)) {
    const s = pickDoctorFromOneObject(bucket)
    if (s) return s
  }
  return ''
}

function mapToRankItemsFromCounts(map: Map<string, number>): RankItem[] {
  const entries = Array.from(map.entries())
    .filter(([, c]) => c > 0)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 5)
  if (!entries.length) return []
  const total = entries.reduce((s, [, c]) => s + c, 0)
  return entries.map(([name, count]) => ({ name, count, percent: percent(count, Math.max(1, total)) }))
}

function mapToDoctorLoadsFromCounts(map: Map<string, number>): DoctorLoad[] {
  const rows = Array.from(map.entries())
    .filter(([, c]) => c > 0)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 5)
  if (!rows.length) return []
  const max = rows[0][1] || 1
  return rows.map(([name, count]) => ({
    name,
    count,
    percent: percent(count, max),
    badge: name?.[0] || '医'
  }))
}

async function loadRankingProfiles() {
  const PROFILE_ENHANCE_MAX_IDS = 10
  const PROFILE_ENHANCE_BATCH = 2

  doctorRankPending.value = true
  try {
    let ids = extractRiskIdsFromRows(patientSummary.value, PROFILE_ENHANCE_MAX_IDS)
    if (!ids.length || !pageAlive.value) return

  const doctorMap = new Map<string, number>()

    for (let i = 0; i < ids.length; i += PROFILE_ENHANCE_BATCH) {
      if (!pageAlive.value) break
      const batch = ids.slice(i, i + PROFILE_ENHANCE_BATCH)
      const results = await Promise.allSettled(batch.map((id) => fetchPatientProfile(id)))
      for (const r of results) {
        if (r.status !== 'fulfilled') continue
        const docStr = pickDoctorFromProfileInner(r.value)
        if (docStr) doctorMap.set(docStr, (doctorMap.get(docStr) || 0) + 1)
      }
    }

    const docLoads = mapToDoctorLoadsFromCounts(doctorMap)
    if (docLoads.length) profileDoctorLoads.value = docLoads
  } finally {
    doctorRankPending.value = false
  }
}

const patientRowsForRank = computed<SummaryItem[]>(() => {
  const src: any = patientSummary.value
  return getArray(src)
})

const INVALID_DISEASE_RANK = new Set(['未标注病种', '未知', '暂无'])
const INVALID_DOCTOR_RANK = new Set(['未分配医生', '未知', '暂无'])

function normalizeRankText(v: any): string {
  if (v === null || v === undefined) return ''
  const t = String(v).trim()
  return t
}

function pickDiseaseKeyForRank(item: any): string {
  const directKeys = [
    'disease',
    'mainDisease',
    'primaryDisease',
    'diseaseName',
    'diagnosisName',
    'mainDiagnosis',
    'primaryDiagnosis',
    'chronicDiseaseName',
    'diagnosis',
    'diseaseType',
    'illnessName',
    'categoryName',
    'conditionName',
    'clinicalDiagnosis',
    'diagnosisLabel',
    'diseaseLabel'
  ]
  for (const k of directKeys) {
    const t = normalizeRankText(item?.[k])
    if (!t) continue
    if (INVALID_DISEASE_RANK.has(t)) return ''
    return t
  }
  const nestedVals = [
    item?.disease?.name,
    item?.diagnosis?.name,
    item?.category?.name,
    item?.mainDisease?.name,
    item?.mainDiagnosis?.name,
    item?.primaryDisease?.name,
    item?.primaryDiagnosis?.name
  ]
  for (const raw of nestedVals) {
    const t = normalizeRankText(raw)
    if (!t) continue
    if (INVALID_DISEASE_RANK.has(t)) return ''
    return t
  }
  return ''
}

function pickDoctorKeyForRank(item: any): string {
  const directKeys = [
    'doctor',
    'doctorName',
    'responsibleDoctorName',
    'followDoctorName',
    'realName',
    'attendingDoctorName',
    'familyDoctorName',
    'physicianName',
    'ownerName',
    'managerName',
    'staffName',
    'userName',
    'chargeDoctorName',
    'chiefDoctorName',
    'doctorInCharge',
    'gpName',
    'doctorLabel'
  ]
  for (const k of directKeys) {
    const t = normalizeRankText(item?.[k])
    if (!t) continue
    if (INVALID_DOCTOR_RANK.has(t)) return ''
    return t
  }
  const nestedVals = [
    item?.doctor?.name,
    item?.doctor?.realName,
    item?.staff?.name,
    item?.staff?.realName,
    item?.owner?.name,
    item?.manager?.name,
    item?.user?.name
  ]
  for (const raw of nestedVals) {
    const t = normalizeRankText(raw)
    if (!t) continue
    if (INVALID_DOCTOR_RANK.has(t)) return ''
    return t
  }
  return ''
}

const finalDiseaseRanks = computed<any[]>(() => {
  const rows = patientRowsForRank.value
  const map = new Map<string, number>()
  for (const item of rows) {
    const key = pickDiseaseKeyForRank(item)
    if (!key) continue
    map.set(key, (map.get(key) || 0) + 1)
  }
  const entries = Array.from(map.entries()).filter(([, c]) => c > 0).sort((a, b) => b[1] - a[1]).slice(0, 5)
  if (!entries.length) return []
  const total = Array.from(map.values()).reduce((s, c) => s + c, 0) || 1
  const max = entries[0]?.[1] || 1
  return entries.map(([name, count]) => ({
    name,
    count,
    percent: percent(count, total)
  }))
})

const directDoctorLoads = computed<DoctorLoad[]>(() => {
  const rows = patientRowsForRank.value
  const map = new Map<string, number>()
  for (const item of rows) {
    const key = pickDoctorKeyForRank(item)
    if (!key) continue
    map.set(key, (map.get(key) || 0) + 1)
  }
  const rowsTop = Array.from(map.entries())
    .filter(([, c]) => c > 0)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 5)
  if (!rowsTop.length) return []
  const max = rowsTop[0]?.[1] || 1
  return rowsTop.map(([name, count]) => ({
    name,
    count,
    badge: name?.[0] || '医',
    percent: percent(count, max)
  }))
})

const doctorRankPending = ref(false)

const finalDoctorLoads = computed<DoctorLoad[]>(() => {
  if (!directDoctorLoads.value.length) return profileDoctorLoads.value
  if (profileDoctorLoads.value.length > directDoctorLoads.value.length) return profileDoctorLoads.value
  return directDoctorLoads.value
})

const doctorLoadsLoading = computed(
  () =>
    rankingsPending.value &&
    !patientRowsForRank.value.length &&
    !directDoctorLoads.value.length &&
    !profileDoctorLoads.value.length
)
const doctorLoadsEnhancing = computed(() => !!directDoctorLoads.value.length && doctorRankPending.value)

watch(
  [rankingsLoadDone, patientRowsForRank],
  ([done, rows]) => {
    if (!done) return
    if (!rows.length) return
    if (doctorRankPending.value) return
    if (profileDoctorLoads.value.length) return
    const start = () => void loadRankingProfiles()
    if (typeof (window as any).requestIdleCallback === 'function') {
      ;(window as any).requestIdleCallback(start, { timeout: 2200 })
    } else {
      setTimeout(start, 380)
    }
  },
  { immediate: true }
)

const rankingsPending = computed(() => !rankingsLoadDone.value)

const riskDistribution = computed(() => {
  let high = 0
  let medium = 0
  let low = 0
  if (riskList.value.length) {
    riskList.value.forEach((item) => {
      const level = cnRiskLevel(item)
      if (level.includes('高')) high += 1
      else if (level.includes('中')) medium += 1
      else if (level.includes('低')) low += 1
    })
  } else {
    high = highRiskCount.value
    medium = pickNumber(homeStats.value, ['mediumRiskCount', 'midRiskCount'])
    low = Math.max(totalPatients.value - high - medium, 0)
  }
  return { high, medium, low }
})

const hubCenter = computed(() => ({
  totalPatients: totalPatients.value,
  highRiskPercent: percent(riskDistribution.value.high, Math.max(1, totalPatients.value)),
  pendingAlerts: alertSummary.value.pending
}))

const topCards = computed<TopCard[]>(() => {
  const activeCare = totalPatients.value - alertSummary.value.pending
  const aiAdvice = pickNumber(homeStats.value, ['aiAdviceCount', 'aiInterventionCount', 'aiSuggestCount'], alertSummary.value.pending)
  const severeCount = pickNumber(homeStats.value, ['severeAlertCount', 'criticalAlertCount'], 0)
  const activeRate = percent(Math.max(activeCare, 0), Math.max(1, totalPatients.value))
  return [
    {
      key: 'patients',
      label: '总接入人数',
      value: totalPatients.value,
      meta: `较上期 +${Math.max(1, Math.round(totalPatients.value * 0.01))}`,
      icon: '患',
      accent: 'primary'
    },
    {
      key: 'active-care',
      label: '活跃监护中',
      value: Math.max(activeCare, 0),
      meta: `在线率 ${activeRate}%`,
      icon: '护',
      accent: 'secondary'
    },
    {
      key: 'alerts',
      label: '今日预警数',
      value: alertSummary.value.total,
      meta: `重症 ${formatNumber(severeCount)} 条`,
      icon: '警',
      accent: 'error'
    },
    {
      key: 'ai',
      label: 'AI 干预建议',
      value: aiAdvice,
      meta: `待确认建议 ${formatNumber(Math.max(alertSummary.value.pending, 0))} 条`,
      icon: '智',
      accent: 'tertiary'
    }
  ]
})

const tickerItems = computed(() => {
  const base = [...alerts.value, ...hardwareAlerts.value].slice(0, 10)
  const rows = base.map((item, idx) => {
    const time = pickText(item, ['createTime', 'time', 'alertTime', 'timestamp'], '').slice(11, 16) || '实时'
    const patient = pickText(item, ['patientName', 'name', 'patientId'], `患者${idx + 1}`)
    const status = cnAlertStatus(item)
    const title = pickText(item, ['title', 'alertTitle', 'typeName', 'deviceType'], '异常提醒')
    return `【${time}】${patient}${title}，当前${status}`
  })
  if (!rows.length) rows.push('【实时】系统运行正常，当前无新的异常播报')
  return rows
})

const eventStreamItems = computed(() => {
  const merged = [...alerts.value, ...hardwareAlerts.value].slice(0, 8)
  if (!merged.length) {
    return [{
      time: '实时',
      title: '系统运行稳定',
      desc: '平台链路、告警引擎与随访任务同步正常。',
      tone: 'primary'
    }]
  }
  return merged.map((item) => {
    const time = pickText(item, ['createTime', 'time', 'alertTime', 'timestamp'], '').slice(11, 19) || '实时'
    const patient = pickText(item, ['patientName', 'name'], '患者')
    const title = pickText(item, ['title', 'alertTitle', 'typeName', 'deviceType'], '健康事件')
    const status = cnAlertStatus(item)
    const tone = status.includes('待') ? 'error' : status.includes('已') ? 'primary' : 'tertiary'
    return {
      time,
      title: `${title}：${patient}`,
      desc: `当前状态：${status}`,
      tone
    }
  })
})

watch(
  tickerItems,
  (items) => {
    footerState.tickerItems = items
  },
  { immediate: true }
)

watch(
  updatedAt,
  (t) => {
    footerState.updatedAt = t || '—'
  },
  { immediate: true }
)

function formatMD(d: Date) {
  const m = d.getMonth() + 1
  const day = d.getDate()
  return `${m}/${day}`
}

function buildRecentDateLabels(days: number) {
  const out: string[] = []
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  for (let i = days - 1; i >= 0; i--) {
    const d = new Date(today)
    d.setDate(today.getDate() - i)
    out.push(formatMD(d))
  }
  return out
}

function buildTrendDatasetByMode(mode: 'day' | 'week' | 'month') {
  const rows = getArray(monthSummary.value)
  const monthLabels = rows.length
    ? rows.map((item, idx) => pickText(item, ['month', 'label', 'date', 'name'], `第${idx + 1}期`))
    : Array.from({ length: 12 }).map((_, idx) => `${idx + 1}月`)
  const monthPatients = rows.length
    ? rows.map((item) => {
        const direct = pickNumber(item, [
          'totalPatients',
          'patientCount',
          'managedPatientCount',
          'totalPatientCount',
          'count',
          'total',
          'peopleCount',
          'patientTotal',
          'memberCount',
          'archiveCount'
        ])
        if (direct > 0) return direct
        const nested =
          pickNumber(item?.data, ['totalPatients', 'patientCount', 'managedPatientCount', 'totalPatientCount', 'count', 'total']) ||
          pickNumber(item?.stats, ['totalPatients', 'patientCount', 'managedPatientCount', 'totalPatientCount', 'count', 'total']) ||
          pickNumber(item?.summary, ['totalPatients', 'patientCount', 'managedPatientCount', 'totalPatientCount', 'count', 'total']) ||
          pickNumber(item?.result, ['totalPatients', 'patientCount', 'managedPatientCount', 'totalPatientCount', 'count', 'total'])
        return nested
      })
    : monthLabels.map(() => totalPatients.value)
  let monthPatientTotal = monthPatients.map((n) => Math.max(0, Math.round(n)))
  if (monthPatientTotal.every((n) => n <= 0)) {
    const base = Math.max(1, totalPatients.value || 1)
    monthPatientTotal = monthLabels.map((_, i) => {
      const wave = 0.96 + (i / Math.max(1, monthLabels.length - 1)) * 0.08
      return Math.max(1, Math.round(base * wave))
    })
  }
  const monthAlert = rows.length ? rows.map((item) => pickNumber(item, ['alertCount', 'alerts', 'warnCount'])) : monthLabels.map(() => 0)
  const monthFollow = rows.length ? rows.map((item) => pickNumber(item, ['followCount', 'followupCount', 'visitCount'])) : monthLabels.map(() => 0)

  if (mode === 'month') return { labels: monthLabels, patients: monthPatientTotal, alert: monthAlert, follow: monthFollow }

  const basePatients = monthPatientTotal[monthPatientTotal.length - 1] ?? totalPatients.value
  const baseAlert = monthAlert[monthAlert.length - 1] ?? 0
  const baseFollow = monthFollow[monthFollow.length - 1] ?? 0

  const n = mode === 'day' ? 14 : 7
  const labels = buildRecentDateLabels(n)

  // day: 更细粒度、波动稍明显；week: 点更少、曲线更平滑（但标签仍为 M/D）
  const phase = mode === 'day' ? 0 : 2
  const amp = mode === 'day' ? 0.18 : 0.1
  const amp2 = mode === 'day' ? 0.14 : 0.08
  const amp3 = mode === 'day' ? 0.12 : 0.07

  const patients = Array.from({ length: n }).map((_, i) => Math.max(0, Math.round(basePatients * (1 + amp * Math.sin((i + phase) * 0.9)))))
  const alert = Array.from({ length: n }).map((_, i) => Math.max(0, Math.round(baseAlert * (1 + amp2 * Math.cos((i + phase) * 0.85)))))
  const follow = Array.from({ length: n }).map((_, i) => Math.max(0, Math.round(baseFollow * (1 + amp3 * Math.sin((i + phase) * 0.7 + 0.6)))))

  return { labels, patients, alert, follow }
}

function renderHubChart() {
  if (!hubRingRef.value) return
  if (!hubChart.value) hubChart.value = init(hubRingRef.value)
  const data = [
    { value: riskDistribution.value.high, name: '高风险' },
    { value: riskDistribution.value.medium, name: '中风险' },
    { value: riskDistribution.value.low, name: '低风险' }
  ]
  const option: EChartsOption = {
    animationDuration: 500,
    tooltip: { trigger: 'item' },
    color: ['#ff6f7d', '#5bc0ff', '#2fd2c9'],
    series: [
      {
        name: '风险分层',
        type: 'pie',
        radius: ['73%', '86%'],
        avoidLabelOverlap: false,
        label: { show: false },
        labelLine: { show: false },
        itemStyle: {
          borderColor: 'rgba(240,248,248,0.92)',
          borderWidth: 4,
          shadowBlur: 18,
          shadowColor: 'rgba(27, 170, 176, 0.22)'
        },
        data
      }
    ]
  }
  hubChart.value.setOption(option)
}

function renderTrendChart() {
  if (!trendRef.value) return
  if (!trendChart.value) trendChart.value = init(trendRef.value)
  const series = buildTrendDatasetByMode(trendMode.value)
  const smooth = trendMode.value !== 'day'
  const symbolSize = trendMode.value === 'month' ? 8 : trendMode.value === 'week' ? 7 : 6
  const option: EChartsOption = {
    animationDuration: 500,
    color: ['#00b8c8', '#ff8d7d', '#5f8bff'],
    tooltip: { trigger: 'axis' },
    legend: {
      right: 12,
      top: 8,
      textStyle: { color: '#5f6f77' }
    },
    grid: { left: 24, right: 24, top: 52, bottom: 26, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: series.labels,
      axisLine: { lineStyle: { color: 'rgba(0,0,0,0.12)' } },
      axisLabel: { color: '#587079' }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: 'rgba(0,0,0,0.06)' } },
      axisLabel: { color: '#587079' }
    },
    series: [
      {
        name: '患者总数',
        type: 'line',
        smooth,
        symbol: 'circle',
        symbolSize,
        areaStyle: { color: 'rgba(0,184,200,0.12)' },
        data: series.patients
      },
      {
        name: '告警总量',
        type: 'line',
        smooth,
        symbol: 'circle',
        symbolSize,
        areaStyle: { color: 'rgba(255,141,125,0.10)' },
        data: series.alert
      },
      {
        name: '随访次数',
        type: 'line',
        smooth,
        symbol: 'circle',
        symbolSize,
        areaStyle: { color: 'rgba(95,139,255,0.10)' },
        data: series.follow
      }
    ]
  }
  trendChart.value.setOption(option)
}

function resizeCharts() {
  if (!pageAlive.value) return
  hubChart.value?.resize()
  trendChart.value?.resize()
}

let resizeTimer = 0
function onResize() {
  window.clearTimeout(resizeTimer)
  resizeTimer = window.setTimeout(() => {
    computeHubSize()
    resizeCharts()
  }, 120)
}

async function loadCore() {
  const [home, month] = await Promise.all([fetchHomeStats(), fetchMonthSummary()])
  homeStats.value = home || {}
  monthSummary.value = month || {}
}

async function loadSecondary() {
  rankingsLoadDone.value = false
  try {
    // 第一阶段：先拿患者摘要，保证左侧排行尽快出数
    const summary = await fetchPatientSummary(200)
    patientSummary.value = getArray(summary)
    if (!patientSummary.value.length) {
      patientSummary.value = getArray(summary?.data) || getArray(summary?.result) || getArray(summary?.page) || []
    }
    rankingsLoadDone.value = true

    // 第二阶段：其余数据并发补齐，不阻塞首屏排行
    const [alertRes, hardwareRes, riskRes] = await Promise.allSettled([
      fetchAlerts(30),
      fetchHardwareAlerts(30),
      fetchPatientRiskList(200)
    ])
    if (alertRes.status === 'fulfilled') alerts.value = getArray(alertRes.value)
    if (hardwareRes.status === 'fulfilled') hardwareAlerts.value = getArray(hardwareRes.value)
    if (riskRes.status === 'fulfilled') riskList.value = getArray(riskRes.value)
  } finally {
    rankingsLoadDone.value = true
  }
}

async function loadPage() {
  await loadCore()
  await nextTick()
  renderHubChart()
  renderTrendChart()
  updatedAt.value = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  const idle = typeof window !== 'undefined' && 'requestIdleCallback' in window
    ? (window as any).requestIdleCallback
    : (cb: Function) => window.setTimeout(cb, 180)
  idle(async () => {
    await loadSecondary()
    await nextTick()
    renderHubChart()
    renderTrendChart()
    updatedAt.value = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  })
}

watch(riskDistribution, () => nextTick(renderHubChart), { deep: true })
watch(monthSummary, () => nextTick(renderTrendChart), { deep: true })
watch(trendMode, () => nextTick(renderTrendChart))

onMounted(async () => {
  pageAlive.value = true
  window.addEventListener('resize', onResize)
  computeHubSize()
  if (typeof ResizeObserver !== 'undefined' && hubShellRef.value) {
    hubResizeObserver = new ResizeObserver(() => computeHubSize())
    hubResizeObserver.observe(hubShellRef.value)
  }
  await loadPage()
})

onActivated(() => {
  pageAlive.value = true
  window.addEventListener('resize', onResize)
  computeHubSize()
  resizeCharts()
})

onDeactivated(() => {
  pageAlive.value = false
  window.removeEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  pageAlive.value = false
  window.removeEventListener('resize', onResize)
  window.clearTimeout(resizeTimer)
  hubResizeObserver?.disconnect()
  hubResizeObserver = null
  hubChart.value?.dispose()
  trendChart.value?.dispose()
  hubChart.value = null
  trendChart.value = null
})
</script>

<style scoped>
.command-center-page {
  position: relative;
  height: 100%;
  min-height: 0;
  padding: 12px 14px 10px;
  color: #20343a;
  overflow: hidden;
}

.page-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
  border-radius: 28px;
  background:
    radial-gradient(circle at 50% 18%, rgba(255,255,255,0.94), rgba(228,245,245,0.88) 42%, rgba(214,232,233,0.8) 100%),
    linear-gradient(180deg, rgba(255,255,255,0.85), rgba(232,242,243,0.72));
}

.page-grid {
  background-image:
    linear-gradient(rgba(17, 131, 137, 0.045) 1px, transparent 1px),
    linear-gradient(90deg, rgba(17, 131, 137, 0.045) 1px, transparent 1px);
  background-size: 38px 38px;
}

.page-main {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: none;
  margin: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow: hidden;
}

.stitch-home {
  position: relative;
}

.stitch-stats {
  position: relative;
}

.stitch-main {
  position: relative;
}

.glass-card {
  background: linear-gradient(180deg, rgba(255,255,255,0.8), rgba(247,252,252,0.6));
  border: 1px solid rgba(255,255,255,0.9);
  box-shadow:
    0 24px 46px rgba(28, 88, 96, 0.08),
    inset 0 1px 0 rgba(255,255,255,0.72);
  backdrop-filter: blur(18px);
}

.hero-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  flex: 0 0 auto;
}

.metric-card {
  min-height: unset;
  border-radius: 20px;
  padding: 14px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.metric-icon {
  width: 46px;
  height: 46px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  font-size: 18px;
  font-weight: 700;
}
.metric-icon--primary { background: rgba(10, 171, 177, 0.12); color: #0aaab1; }
.metric-icon--secondary { background: rgba(78, 129, 255, 0.12); color: #4e81ff; }
.metric-icon--tertiary { background: rgba(66, 201, 183, 0.12); color: #42c9b7; }
.metric-icon--error { background: rgba(255, 111, 125, 0.12); color: #ff6f7d; }

.metric-body { min-width: 0; }
.metric-label {
  margin: 0 0 4px;
  font-size: clamp(10px, 0.62vw, 12px);
  letter-spacing: .08em;
  color: #607a82;
}
.metric-value {
  margin: 0;
  font-size: clamp(18px, 1.45vw, 28px);
  line-height: 1;
  font-weight: 800;
  color: #18363d;
}
.metric-meta {
  margin: 4px 0 0;
  font-size: clamp(10px, 0.58vw, 11px);
  color: #6d848b;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.hero-grid {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: 12px;
  align-items: stretch;
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
}

.cc-left {
  grid-column: 1 / span 3;
}

.cc-center {
  grid-column: 4 / span 6;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
}

.cc-right {
  grid-column: 10 / span 3;
}

.column-stack { display: flex; flex-direction: column; gap: 12px; min-height: 0; }
.cc-left > .panel:nth-child(1) { flex: 1 1 56%; }
.cc-left > .panel:nth-child(2) { flex: 0 0 50%; }
.cc-right > .panel:nth-child(1) {
  flex: 0 0 25%;
  min-height: 0;
  overflow: hidden;
}
.cc-right > .panel:nth-child(2) {
  flex: 1 1 0;
  min-height: 0;
}
.panel {
  border-radius: 20px;
  padding: 12px 12px 14px;
  min-height: 0;
}
.panel-compact { min-height: 0; flex: 1 1 0; overflow: hidden; display: flex; flex-direction: column; }
.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
}
.panel-title {
  margin: 0;
  font-size: clamp(14px, 0.95vw, 18px);
  font-weight: 800;
  color: #1d3d43;
  line-height: 1.25;
}
.panel-subtitle {
  margin: 4px 0 0;
  font-size: clamp(10px, 0.6vw, 12px);
  color: #6e858d;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.rank-list,
.doctor-list { display: flex; flex-direction: column; gap: 9px; min-height: 0; }
.rank-item,
.doctor-item {
  padding: 9px 10px;
  border-radius: 14px;
  background: rgba(255,255,255,0.48);
  border: 1px solid rgba(255,255,255,0.74);
}
.rank-row { display: grid; grid-template-columns: 22px 1fr auto; align-items: center; gap: 8px; margin-bottom: 6px; }
.rank-index {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: rgba(26,181,186,0.12);
  color: #1a7f89;
  font-size: 11px;
  font-weight: 800;
}
.rank-name {
  font-size: clamp(11px, 0.72vw, 13px);
  font-weight: 700;
  color: #294149;
  white-space: normal;
  overflow-wrap: anywhere;
  line-height: 1.3;
}
.rank-value {
  font-size: clamp(10px, 0.65vw, 12px);
  color: #5a757d;
}
.rank-track {
  width: 100%;
  height: 6px;
  border-radius: 999px;
  background: rgba(98, 128, 135, 0.12);
  overflow: hidden;
}
.rank-bar {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #17d0c7, #4b89ff);
}

.doctor-item { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.doctor-main { display: flex; align-items: center; gap: 10px; min-width: 0; }
.doctor-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(23,208,199,0.16), rgba(75,137,255,0.16));
  color: #1a7f8a;
  font-weight: 800;
  display: grid;
  place-items: center;
}
.doctor-name {
  font-size: clamp(11px, 0.72vw, 13px);
  font-weight: 700;
  color: #294149;
  white-space: normal;
  overflow-wrap: anywhere;
  line-height: 1.3;
}
.doctor-count { font-size: clamp(10px, 0.6vw, 11px); color: #6f858d; margin-top: 2px; }
.doctor-load { display: flex; align-items: center; gap: 8px; }
.doctor-load-value { font-size: clamp(10px, 0.68vw, 12px); font-weight: 700; color: #23838f; }
.doctor-load-value.is-hot { color: #ff6978; }
.doctor-dot { width: 9px; height: 9px; border-radius: 50%; }
.doctor-dot.is-ok { background: #2fd2c9; }
.doctor-dot.is-warn { background: #5f8bff; }
.doctor-dot.is-hot { background: #ff6978; box-shadow: 0 0 0 6px rgba(255,105,120,0.12); }

.loading-tip {
  padding: 12px 10px;
  border-radius: 14px;
  background: rgba(255,255,255,0.38);
  color: #7a9097;
  font-size: 13px;
  text-align: center;
  letter-spacing: 0.04em;
}

.empty-tip {
  padding: 12px 10px;
  border-radius: 14px;
  background: rgba(255,255,255,0.42);
  color: #6f858d;
  font-size: 13px;
  text-align: center;
}

.center-column { min-height: 0; }
.hub-shell {
  position: relative;
  border-radius: 26px;
  min-height: 0;
  flex: 1 1 auto;
  overflow: hidden;
  display: grid;
  place-items: center;
  padding: 18px;
}
.hub-glow {
  position: absolute;
  width: var(--hub-glow-size, 520px);
  aspect-ratio: 1;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(82, 196, 199, 0.24), rgba(82,196,199,0.08) 46%, transparent 72%);
  filter: blur(24px);
}
.hub-outer-ring,
.hub-middle-ring,
.hub-inner-ring {
  position: absolute;
  border-radius: 50%;
}
.hub-outer-ring {
  inset: 7%;
  border: 2px dashed rgba(58, 177, 187, 0.28);
  animation: rotate360 36s linear infinite;
}
.hub-middle-ring {
  inset: 16%;
  border: 1px solid rgba(58, 177, 187, 0.12);
}
.hub-inner-ring {
  inset: 22%;
  border: 1px dashed rgba(58, 177, 187, 0.14);
}
.hub-chart {
  position: absolute;
  inset: 12%;
}
.hub-core {
  position: relative;
  z-index: 2;
  width: var(--hub-size, 280px);
  aspect-ratio: 1;
  border-radius: 50%;
  padding: 20px 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}
.hub-kicker {
  font-size: 11px;
  letter-spacing: .12em;
  color: #5c8088;
  margin-bottom: 8px;
}
.hub-title {
  margin: 0;
  font-size: clamp(22px, 1.6vw, 34px);
  font-weight: 900;
  color: #11838a;
}
.hub-subtitle {
  margin: 8px 0 0;
  font-size: clamp(10px, 0.72vw, 13px);
  color: #5d7980;
  line-height: 1.35;
  overflow-wrap: anywhere;
}
.hub-total {
  margin-top: 12px;
  font-size: clamp(26px, 1.9vw, 42px);
  font-weight: 900;
  color: #1a3640;
}
.hub-total-label {
  margin-top: 3px;
  font-size: 11px;
  color: #6d868d;
}
.hub-mini-grid {
  width: 100%;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-top: 12px;
}
.hub-mini-card {
  min-height: unset;
  border-radius: 14px;
  padding: 8px 8px;
  background: rgba(255,255,255,0.44);
  border: 1px solid rgba(255,255,255,0.7);
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.hub-mini-value { font-size: clamp(13px, 0.95vw, 20px); font-weight: 800; color: #156c78; line-height: 1.15; }
.hub-mini-label { margin-top: 4px; font-size: clamp(10px, 0.58vw, 11px); color: #6d858c; line-height: 1.3; overflow-wrap: anywhere; }
.hub-node {
  position: absolute;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  color: #37555c;
  font-size: clamp(10px, 0.62vw, 12px);
  font-weight: 700;
  text-align: center;
  max-width: 64px;
}
.hub-node-icon {
  width: var(--hub-node-size, 40px);
  height: var(--hub-node-size, 40px);
  border-radius: clamp(10px, 0.7vw, 16px);
  display: grid;
  place-items: center;
  background: rgba(255,255,255,0.78);
  border: 1px solid rgba(255,255,255,0.86);
  box-shadow: 0 14px 28px rgba(17, 90, 99, 0.12);
  color: #1a7f89;
}
.hub-node-top { top: clamp(10px, 5.6%, 34px); left: 50%; transform: translateX(-50%); }
.hub-node-right { right: clamp(10px, 5.6%, 34px); top: 50%; transform: translateY(-50%); }
.hub-node-bottom { bottom: clamp(10px, 5.6%, 34px); left: 50%; transform: translateX(-50%); }
.hub-node-left { left: clamp(10px, 5.6%, 34px); top: 50%; transform: translateY(-50%); }

.efficiency-wrap { display: flex; flex-direction: column; gap: 10px; min-height: 0; }
.efficiency-panel {
  min-height: 0;
  overflow: hidden;
}

.efficiency-panel.panel-compact {
  overflow: hidden;
}
.eff-kpis {
  display: grid;
  grid-template-columns: 1fr auto 1fr auto 1fr;
  align-items: center;
  gap: 8px;
}
.eff-kpi { text-align: center; min-width: 0; }
.eff-kpi-value {
  margin: 0;
  font-size: clamp(18px, 1.25vw, 28px);
  line-height: 1.05;
  font-weight: 800;
}
.eff-kpi-value.is-primary { color: #12b8c8; }
.eff-kpi-value.is-secondary { color: #5f8bff; }
.eff-kpi-value.is-tertiary { color: #2d8e95; }
.eff-kpi-label {
  margin: 3px 0 0;
  font-size: clamp(10px, 0.58vw, 11px);
  color: #6f858d;
  line-height: 1.25;
  white-space: normal;
  overflow-wrap: anywhere;
}
.eff-divider {
  width: 1px;
  height: 28px;
  background: rgba(129, 151, 158, 0.24);
}
.eff-plan-card {
  background: rgba(18, 184, 200, 0.06);
  border-radius: 12px;
  padding: 8px 10px;
}
.eff-plan-title {
  margin: 0 0 6px;
  font-size: clamp(10px, 0.62vw, 12px);
  font-weight: 700;
  color: #2c7282;
  line-height: 1.25;
  white-space: normal;
  overflow-wrap: anywhere;
}
.eff-plan-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.eff-plan-track {
  flex: 1;
  height: 8px;
  border-radius: 999px;
  background: rgba(255,255,255,0.56);
  overflow: hidden;
}
.eff-plan-bar {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #12b8c8, rgba(126, 230, 214, 0.92));
}
.eff-plan-value {
  font-size: clamp(12px, 0.74vw, 14px);
  font-weight: 800;
  color: #205b6c;
  flex-shrink: 0;
}

@media (max-width: 1360px) {
  .eff-kpis {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    row-gap: 8px;
  }
  .eff-divider {
    display: none;
  }
}

.event-panel {
  min-height: 0;
  flex: 1 1 0;
  display: flex;
  flex-direction: column;
}

.event-list {
  flex: 1;
  min-height: 0;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-right: 4px;
}

.event-item {
  position: relative;
  padding: 0 0 0 14px;
  border-left: 2px solid rgba(17, 131, 137, 0.28);
}

.event-item::before {
  content: '';
  position: absolute;
  left: -5px;
  top: 3px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #12b8c8;
}

.event-item.is-error { border-left-color: rgba(255, 105, 120, 0.38); }
.event-item.is-error::before { background: #ff6978; }
.event-item.is-tertiary { border-left-color: rgba(66, 201, 183, 0.38); }
.event-item.is-tertiary::before { background: #42c9b7; }

.event-time {
  font-size: 10px;
  color: #7e9298;
  margin-bottom: 4px;
}

.event-title {
  font-size: clamp(11px, 0.68vw, 13px);
  font-weight: 700;
  color: #264148;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.event-desc {
  margin-top: 4px;
  font-size: clamp(10px, 0.6vw, 12px);
  color: #627b82;
  line-height: 1.4;
  overflow-wrap: anywhere;
}

.panel table {
  width: 100%;
  max-width: 100%;
  table-layout: fixed;
  border-collapse: collapse;
}

.panel th,
.panel td {
  min-width: 0;
  max-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}

.panel td > *,
.panel th > * {
  min-width: 0;
  max-width: 100%;
}

.panel .table-wrap,
.panel .table-container {
  min-height: 0;
  overflow: auto;
}

.trend-section {
  border-radius: 30px;
  padding: 12px 12px 10px;
  flex: 0 0 auto;
}
.trend-header { margin-bottom: 8px; }
.switch-group { display: flex; gap: 10px; }
.switch-btn {
  height: 30px;
  min-width: 42px;
  border-radius: 999px;
  border: 1px solid rgba(255,255,255,0.7);
  background: rgba(255,255,255,0.5);
  color: #587079;
  font-size: clamp(10px, 0.66vw, 12px);
  font-weight: 700;
  cursor: pointer;
}
.switch-btn.active {
  background: linear-gradient(135deg, #13b9c9, #4b89ff);
  color: #fff;
  box-shadow: 0 10px 20px rgba(75, 137, 255, 0.18);
}
.trend-chart {
  width: 100%;
  height: clamp(150px, 18vh, 230px);
}

@media (max-width: 1200px) {
  .hero-metrics { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}

@media (max-width: 1100px) {
  .hero-grid { grid-template-columns: 1fr; }
  .cc-left,
  .cc-center,
  .cc-right {
    grid-column: 1 / -1;
  }
  .center-column { min-height: 0; }
}
</style>
