<template>
  <div class="risk-list-page">
    <div class="page-header">
      <!-- 仅美化，不改布局：标题徽章 + 渐变标题 -->
      <div class="page-title">
        <span class="title-badge" aria-hidden="true">
          <!-- 简易用户/档案风格图标（避免引入新依赖） -->
          <svg viewBox="0 0 24 24" fill="none" class="title-badge-icon">
            <path d="M16 7a4 4 0 10-8 0 4 4 0 008 0z" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
            <path d="M4 20a8 8 0 0116 0" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
            <path d="M18 4h2v6h-2" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </span>
        <span class="page-title-text">患者档案管理</span>
      </div>
      <div class="page-subtitle">患者信息综合管理</div>
    </div>

    <!-- 筛选栏 -->
    <div class="card risk-list-big-card">
      <form @submit.prevent="handleSearch" class="filter-bar">
        <select v-model="filters.riskLevel" class="input">
          <option value="">全部风险等级</option>
          <option value="HIGH">高危</option>
          <option value="MID">中危</option>
          <option value="LOW">低危</option>
        </select>

        <select v-model="filters.syndrome" class="input">
          <option value="">全部证型</option>
          <option v-for="s in data.syndromeList" :key="s" :value="s">{{ s }}</option>
        </select>

        <!-- 病种搜索 + 下拉（整合为一个控件，弹出层跳出卡片） -->
        <div class="disease-select">
          <input
            ref="filterDiseaseInputRef"
            v-model="filterDiseaseKeyword"
            class="input"
            type="text"
            placeholder="全部病种 / 输入关键字"
            @input="onDiseaseFilterInput"
            @focus="onDiseaseFilterFocus"
            @blur="onDiseaseFilterBlur"
          />
        </div>

        <select v-model="filters.responsibleDoctor" class="input input-doctor">
          <option value="">全部责任医生</option>
          <option v-for="d in doctorOptions" :key="d.value" :value="d.value">{{ d.label }}</option>
        </select>

        <input
          v-model="filters.q"
          class="input flex-1 search-input"
          type="text"
          placeholder="姓名 / 病案号 / 手机号"
        />

        <button class="btn btn-primary" type="submit">搜索</button>
        <button class="btn btn-secondary" type="button" @click="handleReset">重置</button>
      </form>
    </div>

    <!-- 患者列表 -->
    <div class="card risk-list-big-card">
      <div class="card-header-row">
        <div class="card-title">患者档案管理</div>
        <div class="header-actions">
          <button class="btn btn-secondary" type="button" @click="openAddDiseaseDialog">+ 新增病种</button>
          <button class="btn btn-primary" type="button" @click="showAddDialog = true">+ 新增患者</button>
        </div>
      </div>
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else class="table-scroll">
        <table class="table">
        <thead>
          <tr>
            <th>病案号</th>
            <th>姓名</th>
            <th>性别</th>
            <th>年龄</th>
            <th>手机号</th>
            <th>病种</th>
            <th>证型</th>
            <th>风险等级</th>
            <th>责任医生</th>
            <th>随访次数</th>
            <th>最近随访</th>
            <th>处理中告警</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="data.rows.length === 0">
            <td colspan="12" style="text-align: center; color: #999;">暂无数据</td>
          </tr>
          <tr
            v-for="row in data.rows"
            :key="row.patientId"
            class="row-clickable"
            @click="row.patientId && goToPatientDetail(row.patientId)"
          >
            <td>{{ formatCaseId(row.patientId) }}</td>
            <td>{{ row.name }}</td>
            <td>{{ getGenderText(row.gender) }}</td>
            <td>{{ row.age || '--' }}</td>
            <td>{{ row.phone || '--' }}</td>
            <td>{{ row.disease || '--' }}</td>
            <td>{{ row.syndrome || '--' }}</td>
            <td>
              <span v-if="row.riskLevel" :class="getRiskLevelClass(row.riskLevel)">
                {{ getRiskLevelText(row.riskLevel) }}
              </span>
              <span v-else>--</span>
            </td>
            <td>{{ row.responsibleDoctor || '--' }}</td>
            <td>{{ row.followupCount }}</td>
            <td>{{ formatDate(row.lastFollowupTime) }}</td>
            <td>
              <span v-if="row.activeAlertCount > 0" class="badge badge-danger">
                {{ row.activeAlertCount }}
              </span>
              <span v-else>0</span>
            </td>
          </tr>
        </tbody>
      </table>
      </div>

      <!-- 分页 -->
      <div v-if="!loading && data.totalPages > 0" class="pagination">
        <div class="page-info">
          共 {{ data.total }} 条记录，第 {{ data.pageNo }} / {{ data.totalPages }} 页
        </div>
        <div class="page-controls">
          <button 
            class="btn btn-secondary" 
            :disabled="data.pageNo <= 1"
            @click="goToPage(data.pageNo - 1)">
            上一页
          </button>
          <span class="page-numbers">
            <button
              v-for="page in getPageNumbers()"
              :key="page"
              class="page-btn"
              :class="{ active: page === data.pageNo, disabled: page === '...' }"
              :disabled="page === '...' || page === data.pageNo"
              @click="page !== '...' && goToPage(page)">
              {{ page }}
            </button>
          </span>
          <button 
            class="btn btn-secondary" 
            :disabled="data.pageNo >= data.totalPages"
            @click="goToPage(data.pageNo + 1)">
            下一页
          </button>
        </div>
      </div>
    </div>

    <!-- 新增患者弹窗 -->
    <Teleport to="body">
      <div v-if="showAddDialog" class="modal-overlay" @click.self="closeAddDialog">
        <div class="modal-content">
          <div class="modal-header">
            <h3>新增患者</h3>
            <button class="modal-close" @click="closeAddDialog">×</button>
          </div>
          <div class="modal-body">
          <form @submit.prevent="handleAddPatient">
            <div class="form-group">
              <label>姓名 <span class="required">*</span></label>
              <input v-model="newPatient.name" class="input" type="text" required placeholder="请输入患者姓名" />
            </div>
            <div class="form-group">
              <label>性别</label>
              <select v-model="newPatient.gender" class="input">
                <option value="">请选择</option>
                <option value="男">男</option>
                <option value="女">女</option>
              </select>
            </div>
            <div class="form-group">
              <label>风险等级 <span class="required">*</span></label>
              <select v-model="newPatient.riskLevel" class="input" required>
                <option value="">请选择</option>
                <option value="LOW">低</option>
                <option value="MID">中</option>
                <option value="HIGH">高</option>
              </select>
            </div>
            <div class="form-group">
              <label>身份证号</label>
              <input v-model="newPatient.idCard" class="input" type="text" placeholder="请输入18位身份证号" maxlength="18" />
              <div v-if="idCardError" class="input-error">{{ idCardError }}</div>
            </div>
            <div class="form-group">
              <label>年龄</label>
              <input v-model.number="newPatient.age" class="input" type="number" placeholder="请输入年龄" min="0" max="150" />
            </div>
            <div class="form-group">
              <label>手机号</label>
              <input
                v-model="newPatient.phone"
                class="input"
                type="tel"
                inputmode="numeric"
                maxlength="11"
                placeholder="请输入手机号（注册小程序账号时需要）"
                @input="newPatient.phone = String(newPatient.phone || '').replace(/\\D/g, '').slice(0, 11)"
              />
            </div>
            <div class="form-group">
              <label>医生密码 <span class="required">*</span></label>
              <input v-model="doctorPassword" class="input" type="password" required placeholder="用于后端二次校验，不会保存" />
            </div>
            <div class="form-group">
              <label>病种</label>
              <div ref="diseaseSelectAddRef" class="disease-select disease-select-add" @click="focusDiseaseInput">
                <div class="selected-tags" v-if="selectedDiseaseTypes.length">
                  <span v-for="(t, idx) in selectedDiseaseTypes" :key="t.value + '_' + idx" class="tag">
                    {{ t.value }}
                    <button type="button" class="tag-remove" @click="removeSelectedDisease(idx)">×</button>
                  </span>
                </div>
                <input
                  ref="diseaseInputRef"
                  v-model="diseaseQuery"
                  class="input"
                  type="text"
                  placeholder="输入几个字后选择（支持多选）"
                  @input.stop="onDiseaseQueryInput"
                  @focus="onDiseaseQueryInput"
                  @blur="commitTypedDisease()"
                  @click.stop
                />
                <div v-if="showDiseaseDropdown" class="dropdown" @click.stop>
                  <div v-if="diseaseLoading" class="dropdown-item muted">加载中...</div>
                  <button
                    v-for="opt in diseaseOptions"
                    :key="opt.value"
                    type="button"
                    class="dropdown-item"
                    @click="selectDiseaseOption(opt)"
                  >
                    {{ opt.label }}
                  </button>
                  <div v-if="!diseaseLoading && !diseaseOptions.length" class="dropdown-item muted">无匹配结果</div>
                </div>
              </div>
            </div>
            <div class="form-group">
              <label>证型</label>
              <input v-model="newPatient.syndrome" class="input" type="text" placeholder="请输入证型" />
            </div>
            <div class="form-group">
              <label>体质</label>
              <input v-model="newPatient.constitution" class="input" type="text" placeholder="请输入体质" />
            </div>
            <div class="form-group">
              <label>家族遗传病史</label>
              <input v-model="newPatient.familyHistory" class="input" type="text" placeholder="请输入家族遗传病史" />
            </div>
            <div class="form-group">
              <label class="checkbox-label">
                <input type="checkbox" v-model="registerMiniProgram" class="checkbox-input" />
                <span>同时为患者注册小程序账号</span>
              </label>
              <div v-if="registerMiniProgram" class="register-hint">
                <span class="hint-icon">ℹ️</span>
                <span>注册小程序账号需要填写手机号和密码，账号将使用手机号作为用户名</span>
              </div>
            </div>
            <div v-if="registerMiniProgram" class="form-group">
              <label>小程序账号密码 <span class="required">*</span></label>
              <input v-model="miniProgramPassword" class="input" type="password" placeholder="请输入小程序账号密码（至少6位）" minlength="6" />
            </div>
            <div v-if="registerMiniProgram" class="form-group">
              <label>确认密码 <span class="required">*</span></label>
              <input v-model="miniProgramPasswordConfirm" class="input" type="password" placeholder="请再次输入密码" minlength="6" />
            </div>
            <div class="form-actions">
              <button type="button" class="btn btn-secondary" @click="closeAddDialog">取消</button>
              <button type="submit" class="btn btn-primary" :disabled="adding">
                {{ adding ? '添加中...' : '添加患者' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
    </Teleport>

    <!-- 新增病种弹窗 -->
    <Teleport to="body">
      <div v-if="showAddDiseaseDialog" class="modal-overlay" @click.self="closeAddDiseaseDialog">
        <div class="modal-content" style="max-width: 520px;">
          <div class="modal-header">
            <h3>新增病种</h3>
            <button class="modal-close" @click="closeAddDiseaseDialog">×</button>
          </div>
          <div class="modal-body">
            <form @submit.prevent="handleCreateDiseaseType">
              <div class="form-group">
                <label>病种名称 <span class="required">*</span></label>
                <input v-model="newDiseaseName" class="input" type="text" required placeholder="例如：高血压" />
              </div>
              <div class="form-actions">
                <button type="button" class="btn btn-secondary" @click="closeAddDiseaseDialog">取消</button>
                <button type="submit" class="btn btn-primary" :disabled="creatingDisease">
                  {{ creatingDisease ? '创建中...' : '创建病种' }}
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 病种下拉浮层 Teleport 到 body，避免被卡片裁剪 -->
    <Teleport to="body">
      <div
        v-if="showFilterDiseaseDropdown"
        class="disease-dropdown-floating"
        :style="filterDiseaseDropdownStyle"
      >
        <div class="dropdown">
          <button
            v-for="opt in filterDiseaseOptions"
            :key="opt.value"
            type="button"
            class="dropdown-item"
            @mousedown.prevent="selectFilterDiseaseOption(opt)"
          >
            {{ opt.label || opt.value }}
          </button>
          <div
            v-if="!filterDiseaseOptions.length"
            class="dropdown-item muted"
          >
            无匹配结果
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { patientApi, type PatientSummaryFilter, type PatientSummaryData, type DiseaseTypeOption } from '@/api/patient'
import { useAuthStore } from '@/stores/auth'

const loading = ref(false)
const showAddDialog = ref(false)
const adding = ref(false)
const showAddDiseaseDialog = ref(false)
const newDiseaseName = ref('')
const creatingDisease = ref(false)

// 所有患者原始数据（一次性从接口获取，用于前端本地筛选和分页）
const allRows = ref<PatientSummaryData['rows']>([])

// 前端筛选条件
const filters = ref<PatientSummaryFilter>({
  riskLevel: '',
  disease: '',
  syndrome: '',
  responsibleDoctor: '',
  q: ''
})

const newPatient = ref({
  name: '',
  gender: '',
  age: null as number | null,
  idCard: '',
  phone: '',
  disease: '',
  syndrome: '',
  constitution: '',
  familyHistory: '',
  riskLevel: ''
})

const diseaseQuery = ref('')
const diseaseInputRef = ref<HTMLInputElement | null>(null)
const diseaseSelectAddRef = ref<HTMLElement | null>(null)
const diseaseOptions = ref<DiseaseTypeOption[]>([])
const diseaseLoading = ref(false)
const showDiseaseDropdown = ref(false)
const selectedDiseaseTypes = ref<Array<{ value: string; label: string }>>([])
let diseaseSearchTimer: any = null
let diseaseQuerySeq = 0

// 筛选栏专用病种搜索 + 选项（单选，下拉样式与其它 select 保持一致）
const filterDiseaseKeyword = ref('')
const baseDiseaseOptions = ref<DiseaseTypeOption[]>([])
const filterDiseaseOptions = ref<DiseaseTypeOption[]>([])
const showFilterDiseaseDropdown = ref(false)
const filterDiseaseInputRef = ref<HTMLInputElement | null>(null)
const filterDiseaseDropdownStyle = ref<Record<string, string>>({
  left: '0px',
  top: '0px',
  minWidth: '160px'
})

const registerMiniProgram = ref(false)
const miniProgramPassword = ref('')
const miniProgramPasswordConfirm = ref('')
const doctorPassword = ref('')

const authStore = useAuthStore()
const router = useRouter()

const idCardError = ref('')

function parseBirthDateFromIdCard(id: string): { y: number; m: number; d: number } | null {
  const raw = String(id || '').trim()
  if (!raw) return null
  const v = raw.replace(/\s+/g, '').toUpperCase()
  let birth = ''
  if (/^\d{17}[\dX]$/.test(v)) {
    birth = v.slice(6, 14) // YYYYMMDD
  } else if (/^\d{15}$/.test(v)) {
    birth = '19' + v.slice(6, 12) // YYMMDD -> 19YYMMDD
  } else {
    return null
  }

  const y = Number(birth.slice(0, 4))
  const m = Number(birth.slice(4, 6))
  const d = Number(birth.slice(6, 8))
  if (!y || m < 1 || m > 12 || d < 1 || d > 31) return null

  // 校验是否为真实日期（如 20250231 应判为非法）
  const dt = new Date(y, m - 1, d)
  if (dt.getFullYear() !== y || dt.getMonth() + 1 !== m || dt.getDate() !== d) return null
  return { y, m, d }
}

function calcAgeFromIdCard(id: string): number | null {
  const birth = parseBirthDateFromIdCard(id)
  if (!birth) return null

  const now = new Date()
  const curY = now.getFullYear()
  const curM = now.getMonth() + 1
  const curD = now.getDate()

  let age = curY - birth.y
  // 未过生日则 -1
  if (curM < birth.m || (curM === birth.m && curD < birth.d)) age -= 1
  if (age < 0 || age > 150) return null
  return age
}

watch(
  () => newPatient.value.idCard,
  (val) => {
    const raw = String(val || '').trim()
    if (!raw) {
      idCardError.value = ''
      newPatient.value.age = null
      return
    }

    const v = raw.replace(/\s+/g, '').toUpperCase()
    // 未输入到 15/18 位之前不提示错误，避免输入过程干扰
    if (v.length < 15) {
      idCardError.value = ''
      return
    }

    // 仅在长度为 15 或 18 时做日期合法性提示
    if (v.length === 15 || v.length === 18) {
      const birth = parseBirthDateFromIdCard(v)
      if (!birth) {
        idCardError.value = '身份证日期不合法'
        newPatient.value.age = null
        return
      }
      idCardError.value = ''
      const age = calcAgeFromIdCard(v)
      newPatient.value.age = age
      return
    }

    // 其它长度（例如 16/17）先不提示
    idCardError.value = ''
  }
)

// 当前表格展示和分页信息
const data = ref<PatientSummaryData>({
  diseaseList: [],
  syndromeList: [],
  doctorList: [],
  pageNo: 1,
  pageSize: 10,
  total: 0,
  totalPages: 0,
  rows: []
})

const doctorOptions = computed(() => {
  const list: any[] = Array.isArray((data.value as any)?.doctorList) ? ((data.value as any).doctorList as any[]) : []
  const options = list
    .map((it: any) => {
      if (it == null) return null
      if (typeof it === 'string' || typeof it === 'number') {
        const s = String(it)
        return { label: s, value: s }
      }
      if (typeof it === 'object') {
        const label = String(it.label ?? it.text ?? it.name ?? it.username ?? it.value ?? it.id ?? '')
        if (!label) return null
        // 这里用姓名作为 value，保证与 row.responsibleDoctor（表格里的责任医生字段）可直接匹配
        return { label, value: label }
      }
      return null
    })
    .filter(Boolean) as Array<{ label: string; value: string }>

  // 去重
  const seen = new Set<string>()
  return options.filter((o) => {
    if (!o || !o.value) return false
    if (seen.has(o.value)) return false
    seen.add(o.value)
    return true
  })
})

onMounted(() => {
  loadData()
  window.addEventListener('click', handleGlobalClick)
})

onUnmounted(() => {
  window.removeEventListener('click', handleGlobalClick)
})

function handleGlobalClick(e: MouseEvent) {
  // 仅当点击在“新增患者-病种”控件外部时才收起
  const root = diseaseSelectAddRef.value
  const target = e.target as Node | null
  if (root && target && root.contains(target)) return
  showDiseaseDropdown.value = false
}

function focusDiseaseInput() {
  if (diseaseInputRef.value) {
    diseaseInputRef.value.focus()
    onDiseaseQueryInput()
  }
}

// 下拉筛选（风险等级 / 病种 / 证型 / 责任医生）变化时，自动应用筛选，
// 和“健康数据异常预警”页面一样，无需再点按钮
watch(
  () => ({
    riskLevel: filters.value.riskLevel,
    disease: filters.value.disease,
    syndrome: filters.value.syndrome,
    responsibleDoctor: filters.value.responsibleDoctor
  }),
  () => {
    data.value.pageNo = 1
    applyFilters()
  }
)

async function loadData() {
  loading.value = true
  try {
    // 后端约束：pageSize 仅支持 1-200，这里按分页循环拉取并汇总（便于前端本地筛选）
    const pageSize = 200
    const merged: any[] = []
    let pageNo = 1
    let total = 0
    let baseMeta: any = null

    while (true) {
      const result = await patientApi.getPatientSummary({ pageNo, pageSize })
      if (!(result && result.success && result.data)) break

      const res: any = result.data
      if (!baseMeta) baseMeta = res

      const rows: any[] = Array.isArray(res.rows) ? res.rows : []
      merged.push(...rows)

      total = typeof res.total === 'number' ? res.total : Number(res.total || 0)
      if (rows.length < pageSize) break
      if (total > 0 && merged.length >= total) break

      pageNo += 1
      // 防御：避免异常情况下无限拉取
      if (pageNo > 50) break
    }

    if (baseMeta) {
      allRows.value = merged
      data.value.diseaseList = baseMeta.diseaseList || []
      data.value.syndromeList = baseMeta.syndromeList || []
      data.value.doctorList = baseMeta.doctorList || []
      data.value.pageNo = 1
      data.value.pageSize = 10

      // 初始化病种下拉选项
      const list: any[] = Array.isArray((baseMeta as any).diseaseList)
        ? ((baseMeta as any).diseaseList as any[])
        : []
      const mapped = list
        .map((it: any) => {
          if (it == null) return null
          if (typeof it === 'string' || typeof it === 'number') {
            const s = String(it)
            return { value: s, label: s } as DiseaseTypeOption
          }
          if (typeof it === 'object') {
            const value = String(it.value ?? it.name ?? it.label ?? '')
            if (!value) return null
            const label = String(it.label ?? it.name ?? value)
            return { value, label } as DiseaseTypeOption
          }
          return null
        })
        .filter(Boolean) as DiseaseTypeOption[]

      baseDiseaseOptions.value = mapped
      filterDiseaseOptions.value = mapped
      applyFilters()
    }
  } catch (error) {
    console.error('加载数据失败:', error)
  } finally {
    loading.value = false
  }
}

function onDiseaseQueryInput() {
  const q = (diseaseQuery.value || '').trim()
  if (diseaseSearchTimer) {
    clearTimeout(diseaseSearchTimer)
    diseaseSearchTimer = null
  }
  if (!q) {
    // 未输入时：从病种接口拉取「全部病种」，不依赖患者列表的 diseaseList（后者只含已有患者的病种）
    showDiseaseDropdown.value = true
    diseaseLoading.value = true
    const seq = ++diseaseQuerySeq
    ;(async () => {
      try {
        const res = await patientApi.searchDiseaseTypes({ q: '', limit: 500 })
        const rows = res && res.success && res.data ? (res.data as any).rows : []
        const list = Array.isArray(rows) ? rows : []
        // 若期间查询已变化，丢弃过期结果
        if (seq !== diseaseQuerySeq) return
        diseaseOptions.value = list.length > 0 ? list : baseDiseaseOptions.value.slice()
        if (list.length > 0) baseDiseaseOptions.value = list
      } catch (e) {
        if (seq !== diseaseQuerySeq) return
        diseaseOptions.value = baseDiseaseOptions.value.slice()
      } finally {
        if (seq !== diseaseQuerySeq) return
        diseaseLoading.value = false
      }
    })()
    return
  }
  showDiseaseDropdown.value = true
  // 关键：一旦开始输入关键字，立刻清空旧的“全部病种列表”，避免首次输入时短暂显示全量内容
  diseaseOptions.value = []
  diseaseLoading.value = true
  const seq = ++diseaseQuerySeq
  const queryText = q
  diseaseSearchTimer = setTimeout(async () => {
    try {
      const res = await patientApi.searchDiseaseTypes({ q: queryText, limit: 100 })
      const rows = res && res.success && res.data ? (res.data as any).rows : []
      // 前端二次过滤：仅按病种名称匹配（避免输入 e 时按 ICD 编码 E11/E78 命中）
      const list = Array.isArray(rows) ? (rows as DiseaseTypeOption[]) : []
      const kw = queryText.toLowerCase()
      const filtered = list.filter((opt) => {
        const nameText = String((opt as any)?.name ?? (opt as any)?.label ?? (opt as any)?.value ?? '').trim().toLowerCase()
        return nameText.includes(kw)
      })
      // 若期间查询已变化，丢弃过期结果
      if (seq !== diseaseQuerySeq) return
      diseaseOptions.value = filtered
    } catch (e) {
      if (seq !== diseaseQuerySeq) return
      diseaseOptions.value = []
    } finally {
      if (seq !== diseaseQuerySeq) return
      diseaseLoading.value = false
    }
  }, 250)
}

function selectDiseaseOption(opt: DiseaseTypeOption) {
  if (!opt || !opt.value) return
  const exists = selectedDiseaseTypes.value.some((x) => x.value === opt.value)
  if (!exists) {
    selectedDiseaseTypes.value.push({ value: opt.value, label: opt.label || opt.value })
  }
  diseaseQuery.value = ''
  diseaseOptions.value = []
  showDiseaseDropdown.value = false
}

function commitTypedDisease() {
  const v = (diseaseQuery.value || '').trim()
  if (!v) return
  const exists = selectedDiseaseTypes.value.some((x) => (x.value || '').trim() === v)
  if (!exists) {
    selectedDiseaseTypes.value.push({ value: v, label: v })
  }
  diseaseQuery.value = ''
  diseaseOptions.value = []
  showDiseaseDropdown.value = false
}

async function syncNewDiseasesToDb(diseases: string[]) {
  const list = (diseases || []).map((x) => (x || '').trim()).filter(Boolean)
  if (!list.length) return

  // 仅补录当前病种库里没有的
  const missing = list.filter((d) => {
    return !baseDiseaseOptions.value.some((opt) => {
      const t = String(opt?.value || opt?.name || opt?.label || '').trim()
      return t === d
    })
  })
  if (!missing.length) return

  for (const name of missing) {
    try {
      const res = await patientApi.createDiseaseType({ name })
      const item = res && res.success && res.data ? (res.data as any).item : null
      const n = item?.name ? String(item.name) : name
      const newOpt = { value: n, label: item?.label || n } as DiseaseTypeOption
      if (!baseDiseaseOptions.value.some((x) => String(x.value || '').trim() === n)) {
        baseDiseaseOptions.value = [newOpt, ...baseDiseaseOptions.value]
      }
    } catch {
      // 不影响新增患者成功；忽略补录失败
    }
  }
  applyDiseaseKeywordFilter()
}

function removeSelectedDisease(idx: number) {
  if (idx < 0 || idx >= selectedDiseaseTypes.value.length) return
  selectedDiseaseTypes.value.splice(idx, 1)
}

function openAddDiseaseDialog() {
  showAddDiseaseDialog.value = true
  newDiseaseName.value = ''
}

function closeAddDiseaseDialog() {
  showAddDiseaseDialog.value = false
  newDiseaseName.value = ''
}

async function handleCreateDiseaseType() {
  const name = (newDiseaseName.value || '').trim()
  if (!name) {
    alert('请输入病种名称')
    return
  }
  if (!authStore.token) {
    alert('当前未登录医生账号，请先登录')
    return
  }

  creatingDisease.value = true
  try {
    const res = await patientApi.createDiseaseType({ name })
    const item = res && res.success && res.data ? (res.data as any).item : null
    if (!item || !item.name) {
      alert(res?.message || '创建失败')
      return
    }

    // 更新筛选下拉的 diseaseList（去重）
    const list = Array.isArray(data.value.diseaseList) ? data.value.diseaseList : []
    if (!list.includes(item.name)) {
      data.value.diseaseList = [item.name, ...list]
    }

    // 让新增的病种直接可用于“新增患者”病种多选
    const exists = selectedDiseaseTypes.value.some((x) => x.value === item.name)
    if (!exists) {
      selectedDiseaseTypes.value.push({ value: item.name, label: item.label || item.name })
    }
    diseaseQuery.value = ''
    diseaseOptions.value = []
    showDiseaseDropdown.value = false

    // 同时刷新筛选栏下拉里的病种选项（基础列表 + 当前过滤）
    const existsFilter = baseDiseaseOptions.value.some((x) => x.value === item.name)
    if (!existsFilter) {
      const newOpt = { value: item.name, label: item.label || item.name } as DiseaseTypeOption
      baseDiseaseOptions.value = [newOpt, ...baseDiseaseOptions.value]
      // 按当前关键字重新过滤
      applyDiseaseKeywordFilter()
    }

    alert('病种创建成功')
    closeAddDiseaseDialog()
  } catch (e: any) {
    alert(e?.message || '创建失败')
  } finally {
    creatingDisease.value = false
  }
}

function applyDiseaseKeywordFilter() {
  const kw = (filterDiseaseKeyword.value || '').trim().toLowerCase()
  if (!kw) {
    filterDiseaseOptions.value = baseDiseaseOptions.value.slice()
    return
  }
  filterDiseaseOptions.value = baseDiseaseOptions.value.filter((opt) => {
    const text = ((opt.label || opt.value) ?? '').toString().toLowerCase()
    return text.includes(kw)
  })
}

function onDiseaseFilterInput() {
  applyDiseaseKeywordFilter()
  updateDiseaseDropdownPosition()
  showFilterDiseaseDropdown.value = true
}

function onDiseaseFilterFocus() {
  applyDiseaseKeywordFilter()
  updateDiseaseDropdownPosition()
  showFilterDiseaseDropdown.value = true
}

function onDiseaseFilterBlur() {
  // 延迟关闭，保证 mousedown 选择事件能先触发
  setTimeout(() => {
    showFilterDiseaseDropdown.value = false
  }, 150)
}

function selectFilterDiseaseOption(opt: DiseaseTypeOption | null) {
  if (!opt) {
    filters.value.disease = ''
    filterDiseaseKeyword.value = ''
  } else {
    filters.value.disease = opt.value
    filterDiseaseKeyword.value = opt.label || opt.value
  }
  showFilterDiseaseDropdown.value = false
}

function updateDiseaseDropdownPosition() {
  const el = filterDiseaseInputRef.value
  if (!el) return
  const rect = el.getBoundingClientRect()
  filterDiseaseDropdownStyle.value = {
    left: `${rect.left + window.scrollX}px`,
    top: `${rect.bottom + window.scrollY + 4}px`,
    minWidth: `${rect.width}px`,
    width: `${rect.width}px`
  }
}

function handleSearch() {
  data.value.pageNo = 1
  applyFilters()
}

function handleReset() {
  filters.value = {
    riskLevel: '',
    disease: '',
    syndrome: '',
    responsibleDoctor: '',
    q: ''
  }
  data.value.pageNo = 1
  applyFilters()
}

function goToPage(page: number) {
  if (page < 1 || page > data.value.totalPages) return
  data.value.pageNo = page
  applyFilters()
}

function normalizeText(v: any): string {
  if (v === null || v === undefined) return ''
  return String(v).trim()
}

// 本地筛选 + 分页
function applyFilters() {
  let rows = [...allRows.value]

  const { riskLevel, disease, syndrome, responsibleDoctor, q } = filters.value
  const keyword = (q || '').trim().toLowerCase()

  if (riskLevel) {
    rows = rows.filter((row) => normalizeRiskLevel(row.riskLevel) === normalizeRiskLevel(riskLevel))
  }
  if (disease) {
    const d = normalizeText(disease)
    rows = rows.filter((row) => normalizeText(row.disease) === d)
  }
  if (syndrome) {
    const s = normalizeText(syndrome)
    rows = rows.filter((row) => normalizeText(row.syndrome) === s)
  }
  if (responsibleDoctor) {
    const doc = normalizeText(responsibleDoctor)
    rows = rows.filter((row) => normalizeText(row.responsibleDoctor) === doc)
  }
  if (keyword) {
    rows = rows.filter((row) => {
      const name = (row.name || '').toLowerCase()
      const caseId = String(row.patientId || '')
      const phone = (row.phone || '').toLowerCase()
      return (
        name.includes(keyword) ||
        caseId.includes(keyword) ||
        phone.includes(keyword)
      )
    })
  }

  const pageSize = data.value.pageSize
  const total = rows.length
  const totalPages = total > 0 ? Math.ceil(total / pageSize) : 1

  // 修正当前页码在合法范围内
  if (data.value.pageNo > totalPages) {
    data.value.pageNo = totalPages
  }
  if (data.value.pageNo < 1) {
    data.value.pageNo = 1
  }

  const start = (data.value.pageNo - 1) * pageSize
  const pageRows = rows.slice(start, start + pageSize)

  data.value.total = total
  data.value.totalPages = totalPages
  data.value.rows = pageRows
}

function getPageNumbers(): (number | string)[] {
  const current = data.value.pageNo
  const total = data.value.totalPages
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

function getRiskLevelClass(level: string): string {
  switch (normalizeRiskLevel(level)) {
    case 'HIGH':
      return 'badge badge-danger'
    case 'MID':
      return 'badge badge-warning'
    case 'LOW':
      return 'badge badge-success'
    // 兼容后端部分数据可能存在“上”
    case 'TOP':
      return 'badge badge-danger'
    default:
      return 'badge'
  }
}

function getRiskLevelText(level: string): string {
  switch (normalizeRiskLevel(level)) {
    case 'HIGH':
      return '高危'
    case 'MID':
      return '中危'
    case 'LOW':
      return '低危'
    case 'TOP':
      return '特高危'
    default:
      return level
  }
}

function normalizeRiskLevel(v: any): string {
  if (v === null || v === undefined) return ''
  const s = String(v).trim()
  if (!s) return ''
  // 中文值（与部分后端校验一致）
  if (s === '高') return 'HIGH'
  if (s === '中') return 'MID'
  if (s === '低') return 'LOW'
  if (s === '上') return 'TOP'

  const up = s.toUpperCase()
  if (up === 'MEDIUM') return 'MID'
  if (up === 'MIDDLE') return 'MID'
  if (up === 'MID') return 'MID'
  if (up === 'HIGH') return 'HIGH'
  if (up === 'LOW') return 'LOW'
  return up
}

function getGenderText(gender?: string | null): string {
  if (!gender) return '--'
  const s = String(gender).trim()
  if (!s) return '--'
  if (s === 'M' || s === '男' || s === '1') return '男'
  if (s === 'F' || s === '女' || s === '0') return '女'
  const up = s.toUpperCase()
  if (up === 'MALE') return '男'
  if (up === 'FEMALE') return '女'
  return s
}

function formatCaseId(id?: number | string | null) {
  if (id === undefined || id === null) return '--'
  const num = Number(id)
  if (Number.isNaN(num)) return String(id)
  return `1${String(Math.floor(num)).padStart(3, '0')}`
}

function formatDate(dateStr: string | null): string {
  if (!dateStr) return '--'
  try {
    const date = new Date(dateStr)
    return date.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
  } catch {
    return dateStr
  }
}

function goToPatientDetail(patientId: number) {
  if (!patientId) return
  router.push(`/patient/detail/${patientId}?from=risklist`)
}

function closeAddDialog() {
  showAddDialog.value = false
  newPatient.value = {
    name: '',
    gender: '',
    age: null,
    idCard: '',
    phone: '',
    disease: '',
    syndrome: '',
    constitution: '',
    familyHistory: '',
    riskLevel: ''
  }
  registerMiniProgram.value = false
  miniProgramPassword.value = ''
  miniProgramPasswordConfirm.value = ''
  doctorPassword.value = ''
  diseaseQuery.value = ''
  diseaseOptions.value = []
  diseaseLoading.value = false
  showDiseaseDropdown.value = false
  selectedDiseaseTypes.value = []
}

async function handleAddPatient() {
  if (!newPatient.value.idCard) {
    alert('请填写身份证号')
    return
  }
  if (!newPatient.value.riskLevel) {
    alert('请填写风险等级')
    return
  }
  if (!authStore.token) {
    alert('当前未登录医生账号，请先登录')
    return
  }
  if (!doctorPassword.value) {
    alert('请输入医生密码（用于创建患者账号）')
    return
  }

  // 如果勾选了注册小程序账号，需要验证手机号和密码
  if (registerMiniProgram.value) {
    if (!newPatient.value.phone) {
      alert('注册小程序账号需要填写手机号')
      return
    }
    if (!miniProgramPassword.value || miniProgramPassword.value.length < 6) {
      alert('小程序账号密码至少需要6位')
      return
    }
    if (miniProgramPassword.value !== miniProgramPasswordConfirm.value) {
      alert('两次输入的密码不一致')
      return
    }
  }

  adding.value = true
  try {
    const sex = newPatient.value.gender ? newPatient.value.gender : undefined
    // 允许“只输入不点选”：提交时把输入框内容视为一个病种加入
    commitTypedDisease()
    const selectedDiseases = selectedDiseaseTypes.value.map((x) => (x.value || '').trim()).filter(Boolean)
    const diseaseText = selectedDiseases.length ? selectedDiseases.join('，') : (newPatient.value.disease || '')
    const result = await patientApi.createPatientByDoctor({
      doctorId: Number(authStore.token),
      doctorPassword: doctorPassword.value,
      name: newPatient.value.name,
      phone: newPatient.value.phone,
      idCard: newPatient.value.idCard,
      disease: diseaseText,
      syndrome: newPatient.value.syndrome,
      constitution: newPatient.value.constitution,
      familyHistory: newPatient.value.familyHistory,
      riskLevel: newPatient.value.riskLevel,
      age: newPatient.value.age,
      sex
    })
    if (result.success) {
      // 新增患者成功后，再把“库里没有的病种”补录进病种库（失败不影响提示）
      ;(async () => {
        await syncNewDiseasesToDb(selectedDiseases)
      })()

      // 如果勾选了注册小程序账号，则调用注册接口
      if (registerMiniProgram.value && newPatient.value.phone && miniProgramPassword.value) {
        try {
          const registerResult = await patientApi.registerMiniProgramAccount({
            phone: newPatient.value.phone,
            password: miniProgramPassword.value,
            name: newPatient.value.name,
            idCard: newPatient.value.idCard
          })
          if (registerResult.success) {
            alert('患者添加成功，小程序账号注册成功！')
          } else {
            alert('患者添加成功，但小程序账号注册失败：' + (registerResult.message || '未知错误'))
          }
        } catch (error: any) {
          alert('患者添加成功，但小程序账号注册失败：' + (error.message || '网络错误'))
        }
      } else {
        alert('患者添加成功！')
      }
      closeAddDialog()
      loadData() // 重新加载数据
    } else {
      alert('添加失败：' + (result.message || '未知错误'))
    }
  } catch (error: any) {
    alert('添加失败：' + (error.message || '网络错误'))
  } finally {
    adding.value = false
  }
}
</script>

<style scoped>
.risk-list-page {
  /* 修复：避免固定宽度 + 横向裁切导致 hover scale 被左右遮挡 */
  width: 100%;
  max-width: 1120px;
  min-width: 0;
  margin: 0 auto;
  box-sizing: border-box;
  overflow: visible;
  padding-left: 12px;
  padding-right: 12px;
  overflow-y: visible;
  background: transparent;
}

/* =========================
   仅美化，不改布局（标题区）
   ========================= */
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

/* =========================
   修复：不要在本页复制全局 `.card` 逻辑
   仅为本页的“大卡 hover”做克制覆写，避免左右被遮挡
   ========================= */
.risk-list-big-card.card:hover {
  /* 1.004 ~ 1.008 范围内，最大不超过 1.008 */
  transform: scale(1.006);
  border-color: rgba(102,126,234,0.20);
  box-shadow:
    0 18px 40px rgba(102, 126, 234, 0.12),
    0 8px 20px rgba(15, 23, 42, 0.06),
    0 0 0 1px rgba(255, 255, 255, 0.12);
  z-index: 2;
}

.risk-list-big-card.card:hover::before {
  opacity: 0.95;
}

/* 保持两张大卡之间的垂直间距（不重写全局 `.card` 基础样式） */
.risk-list-big-card.card + .risk-list-big-card.card {
  margin-top: 16px;
}

.filter-bar {
  display: flex;
  gap: 12px;
  flex-wrap: nowrap;
  align-items: center;
  overflow-x: auto;
  padding: 16px 20px;
  /* 仅美化：筛选栏卡片化（不改变筛选项排列） */
  background: transparent;
  border-radius: 16px;
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
}

.filter-bar .input:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.12);
  background: #ffffff;
  outline: none;
}

.filter-bar .input-doctor {
  max-width: 140px;
}

.disease-select {
  position: relative;
}

/* 新增患者弹窗中的病种输入，独占一行 */
.disease-select-add {
  width: 100%;
}

.disease-select-add .input {
  width: 100%;
  max-width: 100%;
}

/* 病种下拉输入框右侧显示小箭头，视觉上接近原生 select（筛选栏用） */
.filter-bar .disease-select .input {
  padding-right: 28px;
  max-width: 220px;
}

.disease-select::after {
  content: '';
  position: absolute;
  right: 12px;
  top: 50%;
  margin-top: -3px;
  border-width: 6px 5px 0 5px;
  border-style: solid;
  border-color: #a0aec0 transparent transparent transparent;
  pointer-events: none;
}

.disease-dropdown-floating {
  position: absolute;
  z-index: 1600;
}

/* 病种筛选浮层：显示库内全部病种，列表高度适中、可滚动 */
.disease-dropdown-floating .dropdown {
  position: relative;
  left: 0;
  right: 0;
  top: 0;
  max-height: 200px;
  overflow-y: auto;
}


.flex-1 {
  flex: 1;
}

/* 仅美化：搜索框加轻量图标（不改变宽度关系） */
.search-input {
  padding-left: 40px;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='18' height='18' viewBox='0 0 24 24' fill='none'%3E%3Cpath d='M11 19a8 8 0 1 1 0-16 8 8 0 0 1 0 16Z' stroke='%2394a3b8' stroke-width='2' stroke-linecap='round'/%3E%3Cpath d='M21 21l-4.3-4.3' stroke='%2394a3b8' stroke-width='2' stroke-linecap='round'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: 14px 50%;
  background-size: 18px 18px;
}
.badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12.5px;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.badge-danger {
  background: linear-gradient(135deg, #fed7d7 0%, #feb2b2 100%);
  color: #c53030;
  border: 1px solid #fc8181;
}

.badge-warning {
  background: linear-gradient(135deg, #fefcbf 0%, #faf089 100%);
  color: #d69e2e;
  border: 1px solid #f6e05e;
}

.badge-success {
  background: linear-gradient(135deg, #c6f6d5 0%, #9ae6b4 100%);
  color: #2f855a;
  border: 1px solid #68d391;
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

.btn-secondary:hover {
  background: #ffffff;
  border-color: #cbd5e0;
  transform: translateY(-1px);
  box-shadow: 0 10px 22px rgba(15,23,42,0.06);
}

/* 仅美化：主按钮统一为渐变主按钮（不改位置/逻辑） */
.btn.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  color: #ffffff;
  box-shadow: 0 10px 22px rgba(102, 126, 234, 0.18);
  transition: transform 0.22s ease, box-shadow 0.22s ease;
}

.btn.btn-primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 14px 28px rgba(102, 126, 234, 0.24);
}

.btn:focus,
.btn:focus-visible {
  outline: none;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.14);
}

.text-muted {
  color: #a0aec0;
  font-size: 12px;
  font-style: italic;
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
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.page-btn.active {
  background: rgba(102, 126, 234, 0.12);
  border-color: rgba(102, 126, 234, 0.55);
  color: #4c51bf;
  cursor: default;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.12);
}

.page-btn.disabled {
  cursor: default;
  color: #a0aec0;
  background: rgba(248, 250, 252, 0.5);
  border-color: #f1f5f9;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.card-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 2px solid rgba(226, 232, 240, 0.3);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.card-header-row .card-title {
  font-size: 18px;
  font-weight: 700;
  color: #2d3748;
  margin-bottom: 0;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  /* 仅美化：遮罩半透明 + blur */
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
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  width: 90%;
  max-width: 600px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 
    0 25px 50px -12px rgba(0, 0, 0, 0.25),
    0 0 0 1px rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.3);
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
  font-size: 18px;
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
  transform: scale(1.1);
}

.modal-body {
  padding: 28px;
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
  font-weight: 700;
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
  background: #ffffff;
}

.input-error {
  margin-top: 6px;
  font-size: 12px;
  color: #e53e3e;
  font-weight: 600;
}

.selected-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}

