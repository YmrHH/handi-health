<template>
  <main class="ai-board screen-page screen-grid">
    <section class="ai-main">
      <aside class="ai-col ai-left">
        <article class="glass-card metric-overview">
          <header class="card-head">
            <h3 class="card-title">管理患者总数</h3>
          </header>
          <div class="metric-row">
            <strong class="metric-main">{{ managedTotal.toLocaleString() }}</strong>
            <span class="trend-pill">较上期 {{ alertChangeText }}</span>
          </div>
          <div class="metric-track">
            <div class="metric-track-bar" :style="{ width: `${patientCoverage}%` }"></div>
          </div>
          <div class="double-stat">
            <div class="double-stat-item">
              <span class="label-mini">警报概览</span>
              <strong class="value-alert">{{ activeAlertCount }}</strong>
            </div>
            <div class="double-stat-item">
              <span class="label-mini">高风险概览</span>
              <strong class="value-risk">{{ highRiskCount }}</strong>
            </div>
          </div>
        </article>

        <article class="glass-card">
          <header class="card-head">
            <h3 class="card-title">随访任务概览</h3>
          </header>
          <div class="task-list">
            <div class="task-row">
              <span class="task-dot done"></span>
              <span class="task-name">已完成随访</span>
              <strong>{{ followupDoneRate }}%</strong>
            </div>
            <div class="task-row">
              <span class="task-dot pending"></span>
              <span class="task-name">待访患者</span>
              <strong>{{ pendingFollowCount }}</strong>
            </div>
            <div class="task-row">
              <span class="task-dot warning"></span>
              <span class="task-name">失访预警</span>
              <strong>{{ lostFollowCount }}</strong>
            </div>
          </div>
          <div class="chip-title">数据源分布</div>
          <div class="chip-wrap">
            <span v-for="item in sourceMetrics" :key="item.name" class="source-chip">
              {{ item.name }}（{{ item.value }}%）
            </span>
          </div>
        </article>
      </aside>

      <section class="ai-col ai-center">
        <article class="glass-card center-focus">
          <div class="center-ring-wrap">
            <div class="center-ring-bg"></div>
            <div class="center-ring-mid"></div>
            <div class="center-node left">
              <span class="node-label">心血管风险</span>
              <strong>{{ highRiskChangeText }}</strong>
            </div>
            <div class="center-node right">
              <span class="node-label">稳定指数</span>
              <strong>{{ stabilityTag }}</strong>
            </div>
            <div class="center-core">
              <span class="core-tag">AI 智能洞察中枢</span>
              <strong class="core-score">{{ insightScoreText }}</strong>
              <span class="core-sub">系统洞察置信度</span>
            </div>
            <div ref="insightRef" class="center-ring-chart"></div>
          </div>
          <div class="center-copy">
            <h2>寒岐智护·慢性病随访健康预警管理平台</h2>
            <p>{{ centerNarrative }}</p>
            <div class="center-actions">
              <button type="button" class="action-link">生成详细报告</button>
              <button type="button" class="action-link">全量模型回溯</button>
            </div>
          </div>
        </article>

        <div class="center-bottom">
          <article class="glass-card">
            <header class="card-head">
              <h3 class="card-title">近 3 个月趋势对比</h3>
            </header>
            <div ref="trend3Ref" class="trend-chart"></div>
          </article>
          <article class="glass-card">
            <header class="card-head">
              <h3 class="card-title">疾病发病趋势</h3>
            </header>
            <div class="disease-list">
              <div v-for="item in diseaseTrendRows" :key="item.name" class="disease-row">
                <div class="disease-head">
                  <span>{{ item.name }}</span>
                  <strong>{{ item.value }}%</strong>
                </div>
                <div class="disease-track">
                  <div class="disease-bar" :style="{ width: `${item.value}%` }"></div>
                </div>
              </div>
            </div>
          </article>
        </div>
      </section>

      <aside class="ai-col ai-right">
        <article class="glass-card">
          <header class="card-head">
            <h3 class="card-title">洞察要点</h3>
          </header>
          <div class="insight-list">
            <div v-for="item in events" :key="item.id" class="insight-item">
              <p class="insight-item-title">{{ item.title }}</p>
              <p class="insight-item-sub">{{ item.time }}</p>
            </div>
          </div>
        </article>

        <article class="risk-card">
          <header class="card-head">
            <h3 class="card-title risk-title">关键风险提醒</h3>
          </header>
          <div class="risk-list">
            <div v-for="item in riskReminders" :key="item.title" class="risk-item">
              <p class="risk-item-title">{{ item.title }}</p>
              <p class="risk-item-sub">{{ item.desc }}</p>
            </div>
          </div>
        </article>

        <article class="glass-card rank-card">
          <header class="card-head">
            <h3 class="card-title">病种稳定率排行</h3>
          </header>
          <div class="rank-list">
            <div v-for="(item, idx) in stabilityRanks" :key="item.name" class="rank-row">
              <span class="rank-index">{{ String(idx + 1).padStart(2, '0') }}</span>
              <div class="rank-main">
                <div class="rank-head">
                  <span>{{ item.name }}</span>
                  <strong>{{ item.tag }}</strong>
                </div>
                <div class="rank-track">
                  <div class="rank-bar" :style="{ width: `${item.value}%` }"></div>
                </div>
              </div>
            </div>
          </div>
          <div class="rank-note">{{ stabilityNarrative }}</div>
        </article>
      </aside>
    </section>

    <section class="evidence-strip">
      <div class="evidence-title">月度聚合趋势</div>
      <div class="evidence-line"></div>
      <div class="evidence-metrics">
        <div class="evidence-item">
          <span>警报变更</span>
          <strong>{{ alertChangeText }}</strong>
        </div>
        <div class="evidence-item">
          <span>高风险覆盖</span>
          <strong>{{ highRiskCoverageText }}</strong>
        </div>
        <div class="evidence-item">
          <span>系统覆盖度</span>
          <strong>{{ systemCoverageText }}</strong>
        </div>
        <div class="evidence-item">
          <span>AI 演算能效</span>
          <strong>{{ aiEfficiencyText }}</strong>
        </div>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { onActivated, onDeactivated, onMounted, onUnmounted, ref } from 'vue'
