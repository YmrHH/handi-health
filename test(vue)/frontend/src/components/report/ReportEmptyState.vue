<template>
  <div class="report-empty card">
    <div class="card-title">{{ title }}</div>
    <div class="report-empty-body">
      <div class="report-empty-text">{{ message }}</div>
      <div v-if="tips && tips.length" class="report-empty-tips">
        <div class="tip" v-for="(t, idx) in tips" :key="idx">- {{ t }}</div>
      </div>
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  message: string
  tips?: string[]
}>()
</script>

<style scoped>
.report-empty {
  margin-bottom: 16px;
}
.report-empty-body {
  color: #475569;
  font-size: 14px;
  line-height: 1.7;
}
.report-empty-text {
  margin-bottom: 10px;
}
.report-empty-tips .tip {
  color: #64748b;
  font-size: 13px;
  margin: 4px 0;
}
</style>

