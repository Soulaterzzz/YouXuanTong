import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  build: {
    assetsInlineLimit: 4096,
    cssCodeSplit: true,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) {
            return undefined
          }

          if (id.includes('vue') || id.includes('vue-router')) {
            return 'vue-vendor'
          }

          if (id.includes('element-plus')) {
            return 'element-plus'
          }

          if (id.includes('axios')) {
            return 'axios'
          }

          return undefined
        },
        chunkFileNames: 'assets/[name]-[hash].js',
        entryFileNames: 'assets/[name]-[hash].js',
        assetFileNames: 'assets/[name]-[hash][extname]'
      }
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      '/files': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      '/agreements': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      }
    }
  }
})
