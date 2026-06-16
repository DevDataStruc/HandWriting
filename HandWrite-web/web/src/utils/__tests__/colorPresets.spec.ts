import { describe, expect, it } from 'vitest'
import {
  COLOR_PALETTE,
  mixHex,
  resolveDynamicColor,
  toDynamicColor,
  type DynamicColor,
} from '../colorPresets'

describe('colorPresets', () => {
  it('palette has at least 24 colors', () => {
    expect(COLOR_PALETTE.length).toBeGreaterThanOrEqual(24)
  })

  it('palette entries are unique and valid hex', () => {
    const seen = new Set<string>()
    for (const c of COLOR_PALETTE) {
      expect(c).toMatch(/^#[0-9a-f]{3}([0-9a-f]{3})?$/i)
      expect(seen.has(c)).toBe(false)
      seen.add(c)
    }
  })

  it('mixHex is bounded and monotone', () => {
    expect(mixHex('#000000', '#ffffff', 0)).toBe('#000000')
    expect(mixHex('#000000', '#ffffff', 1)).toBe('#ffffff')
    expect(mixHex('#000000', '#ffffff', 0.5)).toBe('#808080')
    // clamp
    expect(mixHex('#ff0000', '#00ff00', 2)).toBe('#00ff00')
    expect(mixHex('#ff0000', '#00ff00', -1)).toBe('#ff0000')
  })

  it('solid mode returns primary', () => {
    const dyn: DynamicColor = { mode: 'solid', primary: '#abcdef' }
    expect(resolveDynamicColor(dyn, 0)).toBe('#abcdef')
    expect(resolveDynamicColor(dyn, 9999)).toBe('#abcdef')
  })

  it('gradient mode returns primary (沿笔画方向由 primary 决定)', () => {
    const dyn: DynamicColor = { mode: 'gradient', primary: '#112233', secondary: '#aabbcc' }
    expect(resolveDynamicColor(dyn, 0)).toBe('#112233')
  })

  it('breathing mode oscillates between primary and secondary', () => {
    const dyn: DynamicColor = {
      mode: 'breathing',
      primary: '#000000',
      secondary: '#ffffff',
      durationMs: 1000,
    }
    // 周期内至少能取到不同色值
    const samples = new Set<string>()
    for (let t = 0; t < 1000; t += 50) samples.add(resolveDynamicColor(dyn, t))
    expect(samples.size).toBeGreaterThan(2)
    // 端点 = primary
    expect(resolveDynamicColor(dyn, 0)).toBe('#000000')
    // 半个周期 = secondary
    expect(resolveDynamicColor(dyn, 500)).toBe('#ffffff')
  })

  it('toDynamicColor accepts a plain hex string', () => {
    expect(toDynamicColor('#123456')).toEqual({ mode: 'solid', primary: '#123456' })
  })

  it('toDynamicColor returns input as-is when it is already a DynamicColor', () => {
    const dyn: DynamicColor = { mode: 'breathing', primary: '#aaa', secondary: '#bbb' }
    expect(toDynamicColor(dyn)).toBe(dyn)
  })
})
