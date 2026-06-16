/**
 * 国画 24 套传统矿物颜料色
 *  按 6×4 分组：朱红系 / 藤黄系 / 绿青系 / 花青蓝 / 墨色系 / 矿物白褐
 */
export interface InkColor {
  /** 唯一 id */
  id: string
  /** 颜料名（古称） */
  name: string
  /** 短标识（一字） */
  short: string
  /** 主色（HEX） */
  hex: string
  /** 色调系（用于分组） */
  family: 'red' | 'yellow' | 'green' | 'blue' | 'ink' | 'earth'
}

export const INK_PALETTE: InkColor[] = [
  // —— 朱红系 ——
  { id: 'cinnabar', name: '朱砂', short: '朱', hex: '#A83C32', family: 'red' },
  { id: 'rouge', name: '胭脂', short: '胭', hex: '#8E2A3B', family: 'red' },
  { id: 'dawnred', name: '曙红', short: '曙', hex: '#C03A4D', family: 'red' },
  { id: 'vermilion', name: '朱磦', short: '磦', hex: '#D75B47', family: 'red' },
  // —— 藤黄系 ——
  { id: 'rattan', name: '藤黄', short: '藤', hex: '#D4B16A', family: 'yellow' },
  { id: 'stone-yellow', name: '石黄', short: '石', hex: '#C99A3F', family: 'yellow' },
  { id: 'orpiment', name: '雄黄', short: '雄', hex: '#B58737', family: 'yellow' },
  { id: 'earth-yellow', name: '土黄', short: '土', hex: '#9A7B3E', family: 'yellow' },
  // —— 绿青系 ——
  { id: 'malachite', name: '石绿', short: '绿', hex: '#7A9A6E', family: 'green' },
  { id: 'jade-green', name: '三绿', short: '三', hex: '#9CB391', family: 'green' },
  { id: 'bamboo', name: '竹青', short: '竹', hex: '#6B8E7F', family: 'green' },
  { id: 'moss', name: '苍绿', short: '苍', hex: '#5C7A5A', family: 'green' },
  // —— 花青蓝 ——
  { id: 'indigo', name: '花青', short: '花', hex: '#4A6F70', family: 'blue' },
  { id: 'azure', name: '头青', short: '头', hex: '#3F5E84', family: 'blue' },
  { id: 'cobalt', name: '二青', short: '二', hex: '#5878A0', family: 'blue' },
  { id: 'azurite', name: '石青', short: '青', hex: '#2D4D72', family: 'blue' },
  // —— 墨色系 ——
  { id: 'pine-ink', name: '松烟', short: '烟', hex: '#2D2A26', family: 'ink' },
  { id: 'oil-ink', name: '油烟', short: '油', hex: '#1F1B17', family: 'ink' },
  { id: 'light-ink', name: '淡墨', short: '淡', hex: '#6E665C', family: 'ink' },
  { id: 'ochre', name: '赭石', short: '赭', hex: '#A65E44', family: 'ink' },
  // —— 矿物白褐 ——
  { id: 'titanium', name: '钛白', short: '钛', hex: '#F4ECDB', family: 'earth' },
  { id: 'lotus', name: '藕褐', short: '藕', hex: '#B0937A', family: 'earth' },
  { id: 'sandalwood', name: '檀褐', short: '檀', hex: '#7B5A47', family: 'earth' },
  { id: 'sumi-paper', name: '宣纸', short: '宣', hex: '#F5F0E6', family: 'earth' },
]

export const INK_PALETTE_BY_ID: Record<string, InkColor> = INK_PALETTE.reduce(
  (acc, c) => {
    acc[c.id] = c
    return acc
  },
  {} as Record<string, InkColor>
)

/** 根据 hex 反查最近的颜料（无则返回宣纸） */
export function closestInk(hex: string): InkColor {
  const target = hex.toLowerCase()
  for (const c of INK_PALETTE) {
    if (c.hex.toLowerCase() === target) return c
  }
  return INK_PALETTE_BY_ID['sumi-paper']
}

/* ============================================================
 *  SVG 装饰纹样（极简水墨竹叶 / 远山 / 梅枝 / 印章朱砂）
 *  以 data-uri 形式导出，方便在 CSS 中作为 background-image 引用。
 * ========================================================== */

function svgToDataUri(svg: string): string {
  const compact = svg
    .replace(/\s{2,}/g, ' ')
    .replace(/>\s+</g, '><')
    .trim()
  return `data:image/svg+xml;utf8,${encodeURIComponent(compact)}`
}

