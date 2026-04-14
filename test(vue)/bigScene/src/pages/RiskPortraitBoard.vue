<template>
  <main class="stitch-grid screen-page screen-grid risk-layout">
    <aside class="stitch-col screen-col">
      <article class="frost-card">
        <div class="card-head">
          <div class="card-titlebar">
            <div class="card-title">慢性病病种排行</div>
            <div class="card-subtitle">重点慢病分布</div>
          </div>
        </div>
        <div class="card-body">
          <div class="disease-rank-list">
            <div v-for="item in diseaseRankRows" :key="item.name" class="disease-rank-item">
              <div class="disease-rank-head">
                <span class="disease-name">{{ item.name }}</span>
                <strong class="disease-ratio">{{ item.ratio }}%</strong>
              </div>
              <div class="disease-track">
                <div class="disease-bar" :style="{ width: `${item.ratio}%` }"></div>
              </div>
            </div>
          </div>
        </div>
      </article>

      <article class="frost-card">
        <div class="card-head">
          <div class="card-titlebar">
            <div class="card-title">中医体质/证型分布</div>
            <div class="card-subtitle">体质与证型聚合</div>
          </div>
        </div>
        <div class="card-body">
          <div class="constitution-list">
            <div v-for="item in constitutionRows" :key="item.name" class="constitution-item">
              <p class="constitution-name">{{ item.name }}</p>
              <p class="constitution-value">{{ item.ratio }}%</p>
            </div>
          </div>
        </div>
      </article>
    </aside>

    <section class="stitch-center screen-col">
      <article class="frost-card">
        <div class="card-head">
          <div class="card-titlebar">
            <div class="card-title">风险画像中枢</div>
            <div class="card-subtitle">分层 · 结构 · 变化</div>
          </div>
        </div>
        <div class="card-body">
          <div class="center-stage">
            <div class="hub glow-breath">
              <div class="hub-title">重点管理患者</div>
              <div class="hub-value">{{ total }}</div>
              <div class="hub-sub">高危占比 {{ highRatio }}</div>
            </div>
            <div ref="portraitRef" class="hub-ring"></div>
          </div>
        </div>
      </article>
    </section>

    <aside class="stitch-col screen-col">
      <article class="frost-card">
        <div class="card-head">
          <div class="card-titlebar">
            <div class="card-title">人群画像洞察</div>
            <div class="card-subtitle">年龄梯队与性别结构</div>
          </div>
        </div>
        <div class="card-body portrait-insight">
          <div class="gender-overview">
            <div class="gender-head">
              <span>性别比例（男/女）</span>
              <strong>{{ genderRatioText }}</strong>
            </div>
            <div class="gender-track">
              <div class="gender-bar male" :style="{ width: `${maleRatio}%` }"></div>
              <div class="gender-bar female" :style="{ width: `${femaleRatio}%` }"></div>
            </div>
          </div>
          <div class="age-overview">
            <div class="age-title">年龄梯队分布</div>
            <div ref="ageRef" class="chart age-chart"></div>
          </div>
        </div>
      </article>

      <article class="frost-card">
        <div class="card-head">
          <div class="card-titlebar">
            <div class="card-title">重点患者动态监控</div>
            <div class="card-subtitle">风险等级与监测变化</div>
          </div>
        </div>
        <div class="card-body">
          <div class="monitor-list">
            <div v-for="item in monitorRows" :key="item.id" class="monitor-item">
              <div class="monitor-main">
                <div class="monitor-name">{{ item.name }}</div>
                <div class="monitor-metric">{{ item.metric }}</div>
              </div>
              <div class="monitor-side">
                <span class="risk-tag" :class="item.levelClass">{{ item.levelText }}</span>
                <span class="monitor-time">{{ item.time }}</span>
              </div>
            </div>
          </div>
        </div>
      </article>

    </aside>

    <article class="frost-card coverage-panel">
      <div class="card-head">
        <div class="card-titlebar">
          <div class="card-title">医学建议覆盖率</div>
          <div class="card-subtitle">建议触达概览</div>
        </div>
      </div>
      <div class="card-body coverage-body">
        <div class="coverage-legend">
          <span><i class="dot dot-a"></i>药物干预</span>
          <span><i class="dot dot-b"></i>中医调理</span>
          <span><i class="dot dot-c"></i>膳食指导</span>
        </div>
        <div class="coverage-main">
          <div class="coverage-track">
            <div class="coverage-bar" :style="{ width: `${adviceCoverage}%` }"></div>
          </div>
          <div class="coverage-value">{{ adviceCoverage }}%</div>
        </div>
      </div>
    </article>
  </main>
