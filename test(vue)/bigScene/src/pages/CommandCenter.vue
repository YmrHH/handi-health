<template>
  <div class="command-center-page">
    <div class="page-bg page-grid"></div>

    <main class="page-main">
      <section class="hero-metrics" aria-label="首页核心指标">
        <article v-for="card in topCards" :key="card.key" class="metric-card glass-card">
          <div class="metric-icon" :class="`metric-icon--${card.accent}`">{{ card.icon }}</div>
          <div class="metric-body">
            <p class="metric-label">{{ card.label }}</p>
            <p class="metric-value">{{ formatNumber(card.value) }}</p>
            <p class="metric-meta">{{ card.meta }}</p>
          </div>
        </article>
      </section>

      <section class="hero-grid">
        <aside class="left-column column-stack">
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
                  <span class="rank-value">{{ item.percent }}%</span>
                </div>
                <div class="rank-track">
                  <div class="rank-bar" :style="{ width: `${item.percent}%` }"></div>
                </div>
              </div>
              <div v-if="rankingsPending" class="loading-tip">正在统计...</div>
              <div v-else-if="!finalDiseaseRanks.length" class="empty-tip">暂无可展示的病种分布数据</div>
            </div>
          </section>

          <section class="glass-card panel panel-compact">
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
              <div v-if="rankingsPending" class="loading-tip">正在统计...</div>
              <div v-else-if="!finalDoctorLoads.length" class="empty-tip">暂无可展示的医生负载数据</div>
            </div>
          </section>
        </aside>

        <section class="center-column">
          <section class="hub-shell glass-card">
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
        </section>

        <aside class="right-column column-stack">
          <section class="glass-card panel panel-compact">
            <div class="panel-header">
              <div>
                <h3 class="panel-title">处置效率</h3>
                <p class="panel-subtitle">近 30 天告警闭环处理情况</p>
              </div>
            </div>
            <div class="efficiency-wrap">
              <div class="big-progress">
                <svg viewBox="0 0 120 120" class="big-progress-svg">
                  <circle cx="60" cy="60" r="48" class="big-progress-track"></circle>
                  <circle
                    cx="60"
                    cy="60"
                    r="48"
                    class="big-progress-bar"
                    :style="{ strokeDashoffset: bigCircleOffset(disposalRate) }"
                  ></circle>
                </svg>
                <div class="big-progress-center">
                  <div class="big-progress-value">{{ disposalRate }}%</div>
                  <div class="big-progress-label">闭环率</div>
                </div>
              </div>
              <div class="legend-row">
                <div class="legend-item"><span class="legend-dot is-primary"></span>已处理 {{ formatNumber(alertSummary.closed) }}</div>
                <div class="legend-item"><span class="legend-dot is-muted"></span>待处理 {{ formatNumber(alertSummary.pending) }}</div>
              </div>
            </div>
          </section>

          <section class="glass-card panel panel-compact">
            <div class="panel-header">
              <div>
                <h3 class="panel-title">完成率矩阵</h3>
                <p class="panel-subtitle">随访执行与服务执行效率</p>
              </div>
            </div>
            <div class="mini-progress-grid">
              <div class="mini-progress-card">
                <div class="mini-progress">
                  <svg viewBox="0 0 100 100" class="mini-progress-svg">
                    <circle cx="50" cy="50" r="38" class="mini-progress-track"></circle>
                    <circle
                      cx="50"
                      cy="50"
                      r="38"
                      class="mini-progress-bar is-primary"
                      :style="{ strokeDashoffset: miniCircleOffset(followRate) }"
                    ></circle>
                  </svg>
                  <div class="mini-progress-center">{{ followRate }}%</div>
                </div>
                <div class="mini-progress-label">随访完成率</div>
              </div>

              <div class="mini-progress-card" :class="{ 'is-pending': !serviceRateHasData }">
                <div class="mini-progress">
                  <svg viewBox="0 0 100 100" class="mini-progress-svg">
                    <circle cx="50" cy="50" r="38" class="mini-progress-track"></circle>
                    <circle
                      cx="50"
                      cy="50"
                      r="38"
                      class="mini-progress-bar is-secondary"
                      :class="{ 'is-muted': !serviceRateHasData }"
                      :style="{ strokeDashoffset: miniCircleOffset(serviceRateForRing) }"
                    ></circle>
                  </svg>
                  <div class="mini-progress-center" :class="{ 'is-pending-text': !serviceRateHasData }">{{ serviceRateDisplay }}</div>
                </div>
                <div class="mini-progress-label" :class="{ 'is-pending-caption': !serviceRateHasData }">{{ serviceRateLabel }}</div>
              </div>
            </div>
          </section>
        </aside>
      </section>

      <section class="trend-section glass-card">
        <div class="panel-header trend-header">
          <div>
            <h3 class="panel-title">综合趋势总览</h3>
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
    </main>

    <footer class="status-band glass-card">
      <div class="status-indicator">
        <span class="status-dot"></span>
        <span>系统运行正常</span>
      </div>
      <div class="ticker-window">
        <div class="ticker-track" :style="tickerStyle">
          <div v-for="(item, idx) in tickerItems" :key="`ticker-a-${idx}`" class="ticker-item">
            {{ item }}
          </div>
          <div v-for="(item, idx) in tickerItems" :key="`ticker-b-${idx}`" class="ticker-item">
            {{ item }}
          </div>
        </div>
      </div>
      <div class="band-meta">更新时间 {{ updatedAt }}</div>
    </footer>
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
const trendRef = ref<HTMLDivElement | null>(null)
const hubChart = shallowRef<ECharts | null>(null)
const trendChart = shallowRef<ECharts | null>(null)

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
  const fromHome = pickNumber(homeStats.value, ['totalPatients', 'patientCount', 'managedPatientCount', 'totalPatientCount'])
  return fromHome || patientSummary.value.length || pickNumber(monthSummary.value, ['totalPatients'])
})

