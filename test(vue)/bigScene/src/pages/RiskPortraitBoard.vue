<template>
  <div class="layout">
    <div class="col left">
      <ScreenPanel title="年龄结构" subtitle="高危人群年龄段">
        <div ref="ageRef" class="chart"></div>
      </ScreenPanel>
      <ScreenPanel title="性别占比" subtitle="患者档案聚合">
        <div ref="genderRef" class="chart"></div>
      </ScreenPanel>
    </div>

    <div class="col center">
      <ScreenPanel title="患者风险画像中枢" subtitle="中心主视觉（画像盘）">
        <div class="center-stage">
          <div class="hub glow-breath">
            <div class="hub-title">重点管理患者</div>
            <div class="hub-value">{{ total }}</div>
            <div class="hub-sub">高危占比 {{ highRatio }}</div>
          </div>
          <div ref="portraitRef" class="hub-ring"></div>
        </div>
      </ScreenPanel>
      <ScreenPanel title="重点患者动态" subtitle="事件流（轻量）">
        <EventTicker :items="events" />
      </ScreenPanel>
    </div>

    <div class="col right">
      <ScreenPanel title="病种排行" subtitle="Top8">
        <div ref="diseaseRef" class="chart"></div>
      </ScreenPanel>
      <ScreenPanel title="体质/证型分布" subtitle="占位（来自患者详情/AI 分析）">
        <div class="placeholder">待接入体质 constitution / syndrome 等字段的聚合展示</div>
      </ScreenPanel>
      <ScreenPanel title="医学建议覆盖率" subtitle="占位（DoctorAdvice）">
        <div ref="adviceRef" class="chart"></div>
      </ScreenPanel>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onActivated, onDeactivated, onMounted, onUnmounted, ref } from 'vue'
import { init, type ECharts } from '../utils/echarts'
import ScreenPanel from '../components/ScreenPanel.vue'
import EventTicker from '../components/EventTicker.vue'
import { axisStyle, baseGrid, legendStyle, tooltipStyle } from '../utils/chartTheme'
import { fetchAlerts, fetchHomeStats, fetchPatientRiskList, fetchPatientSummary, fetchReportBoard } from '../api'
import { rafThrottle } from '../utils/perf'

const total = ref(0)
const high = ref(0)
const mid = ref(0)
const low = ref(0)
const topDiseaseCount = ref(0)
const activeAlertPatients = ref(0)

const highRatio = computed(() => {
  if (!total.value) return '—'
  return `${((high.value / total.value) * 100).toFixed(1)}%`
})

const events = ref<Array<{ id: string; title: string; time: string }>>([])
const patientList = ref<any[]>([])

const ageRef = ref<HTMLElement | null>(null)
const genderRef = ref<HTMLElement | null>(null)
const diseaseRef = ref<HTMLElement | null>(null)
const portraitRef = ref<HTMLElement | null>(null)
const adviceRef = ref<HTMLElement | null>(null)

let ageChart: ECharts | null = null
let genderChart: ECharts | null = null
let diseaseChart: ECharts | null = null
let portraitChart: ECharts | null = null
let adviceChart: ECharts | null = null

let activeAlive = false

function buildAge(list: any[]) {
  if (!ageRef.value) return
  if (!ageChart) ageChart = init(ageRef.value)
  const highList = list.filter((r: any) => String(r.riskLevel || '').toUpperCase().includes('HIGH'))
  const buckets: Record<string, number> = { '0-44': 0, '45-59': 0, '60-74': 0, '75+': 0 }
  highList.forEach((r: any) => {
    const age = Number(r.age || 0)
    if (age >= 75) buckets['75+']++
    else if (age >= 60) buckets['60-74']++
    else if (age >= 45) buckets['45-59']++
    else buckets['0-44']++
  })
  const axis = axisStyle()
  ageChart.setOption({
    tooltip: tooltipStyle(),
    grid: baseGrid(),
    xAxis: { type: 'category', data: Object.keys(buckets), ...axis },
    yAxis: { type: 'value', ...axis },
  series: [{ type: 'bar', data: Object.values(buckets), barWidth: 16, itemStyle: { color: '#ee8d99', borderRadius: [6, 6, 0, 0] } }]
  })
}

