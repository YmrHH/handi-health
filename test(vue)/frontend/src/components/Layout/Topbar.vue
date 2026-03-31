<template>
  <div class="topbar">
    <div class="topbar-left">
      <div class="title-icon" aria-hidden="true">
        <img v-if="pageIconUrl" class="icon-img" :src="safeUrl(pageIconUrl)" alt="" />
      </div>
      <div class="topbar-title">{{ pageTitle }}</div>
    </div>
    <div class="topbar-right">
      <span class="topbar-avatar"></span>
      <span class="topbar-username">{{ username || 'Admin' }}</span>
      <a href="javascript:void(0);" class="topbar-logout" @click="handleLogout">退出</a>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

function safeUrl(url: string) {
  return String(url || '').replace('/public/', '/')
}

defineProps<{
  pageTitle: string
  pageIconUrl?: string
}>()

const router = useRouter()
const authStore = useAuthStore()

const username = computed(() => authStore.username)

async function handleLogout() {
  await authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 60px;
  padding: 0 24px;
  background-color: #fff;
  border-bottom: 1px solid #e2e8f0;
  flex-shrink: 0;
  position: relative;
  z-index: 20;
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.title-icon {
  width: 40px;
  height: 40px;
  border-radius: 14px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.92);
  box-shadow: 0 10px 25px rgba(102, 126, 234, 0.18);
  flex-shrink: 0;
}

.title-icon .icon-img {
  width: 22px;
  height: 22px;
  display: block;
}

.topbar-title {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.topbar-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: #3c7cff;
  display: inline-block;
}

.topbar-username {
  font-size: 14px;
  color: #64748b;
}

.topbar-logout {
  color: #64748b;
  text-decoration: none;
  font-size: 14px;
  cursor: pointer;
  transition: color 0.2s;
}

.topbar-logout:hover {
  color: #3c7cff;
}
</style>