/** 极简水墨竹叶（用于控件选中态/装饰） */
export const BAMBOO_LEAF_SVG = svgToDataUri(
  `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none">
    <path d="M12 2 C 6 8, 6 16, 12 22" stroke="#5A554E" stroke-width="1.2" stroke-linecap="round"/>
    <path d="M12 7 C 16 8, 18 9, 20 7" stroke="#5A554E" stroke-width="1.2" stroke-linecap="round"/>
    <path d="M12 12 C 8 13, 6 14, 4 12" stroke="#5A554E" stroke-width="1.2" stroke-linecap="round"/>
    <path d="M12 17 C 16 18, 18 19, 20 17" stroke="#5A554E" stroke-width="1.2" stroke-linecap="round"/>
  </svg>`
)

/** 远山轮廓（用于状态栏装饰） */
export const DISTANT_MOUNTAIN_SVG = svgToDataUri(
  `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 120 16" fill="none">
    <path d="M0 14 L 18 4 L 30 10 L 44 2 L 58 12 L 74 6 L 92 12 L 110 4 L 120 14 Z" fill="#A09788" fill-opacity="0.35"/>
    <path d="M0 14 L 20 8 L 36 12 L 52 6 L 70 12 L 88 8 L 108 12 L 120 10" stroke="#7B6E5F" stroke-width="0.6" stroke-opacity="0.6" fill="none"/>
  </svg>`
)

/** 细枝梅枝（用作色区分隔线） */
export const PLUM_BRANCH_SVG = svgToDataUri(
  `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 80 12" fill="none">
    <path d="M0 6 C 20 4, 40 8, 80 5" stroke="#7B6E5F" stroke-width="0.8" stroke-opacity="0.7"/>
    <circle cx="22" cy="5.5" r="1" fill="#A83C32" fill-opacity="0.75"/>
    <circle cx="48" cy="6.5" r="1" fill="#A83C32" fill-opacity="0.75"/>
    <circle cx="68" cy="5.2" r="1" fill="#A83C32" fill-opacity="0.75"/>
  </svg>`
)

/** 宣纸肌理（淡暗纹 + 极淡竹纹） */
export const RICE_PAPER_TEXTURE_SVG = svgToDataUri(
  `<svg xmlns="http://www.w3.org/2000/svg" width="240" height="240" viewBox="0 0 240 240">
    <defs>
      <filter id="noise" x="0" y="0" width="100%" height="100%">
        <feTurbulence type="fractalNoise" baseFrequency="0.85" numOctaves="2" seed="3" stitchTiles="stitch"/>
        <feColorMatrix values="0 0 0 0 0.18  0 0 0 0 0.16  0 0 0 0 0.13  0 0 0 0.06 0"/>
      </filter>
      <pattern id="bamboo" x="0" y="0" width="60" height="60" patternUnits="userSpaceOnUse">
        <line x1="0" y1="0" x2="60" y2="60" stroke="#C8C0B0" stroke-width="0.4" stroke-opacity="0.18"/>
        <line x1="60" y1="0" x2="0" y2="60" stroke="#C8C0B0" stroke-width="0.4" stroke-opacity="0.18"/>
      </pattern>
    </defs>
    <rect width="240" height="240" fill="url(#bamboo)"/>
    <rect width="240" height="240" filter="url(#noise)"/>
  </svg>`
)

/** 画稿格线笺纸肌理（用于画布背景） */
export const GRID_PAPER_TEXTURE_SVG = svgToDataUri(
  `<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100" viewBox="0 0 100 100">
    <line x1="0" y1="25" x2="100" y2="25" stroke="#E0D8C8" stroke-width="0.5"/>
    <line x1="0" y1="50" x2="100" y2="50" stroke="#E0D8C8" stroke-width="0.5"/>
    <line x1="0" y1="75" x2="100" y2="75" stroke="#E0D8C8" stroke-width="0.5"/>
    <line x1="25" y1="0" x2="25" y2="100" stroke="#E0D8C8" stroke-width="0.5"/>
    <line x1="50" y1="0" x2="50" y2="100" stroke="#E0D8C8" stroke-width="0.5"/>
    <line x1="75" y1="0" x2="75" y2="100" stroke="#E0D8C8" stroke-width="0.5"/>
    <rect x="25" y="25" width="50" height="50" fill="none" stroke="#A09788" stroke-width="0.6" stroke-opacity="0.55"/>
  </svg>`
)
