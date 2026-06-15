import type { TokenPayload } from '@/api/contracts/auth'
import type { Role } from '@/types/role'

/**
 * JWT 简易工具（仅做解析与失效判断，不做签名校验，服务端为最终权威）
 */
export function decodeToken(token: string): TokenPayload | null {
  if (!token) return null
  try {
    const parts = token.split('.')
    if (parts.length !== 3) return null
    const payload = parts[1]
    const padded = payload.replace(/-/g, '+').replace(/_/g, '/')
    const json = decodeURIComponent(
      atob(padded)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    )
    return JSON.parse(json) as TokenPayload
  } catch (err) {
    console.warn('[auth] decodeToken error', err)
    return null
  }
}

export function isTokenExpired(token: string | null | undefined, skew = 30): boolean {
  if (!token) return true
  const payload = decodeToken(token)
  if (!payload || !payload.exp) return false
  const now = Math.floor(Date.now() / 1000)
  return payload.exp + skew < now
}

export function getRolesFromToken(token: string | null | undefined): Role[] {
  const payload = decodeToken(token || '')
  if (!payload) return []
  return (payload.roles || []) as Role[]
}

export function getUserIdFromToken(token: string | null | undefined): string | null {
  const payload = decodeToken(token || '')
  return payload?.sub ?? null
}
