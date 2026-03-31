<template>
  <div class="data-log-page">
    <div class="page-header">
      <!-- 仅美化，不改布局：标题徽章 + 渐变标题 -->
      <div class="page-title">
        <span class="title-badge" aria-hidden="true">
          <!-- FileText / History 风格图标（避免引入新依赖） -->
          <svg viewBox="0 0 24 24" fill="none" class="title-badge-icon">
            <path d="M8 3h6l4 4v14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2Z" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/>
            <path d="M14 3v5h5" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/>
            <path d="M9 12h6M9 16h6" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
          </svg>
        </span>
        <span class="page-title-text">日志审计</span>
      </div>
      <div class="page-subtitle">医生建议、数据分析和系统修改日志</div>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <template v-else>

      <!-- 前端操作日志 -->
      <div class="card">
        <div class="card-title">
          <span class="title-icon">🧭</span>
          <span>前端操作日志</span>
        </div>

        <div class="filter-section">
          <div class="filter-row">
            <div class="filter-item">
              <label class="filter-label">类型</label>
              <select v-model="frontendFilter.type" class="input" @change="handleFrontendFilterChange">
                <option value="">全部</option>
                <option value="api">API</option>
                <option value="route">路由</option>
                <option value="action">操作</option>
              </select>
            </div>
            <div class="filter-item">
              <label class="filter-label">用户</label>
              <input
                v-model="frontendFilter.user"
                type="text"
                class="input"
                placeholder="请输入用户名"
                @input="handleFrontendFilterChange"
              />
            </div>
            <div class="filter-item">
              <label class="filter-label">日期范围</label>
              <div class="date-range">
                <input v-model="frontendFilter.startDate" type="date" class="input input-small" @change="handleFrontendFilterChange" />
                <span class="date-separator">至</span>
                <input v-model="frontendFilter.endDate" type="date" class="input input-small" @change="handleFrontendFilterChange" />
              </div>
            </div>
            <div class="filter-actions">
              <button class="btn btn-primary" @click="reloadFrontendLogs">
                <span>🔄</span> 刷新
              </button>
              <button class="btn btn-secondary" @click="exportFrontendLogsFile" :disabled="filteredFrontendLogs.length === 0">
                <span>⬇️</span> 导出
              </button>
              <button class="btn btn-secondary" @click="clearFrontendLogs" :disabled="frontendLogs.length === 0">
                <span>🗑️</span> 清空
              </button>
            </div>
          </div>
        </div>

        <div v-if="filteredFrontendLogs.length > 0" class="log-list">
          <div
            v-for="log in paginatedFrontendLogs"
            :key="log.id"
            class="log-item"
            :class="`log-type-${mapFrontendTypeToStyle(log.type)}`"
            @click.stop="openFrontendLogDetail(log)"
          >
            <div class="log-header">
              <div class="log-type-badge" :class="`badge-${mapFrontendTypeToStyle(log.type)}`">
                {{ getFrontendTypeLabel(log.type) }}
              </div>
              <div class="log-meta">
                <span class="log-operator">{{ log.user || '-' }}</span>
                <span class="log-time">{{ formatFrontendTime(log.time) }}</span>
              </div>
            </div>
            <div class="log-content">
              <p class="log-action">{{ getFrontendSummary(log) }}</p>
            </div>
          </div>
        </div>
        <div v-else class="empty-message">暂无前端操作日志</div>

        <div v-if="filteredFrontendLogs.length > frontendPageSize" class="pagination">
          <div class="page-info">
            共 {{ filteredFrontendLogs.length }} 条记录，第 {{ frontendPageNo }} / {{ frontendTotalPages }} 页
          </div>
          <div class="page-controls">
            <button class="btn btn-secondary" :disabled="frontendPageNo <= 1" @click="goToFrontendPage(frontendPageNo - 1)">上一页</button>
            <span class="page-numbers">
              <button
                v-for="page in getFrontendPageNumbers()"
                :key="String(page)"
                class="page-btn"
                :class="{ active: page === frontendPageNo, disabled: page === '...' }"
                :disabled="page === '...' || page === frontendPageNo"
                @click="page !== '...' && goToFrontendPage(page as number)"
              >
                {{ page }}
              </button>
            </span>
            <button class="btn btn-secondary" :disabled="frontendPageNo >= frontendTotalPages" @click="goToFrontendPage(frontendPageNo + 1)">下一页</button>
          </div>
        </div>
      </div>

      <!-- 修改日志 -->
      <div class="card">
        <div class="card-title">
          <span class="title-icon">📝</span>
          <span>系统修改日志</span>
        </div>
        
        <!-- 筛选区域 -->
        <div class="filter-section">
          <div class="filter-row">
            <div class="filter-item">
              <label class="filter-label">操作类型</label>
              <select v-model="filter.type" class="input" @change="handleFilterChange">
                <option value="">全部</option>
                <option value="followup">随访记录</option>
                <option value="patient">患者档案</option>
                <option value="alert">告警记录</option>
                <option value="plan">康养计划</option>
                <option value="user">用户信息</option>
                <option value="other">其他</option>
              </select>
            </div>
            <div class="filter-item">
              <label class="filter-label">操作人</label>
              <input 
                v-model="filter.operator" 
                type="text" 
                class="input" 
                placeholder="请输入操作人姓名"
                @input="handleFilterChange"
              />
            </div>
            <div class="filter-item">
              <label class="filter-label">日期范围</label>
              <div class="date-range">
                <input 
                  v-model="filter.startDate" 
                  type="date" 
                  class="input input-small"
                  @change="handleFilterChange"
                />
                <span class="date-separator">至</span>
                <input 
                  v-model="filter.endDate" 
                  type="date" 
                  class="input input-small"
                  @change="handleFilterChange"
                />
              </div>
            </div>
            <div class="filter-actions">
              <button class="btn btn-primary" @click="handleFilterChange">
                <span>🔍</span> 筛选
              </button>
              <button class="btn btn-secondary" @click="resetFilter">
                <span>🔄</span> 重置
              </button>
            </div>
          </div>
        </div>

        <!-- 日志列表 -->
        <div v-if="filteredLogs.length > 0" class="log-list">
          <div 
            v-for="log in paginatedLogs" 
            :key="log.id"
            class="log-item"
            :class="`log-type-${log.type}`"
            @click.stop="openBackendLogDetail(log)"
          >
            <div class="log-header">
              <div class="log-type-badge" :class="`badge-${log.type}`">
                {{ getLogTypeLabel(log.type) }}
              </div>
              <div class="log-meta">
                <span class="log-operator">{{ log.operator }}</span>
                <span class="log-time">{{ log.time }}</span>
              </div>
            </div>
            <div class="log-content">
              <p class="log-action">{{ log.action }}</p>
            </div>
          </div>
        </div>
        <div v-else class="empty-message">暂无修改日志</div>

        <!-- 分页 -->
        <div v-if="filteredLogs.length > pageSize" class="pagination">
          <div class="page-info">
            共 {{ filteredLogs.length }} 条记录，第 {{ logPageNo }} / {{ logTotalPages }} 页
          </div>
          <div class="page-controls">
            <button 
              class="btn btn-secondary" 
              :disabled="logPageNo <= 1"
              @click="goToLogPage(logPageNo - 1)">
              上一页
            </button>
            <span class="page-numbers">
              <button
                v-for="page in getLogPageNumbers()"
                :key="page"
                class="page-btn"
                :class="{ active: page === logPageNo, disabled: page === '...' }"
                :disabled="page === '...' || page === logPageNo"
                @click="page !== '...' && goToLogPage(page)">
                {{ page }}
              </button>
            </span>
            <button 
              class="btn btn-secondary" 
              :disabled="logPageNo >= logTotalPages"
              @click="goToLogPage(logPageNo + 1)">
              下一页
            </button>
          </div>
        </div>
      </div>

      <div v-if="detailModalVisible" class="detail-modal-overlay" @click.self="closeDetailModal">
        <div class="detail-modal">
          <div class="detail-modal-header">
            <div class="detail-modal-title">操作详情</div>
            <button class="detail-modal-close" @click="closeDetailModal">×</button>
          </div>
          <div class="detail-modal-body">
            <div v-if="detailModalKind === 'frontend' && selectedFrontendLog" class="detail-grid">
              <div class="detail-row">
                <span class="detail-k">时间</span>
                <span class="detail-v">{{ formatFrontendTime(selectedFrontendLog.time) }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-k">用户</span>
                <span class="detail-v">{{ selectedFrontendLog.user || '-' }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-k">事件</span>
                <span class="detail-v">{{ getFrontendSummary(selectedFrontendLog) }}</span>
              </div>
              <div v-if="selectedFrontendLog.page" class="detail-row">
                <span class="detail-k">页面</span>
                <span class="detail-v">{{ selectedFrontendLog.page }}</span>
              </div>
              <div v-if="selectedFrontendLog.type === 'route'" class="detail-row">
                <span class="detail-k">路由</span>
                <span class="detail-v">{{ selectedFrontendLog.routeFrom }} → {{ selectedFrontendLog.routeTo }}</span>
              </div>
              <div v-if="selectedFrontendLog.type === 'api'" class="detail-row">
                <span class="detail-k">请求</span>
                <span class="detail-v">{{ selectedFrontendLog.method }} {{ selectedFrontendLog.url }}</span>
              </div>
              <div v-if="selectedFrontendLog.type === 'api'" class="detail-row">
                <span class="detail-k">结果</span>
                <span class="detail-v">{{ selectedFrontendLog.success ? '成功' : '失败' }}<span v-if="selectedFrontendLog.status">（{{ selectedFrontendLog.status }}）</span><span v-if="typeof selectedFrontendLog.durationMs === 'number'">，耗时 {{ selectedFrontendLog.durationMs }}ms</span></span>
              </div>
            </div>

            <div v-else-if="detailModalKind === 'backend' && selectedBackendLog" class="detail-grid">
              <div class="detail-row">
                <span class="detail-k">时间</span>
                <span class="detail-v">{{ selectedBackendLog.time }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-k">操作人</span>
                <span class="detail-v">{{ selectedBackendLog.operator }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-k">事件</span>
                <span class="detail-v">{{ selectedBackendLog.action }}</span>
              </div>
              <div v-if="selectedBackendLog.target" class="detail-row">
                <span class="detail-k">对象</span>
                <span class="detail-v">{{ selectedBackendLog.target }}</span>
              </div>
              <div v-if="selectedBackendLog.description" class="detail-row">
                <span class="detail-k">备注</span>
                <span class="detail-v">{{ selectedBackendLog.description }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { systemApi, type SystemDataLogData } from '@/api/system'
import {
  listFrontendAuditLogs,
  clearFrontendAuditLogs,
  exportFrontendAuditLogs,
  type FrontendAuditLog
} from '@/audit/frontendAudit'

const loading = ref(false)
const data = ref<SystemDataLogData>({
  dictList: [],
  logList: []
})

// 筛选条件
const filter = ref({
  type: '',
  operator: '',
  startDate: '',
  endDate: ''
})

// 分页
const logPageNo = ref(1)
const pageSize = 10

// 筛选后的日志列表
const filteredLogs = computed(() => {
  if (!data.value.modificationLogs) return []
  
  let logs = [...data.value.modificationLogs]
  
  // 操作类型筛选
  if (filter.value.type) {
    logs = logs.filter(log => log.type === filter.value.type)
  }
  
  // 操作人筛选
  if (filter.value.operator) {
    const keyword = filter.value.operator.toLowerCase()
    logs = logs.filter(log => 
      log.operator.toLowerCase().includes(keyword)
    )
  }
  
  // 日期范围筛选
  if (filter.value.startDate) {
    logs = logs.filter(log => log.time >= filter.value.startDate)
  }
  if (filter.value.endDate) {
    logs = logs.filter(log => log.time <= filter.value.endDate + ' 23:59:59')
  }
  
  // 按时间倒序排列
  return logs.sort((a, b) => new Date(b.time).getTime() - new Date(a.time).getTime())
})

// 分页后的日志列表
const paginatedLogs = computed(() => {
  const start = (logPageNo.value - 1) * pageSize
  const end = start + pageSize
  return filteredLogs.value.slice(start, end)
})

// 总页数
const logTotalPages = computed(() => {
  return Math.ceil(filteredLogs.value.length / pageSize)
})

onMounted(() => {
  reloadFrontendLogs()
  loadData()
})

const frontendLogs = ref<FrontendAuditLog[]>([])

const frontendFilter = ref({
  type: '',
  user: '',
  startDate: '',
  endDate: ''
})

const frontendPageNo = ref(1)
const frontendPageSize = 10

const detailModalVisible = ref(false)
const detailModalKind = ref<'frontend' | 'backend'>('backend')
const selectedFrontendLog = ref<FrontendAuditLog | null>(null)
const selectedBackendLog = ref<any | null>(null)

const filteredFrontendLogs = computed(() => {
  let logs = [...frontendLogs.value]

  if (frontendFilter.value.type) {
    logs = logs.filter((l) => l.type === frontendFilter.value.type)
  }

  if (frontendFilter.value.user) {
    const kw = frontendFilter.value.user.toLowerCase()
    logs = logs.filter((l) => (l.user || '').toLowerCase().includes(kw))
  }

  if (frontendFilter.value.startDate) {
    logs = logs.filter((l) => (l.time || '') >= frontendFilter.value.startDate)
  }
  if (frontendFilter.value.endDate) {
    logs = logs.filter((l) => (l.time || '') <= frontendFilter.value.endDate + 'T23:59:59.999Z')
  }

  return logs.sort((a, b) => new Date(b.time).getTime() - new Date(a.time).getTime())
})

const paginatedFrontendLogs = computed(() => {
  const start = (frontendPageNo.value - 1) * frontendPageSize
  const end = start + frontendPageSize
  return filteredFrontendLogs.value.slice(start, end)
})

const frontendTotalPages = computed(() => {
  return Math.ceil(filteredFrontendLogs.value.length / frontendPageSize)
})

function reloadFrontendLogs() {
  frontendLogs.value = listFrontendAuditLogs()
  frontendPageNo.value = 1
}

function handleFrontendFilterChange() {
  frontendPageNo.value = 1
}

function clearFrontendLogs() {
  clearFrontendAuditLogs()
  reloadFrontendLogs()
}

function exportFrontendLogsFile() {
  const content = exportFrontendAuditLogs()
  const blob = new Blob([content], { type: 'application/json;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `frontend-audit-${new Date().toISOString().slice(0, 10)}.json`
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}

function goToFrontendPage(page: number) {
  if (page >= 1 && page <= frontendTotalPages.value) {
    frontendPageNo.value = page
  }
}

function getFrontendPageNumbers(): (number | string)[] {
  const current = frontendPageNo.value
  const total = frontendTotalPages.value
  const pages: (number | string)[] = []

  if (total <= 7) {
    for (let i = 1; i <= total; i++) {
      pages.push(i)
    }
  } else {
    if (current <= 4) {
      for (let i = 1; i <= 5; i++) {
        pages.push(i)
      }
      pages.push('...')
      pages.push(total)
    } else if (current >= total - 3) {
      pages.push(1)
      pages.push('...')
      for (let i = total - 4; i <= total; i++) {
        pages.push(i)
      }
    } else {
      pages.push(1)
      pages.push('...')
      for (let i = current - 1; i <= current + 1; i++) {
        pages.push(i)
      }
      pages.push('...')
      pages.push(total)
    }
  }

  return pages
}

function getFrontendTypeLabel(type: string): string {
  const map: Record<string, string> = {
    api: 'API',
    route: '路由',
    action: '操作'
  }
  return map[type] || type
}

function mapFrontendTypeToStyle(type: string): string {
  if (type === 'api') return 'other'
  if (type === 'route') return 'plan'
  if (type === 'action') return 'user'
  return 'other'
}

function formatFrontendTime(iso: string): string {
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

function getFrontendSummary(log: FrontendAuditLog): string {
  if (log.type === 'route') {
    return `页面跳转：${log.routeFrom || '-'} → ${log.routeTo || '-'}`
  }
  if (log.type === 'api') {
    return `${log.method || 'GET'} ${log.url || ''}`
  }
  return log.action || '前端操作'
}

function openFrontendLogDetail(log: FrontendAuditLog) {
  detailModalKind.value = 'frontend'
  selectedFrontendLog.value = log
  selectedBackendLog.value = null
  detailModalVisible.value = true
}

function openBackendLogDetail(log: any) {
  detailModalKind.value = 'backend'
  selectedBackendLog.value = log
  selectedFrontendLog.value = null
  detailModalVisible.value = true
}

function closeDetailModal() {
  detailModalVisible.value = false
  selectedFrontendLog.value = null
  selectedBackendLog.value = null
}

async function loadData() {
  loading.value = true
  try {
    const result = await systemApi.getDataLog()
    if (result.success && result.data) {
      data.value = result.data

      ;(data.value as any).doctorRecommendations = Array.isArray((data.value as any).doctorRecommendations)
        ? (data.value as any).doctorRecommendations
        : []
      ;(data.value as any).modificationLogs = Array.isArray((data.value as any).modificationLogs)
        ? (data.value as any).modificationLogs
        : []
    }
  } catch (error) {
    console.error('加载数据失败:', error)

    ;(data.value as any).doctorRecommendations = []
    ;(data.value as any).dataAnalysis = []
    ;(data.value as any).modificationLogs = []
  } finally {
    loading.value = false
  }
}

// 筛选变化处理
function handleFilterChange() {
  logPageNo.value = 1 // 重置到第一页
}

// 重置筛选
function resetFilter() {
  filter.value = {
    type: '',
    operator: '',
    startDate: '',
    endDate: ''
  }
  logPageNo.value = 1
}

// 跳转到指定页
function goToLogPage(page: number) {
  if (page >= 1 && page <= logTotalPages.value) {
    logPageNo.value = page
  }
}

// 获取分页数字列表
function getLogPageNumbers(): (number | string)[] {
  const current = logPageNo.value
  const total = logTotalPages.value
  const pages: (number | string)[] = []

  if (total <= 7) {
    for (let i = 1; i <= total; i++) {
      pages.push(i)
    }
  } else {
    if (current <= 4) {
      for (let i = 1; i <= 5; i++) {
        pages.push(i)
      }
      pages.push('...')
      pages.push(total)
    } else if (current >= total - 3) {
      pages.push(1)
      pages.push('...')
      for (let i = total - 4; i <= total; i++) {
        pages.push(i)
      }
    } else {
      pages.push(1)
      pages.push('...')
      for (let i = current - 1; i <= current + 1; i++) {
        pages.push(i)
      }
      pages.push('...')
      pages.push(total)
    }
  }

  return pages
}

// 获取日志类型标签
function getLogTypeLabel(type: string): string {
  const typeMap: Record<string, string> = {
    followup: '随访记录',
    patient: '患者档案',
    alert: '告警记录',
    plan: '康养计划',
    user: '用户信息',
    other: '其他'
  }
  return typeMap[type] || '其他'
}

// 获取分析类型标签
function getAnalysisTypeLabel(type: string): string {
  const typeMap: Record<string, string> = {
    patient: '患者数据',
    followup: '随访数据',
    alert: '告警数据',
    plan: '计划数据',
    other: '其他'
  }
  return typeMap[type] || '其他'
}
</script>

<style scoped>
/* =========================
   仅美化，不改布局（system/DataLog.vue）
   ========================= */

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
  padding: 6px 0 14px 0;
  border-bottom: 1px solid rgba(148, 163, 184, 0.18);
  flex-wrap: wrap;
  gap: 10px 16px;
}

.page-title {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  font-size: 26px;
  font-weight: 800;
  letter-spacing: 0.2px;
  color: #0f172a;
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
  color: #94a3b8;
  margin-top: 2px;
}

.btn:focus,
.btn:focus-visible,
.input:focus-visible,
button:focus-visible,
select:focus-visible,
input:focus-visible,
.detail-modal-close:focus-visible {
  outline: none;
}

.data-log-page {
  /* 仅美化，不改布局（system/DataLog.vue）：修复日志 hover 遮挡裁切 */
  width: 100%;
  max-width: 1120px;
  min-width: 0;
  margin: 0 auto;
  box-sizing: border-box;
  overflow: visible;
  padding-inline: 12px;
  background: transparent;
}

/* 日志外层大卡：取消明显 scale，允许阴影与高光线正常显示 */
.data-log-page > .card {
  overflow: visible;
}

.data-log-page > .card:hover {
  transform: scale(1) !important;
}

.loading {
  text-align: center;
  padding: 40px;
  color: #64748b;
}

.log-item {
  cursor: pointer;
}

.log-item:hover {
  box-shadow: 0 14px 34px rgba(102, 126, 234, 0.10), 0 8px 18px rgba(15, 23, 42, 0.06);
}

.detail-modal-overlay {
  position: fixed;
  left: 0;
  top: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(15, 23, 42, 0.55);
  backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  padding: 20px;
  box-sizing: border-box;
}

.detail-modal {
  width: 720px;
  max-width: 100%;
  max-height: 80vh;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(14px);
  border-radius: 16px;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.22);
  border: 1px solid rgba(226, 232, 240, 0.7);
}

.detail-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.9);
}

.detail-modal-title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.detail-modal-close {
  border: none;
  background: transparent;
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
  color: #64748b;
}

.detail-modal-close:hover {
  color: #0f172a;
}

.detail-modal-body {
  padding: 14px 16px;
  overflow: auto;
  max-height: calc(80vh - 54px);
}

.detail-grid {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.detail-row {
  display: grid;
  grid-template-columns: 120px 1fr;
  gap: 12px;
  align-items: start;
}

.detail-k {
  color: #64748b;
  font-size: 13px;
}

.detail-v {
  color: #0f172a;
  font-size: 13px;
  word-break: break-all;
}

.title-icon {
  font-size: 20px;
  line-height: 1;
  margin-right: 8px;
}

/* 医生建议样式 */
.recommendations-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: 600px;
  overflow-y: auto;
}

.recommendation-item {
  padding: 16px;
  background: rgba(247, 250, 252, 0.6);
  border-radius: 12px;
  border-left: 4px solid #667eea;
  transition: background 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease;
}

.recommendation-item:hover {
  background: rgba(247, 250, 252, 0.9);
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.1);
}

.rec-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.rec-date {
  font-size: 12px;
  color: #718096;
  font-weight: 500;
}

.rec-doctor {
  font-size: 13px;
  color: #667eea;
  font-weight: 600;
  padding: 4px 12px;
  background: rgba(102, 126, 234, 0.1);
  border-radius: 12px;
}

.rec-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.rec-title {
  font-size: 15px;
  font-weight: 600;
  color: #2d3748;
  margin: 0;
}

.rec-description {
  font-size: 14px;
  color: #4a5568;
  line-height: 1.6;
  margin: 0;
}

.rec-patients {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid rgba(226, 232, 240, 0.5);
}

.rec-patients-label {
  font-size: 12px;
  color: #718096;
}

.rec-patients-list {
  font-size: 12px;
  color: #4a5568;
  font-weight: 500;
}

/* 数据分析样式 */
.analysis-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: 600px;
  overflow-y: auto;
}

.analysis-item {
  padding: 16px;
  background: rgba(247, 250, 252, 0.6);
  border-radius: 12px;
  border-left: 4px solid #48bb78;
  transition: all 0.2s ease;
}

.analysis-item:hover {
  background: rgba(247, 250, 252, 0.9);
  transform: translateX(4px);
  box-shadow: 0 2px 8px rgba(72, 187, 120, 0.1);
}

.analysis-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.analysis-date {
  font-size: 12px;
  color: #718096;
  font-weight: 500;
}

.analysis-type {
  font-size: 12px;
  font-weight: 600;
  padding: 4px 12px;
  border-radius: 12px;
}

.type-patient {
  background: rgba(102, 126, 234, 0.1);
  color: #667eea;
}

.type-followup {
  background: rgba(72, 187, 120, 0.1);
  color: #2f855a;
}

.type-alert {
  background: rgba(245, 101, 101, 0.1);
  color: #c53030;
}

.type-plan {
  background: rgba(250, 240, 137, 0.2);
  color: #d69e2e;
}

.type-other {
  background: rgba(160, 174, 192, 0.1);
  color: #718096;
}

.analysis-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.analysis-title {
  font-size: 15px;
  font-weight: 600;
  color: #2d3748;
  margin: 0;
}

.analysis-summary {
  font-size: 14px;
  color: #4a5568;
  line-height: 1.6;
  margin: 0;
}

.analysis-metrics {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid rgba(226, 232, 240, 0.5);
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.metric-item {
  font-size: 13px;
}

.metric-label {
  color: #718096;
}

.metric-value {
  color: #2d3748;
  font-weight: 600;
}

/* 筛选区域样式 */
.filter-section {
  margin-bottom: 24px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  border: 1px solid rgba(226, 232, 240, 0.70);
  box-shadow: 0 10px 30px rgba(102,126,234,0.08), 0 4px 12px rgba(15,23,42,0.04);
  position: relative;
  overflow: visible;
}

.filter-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  opacity: 0.70;
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: flex-end;
}

.filter-item {
  flex: 1;
  min-width: 180px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.filter-label {
  font-size: 13px;
  font-weight: 700;
  color: #6b7280;
}

.date-range {
  display: flex;
  align-items: center;
  gap: 8px;
}

.input-small {
  flex: 1;
  min-width: 120px;
}

.date-separator {
  color: #718096;
  font-weight: 500;
  flex-shrink: 0;
}

.filter-actions {
  display: flex;
  gap: 12px;
  flex-shrink: 0;
}

.filter-actions .btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  font-size: 14px;
  white-space: nowrap;
}

/* 日志列表样式 */
.log-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow: visible;
  padding-left: 10px;
  padding-right: 10px;
}

.log-item {
  padding: 20px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 12px;
  border-left: 4px solid;
  position: relative;
  overflow: hidden;
  transform: scale(1);
  transform-origin: center center;
  will-change: transform, box-shadow;
  transition:
    transform 0.28s cubic-bezier(0.22, 1, 0.36, 1),
    box-shadow 0.28s cubic-bezier(0.22, 1, 0.36, 1),
    border-color 0.28s ease,
    background 0.28s ease;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
}

.log-item::before {
  content: "";
  position: absolute;
  left: 0;
  top: 0;
  width: 100%;
  height: 2px;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  opacity: 0.38;
  transition: opacity 0.28s ease;
  z-index: 1;
  pointer-events: none;
}

.log-item > * {
  position: relative;
  z-index: 2;
}

.log-item:hover {
  transform: scale(1.007);
  box-shadow:
    0 14px 34px rgba(102, 126, 234, 0.10),
    0 8px 18px rgba(15, 23, 42, 0.06);
  border-color: rgba(203, 213, 225, 0.66);
}

.log-item:hover::before {
  opacity: 0.94;
}

.log-type-followup {
  border-left-color: #667eea;
}

.log-type-patient {
  border-left-color: #48bb78;
}

.log-type-alert {
  border-left-color: #e53e3e;
}

.log-type-plan {
  border-left-color: #d69e2e;
}

.log-type-user {
  border-left-color: #764ba2;
}

.log-type-other {
  border-left-color: #718096;
}

.log-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.log-type-badge {
  font-size: 12px;
  font-weight: 800;
  padding: 6px 12px;
  border-radius: 999px;
  letter-spacing: 0.2px;
}

.badge-followup {
  background: rgba(102, 126, 234, 0.1);
  color: #667eea;
}

.badge-patient {
  background: rgba(72, 187, 120, 0.1);
  color: #2f855a;
}

.badge-alert {
  background: rgba(245, 101, 101, 0.1);
  color: #c53030;
}

.badge-plan {
  background: rgba(250, 240, 137, 0.2);
  color: #d69e2e;
}

.badge-user {
  background: rgba(118, 75, 162, 0.1);
  color: #764ba2;
}

.badge-other {
  background: rgba(160, 174, 192, 0.1);
  color: #718096;
}

.log-meta {
  display: flex;
  align-items: center;
  gap: 16px;
}

.log-operator {
  font-size: 14px;
  color: #667eea;
  font-weight: 600;
}

.log-time {
  font-size: 13px;
  color: #718096;
}

.log-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.log-action {
  font-size: 15px;
  font-weight: 600;
  color: #2d3748;
  margin: 0;
}

.log-details {
  padding: 12px;
  background: rgba(247, 250, 252, 0.6);
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 14px;
}

.detail-label {
  color: #718096;
  font-weight: 500;
  flex-shrink: 0;
}

.detail-value {
  color: #2d3748;
  flex: 1;
}

.old-value {
  color: #e53e3e;
  text-decoration: line-through;
}

.new-value {
  color: #2f855a;
  font-weight: 600;
}

.empty-message {
  text-align: center;
  padding: 60px 20px;
  color: #718096;
  font-size: 16px;
}

/* 分页样式 */
.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  margin-top: 24px;
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  border: 1px solid rgba(226, 232, 240, 0.70);
  box-shadow: 0 10px 30px rgba(102,126,234,0.08), 0 4px 12px rgba(15,23,42,0.04);
  flex-wrap: wrap;
  gap: 16px;
}

