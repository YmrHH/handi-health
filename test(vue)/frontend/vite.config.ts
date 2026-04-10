import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const backendTarget = 'http://192.140.173.165:8081'

  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    server: {
      port: 5173,
      proxy: {
        // 只代理 API 请求，前端路由（/login, /register 等）不需要代理
        '/api': {
          target: backendTarget, // 强制 IPv4，避免部分环境下 localhost 解析为 ::1 导致 ECONNREFUSED
          changeOrigin: true,
          rewrite: (path) => path
        },
        // 仅代理后端 PatientController 等无 /api 前缀的接口（必须以 /patient/ 开头）
        // 这样不会劫持前端路由 /patient（无尾斜杠）
        '/patient/': {
          target: backendTarget,
          changeOrigin: true,
          rewrite: (path) => path
        }
      }
    },
    build: {
      outDir: 'dist',
      assetsDir: 'assets',
      sourcemap: false,
      minify: 'terser',
      terserOptions: {
        compress: {
          drop_console: true,
          drop_debugger: true
        }
      }
    }
  }
})

