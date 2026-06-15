import { describe, expect, it } from 'vitest'
import { storage } from '../storage'

describe('storage', () => {
  it('stores and retrieves primitives', () => {
    storage.set('test_key', { a: 1 })
    expect(storage.get<{ a: number }>('test_key')).toEqual({ a: 1 })
    expect(storage.getString('test_key')).toContain('1')
  })

  it('removes keys', () => {
    storage.set('test_remove', 1)
    storage.remove('test_remove')
    expect(storage.get('test_remove')).toBeUndefined()
  })
})
