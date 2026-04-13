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
  gap: 3px;
  padding: 2px 3px;
  border-radius: 999px;
  border: 1px solid rgba(114, 180, 205, 0.12);
  background: rgba(255, 255, 255, 0.28);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
}

.tab-btn {
  padding: 4px 9px;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.02em;
  line-height: 1.1;
  white-space: nowrap;
  border-radius: 999px;
}

.tab-btn.active {
  background: linear-gradient(180deg, rgba(95, 199, 216, 0.12), rgba(255, 255, 255, 0.52));
  border-color: rgba(95, 199, 216, 0.2);
  color: rgba(33, 75, 103, 0.96);
}
</style>