.tag {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.12) 0%, rgba(118, 75, 162, 0.12) 100%);
  border: 1px solid rgba(102, 126, 234, 0.25);
  color: #4c51bf;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
  user-select: none;
}

.tag-remove {
  appearance: none;
  border: none;
  background: rgba(76, 81, 191, 0.12);
  color: #4c51bf;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 14px;
  line-height: 18px;
  padding: 0;
  transition: all 0.15s ease;
}

.tag-remove:hover {
  background: rgba(229, 62, 62, 0.12);
  color: #e53e3e;
  transform: scale(1.05);
}

.dropdown {
  position: absolute;
  left: 0;
  right: 0;
  top: calc(100% + 6px);
  background: rgba(255, 255, 255, 0.98);
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 12px;
  box-shadow:
    0 12px 30px rgba(0, 0, 0, 0.12),
    0 0 0 1px rgba(255, 255, 255, 0.2);
  overflow: hidden;
  z-index: 1200;
  max-height: 200px; /* 控制下拉高度适中，可滚动查看更多 */
  overflow-y: auto;
  backdrop-filter: blur(12px);
}

.dropdown-item {
  width: 100%;
  text-align: left;
  padding: 10px 14px;
  font-size: 13px;
  font-weight: 600;
  color: #2d3748;
  background: transparent;
  border: none;
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
}

