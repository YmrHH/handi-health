
<template>
  <div class="command-center-page">
    <section class="overview-row">
      <article v-for="card in topCards" :key="card.key" class="metric-card glass-card">
        <div class="metric-icon" :class="`metric-icon--${card.tone}`">
          <span>{{ card.icon }}</span>
        </div>
        <div class="metric-body">
          <p class="metric-label">{{ card.label }}</p>
          <div class="metric-main">
            <strong>{{ card.value }}</strong>
            <em v-if="card.unit">{{ card.unit }}</em>
          </div>
          <p class="metric-meta">{{ card.meta }}</p>
        </div>
      </article>
    </section>

    <section class="hero-grid">
      <aside class="side-col">
        <section class="glass-card info-panel">
          <header class="panel-head">
            <div>
              <h3>病种排名 TOP5</h3>
              <p>基于患者档案与画像数据统计</p>
            </div>
          </header>

          <div v-if="rankingsPending" class="panel-empty panel-loading">正在统计…</div>
          <div v-else-if="finalDiseaseRanks.length === 0" class="panel-empty">暂无可展示的病种分布数据</div>
          <div v-else class="rank-list">
            <article v-for="(item, index) in finalDiseaseRanks" :key="`${item.name}-${index}`" class="rank-item">
              <div class="rank-row">
                <span class="rank-name">{{ item.name }}</span>
                <span class="rank-value">{{ item.percentText }}</span>
              </div>
              <div class="rank-bar">
                <div class="rank-bar-fill" :style="{ width: `${item.ratio}%` }"></div>
              </div>
              <div class="rank-meta">
                <span>样本 {{ item.count }}</span>
                <span>占比 {{ item.percentText }}</span>
              </div>
            </article>
          </div>
        </section>

        <section class="glass-card info-panel">
          <header class="panel-head">
            <div>
              <h3>医生负载 TOP5</h3>
              <p>基于患者归属与画像数据统计</p>
            </div>
          </header>

          <div v-if="rankingsPending" class="panel-empty panel-loading">正在统计…</div>
          <div v-else-if="finalDoctorLoads.length === 0" class="panel-empty">暂无可展示的医生负载数据</div>
          <div v-else class="doctor-list">
            <article v-for="(item, index) in finalDoctorLoads" :key="`${item.name}-${index}`" class="doctor-item">
              <div class="doctor-avatar">{{ item.shortName }}</div>
              <div class="doctor-body">
                <div class="doctor-row">
                  <span class="doctor-name">{{ item.name }}</span>
                  <span class="doctor-load">{{ item.percentText }}</span>
                </div>
                <div class="doctor-sub">
                  <span>关联样本 {{ item.count }}</span>
                  <span class="doctor-dot" :class="{ 'doctor-dot--warm': index === 0 }"></span>
                </div>
              </div>
            </article>
          </div>
        </section>
      </aside>

      <section class="hub-col">
        <section class="glass-card hub-panel">
          <div class="hub-shell">
            <div class="hub-glow"></div>
            <div class="hub-orbit hub-orbit--outer"></div>
            <div class="hub-orbit hub-orbit--inner"></div>

            <div class="hub-ring" :style="{ background: hubGradient }"></div>

            <div class="hub-core">
              <p class="hub-kicker">平台中枢</p>
              <h2>寒岐智护</h2>
              <p class="hub-caption">慢性病随访健康预警管理平台</p>
              <div class="hub-value">{{ formatNumber(patientTotal) }}</div>
              <p class="hub-value-label">当前受管患者总数</p>

              <div class="hub-mini-grid">
                <article class="hub-mini-card">
                  <span>高风险患者</span>
                  <strong>{{ formatNumber(highRiskPatients) }}</strong>
                </article>
                <article class="hub-mini-card">
                  <span>未处理告警</span>
                  <strong>{{ formatNumber(pendingAlertsCount) }}</strong>
                </article>
              </div>
            </div>

            <div class="hub-node hub-node--top">
              <strong>患者</strong>
              <span>{{ formatNumber(patientTotal) }}</span>
            </div>
            <div class="hub-node hub-node--right">
              <strong>协同网络</strong>
              <span>{{ formatNumber(totalEventsCount) }}</span>
            </div>
            <div class="hub-node hub-node--bottom">
              <strong>AI 中枢</strong>
              <span>{{ hubRiskSummary }}</span>
            </div>
            <div class="hub-node hub-node--left">
              <strong>医生</strong>
              <span>{{ formatNumber(activeDoctorsCount) }}</span>
            </div>
          </div>
        </section>
      </section>

      <aside class="side-col">
        <section class="glass-card info-panel">
          <header class="panel-head">
            <div>
              <h3>处置效率</h3>
              <p>健康告警与设备异常的闭环处理情况</p>
            </div>
          </header>

          <div class="efficiency-wrap">
            <div class="efficiency-ring" :style="{ '--eff': `${disposalRate}%` }">
              <div class="efficiency-inner">
                <strong>{{ disposalRateText }}</strong>
                <span>已处理占比</span>
              </div>
            </div>
            <div class="efficiency-meta">
              <div><i class="dot dot--primary"></i>已处理 {{ formatNumber(closedAlertsCount) }}</div>
              <div><i class="dot dot--muted"></i>待处理 {{ formatNumber(pendingAlertsCount) }}</div>
            </div>
          </div>
        </section>

        <section class="glass-card info-panel">
          <header class="panel-head">
            <div>
              <h3>完成率矩阵</h3>
              <p>随访执行与服务落地的关键完成率</p>
            </div>
          </header>

          <div class="matrix-grid">
            <article class="matrix-item">
              <div class="mini-ring" :style="{ '--mini-ring': `${followRatePercent}%` }">
                <div class="mini-ring-inner">
                  <strong>{{ followRateText }}</strong>
                </div>
              </div>
              <span>随访完成率</span>
            </article>

            <article class="matrix-item">
              <div class="mini-ring" :class="{ 'mini-ring--placeholder': serviceRatePercent === null }"
                   :style="{ '--mini-ring': `${serviceRatePercent ?? 0}%` }">
                <div class="mini-ring-inner">
                  <strong>{{ serviceRateText }}</strong>
                </div>
              </div>
              <span>服务执行率</span>
            </article>
          </div>
        </section>
      </aside>
    </section>

    <section class="glass-card trend-panel">
      <header class="panel-head trend-head">
        <div>
          <h3>综合趋势总览</h3>
          <p>聚合展示风险、告警与随访完成的阶段性变化</p>
        </div>
        <div class="trend-switch">
          <button type="button" class="trend-btn">日</button>
          <button type="button" class="trend-btn trend-btn--active">周</button>
          <button type="button" class="trend-btn">月</button>
        </div>
      </header>

      <div class="trend-stage">
        <div class="trend-gridline" v-for="line in 4" :key="line" :style="{ bottom: `${line * 20}%` }"></div>

        <svg class="trend-svg" viewBox="0 0 1000 300" preserveAspectRatio="none">
          <polyline class="trend-line trend-line--risk" :points="riskPolyline" />
          <polyline class="trend-line trend-line--alert" :points="alertPolyline" />
          <polyline class="trend-line trend-line--follow" :points="followPolyline" />
        </svg>

        <div class="trend-bars">
          <article v-for="(item, index) in trendSeries" :key="`${item.label}-${index}`" class="trend-col">
            <div class="trend-bar-wrap">
              <div class="trend-bar trend-bar--risk" :style="{ height: `${item.riskHeight}%` }"></div>
              <div class="trend-bar trend-bar--alert" :style="{ height: `${item.alertHeight}%` }"></div>
              <div class="trend-bar trend-bar--follow" :style="{ height: `${item.followHeight}%` }"></div>
            </div>
            <span class="trend-label">{{ item.label }}</span>
          </article>
        </div>
      </div>

      <div class="trend-legend">
        <span><i class="dot dot--risk"></i>风险波动</span>
        <span><i class="dot dot--alert"></i>告警数量</span>
        <span><i class="dot dot--follow"></i>随访完成</span>
      </div>
    </section>

    <footer class="status-band glass-card">
      <div class="status-left">
        <i class="status-dot"></i>
        <span>{{ systemStatusText }}</span>
      </div>
      <div class="ticker">
        <div class="ticker-track">
          <span v-for="(item, index) in tickerItems" :key="`${item}-${index}`" class="ticker-item">{{ item }}</span>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { computed, onActivated, onDeactivated, onMounted, ref } from 'vue'
