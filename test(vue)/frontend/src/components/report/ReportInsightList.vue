<template>
  <div class="card report-insights">
    <div class="card-title">关键洞察</div>
    <div v-if="insights.length === 0" class="empty">暂无可生成洞察（可能因为数据不足或维度不支持）。</div>
    <ul v-else class="insight-list">
      <li v-for="(t, idx) in insights" :key="idx" class="insight-item">{{ t }}</li>
    </ul>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  insights: string[]
}>()
</script>

<style scoped>
.report-insights {
  margin-bottom: 16px;
}
.empty {
  color: #64748b;
  font-size: 13px;
}
.insight-list {
  padding-left: 18px;
  color: #334155;
  line-height: 1.8;
  font-size: 14px;
}
.insight-item {
  margin: 6px 0;
}
</style>

