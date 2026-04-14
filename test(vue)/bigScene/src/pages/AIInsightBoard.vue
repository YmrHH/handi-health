<template>
  <main class="stitch-grid screen-page screen-grid">
    <aside class="stitch-col screen-col">
      <article class="frost-card">
        <div class="card-head">
          <div class="card-titlebar">
            <div class="card-title">模型效果与覆盖</div>
            <div class="card-subtitle">AUC / F1 / 建议覆盖率</div>
          </div>
        </div>
        <div class="card-body">
          <div ref="kpiRef" class="chart"></div>
        </div>
      </article>

      <article class="frost-card">
        <div class="card-head">
          <div class="card-titlebar">
            <div class="card-title">近三个月趋势</div>
            <div class="card-subtitle">告警 / 随访 / 高危</div>
          </div>
        </div>
        <div class="card-body">
          <div ref="trend3Ref" class="chart chart-tall"></div>
        </div>
      </article>

      <article class="frost-card">
        <div class="card-head">
          <div class="card-titlebar">
            <div class="card-title">数据来源分布</div>
            <div class="card-subtitle">来源结构</div>
          </div>
        </div>
        <div class="card-body">
          <div ref="typeRef" class="chart"></div>
        </div>
      </article>
    </aside>

    <section class="stitch-center screen-col">
      <article class="frost-card">
        <div class="card-head">
          <div class="card-titlebar">
            <div class="card-title">AI 趋势中枢</div>
            <div class="card-subtitle">多层环与趋势锚点</div>
          </div>
        </div>
        <div class="card-body">
          <div class="center-stage">
            <div class="hub glow-breath">
              <div class="hub-title">AI 洞察指数</div>
              <div class="hub-value">{{ insightIndexText }}</div>
              <div class="hub-sub">近三个月 · 告警 · 风险 · 建议</div>
            </div>
            <div ref="insightRef" class="hub-ring"></div>
          </div>
        </div>
      </article>

      <article class="frost-card">
        <div class="card-head">
          <div class="card-titlebar">
            <div class="card-title">病种趋势</div>
            <div class="card-subtitle">重点病种走势</div>
          </div>
        </div>
        <div class="card-body insight-center-body">
          <div ref="diseaseTrendRef" class="chart chart-tall chart-slim"></div>
          <div class="insight-summary">
            <div class="insight-summary-title">AI 智能洞察中枢</div>
            <div class="insight-summary-text">
              当前系统通过神经网络多维聚合，持续识别风险波动趋势与干预机会，支持临床与运营联动决策。
            </div>
            <div class="insight-actions">
              <button type="button" class="action-btn action-btn-primary">生成详细报告</button>
              <button type="button" class="action-btn action-btn-muted">全量模型回溯</button>
            </div>
          </div>
        </div>
      </article>
    </section>

    <aside class="stitch-col screen-col">
      <article class="frost-card">
        <div class="card-head">
          <div class="card-titlebar">
            <div class="card-title">洞察要点</div>
            <div class="card-subtitle">动态摘要</div>
          </div>
        </div>
        <div class="card-body">
          <EventTicker :items="events" />
        </div>
      </article>

      <article class="frost-card">
        <div class="card-head">
          <div class="card-titlebar">
            <div class="card-title">病种稳定率排行</div>
            <div class="card-subtitle">前五病种</div>
          </div>
        </div>
        <div class="card-body">
          <div ref="rankRef" class="chart"></div>
        </div>
      </article>

      <article class="frost-card">
        <div class="card-head">
          <div class="card-titlebar">
            <div class="card-title">重点提示</div>
            <div class="card-subtitle">关键判断</div>
          </div>
        </div>
        <div class="card-body">
          <div class="insight-notes">
            <div v-for="item in insightNotes" :key="item" class="note">{{ item }}</div>
          </div>
          <div class="source-metrics">
            <div v-for="item in sourceMetrics" :key="item.name" class="source-item">
              <div class="source-head">
                <span class="source-name">{{ item.name }}</span>
                <span class="source-val">{{ item.value }}%</span>
              </div>
              <div class="source-track">
                <div class="source-bar" :style="{ width: `${item.value}%` }"></div>
              </div>
            </div>
          </div>
        </div>
      </article>
    </aside>
  </main>
</template>

<script setup lang="ts">
import { onActivated, onDeactivated, onMounted, onUnmounted, ref } from 'vue'
import { init, type ECharts } from '../utils/echarts'
import EventTicker from '../components/EventTicker.vue'
import { axisStyle, baseGrid, legendStyle, tooltipStyle } from '../utils/chartTheme'
import { fetchAlerts, fetchHomeStats, fetchPatientSummary, fetchReportBoard } from '../api'
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

