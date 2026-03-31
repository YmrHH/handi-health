<template>
  <div class="stat">
    <div class="label">{{ label }}</div>
    <div class="value" :class="tone">{{ value }}<span v-if="unit" class="unit">{{ unit }}</span></div>
    <div v-if="hint" class="hint">{{ hint }}</div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  label: string
  value: string | number
  unit?: string
  hint?: string
  tone?: 'cyan' | 'gold' | 'danger' | 'warning' | 'success'
}>()
</script>

<style scoped>
.unit {
  margin-left: 4px;
  font-size: 12px;
  color: var(--t-3);
  font-weight: 700;
}

.hint {
  margin-top: 4px;
  font-size: 11px;
  color: var(--t-3);
}
</style>

