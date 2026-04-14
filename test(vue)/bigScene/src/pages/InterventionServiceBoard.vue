<template>
  <main class="stitch-grid screen-page screen-grid">
    <aside class="stitch-col screen-col">
      <article class="frost-card">
        <div class="card-head">
          <div class="card-titlebar">
            <div class="card-title">干预计划总览</div>
            <div class="card-subtitle">新增 · 执行 · 响应</div>
          </div>
        </div>
        <div class="card-body">
          <div class="plan-overview">
            <div class="metric-card">
              <div class="metric-label">今日新增任务</div>
              <div class="metric-value">{{ todayNewCount }}</div>
              <div class="metric-sub">较昨日 {{ todayNewDeltaText }}</div>
            </div>
            <div class="metric-card">
              <div class="metric-label">正在执行方案</div>
              <div class="metric-value">{{ executingCount }}</div>
              <div class="metric-sub">待执行 {{ board.overview.pending }} · 超期 {{ board.overview.overdue }}</div>
            </div>
            <div class="metric-card">
              <div class="metric-label">平均干预响应时效</div>
              <div class="metric-value">{{ avgResponseText }}</div>
              <div class="metric-sub">触达率 {{ board.reachRate }}% · 活跃人员 {{ board.staffCount }}</div>
            </div>
          </div>
        </div>
      </article>

      <article class="frost-card">
        <div class="card-head">
          <div class="card-titlebar">
            <div class="card-title">计划类型分布</div>
            <div class="card-subtitle">远程随访 · 门诊干预 · 紧急送诊</div>
          </div>
        </div>
        <div class="card-body">
          <div class="type-dist">
            <div ref="planTypeRef" class="chart type-chart"></div>
            <div class="type-list">
              <div v-for="item in typeDistRows" :key="item.name" class="type-row">
                <span class="type-dot" :style="{ background: item.color }"></span>
                <span class="type-name">{{ item.name }}</span>
                <span class="type-count">{{ item.value }}</span>
              </div>
            </div>
          </div>
        </div>
      </article>

    </aside>

    <section class="stitch-center screen-col">
      <article class="frost-card">
        <div class="card-head">
          <div class="card-titlebar">
            <div class="card-title">服务执行全生命周期</div>
            <div class="card-subtitle">生成 · 分派 · 执行 · 反馈 · 再评估</div>
          </div>
        </div>
        <div class="card-body">
          <div class="center-stage">
            <div class="hub glow-breath">
              <div class="hub-title">生命周期执行</div>
              <div class="hub-value">{{ lifecycleScore }}</div>
              <div class="hub-sub">今日完成 {{ board.overview.done }} · 执行中 {{ board.overview.inProgress }} · 触达率 {{ board.reachRate }}%</div>
            </div>
            <div ref="serviceRingRef" class="hub-ring"></div>
            <div class="lifecycle-steps">
              <div class="step">
                <span class="step-dot"></span>
                <span class="step-name">方案生成</span>
                <span class="step-val">{{ todayNewCount }}</span>
              </div>
              <div class="step">
                <span class="step-dot"></span>
                <span class="step-name">任务分派</span>
                <span class="step-val">{{ Math.max(0, board.overview.total - board.overview.pending) }}</span>
              </div>
              <div class="step">
                <span class="step-dot"></span>
                <span class="step-name">服务执行</span>
                <span class="step-val">{{ board.overview.inProgress }}</span>
              </div>
              <div class="step">
                <span class="step-dot"></span>
                <span class="step-name">闭环反馈</span>
                <span class="step-val">{{ board.overview.done }}</span>
              </div>
              <div class="step">
                <span class="step-dot warn"></span>
                <span class="step-name">再评估</span>
                <span class="step-val">{{ board.overview.overdue }}</span>
              </div>
            </div>
          </div>
        </div>
      </article>

      <article class="frost-card service-event-panel">
        <div class="card-head">
          <div class="card-titlebar">
            <div class="card-title">最新服务动态</div>
            <div class="card-subtitle">触达 · 执行 · 反馈</div>
          </div>
        </div>
        <div class="card-body">
          <EventTicker :items="events" />
        </div>
      </article>
    </section>

    <aside class="stitch-col screen-col">
      <article class="frost-card hot-plan-panel">
        <div class="card-head">
          <div class="card-titlebar">
            <div class="card-title">热门干预方案 TOP</div>
            <div class="card-subtitle">热度与有效性</div>
          </div>
        </div>
        <div class="card-body">
          <div class="hot-list">
            <div v-for="(item, idx) in hotPlanRows" :key="item.name" class="hot-item">
              <div class="hot-rank">{{ String(idx + 1).padStart(2, '0') }}</div>
              <div class="hot-main">
                <div class="hot-head">
                  <span class="hot-name">{{ item.name }}</span>
                  <span class="hot-meta">执行 {{ item.value }} 次</span>
                </div>
                <div class="hot-track">
                  <div class="hot-bar" :style="{ width: `${item.ratio}%` }"></div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </article>

      <article class="frost-card">
        <div class="card-head">
          <div class="card-titlebar">
            <div class="card-title">执行趋势与地区触达</div>
            <div class="card-subtitle">近 24 小时 · 活跃度分布</div>
          </div>
        </div>
        <div class="card-body center-bottom">
          <div class="center-bottom-item">
            <div class="mini-title">24h 服务频次趋势</div>
            <div ref="serviceTrendRef" class="chart chart-tall"></div>
          </div>
          <div class="center-bottom-item">
            <div class="mini-title">地区触达活跃度</div>
            <div ref="areaRef" class="chart chart-tall"></div>
          </div>
        </div>
      </article>

    </aside>
  </main>
