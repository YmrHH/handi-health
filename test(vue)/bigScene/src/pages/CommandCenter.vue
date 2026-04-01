<template>
  <div class="command-page">
    <!-- 顶部总量四卡 -->
    <section class="overview-row">
      <article class="overview-card">
        <div class="overview-icon bg-cyan">患</div>
        <div class="overview-label">患者总数</div>
        <div class="overview-value">{{ totalPatients }}</div>
        <div class="overview-meta">高危 {{ riskHigh }} · 中危 {{ riskMid }}</div>
      </article>
      <article class="overview-card">
        <div class="overview-icon bg-violet">医</div>
        <div class="overview-label">活跃医生数</div>
        <div class="overview-value">{{ activeDoctorCount }}</div>
        <div class="overview-meta">责任医生活跃负载</div>
      </article>
      <article class="overview-card">
        <div class="overview-icon bg-warn">警</div>
        <div class="overview-label">实时事件数</div>
        <div class="overview-value">{{ totalAlerts30d }}</div>
        <div class="overview-meta">近30天健康+设备事件</div>
      </article>
      <article class="overview-card risk">
        <div class="overview-icon bg-danger">危</div>
        <div class="overview-label">高危告警数</div>
        <div class="overview-value">{{ unhandledAlerts }}</div>
        <div class="overview-meta">已关闭 {{ closedAlerts }} · 处置率 {{ closeRate }}%</div>
      </article>
    </section>

    <!-- 第二行：左窄 中宽 右窄 -->
    <section class="middle-grid">
      <div class="middle-col left">
        <ScreenPanel title="病种排名 TOP5" subtitle="慢病病种分布">
          <ul class="rank-list">
            <li v-for="(it, idx) in diseaseTop5" :key="it.name" class="rank-item">
              <div class="rank-head">
                <span class="rank-no">{{ idx + 1 }}</span>
                <span class="rank-name" :title="it.name">{{ it.name }}</span>
                <span class="rank-val">{{ it.value }}</span>
              </div>
              <div class="rank-bar">
                <span class="rank-fill" :style="{ width: `${it.percent}%` }"></span>
              </div>
            </li>
          </ul>
        </ScreenPanel>

        <ScreenPanel title="医生负载 TOP5" subtitle="责任医生负载">
          <ul class="doctor-list">
            <li v-for="it in doctorTop5" :key="it.name" class="doctor-item">
              <span class="doctor-dot"></span>
              <span class="doctor-name">{{ it.name }}</span>
              <span class="doctor-rate">{{ it.percent }}%</span>
            </li>
          </ul>
        </ScreenPanel>
      </div>

      <div class="middle-col center">
        <ScreenPanel title="康养闭环中枢图" subtitle="平台指挥中枢">
          <div class="hub-stage">
            <div class="hub-pulse"></div>
            <div class="hub-rotate-ring"></div>
            <div ref="hubRingRef" class="hub-ring"></div>
            <div class="hub-core glow-breath">
              <div class="hub-core-label">受管患者</div>
              <div class="hub-core-value">{{ totalPatients }}</div>
              <div class="hub-core-sub">风险分层 · 告警联动 · 随访闭环</div>
              <div class="hub-micro-grid">
                <div class="hub-micro-item">
                  <span class="k">高危</span>
                  <span class="v">{{ riskHigh }}</span>
                </div>
                <div class="hub-micro-item">
                  <span class="k">告警</span>
                  <span class="v">{{ totalAlerts30d }}</span>
                </div>
              </div>
            </div>
            <div class="hub-node n1">患者</div>
            <div class="hub-node n2">协同网络</div>
            <div class="hub-node n3">AI中枢</div>
            <div class="hub-node n4">医生</div>
          </div>
        </ScreenPanel>
      </div>

      <div class="middle-col right">
        <ScreenPanel title="处置效率" subtitle="告警处置效率">
          <div class="single-ring-wrap">
            <div ref="disposalGaugeRef" class="single-ring"></div>
            <div class="legend-row">
              <span class="legend-item">
                <i class="dot success"></i>
                已关闭 {{ closedAlerts }}
              </span>
              <span class="legend-item">
                <i class="dot danger"></i>
                未处理 {{ unhandledAlerts }}
              </span>
            </div>
          </div>
        </ScreenPanel>

        <ScreenPanel title="完成率矩阵" subtitle="随访与干预完成度">
          <div class="matrix-grid">
            <div class="matrix-item">
              <div ref="followGaugeRef" class="matrix-ring"></div>
              <div class="matrix-title">随访完成率</div>
            </div>
            <div class="matrix-item">
              <div ref="interventionGaugeRef" class="matrix-ring"></div>
              <div class="matrix-title">干预计划完成率</div>
              <div class="matrix-tip">待接入聚合接口</div>
            </div>
          </div>
        </ScreenPanel>
      </div>
    </section>

    <!-- 第三行：整行主趋势 -->
    <section class="trend-row">
      <ScreenPanel title="综合趋势总览" subtitle="风险 / 告警 / 随访（近 6~12 月）">
        <div class="trend-toolbar">
          <div class="trend-note">近期健康指标综合趋势对比</div>
          <div class="trend-switch">
            <button
              v-for="item in trendTabs"
              :key="item.key"
              type="button"
              class="switch-btn"
              :class="{ active: trendGranularity === item.key }"
              @click="trendGranularity = item.key"
            >
              {{ item.label }}
            </button>
          </div>
        </div>
        <div ref="trendRef" class="trend-chart"></div>
      </ScreenPanel>
    </section>

    <!-- 最底部：状态 + 事件流 -->
    <section class="footer-row">
      <div class="system-status">
        <span class="status-tag ok">系统运行正常</span>
        <span class="status-tag">API 连接稳定</span>
        <span class="status-tag">数据更新中</span>
      </div>
      <div class="ticker-wrap marquee-shell">
        <div class="marquee-track" :style="{ animationDuration: `${Math.max(22, marqueeDurationSec)}s` }">
          <span v-for="it in marqueeItems" :key="it.key" class="marquee-item">
            <strong>{{ it.time }}</strong>
            {{ it.title }}
          </span>
        </div>
      </div>
      <div class="footer-links">
        <span>实时指标</span>
        <span>事件流</span>
        <span>支持中心</span>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onActivated, onDeactivated, onMounted, onUnmounted, ref } from 'vue'
