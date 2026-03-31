<template>
  <div class="metric-cards">
    <div class="metric-card" v-for="(m, idx) in metrics" :key="idx">
      <div class="metric-label">{{ m.label }}</div>
      <div class="metric-value">{{ m.valueText }}</div>
      <div v-if="m.subText" class="metric-sub">{{ m.subText }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  metrics: Array<{ label: string; valueText: string; subText?: string }>
}>()
</script>

<style scoped>
.metric-cards {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
  overflow: visible;
  padding-left: 10px;
  padding-right: 10px;
}

.metric-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 14px;
  padding: 14px 16px;
  border: 1px solid rgba(226, 232, 240, 0.7);
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.05);
  position: relative;
  overflow: hidden;
  transform: scale(1);
  transform-origin: center center;
  will-change: transform, box-shadow;
  transition:
    transform 0.28s cubic-bezier(0.22, 1, 0.36, 1),
    box-shadow 0.28s cubic-bezier(0.22, 1, 0.36, 1),
    border-color 0.28s ease;
}

.metric-card::before {
  content: "";
  position: absolute;
  left: 0;
  top: 0;
  width: 100%;
  height: 2px;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  opacity: 0.38;
  transition: opacity 0.28s ease;
  z-index: 1;
  pointer-events: none;
}

.metric-card > * {
  position: relative;
  z-index: 2;
}

/* 四列密集：hover 更克制 */
.metric-card:hover {
  transform: scale(1.016);
  border-color: rgba(203, 213, 225, 0.78);
  box-shadow:
    0 16px 36px rgba(102, 126, 234, 0.12),
    0 8px 18px rgba(15, 23, 42, 0.06),
    0 0 0 1px rgba(255, 255, 255, 0.10);
}

.metric-card:hover::before {
  opacity: 0.94;
}

.metric-label {
  font-size: 12px;
  color: #64748b;
  font-weight: 600;
  margin-bottom: 6px;
}

.metric-value {
  font-size: 20px;
  font-weight: 800;
  color: #1e293b;
  line-height: 1.2;
}

.metric-sub {
  margin-top: 6px;
  font-size: 12px;
  color: #64748b;
}

@media (max-width: 1024px) {
  .metric-cards {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 480px) {
  .metric-cards {
    grid-template-columns: 1fr;
  }
}
</style>