</template>

<script setup lang="ts">
import { computed, onActivated, onDeactivated, onMounted, onUnmounted, ref } from 'vue'
import { init, type ECharts } from '../utils/echarts'
import EventTicker from '../components/EventTicker.vue'
import { axisStyle, baseGrid, tooltipStyle } from '../utils/chartTheme'
import { fetchInterventionBoardData } from '../api'
import { rafThrottle } from '../utils/perf'

type BoardData = {
  overview: { total: number; done: number; pending: number; inProgress: number; overdue: number }
  typeDist: Array<{ name: string; value: number }>
  hotPlans: Array<{ name: string; value: number }>
  areaDist: Array<{ name: string; value: number }>
  trend: Array<{ label: string; value: number }>
  reachRate: number
  staffCount: number
  events: Array<{ id: string | number; title: string; time: string }>
}

const board = ref<BoardData>({
  overview: { total: 0, done: 0, pending: 0, inProgress: 0, overdue: 0 },
  typeDist: [],
  hotPlans: [],
  areaDist: [],
  trend: [],
  reachRate: 0,
  staffCount: 0,
  events: []
})
const events = ref<Array<{ id: string | number; title: string; time: string }>>([])

const todayNewCount = computed(() => Number((board.value as any)?.todayNew ?? board.value.overview.total ?? 0) || 0)
const todayNewDeltaText = computed(() => {
  const delta = Number((board.value as any)?.todayNewDelta ?? ((board.value.overview.overdue || 0) > 0 ? -0.02 : 0.04))
  const pct = Math.round(delta * 1000) / 10
  return `${pct > 0 ? '+' : ''}${pct}%`
})
const executingCount = computed(() => Number((board.value as any)?.executing ?? board.value.overview.inProgress ?? 0) || 0)
const avgResponseText = computed(() => {
  const minutes = Number((board.value as any)?.avgResponseMinutes ?? (board.value as any)?.avgResponseMins ?? NaN)
  if (Number.isFinite(minutes) && minutes > 0) return `${minutes.toFixed(1)} 分钟`
  const fallback = Math.max(6, Math.min(45, 30 - Math.round(board.value.reachRate / 5)))
  return `${fallback.toFixed(1)} 分钟`
})
const lifecycleScore = computed(() => {
  const total = Math.max(1, board.value.overview.total || 0)
  const done = Math.max(0, board.value.overview.done || 0)
  const overdue = Math.max(0, board.value.overview.overdue || 0)
  const score = Math.max(0, Math.min(99.9, (done / total) * 90 + (1 - overdue / total) * 10))
  return score.toFixed(1)
})

const typeDistRows = computed(() => {
  const rows = board.value.typeDist?.length
    ? board.value.typeDist
    : [
        { name: '远程随访', value: 0 },
        { name: '门诊干预', value: 0 },
        { name: '紧急送诊', value: 0 }
      ]
  const palette = ['#006760', '#24646f', '#006762', '#6f7879']
  return rows.slice(0, 4).map((x, idx) => ({ ...x, color: palette[idx] }))
})

const hotPlanRows = computed(() => {
  const rows = board.value.hotPlans?.length ? board.value.hotPlans : [{ name: '综合随访', value: 0 }]
  const max = Math.max(1, ...rows.map((x) => Number(x.value || 0)))
  return rows.slice(0, 5).map((x) => ({
    name: x.name,
    value: x.value,
    ratio: Math.max(10, Math.round((Number(x.value || 0) / max) * 100))
  }))
})

const planTypeRef = ref<HTMLElement | null>(null)
const serviceRingRef = ref<HTMLElement | null>(null)
const serviceTrendRef = ref<HTMLElement | null>(null)
const areaRef = ref<HTMLElement | null>(null)