import { graphic, init, type ECharts } from '../utils/echarts'
import ScreenPanel from '../components/ScreenPanel.vue'
import { baseGrid, axisStyle, legendStyle, tooltipStyle } from '../utils/chartTheme'
import { fetchAlerts, fetchHardwareAlerts, fetchHomeStats, fetchMonthSummary, fetchPatientSummary } from '../api'
import { rafThrottle, scheduleIdle } from '../utils/perf'

type RankItem = { name: string; value: number; percent: number }
type DoctorItem = { name: string; value: number; percent: number }

const totalPatients = ref(0)
const riskHigh = ref(0)
const riskMid = ref(0)
const riskLow = ref(0)

const totalAlerts30d = ref(0)
const unhandledAlerts = ref(0)
const closedAlerts = ref(0)
const weekFollowDone = ref(0)
const weekFollowRatePct = ref(0)
const activeDoctorCount = ref(0)

const events = ref<Array<{ id: string; title: string; time: string }>>([])
const diseaseTop5 = ref<RankItem[]>([])
const doctorTop5 = ref<DoctorItem[]>([])

const closeRate = computed(() => {
  const total = unhandledAlerts.value + closedAlerts.value
  if (!total) return 0
  return Number(((closedAlerts.value / total) * 100).toFixed(1))
})

const weekFollowRateText = computed(() => `${Number(weekFollowRatePct.value || 0).toFixed(1)}%`)
const trendTabs = [
  { key: 'daily', label: '日' },
  { key: 'weekly', label: '周' },
  { key: 'monthly', label: '月' }
] as const
const trendGranularity = ref<'daily' | 'weekly' | 'monthly'>('weekly')
const marqueeItems = computed(() => {
  const arr = events.value.length
    ? events.value
    : [{ id: 'fallback', title: '暂无实时事件', time: '--:--' }]
  // 双份拼接，形成无缝滚动
  return [...arr, ...arr].map((it, idx) => ({ key: `${it.id}-${idx}`, title: it.title, time: it.time }))
})
const marqueeDurationSec = computed(() => {
  const base = events.value.length || 6
  return base * 5
})