import {
  fetchAlerts,
  fetchHardwareAlerts,
  fetchHomeStats,
  fetchMonthSummary,
  fetchPatientProfile,
  fetchPatientRiskList,
  fetchPatientSummary
} from '../api'

type AnyRecord = Record<string, any>
type RankEntry = { name: string; count: number; ratio: number; percentText: string; shortName?: string }
type TrendItem = {
  label: string
  risk: number
  alert: number
  follow: number
  riskHeight: number
  alertHeight: number
  followHeight: number
}

const corePending = ref(true)
const secondaryPending = ref(true)
const rankingsPending = ref(true)

const homeStats = ref<AnyRecord>({})
const monthSummary = ref<any>(null)
const patientSummary = ref<any>(null)
const riskList = ref<any>(null)
const alerts = ref<any>(null)
const hardwareAlerts = ref<any>(null)

const profileDiseaseRanks = ref<RankEntry[]>([])
const profileDoctorLoads = ref<RankEntry[]>([])

const mountedOnce = ref(false)
const isActive = ref(false)

function isFiniteNumber(value: any) {
  return typeof value === 'number' && Number.isFinite(value)
}

function toNumber(value: any): number | null {
  if (isFiniteNumber(value)) return value
  if (typeof value === 'string') {
    const cleaned = value.replace(/,/g, '').trim()
    if (!cleaned) return null
    const num = Number(cleaned)
    return Number.isFinite(num) ? num : null
  }
  return null
}

function pickNumber(...values: any[]): number | null {
  for (const value of values) {
    const num = toNumber(value)
    if (num !== null) return num
  }
  return null
}

function clampPercent(value: number | null | undefined): number {
  if (value === null || value === undefined || Number.isNaN(value)) return 0
  return Math.max(0, Math.min(100, Number(value)))
}

function formatNumber(value: number | null | undefined): string {
  const safe = Number(value ?? 0)
  return safe.toLocaleString('zh-CN')
}

function formatPercent(value: number | null | undefined): string {
  if (value === null || value === undefined || Number.isNaN(value)) return '—'
  return `${Math.round(value)}%`
}

function sanitizeText(value: any): string {
  return String(value ?? '').trim()
}

const INVALID_DISEASE = new Set(['', '未标注病种', '未知', '暂无'])
const INVALID_DOCTOR = new Set(['', '未分配医生', '未知', '暂无'])

