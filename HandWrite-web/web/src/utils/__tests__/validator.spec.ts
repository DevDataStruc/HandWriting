import { describe, expect, it } from 'vitest'
import { isEmail, isPassword, isPhone, isUsername } from '../validator'

describe('validators', () => {
  it('isUsername validates length and chars', () => {
    expect(isUsername('abc')).toBe(true)
    expect(isUsername('a_b-c')).toBe(false)
    expect(isUsername('a'.repeat(30))).toBe(false)
  })
  it('isPassword enforces length', () => {
    expect(isPassword('123456')).toBe(true)
    expect(isPassword('123')).toBe(false)
  })
  it('isEmail covers common cases', () => {
    expect(isEmail('a@b.com')).toBe(true)
    expect(isEmail('not-an-email')).toBe(false)
  })
  it('isPhone requires CN mobile prefix', () => {
    expect(isPhone('13800000000')).toBe(true)
    expect(isPhone('12800000000')).toBe(false)
  })
})
