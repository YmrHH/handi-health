<template>
  <div class="layout">
    <div class="col left">
      <ScreenPanel title="平台总量卡组" subtitle="患者 / 告警 / 随访 / 干预">
        <div class="stat-grid">
          <StatCard label="总患者数" :value="totalPatients" tone="gold" />
          <StatCard label="高危患者数" :value="riskHigh" tone="danger" />
          <StatCard label="未处理告警" :value="unhandledAlerts" tone="warning" />
          <StatCard label="本周完成随访" :value="weekFollowDone" tone="cyan" hint="来自 /api/home/stats" />
        </div>
      </ScreenPanel>

      <ScreenPanel title="病种 Top5 排行条" subtitle="按患者数聚合">
        <div ref="diseaseRankRef" class="chart chart-rank"></div>
      </ScreenPanel>

      <ScreenPanel title="责任医生负载 Top5" subtitle="按随访任务聚合">
        <div ref="doctorRankRef" class="chart chart-rank"></div>
      </ScreenPanel>
    </div>

    <div class="col center">
      <ScreenPanel title="康养闭环中枢图" subtitle="平台核心态势主视觉" >
        <div class="center-stage">
          <div class="hub glow-breath">
            <div class="hub-title">受管患者</div>
            <div class="hub-value">{{ totalPatients }}</div>
            <div class="hub-sub">风险分层与告警分布</div>
          </div>
          <div ref="hubRingRef" class="hub-ring"></div>
        </div>
      </ScreenPanel>

      <ScreenPanel title="综合趋势主图" subtitle="风险 / 告警 / 随访（近 6~12 月）">
        <div ref="trendRef" class="chart chart-tall chart-trend"></div>
      </ScreenPanel>
    </div>

    <div class="col right">
      <ScreenPanel title="告警处置效率卡" subtitle="未处理 / 已关闭">
        <div class="stat-grid">
          <StatCard label="未处理" :value="unhandledAlerts" tone="danger" />
          <StatCard label="已关闭" :value="closedAlerts" tone="success" />
          <StatCard label="近30天告警" :value="totalAlerts30d" tone="cyan" />
          <StatCard label="关闭率" :value="closeRateText" tone="gold" />
        </div>
      </ScreenPanel>

      <ScreenPanel title="随访完成率" subtitle="任务推进">
        <div ref="followGaugeRef" class="chart"></div>
      </ScreenPanel>

      <ScreenPanel title="干预计划完成率" subtitle="占位（后续接入）">
        <div class="placeholder">待接入 VisitPlan / HomeService / Recommend 的聚合指标</div>
      </ScreenPanel>

      <ScreenPanel title="滚动事件流" subtitle="最近告警 / 随访 / 服务">
        <EventTicker :items="events" />
      </ScreenPanel>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import * as echarts from 'echarts'
import ScreenPanel from '../components/ScreenPanel.vue'
import StatCard from '../components/StatCard.vue'
import EventTicker from '../components/EventTicker.vue'
import { baseGrid, axisStyle, colorPalette, legendStyle, tooltipStyle } from '../utils/chartTheme'
import { fetchAlerts, fetchHardwareAlerts, fetchHomeStats, fetchMonthSummary, fetchPatientRiskList, fetchPatientSummary } from '../api'

const totalPatients = ref(0)
const riskHigh = ref(0)
const riskMid = ref(0)
const riskLow = ref(0)

const totalAlerts30d = ref(0)
const unhandledAlerts = ref(0)
const closedAlerts = ref(0)
const weekFollowDone = ref(0)
const weekFollowRatePct = ref(0)

const events = ref<Array<{ id: string; title: string; time: string }>>([])

const closeRateText = computed(() => {
  const total = unhandledAlerts.value + closedAlerts.value
  if (!total) return '—'
  return `${((closedAlerts.value / total) * 100).toFixed(1)}%`
})

const diseaseRankRef = ref<HTMLElement | null>(null)
const doctorRankRef = ref<HTMLElement | null>(null)
const hubRingRef = ref<HTMLElement | null>(null)
const trendRef = ref<HTMLElement | null>(null)
const followGaugeRef = ref<HTMLElement | null>(null)