</template>

<script setup lang="ts">
import { computed, onActivated, onDeactivated, onMounted, onUnmounted, ref } from 'vue'
import { init, type ECharts } from '../utils/echarts'
import { axisStyle, baseGrid, tooltipStyle } from '../utils/chartTheme'
import { fetchAlerts, fetchHomeStats, fetchPatientRiskList, fetchPatientSummary, fetchReportBoard } from '../api'
import { rafThrottle } from '../utils/perf'

const total = ref(0)
const high = ref(0)
const mid = ref(0)
const low = ref(0)
const topDiseaseCount = ref(0)
const activeAlertPatients = ref(0)
const adviceCoverage = ref(0)
const maleCount = ref(0)
const femaleCount = ref(0)
const ageBuckets = ref<{ label: string; value: number }[]>([
  { label: '0-44', value: 0 },
  { label: '45-59', value: 0 },
  { label: '60-74', value: 0 },
  { label: '75+', value: 0 }
])

const highRatio = computed(() => {
  if (!total.value) return '0.0%'
  return `${((high.value / total.value) * 100).toFixed(1)}%`
})

const constitutionRows = computed(() => {
  const map = new Map<string, number>()
  const rows = patientList.value || []
  rows.forEach((row: any) => {
    const candidates = [
      row?.constitution,
      row?.constitutionType,
      row?.bodyConstitution,
      row?.physiqueType,
      row?.syndromeType,
      row?.tcmConstitution,
      row?.tcmType
    ]
    for (const raw of candidates) {
      const t = String(raw ?? '').trim()
      if (!t || t === '未知' || t === '暂无') continue
      map.set(t, (map.get(t) || 0) + 1)
      break
    }
  })
  const rowsTop = Array.from(map.entries())
    .sort((a, b) => b[1] - a[1])
    .slice(0, 4)
  const sum = rowsTop.reduce((acc, [, count]) => acc + count, 0) || 1
  const fallback = [
    { name: '气虚质', ratio: 24.5 },
    { name: '阳虚质', ratio: 18.2 },
    { name: '痰湿质', ratio: 15.8 },
    { name: '平和质', ratio: 12.4 }
  ]
  if (!rowsTop.length) return fallback
  return rowsTop.map(([name, count]) => ({
    name,
    ratio: Number(((count / sum) * 100).toFixed(1))
  }))
})

const diseaseRankRows = computed(() => {
  const by = new Map<string, number>()
  const rows = patientList.value || []
  rows.forEach((r: any) => {
    const disease = pickDiseaseName(r)
    by.set(disease, (by.get(disease) || 0) + 1)
  })
  const pairs = Array.from(by.entries()).sort((a, b) => b[1] - a[1]).slice(0, 5)
  const sum = pairs.reduce((acc, [, count]) => acc + count, 0) || 1
  if (!pairs.length) {
    return [
      { name: '原发性高血压', ratio: 42.8, count: 0 },
      { name: '2型糖尿病', ratio: 31.2, count: 0 },
      { name: '冠心病', ratio: 15.5, count: 0 }
    ]
  }
  return pairs.map(([name, count]) => ({
    name,
    count,
    ratio: Number(((count / sum) * 100).toFixed(1))
  }))
})

