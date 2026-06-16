import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { downloadBlob, downloadSvgString, triggerDownload } from '../download'

/**
 * 验证 download 工具不会泄漏 Blob URL：
 *   - 正常路径必须 revoke
 *   - 异常路径（triggerDownload 抛错）也必须 revoke
 */

// jsdom / happy-dom 不一定带 URL.createObjectURL，手动 polyfill
if (typeof URL.createObjectURL !== 'function') {
  let _i = 0
  Object.defineProperty(URL, 'createObjectURL', {
    configurable: true,
    value: () => `blob:mock/${++_i}`,
  })
  Object.defineProperty(URL, 'revokeObjectURL', {
    configurable: true,
    value: () => undefined,
  })
}

describe('download', () => {
  const revoked: string[] = []
  const created: string[] = []

  beforeEach(() => {
    vi.useFakeTimers()
    revoked.length = 0
    created.length = 0
    let i = 0
    vi.spyOn(URL, 'createObjectURL').mockImplementation(() => {
      const u = `blob:test/${++i}`
      created.push(u)
      return u
    })
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation((u) => {
      revoked.push(u as string)
    })
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
  })

  it('downloadBlob 正常路径会 revoke URL', async () => {
    const blob = new Blob(['x'], { type: 'image/png' })
    await downloadBlob(blob, 'a.png')
    // 内部用 setTimeout(_, 1000) 延迟释放，需要把时间拨过去
    vi.advanceTimersByTime(1500)
    expect(created.length).toBe(1)
    expect(revoked).toContain(created[0])
  })

  it('downloadSvgString 正常路径会 revoke URL', () => {
    downloadSvgString('<svg/>', 'a.svg')
    vi.advanceTimersByTime(1500)
    expect(created.length).toBe(1)
    expect(revoked).toContain(created[0])
  })

  it('downloadSvgString 在 triggerDownload 抛错时仍能 revoke URL（防内存泄漏）', () => {
    // 替换 a.click 让其抛错，模拟下载被拦截 / DOM 异常
    const clickSpy = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {
      throw new Error('simulated download failure')
    })

    try {
      // try/finally 应把异常吞掉，调用方不应感知
      downloadSvgString('<svg/>', 'a.svg')
    } catch {
      /* 如果 finally 没兜住，catch 兜 */
    } finally {
      clickSpy.mockRestore()
    }

    vi.advanceTimersByTime(1500)
    expect(created.length).toBe(1)
    // 关键断言：即使 triggerDownload 抛错，URL 也必须进入 revoke 队列
    expect(revoked).toContain(created[0])
  })

  it('triggerDownload 在没有 document.body 时抛错', () => {
    const orig = document.body
    Object.defineProperty(document, 'body', {
      configurable: true,
      get: () => null,
    })
    try {
      expect(() => triggerDownload('blob:test/x', 'x.txt')).toThrow()
    } finally {
      Object.defineProperty(document, 'body', { configurable: true, value: orig })
    }
  })
})
