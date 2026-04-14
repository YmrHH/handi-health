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
  border: 1px solid rgba(255, 255, 255, 0.025);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.14), rgba(247, 252, 255, 0.08));
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.tab-btn {
  padding: 3px 10px;
  font-size: 10px;
  font-weight: 520;
  letter-spacing: 0.02em;
  line-height: 1.1;
  white-space: nowrap;
  border-radius: 999px;
  color: rgba(92, 130, 156, 0.8);
  transition: color 0.18s ease, background-color 0.18s ease, box-shadow 0.18s ease;
}

.tab-btn:hover {
  color: rgba(46, 113, 140, 0.9);
  background: rgba(255, 255, 255, 0.2);
}

.tab-btn.active {
  background: linear-gradient(135deg, rgba(95, 199, 216, 0.1), rgba(255, 255, 255, 0.38));
  border-color: rgba(255, 255, 255, 0.03);
  color: rgba(27, 91, 120, 0.94);
  box-shadow: 0 2px 6px rgba(79, 209, 197, 0.03), inset 0 1px 0 rgba(255, 255, 255, 0.14);
}
</style>

