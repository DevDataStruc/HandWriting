import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as dictApi from '@/api/dict'
import type { DictChar, DictQuery } from '@/api/contracts/sample'
import type { PageResult } from '@/api/contracts/common'

export const useDictStore = defineStore('dict', () => {
  const chars = ref<DictChar[]>([])
  const total = ref(0)
  const currentChar = ref<DictChar | null>(null)
  const categories = ref<string[]>([])
  const loading = ref(false)

  async function fetchChars(params?: DictQuery): Promise<PageResult<DictChar>> {
    loading.value = true
    try {
      const res = await dictApi.fetchCharDict(params)
      chars.value = res.list
      total.value = res.total
      return res
    } finally {
      loading.value = false
    }
  }

  async function fetchRandom(): Promise<DictChar> {
    const data = await dictApi.fetchRandomChar()
    currentChar.value = data
    return data
  }

  async function fetchById(id: number | string): Promise<DictChar> {
    const data = await dictApi.fetchChar(id)
    currentChar.value = data
    return data
  }

  async function fetchCategories(): Promise<string[]> {
    if (categories.value.length > 0) return categories.value
    const data = await dictApi.fetchCategories()
    categories.value = data
    return data
  }

  function setCurrent(c: DictChar | null): void {
    currentChar.value = c
  }

  return {
    chars,
    total,
    currentChar,
    categories,
    loading,
    fetchChars,
    fetchRandom,
    fetchById,
    fetchCategories,
    setCurrent,
  }
})
