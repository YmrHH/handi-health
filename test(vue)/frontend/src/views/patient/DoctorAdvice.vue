<template>
  <div class="doctor-advice-page">
    <div class="page-header">
      <!-- 仅美化，不改布局：标题徽章 + 渐变标题 -->
      <div class="page-title">
        <span class="title-badge" aria-hidden="true">
          <!-- ClipboardPen / FileHeart 风格图标（避免引入新依赖） -->
          <svg viewBox="0 0 24 24" fill="none" class="title-badge-icon">
            <path d="M8 4h8" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
            <path d="M8 4a2 2 0 0 0-2 2v1h12V6a2 2 0 0 0-2-2" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/>
            <path d="M6 7h12a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2Z" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/>
            <path d="M9 16l6-6" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
            <path d="M9 16l-.5 2.5L11 18l-2-2Z" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/>
          </svg>
        </span>
        <span class="page-title-text">医学建议数据留存</span>
      </div>
      <div class="page-subtitle">医生建议内容的集中展示与留存</div>
    </div>

    <div class="card">
      <div class="card-title">
        <span class="title-icon">💡</span>
        <span>医生建议列表</span>
      </div>
      <form class="filter-bar" @submit.prevent="handleSearch">
        <input v-model="filters.keyword" class="input" type="text" placeholder="标题/内容关键词" />
        <input v-model="filters.startDate" class="input" type="datetime-local" />
        <input v-model="filters.endDate" class="input" type="datetime-local" />
        <button class="btn btn-primary" type="submit">筛选</button>
        <button class="btn btn-ghost" type="button" @click="handleReset">重置</button>
      </form>

      <div v-if="loading" class="loading loading-inline">加载中...</div>
      <template v-else>
        <div v-if="loadError" class="empty-message">{{ loadError }}</div>
        <div v-else-if="rows.length > 0" class="recommendations-list">
          <div
            v-for="rec in rows"
            :key="rec.id"
            class="recommendation-item"
            style="cursor: pointer;"
            @click="openDetail(rec.id)"
          >
            <div class="rec-header">
              <span class="rec-date">{{ rec.adviceDate || '--' }}</span>
              <span class="rec-doctor">{{ rec.doctorName || '医生' }}</span>
            </div>
            <div class="rec-content">
              <p class="rec-title">{{ rec.title }}</p>
              <!-- 仅显示优化：每条卡片直接展示“建议正文”滚动块（不改整体布局） -->
              <div
                class="rec-content-block"
                @click.stop
                @mousedown.stop
                @wheel.stop
              >
                {{ rec.contentText }}
              </div>
              <div v-if="rec.patients && rec.patients.length > 0" class="rec-patients">
                <span class="rec-patients-label">相关患者：</span>
                <span class="rec-patients-list">{{ rec.patients.join('、') }}</span>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="empty-message">暂无医生建议数据</div>

        <div v-if="totalPages > 1" class="pagination">
          <button class="btn btn-secondary" :disabled="page <= 1" @click="goPage(page - 1)">上一页</button>
          <span class="page-info">第 {{ page }} / {{ totalPages }} 页（共 {{ total }} 条）</span>
          <button class="btn btn-secondary" :disabled="page >= totalPages" @click="goPage(page + 1)">下一页</button>
        </div>
      </template>
    </div>

    <div v-if="showDetail" class="modal-backdrop" @click.self="closeDetail">
      <div class="modal">
        <div class="modal-header">
          <h3>建议详情</h3>
          <button class="btn btn-ghost" @click="closeDetail">✕</button>
        </div>
        <div class="modal-body">
          <div class="detail-line"><span class="label">日期：</span>{{ detail?.adviceDate || '--' }}</div>
          <div class="detail-line"><span class="label">医生：</span>{{ detail?.doctorName || '医生' }}</div>
          <div class="detail-line"><span class="label">标题：</span>{{ detail?.title || '' }}</div>
          <div class="detail-line" v-if="detail?.patients && detail.patients.length">
            <span class="label">相关患者：</span>{{ detail.patients.join('、') }}
          </div>
          <div class="detail-content">{{ resolveAdviceText(detail) }}</div>
          <div v-if="detailError" class="form-error">{{ detailError }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { systemApi, type DoctorAdviceRow } from '@/api/system'

const loading = ref(false)
type DoctorAdviceListRow = DoctorAdviceRow & { contentText: string }
const rows = ref<DoctorAdviceListRow[]>([])
const loadError = ref('')

const EMPTY_ADVICE_TEXT = '暂无建议内容'

// 公共正文解析：content -> description -> adviceContent -> summary -> remark
function resolveAdviceText(record: any): string {
  if (!record) return EMPTY_ADVICE_TEXT
  const candidates = [
    record?.content,
    record?.description,
    record?.adviceContent,
    record?.summary,
    record?.remark
  ]
  for (const v of candidates) {
    if (typeof v === 'string' && v.trim()) return v
    if (v !== undefined && v !== null) {
      const s = String(v)
      if (s.trim()) return s
    }
  }
  return EMPTY_ADVICE_TEXT
}

const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const filters = ref({
  keyword: '',
  startDate: '',
  endDate: ''
})

const showDetail = ref(false)
const detail = ref<DoctorAdviceRow | null>(null)
const detailLoading = ref(false)
const detailError = ref('')

onMounted(() => {
  loadData()
})

async function loadData() {
  loading.value = true
  loadError.value = ''
  try {
    const result = await systemApi.getDoctorAdviceList({
      page: page.value,
      pageSize: pageSize.value,
      keyword: filters.value.keyword || undefined,
      startDate: filters.value.startDate || undefined,
      endDate: filters.value.endDate || undefined
    })
    const payload: any = result.data as any
    const data = payload?.data ?? payload
    const rawRows = Array.isArray(data?.rows) ? data.rows : []
    // 1) 先用列表数据生成 rows
    rows.value = rawRows.map((row: any) => ({
      ...row,
      contentText: resolveAdviceText(row)
    }))
    total.value = Number(data?.total ?? 0) || 0

    // 2) 对正文为空的记录，再调用详情接口补全正文并回填 contentText
    const needEnrichIds = rows.value
      .filter(r => r.contentText === EMPTY_ADVICE_TEXT)
      .map(r => Number(r.id))
      .filter(id => !Number.isNaN(id))

    if (needEnrichIds.length > 0) {
      await Promise.all(
        needEnrichIds.map(async (id) => {
          try {
            const res = await systemApi.getDoctorAdviceDetail(id)
            if (res?.success && res?.data) {
              const text = resolveAdviceText(res.data as any)
              const idx = rows.value.findIndex(r => Number(r.id) === id)
              if (idx !== -1) rows.value[idx].contentText = text
            }
          } catch (e) {
            console.error('补全建议正文失败:', e)
          }
        })
      )
    }
  } catch (error) {
    console.error('加载医生建议数据失败:', error)
    rows.value = []
    total.value = 0
    loadError.value = (error as any)?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  loadData()
}

function handleReset() {
  filters.value = { keyword: '', startDate: '', endDate: '' }
  page.value = 1
  loadData()
}

function goPage(p: number) {
  page.value = p
  loadData()
}

const totalPages = computed(() => {
  return total.value > 0 ? Math.ceil(total.value / pageSize.value) : 1
})

async function openDetail(id: number) {
  showDetail.value = true
  detail.value = null
  detailError.value = ''
  detailLoading.value = true
  try {
    const res = await systemApi.getDoctorAdviceDetail(id)
    if (res.success && res.data) {
      detail.value = res.data as any
    } else {
      detailError.value = res.message || '加载详情失败'
    }
  } catch (e: any) {
    detailError.value = e.message || '加载详情失败'
  } finally {
    detailLoading.value = false
  }
}

function closeDetail() {
  showDetail.value = false
}
</script>

<style scoped>
.doctor-advice-page {
  width: 1120px;
  min-width: 1120px;
  margin: 0 auto;
  box-sizing: border-box;
}

/* 仅修复：本页大白卡允许子内容溢出显示（不改布局结构） */
.doctor-advice-page > .card {
  overflow: visible !important;
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
  margin-top: 4px;
  font-size: 14px;
  color: #94a3b8;
}

.loading {
  text-align: center;
  padding: 40px;
  color: #64748b;
}

.loading-inline {
  padding: 24px 0;
}

.btn:focus,
.btn:focus-visible,
.input:focus-visible,
textarea:focus-visible,
button:focus-visible {
  outline: none;
}

.filter-bar {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.input {
  height: 40px;
  border-radius: 8px;
  border: 2px solid #e2e8f0;
  padding: 0 12px;
  font-size: 14px;
  background: rgba(255, 255, 255, 0.9);
  font-weight: 500;
  transition: all 0.22s ease;
}

.input:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.12);
  background: #ffffff;
}

.btn {
  height: 40px;
  padding: 0 14px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  border: none;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.btn-ghost {
  background: #fff;
  border: 2px solid #e2e8f0;
  color: #4a5568;
}

.btn-secondary {
  background: #fff;
  border: 2px solid #e2e8f0;
  color: #4a5568;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
}

.page-info {
  font-size: 13px;
  color: #4a5568;
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.55);
  backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
  padding: 16px;
}

.modal {
  width: 720px;
  max-width: 100%;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(14px);
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid rgba(226, 232, 240, 0.7);
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.22);
  max-height: 90vh;
}