.dropdown-item:hover {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.12) 0%, rgba(118, 75, 162, 0.12) 100%);
  color: #4c51bf;
}

.dropdown-item.muted {
  cursor: default;
  font-weight: 500;
  color: #a0aec0;
}

.dropdown-item.muted:hover {
  background: transparent;
  color: #a0aec0;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 2px solid rgba(226, 232, 240, 0.3);
}

.form-actions .btn {
  min-width: 100px;
  height: 44px;
  font-weight: 600;
}

.match-hint {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 16px 20px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
  border: 1px solid rgba(102, 126, 234, 0.2);
  border-radius: 12px;
  margin-bottom: 24px;
  font-size: 14px;
  color: #4c51bf;
  line-height: 1.5;
}

.match-hint .hint-icon {
  font-size: 18px;
  flex-shrink: 0;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-weight: 500;
  color: #2d3748;
  margin-bottom: 0 !important;
}

.checkbox-input {
  width: 18px;
  height: 18px;
  cursor: pointer;
  accent-color: #667eea;
}

.register-hint {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 12px 16px;
  margin-top: 12px;
  background: rgba(102, 126, 234, 0.08);
  border: 1px solid rgba(102, 126, 234, 0.2);
  border-radius: 8px;
  font-size: 13px;
  color: #4c51bf;
  line-height: 1.5;
}