let diseaseRankChart: echarts.ECharts | null = null
let doctorRankChart: echarts.ECharts | null = null
let hubRingChart: echarts.ECharts | null = null
let trendChart: echarts.ECharts | null = null
let followGaugeChart: echarts.ECharts | null = null

function buildDiseaseRank(list: any[]) {
  if (!diseaseRankRef.value) return
  if (!diseaseRankChart) diseaseRankChart = echarts.init(diseaseRankRef.value)
  const by: Record<string, number> = {}
  list.forEach((r) => {
    const d = (r.disease || '未填写').toString()
    by[d] = (by[d] || 0) + 1
  })
  const pairs = Object.entries(by).sort((a, b) => b[1] - a[1]).slice(0, 5)
  const names = pairs.map((p) => p[0])
  const vals = pairs.map((p) => p[1])
  const axis = axisStyle()
  diseaseRankChart.setOption({
    color: colorPalette(),
    tooltip: tooltipStyle(),
    // bottom 留足空间，避免 xAxis 数字被裁切
    grid: { left: 90, right: 18, top: 18, bottom: 26 },
    xAxis: { type: 'value', ...axis, axisLabel: { ...(axis as any).axisLabel, margin: 12 } },
    yAxis: { type: 'category', data: names, axisLabel: { color: 'rgba(39,85,113,0.92)' }, axisLine: axis.axisLine },
    series: [
      {
        type: 'bar',
        name: '患者数',
        data: vals,
        barWidth: 14,
        itemStyle: {
          borderRadius: [0, 8, 8, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#5fc7d8' },
            { offset: 1, color: '#9ea9e6' }
          ])
        }
      }
    ]
  })
}

function buildDoctorRank(pList: any[]) {
  if (!doctorRankRef.value) return
  if (!doctorRankChart) doctorRankChart = echarts.init(doctorRankRef.value)
  const by: Record<string, number> = {}
  pList.forEach((r: any) => {
    const d = (r.responsibleDoctor || r.doctor || '未分配').toString().trim() || '未分配'
    by[d] = (by[d] || 0) + 1
  })
  const pairs = Object.entries(by).sort((a, b) => b[1] - a[1]).slice(0, 5)
  const names = pairs.map((p) => p[0])
  const vals = pairs.map((p) => p[1])
  const axis = axisStyle()
  doctorRankChart.setOption({
    tooltip: tooltipStyle(),
    // bottom 留足空间，避免 xAxis 数字被裁切
    grid: { left: 90, right: 18, top: 18, bottom: 26 },
    xAxis: { type: 'value', ...axis, axisLabel: { ...(axis as any).axisLabel, margin: 12 } },
    yAxis: { type: 'category', data: names, axisLabel: { color: 'rgba(39,85,113,0.92)' }, axisLine: axis.axisLine },
    series: [
      {
        type: 'bar',
        name: '任务数',
        data: vals,
        barWidth: 14,
        itemStyle: {
          borderRadius: [0, 8, 8, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#78c4a0' },
            { offset: 1, color: '#9ea9e6' }
          ])
        }
      }
    ]
  })
}

