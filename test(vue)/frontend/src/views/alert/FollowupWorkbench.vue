<template>
  <div class="followup-workbench-page">
    <div class="page-header">
      <!-- 仅美化，不改布局：标题徽章 + 渐变标题 -->
      <div class="page-title">
        <span class="title-badge" aria-hidden="true">
          <!-- ClipboardList / Activity 风格图标（避免引入新依赖） -->
          <svg viewBox="0 0 24 24" fill="none" class="title-badge-icon">
            <path d="M9 4h6a2 2 0 0 1 2 2v15a2 2 0 0 1-2 2H9a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2Z" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M10 3h4a1.5 1.5 0 0 1 1.5 1.5V6H8.5V4.5A1.5 1.5 0 0 1 10 3Z" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M9.5 11h5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
            <path d="M9.5 15h5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
          </svg>
        </span>
        <span class="page-title-text">进行主动随访</span>
      </div>
      <div class="page-subtitle">{{ activeTab === 'pending' ? '待随访患者列表' : '已随访患者列表' }}</div>
    </div>

    <!-- 切换标签 -->
    <div class="tab-container">
      <button 
        class="tab-btn" 
        :class="{ active: activeTab === 'pending' }"
        @click="activeTab = 'pending'">
        <span class="tab-icon">⏰</span>
        <span class="tab-text">待随访</span>
        <span v-if="activeTab === 'pending'" class="tab-badge">{{ pendingRows.length }}</span>
      </button>
      <button 
        class="tab-btn" 
        :class="{ active: activeTab === 'completed' }"
        @click="activeTab = 'completed'">
        <span class="tab-icon">✅</span>
        <span class="tab-text">已随访</span>
        <span v-if="activeTab === 'completed'" class="tab-badge">{{ completedRows.length }}</span>
      </button>
    </div>

    <!-- 筛选栏 -->
    <!-- 仅美化，不改布局：工具区毛玻璃卡片 -->
    <div class="card filter-card">
      <div class="filter-bar">
        <select v-model="filters.status" class="input">
          <option value="">全部随访状态</option>
          <option value="PENDING">待随访</option>
          <option value="DONE">已就诊</option>
          <option value="CANCELLED">已取消</option>
        </select>

        <select v-model="filters.doctor" class="input">
          <option value="">全部责任医生</option>
          <option v-for="d in data.doctorList" :key="d" :value="d">{{ d }}</option>
        </select>

        <input v-model="filters.startDate" class="input" type="date" />
        <span class="date-sep">至</span>
        <input v-model="filters.endDate" class="input" type="date" />

        <button type="button" class="btn btn-ghost" @click="setQuickDate('week')">本周</button>
        <button type="button" class="btn btn-ghost" @click="setQuickDate('month')">本月</button>

        <input v-model="filters.q" class="input" type="text" placeholder="患者姓名或病案号" />

        <button class="btn btn-primary" @click="handleSearch">查询</button>
        <button class="btn btn-ghost" @click="handleReset">重置</button>
      </div>
    </div>

    <!-- 表格 -->
    <div class="card">
      <div class="card-title">{{ activeTab === 'pending' ? '待随访患者清单' : '已随访患者清单' }}（共 {{ filteredRows.length }} 人）</div>
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else class="table-scroll">
        <table class="table workbench-table">
          <thead>
            <tr>
              <th>病案号</th>
              <th>患者姓名</th>
              <th>风险等级</th>
              <th>手机号</th>
              <th>身份证号</th>
              <th>病种</th>
              <th>就诊日期</th>
              <th>随访类型</th>
              <th>状态</th>
              <th>责任医生</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="filteredAndPaginatedRows.length === 0">
              <td colspan="12" class="table-empty">暂无数据</td>
            </tr>
            <tr v-for="row in filteredAndPaginatedRows" :key="(row as any).taskId ?? row.patientId">
              <td>{{ formatCaseId(row.patientIdNumber || row.patientId) }}</td>
              <td>{{ row.name }}</td>
              <td>
                <span v-if="row.riskLevel" :class="getRiskLevelClass(row.riskLevel)">
                  {{ getRiskLevelText(row.riskLevel) }}
                </span>
                <span v-else>--</span>
              </td>
              <td>{{ row.phone }}</td>
              <td>{{ row.idCard }}</td>
              <td>{{ row.disease || '——' }}</td>
              <td class="cell-muted">{{ row.visitDate }}</td>
              <td>{{ row.followupType || '——' }}</td>
              <td>
                <span
                  class="status-badge"
                  :class="[
                    row.statusText === '待随访' ? 'status-pending'
                      : (row.statusText === '已就诊' || row.statusText === '已完成') ? 'status-done'
                        : row.statusText === '已取消' ? 'status-cancelled'
                          : 'status-pending'
                  ]"
                >
                  {{ row.statusText }}
                </span>
              </td>
              <td><span class="doctor-badge">{{ row.doctor }}</span></td>
              <td class="cell-muted">{{ formatCreatedAt((row as any).createdAt) }}</td>
              <td>
                <RouterLink 
                  v-if="activeTab === 'pending'"
                  :to="`/followup/pending/${row.patientId}`" 
                  class="link-primary">
                  查看详情
                </RouterLink>
                <RouterLink 
                  v-else
                  :to="`/followup/detail/${row.followupRecordId || row.patientId}`" 
                  class="link-primary">
                  查看详情
                </RouterLink>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 分页 -->
      <div v-if="totalPages > 1" class="pagination">
        <div class="page-info">
          共 {{ filteredRows.length }} 条记录，第 {{ pageNo }} / {{ totalPages }} 页
        </div>
        <div class="page-controls">
          <button 
            class="btn btn-secondary" 
            :disabled="pageNo <= 1"
            @click="goToPage(pageNo - 1)">
            上一页
          </button>
          <span class="page-numbers">
            <button
              v-for="page in getPageNumbers()"
              :key="page"
              class="page-btn"
              :class="{ active: page === pageNo, disabled: page === '...' }"
              :disabled="page === '...' || page === pageNo"
              @click="page !== '...' && goToPage(page)">
              {{ page }}
            </button>
          </span>
          <button 
            class="btn btn-secondary" 
            :disabled="pageNo >= totalPages"
            @click="goToPage(pageNo + 1)">
            下一页
          </button>
        </div>
      </div>
    </div>

    <!-- 待随访详情面板 -->
    <Teleport to="body">
      <div v-if="showPendingDetail" class="detail-panel-overlay" @click="closePendingDetail">
        <div class="detail-panel" @click.stop>
          <div class="detail-panel-header">
            <h3>待随访详情</h3>
            <button class="detail-panel-close" @click="closePendingDetail">×</button>
          </div>
          <div class="detail-panel-body" v-if="selectedPendingRow">
            <!-- 患者基本信息 -->
            <div class="detail-section">
              <h4 class="section-title">患者信息</h4>
              <div class="detail-grid">
                <div class="detail-item">
                  <span class="detail-label">患者姓名：</span>
                  <span class="detail-value">{{ selectedPendingRow.name }}</span>
                </div>
                <div class="detail-item">
                  <span class="detail-label">病案号：</span>
                  <span class="detail-value">{{ formatCaseId(selectedPendingRow.patientIdNumber || selectedPendingRow.patientId) }}</span>
                </div>
                <div class="detail-item">
                  <span class="detail-label">风险等级：</span>
                  <span :class="getRiskLevelClass(selectedPendingRow.riskLevel)">
                    {{ getRiskLevelText(selectedPendingRow.riskLevel) }}
                  </span>
                </div>
                <div class="detail-item">
                  <span class="detail-label">责任医生：</span>
                  <span class="detail-value">{{ selectedPendingRow.doctor }}</span>
                </div>
              </div>
            </div>

            <!-- 随访任务信息 -->
            <div class="detail-section">
              <h4 class="section-title">随访任务信息</h4>
              <div class="form-group">
                <label>随访方式 <span class="required">*</span></label>
                <select v-model="pendingDetailForm.followupType" class="input">
                  <option value="">请选择随访方式</option>
                  <option value="电话随访">电话随访</option>
                  <option value="视频随访">视频随访</option>
                  <option value="上门随访">上门随访</option>
                  <option value="门诊随访">门诊随访</option>
                </select>
              </div>

              <div class="form-group">
                <label>计划随访时间 <span class="required">*</span></label>
                <input v-model="pendingDetailForm.planDate" type="date" class="input" />
              </div>

              <div class="form-group">
                <label>随访人员</label>
                <select v-model="pendingDetailForm.staffId" class="input">
                  <option value="">指派人员（科室）</option>
                  <option v-for="staff in staffList" :key="staff.id" :value="staff.id">
                    {{ staff.name }}（{{ staff.department || '——' }}）
                  </option>
                </select>
              </div>

              <div class="form-group">
                <label>随访清单</label>
                <div class="checklist-container">
                  <label v-for="item in followupChecklist" :key="item.id" class="checkbox-label">
                    <input 
                      type="checkbox" 
                      :value="item.id"
                      v-model="pendingDetailForm.checklist"
                      class="checkbox-input"
                    />
                    <span>{{ item.label }}</span>
                  </label>
                </div>
              </div>

              <div class="form-group">
                <label>随访内容/备注</label>
                <textarea 
                  v-model="pendingDetailForm.content" 
                  class="input" 
                  rows="5" 
                  placeholder="请输入随访的具体内容、注意事项等..."
                ></textarea>
              </div>
            </div>

            <div class="detail-panel-footer">
              <button class="btn btn-ghost" @click="closePendingDetail">取消</button>
              <button 
                class="btn btn-primary" 
                @click="savePendingDetail"
                :disabled="!isPendingDetailValid || submittingDetail">
                {{ submittingDetail ? '保存中...' : '保存' }}
              </button>
              <button 
                class="btn btn-success" 
                @click="resendPendingDetail"
                :disabled="!isPendingDetailValid || submittingDetail">
                {{ submittingDetail ? '下发中...' : '重新下发' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch, Teleport } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import type { FollowupWorkbenchData, FollowupWorkbenchFilter, FollowupWorkbenchRow } from '@/api/alert'
import { followupApi, type StaffListRow } from '@/api/followup'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const pageNo = ref(1)
const pageSize = 10
const activeTab = ref<'pending' | 'completed'>(
  (route.query.tab as 'pending' | 'completed') || 'pending'
)

// 待随访详情相关
const showPendingDetail = ref(false)
const selectedPendingRow = ref<FollowupWorkbenchRow | null>(null)
const submittingDetail = ref(false)
const staffList = ref<Array<{ id: number | string; name: string; department?: string }>>([])

// 随访清单选项
const followupChecklist = ref([
  { id: 'blood_pressure', label: '血压监测' },
  { id: 'blood_sugar', label: '血糖监测' },
  { id: 'medication', label: '用药情况' },
  { id: 'diet', label: '饮食控制' },
  { id: 'exercise', label: '运动情况' },
  { id: 'symptoms', label: '症状询问' },
  { id: 'life_quality', label: '生活质量评估' }
])

// 待随访详情表单
const pendingDetailForm = ref({
  followupType: '',
  planDate: '',
  staffId: '' as string | number,
  checklist: [] as string[],
  content: ''
})

const isPendingDetailValid = computed(() => {
  return pendingDetailForm.value.followupType && pendingDetailForm.value.planDate
})

const filters = ref<FollowupWorkbenchFilter>({
  status: '',
  doctor: '',
  startDate: '',
  endDate: '',
  q: ''
})

// 存储所有数据（不筛选）
const allRows = ref<FollowupWorkbenchRow[]>([])

const data = ref<FollowupWorkbenchData>({
  statusParam: null,
  doctorParam: null,
  startDate: null,
  endDate: null,
  keyword: null,
  pageNo: 1,
  totalPages: 1,
  total: 0,
  doctorList: [],
  rows: []
})

function normalizeTaskStatus(raw?: any): 'PENDING' | 'DONE' | 'CANCELLED' | '' {
  if (raw == null) return ''
  const s = String(raw).trim().toUpperCase()
  if (!s) return ''
  if (s === 'PENDING') return 'PENDING'
  if (s === 'ASSIGNED' || s === 'IN_PROGRESS' || s === 'TODO') return 'PENDING'
  if (s === 'DONE' || s === 'COMPLETED' || s === 'FINISHED' || s === 'SUCCESS') return 'DONE'
  if (s === 'CANCELLED' || s === 'CANCELED') return 'CANCELLED'
  return ''
}

function taskStatusText(status?: string | null): string {
  const s = (status || '').trim().toUpperCase()
  if (s === 'PENDING') return '待随访'
  if (s === 'DONE') return '已就诊'
  if (s === 'CANCELLED') return '已取消'
  return status || ''
}

// 根据当前标签分离数据
const pendingRows = computed(() => {
  return allRows.value.filter(row => 
    row.statusText === '待随访' || row.status === 'PENDING'
  )
})

const completedRows = computed(() => {
  return allRows.value.filter(row => 
    row.statusText === '已就诊' || row.status === 'DONE' || row.statusText === '已完成'
  )
})

// 前端筛选后的数据
const filteredRows = computed(() => {
  // 根据当前标签选择数据源
  const sourceRows = activeTab.value === 'pending' ? pendingRows.value : completedRows.value
  let filtered = [...sourceRows]
  
  // 状态筛选（只在已随访列表中有效，待随访列表不需要）
  if (filters.value.status && activeTab.value === 'completed') {
    const statusMap: Record<string, string> = {
      'PENDING': '待随访',
      'DONE': '已就诊',
      'CANCELLED': '已取消'
    }
    const statusText = statusMap[filters.value.status]
    if (statusText) {
      filtered = filtered.filter(row => row.statusText === statusText)
    }
  }
  
  // 责任医生筛选
  if (filters.value.doctor) {
    filtered = filtered.filter(row => row.doctor === filters.value.doctor)
  }
  
  // 日期范围筛选
  if (filters.value.startDate) {
    filtered = filtered.filter(row => {
      if (!row.visitDate) return false
      return row.visitDate >= filters.value.startDate!
    })
  }
  
  if (filters.value.endDate) {
    filtered = filtered.filter(row => {
      if (!row.visitDate) return false
      return row.visitDate <= filters.value.endDate!
    })
  }
  
  // 关键词搜索（如果有）
  if (filters.value.q) {
    const keyword = filters.value.q.toLowerCase()
    filtered = filtered.filter(row => 
      (row.name && row.name.toLowerCase().includes(keyword)) ||
      (row.phone && row.phone.includes(keyword)) ||
      (row.idCard && row.idCard.includes(keyword)) ||
      (row.disease && row.disease.toLowerCase().includes(keyword))
    )
  }
  
  return filtered
})

// 分页后的数据
const filteredAndPaginatedRows = computed(() => {
  const start = (pageNo.value - 1) * pageSize
  const end = start + pageSize
  return filteredRows.value.slice(start, end)
})

// 总页数
const totalPages = computed(() => {
  return Math.ceil(filteredRows.value.length / pageSize)
})

onMounted(() => {
  loadData()
  loadStaffList()

  ;(window as any).__followupWorkbench = {
    allRows,
    pendingRows,
    completedRows,
    filteredRows,
    filteredAndPaginatedRows,
    activeTab,
    filters,
    pageNo
  }

  console.error('[FollowupWorkbench] mounted')
  
  // 监听路由变化，如果有 refresh 参数则刷新数据
  watch(() => route.query.refresh, (refresh) => {
    if (refresh === 'true') {
      loadData()
      // 清除 refresh 参数
      router.replace({ query: { ...route.query, refresh: undefined } })
    }
  }, { immediate: true })
})

async function loadData() {
  loading.value = true
  try {
    const result = await followupApi.getWorkbenchTasks({
      page: 1,
      pageSize: 200
    })

    const payload = result.success ? (result.data as any) : null
    const rowsRaw = (payload?.rows || []) as any[]

    const rows: FollowupWorkbenchRow[] = Array.isArray(rowsRaw)
      ? rowsRaw.map((r: any) => {
          const taskId = r.taskId ?? r.task_id ?? r.id
          const patientId = r.patientId ?? r.patient_id
          const patientName = r.patientName ?? r.patient_name
          const planTime = r.planTime ?? r.dueAt ?? r.due_at
          const createdAt = r.createdAt ?? r.created_at ?? r.taskCreatedAt ?? r.task_created_at
          const riskLevel = r.riskLevel ?? r.risk_level
          const status = normalizeTaskStatus(r.status)

          const planStr = planTime ? String(planTime).replace('T', ' ') : ''
          const visitDate = planStr ? planStr.slice(0, 10) : ''
          const visitTime = planStr ? planStr.slice(11, 19) : ''

          return {
            taskId: Number(taskId) || undefined,
            patientId: Number(patientId) || 0,
            patientIdNumber: Number(patientId) || 0,
            name: patientName || '',
            phone: (r.patientPhone ?? r.phone ?? '') as any,
            idCard: (r.idCard ?? '') as any,
            symptom: '' as any,
            systemSyndrome: '' as any,
            visitDate: visitDate as any,
            visitTime: visitTime as any,
            status: status as any,
            statusText: taskStatusText(status),
            doctor: (r.doctorName ?? r.doctor ?? '') as any,
            disease: (r.mainDisease ?? r.disease ?? '') as any,
            followupType: normalizeFollowupType(r.followMethod ?? r.triggerType ?? r.followupType ?? ''),
            riskLevel: riskLevel as any,
            followupRecordId: r.followupRecordId ?? undefined,
            createdAt: createdAt ? String(createdAt).replace('T', ' ') : ''
          }
        })
      : []

    allRows.value = rows
    console.error('[FollowupWorkbench] loaded', {
      success: result.success,
      rawCount: Array.isArray(rowsRaw) ? rowsRaw.length : -1,
      mappedCount: rows.length,
      pendingCount: rows.filter(r => (r as any).status === 'PENDING' || (r as any).statusText === '待随访').length,
      completedCount: rows.filter(r => (r as any).status === 'DONE' || (r as any).statusText === '已就诊').length
    })
    data.value.doctorList = []
    data.value.total = Number(payload?.total ?? allRows.value.length) || 0
    data.value.totalPages = 1
    data.value.pageNo = 1
  } catch (error) {
    console.error('加载数据失败:', error)
    allRows.value = []
    data.value.doctorList = []
    data.value.total = 0
    data.value.totalPages = 1
    data.value.pageNo = 1
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  // 前端筛选，不需要调用 API，computed 会自动响应筛选条件变化
  pageNo.value = 1
}

function handleReset() {
  filters.value = {
    status: '',
    doctor: '',
    startDate: '',
    endDate: '',
    q: ''
  }
  pageNo.value = 1
}

function setQuickDate(type: 'week' | 'month') {
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  
  if (type === 'week') {
    // 本周：从周一到今天
    const dayOfWeek = today.getDay()
    const mondayOffset = dayOfWeek === 0 ? -6 : 1 - dayOfWeek
    const monday = new Date(today)
    monday.setDate(today.getDate() + mondayOffset)
    
    filters.value.startDate = monday.toISOString().split('T')[0]
    filters.value.endDate = today.toISOString().split('T')[0]
  } else if (type === 'month') {
    // 本月：从月初到今天
    const firstDay = new Date(today.getFullYear(), today.getMonth(), 1)
    
    filters.value.startDate = firstDay.toISOString().split('T')[0]
    filters.value.endDate = today.toISOString().split('T')[0]
  }
  
  // 触发搜索
  handleSearch()
}

function goToPage(page: number | string) {
  if (typeof page === 'string' || page < 1 || page > totalPages.value) return
    pageNo.value = page
}

// 切换标签时重置分页
watch(() => activeTab.value, () => {
  pageNo.value = 1
})

function getPageNumbers(): (number | string)[] {
  const current = pageNo.value
  const total = totalPages.value
  const pages: (number | string)[] = []
  
  if (total <= 7) {
    // 如果总页数少于等于7页，显示所有页码
    for (let i = 1; i <= total; i++) {
      pages.push(i)
    }
  } else {
    // 总页数大于7页，显示省略号
    if (current <= 4) {
      // 当前页在前4页
      for (let i = 1; i <= 5; i++) {
        pages.push(i)
      }
      pages.push('...')
      pages.push(total)
    } else if (current >= total - 3) {
      // 当前页在后4页
      pages.push(1)
      pages.push('...')
      for (let i = total - 4; i <= total; i++) {
        pages.push(i)
      }
    } else {
      // 当前页在中间
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

function formatCaseId(id?: number | string | null) {
  if (id === undefined || id === null) return '--'
  const num = Number(id)
  if (Number.isNaN(num)) return String(id)
  return `1${String(Math.floor(num)).padStart(3, '0')}`
}

function getRiskLevelClass(level?: string | null): string {
  if (!level) return 'badge'
  const text = level.toUpperCase()
  if (text.includes('HIGH') || text.includes('高')) return 'badge badge-danger'
  if (text.includes('MED') || text.includes('中')) return 'badge badge-warning'
  if (text.includes('LOW') || text.includes('低')) return 'badge badge-success'
  return 'badge'
}

function getRiskLevelText(level?: string | null): string {
  if (!level) return '--'
  const text = level.toUpperCase()
  if (text.includes('HIGH')) return '高危'
  if (text.includes('MED')) return '中危'
  if (text.includes('LOW')) return '低危'
  return level
}

function normalizeFollowupType(v: any): string {
  if (v == null) return ''
  const s = String(v).trim()
  if (!s) return ''
  // 兼容：后端可能返回 code（PHONE/VIDEO/HOME/CLINIC）或中文
  const up = s.toUpperCase()
  if (up === 'PHONE' || up === 'TEL' || up === 'TELEPHONE') return '电话随访'
  if (up === 'VIDEO') return '视频随访'
  if (up === 'HOME' || up === 'HOME_VISIT') return '上门随访'
  if (up === 'CLINIC' || up === 'OUTPATIENT') return '门诊随访'
  return s
}

function formatCreatedAt(v: any): string {
  if (!v) return '--'
  return String(v).replace('T', ' ')
}

// 加载随访人员列表
async function loadStaffList() {
  try {
    const result = await followupApi.getStaffList({ pageNo: 1 })
    if (result.success && result.data) {
      const rows: StaffListRow[] = result.data.rows || []
      staffList.value = rows
        .filter(row => row.id || row.staffId)
        .map(row => ({
          id: row.id || row.staffId || 0,
          name: row.name,
          department: row.department
        }))
    }
  } catch (error) {
    console.error('加载随访人员列表失败:', error)
  }
}

// 打开待随访详情
function openPendingDetail(row: FollowupWorkbenchRow) {
  selectedPendingRow.value = row
  
  // 初始化表单数据（如果有已保存的数据，则加载；否则使用默认值）
  const tomorrow = new Date()
  tomorrow.setDate(tomorrow.getDate() + 1)
  
  pendingDetailForm.value = {
    followupType: row.followupType || '',
    planDate: (row as any).planDate || tomorrow.toISOString().split('T')[0],
    staffId: (row as any).staffId || '',
    checklist: (row as any).checklist || [],
    content: (row as any).followupContent || (row as any).content || ''
  }
  
  showPendingDetail.value = true
}

// 关闭待随访详情
function closePendingDetail() {
  showPendingDetail.value = false
  selectedPendingRow.value = null
  // 重置表单
  pendingDetailForm.value = {
    followupType: '',
    planDate: '',
    staffId: '',
    checklist: [],
    content: ''
  }
}

// 保存待随访详情
async function savePendingDetail() {
  if (!isPendingDetailValid.value || !selectedPendingRow.value) return
  
  submittingDetail.value = true
  try {
    // 模拟保存API调用
    await new Promise(resolve => setTimeout(resolve, 500))
    
    // 更新列表中的数据
    const row = allRows.value.find(r => r.patientId === selectedPendingRow.value?.patientId)
    if (row) {
      (row as any).followupType = pendingDetailForm.value.followupType
      ;(row as any).planDate = pendingDetailForm.value.planDate
      ;(row as any).staffId = pendingDetailForm.value.staffId
      ;(row as any).checklist = pendingDetailForm.value.checklist
      ;(row as any).followupContent = pendingDetailForm.value.content
      ;(row as any).content = pendingDetailForm.value.content
      
      // 如果有随访人员，更新显示
      if (pendingDetailForm.value.staffId) {
        const staff = staffList.value.find(s => s.id === pendingDetailForm.value.staffId)
        if (staff) {
          ;(row as any).staffName = staff.name
        }
      }
    }
    
    alert('保存成功！')
    closePendingDetail()
  } catch (error: any) {
    console.error('保存失败:', error)
    alert('保存失败：' + (error.message || '未知错误'))
  } finally {
    submittingDetail.value = false
  }
}

// 重新下发给随访人员
async function resendPendingDetail() {
  if (!isPendingDetailValid.value || !selectedPendingRow.value) return
  
  const confirmed = confirm(
    `确定要重新下发此随访任务吗？\n\n` +
    `患者：${selectedPendingRow.value.name}\n` +
    `随访方式：${pendingDetailForm.value.followupType}\n` +
    `计划时间：${pendingDetailForm.value.planDate}\n\n` +
    `任务将重新下发给随访人员。`
  )
  
  if (!confirmed) return
  
  submittingDetail.value = true
  try {
    // 先保存
    await savePendingDetail()
    
    // 模拟下发API调用
    await new Promise(resolve => setTimeout(resolve, 500))
    
    const staffName = pendingDetailForm.value.staffId 
      ? staffList.value.find(s => s.id === pendingDetailForm.value.staffId)?.name || '未指派'
      : '未指派'
    
    alert(
      `随访任务已重新下发！\n\n` +
      `患者：${selectedPendingRow.value.name}\n` +
      `随访人员：${staffName}\n` +
      `随访方式：${pendingDetailForm.value.followupType}\n` +
      `计划时间：${pendingDetailForm.value.planDate}`
    )
    
    closePendingDetail()
  } catch (error: any) {
    console.error('下发失败:', error)
    alert('下发失败：' + (error.message || '未知错误'))
  } finally {
    submittingDetail.value = false
  }
}
</script>

<style scoped>
/* =========================
   仅美化，不改布局（FollowupWorkbench.vue）
   ========================= */

.followup-workbench-page {
  /* 固定页面宽度，避免随浏览器宽度自适应缩放；放大时通过横向滚动查看 */
  width: 1120px;
  min-width: 1120px;
  margin: 0 auto;
  background: transparent;
  padding: 20px;
  box-sizing: border-box;
  overflow-x: auto;
  overflow-y: visible;
  -webkit-overflow-scrolling: touch;
}

/* 页面标题区 */
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

/* 卡片统一（16px 体系 + 毛玻璃 + 细高光线） */
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

.card:hover::before {
  opacity: 0.94;
}

.card > * {
  position: relative;
  z-index: 2;
}

.card-title {
  font-weight: 700;
  font-size: 16px;
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

/* 按钮统一（主/次/危险） */
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

/* 表格区域：参考个性化建议下发页面的布局，保证清单内容完整显示 */
.followup-workbench-page .table-scroll {
  width: 100%;
  overflow-x: auto;
  overflow-y: visible;
  -webkit-overflow-scrolling: touch;
}

.followup-workbench-page .workbench-table {
  min-width: 1100px;
  width: 100%;
}

.followup-workbench-page .workbench-table th,
.followup-workbench-page .workbench-table td {
  padding: 10px 12px;
  font-size: 13px;
  white-space: nowrap;
}

.followup-workbench-page .workbench-table thead th {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.08) 0%, rgba(118, 75, 162, 0.06) 100%);
  font-size: 12.5px;
  font-weight: 700;
  color: #64748b;
  border-bottom: 1px solid rgba(226, 232, 240, 0.85);
  letter-spacing: 0.2px;
}

.followup-workbench-page .workbench-table tbody td {
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
  font-weight: 600;
}

/* 各列最小宽度设置，避免被挤掉 */
.workbench-table th:nth-child(1),
.workbench-table td:nth-child(1) { min-width: 90px; }   /* 病案号 */
.workbench-table th:nth-child(2),
.workbench-table td:nth-child(2) { min-width: 100px; }  /* 患者姓名 */
.workbench-table th:nth-child(3),
.workbench-table td:nth-child(3) { min-width: 90px; }   /* 风险等级 */
.workbench-table th:nth-child(4),
.workbench-table td:nth-child(4) { min-width: 120px; }  /* 手机号 */
.workbench-table th:nth-child(5),
.workbench-table td:nth-child(5) { min-width: 160px; }  /* 身份证号 */
.workbench-table th:nth-child(6),
.workbench-table td:nth-child(6) { min-width: 110px; }  /* 病种 */
.workbench-table th:nth-child(7),
.workbench-table td:nth-child(7) { min-width: 110px; }  /* 就诊日期 */
.workbench-table th:nth-child(8),
.workbench-table td:nth-child(8) { min-width: 100px; }  /* 随访类型 */
.workbench-table th:nth-child(9),
.workbench-table td:nth-child(9) { min-width: 90px; }   /* 状态 */
.workbench-table th:nth-child(10),
.workbench-table td:nth-child(10) { min-width: 110px; } /* 责任医生 */
.workbench-table th:nth-child(11),
.workbench-table td:nth-child(11) { min-width: 140px; } /* 创建时间 */
.workbench-table th:nth-child(12),
.workbench-table td:nth-child(12) { min-width: 90px; }  /* 操作 */

/* 标签按钮容器 */
.tab-container {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
  padding: 8px;
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  box-shadow: 0 10px 30px rgba(102,126,234,0.08), 0 4px 12px rgba(15,23,42,0.04);
  border: 1px solid rgba(226, 232, 240, 0.70);
  width: fit-content;
  position: relative;
}

/* 标签按钮 */
.tab-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 14px 32px;
  background: transparent;
  border: 2px solid transparent;
  border-radius: 12px;
  color: #718096;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.22s ease;
  position: relative;
  overflow: hidden;
  white-space: nowrap;
  min-width: 140px;
}

.tab-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.05) 0%, rgba(118, 75, 162, 0.05) 100%);
  opacity: 0;
  transition: opacity 0.3s ease;
  border-radius: 12px;
}

