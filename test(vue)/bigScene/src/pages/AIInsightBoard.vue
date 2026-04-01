<template>
  <div class="layout">
    <div class="col left">
      <ScreenPanel title="模型效果与覆盖" subtitle="AUC / F1 / 建议覆盖率（来自 Board）">
        <div ref="kpiRef" class="chart"></div>
      </ScreenPanel>
      <ScreenPanel title="近三个月趋势" subtitle="告警 / 随访 / 高危（强约束：只取最近3个月）">
        <div ref="trend3Ref" class="chart chart-tall"></div>
      </ScreenPanel>
      <ScreenPanel title="数据来源分布" subtitle="来自 Report Board">
        <div v-if="sourceHasData" ref="typeRef" class="chart"></div>
        <div v-else class="placeholder">暂无数据</div>
      </ScreenPanel>
    </div>

    <div class="col center">
      <ScreenPanel title="AI 趋势中枢" subtitle="中心主视觉（多层环 + 趋势锚点）">
        <div class="center-stage">
          <div class="hub glow-breath">
            <div class="hub-title">AI 洞察指数</div>
            <div class="hub-value">{{ insightIndexText }}</div>
            <div class="hub-sub">近3个月 · 告警 · 风险 · 建议</div>
          </div>
          <div ref="insightRef" class="hub-ring"></div>
        </div>
      </ScreenPanel>
      <ScreenPanel title="病种趋势" subtitle="Top 病种（来自 Report Board）">
        <div v-if="diseaseHasData" ref="diseaseTrendRef" class="chart chart-tall"></div>
        <div v-else class="placeholder">暂无数据</div>
      </ScreenPanel>
    </div>

    <div class="col right">
      <ScreenPanel title="洞察要点" subtitle="事件流（替代表格）">
        <EventTicker :items="events" />
      </ScreenPanel>
      <ScreenPanel title="病种稳定率排行" subtitle="Top5（来自 Report Board）">
        <div v-if="stableHasData" ref="rankRef" class="chart"></div>
        <div v-else class="placeholder">暂无数据</div>
      </ScreenPanel>
      <ScreenPanel title="重点提示" subtitle="占位">
        <div class="placeholder">可在此展示“本周风险上升人群 / 告警集中设备类型 / 随访滞后指标”等</div>
      </ScreenPanel>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onActivated, onDeactivated, onMounted, onUnmounted, ref } from 'vue'
import { init, type ECharts } from '../utils/echarts'
import ScreenPanel from '../components/ScreenPanel.vue'
import EventTicker from '../components/EventTicker.vue'
import { axisStyle, baseGrid, legendStyle, tooltipStyle } from '../utils/chartTheme'
import { fetchReportBoard } from '../api'
import { rafThrottle } from '../utils/perf'

const kpiRef = ref<HTMLElement | null>(null)
const trend3Ref = ref<HTMLElement | null>(null)
const typeRef = ref<HTMLElement | null>(null)
const insightRef = ref<HTMLElement | null>(null)
const diseaseTrendRef = ref<HTMLElement | null>(null)
const rankRef = ref<HTMLElement | null>(null)

let kpiChart: ECharts | null = null
let trend3Chart: ECharts | null = null
let typeChart: ECharts | null = null
let insightChart: ECharts | null = null
let diseaseTrendChart: ECharts | null = null
let rankChart: ECharts | null = null

let activeAlive = false

const events = ref<Array<{ id: string; title: string; time: string }>>([])

const insightIndexText = ref('—')
const sourceHasData = ref(false)
const diseaseHasData = ref(false)
const stableHasData = ref(false)

function buildKpi(auc: number, f1: number) {
  if (!kpiRef.value) return
  if (!kpiChart) kpiChart = init(kpiRef.value)
  kpiChart.setOption({
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
        detail: { valueAnimation: true, formatter: '{value}%', color: 'rgba(20,52,79,0.96)', fontSize: 22, fontWeight: 900 },
        title: { color: 'rgba(39,85,113,0.92)', fontSize: 12, offsetCenter: [0, '58%'] },
        data: [{ value: Math.max(0, Math.min(100, (auc + f1) / 2)), name: 'AUC/F1 均值' }]
      }
    ]
  })
}

