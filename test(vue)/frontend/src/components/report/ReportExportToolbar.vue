<template>
  <div class="card report-export-toolbar">
    <div class="toolbar-left">
      <div class="toolbar-title">导出工具</div>
      <div class="toolbar-sub">导出内容与当前筛选/图表/表格完全一致。</div>
    </div>
    <div class="toolbar-right">
      <button class="btn btn-primary" type="button" :disabled="!canExport" @click="$emit('exportCsv')">导出 CSV</button>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  canExport: boolean
}>()

defineEmits<{
  (e: 'exportCsv'): void
}>()
</script>

<style scoped>
.report-export-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.toolbar-title {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 4px;
}
.toolbar-sub {
  font-size: 12px;
  color: #64748b;
}
</style>