.tab-btn:hover::before {
  opacity: 1;
}

.tab-btn:hover {
  color: #667eea;
}

.tab-btn.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
  border-color: transparent;
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.35);
}

.tab-btn.active::before {
  opacity: 0;
}

.tab-icon {
  font-size: 18px;
  line-height: 1;
  filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.1));
}

.tab-btn.active .tab-icon {
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.2));
}

.tab-text {
  font-weight: 600;
  letter-spacing: 0.3px;
}

.tab-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  height: 24px;
  padding: 0 8px;
  background: rgba(255, 255, 255, 0.25);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  font-size: 12px;
  font-weight: 700;
  color: #ffffff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.15);
  margin-left: 4px;
  animation: slideIn 0.3s ease;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: scale(0.8) translateX(-10px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateX(0);
  }
}

.filter-bar {
  display: flex;
  gap: 12px;
  flex-wrap: nowrap;
  align-items: center;
  overflow-x: auto;
  padding: 16px 20px;
  background: transparent;
  backdrop-filter: none;
  border-radius: 16px;
  border: none;
}

.filter-bar .input {
  height: 44px;
  border-radius: 8px;
  border: 2px solid #e2e8f0;
  background: rgba(255, 255, 255, 0.9);
  font-weight: 500;
  transition: all 0.2s ease;
}