const highRiskCount = computed(() => {
  const fromHome = pickNumber(homeStats.value, ['highRiskPatients', 'highRiskCount', 'highRiskPatientCount', 'riskHighCount'])
  if (fromHome) return fromHome
  return riskList.value.filter((item) => cnRiskLevel(item).includes('高')).length
})

const activeDoctorCount = computed(() => {
  const fromHome = pickNumber(homeStats.value, ['activeDoctorCount', 'doctorCount', 'activeDoctors'])
  if (fromHome) return fromHome
  return new Set(patientSummary.value.map((item) => doctorName(item)).filter(Boolean)).size
})

const weeklyFollowDone = computed(() => pickNumber(homeStats.value, ['weekFollowDone', 'weeklyFollowCount', 'followDoneThisWeek', 'weekDoneCount']))
const followRate = computed(() => pickNumber(homeStats.value, ['weekFollowRate', 'followRate', 'weekCompleteRate'], 0))

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

const SERVICE_RATE_KEYS = [
  'serviceRate',
  'taskCompleteRate',
  'interventionRate',
  'closeLoopRate',
  'planFinishRate',
  'executionRate',
  'completionRate'
]

function normalizePercentish(n: number): number {
  if (!Number.isFinite(n) || n < 0) return 0
  if (n > 0 && n <= 1) return Math.round(n * 100)
  return Math.round(Math.min(100, n))
}

function pickServiceRateFromObject(obj: any): number | null {
  if (!obj || typeof obj !== 'object') return null
  for (const key of SERVICE_RATE_KEYS) {
    const v = obj[key]
    if (typeof v === 'number' && Number.isFinite(v)) return normalizePercentish(v)
    if (typeof v === 'string' && v.trim() !== '' && !Number.isNaN(Number(v))) return normalizePercentish(Number(v))
  }
  return null
}

function pickServiceRateFromHome(): number | null {
  const src = homeStats.value
  const direct = pickServiceRateFromObject(src)
  if (direct != null) return direct
  if (!src || typeof src !== 'object') return null
  for (const nest of ['data', 'stats', 'summary', 'board', 'overview', 'result']) {
    const v = pickServiceRateFromObject(src[nest])
    if (v != null) return v
  }
  return null
}

