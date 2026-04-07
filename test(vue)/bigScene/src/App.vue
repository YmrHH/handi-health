<template>
  <div class="frame">
    <template v-if="!isAuthed">
      <ScreenHeader title="寒岐智护·慢性病随访健康预警管理平台" sub-title="请先登录医生账号以加载真实数据" />
      <div class="content">
        <LoginScreen @logged-in="handleLoggedIn" />
      </div>
    </template>

    <template v-else>
      <ScreenHeader title="寒岐智护·慢性病随访健康预警管理平台" sub-title="医疗运营监控数据大屏">
        <ScreenTabs v-model="active" :tabs="tabs" />
      </ScreenHeader>
      <div class="content">
        <KeepAlive>
          <component :is="activeComp" />
        </KeepAlive>
      </div>
      <ScreenFooter
        :brand="footerBrand"
        :page-label="footerPageLabel"
        :status-text="footerState.statusText"
        :status-tone="footerState.statusTone"
        :latency-ms="footerState.latencyMs"
        :security-text="footerState.securityText"
        :version-text="footerState.versionText"
        :updated-at="footerState.updatedAt"
        :ticker-items="footerState.tickerItems"
      />
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent, ref } from 'vue'
import ScreenHeader from './components/ScreenHeader.vue'
import ScreenTabs from './components/ScreenTabs.vue'
import ScreenFooter from './components/ScreenFooter.vue'
import { footerState } from './utils/footerState'

const CommandCenter = defineAsyncComponent(() => import('./pages/CommandCenter.vue'))
const RiskPortraitBoard = defineAsyncComponent(() => import('./pages/RiskPortraitBoard.vue'))
const AlertLinkageBoard = defineAsyncComponent(() => import('./pages/AlertLinkageBoard.vue'))
const InterventionServiceBoard = defineAsyncComponent(() => import('./pages/InterventionServiceBoard.vue'))
const AIInsightBoard = defineAsyncComponent(() => import('./pages/AIInsightBoard.vue'))
const LoginScreen = defineAsyncComponent(() => import('./pages/LoginScreen.vue'))

const tabs = [
  { key: 'command', label: '总指挥舱' },
  { key: 'risk', label: '风险画像舱' },
  { key: 'alert', label: '告警联动舱' },
  { key: 'service', label: '干预服务舱' },
  { key: 'ai', label: 'AI 洞察舱' }
]

const active = ref('command')
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

const pageLabel = computed(() => {
  const cur = tabs.find((x) => x.key === active.value)
  return cur?.label || '总指挥舱'
})

const footerBrand = computed(() => footerState.brand)
const footerPageLabel = computed(() => pageLabel.value)
</script>
