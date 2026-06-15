/**
 * 全局类型声明
 */
export {}

declare global {
  interface Window {
    __APP_ENV__: 'development' | 'production' | 'staging'
  }
}