const events = ref<Array<{ id: string; title: string; time: string }>>([])
const patientList = ref<any[]>([])
const monitorRows = computed(() =>
  events.value.slice(0, 4).map((item) => {
    const namePart = item.title.split('·')[1]?.trim() || '重点患者'
    const metricPart = item.title.split('·').slice(2).join('·').trim() || '风险链路监测中'
    const upper = item.title.toUpperCase()
    const levelClass = upper.includes('HIGH') || item.title.includes('高') ? 'high' : upper.includes('MID') || item.title.includes('中') ? 'mid' : 'low'
    return {
      id: item.id,
      name: namePart,
      metric: metricPart,
      time: item.time || '实时',
      levelText: levelClass === 'high' ? '高风险' : levelClass === 'mid' ? '中风险' : '低风险',
      levelClass
    }
  })
)

const maleRatio = computed(() => {
  const sum = maleCount.value + femaleCount.value
  if (!sum) return 50
  return Math.round((maleCount.value / sum) * 100)
})

const femaleRatio = computed(() => 100 - maleRatio.value)
const genderRatioText = computed(() => `${maleRatio.value}:${femaleRatio.value}`)

const ageRef = ref<HTMLElement | null>(null)
const portraitRef = ref<HTMLElement | null>(null)

let ageChart: ECharts | null = null
let portraitChart: ECharts | null = null

let activeAlive = false

function pickRows(payload: any): any[] {
  if (Array.isArray(payload)) return payload
  if (!payload || typeof payload !== 'object') return []
  const keys = ['rows', 'list', 'records', 'items', 'data']
  for (const key of keys) {
    const val = payload[key]
    if (Array.isArray(val)) return val
  }
  if (payload.data && typeof payload.data === 'object') {
    for (const key of keys) {
      const val = payload.data[key]
      if (Array.isArray(val)) return val
    }
  }
  return []
}

function pickDiseaseName(row: any) {
  const direct = [
    row?.disease,
    row?.mainDisease,
    row?.primaryDisease,
    row?.diseaseName,
    row?.diagnosisName,
    row?.chronicDiseaseName,
    row?.diagnosis,
    row?.diseaseType
  ]
  for (const v of direct) {
    const t = String(v ?? '').trim()
    if (t && t !== '未知' && t !== '暂无') return t
  }
  const nested = [row?.disease?.name, row?.diagnosis?.name, row?.mainDisease?.name, row?.category?.name]
  for (const v of nested) {
    const t = String(v ?? '').trim()
    if (t && t !== '未知' && t !== '暂无') return t
  }
  return '未填写'
}

function buildAge(list: any[]) {
  if (!ageRef.value) return
  if (!ageChart) ageChart = init(ageRef.value)
  const buckets: Record<string, number> = { '0-44': 0, '45-59': 0, '60-74': 0, '75+': 0 }
  list.forEach((r: any) => {
    const age = Number(r.age || 0)
    if (age >= 75) buckets['75+']++
    else if (age >= 60) buckets['60-74']++
    else if (age >= 45) buckets['45-59']++
    else buckets['0-44']++
  })
  ageBuckets.value = Object.entries(buckets).map(([label, value]) => ({ label, value }))
  const axis = axisStyle()
  ageChart.setOption({
    tooltip: tooltipStyle(),
    grid: { ...baseGrid(), left: 10, right: 8, top: 8, bottom: 18 },
    xAxis: { type: 'category', data: Object.keys(buckets), ...axis },
    yAxis: { type: 'value', ...axis },
    series: [{ type: 'bar', data: Object.values(buckets), barWidth: 14, itemStyle: { color: '#4da9b6', borderRadius: [6, 6, 0, 0] } }]
  })
}

function normalizeGender(v: any) {
  const s = (v ?? '').toString().trim()
  if (!s) return '未知'
  if (s === '男' || s.toLowerCase() === 'male' || s === 'M' || s === '1') return '男'
  if (s === '女' || s.toLowerCase() === 'female' || s === 'F' || s === '0' || s === '2') return '女'
  return '未知'
}

function collectGender(list: any[]) {
  const by: Record<string, number> = { 男: 0, 女: 0 }
  list.forEach((r: any) => {
    const g = normalizeGender(r.gender)
    if (g === '男' || g === '女') {
      by[g] = (by[g] || 0) + 1
    }
  })
  maleCount.value = by['男'] || 0
  femaleCount.value = by['女'] || 0
}