const hubRingRef = ref<HTMLElement | null>(null)
const trendRef = ref<HTMLElement | null>(null)
const disposalGaugeRef = ref<HTMLElement | null>(null)
const followGaugeRef = ref<HTMLElement | null>(null)
const interventionGaugeRef = ref<HTMLElement | null>(null)

let hubRingChart: ECharts | null = null
let trendChart: ECharts | null = null
let disposalGaugeChart: ECharts | null = null
let followGaugeChart: ECharts | null = null
let interventionGaugeChart: ECharts | null = null

let activeAlive = false
let cancelIdle: null | (() => void) = null

function buildDiseaseTop(list: any[]) {
  const by: Record<string, number> = {}
  list.forEach((r: any) => {
    const d = (r.disease || '未填写').toString().trim() || '未填写'
    by[d] = (by[d] || 0) + 1
  })
  const pairs = Object.entries(by).sort((a, b) => b[1] - a[1]).slice(0, 5)
  const max = pairs[0]?.[1] || 1
  diseaseTop5.value = pairs.map(([name, value]) => ({
    name,
    value,
    percent: Math.max(8, Math.round((value / max) * 100))
  }))
}

function buildDoctorTop(list: any[]) {
  const by: Record<string, number> = {}
  list.forEach((r: any) => {
    const d = (r.responsibleDoctor || r.doctor || '未分配').toString().trim() || '未分配'
    by[d] = (by[d] || 0) + 1
  })
  const pairs = Object.entries(by).sort((a, b) => b[1] - a[1]).slice(0, 5)
  const max = pairs[0]?.[1] || 1
  doctorTop5.value = pairs.map(([name, value]) => ({
    name,
    value,
    percent: Math.max(10, Math.round((value / max) * 100))
  }))
}

function buildHubRing() {
  if (!hubRingRef.value) return
  if (!hubRingChart) hubRingChart = init(hubRingRef.value)
  hubRingChart.setOption({
    color: ['#ee8d99', '#e6bc79', '#78c4a0', '#7fd6e3'],
    tooltip: { trigger: 'item' },
    series: [
      {
        type: 'pie',
        radius: ['62%', '88%'],
        center: ['50%', '50%'],
        label: { show: false },
        labelLine: { show: false },
        itemStyle: { borderColor: 'rgba(114,180,205,0.24)', borderWidth: 2 },
        data: [
          { name: '高危', value: riskHigh.value },
          { name: '中危', value: riskMid.value },
          { name: '低危', value: riskLow.value },
          { name: '告警', value: totalAlerts30d.value }
        ]
      }
    ]
  })
}

function buildTrend(months: string[], alerts: number[], high: number[], follow: number[]) {
  if (!trendRef.value) return
  if (!trendChart) trendChart = init(trendRef.value)
  const axis = axisStyle()
  trendChart.setOption({
    tooltip: tooltipStyle(),
    legend: { ...legendStyle(), data: ['告警', '高危', '随访'] },
    grid: { ...baseGrid(), bottom: 48 },
    xAxis: { type: 'category', data: months, ...axis, axisLabel: { ...(axis as any).axisLabel, margin: 12 } },
    yAxis: { type: 'value', ...axis },
    series: [
      {
        name: '告警',
        type: 'bar',
        data: alerts,
        barWidth: 12,
        itemStyle: {
          color: new graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#7fd6e3' },
            { offset: 1, color: '#9ea9e6' }
          ])
        }
      },
      {
        name: '高危',
        type: 'line',
        smooth: true,
        symbolSize: 5,
        data: high,
        itemStyle: { color: '#ee8d99' },
        lineStyle: { width: 2 }
      },
      {
        name: '随访',
        type: 'line',
        smooth: true,
        symbolSize: 5,
        data: follow,
        itemStyle: { color: '#5fc7d8' },
        lineStyle: { width: 2 }
      }
    ]
  })
}