function getNested(record: any, path: string): any {
  return path.split('.').reduce((acc, key) => (acc == null ? undefined : acc[key]), record)
}

function extractArray(payload: any): AnyRecord[] {
  if (!payload) return []
  if (Array.isArray(payload)) return payload as AnyRecord[]

  const queue: any[] = [payload]
  const keys = ['rows', 'list', 'records', 'items', 'data', 'content', 'result', 'datas', 'recordsList', 'dataList']
  while (queue.length) {
    const current = queue.shift()
    if (!current || typeof current !== 'object') continue
    for (const key of keys) {
      if (Array.isArray(current[key])) return current[key] as AnyRecord[]
    }
    for (const value of Object.values(current)) {
      if (Array.isArray(value)) return value as AnyRecord[]
      if (value && typeof value === 'object') queue.push(value)
    }
  }
  return []
}

function pickFirstText(record: AnyRecord, keys: string[]): string {
  for (const key of keys) {
    const value = key.includes('.') ? getNested(record, key) : record[key]
    if (Array.isArray(value)) {
      for (const item of value) {
        if (typeof item === 'string' && sanitizeText(item)) return sanitizeText(item)
        if (item && typeof item === 'object') {
          const nested = sanitizeText(item.name ?? item.label ?? item.value ?? '')
          if (nested) return nested
        }
      }
    } else {
      const text = sanitizeText(value)
      if (text) return text
    }
  }
  return ''
}

function extractDiseaseName(record: AnyRecord): string {
  const text = pickFirstText(record, [
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
    'disease.name',
    'diagnosis.name',
    'category.name',
    'mainDisease.name',
    'mainDiagnosis.name',
    'primaryDisease.name',
    'primaryDiagnosis.name'
  ])
  return INVALID_DISEASE.has(text) ? '' : text
}

function extractDoctorName(record: AnyRecord): string {
  const text = pickFirstText(record, [
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
    'doctor.name',
    'doctor.realName',
    'staff.name',
    'staff.realName',
    'owner.name',
    'manager.name',
    'user.name'
  ])
  return INVALID_DOCTOR.has(text) ? '' : text
}

function buildRankEntries(entries: Record<string, number>, topN = 5): RankEntry[] {
  const rows = Object.entries(entries)
    .filter(([, count]) => count > 0)
    .sort((a, b) => b[1] - a[1])
    .slice(0, topN)

  if (rows.length === 0) return []
  const total = rows.reduce((sum, [, count]) => sum + count, 0)
  const max = Math.max(...rows.map(([, count]) => count), 1)

  return rows.map(([name, count]) => {
    const ratio = Math.max(14, Math.round((count / max) * 100))
    const percent = total > 0 ? (count / total) * 100 : 0
    return {
      name,
      count,
      ratio,
      percentText: `${percent.toFixed(percent >= 10 ? 0 : 1)}%`,
      shortName: name.slice(0, 1)
    }
  })
}

async function runInBatches<T, R>(items: T[], size: number, worker: (item: T) => Promise<R>) {
  const results: PromiseSettledResult<R>[] = []
  for (let i = 0; i < items.length; i += size) {
    const batch = items.slice(i, i + size)
    const settled = await Promise.allSettled(batch.map((item) => worker(item)))
    results.push(...settled)
  }
  return results
}

function uniqueIdsFromRows(rows: AnyRecord[]): string[] {
  const ids: string[] = []
  const seen = new Set<string>()
  for (const row of rows) {
    const raw = [
      row.patientId,
      row.id,
      row.riskId,
      row.patientBasicInfoId,
      row.archiveId,
      row.userId,
      getNested(row, 'patient.id'),
      getNested(row, 'patient.patientId'),
      getNested(row, 'profile.id'),
      getNested(row, 'archive.id')
    ]
    for (const candidate of raw) {
      const value = sanitizeText(candidate)
      if (value && !seen.has(value)) {
        seen.add(value)
        ids.push(value)
        break
      }
    }
  }
  return ids
}

function buildFallbackRanks() {
  const rows = [...extractArray(patientSummary.value), ...extractArray(riskList.value)]
  const diseaseMap: Record<string, number> = {}
  const doctorMap: Record<string, number> = {}

  rows.forEach((row) => {
    const disease = extractDiseaseName(row)
    if (disease) diseaseMap[disease] = (diseaseMap[disease] || 0) + 1

    const doctor = extractDoctorName(row)
    if (doctor) doctorMap[doctor] = (doctorMap[doctor] || 0) + 1
  })

  return {
    disease: buildRankEntries(diseaseMap),
    doctor: buildRankEntries(doctorMap)
  }
}

async function loadRankingProfiles() {
  rankingsPending.value = true

  const ids = uniqueIdsFromRows([
    ...extractArray(patientSummary.value),
    ...extractArray(riskList.value)
  ]).slice(0, 80)

  const diseaseMap: Record<string, number> = {}
  const doctorMap: Record<string, number> = {}

  if (ids.length > 0) {
    const settled = await runInBatches(ids, 8, async (id) => fetchPatientProfile(id))
    settled.forEach((result) => {
      if (result.status !== 'fulfilled') return
      const profile = (result.value || {}) as AnyRecord
      const disease = extractDiseaseName(profile)
      if (disease) diseaseMap[disease] = (diseaseMap[disease] || 0) + 1

      const doctor = extractDoctorName(profile)
      if (doctor) doctorMap[doctor] = (doctorMap[doctor] || 0) + 1
    })
  }

  profileDiseaseRanks.value = buildRankEntries(diseaseMap)
  profileDoctorLoads.value = buildRankEntries(doctorMap)
  rankingsPending.value = false
}

