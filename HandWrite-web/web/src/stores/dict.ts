import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as dictApi from '@/api/dict'
import type { CharDict, DictPageQuery } from '@/api/contracts/sample'
import type { PageResult } from '@/api/contracts/common'

/**
 * 字典 store
 * - 后端字段 charValue 镜像为 char（前端统一用 char 显示）
 * - fetchRandom / fetchById 属于前端语义包装：
 *   API.md 未定义 /dict/random、/dict/{id}，
 *   这里通过 listByCategory + 前端过滤/随机实现。
 */
function normalizeChar(c: CharDict): CharDict {
  return { ...c, char: c.char ?? c.charValue }
}

function pickRandom<T>(arr: T[]): T | null {
  if (arr.length === 0) return null
  return arr[Math.floor(Math.random() * arr.length)]
}

export const useDictStore = defineStore('dict', () => {
  const chars = ref<CharDict[]>([])
  const total = ref(0)
  const currentChar = ref<CharDict | null>(null)
  const categories = ref<string[]>([])
  const loading = ref(false)

  /** GET /v1/dict/chars（分页） */
  async function fetchPageChars(params?: DictPageQuery): Promise<PageResult<CharDict>> {
    loading.value = true
    try {
      const res = await dictApi.pageChars(params)
      chars.value = res.records.map(normalizeChar)
      total.value = res.total
      return res
    } finally {
      loading.value = false
    }
  }

  /** GET /v1/dict/chars/list（按分类） */
  async function listByCategory(category?: string): Promise<CharDict[]> {
    const list = await dictApi.listChars({ category })
    return list.map(normalizeChar)
  }

  /**
   * 取一个随机字符（前端包装：拉一页再随机）
   * 优先按传入的 category 过滤；为空则取全部
   */
  async function fetchRandom(category?: string): Promise<CharDict | null> {
    const list = await listByCategory(category)
    const c = pickRandom(list)
    if (c) setCurrent(c)
    return c
  }

  /**
   * 按 id 查找字符（前端包装：先按 id 精确匹配）
   * 实际后端可补 /v1/dict/chars/{id}，这里以分页 + 前端过滤做兜底
   */
  async function fetchById(id: number | string): Promise<CharDict | null> {
    const num = Number(id)
    // 尝试在已加载的列表中找
    const cached = chars.value.find((c) => Number(c.id) === num)
    if (cached) {
      setCurrent(cached)
      return cached
    }
    // 退而求其次：拉一页全量 + 过滤
    const all = await listByCategory(undefined)
    const found = all.find((c) => Number(c.id) === num) || null
    if (found) setCurrent(found)
    return found
  }

  function setCurrent(c: CharDict | null): void {
    currentChar.value = c ? normalizeChar(c) : null
  }

  return {
    chars,
    total,
    currentChar,
    categories,
    loading,
    fetchPageChars,
    listByCategory,
    fetchRandom,
    fetchById,
    setCurrent,
  }
})
