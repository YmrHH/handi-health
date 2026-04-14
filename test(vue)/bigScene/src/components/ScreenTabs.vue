<template>
  <div class="tabs">
    <button
      v-for="t in tabs"
      :key="t.key"
      type="button"
      class="tab-btn"
      :class="{ active: modelValue === t.key }"
      @click="$emit('update:modelValue', t.key)"
    >
      {{ t.label }}
    </button>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  modelValue: string
  tabs: Array<{ key: string; label: string }>
}>()
defineEmits<{ (e: 'update:modelValue', v: string): void }>()
</script>

<style scoped>
.tabs {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 4px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.38), rgba(247, 252, 255, 0.24));
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

.tab-btn {
  padding: 4px 11px;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.02em;
  line-height: 1.1;
  white-space: nowrap;
  border-radius: 999px;
  color: rgba(92, 130, 156, 0.86);
}

.tab-btn.active {
  background: linear-gradient(135deg, rgba(95, 199, 216, 0.24), rgba(255, 255, 255, 0.72));
  border-color: rgba(255, 255, 255, 0.2);
  color: rgba(27, 91, 120, 0.98);
  box-shadow: 0 8px 16px rgba(79, 209, 197, 0.12), inset 0 1px 0 rgba(255, 255, 255, 0.36);
}
</style>