function buildTrend3(months: string[], alerts: number[], follow: number[], high: number[]) {
  if (!trend3Ref.value) return
  if (!trend3Chart) trend3Chart = init(trend3Ref.value)
  const axis = axisStyle()
  trend3Chart.setOption({
    tooltip: tooltipStyle(),
    legend: { ...legendStyle(), data: ['告警', '随访', '高危'] },
    grid: { ...baseGrid(), bottom: 36 },
    xAxis: { type: 'category', data: months, ...axis },
    yAxis: { type: 'value', ...axis },
    series: [
      { name: '告警', type: 'bar', data: alerts, barWidth: 14, itemStyle: { color: '#5fc7d8' } },
      { name: '随访', type: 'bar', data: follow, barWidth: 14, itemStyle: { color: '#7fd6e3' } },
      { name: '高危', type: 'line', smooth: true, data: high, itemStyle: { color: '#ee8d99' }, lineStyle: { width: 2 } }
    ]
  })
}

function buildSourcePie(names: string[], counts: number[]) {
  if (!typeRef.value) return
  if (!typeChart) typeChart = init(typeRef.value)
  typeChart.setOption({
    legend: legendStyle(),
    series: [
      {
        type: 'pie',
        radius: ['52%', '80%'],
        center: ['50%', '45%'],
        label: { color: 'rgba(20,52,79,0.96)', formatter: '{b}\n{d}%' },
        itemStyle: { borderColor: 'rgba(114,180,205,0.24)', borderWidth: 2 },
        data: names.map((n, i) => ({ name: n, value: Number(counts[i] || 0) }))
      }
    ]
  })
}

function buildInsightCenter(latest: { alerts?: number; followups?: number; highRisk?: number; auc?: number; f1?: number }) {
  if (!insightRef.value) return
  if (!insightChart) insightChart = init(insightRef.value)
  const a = Number(latest.alerts || 0)
  const f = Number(latest.followups || 0)
  const h = Number(latest.highRisk || 0)
  const auc = Math.max(0, Math.min(100, Number(latest.auc || 0) * 100))
  const f1 = Math.max(0, Math.min(100, Number(latest.f1 || 0) * 100))
  insightChart.setOption({
    series: [
      {
        type: 'pie',
        radius: ['62%', '86%'],
        center: ['50%', '50%'],
        label: { show: false },
        itemStyle: { borderColor: 'rgba(114,180,205,0.24)', borderWidth: 2 },
        data: [
          { name: '当月告警', value: a },
          { name: '当月随访', value: f },
          { name: '当月高危', value: h },
          { name: 'AUC/F1', value: Math.round((auc + f1) / 2) }
        ]
      }
    ]
  })
}

function buildDiseaseTop(diseaseAnalysis: any[]) {
  if (!diseaseTrendRef.value) return
  if (!diseaseTrendChart) diseaseTrendChart = init(diseaseTrendRef.value)
  const top = [...diseaseAnalysis]
    .filter((x: any) => x && x.disease)
    .sort((a: any, b: any) => Number(b.patientCount || 0) - Number(a.patientCount || 0))
    .slice(0, 6)
  const names = top.map((x: any) => String(x.disease))
  const vals = top.map((x: any) => Number(x.patientCount || 0))
  const axis = axisStyle()
  diseaseTrendChart.setOption({
    tooltip: tooltipStyle(),
    grid: { ...baseGrid(), bottom: 28 },
    xAxis: { type: 'category', data: names, ...axis, axisLabel: { color: 'rgba(39,85,113,0.92)', rotate: 22 } },
    yAxis: { type: 'value', ...axis },
    series: [
      { type: 'bar', data: vals, barWidth: 16, itemStyle: { color: '#7fd6e3', borderRadius: [6, 6, 0, 0] }, name: '患者数' }
    ]
  })
}

