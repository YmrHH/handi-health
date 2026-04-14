<template>
  <main class="stitch-grid screen-page screen-grid">
    <aside class="stitch-col screen-col">
      <article class="frost-card">
        <div class="card-head">
          <div class="card-titlebar">
            <div class="card-title">告警等级分布</div>
            <div class="card-subtitle">健康 + 设备（近 30 天）</div>
          </div>
        </div>
        <div class="card-body">
          <div ref="levelRef" class="chart"></div>
        </div>
      </article>
      <article class="frost-card">
        <div class="card-head">
          <div class="card-titlebar">
            <div class="card-title">健康告警趋势</div>
            <div class="card-subtitle">近 7 天</div>
          </div>
        </div>
        <div class="card-body">
          <div ref="patientTrendRef" class="chart"></div>
        </div>
      </article>
      <article class="frost-card">
        <div class="card-head">
          <div class="card-titlebar">
            <div class="card-title">设备告警分布</div>
            <div class="card-subtitle">按设备类型告警占比</div>
          </div>
        </div>
        <div class="card-body">
          <div class="device-distribution-list">
            <div v-for="item in deviceDistributionRows" :key="item.name" class="device-row">
              <div class="device-row-head">
                <span class="device-name">{{ item.name }}</span>
                <span class="device-count">{{ item.count }} 次</span>
              </div>
              <div class="device-track">
                <div class="device-bar" :style="{ width: `${item.ratio}%` }"></div>
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
            <div class="card-title">实时告警联动中枢</div>
            <div class="card-subtitle">告警分发 · 处置协同 · 闭环追踪</div>
          </div>
        </div>
        <div class="card-body">
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
              <div class="node-value">{{ assignedFlow }}</div>
            </div>
            <div class="arrow"></div>
            <div class="node n4">
              <div class="node-title">随访中</div>
              <div class="node-value">{{ inFollowFlow }}</div>
            </div>
            <div class="arrow"></div>
            <div class="node n5">
              <div class="node-title">已闭环</div>
              <div class="node-value success">{{ closed }}</div>
            </div>
          </div>
        </div>
      </article>

    </section>

    <aside class="stitch-col screen-col">
      <article class="frost-card">
        <div class="card-head">
          <div class="card-titlebar">
            <div class="card-title">责任医生告警负载排行</div>
            <div class="card-subtitle">按当前告警压力排序</div>
          </div>
        </div>
        <div class="card-body">
          <div class="doctor-rank-list">
            <div v-for="item in doctorRankRows" :key="item.name" class="doctor-row">
              <div class="doctor-row-head">
                <span class="doctor-name">{{ item.name }}</span>
                <span class="doctor-count">{{ item.count }}</span>
              </div>
              <div class="doctor-track">
                <div class="doctor-bar" :style="{ width: `${item.ratio}%` }"></div>
              </div>
            </div>
          </div>
          <div class="doctor-meta">
            <span>关闭率 {{ closeRateText }}</span>
            <span>超时率 {{ overtimeRateText }}</span>
            <span>复发告警 {{ recurrenceCountText }}</span>
          </div>
        </div>
      </article>

      <article class="frost-card">
        <div class="card-head">
          <div class="card-titlebar">
            <div class="card-title">实时处置流</div>
            <div class="card-subtitle">动态事件追踪</div>
          </div>
        </div>
        <div class="card-body">
          <EventTicker :items="events" />
        </div>
      </article>

      <article class="frost-card">
        <div class="card-head">
          <div class="card-titlebar">
            <div class="card-title">待处理任务</div>
            <div class="card-subtitle">优先处置清单</div>
          </div>
        </div>
        <div class="card-body">
          <div class="task-list">
            <article v-for="(item, idx) in taskItems" :key="`${item.id}-${idx}`" class="task-item" :class="`is-${item.tone}`">
              <div class="task-main">
                <div class="task-title">{{ item.title }}</div>
                <div class="task-time">{{ item.time }}</div>
              </div>
              <div class="task-desc">{{ item.desc }}</div>
            </article>
          </div>
          <div class="dispatch-entry">
            <button type="button" class="dispatch-link">进入调度台</button>
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
import { fetchAlerts, fetchHardwareAlerts, fetchPatientSummary } from '../api'
import { rafThrottle } from '../utils/perf'

const totalAlerts = ref(0)
const unhandled = ref(0)
const closed = ref(0)
const assignedFlow = ref(0)
const inFollowFlow = ref(0)
const overtimeRateText = ref('0.0%')
const recurrenceCountText = ref('0')
const doctorRankRows = ref<Array<{ name: string; count: number; ratio: number }>>([])
const deviceDistributionRows = ref<Array<{ name: string; count: number; ratio: number }>>([])

const closeRateText = computed(() => {
  if (!totalAlerts.value) return '0.0%'
  return `${((closed.value / totalAlerts.value) * 100).toFixed(1)}%`
})

