<template>
  <div class="home-page">
    <div class="page-header">
      <div class="title-row">
        <div class="title-badge" aria-hidden="true">总览</div>
        <div class="title-texts">
          <div class="page-title">数据总览</div>
          <div class="page-subtitle">在管患者、风险分层、预警与随访情况一览</div>
        </div>
      </div>
    </div>

    <!-- 顶部三个统计卡片 -->
    <div class="card-grid cols-3">
      <div class="card card-stat card-clickable" @click="goToPatientList">
        <div class="stat-label">在管患者总数</div>
        <div class="stat-value">{{ stats.totalPatients || '--' }} 人</div>
        <div class="stat-desc">较上周 {{ stats.totalPatientsChange || '--' }}</div>
      </div>
      <div class="card card-stat card-clickable" @click="goToFollowupWorkbench">
        <div class="stat-label">本周随访完成率</div>
        <div class="stat-value">{{ stats.weekFollowRate != null ? Math.round(Number(stats.weekFollowRate) * 100) + '%' : '--%' }}</div>
        <div class="stat-desc">共 {{ stats.weekFollowDone != null ? stats.weekFollowDone : '--' }} 人完成随访</div>
      </div>
      <div class="card card-stat card-clickable" @click="goToAlertCenter">
        <div class="stat-label">异常告警处理中</div>
        <div class="stat-value">{{ stats.processingAlerts || '--' }} 条</div>
        <div class="stat-desc">其中 {{ stats.redAlerts || '--' }} 条为红色告警</div>
      </div>
    </div>

    <!-- 中间图表区域 -->
    <div class="card-grid cols-3">
      <div class="card">
        <div class="card-title">风险分层趋势</div>
        <div id="riskTrendChart" class="chart-box">
          <div v-if="!hasMonthSummary" class="chart-empty">暂无数据</div>
        </div>
      </div>
      <div class="card">
        <div class="card-title">告警趋势</div>
        <div id="alertTrendChart" class="chart-box">
          <div v-if="!hasMonthSummary" class="chart-empty">暂无数据</div>
        </div>
      </div>
      <div class="card">
        <div class="card-title">患者占比</div>
        <div id="riskPieChart" class="chart-box"></div>
      </div>
    </div>

    <!-- 底部两块表格 -->
    <div class="card-grid cols-2">
      <div class="card card-clickable" @click="goToAlertCenterWithHighRisk">
        <div class="card-title">待处理高危告警清单</div>
        <div class="table-scroll">
          <table class="table">
            <thead>
              <tr>
                <th>患者姓名</th>
                <th>主要风险因素</th>
                <th>告警时长</th>
                <th>责任医生</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="tables.alerts.length === 0">
                <td colspan="4" style="text-align: center; color: #999;">暂无数据</td>
              </tr>
              <tr 
                v-for="item in tables.alerts" 
                :key="item.patientName"
              >
                <td>{{ item.patientName }}</td>
                <td>{{ item.riskFactors }}</td>
                <td>{{ item.durationText }}</td>
                <td>{{ item.doctorName }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="card">
        <div class="card-title">近期症状加重患者</div>
        <div class="table-scroll">
          <table class="table">
            <thead>
              <tr>
                <th>患者姓名</th>
                <th>症状变化</th>
                <th>时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="tables.syndromes.length === 0">
                <td colspan="3" style="text-align: center; color: #999;">暂无数据</td>
              </tr>
              <tr v-for="item in tables.syndromes" :key="item.patientName">
                <td>{{ item.patientName }}</td>
                <td>{{ item.changeDesc }}</td>
                <td>{{ item.timeText }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import request from '@/api/request'

const router = useRouter()

interface HomeStats {
  totalPatients?: number
  totalPatientsChange?: string
  weekFollowRate?: number
  weekFollowDone?: number
  highRiskRatio?: number
  highRiskCount?: number
  midRiskCount?: number
  lowRiskCount?: number
  processingAlerts?: number
  redAlerts?: number
}

interface TableRow {
  patientName?: string
  riskFactors?: string
  durationText?: string
  doctorName?: string
  changeDesc?: string
  timeText?: string
  finished?: number
  total?: number
  rate?: number | string
}

interface HomeTables {
  alerts: TableRow[]
  syndromes: TableRow[]
}

interface MonthSummary {
  months: string[]
  highRisk: number[]
  midRisk: number[]
  lowRisk: number[]
  alerts: number[]
  followups: number[]
}

const stats = ref<HomeStats>({})
const tables = ref<HomeTables>({
  alerts: [],
  syndromes: []
})
const monthSummary = ref<MonthSummary>({
  months: [],
  highRisk: [],
  midRisk: [],
  lowRisk: [],
  alerts: [],
  followups: []
})

const hasMonthSummary = computed(() => monthSummary.value.months.length > 0)

let riskChart: echarts.ECharts | null = null
let alertChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null

let initChartRaf: number | null = null
let initChartRetry = 0

function handleChartResize() {
  riskChart?.resize()
  alertChart?.resize()
  pieChart?.resize()
}

function durationFromCreatedAt(createdAt: any): string {
  if (!createdAt) return ''
  const s = String(createdAt).trim()
  if (!s) return ''
  const d = new Date(s.replace(' ', 'T'))
  const ts = d.getTime()
  if (Number.isNaN(ts)) return ''

  const minutes = Math.max(0, Math.floor((Date.now() - ts) / 60000))
  if (minutes < 60) return `${minutes}分钟`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时`
  const days = Math.floor(hours / 24)
  return `${days}天`
}

onMounted(async () => {
  // 并行加载，避免三个接口串行导致总耗时叠加
  await Promise.all([loadStats(), loadTables(), loadMonthSummary()])
  await nextTick()
  initChartRetry = 0
  initChartRaf = requestAnimationFrame(() => {
    initCharts()
    handleChartResize()
  })
  window.addEventListener('resize', handleChartResize)
})

async function loadStats() {
  try {
    const response = await request.get('/api/home/stats')
    stats.value = response.data || response
  } catch (error) {
    console.error('加载首页统计失败:', error)
  }
}

async function loadTables() {
  // 直接使用模拟数据
  try {
    const response: any = await request.get('/api/home/tables')
    const data: any = response && (response as any).data ? (response as any).data : response

    const alertsRaw = data && Array.isArray(data.alerts) ? data.alerts : []
    const syndromesRaw = data && Array.isArray(data.syndromes) ? data.syndromes : []

    tables.value = {
      alerts: alertsRaw.map((it: any) => ({
        patientName: it.patientName || it.name,
        riskFactors: it.riskFactors || it.content || it.summary || it.risk || '',
        durationText: it.durationText || it.durationText || it.duration || it.elapsed || durationFromCreatedAt(it.createdAt) || '',
        doctorName: it.doctorName || it.doctor || ''
      })),
      syndromes: syndromesRaw.map((it: any) => ({
        patientName: it.patientName || it.name,
        changeDesc: it.changeDesc || it.change || it.summary || '',
        timeText: it.timeText || it.time || it.createdAt || ''
      }))
    }
  } catch (error) {
    console.error('加载首页表格失败:', error)
    tables.value = { alerts: [], syndromes: [] }
  }
}

async function loadMonthSummary() {
  try {
    const response = await request.get('/api/report/month-summary')
    const data = response.data || response
    if (Array.isArray(data) && data.length > 0) {
      const monthsAll = data.map((item: any) => item.month || item.months?.[0] || '')
      const highAll = data.map((item: any) => item.highRisk || item.high_risk || 0)
      const midAll = data.map((item: any) => item.midRisk || item.mid_risk || 0)
      const lowAll = data.map((item: any) => item.lowRisk || item.low_risk || 0)
      const alertsAll = data.map((item: any) => item.alerts || item.alert_count || 0)
      const followupsAll = data.map((item: any) => item.followups || item.followup_count || 0)

      // 只保留最近三个月的数据用于风险分层趋势和告警趋势
      const len = monthsAll.length
      const start = len > 3 ? len - 3 : 0

      monthSummary.value = {
        months: monthsAll.slice(start),
        highRisk: highAll.slice(start),
        midRisk: midAll.slice(start),
        lowRisk: lowAll.slice(start),
        alerts: alertsAll.slice(start),
        followups: followupsAll.slice(start)
      }
    }
  } catch (error) {
    console.error('加载月度汇总失败:', error)
  }
}

onUnmounted(() => {
  window.removeEventListener('resize', handleChartResize)
  if (initChartRaf != null) {
    cancelAnimationFrame(initChartRaf)
    initChartRaf = null
  }
  if (riskChart) {
    riskChart.dispose()
    riskChart = null
  }
  if (alertChart) {
    alertChart.dispose()
    alertChart = null
  }
  if (pieChart) {
    pieChart.dispose()
    pieChart = null
  }
})

function initCharts() {
  const riskDom = document.getElementById('riskTrendChart')
  const alertDom = document.getElementById('alertTrendChart')
  const pieDom = document.getElementById('riskPieChart')
  
  if (!riskDom || !alertDom || !pieDom) return

  const w1 = (riskDom as any).clientWidth || 0
  const h1 = (riskDom as any).clientHeight || 0
  const w2 = (alertDom as any).clientWidth || 0
  const h2 = (alertDom as any).clientHeight || 0
  const w3 = (pieDom as any).clientWidth || 0
  const h3 = (pieDom as any).clientHeight || 0

  if (w1 <= 0 || h1 <= 0 || w2 <= 0 || h2 <= 0 || w3 <= 0 || h3 <= 0) {
    if (initChartRetry < 10) {
      initChartRetry += 1
      initChartRaf = requestAnimationFrame(() => {
        initCharts()
      })
    }
    return
  }

  if (riskChart) {
    riskChart.dispose()
    riskChart = null
  }
  if (alertChart) {
    alertChart.dispose()
    alertChart = null
  }
  if (pieChart) {
    pieChart.dispose()
    pieChart = null
  }

  riskChart = echarts.init(riskDom)
  alertChart = echarts.init(alertDom)
  pieChart = echarts.init(pieDom)

  const summary = monthSummary.value

  riskChart.setOption({
    tooltip: { trigger: 'axis', valueFormatter: (v: any) => String(Math.round(Number(v) || 0)) },
    legend: { 
      data: ['高危', '中危', '低危'],
      bottom: 0,
      textStyle: { fontSize: 12 }
    },
    grid: {
      left: '10%',
      right: '4%',
      bottom: '15%',
      top: '18%', // 增加上内边距，避免顶部文字压到图形
      containLabel: true
    },
    xAxis: { 
      type: 'category', 
      data: summary.months,
      axisLabel: { fontSize: 11, rotate: 0 }
    },
    yAxis: { 
      type: 'value', 
      name: '人数',
      nameLocation: 'end',
      nameRotate: 0,
      nameGap: 15,
      nameTextStyle: { fontSize: 12, padding: [0, 0, 0, 0] },
      minInterval: 1,
      axisLabel: { fontSize: 11, formatter: (v: any) => String(Math.round(Number(v) || 0)) }
    },
    series: [
      { name: '高危', type: 'line', smooth: true, data: summary.highRisk },
      { name: '中危', type: 'line', smooth: true, data: summary.midRisk },
      { name: '低危', type: 'line', smooth: true, data: summary.lowRisk }
    ]
  })

  alertChart.setOption({
    tooltip: { trigger: 'axis', valueFormatter: (v: any) => String(Math.round(Number(v) || 0)) },
    legend: { 
      data: ['告警数', '随访数'],
      bottom: 0,
      textStyle: { fontSize: 12 }
    },
    grid: {
      left: '10%',
      right: '4%',
      bottom: '15%',
      top: '18%', // 增加上内边距，避免顶部文字压到图形
      containLabel: true
    },
    xAxis: { 
      type: 'category', 
      data: summary.months,
      axisLabel: { fontSize: 11, rotate: 0 }
    },
    yAxis: { 
      type: 'value', 
      name: '次数',
      nameLocation: 'end',
      nameRotate: 0,
      nameGap: 15,
      nameTextStyle: { fontSize: 12, padding: [0, 0, 0, 0] },
      minInterval: 1,
      axisLabel: { fontSize: 11, formatter: (v: any) => String(Math.round(Number(v) || 0)) }
    },
    series: [
      { name: '告警数', type: 'bar', data: summary.alerts },
      { name: '随访数', type: 'line', smooth: true, data: summary.followups }
    ]
  })

  // 患者占比饼图：使用最新月份的数据，如果没有则使用默认值
  const lastIndex = summary.highRisk.length > 0 ? summary.highRisk.length - 1 : 0
  const highRiskCount = summary.highRisk.length > 0 ? summary.highRisk[lastIndex] : stats.value.highRiskCount || 0
  const midRiskCount = summary.midRisk.length > 0 ? summary.midRisk[lastIndex] : 0
  const lowRiskCount = summary.lowRisk.length > 0 ? summary.lowRisk[lastIndex] : 0

  // 如果从月度数据中获取不到，尝试从 stats 中获取
  const highRisk = (stats.value.highRiskCount ?? 0) || highRiskCount || 0
  const midRisk = (stats.value.midRiskCount ?? 0) || midRiskCount || 0
  const lowRisk = (stats.value.lowRiskCount ?? 0) || lowRiskCount || 0

  // 使用固定配置初始化饼图
  updatePieChart(highRisk, midRisk, lowRisk)
}

function updatePieChart(highRisk: number, midRisk: number, lowRisk: number) {
  // 使用固定配置，确保饼图在上方、三个危险等级标识在下方，并尽量保证数值完整显示
  pieChart?.setOption({
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'horizontal',
      bottom: 0,
      left: 'center',
      textStyle: { fontSize: 12 },
      data: ['高危', '中危', '低危'],
      itemWidth: 14,
      itemHeight: 14,
      itemGap: 10
    },
    series: [
      {
        name: '患者占比',
        type: 'pie',
        radius: ['32%', '58%'],            // 进一步缩小饼图
        center: ['50%', '42%'],            // 居中位置，为文字和底部图例留出合适空间
        avoidLabelOverlap: true,
        itemStyle: {
          borderRadius: 8,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: true,
          // 名称在下方图例里已经显示，这里只保留数值 + 百分比，文字更短更容易完全展示
          formatter: '{c}\n({d}%)',
          fontSize: 10,
          lineHeight: 13
        },
        labelLine: {
          show: true,
          length: 14,
          length2: 10,
          smooth: true
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 14,
            fontWeight: 'bold'
          }
        },
        data: [
          { value: highRisk, name: '高危', itemStyle: { color: '#ef4444' } },
          { value: midRisk, name: '中危', itemStyle: { color: '#f59e0b' } },
          { value: lowRisk, name: '低危', itemStyle: { color: '#10b981' } }
        ]
      }
    ]
  })
}

function goToPatientList() {
  // 从数据总览跳转到患者档案管理（风险列表）页面
  router.push({ name: 'PatientList' })
}

function goToFollowupWorkbench() {
  router.push({ name: 'FollowupWorkbench' })
}

function goToAlertCenter() {
  // 从数据总览跳转到预警管理 / 健康数据异常预警列表页面
  router.push({ name: 'PatientAlert' })
}

function goToAlertCenterWithHighRisk() {
  // 跳转到患者异常列表，并携带高危筛选条件
  router.push({
    name: 'PatientAlert',
    query: { severity: 'HIGH' }
  })
}
</script>

<style scoped>
.home-page {
  /* 固定页面宽度，避免随浏览器宽度自适应缩放；放大时通过横向滚动查看
     宽度取 1120px，进一步缩小整体页面，保证 100% 缩放更容易放下 */
  width: 1120px;
  min-width: 1120px;
  margin: 0 auto;
  box-sizing: border-box;
  overflow-x: auto;
  overflow-y: visible;
  -webkit-overflow-scrolling: touch;
}

/* 缩小首页间距，让内容更紧凑 */
.home-page .page-header {
  margin-bottom: 22px;
  padding-bottom: 14px;
  border-bottom: 1px solid rgba(203, 213, 225, 0.45);
}

/* 标题区：图标徽章 + 渐变标题 + 副标题 */
.title-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title-badge {
  width: 40px;
  height: 40px;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.22) 0%, rgba(118, 75, 162, 0.18) 100%);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.10);
  border: 1px solid rgba(255, 255, 255, 0.7);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: rgba(15, 23, 42, 0.75);
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.2px;
  flex-shrink: 0;
}

.title-texts {
  min-width: 0;
}

.home-page :deep(.page-title) {
  font-size: 24px;
  font-weight: 900;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.home-page :deep(.page-subtitle) {
  font-size: 13px;
  color: #64748b;
  margin-top: 4px;
  font-weight: 600;
}

/* 首页卡片网格固定布局，不自适应折行或单列 */
.home-page .card-grid.cols-3 {
  grid-template-columns: repeat(3, 1fr) !important;
  grid-gap: 16px !important;
}

.home-page .card-grid.cols-2 {
  grid-template-columns: repeat(2, 1fr) !important;
  grid-gap: 16px !important;
}

.card-stat {
  padding: 24px 20px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.9) 0%, rgba(255, 255, 255, 0.7) 100%);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(226, 232, 240, 0.9);
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.08);
  position: relative;
  overflow: hidden;
}

.card-stat::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  opacity: 0.38;
  transition: opacity 0.28s ease;
  z-index: 1;
}

.card-clickable {
  cursor: pointer;
}

.card-clickable:hover {
  /* 统一交给全站卡片 hover（.card / main.css）：不做 translate，避免布局抖动 */
}

.stat-label {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 8px;
  font-weight: 600;
  letter-spacing: 0.2px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 6px;
  color: #2d3748;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.stat-desc {
  font-size: 12px;
  color: #a0aec0;
  font-weight: 600;
}

.chart-box {
  width: 100%;
  height: 240px;
  min-height: 240px;
  border-radius: 14px;
  border: 1px dashed rgba(102, 126, 234, 0.22);
  background: linear-gradient(135deg, 
    rgba(255, 255, 255, 0.9) 0%, 
    rgba(247, 250, 252, 0.9) 100%);
  backdrop-filter: blur(10px);
  position: relative;
  overflow: hidden;
}

 .chart-empty {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #a0aec0;
  font-size: 14px;
  font-style: italic;
  pointer-events: none;
  z-index: 2;
 }


.chart-box::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(102, 126, 234, 0.05) 0%, transparent 70%);
  animation: rotate 26s linear infinite;
}

@keyframes rotate {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 统一交给全站卡片 hover（.card / main.css）：移除重复的 clickable hover 规则 */

/* 表格美化 */
.table {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid rgba(226, 232, 240, 0.9);
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.08);
}

.table th {
  background: linear-gradient(135deg, #f7fafc 0%, #edf2f7 100%);
  color: #2d3748;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  font-size: 11px;
  padding: 16px 18px;
}

.table td {
  padding: 16px 18px;
  font-weight: 500;
  color: #4a5568;
}

.table tbody tr {
  transition: all 0.2s ease;
  border-bottom: 1px solid rgba(226, 232, 240, 0.5);
}

.table tbody tr:hover {
  background: rgba(102, 126, 234, 0.05);
  transform: none;
}

.table tbody tr:last-child {
  border-bottom: none;
}

/* 空状态美化 */
.table tbody tr td[colspan] {
  text-align: center;
  color: #a0aec0;
  font-style: italic;
  padding: 40px;
  background: rgba(247, 250, 252, 0.5);
}
</style>

