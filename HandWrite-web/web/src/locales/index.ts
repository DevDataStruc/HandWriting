import { createI18n } from 'vue-i18n'
import zhCN from './zh-CN'
import enUS from './en-US'
import { storage } from '@/utils/storage'
import { LOCALE_KEY } from '@/utils/constants'

export type AppLocale = 'zh-CN' | 'en-US'

const stored = storage.getString(LOCALE_KEY, 'zh-CN') as AppLocale

export const i18n = createI18n({
  legacy: false,
  locale: stored || 'zh-CN',
  fallbackLocale: 'en-US',
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS,
  },
})

export function setLocale(locale: AppLocale): void {
  i18n.global.locale.value = locale
  document.documentElement.lang = locale
}

export default i18n
