import request from '@/utils/request'
import type { CharDict, DictListQuery, DictPageQuery } from './contracts/sample'
import type { PageResult } from './contracts/common'

/**
 * 字典模块 - 与 API.md §1.2 / §7.3 一致
 * 基础路径：/v1/dict
 *
 * ⚠️ 重点：后端 PageQuery 是 query 复杂对象（@ModelAttribute），
 *   前端使用 qs.stringify 展开嵌套对象，详见 request 层
 */

/** GET /v1/dict/chars - 分页查询字符字典 */
export function pageChars(params?: DictPageQuery): Promise<PageResult<CharDict>> {
  return request.get<PageResult<CharDict>>('/dict/chars', {
    ...(params as Record<string, unknown> | undefined),
  })
}

/** GET /v1/dict/chars/list - 按分类列出字符（缓存友好，量小时优选） */
export function listChars(params?: DictListQuery): Promise<CharDict[]> {
  return request.get<CharDict[]>('/dict/chars/list', {
    ...(params as Record<string, unknown> | undefined),
  })
}