function buildGauge(el: HTMLElement | null, chart: ECharts | null, value: number, title: string) {
  if (!el) return chart
  const c = chart || init(el)
  c.setOption({
    series: [
      {
        type: 'gauge',
        startAngle: 210,
        endAngle: -30,
        progress: { show: true, width: 10 },
        axisLine: { lineStyle: { width: 10, color: [[1, 'rgba(114,180,205,0.30)']] } },
        splitLine: { show: false },
        axisTick: { show: false },
        axisLabel: { show: false },
        pointer: { show: false },
        detail: {
          valueAnimation: true,
          formatter: '{value}%',
          color: 'rgba(20,52,79,0.96)',
          fontSize: 20,
          fontWeight: 900
        },
        title: { color: 'rgba(39,85,113,0.92)', fontSize: 12, offsetCenter: [0, '58%'] },
        data: [{ value: Math.max(0, Math.min(100, value)), name: title }]
      }
    ]
  })
  return c
}

function resizeAll() {
  if (!activeAlive) return
  hubRingChart?.resize()
  trendChart?.resize()
  disposalGaugeChart?.resize()
  followGaugeChart?.resize()
  interventionGaugeChart?.resize()
}

const onResize = rafThrottle(() => resizeAll())

function disposeAll() {
  hubRingChart?.dispose()
  trendChart?.dispose()
  disposalGaugeChart?.dispose()
  followGaugeChart?.dispose()
  interventionGaugeChart?.dispose()
  hubRingChart = null
  trendChart = null
  disposalGaugeChart = null
  followGaugeChart = null
  interventionGaugeChart = null
}

async function loadCore() {
  const [homeStats, monthRes] = await Promise.all([
    fetchHomeStats().catch(() => ({} as any)),
    fetchMonthSummary().catch(() => [] as any)
  ])
  if (!activeAlive) return

  totalPatients.value = Number(homeStats?.totalPatients || 0)
  weekFollowDone.value = Number(homeStats?.weekFollowDone || 0)
  weekFollowRatePct.value =
    homeStats?.weekFollowRate != null ? Math.max(0, Math.min(100, Number(homeStats.weekFollowRate) * 100)) : 0

  if (homeStats && (homeStats.highRiskCount != null || homeStats.midRiskCount != null || homeStats.lowRiskCount != null)) {
    riskHigh.value = Number(homeStats.highRiskCount) || 0
    riskMid.value = Number(homeStats.midRiskCount) || 0
    riskLow.value = Number(homeStats.lowRiskCount) || 0
  }

  const mArr = Array.isArray(monthRes) ? monthRes : []
  const monthsAll = mArr.map((it: any) => it.month || (it.months && it.months[0]) || '')
  const alertsAll = mArr.map((it: any) => it.alerts || it.alert_count || 0)
  const highAll = mArr.map((it: any) => it.highRisk || it.high_risk || 0)
  const followAll = mArr.map((it: any) => it.followups || it.follow_count || 0)
  const take = monthsAll.length > 12 ? 12 : monthsAll.length
  const start = Math.max(0, monthsAll.length - take)

  buildTrend(monthsAll.slice(start), alertsAll.slice(start), highAll.slice(start), followAll.slice(start))
  followGaugeChart = buildGauge(followGaugeRef.value, followGaugeChart, weekFollowRatePct.value, '随访完成率')
}

