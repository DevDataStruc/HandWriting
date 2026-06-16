import { describe, expect, it } from 'vitest'
import {
  applySkin,
  DEFAULT_SKIN_ID,
  SKIN_PRESETS,
  SKIN_PRESETS_BY_ID,
  type SkinId,
} from '../skinPresets'

describe('skinPresets', () => {
  it('provides at least 5 themes', () => {
    expect(SKIN_PRESETS.length).toBeGreaterThanOrEqual(5)
  })

  it('every preset has a unique id and a name', () => {
    const ids = SKIN_PRESETS.map((s) => s.id)
    expect(new Set(ids).size).toBe(ids.length)
    for (const s of SKIN_PRESETS) {
      expect(s.name).toBeTruthy()
      expect(s.nameEn).toBeTruthy()
      expect(s.primary).toMatch(/^#[0-9a-f]{6}$/i)
    }
  })

  it('covers at least 5 distinct categories of style', () => {
    const ids = new Set(SKIN_PRESETS.map((s) => s.id as SkinId))
    expect(ids.has('default')).toBe(true)
    // 至少还要有 4 套非默认主题
    expect(ids.size).toBeGreaterThanOrEqual(5)
  })

  it('applySkin writes CSS variables onto a DOM element', () => {
    const fake = {
      style: { setProperty: (() => undefined) as (k: string, v: string) => void },
    } as unknown as HTMLElement
    const captured: Record<string, string> = {}
    const realEl = fake as HTMLElement
    realEl.style.setProperty = (k: string, v: string) => {
      captured[k] = v
    }
    const ink = SKIN_PRESETS_BY_ID['ink']
    applySkin(realEl, ink)
    expect(captured['--skin-primary']).toBe(ink.primary)
    expect(captured['--skin-canvas-bg']).toBe(ink.canvasBg)
    expect(captured['--skin-grid-border']).toBe(ink.gridBorder)
  })

  it('default skin id points to a valid preset', () => {
    expect(SKIN_PRESETS_BY_ID[DEFAULT_SKIN_ID]).toBeDefined()
  })
})
