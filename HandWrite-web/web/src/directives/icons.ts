/**
 * Element Plus 图标全局注册
 * 避免每个组件单独 import 大量图标
 */
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import type { App } from 'vue'

export function setupIcons(app: App): void {
  for (const [name, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(name, component)
  }
}