async function loadCore() {
  corePending.value = true
  try {
    const [stats, monthly] = await Promise.all([
      fetchHomeStats(),
      fetchMonthSummary()
    ])
    homeStats.value = stats || {}
    monthSummary.value = monthly || {}
  } finally {
    corePending.value = false
  }
}

async function loadSecondary() {
  secondaryPending.value = true
  try {
    const [summary, alertData, hardwareData, riskData] = await Promise.all([
      fetchPatientSummary(500),
      fetchAlerts(30),
      fetchHardwareAlerts(30),
      fetchPatientRiskList(200)
    ])
    patientSummary.value = summary || {}
    alerts.value = alertData || {}
    hardwareAlerts.value = hardwareData || {}
    riskList.value = riskData || {}
    await loadRankingProfiles()
  } finally {
    rankingsPending.value = false
    secondaryPending.value = false
  }
}

async function refreshIfNeeded(force = false) {
  if (!force && mountedOnce.value) return
  mountedOnce.value = true
  await loadCore()
  await loadSecondary()
}

onMounted(async () => {
  isActive.value = true
  await refreshIfNeeded(true)
})

onActivated(() => {
  isActive.value = true
})

onDeactivated(() => {
  isActive.value = false
})

const summaryRows = computed(() => extractArray(patientSummary.value))
const riskRows = computed(() => extractArray(riskList.value))
const alertRows = computed(() => extractArray(alerts.value))
const hardwareRows = computed(() => extractArray(hardwareAlerts.value))

const patientTotal = computed(() => {
  return pickNumber(
    homeStats.value.totalPatients,
    homeStats.value.patientCount,
    homeStats.value.totalPatientCount,
    homeStats.value.managedPatientCount,
    summaryRows.value.length
  ) ?? 0
})

const highRiskPatients = computed(() => {
  return pickNumber(
    homeStats.value.highRiskPatients,
    homeStats.value.highRiskCount,
    homeStats.value.highCount,
    riskRows.value.filter((row) => /高/.test(sanitizeText(row.level ?? row.riskLevel ?? row.riskName))).length
  ) ?? 0
})

const activeDoctorsCount = computed(() => {
  const byStats = pickNumber(
    homeStats.value.activeDoctors,
    homeStats.value.activeDoctorCount,
    homeStats.value.doctorCount
  )
  if (byStats !== null) return byStats

  const names = new Set<string>()
  for (const row of summaryRows.value) {
    const name = extractDoctorName(row)
    if (name) names.add(name)
  }
  return names.size
})

function countStatuses(payload: any, rows: AnyRecord[]) {
  const pendingFromPayload = pickNumber(
    payload?.pendingCount,
    payload?.unhandledCount,
    payload?.todoCount,
    payload?.openCount
  )
  const closedFromPayload = pickNumber(
    payload?.closedCount,
    payload?.handledCount,
    payload?.resolvedCount
  )

  if (pendingFromPayload !== null || closedFromPayload !== null) {
    return {
      pending: pendingFromPayload ?? 0,
      closed: closedFromPayload ?? 0
    }
  }

  let pending = 0
  let closed = 0
  rows.forEach((row) => {
    const status = sanitizeText(row.status ?? row.handleStatus ?? row.state).toLowerCase()
    if (status.includes('closed') || status.includes('handled') || status.includes('resolved') || /已关闭|已处理|已解决/.test(status)) {
      closed += 1
    } else {
      pending += 1
    }
  })
  return { pending, closed }
}

const alertStatusCounts = computed(() => countStatuses(alerts.value, alertRows.value))
const hardwareStatusCounts = computed(() => countStatuses(hardwareAlerts.value, hardwareRows.value))

const pendingAlertsCount = computed(() => {
  const direct = pickNumber(
    homeStats.value.pendingAlerts,
    homeStats.value.unhandledAlerts,
    homeStats.value.pendingAlertCount
  )
  if (direct !== null) return direct
  return alertStatusCounts.value.pending + hardwareStatusCounts.value.pending
})

const closedAlertsCount = computed(() => {
  return alertStatusCounts.value.closed + hardwareStatusCounts.value.closed
})

const totalAlertsCount = computed(() => pendingAlertsCount.value + closedAlertsCount.value)
const totalEventsCount = computed(() => alertRows.value.length + hardwareRows.value.length)

const weekFollowDone = computed(() => {
  return pickNumber(
    homeStats.value.weekFollowDone,
    homeStats.value.weekFollowCount,
    homeStats.value.weekFollowFinishCount,
    homeStats.value.followDoneThisWeek
  ) ?? 0
})

const followRatePercent = computed(() => clampPercent(
  pickNumber(
    homeStats.value.weekFollowRate,
    homeStats.value.followRate,
    homeStats.value.followupRate
  )
))

const serviceRatePercent = computed<number | null>(() => {
  const value = pickNumber(
    homeStats.value.serviceRate,
    homeStats.value.taskCompleteRate,
    homeStats.value.interventionRate,
    homeStats.value.closeLoopRate,
    homeStats.value.planFinishRate,
    homeStats.value.executionRate,
    homeStats.value.completionRate
  )
  return value === null ? null : clampPercent(value)
})