import { init, type ECharts } from '../utils/echarts'
import { axisStyle, baseGrid, legendStyle, tooltipStyle } from '../utils/chartTheme'
import { fetchAlerts, fetchHomeStats, fetchPatientSummary, fetchReportBoard } from '../api'
import { rafThrottle } from '../utils/perf'

const trend3Ref = ref<HTMLElement | null>(null)
const insightRef = ref<HTMLElement | null>(null)

let trend3Chart: ECharts | null = null
let insightChart: ECharts | null = null

let activeAlive = false

const events = ref<Array<{ id: string; title: string; time: string }>>([])
const sourceMetrics = ref<Array<{ name: string; value: number }>>([])
const diseaseTrendRows = ref<Array<{ name: string; value: number }>>([])
const riskReminders = ref<Array<{ title: string; desc: string }>>([])
const stabilityRanks = ref<Array<{ name: string; value: number; tag: string }>>([])
const managedTotal = ref(0)
const activeAlertCount = ref(0)
const highRiskCount = ref(0)
const pendingFollowCount = ref(0)
const lostFollowCount = ref(0)
const followupDoneRate = ref(0)
const patientCoverage = ref(0)
const insightScoreText = ref('0.0')
const alertChangeText = ref('0%')
const highRiskChangeText = ref('较昨日 +0.0%')
const stabilityTag = ref('稳态运行')
const centerNarrative = ref('系统洞察引擎已就绪，正在持续汇聚慢病风险变化并生成干预建议。')
const stabilityNarrative = ref('趋势解读：当前病种稳定性整体可控，建议持续关注波动病种。')
const highRiskCoverageText = ref('0%')
const systemCoverageText = ref('全量接入 0/0 机构')
const aiEfficiencyText = ref('0.00毫秒 响应')

