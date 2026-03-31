<template>
  <div>
    <div class="header-bar">
      <div class="header-title">
        <div class="header-sub">{{ subTitle }}</div>
        <div class="header-main">
          <h1>{{ title }}</h1>
          <span class="header-pill">{{ orgText }}</span>
          <span class="header-pill">{{ timeText }}</span>
          <span class="header-pill">数据更新：{{ updatedText }}</span>
        </div>
      </div>
      <div class="header-right">
        <slot />
      </div>
    </div>
    <div class="hud-line"></div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'

const props = defineProps<{
  title: string
  subTitle: string
  orgName?: string
  updatedAt?: string
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

const orgText = computed(() => props.orgName || '机构：默认组织')
const updatedText = computed(() => props.updatedAt || '—')

onMounted(() => {
  updateNow()
  timer = window.setInterval(updateNow, 1000)
})

onUnmounted(() => {
  if (timer != null) window.clearInterval(timer)
  timer = null
})
</script>

