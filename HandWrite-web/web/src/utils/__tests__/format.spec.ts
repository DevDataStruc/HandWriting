import { describe, expect, it } from 'vitest'
import { formatDate, formatFileSize, formatNumber, maskEmail, maskPhone } from '../format'

describe('format utilities', () => {
  it('formatNumber handles null and large numbers', () => {
    expect(formatNumber(null)).toBe('-')
    expect(formatNumber(1234567)).toContain('1')
    expect(formatNumber(1234.5, 1)).toMatch(/1,234\.5/)
  })

  it('formatDate returns - for empty input', () => {
    expect(formatDate(null)).toBe('-')
    expect(formatDate('2026-01-01')).toBe('2026-01-01 00:00:00')
  })

  it('formatFileSize returns correct unit', () => {
    expect(formatFileSize(0)).toBe('0 B')
    expect(formatFileSize(2048)).toMatch(/KB/)
    expect(formatFileSize(5 * 1024 * 1024)).toMatch(/MB/)
  })

  it('maskPhone and maskEmail hide middle characters', () => {
    expect(maskPhone('13800001234')).toBe('138****1234')
    expect(maskEmail('alice@example.com')).toBe('al***@example.com')
  })
})