const events = ref<Array<{ id: string; title: string; time: string }>>([])
const taskItems = computed(() => {
  const rows = events.value.slice(0, 3)
  if (!rows.length) {
    return [{ id: 'task-default', title: '当前暂无高优先任务', time: '实时', desc: '系统持续监控中', tone: 'primary' }]
  }
  return rows.map((item, idx) => ({
    id: item.id,
    title: idx === 0 ? `高危：${item.title}` : `提醒：${item.title}`,
    time: item.time,
    desc: idx === 0 ? '建议优先进行闭环处置' : '请尽快安排后续跟进',
    tone: idx === 0 ? 'danger' : idx === 1 ? 'warning' : 'primary'
  }))
})

const levelRef = ref<HTMLElement | null>(null)
const patientTrendRef = ref<HTMLElement | null>(null)

let levelChart: ECharts | null = null
let patientTrendChart: ECharts | null = null

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

function computeDoctorRank(rows: any[]) {
  const by: Record<string, number> = {}
  rows.forEach((r: any) => {
    const d = (r.responsibleDoctor || r.doctor || '未分配').toString().trim() || '未分配'
    const c = Number(r.activeAlertCount || 0)
    by[d] = (by[d] || 0) + (Number.isFinite(c) ? c : 0)
  })
  const pairs = Object.entries(by).sort((a, b) => b[1] - a[1]).slice(0, 4)
  const max = Math.max(1, ...pairs.map((x) => Number(x[1] || 0)))
  doctorRankRows.value = (pairs.length ? pairs : [['未分配', 0]]).map(([name, count]) => ({
    name: String(name),
    count: Number(count || 0),
    ratio: Math.max(8, Math.round((Number(count || 0) / max) * 100))
  }))
}

function computeDeviceDistribution(rows: any[]) {
  const by: Record<string, number> = {}
  rows.forEach((r: any) => {
    const name = String(r.deviceType || r.deviceName || r.hardwareType || r.category || '其他设备').trim() || '其他设备'
    by[name] = (by[name] || 0) + 1
  })
  const pairs = Object.entries(by).sort((a, b) => b[1] - a[1]).slice(0, 4)
  const max = Math.max(1, ...pairs.map((x) => Number(x[1] || 0)))
  deviceDistributionRows.value = (pairs.length
    ? pairs
    : [
        ['智能血压计', 0],
        ['动态血糖仪', 0]
      ]
  ).map(([name, count]) => ({
    name: String(name),
    count: Number(count || 0),
    ratio: Math.max(10, Math.round((Number(count || 0) / max) * 100))
  }))
}

function resizeAll() {
  if (!activeAlive) return
  levelChart?.resize()
  patientTrendChart?.resize()
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
  await loadBoard()
  window.addEventListener('resize', onResize)
})

async function loadBoard() {
  const [a, h, ps] = await Promise.all([fetchAlerts(30), fetchHardwareAlerts(30), fetchPatientSummary(200)])
  const aRows = pickRows(a) as any[]
  const hRows = pickRows(h) as any[]
  const all = [...aRows, ...hRows]
  totalAlerts.value = all.length
  unhandled.value = all.filter((r: any) => String(r.statusText || r.status || '').includes('未处理') || String(r.status || '').toUpperCase() === 'NEW').length
  closed.value = all.filter((r: any) => String(r.statusText || r.status || '').includes('已关闭') || String(r.status || '').toUpperCase() === 'CLOSED').length

  assignedFlow.value = all.filter((r: any) => String(r.statusText || r.status || '').includes('分派') || String(r.status || '').toUpperCase() === 'ASSIGNED').length

  const overtimeCount = all.filter((r: any) => {
    const txt = String(r.statusText || r.status || '').toLowerCase()
    return txt.includes('超时') || r.isOvertime === true || r.overtime === true
  }).length
  overtimeRateText.value = totalAlerts.value ? `${((overtimeCount / totalAlerts.value) * 100).toFixed(1)}%` : '0.0%'

  const patientMap = new Map<string, number>()
  all.forEach((r: any) => {
    const key = String(r.patientId || r.patientName || r.name || '').trim()
    if (!key) return
    patientMap.set(key, (patientMap.get(key) || 0) + 1)
  })
  const recurrence = Array.from(patientMap.values()).reduce((sum, n) => sum + (n > 1 ? n - 1 : 0), 0)
  recurrenceCountText.value = String(recurrence)

  events.value = all.slice(0, 8).map((r: any, idx: number) => ({
    id: String(r.id || idx),
    title: `告警 · ${r.patientName || '患者'} · ${r.summary || r.alertType || ''}`,
    time: (r.alertTime || r.firstTime || '').toString().replace('T', ' ')
  }))
  if (!events.value.length) {
    events.value = [{ id: 'evt-default', title: '联动链路运行稳定', time: '实时' }]
  }

  buildLevel(aRows.length, hRows.length)
  computeDeviceDistribution(hRows)
  const labels = lastNDaysLabels(7)
  const aByDay = countByDay(aRows, 7, (r) => parseTs(r.alertTime || r.firstTime || r.createdAt))
  patientTrendChart = buildTrend(patientTrendRef.value, patientTrendChart, 'rgb(95,199,216)', labels, aByDay)
  const pRows = pickRows(ps) as any[]
  inFollowFlow.value = pRows.filter((r: any) => {
    const txt = String(r.followStatus || r.taskStatus || r.status || '')
    return txt.includes('随访中') || txt.includes('执行中') || txt.toUpperCase() === 'PROCESSING'
  }).length
  computeDoctorRank(pRows)
}

