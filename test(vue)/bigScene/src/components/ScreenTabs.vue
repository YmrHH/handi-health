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
  gap: 4px;
  padding: 3px;
  border-radius: 999px;
  border-color: rgba(114, 180, 205, 0.16);
  background: rgba(255, 255, 255, 0.34);
}

.tab-btn {
  padding: 4px 9px;
  font-size: 10px;
  font-weight: 500;
  letter-spacing: 0.03em;
}

.tab-btn.active {
  background: linear-gradient(180deg, rgba(95, 199, 216, 0.1), rgba(255, 255, 255, 0.52));
  border-color: rgba(95, 199, 216, 0.26);
  color: var(--t-2);
}
</style>

