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
  gap: 3px;
  padding: 2px;
  border-radius: 999px;
  border-color: rgba(114, 180, 205, 0.12);
  background: rgba(255, 255, 255, 0.28);
}

.tab-btn {
  padding: 3px 8px;
  font-size: 9px;
  font-weight: 400;
  letter-spacing: 0.02em;
  line-height: 1.25;
}

.tab-btn.active {
  background: linear-gradient(180deg, rgba(95, 199, 216, 0.07), rgba(255, 255, 255, 0.46));
  border-color: rgba(95, 199, 216, 0.2);
  color: var(--t-2);
}
</style>

