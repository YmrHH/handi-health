<template>
  <div class="recommend-page">
    <div class="page-header">
      <!-- 仅美化，不改布局：标题徽章 + 渐变标题 -->
      <div class="page-title">
        <span class="title-badge" aria-hidden="true">
          <!-- Sparkles / Wand 风格图标（避免引入新依赖） -->
          <svg viewBox="0 0 24 24" fill="none" class="title-badge-icon">
            <path d="M12 2l.9 3.6L16.5 7l-3.6.9L12 11.5l-.9-3.6L7.5 7l3.6-1.4L12 2Z" stroke="currentColor" stroke-width="1.7" stroke-linejoin="round"/>
            <path d="M19 9l.6 2.2L22 12l-2.4.8L19 15l-.6-2.2L16 12l2.4-.8L19 9Z" stroke="currentColor" stroke-width="1.7" stroke-linejoin="round"/>
            <path d="M4 14l7-7 3 3-7 7H4v-3Z" stroke="currentColor" stroke-width="1.7" stroke-linejoin="round"/>
            <path d="M14 7l3 3" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/>
          </svg>
        </span>
        <span class="page-title-text">健康建议下发</span>
      </div>
      <div class="page-subtitle">基于告警和随访结果的个性化健康建议管理</div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon alert-icon">⚠️</div>
        <div class="stat-info">
          <div class="stat-value">{{ pendingAlertCount }}</div>
          <div class="stat-label">告警待下发</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon followup-icon">📋</div>
        <div class="stat-info">
          <div class="stat-value">{{ pendingFollowupCount }}</div>
          <div class="stat-label">随访待下发</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon sent-icon">✅</div>
        <div class="stat-info">
          <div class="stat-value">{{ sentTodayCount }}</div>
          <div class="stat-label">今日已下发</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon total-icon">📊</div>
        <div class="stat-info">
          <div class="stat-value">{{ totalPendingCount }}</div>
          <div class="stat-label">总待处理</div>
        </div>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="card">
      <form @submit.prevent="handleSearch" class="filter-bar">
        <select v-model="filters.sourceType" class="input">
          <option value="">全部来源</option>
          <option value="ALERT">异常告警触发</option>
          <option value="FOLLOWUP">随访完成触发</option>
        </select>

        <select v-model="filters.status" class="input">
          <option value="">全部状态</option>
          <option value="PENDING">待下发</option>
          <option value="SENT">已下发</option>
        </select>

        <select v-model="filters.riskLevel" class="input">
          <option value="">全部风险等级</option>
          <option value="HIGH">高危</option>
          <option value="MID">中危</option>
          <option value="LOW">低危</option>
        </select>

        <input 
          v-model="filters.keyword" 
          class="input flex-1" 
          type="text" 
          placeholder="患者姓名 / 病案号"
        />

        <button class="btn btn-primary" type="submit">筛选</button>
        <button class="btn btn-ghost" type="button" @click="handleReset">重置</button>
      </form>
    </div>

    <!-- 左右两列 -->
    <div class="card-grid cols-2">
      <!-- 左：待下发列表 -->
      <div class="card">
        <div class="card-title-header">
          <span>待下发建议列表</span>
        </div>
        <div v-if="loading" class="loading">加载中...</div>
        <div v-else class="table-scroll">
          <div v-if="loadError" class="empty" style="margin-bottom: 12px; color: #c00;">
            {{ loadError }}
            <button class="btn btn-sm btn-ghost" style="margin-left: 8px;" @click="loadData()">重试</button>
          </div>
          <table class="table">
          <thead>
            <tr>
              <th>来源</th>
              <th>患者</th>
              <th>风险等级</th>
              <th>触发原因</th>
              <th>触发时间</th>
              <th>状态</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="filteredList.length === 0">
                <td colspan="6" style="text-align: center; color: #999;">
                  暂无待下发建议
                  <button class="btn btn-sm btn-ghost" style="margin-left: 8px;" @click="useLocalTestItem()">使用本地测试条目</button>
                </td>
            </tr>
            <tr 
                v-for="item in paginatedList" 
              :key="item.id"
              :class="{ 'row-selected': selectedItemId === item.id }"
              @click="selectItem(item)"
              style="cursor: pointer;"
            >
              <td>
                <span :class="getSourceClass(item.sourceType)">
                  {{ item.sourceType === 'ALERT' ? '告警' : '随访' }}
                </span>
              </td>
              <td>{{ item.patientName }}</td>
              <td>
                <span v-if="normalizeRiskLevel(item.riskLevel)" :class="getRiskLevelClass(item.riskLevel)">
                  {{ getRiskLevelText(item.riskLevel) }}
                </span>
                <span v-else>--</span>
              </td>
              <td 
                class="trigger-reason"
                :title="item.triggerReason"
              >
                {{ item.triggerReason }}
              </td>
              <td>{{ formatDateTime(item.triggerTime) }}</td>
              <td>
                <span :class="getStatusClass(item.status)">
                  {{ item.status === 'PENDING' ? '待下发' : '已下发' }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
          <div class="table-pagination" v-if="filteredList.length > 0">
            <button 
              class="btn btn-secondary btn-sm" 
              :disabled="currentPage === 1"
              @click="goPage(currentPage - 1)"
            >上一页</button>
            <span class="page-info">第 {{ currentPage }} / {{ totalPages }} 页（共 {{ filteredList.length }} 条）</span>
            <button 
              class="btn btn-secondary btn-sm" 
              :disabled="currentPage === totalPages"
              @click="goPage(currentPage + 1)"
            >下一页</button>
          </div>
        </div>
      </div>

      <!-- 右：建议内容 -->
      <div class="card">
        <div class="card-title-header">
          <span>建议内容编辑</span>
        </div>
        
        <div v-if="!selectedItem" class="empty-preview">
          请从左侧列表选择一条记录
        </div>
        <div v-else class="recommend-preview">
          <!-- 患者信息卡片 -->
          <div class="patient-info-card">
            <div class="patient-header">
              <span class="patient-name">{{ selectedItem.patientName }}</span>
              <span v-if="normalizeRiskLevel(selectedItem.riskLevel)" :class="getRiskLevelClass(selectedItem.riskLevel)">
                {{ getRiskLevelText(selectedItem.riskLevel) }}
              </span>
              <span v-else class="badge">--</span>
            </div>
            <div class="patient-meta">
              <span class="meta-item">
                <span class="meta-label">来源：</span>
                {{ selectedItem.sourceType === 'ALERT' ? '异常告警触发' : '随访完成触发' }}
              </span>
              <span class="meta-item">
                <span class="meta-label">触发原因：</span>
                {{ selectedItem.triggerReason }}
              </span>
              <span class="meta-item">
                <span class="meta-label">体质：</span>
                {{ patientConstitution || selectedItem.bodyType || '未知' }}
              </span>
              <span class="meta-item">
                <span class="meta-label">证型：</span>
                {{ patientSyndrome || selectedItem.pattern || '未知' }}
              </span>
              <span class="meta-item">
                <span class="meta-label">病种：</span>
                {{ patientDisease || selectedItem.disease || '未知' }}
              </span>
              <span class="meta-item">
                <span class="meta-label">手机号：</span>
                {{ patientPhone || '--' }}
              </span>
              <span class="meta-item">
                <span class="meta-label">责任医生：</span>
                {{ patientDoctor || selectedItem.doctor || '未分配' }}
              </span>
            </div>
          </div>

          <!-- 建议内容编辑区 -->
          <div class="recommendation-section">
            <div class="section-title">建议内容</div>
            <textarea 
              v-model="editableRecommendation"
              class="recommend-content-editable"
              :placeholder="selectedItem.status === 'SENT' ? '该建议已下发' : '请输入建议内容，或点击AI生成建议按钮自动生成'"
              :readonly="selectedItem.status === 'SENT'"
              rows="12"
            ></textarea>
          </div>

          <!-- 操作按钮：两行布局，位于建议内容下方 -->
          <div v-if="selectedItem" class="recommend-actions">
            <div class="recommend-actions-row">
              <button 
                class="btn btn-sm btn-ghost" 
                @click="fillTestTemplate"
              >
                🧪 测试模板
              </button>
              <button 
                class="btn btn-sm btn-ghost" 
                @click="generateAISuggestion"
                :disabled="generatingAI"
              >
                {{ generatingAI ? '生成中...' : '🤖 AI生成建议' }}
              </button>
            </div>
            <div class="recommend-actions-row">
              <button 
                v-if="selectedItem.status === 'PENDING' && (selectedItem as any).patientId > 0" 
                class="btn btn-sm btn-secondary" 
                @click="handleSaveDraft"
                :disabled="!canSaveDraft"
              >
                {{ savingDraft ? '保存中...' : '保存草稿' }}
              </button>
              <button 
                v-if="selectedItem.status === 'PENDING' && (selectedItem as any).patientId > 0" 
                class="btn btn-sm btn-primary" 
                @click="handleSendSingle"
                :disabled="!canSend"
              >
                下发建议
              </button>
            </div>
          </div>

          <!-- 已下发信息 -->
          <div v-if="selectedItem.status === 'SENT'" class="sent-info">
            <span class="sent-icon">✅</span>
            <span>已于 {{ formatDateTime(selectedItem.sentTime) }} 下发</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import request from '@/api/request'

const route = useRoute()

interface RecommendItem {
  id: number
  sourceType: 'ALERT' | 'FOLLOWUP'
  sourceId: number
  patientId: number
  patientName: string
  riskLevel: string
  triggerReason: string
  triggerTime: string
  status: 'PENDING' | 'SENT'
  sentTime?: string
  bodyType?: string
  pattern?: string
  disease?: string
  doctor?: string
  recommendation?: string
}

const loading = ref(false)
const loadError = ref('')
const generatingAI = ref(false)
const savingDraft = ref(false)
const sending = ref(false)
const selectedItemId = ref<number | null>(null)
const editableRecommendation = ref('')
const pageSize = ref(8)
const currentPage = ref(1)

const patientProfile = ref<any | null>(null)
const patientProfileLoading = ref(false)

const routeContextItem = ref<RecommendItem | null>(null)

const filters = ref({
  sourceType: '',
  status: '',
  riskLevel: '',
  keyword: ''
})

// 所有数据
const allItems = ref<RecommendItem[]>([])

// 筛选后的列表
const filteredList = computed(() => {
  let filtered = [...allItems.value]
  
  if (filters.value.sourceType) {
    filtered = filtered.filter(item => item.sourceType === filters.value.sourceType)
  }
  
  if (filters.value.status) {
    filtered = filtered.filter(item => item.status === filters.value.status)
  }
  
  if (filters.value.riskLevel) {
    filtered = filtered.filter(item => normalizeRiskLevel(item.riskLevel) === normalizeRiskLevel(filters.value.riskLevel))
  }
  
  if (filters.value.keyword) {
    const keyword = filters.value.keyword.toLowerCase()
    filtered = filtered.filter(item => 
      item.patientName.toLowerCase().includes(keyword) ||
      String(item.patientId).includes(keyword)
    )
  }
  
  return filtered
})

// 选中的项目
const selectedItem = computed(() => {
  if (selectedItemId.value) {
    return allItems.value.find(item => item.id === selectedItemId.value) || null
  }
  return routeContextItem.value
})

const canSend = computed(() => {
  return !!(
    selectedItem.value &&
    selectedItem.value.status === 'PENDING' &&
    Number((selectedItem.value as any).patientId || 0) > 0 &&
    !sending.value &&
    editableRecommendation.value.trim()
  )
})

const canSaveDraft = computed(() => {
  return !!(
    selectedItem.value &&
    selectedItem.value.status === 'PENDING' &&
    Number((selectedItem.value as any).patientId || 0) > 0 &&
    !savingDraft.value &&
    editableRecommendation.value.trim()
  )
})

const totalPages = computed(() => {
  const total = filteredList.value.length
  return total > 0 ? Math.ceil(total / pageSize.value) : 1
})

const paginatedList = computed(() => {
  if (filteredList.value.length === 0) return []
  const start = (currentPage.value - 1) * pageSize.value
  return filteredList.value.slice(start, start + pageSize.value)
})

// 统计数据
const pendingAlertCount = computed(() => 
  allItems.value.filter(item => item.sourceType === 'ALERT' && item.status === 'PENDING').length
)

const pendingFollowupCount = computed(() => 
  allItems.value.filter(item => item.sourceType === 'FOLLOWUP' && item.status === 'PENDING').length
)

const sentTodayCount = computed(() => {
  const today = new Date().toISOString().split('T')[0]
  return allItems.value.filter(item => 
    item.status === 'SENT' && item.sentTime?.startsWith(today)
  ).length
})

const totalPendingCount = computed(() => 
  allItems.value.filter(item => item.status === 'PENDING').length
)

// 监听选中项变化
watch(selectedItem, (item) => {
  if (item) {
    editableRecommendation.value = (item as any).recommendation || (item as any).text || (item as any).content || (item as any).advice || (item as any).suggestion || ''
  } else {
    editableRecommendation.value = ''
  }
})

watch(
  () => selectedItem.value?.patientId,
  async (pid) => {
    patientProfile.value = null
    if (!pid || Number(pid) <= 0) return
    patientProfileLoading.value = true
    try {
      // /api/patient/detail 返回 patient.phone / patient.constitution 等完整字段
      const resp = await request.get<any>(`/api/patient/detail?patientId=${Number(pid)}`)
      const layer1: any = (resp as any)?.data ?? resp
      const payload: any = layer1?.data ?? layer1
      const data: any = payload?.data ?? payload
      patientProfile.value = data
    } catch (e) {
      patientProfile.value = null
    } finally {
      patientProfileLoading.value = false
    }
  },
  { immediate: true }
)

const patientPhone = computed(() => {
  const v = patientProfile.value?.patient?.phone ?? patientProfile.value?.phone
  return v == null ? '' : String(v)
})

const patientDisease = computed(() => {
  const v = patientProfile.value?.patient?.disease ?? patientProfile.value?.disease
  return v == null ? '' : String(v)
})

const patientSyndrome = computed(() => {
  const v = patientProfile.value?.patient?.syndrome ?? patientProfile.value?.syndrome
  return v == null ? '' : String(v)
})

const patientConstitution = computed(() => {
  const v = patientProfile.value?.patient?.constitution ?? patientProfile.value?.constitution
  return v == null ? '' : String(v)
})

const patientDoctor = computed(() => {
  const v =
    patientProfile.value?.patient?.responsibleDoctor ??
    patientProfile.value?.patient?.doctor ??
    patientProfile.value?.doctor
  return v == null ? '' : String(v)
})

watch(filteredList, () => {
  currentPage.value = 1
})

onMounted(() => {
  initPage()
})

async function initPage() {
  routeContextItem.value = null
  await loadData()
}

async function loadData(ensureTried = false) {
  loading.value = true
  try {
    loadError.value = ''
    const params: any = {}
    const response = await request.get('/api/intervention/recommend/list', { params })
    const respAny: any = response
    const layer1 = respAny?.data ?? respAny
    const payload = layer1?.data ?? layer1
    const rows = payload?.rows || payload?.list || []
    allItems.value = Array.isArray(rows)
      ? (rows as any[]).map((r: any) => {
          const rawStatus = String(r?.status ?? '').trim().toUpperCase()
          const status = rawStatus === 'SENT' ? 'SENT' : 'PENDING'
          const rawSource = String(r?.sourceType ?? r?.source ?? '').trim().toUpperCase()
          const sourceType = rawSource === 'FOLLOWUP' ? 'FOLLOWUP' : 'ALERT'
          const riskLevel = normalizeRiskLevel(r?.riskLevel ?? r?.risk_level ?? r?.riskLevelText ?? r?.risk ?? '')
          const recommendation = String(r?.recommendation ?? r?.text ?? r?.content ?? r?.advice ?? r?.suggestion ?? '').trim()
          return {
            id: Number(r?.id ?? r?.recommendId ?? 0),
            sourceType,
            sourceId: Number(r?.sourceId ?? 0),
            patientId: Number(r?.patientId ?? 0),
            patientName: String(r?.patientName ?? r?.name ?? ''),
            riskLevel,
            triggerReason: String(r?.triggerReason ?? r?.reason ?? ''),
            triggerTime: String(r?.triggerTime ?? r?.createdAt ?? r?.time ?? ''),
            status,
            sentTime: r?.sentTime,
            bodyType: r?.bodyType,
            pattern: r?.pattern,
            disease: r?.disease,
            doctor: r?.doctor,
            recommendation
          } as RecommendItem
        })
      : []

    // 检查是否从告警页面跳转过来：仅用于自动选中（不再前端新建数据）
    const source = route.query.source as string
    const patientName = route.query.patientName as string
    const patientId = route.query.patientId as string
    const alertId = route.query.alertId as string
    const alertType = route.query.alertType as string

    if (source === 'alert') {
      const pid = Number(patientId)
      const aid = Number(alertId)

      let found: RecommendItem | undefined
      if (!isNaN(aid) && aid) {
        found = allItems.value.find(item => item.sourceType === 'ALERT' && Number(item.sourceId) === aid)
      }

      if (!found && (patientName || (!isNaN(pid) && pid))) {
        found = allItems.value.find(item =>
          (patientName ? item.patientName === patientName : true) &&
          (!isNaN(pid) && pid ? Number(item.patientId) === pid : true) &&
          item.status === 'PENDING'
        )
      }

      if (found) {
        routeContextItem.value = null
        selectedItemId.value = found.id
      } else {
        if (!ensureTried && !isNaN(pid) && pid) {
          try {
            const ensureResp = await request.post('/api/intervention/recommend/ensure', {
              sourceType: 'ALERT',
              sourceId: !isNaN(aid) ? aid : null,
              patientId: pid,
              patientName: patientName || '患者',
              triggerReason: alertType || '',
              triggerTime: new Date().toISOString().slice(0, 16).replace('T', ' ')
            })
            const ensureAny: any = ensureResp
            const ensureLayer1 = ensureAny?.data ?? ensureAny
            const ensurePayload = ensureLayer1?.data ?? ensureLayer1
            const newId = Number(ensurePayload?.id)
            if (newId && !isNaN(newId)) {
              await loadData(true)
              selectedItemId.value = newId
              routeContextItem.value = null
              return
            }
          } catch (e) {
            // 忽略，继续走本地占位显示
          }
        }

        if (!isNaN(pid) && pid) {
          routeContextItem.value = {
            id: 0,
            sourceType: 'ALERT',
            sourceId: !isNaN(aid) ? aid : 0,
            patientId: pid,
            patientName: patientName || '患者',
            riskLevel: '',
            triggerReason: alertType || '',
            triggerTime: new Date().toISOString().slice(0, 16).replace('T', ' '),
            status: 'PENDING',
            recommendation: ''
          }
          selectedItemId.value = null
        }
      }
    }
  } catch (error) {
    console.error('加载建议列表失败:', error)
    loadError.value = (error as any)?.message ? String((error as any).message) : '加载建议列表失败'
    allItems.value = []
  } finally {
    loading.value = false
  }
}

function useLocalTestItem() {
  routeContextItem.value = {
    id: 0,
    sourceType: 'ALERT',
    sourceId: 0,
    patientId: 0,
    patientName: '测试患者',
    riskLevel: 'HIGH',
    triggerReason: '测试：血压波动（仅用于联调 AI）',
    triggerTime: new Date().toISOString().slice(0, 16).replace('T', ' '),
    status: 'PENDING',
    recommendation: ''
  }
  selectedItemId.value = null
}

function selectItem(item: RecommendItem) {
  selectedItemId.value = item.id
  routeContextItem.value = null
}

function goPage(page: number) {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
}

function handleSearch() {
  // 前端筛选，computed 自动响应
}

function handleReset() {
  filters.value = {
    sourceType: '',
    status: '',
    riskLevel: '',
    keyword: ''
  }
}

async function generateAISuggestion() {
  if (!selectedItem.value) return
  
  generatingAI.value = true
  try {
    const item = selectedItem.value
    const response = await request.post('/api/intervention/recommend/ai-suggest', {
      id: item.id > 0 ? item.id : null,
      sourceType: item.sourceType,
      sourceId: item.sourceId,
      patientId: item.patientId,
      patientName: item.patientName,
      riskLevel: item.riskLevel,
      triggerReason: item.triggerReason,
      triggerTime: item.triggerTime,
      bodyType: item.bodyType,
      pattern: item.pattern,
      disease: item.disease,
      doctor: item.doctor
    })
    const respAny: any = response
    const layer1 = respAny?.data ?? respAny
    const payload = layer1?.data ?? layer1
    editableRecommendation.value = payload?.text || payload?.recommendation || ''
  } finally {
    generatingAI.value = false
  }
}

function fillTestTemplate() {
  if (!selectedItem.value) return
  const item = selectedItem.value
  const name = item.patientName || '患者'
  const risk = item.riskLevel ? getRiskLevelText(item.riskLevel) : '未知'
  const reason = item.triggerReason || '近期监测指标波动'
  editableRecommendation.value =
    `尊敬的${name}：\n\n` +
    `【风险提示】您当前风险等级：${risk}。请继续关注相关指标变化。\n\n` +
    `【原因与解读】本次触发原因：${reason}。这通常提示近期状态存在波动，需要加强自我管理与监测。\n\n` +
    `【生活方式建议】\n` +
    `1) 饮食：清淡少盐少油，控制高糖高脂；\n` +
    `2) 运动：根据体力每周≥5天、每次20-30分钟中等强度活动；\n` +
    `3) 作息：规律作息，避免熬夜，保证睡眠；\n` +
    `4) 监测：按医嘱监测血压/血糖/体重，记录并反馈异常。\n\n` +
    `【用药与复诊提醒】请按时服药，不要自行停药/加量；如出现胸闷胸痛、呼吸困难、头晕加重等不适，请及时就医或联系医生复诊。\n`
}

async function handleSaveDraft() {
  if (!selectedItem.value || !editableRecommendation.value.trim() || savingDraft.value) return
  if (Number((selectedItem.value as any).patientId || 0) <= 0) {
    alert('当前为本地测试条目（无患者ID），不支持保存草稿。请先确保左侧列表加载到真实数据。')
    return
  }
  savingDraft.value = true
  try {
    const item = selectedItem.value
    const resp = await request.post('/api/intervention/recommend/save', {
      id: item.id > 0 ? item.id : null,
      sourceType: item.sourceType,
      sourceId: item.sourceId,
      patientId: item.patientId,
      patientName: item.patientName,
      riskLevel: item.riskLevel,
      triggerReason: item.triggerReason,
      triggerTime: item.triggerTime,
      bodyType: item.bodyType,
      pattern: item.pattern,
      disease: item.disease,
      doctor: item.doctor,
      recommendation: editableRecommendation.value
    })
    const data = (resp as any)?.data ?? resp
    const newId = Number((data as any)?.id)
    if (newId && !isNaN(newId)) {
      await loadData()
      selectedItemId.value = newId
      routeContextItem.value = null
    }
  } catch (error: any) {
    console.error('保存失败:', error)
    alert('保存失败：' + (error.message || '未知错误'))
  } finally {
    savingDraft.value = false
  }
}

async function handleSendSingle() {
  if (!selectedItem.value || !editableRecommendation.value.trim() || sending.value) return
  if (Number((selectedItem.value as any).patientId || 0) <= 0) {
    alert('当前为本地测试条目（无患者ID），不支持下发。请先确保左侧列表加载到真实数据。')
    return
  }
  sending.value = true
  try {
    const item = selectedItem.value
    const resp = await request.post('/api/intervention/recommend/send', {
      id: item.id > 0 ? item.id : null,
      sourceType: item.sourceType,
      sourceId: item.sourceId,
      patientId: item.patientId,
      patientName: item.patientName,
      riskLevel: item.riskLevel,
      triggerReason: item.triggerReason,
      triggerTime: item.triggerTime,
      bodyType: item.bodyType,
      pattern: item.pattern,
      disease: item.disease,
      doctor: item.doctor,
      recommendation: editableRecommendation.value
    })

    const data = (resp as any)?.data ?? resp
    const newId = Number((data as any)?.id)

    if (newId && !isNaN(newId)) {
      await loadData()
      selectedItemId.value = newId
      routeContextItem.value = null
    } else {
      const local = allItems.value.find(i => i.id === item.id)
      if (local) {
        local.status = 'SENT'
        local.sentTime = new Date().toISOString().slice(0, 16).replace('T', ' ')
        local.recommendation = editableRecommendation.value
      }
    }

    alert(`已成功向患者 ${item.patientName} 下发个性化建议！`)
  } catch (error: any) {
    console.error('下发失败:', error)
    alert('下发失败：' + (error.message || '未知错误'))
  } finally {
    sending.value = false
  }
}

function formatDateTime(dateStr?: string): string {
  if (!dateStr) return '--'
  return dateStr
}

function getSourceClass(sourceType: string): string {
  return sourceType === 'ALERT' ? 'source-badge source-alert' : 'source-badge source-followup'
}

function getRiskLevelClass(level: string): string {
  switch (normalizeRiskLevel(level)) {
    case 'HIGH': return 'badge badge-danger'
    case 'MID': return 'badge badge-warning'
    case 'LOW': return 'badge badge-success'
    default: return 'badge'
  }
}

function getRiskLevelText(level: string): string {
  switch (normalizeRiskLevel(level)) {
    case 'HIGH': return '高危'
    case 'MID': return '中危'
    case 'LOW': return '低危'
    default: return level
  }
}

function normalizeRiskLevel(v: any): string {
  if (v === null || v === undefined) return ''
  const s = String(v).trim()
  if (!s) return ''
  if (s === '高' || s === '高危') return 'HIGH'
  if (s === '中' || s === '中危') return 'MID'
  if (s === '低' || s === '低危') return 'LOW'
  const up = s.toUpperCase()
  if (up === 'HIGH_RISK' || up === 'HIGH-RISK') return 'HIGH'
  if (up === 'MEDIUM_RISK' || up === 'MEDIUM-RISK') return 'MID'
  if (up === 'LOW_RISK' || up === 'LOW-RISK') return 'LOW'
  if (up === 'MEDIUM') return 'MID'
  if (up === 'MIDDLE') return 'MID'
  if (up === 'MID') return 'MID'
  if (up === 'HIGH') return 'HIGH'
  if (up === 'LOW') return 'LOW'
  return up
}

function getStatusClass(status: string): string {
  return status === 'PENDING' ? 'status-badge status-pending' : 'status-badge status-sent'
}
</script>

<style scoped>
/* =========================
   仅美化，不改布局（intervention/Recommend.vue）
   ========================= */

.recommend-page {
  /* 修复：避免固定宽度 + overflow-x:auto 裁切 hover scale */
  width: 100%;
  max-width: 1120px;
  min-width: 0;
  margin: 0 auto;
  box-sizing: border-box;
  overflow: visible;
  overflow-y: visible;
  padding-inline: 12px;
  -webkit-overflow-scrolling: touch;
  background: transparent;
}

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
textarea:focus-visible,
button:focus-visible {
  outline: none;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
  overflow: visible;
  padding-left: 10px;
  padding-right: 10px;
}

.stat-card {
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 20px 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 10px 30px rgba(102,126,234,0.08), 0 4px 12px rgba(15,23,42,0.04);
  border: 1px solid rgba(226, 232, 240, 0.70);
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

.stat-card::before {
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

.stat-card:hover {
  box-shadow: 0 14px 36px rgba(102,126,234,0.10), 0 8px 18px rgba(15,23,42,0.06);
  border-color: rgba(203, 213, 225, 0.78);
}

/* 四列密集统计卡：hover 更克制，避免“撞边” */
.stats-row .stat-card:hover {
  /* 缩小 hover scale：1.01 ~ 1.014 */
  transform: scale(1.012);
}

.stat-card:hover::before {
  opacity: 0.94;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.alert-icon {
  background: linear-gradient(135deg, #fed7d7 0%, #feb2b2 100%);
}

.followup-icon {
  background: linear-gradient(135deg, #bee3f8 0%, #90cdf4 100%);
}

.sent-icon {
  background: linear-gradient(135deg, #c6f6d5 0%, #9ae6b4 100%);
}

.total-icon {
  background: linear-gradient(135deg, #e9d8fd 0%, #d6bcfa 100%);
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #2d3748;
  line-height: 1;
}

.stat-label {
  font-size: 13px;
  color: #718096;
  margin-top: 4px;
  font-weight: 500;
}

.card-title-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.7);
  position: relative;
}

.card-title-header > span {
  position: relative;
  padding-left: 12px;
}

.card-title-header > span::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 16px;
  border-radius: 999px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.header-actions {
  display: flex;
  gap: 8px;
}

.recommend-actions {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}

.recommend-actions-row {
  display: flex;
  justify-content: flex-start;
  gap: 12px;
}

.btn-sm {
  padding: 8px 16px;
  font-size: 13px;
  font-weight: 600;
  border-radius: 8px;
  transition: all 0.2s ease;
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
  border: 0;
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
}

.flex-1 {
  flex: 1;
  min-width: 150px;
}

.source-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.2px;
}

.source-alert {
  background: linear-gradient(135deg, #fed7d7 0%, #feb2b2 100%);
  color: #c53030;
}

.source-followup {
  background: linear-gradient(135deg, #bee3f8 0%, #90cdf4 100%);
  color: #2c5282;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.2px;
}

.status-pending {
  background: linear-gradient(135deg, rgba(254, 243, 199, 0.95) 0%, rgba(253, 230, 138, 0.95) 100%);
  color: #b45309;
  border: 1px solid rgba(245, 158, 11, 0.35);
}

.status-sent {
  background: linear-gradient(135deg, rgba(209, 250, 229, 0.95) 0%, rgba(167, 243, 208, 0.95) 100%);
  color: #047857;
  border: 1px solid rgba(52, 211, 153, 0.45);
}

.trigger-reason {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 调整左右布局比例，使待下发列表区域更宽一些，100% 缩放下更易完整展示 */
.recommend-page .card-grid.cols-2 {
  grid-template-columns: 1.5fr 1fr;
  align-items: stretch;
}

/* 让两侧卡片在网格中高度拉满，视觉更统一 */
.recommend-page .card {
  height: 100%;
}

/* =========================
   修复：左右两列大卡 hover 遮挡
   - 只对 cols-2 内的大卡做克制 hover 缩放
   - 不改布局，只修遮挡
   ========================= */
.recommend-page .card-grid.cols-2 {
  overflow: visible;
}

.recommend-page .card-grid.cols-2 .card:hover {
  transform: scale(1.006);
  border-color: rgba(102,126,234,0.20);
  box-shadow:
    0 18px 40px rgba(102, 126, 234, 0.14),
    0 8px 20px rgba(15, 23, 42, 0.06);
}

/* 缩小表格行高与字体，在当前宽度下容纳更多列内容 */
.recommend-page .table th,
.recommend-page .table td {
  padding: 10px 12px;
  font-size: 13px;
}

.table-pagination {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
}

.table-pagination .page-info {
  color: #4a5568;
  font-size: 13px;
}

.empty-preview {
  text-align: center;
  padding: 60px 20px;
  color: #a0aec0;
  font-size: 16px;
  font-weight: 500;
  background: rgba(247, 250, 252, 0.5);
  border-radius: 12px;
  border: 2px dashed rgba(160, 174, 192, 0.3);
}

.empty-preview::before {
  content: '📋';
  display: block;
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.recommend-preview {
  padding: 0;
}

.patient-info-card {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
  border: 1px solid rgba(102, 126, 234, 0.2);
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
}

.patient-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.patient-name {
  font-size: 20px;
  font-weight: 700;
  color: #2d3748;
}

.patient-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.meta-item {
  font-size: 13px;
  color: #4a5568;
  padding: 6px 12px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 8px;
  font-weight: 500;
}

.meta-label {
  color: #718096;
}

.recommendation-section {
  margin-bottom: 20px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #2d3748;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-title::before {
  content: '';
  width: 4px;
  height: 14px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 2px;
}

.recommend-content-editable {
  width: 100%;
  padding: 20px;
  background: rgba(255, 255, 255, 0.9);
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  line-height: 1.8;
  color: #2d3748;
  font-size: 14px;
  font-family: inherit;
  resize: vertical;
  min-height: 300px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.recommend-content-editable:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
  background: #ffffff;
}

.recommend-content-editable:read-only {
  background: rgba(247, 250, 252, 0.8);
  cursor: not-allowed;
}

.recommend-content-editable::placeholder {
  color: #a0aec0;
  font-weight: 400;
  font-style: italic;
}

.sent-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #c6f6d5 0%, #9ae6b4 100%);
  border-radius: 8px;
  color: #2f855a;
  font-weight: 600;
  font-size: 14px;
}

.sent-info .sent-icon {
  font-size: 18px;
}

.row-selected {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%) !important;
  border-left: 4px solid #667eea;
}

.badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.2px;
}

.badge-danger {
  background: linear-gradient(135deg, rgba(254, 226, 226, 0.95) 0%, rgba(254, 202, 202, 0.95) 100%);
  color: #b91c1c;
  border: 1px solid rgba(248, 113, 113, 0.35);
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

/* 表格样式 */
.table thead th {
  background: linear-gradient(180deg, rgba(102, 126, 234, 0.10) 0%, rgba(118, 75, 162, 0.06) 100%);
  color: #64748b;
  font-size: 12.5px;
  font-weight: 800;
  letter-spacing: 0.3px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.85);
}

.table td {
  color: #1f2937;
  border-bottom: 1px solid rgba(226, 232, 240, 0.70);
}

.table tbody tr {
  transition: background 0.22s ease;
  cursor: pointer;
}

.table tbody tr:hover {
  background: rgba(102, 126, 234, 0.08);
}

input[type="checkbox"] {
  width: 16px;
  height: 16px;
  accent-color: #667eea;
  cursor: pointer;
}

/* 响应式 */
@media (max-width: 1200px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: 1fr;
  }
}
</style>
