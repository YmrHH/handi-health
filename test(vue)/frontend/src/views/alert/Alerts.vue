<template>
  <div class="alerts-page">
    <div class="page-header">
      <!-- 仅美化，不改布局：标题徽章 + 渐变标题 -->
      <div class="page-title">
        <span class="title-badge" aria-hidden="true">
          <!-- Bell / Alert 风格图标（避免引入新依赖） -->
          <svg viewBox="0 0 24 24" fill="none" class="title-badge-icon">
            <path d="M18 8a6 6 0 1 0-12 0c0 7-3 8-3 8h18s-3-1-3-8Z" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M13.73 21a2 2 0 0 1-3.46 0" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
            <path d="M12 3v1" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
          </svg>
        </span>
        <span class="page-title-text">{{ pageTitle }}</span>
      </div>
      <div class="page-subtitle">{{ pageSubtitle }}</div>
    </div>

    <!-- 筛选栏 -->
    <!-- 仅美化，不改布局：筛选区毛玻璃卡片 -->
    <div class="card filter-card">
      <form @submit.prevent="handleSearch" class="filter-bar">
        <select v-model="filters.status" class="input">
          <option value="">全部状态</option>
          <option value="NEW">未处理</option>
          <option value="PROCESSING">处理中</option>
          <option value="CLOSED">已关闭</option>
        </select>

        <select v-if="activeTab === 'patient'" v-model="filters.type" class="input">
          <option value="">全部类型</option>
          <option v-for="t in data.typeList" :key="t" :value="t">{{ t }}</option>
        </select>

        <select v-if="activeTab === 'hardware'" v-model="filters.deviceType" class="input">
          <option value="">全部设备类型</option>
          <option v-for="dt in hardwareData.deviceTypeList" :key="dt" :value="dt">{{ dt }}</option>
        </select>

        <select v-if="activeTab === 'patient'" v-model="filters.doctor" class="input">
          <option value="">全部责任医生</option>
          <option v-for="d in data.doctorList" :key="d" :value="d">{{ d }}</option>
        </select>

        <select v-model="filters.range" class="input">
          <option value="7">近 7 天</option>
          <option value="30">近 30 天</option>
          <option value="ALL">全部时间</option>
        </select>

        <button class="btn btn-primary" type="submit">筛选</button>
        <button class="btn btn-ghost" type="button" @click="handleReset">重置</button>
      </form>
    </div>

    <!-- 主体：左列表 + 右详情 -->
    <div class="card-grid cols-2">
      <!-- 左：告警列表 -->
      <div class="card alerts-list-card">
        <div class="card-title">
          {{ activeTab === 'patient' ? '健康数据异常预警列表' : '健康设备异常预警列表' }}
        </div>
        <div v-if="loading" class="loading">加载中...</div>
        <!-- 患者异常列表 -->
        <div v-else-if="activeTab === 'patient'" class="table-wrapper">
        <table class="table alert-table">
          <thead>
            <tr>
              <th>严重程度</th>
              <th>患者</th>
              <th>类型</th>
              <th>首次时间</th>
              <th>责任医生</th>
              <th>状态</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="paginatedRows.length === 0">
              <td colspan="6" class="table-empty">
                {{ filters.severity === 'HIGH' ? '暂无高危告警' : '暂无数据' }}
              </td>
            </tr>
            <tr
              v-for="row in paginatedRows"
              :key="row.id"
              :class="{ 'row-selected': row.selected }"
              @click="selectAlert(row.id)"
              style="cursor: pointer;"
            >
              <td>
                <span :class="getSeverityClass(row.severityText)">{{ row.severityText }}</span>
              </td>
              <td>{{ row.patientName }}</td>
              <td>{{ row.alertType }}</td>
              <td class="cell-muted">{{ row.firstTime }}</td>
              <td>{{ row.doctor }}</td>
              <td>
                <span
                  class="status-badge"
                  :class="[
                    row.statusText === '未处理' ? 'status-new'
                      : row.statusText === '处理中' ? 'status-processing'
                        : row.statusText === '已关闭' ? 'status-closed'
                          : row.statusText === '已恢复' ? 'status-restored'
                            : 'status-closed'
                  ]"
                >
                  {{ row.statusText }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
        </div>
        <!-- 硬件异常列表 -->
        <div v-else class="table-wrapper">
        <table class="table alert-table">
          <thead>
            <tr>
              <th>严重程度</th>
              <th>患者</th>
              <th>设备类型</th>
              <th>异常类型</th>
              <th>首次时间</th>
              <th>状态</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="paginatedHardwareRows.length === 0">
              <td colspan="6" class="table-empty">暂无数据</td>
            </tr>
            <tr
              v-for="row in paginatedHardwareRows"
              :key="row.id"
              :class="{ 'row-selected': row.selected }"
              @click="selectHardwareAlert(row.id)"
              style="cursor: pointer;"
            >
              <td>
                <span :class="getSeverityClass(row.severityText)">{{ row.severityText }}</span>
              </td>
              <td>{{ row.patientName || '--' }}</td>
              <td>{{ row.deviceType }}</td>
              <td>{{ row.alertType }}</td>
              <td class="cell-muted">{{ row.firstTime }}</td>
              <td>
                <span
                  class="status-badge"
                  :class="[
                    row.statusText === '未处理' ? 'status-new'
                      : row.statusText === '处理中' ? 'status-processing'
                        : row.statusText === '已关闭' ? 'status-closed'
                          : row.statusText === '已恢复' ? 'status-restored'
                            : 'status-closed'
                  ]"
                >
                  {{ row.statusText }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
        </div>
        
        <!-- 分页 -->
        <div v-if="getCurrentTotalPages() > 1" class="pagination">
          <div class="page-info">
            共 {{ getCurrentFilteredCount() }} 条记录，第 {{ pageNo }} / {{ getCurrentTotalPages() }} 页
          </div>
          <div class="page-controls">
            <button 
              class="btn btn-secondary" 
              :disabled="pageNo <= 1"
              @click="goToPage(pageNo - 1)">
              上一页
            </button>
            <button 
              class="btn btn-secondary" 
              :disabled="pageNo >= getCurrentTotalPages()"
              @click="goToPage(pageNo + 1)">
              下一页
            </button>
          </div>
        </div>
      </div>

      <!-- 右：详情面板 -->
      <div class="card">
        <div class="card-title">
          {{ activeTab === 'patient' ? '健康数据异常预警详情' : '健康设备异常预警详情' }}
        </div>
        <!-- 患者异常详情 -->
        <div v-if="activeTab === 'patient'">
          <div v-if="!data.detail" class="empty-detail">
            请从左侧列表选择一条告警记录
          </div>
          <div v-else class="detail-content">
            <!-- 基本信息 -->
            <div class="detail-section">
              <h4 class="section-title">基本信息</h4>
              <div class="detail-grid">
            <div class="detail-item">
              <span class="detail-label">患者：</span>
              <span class="detail-value">{{ data.detail.patient }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">严重程度：</span>
              <span :class="getSeverityClass(data.detail.severityText)">{{ data.detail.severityText }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">告警类型：</span>
              <span class="detail-value">{{ data.detail.alertType }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">首次时间：</span>
              <span class="detail-value">{{ data.detail.firstTime }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">责任医生：</span>
              <span class="detail-value">{{ data.detail.doctor }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">状态：</span>
              <span
                class="status-badge"
                :class="[
                  data.detail.statusText === '未处理' ? 'status-new'
                    : data.detail.statusText === '处理中' ? 'status-processing'
                      : data.detail.statusText === '已关闭' ? 'status-closed'
                        : data.detail.statusText === '已恢复' ? 'status-restored'
                          : 'status-closed'
                ]"
              >
                {{ data.detail.statusText }}
              </span>
            </div>
              </div>
            </div>

            <!-- 异常数据趋势 -->
            <div class="detail-section">
              <h4 class="section-title">异常数据趋势分析</h4>
              <!-- 仅修复图表显示：不改布局结构，仅增加 ref 与空态控制 -->
              <div
                id="alertTrendChart"
                ref="alertTrendChartRef"
                class="chart-container"
                :class="{ 'has-chart': trendHasData }"
                @click.capture="handleTrendChartClick"
              >
                <div ref="alertTrendEchartsRef" class="chart-echarts"></div>
                <div v-if="!trendHasData" class="chart-empty">暂无数据</div>
              </div>
            </div>

            <!-- 数据统计 -->
            <div class="detail-section">
              <h4 class="section-title">数据统计</h4>
            <div class="detail-item">
              <span class="detail-label">摘要：</span>
              <div class="detail-value summary-text">{{ data.detail.summary }}</div>
            </div>
            </div>

            <!-- 操作按钮组 -->
            <div class="detail-actions">
              <button class="btn btn-primary" @click="goToRecommendFromDetail">
                📝 下发个体化建议
              </button>
              <button class="btn btn-success" @click="openAssignTaskDialogFromDetail">
                📋 指派随访任务
              </button>
              <button
                v-if="data.detail.statusText !== '已关闭'"
                class="btn btn-danger"
                @click="handleCloseAlertFromDetail"
              >
                ✕ 关闭告警
              </button>
              <button
                v-else
                class="btn btn-secondary"
                @click="handleRestoreAlertFromDetail"
              >
                ↩ 恢复预警
              </button>
            </div>
          </div>
        </div>
        <!-- 硬件异常详情 -->
        <div v-else>
          <div v-if="!hardwareData.detail" class="empty-detail">
            请从左侧列表选择一条硬件异常记录
          </div>
          <div v-else class="detail-content hardware-detail-content">
            <!-- 基本信息 -->
            <div class="detail-section">
              <h4 class="section-title">基本信息</h4>
              <div class="detail-grid">
            <div class="detail-item">
              <span class="detail-label">患者：</span>
              <span class="detail-value">{{ hardwareData.detail.patientName || '--' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">手机号：</span>
              <span class="detail-value">{{ (hardwareData.detail as any).patientPhone || '暂无' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">严重程度：</span>
              <span :class="getSeverityClass(hardwareData.detail.severityText)">{{ hardwareData.detail.severityText }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">设备类型：</span>
              <span class="detail-value">{{ hardwareData.detail.deviceType }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">异常类型：</span>
              <span class="detail-value">{{ hardwareData.detail.alertType }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">设备位置：</span>
              <span class="detail-value">{{ hardwareData.detail.location || '--' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">状态：</span>
              <span
                class="status-badge"
                :class="[
                  hardwareData.detail.statusText === '未处理' ? 'status-new'
                    : hardwareData.detail.statusText === '处理中' ? 'status-processing'
                      : hardwareData.detail.statusText === '已关闭' ? 'status-closed'
                        : hardwareData.detail.statusText === '已恢复' ? 'status-restored'
                          : 'status-closed'
                ]"
              >
                {{ hardwareData.detail.statusText }}
              </span>
            </div>
              </div>
            </div>

            <!-- 异常摘要 -->
            <div class="detail-section">
              <h4 class="section-title">异常摘要</h4>
            <div class="detail-item">
              <div class="detail-value summary-text">{{ hardwareData.detail.summary }}</div>
              </div>
            </div>

            <!-- 操作按钮组 -->
            <div class="detail-actions">
              <button class="btn btn-primary" @click="handleCallPatientForHardware">
                📞 打电话询问
              </button>
              <button
                v-if="hardwareData.detail.statusText !== '已关闭'"
                class="btn btn-danger"
                @click="handleCloseHardwareAlertFromDetail"
              >
                ✕ 关闭告警
              </button>
              <button
                v-else
                class="btn btn-secondary"
                @click="handleRestoreHardwareAlertFromDetail"
              >
                ↩ 恢复预警
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 指派随访任务弹窗 -->
    <Teleport to="body">
      <div v-if="showAssignTaskDialog" class="modal-overlay" @click="closeAssignTaskDialog">
        <div class="modal-content" @click.stop>
          <div class="modal-header">
            <h3>指派随访任务</h3>
            <button class="modal-close" @click="closeAssignTaskDialog">×</button>
          </div>
          <div class="modal-body">
            <!-- 告警信息 -->
            <div class="alert-info-card">
              <div class="alert-info-header">
                <span class="alert-icon">⚠️</span>
                <span class="alert-title">关联告警信息</span>
              </div>
              <div class="alert-info-content">
                <div class="info-row">
                  <span class="info-label">患者：</span>
                  <span class="info-value">{{ taskForm.patientName }}</span>
                </div>
                <div class="info-row">
                  <span class="info-label">告警类型：</span>
                  <span class="info-value">{{ taskForm.alertType }}</span>
                </div>
                <div class="info-row">
                  <span class="info-label">严重程度：</span>
                  <span :class="getSeverityClass(taskForm.severityText)">{{ taskForm.severityText }}</span>
                </div>
              </div>
            </div>

            <div class="form-group">
              <label>随访方式 <span class="required">*</span></label>
              <select v-model="taskForm.followupType" class="input">
                <option value="">请选择随访方式</option>
                <option value="电话随访">电话随访</option>
                <option value="视频随访">视频随访</option>
                <option value="上门随访">上门随访</option>
              </select>
            </div>

            <div class="form-group">
              <label>计划日期 <span class="required">*</span></label>
              <input v-model="taskForm.planDate" type="date" class="input" />
            </div>

            <div class="form-group">
              <label>随访人员</label>
              <select v-model="taskForm.staffId" class="input">
                <option value="">指派人员（科室）</option>
                <option v-for="staff in staffList" :key="staff.id" :value="staff.id">
                  {{ staff.name }}（{{ staff.department || '——' }}）
                </option>
              </select>
            </div>

            <div class="form-group">
              <label>优先级</label>
              <select v-model="taskForm.priority" class="input">
                <option value="HIGH">高优先级 - 需紧急处理</option>
                <option value="MEDIUM">中优先级 - 常规跟进</option>
                <option value="LOW">低优先级 - 择期处理</option>
              </select>
            </div>

            <div class="form-group">
              <label>任务备注</label>
              <textarea 
                v-model="taskForm.remark" 
                class="input" 
                rows="3" 
                placeholder="请输入随访任务的具体要求或注意事项..."
              ></textarea>
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn btn-ghost" @click="closeAssignTaskDialog">取消</button>
            <button 
              class="btn btn-primary" 
              @click="handleAssignTask" 
              :disabled="!isTaskFormValid || submittingTask"
            >
              {{ submittingTask ? '创建中...' : '创建随访任务' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch, onUnmounted, nextTick, onActivated, onDeactivated } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { alertApi, type AlertCenterData } from '@/api/alert'
import { followupApi } from '@/api/followup'
import * as echarts from 'echarts'

const route = useRoute()
const router = useRouter()
const loading = ref(false)

// 明确路由判断：仅当处于告警页面时才允许加载与刷新
const isAlertRoute = computed(() => route.name === 'PatientAlert' || route.name === 'HardwareAlert')

// 当前标签页由路由实时计算（离开告警页时返回 null，不再兜底成 patient）
const activeTab = computed<'patient' | 'hardware' | null>(() => {
  if (!isAlertRoute.value) return null
  const name = route.name as string | undefined
  if (name === 'HardwareAlert') return 'hardware'
  if (name === 'PatientAlert') return 'patient'
  return route.path.includes('/alert/hardware') ? 'hardware' : 'patient'
})

// 根据当前标签计算页面标题与副标题
const pageTitle = computed(() => {
  return activeTab.value === 'patient' ? '健康数据异常预警' : '健康设备异常预警'
})

const pageSubtitle = computed(() => {
  return activeTab.value === 'patient' ? '患者健康数据异常预警管理' : '设备健康异常预警管理'
})

const pageNo = ref(1)
const pageSize = 10

// 图表相关
let trendChart: echarts.ECharts | null = null
const alertTrendChartRef = ref<HTMLElement | null>(null)
const alertTrendEchartsRef = ref<HTMLElement | null>(null)
const trendHasData = ref(false)

// 指派随访任务相关
const showAssignTaskDialog = ref(false)
const submittingTask = ref(false)
type StaffItem = { id: number | string; name: string; department?: string }
const staffList = ref<StaffItem[]>([])

const taskForm = ref({
  alertId: 0,
  patientId: 0,
  patientName: '',
  alertType: '',
  severityText: '',
  followupType: '',
  planDate: '',
  staffId: '' as string | number,
  priority: 'MEDIUM' as 'HIGH' | 'MEDIUM' | 'LOW',
  remark: ''
})

const isTaskFormValid = computed(() => {
  return taskForm.value.followupType && taskForm.value.planDate
})

const filters = ref({
  status: '',
  type: '',
  deviceType: '',
  doctor: '',
  range: 'ALL',
  severity: ''
})

const allRows = ref<any[]>([])
const allHardwareRows = ref<any[]>([])

// AlertCenterData 的 detail 字段在本页会附加 alertId/patientId 等信息，这里放宽类型避免 TS 误报
const data = ref<(AlertCenterData & { detail: any })>({
  statusParam: null,
  typeParam: null,
  doctorParam: null,
  rangeParam: null,
  selectedAlertId: null,
  typeList: [],
  doctorList: [],
  rows: [],
  detail: null
})

const hardwareData = ref({
  deviceTypeList: [] as string[],
  rows: [] as any[],
  detail: null as any
})

// 本地缓存：记录已在前端关闭的患者告警ID，避免刷新后状态回退
const LOCAL_CLOSED_ALERT_IDS_KEY = 'patient_closed_alert_ids'

const filteredRows = computed(() => {
  let filtered = [...allRows.value]
  
  if (filters.value.status) {
    const statusMap: Record<string, string> = {
      'NEW': '未处理',
      'PROCESSING': '处理中',
      'CLOSED': '已关闭'
    }
    const statusText = statusMap[filters.value.status]
    if (statusText) {
      filtered = filtered.filter(row => row.statusText === statusText)
    }
  }
  
  if (filters.value.type) {
    filtered = filtered.filter(row => row.alertType === filters.value.type)
  }
  
  if (filters.value.doctor) {
    filtered = filtered.filter(row => row.doctor === filters.value.doctor)
  }
  
  if (filters.value.severity === 'HIGH') {
    filtered = filtered.filter(row => 
      row.severityText && (
        row.severityText.includes('红色') || 
        row.severityText.includes('高') ||
        row.severityText.includes('HIGH')
      )
    )
  }
  
  if (filters.value.range !== 'ALL') {
    const days = Number(filters.value.range)
    const cutoffDate = new Date()
    cutoffDate.setDate(cutoffDate.getDate() - days)
    
    filtered = filtered.filter(row => {
      if (!row.firstTime) return false
      const firstTime = new Date(row.firstTime)
      return firstTime >= cutoffDate
    })
  }
  
  return filtered
})

const paginatedRows = computed(() => {
  const start = (pageNo.value - 1) * pageSize
  const end = start + pageSize
  return filteredRows.value.slice(start, end)
})

const totalPagesPatient = computed(() => {
  return Math.ceil(filteredRows.value.length / pageSize)
})

const filteredHardwareRows = computed(() => {
  let filtered = [...allHardwareRows.value]
  
  if (filters.value.status) {
    const statusMap: Record<string, string> = {
      'NEW': '未处理',
      'PROCESSING': '处理中',
      'CLOSED': '已关闭'
    }
    const statusText = statusMap[filters.value.status]
    if (statusText) {
      filtered = filtered.filter(row => row.statusText === statusText)
    }
  }
  
  if (filters.value.deviceType) {
    filtered = filtered.filter(row => row.deviceType === filters.value.deviceType)
  }
  
  if (filters.value.range !== 'ALL') {
    const days = Number(filters.value.range)
    const cutoffDate = new Date()
    cutoffDate.setDate(cutoffDate.getDate() - days)
    
    filtered = filtered.filter(row => {
      if (!row.firstTime) return false
      const firstTime = new Date(row.firstTime)
      return firstTime >= cutoffDate
    })
  }
  
  return filtered
})

const paginatedHardwareRows = computed(() => {
  const start = (pageNo.value - 1) * pageSize
  const end = start + pageSize
  return filteredHardwareRows.value.slice(start, end)
})

// 当从“患者异常”切换到“硬件异常”（或反向）时，重置当前页码和选中状态，
// 防止组件复用时仍保留上一个标签的选中行和详情
let refreshInFlight = false
let pageAlive = false
let refreshEpoch = 0
let pendingRefreshKey: string | null = null
let lastLoadedRouteKey = route.fullPath
let didInitialMount = false
let skipFirstActivated = true

function resolvePatientId(row: any): number {
  // 兼容后端/Mock 不同字段命名，避免跳转到错误患者或 /patient/detail/0
  const raw =
    row?.patientId ??
    row?.patientIdNumber ??
    row?.patient_id_number ??
    row?.patientNo ??
    row?.patient_no ??
    row?.medicalRecordNo ??
    row?.medical_record_no ??
    row?.patient_id ??
    row?.pid ??
    row?.userId ??
    row?.user_id ??
    row?.patient?.id ??
    0
  const pid = Number(raw)
  return Number.isFinite(pid) ? pid : 0
}

function resetPageState() {
  // 页面级 reset：清空分页、筛选、选中行与详情，避免路由复用残留
  pageNo.value = 1
  loading.value = false

  filters.value = {
    status: '',
    type: '',
    deviceType: '',
    doctor: '',
    range: 'ALL',
    severity: ''
  }

  // 清空列表与详情
  allRows.value = []
  allHardwareRows.value = []
  data.value.selectedAlertId = null
  data.value.detail = null
  data.value.rows = []
  data.value.typeList = []
  data.value.doctorList = []
  hardwareData.value.rows = []
  hardwareData.value.deviceTypeList = []
  hardwareData.value.detail = null

  // 图表状态也要重置，避免 detail 被清空但图表仍显示旧内容
  trendHasData.value = false
  try {
    if (trendChart) {
      trendChart.dispose()
      trendChart = null
    }
  } catch {}

  // 关闭可能残留的弹窗状态
  showAssignTaskDialog.value = false
  submittingTask.value = false
}

async function refreshPageFromRoute(force = false) {
  // 离开告警页面后禁止继续刷新
  if (!pageAlive || !isAlertRoute.value) return

  const key = route.fullPath
  if (!force && key === lastLoadedRouteKey) return

  if (refreshInFlight) {
    pendingRefreshKey = key
    return
  }

  refreshInFlight = true
  // 本次刷新生成新的 epoch：用于阻断离页后旧异步写回
  refreshEpoch += 1
  const epoch = refreshEpoch
  // 先记录 key，避免 watch(activeTab) 与 watch(route.fullPath) 的重复刷新
  lastLoadedRouteKey = key

  try {
    resetPageState()
    // 路由可能在异步过程中变化：再次校验，避免离页后继续 load
    if (!pageAlive || !isAlertRoute.value || epoch !== refreshEpoch) return

    // 根据路由 query 刷新筛选条件（当前仅用 severity=HIGH）
    const severityParam = route.query.severity as string | undefined
    if (severityParam === 'HIGH') {
      filters.value.severity = 'HIGH'
    }

    if (activeTab.value === 'patient') {
      if (!pageAlive || !isAlertRoute.value || epoch !== refreshEpoch) return
      await loadData(epoch)
      if (!pageAlive || !isAlertRoute.value || epoch !== refreshEpoch) return
      await loadStaffList(epoch)
    } else {
      if (!pageAlive || !isAlertRoute.value || epoch !== refreshEpoch) return
      await loadHardwareData(epoch)
    }
  } catch (e) {
    console.error('刷新告警中心页面状态失败:', e)
  } finally {
    refreshInFlight = false

    if (pendingRefreshKey && pendingRefreshKey !== lastLoadedRouteKey) {
      const nextKey = pendingRefreshKey
      pendingRefreshKey = null
      // 可能发生了更晚的路由变更，继续执行一次最新刷新
      // 这里用 force=true，确保能跳过 lastLoadedRouteKey 的保护
      await refreshPageFromRoute(true)
      // nextKey 仅用于判断是否需要再次刷新；实际数据仍以最新 route 为准
      void nextKey
    }
  }
}

watch(
  () => activeTab.value,
  () => {
    if (!isAlertRoute.value) return
    void refreshPageFromRoute(false)
  }
)

// 补充：监听 route.fullPath（包含 path+query），确保路由复用时 query 变化也会刷新页面级状态
watch(
  () => route.fullPath,
  () => {
    if (!isAlertRoute.value) return
    void refreshPageFromRoute(false)
  }
)

onActivated(() => {
  // KeepAlive 场景下重新激活时也强制刷新，避免复用导致的选中行/详情残留
  pageAlive = isAlertRoute.value
  if (!isAlertRoute.value) return
  if (skipFirstActivated) {
    skipFirstActivated = false
  }
  if (!didInitialMount) return
  void refreshPageFromRoute(true)
})

const totalPagesHardware = computed(() => {
  return Math.ceil(filteredHardwareRows.value.length / pageSize)
})

onMounted(() => {
  didInitialMount = true
  lastLoadedRouteKey = route.fullPath
  pageAlive = isAlertRoute.value
  if (!isAlertRoute.value) return
  // 统一入口：只在 onMounted 初次加载时强制刷新一次，避免首屏与 watcher 双重请求
  void refreshPageFromRoute(true)
})

onBeforeRouteLeave(() => {
  // 离开页面时强制清理：阻止离页后继续刷新与图表残留
  if (trendChart) {
    try { trendChart.dispose() } catch {}
    trendChart = null
  }
  loading.value = false
  pageAlive = false
  refreshEpoch += 1
  refreshInFlight = false
  pendingRefreshKey = null
  // 关键：清空 key，确保回到同一路由时也会触发刷新（避免复用导致“卡住”）
  lastLoadedRouteKey = ''
  showAssignTaskDialog.value = false
})

onUnmounted(() => {
  if (trendChart) {
    try { trendChart.dispose() } catch {}
    trendChart = null
  }
  loading.value = false
  pageAlive = false
  refreshEpoch += 1
  refreshInFlight = false
  pendingRefreshKey = null
  lastLoadedRouteKey = ''
  showAssignTaskDialog.value = false
})

onDeactivated(() => {
  // KeepAlive 场景下离开视图但不卸载时，执行同等 cleanup，避免旧请求写回
  if (trendChart) {
    try { trendChart.dispose() } catch {}
    trendChart = null
  }
  loading.value = false
  pageAlive = false
  refreshEpoch += 1
  refreshInFlight = false
  pendingRefreshKey = null
  // 关键：清空 key，确保回到同一路由时也会触发刷新（避免复用导致“卡住”）
  lastLoadedRouteKey = ''
  showAssignTaskDialog.value = false
})

async function loadStaffList(epoch: number = refreshEpoch) {
  // 阻断离页/旧 epoch 写回
  if (!pageAlive || !isAlertRoute.value || epoch !== refreshEpoch) return
  try {
    const result = await followupApi.getStaffList({ pageNo: 1 })
    if (!pageAlive || !isAlertRoute.value || epoch !== refreshEpoch) return
    if (result.success && result.data) {
      // 后端/Mock 数据可能存在 id 为空的情况，这里做一次映射/过滤，保证下拉选项稳定
      const rows = (result.data.rows || []) as Array<any>
      staffList.value = rows
        .filter(r => r && r.id !== undefined && r.id !== null)
        .map(r => ({ id: r.id, name: r.name, department: r.department }))
    }
  } catch (error) {
    console.error('加载随访人员列表失败:', error)
  }
}

async function loadData(epoch: number = refreshEpoch) {
  if (!pageAlive || !isAlertRoute.value || epoch !== refreshEpoch) return
  loading.value = true
  try {
    const result = await alertApi.getAlerts({})
    if (!pageAlive || !isAlertRoute.value || epoch !== refreshEpoch) return
    const payload = result.success ? (result.data as any) : null

    const rows = (payload?.rows || []) as any[]
    let mappedRows = rows.map(r => ({ ...r, selected: false }))

    // 读取本地标记为“已关闭”的告警ID，刷新时优先覆盖状态
    try {
      const raw = localStorage.getItem(LOCAL_CLOSED_ALERT_IDS_KEY)
      const closedIds: number[] = raw ? JSON.parse(raw) : []
      if (Array.isArray(closedIds) && closedIds.length > 0) {
        const closedSet = new Set(closedIds)
        mappedRows = mappedRows.map(r => closedSet.has(r.id) ? { ...r, statusText: '已关闭' } : r)
      }
    } catch (e) {
      console.error('读取本地关闭告警ID失败:', e)
    }

    allRows.value = mappedRows

    data.value.typeList = Array.isArray(payload?.typeList) ? payload.typeList : []
    data.value.doctorList = Array.isArray(payload?.doctorList) ? payload.doctorList : []
    data.value.selectedAlertId = payload?.selectedAlertId ?? null
    data.value.detail = payload?.detail ?? null

    const defaultId = data.value.selectedAlertId ?? (allRows.value.length > 0 ? allRows.value[0].id : null)
    if (defaultId !== null && defaultId !== undefined) {
      await selectAlert(Number(defaultId), epoch)
    }
  } catch (error) {
    console.error('加载数据失败:', error)
  } finally {
    if (pageAlive && isAlertRoute.value && epoch === refreshEpoch) {
      loading.value = false
    }
  }
}

async function loadHardwareData(epoch: number = refreshEpoch) {
  if (!pageAlive || !isAlertRoute.value || epoch !== refreshEpoch) return
  try {
    const result = await alertApi.getHardwareAlerts({})
    if (!pageAlive || !isAlertRoute.value || epoch !== refreshEpoch) return
    const payload = result.success ? (result.data as any) : null

    const rows = (payload?.rows || []) as any[]
    allHardwareRows.value = rows.map(r => ({ ...r, selected: false }))
    hardwareData.value.deviceTypeList = Array.isArray(payload?.deviceTypeList) ? payload.deviceTypeList : []
    hardwareData.value.detail = payload?.detail ?? null

    const defaultId = payload?.selectedAlertId ?? (allHardwareRows.value.length > 0 ? allHardwareRows.value[0].id : null)
    if (defaultId !== null && defaultId !== undefined) {
      selectHardwareAlert(Number(defaultId), epoch)
    }
  } catch (error) {
    console.error('加载硬件异常数据失败:', error)
  }
}

async function selectAlert(alertId: number, epoch: number = refreshEpoch) {
  // 移除不必要的条件判断，确保函数能够执行
  allRows.value.forEach(row => { row.selected = row.id === alertId })
  
  const selectedRow = allRows.value.find(row => row.id === alertId)
  if (selectedRow) {
    const pid = resolvePatientId(selectedRow)
    data.value.detail = {
      patient: selectedRow.patientName,
      patientId: pid,
      alertId: selectedRow.id,
      severityText: selectedRow.severityText,
      alertType: selectedRow.alertType,
      firstTime: selectedRow.firstTime,
      doctor: selectedRow.doctor,
      statusText: selectedRow.statusText,
      summary: generatePatientSummary(selectedRow)
    }
    data.value.selectedAlertId = alertId
    // 初始化图表
    await nextTick()
    await initChart(epoch)
  }
}

function selectHardwareAlert(alertId: number, epoch: number = refreshEpoch) {
  if (!pageAlive || !isAlertRoute.value || epoch !== refreshEpoch) return
  allHardwareRows.value.forEach(row => { row.selected = row.id === alertId })
  
  const selectedRow = allHardwareRows.value.find(row => row.id === alertId)
  if (selectedRow) {
    if (!pageAlive || !isAlertRoute.value || epoch !== refreshEpoch) return
    hardwareData.value.detail = {
      alertId: selectedRow.id,
      patientName: selectedRow.patientName,
      patientPhone: (selectedRow as any).patientPhone ?? (selectedRow as any).phone ?? '',
      severityText: selectedRow.severityText,
      deviceType: selectedRow.deviceType,
      alertType: selectedRow.alertType,
      location: '患者家中',
      statusText: selectedRow.statusText,
      summary: `患者${selectedRow.patientName}的${selectedRow.deviceType}出现${selectedRow.alertType}，需要及时跟进处理。`
    }
  }
}

function generatePatientSummary(row: any): string {
  const summaries: Record<string, string> = {
    '血压异常': `患者${row.patientName}的血压监测数据显示异常波动，收缩压超出正常范围。建议及时调整降压药物剂量，加强血压监测频次。`,
    '血糖异常': `患者血糖水平出现异常，可能与饮食控制不当或药物依从性差有关。建议重新评估降糖方案。`,
    '心率异常': `心率监测显示异常，可能存在心律不齐或其他心血管问题。建议进行心电图检查。`,
    '用药异常': `患者用药依从性差或出现药物不良反应。建议重新评估用药方案，加强用药指导。`,
    '体重异常': `患者体重出现异常变化，可能影响疾病控制效果。建议评估营养状况，调整饮食方案。`
  }
  return summaries[row.alertType] || `患者${row.patientName}出现${row.alertType}，需要医生及时关注和处理。`
}

// 从详情面板跳转
function goToRecommendFromDetail() {
  if (data.value.detail) {
    try {
      router.push({
        name: 'PatientRecommend',
        query: {
          source: 'alert',
          alertId: data.value.detail.alertId,
          patientName: data.value.detail.patient,
          patientId: data.value.detail.patientId,
          alertType: data.value.detail.alertType
        }
      })
    } catch (error) {
      console.error('跳转失败:', error)
      alert('跳转失败，请稍后重试')
    }
  } else {
    alert('请先选择一条告警记录')
  }
}

function handleTrendChartClick() {
  const detail: any = data.value.detail
  const pid = Number(detail?.patientId || 0)
  try {
    console.log('[Alerts] trend chart click', {
      pid,
      route: { name: route.name, fullPath: route.fullPath },
      detail
    })
  } catch {}
  if (!pid) {
    alert('缺少患者ID，无法跳转到患者档案')
    return
  }
  router
    .push({ name: 'PatientDetail', params: { patientId: String(pid) } })
    .then(() => {
      try {
        console.log('[Alerts] router.push PatientDetail ok', { pid })
      } catch {}
    })
    .catch((e) => {
      console.error('[Alerts] router.push PatientDetail failed:', e)
      alert('跳转失败：' + (e?.message || '未知错误'))
    })
}

function handleSearch() {
  pageNo.value = 1
}

function handleReset() {
  filters.value = {
    status: '',
    type: '',
    deviceType: '',
    doctor: '',
    range: 'ALL',
    severity: ''
  }
  pageNo.value = 1
  data.value.detail = null
  hardwareData.value.detail = null
  data.value.selectedAlertId = null
  allRows.value.forEach(row => { row.selected = false })
  allHardwareRows.value.forEach(row => { row.selected = false })
}

function getCurrentTotalPages(): number {
  return activeTab.value === 'patient' ? totalPagesPatient.value : totalPagesHardware.value
}

function getCurrentFilteredCount(): number {
  return activeTab.value === 'patient' ? filteredRows.value.length : filteredHardwareRows.value.length
}

function goToPage(page: number) {
  const totalPages = getCurrentTotalPages()
  if (page >= 1 && page <= totalPages) {
    pageNo.value = page
  }
}

function getSeverityClass(severity: string): string {
  if (severity.includes('红色') || severity.includes('高')) {
    return 'badge badge-danger'
  } else if (severity.includes('黄色') || severity.includes('中')) {
    return 'badge badge-warning'
  } else {
    return 'badge badge-info'
  }
}

// 指派随访任务相关函数
function openAssignTaskDialogFromDetail() {
  if (data.value.detail) {
    const detail = data.value.detail
    const tomorrow = new Date()
    tomorrow.setDate(tomorrow.getDate() + 1)
    
    taskForm.value = {
      alertId: detail.alertId || 0,
      patientId: detail.patientId || 0,
      patientName: detail.patient,
      alertType: detail.alertType,
      severityText: detail.severityText,
      followupType: '',
      planDate: tomorrow.toISOString().split('T')[0],
      staffId: '',
      priority: detail.severityText.includes('红色') ? 'HIGH' : 'MEDIUM',
      remark: `关联告警：${detail.alertType}（${detail.firstTime}）\n\n告警摘要：${detail.summary}`
    }
    showAssignTaskDialog.value = true
  }
}

function closeAssignTaskDialog() {
  showAssignTaskDialog.value = false
}

async function handleAssignTask() {
  if (!isTaskFormValid.value) return
  
  submittingTask.value = true
  try {
    // 模拟创建随访任务
    await new Promise(resolve => setTimeout(resolve, 800))
    
    const staffName = taskForm.value.staffId 
      ? staffList.value.find(s => s.id === taskForm.value.staffId)?.name || '未指派'
      : '未指派'
    
    closeAssignTaskDialog()
    
    // 提示成功：保持当前页面（不跳转）
    alert(
      `随访任务创建成功！\n\n` +
      `患者：${taskForm.value.patientName}\n` +
      `随访方式：${taskForm.value.followupType}\n` +
      `计划日期：${taskForm.value.planDate}\n` +
      `随访人员：${staffName}`
    )
  } catch (error: any) {
    console.error('创建随访任务失败:', error)
    alert('创建随访任务失败：' + (error.message || '未知错误'))
  } finally {
    submittingTask.value = false
  }
}

// 从详情面板关闭告警
function handleCloseAlertFromDetail() {
  if (!data.value.detail) return
  
  const detail = data.value.detail
  const confirmed = confirm(
    `确定要关闭此告警吗？\n\n` +
    `患者：${detail.patient}\n` +
    `告警类型：${detail.alertType}\n` +
    `严重程度：${detail.severityText}\n\n` +
    `关闭后告警将标记为"已关闭"状态，记录将保留以供追溯。`
  )
  
  if (confirmed) {
    doUpdateAlertStatus('HEALTH', detail.alertId, 'CLOSED')
  }
}

function handleRestoreAlertFromDetail() {
  if (!data.value.detail) return
  const detail = data.value.detail
  const confirmed = confirm(
    `确定要恢复此预警吗？\n\n` +
    `患者：${detail.patient}\n` +
    `告警类型：${detail.alertType}\n\n` +
    `恢复后告警将重新变为“未处理”。`
  )
  if (confirmed) {
    doUpdateAlertStatus('HEALTH', detail.alertId, 'NEW')
  }
}

// 硬件异常：打电话询问
function handleCallPatientForHardware() {
  if (!hardwareData.value.detail) return
  
  const detail = hardwareData.value.detail
  const patientName = detail.patientName || '患者'
  const phone = (detail as any).patientPhone ? String((detail as any).patientPhone) : ''
  const phoneText = phone ? `手机号：${phone}` : '手机号：暂无'
  
  const confirmed = confirm(
    `确定要联系患者进行电话询问吗？\n\n` +
    `患者：${patientName}\n` +
    `${phoneText}\n` +
    `设备类型：${detail.deviceType}\n` +
    `异常类型：${detail.alertType}\n` +
    `设备位置：${detail.location || '--'}\n\n` +
    `点击确定后将记录本次电话询问操作。`
  )
  
  if (confirmed) {
    // 这里可以添加实际的电话拨打逻辑
    // 例如：window.location.href = `tel:${phoneNumber}`
    // 或者记录电话询问的记录
    
    alert(
      `电话询问操作已记录\n\n` +
      `患者：${patientName}\n` +
      `${phoneText}\n` +
      `设备类型：${detail.deviceType}\n` +
      `异常类型：${detail.alertType}\n\n` +
      `请通过电话联系患者，询问设备异常情况。`
    )
    
    // 可以在这里添加后续操作，比如记录电话询问日志、更新状态等
    // 例如：更新硬件异常状态为"处理中"
    if (detail.statusText === '未处理') {
      detail.statusText = '处理中'
      // 同步更新列表中对应的行
      const row = allHardwareRows.value.find(r => 
        r.patientName === detail.patientName &&
        r.deviceType === detail.deviceType &&
        r.alertType === detail.alertType
      )
      if (row) {
        row.statusText = '处理中'
      }
    }
  }
}

// 硬件异常：从详情面板关闭告警
function handleCloseHardwareAlertFromDetail() {
  if (!hardwareData.value.detail) return
  
  const detail = hardwareData.value.detail
  const patientName = detail.patientName || '患者'
  
  const confirmed = confirm(
    `确定要关闭此硬件告警吗？\n\n` +
    `患者：${patientName}\n` +
    `设备类型：${detail.deviceType}\n` +
    `异常类型：${detail.alertType}\n\n` +
    `关闭后告警将标记为\"已关闭\"状态，记录将保留以供追溯。`
  )
  
  if (confirmed) {
    doUpdateAlertStatus('DEVICE', detail.alertId, 'CLOSED')
  }
}

function handleRestoreHardwareAlertFromDetail() {
  if (!hardwareData.value.detail) return
  const detail = hardwareData.value.detail
  const patientName = detail.patientName || '患者'
  const confirmed = confirm(
    `确定要恢复此硬件预警吗？\n\n` +
    `患者：${patientName}\n` +
    `设备类型：${detail.deviceType}\n` +
    `异常类型：${detail.alertType}\n\n` +
    `恢复后告警将重新变为“未处理”。`
  )
  if (confirmed) {
    doUpdateAlertStatus('DEVICE', detail.alertId, 'NEW')
  }
}

async function doUpdateAlertStatus(source: 'HEALTH' | 'DEVICE', alertId: number, status: 'NEW' | 'PROCESSING' | 'CLOSED') {
  try {
    const res = await alertApi.updateAlertStatus({ source, alertId, status })
    if (!res || !res.success) {
      alert(res?.message || '更新失败')
      return
    }

    const nextStatusText =
      status === 'CLOSED' ? '已关闭' :
      status === 'PROCESSING' ? '处理中' : '未处理'

    if (source === 'HEALTH') {
      if (data.value.detail) data.value.detail.statusText = nextStatusText
      const row = allRows.value.find(r => r.id === alertId)
      if (row) row.statusText = nextStatusText

      // 同步本地缓存（兼容旧逻辑）
      try {
        const raw = localStorage.getItem(LOCAL_CLOSED_ALERT_IDS_KEY)
        const closedIds: number[] = raw ? JSON.parse(raw) : []
        const next = Array.isArray(closedIds) ? closedIds.slice() : []
        const idx = next.indexOf(alertId)
        if (status === 'CLOSED' && idx < 0) next.push(alertId)
        if (status !== 'CLOSED' && idx >= 0) next.splice(idx, 1)
        localStorage.setItem(LOCAL_CLOSED_ALERT_IDS_KEY, JSON.stringify(next))
      } catch (e) {}
    } else {
      if (hardwareData.value.detail) hardwareData.value.detail.statusText = nextStatusText
      const row = allHardwareRows.value.find(r => r.id === alertId)
      if (row) row.statusText = nextStatusText
    }

    // 如需确保列表与后台一致：按 source 与当前 activeTab 精准刷新
    const epoch = refreshEpoch
    const canRefresh =
      pageAlive &&
      isAlertRoute.value &&
      epoch === refreshEpoch

    if (canRefresh) {
      if (source === 'HEALTH' && activeTab.value === 'patient') {
        await loadData(epoch)
      } else if (source === 'DEVICE' && activeTab.value === 'hardware') {
        await loadHardwareData(epoch)
      }
    }

    // 刷新后继续校验：离页/复用发生就不允许再写回
    if (!pageAlive || !isAlertRoute.value || epoch !== refreshEpoch) return
  } catch (e: any) {
    alert(e?.message || '更新失败')
  }
}

// 图表初始化
async function initChart(epoch: number = refreshEpoch) {
  if (!pageAlive || !isAlertRoute.value || epoch !== refreshEpoch) return
  if (!data.value.detail) return
  
  const chartDom = alertTrendEchartsRef.value
  if (!chartDom) return
  
  // 如果图表已存在，先销毁
  if (trendChart) {
    trendChart.dispose()
  }
  
  trendChart = echarts.init(chartDom)
  const detail = data.value.detail as any

  let labels: string[] = []
  let values: number[] = []
  let metricName = '指标'
  let unit = ''

  // 尝试从patient_daily_measurement表获取真实数据
  try {
    const patientId = detail.patientId
    if (patientId) {
      const response = await alertApi.getPatientDailyMeasurements(patientId, 7)
      if (response.success && response.data) {
        const measurements = response.data
        if (Array.isArray(measurements) && measurements.length > 0) {
          // 按测量时间排序
          measurements.sort((a: any, b: any) => {
            const dateA = new Date(a.measuredAt || a.measured_at || 0).getTime()
            const dateB = new Date(b.measuredAt || b.measured_at || 0).getTime()
            return dateA - dateB
          })

          // 根据告警类型选择对应的指标
          const alertType = String(detail.alertType || '')
          let valueKey = ''

          if (alertType.includes('血压')) {
            metricName = '收缩压'
            unit = 'mmHg'
            valueKey = 'sbp'
          } else if (alertType.includes('血糖')) {
            metricName = '血糖'
            unit = 'mmol/L'
            valueKey = 'glucose'
          } else if (alertType.includes('心率')) {
            metricName = '心率'
            unit = '次/分'
            valueKey = 'heartRate'
          } else if (alertType.includes('血氧')) {
            metricName = '血氧'
            unit = '%'
            valueKey = 'spo2'
          } else if (alertType.includes('体重')) {
            metricName = '体重'
            unit = 'kg'
            valueKey = 'weightKg'
          }

          // 提取数据
          measurements.forEach((item: any) => {
            const date = new Date(item.measuredAt || item.measured_at)
            const mm = String(date.getMonth() + 1).padStart(2, '0')
            const dd = String(date.getDate()).padStart(2, '0')
            labels.push(`${mm}-${dd}`)
            
            const value = item[valueKey] || item[valueKey.toLowerCase()]
            values.push(Number(value) || 0)
          })
        } else {
          console.warn('[Alerts] daily measurements empty or not array:', {
            patientId,
            days: 7,
            measurementsType: typeof measurements,
            measurements
          })
        }
      }
    } else {
      console.warn('[Alerts] missing patientId for trend chart, cannot fetch daily measurements:', {
        detail,
        selectedAlertId: data.value.selectedAlertId
      })
    }
  } catch (error) {
    console.error('获取患者每日测量数据失败:', error)
  }

  trendChart.setOption({
    color: ['#667eea'],
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(15, 23, 42, 0.86)',
      borderColor: 'rgba(255, 255, 255, 0.08)',
      borderWidth: 1,
      textStyle: { color: '#e5e7eb', fontSize: 12, lineHeight: 18 },
      padding: [10, 12],
      extraCssText: 'border-radius:12px; box-shadow:0 12px 26px rgba(15,23,42,0.26);'
    },
    // 仅修复遮挡：加大 grid 留白，避免轴标题/tooltip 被裁切
    grid: { left: 56, right: 18, top: 34, bottom: 40, containLabel: true },
    xAxis: {
      type: 'category',
      data: labels,
      axisLine: { lineStyle: { color: 'rgba(148,163,184,0.55)' } },
      axisTick: { lineStyle: { color: 'rgba(148,163,184,0.35)' } },
      axisLabel: { color: '#94a3b8', fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      name: unit ? `${metricName}（${unit}）` : metricName,
      nameLocation: 'middle',
      nameGap: 44,
      nameTextStyle: { color: '#94a3b8', fontSize: 12 },
      axisLine: { lineStyle: { color: 'rgba(148,163,184,0.55)' } },
      axisTick: { lineStyle: { color: 'rgba(148,163,184,0.35)' } },
      axisLabel: { color: '#94a3b8', fontSize: 11 },
      splitLine: { lineStyle: { color: 'rgba(148,163,184,0.18)' } }
    },
    series: [{
      name: metricName,
      type: 'line',
      data: values,
      smooth: 0.35,
      symbol: 'circle',
      symbolSize: 7,
      lineStyle: { width: 3, color: '#667eea' },
      itemStyle: { color: '#667eea' },
      areaStyle: { color: 'rgba(102, 126, 234, 0.12)' }
    }]
  })

  const pid = Number(detail.patientId || 0)
  try {
    ;(alertTrendChartRef.value as HTMLElement | null)?.style && (((alertTrendChartRef.value as HTMLElement).style.cursor = pid ? 'pointer' : 'default'))
  } catch {}
  try {
    const zr = trendChart.getZr()
    zr.off('click')
    zr.on('click', () => {
      if (!pid) {
        alert('缺少患者ID，无法跳转到患者档案')
        return
      }
      router.push({ name: 'PatientDetail', params: { patientId: String(pid) } })
    })
  } catch {}

  trendHasData.value = labels.length > 0
  nextTick(() => {
    try { trendChart?.resize() } catch {}
  })
}

// 图表初始化保留在 selectAlert() / selectHardwareAlert() 的时机
// （删除 data.detail 的额外 watch，避免详情加载后重复 initChart）
</script>

<style scoped>
/* =========================
   仅美化，不改布局（告警中心 Alerts.vue）
   ========================= */

.alerts-page {
  /* 仅美化，不改布局（alert/Alerts.vue）：修复两列大卡 hover 被裁切 */
  width: 100%;
  max-width: 1120px;
  min-width: 0;
  margin: 0 auto;
  box-sizing: border-box;
  overflow: visible;
  padding-inline: 12px;
  background: transparent;
}

/* 两列容器：避免父级裁切 hover 放大的溢出区域 */
.alerts-page .card-grid.cols-2 {
  overflow: visible;
  min-width: 0;
}

/* 两列里的大卡：避免被内容撑开导致交互层叠盖 */
.alerts-page .card-grid.cols-2 > .card {
  min-width: 0;
}

/* 页面标题区（保持 page-header 原结构与位置） */
.page-header {
  padding: 6px 0 14px 0;
  border-bottom: 1px solid rgba(148, 163, 184, 0.18);
  margin-bottom: 16px;
}

.page-title {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  font-size: 26px;
  font-weight: 800;
  letter-spacing: 0.2px;
  color: #0f172a;
  margin: 0 0 6px 0;
}

.page-title-text {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.title-badge {
  width: 40px;
  height: 40px;
  border-radius: 14px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10px 20px rgba(102, 126, 234, 0.22);
  flex-shrink: 0;
}

.title-badge-icon {
  width: 22px;
  height: 22px;
}

.page-subtitle {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

/* 统一卡片外观（不改布局结构） */
.card {
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  border: 1px solid rgba(226, 232, 240, 0.70);
  box-shadow: 0 10px 30px rgba(102, 126, 234, 0.08), 0 4px 12px rgba(15, 23, 42, 0.04);
  position: relative;
  overflow: hidden;
  transform: scale(1);
  transform-origin: center center;
  will-change: transform, box-shadow;
  transition:
    transform 0.28s cubic-bezier(0.22, 1, 0.36, 1),
    box-shadow 0.28s cubic-bezier(0.22, 1, 0.36, 1),
    border-color 0.28s ease;
}

.card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  opacity: 0.38;
  transition: opacity 0.28s ease;
  z-index: 1;
  pointer-events: none;
}

.card:hover {
  box-shadow: 0 14px 36px rgba(102, 126, 234, 0.10), 0 8px 18px rgba(15, 23, 42, 0.06);
  border-color: rgba(203, 213, 225, 0.78);
  transform: scale(1.016);
  z-index: 2;
}

/* 仅收紧左列表卡 + 右详情卡的 hover 缩放（避免遮挡/裁切），不改结构 */
.alerts-page .card-grid.cols-2 > .card:hover {
  transform: scale(1.006);
}

.card:hover::before {
  opacity: 0.94;
}

.card > * {
  position: relative;
  z-index: 2;
}

/* card-title：若现有为标题行则统一风格；无标题不新增模块 */
.card-title {
  font-size: 16px;
  font-weight: 700;
  color: #1f2937;
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.card-title::before {
  content: '';
  width: 4px;
  height: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 2px;
}

/* 按钮统一（仅视觉，不改逻辑） */
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 40px;
  padding: 0 14px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.22s ease;
  text-decoration: none;
  user-select: none;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
  border: none;
  box-shadow: 0 12px 26px rgba(102, 126, 234, 0.20);
}

.btn-primary:hover {
  box-shadow: 0 14px 30px rgba(102, 126, 234, 0.26);
}

.btn-ghost,
.btn-secondary {
  background: rgba(255, 255, 255, 0.92);
  border: 2px solid rgba(226, 232, 240, 0.90);
  color: #4a5568;
  backdrop-filter: blur(10px);
}

.btn-ghost:hover,
.btn-secondary:hover:not(:disabled) {
  border-color: rgba(102, 126, 234, 0.55);
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.12);
}

.btn-danger {
  background: rgba(254, 226, 226, 0.9);
  border: 2px solid rgba(248, 113, 113, 0.45);
  color: #991b1b;
}

.btn-danger:hover {
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.12);
}

.btn-success {
  background: rgba(209, 250, 229, 0.92);
  border: 2px solid rgba(16, 185, 129, 0.35);
  color: #065f46;
}

.btn-success:hover {
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.12);
}

.btn:focus,
.btn:focus-visible {
  outline: none;
}

.filter-bar {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  align-items: center;
  padding: 16px 20px;
  background: transparent;
  backdrop-filter: none;
  border-radius: 16px;
  box-sizing: border-box;
}

.filter-bar .input {
  min-width: 120px;
  max-width: 180px;
  height: 44px;
  border-radius: 8px;
  border: 2px solid #e2e8f0;
  background: rgba(255, 255, 255, 0.9);
  font-weight: 500;
  transition: all 0.2s ease;
  box-sizing: border-box;
  flex: 1;
}

.filter-bar .input:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.12);
  outline: none;
}

/* 列表区域：参考个性化建议下发的表格布局，确保列完整显示 */
.table-wrapper {
  width: 100%;
  min-width: 0;
  overflow-x: auto;
  overflow-y: visible;
  -webkit-overflow-scrolling: touch;
}

.alert-table {
  width: max-content;
  min-width: 980px;
}

.alert-table th,
.alert-table td {
  padding: 10px 12px;
  white-space: nowrap;
}

/* 表头/内容层级（仅视觉，不改列顺序） */
.alert-table thead th {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.08) 0%, rgba(118, 75, 162, 0.06) 100%);
  font-size: 12.5px;
  font-weight: 700;
  color: #64748b;
  border-bottom: 1px solid rgba(226, 232, 240, 0.85);
}

.alert-table tbody td {
  font-size: 13.5px;
  color: #1f2937;
  border-bottom: 1px solid rgba(226, 232, 240, 0.45);
}

.table-empty {
  text-align: center;
  color: #94a3b8;
  padding: 18px 12px;
  font-weight: 600;
}

.cell-muted {
  color: #94a3b8;
  font-size: 13px;
  font-weight: 600;
}

/* 患者异常列宽 */
.alert-table th:nth-child(1),
.alert-table td:nth-child(1) { min-width: 90px; }   /* 严重程度 */
.alert-table th:nth-child(2),
.alert-table td:nth-child(2) { min-width: 110px; }  /* 患者 */
.alert-table th:nth-child(3),
.alert-table td:nth-child(3) { min-width: 120px; }  /* 类型/异常类型 */
.alert-table th:nth-child(4),
.alert-table td:nth-child(4) { min-width: 150px; }  /* 首次时间 */
.alert-table th:nth-child(5),
.alert-table td:nth-child(5) { min-width: 110px; }  /* 责任医生/设备类型 */
.alert-table th:nth-child(6),
.alert-table td:nth-child(6) { min-width: 90px; }   /* 状态 */

.row-selected {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%) !important;
  border-left: 4px solid #667eea;
}

.empty-detail {
  text-align: center;
  padding: 60px 20px;
  color: #a0aec0;
  font-size: 16px;
  font-weight: 500;
  background: rgba(247, 250, 252, 0.5);
  border-radius: 12px;
  border: 2px dashed rgba(160, 174, 192, 0.3);
}

.empty-detail::before {
  content: '📋';
  display: block;
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.detail-content {
  padding: 20px 0;
  box-sizing: border-box;
}

.hardware-detail-content {
  padding: 12px 0;
}

.detail-section {
  margin-bottom: 28px;
}

.detail-section:last-of-type {
  margin-bottom: 0;
}

.section-title {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 700;
  color: #2d3748;
  padding-bottom: 12px;
  border-bottom: 2px solid rgba(226, 232, 240, 0.5);
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-title::before {
  content: '';
  width: 4px;
  height: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 2px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.chart-container {
  width: 100%;
  height: 300px;
  min-height: 300px;
  position: relative;
  /* 仅美化：趋势图容器毛玻璃卡片（不改图表数量/位置/类型/数据） */
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 8px;
  box-sizing: border-box;
  box-shadow: 0 10px 30px rgba(102, 126, 234, 0.08), 0 4px 12px rgba(15, 23, 42, 0.04);
  border: 1px solid rgba(226, 232, 240, 0.70);
  overflow: hidden;
}

.chart-echarts {
  position: absolute;
  inset: 8px;
}

/* 仅修复图表显示：当已渲染图表时，隐藏空态覆盖层 */
.chart-container.has-chart .chart-empty {
  display: none;
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
 }

.detail-item {
  margin-bottom: 0;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.7);
  border-radius: 8px;
  border-left: 4px solid #667eea;
  transition: all 0.2s ease;
  box-sizing: border-box;
}

.detail-item:hover {
  background: rgba(255, 255, 255, 0.9);
  /* 仅美化：hover 不位移，不改布局关系 */
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.06);
}

.detail-label {
  color: #4a5568;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 4px;
  display: block;
}

.detail-value {
  color: #2d3748;
  font-size: 14px;
  font-weight: 500;
  word-break: break-word;
}

.summary-text {
  margin-top: 8px;
  line-height: 1.6;
}

.detail-actions {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 2px solid rgba(226, 232, 240, 0.3);
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.hardware-detail-content .detail-actions {
  margin-top: 16px;
  padding-top: 16px;
}

.action-btns {
  display: flex;
  gap: 6px;
}

.badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 6px 12px;
  min-height: 28px;
  border-radius: 999px;
  font-size: 12.5px;
  font-weight: 700;
  letter-spacing: 0.2px;
  border: 1px solid rgba(226, 232, 240, 0.85);
  box-shadow: 0 6px 14px rgba(15, 23, 42, 0.06);
}

.badge-danger {
  background: linear-gradient(135deg, #fed7d7 0%, #feb2b2 100%);
  color: #991b1b;
  border-color: rgba(248, 113, 113, 0.55);
}

.badge-warning {
  background: linear-gradient(135deg, #fefcbf 0%, #faf089 100%);
  color: #92400e;
  border-color: rgba(245, 158, 11, 0.45);
}

.badge-info {
  background: linear-gradient(135deg, #bee3f8 0%, #90cdf4 100%);
  color: #1e3a8a;
  border-color: rgba(96, 165, 250, 0.40);
}

/* 状态标签：pill badge（不改状态流转逻辑，仅根据现有文本渲染） */
.status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 6px 12px;
  min-height: 28px;
  border-radius: 999px;
  font-size: 12.5px;
  font-weight: 700;
  letter-spacing: 0.2px;
  border: 1px solid rgba(226, 232, 240, 0.85);
  box-shadow: 0 6px 14px rgba(15, 23, 42, 0.06);
  white-space: nowrap;
}

.status-new {
  background: linear-gradient(135deg, rgba(254, 202, 202, 0.95) 0%, rgba(254, 226, 226, 0.85) 100%);
  color: #991b1b;
  border-color: rgba(248, 113, 113, 0.55);
}

.status-processing {
  background: linear-gradient(135deg, rgba(254, 249, 195, 0.95) 0%, rgba(253, 230, 138, 0.55) 100%);
  color: #92400e;
  border-color: rgba(245, 158, 11, 0.45);
}

.status-closed {
  background: rgba(226, 232, 240, 0.75);
  color: #475569;
  border-color: rgba(203, 213, 225, 0.85);
}

.status-restored {
  background: linear-gradient(135deg, rgba(209, 250, 229, 0.92) 0%, rgba(187, 247, 208, 0.68) 100%);
  color: #065f46;
  border-color: rgba(16, 185, 129, 0.38);
}

.btn-sm {
  padding: 6px 10px;
  font-size: 11px;
  font-weight: 600;
  border-radius: 6px;
}

.btn-action {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-action:hover {
  /* 仅美化：hover 不位移 */
  box-shadow: 0 12px 26px rgba(102, 126, 234, 0.22);
}

.btn-action-secondary {
  background: linear-gradient(135deg, #48bb78 0%, #38a169 100%);
  color: white;
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-action-secondary:hover {
  /* 仅美化：hover 不位移 */
  box-shadow: 0 12px 26px rgba(72, 187, 120, 0.18);
}

.btn-action-danger {
  background: linear-gradient(135deg, #fc8181 0%, #e53e3e 100%);
  color: white;
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-action-danger:hover {
  /* 仅美化：hover 不位移 */
  box-shadow: 0 12px 26px rgba(229, 62, 62, 0.16);
}

.btn-danger {
  /* 危险操作：浅红底 + 深红字（不做大红实心） */
  background: rgba(254, 226, 226, 0.9);
  border: 2px solid rgba(248, 113, 113, 0.45);
  color: #991b1b;
}

.btn-danger:hover {
  /* 仅美化：hover 不位移 */
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.12);
}

.btn-success {
  background: rgba(209, 250, 229, 0.92);
  border: 2px solid rgba(16, 185, 129, 0.35);
  color: #065f46;
}

.btn-success:hover {
  /* 仅美化：hover 不位移 */
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.12);
}

.loading {
  text-align: center;
  padding: 60px 20px;
  color: #718096;
}

.loading::before {
  content: '';
  display: inline-block;
  width: 20px;
  height: 20px;
  border: 3px solid #e2e8f0;
  border-top: 3px solid #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-right: 12px;
  vertical-align: middle;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.tab-container {
  display: flex;
  gap: 0;
  border-bottom: 2px solid rgba(226, 232, 240, 0.5);
  padding: 0 20px;
}

.tab-btn {
  padding: 16px 32px;
  background: none;
  border: none;
  border-bottom: 3px solid transparent;
  color: #718096;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-bottom: -2px;
}

.tab-btn:hover {
  color: #667eea;
  background: rgba(102, 126, 234, 0.05);
}

.tab-btn.active {
  color: #667eea;
  border-bottom-color: #667eea;
  font-weight: 700;
  background: rgba(102, 126, 234, 0.1);
}

.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 2px solid rgba(226, 232, 240, 0.5);
}

.page-info {
  color: #718096;
  font-size: 14px;
}

.page-controls {
  display: flex;
  gap: 8px;
}

.btn-secondary {
  background: rgba(255, 255, 255, 0.9);
  border: 2px solid #e2e8f0;
  color: #4a5568;
  font-weight: 600;
  padding: 8px 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-secondary:hover:not(:disabled) {
  background: #ffffff;
  border-color: #cbd5e0;
}

.btn-secondary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.table tbody tr {
  transition: all 0.22s ease;
  cursor: pointer;
}

.table tbody tr:hover {
  background: rgba(102, 126, 234, 0.05);
}

/* 告警列表卡片：固定分页条在卡片底部 */
.alerts-list-card {
  display: flex;
  flex-direction: column;
  /* 固定卡片高度，避免随列表内容增减而变化（约可容纳 10 条记录） */
  height: 680px;
  min-width: 0;
}

.alerts-list-card .table-wrapper {
  flex: 1 1 auto;
  /* 列表内容在内部滚动，卡片整体高度保持不变 */
  min-width: 0;
  overflow-x: auto;
  overflow-y: auto;
}

.alerts-list-card .pagination {
  margin-top: auto;
}

/* 弹窗样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(15, 23, 42, 0.45);
  backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.modal-content {
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  width: 90%;
  max-width: 600px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from { 
    opacity: 0;
    transform: translateY(20px) scale(0.95);
  }
  to { 
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 28px;
  border-bottom: 2px solid rgba(226, 232, 240, 0.3);
}

.modal-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #2d3748;
}

.modal-close {
  background: none;
  border: none;
  font-size: 28px;
  color: #a0aec0;
  cursor: pointer;
  padding: 0;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.modal-close:hover {
  background: rgba(226, 232, 240, 0.2);
  color: #718096;
}

.modal-close:focus,
.modal-close:focus-visible,
.filter-bar .input:focus-visible,
.form-group .input:focus-visible,
.btn:focus-visible {
  outline: none;
}

.modal-body {
  padding: 28px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  padding: 24px 28px;
  border-top: 2px solid rgba(226, 232, 240, 0.3);
  background: rgba(248, 250, 252, 0.5);
}

.alert-info-card {
  background: linear-gradient(135deg, rgba(237, 137, 54, 0.1) 0%, rgba(221, 107, 32, 0.1) 100%);
  border: 1px solid rgba(237, 137, 54, 0.3);
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 24px;
}

.alert-info-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.alert-icon {
  font-size: 20px;
}

.alert-title {
  font-weight: 600;
  color: #c05621;
  font-size: 14px;
}

.alert-info-content {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.info-row {
  display: flex;
  align-items: center;
  gap: 4px;
}

.info-label {
  color: #744210;
  font-size: 13px;
  font-weight: 500;
}

.info-value {
  color: #2d3748;
  font-size: 13px;
  font-weight: 600;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #2d3748;
}

.required {
  color: #e53e3e;
}

.form-group .input {
  width: 100%;
  padding: 12px 16px;
  border: 2px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
  background: rgba(255, 255, 255, 0.9);
}

.form-group .input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.form-group textarea.input {
  resize: vertical;
  min-height: 80px;
}
</style>