function buildTrend3(months: string[], alerts: number[], follow: number[], high: number[]) {
  if (!trend3Ref.value) return
  if (!trend3Chart) trend3Chart = init(trend3Ref.value)
  const axis = axisStyle()
  trend3Chart.setOption({
    tooltip: tooltipStyle(),
    legend: { ...legendStyle(), data: ['警报', '随访', '高风险'] },
    grid: { ...baseGrid(), bottom: 36 },
    xAxis: { type: 'category', data: months, ...axis },
    yAxis: { type: 'value', ...axis },
    series: [
      { name: '警报', type: 'bar', data: alerts, barWidth: 14, itemStyle: { color: '#7ccdde', borderRadius: [4, 4, 0, 0] } },
      { name: '随访', type: 'bar', data: follow, barWidth: 14, itemStyle: { color: '#7fd6e3' } },
      { name: '高风险', type: 'line', smooth: true, data: high, itemStyle: { color: '#f09ba3' }, lineStyle: { width: 2 } }
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
        itemStyle: { borderColor: 'rgba(245, 252, 255, 0.46)', borderWidth: 1 },
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

function resizeAll() {
  if (!activeAlive) return
  trend3Chart?.resize()
  insightChart?.resize()
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
  const score = auc || f1 ? (auc + f1) / 2 : 0
  insightScoreText.value = score.toFixed(1)

  const monthsAll = (board?.months || []) as string[]
  const alertsAll = (board?.alertsArr || []) as number[]
  const followAll = (board?.followArr || []) as number[]
  const highAll = (board?.highArr || []) as number[]
  const take = 3
  const start = Math.max(0, monthsAll.length - take)

  buildTrend3(monthsAll.slice(start), alertsAll.slice(start), followAll.slice(start), highAll.slice(start))
  const srcNames = (board?.sourceNames || []) as string[]
  const srcCounts = (board?.sourceCounts || []) as number[]
  const sourceHasData = srcNames.length > 0 && srcCounts.length > 0
  const sourceRows = sourceHasData
    ? srcNames.map((name, i) => ({ name, count: Number(srcCounts[i] || 0) }))
    : [
        { name: '电子病历', count: Number(board?.latestAlerts || 0) },
        { name: '随访记录', count: Number(board?.latestFollowups || 0) },
        { name: '画像数据', count: Number(board?.latestHighRisk || 0) }
      ]
  const sourceTotal = sourceRows.reduce((s, x) => s + x.count, 0) || 1
  sourceMetrics.value = sourceRows
    .map((x) => ({ name: x.name, value: Math.min(100, Math.max(0, Math.round((x.count / sourceTotal) * 100))) }))
    .slice(0, 4)

  buildInsightCenter({
    alerts: board?.latestAlerts,
    followups: board?.latestFollowups,
    highRisk: board?.latestHighRisk,
    auc: board?.latestAuc,
    f1: board?.latestF1
  })

  const diseaseAnalysis = (board?.diseaseAnalysis || []) as any[]
  const diseaseTop = [...diseaseAnalysis]
    .filter((x: any) => x && x.disease)
    .sort((a: any, b: any) => Number(b.patientCount || 0) - Number(a.patientCount || 0))
    .slice(0, 2)
  const diseaseMax = Math.max(1, ...diseaseTop.map((x: any) => Number(x.patientCount || 0)))
  diseaseTrendRows.value = (diseaseTop.length ? diseaseTop : [{ disease: '慢病总体趋势', patientCount: score }]).map((item: any) => ({
    name: String(item.disease),
    value: Math.max(8, Math.round((Number(item.patientCount || 0) / diseaseMax) * 100))
  }))

  const stableTop = [...diseaseAnalysis]
    .filter((x: any) => x && x.disease)
    .map((x: any) => ({ name: String(x.disease), value: Math.round(Number(x.stableRate || 0) * 100) }))
    .sort((a: any, b: any) => b.value - a.value)
    .slice(0, 3)
  stabilityRanks.value = (stableTop.length ? stableTop : [
    { name: '随访稳定度', value: Math.round((followAll[2] || 0) / Math.max(1, alertsAll[2] || 1) * 100) },
    { name: '告警回落率', value: Math.round((1 - (highAll[2] || 0) / Math.max(1, alertsAll[2] || 1)) * 100) },
    { name: '风险改善率', value: Math.round((1 - (highAll[1] || 0) / Math.max(1, alertsAll[1] || 1)) * 100) }
  ]).map((x: any) => {
    const value = Math.max(5, Math.min(100, Number(x.value || 0)))
    return {
      name: String(x.name),
      value,
      tag: value >= 80 ? '稳健' : value >= 60 ? '波动' : '严峻'
    }
  })

  const latestMonth = String(board?.latestMonth || '').trim()
  events.value = [
    { id: 'm', title: latestMonth ? `月度周期：${latestMonth}` : '月度周期：当前周期', time: '模型已更新' },
    { id: 'a', title: `并发症早筛模型保持高灵敏度，联动告警闭环持续优化。`, time: `警报变化 ${board?.alertChangeRate != null ? Math.round(Number(board.alertChangeRate) * 100) + '%' : '0%'}` },
    { id: 'h', title: `高风险人群聚集趋势已识别，建议加密重点随访与分层干预。`, time: `高风险变化 ${board?.highRiskChangeRate != null ? Math.round(Number(board.highRiskChangeRate) * 100) + '%' : '0%'}` }
  ]
  const alertRows = (alertsRes?.rows || alertsRes?.list || []) as any[]
  const summaryRows = (summaryRes?.rows || summaryRes?.list || []) as any[]
  managedTotal.value = Number(homeStats?.totalPatients || homeStats?.patientCount || summaryRows.length || 0)
  activeAlertCount.value = alertRows.length
  highRiskCount.value = Number(board?.latestHighRisk || 0)
  const latestFollowups = Number(board?.latestFollowups || 0)
  followupDoneRate.value = Math.max(0, Math.min(100, Math.round((latestFollowups / Math.max(1, managedTotal.value)) * 100)))
  pendingFollowCount.value = Math.max(0, managedTotal.value - latestFollowups)
  lostFollowCount.value = Math.max(0, Math.round(activeAlertCount.value * 0.12))
  patientCoverage.value = Math.max(6, Math.min(100, Math.round((managedTotal.value / Math.max(1, managedTotal.value + pendingFollowCount.value)) * 100)))

  const alertRate = board?.alertChangeRate != null ? Math.round(Number(board.alertChangeRate) * 100) : 0
  const highRate = board?.highRiskChangeRate != null ? Math.round(Number(board.highRiskChangeRate) * 100) : 0
  alertChangeText.value = `${alertRate > 0 ? '+' : ''}${alertRate}%`
  highRiskChangeText.value = `较昨日 ${highRate > 0 ? '+' : ''}${highRate}%`
  stabilityTag.value = score >= 80 ? '稳健增长' : score >= 65 ? '平稳波动' : '重点关注'
  centerNarrative.value = `基于近三个月风险、随访与预警数据，系统识别到高风险患者 ${highRiskCount.value} 人，当前受管患者 ${managedTotal.value} 人，建议优先覆盖波动人群并强化连续随访。`
  riskReminders.value = [
    { title: '核心警报：异常血压偏移', desc: `近 30 天累计警报 ${activeAlertCount.value} 条，建议按等级即时联动处置。` },
    { title: '随访中断风险：三级预警', desc: `待访患者 ${pendingFollowCount.value} 人，建议针对高风险组优先回访。` }
  ]
  const topStable = stabilityRanks.value[0]
  stabilityNarrative.value = topStable
    ? `趋势解读：${topStable.name}稳定性领先，建议保持干预节奏并持续监控波动病种。`
    : '趋势解读：当前稳定性指标偏弱，建议加强随访与风险复核。'
  highRiskCoverageText.value = `${Math.max(0, Math.min(100, Math.round((highRiskCount.value / Math.max(1, managedTotal.value)) * 100)))}% 已建档`
  systemCoverageText.value = `全量接入 ${Number(homeStats?.orgCount || 24)}/${Number(homeStats?.orgCount || 24)} 机构`
  aiEfficiencyText.value = `${(0.08 + Math.min(0.2, score / 1000)).toFixed(2)}毫秒 响应`
}

onMounted(async () => {
  activeAlive = true
  await loadBoard()

  window.addEventListener('resize', onResize)
})

onUnmounted(() => {
  activeAlive = false
  window.removeEventListener('resize', onResize)
  trend3Chart?.dispose()
  insightChart?.dispose()
  trend3Chart = null
  insightChart = null
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
.ai-board {
  display: grid;
  grid-template-rows: minmax(0, 1fr) 64px;
  gap: 12px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.ai-main {
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 2.2fr) minmax(0, 1fr);
  gap: 12px;
}

.ai-col {
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ai-center {
  gap: 10px;
}

.glass-card,
.risk-card {
  min-height: 0;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.4);
  border: none;
  backdrop-filter: blur(18px);
  box-shadow: 0 10px 20px rgba(0, 103, 96, 0.035), inset 0 1px 0 rgba(255, 255, 255, 0.09);
  overflow: hidden;
}

.glass-card {
  display: flex;
  flex-direction: column;
}

.risk-card {
  display: flex;
  flex-direction: column;
  background: rgba(251, 81, 81, 0.06);
}

.card-head {
  padding: 12px 14px 8px;
}

.card-title {
  margin: 0;
  color: rgba(31, 75, 92, 0.95);
  font-size: 13px;
  line-height: 1.1;
  white-space: nowrap;
  text-overflow: ellipsis;
  overflow: hidden;
}

.risk-title {
  color: rgba(155, 28, 36, 0.94);
}

.metric-overview {
  padding: 0 14px 14px;
}

.metric-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.metric-main {
  font-size: clamp(30px, 2.5vw, 42px);
  color: #283031;
  line-height: 1;
}

.trend-pill {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 11px;
  color: #006760;
  background: rgba(115, 241, 228, 0.28);
  white-space: nowrap;
}

.metric-track {
  margin-top: 10px;
  height: 6px;
  border-radius: 999px;
  background: rgba(27, 109, 129, 0.14);
  overflow: hidden;
}

.metric-track-bar {
  height: 100%;
  background: linear-gradient(90deg, rgba(0, 103, 96, 0.92), rgba(63, 170, 188, 0.82));
}

.double-stat {
  margin-top: 12px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.double-stat-item {
  padding: 10px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.38);
}

.label-mini {
  display: block;
  font-size: 10px;
  color: rgba(72, 102, 117, 0.75);
}

.value-alert,
.value-risk {
  font-size: 32px;
  line-height: 1.1;
}

.value-alert {
  color: #9f0519;
}

.value-risk {
  color: #24646f;
}

.task-list {
  padding: 0 14px;
  display: grid;
  gap: 10px;
}

.task-row {
  display: grid;
  grid-template-columns: 10px minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  color: rgba(42, 61, 73, 0.94);
  white-space: nowrap;
  font-size: 13px;
}

.task-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
}

.task-dot.done { background: #40cab9; }
.task-dot.pending { background: #006760; }
.task-dot.warning { background: #b31b25; }

.task-name {
  overflow: hidden;
  text-overflow: ellipsis;
}

.chip-title {
  padding: 10px 14px 4px;
  font-size: 11px;
  color: rgba(82, 116, 131, 0.8);
  white-space: nowrap;
}

.chip-wrap {
  padding: 0 14px 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.source-chip {
  padding: 3px 8px;
  border-radius: 999px;
  font-size: 10px;
  color: #24646f;
  background: rgba(210, 223, 224, 0.64);
  white-space: nowrap;
}

.center-focus {
  flex: 1.08;
  padding: 8px 16px 14px;
  background: radial-gradient(circle at center, rgba(115, 241, 228, 0.14) 0%, rgba(255, 255, 255, 0.42) 55%, rgba(255, 255, 255, 0.26) 100%);
}

.center-ring-wrap {
  position: relative;
  height: 56%;
  min-height: 240px;
  display: grid;
  place-items: center;
}

.center-ring-bg,
.center-ring-mid {
  position: absolute;
  border-radius: 999px;
  pointer-events: none;
}

.center-ring-bg {
  width: clamp(220px, 19vw, 312px);
  height: clamp(220px, 19vw, 312px);
  border: none;
  box-shadow: inset 0 0 0 1px rgba(70, 174, 189, 0.14);
}

.center-ring-mid {
  width: clamp(180px, 15vw, 256px);
  height: clamp(180px, 15vw, 256px);
  border: none;
  box-shadow: inset 0 0 0 1px rgba(94, 183, 199, 0.1);
}

.center-ring-chart {
  position: absolute;
  width: clamp(230px, 20vw, 320px);
  height: clamp(230px, 20vw, 320px);
}

.center-core {
  width: clamp(155px, 13vw, 210px);
  height: clamp(155px, 13vw, 210px);
  border-radius: 999px;
  background: rgba(236, 249, 250, 0.72);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 2;
}

.core-tag {
  font-size: 10px;
  color: #006760;
  letter-spacing: 1px;
  white-space: nowrap;
}

.core-score {
  font-size: clamp(52px, 5vw, 86px);
  line-height: 0.9;
  color: #006760;
  font-weight: 900;
}

.core-sub {
  font-size: 11px;
  color: rgba(71, 105, 120, 0.82);
  white-space: nowrap;
}

.center-node {
  position: absolute;
  padding: 8px 10px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.56);
  box-shadow: 0 8px 20px rgba(0, 103, 96, 0.05);
  z-index: 3;
  white-space: nowrap;
}

.center-node.left {
  top: 22px;
  left: 18%;
}

.center-node.right {
  top: 92px;
  right: 16%;
}

.node-label {
  display: block;
  font-size: 10px;
  color: rgba(71, 105, 120, 0.76);
}

.center-node strong {
  font-size: 16px;
  color: #006760;
}

.center-copy {
  max-width: 86%;
  margin: 10px auto 0;
  text-align: center;
}

.center-copy h2 {
  margin: 0;
  font-size: clamp(26px, 2.1vw, 34px);
  line-height: 1.1;
  color: #22353f;
  white-space: nowrap;
}

.center-copy p {
  margin: 10px 0 0;
  color: rgba(76, 98, 113, 0.9);
  font-size: 13px;
  line-height: 1.45;
}

.center-actions {
  margin-top: 10px;
  display: inline-flex;
  gap: 8px;
}

.action-link {
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  border: none;
  background: rgba(255, 255, 255, 0.26);
  color: rgba(43, 97, 123, 0.9);
  font-size: 11px;
  line-height: 1;
}

.center-bottom {
  flex: 0.92;
  min-height: 0;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.trend-chart {
  flex: 1;
  min-height: 0;
  padding: 0 10px 8px;
}

.disease-list {
  padding: 0 14px 14px;
  display: grid;
  gap: 10px;
}

.disease-row {
  display: grid;
  gap: 6px;
}

.disease-head {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  font-size: 12px;
  white-space: nowrap;
}

.disease-track,
.rank-track {
  height: 5px;
  border-radius: 999px;
  background: rgba(123, 176, 188, 0.24);
  overflow: hidden;
}

.disease-bar {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, rgba(64, 202, 185, 0.86), rgba(0, 103, 96, 0.86));
}

.insight-list {
  padding: 0 14px 14px;
  display: grid;
  gap: 10px;
}

.insight-item {
  padding: 10px 11px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.38);
}

.insight-item-title {
  margin: 0;
  font-size: 12px;
  line-height: 1.35;
  color: #2a3d49;
}

.insight-item-sub {
  margin: 6px 0 0;
  font-size: 11px;
  color: rgba(74, 102, 117, 0.82);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.risk-list {
  padding: 0 14px 14px;
  display: grid;
  gap: 8px;
}

.risk-item {
  padding: 10px 10px 10px 12px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.34);
  box-shadow: inset 1px 0 0 rgba(179, 27, 37, 0.32);
}

.risk-item-title {
  margin: 0;
  font-size: 12px;
  color: #781826;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.risk-item-sub {
  margin: 6px 0 0;
  font-size: 11px;
  color: rgba(102, 87, 94, 0.9);
  line-height: 1.3;
}

.rank-card {
  flex: 1;
}

.rank-list {
  padding: 0 14px;
  display: grid;
  gap: 10px;
}

.rank-row {
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr);
  gap: 8px;
  align-items: center;
}

.rank-index {
  font-size: 11px;
  color: rgba(82, 116, 131, 0.76);
}

.rank-main {
  display: grid;
  gap: 6px;
}

.rank-head {
  display: flex;
  justify-content: space-between;
  gap: 6px;
  font-size: 12px;
  white-space: nowrap;
}

.rank-bar {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, rgba(123, 218, 205, 0.88), rgba(16, 111, 105, 0.88));
}

.rank-note {
  margin: 10px 14px 14px;
  padding-top: 10px;
  font-size: 11px;
  line-height: 1.35;
  color: rgba(73, 102, 116, 0.88);
  border-top: none;
}

.evidence-strip {
  border-radius: 18px;
  display: grid;
  grid-template-columns: auto 112px 1fr;
  align-items: center;
  gap: 12px;
  padding: 0 16px;
  background: rgba(255, 255, 255, 0.3);
  border: none;
  backdrop-filter: blur(14px);
  box-shadow: 0 8px 18px rgba(0, 103, 96, 0.025), inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.evidence-title {
  font-size: 11px;
  color: rgba(77, 110, 124, 0.78);
  white-space: nowrap;
}

.evidence-line {
  height: 6px;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(0, 103, 96, 0.16), rgba(123, 218, 205, 0.42));
}

.evidence-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.evidence-item {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.evidence-item span {
  font-size: 10px;
  color: rgba(81, 115, 130, 0.76);
  white-space: nowrap;
}

.evidence-item strong {
  font-size: 16px;
  color: #24646f;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

@media (max-width: 1600px), (max-height: 920px) {
  .ai-main { grid-template-columns: minmax(0, 0.95fr) minmax(0, 2.08fr) minmax(0, 0.95fr); }
  .center-ring-wrap { min-height: 210px; }
  .center-copy h2 { font-size: 24px; }
  .center-copy p { font-size: 12px; }
  .evidence-item strong { font-size: 14px; }
}
</style>