function normalizeGender(v: any) {
  const s = (v ?? '').toString().trim()
  if (!s) return '未知'
  if (s === '男' || s.toLowerCase() === 'male' || s === 'M' || s === '1') return '男'
  if (s === '女' || s.toLowerCase() === 'female' || s === 'F' || s === '0' || s === '2') return '女'
  return '未知'
}

function buildGender(list: any[]) {
  if (!genderRef.value) return
  if (!genderChart) genderChart = init(genderRef.value)
  const w = genderRef.value.clientWidth || 320
  const fontSize = Math.max(10, Math.min(14, Math.round(w / 26)))
  const labelWidth = Math.max(56, Math.min(96, Math.round(w / 4.6)))
  const by: Record<string, number> = { 男: 0, 女: 0 }
  list.forEach((r: any) => {
    const g = normalizeGender(r.gender)
    if (g === '男' || g === '女') {
      by[g] = (by[g] || 0) + 1
    }
  })
  genderChart.setOption({
    // 显式绑定每个扇区颜色，确保标签颜色严格一致
    tooltip: { trigger: 'item' },
    series: [
      {
        type: 'pie',
        radius: ['52%', '80%'],
        center: ['50%', '45%'],
        label: {
          show: true,
          position: 'outside',
          formatter: (p: any) => `${p.name}\n${p.value}`,
          color: (p: any) => (p?.data?.itemStyle?.color as string) || (p?.color as string) || 'rgba(20,52,79,0.96)',
          fontSize,
          fontWeight: 800,
          lineHeight: Math.round(fontSize * 1.25),
          width: labelWidth,
          overflow: 'break'
        },
        emphasis: {
          // 防止 hover 时标签颜色发生变化
          label: {
            color: (p: any) =>
              (p?.data?.itemStyle?.color as string) || (p?.color as string) || 'rgba(20,52,79,0.96)',
            fontWeight: 800
          },
          labelLine: {
            lineStyle: { color: 'rgba(114,180,205,0.35)', width: 1 }
          }
        },
        labelLine: {
          show: true,
          length: 18,
          length2: 14,
          smooth: false,
          lineStyle: { color: 'rgba(114,180,205,0.35)', width: 1 }
        },
        itemStyle: { borderColor: 'rgba(114,180,205,0.24)', borderWidth: 2 },
        data: [
          { name: '男', value: by['男'] || 0, itemStyle: { color: '#7fd6e3' } },
          { name: '女', value: by['女'] || 0, itemStyle: { color: '#9ea9e6' } }
        ]
      }
    ]
  })
}

function buildDisease(list: any[]) {
  if (!diseaseRef.value) return
  if (!diseaseChart) diseaseChart = init(diseaseRef.value)
  const by: Record<string, number> = {}
  list.forEach((r: any) => {
    const d = (r.disease || '未填写').toString()
    by[d] = (by[d] || 0) + 1
  })
  const pairs = Object.entries(by).sort((a, b) => b[1] - a[1]).slice(0, 8)
  topDiseaseCount.value = pairs[0]?.[1] ? Number(pairs[0][1]) : 0
  const names = pairs.map((p) => p[0])
  const vals = pairs.map((p) => p[1])
  const axis = axisStyle()
  diseaseChart.setOption({
    tooltip: tooltipStyle(),
    grid: { left: 40, right: 18, top: 20, bottom: 40 },
    xAxis: { type: 'category', data: names, axisLabel: { color: 'rgba(39,85,113,0.92)', rotate: 24 } },
    yAxis: { type: 'value', ...axis },
    series: [{ type: 'bar', data: vals, barWidth: 16, itemStyle: { color: '#7fd6e3', borderRadius: [6, 6, 0, 0] } }]
  })
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

function buildAdvice(board: any) {
  if (!adviceRef.value) return
  if (!adviceChart) adviceChart = init(adviceRef.value)
  const auc = Number(board?.latestAuc || 0) * 100
  const f1 = Number(board?.latestF1 || 0) * 100
  adviceChart.setOption({
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
        detail: { valueAnimation: true, formatter: '{value}%', color: 'rgba(20,52,79,0.96)', fontSize: 20, fontWeight: 900 },
        title: { color: 'rgba(39,85,113,0.92)', fontSize: 12, offsetCenter: [0, '58%'] },
        data: [{ value: Math.max(0, Math.min(100, (auc + f1) / 2)), name: '综合指标' }]
      }
    ]
  })
}

