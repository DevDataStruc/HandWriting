import { describe, expect, it } from 'vitest'
import { decodeToken, getRolesFromToken, isTokenExpired } from '../auth'

function makeToken(payload: Record<string, unknown>): string {
  const header = btoa(JSON.stringify({ alg: 'none', typ: 'JWT' }))
  const body = btoa(JSON.stringify(payload))
  return `${header}.${body}.signature`
}

describe('auth utils', () => {
  it('decodes a valid token payload', () => {
    const t = makeToken({ sub: '1', username: 'a', roles: ['USER'] })
    const p = decodeToken(t)
    expect(p?.sub).toBe('1')
    expect(p?.username).toBe('a')
    expect(p?.roles).toEqual(['USER'])
  })

  it('detects expired tokens', () => {
    const past = makeToken({ sub: '1', exp: Math.floor(Date.now() / 1000) - 100 })
    expect(isTokenExpired(past)).toBe(true)
  })

  it('returns roles from token', () => {
    const t = makeToken({ roles: ['ADMIN'] })
    expect(getRolesFromToken(t)).toEqual(['ADMIN'])
  })
})