onUnmounted(() => {
  activeAlive = false
  window.removeEventListener('resize', onResize)
  levelChart?.dispose()
  patientTrendChart?.dispose()
  levelChart = null
  patientTrendChart = null
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

.flow-stage {
  display: grid;
  grid-template-columns: 1fr 36px 1fr 36px 1fr 36px 1fr 36px 1fr;
  align-items: center;
  gap: 6px;
  padding: 10px 2px 4px;
}

.node {
  border-radius: var(--r-md);
  border: 1px solid rgba(255, 255, 255, 0.05);
  background: rgba(255, 255, 255, 0.56);
  padding: 10px 10px;
  text-align: center;
}

.node-title {
  font-size: 11px;
  color: var(--t-2);
}

.node-value {
  margin-top: 6px;
  font-size: 20px;
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
  background: linear-gradient(90deg, transparent, rgba(95, 199, 216, 0.26), transparent);
  box-shadow: 0 0 8px rgba(95, 199, 216, 0.08);
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

.stitch-col:first-child > .frost-card:nth-child(1) { flex: 1.02 1 0; }
.stitch-col:first-child > .frost-card:nth-child(2) { flex: 0.98 1 0; }
.stitch-col:first-child > .frost-card:nth-child(3) { flex: 0.96 1 0; }
.stitch-center > .frost-card:nth-child(1) { flex: 1 1 0; }
.stitch-col:last-child > .frost-card:nth-child(1) { flex: 0.94 1 0; }
.stitch-col:last-child > .frost-card:nth-child(2) { flex: 1.08 1 0; }
.stitch-col:last-child > .frost-card:nth-child(3) { flex: 0.98 1 0; }

.device-distribution-list {
  display: grid;
  gap: 8px;
}

.device-row {
  display: grid;
  gap: 5px;
}

.device-row-head {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  align-items: center;
}

.device-name {
  min-width: 0;
  font-size: 11px;
  color: rgba(39, 85, 113, 0.92);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.device-count {
  flex-shrink: 0;
  font-size: 11px;
  color: rgba(92, 130, 156, 0.86);
}

.device-track,
.doctor-track {
  height: 6px;
  width: 100%;
  border-radius: 999px;
  background: rgba(114, 180, 205, 0.2);
  overflow: hidden;
}

.device-bar,
.doctor-bar {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, rgba(95, 199, 216, 0.92), rgba(140, 188, 227, 0.88));
}

.doctor-rank-list {
  display: grid;
  gap: 8px;
}

.doctor-row {
  display: grid;
  gap: 5px;
}

.doctor-row-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.doctor-name {
  min-width: 0;
  font-size: 11px;
  color: rgba(39, 85, 113, 0.92);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.doctor-count {
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 700;
  color: rgba(22, 97, 107, 0.96);
}

.doctor-meta {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 10px;
  color: rgba(92, 130, 156, 0.84);
}

.task-list {
  display: grid;
  gap: 8px;
}

.task-item {
  border-radius: var(--r-md);
  padding: 8px 10px;
  border-left: 3px solid rgba(15, 142, 133, 0.9);
  background: rgba(255, 255, 255, 0.52);
}

.task-item.is-danger { border-left-color: #cf3948; }
.task-item.is-warning { border-left-color: #d7922f; }

.task-main {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.task-title {
  min-width: 0;
  font-size: 11px;
  font-weight: 700;
  color: rgba(33, 75, 103, 0.96);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.task-time {
  flex-shrink: 0;
  font-size: 10px;
  color: rgba(92, 130, 156, 0.82);
}

.task-desc {
  margin-top: 4px;
  font-size: 10px;
  color: rgba(92, 130, 156, 0.88);
}

.dispatch-entry {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
}

.dispatch-link {
  height: 28px;
  border: none;
  background: transparent;
  color: rgba(39, 85, 113, 0.88);
  font-size: 11px;
  font-weight: 700;
  cursor: pointer;
  padding: 0 4px;
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
  .node-value { font-size: 18px; }
  .eff-value { font-size: 18px; }
}
</style>

