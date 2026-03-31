 <template>
  <div class="auth-wrapper">
    <div class="auth-card">
      <div class="auth-title">注册新账号</div>
      <div class="auth-subtitle">创建一个新账号以使用 XX 管理系统（手机号需唯一，用于登录）</div>

      <form class="auth-form" @submit.prevent="handleRegister">
        <div v-if="errorMessage" class="auth-error">
          {{ errorMessage }}
        </div>
        <div v-if="successMessage" class="auth-success">
          {{ successMessage }}
        </div>

        <input
          class="auth-input"
          type="text"
          name="name"
          v-model="form.name"
          placeholder="医生姓名（必填，可重复）"
          autocomplete="name"
          required
        />
        <input
          class="auth-input"
          type="password"
          name="password"
          v-model="form.password"
          placeholder="密码（必填）"
          autocomplete="new-password"
          required
        />
        <input
          class="auth-input"
          type="password"
          name="confirm"
          v-model="form.confirmPassword"
          placeholder="再次输入密码"
          autocomplete="new-password"
          required
        />
        <input
          class="auth-input"
          type="tel"
          name="phone"
          v-model="form.phone"
          placeholder="手机号（必填）"
          required
        />

        <button class="auth-btn" type="submit" :disabled="loading">
          {{ loading ? '注册中...' : '注册' }}
        </button>
      </form>

      <div class="auth-footer">
        已有账号？
        <RouterLink to="/login" class="auth-footer-link">直接登录</RouterLink>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const form = ref({
  name: '',
  password: '',
  confirmPassword: '',
  phone: ''
})

const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

async function handleRegister() {
  errorMessage.value = ''
  successMessage.value = ''

  // 验证密码确认
  if (form.value.password !== form.value.confirmPassword) {
    errorMessage.value = '两次输入的密码不一致'
    return
  }

  loading.value = true

  try {
    const result = await authStore.register({
      name: form.value.name,
      password: form.value.password,
      confirmPassword: form.value.confirmPassword,
      phone: form.value.phone
    })

    if (result.success) {
      successMessage.value = result.message || '注册成功，请使用新账号登录'
      // 2秒后跳转到登录页
      setTimeout(() => {
        router.push('/login')
      }, 2000)
    } else {
      errorMessage.value = result.message || '注册失败'
    }
  } catch (error: any) {
    errorMessage.value = error.message || '注册失败，请检查网络连接'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Helvetica, Arial, sans-serif;
  padding: 20px;
  position: relative;
  overflow: hidden;
}

.auth-wrapper::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><defs><pattern id="grain" width="100" height="100" patternUnits="userSpaceOnUse"><circle cx="25" cy="25" r="1" fill="white" opacity="0.1"/><circle cx="75" cy="75" r="1" fill="white" opacity="0.1"/><circle cx="50" cy="10" r="0.5" fill="white" opacity="0.1"/><circle cx="10" cy="50" r="0.5" fill="white" opacity="0.1"/><circle cx="90" cy="30" r="0.5" fill="white" opacity="0.1"/></pattern></defs><rect width="100" height="100" fill="url(%23grain)"/></svg>');
  opacity: 0.3;
}

.auth-card {
  width: 420px;
  max-width: 90vw;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  box-shadow: 
    0 20px 25px -5px rgba(0, 0, 0, 0.1),
    0 10px 10px -5px rgba(0, 0, 0, 0.04),
    0 0 0 1px rgba(255, 255, 255, 0.05);
  padding: 40px 36px 32px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  position: relative;
  z-index: 1;
  animation: slideUp 0.6s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.auth-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  border-radius: 20px 20px 0 0;
}

.auth-title {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 8px;
  color: #1a202c;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.auth-subtitle {
  font-size: 14px;
  color: #718096;
  margin-bottom: 32px;
  font-weight: 500;
}

.auth-form {
  width: 100%;
}

.auth-input {
  width: 100%;
  height: 48px;
  border-radius: 12px;
  border: 2px solid #e2e8f0;
  padding: 0 16px;
  outline: none;
  box-sizing: border-box;
  margin-bottom: 20px;
  font-size: 14px;
  font-weight: 500;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  color: #2d3748;
}

.auth-input::placeholder {
  color: #a0aec0;
  font-weight: 400;
}

.auth-input:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
  background: #ffffff;
  transform: translateY(-1px);
}

.auth-btn {
  width: 100%;
  height: 48px;
  border-radius: 12px;
  border: none;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
  font-weight: 600;
  font-size: 16px;
  cursor: pointer;
  margin-top: 8px;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 14px 0 rgba(102, 126, 234, 0.39);
  position: relative;
  overflow: hidden;
}

.auth-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.5s;
}

.auth-btn:hover:not(:disabled)::before {
  left: 100%;
}

.auth-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px 0 rgba(102, 126, 234, 0.5);
}

.auth-btn:active {
  transform: translateY(0);
}

.auth-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
  box-shadow: 0 4px 14px 0 rgba(102, 126, 234, 0.2);
}

.auth-footer {
  margin-top: 24px;
  font-size: 14px;
  text-align: center;
  color: #4a5568;
  font-weight: 500;
}

.auth-footer-link {
  color: #667eea;
  text-decoration: none;
  font-weight: 600;
  transition: color 0.2s ease;
  position: relative;
}

.auth-footer-link::after {
  content: '';
  position: absolute;
  width: 0;
  height: 2px;
  bottom: -2px;
  left: 0;
  background: linear-gradient(90deg, #667eea, #764ba2);
  transition: width 0.3s ease;
}

.auth-footer-link:hover {
  color: #764ba2;
}

.auth-footer-link:hover::after {
  width: 100%;
}

.auth-error {
  font-size: 13px;
  color: #e53e3e;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: rgba(254, 226, 226, 0.9);
  border-radius: 8px;
  border: 1px solid #fc8181;
  font-weight: 500;
  backdrop-filter: blur(10px);
}

.auth-success {
  font-size: 13px;
  color: #38a169;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: rgba(198, 246, 213, 0.9);
  border-radius: 8px;
  border: 1px solid #68d391;
  font-weight: 500;
  backdrop-filter: blur(10px);
}

/* 响应式设计 */
@media (max-width: 480px) {
  .auth-card {
    padding: 32px 28px 24px;
  }
  
  .auth-title {
    font-size: 20px;
  }
  
  .auth-input {
    height: 44px;
    margin-bottom: 16px;
  }
  
  .auth-btn {
    height: 44px;
    font-size: 15px;
  }
}
</style>