.filter-bar .input:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.12);
  background: #ffffff;
}

.filter-bar select.input {
  cursor: pointer;
}

.filter-bar span {
  font-size: 12px;
  color: #718096;
  font-weight: 500;
  white-space: nowrap;
}

.date-sep {
  font-size: 12px;
  color: #94a3b8;
  font-weight: 600;
  margin: 0 5px;
}

.flex-1 {
  flex: 1;
}

.pagination {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  margin-top: 24px;
  padding: 20px 24px;
  background: transparent;
  backdrop-filter: none;
  border-radius: 16px;
  border: none;
}

.page-info {
  color: #4a5568;
  font-size: 14px;
  font-weight: 500;
}

.page-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-numbers {
  display: flex;
  align-items: center;
  gap: 6px;
}

.page-btn {
  min-width: 36px;
  height: 36px;
  padding: 0 10px;
  border: 2px solid #e2e8f0;
  background: rgba(255, 255, 255, 0.9);
  color: #4a5568;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  backdrop-filter: blur(10px);
}

.page-btn:hover:not(.disabled):not(.active) {
  background: #ffffff;
  border-color: #cbd5e0;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.12);
}

.page-btn.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-color: transparent;
  color: #ffffff;
  box-shadow: 0 4px 14px 0 rgba(102, 126, 234, 0.39);
}

