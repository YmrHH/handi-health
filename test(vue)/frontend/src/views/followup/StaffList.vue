<template>
  <div class="staff-list-page">
    <div class="page-header">
      <!-- 仅美化，不改布局：标题徽章 + 渐变标题 -->
      <div class="page-title">
        <span class="title-badge" aria-hidden="true">
          <!-- Users / UserCheck 风格图标（避免引入新依赖） -->
          <svg viewBox="0 0 24 24" fill="none" class="title-badge-icon">
            <path d="M16 21v-1a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v1" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M9 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M17 11l1.2 1.2L21 9.4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </span>
        <span class="page-title-text">随访人员管理</span>
      </div>
      <div class="page-subtitle">查看和管理随访人员信息</div>
    </div>

    <!-- 筛选栏 -->
    <!-- 仅美化，不改布局：筛选区毛玻璃卡片 -->
    <div class="card filter-card">
      <div class="filter-bar">
        <input v-model="filters.staffName" class="input" type="text" placeholder="随访人员姓名" />
        <select v-model="filters.department" class="input">
          <option value="">全部部门</option>
          <option v-for="dept in departmentList" :key="dept" :value="dept">{{ dept }}</option>
        </select>
        <select v-model="filters.status" class="input">
          <option value="">全部状态</option>
          <option value="在职">在职</option>
          <option value="离职">离职</option>
        </select>
        <button class="btn btn-primary" @click="handleSearch">查询</button>
        <button class="btn btn-ghost" @click="handleReset">重置</button>
      </div>
    </div>

    <!-- 表格 -->
    <div class="card">
      <div class="card-header">
        <span class="card-title">随访人员列表</span>
        <span class="card-subtitle">共 {{ filteredRows.length }} 人</span>
        <button class="btn btn-primary" @click="openRegisterModal">注册随访人员</button>
      </div>
      <div v-if="loading" class="loading">加载中...</div>
      <table v-else class="table">
        <thead>
          <tr>
            <th>序号</th>
            <th>姓名</th>
            <th>工号</th>
            <th>部门</th>
            <th>联系电话</th>
            <th>负责患者数</th>
            <th>随访总次数</th>
            <th>本月随访次数</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="filteredAndPaginatedRows.length === 0">
            <td colspan="10" class="table-empty">暂无数据</td>
          </tr>
          <tr v-for="(row, index) in filteredAndPaginatedRows" :key="row.id">
            <td>{{ (pageNo - 1) * pageSize + index + 1 }}</td>
            <td>{{ row.name }}</td>
            <td>{{ row.staffId || '——' }}</td>
            <td>{{ row.department || '——' }}</td>
            <td>{{ row.phone || '——' }}</td>
            <td>{{ row.patientCount || 0 }}</td>
            <td>{{ row.totalFollowupCount || 0 }}</td>
            <td>{{ row.monthFollowupCount || 0 }}</td>
            <td>
              <span :class="getStatusClass(row.status)">{{ row.status || '在职' }}</span>
            </td>
            <td>
              <RouterLink :to="`/followup/staff/${row.id || row.staffId}`" class="link-primary">查看详情</RouterLink>
            </td>
          </tr>
        </tbody>
      </table>

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

    <!-- 统计区域 -->
    <div class="summary-section">
      <div class="summary-card">
        <div class="summary-card-header">人员统计</div>
        <div class="summary-card-body">
          <div class="stat-item">
            <span>总人数</span>
            <span class="stat-number primary">{{ stats.totalStaff }} 人</span>
          </div>
          <div class="stat-item">
            <span>在职人员</span>
            <span class="stat-number success">{{ stats.activeStaff }} 人</span>
          </div>
          <div class="stat-item">
            <span>本月活跃</span>
            <span class="stat-number">{{ stats.activeThisMonth }} 人</span>
          </div>
        </div>
      </div>

      <div class="summary-card">
        <div class="summary-card-header">工作量统计</div>
        <div class="summary-card-body">
          <div class="stat-item">
            <span>总随访次数</span>
            <span class="stat-number primary">{{ stats.totalFollowups }} 次</span>
          </div>
          <div class="stat-item">
            <span>本月随访</span>
            <span class="stat-number success">{{ stats.monthFollowups }} 次</span>
          </div>
          <div class="stat-item">
            <span>平均每人</span>
            <span class="stat-number">{{ stats.avgFollowups }} 次/人</span>
          </div>
        </div>
      </div>

      <div class="summary-card">
        <div class="summary-card-header">患者覆盖</div>
        <div class="summary-card-body">
          <div class="stat-item">
            <span>总负责患者</span>
            <span class="stat-number primary">{{ stats.totalPatients }} 人</span>
          </div>
          <div class="stat-item">
            <span>平均每人</span>
            <span class="stat-number">{{ stats.avgPatients }} 人/人</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 注册随访人员弹窗 -->
    <div v-if="showRegisterModal" class="modal-backdrop">
      <div class="modal">
        <div class="modal-header">
          <h3>注册随访人员</h3>
          <button class="btn btn-ghost" @click="closeRegisterModal">✕</button>
        </div>
        <div class="modal-body">
          <div class="form-row">
            <label>工号 <span class="required">*</span></label>
            <input v-model="registerForm.staffNo" class="input" type="text" placeholder="请输入工号" />
          </div>
          <div class="form-row">
            <label>姓名 <span class="required">*</span></label>
            <input v-model="registerForm.name" class="input" type="text" placeholder="请输入姓名" />
          </div>
          <div class="form-row">
            <label>联系电话 <span class="required">*</span></label>
            <input
              v-model="registerForm.phone"
              class="input"
              type="tel"
              inputmode="numeric"
              maxlength="11"
              placeholder="请输入联系电话（11位手机号）"
              @input="onPhoneInput"
            />
          </div>
          <div class="form-row">
            <label>部门</label>
            <div class="dept-select" @click="focusDeptInput">
              <input
                ref="deptInputRef"
                v-model="departmentQuery"
                class="input"
                type="text"
                placeholder="输入搜索部门，如：心内科 / 内分泌科"
                @focus="openDeptDropdown"
                @input="openDeptDropdown"
                @click.stop
              />
              <div v-if="showDeptDropdown" class="dept-dropdown" @mousedown.stop>
                <button
                  v-for="dept in filteredDepartments"
                  :key="dept"
                  type="button"
                  class="dept-item"
                  @mousedown.prevent="selectDepartment(dept)"
                >
                  {{ dept }}
                </button>
                <div v-if="!filteredDepartments.length" class="dept-empty">无匹配部门</div>
              </div>
            </div>
          </div>
          <p class="form-hint">注册成功后，默认密码为 123456</p>
          <p v-if="registerError" class="form-error">{{ registerError }}</p>
        </div>
        <div class="modal-footer">
          <button class="btn btn-ghost" @click="closeRegisterModal">取消</button>
          <button class="btn btn-primary" @click="submitRegister" :disabled="registering">{{ registering ? '提交中...' : '提交' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { RouterLink } from 'vue-router'
import { followupApi, type StaffListFilter, type StaffListData } from '@/api/followup'

const loading = ref(false)
const pageNo = ref(1)
const pageSize = 10

// 一次性从接口获取的全部人员数据，用于本地筛选与分页
const allRows = ref<StaffListData['rows']>([])

// 注册弹窗与表单
const showRegisterModal = ref(false)
const registerForm = ref({
  staffNo: '',
  name: '',
  phone: '',
  department: ''
})
const registerError = ref('')
const registering = ref(false)

// 注册弹窗：部门搜索选择
const departmentQuery = ref('')
const showDeptDropdown = ref(false)
const deptInputRef = ref<HTMLInputElement | null>(null)

const filteredDepartments = computed(() => {
  const kw = departmentQuery.value.trim().toLowerCase()
  const list = departmentList.value
  if (!kw) return list.slice(0, 20)
  return list.filter((d) => d.toLowerCase().includes(kw)).slice(0, 20)
})

const filters = ref<Omit<StaffListFilter, 'pageNo'>>({
  staffName: '',
  department: '',
  status: ''
})

const data = ref<StaffListData>({
  rows: [],
  pageNo: 1,
  totalPages: 1,
  total: 0,
  stats: {
    totalStaff: 0,
    activeStaff: 0,
    activeThisMonth: 0,
    totalFollowups: 0,
    monthFollowups: 0,
    avgFollowups: 0,
    totalPatients: 0,
    avgPatients: 0
  }
})

// 从所有数据中提取部门列表
const departmentList = computed(() => {
  const departments = new Set<string>()
  allRows.value.forEach(row => {
    const dept = (row.department || '').trim()
    if (dept) departments.add(dept)
  })
  return Array.from(departments).sort()
})

// 前端筛选后的数据
const filteredRows = computed(() => {
  let filtered = [...allRows.value]
  
  // 随访人员姓名筛选
  if (filters.value.staffName) {
    const keyword = filters.value.staffName.trim().toLowerCase()
    filtered = filtered.filter(row => 
      row.name && row.name.toLowerCase().includes(keyword)
    )
  }
  
  // 部门筛选
  if (filters.value.department) {
    const target = filters.value.department.trim()
    filtered = filtered.filter(row => (row.department || '').trim() === target)
  }
  
  // 状态筛选
  if (filters.value.status) {
    filtered = filtered.filter(row => (row.status || '在职') === filters.value.status)
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

const stats = computed(() => data.value.stats)

onMounted(() => {
  loadData()
  window.addEventListener('pointerdown', handleDeptGlobalPointerDown, true)
})

onUnmounted(() => {
  window.removeEventListener('pointerdown', handleDeptGlobalPointerDown, true)
})

async function loadData() {
  loading.value = true
  try {
    // 不传筛选条件，加载所有数据
    const result = await followupApi.getStaffList({ pageNo: 1, pageSize: 2000 })
    if (result.success && result.data) {
      const apiData = result.data as any
      allRows.value = apiData.rows || apiData.list || []
      data.value.stats = apiData.stats || data.value.stats
      pageNo.value = 1
    }
  } catch (error) {
    console.error('加载数据失败:', error)
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
    staffName: '',
    department: '',
    status: ''
  }
  pageNo.value = 1
}

function goToPage(page: number | string) {
  if (typeof page === 'string') return
  if (page >= 1 && page <= totalPages.value) {
    pageNo.value = page
  }
}

function openRegisterModal() {
  resetRegisterForm()
  // 默认将部门输入框显示为当前值（便于直接搜索/选择）
  departmentQuery.value = registerForm.value.department
  showRegisterModal.value = true
}

function closeRegisterModal() {
  showRegisterModal.value = false
  registerError.value = ''
}

function resetRegisterForm() {
  registerForm.value = {
    staffNo: '',
    name: '',
    phone: '',
    department: ''
  }
  departmentQuery.value = ''
  showDeptDropdown.value = false
}

function onPhoneInput() {
  // 仅做输入约束：保留数字、最多 11 位（不改变后端接口/逻辑）
  const raw = String(registerForm.value.phone || '')
  const digits = raw.replace(/\D/g, '').slice(0, 11)
  if (digits !== raw) registerForm.value.phone = digits
}

function focusDeptInput() {
  deptInputRef.value?.focus()
}

function openDeptDropdown() {
  showDeptDropdown.value = true
}

function selectDepartment(dept: string) {
  registerForm.value.department = dept
  departmentQuery.value = dept
  showDeptDropdown.value = false
}

function handleDeptGlobalPointerDown(e: PointerEvent) {
  if (!showDeptDropdown.value) return
  const target = e.target as Node | null
  const inputEl = deptInputRef.value
  if (!target || !inputEl) {
    showDeptDropdown.value = false
    return
  }
  if (inputEl.contains(target)) return
  const wrap = inputEl.closest('.dept-select')
  if (wrap && wrap.contains(target)) return
  showDeptDropdown.value = false
}
async function submitRegister() {
  registerError.value = ''
  if (!registerForm.value.staffNo.trim()) {
    registerError.value = '工号不能为空'
    return
  }
  if (!registerForm.value.name.trim()) {
    registerError.value = '姓名不能为空'
    return
  }
  if (!registerForm.value.phone.trim()) {
    registerError.value = '联系电话不能为空'
    return
  }
  if (registerForm.value.phone.trim().length !== 11) {
    registerError.value = '联系电话需为 11 位手机号'
    return
  }

  registering.value = true
  try {
    const result = await followupApi.registerStaff({
      staffNo: registerForm.value.staffNo.trim(),
      name: registerForm.value.name.trim(),
      phone: registerForm.value.phone.trim(),
      address: registerForm.value.department.trim() || undefined
    })

    if (result.success) {
      // 添加到列表顶部
      const newStaff = {
        id: result.data?.userId || Date.now(),
        staffId: registerForm.value.staffNo.trim() || undefined,
        name: registerForm.value.name.trim(),
        department: registerForm.value.department.trim() || undefined,
        phone: registerForm.value.phone.trim() || undefined,
        status: '在职',
        patientCount: 0,
        totalFollowupCount: 0,
        monthFollowupCount: 0
      }
      allRows.value = [newStaff, ...allRows.value]
      data.value.stats.totalStaff += 1
      data.value.stats.activeStaff += 1
      pageNo.value = 1
      closeRegisterModal()
    } else {
      registerError.value = result.message || '注册失败'
    }
  } catch (error: any) {
    registerError.value = error.message || '注册失败'
  } finally {
    registering.value = false
  }
}

function getStatusClass(status?: string): string {
  const value = status || '在职'
  if (value === '在职') {
    return 'badge badge-success'
  } else {
    return 'badge badge-danger'
  }
}

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
</script>


<style scoped>
/* =========================
   仅美化，不改布局（followup/StaffList.vue）
   ========================= */

.staff-list-page {
  /* 仅美化，不改布局（followup/StaffList.vue）：修复大卡 hover 越界裁切 */
  width: 100%;
  max-width: 1120px;
  min-width: 0;
  margin: 0 auto;
  padding-inline: 12px;
  box-sizing: border-box;
  overflow: visible;
  background: transparent;
}

/* 标题区（保持 page-header 原结构与位置） */
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

/* 统一卡片（16px 体系 + 毛玻璃 + 细高光线） */
.card,
.summary-card {
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  border: 1px solid rgba(226, 232, 240, 0.70);
  box-shadow: 0 10px 30px rgba(102,126,234,0.08), 0 4px 12px rgba(15,23,42,0.04);
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

.card::before,
.summary-card::before {
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

.card:hover,
.summary-card:hover {
  box-shadow: 0 14px 36px rgba(102,126,234,0.10), 0 8px 18px rgba(15,23,42,0.06);
  border-color: rgba(203, 213, 225, 0.78);
  z-index: 2;
}

/* 表格外层大卡更克制 */
.card:hover {
  transform: scale(1.006);
}

/* 统计卡更明显一点 */
.summary-card:hover {
  transform: scale(1.012);
}

.card:hover::before,
.summary-card:hover::before {
  opacity: 0.94;
}

.card > *,
.summary-card > * {
  position: relative;
  z-index: 2;
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

.btn:focus,
.btn:focus-visible {
  outline: none;
}

.filter-bar {
  display: flex;
  gap: 12px;
  flex-wrap: nowrap;
  align-items: center;
  overflow: visible;
  padding: 16px 20px;
  background: transparent;
  backdrop-filter: none;
  border-radius: 16px;
  border: none;
}

/* 表格外层大卡：允许 hover 放大溢出显示，避免被裁切 */
.staff-list-page > .card {
  overflow: visible;
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

.filter-bar .input:focus-visible {
  outline: none;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 2px solid rgba(226, 232, 240, 0.3);
}

.card-header .card-title {
  font-size: 16px;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 0;
}

.card-header .card-title::before {
  content: '';
  width: 4px;
  height: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 2px;
  margin-right: 8px;
}

.card-subtitle {
  font-size: 13px;
  color: #718096;
  font-weight: 500;
}

.pagination {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  margin-top: 24px;
  padding-top: 24px;
  border-top: 2px solid rgba(226, 232, 240, 0.5);
}

.page-info {
  color: #718096;
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
  transition: all 0.2s ease;
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
  font-weight: 700;
  box-shadow: 0 4px 14px 0 rgba(102, 126, 234, 0.39);
}

.page-btn.disabled {
  cursor: default;
  color: #a0aec0;
  background: rgba(248, 250, 252, 0.5);
  border-color: #f1f5f9;
}

.btn-secondary {
  background: rgba(255, 255, 255, 0.9);
  border-color: #e2e8f0;
  color: #4a5568;
  backdrop-filter: blur(10px);
  font-weight: 600;
  height: 44px;
  padding: 0 20px;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.btn-secondary:hover:not(:disabled) {
  background: #ffffff;
  border-color: #cbd5e0;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.12);
}

.btn-secondary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.summary-section {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-top: 24px;
  overflow: visible;
  padding-left: 10px;
  padding-right: 10px;
}

.summary-card {
  padding: 24px;
}

.summary-card::before {
  /* 顶部极细高光线由统一卡片样式提供 */
}

.summary-card:hover {
  /* 仅增强阴影，不位移 */
}

.summary-card-header {
  font-weight: 700;
  font-size: 16px;
  margin-bottom: 16px;
  color: #2d3748;
  display: flex;
  align-items: center;
  gap: 8px;
}

.summary-card-header::before {
  content: '';
  width: 4px;
  height: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 2px;
}

.summary-card-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.stat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  padding: 12px 16px;
  background: rgba(247, 250, 252, 0.7);
  border-radius: 8px;
  transition: all 0.2s ease;
}

.stat-item:hover {
  background: rgba(247, 250, 252, 1);
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.06);
}

.stat-item span:first-child {
  color: #4a5568;
  font-weight: 600;
}

.stat-number {
  font-weight: 700;
  font-size: 18px;
}

.stat-number.primary {
  color: #667eea;
}

.stat-number.success {
  color: #48bb78;
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

.badge-success {
  background: linear-gradient(135deg, #c6f6d5 0%, #9ae6b4 100%);
  color: #065f46;
  border-color: rgba(16, 185, 129, 0.38);
}

.badge-danger {
  background: linear-gradient(135deg, #fed7d7 0%, #feb2b2 100%);
  color: #991b1b;
  border-color: rgba(248, 113, 113, 0.55);
}

/* 表格本体：表头渐变、分隔线更淡、hover 不位移 */
.table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
}

.table thead th {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.08) 0%, rgba(118, 75, 162, 0.06) 100%);
  font-size: 12.5px;
  font-weight: 700;
  color: #64748b;
  padding: 14px 16px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.85);
  letter-spacing: 0.2px;
}

.table tbody td {
  font-size: 13.5px;
  color: #1f2937;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.45);
}

.table tbody tr {
  transition: background 0.22s ease;
}

.table tbody tr:hover {
  background: rgba(102, 126, 234, 0.05);
}

.table-empty {
  text-align: center;
  color: #94a3b8;
  padding: 18px 12px;
  font-weight: 600;
}

/* 弹窗：遮罩 blur + 内容半透明 16px 圆角（不改逻辑） */
.modal-backdrop {
  backdrop-filter: blur(6px);
}

.modal {
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  max-height: 90vh;
}

.modal .btn.btn-ghost:focus-visible,
.modal .btn.btn-primary:focus-visible {
  outline: none;
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

.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
  padding: 16px;
}

.modal {
  width: 420px;
  max-width: 100%;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
  border: 1px solid rgba(226, 232, 240, 0.8);
  overflow: hidden;
}

.modal-header,
.modal-footer {
  padding: 14px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  background: #f8fafc;
}

.modal-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #1f2937;
}

.modal-body {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 注册弹窗：部门搜索下拉 */
.dept-select {
  position: relative;
}

.dept-dropdown {
  position: absolute;
  left: 0;
  right: 0;
  top: calc(100% + 8px);
  z-index: 10;
  background: rgba(255, 255, 255, 0.98);
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 12px;
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.12);
  overflow: hidden;
  max-height: 240px;
  overflow-y: auto;
}

.dept-item {
  width: 100%;
  text-align: left;
  padding: 10px 12px;
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  color: #1f2937;
  transition: background 0.2s ease, color 0.2s ease;
}

.dept-item:hover {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.12) 0%, rgba(118, 75, 162, 0.12) 100%);
  color: #4c51bf;
}

.dept-empty {
  padding: 12px;
  font-size: 12px;
  color: #94a3b8;
}

.form-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-row label {
  font-size: 13px;
  font-weight: 600;
  color: #4b5563;
}

.form-error {
  color: #c53030;
  font-size: 12px;
  margin: 4px 0 0;
}

.modal .btn.btn-ghost {
  height: 36px;
  padding: 0 12px;
}

.modal .btn.btn-primary {
  height: 36px;
  padding: 0 16px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .summary-section {
    grid-template-columns: 1fr;
  }
  
  .filter-bar {
    flex-wrap: wrap;
    gap: 8px;
  }
  
  .summary-card {
    padding: 20px;
  }
}
</style>

