/**
 * 手写板皮肤主题 — 5 套（简约 / 墨韵 / 极光 / 暮色 / 森林）
 *
 * 实现思路：CSS 变量驱动。每个主题提供一组 token，挂载到容器上后，
 * 子组件通过 var(--skin-xxx) 即可拿到对应颜色。不依赖额外样式表预加载。
 */
export type SkinId = 'default' | 'ink' | 'aurora' | 'sunset' | 'forest'

export interface SkinPreset {
  id: SkinId
  /** 展示名（zh-CN） */
  name: string
  /** 英文名 */
  nameEn: string
  /** 主题主色 */
  primary: string
  /** 主色 - 浅 */
  primaryLight: string
  /** 主色 - 浅透（hover/选中态背景） */
  primarySofter: string
  /** 主色 - 深（按压/文字） */
  primaryDark: string
  /** 画布背景 */
  canvasBg: string
  /** 田字格虚线 */
  gridLine: string
  /** 田字格外框 */
  gridBorder: string
  /** 画布阴影基色（带 alpha） */
  canvasShadow: string
  /** 工具栏背景 */
  toolbarBg: string
  /** 提示气泡背景 */
  hintBg: string
}

export const SKIN_PRESETS: SkinPreset[] = [
  {
    id: 'default',
    name: '简约默认',
    nameEn: 'Default',
    primary: '#0D9488',
    primaryLight: '#14B8A6',
    primarySofter: '#5EEAD4',
    primaryDark: '#134E4A',
    canvasBg: '#FFFFFF',
    gridLine: '#E0F2F1',
    gridBorder: '#5EEAD4',
    canvasShadow: 'rgba(13, 78, 74, 0.10)',
    toolbarBg: '#FFFFFF',
    hintBg: 'rgba(13, 78, 74, 0.6)',
  },
  {
    id: 'ink',
    name: '墨韵艺术',
    nameEn: 'Ink',
    primary: '#1F2937',
    primaryLight: '#374151',
    primarySofter: '#9CA3AF',
    primaryDark: '#0B1220',
    canvasBg: '#FAF7F2',
    gridLine: '#E7E2D8',
    gridBorder: '#B7A78E',
    canvasShadow: 'rgba(31, 41, 55, 0.18)',
    toolbarBg: '#F3EFE6',
    hintBg: 'rgba(31, 41, 55, 0.7)',
  },
  {
    id: 'aurora',
    name: '极光科技',
    nameEn: 'Aurora',
    primary: '#6366F1',
    primaryLight: '#818CF8',
    primarySofter: '#C7D2FE',
    primaryDark: '#312E81',
    canvasBg: '#0F172A',
    gridLine: 'rgba(129, 140, 248, 0.20)',
    gridBorder: 'rgba(129, 140, 248, 0.65)',
    canvasShadow: 'rgba(99, 102, 241, 0.35)',
    toolbarBg: 'rgba(15, 23, 42, 0.85)',
    hintBg: 'rgba(99, 102, 241, 0.75)',
  },
  {
    id: 'sunset',
    name: '暮色温暖',
    nameEn: 'Sunset',
    primary: '#F97316',
    primaryLight: '#FB923C',
    primarySofter: '#FED7AA',
    primaryDark: '#9A3412',
    canvasBg: '#FFFBF5',
    gridLine: '#FFE4C7',
    gridBorder: '#FB923C',
    canvasShadow: 'rgba(249, 115, 22, 0.20)',
    toolbarBg: '#FFF7ED',
    hintBg: 'rgba(249, 115, 22, 0.75)',
  },
  {
    id: 'forest',
    name: '森林清新',
    nameEn: 'Forest',
    primary: '#16A34A',
    primaryLight: '#4ADE80',
    primarySofter: '#BBF7D0',
    primaryDark: '#14532D',
    canvasBg: '#F0FDF4',
    gridLine: '#DCFCE7',
    gridBorder: '#4ADE80',
    canvasShadow: 'rgba(22, 163, 74, 0.18)',
    toolbarBg: '#FFFFFF',
    hintBg: 'rgba(22, 163, 74, 0.75)',
  },
]

export const SKIN_PRESETS_BY_ID: Record<string, SkinPreset> = SKIN_PRESETS.reduce(
  (acc, s) => {
    acc[s.id] = s
    return acc
  },
  {} as Record<string, SkinPreset>
)

export const DEFAULT_SKIN_ID: SkinId = 'default'

/** 把主题应用到某个 DOM 根元素（在 :root 或指定容器上写 CSS 变量） */
export function applySkin(elm: HTMLElement, skin: SkinPreset): void {
  const style = elm.style
  style.setProperty('--skin-primary', skin.primary)
  style.setProperty('--skin-primary-light', skin.primaryLight)
  style.setProperty('--skin-primary-softer', skin.primarySofter)
  style.setProperty('--skin-primary-dark', skin.primaryDark)
  style.setProperty('--skin-canvas-bg', skin.canvasBg)
  style.setProperty('--skin-grid-line', skin.gridLine)
  style.setProperty('--skin-grid-border', skin.gridBorder)
  style.setProperty('--skin-canvas-shadow', skin.canvasShadow)
  style.setProperty('--skin-toolbar-bg', skin.toolbarBg)
  style.setProperty('--skin-hint-bg', skin.hintBg)
}