.register-hint .hint-icon {
  font-size: 16px;
  flex-shrink: 0;
  margin-top: 2px;
}

/* 表格滚动容器优化 */
.risk-list-page .table-scroll {
  width: 100%;
  overflow-x: auto;
  overflow-y: visible;
  border-radius: 14px;
}

/* 表格最小宽度，确保所有列都能显示 */
.risk-list-page .table {
  width: 100%;
  min-width: 1200px;
  table-layout: fixed;
}

/* 优化表格列宽和padding，让表格更紧凑 */
.risk-list-page .table th,
.risk-list-page .table td {
  padding: 10px 12px;
  font-size: 13px;
  white-space: nowrap;          /* 确保一行显示 */
  overflow: hidden;             /* 超出隐藏 */
  text-overflow: ellipsis;      /* 超出省略号 */
  word-break: normal;
}

/* 仅美化：表头层次与线条更淡（不改列结构） */
.risk-list-page .table {
  border-collapse: collapse;
}

.risk-list-page .table thead th {
  background: linear-gradient(135deg, rgba(248, 250, 252, 0.95) 0%, rgba(241, 245, 249, 0.95) 100%);
  font-size: 12.5px;
  font-weight: 800;
  color: #64748b;
  letter-spacing: 0.4px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.9);
}

