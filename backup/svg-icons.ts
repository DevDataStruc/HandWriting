/**
 * admin 页面通用 SVG 图标库
 *
 * 所有图标均采用 24x24 viewBox，stroke 模式（无 fill），可继承 currentColor。
 * 使用方式：import { SvgIcon } 后通过 name 取用，对应模板为：
 *   <SvgIcon name="dashboard" :size="18" />
 */
export type SvgIconName =
  /* 导航 / 侧边栏 */
  | 'dashboard' // 仪表盘（替代 Odometer）
  | 'audit' // 审核（替代 CircleCheck）
  | 'user' // 用户
  | 'users' // 多人（替代 People）
  | 'trend-up' // 趋势上升（替代 TrendCharts）
  | 'document' // 文档（替代 Document）
  | 'clipboard' // 剪贴板
  | 'bell' // 通知
  | 'data-line' // 数据线
  | 'package' // 收纳/包
  | 'calendar' // 日历
  /* 通用动作 */
  | 'search' // 搜索
  | 'edit' // 编辑（铅笔）
  | 'check' // 对号
  | 'close' // 关闭 X
  | 'check-circle' // 圆圈对号
  | 'cross' // 叉号
  | 'ban' // 禁止
  | 'refresh' // 刷新
  /* 方向 / 折叠 */
  | 'caret-up'
  | 'caret-down'
  | 'caret-left'
  | 'caret-right'
  | 'chevron-left'
  | 'chevron-right'
  | 'expand' // 展开
  | 'fold' // 折叠
  /* 主题 */
  | 'sun'
  | 'moon'
  | 'arrow-down' // ▾

/**
 * 内联 SVG path 集合
 * 统一约定：
 *   - viewBox 0 0 24 24
 *   - 描边色 currentColor，stroke-width 默认 1.8
 *   - fill 默认 none，stroke-linecap/linejoin round
 */
const ICONS: Record<SvgIconName, string> = {
  /* ===== 导航类 ===== */
  dashboard:
    '<path d="M12 13.5a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3Z"/><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3h.1a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8v.1a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1Z"/>',

  audit:
    '<path d="M22 11.1V12a10 10 0 1 1-5.9-9.1"/><path d="m9 11 3 3L22 4"/>',

  user:
    '<path d="M20 21a8 8 0 0 0-16 0"/><circle cx="12" cy="7" r="4"/>',

  users:
    '<path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.9"/><path d="M16 3.1A4 4 0 0 1 16 11"/>',

  'trend-up':
    '<polyline points="22 7 13.5 15.5 8.5 10.5 2 17"/><polyline points="16 7 22 7 22 13"/>',

  document:
    '<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8Z"/><polyline points="14 2 14 8 20 8"/><line x1="8" y1="13" x2="16" y2="13"/><line x1="8" y1="17" x2="13" y2="17"/>',

  clipboard:
    '<rect x="8" y="2" width="8" height="4" rx="1" ry="1"/><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/><line x1="9" y1="12" x2="15" y2="12"/><line x1="9" y1="16" x2="13" y2="16"/>',

  bell:
    '<path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9"/><path d="M10.3 21a1.94 1.94 0 0 0 3.4 0"/>',

  'data-line':
    '<path d="M3 3v18h18"/><path d="m19 9-5 5-4-4-3 3"/>',

  package:
    '<path d="M21 8 12 3 3 8v8l9 5 9-5V8Z"/><path d="M3.3 7 12 12l8.7-5"/><path d="M12 22V12"/>',

  calendar:
    '<rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/>',

  /* ===== 通用动作 ===== */
  search:
    '<circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>',

  edit:
    '<path d="M17 3a2.85 2.85 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z"/><line x1="15" y1="5" x2="19" y2="9"/>',

  check:
    '<polyline points="20 6 9 17 4 12"/>',

  close:
    '<line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>',

  'check-circle':
    '<circle cx="12" cy="12" r="10"/><polyline points="9 12 11.5 14.5 16 9.5"/>',

  cross:
    '<circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/>',

  ban:
    '<circle cx="12" cy="12" r="10"/><line x1="5" y1="19" x2="19" y2="5"/>',

  refresh:
    '<path d="M3 12a9 9 0 0 1 15.5-6.3L21 8"/><polyline points="21 3 21 8 16 8"/><path d="M21 12a9 9 0 0 1-15.5 6.3L3 16"/><polyline points="3 21 3 16 8 16"/>',

  /* ===== 方向 ===== */
  'caret-up':
    '<polyline points="6 15 12 9 18 15"/>',

  'caret-down':
    '<polyline points="6 9 12 15 18 9"/>',

  'caret-left':
    '<polyline points="15 18 9 12 15 6"/>',

  'caret-right':
    '<polyline points="9 18 15 12 9 6"/>',

  'chevron-left':
    '<polyline points="15 18 9 12 15 6"/>',

  'chevron-right':
    '<polyline points="9 18 15 12 9 6"/>',

  expand:
    '<polyline points="9 6 3 6 3 12"/><polyline points="15 6 21 6 21 12"/><polyline points="9 18 3 18 3 12"/><polyline points="15 18 21 18 21 12"/>',

  fold:
    '<polyline points="4 14 10 14 10 20"/><polyline points="20 10 14 10 14 4"/><line x1="14" y1="10" x2="21" y2="3"/><line x1="3" y1="21" x2="10" y2="14"/>',

  /* ===== 主题 ===== */
  sun:
    '<circle cx="12" cy="12" r="4"/><line x1="12" y1="2" x2="12" y2="4"/><line x1="12" y1="20" x2="12" y2="22"/><line x1="4.93" y1="4.93" x2="6.34" y2="6.34"/><line x1="17.66" y1="17.66" x2="19.07" y2="19.07"/><line x1="2" y1="12" x2="4" y2="12"/><line x1="20" y1="12" x2="22" y2="12"/><line x1="4.93" y1="19.07" x2="6.34" y2="17.66"/><line x1="17.66" y1="6.34" x2="19.07" y2="4.93"/>',

  moon:
    '<path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8Z"/>',

  'arrow-down':
    '<polyline points="6 9 12 15 18 9"/>',
}

export function getSvgIconPath(name: SvgIconName): string {
  return ICONS[name] ?? ''
}

export const SVG_ICON_NAMES = Object.keys(ICONS) as SvgIconName[]
