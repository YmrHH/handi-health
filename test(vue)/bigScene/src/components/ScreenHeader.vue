<template>
  <header class="screen-header">
    <div class="header-bar">
      <div class="header-left">
        <h1 class="brand">{{ title }}</h1>
        <div class="nav-slot">
          <slot />
        </div>
      </div>
      <div class="header-right">
        <span class="sub">{{ subTitle }}</span>
        <span class="time">{{ timeText }}</span>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'

defineProps<{
  title: string
  subTitle: string
}>()

const timeText = ref('')
let timer: number | null = null

function updateNow() {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  timeText.value = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(
    d.getMinutes()
  )}:${pad(d.getSeconds())}`
}

onMounted(() => {
  updateNow()
  timer = window.setInterval(updateNow, 1000)
})

onUnmounted(() => {
  if (timer != null) window.clearInterval(timer)
  timer = null
})
</script>

<style scoped>
.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.brand {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.01em;
  background: linear-gradient(90deg, rgba(32, 82, 110, 0.98), rgba(46, 113, 140, 0.96), rgba(95, 199, 216, 0.92));
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  white-space: nowrap;
}

.nav-slot {
  min-width: 0;
}

.header-right {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: rgba(92, 130, 156, 0.72);
  font-size: 11px;
  white-space: nowrap;
}

.sub {
  color: rgba(92, 130, 156, 0.62);
}

.time {
  color: rgba(92, 130, 156, 0.66);
  font-variant-numeric: tabular-nums;
}
</style>
