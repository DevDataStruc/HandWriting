import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import pinia from './stores'
import i18n from './locales'
import { setupDirectives } from './directives'
import { setupIcons } from './directives/icons'

// Element Plus 全局样式按需由 unplugin-vue-components 注入
import 'virtual:svg-icons-register'
import './styles/index.scss'

const app = createApp(App)

app.use(pinia)
app.use(router)
app.use(i18n)

setupDirectives(app)
setupIcons(app)

// 全局错误处理
app.config.errorHandler = (err, _vm, info) => {
  console.error('[Global Error]', err, info)
}

router.isReady().then(() => {
  app.mount('#app')
  // 移除首屏 loading
  const loadingEl = document.getElementById('app-loading')
  if (loadingEl) loadingEl.remove()
})