const serviceRateValue = computed(() => pickServiceRateFromHome())
const serviceRateHasData = computed(() => serviceRateValue.value !== null)
const serviceRateForRing = computed(() => (serviceRateHasData.value ? (serviceRateValue.value as number) : 0))
const serviceRateLabel = computed(() => '服务执行率')
const serviceRateDisplay = computed(() => (serviceRateHasData.value ? `${serviceRateValue.value}%` : '待接入'))

function diseaseName(item: SummaryItem) {
  const direct = pickText(item, [
    'diseaseName',
    'chronicName',
    'diseaseType',
    'categoryName',
    'disease',
    'mainDisease',
    'diagnosisName',
    'diagnosis',
    'diseaseLabel',
    'illnessName',
    'chronicDisease',
    'chronicDiseaseName',
    'majorDisease',
    'patientDisease',
    'conditionName',
    'tagName',
    'typeName'
  ])
  if (direct) return direct

  const nested = pickFirstText([
    item?.disease?.name,
    item?.disease?.label,
    item?.mainDisease?.name,
    item?.diagnosis?.name,
    item?.diagnosis?.label,
    item?.category?.name,
    item?.chronicDisease?.name,
    item?.chronicDiseaseInfo?.name,
    item?.majorDiseaseInfo?.name,
    item?.condition?.name,
    item?.patient?.disease,
    item?.patient?.diseaseName,
    item?.profile?.diseaseName,
    item?.archive?.diseaseName
  ])
  if (nested) return nested

  const fromArrays = [
    ...(Array.isArray(item?.diseaseNames) ? item.diseaseNames : []),
    ...(Array.isArray(item?.diagnosisList) ? item.diagnosisList : []),
    ...(Array.isArray(item?.tags) ? item.tags : [])
  ]
  const arrName = pickFirstText(
    fromArrays.map((x: any) => (typeof x === 'string' ? x : x?.name || x?.label || x?.title || ''))
  )
  return arrName || '未标注病种'
}

function doctorName(item: SummaryItem) {
  const direct = pickText(item, [
    'doctorName',
    'responsibleDoctorName',
    'followDoctorName',
    'attendingDoctorName',
    'familyDoctorName',
    'staffName',
    'ownerName',
    'managerName',
    'physicianName',
    'attendingDoctorName',
    'familyDoctorName',
    'doctor',
    'doctorNickName',
    'nickname',
    'realName',
    'userName',
    'staffRealName',
    'followUserName',
    'createByName'
  ])
  if (direct) return direct

  const nested = pickFirstText([
    item?.doctor?.name,
    item?.doctor?.realName,
    item?.doctor?.nickname,
    item?.staff?.name,
    item?.staff?.realName,
    item?.owner?.name,
    item?.manager?.name,
    item?.user?.name,
    item?.patient?.doctorName,
    item?.patient?.responsibleDoctorName,
    item?.profile?.doctorName,
    item?.profile?.realName
  ])
  return nested || '未分配医生'
}

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

const PROFILE_RANK_MAX_IDS = 72
const PROFILE_RANK_BATCH = 8

