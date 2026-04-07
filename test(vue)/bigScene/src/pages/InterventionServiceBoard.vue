<template>
  <main class="stitch-grid">
    <aside class="stitch-col">
      <section class="panel">
        <div class="panel-corners"></div>
        <div class="panel-header">
          <div class="panel-titlebar">
            <div class="panel-title">服务任务总览</div>
            <div class="panel-subtitle">今日执行与积压</div>
          </div>
        </div>
        <div class="panel-body">
          <div class="stat-grid">
            <StatCard label="今日服务任务" :value="demo.overview.followToday.total" tone="cyan" />
            <StatCard label="已完成" :value="demo.overview.followToday.done" tone="success" />
            <StatCard label="待执行" :value="demo.overview.followToday.pending" tone="warning" />
            <StatCard label="超期" :value="demo.overview.followToday.overdue" tone="danger" />
          </div>
        </div>
      </section>

      <section class="panel">
        <div class="panel-corners"></div>
        <div class="panel-header">
          <div class="panel-titlebar">
            <div class="panel-title">计划类型占比</div>
            <div class="panel-subtitle">任务结构</div>
          </div>
        </div>
        <div class="panel-body">
          <div ref="planTypeRef" class="chart"></div>
        </div>
      </section>

      <section class="panel">
        <div class="panel-corners"></div>
        <div class="panel-header">
          <div class="panel-titlebar">
            <div class="panel-title">热门方案 TOP</div>
            <div class="panel-subtitle">近期执行热度</div>
          </div>
        </div>
        <div class="panel-body">
          <div ref="hotPlanRef" class="chart"></div>
        </div>
      </section>
    </aside>

    <section class="stitch-center">
      <section class="panel">
        <div class="panel-corners"></div>
        <div class="panel-header">
          <div class="panel-titlebar">
            <div class="panel-title">服务执行路径盘</div>
            <div class="panel-subtitle">触达 · 执行 · 达标</div>
          </div>
        </div>
        <div class="panel-body">
          <div class="center-stage">
            <div class="hub glow-breath">
              <div class="hub-title">服务执行</div>
              <div class="hub-value">{{ demo.overview.followToday.done }}</div>
              <div class="hub-sub">今日完成 · 待办 {{ demo.overview.followToday.pending }}</div>
            </div>
            <div ref="serviceRingRef" class="hub-ring"></div>
          </div>
        </div>
      </section>

      <section class="panel">
        <div class="panel-corners"></div>
        <div class="panel-header">
          <div class="panel-titlebar">
            <div class="panel-title">时间序列趋势</div>
            <div class="panel-subtitle">近时段执行变化</div>
          </div>
        </div>
        <div class="panel-body">
          <div ref="serviceTrendRef" class="chart chart-tall"></div>
        </div>
      </section>
    </section>

    <aside class="stitch-col">
      <section class="panel">
        <div class="panel-corners"></div>
        <div class="panel-header">
          <div class="panel-titlebar">
            <div class="panel-title">上门服务触达</div>
            <div class="panel-subtitle">触达与完成</div>
          </div>
        </div>
        <div class="panel-body">
          <div ref="reachGaugeRef" class="chart"></div>
        </div>
      </section>

      <section class="panel">
        <div class="panel-corners"></div>
        <div class="panel-header">
          <div class="panel-titlebar">
            <div class="panel-title">地区 / 人员分布</div>
            <div class="panel-subtitle">触达活跃度</div>
          </div>
        </div>
        <div class="panel-body">
          <div ref="areaRef" class="chart"></div>
        </div>
      </section>

      <section class="panel">
        <div class="panel-corners"></div>
        <div class="panel-header">
          <div class="panel-titlebar">
            <div class="panel-title">最新服务动态</div>
            <div class="panel-subtitle">事件流</div>
          </div>
        </div>
        <div class="panel-body">
          <EventTicker :items="events" />
        </div>
      </section>
    </aside>
  </main>
</template>

<script setup lang="ts">
import { onActivated, onDeactivated, onMounted, onUnmounted, ref } from 'vue'
import { init, type ECharts } from '../utils/echarts'
import EventTicker from '../components/EventTicker.vue'
import StatCard from '../components/StatCard.vue'
import { axisStyle, baseGrid, tooltipStyle } from '../utils/chartTheme'
import { getBigscreenDemoData } from '../mock/bigscreenData'
import { rafThrottle } from '../utils/perf'

const demo = getBigscreenDemoData()
const events = ref(demo.lists.interventions.map((x) => ({ id: x.id, title: `${x.patient} · ${x.plan} · ${x.status}`, time: x.time })))

