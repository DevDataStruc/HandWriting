import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import { storage } from '@/utils/storage'
import { LOCALE_KEY, SIDEBAR_KEY, THEME_KEY } from '@/utils/constants'
import { ThemeMode } from '@/utils/constants'

export type Locale = 'zh-CN' | 'en-US'

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref<boolean>(storage.get<boolean>(SIDEBAR_KEY, false))
  const theme = ref<ThemeMode>((storage.getString(THEME_KEY, ThemeMode.LIGHT) as ThemeMode) || ThemeMode.LIGHT)
  const locale = ref<Locale>((storage.getString(LOCALE_KEY, 'zh-CN') as Locale) || 'zh-CN')
  const isMobile = ref<boolean>(window.innerWidth < 768)
  const devicePixelRatio = ref<number>(window.devicePixelRatio || 1)

  function toggleSidebar(): void {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function setSidebar(v: boolean): void {
    sidebarCollapsed.value = v
  }

  function setTheme(t: ThemeMode): void {
    theme.value = t
    document.documentElement.dataset.theme = t
  }

  function setLocale(l: Locale): void {
    locale.value = l
  }

  function setMobile(v: boolean): void {
    isMobile.value = v
  }

  watch(sidebarCollapsed, (v) => storage.set(SIDEBAR_KEY, v), { immediate: false })
  watch(theme, (v) => storage.setString(THEME_KEY, v), { immediate: false })
  watch(locale, (v) => storage.setString(LOCALE_KEY, v), { immediate: false })

  // 初始化主题
  document.documentElement.dataset.theme = theme.value

  return {
    sidebarCollapsed,
    theme,
    locale,
    isMobile,
    devicePixelRatio,
    toggleSidebar,
    setSidebar,
    setTheme,
    setLocale,
    setMobile,
  }
})
