<template>
  <div class="login-page">
    <div class="login-shell">
      <!-- 左侧蓝色背景 + 插画区域（布局和旧 JSP 一致） -->
      <div class="login-left-wrap"></div>
      <div class="login-logo">寒岐智护·慢性病随访健康预警管理平台</div>
      <div class="login-left">
        <!-- 左侧插画：使用 picture/login.png -->
        <div class="login-illustration-image"></div>
      </div>

      <!-- 右侧表单区域 -->
      <div class="login-right">
        <div class="login-title">欢迎登录寒岐智护·慢性病随访健康预警管理平台</div>
        <div class="login-subtitle">请使用您的账号登录系统，进行随访管理与数据分析</div>

        <form class="login-form" @submit.prevent="handleLogin">
          <div v-if="errorMessage" class="login-error">
            {{ errorMessage }}
          </div>

          <div class="form-label">手机号</div>
          <input
            id="phone"
            v-model="form.phone"
            class="login-input"
            type="text"
            placeholder="请输入手机号"
            autocomplete="tel"
            required
          />

          <div class="form-label">密码</div>
          <input
            id="password"
            v-model="form.password"
            class="login-input"
            type="password"
            placeholder="请输入登录密码"
            autocomplete="current-password"
            required
          />

          <div class="login-row">
            <label>
              <input type="checkbox" v-model="remember" />
              记住密码
            </label>
            <span class="login-row-right">
              <!-- 保留可点击注册入口，但样式贴近原 JSP 的灰色文案 -->
              <RouterLink to="/register" class="link-register">注册账号</RouterLink>
            </span>
          </div>

          <button class="login-btn" type="submit" :disabled="loading">
            {{ loading ? '登录中...' : '登 录' }}
          </button>
        </form>

        <div class="login-corner"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const form = ref({
  phone: '',
  password: ''
})

const remember = ref(false)
const loading = ref(false)
const errorMessage = ref('')

onMounted(() => {
  // 从localStorage读取记住的密码
  const savedUsername = localStorage.getItem('remembered_username')
  const savedPassword = localStorage.getItem('remembered_password')
  if (savedUsername && savedPassword) {
    form.value.phone = savedUsername
    form.value.password = savedPassword
    remember.value = true
  }
})

async function handleLogin() {
  errorMessage.value = ''
  loading.value = true

  try {
    const result = await authStore.login({
      phone: form.value.phone,
      password: form.value.password
    })

    if (result.success) {
      // 记住密码
      if (remember.value) {
        localStorage.setItem('remembered_username', form.value.phone)
        localStorage.setItem('remembered_password', form.value.password)
      } else {
        localStorage.removeItem('remembered_username')
        localStorage.removeItem('remembered_password')
      }

      // 跳转到目标页面或首页
      const redirect = (route.query.redirect as string) || '/home'
      router.push(redirect)
    } else {
      errorMessage.value = result.message || '登录失败'
    }
  } catch (error: any) {
    errorMessage.value = error.message || '登录失败，请检查网络连接'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  position: relative;
}

.login-page::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><defs><pattern id="grain" width="100" height="100" patternUnits="userSpaceOnUse"><circle cx="25" cy="25" r="1" fill="rgba(255,255,255,0.1)"/><circle cx="75" cy="75" r="1" fill="rgba(255,255,255,0.1)"/><circle cx="50" cy="10" r="0.5" fill="rgba(255,255,255,0.05)"/><circle cx="10" cy="60" r="0.5" fill="rgba(255,255,255,0.05)"/><circle cx="90" cy="40" r="0.5" fill="rgba(255,255,255,0.05)"/></pattern></defs><rect width="100" height="100" fill="url(%23grain)"/></svg>');
  animation: float 20s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0px) rotate(0deg); }
  50% { transform: translateY(-20px) rotate(1deg); }
}

