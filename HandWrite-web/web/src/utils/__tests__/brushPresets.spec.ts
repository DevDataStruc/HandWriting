import { describe, expect, it } from 'vitest'
import {
  ANGLE_MAX,
  ANGLE_MIN,
  BRUSH_PRESETS,
  BRUSH_PRESETS_BY_ID,
  DEFAULT_BRUSH_ID,
  SIZE_MAX,
  SIZE_MIN,
  type BrushCategory,
} from '../brushPresets'

describe('brushPresets', () => {
  it('exposes at least 4 willow-leaf variants', () => {
    const willow = BRUSH_PRESETS.filter((b) => b.category === 'willow')
    expect(willow.length).toBeGreaterThanOrEqual(4)
  })

  it('every preset has a unique id and a name', () => {
    const ids = BRUSH_PRESETS.map((b) => b.id)
    expect(new Set(ids).size).toBe(ids.length)
    for (const b of BRUSH_PRESETS) {
      expect(b.name).toBeTruthy()
      expect(b.nameEn).toBeTruthy()
    }
  })

  it('all preset fields are within valid ranges', () => {
    for (const b of BRUSH_PRESETS) {
      expect(b.size).toBeGreaterThanOrEqual(SIZE_MIN)
      expect(b.size).toBeLessThanOrEqual(SIZE_MAX)
      expect(b.thinning).toBeGreaterThanOrEqual(0)
      expect(b.thinning).toBeLessThanOrEqual(1)
      expect(b.smoothing).toBeGreaterThanOrEqual(0)
      expect(b.smoothing).toBeLessThanOrEqual(1)
      expect(b.streamline).toBeGreaterThanOrEqual(0)
      expect(b.streamline).toBeLessThanOrEqual(1)
      expect(b.taperStart).toBeGreaterThanOrEqual(0)
      expect(b.taperStart).toBeLessThanOrEqual(1)
      expect(b.taperEnd).toBeGreaterThanOrEqual(0)
      expect(b.taperEnd).toBeLessThanOrEqual(1)
      expect(['pen', 'brush', 'willow']).toContain(b.category)
    }
  })

  it('willow presets have tapered ends (柳叶特征)', () => {
    const willow = BRUSH_PRESETS.filter((b) => b.category === 'willow')
    for (const b of willow) {
      expect(b.taperStart + b.taperEnd).toBeGreaterThan(0.2)
      expect(b.thinning).toBeGreaterThanOrEqual(0.4)
    }
  })

  it('pen presets have zero taper (钢笔特征)', () => {
    const pens = BRUSH_PRESETS.filter((b) => b.category === 'pen')
    for (const b of pens) {
      expect(b.taperStart).toBe(0)
      expect(b.taperEnd).toBe(0)
    }
  })

  it('BRUSH_PRESETS_BY_ID lookup is consistent', () => {
    for (const b of BRUSH_PRESETS) {
      expect(BRUSH_PRESETS_BY_ID[b.id]).toEqual(b)
    }
    expect(BRUSH_PRESETS_BY_ID['__nope__']).toBeUndefined()
  })

  it('default brush id points to a willow preset', () => {
    const def = BRUSH_PRESETS_BY_ID[DEFAULT_BRUSH_ID]
    expect(def.category as BrushCategory).toBe('willow')
  })

  it('angle limits are 0..180', () => {
    expect(ANGLE_MIN).toBe(0)
    expect(ANGLE_MAX).toBe(180)
  })
})
