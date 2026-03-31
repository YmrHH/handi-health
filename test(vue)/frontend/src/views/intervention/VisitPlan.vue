<template>
  <div class="visit-plan-page">
    <div class="page-header">
      <!-- 仅美化，不改布局：标题徽章 + 渐变标题 -->
      <div class="page-title">
        <span class="title-badge" aria-hidden="true">
          <!-- Calendar / Clipboard 风格图标（避免引入新依赖） -->
          <svg viewBox="0 0 24 24" fill="none" class="title-badge-icon">
            <path d="M8 3v3M16 3v3" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
            <path d="M4 8h16" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
            <path d="M6 5h12a2 2 0 0 1 2 2v13a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2Z" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M8 12h8" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
            <path d="M8 16h5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
          </svg>
        </span>
        <span class="page-title-text">复查计划管理</span>
      </div>
      <div class="page-subtitle">复查任务的编辑、下发和完成情况查看</div>
      <div class="header-actions">
        <button class="btn btn-primary" @click="handleAddNew">
          <span>+</span> 新增复查计划
        </button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="card-grid cols-3">
      <div class="card stat-card">
        <div class="stat-header">
          <div class="stat-icon blue">📋</div>
          <div class="stat-info">
            <div class="stat-label">待下发</div>
            <div class="stat-value">{{ stats.pending }}</div>
          </div>
        </div>
      </div>
      <div class="card stat-card">
        <div class="stat-header">
          <div class="stat-icon orange">📤</div>
          <div class="stat-info">
            <div class="stat-label">已下发</div>
            <div class="stat-value">{{ stats.sent }}</div>
          </div>
        </div>
      </div>
      <div class="card stat-card">
        <div class="stat-header">
          <div class="stat-icon green">✅</div>
          <div class="stat-info">
            <div class="stat-label">已复查</div>
            <div class="stat-value">{{ stats.completed }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 筛选区域 -->
    <!-- 仅美化，不改布局：筛选区毛玻璃卡片 -->
    <div class="card filter-section">
      <div class="filter-left">
        <input 
          v-model="searchText" 
          type="text" 
          class="input search-box" 
          placeholder="🔍 搜索患者姓名、医生..."
        />
        <select v-model="filters.doctor" class="input filter-select">
          <option value="">全部医生</option>
          <option v-for="doctor in doctorList" :key="doctor" :value="doctor">{{ doctor }}</option>
        </select>
      </div>
      <div class="filter-right">
        <button class="btn btn-ghost" @click="resetFilters">重置</button>
        <button class="btn btn-primary" @click="loadData">查询</button>
      </div>
    </div>

    <!-- 看板视图（横向翻页） -->
    <div class="kanban-pager">
      <div class="kanban-tabs">
        <button
          v-for="(column, idx) in kanbanColumns"
          :key="column.status"
          type="button"
          class="kanban-tab"
          :class="{ active: idx === boardIndex }"
          @click="boardIndex = idx"
        >
          {{ column.title }}
          <span class="tab-count">{{ getColumnCount(column.status) }}</span>
        </button>
      </div>

      <div class="kanban-board">
        <div class="kanban-track" :style="{ transform: `translateX(-${boardIndex * 100}%)` }">
          <div v-for="column in kanbanColumns" :key="column.status" class="kanban-column">
            <div class="column-header">
              <div class="column-title">
                <span :class="['status-dot', column.color]"></span>
                <span>{{ column.title }}</span>
              </div>
              <div class="column-count">{{ getColumnCount(column.status) }}</div>
            </div>

            <div class="column-content">
              <div v-for="plan in getPaginatedPlans(column.status)" :key="plan.planId" class="kanban-card">
                <div class="card-header">
                  <div class="card-date">
                    <div class="date-day">{{ formatDateDay(plan.planDate) }}</div>
                    <div class="date-month">{{ formatDateMonth(plan.planDate) }}</div>
                  </div>
                  <div class="card-status">
                    <span :class="['status-badge', column.badgeClass]">
                      {{ column.title }}
                    </span>
                  </div>
                </div>
                <div class="card-body">
                  <div class="card-patient">
                    <div class="patient-avatar">{{ plan.patientName.charAt(0) }}</div>
                    <div class="patient-info">
                      <div class="patient-name">{{ plan.patientName }}</div>
                      <div class="patient-disease">{{ plan.disease }}</div>
                    </div>
                  </div>
                  <div class="card-item">
                    <span class="item-icon">🏥</span>
                    <span class="item-text">{{ plan.visitType }}</span>
                  </div>
                  <div class="card-meta">
                    <div class="meta-item">
                      <span class="meta-icon">👨‍⚕️</span>
                      <span class="meta-text">{{ plan.doctor }}</span>
                    </div>
                  </div>
                </div>
                <div class="card-footer">
                  <button v-if="plan.status === 'PENDING'" class="btn-action btn-send" @click.stop="handleSend(plan)">
                    📤 下发
                  </button>
                  <button v-if="plan.status === 'SENT'" class="btn-action btn-complete" @click.stop="handleMarkComplete(plan)">
                    ✅ 标记已复查
                  </button>
                  <button class="btn-action btn-edit" @click.stop="handleEdit(plan)">✏️ 编辑</button>
                </div>
              </div>

              <div v-if="getColumnPlans(column.status).length === 0" class="empty-column">
                <div class="empty-icon">📋</div>
                <div class="empty-text">暂无计划</div>
              </div>
            </div>

            <!-- 分页控件 -->
            <div v-if="getColumnPlans(column.status).length > pageSize" class="column-pagination">
              <div class="pagination-info">
                共 {{ getColumnPlans(column.status).length }} 条，第 {{ getCurrentPage(column.status) }} / {{ getTotalPages(column.status) }} 页
              </div>
              <div class="pagination-controls">
                <button
                  class="pagination-btn"
                  :disabled="getCurrentPage(column.status) <= 1"
                  @click="goToPage(column.status, getCurrentPage(column.status) - 1)"
                >
                  ‹ 上一页
                </button>
                <div class="pagination-numbers">
                  <button
                    v-for="page in getPageNumbers(column.status)"
                    :key="page"
                    class="pagination-number"
                    :class="{ active: page === getCurrentPage(column.status), disabled: page === '...' }"
                    :disabled="page === '...' || page === getCurrentPage(column.status)"
                    @click="page !== '...' && goToPage(column.status, page)"
                  >
                    {{ page }}
                  </button>
                </div>
                <button
                  class="pagination-btn"
                  :disabled="getCurrentPage(column.status) >= getTotalPages(column.status)"
                  @click="goToPage(column.status, getCurrentPage(column.status) + 1)"
                >
                  下一页 ›
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="kanban-page-controls">
        <button class="btn btn-secondary" type="button" :disabled="boardIndex <= 0" @click="boardIndex = boardIndex - 1">
          ‹ 上一页
        </button>
        <div class="page-indicator">{{ boardIndex + 1 }} / {{ kanbanColumns.length }}</div>
        <button
          class="btn btn-secondary"
          type="button"
          :disabled="boardIndex >= kanbanColumns.length - 1"
          @click="boardIndex = boardIndex + 1"
        >
          下一页 ›
        </button>
      </div>
    </div>

    <!-- 编辑弹窗 -->
    <Teleport to="body">
      <div v-if="showEditDialog" class="modal-overlay" @click="closeEditDialog">
        <div class="modal-content edit-modal" @click.stop>
          <div class="modal-header">
            <h3>{{ editForm.planId ? '编辑复查计划' : '新增复查计划' }}</h3>
            <button class="modal-close" @click="closeEditDialog">×</button>
          </div>
          <div class="modal-body">
            <div class="form-grid">
              <div class="form-group">
                <label>患者姓名：*</label>
                <input v-model="editForm.patientName" type="text" class="input" placeholder="请输入患者姓名" />
              </div>
              <div class="form-group">
                <label>疾病诊断：*</label>
                <input v-model="editForm.disease" type="text" class="input" placeholder="请输入疾病诊断" />
              </div>
              <div class="form-group">
                <label>复查类型：*</label>
                <select v-model="editForm.visitType" class="input">
                  <option value="">请选择复查类型</option>
                  <option value="门诊复诊">门诊复诊</option>
                  <option value="专科复查">专科复查</option>
                  <option value="检验复查">检验复查</option>
                  <option value="影像复查">影像复查</option>
                  <option value="随访复诊">随访复诊</option>
                </select>
              </div>
              <div class="form-group">
                <label>计划日期：*</label>
                <input v-model="editForm.planDate" type="date" class="input" />
              </div>
              <div class="form-group">
                <label>责任医生：*</label>
                <select v-model="editForm.doctor" class="input">
                  <option value="">请选择医生</option>
                  <option v-for="doctor in doctorList" :key="doctor" :value="doctor">{{ doctor }}</option>
                </select>
              </div>
              <div class="form-group full-width">
                <label>备注信息：</label>
                <textarea v-model="editForm.remark" class="input" rows="3" placeholder="请输入备注信息（可选）"></textarea>
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn btn-ghost" @click="closeEditDialog">取消</button>
            <button class="btn btn-primary" @click="handleSave" :disabled="!isFormValid">
              {{ editForm.planId ? '保存' : '创建' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { interventionApi, type VisitPlan } from '@/api/intervention'
import { systemApi } from '@/api/system'
import { patientApi } from '@/api/patient'

const loading = ref(false)
const planList = ref<VisitPlan[]>([])
const searchText = ref('')
const showEditDialog = ref(false)
const boardIndex = ref(0)

const editForm = ref({
  planId: 0,
  patientName: '',
  disease: '',
  visitType: '',
  planDate: '',
  doctor: '',
  status: 'PENDING',
  statusText: '待下发',
  remark: ''
})

const filters = ref({
  doctor: ''
})

const stats = ref({
  pending: 0,
  sent: 0,
  completed: 0
})

const doctorList = ref<string[]>([])

// 分页配置：每页固定显示 3 张卡片（左中右一行）
const pageSize = ref(3)
const currentPages = ref<Record<string, number>>({
  PENDING: 1,
  SENT: 1,
  COMPLETED: 1
})

const kanbanColumns = [
  {
    status: 'PENDING',
    title: '待下发',
    color: 'blue',
    badgeClass: 'badge-info'
  },
  {
    status: 'SENT',
    title: '已下发',
    color: 'orange',
    badgeClass: 'badge-warning'
  },
  {
    status: 'COMPLETED',
    title: '已复查',
    color: 'green',
    badgeClass: 'badge-success'
  }
]

const isFormValid = computed(() => {
  return editForm.value.patientName && 
         editForm.value.disease && 
         editForm.value.visitType && 
         editForm.value.planDate && 
         editForm.value.doctor
})

const filteredPlans = computed(() => {
  let filtered = [...planList.value]
  
  if (searchText.value) {
    const keyword = searchText.value.toLowerCase()
    filtered = filtered.filter(p => 
      p.patientName.toLowerCase().includes(keyword) ||
      p.doctor.toLowerCase().includes(keyword)
    )
  }
  
  if (filters.value.doctor) {
    filtered = filtered.filter(p => p.doctor === filters.value.doctor)
  }
  
  return filtered
})

onMounted(() => {
  loadDoctors()
  loadData()
})

async function loadDoctors() {
  try {
    const res = await systemApi.getOrgUser()
    if (res && res.success && res.data) {
      const payload: any = res.data as any
      const raw = (payload.doctorList || payload.userList || []) as any[]
      const names = raw
        .map((x) => {
          if (!x) return ''
          if (typeof x === 'string' || typeof x === 'number') {
            return String(x).trim()
          }
          return String(
            (x.label ?? x.text ?? x.name ?? x.username ?? x.userName ?? x.loginName ?? '')
          ).trim()
        })
        .filter((s) => !!s)
      const uniq = Array.from(new Set(names)).sort()
      if (uniq.length > 0) {
        doctorList.value = uniq
      }
    }
  } catch (e) {
    // ignore, fallback to extracting from plan list
  }
}

function getColumnCount(status: string) {
  return getColumnPlans(status).length
}

function getColumnPlans(status: string) {
  return filteredPlans.value.filter(p => p.status === status).sort((a, b) => {
    const t1 = new Date(a.planDate as any).getTime()
    const t2 = new Date(b.planDate as any).getTime()
    return t1 - t2
  })
}

// 获取分页后的计划列表
function getPaginatedPlans(status: string) {
  const allPlans = getColumnPlans(status)
  const currentPage = currentPages.value[status] || 1
  const start = (currentPage - 1) * pageSize.value
  const end = start + pageSize.value
  return allPlans.slice(start, end)
}

// 获取当前页
function getCurrentPage(status: string): number {
  return currentPages.value[status] || 1
}

// 获取总页数
function getTotalPages(status: string): number {
  const total = getColumnPlans(status).length
  return Math.ceil(total / pageSize.value)
}

// 跳转到指定页
function goToPage(status: string, page: number | string) {
  if (typeof page === 'string') return // 忽略 '...' 字符串
  const totalPages = getTotalPages(status)
  if (page >= 1 && page <= totalPages) {
    currentPages.value[status] = page
  }
}

// 获取页码数组（用于显示分页按钮）
function getPageNumbers(status: string): (number | string)[] {
  const current = getCurrentPage(status)
  const total = getTotalPages(status)
  const pages: (number | string)[] = []

  if (total <= 7) {
    // 如果总页数少于等于7页，显示所有页码
    for (let i = 1; i <= total; i++) {
      pages.push(i)
    }
  } else {
    // 如果总页数大于7页，显示省略号
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

async function loadData() {
  loading.value = true
  try {
    const result = await interventionApi.getVisitPlan()
    if (result.success && result.data) {
      // 转换旧状态到新状态
      planList.value = result.data.map(plan => {
        let newStatus = plan.status
        // 状态映射（兼容后端多套状态）：
        // NOT_REMIND/DUE/PENDING -> PENDING
        // REMIND/NOT_ARRIVED/SENT -> SENT
        // ARRIVED/COMPLETED -> COMPLETED
        const rawStatus = (plan.status || '').toString().trim().toUpperCase()
        if (rawStatus === 'NOT_REMIND' || rawStatus === 'DUE' || rawStatus === 'PENDING') {
          newStatus = 'PENDING'
        } else if (rawStatus === 'REMIND' || rawStatus === 'NOT_ARRIVED' || rawStatus === 'SENT') {
          newStatus = 'SENT'
        } else if (rawStatus === 'ARRIVED' || rawStatus === 'COMPLETED') {
          newStatus = 'COMPLETED'
        }
        return { ...plan, status: newStatus, statusText: getStatusText(newStatus) }
      })
      calculateStats()
      if (!doctorList.value || doctorList.value.length === 0) {
        extractDoctorsFromPlans()
      }
      // 重置分页（如果当前页超出范围，重置到第一页）
      Object.keys(currentPages.value).forEach(status => {
        const totalPages = getTotalPages(status)
        if (currentPages.value[status] > totalPages && totalPages > 0) {
          currentPages.value[status] = 1
        }
      })
    } else {
      planList.value = []
      calculateStats()
      alert(result.message || '加载复查计划失败（接口未返回数据）')
    }
  } catch (error) {
    console.error('加载数据失败:', error)
    planList.value = []
    calculateStats()
    alert((error as any)?.message || '加载复查计划失败')
  } finally {
    loading.value = false
  }
}

function getStatusText(status: string): string {
  const statusMap: Record<string, string> = {
    'PENDING': '待下发',
    'SENT': '已下发',
    'COMPLETED': '已复查'
  }
  return statusMap[status] || '待下发'
}

function calculateStats() {
  stats.value.pending = planList.value.filter(p => p.status === 'PENDING').length
  stats.value.sent = planList.value.filter(p => p.status === 'SENT').length
  stats.value.completed = planList.value.filter(p => p.status === 'COMPLETED').length
}

function extractDoctorsFromPlans() {
  const doctors = new Set<string>()
  planList.value.forEach(plan => {
    if (plan.doctor) {
      doctors.add(plan.doctor)
    }
  })
  doctorList.value = Array.from(doctors).sort()
}

function resetFilters() {
  searchText.value = ''
  filters.value = { doctor: '' }
  // 重置所有列的分页
  currentPages.value = {
    PENDING: 1,
    SENT: 1,
    COMPLETED: 1
  }
}

function handleAddNew() {
  resetEditForm()
  showEditDialog.value = true
}

function formatDateDay(date: string): string {
  return new Date(date).getDate().toString()
}

function formatDateMonth(date: string): string {
  const month = new Date(date).getMonth() + 1
  return `${month}月`
}

function handleSend(plan: VisitPlan) {
  void (async () => {
    try {
      const res = await interventionApi.updateVisitPlanStatus({
        planId: plan.planId,
        newStatus: 'SENT'
      })
      if (!res || !res.success) {
        alert(res?.message || '下发失败')
        return
      }
      alert(`已成功下发复查计划给患者 ${plan.patientName}`)
      await loadData()
    } catch (e: any) {
      alert(e?.message || '下发失败')
    }
  })()
}

function handleMarkComplete(plan: VisitPlan) {
  void (async () => {
    try {
      const res = await interventionApi.updateVisitPlanStatus({
        planId: plan.planId,
        newStatus: 'COMPLETED'
      })
      if (!res || !res.success) {
        alert(res?.message || '更新失败')
        return
      }
      await loadData()
    } catch (e: any) {
      alert(e?.message || '更新失败')
    }
  })()
}

function handleEdit(plan: VisitPlan) {
  editForm.value = {
    planId: plan.planId,
    patientName: plan.patientName,
    disease: plan.disease,
    visitType: plan.visitType,
    planDate: plan.planDate,
    doctor: plan.doctor,
    status: plan.status,
    statusText: plan.statusText,
    remark: plan.remark || ''
  }
  showEditDialog.value = true
}

function closeEditDialog() {
  showEditDialog.value = false
  resetEditForm()
}

function resetEditForm() {
  editForm.value = {
    planId: 0,
    patientName: '',
    disease: '',
    visitType: '',
    planDate: '',
    doctor: '',
    status: 'PENDING',
    statusText: '待下发',
    remark: ''
  }
}

async function handleSave() {
  if (!isFormValid.value) return

  if (editForm.value.planId) {
    alert('当前版本暂不支持编辑后落库（仅前端展示），如需修改请重新创建一条计划。')
    // 仍保留原有的前端编辑体验
    const index = planList.value.findIndex(p => p.planId === editForm.value.planId)
    if (index !== -1) {
      planList.value[index] = {
        ...planList.value[index],
        patientName: editForm.value.patientName,
        disease: editForm.value.disease,
        visitType: editForm.value.visitType,
        planDate: editForm.value.planDate,
        doctor: editForm.value.doctor,
        remark: editForm.value.remark
      }
    }
    closeEditDialog()
    calculateStats()
    return
  }

  // 新增：先根据患者姓名解析 patientId，再调用后端落库接口
  loading.value = true
  try {
    const summary = await patientApi.getPatientSummary({ q: editForm.value.patientName, pageNo: 1, pageSize: 1 })
    const rows: any[] = (summary as any)?.data?.rows || []
    const patientId = rows.length > 0 ? Number(rows[0]?.patientId) : null
    if (!patientId) {
      alert('未找到该患者，请确认患者姓名与系统中一致（建议从患者列表复制姓名）。')
      return
    }

    const createRes = await interventionApi.generateFollowupTasks({
      patientId,
      serviceType: editForm.value.visitType,
      planDate: editForm.value.planDate,
      priority: 'MEDIUM',
      remark: editForm.value.remark
    })

    if (!createRes || !createRes.success) {
      alert(createRes?.message || '创建失败')
      return
    }

    alert('计划创建成功！')
    closeEditDialog()
    await loadData()
  } catch (e: any) {
    alert(e?.message || '创建失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* =========================
   仅美化，不改布局（intervention/VisitPlan.vue）
   ========================= */
.visit-plan-page {
  /* 仅美化，不改布局：第1步仅修复统计卡 hover 越界裁切 */
  width: 100%;
  max-width: 1120px;
  min-width: 0;
  margin: 0 auto;
  padding: 20px 12px;
  box-sizing: border-box;
  overflow: visible;
  background: transparent;
}

/* 统计卡容器：避免父级裁切 hover 放大效果 */
.visit-plan-page .card-grid.cols-3 {
  overflow: visible;
}

.visit-plan-page .card-grid {
  overflow: visible;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  padding: 6px 0 14px 0;
  border-bottom: 1px solid rgba(148, 163, 184, 0.18);
  flex-wrap: wrap;
  gap: 16px;
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
  color: #718096;
  margin-top: 4px;
}

.header-actions {
  margin-left: auto;
}

.btn:focus,
.btn:focus-visible,
.search-box:focus-visible,
.filter-select:focus-visible,
.form-group .input:focus-visible,
.modal-close:focus-visible,
.kanban-tab:focus-visible,
.pagination-btn:focus-visible,
.pagination-number:focus-visible,
.btn-action:focus-visible {
  outline: none;
}

.stat-card {
  padding: 24px 20px;
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

.visit-plan-page .stat-card::before {
  /* 顶部极细高光线：确保渐变条在统计卡 hover 时可见 */
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  width: 100%;
  height: 2px;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  opacity: 0.35;
  transition: opacity 0.28s ease;
  z-index: 1;
  pointer-events: none;
}

.visit-plan-page .stat-card:hover {
  /* 仅美化，不改布局：统计卡 hover 允许极克制 scale */
  transform: scale(1.008);
  box-shadow: 0 18px 40px rgba(102, 126, 234, 0.14), 0 8px 20px rgba(15, 23, 42, 0.06);
  border-color: rgba(102, 126, 234, 0.2);
}

.visit-plan-page .stat-card:hover::before {
  opacity: 0.95;
}

.visit-plan-page .stat-card .stat-header,
.visit-plan-page .stat-card .stat-icon,
.visit-plan-page .stat-card .stat-info {
  position: relative;
  z-index: 2; /* 避免顶部渐变条盖住内容 */
}

.stat-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  flex-shrink: 0;
}

.stat-icon.blue {
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
}

.stat-icon.orange {
  background: linear-gradient(135deg, #fed7aa 0%, #fdba74 100%);
}

.stat-icon.green {
  background: linear-gradient(135deg, #d1fae5 0%, #a7f3d0 100%);
}

.stat-info {
  flex: 1;
  min-width: 0;
}

.stat-label {
  font-size: 13px;
  color: #718096;
  margin-bottom: 6px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #2d3748;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.filter-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  margin-bottom: 24px;
  padding: 20px 24px;
  background: transparent;
  backdrop-filter: none;
  flex-wrap: wrap;
}

.filter-left {
  display: flex;
  gap: 16px;
  flex: 1;
  min-width: 0;
  flex-wrap: wrap;
}

.search-box {
  flex: 1;
  min-width: 200px;
  height: 48px;
  border-radius: 8px;
  border: 2px solid #e2e8f0;
  background: rgba(255, 255, 255, 0.9);
  font-weight: 500;
  transition: all 0.2s ease;
  box-sizing: border-box;
}

.search-box:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.12);
  background: #ffffff;
  outline: none;
}

.filter-select {
  width: 160px;
  min-width: 160px;
  height: 48px;
  border-radius: 8px;
  border: 2px solid #e2e8f0;
  background: rgba(255, 255, 255, 0.9);
  font-weight: 500;
  transition: all 0.2s ease;
  box-sizing: border-box;
}

.filter-select:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.12);
  background: #ffffff;
  outline: none;
}

.filter-right {
  display: flex;
  gap: 12px;
  flex-shrink: 0;
}

.kanban-pager {
  margin-top: 24px;
}

.kanban-tabs {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 14px;
}

.kanban-tab {
  appearance: none;
  border: 2px solid rgba(226, 232, 240, 0.9);
  background: rgba(255, 255, 255, 0.9);
  border-radius: 12px;
  padding: 10px 14px;
  font-weight: 700;
  color: #2d3748;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  transition: all 0.2s ease;
}

.kanban-tab .tab-count {
  min-width: 26px;
  height: 20px;
  padding: 0 8px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 800;
  background: rgba(102, 126, 234, 0.12);
  color: #4c51bf;
}

.kanban-tab.active {
  border-color: rgba(102, 126, 234, 0.9);
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.12);
}

.kanban-board {
  width: 100%;
  overflow: hidden;
  position: relative;
  box-sizing: border-box;
  padding-left: 10px;
  padding-right: 10px;
}

.kanban-track {
  display: flex;
  width: 100%;
  transition: transform 0.25s ease;
}

.kanban-column {
  flex: 0 0 100%;
  width: 100%;
  min-width: 100%;
  box-sizing: border-box;
}

.kanban-column {
  background: rgba(248, 250, 252, 0.8);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 20px;
  min-height: 600px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  transition: box-shadow 0.22s ease, background 0.22s ease, border-color 0.22s ease;
  box-sizing: border-box;
}

.kanban-page-controls {
  margin-top: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.page-indicator {
  font-size: 13px;
  font-weight: 700;
  color: #4a5568;
  min-width: 70px;
  text-align: center;
}

.kanban-column:hover {
  background: rgba(248, 250, 252, 0.95);
  box-shadow: 0 14px 34px rgba(102, 126, 234, 0.10), 0 8px 18px rgba(15, 23, 42, 0.06);
  border-color: rgba(203, 213, 225, 0.65);
}

.column-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 2px solid rgba(226, 232, 240, 0.5);
}

.column-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 700;
  font-size: 15px;
  color: #2d3748;
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  flex-shrink: 0;
}

.status-dot.blue {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.status-dot.orange {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
}

.status-dot.green {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
}

.column-count {
  background: linear-gradient(135deg, #e2e8f0 0%, #cbd5e0 100%);
  color: #4a5568;
  padding: 6px 12px;
  border-radius: 16px;
  font-size: 12px;
  font-weight: 700;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.column-content {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.kanban-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(226, 232, 240, 0.5);
  border-radius: 12px;
  padding: 18px;
  cursor: pointer;
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

/* 空态占满整行（避免落在某一列） */
.empty-column {
  grid-column: 1 / -1;
}

/* 分页区域占满整行 */
.column-pagination {
  grid-column: 1 / -1;
}

.kanban-card::before {
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
  pointer-events: none;
}

.kanban-card:hover {
  box-shadow: 0 14px 34px rgba(102, 126, 234, 0.12), 0 8px 18px rgba(15, 23, 42, 0.06);
  border-color: rgba(102, 126, 234, 0.25);
}

.kanban-card:hover::before {
  opacity: 0.94;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
  gap: 12px;
}

.card-date {
  text-align: center;
  padding: 10px 12px;
  background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%);
  border-radius: 12px;
  min-width: 60px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  flex-shrink: 0;
}

.date-day {
  font-size: 24px;
  font-weight: 700;
  color: #667eea;
  line-height: 1;
}

.date-month {
  font-size: 11px;
  color: #718096;
  margin-top: 4px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.card-status {
  flex-shrink: 0;
}

.status-badge {
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.3px;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.06);
  white-space: nowrap;
}

.badge-info {
  background: linear-gradient(135deg, rgba(219, 234, 254, 0.95) 0%, rgba(191, 219, 254, 0.95) 100%);
  color: #1e3a8a;
  border: 1px solid rgba(147, 197, 253, 0.75);
}

.badge-warning {
  background: linear-gradient(135deg, rgba(254, 243, 199, 0.95) 0%, rgba(253, 230, 138, 0.95) 100%);
  color: #b45309;
  border: 1px solid rgba(245, 158, 11, 0.35);
}

.badge-success {
  background: linear-gradient(135deg, rgba(209, 250, 229, 0.95) 0%, rgba(167, 243, 208, 0.95) 100%);
  color: #047857;
  border: 1px solid rgba(52, 211, 153, 0.45);
}

.card-body {
  margin-bottom: 16px;
}

.card-patient {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding: 12px;
  background: rgba(247, 250, 252, 0.7);
  border-radius: 10px;
  transition: all 0.2s ease;
}

.card-patient:hover {
  background: rgba(247, 250, 252, 1);
}

.patient-avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 16px;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
  flex-shrink: 0;
}

.patient-info {
  flex: 1;
  min-width: 0;
}

.patient-name {
  font-size: 15px;
  font-weight: 700;
  color: #2d3748;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.patient-disease {
  font-size: 12px;
  color: #718096;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding: 10px 12px;
  background: rgba(248, 250, 252, 0.7);
  border-radius: 8px;
  transition: all 0.2s ease;
}

.card-item:hover {
  background: rgba(248, 250, 252, 1);
}

.item-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.item-text {
  font-size: 13px;
  color: #4a5568;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #718096;
  padding: 6px 8px;
  background: rgba(247, 250, 252, 0.5);
  border-radius: 6px;
  font-weight: 500;
}

.meta-icon {
  font-size: 14px;
  flex-shrink: 0;
}

.meta-text {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-footer {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  padding-top: 16px;
  border-top: 1px solid rgba(241, 245, 249, 0.8);
}

.btn-action {
  flex: 1;
  min-width: 80px;
  padding: 8px 12px;
  border: 2px solid #e2e8f0;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 8px;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  backdrop-filter: blur(10px);
  box-sizing: border-box;
}

.btn-send:hover {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  border-color: #f59e0b;
  color: #d97706;
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.2);
}

.btn-complete:hover {
  background: linear-gradient(135deg, #d1fae5 0%, #a7f3d0 100%);
  border-color: #10b981;
  color: #047857;
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.2);
}

.btn-edit:hover {
  background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%);
  border-color: #cbd5e1;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.empty-column {
  text-align: center;
  padding: 60px 20px;
  color: #a0aec0;
  background: rgba(247, 250, 252, 0.5);
  border-radius: 12px;
  border: 2px dashed rgba(160, 174, 192, 0.3);
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
  opacity: 0.5;
}

.empty-text {
  font-size: 14px;
  font-weight: 500;
}

/* 响应式设计 */
@media (max-width: 1400px) {
  /* 横向翻页布局不需要 grid 调整 */
}

@media (max-width: 1024px) {
  .column-content {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .kanban-column {
    padding: 16px;
    min-height: 500px;
  }
}

@media (max-width: 768px) {
  .column-content {
    grid-template-columns: 1fr;
  }

  .visit-plan-page {
    padding: 16px 12px;
  }
  
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .header-actions {
    margin-left: 0;
    width: 100%;
  }
  
  .filter-section {
    flex-direction: column;
    align-items: stretch;
  }
  
  .filter-left {
    flex-direction: column;
  }
  
  .search-box,
  .filter-select {
    width: 100%;
    min-width: 100%;
  }
  
  .filter-right {
    width: 100%;
    justify-content: stretch;
  }
  
  .filter-right .btn {
    flex: 1;
  }
  
  .kanban-column {
    min-height: auto;
  }
}

@media (max-width: 480px) {
  .stat-card {
    padding: 16px;
  }
  
  .stat-icon {
    width: 48px;
    height: 48px;
    font-size: 24px;
  }
  
  .stat-value {
    font-size: 28px;
  }
  
  .card-footer {
    flex-direction: column;
  }
  
  .btn-action {
    width: 100%;
    min-width: 100%;
  }
}

/* 分页样式 */
.column-pagination {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 2px solid rgba(226, 232, 240, 0.5);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.pagination-info {
  font-size: 12px;
  color: #718096;
  font-weight: 500;
  text-align: center;
}

.pagination-controls {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
}

.pagination-btn {
  padding: 8px 16px;
  border: 2px solid #e2e8f0;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #4a5568;
  cursor: pointer;
  transition: all 0.2s ease;
  backdrop-filter: blur(10px);
  min-width: 80px;
}

.pagination-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%);
  border-color: #cbd5e1;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.pagination-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  background: rgba(247, 250, 252, 0.5);
}

.pagination-numbers {
  display: flex;
  gap: 4px;
  align-items: center;
}

.pagination-number {
  min-width: 36px;
  height: 36px;
  padding: 0 12px;
  border: 2px solid #e2e8f0;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #4a5568;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(10px);
}

.pagination-number:hover:not(:disabled):not(.disabled) {
  background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%);
  border-color: #cbd5e1;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.pagination-number.active {
  background: rgba(102, 126, 234, 0.12);
  border-color: rgba(102, 126, 234, 0.55);
  color: #4c51bf;
  font-weight: 800;
  box-shadow: 0 10px 20px rgba(102, 126, 234, 0.12);
}

.pagination-number.disabled {
  cursor: not-allowed;
  opacity: 0.5;
  background: rgba(247, 250, 252, 0.5);
}

.pagination-number:disabled {
  cursor: not-allowed;
  opacity: 0.5;
  background: rgba(247, 250, 252, 0.5);
}

/* 当前页按钮通常会被置为 disabled，需覆盖 disabled 的灰化样式 */
.pagination-number.active:disabled {
  opacity: 1;
  background: rgba(102, 126, 234, 0.12);
  border-color: rgba(102, 126, 234, 0.55);
  color: #4c51bf;
  box-shadow: 0 10px 20px rgba(102, 126, 234, 0.12);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .pagination-controls {
    flex-direction: column;
    gap: 12px;
  }
  
  .pagination-numbers {
    width: 100%;
    justify-content: center;
    flex-wrap: wrap;
  }
  
  .pagination-btn {
    flex: 1;
    min-width: auto;
  }
}
</style>

<style>
/* 全局弹窗样式 */
.modal-overlay {
  position: fixed !important;
  top: 0 !important;
  left: 0 !important;
  right: 0 !important;
  bottom: 0 !important;
  background: rgba(15, 23, 42, 0.55);
  backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999999 !important;
  animation: fadeIn 0.3s ease;
  width: 100vw !important;
  height: 100vh !important;
  box-sizing: border-box;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.modal-content {
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(14px);
  border-radius: 16px;
  width: 90%;
  max-width: 900px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 
    0 24px 60px rgba(15, 23, 42, 0.22),
    0 0 0 1px rgba(255, 255, 255, 0.35);
  border: 1px solid rgba(226, 232, 240, 0.7);
  animation: slideUp 0.3s ease;
  position: relative;
  z-index: 1000000 !important;
  box-sizing: border-box;
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
  flex-shrink: 0;
}

.modal-close:hover {
  background: rgba(226, 232, 240, 0.2);
  color: #718096;
  transform: scale(1.1);
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
  border-radius: 0 0 16px 16px;
  flex-wrap: wrap;
}

/* 编辑弹窗样式 */
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.form-group.full-width {
  grid-column: 1 / -1;
}

.form-group label {
  font-weight: 600;
  color: #2d3748;
  font-size: 14px;
}

.form-group .input {
  padding: 12px 16px;
  border: 2px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
  background: rgba(255, 255, 255, 0.9);
  box-sizing: border-box;
  width: 100%;
}

.form-group .input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
  background: #ffffff;
}

.form-group textarea.input {
  resize: vertical;
  min-height: 100px;
  font-family: inherit;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .modal-content {
    width: 95%;
    max-height: 95vh;
  }
  
  .form-grid {
    grid-template-columns: 1fr;
  }
  
  .modal-footer {
    flex-direction: column-reverse;
  }
  
  .modal-footer .btn {
    width: 100%;
  }
}

@media (max-width: 480px) {
  .modal-header,
  .modal-body,
  .modal-footer {
    padding: 20px;
  }
}
</style>