const serviceRateText = computed(() => (serviceRatePercent.value === null ? '待接入' : formatPercent(serviceRatePercent.value)))
const followRateText = computed(() => formatPercent(followRatePercent.value))

const disposalRate = computed(() => {
  const total = totalAlertsCount.value
  if (total <= 0) return 0
  return Math.round((closedAlertsCount.value / total) * 100)
})
const disposalRateText = computed(() => formatPercent(disposalRate.value))

const fallbackRanks = computed(() => buildFallbackRanks())
const finalDiseaseRanks = computed(() => {
  return profileDiseaseRanks.value.length > 0 ? profileDiseaseRanks.value : fallbackRanks.value.disease
})
const finalDoctorLoads = computed(() => {
  return profileDoctorLoads.value.length > 0 ? profileDoctorLoads.value : fallbackRanks.value.doctor
})

const topCards = computed(() => [
  {
    key: 'patients',
    label: '患者总数',
    value: formatNumber(patientTotal.value),
    unit: '',
    meta: `高风险患者 ${formatNumber(highRiskPatients.value)} 人`,
    tone: 'primary',
    icon: '患'
  },
  {
    key: 'risk',
    label: '高风险患者',
    value: formatNumber(highRiskPatients.value),
    unit: '人',
    meta: `风险关注占比 ${formatPercent(patientTotal.value ? (highRiskPatients.value / patientTotal.value) * 100 : 0)}`,
    tone: 'danger',
    icon: '险'
  },
  {
    key: 'alerts',
    label: '未处理告警',
    value: formatNumber(pendingAlertsCount.value),
    unit: '条',
    meta: `近 30 天累计 ${formatNumber(totalAlertsCount.value)} 条`,
    tone: 'warning',
    icon: '警'
  },
  {
    key: 'follow',
    label: '本周完成随访',
    value: formatNumber(weekFollowDone.value),
    unit: '次',
    meta: `活跃医生 ${formatNumber(activeDoctorsCount.value)} 人`,
    tone: 'secondary',
    icon: '访'
  }
])

function monthLabel(row: AnyRecord, index: number): string {
  const raw = sanitizeText(
    row.label ?? row.month ?? row.period ?? row.name ?? row.date ?? row.statMonth
  )
  if (!raw) return `M${index + 1}`
  return raw.length > 6 ? raw.slice(-5) : raw
}

const trendSeries = computed<TrendItem[]>(() => {
  const rows = extractArray(monthSummary.value).slice(-8)
  const normalized = rows.map((row, index) => ({
    label: monthLabel(row, index),
    risk: pickNumber(row.risk, row.highRisk, row.highCount, row.riskCount, row.riskTotal) ?? 0,
    alert: pickNumber(row.alert, row.alertCount, row.warningCount, row.alarmCount, row.eventCount) ?? 0,
    follow: pickNumber(row.follow, row.followup, row.followCount, row.followupCount, row.taskCount, row.visitCount) ?? 0
  }))

  const fallback = normalized.length > 0 ? normalized : Array.from({ length: 6 }, (_, index) => ({
    label: `M${index + 1}`,
    risk: 0,
    alert: 0,
    follow: 0
  }))

  const maxValue = Math.max(
    1,
    ...fallback.flatMap((item) => [item.risk, item.alert, item.follow])
  )

  return fallback.map((item) => ({
    ...item,
    riskHeight: Math.max(8, Math.round((item.risk / maxValue) * 100)),
    alertHeight: Math.max(8, Math.round((item.alert / maxValue) * 100)),
    followHeight: Math.max(8, Math.round((item.follow / maxValue) * 100))
  }))
})

function buildPolyline(values: number[], key: keyof TrendItem) {
  const count = values.length
  if (count === 0) return ''
  const max = Math.max(1, ...values)
  return values
    .map((value, index) => {
      const x = count === 1 ? 500 : (index / (count - 1)) * 1000
      const y = 260 - (value / max) * 180
      return `${x},${y}`
    })
    .join(' ')
}

const riskPolyline = computed(() => buildPolyline(trendSeries.value.map((item) => item.risk), 'risk'))
const alertPolyline = computed(() => buildPolyline(trendSeries.value.map((item) => item.alert), 'alert'))
const followPolyline = computed(() => buildPolyline(trendSeries.value.map((item) => item.follow), 'follow'))

const hubGradient = computed(() => {
  const total = Math.max(1, patientTotal.value)
  const high = clampPercent((highRiskPatients.value / total) * 100)
  const pending = clampPercent((pendingAlertsCount.value / total) * 100)
  const mid = Math.max(10, 100 - high - pending)
  return `conic-gradient(
    rgba(82, 195, 214, 0.92) 0 ${Math.max(6, high)}%,
    rgba(255, 174, 92, 0.88) ${Math.max(6, high)}% ${Math.max(12, high + pending)}%,
    rgba(122, 217, 169, 0.82) ${Math.max(12, high + pending)}% 100%
  )`
})

const hubRiskSummary = computed(() => {
  if (highRiskPatients.value <= 0) return '平稳'
  if (highRiskPatients.value < 10) return '轻度预警'
  if (highRiskPatients.value < 50) return '中度预警'
  return '重点预警'
})