function resizeAll() {
  if (!activeAlive) return
  ageChart?.resize()
  genderChart?.resize()
  diseaseChart?.resize()
  portraitChart?.resize()
  adviceChart?.resize()
  // 标签字体随容器自适应（避免省略号）
  if (patientList.value.length) buildGender(patientList.value)
}

const onResize = rafThrottle(() => resizeAll())

onMounted(async () => {
  activeAlive = true
  const [homeStats, riskRes, patientRes, alertRes, board] = await Promise.all([
    fetchHomeStats().catch(() => ({} as any)),
    fetchPatientRiskList(200),
    fetchPatientSummary(200),
    fetchAlerts(30),
    fetchReportBoard().catch(() => ({} as any))
  ])
  const list = (patientRes?.rows || []) as any[]
  patientList.value = list
  total.value = Number(homeStats?.totalPatients || patientRes?.total || list.length || riskRes?.total || 0)
  const listForRisk = list.length ? list : ((riskRes?.rows || []) as any[])
  high.value = listForRisk.filter((r: any) => String(r.riskLevel || '').toUpperCase().includes('HIGH')).length
  mid.value = listForRisk.filter((r: any) => String(r.riskLevel || '').toUpperCase().includes('MID')).length
  low.value = listForRisk.filter((r: any) => String(r.riskLevel || '').toUpperCase().includes('LOW')).length
  activeAlertPatients.value = list.filter((r: any) => Number(r.activeAlertCount || 0) > 0).length

  if (homeStats && (homeStats.highRiskCount != null || homeStats.midRiskCount != null || homeStats.lowRiskCount != null)) {
    if (homeStats.highRiskCount != null) high.value = Number(homeStats.highRiskCount) || 0
    if (homeStats.midRiskCount != null) mid.value = Number(homeStats.midRiskCount) || 0
    if (homeStats.lowRiskCount != null) low.value = Number(homeStats.lowRiskCount) || 0
  }
  const aRows = (alertRes?.rows || []) as any[]
  events.value = aRows.slice(0, 8).map((r: any, idx: number) => ({
    id: String(r.id || idx),
    title: `告警 · ${r.patientName || '患者'} · ${r.summary || r.alertType || ''}`,
    time: (r.alertTime || '').toString().replace('T', ' ')
  }))

  buildAge(list)
  buildGender(list)
  buildDisease(list)
  buildPortrait()
  buildAdvice(board)

  window.addEventListener('resize', onResize)
})

onUnmounted(() => {
  activeAlive = false
  window.removeEventListener('resize', onResize)
  ageChart?.dispose()
  genderChart?.dispose()
  diseaseChart?.dispose()
  portraitChart?.dispose()
  adviceChart?.dispose()
  ageChart = null
  genderChart = null
  diseaseChart = null
  portraitChart = null
  adviceChart = null
})

onActivated(() => {
  activeAlive = true
  window.addEventListener('resize', onResize)
  onResize()
})

onDeactivated(() => {
  activeAlive = false
  window.removeEventListener('resize', onResize)
})
</script>

<style scoped>
.layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 2.35fr) minmax(0, 1fr);
  gap: 12px;
  height: 100%;
  min-height: 0;
}

.col {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
}

.col :deep(.panel) {
  flex: 1;
  min-height: 0;
}

.chart {
  height: 220px;
}

.center-stage {
  position: relative;
  height: 300px;
  display: grid;
  place-items: center;
}

.hub {
  position: absolute;
  width: 250px;
  height: 250px;
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
  font-size: 44px;
  font-weight: 900;
  color: var(--c-gold);
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

