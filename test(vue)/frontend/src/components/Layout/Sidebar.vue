<template>
  <aside ref="sidebarRef" class="sidebar" :class="{ collapsed: isCollapsed }">
    <div class="sidebar-header">
      <div class="brand" :title="isCollapsed ? '寒岐智护·慢性病随访健康预警管理平台' : undefined">
        <div class="brand-icon" aria-hidden="true">
          <img class="brand-img" :src="safeUrl(ICONS.platformLogo)" alt="" />
        </div>
        <span v-if="!isCollapsed" class="brand-text">寒岐智护·慢性病随访健康预警管理平台</span>
      </div>
    </div>

    <ul class="sidebar-menu">
      <li
        v-for="m in menus"
        :key="m.key"
        class="menu-item"
        :class="{ active: currentMenu === m.key }"
        @mouseenter="onMenuEnter(m.key)"
        @mouseleave="onMenuLeave(m.key)"
      >
        <a href="javascript:void(0);" class="menu-link" @click="onMenuClick(m.key)">
          <span class="menu-icon" aria-hidden="true">
            <img class="menu-img" :src="safeUrl(m.iconUrl)" alt="" />
          </span>
          <span v-if="!isCollapsed" class="menu-text">{{ m.label }}</span>
        </a>

        <!-- 展开态：内嵌二级菜单 -->
        <ul v-if="!isCollapsed && m.children?.length" class="submenu" v-show="openMenuKey === m.key">
          <li v-for="c in m.children" :key="c.subKey" class="submenu-item" :class="{ active: activeSubMenuKey === c.subKey }">
            <RouterLink :to="c.to" @click="activeSubMenuKey = c.subKey">{{ c.label }}</RouterLink>
          </li>
        </ul>

        <!-- 折叠态：右侧浮层子菜单 -->
        <div
          v-if="isCollapsed && m.children?.length && popoverMenu === m.key"
          class="submenu-popover"
          @mouseenter="popoverHold = true"
          @mouseleave="popoverHold = false; hidePopoverSoon()"
        >
          <div class="popover-title">{{ m.label }}</div>
          <RouterLink
            v-for="c in m.children"
            :key="c.subKey"
            :to="c.to"
            class="popover-item"
            :class="{ active: activeSubMenuKey === c.subKey }"
            @click="activeSubMenuKey = c.subKey; popoverMenu = null"
          >
            {{ c.label }}
          </RouterLink>
        </div>
      </li>
    </ul>
  </aside>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { RouterLink } from 'vue-router'
import { FOLLOWUP_ICONS } from '@/constants/followup-icons'

// 在模板中使用常量
const ICONS = FOLLOWUP_ICONS

const props = defineProps<{
  collapsed: boolean
}>()

const emit = defineEmits<{
  (e: 'toggle-collapsed'): void
}>()

const route = useRoute()
const sidebarRef = ref<HTMLElement | null>(null)

const isCollapsed = computed(() => props.collapsed)

function safeUrl(url: string) {
  // 兜底：避免错误地拼出 /public 前缀导致 404
  return String(url || '').replace('/public/', '/')
}

type MenuKey = 'home' | 'patient' | 'alert' | 'followup' | 'system'

// 一级菜单展开：保持原有“点击展开/再点收起”的逻辑，但用单 key 控制
const openMenuKey = ref<MenuKey | null>(null)
// 二级菜单选中态：独立状态，路由变化会同步更新
const activeSubMenuKey = ref<string>('')

const popoverMenu = ref<MenuKey | null>(null)
const popoverHold = ref(false)
let popoverTimer: any = null

function clearPopoverTimer() {
  if (popoverTimer) {
    clearTimeout(popoverTimer)
    popoverTimer = null
  }
}

function hidePopoverSoon() {
  clearPopoverTimer()
  popoverTimer = setTimeout(() => {
    if (!popoverHold.value) popoverMenu.value = null
  }, 120)
}

const currentMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/home') || path === '/') return 'home'
  if (path.startsWith('/patient')) return 'patient'
  if (path.startsWith('/alert')) return 'alert'
  if (path.startsWith('/followup')) return 'followup'
  if (path.startsWith('/system')) return 'system'
  return ''
})

