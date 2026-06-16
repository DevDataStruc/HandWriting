import { describe, expect, it } from 'vitest'
import {
  applyPressure,
  DEFAULT_PRESSURE_SENSITIVITY,
  PRESSURE_CURVES,
  type PressureCurve,
} from '../brushPresets'

describe('pressure pipeline', () => {
  it('clamps inputs to 0..1', () => {
    expect(applyPressure(-1, 100, 'linear')).toBe(0)
    expect(applyPressure(2, 100, 'linear')).toBe(1)
  })

  it('linear curve preserves the input', () => {
    for (const p of [0, 0.25, 0.5, 0.75, 1]) {
      expect(applyPressure(p, 100, 'linear')).toBeCloseTo(p, 5)
    }
  })

  it('soft curve pushes light pressure higher (more responsive)', () => {
    expect(applyPressure(0.25, 100, 'soft')).toBeGreaterThan(applyPressure(0.25, 100, 'linear'))
    expect(applyPressure(0.5, 100, 'soft')).toBeCloseTo(Math.sqrt(0.5), 5)
  })

  it('hard curve squashes light pressure to near zero', () => {
    expect(applyPressure(0.25, 100, 'hard')).toBeLessThan(applyPressure(0.25, 100, 'linear'))
    expect(applyPressure(0.5, 100, 'hard')).toBeCloseTo(0.25, 5)
  })

  it('sensitivity 0 always returns 0', () => {
    expect(applyPressure(0.7, 0, 'linear')).toBe(0)
    expect(applyPressure(0.7, 0, 'soft')).toBe(0)
  })

  it('sensitivity 100 = standard (curve only)', () => {
    expect(applyPressure(0.6, 100, 'linear')).toBeCloseTo(0.6, 5)
  })

  it('sensitivity 200 amplifies output up to 1', () => {
    expect(applyPressure(0.5, 200, 'linear')).toBeCloseTo(1.0, 5)
    // 0.7 * 2 = 1.4 -> clamp to 1
    expect(applyPressure(0.7, 200, 'linear')).toBe(1)
  })

  it('curves endpoint values', () => {
    for (const c of Object.keys(PRESSURE_CURVES) as PressureCurve[]) {
      expect(applyPressure(0, 100, c)).toBe(0)
      expect(applyPressure(1, 100, c)).toBe(1)
    }
  })

  it('default sensitivity is 100', () => {
    expect(DEFAULT_PRESSURE_SENSITIVITY).toBe(100)
  })
})