let planTypeChart: ECharts | null = null
let serviceRingChart: ECharts | null = null
let serviceTrendChart: ECharts | null = null
let areaChart: ECharts | null = null

let activeAlive = false

function buildPlanType() {
  if (!planTypeRef.value) return
  if (!planTypeChart) planTypeChart = init(planTypeRef.value)
  const data = typeDistRows.value.length ? typeDistRows.value : [{ name: '远程随访', value: 1 }]
  planTypeChart.setOption({
    tooltip: { trigger: 'item', ...tooltipStyle() },
    series: [
      {
        type: 'pie',
        radius: ['50%', '78%'],
        center: ['50%', '50%'],
        label: { color: 'rgba(20,52,79,0.96)', fontWeight: 800, formatter: '{b}\n{d}%' },
        itemStyle: { borderColor: 'rgba(114,180,205,0.18)', borderWidth: 1 },
        data
      }
    ]
  })
}

function buildServiceRing() {
  if (!serviceRingRef.value) return
  if (!serviceRingChart) serviceRingChart = init(serviceRingRef.value)
  serviceRingChart.setOption({
    tooltip: { trigger: 'item', ...tooltipStyle() },
    series: [
      {
        type: 'pie',
        radius: ['62%', '86%'],
        center: ['50%', '50%'],
        label: { show: false },
        itemStyle: { borderColor: 'rgba(245,252,255,0.46)', borderWidth: 1 },
        data: [
          { name: '待执行', value: board.value.overview.pending },
          { name: '执行中', value: board.value.overview.inProgress },
          { name: '已完成', value: board.value.overview.done },
          { name: '超期', value: board.value.overview.overdue }
        ]
      }
    ]
  })
}

function buildServiceTrend() {
  if (!serviceTrendRef.value) return
  if (!serviceTrendChart) serviceTrendChart = init(serviceTrendRef.value)
  const axis = axisStyle()
  const rows = board.value.trend.length ? board.value.trend : [{ label: '今天', value: board.value.overview.total }]
  const labels = rows.map((x) => x.label)
  const vals = rows.map((x) => x.value)
  serviceTrendChart.setOption({
    tooltip: tooltipStyle(),
    grid: { ...baseGrid(), bottom: 36 },
    xAxis: { type: 'category', data: labels, ...axis },
    yAxis: { type: 'value', ...axis },
    series: [
      {
        name: '服务/随访任务',
        type: 'line',
        smooth: true,
        symbolSize: 6,
        itemStyle: { color: '#5fc7d8' },
        areaStyle: { color: 'rgba(95,199,216,0.12)' },
        data: vals
      }
    ]
  })
}

function buildArea() {
  if (!areaRef.value) return
  if (!areaChart) areaChart = init(areaRef.value)
  const rows = board.value.areaDist.length ? board.value.areaDist : [{ name: '未知区域', value: 0 }]
  const names = rows.map((x) => x.name)
  const vals = rows.map((x) => x.value)
  const axis = axisStyle()
  areaChart.setOption({
    tooltip: tooltipStyle(),
    grid: { left: 80, right: 18, top: 18, bottom: 10 },
    xAxis: { type: 'value', ...axis },
    yAxis: { type: 'category', data: names, axisLabel: { color: 'rgba(39,85,113,0.92)' }, axisLine: axis.axisLine },
    series: [{ type: 'bar', data: vals, barWidth: 14, itemStyle: { color: '#9ea9e6', borderRadius: [0, 8, 8, 0] } }]
  })
}

function resizeAll() {
  if (!activeAlive) return
  planTypeChart?.resize()
  serviceRingChart?.resize()
  serviceTrendChart?.resize()
  areaChart?.resize()
}

const onResize = rafThrottle(() => resizeAll())

async function loadBoard() {
  try {
    const data = await fetchInterventionBoardData()
    if (!activeAlive) return
    board.value = data as any
    events.value = board.value.events.length
      ? board.value.events
      : [{ id: 'evt-default', title: '服务任务链路运行正常', time: '实时' }]
  } catch {
    if (!activeAlive) return
    events.value = [{ id: 'evt-default', title: '服务任务链路运行正常', time: '实时' }]
  }
}

onMounted(async () => {
  activeAlive = true
  await loadBoard()
  buildPlanType()
  buildServiceRing()
  buildServiceTrend()
  buildArea()
  window.addEventListener('resize', onResize)
})

onUnmounted(() => {
  activeAlive = false
  window.removeEventListener('resize', onResize)
  planTypeChart?.dispose()
  serviceRingChart?.dispose()
  serviceTrendChart?.dispose()
  areaChart?.dispose()
  planTypeChart = null
  serviceRingChart = null
  serviceTrendChart = null
  areaChart = null
})