.page-btn.disabled {
  cursor: default;
  color: #a0aec0;
  opacity: 0.6;
}

.btn-secondary {
  padding: 10px 18px;
  border: 2px solid #e2e8f0;
  background: rgba(255, 255, 255, 0.9);
  color: #4a5568;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  backdrop-filter: blur(10px);
}

.btn-secondary:hover:not(:disabled) {
  background: #ffffff;
  border-color: #cbd5e0;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.12);
}

.btn-secondary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.link-primary {
  color: #667eea;
  font-weight: 600;
  text-decoration: none;
  transition: all 0.2s ease;
  position: relative;
}

.link-primary::after {
  content: '';
  position: absolute;
  width: 0;
  height: 2px;
  bottom: -2px;
  left: 0;
  background: linear-gradient(90deg, #667eea, #764ba2);
  transition: width 0.3s ease;
}

.link-primary:hover {
  color: #764ba2;
}

.link-primary:hover::after {
  width: 100%;
}

.detail-panel-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.detail-panel {
  width: 92%;
  max-width: 920px;
  max-height: 90vh;
  overflow: auto;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  border: 1px solid rgba(226, 232, 240, 0.80);
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
}

.detail-panel-header {
  position: relative;
  padding: 16px 24px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.75);
}

.detail-panel-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
  color: #1f2937;
}