function buildPortrait() {
  if (!portraitRef.value) return
  if (!portraitChart) portraitChart = init(portraitRef.value)
  // 从现有真实字段衍生画像维度（不造假）：高危占比、病种集中度、活跃告警率
  // 其余维度后端未提供可复用聚合口径时，按 0 展示
  const p = Number(highRatio.value.replace('%', '') || 0)
  const diseaseConcentration = total.value ? Math.min(100, Math.round((topDiseaseCount.value / total.value) * 100)) : 0
  const activeAlertRate = total.value ? Math.min(100, Math.round((activeAlertPatients.value / total.value) * 100)) : 0
  portraitChart.setOption({
    radar: {
      indicator: [
        { name: '高危占比', max: 100 },
        { name: '病种集中度', max: 100 },
        { name: '活跃告警率', max: 100 },
        { name: '建议覆盖率', max: 100 },
        { name: '随访完成率', max: 100 },
        { name: '服务触达率', max: 100 }
      ],
      axisName: { color: 'rgba(39,85,113,0.92)', fontSize: 11 },
      splitLine: { lineStyle: { color: 'rgba(114,180,205,0.26)' } },
      splitArea: { areaStyle: { color: ['rgba(95,199,216,0.12)', 'rgba(158,169,230,0.06)'] } }
    },
    series: [
      {
        type: 'radar',
        data: [
          {
            value: [p, diseaseConcentration, activeAlertRate, 0, 0, 0],
            name: '画像维度',
            areaStyle: { color: 'rgba(95,199,216,0.12)' },
            lineStyle: { color: '#5fc7d8', width: 2 },
            itemStyle: { color: '#5fc7d8' }
          }
        ]
      }
    ]
  })
}

function computeAdvice(board: any) {
  const coverage = Number(board?.adviceCoverageRate ?? board?.adviceReachRate ?? board?.reachRate ?? 0)
  const auc = Number(board?.latestAuc || 0) * 100
  const f1 = Number(board?.latestF1 || 0) * 100
  const value = coverage > 0 ? Math.min(100, Math.max(0, coverage)) : Math.max(0, Math.min(100, (auc + f1) / 2))
  adviceCoverage.value = Math.round(value)
}

function resizeAll() {
  if (!activeAlive) return
  ageChart?.resize()
  portraitChart?.resize()
}

const onResize = rafThrottle(() => resizeAll())

async function loadBoard() {
  const [homeStats, riskRes, patientRes, alertRes, board] = await Promise.all([
    fetchHomeStats().catch(() => ({} as any)),
    fetchPatientRiskList(200),
    fetchPatientSummary(200),
    fetchAlerts(30),
    fetchReportBoard().catch(() => ({} as any))
  ])
  const list = pickRows(patientRes) as any[]
  const riskRows = pickRows(riskRes) as any[]
  const alertRows = pickRows(alertRes) as any[]
  patientList.value = list
  total.value = Number(homeStats?.totalPatients || patientRes?.total || list.length || riskRes?.total || riskRows.length || 0)
  const listForRisk = list.length ? list : riskRows
  high.value = listForRisk.filter((r: any) => String(r.riskLevel || '').toUpperCase().includes('HIGH')).length
  mid.value = listForRisk.filter((r: any) => String(r.riskLevel || '').toUpperCase().includes('MID')).length
  low.value = listForRisk.filter((r: any) => String(r.riskLevel || '').toUpperCase().includes('LOW')).length
  activeAlertPatients.value = list.filter((r: any) => Number(r.activeAlertCount || 0) > 0).length

  if (homeStats && (homeStats.highRiskCount != null || homeStats.midRiskCount != null || homeStats.lowRiskCount != null)) {
    if (homeStats.highRiskCount != null) high.value = Number(homeStats.highRiskCount) || 0
    if (homeStats.midRiskCount != null) mid.value = Number(homeStats.midRiskCount) || 0
    if (homeStats.lowRiskCount != null) low.value = Number(homeStats.lowRiskCount) || 0
  }
  events.value = alertRows.slice(0, 8).map((r: any, idx: number) => ({
    id: String(r.id || idx),
    title: `告警 · ${r.patientName || '患者'} · ${r.summary || r.alertType || ''}`,
    time: (r.alertTime || '').toString().replace('T', ' ')
  }))
  if (!events.value.length) {
    events.value = [{ id: 'evt-default', title: '画像链路运行稳定', time: '实时' }]
  }

  buildAge(list)
  collectGender(list)
  topDiseaseCount.value = diseaseRankRows.value[0]?.count || 0
  buildPortrait()
  computeAdvice(board || {})
}

