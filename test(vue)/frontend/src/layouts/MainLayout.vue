<template>
  <div class="layout">
    <Sidebar :collapsed="sidebarCollapsed" @toggle-collapsed="sidebarCollapsed = !sidebarCollapsed" />
    <div class="main">
      <Topbar :page-title="pageTitle" :page-icon-url="pageIconUrl" />
      <div class="content">
        <RouterView />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import Sidebar from '@/components/Layout/Sidebar.vue'
import Topbar from '@/components/Layout/Topbar.vue'
import { FOLLOWUP_ICONS } from '@/constants/followup-icons'

const route = useRoute()

const pageTitle = computed(() => {
  return (route.meta.title as string) || '数据管理 / 数据总览'
})

const sidebarCollapsed = ref(false)

watch(
  () => route.path,
  () => {
    // 让折叠态跨页面保持（不写入全局设置文件，仅本地存储）
    try {
      const v = localStorage.getItem('ui_sidebar_collapsed')
      if (v === '1') sidebarCollapsed.value = true
      else if (v === '0') sidebarCollapsed.value = false
    } catch {
      // ignore
    }
  },
  { immediate: true }
)

watch(
  () => sidebarCollapsed.value,
  (v) => {
    try {
      localStorage.setItem('ui_sidebar_collapsed', v ? '1' : '0')
    } catch {
      // ignore
    }
  }
)

const pageIconUrl = computed(() => {
  const path = route.path
  if (path.includes('/followup/task-assign')) return FOLLOWUP_ICONS.followupTaskAssignTitle
  if (path.startsWith('/home')) return FOLLOWUP_ICONS.dataManagement
  if (path.startsWith('/patient')) return FOLLOWUP_ICONS.patientManagement
  if (path.startsWith('/alert')) return FOLLOWUP_ICONS.warningManagement
  if (path.startsWith('/followup')) return FOLLOWUP_ICONS.followupManagement
  if (path.startsWith('/system')) return FOLLOWUP_ICONS.systemManagement
  return FOLLOWUP_ICONS.dataManagement
})
</script>

<style scoped>
.layout {
  display: flex;
  width: 100%;
  height: 100vh;
  position: relative;
}

.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.content {
  flex: 1;
  overflow-y: auto;
  /* 允许页面在放大后横向滚动查看完整内容 */
  overflow-x: auto;
  min-width: 0;
  position: relative;
  z-index: 1;
  padding: 16px;
  background-color: #f5f7fd;
}
</style>