function eventLabel(row: AnyRecord): string {
  const patientCode = sanitizeText(
    row.patientCode ?? row.patientId ?? row.userId ?? row.archiveNo ?? row.code
  )
  const title = sanitizeText(
    row.title ?? row.alertTypeName ?? row.deviceType ?? row.reason ?? row.message ?? row.content
  )
  const level = sanitizeText(row.levelName ?? row.level ?? row.riskLevel ?? row.severity)
  const ts = sanitizeText(row.time ?? row.createTime ?? row.createdAt ?? row.alertTime)
  const shortTs = ts ? `【${ts.slice(11, 16) || ts.slice(0, 16)}】` : '【实时】'
  const bits = [patientCode ? `患者 ${patientCode}` : '', title, level ? `等级 ${level}` : ''].filter(Boolean)
  return `${shortTs}${bits.join('，')}`
}

const tickerItems = computed(() => {
  const merged = [...alertRows.value, ...hardwareRows.value]
    .slice(0, 10)
    .map(eventLabel)
    .filter(Boolean)

  return merged.length > 0 ? merged : ['【实时】系统运行正常，当前暂无新的异常播报']
})

const systemStatusText = computed(() => {
  if (pendingAlertsCount.value > 0) return `系统运行正常 · 待处理告警 ${formatNumber(pendingAlertsCount.value)} 条`
  return '系统运行正常'
})
</script>

<style scoped>
.command-center-page {
  position: relative;
  display: grid;
  gap: 18px;
  padding-bottom: 108px;
  min-height: 100%;
}

.glass-card {
  position: relative;
  border-radius: 22px;
  border: 1px solid rgba(159, 214, 226, 0.28);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.82), rgba(244, 251, 252, 0.68)),
    rgba(255, 255, 255, 0.5);
  box-shadow:
    0 14px 34px rgba(83, 132, 149, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
}

.overview-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.metric-card {
  display: grid;
  grid-template-columns: 56px 1fr;
  gap: 14px;
  align-items: center;
  padding: 16px 18px;
}

.metric-icon {
  width: 56px;
  height: 56px;
  border-radius: 18px;
  display: grid;
  place-items: center;
  font-size: 22px;
  font-weight: 700;
  color: #165f70;
  background: rgba(103, 208, 223, 0.16);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.65);
}

.metric-icon--danger { background: rgba(255, 121, 121, 0.16); color: #b25a5a; }
.metric-icon--warning { background: rgba(255, 183, 96, 0.18); color: #a36a18; }
.metric-icon--secondary { background: rgba(115, 163, 255, 0.16); color: #4f74c7; }

.metric-body {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.metric-label {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.04em;
  color: rgba(71, 102, 111, 0.78);
}

.metric-main {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.metric-main strong {
  font-size: clamp(24px, 2vw, 34px);
  line-height: 1;
  font-weight: 700;
  color: #1d4752;
}

.metric-main em {
  font-style: normal;
  color: rgba(78, 112, 122, 0.78);
  font-size: 12px;
}

.metric-meta {
  margin: 0;
  font-size: 12px;
  color: rgba(75, 110, 119, 0.74);
}

.hero-grid {
  display: grid;
  grid-template-columns: minmax(290px, 1fr) minmax(560px, 2.35fr) minmax(300px, 1fr);
  gap: 18px;
  align-items: stretch;
}

.side-col,
.hub-col {
  display: grid;
  gap: 18px;
}

.info-panel,
.trend-panel {
  padding: 18px 18px 16px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 16px;
}

.panel-head h3 {
  margin: 0;
  font-size: 16px;
  line-height: 1.15;
  color: #183d46;
}

.panel-head p {
  margin: 6px 0 0;
  font-size: 12px;
  color: rgba(72, 110, 120, 0.72);
}

.panel-empty {
  min-height: 180px;
  display: grid;
  place-items: center;
  border-radius: 18px;
  border: 1px dashed rgba(144, 193, 206, 0.32);
  background: rgba(255, 255, 255, 0.36);
  color: rgba(84, 118, 126, 0.72);
  font-size: 13px;
}

.panel-loading {
  position: relative;
  overflow: hidden;
}

.panel-loading::after {
  content: '';
  position: absolute;
  inset: 0;
  transform: translateX(-100%);
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.45), transparent);
  animation: sweep 1.6s linear infinite;
}

@keyframes sweep {
  to { transform: translateX(100%); }
}

.rank-list {
  display: grid;
  gap: 12px;
}

.rank-item {
  display: grid;
  gap: 8px;
}

.rank-row,
.rank-meta,
.doctor-row,
.doctor-sub {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.rank-name,
.doctor-name {
  font-size: 13px;
  color: #21454e;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-value,
.rank-meta,
.doctor-load,
.doctor-sub {
  font-size: 12px;
  color: rgba(79, 114, 123, 0.76);
}

.rank-bar {
  height: 8px;
  border-radius: 999px;
  background: rgba(129, 182, 194, 0.18);
  overflow: hidden;
}

.rank-bar-fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, rgba(99, 213, 224, 0.82), rgba(102, 159, 255, 0.84));
}

.doctor-list {
  display: grid;
  gap: 10px;
}

.doctor-item {
  display: grid;
  grid-template-columns: 42px 1fr;
  gap: 12px;
  align-items: center;
  padding: 12px 12px 11px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.46);
  border: 1px solid rgba(151, 204, 214, 0.22);
}

.doctor-avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: rgba(93, 200, 220, 0.16);
  color: #215c6a;
  font-size: 16px;
  font-weight: 700;
}

.doctor-body {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.doctor-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(90, 190, 145, 0.8);
  display: inline-block;
}

.doctor-dot--warm {
  background: rgba(255, 153, 91, 0.92);
  box-shadow: 0 0 0 6px rgba(255, 153, 91, 0.12);
}

.hub-panel {
  padding: 20px;
  min-height: 540px;
}

.hub-shell {
  position: relative;
  min-height: 500px;
  border-radius: 30px;
  overflow: hidden;
  display: grid;
  place-items: center;
  background:
    radial-gradient(circle at center, rgba(255,255,255,0.95) 0, rgba(239,248,250,0.88) 42%, rgba(227,242,246,0.58) 76%, rgba(219,236,240,0.12) 100%);
}

.hub-glow {
  position: absolute;
  width: 68%;
  aspect-ratio: 1;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(109, 209, 223, 0.22), rgba(109, 209, 223, 0.03) 70%, transparent 85%);
  filter: blur(16px);
}