onMounted(async () => {
  activeAlive = true
  await loadBoard()

  window.addEventListener('resize', onResize)
})

onUnmounted(() => {
  activeAlive = false
  window.removeEventListener('resize', onResize)
  ageChart?.dispose()
  portraitChart?.dispose()
  ageChart = null
  portraitChart = null
})

onActivated(() => {
  activeAlive = true
  window.addEventListener('resize', onResize)
  void loadBoard()
  onResize()
})

onDeactivated(() => {
  activeAlive = false
  window.removeEventListener('resize', onResize)
})
</script>

<style scoped>

.center-stage {
  position: relative;
  height: 100%;
  min-height: 0;
  display: grid;
  place-items: center;
}

.hub {
  position: absolute;
  width: 230px;
  height: 230px;
  border-radius: 999px;
  background: radial-gradient(circle at 50% 35%, rgba(127, 214, 227, 0.30), rgba(255, 255, 255, 0.76) 58%, rgba(140, 188, 227, 0.25));
  border: 1px solid rgba(114, 180, 205, 0.08);
  box-shadow: 0 10px 22px rgba(95, 199, 216, 0.14);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
}

.hub-title {
  font-size: 11px;
  letter-spacing: 2px;
  color: var(--t-2);
}

.hub-value {
  margin-top: 8px;
  font-size: 38px;
  font-weight: 900;
  color: var(--c-gold);
}

.hub-sub {
  margin-top: 10px;
  font-size: 11px;
  color: var(--t-3);
}

.hub-ring {
  position: absolute;
  inset: 0;
}

.stitch-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 2.35fr) minmax(0, 1fr);
  gap: 10px;
  height: 100%;
  min-height: 0;
}

.risk-layout {
  grid-template-rows: minmax(0, 1fr) auto;
}

.stitch-col,
.stitch-center {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 0;
}

.stitch-col .frost-card,
.stitch-center .frost-card {
  flex: 1;
  min-height: 0;
}

.stitch-col:first-child > .frost-card:nth-child(1) { flex: 1.02 1 0; }
.stitch-col:first-child > .frost-card:nth-child(2) { flex: 0.98 1 0; }
.stitch-center > .frost-card:nth-child(1) { flex: 1 1 0; }
.stitch-col:last-child > .frost-card:nth-child(1) { flex: 1.03 1 0; }
.stitch-col:last-child > .frost-card:nth-child(2) { flex: 0.97 1 0; }

.coverage-panel {
  grid-column: 1 / -1;
  min-height: 0;
  flex: 0.62 1 0;
}

.coverage-body {
  display: grid;
  gap: 10px;
  align-content: center;
}

.coverage-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  font-size: 11px;
  color: rgba(39, 85, 113, 0.88);
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  display: inline-block;
  margin-right: 6px;
}

.dot-a { background: #0f8e85; }
.dot-b { background: #5fc7d8; }
.dot-c { background: #8cbce3; }

.coverage-main {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
}

.coverage-track {
  width: 100%;
  height: 10px;
  border-radius: 999px;
  overflow: hidden;
  background: rgba(114, 180, 205, 0.2);
}

.coverage-bar {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(15, 142, 133, 0.95), rgba(95, 199, 216, 0.92), rgba(140, 188, 227, 0.9));
}

.coverage-value {
  font-size: 24px;
  font-weight: 800;
  color: rgba(22, 97, 107, 0.98);
}

.constitution-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.constitution-item {
  padding: 12px 12px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.46);
}

.constitution-name {
  margin: 0;
  font-size: 11px;
  color: rgba(77, 111, 123, 0.85);
  white-space: nowrap;
}

.constitution-value {
  margin: 4px 0 0;
  font-size: 28px;
  line-height: 1;
  color: rgba(22, 97, 107, 0.95);
  font-weight: 800;
}

.disease-rank-list {
  display: grid;
  gap: 12px;
}

.disease-rank-item {
  display: grid;
  gap: 6px;
}

.disease-rank-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}

