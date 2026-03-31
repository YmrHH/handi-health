<template>
  <div class="login-wrap">
      <div class="login-card">
      <div class="login-title">寒岐智护·慢性病随访健康预警管理平台 · 大屏登录</div>
      <div class="login-sub">使用医生账号登录后，按真实权限加载病种与患者数据</div>

      <form class="login-form" @submit.prevent="handleSubmit">
        <label class="field">
          <span>手机号</span>
          <input v-model.trim="phone" type="text" placeholder="请输入手机号" />
        </label>
        <label class="field">
          <span>密码</span>
          <input v-model="password" type="password" placeholder="请输入密码" />
        </label>

        <button type="submit" class="btn" :disabled="submitting">
          {{ submitting ? '登录中...' : '登录并进入大屏' }}
        </button>

        <p v-if="error" class="error">{{ error }}</p>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { clearApiCache, loginByPhone } from '../api'

const phone = ref('17688498714')
const password = ref('111111')
const submitting = ref(false)
const error = ref('')

const emit = defineEmits<{ (e: 'logged-in'): void }>()

async function handleSubmit() {
  error.value = ''
  if (!phone.value || !password.value) {
    error.value = '请输入手机号和密码'
    return
  }
  submitting.value = true
  try {
    const res = await loginByPhone(phone.value, password.value)
    const userId = res?.userId ?? res?.id
    if (!userId) {
      error.value = res?.message || '登录失败，未获取到用户ID'
      return
    }
    sessionStorage.setItem('token', String(userId))
    clearApiCache()
    emit('logged-in')
  } catch (e: any) {
    error.value = e?.message || '登录失败，请检查网络或账号密码'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.login-wrap {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-card {
  width: 420px;
  padding: 32px 28px 28px;
  border-radius: 18px;
  border: 1px solid rgba(114, 180, 205, 0.34);
  background: radial-gradient(circle at 0 0, rgba(95, 199, 216, 0.22), transparent 55%),
    radial-gradient(circle at 100% 100%, rgba(158, 169, 230, 0.18), transparent 55%),
    rgba(255, 255, 255, 0.86);
  box-shadow: var(--shadow), var(--glow);
}

.login-title {
  font-size: 18px;
  font-weight: 800;
  letter-spacing: 1px;
}

.login-sub {
  margin-top: 8px;
  font-size: 12px;
  color: var(--t-2);
}

.login-form {
  margin-top: 18px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
}

.field span {
  color: var(--t-1);
}

input {
  padding: 8px 10px;
  border-radius: 8px;
  border: 1px solid rgba(114, 180, 205, 0.45);
  background: rgba(255, 255, 255, 0.76);
  color: var(--t-0);
  outline: none;
}

input:focus {
  border-color: rgba(95, 199, 216, 0.78);
  box-shadow: 0 0 0 1px rgba(95, 199, 216, 0.35);
}

.btn {
  margin-top: 6px;
  padding: 9px 12px;
  border-radius: 999px;
  border: none;
  background: linear-gradient(90deg, #5fc7d8, #7fd6e3);
  color: #020617;
  font-weight: 800;
  font-size: 13px;
  letter-spacing: 1px;
  cursor: pointer;
}

.btn:disabled {
  opacity: 0.7;
  cursor: default;
}

.error {
  margin-top: 4px;
  font-size: 12px;
  color: rgba(238, 141, 153, 0.95);
}
</style>