.page-info {
  font-size: 14px;
  color: #718096;
  font-weight: 500;
}

.page-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-numbers {
  display: flex;
  gap: 4px;
}

.page-btn {
  min-width: 36px;
  height: 36px;
  padding: 0 12px;
  border: 1px solid rgba(226, 232, 240, 0.8);
  background: #ffffff;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #4a5568;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.page-btn:hover:not(:disabled):not(.disabled) {
  background: rgba(102, 126, 234, 0.1);
  border-color: #667eea;
  color: #667eea;
}

.page-btn.active {
  background: rgba(102, 126, 234, 0.12);
  border-color: rgba(102, 126, 234, 0.55);
  color: #4c51bf;
  font-weight: 800;
  box-shadow: 0 10px 20px rgba(102, 126, 234, 0.12);
}

.page-btn.disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.page-btn:disabled {
  cursor: not-allowed;
  opacity: 0.5;
  background: #f7fafc;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .data-log-page {
    min-width: 100%;
  }

  .filter-row {
    flex-direction: column;
  }

  .filter-item {
    min-width: 100%;
  }

  .filter-actions {
    width: 100%;
  }

  .filter-actions .btn {
    flex: 1;
  }

  .pagination {
    flex-direction: column;
    align-items: stretch;
  }

  .page-controls {
    width: 100%;
    justify-content: center;
  }

  .log-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .log-meta {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }
}
</style>
