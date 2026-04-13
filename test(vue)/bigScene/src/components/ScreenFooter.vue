<template>
  <footer class="screen-footer">
    <div class="footer-left">
      <span class="footer-brand">© {{ year }} {{ brand }}</span>
      <span class="footer-divider"></span>
      <span class="footer-page">{{ pageLabel }}</span>
      <span class="footer-divider"></span>
      <span class="footer-status">
        <span class="status-dot" :class="statusToneClass"></span>
        <span>{{ statusText }}</span>
      </span>
    </div>

    <div v-if="tickerItems.length" class="footer-center">
      <div class="ticker-window">
        <div class="ticker-track" :style="tickerStyle">
          <div v-for="(item, idx) in tickerItems" :key="`ticker-a-${idx}`" class="ticker-item">
            {{ item }}
          </div>
          <div v-for="(item, idx) in tickerItems" :key="`ticker-b-${idx}`" class="ticker-item">
            {{ item }}
          </div>
        </div>
      </div>
    </div>

    <div class="footer-right">
      <span class="footer-meta">网络延迟：{{ latencyText }}</span>
      <span class="footer-meta">安全等级：{{ securityText }}</span>
      <span class="footer-version">{{ versionText }}</span>
      <span class="footer-divider"></span>
      <span class="footer-updated">更新时间 {{ updatedAt }}</span>
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
    versionText?: string
    updatedAt: string
    tickerItems?: string[]
  }>(),
  {
    statusText: '运行状态：正常',
    statusTone: 'success',
    latencyMs: null,
    securityText: '高',
    versionText: '大屏端',
    tickerItems: () => []
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

const tickerStyle = computed(() => ({
  animationDuration: `${Math.max(24, props.tickerItems.length * 6)}s`
}))
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
  gap: 10px;
  padding: 8px var(--canvas-pad-x) calc(8px + env(safe-area-inset-bottom, 0px));
  background: rgba(255, 255, 255, 0.28);
  backdrop-filter: blur(var(--blur-soft));
  -webkit-backdrop-filter: blur(var(--blur-soft));
  border-top: 1px solid rgba(255, 255, 255, 0.18);
  box-shadow: 0 -6px 18px rgba(79, 209, 197, 0.06);
  color: rgba(39, 85, 113, 0.92);
  font-size: 11px;
  letter-spacing: 0.02em;
}

.footer-left,
.footer-right {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  flex-shrink: 0;
}

.footer-center {
  flex: 1;
  min-width: 0;
  display: flex;
  justify-content: center;
}

.footer-brand {
  white-space: nowrap;
  font-weight: 600;
  color: rgba(32, 82, 110, 0.94);
}

.footer-page {
  white-space: nowrap;
  color: rgba(92, 130, 156, 0.90);
}

.footer-version {
  white-space: nowrap;
  color: rgba(46, 113, 140, 0.92);
  font-weight: 700;
}

.footer-updated {
  white-space: nowrap;
  color: rgba(92, 130, 156, 0.82);
}

.footer-meta {
  white-space: nowrap;
  color: rgba(92, 130, 156, 0.78);
}

.footer-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
  color: rgba(46, 113, 140, 0.92);
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  box-shadow: 0 0 14px rgba(95, 199, 216, 0.22);
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

.footer-divider {
  width: 1px;
  height: 12px;
  background: rgba(114, 180, 205, 0.18);
}

.ticker-window {
  width: 100%;
  max-width: 700px;
  overflow: hidden;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(255, 255, 255, 0.16);
  padding: 4px 10px;
}

.ticker-track {
  display: inline-flex;
  align-items: center;
  gap: 24px;
  white-space: nowrap;
  animation-name: tickerMove;
  animation-timing-function: linear;
  animation-iteration-count: infinite;
}

.ticker-item {
  color: rgba(39, 85, 113, 0.72);
}

@keyframes tickerMove {
  0% {
    transform: translateX(0);
  }
  100% {
    transform: translateX(-50%);
  }
}
</style>

