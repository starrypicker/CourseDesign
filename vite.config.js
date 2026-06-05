import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src') // 配置@等价于src文件夹，适配项目所有代码
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
        // 不需要 rewrite，后端路径本身就带 /api 前缀
        // 前端请求 /api/product/list → 代理到 http://localhost:8080/api/product/list
      }
    }
  }
})