.modal-header {
  padding: 12px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(248, 250, 252, 0.65);
  border-bottom: 1px solid rgba(226, 232, 240, 0.75);
}

.modal-body {
  padding: 16px;
  max-height: calc(90vh - 64px);
  overflow-y: auto;
  overflow-x: hidden;
}

.detail-line {
  margin-bottom: 8px;
  font-size: 14px;
}

.detail-line .label {
  color: #4a5568;
  font-weight: 600;
  margin-right: 6px;
}

.detail-content {
  margin-top: 12px;
  white-space: pre-wrap;
  font-size: 14px;
  line-height: 1.7;
  color: #1f2937;
  padding: 12px;
  border: 2px solid #e2e8f0;
  border-radius: 10px;
  background: rgba(247, 250, 252, 0.6);
}

.form-error {
  margin-top: 10px;
  color: #c53030;
  font-size: 12px;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
  position: relative;
  padding-left: 12px;
}

.card-title::before {
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

.title-icon {
  font-size: 20px;
}

.recommendations-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: 600px;
  overflow-y: auto;
  overflow-x: visible;
  padding-left: 10px;
  padding-right: 10px;
}

.recommendation-item {
  padding: 16px;
  background: rgba(247, 250, 252, 0.6);
  border-radius: 12px;
  border-left: 4px solid #667eea;
  position: relative;
  overflow: visible !important;
  transform: scale(1);
  transform-origin: center center;
  will-change: transform, box-shadow;
  transition:
    transform 0.28s cubic-bezier(0.22, 1, 0.36, 1),
    box-shadow 0.28s cubic-bezier(0.22, 1, 0.36, 1),
    border-color 0.28s ease,
    background 0.28s ease;
}