const profileDiseaseRanks = ref<RankItem[]>([])
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
  'clinicalDiagnosis'
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
  'attendingDoctorName',
  'familyDoctorName',
  'physicianName',
  'ownerName',
  'managerName',
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
  profileDiseaseRanks.value = []
  profileDoctorLoads.value = []
  let ids = extractRiskIdsFromRows(patientSummary.value, PROFILE_RANK_MAX_IDS)
  if (ids.length < PROFILE_RANK_MAX_IDS && riskList.value.length) {
    const more = extractRiskIdsFromRows(riskList.value as SummaryItem[], PROFILE_RANK_MAX_IDS)
    const seen = new Set(ids)
    for (const id of more) {
      if (seen.has(id)) continue
      seen.add(id)
      ids.push(id)
      if (ids.length >= PROFILE_RANK_MAX_IDS) break
    }
  }
  if (ids.length < PROFILE_RANK_MAX_IDS) {
    const homeExtra = extractRiskIdsFromRows(getArray(homeStats.value) as SummaryItem[], PROFILE_RANK_MAX_IDS)
    const monthExtra = extractRiskIdsFromRows(getArray(monthSummary.value) as SummaryItem[], PROFILE_RANK_MAX_IDS)
    const seen = new Set(ids)
    for (const id of [...homeExtra, ...monthExtra]) {
      if (seen.has(id)) continue
      seen.add(id)
      ids.push(id)
      if (ids.length >= PROFILE_RANK_MAX_IDS) break
    }
  }
  if (!ids.length || !pageAlive.value) return

  const diseaseMap = new Map<string, number>()
  const doctorMap = new Map<string, number>()

  for (let i = 0; i < ids.length; i += PROFILE_RANK_BATCH) {
    if (!pageAlive.value) break
    const batch = ids.slice(i, i + PROFILE_RANK_BATCH)
    const results = await Promise.allSettled(batch.map((id) => fetchPatientProfile(id)))
    for (const r of results) {
      if (r.status !== 'fulfilled') continue
      const dStr = pickDiseaseFromProfileInner(r.value)
      const docStr = pickDoctorFromProfileInner(r.value)
      if (dStr) diseaseMap.set(dStr, (diseaseMap.get(dStr) || 0) + 1)
      if (docStr) doctorMap.set(docStr, (doctorMap.get(docStr) || 0) + 1)
    }
  }

  const disRanks = mapToRankItemsFromCounts(diseaseMap)
  const docLoads = mapToDoctorLoadsFromCounts(doctorMap)

  if (disRanks.length) profileDiseaseRanks.value = disRanks
  if (docLoads.length) profileDoctorLoads.value = docLoads
}

const diseaseRanksFallback = computed<RankItem[]>(() => {
  const map = new Map<string, number>()
  const sources: SummaryItem[] = [
    ...patientSummary.value,
    ...riskList.value,
    ...getArray(monthSummary.value),
    ...getArray(homeStats.value)
  ]
  sources.forEach((item) => {
    const key = diseaseName(item)
    if (isValidProfileDisease(key)) {
      map.set(key, (map.get(key) || 0) + 1)
    }
  })

  if (!map.size) {
    const homeRows = getArray(homeStats.value)
    homeRows.forEach((item: any) => {
      const key = diseaseName(item)
      if (isValidProfileDisease(key)) {
        map.set(key, (map.get(key) || 0) + 1)
      }
    })
  }

  const entries = Array.from(map.entries()).filter(([, c]) => c > 0)
  if (!entries.length) return []
  const total = entries.reduce((sum, [, cur]) => sum + cur, 0)
  if (total <= 0) return []
  return entries
    .map(([name, count]) => ({ name, count, percent: percent(count, total) }))
    .sort((a, b) => b.count - a.count)
    .slice(0, 5)
})

const doctorLoadsFallback = computed<DoctorLoad[]>(() => {
  const map = new Map<string, number>()
  const sources: SummaryItem[] = [
    ...patientSummary.value,
    ...riskList.value,
    ...getArray(monthSummary.value),
    ...getArray(homeStats.value)
  ]
  sources.forEach((item) => {
    const key = doctorName(item)
    if (isValidProfileDoctor(key)) {
      map.set(key, (map.get(key) || 0) + 1)
    }
  })

  if (!map.size) {
    const homeRows = getArray(homeStats.value)
    homeRows.forEach((item: any) => {
      const key = doctorName(item)
      if (isValidProfileDoctor(key)) {
        map.set(key, (map.get(key) || 0) + 1)
      }
    })
  }

  const rows = Array.from(map.entries())
    .filter(([, c]) => c > 0)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 5)
  if (!rows.length) return []
  const max = rows[0]?.[1] || 1
  return rows.map(([name, count]) => ({
    name,
    count,
    percent: percent(count, max),
    badge: name?.[0] || '医'
  }))
})