.risk-list-page .table tbody td {
  color: #1f2937;
  font-size: 13.5px;
  line-height: 1.55;
  border-bottom: 1px solid rgba(226, 232, 240, 0.65);
}

/* 核心列略加粗（不改结构） */
.risk-list-page .table tbody td:nth-child(1),
.risk-list-page .table tbody td:nth-child(2) {
  font-weight: 600;
  color: #0f172a;
}

/* 次信息灰度层级 */
.risk-list-page .table tbody td:nth-child(5),
.risk-list-page .table tbody td:nth-child(11) {
  color: #64748b;
}

/* 某些列设置固定宽度 */
.risk-list-page .table th:nth-child(1),
.risk-list-page .table td:nth-child(1) {
  width: 72px; /* 病案号（调窄） */
}

.risk-list-page .table th:nth-child(2),
.risk-list-page .table td:nth-child(2) {
  width: 80px; /* 姓名 */
}

.risk-list-page .table th:nth-child(3),
.risk-list-page .table td:nth-child(3) {
  width: 50px; /* 性别 */
}

.risk-list-page .table th:nth-child(4),
.risk-list-page .table td:nth-child(4) {
  width: 56px; /* 年龄 */
}

.risk-list-page .table th:nth-child(5),
.risk-list-page .table td:nth-child(5) {
  width: 116px; /* 手机号 */
}

.risk-list-page .table th:nth-child(6),
.risk-list-page .table td:nth-child(6) {
  width: 140px; /* 病种（给更多空间） */
}

.risk-list-page .table th:nth-child(7),
.risk-list-page .table td:nth-child(7) {
  width: 140px; /* 证型（给更多空间） */
}

.risk-list-page .table th:nth-child(8),
.risk-list-page .table td:nth-child(8) {
  width: 84px; /* 风险等级 */
}

.risk-list-page .table th:nth-child(9),
.risk-list-page .table td:nth-child(9) {
  width: 96px; /* 责任医生 */
}

.risk-list-page .table th:nth-child(10),
.risk-list-page .table td:nth-child(10) {
  width: 75px; /* 随访次数 */
}

.risk-list-page .table th:nth-child(11),
.risk-list-page .table td:nth-child(11) {
  width: 110px; /* 最近随访 */
}

.risk-list-page .table th:nth-child(12),
.risk-list-page .table td:nth-child(12) {
  width: 92px; /* 处理中告警 */
}

.row-clickable {
  cursor: pointer;
}

.row-clickable:hover {
  background: rgba(102, 126, 234, 0.06);
}
</style>