function buildStableRank(diseaseAnalysis: any[]) {
  if (!rankRef.value) return
  if (!rankChart) rankChart = init(rankRef.value)
  const top = [...diseaseAnalysis]
    .filter((x: any) => x && x.disease && x.stableRate != null)
    .sort((a: any, b: any) => Number(b.stableRate || 0) - Number(a.stableRate || 0))
    .slice(0, 5)
  const names = top.map((x: any) => String(x.disease))
  const vals = top.map((x: any) => Math.round(Number(x.stableRate || 0) * 100))
  const axis = axisStyle()
  rankChart.setOption({
    tooltip: tooltipStyle(),
    grid: { left: 90, right: 18, top: 18, bottom: 10 },
    xAxis: { type: 'value', ...axis },
    yAxis: { type: 'category', data: names, axisLabel: { color: 'rgba(39,85,113,0.92)' }, axisLine: axis.axisLine },
    series: [{ type: 'bar', data: vals, barWidth: 14, itemStyle: { color: '#78c4a0', borderRadius: [0, 8, 8, 0] } }]
  })
}

function resizeAll() {
  if (!activeAlive) return
  kpiChart?.resize()
  trend3Chart?.resize()
  typeChart?.resize()
  insightChart?.resize()
  diseaseTrendChart?.resize()
  rankChart?.resize()
}

onMounted(async () => {
  activeAlive = true
  const board = await fetchReportBoard().catch(() => ({} as any))
  const auc = Number(board?.latestAuc || 0) * 100
  const f1 = Number(board?.latestF1 || 0) * 100
  insightIndexText.value = auc || f1 ? `${Math.round((auc + f1) / 2)}` : '—'

  // v4 强约束：趋势只取最近 3 个月
  const monthsAll = (board?.months || []) as string[]
  const alertsAll = (board?.alertsArr || []) as number[]
  const followAll = (board?.followArr || []) as number[]
  const highAll = (board?.highArr || []) as number[]
  const take = 3
  const start = Math.max(0, monthsAll.length - take)

  buildKpi(auc, f1)
  buildTrend3(monthsAll.slice(start), alertsAll.slice(start), followAll.slice(start), highAll.slice(start))
  const srcNames = (board?.sourceNames || []) as string[]
  const srcCounts = (board?.sourceCounts || []) as number[]
  sourceHasData.value = srcNames.length > 0 && srcCounts.length > 0
  if (sourceHasData.value) buildSourcePie(srcNames, srcCounts)

  buildInsightCenter({
    alerts: board?.latestAlerts,
    followups: board?.latestFollowups,
    highRisk: board?.latestHighRisk,
    auc: board?.latestAuc,
    f1: board?.latestF1
  })

  const diseaseAnalysis = (board?.diseaseAnalysis || []) as any[]
  diseaseHasData.value = Array.isArray(diseaseAnalysis) && diseaseAnalysis.length > 0
  if (diseaseHasData.value) buildDiseaseTop(diseaseAnalysis)
  stableHasData.value = Array.isArray(diseaseAnalysis) && diseaseAnalysis.some((x: any) => x && x.stableRate != null)
  if (stableHasData.value) buildStableRank(diseaseAnalysis)

  const latestMonth = String(board?.latestMonth || '').trim()
  events.value = [
    { id: 'm', title: latestMonth ? `本月：${latestMonth}` : '本月：—', time: '最新' },
    { id: 'a', title: `告警变化率：${board?.alertChangeRate != null ? Math.round(Number(board.alertChangeRate) * 100) + '%' : '—'}`, time: '最新' },
    { id: 'h', title: `高危变化率：${board?.highRiskChangeRate != null ? Math.round(Number(board.highRiskChangeRate) * 100) + '%' : '—'}`, time: '最新' }
  ]

  window.addEventListener('resize', onResize)
})

onUnmounted(() => {
  activeAlive = false
  window.removeEventListener('resize', onResize)
  kpiChart?.dispose()
  trend3Chart?.dispose()
  typeChart?.dispose()
  insightChart?.dispose()
  diseaseTrendChart?.dispose()
  rankChart?.dispose()
  kpiChart = null
  trend3Chart = null
  typeChart = null
  insightChart = null
  diseaseTrendChart = null
  rankChart = null
})

const onResize = rafThrottle(() => resizeAll())

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
}

.col {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
}

.chart {
  height: 210px;
}

.chart-tall {
  height: 340px;
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