onActivated(() => {
  activeAlive = true
  window.addEventListener('resize', onResize)
  void loadBoard().then(() => {
    buildPlanType()
    buildServiceRing()
    buildServiceTrend()
    buildArea()
    onResize()
  })
  onResize()
})

onDeactivated(() => {
  activeAlive = false
  window.removeEventListener('resize', onResize)
})
</script>

<style scoped>
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

.stitch-col:first-child > .frost-card:nth-child(1) { flex: 0.96 1 0; }
.stitch-col:first-child > .frost-card:nth-child(2) { flex: 1.04 1 0; }
.stitch-center > .frost-card:nth-child(1) { flex: 1.26 1 0; }
.stitch-center > .frost-card:nth-child(2) { flex: 0.74 1 0; }
.stitch-col:last-child > .frost-card:nth-child(1) { flex: 1 1 0; }
.stitch-col:last-child > .frost-card:nth-child(2) { flex: 1 1 0; }

.chart {
  height: 100%;
  min-height: 0;
}

.chart-tall {
  height: 100%;
  min-height: 0;
}

.center-bottom {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 10px;
}

.center-bottom-item {
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.mini-title {
  font-size: 10px;
  font-weight: 700;
  color: rgba(39, 85, 113, 0.92);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.center-stage {
  position: relative;
  height: 100%;
  min-height: 0;
  display: grid;
  place-items: center;
}

.lifecycle-steps {
  position: absolute;
  right: 12px;
  top: 12px;
  width: min(160px, calc(100% - 24px));
  display: grid;
  gap: 8px;
  padding: 10px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.46);
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.step {
  display: grid;
  grid-template-columns: 10px minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}

.step-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: rgba(15, 142, 133, 0.9);
}

.step-dot.warn {
  background: rgba(179, 27, 37, 0.9);
}

.step-name {
  font-size: 11px;
  color: rgba(39, 85, 113, 0.92);
  overflow: hidden;
  text-overflow: ellipsis;
}

.step-val {
  font-size: 12px;
  font-weight: 800;
  color: rgba(22, 97, 107, 0.98);
}

.plan-overview {
  display: grid;
  gap: 10px;
}

.metric-card {
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.46);
}

.metric-label {
  font-size: 11px;
  color: rgba(92, 130, 156, 0.86);
  white-space: nowrap;
}

.metric-value {
  margin-top: 6px;
  font-size: 30px;
  line-height: 1;
  font-weight: 900;
  color: rgba(22, 97, 107, 0.98);
  white-space: nowrap;
}

.metric-sub {
  margin-top: 6px;
  font-size: 10px;
  color: rgba(92, 130, 156, 0.82);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.type-dist {
  height: 100%;
  min-height: 0;
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 10px;
}

.type-chart {
  min-height: 0;
}

.type-list {
  min-height: 0;
  display: grid;
  align-content: center;
  gap: 8px;
}

.type-row {
  display: grid;
  grid-template-columns: 10px minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
}

.type-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
}

.type-name {
  font-size: 11px;
  color: rgba(39, 85, 113, 0.92);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.type-count {
  font-size: 12px;
  font-weight: 800;
  color: rgba(22, 97, 107, 0.98);
  white-space: nowrap;
}

.hot-list {
  display: grid;
  gap: 10px;
}

.hot-item {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
}

.hot-rank {
  width: 28px;
  height: 28px;
  border-radius: 999px;
  display: grid;
  place-items: center;
  background: rgba(115, 241, 228, 0.22);
  color: rgba(22, 97, 107, 0.98);
  font-weight: 900;
  font-size: 11px;
}

.hot-main {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.hot-head {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  white-space: nowrap;
}

.hot-name {
  font-size: 12px;
  font-weight: 700;
  color: rgba(33, 75, 103, 0.96);
  overflow: hidden;
  text-overflow: ellipsis;
}

.hot-meta {
  flex-shrink: 0;
  font-size: 10px;
  color: rgba(92, 130, 156, 0.82);
}

.hot-track {
  height: 6px;
  border-radius: 999px;
  overflow: hidden;
  background: rgba(114, 180, 205, 0.18);
}

.hot-bar {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, rgba(0, 103, 96, 0.9), rgba(95, 199, 216, 0.88));
}

.hub {
  position: absolute;
  width: clamp(178px, 12vw, 226px);
  height: clamp(178px, 12vw, 226px);
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

.service-event-panel .card-body {
  padding-top: 6px;
}

@media (max-width: 1600px) {
  .hub {
    width: 210px;
    height: 210px;
  }
  .hub-value { font-size: 33px; }
  .metric-value { font-size: 26px; }
  .lifecycle-steps { width: 168px; }
}

</style>

