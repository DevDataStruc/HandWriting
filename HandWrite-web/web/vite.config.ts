import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'node:path'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { createSvgIconsPlugin } from 'vite-plugin-svg-icons'
import { viteMockServe } from 'vite-plugin-mock'
import compression from 'vite-plugin-compression'

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  return {
    base: './',
    plugins: [
      vue(),
      AutoImport({
        imports: ['vue', 'vue-router', 'pinia', '@vueuse/core'],
        dts: 'src/auto-imports.d.ts',
        resolvers: [ElementPlusResolver()],
        eslintrc: {
          enabled: true,
        },
      }),
      Components({
        dts: 'src/components.d.ts',
        resolvers: [ElementPlusResolver({ importStyle: 'scss' })],
      }),
      createSvgIconsPlugin({
        iconDirs: [path.resolve(process.cwd(), 'src/assets/icons')],
        symbolId: 'icon-[name]',
      }),
      viteMockServe({
        mockPath: 'mock',
        enable: env.VITE_USE_MOCK === 'true',
      }),
      compression({
        algorithm: 'gzip',
        ext: '.gz',
        threshold: 1024,
      }),
      compression({
        algorithm: 'brotliCompress',
        ext: '.br',
        threshold: 1024,
      }),
    ],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, 'src'),
      },
    },
    css: {
      preprocessorOptions: {
        scss: {
          api: 'modern-compiler',
          loadPaths: [path.resolve(__dirname, 'src/styles')],
          // The `@` alias is a Vite resolver, not a Sass resolver, so we
          // use `loadPaths` + bare module names to inject the shared
          // variables/mixins into every SCSS file. Files inside src/styles/
          // manage their own @use imports to avoid double-loading.
          additionalData: (source: string, filename: string) => {
            if (filename.replace(/\\/g, '/').includes('/src/styles/')) {
              return source
            }
            return `@use "variables" as *;\n@use "mixins" as *;\n${source}`
          },
        },
      },
    },
    server: {
      host: '0.0.0.0',
      port: 5173,
      open: true,
      proxy: {
        '/v1': {
          target: 'http://localhost:8080',
          changeOrigin: true,
          // 不再 rewrite：后端 Controller 实际路径是 /v1/auth/*、/v1/sample/* 等
          // 之前用 rewrite 去掉 /v1 会导致后端 404，进而被 Security 兜底返回 1001
        },
      },
    },
    build: {
      target: 'es2015',
      outDir: 'dist',
      assetsDir: 'static',
      sourcemap: false,
      chunkSizeWarningLimit: 1500,
      rollupOptions: {
        output: {
          manualChunks: {
            vue: ['vue', 'vue-router', 'pinia'],
            element: ['element-plus'],
            echarts: ['echarts', 'vue-echarts'],
            utils: ['axios', 'dayjs', 'lodash-es'],
          },
        },
      },
    },
    optimizeDeps: {
      include: ['perfect-freehand', 'signature_pad'],
    },
  }
})
