<template>
  <div class="screen-header">
    <div class="header-bar">
      <div class="header-title">
        <div class="header-main">
          <h1>{{ title }}</h1>
          <span class="header-pill">{{ timeText }}</span>
        </div>
        <div class="header-sub">{{ subTitle }}</div>
      </div>
      <div class="header-right">
        <slot />
      </div>
    </div>
    <div class="hud-line"></div>
  </div>
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
.screen-header .header-bar {
  min-height: 0;
}

.screen-header .header-main h1 {
  font-size: 16px;
  font-weight: 650;
  letter-spacing: 0.02em;
  background: linear-gradient(90deg, rgba(32, 82, 110, 0.98), rgba(46, 113, 140, 0.96), rgba(95, 199, 216, 0.92));
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  text-shadow: none;
}

.screen-header .header-sub {
  font-size: 9px;
  opacity: 0.66;
  letter-spacing: 0.04em;
}

.screen-header .header-pill {
  padding: 3px 7px;
  font-size: 10px;
  font-weight: 520;
  border-color: rgba(114, 180, 205, 0.14);
  background: rgba(255, 255, 255, 0.34);
  color: rgba(92, 130, 156, 0.92);
}
</style>
