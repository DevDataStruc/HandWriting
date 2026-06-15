/**
 * localStorage 简单包装，支持 JSON 自动序列化与反序列化
 */
const PREFIX = 'hw:'

function fullKey(key: string): string {
  return `${PREFIX}${key}`
}

export const storage = {
  get<T = unknown>(key: string, fallback?: T): T | undefined {
    try {
      const raw = localStorage.getItem(fullKey(key))
      if (raw === null) return fallback
      return JSON.parse(raw) as T
    } catch (err) {
      console.warn('[storage] get error', err)
      return fallback
    }
  },

  getString(key: string, fallback = ''): string {
    try {
      return localStorage.getItem(fullKey(key)) ?? fallback
    } catch {
      return fallback
    }
  },

  set<T>(key: string, value: T): void {
    try {
      localStorage.setItem(fullKey(key), JSON.stringify(value))
    } catch (err) {
      console.warn('[storage] set error', err)
    }
  },

  setString(key: string, value: string): void {
    try {
      localStorage.setItem(fullKey(key), value)
    } catch (err) {
      console.warn('[storage] setString error', err)
    }
  },

  remove(key: string): void {
    try {
      localStorage.removeItem(fullKey(key))
    } catch (err) {
      console.warn('[storage] remove error', err)
    }
  },

  clear(): void {
    try {
      const keys: string[] = []
      for (let i = 0; i < localStorage.length; i++) {
        const k = localStorage.key(i)
        if (k && k.startsWith(PREFIX)) keys.push(k)
      }
      keys.forEach((k) => localStorage.removeItem(k))
    } catch (err) {
      console.warn('[storage] clear error', err)
    }
  },
}

export const sessionStore = {
  get<T = unknown>(key: string, fallback?: T): T | undefined {
    try {
      const raw = sessionStorage.getItem(fullKey(key))
      if (raw === null) return fallback
      return JSON.parse(raw) as T
    } catch {
      return fallback
    }
  },
  set<T>(key: string, value: T): void {
    try {
      sessionStorage.setItem(fullKey(key), JSON.stringify(value))
    } catch (err) {
      console.warn('[sessionStore] set error', err)
    }
  },
  remove(key: string): void {
    try {
      sessionStorage.removeItem(fullKey(key))
    } catch (err) {
      console.warn('[sessionStore] remove error', err)
    }
  },
}
