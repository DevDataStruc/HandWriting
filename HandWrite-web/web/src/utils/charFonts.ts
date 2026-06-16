/**
 * 中文书法字体示例 — 用于"开始书写"左侧示范字符区域
 *
 * 字段说明：
 *  - id      唯一标识
 *  - name    中文名称
 *  - stack   字体栈（与系统字体对齐；缺失时按 fallback 列表兜底）
 *  - tip     风格简介（悬停提示）
 */
export interface CharFont {
  id: string
  name: string
  stack: string
  tip: string
}

const SONG = "'SimSun','NSimSun','Songti SC','STSong','宋体',serif"
const KAI = "'STKaiti','KaiTi','Kaiti SC','楷体',serif"
const FANG = "'FangSong','STFangsong','FangSong_GB2312','仿宋',serif"
const XING = "'STXingkai','华文行楷','Xingkai SC','行楷',cursive"
const WEIBEI = "'STWeibei','华文魏碑','STXinwei','Weibei SC','隶书',serif"
const LI = "'STLiti','华文隶书','LiSu','隶书',serif"
const ZHUAN = "'STKaiti','KaiTi','楷体',serif" // 篆体无系统字体时回落楷体
const YUAN = "'STYuanjian','华文圆体','Yuanti SC','圆体',serif"

export const CHAR_FONTS: CharFont[] = [
  { id: 'kai', name: '楷书', stack: KAI, tip: '端正秀丽、笔画分明 — 通用示范体' },
  { id: 'xing', name: '行书', stack: XING, tip: '行云流水、笔意连贯' },
  { id: 'li', name: '隶书', stack: LI, tip: '蚕头燕尾、波磔分明' },
  { id: 'weibei', name: '魏碑', stack: WEIBEI, tip: '刀刻斧凿、结体方严' },
  { id: 'song', name: '宋体', stack: SONG, tip: '横平竖直、印刷规范' },
  { id: 'fang', name: '仿宋', stack: FANG, tip: '细瘦清雅、书卷气' },
  { id: 'zhuan', name: '篆体', stack: ZHUAN, tip: '古朴圆转、字形渊源' },
  { id: 'yuan', name: '圆体', stack: YUAN, tip: '圆润柔和、辨识度高' },
]

export const CHAR_FONTS_BY_ID: Record<string, CharFont> = CHAR_FONTS.reduce(
  (acc, f) => {
    acc[f.id] = f
    return acc
  },
  {} as Record<string, CharFont>
)

export const DEFAULT_CHAR_FONT_ID = 'kai'