.detail-panel-close {
  position: absolute;
  top: 12px;
  right: 18px;
  appearance: none;
  border: none;
  background: transparent;
  font-size: 20px;
  line-height: 1;
  cursor: pointer;
  color: #94a3b8;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  transition: all 0.22s ease;
}

.detail-panel-close:hover {
  background: rgba(226, 232, 240, 0.45);
  color: #475569;
}

.detail-panel-close:focus,
.detail-panel-close:focus-visible {
  outline: none;
}

.detail-panel-body {
  padding: 20px 24px 16px;
}

.detail-panel-footer {
  padding: 12px 24px 18px;
  border-top: 1px solid rgba(226, 232, 240, 0.75);
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  background: rgba(248, 250, 252, 0.6);
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 700;
  color: #334155;
}

.form-group .input {
  width: 100%;
  height: 44px;
  border-radius: 8px;
  border: 2px solid #e2e8f0;
  padding: 10px 12px;
  background: rgba(255, 255, 255, 0.9);
  font-weight: 500;
  transition: all 0.22s ease;
  box-sizing: border-box;
}

.form-group textarea.input {
  height: auto;
  resize: vertical;
}

.form-group .input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.12);
  background: #ffffff;
}

.required {
  color: #e53e3e;
}

.checklist-container {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 12px;
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(248, 250, 252, 0.7);
  border: 1px solid rgba(226, 232, 240, 0.75);
}

