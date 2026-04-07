<template>
  <main class="stitch-grid">
    <aside class="stitch-col">
      <section class="panel">
        <div class="panel-corners"></div>
        <div class="panel-header">
          <div class="panel-titlebar">
            <div class="panel-title">告警等级分布</div>
            <div class="panel-subtitle">健康 + 设备（近 30 天）</div>
          </div>
        </div>
        <div class="panel-body">
          <div ref="levelRef" class="chart"></div>
        </div>
      </section>
      <section class="panel">
        <div class="panel-corners"></div>
        <div class="panel-header">
          <div class="panel-titlebar">
            <div class="panel-title">健康告警趋势</div>
            <div class="panel-subtitle">近 7 天</div>
          </div>
        </div>
        <div class="panel-body">
          <div ref="patientTrendRef" class="chart"></div>
        </div>
      </section>
      <section class="panel">
        <div class="panel-corners"></div>
        <div class="panel-header">
          <div class="panel-titlebar">
            <div class="panel-title">设备告警趋势</div>
            <div class="panel-subtitle">近 7 天</div>
          </div>
        </div>
        <div class="panel-body">
          <div ref="hardwareTrendRef" class="chart"></div>
        </div>
      </section>
    </aside>

    <section class="stitch-center">
      <section class="panel">
        <div class="panel-corners"></div>
        <div class="panel-header">
          <div class="panel-titlebar">
            <div class="panel-title">告警联动中枢</div>
            <div class="panel-subtitle">告警 · 处置 · 闭环</div>
          </div>
        </div>
        <div class="panel-body">
          <div class="flow-stage">
            <div class="node n1">
              <div class="node-title">告警源头</div>
              <div class="node-value">{{ totalAlerts }}</div>
            </div>
            <div class="arrow"></div>
            <div class="node n2">
              <div class="node-title">待处理</div>
              <div class="node-value danger">{{ unhandled }}</div>
            </div>
            <div class="arrow"></div>
            <div class="node n3">
              <div class="node-title">分派</div>
              <div class="node-value muted">—</div>
            </div>
            <div class="arrow"></div>
            <div class="node n4">
              <div class="node-title">随访中</div>
              <div class="node-value muted">—</div>
            </div>
            <div class="arrow"></div>
            <div class="node n5">
              <div class="node-title">已闭环</div>
              <div class="node-value success">{{ closed }}</div>
            </div>
          </div>
        </div>
      </section>

      <section class="panel">
        <div class="panel-corners"></div>
        <div class="panel-header">
          <div class="panel-titlebar">
            <div class="panel-title">实时处置流</div>
            <div class="panel-subtitle">动态事件</div>
          </div>
        </div>
        <div class="panel-body">
          <EventTicker :items="events" />
        </div>
      </section>
    </section>

    <aside class="stitch-col">
      <section class="panel">
        <div class="panel-corners"></div>
        <div class="panel-header">
          <div class="panel-titlebar">
            <div class="panel-title">处置概览</div>
            <div class="panel-subtitle">任务与闭环</div>
          </div>
        </div>
        <div class="panel-body">
          <div class="stat-grid">
            <StatCard label="未处理告警" :value="unhandled" tone="danger" />
            <StatCard label="处理中告警" :value="processing" tone="warning" />
            <StatCard label="待随访任务" :value="pendingFollow" tone="cyan" />
            <StatCard label="闭环率" :value="closeRateText" tone="gold" />
          </div>
        </div>
      </section>

      <section class="panel">
        <div class="panel-corners"></div>
        <div class="panel-header">
          <div class="panel-titlebar">
            <div class="panel-title">责任医生告警负载</div>
            <div class="panel-subtitle">Top5</div>
          </div>
        </div>
        <div class="panel-body">
          <div ref="doctorRef" class="chart"></div>
        </div>
      </section>

      <section class="panel">
        <div class="panel-corners"></div>
        <div class="panel-header">
          <div class="panel-titlebar">
            <div class="panel-title">响应与超时</div>
            <div class="panel-subtitle">闭环效率</div>
          </div>
        </div>
        <div class="panel-body">
          <div class="kpi-mini">
            <div class="kpi">平均响应：<b>—</b></div>
            <div class="kpi">超时率：<b>—</b></div>
            <div class="kpi">复发告警：<b>—</b></div>
          </div>
        </div>
      </section>
    </aside>
  </main>
</template>

<script setup lang="ts">
import { computed, onActivated, onDeactivated, onMounted, onUnmounted, ref } from 'vue'
import { init, type ECharts } from '../utils/echarts'
import StatCard from '../components/StatCard.vue'
import EventTicker from '../components/EventTicker.vue'
import { axisStyle, baseGrid, legendStyle, tooltipStyle } from '../utils/chartTheme'
import { fetchAlerts, fetchHardwareAlerts, fetchPatientSummary } from '../api'
import { rafThrottle } from '../utils/perf'

