<template>
  <div v-if="totalPages > 1" class="pagination">
    <div class="page-info">
      共 {{ total }} 条记录，第 {{ page }} / {{ totalPages }} 页
    </div>
    <div class="page-controls">
      <button
        class="btn btn-secondary"
        :disabled="page <= 1"
        @click="changePage(page - 1)"
      >
        上一页
      </button>
      <span class="page-numbers">
        <button
          v-for="p in pageList"
          :key="p"
          class="page-btn"
          :class="{ active: p === page, disabled: p === '...' }"
          :disabled="p === '...' || p === page"
          @click="p !== '...' && changePage(p as number)"
        >
          {{ p }}
        </button>
      </span>
      <button
        class="btn btn-secondary"
        :disabled="page >= totalPages"
        @click="changePage(page + 1)"
      >
        下一页
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  total: number
  page: number
  totalPages: number
}>()

const emit = defineEmits<{
  (e: 'change', page: number): void
}>()

const pageList = computed<(number | string)[]>(() => {
  const current = props.page
  const total = props.totalPages
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
})

function changePage(page: number) {
  if (page < 1 || page > props.totalPages) return
  emit('change', page)
}
</script>

<style scoped>
.pagination {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e2e8f0;
}

.page-info {
  color: #64748b;
  font-size: 14px;
}

.page-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-numbers {
  display: flex;
  align-items: center;
  gap: 4px;
}

.page-btn {
  min-width: 32px;
  height: 32px;
  padding: 0 8px;
  border: 1px solid #d0d5e5;
  background: #ffffff;
  color: #4f5673;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.page-btn:hover:not(.disabled):not(.active) {
  background: #f5f7fd;
  border-color: #c0c5d5;
}

.page-btn.active {
  background: #e0e7ff;
  border-color: #3c7cff;
  color: #3c7cff;
  font-weight: 500;
}

.page-btn.disabled {
  cursor: default;
  color: #94a3b8;
}

.btn-secondary {
  padding: 6px 16px;
  border: 1px solid #d0d5e5;
  background: #ffffff;
  color: #4f5673;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-secondary:hover:not(:disabled) {
  background: #f5f7fd;
  border-color: #c0c5d5;
}

.btn-secondary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>