.hub-orbit {
  position: absolute;
  border-radius: 50%;
  border: 1px dashed rgba(122, 184, 198, 0.38);
}

.hub-orbit--outer {
  width: 74%;
  aspect-ratio: 1;
  animation: rotateSlow 22s linear infinite;
}

.hub-orbit--inner {
  width: 58%;
  aspect-ratio: 1;
  border-style: solid;
  border-color: rgba(145, 204, 216, 0.26);
}

@keyframes rotateSlow {
  to { transform: rotate(360deg); }
}

.hub-ring {
  position: absolute;
  width: 48%;
  aspect-ratio: 1;
  border-radius: 50%;
  mask: radial-gradient(circle, transparent 50%, #000 51%);
  opacity: 0.94;
  box-shadow: 0 0 0 1px rgba(255,255,255,0.5), 0 12px 32px rgba(72, 131, 149, 0.12);
}

.hub-core {
  position: relative;
  z-index: 2;
  width: 42%;
  aspect-ratio: 1;
  border-radius: 50%;
  display: grid;
  place-items: center;
  align-content: center;
  text-align: center;
  padding: 22px;
  background: rgba(255,255,255,0.66);
  border: 1px solid rgba(160, 212, 222, 0.26);
  box-shadow: inset 0 1px 0 rgba(255,255,255,0.8), 0 18px 36px rgba(86, 140, 154, 0.08);
}

.hub-kicker {
  margin: 0 0 4px;
  font-size: 11px;
  letter-spacing: 0.08em;
  color: rgba(77, 113, 123, 0.72);
}

.hub-core h2 {
  margin: 0;
  font-size: clamp(20px, 2.2vw, 30px);
  color: #174852;
}

.hub-caption {
  margin: 6px 0 12px;
  font-size: 12px;
  color: rgba(75, 111, 121, 0.72);
}

.hub-value {
  font-size: clamp(26px, 2.8vw, 38px);
  font-weight: 700;
  color: #1d4853;
  line-height: 1;
}

.hub-value-label {
  margin: 8px 0 14px;
  font-size: 12px;
  color: rgba(77, 114, 124, 0.76);
}

.hub-mini-grid {
  width: 100%;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.hub-mini-card {
  display: grid;
  gap: 4px;
  padding: 10px 8px;
  border-radius: 14px;
  background: rgba(246, 252, 253, 0.72);
  border: 1px solid rgba(165, 213, 222, 0.18);
}

.hub-mini-card span {
  font-size: 11px;
  color: rgba(77, 113, 123, 0.74);
}

.hub-mini-card strong {
  font-size: 16px;
  color: #1e4e58;
}

.hub-node {
  position: absolute;
  z-index: 2;
  display: grid;
  gap: 4px;
  min-width: 112px;
  padding: 10px 12px;
  border-radius: 16px;
  background: rgba(255,255,255,0.64);
  border: 1px solid rgba(164, 213, 222, 0.24);
  box-shadow: 0 10px 26px rgba(74, 126, 140, 0.08);
  text-align: center;
}

.hub-node strong {
  font-size: 12px;
  color: #19424b;
}

.hub-node span {
  font-size: 12px;
  color: rgba(78, 114, 123, 0.78);
}

.hub-node--top { top: 10%; left: 50%; transform: translateX(-50%); }
.hub-node--right { right: 6%; top: 50%; transform: translateY(-50%); }
.hub-node--bottom { bottom: 10%; left: 50%; transform: translateX(-50%); }
.hub-node--left { left: 6%; top: 50%; transform: translateY(-50%); }

.efficiency-wrap {
  display: grid;
  gap: 14px;
}

.efficiency-ring {
  --eff: 0%;
  width: min(220px, 100%);
  aspect-ratio: 1;
  margin: 0 auto;
  border-radius: 50%;
  background: conic-gradient(rgba(94, 204, 220, 0.96) 0 var(--eff), rgba(218, 234, 238, 0.66) var(--eff) 100%);
  display: grid;
  place-items: center;
  box-shadow: inset 0 1px 0 rgba(255,255,255,0.72);
}

.efficiency-inner {
  width: 72%;
  aspect-ratio: 1;
  border-radius: 50%;
  background: rgba(255,255,255,0.88);
  display: grid;
  place-items: center;
  align-content: center;
  box-shadow: 0 10px 22px rgba(76, 129, 142, 0.08);
}

.efficiency-inner strong {
  font-size: 30px;
  color: #18454f;
}

.efficiency-inner span {
  font-size: 12px;
  color: rgba(77, 111, 120, 0.74);
}

.efficiency-meta,
.trend-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 18px;
  font-size: 12px;
  color: rgba(77, 112, 121, 0.78);
}

.dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 6px;
}