const totalAlerts = ref(0)
const unhandled = ref(0)
const closed = ref(0)
const processing = ref(0)
const pendingFollow = ref(0)

const closeRateText = computed(() => {
  if (!totalAlerts.value) return '—'
  return `${((closed.value / totalAlerts.value) * 100).toFixed(1)}%`
})

const events = ref<Array<{ id: string; title: string; time: string }>>([])

const levelRef = ref<HTMLElement | null>(null)
const patientTrendRef = ref<HTMLElement | null>(null)
const hardwareTrendRef = ref<HTMLElement | null>(null)
const doctorRef = ref<HTMLElement | null>(null)

let levelChart: ECharts | null = null
let patientTrendChart: ECharts | null = null
let hardwareTrendChart: ECharts | null = null
let doctorChart: ECharts | null = null

let activeAlive = false

function buildLevel(healthCount: number, deviceCount: number) {
  if (!levelRef.value) return
  if (!levelChart) levelChart = init(levelRef.value)
  levelChart.setOption({
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
          color: (p: any) =>
            (p?.data?.itemStyle?.color as string) || (p?.color as string) || 'rgba(20,52,79,0.96)',
          fontSize: 12,
          fontWeight: 800,
          lineHeight: 16,
          width: 92,
          overflow: 'break'
        },
        emphasis: {
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
          { name: '健康告警', value: healthCount, itemStyle: { color: '#78c4a0' } },
          { name: '设备告警', value: deviceCount, itemStyle: { color: '#efc56f' } }
        ]
      }
    ]
  })
}

function buildTrend(
  refEl: HTMLElement | null,
  chart: ECharts | null,
  color: string,
  labels: string[],
  vals: number[]
) {
  if (!refEl) return chart
  const c = chart || init(refEl)
  const axis = axisStyle()
  c.setOption({
    tooltip: tooltipStyle(),
    grid: baseGrid(),
    xAxis: { type: 'category', data: labels, ...axis },
    yAxis: { type: 'value', ...axis },
    series: [
      {
        type: 'line',
        smooth: true,
        symbolSize: 6,
        itemStyle: { color },
        areaStyle: { color: color.replace(')', ',0.12)').replace('rgb', 'rgba') },
        data: vals
      }
    ]
  })
  return c
}

function buildDoctor(rows: any[]) {
  if (!doctorRef.value) return
  if (!doctorChart) doctorChart = init(doctorRef.value)
  const by: Record<string, number> = {}
  rows.forEach((r: any) => {
    const d = (r.responsibleDoctor || r.doctor || '未分配').toString().trim() || '未分配'
    const c = Number(r.activeAlertCount || 0)
    by[d] = (by[d] || 0) + (Number.isFinite(c) ? c : 0)
  })
  const pairs = Object.entries(by).sort((a, b) => b[1] - a[1]).slice(0, 5)
  const names = pairs.map((p) => p[0])
  const vals = pairs.map((p) => p[1])
  const axis = axisStyle()
  doctorChart.setOption({
    tooltip: tooltipStyle(),
    grid: { left: 90, right: 18, top: 18, bottom: 10 },
    xAxis: { type: 'value', ...axis },
    yAxis: {
      type: 'category',
      data: names,
      axisLabel: { color: 'rgba(39,85,113,0.92)' },
      axisLine: axis.axisLine
    },
    series: [
      {
        type: 'bar',
        data: vals,
        barWidth: 14,
        itemStyle: { color: '#7fd6e3', borderRadius: [0, 8, 8, 0] }
      }
    ]
  })
}

function resizeAll() {
  if (!activeAlive) return
  levelChart?.resize()
  patientTrendChart?.resize()
  hardwareTrendChart?.resize()
  doctorChart?.resize()
}

const onResize = rafThrottle(() => resizeAll())

function parseTs(v: any) {
  if (!v) return NaN
  const s = String(v).trim()
  if (!s) return NaN
  const d = new Date(s.includes('T') ? s : s.replace(' ', 'T'))
  return d.getTime()
}

function lastNDaysLabels(n: number) {
  const out: string[] = []
  for (let i = n - 1; i >= 0; i--) {
    const d = new Date(Date.now() - i * 86400000)
    out.push(`${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`)
  }
  return out
}

