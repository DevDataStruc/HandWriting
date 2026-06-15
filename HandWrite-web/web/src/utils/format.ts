import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import relativeTime from 'dayjs/plugin/relativeTime'
import duration from 'dayjs/plugin/duration'

dayjs.extend(relativeTime)
dayjs.extend(duration)
dayjs.locale('zh-cn')

/** 标准化日期显示 */
export function formatDate(value: string | number | Date | null | undefined, pattern = 'YYYY-MM-DD HH:mm:ss'): string {
  if (!value) return '-'
  const d = dayjs(value)
  return d.isValid() ? d.format(pattern) : '-'
}

export function formatDateOnly(value: string | number | Date | null | undefined): string {
  return formatDate(value, 'YYYY-MM-DD')
}

export function formatTimeOnly(value: string | number | Date | null | undefined): string {
  return formatDate(value, 'HH:mm:ss')
}

/** 相对时间 */
export function fromNow(value: string | number | Date | null | undefined): string {
  if (!value) return '-'
  const d = dayjs(value)
  return d.isValid() ? d.fromNow() : '-'
}

/** 毫秒 -> mm:ss */
export function formatDuration(ms: number | undefined | null): string {
  if (ms == null || isNaN(ms)) return '00:00'
  const d = dayjs.duration(ms)
  const m = Math.floor(d.asMinutes()).toString().padStart(2, '0')
  const s = Math.floor(d.seconds()).toString().padStart(2, '0')
  return `${m}:${s}`
}

/** 数字千分位 */
export function formatNumber(value: number | string | null | undefined, fractionDigits = 0): string {
  if (value == null) return '-'
  const n = Number(value)
  if (isNaN(n)) return '-'
  return n.toLocaleString('zh-CN', { maximumFractionDigits: fractionDigits, minimumFractionDigits: fractionDigits })
}

/** 文件大小 */
export function formatFileSize(bytes: number | null | undefined): string {
  if (bytes == null || isNaN(bytes)) return '-'
  if (bytes < 1024) return `${bytes} B`
  const units = ['KB', 'MB', 'GB', 'TB']
  let i = -1
  let n = bytes
  do {
    n /= 1024
    i++
  } while (n >= 1024 && i < units.length - 1)
  return `${n.toFixed(2)} ${units[i]}`
}

/** 脱敏（手机号/邮箱） */
export function maskPhone(value: string | null | undefined): string {
  if (!value) return '-'
  return value.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
}

export function maskEmail(value: string | null | undefined): string {
  if (!value || !value.includes('@')) return '-'
  const [name, domain] = value.split('@')
  if (name.length <= 2) return `${name[0]}***@${domain}`
  return `${name.slice(0, 2)}***@${domain}`
}

export { dayjs }