function buildHubRing() {
  if (!hubRingRef.value) return
  if (!hubRingChart) hubRingChart = echarts.init(hubRingRef.value)
  hubRingChart.setOption({
    color: ['#ee8d99', '#e6bc79', '#78c4a0', '#7fd6e3', '#9ea9e6'],
    tooltip: { trigger: 'item' },
    series: [
      {
        type: 'pie',
        radius: ['62%', '86%'],
        center: ['50%', '50%'],
        label: {
          show: true,
          position: 'outside',
          formatter: (p: any) => `${p.name}  ${p.value}`,
          color: (p: any) => p.color,
          fontSize: 12,
          padding: [2, 2, 2, 2],
          fontWeight: 800
        },
        labelLine: {
          show: true,
          length: 18,
          length2: 14,
          smooth: false,
          lineStyle: { color: 'rgba(39,85,113,0.32)', width: 1 }
        },
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

function buildTrend(months: string[], alerts: number[], high: number[]) {
  if (!trendRef.value) return
  if (!trendChart) trendChart = echarts.init(trendRef.value)
  const axis = axisStyle()
  trendChart.setOption({
    tooltip: tooltipStyle(),
    legend: { ...legendStyle(), data: ['告警', '高危'] },
    // 增大底部留白，避免 xAxis 标签贴边
    grid: { ...baseGrid(), bottom: 54 },
    xAxis: { type: 'category', data: months, ...axis, axisLabel: { ...(axis as any).axisLabel, margin: 14 } },
    yAxis: { type: 'value', ...axis },
    series: [
      {
        name: '告警',
        type: 'bar',
        data: alerts,
        barWidth: 14,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#5fc7d8' },
            { offset: 1, color: '#9ea9e6' }
          ])
        }
      },
      {
        name: '高危',
        type: 'line',
        smooth: true,
        symbolSize: 6,
        data: high,
        itemStyle: { color: '#ee8d99' },
        lineStyle: { width: 2 }
      }
    ]
  })
}

function buildFollowGauge() {
  if (!followGaugeRef.value) return
  if (!followGaugeChart) followGaugeChart = echarts.init(followGaugeRef.value)
  followGaugeChart.setOption({
    series: [
      {
        type: 'gauge',
        startAngle: 210,
        endAngle: -30,
        progress: { show: true, width: 10 },
        axisLine: { lineStyle: { width: 10, color: [[1, 'rgba(114,180,205,0.32)']] } },
        splitLine: { show: false },
        axisTick: { show: false },
        axisLabel: { show: false },
        pointer: { show: false },
        detail: {
          valueAnimation: true,
          formatter: '{value}%',
          color: 'rgba(20,52,79,0.96)',
          fontSize: 24,
          fontWeight: 900
        },
        title: { color: 'rgba(39,85,113,0.92)', fontSize: 12, offsetCenter: [0, '58%'] },
        data: [{ value: weekFollowRatePct.value, name: '完成率' }]
      }
    ]
  })
}

function resizeAll() {
  diseaseRankChart?.resize()
  doctorRankChart?.resize()
  hubRingChart?.resize()
  trendChart?.resize()
  followGaugeChart?.resize()
}

onMounted(async () => {
  const [homeStats, riskRes, patientRes, alertRes, hardwareRes, monthRes] = await Promise.all([
    fetchHomeStats().catch(() => ({} as any)),
    fetchPatientRiskList(200),
    fetchPatientSummary(200),
    fetchAlerts(30),
    fetchHardwareAlerts(30),
    fetchMonthSummary()
  ])

  // 受管患者：与网页版一致，优先用 /api/home/stats 的 totalPatients
  const pList = (patientRes?.rows || []) as any[]
  totalPatients.value = Number(homeStats?.totalPatients || patientRes?.total || pList.length || riskRes?.total || 0)
  weekFollowDone.value = Number(homeStats?.weekFollowDone || 0)
  weekFollowRatePct.value =
    homeStats?.weekFollowRate != null ? Math.max(0, Math.min(100, Number(homeStats.weekFollowRate) * 100)) : 0
  const listForRisk = pList.length ? pList : ((riskRes?.rows || []) as any[])
  riskHigh.value = listForRisk.filter((r: any) => String(r.riskLevel || '').toUpperCase().includes('HIGH')).length
  riskMid.value = listForRisk.filter((r: any) => String(r.riskLevel || '').toUpperCase().includes('MID')).length
  riskLow.value = listForRisk.filter((r: any) => String(r.riskLevel || '').toUpperCase().includes('LOW')).length

  // 如果网页版 stats 里直接给了分层数量，则覆盖（口径一致）
  if (homeStats && (homeStats.highRiskCount != null || homeStats.midRiskCount != null || homeStats.lowRiskCount != null)) {
    if (homeStats.highRiskCount != null) riskHigh.value = Number(homeStats.highRiskCount) || 0
    if (homeStats.midRiskCount != null) riskMid.value = Number(homeStats.midRiskCount) || 0
    if (homeStats.lowRiskCount != null) riskLow.value = Number(homeStats.lowRiskCount) || 0
  }

  // 告警统计（健康 + 设备）
  const aRows = (alertRes?.rows || []) as any[]
  const hRows = (hardwareRes?.rows || []) as any[]
  totalAlerts30d.value = aRows.length + hRows.length
  const all = [...aRows, ...hRows]
  unhandledAlerts.value = all.filter((r: any) => String(r.statusText || r.status || '').includes('未处理') || String(r.status || '').toUpperCase() === 'NEW').length
  closedAlerts.value = all.filter((r: any) => String(r.statusText || r.status || '').includes('已关闭') || String(r.status || '').toUpperCase() === 'CLOSED').length

  // 中心趋势（month-summary：取近 6~12）
  const mArr = Array.isArray(monthRes) ? monthRes : []
  const monthsAll = mArr.map((it: any) => it.month || (it.months && it.months[0]) || '')
  const alertsAll = mArr.map((it: any) => it.alerts || it.alert_count || 0)
  const highAll = mArr.map((it: any) => it.highRisk || it.high_risk || 0)
  const take = monthsAll.length > 12 ? 12 : monthsAll.length
  const start = monthsAll.length - take

  buildDiseaseRank(pList)
  buildDoctorRank(pList)
  buildHubRing()
  buildTrend(monthsAll.slice(start), alertsAll.slice(start), highAll.slice(start))
  buildFollowGauge()

  // 事件流
  const ev: Array<{ id: string; title: string; time: string }> = []
  aRows.slice(0, 4).forEach((r: any, idx: number) => {
    ev.push({ id: `a-${r.id || idx}`, title: `告警 · ${r.patientName || '患者'} · ${r.summary || r.alertType || ''}`, time: (r.alertTime || r.firstTime || '').toString().replace('T', ' ') })
  })
  hRows.slice(0, 4).forEach((r: any, idx: number) => {
    ev.push({ id: `h-${r.id || idx}`, title: `设备 · ${r.patientName || '患者'} · ${r.alertType || r.summary || ''}`, time: (r.alertTime || r.firstTime || '').toString().replace('T', ' ') })
  })
  events.value = ev.slice(0, 8)

  window.addEventListener('resize', resizeAll)
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeAll)
  diseaseRankChart?.dispose()
  doctorRankChart?.dispose()
  hubRingChart?.dispose()
  trendChart?.dispose()
  followGaugeChart?.dispose()
  diseaseRankChart = null
  doctorRankChart = null
  hubRingChart = null
  trendChart = null
  followGaugeChart = null
})
</script>

<style scoped>
.layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 2.35fr) minmax(0, 1fr);
  gap: 12px;
}