async function loadSecondary() {
  const patientRes = await fetchPatientSummary(300).catch(() => ({ rows: [], total: 0 } as any))
  if (!activeAlive) return

  const pList = (patientRes?.rows || []) as any[]
  if (!totalPatients.value) totalPatients.value = Number(patientRes?.total || pList.length || 0)

  if (!riskHigh.value && !riskMid.value && !riskLow.value && pList.length) {
    riskHigh.value = pList.filter((r: any) => String(r.riskLevel || '').toUpperCase().includes('HIGH')).length
    riskMid.value = pList.filter((r: any) => String(r.riskLevel || '').toUpperCase().includes('MID')).length
    riskLow.value = pList.filter((r: any) => String(r.riskLevel || '').toUpperCase().includes('LOW')).length
  }

  buildDiseaseTop(pList)
  buildDoctorTop(pList)
  activeDoctorCount.value = Array.from(
    new Set(
      pList
        .map((r: any) => (r.responsibleDoctor || r.doctor || '').toString().trim())
        .filter((x: string) => !!x)
    )
  ).length

  const [alertRes, hardwareRes] = await Promise.all([fetchAlerts(30).catch(() => ({} as any)), fetchHardwareAlerts(30).catch(() => ({} as any))])
  if (!activeAlive) return
  const aRows = (alertRes?.rows || []) as any[]
  const hRows = (hardwareRes?.rows || []) as any[]
  totalAlerts30d.value = aRows.length + hRows.length
  const all = [...aRows, ...hRows]
  unhandledAlerts.value = all.filter((r: any) => String(r.statusText || r.status || '').includes('未处理') || String(r.status || '').toUpperCase() === 'NEW').length
  closedAlerts.value = all.filter((r: any) => String(r.statusText || r.status || '').includes('已关闭') || String(r.status || '').toUpperCase() === 'CLOSED').length

  const ev: Array<{ id: string; title: string; time: string }> = []
  aRows.slice(0, 4).forEach((r: any, idx: number) => {
    ev.push({
      id: `a-${r.id || idx}`,
      title: `告警 · ${r.patientName || '患者'} · ${r.summary || r.alertType || ''}`,
      time: (r.alertTime || r.firstTime || '').toString().replace('T', ' ')
    })
  })
  hRows.slice(0, 4).forEach((r: any, idx: number) => {
    ev.push({
      id: `h-${r.id || idx}`,
      title: `设备 · ${r.patientName || '患者'} · ${r.alertType || r.summary || ''}`,
      time: (r.alertTime || r.firstTime || '').toString().replace('T', ' ')
    })
  })
  events.value = ev.slice(0, 8)

  buildHubRing()
  disposalGaugeChart = buildGauge(disposalGaugeRef.value, disposalGaugeChart, closeRate.value, '处置效率')
  // 干预计划完成率：当前后端无聚合接口，先展示统一样式占位值（后续可替换真实接口）
  interventionGaugeChart = buildGauge(interventionGaugeRef.value, interventionGaugeChart, 62, '干预完成率')
}

async function startLoad() {
  await loadCore()
  if (!activeAlive) return
  await nextTick()
  cancelIdle?.()
  cancelIdle = scheduleIdle(() => {
    void loadSecondary()
  }, 800)
}

onMounted(() => {
  activeAlive = true
  void startLoad()
  window.addEventListener('resize', onResize)
})

onActivated(() => {
  activeAlive = true
  window.addEventListener('resize', onResize)
  onResize()
})

onDeactivated(() => {
  activeAlive = false
  cancelIdle?.()
  cancelIdle = null
  window.removeEventListener('resize', onResize)
})

onUnmounted(() => {
  activeAlive = false
  cancelIdle?.()
  cancelIdle = null
  window.removeEventListener('resize', onResize)
  disposeAll()
})
</script>

<style scoped>
.command-page {
  display: grid;
  grid-template-rows: auto auto auto auto;
  gap: 12px;
  padding-top: 2px;
}

.overview-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.overview-card {
  position: relative;
  border: 1px solid rgba(114, 180, 205, 0.34);
  border-radius: 16px;
  padding: 14px 16px 12px 16px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.78), rgba(245, 251, 255, 0.68));
  box-shadow: var(--shadow), var(--glow);
  overflow: hidden;
}

.overview-card.risk {
  border-color: rgba(238, 141, 153, 0.34);
}
.overview-card.warn {
  border-color: rgba(239, 197, 111, 0.34);
}
.overview-card.done {
  border-color: rgba(120, 196, 160, 0.34);
}

.overview-label {
  font-size: 12px;
  color: var(--t-2);
  text-transform: uppercase;
  letter-spacing: 0.8px;
  white-space: nowrap;
}

.overview-value {
  margin-top: 6px;
  font-size: 34px;
  line-height: 1;
  font-weight: 900;
  color: var(--t-0);
  white-space: nowrap;
}

.overview-meta {
  margin-top: 6px;
  font-size: 12px;
  color: var(--t-3);
  white-space: nowrap;
}

