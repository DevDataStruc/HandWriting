import { describe, expect, it } from 'vitest'
import { BAMBOO_LEAF_SVG, INK_PALETTE, INK_PALETTE_BY_ID, closestInk } from '../inkPalette'

describe('inkPalette', () => {
  it('exposes exactly 24 traditional mineral pigments', () => {
    expect(INK_PALETTE).toHaveLength(24)
  })

  it('all colors have unique ids, valid hex, and family', () => {
    const ids = new Set<string>()
    for (const c of INK_PALETTE) {
      expect(c.id).toBeTruthy()
      expect(ids.has(c.id)).toBe(false)
      ids.add(c.id)
      expect(c.name).toBeTruthy()
      expect(c.short).toBeTruthy()
      expect(c.short.length).toBeLessThanOrEqual(2)
      expect(c.hex).toMatch(/^#[0-9a-f]{6}$/i)
      expect(['red', 'yellow', 'green', 'blue', 'ink', 'earth']).toContain(c.family)
    }
  })

  it('covers all 6 families (朱红/藤黄/绿青/花青/墨/矿物)', () => {
    const families = new Set(INK_PALETTE.map((c) => c.family))
    expect(families.size).toBe(6)
  })

  it('contains the six canonical mineral pigments from spec', () => {
    const names = INK_PALETTE.map((c) => c.name)
    expect(names).toEqual(expect.arrayContaining(['朱砂', '胭脂', '藤黄', '花青', '竹青', '赭石']))
  })

  it('primary color tokens match spec colors', () => {
    expect(INK_PALETTE_BY_ID['cinnabar'].hex.toLowerCase()).toBe('#a83c32')
    expect(INK_PALETTE_BY_ID['rattan'].hex.toLowerCase()).toBe('#d4b16a')
    expect(INK_PALETTE_BY_ID['bamboo'].hex.toLowerCase()).toBe('#6b8e7f')
    expect(INK_PALETTE_BY_ID['indigo'].hex.toLowerCase()).toBe('#4a6f70')
    expect(INK_PALETTE_BY_ID['pine-ink'].hex.toLowerCase()).toBe('#2d2a26')
    expect(INK_PALETTE_BY_ID['ochre'].hex.toLowerCase()).toBe('#a65e44')
  })

  it('closestInk returns exact match for known hex', () => {
    expect(closestInk('#A83C32').id).toBe('cinnabar')
    expect(closestInk('#2D2A26').id).toBe('pine-ink')
  })

  it('closestInk falls back to 宣纸 for unknown hex', () => {
    expect(closestInk('#FF00FF').id).toBe('sumi-paper')
  })

  it('bamboo leaf SVG is a data URI', () => {
    expect(BAMBOO_LEAF_SVG).toMatch(/^data:image\/svg\+xml;utf8,/)
    expect(BAMBOO_LEAF_SVG).toContain('svg')
  })
})