.dot--primary { background: rgba(94, 204, 220, 0.96); }
.dot--muted { background: rgba(174, 200, 208, 0.86); }
.dot--risk { background: rgba(82, 195, 214, 0.96); }
.dot--alert { background: rgba(255, 174, 92, 0.96); }
.dot--follow { background: rgba(114, 198, 141, 0.96); }

.matrix-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.matrix-item {
  display: grid;
  justify-items: center;
  gap: 8px;
  padding: 12px 10px 8px;
  border-radius: 18px;
  background: rgba(255,255,255,0.42);
  border: 1px solid rgba(153, 205, 215, 0.22);
}

.mini-ring {
  --mini-ring: 0%;
  width: 112px;
  aspect-ratio: 1;
  border-radius: 50%;
  background: conic-gradient(rgba(94, 204, 220, 0.96) 0 var(--mini-ring), rgba(218, 234, 238, 0.66) var(--mini-ring) 100%);
  display: grid;
  place-items: center;
}

.mini-ring--placeholder {
  background: conic-gradient(rgba(194, 214, 219, 0.56) 0 100%, rgba(218, 234, 238, 0.66) 100% 100%);
}

.mini-ring-inner {
  width: 72%;
  aspect-ratio: 1;
  border-radius: 50%;
  background: rgba(255,255,255,0.88);
  display: grid;
  place-items: center;
}

.mini-ring-inner strong {
  font-size: 18px;
  color: #1a4751;
}

.matrix-item span {
  font-size: 12px;
  color: rgba(77, 112, 121, 0.78);
}

.trend-head {
  margin-bottom: 18px;
}

.trend-switch {
  display: flex;
  gap: 8px;
}

.trend-btn {
  border: 1px solid rgba(152, 204, 214, 0.24);
  background: rgba(255,255,255,0.4);
  color: rgba(77, 112, 121, 0.78);
  border-radius: 999px;
  padding: 6px 10px;
  font-size: 12px;
  cursor: pointer;
}

.trend-btn--active {
  color: #184651;
  background: rgba(110, 211, 225, 0.16);
}

.trend-stage {
  position: relative;
  min-height: 300px;
  padding-top: 20px;
}

.trend-gridline {
  position: absolute;
  left: 0;
  right: 0;
  border-top: 1px dashed rgba(149, 192, 201, 0.18);
}

.trend-svg {
  position: absolute;
  inset: 0 0 30px 0;
  width: 100%;
  height: calc(100% - 30px);
  overflow: visible;
}

.trend-line {
  fill: none;
  stroke-width: 4;
  stroke-linecap: round;
  stroke-linejoin: round;
  opacity: 0.88;
}

.trend-line--risk { stroke: rgba(82, 195, 214, 0.96); }
.trend-line--alert { stroke: rgba(255, 174, 92, 0.96); }
.trend-line--follow { stroke: rgba(114, 198, 141, 0.96); }

.trend-bars {
  position: relative;
  z-index: 2;
  height: 280px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(64px, 1fr));
  gap: 10px;
  align-items: end;
}

.trend-col {
  display: grid;
  gap: 10px;
}

.trend-bar-wrap {
  height: 240px;
  display: flex;
  align-items: end;
  justify-content: center;
  gap: 6px;
}

.trend-bar {
  width: 14px;
  border-radius: 999px 999px 6px 6px;
  min-height: 8px;
  box-shadow: 0 6px 18px rgba(84, 134, 148, 0.08);
}

.trend-bar--risk { background: linear-gradient(180deg, rgba(82,195,214,0.88), rgba(82,195,214,0.34)); }
.trend-bar--alert { background: linear-gradient(180deg, rgba(255,174,92,0.88), rgba(255,174,92,0.32)); }
.trend-bar--follow { background: linear-gradient(180deg, rgba(114,198,141,0.88), rgba(114,198,141,0.32)); }

.trend-label {
  text-align: center;
  font-size: 11px;
  color: rgba(77, 112, 121, 0.76);
}

.status-band {
  position: fixed;
  left: 18px;
  right: 18px;
  bottom: 14px;
  z-index: 30;
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 16px;
  align-items: center;
  padding: 12px 14px;
}

.status-left {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: #1b4a54;
  white-space: nowrap;
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: rgba(92, 200, 142, 0.92);
  box-shadow: 0 0 0 6px rgba(92, 200, 142, 0.12);
}

.ticker {
  min-width: 0;
  overflow: hidden;
  border-left: 1px solid rgba(155, 202, 212, 0.16);
  padding-left: 14px;
}

.ticker-track {
  display: inline-flex;
  gap: 28px;
  white-space: nowrap;
  animation: tickerMove 24s linear infinite;
}

.ticker-item {
  font-size: 12px;
  color: rgba(74, 110, 119, 0.78);
}

@keyframes tickerMove {
  from { transform: translateX(0); }
  to { transform: translateX(-50%); }
}

@media (max-width: 1440px) {
  .overview-row { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .hero-grid { grid-template-columns: 1fr; }
  .hub-panel { min-height: 460px; }
}

@media (max-width: 960px) {
  .command-center-page { padding-bottom: 124px; }
  .overview-row { grid-template-columns: 1fr; }
  .status-band {
    grid-template-columns: 1fr;
    gap: 8px;
    left: 10px;
    right: 10px;
  }
}

</style>