const insightIndexText = ref('0')
const sourceHasData = ref(false)
const diseaseHasData = ref(false)
const stableHasData = ref(false)
const sourceMetrics = ref<Array<{ name: string; value: number }>>([])
const insightNotes = ref<string[]>([])

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

function buildFallbackDiseaseTop(months: string[], values: number[]) {
  const names = months.length ? months : ['近一月', '近二月', '近三月']
  const vals = values.length ? values : [0, 0, 0]
  buildDiseaseTop(
    names.map((name, idx) => ({
      disease: `${name}风险趋势`,
      patientCount: Number(vals[idx] || 0)
    }))
  )
}

function buildFallbackStableRank(alerts: number[], followups: number[], highRisk: number[]) {
  const rows = [
    { disease: '随访稳定度', stableRate: followups[0] ? Math.min(1, followups[0] / Math.max(1, alerts[0] || 1)) : 0 },
    { disease: '告警回落率', stableRate: alerts[1] ? Math.min(1, 1 - highRisk[1] / Math.max(1, alerts[1])) : 0 },
    { disease: '风险改善率', stableRate: highRisk[2] ? Math.min(1, 1 - highRisk[2] / Math.max(1, alerts[2] || 1)) : 0 }
  ]
  buildStableRank(rows)
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

async function loadBoard() {
  const [board, homeStats, alertsRes, summaryRes] = await Promise.all([
    fetchReportBoard().catch(() => ({} as any)),
    fetchHomeStats().catch(() => ({} as any)),
    fetchAlerts(30).catch(() => ({} as any)),
    fetchPatientSummary(200).catch(() => ({} as any))
  ])
  const auc = Number(board?.latestAuc || 0) * 100
  const f1 = Number(board?.latestF1 || 0) * 100
  insightIndexText.value = auc || f1 ? `${Math.round((auc + f1) / 2)}` : '0'

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
  if (sourceHasData.value) {
    buildSourcePie(srcNames, srcCounts)
  } else {
    buildSourcePie(['告警数据', '随访数据', '画像数据'], [board?.latestAlerts || 0, board?.latestFollowups || 0, board?.latestHighRisk || 0])
  }
  const sourceRows = sourceHasData.value
    ? srcNames.map((name, i) => ({ name, count: Number(srcCounts[i] || 0) }))
    : [
        { name: '告警数据', count: Number(board?.latestAlerts || 0) },
        { name: '随访数据', count: Number(board?.latestFollowups || 0) },
        { name: '画像数据', count: Number(board?.latestHighRisk || 0) }
      ]
  const sourceTotal = sourceRows.reduce((s, x) => s + x.count, 0) || 1
  sourceMetrics.value = sourceRows
    .map((x) => ({ name: x.name, value: Math.min(100, Math.max(0, Math.round((x.count / sourceTotal) * 100))) }))
    .slice(0, 3)

  buildInsightCenter({
    alerts: board?.latestAlerts,
    followups: board?.latestFollowups,
    highRisk: board?.latestHighRisk,
    auc: board?.latestAuc,
    f1: board?.latestF1
  })

  const diseaseAnalysis = (board?.diseaseAnalysis || []) as any[]
  diseaseHasData.value = Array.isArray(diseaseAnalysis) && diseaseAnalysis.length > 0
  if (diseaseHasData.value) {
    buildDiseaseTop(diseaseAnalysis)
  } else {
    buildFallbackDiseaseTop(monthsAll.slice(start), highAll.slice(start))
  }
  stableHasData.value = Array.isArray(diseaseAnalysis) && diseaseAnalysis.some((x: any) => x && x.stableRate != null)
  if (stableHasData.value) {
    buildStableRank(diseaseAnalysis)
  } else {
    buildFallbackStableRank(alertsAll.slice(start), followAll.slice(start), highAll.slice(start))
  }

  const latestMonth = String(board?.latestMonth || '').trim()
  events.value = [
    { id: 'm', title: latestMonth ? `本月：${latestMonth}` : '本月：当前周期', time: '最新' },
    { id: 'a', title: `告警变化率：${board?.alertChangeRate != null ? Math.round(Number(board.alertChangeRate) * 100) + '%' : '0%'}`, time: '最新' },
    { id: 'h', title: `高危变化率：${board?.highRiskChangeRate != null ? Math.round(Number(board.highRiskChangeRate) * 100) + '%' : '0%'}`, time: '最新' }
  ]
  const alertRows = (alertsRes?.rows || alertsRes?.list || []) as any[]
  const summaryRows = (summaryRes?.rows || summaryRes?.list || []) as any[]
  const managedCount = Number(homeStats?.totalPatients || homeStats?.patientCount || summaryRows.length || 0)
  const activeAlertCount = alertRows.length
  const highRiskCount = Number(board?.latestHighRisk || 0)
  insightNotes.value = [
    `风险波动：当前高风险患者 ${highRiskCount} 人，建议优先复核波动人群。`,
    `联动压力：近 30 天告警 ${activeAlertCount} 条，建议按等级优化闭环时效。`,
    `运营覆盖：受管患者 ${managedCount} 人，建议强化高风险随访连续性。`
  ]
}

onMounted(async () => {
  activeAlive = true
  await loadBoard()

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
  void loadBoard()
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
  height: 100%;
  min-height: 0;
}

.chart-tall {
  height: 100%;
  min-height: 0;
}

.chart-slim {
  flex: 1.08 1 0;
}

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
  border: 1px solid rgba(114, 180, 205, 0.34);
  box-shadow: var(--glow-strong);
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

.stitch-col:first-child > .frost-card:nth-child(1) { flex: 0.95 1 0; }
.stitch-col:first-child > .frost-card:nth-child(2) { flex: 1.15 1 0; }
.stitch-col:first-child > .frost-card:nth-child(3) { flex: 0.9 1 0; }
.stitch-center > .frost-card:nth-child(1) { flex: 1.18 1 0; }
.stitch-center > .frost-card:nth-child(2) { flex: 0.82 1 0; }
.stitch-col:last-child > .frost-card:nth-child(1) { flex: 1.05 1 0; }
.stitch-col:last-child > .frost-card:nth-child(2) { flex: 0.95 1 0; }
.stitch-col:last-child > .frost-card:nth-child(3) { flex: 1 1 0; }

.insight-center-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.insight-summary {
  flex: 0.92 1 0;
  min-height: 0;
  border-radius: var(--r-md);
  border: 1px solid rgba(114, 180, 205, 0.2);
  background: rgba(255, 255, 255, 0.5);
  padding: 10px 12px;
  display: grid;
  align-content: center;
  gap: 8px;
}

.insight-summary-title {
  font-size: 22px;
  line-height: 1.1;
  font-weight: 900;
  text-align: center;
  color: rgba(18, 99, 109, 0.98);
}

.insight-summary-text {
  font-size: 12px;
  line-height: 1.45;
  text-align: center;
  color: rgba(58, 91, 112, 0.9);
}

.insight-actions {
  display: flex;
  justify-content: center;
  gap: 10px;
}

.action-btn {
  height: 32px;
  border-radius: 10px;
  border: 1px solid transparent;
  padding: 0 14px;
  font-size: 11px;
  font-weight: 700;
  cursor: pointer;
}

.action-btn-primary {
  color: #fff;
  background: linear-gradient(135deg, #0f8e85, #0b7a72);
}

.action-btn-muted {
  color: rgba(39, 85, 113, 0.94);
  background: rgba(214, 227, 233, 0.72);
  border-color: rgba(114, 180, 205, 0.2);
}

.insight-notes {
  display: grid;
  gap: 8px;
  padding: 4px 2px;
  margin-bottom: 10px;
}

.note {
  padding: 8px 10px;
  border-radius: var(--r-md);
  border: 1px solid rgba(114, 180, 205, 0.22);
  background: rgba(255, 255, 255, 0.58);
  color: rgba(39, 85, 113, 0.92);
  font-size: 10px;
  line-height: 1.35;
}

.source-metrics {
  display: grid;
  gap: 8px;
}

.source-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.source-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 10px;
  color: rgba(39, 85, 113, 0.92);
}

.source-val {
  flex-shrink: 0;
  font-size: 10px;
  font-weight: 700;
  color: rgba(32, 82, 110, 0.94);
}

.source-track {
  width: 100%;
  height: 6px;
  border-radius: 999px;
  background: rgba(114, 180, 205, 0.18);
  overflow: hidden;
}

.source-bar {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(95, 199, 216, 0.9), rgba(140, 188, 227, 0.9));
}

.frost-card {
  min-height: 0;
  display: flex;
  flex-direction: column;
  border-radius: var(--r-lg);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.66), rgba(248, 253, 255, 0.46));
  box-shadow: 0 16px 34px rgba(0, 103, 96, 0.07), inset 0 1px 0 rgba(255, 255, 255, 0.36);
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
  .insight-summary-title { font-size: 18px; }
}
</style>

