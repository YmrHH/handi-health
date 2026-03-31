<template>
  <div class="card report-table-card">
    <div class="card-title">明细数据表</div>
    <div class="table-scroll">
      <table class="table">
        <thead>
          <tr>
            <th v-for="c in columns" :key="c.key">{{ c.label }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="pagedRows.length === 0">
            <td :colspan="Math.max(columns.length, 1)" class="empty-cell">暂无数据</td>
          </tr>
          <tr v-for="r in pagedRows" :key="r.key">
            <td>{{ r.label }}</td>
            <td v-for="c in metricColumns" :key="c.key">{{ formatCell(r.metrics[c.key], c.format) }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="totalPages > 1" class="pagination">
      <div class="page-info">
        共 {{ rows.length }} 条记录，第 {{ pageNo }} / {{ totalPages }} 页（每页 {{ pageSize }} 条）
      </div>
      <div class="page-controls">
        <button class="btn btn-secondary" type="button" :disabled="pageNo <= 1" @click="goToPage(pageNo - 1)">上一页</button>
        <span class="page-numbers">
          <button
            v-for="p in pageNumbers"
            :key="String(p)"
            class="page-btn"
            :class="{ active: p === pageNo, disabled: p === '...' }"
            :disabled="p === '...' || p === pageNo"
            @click="p !== '...' && goToPage(p as number)"
          >
            {{ p }}
          </button>
        </span>
        <button class="btn btn-secondary" type="button" :disabled="pageNo >= totalPages" @click="goToPage(pageNo + 1)">下一页</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { ReportTransformRow } from '@/utils/reportTransform'

const props = defineProps<{
  columns: { key: string; label: string; format?: 'int' | 'percent' }[]
  rows: ReportTransformRow[]
  pageSize?: number
}>()

const metricColumns = computed(() => props.columns.filter((c) => c.key !== 'label'))

const pageNo = ref(1)
const pageSize = computed(() => {
  const n = Number(props.pageSize)
  if (!Number.isFinite(n) || n <= 0) return 20
  return Math.max(1, Math.min(Math.floor(n), 200))
})

const totalPages = computed(() => Math.max(1, Math.ceil((props.rows?.length || 0) / pageSize.value)))

const pagedRows = computed(() => {
  const rows = props.rows || []
  const start = (pageNo.value - 1) * pageSize.value
  return rows.slice(start, start + pageSize.value)
})

watch(
  () => [props.rows, props.pageSize],
  () => {
    pageNo.value = 1
  },
  { deep: false }
)

function goToPage(p: number) {
  const next = Math.max(1, Math.min(p, totalPages.value))
  pageNo.value = next
}

const pageNumbers = computed(() => {
  const current = pageNo.value
  const total = totalPages.value
  const pages: Array<number | '...'> = []

  if (total <= 7) {
    for (let i = 1; i <= total; i++) pages.push(i)
    return pages
  }

  if (current <= 4) {
    pages.push(1, 2, 3, 4, 5, '...', total)
    return pages
  }

  if (current >= total - 3) {
    pages.push(1, '...', total - 4, total - 3, total - 2, total - 1, total)
    return pages
  }

  pages.push(1, '...', current - 1, current, current + 1, '...', total)
  return pages
})

function formatCell(v: any, format?: 'int' | 'percent') {
  const n = Number(v)
  if (format === 'percent') {
    if (!Number.isFinite(n)) return '0%'
    return `${(n * 100).toFixed(1)}%`
  }
  if (format === 'int') {
    if (!Number.isFinite(n)) return '0'
    return String(Math.round(n))
  }
  if (!Number.isFinite(n)) return String(v ?? '')
  return String(n)
}
</script>

<style scoped>
.report-table-card {
  margin-bottom: 16px;
}
.empty-cell {
  text-align: center;
  color: #64748b;
}

.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  gap: 12px;
  flex-wrap: wrap;
}

.page-info {
  font-size: 13px;
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
  min-width: 34px;
  height: 34px;
  padding: 0 10px;
  border: 1px solid rgba(226, 232, 240, 0.8);
  background: #ffffff;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-color: #667eea;
  color: #ffffff;
}

.page-btn.disabled,
.page-btn:disabled {
  cursor: not-allowed;
  opacity: 0.5;
  background: #f7fafc;
}
</style>

