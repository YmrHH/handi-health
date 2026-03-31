<template>
  <div class="frame">
    <template v-if="!isAuthed">
      <ScreenHeader
        title="寒岐智护·慢性病随访健康预警管理平台｜运营驾驶舱"
        sub-title="请先登录医生账号以加载真实数据"
        :org-name="orgName"
        :updated-at="updatedAt"
      >
        <span class="header-pill">登录后仅作只读展示</span>
      </ScreenHeader>
      <div class="content">
        <LoginScreen @logged-in="handleLoggedIn" />
      </div>
    </template>
    <template v-else>
      <ScreenHeader
        title="寒岐智护·慢性病随访健康预警管理平台｜运营驾驶舱"
        sub-title="医疗运营监控数据大屏"
        :org-name="orgName"
        :updated-at="updatedAt"
      >
        <ScreenTabs v-model="active" :tabs="tabs" />
        <span class="header-pill">只读展示</span>
      </ScreenHeader>

      <div class="content">
        <KeepAlive>
          <component :is="activeComp" />
        </KeepAlive>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import ScreenHeader from './components/ScreenHeader.vue'
import ScreenTabs from './components/ScreenTabs.vue'
import CommandCenter from './pages/CommandCenter.vue'
import RiskPortraitBoard from './pages/RiskPortraitBoard.vue'
import AlertLinkageBoard from './pages/AlertLinkageBoard.vue'
import InterventionServiceBoard from './pages/InterventionServiceBoard.vue'
import AIInsightBoard from './pages/AIInsightBoard.vue'
import LoginScreen from './pages/LoginScreen.vue'

const tabs = [
  { key: 'command', label: '总指挥舱' },
  { key: 'risk', label: '风险画像舱' },
  { key: 'alert', label: '告警联动舱' },
  { key: 'service', label: '干预服务舱' },
  { key: 'ai', label: 'AI 洞察舱' }
]

const active = ref('command')
const orgName = ref('机构：默认组织')
const updatedAt = ref('—')

const isAuthed = ref(typeof sessionStorage !== 'undefined' && !!sessionStorage.getItem('token'))

const compMap = {
  command: CommandCenter,
  risk: RiskPortraitBoard,
  alert: AlertLinkageBoard,
  service: InterventionServiceBoard,
  ai: AIInsightBoard
} as const

const activeComp = computed(() => compMap[active.value as keyof typeof compMap] || CommandCenter)

function handleLoggedIn() {
  isAuthed.value = true
}
</script>