const currentSubmenu = computed(() => {
  const path = route.path
  if (path === '/home') return 'overview'
  if (path.includes('/home/board')) return 'home-board'
  if (path.includes('/home/export')) return 'home-export'
  if (path === '/patient') return 'patient-list'
  if (path.includes('/patient/recommend')) return 'patient-recommend'
  if (path.includes('/patient/doctor-advice')) return 'patient-doctor-advice'
  if (path.includes('/patient/visit-plan')) return 'patient-visit-plan'
  if (path.includes('/patient/detail')) return 'patient-list'
  if (path.includes('/alert/patient')) return 'patient-alert'
  if (path.includes('/alert/hardware')) return 'hardware-alert'
  if (path.includes('/followup/workbench')) return 'followup-workbench'
  if (path.includes('/followup/staff')) return 'followup-staff'
  if (path.includes('/followup/task-assign')) return 'task-assign'
  if (path.includes('/system')) return 'system-log'
  return ''
})

function toggleMenu(menu: MenuKey) {
  openMenuKey.value = openMenuKey.value === menu ? null : menu
}

watch(
  () => route.path,
  () => {
    // 路由切换时不自动展开任何二级菜单（按需求保持收起）
    popoverMenu.value = null
    activeSubMenuKey.value = currentSubmenu.value
  },
  { immediate: true }
)

function hasChildren(menu: MenuKey) {
  return menus.value.find((m) => m.key === menu)?.children?.length
}

function onMenuEnter(menu: MenuKey) {
  clearPopoverTimer()
  if (isCollapsed.value && hasChildren(menu)) popoverMenu.value = menu
}

function onMenuLeave(menu: MenuKey) {
  if (isCollapsed.value && popoverMenu.value === menu) hidePopoverSoon()
}

function onMenuClick(menu: MenuKey) {
  // 折叠态：点击一级图标 => 自动展开侧边栏 + 自动展开该一级菜单的二级
  if (isCollapsed.value) {
    if (hasChildren(menu)) {
      popoverMenu.value = null
      openMenuKey.value = menu
      emit('toggle-collapsed')
      return
    }
    // 无二级菜单：保持现有行为（当前无此类菜单）
    return
  }

  // 展开态：保持原有“点击展开/再点收起”
  if (hasChildren(menu)) toggleMenu(menu)
}

function onDocumentClick(e: MouseEvent) {
  // 点击侧边栏以外区域：若当前是展开状态，则自动收回
  if (isCollapsed.value) return
  const el = sidebarRef.value
  const target = e.target as Node | null
  if (!el || !target) return
  if (!el.contains(target)) {
    popoverMenu.value = null
    emit('toggle-collapsed')
  }
}

onMounted(() => {
  document.addEventListener('click', onDocumentClick)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', onDocumentClick)
})

const menus = computed(() => {
  return [
    {
      key: 'home' as const,
      label: '数据管理',
      iconUrl: ICONS.dataManagement,
      children: [
        { subKey: 'overview', label: '数据总览', to: '/home' },
        { subKey: 'home-board', label: 'AI数据分析', to: '/home/board' },
        { subKey: 'home-export', label: '报表与导出', to: '/home/export' }
      ]
    },
    {
      key: 'patient' as const,
      label: '患者管理',
      iconUrl: ICONS.patientManagement,
      children: [
        { subKey: 'patient-list', label: '患者档案管理', to: '/patient' },
        { subKey: 'patient-recommend', label: '健康建议下发', to: '/patient/recommend' },
        { subKey: 'patient-doctor-advice', label: '医学建议数据留存', to: '/patient/doctor-advice' },
        { subKey: 'patient-visit-plan', label: '复诊计划管理', to: '/patient/visit-plan' }
      ]
    },
    {
      key: 'alert' as const,
      label: '预警管理',
      iconUrl: ICONS.warningManagement,
      children: [
        { subKey: 'patient-alert', label: '健康数据异常预警', to: '/alert/patient' },
        { subKey: 'hardware-alert', label: '健康设备异常预警', to: '/alert/hardware' }
      ]
    },
    {
      key: 'followup' as const,
      label: '随访管理',
      iconUrl: ICONS.followupManagement,
      children: [
        { subKey: 'followup-workbench', label: '进行主动随访', to: '/followup/workbench' },
        { subKey: 'followup-staff', label: '随访人员管理', to: '/followup/staff' },
        { subKey: 'task-assign', label: '随访任务指派', to: '/followup/task-assign' }
      ]
    },
    {
      key: 'system' as const,
      label: '系统管理',
      iconUrl: ICONS.systemManagement,
      children: [{ subKey: 'system-log', label: '日志审计', to: '/system/data-log' }]
    }
  ]
})
</script>