.checkbox-label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #334155;
  font-weight: 600;
}

.checkbox-input {
  width: 16px;
  height: 16px;
  accent-color: #667eea;
}
.loading {
  text-align: center;
  padding: 60px 20px;
  color: #718096;
  font-size: 16px;
  font-weight: 500;
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

.badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 6px 12px;
  min-height: 28px;
  border-radius: 999px;
  font-size: 12.5px;
  font-weight: 700;
  text-transform: uppercase;
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

.badge-success {
  background: linear-gradient(135deg, #c6f6d5 0%, #9ae6b4 100%);
  color: #065f46;
  border-color: rgba(16, 185, 129, 0.38);
}

/* 表格增强样式 */
.table {
  background: transparent;
  backdrop-filter: none;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: none;
  border: none;
}

.table th {
  font-weight: 700;
  color: #64748b;
  font-size: 12px;
  text-transform: none;
  letter-spacing: 0.2px;
  padding: 14px 16px;
}

.table td {
  padding: 12px 16px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.45);
}

.table tbody tr {
  transition: all 0.22s ease;
}

.table tbody tr:hover {
  background: rgba(102, 126, 234, 0.05);
}

/* 保持表格结构，不强制斑马纹，避免影响可读性 */

/* 状态/责任医生信息块（不改字段位置） */
.doctor-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  min-height: 28px;
  border-radius: 999px;
  background: rgba(102, 126, 234, 0.08);
  border: 1px solid rgba(102, 126, 234, 0.18);
  color: #334155;
  font-weight: 600;
  line-height: 1.2;
}

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

