import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { useAuthStore } from './stores/auth'
import './assets/styles/main.css'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

const app = createApp(App)

app.config.errorHandler = (err, instance, info) => {
  const name = (instance as any)?.type?.name || (instance as any)?.type?.__name || 'AnonymousComponent'
  console.error('[VueErrorHandler]', { name, info, err })
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus)

// 初始化认证状态（从sessionStorage恢复）
const authStore = useAuthStore()
authStore.initAuth()

app.mount('#app')