const finalDiseaseRanks = computed<RankItem[]>(() =>
  profileDiseaseRanks.value.length ? profileDiseaseRanks.value : diseaseRanksFallback.value
)

const finalDoctorLoads = computed<DoctorLoad[]>(() =>
  profileDoctorLoads.value.length ? profileDoctorLoads.value : doctorLoadsFallback.value
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
  const pendingAlerts = alertSummary.value.pending
  return [
    {
      key: 'patients',
      label: '患者总数',
      value: totalPatients.value,
      meta: `纳入管理 ${formatNumber(totalPatients.value)} 人`,
      icon: '患',
      accent: 'primary'
    },
    {
      key: 'high-risk',
      label: '高风险患者数',
      value: highRiskCount.value,
      meta: `高风险占比 ${percent(highRiskCount.value, Math.max(1, totalPatients.value))}%`,
      icon: '险',
      accent: 'error'
    },
    {
      key: 'alerts',
      label: '未处理告警数',
      value: pendingAlerts,
      meta: `近 30 天累计告警 ${formatNumber(alertSummary.value.total)} 条`,
      icon: '警',
      accent: 'tertiary'
    },
    {
      key: 'follow',
      label: '本周完成随访',
      value: weeklyFollowDone.value,
      meta: `活跃医生 ${formatNumber(activeDoctorCount.value)} 名`,
      icon: '访',
      accent: 'secondary'
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
  if (!rows.length) rows.push('【实时】系统运行正常，当前暂无新的异常播报')
  return rows
})

const tickerStyle = computed(() => ({
  animationDuration: `${Math.max(24, tickerItems.value.length * 6)}s`
}))

function bigCircleOffset(value: number) {
  const circumference = 2 * Math.PI * 48
  return `${circumference * (1 - value / 100)}`
}

function miniCircleOffset(value: number) {
  const circumference = 2 * Math.PI * 38
  return `${circumference * (1 - value / 100)}`
}

function normalizeTrendSeries() {
  const rows = getArray(monthSummary.value)
  if (rows.length) {
    const labels = rows.map((item, idx) => pickText(item, ['month', 'label', 'date', 'name'], `第${idx + 1}期`))
    const risk = rows.map((item) => pickNumber(item, ['highRiskCount', 'riskCount', 'risk', 'warningCount']))
    const alert = rows.map((item) => pickNumber(item, ['alertCount', 'alerts', 'warnCount']))
    const follow = rows.map((item) => pickNumber(item, ['followCount', 'followupCount', 'visitCount']))
    return { labels, risk, alert, follow }
  }
  const labels = Array.from({ length: 12 }).map((_, idx) => `${idx + 1}月`)
  return {
    labels,
    risk: labels.map(() => 0),
    alert: labels.map(() => 0),
    follow: labels.map(() => 0)
  }
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
  const series = normalizeTrendSeries()
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
        name: '风险患者',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        areaStyle: { color: 'rgba(0,184,200,0.12)' },
        data: series.risk
      },
      {
        name: '告警总量',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        areaStyle: { color: 'rgba(255,141,125,0.10)' },
        data: series.alert
      },
      {
        name: '随访次数',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
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
  resizeTimer = window.setTimeout(() => resizeCharts(), 120)
}

async function loadCore() {
  const [home, month] = await Promise.all([fetchHomeStats(), fetchMonthSummary()])
  homeStats.value = home || {}
  monthSummary.value = month || {}
}

async function loadSecondary() {
  rankingsLoadDone.value = false
  try {
    const [summary, alertList, hardwareList, risks] = await Promise.all([
      fetchPatientSummary(500),
      fetchAlerts(30),
      fetchHardwareAlerts(30),
      fetchPatientRiskList(200)
    ])
    patientSummary.value = getArray(summary)
    alerts.value = getArray(alertList)
    hardwareAlerts.value = getArray(hardwareList)
    riskList.value = getArray(risks)

    if (!patientSummary.value.length) {
      patientSummary.value = getArray(summary?.data) || getArray(summary?.result) || getArray(summary?.page) || []
    }

    await loadRankingProfiles()
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
  await loadPage()
})

onActivated(() => {
  pageAlive.value = true
  window.addEventListener('resize', onResize)
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
  hubChart.value?.dispose()
  trendChart.value?.dispose()
  hubChart.value = null
  trendChart.value = null
})
</script>

<style scoped>
.command-center-page {
  position: relative;
  min-height: 100%;
  padding: 18px 18px calc(124px + env(safe-area-inset-bottom, 0px));
  color: #20343a;
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
  max-width: 1880px;
  margin: 0 auto;
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
  gap: 18px;
  margin-bottom: 22px;
}

.metric-card {
  min-height: 124px;
  border-radius: 24px;
  padding: 20px 22px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.metric-icon {
  width: 60px;
  height: 60px;
  border-radius: 18px;
  display: grid;
  place-items: center;
  font-size: 22px;
  font-weight: 700;
}
.metric-icon--primary { background: rgba(10, 171, 177, 0.12); color: #0aaab1; }
.metric-icon--secondary { background: rgba(78, 129, 255, 0.12); color: #4e81ff; }
.metric-icon--tertiary { background: rgba(66, 201, 183, 0.12); color: #42c9b7; }
.metric-icon--error { background: rgba(255, 111, 125, 0.12); color: #ff6f7d; }

.metric-body { min-width: 0; }
.metric-label {
  margin: 0 0 8px;
  font-size: 13px;
  letter-spacing: .08em;
  color: #607a82;
}
.metric-value {
  margin: 0;
  font-size: 30px;
  line-height: 1;
  font-weight: 800;
  color: #18363d;
}
.metric-meta {
  margin: 8px 0 0;
  font-size: 12px;
  color: #6d848b;
}

.hero-grid {
  display: grid;
  grid-template-columns: minmax(290px, 0.94fr) minmax(620px, 2.45fr) minmax(290px, 0.94fr);
  gap: 20px;
  align-items: stretch;
}

.column-stack { display: flex; flex-direction: column; gap: 18px; }
.panel {
  border-radius: 26px;
  padding: 18px 18px 20px;
}
.panel-compact { min-height: 0; }
.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}
.panel-title {
  margin: 0;
  font-size: 20px;
  font-weight: 800;
  color: #1d3d43;
}
.panel-subtitle {
  margin: 6px 0 0;
  font-size: 12px;
  color: #6e858d;
}

.rank-list,
.doctor-list { display: flex; flex-direction: column; gap: 14px; }
.rank-item,
.doctor-item {
  padding: 12px 14px;
  border-radius: 18px;
  background: rgba(255,255,255,0.48);
  border: 1px solid rgba(255,255,255,0.74);
}
.rank-row { display: grid; grid-template-columns: 26px 1fr auto; align-items: center; gap: 10px; margin-bottom: 8px; }
.rank-index {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: rgba(26,181,186,0.12);
  color: #1a7f89;
  font-size: 12px;
  font-weight: 800;
}
.rank-name { font-size: 14px; font-weight: 700; color: #294149; }
.rank-value { font-size: 13px; color: #5a757d; }
.rank-track {
  width: 100%;
  height: 8px;
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
.doctor-main { display: flex; align-items: center; gap: 12px; min-width: 0; }
.doctor-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(23,208,199,0.16), rgba(75,137,255,0.16));
  color: #1a7f8a;
  font-weight: 800;
  display: grid;
  place-items: center;
}
.doctor-name { font-size: 14px; font-weight: 700; color: #294149; }
.doctor-count { font-size: 12px; color: #6f858d; margin-top: 3px; }
.doctor-load { display: flex; align-items: center; gap: 8px; }
.doctor-load-value { font-size: 13px; font-weight: 700; color: #23838f; }
.doctor-load-value.is-hot { color: #ff6978; }
.doctor-dot { width: 9px; height: 9px; border-radius: 50%; }
.doctor-dot.is-ok { background: #2fd2c9; }
.doctor-dot.is-warn { background: #5f8bff; }
.doctor-dot.is-hot { background: #ff6978; box-shadow: 0 0 0 6px rgba(255,105,120,0.12); }

.loading-tip {
  padding: 18px 14px;
  border-radius: 18px;
  background: rgba(255,255,255,0.38);
  color: #7a9097;
  font-size: 13px;
  text-align: center;
  letter-spacing: 0.04em;
}

.empty-tip {
  padding: 18px 14px;
  border-radius: 18px;
  background: rgba(255,255,255,0.42);
  color: #6f858d;
  font-size: 13px;
  text-align: center;
}

.center-column { min-height: 700px; }
.hub-shell {
  position: relative;
  border-radius: 34px;
  min-height: 100%;
  overflow: hidden;
  display: grid;
  place-items: center;
  padding: 30px;
}
.hub-glow {
  position: absolute;
  width: 72%;
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
  width: min(380px, 62%);
  aspect-ratio: 1;
  border-radius: 50%;
  padding: 30px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}
.hub-kicker {
  font-size: 12px;
  letter-spacing: .12em;
  color: #5c8088;
  margin-bottom: 8px;
}
.hub-title {
  margin: 0;
  font-size: 36px;
  font-weight: 900;
  color: #11838a;
}
.hub-subtitle {
  margin: 8px 0 0;
  font-size: 14px;
  color: #5d7980;
}
.hub-total {
  margin-top: 18px;
  font-size: 42px;
  font-weight: 900;
  color: #1a3640;
}
.hub-total-label {
  margin-top: 4px;
  font-size: 13px;
  color: #6d868d;
}
.hub-mini-grid {
  width: 100%;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 18px;
}
.hub-mini-card {
  min-height: 72px;
  border-radius: 18px;
  padding: 12px 10px;
  background: rgba(255,255,255,0.44);
  border: 1px solid rgba(255,255,255,0.7);
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.hub-mini-value { font-size: 21px; font-weight: 800; color: #156c78; }
.hub-mini-label { margin-top: 6px; font-size: 12px; color: #6d858c; }
.hub-node {
  position: absolute;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #37555c;
  font-size: 12px;
  font-weight: 700;
}
.hub-node-icon {
  width: 50px;
  height: 50px;
  border-radius: 18px;
  display: grid;
  place-items: center;
  background: rgba(255,255,255,0.78);
  border: 1px solid rgba(255,255,255,0.86);
  box-shadow: 0 14px 28px rgba(17, 90, 99, 0.12);
  color: #1a7f89;
}
.hub-node-top { top: 6%; left: 50%; transform: translateX(-50%); }
.hub-node-right { right: 6%; top: 50%; transform: translateY(-50%); }
.hub-node-bottom { bottom: 6%; left: 50%; transform: translateX(-50%); }
.hub-node-left { left: 6%; top: 50%; transform: translateY(-50%); }

.efficiency-wrap { display: flex; flex-direction: column; align-items: center; gap: 18px; }
.big-progress {
  position: relative;
  width: 178px;
  height: 178px;
}
.big-progress-svg,
.mini-progress-svg { width: 100%; height: 100%; transform: rotate(-90deg); }
.big-progress-track,
.big-progress-bar,
.mini-progress-track,
.mini-progress-bar {
  fill: none;
  stroke-linecap: round;
}
.big-progress-track { stroke: rgba(100, 128, 134, 0.14); stroke-width: 10; }
.big-progress-bar {
  stroke: #12b8c8;
  stroke-width: 10;
  stroke-dasharray: 301.59;
  transition: stroke-dashoffset .4s ease;
}
.big-progress-center,
.mini-progress-center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.big-progress-value { font-size: 36px; font-weight: 900; color: #1a3640; }
.big-progress-label { margin-top: 4px; font-size: 12px; color: #6b848b; }
.legend-row { width: 100%; display: flex; justify-content: space-between; gap: 12px; font-size: 12px; color: #5b757d; }
.legend-item { display: flex; align-items: center; gap: 6px; }
.legend-dot { width: 8px; height: 8px; border-radius: 50%; }
.legend-dot.is-primary { background: #12b8c8; }
.legend-dot.is-muted { background: rgba(100, 128, 134, 0.34); }

.mini-progress-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}
.mini-progress-card {
  border-radius: 22px;
  padding: 14px 10px 12px;
  background: rgba(255,255,255,0.48);
  border: 1px solid rgba(255,255,255,0.72);
  text-align: center;
}
.mini-progress { position: relative; width: 108px; height: 108px; margin: 0 auto; }
.mini-progress-track { stroke: rgba(100, 128, 134, 0.14); stroke-width: 9; }
.mini-progress-bar {
  stroke-width: 9;
  stroke-dasharray: 238.76;
  transition: stroke-dashoffset .4s ease;
}
.mini-progress-bar.is-primary { stroke: #1ab5ba; }
.mini-progress-bar.is-secondary { stroke: #5f8bff; }
.mini-progress-bar.is-muted {
  stroke: rgba(100, 128, 134, 0.32);
}
.mini-progress-center { font-size: 24px; font-weight: 800; color: #20343a; }
.mini-progress-center.is-pending-text {
  font-size: 15px;
  font-weight: 600;
  color: #8a9da3;
  letter-spacing: 0.02em;
}
.mini-progress-label { margin-top: 8px; font-size: 13px; font-weight: 700; color: #38545b; }
.mini-progress-label.is-pending-caption {
  color: #8a9da3;
  font-weight: 600;
}
.mini-progress-card.is-pending {
  opacity: 0.92;
  background: rgba(255,255,255,0.38);
}
.mini-progress-tip { margin-top: 6px; font-size: 11px; color: #7a8f95; line-height: 1.4; }

.trend-section {
  margin-top: 22px;
  margin-bottom: 8px;
  border-radius: 30px;
  padding: 18px 18px 16px;
}
.trend-header { margin-bottom: 8px; }
.switch-group { display: flex; gap: 10px; }
.switch-btn {
  height: 34px;
  min-width: 48px;
  border-radius: 999px;
  border: 1px solid rgba(255,255,255,0.7);
  background: rgba(255,255,255,0.5);
  color: #587079;
  font-size: 13px;
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
  height: 360px;
}

.status-band {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  width: 100%;
  max-width: none;
  transform: none;
  border-radius: 18px 18px 0 0;
  min-height: 56px;
  padding: 8px 20px calc(10px + env(safe-area-inset-bottom, 0px));
  display: grid;
  grid-template-columns: minmax(160px, 200px) 1fr minmax(120px, 150px);
  gap: 12px;
  align-items: center;
  z-index: 40;
  box-sizing: border-box;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.55), rgba(247, 252, 252, 0.42));
  border: 1px solid rgba(255, 255, 255, 0.75);
  border-bottom: none;
  box-shadow: 0 -8px 32px rgba(28, 88, 96, 0.07);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
}
.status-indicator,
.band-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: #35535a;
  font-weight: 700;
}
.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #20d0c9;
  box-shadow: 0 0 0 7px rgba(32,208,201,0.12);
}
.ticker-window {
  overflow: hidden;
  border-left: 1px solid rgba(17, 131, 137, 0.08);
  border-right: 1px solid rgba(17, 131, 137, 0.08);
  padding: 0 12px;
}
.ticker-track {
  display: inline-flex;
  align-items: center;
  gap: 28px;
  white-space: nowrap;
  min-width: max-content;
  animation: tickerMove linear infinite;
}
.ticker-item {
  font-size: 12px;
  color: #4d6a72;
}
.band-meta { justify-content: flex-end; color: #72878d; }

@keyframes rotate360 {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes tickerMove {
  from { transform: translateX(0); }
  to { transform: translateX(-50%); }
}

@media (max-width: 1480px) {
  .hero-metrics { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .hero-grid { grid-template-columns: 1fr; }
  .center-column { min-height: 560px; }
  .status-band {
    grid-template-columns: 1fr;
    padding: 12px 16px calc(12px + env(safe-area-inset-bottom, 0px));
  }
  .band-meta { justify-content: flex-start; }
}
</style>
