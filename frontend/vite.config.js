import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 前端开发服务器 5173, 把 /api 和 /ws 反向代理到后端 8080
export default defineConfig({
  plugins: [vue()],
  define: {
    global: 'window'
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/ws': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        ws: true
      }
    }
  }
})