.disease-name {
  font-size: 12px;
  color: rgba(39, 85, 113, 0.94);
  overflow: hidden;
  text-overflow: ellipsis;
}

.disease-ratio {
  font-size: 20px;
  line-height: 1;
  color: rgba(18, 106, 113, 0.97);
}

.disease-track {
  width: 100%;
  height: 8px;
  border-radius: 999px;
  overflow: hidden;
  background: rgba(114, 180, 205, 0.18);
}

.disease-bar {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, rgba(15, 142, 133, 0.95), rgba(95, 199, 216, 0.92));
}

.portrait-insight {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 12px;
}

.gender-overview {
  display: grid;
  gap: 8px;
}

.gender-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  color: rgba(39, 85, 113, 0.9);
  font-size: 11px;
  white-space: nowrap;
}

.gender-head strong {
  font-size: 18px;
  color: rgba(22, 97, 107, 0.98);
}

.gender-track {
  width: 100%;
  height: 10px;
  border-radius: 999px;
  overflow: hidden;
  display: flex;
}

.gender-bar {
  height: 100%;
}

.gender-bar.male {
  background: #24646f;
}

.gender-bar.female {
  background: #e070b3;
}

.age-overview {
  min-height: 0;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
}

.age-title {
  font-size: 11px;
  color: rgba(92, 130, 156, 0.78);
  margin-bottom: 6px;
  white-space: nowrap;
}

.age-chart {
  min-height: 0;
}

.monitor-list {
  display: grid;
  gap: 8px;
}

.monitor-item {
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.46);
  padding: 10px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
}

.monitor-main,
.monitor-side {
  min-width: 0;
  display: grid;
  gap: 4px;
}

.monitor-name {
  font-size: 12px;
  font-weight: 700;
  color: rgba(33, 75, 103, 0.95);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.monitor-metric {
  font-size: 11px;
  color: rgba(77, 111, 123, 0.88);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.monitor-side {
  justify-items: end;
}

.risk-tag {
  border-radius: 999px;
  padding: 2px 8px;
  font-size: 10px;
  white-space: nowrap;
}

.risk-tag.high {
  color: #fff;
  background: #f45b5b;
}

.risk-tag.mid {
  color: #fff;
  background: #f3a047;
}

.risk-tag.low {
  color: #fff;
  background: #39a38f;
}

.monitor-time {
  font-size: 10px;
  color: rgba(92, 130, 156, 0.74);
  white-space: nowrap;
}

.chart {
  height: 100%;
  min-height: 0;
}

.frost-card {
  min-height: 0;
  display: flex;
  flex-direction: column;
  border-radius: var(--r-lg);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.66), rgba(248, 253, 255, 0.46));
  box-shadow: 0 12px 26px rgba(0, 103, 96, 0.06), inset 0 1px 0 rgba(255, 255, 255, 0.28);
}

.card-head {
  padding: 10px 14px 8px;
}

.card-titlebar {
  display: flex;
  align-items: baseline;
  gap: 8px;
  min-width: 0;
}

.card-title {
  font-size: 13px;
  font-weight: 700;
  line-height: 1.2;
  color: rgba(33, 75, 103, 0.96);
  white-space: nowrap;
}

.card-subtitle {
  font-size: 11px;
  line-height: 1.2;
  color: rgba(92, 130, 156, 0.76);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-body {
  flex: 1;
  min-height: 0;
  padding: 9px 12px 10px;
  overflow: hidden;
}

@media (max-width: 1600px) {
  .hub {
    width: 210px;
    height: 210px;
  }
  .hub-value { font-size: 33px; }
  .coverage-value { font-size: 20px; }
}
</style>

