import request from '@/utils/request'
import type { DictChar, DictQuery } from './contracts/sample'
import type { PageResult } from './contracts/common'

/** 获取字符字典（分页） */
export function fetchCharDict(params?: DictQuery): Promise<PageResult<DictChar>> {
  return request.get<PageResult<DictChar>>('/dict/chars', params)
}

/** 获取单个字符详情 */
export function fetchChar(id: number | string): Promise<DictChar> {
  return request.get<DictChar>(`/dict/chars/${id}`)
}

/** 随机获取一个待采集字符 */
export function fetchRandomChar(): Promise<DictChar> {
  return request.get<DictChar>('/dict/random')
}

/** 获取字符分类列表 */
export function fetchCategories(): Promise<string[]> {
  return request.get<string[]>('/dict/categories')
}