const planTypeRef = ref<HTMLElement | null>(null)
const hotPlanRef = ref<HTMLElement | null>(null)
const serviceRingRef = ref<HTMLElement | null>(null)
const serviceTrendRef = ref<HTMLElement | null>(null)
const reachGaugeRef = ref<HTMLElement | null>(null)
const areaRef = ref<HTMLElement | null>(null)

let planTypeChart: ECharts | null = null
let hotPlanChart: ECharts | null = null
let serviceRingChart: ECharts | null = null
let serviceTrendChart: ECharts | null = null
let reachGaugeChart: ECharts | null = null
let areaChart: ECharts | null = null

let activeAlive = false

function buildPlanType() {
  if (!planTypeRef.value) return
  if (!planTypeChart) planTypeChart = init(planTypeRef.value)
  const data = [
    { name: '健康评估', value: 22 },
    { name: '用药指导', value: 18 },
    { name: '康复训练', value: 14 },
    { name: '生活指导', value: 12 }
  ]
  planTypeChart.setOption({
    tooltip: { trigger: 'item', ...tooltipStyle() },
    series: [
      {
        type: 'pie',
        radius: ['50%', '78%'],
        center: ['50%', '50%'],
        label: { color: 'rgba(20,52,79,0.96)', fontWeight: 800, formatter: '{b}\n{d}%' },
        itemStyle: { borderColor: 'rgba(114,180,205,0.22)', borderWidth: 2 },
        data
      }
    ]
  })
}

function buildHotPlan() {
  if (!hotPlanRef.value) return
  if (!hotPlanChart) hotPlanChart = init(hotPlanRef.value)
  const names = ['血压管理', '血糖管理', '睡眠改善', '运动处方', '饮食指导']
  const vals = [34, 26, 22, 18, 16]
  const axis = axisStyle()
  hotPlanChart.setOption({
    tooltip: tooltipStyle(),
    grid: { left: 90, right: 18, top: 18, bottom: 10 },
    xAxis: { type: 'value', ...axis },
    yAxis: { type: 'category', data: names, axisLabel: { color: 'rgba(39,85,113,0.92)' }, axisLine: axis.axisLine },
    series: [{ type: 'bar', data: vals, barWidth: 14, itemStyle: { color: '#7fd6e3', borderRadius: [0, 8, 8, 0] } }]
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
        itemStyle: { borderColor: 'rgba(114,180,205,0.22)', borderWidth: 2 },
        data: [
          { name: '待执行', value: demo.overview.followToday.pending },
          { name: '执行中', value: 9 },
          { name: '已完成', value: demo.overview.followToday.done },
          { name: '超期', value: demo.overview.followToday.overdue }
        ]
      }
    ]
  })
}

function buildServiceTrend() {
  if (!serviceTrendRef.value) return
  if (!serviceTrendChart) serviceTrendChart = init(serviceTrendRef.value)
  const axis = axisStyle()
  const labels = demo.trends.last7Days.map((x) => x.day)
  const vals = demo.trends.last7Days.map((x) => x.followups)
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

function buildReachGauge() {
  if (!reachGaugeRef.value) return
  if (!reachGaugeChart) reachGaugeChart = init(reachGaugeRef.value)
  reachGaugeChart.setOption({
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
        data: [{ value: 76, name: '触达率' }]
      }
    ]
  })
}

function buildArea() {
  if (!areaRef.value) return
  if (!areaChart) areaChart = init(areaRef.value)
  const names = ['哈尔滨', '齐齐哈尔', '牡丹江', '佳木斯', '大庆']
  const vals = [38, 24, 19, 16, 14]
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
  hotPlanChart?.resize()
  serviceRingChart?.resize()
  serviceTrendChart?.resize()
  reachGaugeChart?.resize()
  areaChart?.resize()
}

const onResize = rafThrottle(() => resizeAll())

onMounted(() => {
  activeAlive = true
  buildPlanType()
  buildHotPlan()
  buildServiceRing()
  buildServiceTrend()
  buildReachGauge()
  buildArea()
  window.addEventListener('resize', onResize)
})

onUnmounted(() => {
  activeAlive = false
  window.removeEventListener('resize', onResize)
  planTypeChart?.dispose()
  hotPlanChart?.dispose()
  serviceRingChart?.dispose()
  serviceTrendChart?.dispose()
  reachGaugeChart?.dispose()
  areaChart?.dispose()
  planTypeChart = null
  hotPlanChart = null
  serviceRingChart = null
  serviceTrendChart = null
  reachGaugeChart = null
  areaChart = null
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

.muted-ring {
  border-radius: 999px;
  border: 1px dashed rgba(114, 180, 205, 0.32);
  margin: 14px;
}
</style>