<style scoped>
.sidebar {
  width: 296px;
  height: 100vh;
  background-color: #1e293b;
  color: #fff;
  overflow-y: auto;
  flex-shrink: 0;
  transition: width 0.18s ease;
  position: relative;
  z-index: 30;
}

.sidebar.collapsed {
  width: 72px;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 16px 12px;
  height: 84px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.brand-icon {
  width: 40px;
  height: 40px;
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.22) 0%, rgba(118, 75, 162, 0.18) 100%);
  color: rgba(255, 255, 255, 0.92);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.18);
  flex-shrink: 0;
}

.brand-svg {
  width: 22px;
  height: 22px;
}

.brand-img {
  width: 22px;
  height: 22px;
  display: block;
}

.brand-text {
  font-size: 15px;
  line-height: 1.35;
  font-weight: 700;
  color: #fff;
  white-space: normal;
  word-break: break-all;
}

.sidebar-menu {
  list-style: none;
  padding: 10px 0;
  margin: 0;
}

.menu-item {
  position: relative;
}

.menu-link {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  color: #cbd5e1;
  text-decoration: none;
  transition: all 0.2s;
  cursor: pointer;
  position: relative;
  border-radius: 14px;
  margin: 6px 10px;
}

.menu-icon {
  width: 22px;
  height: 22px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: rgba(203, 213, 225, 0.92);
  flex-shrink: 0;
}

.menu-icon :deep(.i) {
  width: 22px;
  height: 22px;
}

.menu-img {
  width: 22px;
  height: 22px;
  display: block;
}

.menu-text {
  font-size: 14px;
  font-weight: 600;
  color: rgba(226, 232, 240, 0.92);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.menu-link:hover {
  background: rgba(255, 255, 255, 0.06);
  color: #fff;
  transform: translateY(-1px);
}

.menu-item.active > .menu-link {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.18) 0%, rgba(118, 75, 162, 0.14) 100%);
  border: 1px solid rgba(102, 126, 234, 0.28);
  box-shadow: 0 10px 25px rgba(102, 126, 234, 0.12);
}

.menu-item.active > .menu-link .menu-icon {
  color: rgba(255, 255, 255, 0.96);
}

.submenu {
  list-style: none;
  padding: 0;
  margin: 6px 10px 12px;
  background-color: rgba(0, 0, 0, 0.2);
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.submenu-item {
  margin: 0;
  padding: 0;
}

.submenu-item a {
  display: block;
  width: 100%;
  padding: 11px 14px 11px 44px;
  color: #94a3b8;
  text-decoration: none;
  transition: all 0.2s;
  font-weight: 600;
  font-size: 13px;
  border-radius: 0;
}

.submenu-item a:hover,
.submenu-item.active a {
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.95);
}

.sidebar.collapsed .menu-link {
  justify-content: center;
  margin: 6px 8px;
  padding: 12px 10px;
}

.submenu-popover {
  position: absolute;
  left: 78px;
  top: 8px;
  min-width: 200px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(14px);
  border-radius: 16px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.18);
  padding: 10px 10px;
  z-index: 999;
}

.popover-title {
  font-size: 12px;
  font-weight: 800;
  color: #334155;
  padding: 8px 10px 10px;
}

.popover-item {
  display: block;
  padding: 10px 10px;
  border-radius: 12px;
  color: #475569;
  font-weight: 700;
  font-size: 13px;
  transition: all 0.18s ease;
  text-decoration: none;
}

.popover-item:hover {
  background: rgba(102, 126, 234, 0.10);
  color: #334155;
}

.popover-item.active {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.16) 0%, rgba(118, 75, 162, 0.12) 100%);
  border: 1px solid rgba(102, 126, 234, 0.22);
}
</style>