.recommendation-item::before {
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

.recommendation-item > * {
  position: relative;
  z-index: 2;
}

.recommendation-item:hover {
  background: rgba(247, 250, 252, 0.9);
  transform: scale(1.016);
  box-shadow:
    0 16px 36px rgba(102, 126, 234, 0.12),
    0 8px 18px rgba(15, 23, 42, 0.06);
  border-color: rgba(203, 213, 225, 0.72);
}

.recommendation-item:hover::before {
  opacity: 0.94;
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
  color: #4c51bf;
  font-weight: 800;
  padding: 4px 12px;
  background: rgba(102, 126, 234, 0.12);
  border-radius: 999px;
  border: 1px solid rgba(102, 126, 234, 0.25);
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

/* 建议正文展示区：固定最大高度 + 块内滚动（不改卡片布局，只增强内容可见性） */
.rec-content-block {
  background: rgba(102, 126, 234, 0.05);
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 12px;
  padding: 13px 14px;
  margin: 0;

  font-size: 14px;
  line-height: 1.7;
  color: #475569;

  min-height: 72px;
  max-height: 140px;
  overflow-y: auto;
  overflow-x: hidden;

  white-space: pre-wrap;
  word-break: break-word;
}

/* 细一点的滚动条（与紫蓝主题协调） */
.rec-content-block {
  scrollbar-width: thin;
  scrollbar-color: rgba(118, 75, 162, 0.55) rgba(226, 232, 240, 0.5);
}
.rec-content-block::-webkit-scrollbar {
  width: 6px;
}
.rec-content-block::-webkit-scrollbar-track {
  background: rgba(226, 232, 240, 0.5);
  border-radius: 999px;
}
.rec-content-block::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, rgba(102, 126, 234, 0.65), rgba(118, 75, 162, 0.65));
  border-radius: 999px;
}

.rec-content-block::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(180deg, rgba(102, 126, 234, 0.85), rgba(118, 75, 162, 0.85));
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

.empty-message {
  text-align: center;
  padding: 40px 20px;
  color: #718096;
  font-size: 14px;
}
</style>

