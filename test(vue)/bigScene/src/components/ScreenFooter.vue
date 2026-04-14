<template>
  <footer class="screen-footer">
    <div class="footer-left">
      <span class="footer-brand">© {{ year }} {{ brand }}</span>
      <span class="footer-page">{{ pageLabel }}</span>
      <span class="footer-status">
        <span class="status-dot" :class="statusToneClass"></span>
        <span>{{ statusText }}</span>
      </span>
    </div>

    <div class="footer-right">
      <span class="footer-meta">网络延迟：{{ latencyText }}</span>
      <span class="footer-meta">安全等级：{{ securityText }}</span>
    </div>
  </footer>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    brand: string
    pageLabel: string
    statusText?: string
    statusTone?: 'success' | 'warning' | 'danger'
    latencyMs?: number | null
    securityText?: string
  }>(),
  {
    statusText: '运行状态：正常',
    statusTone: 'success',
    latencyMs: null,
    securityText: '高'
  }
)

const year = new Date().getFullYear()

const latencyText = computed(() => {
  if (props.latencyMs == null || Number.isNaN(Number(props.latencyMs))) return '—'
  const v = Math.max(0, Math.round(Number(props.latencyMs)))
  return `${v}ms`
})

const statusToneClass = computed(() => {
  if (props.statusTone === 'danger') return 'is-danger'
  if (props.statusTone === 'warning') return 'is-warning'
  return 'is-success'
})

</script>

<style scoped>
.screen-footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1200;
  height: var(--footer-h);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px var(--canvas-pad-x) calc(8px + env(safe-area-inset-bottom, 0px));
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(var(--blur-soft));
  -webkit-backdrop-filter: blur(var(--blur-soft));
  border-top: none;
  box-shadow: 0 -3px 9px rgba(79, 209, 197, 0.014);
  color: rgba(39, 85, 113, 0.92);
  font-size: 10px;
  letter-spacing: 0.02em;
}

.footer-left,
.footer-right {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  flex-shrink: 0;
}

.footer-brand {
  white-space: nowrap;
  font-weight: 600;
  color: rgba(32, 82, 110, 0.94);
}

.footer-page {
  white-space: nowrap;
  color: rgba(92, 130, 156, 0.82);
}

.footer-meta {
  white-space: nowrap;
  color: rgba(92, 130, 156, 0.72);
}

.footer-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
  color: rgba(46, 113, 140, 0.84);
}

.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  box-shadow: 0 0 10px rgba(95, 199, 216, 0.16);
}
.status-dot.is-success {
  background: var(--c-success);
}
.status-dot.is-warning {
  background: var(--c-warning);
}
.status-dot.is-danger {
  background: var(--c-danger);
}

</style>

