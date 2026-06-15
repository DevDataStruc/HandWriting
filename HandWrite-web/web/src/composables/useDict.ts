import { computed, onMounted, ref } from 'vue'
import { useDictStore } from '@/stores/dict'
import type { DictChar } from '@/api/contracts/sample'

export interface UseDictOptions {
  autoLoad?: boolean
  pageSize?: number
}

/**
 * 字符字典 composable
 */
export function useDict(options: UseDictOptions = {}) {
  const dictStore = useDictStore()
  const currentIndex = ref(0)

  const list = computed<DictChar[]>(() => dictStore.chars)
  const current = computed<DictChar | null>(() => list.value[currentIndex.value] || dictStore.currentChar)

  async function loadAll(params?: { category?: string; difficulty?: number }) {
    await dictStore.fetchChars({
      pageNum: 1,
      pageSize: options.pageSize ?? 50,
      ...params,
    })
  }

  async function loadRandom() {
    const c = await dictStore.fetchRandom()
    dictStore.setCurrent(c)
    return c
  }

  function next() {
    if (currentIndex.value < list.value.length - 1) currentIndex.value++
  }

  function prev() {
    if (currentIndex.value > 0) currentIndex.value--
  }

  function setIndex(i: number) {
    if (i >= 0 && i < list.value.length) currentIndex.value = i
  }

  onMounted(() => {
    if (options.autoLoad !== false && list.value.length === 0) {
      loadAll().catch((e) => console.warn('[useDict] loadAll', e))
    }
  })

  return {
    list,
    current,
    currentIndex,
    loadAll,
    loadRandom,
    next,
    prev,
    setIndex,
  }
}