.status-pending {
  background: linear-gradient(135deg, rgba(254, 249, 195, 0.95) 0%, rgba(253, 230, 138, 0.55) 100%);
  color: #92400e;
  border-color: rgba(245, 158, 11, 0.45);
}

.status-done {
  background: linear-gradient(135deg, rgba(209, 250, 229, 0.92) 0%, rgba(187, 247, 208, 0.68) 100%);
  color: #065f46;
  border-color: rgba(16, 185, 129, 0.38);
}

.status-cancelled {
  background: rgba(226, 232, 240, 0.75);
  color: #475569;
  border-color: rgba(203, 213, 225, 0.85);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .followup-workbench-page {
    padding: 12px;
  }
  
  .filter-bar {
    padding: 16px 20px;
    gap: 8px;
  }
  
  .filter-bar .input {
    min-width: 120px;
    font-size: 12px;
  }
  
  .pagination {
    padding: 16px 20px;
  }
  
  .page-controls {
    flex-direction: column;
    gap: 12px;
  }
  
  .table {
    font-size: 12px;
  }
  
  .table th,
  .table td {
    padding: 12px 14px;
  }
}

@media (max-width: 480px) {
  .filter-bar {
    flex-direction: column;
    align-items: stretch;
  }
  
  .filter-bar .input {
    min-width: auto;
  }
  
  .page-numbers {
    flex-wrap: wrap;
    justify-content: center;
  }
}
</style>