function countByDay(rows: any[], n: number, getTime: (r: any) => number) {
  const start = new Date()
  start.setHours(0, 0, 0, 0)
  const base = start.getTime() - (n - 1) * 86400000
  const buckets = new Array(n).fill(0)
  rows.forEach((r) => {
    const ts = getTime(r)
    if (!Number.isFinite(ts)) return
    const idx = Math.floor((ts - base) / 86400000)
    if (idx >= 0 && idx < n) buckets[idx]++
  })
  return buckets
}

onMounted(async () => {
  activeAlive = true
  const [a, h, ps] = await Promise.all([fetchAlerts(30), fetchHardwareAlerts(30), fetchPatientSummary(300)])
  const aRows = (a?.rows || []) as any[]
  const hRows = (h?.rows || []) as any[]
  const all = [...aRows, ...hRows]
  totalAlerts.value = all.length
  unhandled.value = all.filter((r: any) => String(r.statusText || r.status || '').includes('未处理') || String(r.status || '').toUpperCase() === 'NEW').length
  closed.value = all.filter((r: any) => String(r.statusText || r.status || '').includes('已关闭') || String(r.status || '').toUpperCase() === 'CLOSED').length

  // 如果后端给了处理中状态则统计，否则为 0（不造假）
  processing.value = all.filter((r: any) => String(r.statusText || r.status || '').includes('处理中') || String(r.status || '').toUpperCase() === 'PROCESSING').length

  events.value = all.slice(0, 8).map((r: any, idx: number) => ({
    id: String(r.id || idx),
    title: `告警 · ${r.patientName || '患者'} · ${r.summary || r.alertType || ''}`,
    time: (r.alertTime || r.firstTime || '').toString().replace('T', ' ')
  }))

  buildLevel(aRows.length, hRows.length)
  const labels = lastNDaysLabels(7)
  const aByDay = countByDay(aRows, 7, (r) => parseTs(r.alertTime || r.firstTime || r.createdAt))
  const hByDay = countByDay(hRows, 7, (r) => parseTs(r.alertTime || r.firstTime || r.createdAt))
  patientTrendChart = buildTrend(patientTrendRef.value, patientTrendChart, 'rgb(95,199,216)', labels, aByDay)
  hardwareTrendChart = buildTrend(hardwareTrendRef.value, hardwareTrendChart, 'rgb(158,169,230)', labels, hByDay)
  // 性能优化：2000 属于重请求；仅用于“医生负载 Top5”摘要时先降载。
  // TODO(后端): 建议新增聚合接口：按 responsibleDoctor 聚合 activeAlertCount 与随访任务量，避免大屏拉全量患者列表。
  const pRows = (ps?.rows || []) as any[]
  buildDoctor(pRows)

  window.addEventListener('resize', onResize)
})

onUnmounted(() => {
  activeAlive = false
  window.removeEventListener('resize', onResize)
  levelChart?.dispose()
  patientTrendChart?.dispose()
  hardwareTrendChart?.dispose()
  doctorChart?.dispose()
  levelChart = null
  patientTrendChart = null
  hardwareTrendChart = null
  doctorChart = null
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
  height: 210px;
}

.flow-stage {
  display: grid;
  grid-template-columns: 1fr 36px 1fr 36px 1fr 36px 1fr 36px 1fr;
  align-items: center;
  gap: 6px;
  padding: 12px 4px 6px;
}

.node {
  border-radius: var(--r-md);
  border: 1px solid rgba(114, 180, 205, 0.34);
  background: rgba(255, 255, 255, 0.62);
  padding: 10px 10px;
  text-align: center;
}

.node-title {
  font-size: 12px;
  color: var(--t-2);
}

.node-value {
  margin-top: 6px;
  font-size: 22px;
  font-weight: 900;
  color: var(--c-cyan);
}

.node-value.danger {
  color: var(--c-danger);
}

.node-value.success {
  color: var(--c-success);
}

.node-value.muted {
  color: var(--t-3);
}

.arrow {
  height: 2px;
  background: linear-gradient(90deg, transparent, rgba(95, 199, 216, 0.42), transparent);
  box-shadow: var(--glow);
}

.stitch-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 2.35fr) minmax(0, 1fr);
  gap: 12px;
  height: 100%;
  min-height: 0;
}

.stitch-col,
.stitch-center {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
}

.stitch-col .panel,
.stitch-center .panel {
  flex: 1;
  min-height: 0;
}

.kpi-mini {
  display: grid;
  gap: 10px;
}

.kpi {
  padding: 10px 12px;
  border-radius: var(--r-md);
  border: 1px solid rgba(114, 180, 205, 0.22);
  background: rgba(255, 255, 255, 0.58);
  color: rgba(39, 85, 113, 0.92);
  font-size: 12px;
}

.kpi b {
  color: rgba(32, 82, 110, 0.94);
  font-weight: 800;
}
</style>

