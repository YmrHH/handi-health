import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { LoginRequest, RegisterRequest } from '@/api/types'
import { authApi } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const username = ref<string | null>(null)
  const loginUsername = ref<string | null>(null)
  const token = ref<string | null>(null)

  const authChecked = ref(false)
  const authCheckingPromise = ref<Promise<void> | null>(null)

  const isAuthenticated = computed(() => !!token.value)

  // 登录（真实访问后端）
  async function login(loginData: LoginRequest): Promise<{ success: boolean; message?: string }> {
    const res = await authApi.login(loginData)
    if (!res || !res.success) {
      return { success: false, message: res?.message || '登录失败' }
    }

    // 兼容后端：可能返回 { userId, role, name } 或包装在 data 中
    const data: any = (res as any).data || {}
    const userId = data.userId ?? data.id ?? (res as any).userId
    const finalToken = userId != null ? String(userId) : null
    if (!finalToken) {
      return { success: false, message: '登录成功但未获取到 userId' }
    }

    const displayName = (data.name as string) || (data.username as string) || loginData.phone || String(userId)
    const backendUsername = (data.username as string) || null
    username.value = displayName
    loginUsername.value = backendUsername
    token.value = finalToken

    sessionStorage.setItem('username', displayName)
    if (backendUsername) {
      sessionStorage.setItem('loginUsername', backendUsername)
    } else {
      sessionStorage.removeItem('loginUsername')
    }
    sessionStorage.setItem('token', finalToken)
    return { success: true, message: res.message || '登录成功' }
  }

  // 注册（真实访问后端）
  async function register(registerData: RegisterRequest): Promise<{ success: boolean; message?: string }> {
    const res = await authApi.register(registerData)
    if (!res || !res.success) {
      return { success: false, message: res?.message || '注册失败' }
    }
    return { success: true, message: res.message || '注册成功' }
  }

  // 登出
  async function logout(): Promise<void> {
    username.value = null
    loginUsername.value = null
    token.value = null
    authChecked.value = true
    authCheckingPromise.value = null
    sessionStorage.removeItem('username')
    sessionStorage.removeItem('loginUsername')
    sessionStorage.removeItem('token')
  }

  // 初始化（从sessionStorage或cookie恢复登录状态）
  function initAuth(): void {
    // 可以从sessionStorage读取
    const savedUsername = sessionStorage.getItem('username')
    if (savedUsername) {
      username.value = savedUsername
    }

    const savedLoginUsername = sessionStorage.getItem('loginUsername')
    if (savedLoginUsername) {
      loginUsername.value = savedLoginUsername
    }

    const savedToken = sessionStorage.getItem('token')
    if (savedToken) {
      token.value = savedToken
    }

    // 刷新页面后，需要重新向后端验证一次，避免前端仅凭 sessionStorage 误判为已登录
    authChecked.value = false
    authCheckingPromise.value = null
  }

  async function validateSession(): Promise<void> {
    // 无 token 时，直接认为鉴权检查已完成
    if (!token.value) {
      authChecked.value = true
      return
    }

    try {
      const res: any = await authApi.me()
      const data = (res as any)?.data || {}
      const displayName = (data.name as string) || (data.username as string) || username.value
      if (displayName) {
        username.value = displayName
        sessionStorage.setItem('username', displayName)
      }

      const backendUsername = (data.username as string) || null
      loginUsername.value = backendUsername
      if (backendUsername) {
        sessionStorage.setItem('loginUsername', backendUsername)
      } else {
        sessionStorage.removeItem('loginUsername')
      }

      authChecked.value = true
    } catch {
      // 后端校验失败（401/会话过期/用户不存在等）：清空本地登录态
      await logout()
    }
  }

  async function ensureAuthChecked(): Promise<void> {
    if (authChecked.value) return
    if (authCheckingPromise.value) return authCheckingPromise.value

    authCheckingPromise.value = (async () => {
      try {
        await validateSession()
      } finally {
        authChecked.value = true
        authCheckingPromise.value = null
      }
    })()

    return authCheckingPromise.value
  }

  return {
    username,
    loginUsername,
    token,
    isAuthenticated,
    login,
    register,
    logout,
    initAuth,
    authChecked,
    validateSession,
    ensureAuthChecked
  }
})

