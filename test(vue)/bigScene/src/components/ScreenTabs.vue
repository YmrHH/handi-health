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
  background: rgba(255, 255, 255, 0.32);
  backdrop-filter: blur(var(--blur-soft));
  -webkit-backdrop-filter: blur(var(--blur-soft));
}

.tab-btn {
  padding: 5px 12px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.02em;
  line-height: 1.1;
}

.tab-btn.active {
  background: linear-gradient(180deg, rgba(95, 199, 216, 0.16), rgba(255, 255, 255, 0.58));
  border-color: rgba(95, 199, 216, 0.26);
  color: rgba(33, 75, 103, 0.96);
}
</style>