.col {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
}

.chart {
  height: 180px;
}

.chart-rank {
  height: 250px;
}

.chart-tall {
  height: 320px;
}

.chart-trend {
  height: 380px;
}

.center-stage {
  position: relative;
  height: 260px;
  display: grid;
  place-items: center;
}

.hub {
  position: absolute;
  width: 240px;
  height: 240px;
  border-radius: 999px;
  background: radial-gradient(circle at 50% 35%, rgba(127, 214, 227, 0.30), rgba(255, 255, 255, 0.76) 58%, rgba(140, 188, 227, 0.25));
  border: 1px solid rgba(114, 180, 205, 0.34);
  box-shadow: var(--glow-strong);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
}

.hub-title {
  font-size: 12px;
  letter-spacing: 2px;
  color: var(--t-2);
}

.hub-value {
  margin-top: 8px;
  font-size: 42px;
  font-weight: 900;
  color: var(--c-gold);
  text-shadow: 0 0 18px rgba(230, 188, 121, 0.22);
}

.hub-sub {
  margin-top: 10px;
  font-size: 12px;
  color: var(--t-3);
}

.hub-ring {
  position: absolute;
  inset: 0;
}

.placeholder {
  padding: 10px 8px;
  color: var(--t-3);
  font-size: 12px;
}
</style>