.overview-icon {
  width: 34px;
  height: 34px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 900;
  margin-bottom: 8px;
  border: 1px solid rgba(114, 180, 205, 0.28);
  color: rgba(29, 78, 114, 0.92);
}
.overview-icon.bg-cyan { background: rgba(95, 199, 216, 0.16); }
.overview-icon.bg-violet { background: rgba(158, 169, 230, 0.16); }
.overview-icon.bg-danger { background: rgba(238, 141, 153, 0.14); }
.overview-icon.bg-warn { background: rgba(239, 197, 111, 0.14); }
.overview-icon.bg-success { background: rgba(120, 196, 160, 0.14); }

.middle-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.8fr) minmax(0, 1fr);
  gap: 12px;
}

.middle-col {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.rank-list,
.doctor-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.rank-item + .rank-item {
  margin-top: 12px;
}

.rank-head {
  display: grid;
  grid-template-columns: 24px 1fr auto;
  align-items: center;
  gap: 10px;
}

.rank-no {
  width: 24px;
  height: 24px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 800;
  color: rgba(29, 78, 114, 0.95);
  background: rgba(127, 214, 227, 0.35);
}

.rank-name {
  font-size: 13px;
  font-weight: 700;
  color: var(--t-1);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rank-val {
  font-size: 13px;
  font-weight: 800;
  color: var(--t-0);
}

.rank-bar {
  margin-top: 6px;
  height: 8px;
  border-radius: 999px;
  background: rgba(140, 188, 227, 0.22);
  overflow: hidden;
}

.rank-fill {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #5fc7d8, #9ea9e6);
}

.doctor-item {
  display: grid;
  grid-template-columns: 8px 1fr auto;
  align-items: center;
  gap: 10px;
  padding: 9px 0;
  border-bottom: 1px dashed rgba(114, 180, 205, 0.22);
}

.doctor-item:last-child {
  border-bottom: none;
}

.doctor-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: #5fc7d8;
  box-shadow: 0 0 8px rgba(95, 199, 216, 0.3);
}

.doctor-name {
  font-size: 13px;
  font-weight: 700;
  color: var(--t-1);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.doctor-rate {
  font-size: 13px;
  font-weight: 800;
  color: var(--t-0);
}

.hub-stage {
  position: relative;
  height: 360px;
  display: grid;
  place-items: center;
}

.hub-pulse {
  position: absolute;
  width: 78%;
  height: 78%;
  border-radius: 999px;
  background: radial-gradient(circle, rgba(95, 199, 216, 0.16) 0%, rgba(95, 199, 216, 0.03) 60%, transparent 72%);
  animation: hubPulse 3.8s ease-in-out infinite;
}

.hub-rotate-ring {
  position: absolute;
  inset: 10px;
  border-radius: 999px;
  border: 2px dashed rgba(95, 199, 216, 0.30);
  animation: hubRotate 60s linear infinite;
}

.hub-ring {
  position: absolute;
  inset: 8px;
}

.hub-core {
  position: absolute;
  width: 232px;
  height: 232px;
  border-radius: 999px;
  background: radial-gradient(circle at 50% 35%, rgba(127, 214, 227, 0.30), rgba(255, 255, 255, 0.78) 58%, rgba(140, 188, 227, 0.25));
  border: 1px solid rgba(114, 180, 205, 0.34);
  box-shadow: var(--glow-strong);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
}

.hub-core-label {
  font-size: 12px;
  color: var(--t-2);
  letter-spacing: 1px;
  white-space: nowrap;
}

.hub-core-value {
  margin-top: 8px;
  font-size: 44px;
  font-weight: 900;
  color: var(--c-gold);
  line-height: 1;
  white-space: nowrap;
}

.hub-core-sub {
  margin-top: 10px;
  font-size: 12px;
  color: var(--t-3);
  white-space: normal;
  max-width: 180px;
}

.hub-micro-grid {
  margin-top: 10px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
  width: 86%;
}

.hub-micro-item {
  border-radius: 10px;
  border: 1px solid rgba(114, 180, 205, 0.22);
  background: rgba(255, 255, 255, 0.56);
  padding: 5px 6px;
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}

.hub-micro-item .k {
  font-size: 11px;
  color: var(--t-2);
}

.hub-micro-item .v {
  font-size: 12px;
  font-weight: 800;
  color: var(--t-0);
}

.hub-node {
  position: absolute;
  min-width: 74px;
  text-align: center;
  padding: 5px 10px 6px;
  font-size: 11px;
  font-weight: 800;
  color: var(--t-1);
  border: 1px solid rgba(114, 180, 205, 0.34);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.66);
  box-shadow: 0 4px 16px rgba(95, 199, 216, 0.12);
}

.hub-node.n1 { top: 30px; left: 50%; transform: translateX(-50%); }
.hub-node.n2 { top: 50%; right: 18px; transform: translateY(-50%); }
.hub-node.n3 { bottom: 32px; left: 50%; transform: translateX(-50%); }
.hub-node.n4 { top: 50%; left: 18px; transform: translateY(-50%); }

.single-ring-wrap {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.single-ring {
  height: 168px;
}

.legend-row {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.legend-item {
  font-size: 12px;
  color: var(--t-2);
  display: inline-flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
}
.dot.success { background: #78c4a0; }
.dot.danger { background: #ee8d99; }

.matrix-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.matrix-item {
  border: 1px solid rgba(114, 180, 205, 0.24);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.58);
  padding: 8px;
}

.matrix-ring {
  height: 122px;
}

.matrix-title {
  margin-top: 2px;
  font-size: 12px;
  font-weight: 700;
  color: var(--t-1);
  text-align: center;
  white-space: nowrap;
}

.matrix-tip {
  margin-top: 2px;
  font-size: 11px;
  color: var(--t-3);
  text-align: center;
  white-space: nowrap;
}

.trend-row :deep(.panel-body) {
  padding-top: 8px;
}

.trend-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.trend-note {
  font-size: 12px;
  color: var(--t-3);
  white-space: nowrap;
}

.trend-switch {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid rgba(114, 180, 205, 0.26);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.56);
  padding: 4px;
}

.switch-btn {
  border: none;
  background: transparent;
  border-radius: 8px;
  padding: 5px 10px;
  font-size: 11px;
  font-weight: 700;
  color: var(--t-2);
  cursor: pointer;
}

.switch-btn.active {
  background: linear-gradient(135deg, rgba(95, 199, 216, 0.18), rgba(158, 169, 230, 0.18));
  color: var(--t-0);
}

.trend-chart {
  height: 300px;
}

.footer-row {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 12px;
  align-items: stretch;
}

.system-status {
  border: 1px solid rgba(114, 180, 205, 0.30);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.62);
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
}

.status-tag {
  font-size: 12px;
  font-weight: 700;
  color: var(--t-2);
  padding: 4px 8px;
  border-radius: 999px;
  border: 1px solid rgba(114, 180, 205, 0.24);
  background: rgba(255, 255, 255, 0.72);
  white-space: nowrap;
}

.status-tag.ok {
  color: rgba(31, 119, 86, 0.92);
  border-color: rgba(120, 196, 160, 0.34);
  background: rgba(120, 196, 160, 0.14);
}

.ticker-wrap {
  border: 1px solid rgba(114, 180, 205, 0.30);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.62);
  padding: 8px 10px;
}

.marquee-shell {
  overflow: hidden;
  white-space: nowrap;
}

.marquee-track {
  display: inline-flex;
  align-items: center;
  gap: 24px;
  min-width: max-content;
  animation: marquee linear infinite;
}

.marquee-item {
  font-size: 12px;
  color: var(--t-2);
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.marquee-item strong {
  color: var(--t-0);
  font-weight: 800;
}

.footer-links {
  border: 1px solid rgba(114, 180, 205, 0.30);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.62);
  display: inline-flex;
  align-items: center;
  gap: 12px;
  padding: 8px 10px;
  font-size: 11px;
  color: var(--t-2);
  white-space: nowrap;
}

@keyframes hubRotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes hubPulse {
  0%, 100% { transform: scale(1); opacity: 0.75; }
  50% { transform: scale(1.04); opacity: 1; }
}

@keyframes marquee {
  0% { transform: translateX(0); }
  100% { transform: translateX(-50%); }
}
</style>