.login-shell {
  width: 100%;
  max-width: 1080px;
  height: 600px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 
    0 25px 50px -12px rgba(0, 0, 0, 0.25),
    0 0 0 1px rgba(255, 255, 255, 0.2);
  display: flex;
  position: relative;
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.login-left-wrap {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 48%;
  background: linear-gradient(140deg, #667eea 0%, #764ba2 80%);
  border-radius: 24px 0 120px 24px;
  overflow: hidden;
}

.login-left-wrap::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(45deg, rgba(255, 255, 255, 0.1) 0%, transparent 100%);
}

.login-left {
  position: relative;
  width: 48%;
  padding: 40px 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1;
}

.login-illustration-image {
  width: 380px;
  max-width: 100%;
  height: 320px;
  border-radius: 20px;
  background-image: url('/picture/login.png');
  background-size: contain;
  background-position: center;
  background-repeat: no-repeat;
  box-shadow: 
    0 25px 50px -12px rgba(0, 0, 0, 0.25),
    0 0 0 1px rgba(255, 255, 255, 0.1);
  overflow: hidden;
  background-color: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  transition: transform 0.3s ease;
}

.login-illustration-image:hover {
  transform: scale(1.05);
}

.login-right {
  margin-left: auto;
  width: 52%;
  padding: 80px 80px 60px;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  background: rgba(255, 255, 255, 0.02);
  min-width: 0; /* 允许 flex 子项收缩，避免标题撑破 */
}

.login-title {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 12px;
  background: linear-gradient(135deg, #2d3748 0%, #4a5568 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  white-space: nowrap;
  max-width: 100%;
}

.login-subtitle {
  font-size: 14px;
  color: #718096;
  margin-bottom: 40px;
  font-weight: 500;
}

.form-label {
  font-size: 14px;
  margin-bottom: 8px;
  font-weight: 600;
  color: #2d3748;
}

.login-input {
  width: 100%;
  height: 48px;
  border-radius: 12px;
  border: 2px solid #e2e8f0;
  padding: 0 16px;
  outline: none;
  font-size: 14px;
  margin-bottom: 20px;
  transition: all 0.3s ease;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  font-weight: 500;
}

.login-input:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.1);
  background: #ffffff;
}

.login-input::placeholder {
  color: #a0aec0;
  font-weight: 400;
}

.login-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
  margin-bottom: 32px;
  font-size: 13px;
  color: #4a5568;
  font-weight: 500;
}

.login-row label {
  display: flex;
  align-items: center;
  cursor: pointer;
  transition: color 0.2s ease;
}

.login-row label:hover {
  color: #667eea;
}

.login-row input[type='checkbox'] {
  margin-right: 8px;
  transform: scale(1.1);
}

.login-row-right {
  color: #667eea;
  font-size: 13px;
  font-weight: 600;
}

.link-register {
  color: #667eea;
  text-decoration: none;
  font-weight: 600;
  transition: all 0.2s ease;
  position: relative;
}

.link-register::after {
  content: '';
  position: absolute;
  width: 0;
  height: 2px;
  bottom: -2px;
  left: 0;
  background: linear-gradient(90deg, #667eea, #764ba2);
  transition: width 0.3s ease;
}

.link-register:hover::after {
  width: 100%;
}

.login-btn {
  width: 100%;
  height: 52px;
  border-radius: 12px;
  border: none;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 14px 0 rgba(102, 126, 234, 0.39);
  position: relative;
  overflow: hidden;
}

.login-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.5s;
}

.login-btn:hover::before {
  left: 100%;
}

.login-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.login-btn:hover:enabled {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px 0 rgba(102, 126, 234, 0.5);
}

.login-btn:active:enabled {
  transform: translateY(0);
}

.login-error {
  font-size: 13px;
  color: #e53e3e;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: rgba(254, 178, 178, 0.2);
  border-radius: 8px;
  border-left: 4px solid #e53e3e;
  font-weight: 500;
}

.login-logo {
  position: absolute;
  left: 40px;
  top: 30px;
  z-index: 2;
  font-size: 20px;
  color: #ffffff;
  font-weight: 700;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  white-space: nowrap;
}

.login-corner {
  position: absolute;
  right: -120px;
  bottom: -80px;
  width: 260px;
  height: 160px;
  background: rgba(248, 251, 255, 0.1);
  border-radius: 140px 0 0 0;
  backdrop-filter: blur(10px);
}

@media (max-width: 960px) {
  .login-shell {
    max-width: 480px;
    height: auto;
  }
  .login-left-wrap,
  .login-left,
  .login-illustration-image,
  .login-corner,
  .login-logo {
    display: none;
  }
  .login-right {
    width: 100%;
    padding: 40px 32px 32px;
  }
  .login-title {
    font-size: 24px;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}
</style>

