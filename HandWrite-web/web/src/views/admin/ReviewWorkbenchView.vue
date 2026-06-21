<template>
  <div class="review-bench">
    <div class="bench-shell">
      <!-- 顶栏 -->
      <header class="bench-topbar">
        <h2>审核工作台</h2>
        <div class="bench-topbar__actions">
          <div class="text-field">
            <span class="text-field__glyph">&#128269;</span>
            <input v-model="keyword" class="text-field__ctrl" placeholder="搜索字符/用户..." />
          </div>
          <!-- 状态筛选 -->
          <div class="text-field text-field--select">
            <span class="text-field__glyph">&#128196;</span>
            <select v-model="statusFilter" class="text-field__ctrl" @change="handleQuery">
              <option value="">全部状态</option>
              <option value="PENDING">待审核</option>
              <option value="APPROVED">已通过</option>
              <option value="REJECTED">已驳回</option>
            </select>
          </div>
          <!-- 日期范围 -->
          <div class="text-field">
            <span class="text-field__glyph">&#128197;</span>
            <input
              v-model="dateRange"
              class="text-field__ctrl"
              type="text"
              placeholder="YYYY-MM-DD ~ YYYY-MM-DD"
            />
          </div>
          <button class="pill-btn pill-btn--go" @click="handleQuery">查询</button>
          <button class="pill-btn" @click="resetFilters">重置</button>
          <button
            class="pill-btn pill-btn--ok"
            :class="{ 'is-disabled': pickedIds.size === 0 }"
            @click="batchApprove"
          >
            批量通过
          </button>
          <button
            class="pill-btn pill-btn--no"
            :class="{ 'is-disabled': pickedIds.size === 0 }"
            @click="batchReject"
          >
            批量驳回
          </button>
        </div>
      </header>

      <!-- 统计条 -->
      <div class="bench-statrow">
        <div class="bench-stat">
          <span class="bench-stat__num">{{ pendingList.length }}</span>
          <span class="bench-stat__lbl">待审核</span>
        </div>
        <div class="bench-stat">
          <span class="bench-stat__num bench-stat__num--ok">{{ approvedCount }}</span>
          <span class="bench-stat__lbl">已通过</span>
        </div>
        <div class="bench-stat">
          <span class="bench-stat__num bench-stat__num--no">{{ rejectedCount }}</span>
          <span class="bench-stat__lbl">已驳回</span>
        </div>
        <div class="bench-stat">
          <span class="bench-stat__num">{{ pickedIds.size }}</span>
          <span class="bench-stat__lbl">已选中</span>
        </div>
      </div>

      <!-- 审核列表 -->
      <div class="review-list">
        <div
          v-for="item in filteredList"
          :key="item.id"
          class="review-entry"
          :class="{ 'is-picked': pickedIds.has(item.id) }"
        >
          <label class="review-entry__tick">
            <input
              type="checkbox"
              :checked="pickedIds.has(item.id)"
              @change="togglePick(item.id)"
            />
            <span class="tick-mark"></span>
          </label>

          <div class="review-entry__thumb">
            <div class="thumb-glyph" :style="item.fileUrl ? {} : { background: item.bg }">
              <img
                v-if="item.fileUrl"
                :src="item.fileUrl"
                :alt="item.glyph"
                class="thumb-img"
                @error="onImgError($event, item)"
                @load="onImgLoad($event)"
              />
              <span v-else>{{ item.glyph }}</span>
            </div>
          </div>

          <div class="review-entry__body">
            <div class="entry-line">
              <span class="entry-char">{{ item.glyph }}</span>
              <span class="entry-tag entry-tag--wait">待审核</span>
            </div>
            <div class="entry-line entry-line--sub">
              <span>提交人: @{{ item.submitter }}</span>
              <span>笔画: {{ item.strokes }}</span>
              <span>用时: {{ item.duration }}s</span>
            </div>
            <div class="entry-line entry-line--sub">
              <span>{{ item.time }}</span>
              <span v-if="item.remark" class="entry-remark">{{ item.remark }}</span>
            </div>
          </div>

          <div class="review-entry__ops">
            <button class="op-btn op-btn--pass" @click="approveItem(item)">&#10003; 通过</button>
            <button class="op-btn op-btn--fail" @click="rejectItem(item)">&#10007; 驳回</button>
          </div>
        </div>
      </div>

      <div v-if="filteredList.length === 0" class="review-empty">
        <div class="review-empty__icon">&#128203;</div>
        <p>暂无待审核项</p>
      </div>

      <!-- 底部 -->
      <footer class="bench-footer">
        <span class="bench-footer__info">共 {{ filteredList.length }} 条</span>
        <div class="bench-footer__pager">
          <button class="pager-chip" :disabled="page <= 1" @click="page--">&laquo;</button>
          <span
            v-for="p in totalPages"
            :key="p"
            class="pager-chip pager-chip--num"
            :class="{ 'pager-chip--on': p === page }"
            @click="page = p"
            >{{ p }}</span
          >
          <button class="pager-chip" :disabled="page >= totalPages" @click="page++">&raquo;</button>
        </div>
      </footer>
    </div>

    <!-- 驳回弹窗 -->
    <transition name="pop">
      <div v-if="showRejectModal" class="reject-mask" @click.self="showRejectModal = false">
        <div class="reject-dialog">
          <h3>驳回原因</h3>
          <textarea
            v-model="rejectReason"
            class="reject-dialog__area"
            placeholder="请输入驳回原因..."
            rows="4"
          ></textarea>
          <div class="reject-dialog__btns">
            <button class="pill-btn" @click="showRejectModal = false">取消</button>
            <button class="pill-btn pill-btn--no" @click="confirmReject">确认驳回</button>
          </div>
        </div>
      </div>
    </transition>

    <!-- 提示 -->
    <transition name="pop">
      <div v-if="toast" class="bench-toast">{{ toast }}</div>
    </transition>
  </div>
</template>

<script setup lang="ts">
/**
 * 审核工作台（管理员）
 *
 * 数据来源（与 HandWrite-client api2.md §审核 对齐）：
 *   - 待审核列表 → GET   /v1/audit/pending?pageNum=&pageSize=
 *   - 单条通过    → POST  /v1/audit/{id}/approve
 *   - 单条驳回    → POST  /v1/audit/{id}/reject   body: { reason }
 *   - 批量操作    → 前端循环（后端无批量接口，参考 api/audit.batchAudit）
 *
 * 字段映射说明：
 *   - glyph       ← char（后端 SampleVO.char）
 *   - bg          ← 基于 charId 的稳定渐变（原始 mock 用 8 种渐变色）
 *   - submitter   ← "user#" + userId（后端未返回 username）
 *   - strokes / duration ← 后端无此字段，默认 0（仅作展示）
 *   - time        ← createTime 截断为 "YYYY-MM-DD HH:mm"
 */
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { pendingAudits, approve, reject, batchAudit } from '@/api/audit'
import type { SampleVO } from '@/api/contracts/sample'

/* ========================= 类型与常量 ========================= */
interface ReviewRow {
  id: number
  glyph: string
  bg: string
  fileUrl: string
  submitter: string
  strokes: number
  duration: number
  time: string
  remark: string
  status: number
  createTime: string
}

const GRADIENT_PALETTE = [
  'linear-gradient(135deg,#0d9488,#5eead4)',
  'linear-gradient(135deg,#f59e0b,#fbbf24)',
  'linear-gradient(135deg,#22c55e,#86efac)',
  'linear-gradient(135deg,#38bdf8,#67e8f9)',
  'linear-gradient(135deg,#ef4444,#fca5a5)',
  'linear-gradient(135deg,#8b5cf6,#c4b5fd)',
  'linear-gradient(135deg,#ec4899,#f9a8d4)',
  'linear-gradient(135deg,#14b8a6,#5eead4)',
]

/** 状态值映射（后端 0/1/2 数字或全大写枚举字符串） */
function statusToNumber(s: number | string | undefined): number {
  if (typeof s === 'number') return s
  if (typeof s === 'string') {
    const v = s.toUpperCase()
    if (v === 'PENDING' || v === '0') return 0
    if (v === 'APPROVED' || v === '1') return 1
    if (v === 'REJECTED' || v === '2') return 2
  }
  return 0
}

/* ========================= 状态 ========================= */
const keyword = ref('')
const statusFilter = ref('')
const dateRange = ref('')
const page = ref(1)
const pageSize = 10

const pendingList = ref<ReviewRow[]>([])
const total = ref(0)
const loading = ref(false)

// 本次会话内的已通过/已驳回计数（与原 mock 行为一致：单页内累计）
const approvedCount = ref(0)
const rejectedCount = ref(0)

const pickedIds = ref<Set<number>>(new Set())
const showRejectModal = ref(false)
const rejectReason = ref('')
const rejectTarget = ref<ReviewRow | null>(null)
const toast = ref('')
let toastTimer: ReturnType<typeof setTimeout> | null = null



/* ========================= 计算属性 ========================= */
function parseDateRange(): { from: string; to: string } | null {
  const v = dateRange.value.trim()
  if (!v) return null
  // 支持 "YYYY-MM-DD ~ YYYY-MM-DD" / "YYYY-MM-DD - YYYY-MM-DD" / "YYYY-MM-DD,YYYY-MM-DD"
  const parts = v
    .split(/[~\-,，\s]+/)
    .map((s) => s.trim())
    .filter(Boolean)
  if (parts.length === 0) return null
  const ok = (s: string) => /^\d{4}-\d{2}-\d{2}$/.test(s)
  if (parts.length === 1 && ok(parts[0])) {
    return { from: parts[0], to: parts[0] }
  }
  if (parts.length === 2 && ok(parts[0]) && ok(parts[1])) {
    return { from: parts[0], to: parts[1] }
  }
  return null
}

const dateFilter = computed(() => parseDateRange())

const filteredList = computed(() => {
  let list = pendingList.value
  if (keyword.value) {
    const kw = keyword.value.toLowerCase()
    list = list.filter((i) => i.glyph.includes(kw) || i.submitter.toLowerCase().includes(kw))
  }
  if (statusFilter.value) {
    const target = statusToNumber(statusFilter.value)
    list = list.filter((i) => i.status === target)
  }
  if (dateFilter.value) {
    const { from, to } = dateFilter.value
    list = list.filter((i) => {
      const t = (i.createTime || '').substring(0, 10)
      return t >= from && t <= to
    })
  }
  return list
})

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

/* ========================= 工具函数 ========================= */
function pickGradient(charId: number): string {
  return GRADIENT_PALETTE[Math.abs(charId) % GRADIENT_PALETTE.length]
}

function formatTime(iso: string | undefined): string {
  if (!iso) return ''
  // "2024-03-15T12:30:00" → "2024-03-15 12:30"
  return iso.replace('T', ' ').substring(0, 16)
}

function mapSampleVO(vo: SampleVO): ReviewRow {
  return {
    id: Number(vo.id),
    glyph: vo.char || '?',
    bg: pickGradient(Number(vo.charId)),
    fileUrl: vo.fileUrl || '',
    submitter: `user#${vo.userId}`,
    strokes: 0,
    duration: 0,
    time: formatTime(vo.createTime),
    remark: vo.rejectReason || '',
    status: statusToNumber(vo.status),
    createTime: vo.createTime || '',
  }
}

function showToastMsg(msg: string) {
  toast.value = msg
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => {
    toast.value = ''
  }, 2000)
}

/**
 * 图片加载失败：清空该行的 fileUrl，让模板回退到渐变 + 文字。
 * 避免后端图片不存在时显示破图标。
 */
function onImgError(_evt: Event, item: ReviewRow) {
  if (item.fileUrl) {
    item.fileUrl = ''
  }
}

function onImgLoad(_evt: Event) {
  // 加载成功占位（无需额外处理，预留扩展）
}

/* ========================= 数据加载 ========================= */
async function loadPending() {
  loading.value = true
  try {
    const res = await pendingAudits({ pageNum: page.value, pageSize })
    pendingList.value = (res.records ?? []).map(mapSampleVO)
    total.value = Number(res.total ?? 0)
  } catch (err) {
    console.error('[ReviewWorkbench] 加载待审核列表失败', err)
    pendingList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

/* ========================= 选择与查询 ========================= */
function togglePick(id: number) {
  const s = new Set(pickedIds.value)
  s.has(id) ? s.delete(id) : s.add(id)
  pickedIds.value = s
}

function handleQuery() {
  page.value = 1
  loadPending()
}

function resetFilters() {
  keyword.value = ''
  statusFilter.value = ''
  dateRange.value = ''
  page.value = 1
  loadPending()
}

/* ========================= 单条审核 ========================= */
async function approveItem(item: ReviewRow) {
  try {
    await approve(item.id, {})
    pendingList.value = pendingList.value.filter((i) => i.id !== item.id)
    total.value = Math.max(0, total.value - 1)
    approvedCount.value++
    // 从选中集合中移除
    if (pickedIds.value.has(item.id)) {
      const s = new Set(pickedIds.value)
      s.delete(item.id)
      pickedIds.value = s
    }
    showToastMsg(`已通过：${item.glyph}`)
  } catch (err) {
    console.error('[ReviewWorkbench] 通过失败', err)
    ElMessage.error(`通过失败：${item.glyph}`)
  }
}

function rejectItem(item: ReviewRow) {
  rejectTarget.value = item
  rejectReason.value = ''
  showRejectModal.value = true
}

async function confirmReject() {
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请填写驳回原因')
    return
  }

  // 单条模式：rejectTarget 有值；批量模式：rejectTarget 为空，使用 pickedIds
  if (rejectTarget.value) {
    const target = rejectTarget.value
    try {
      await reject(target.id, { reason: rejectReason.value.trim() })
      pendingList.value = pendingList.value.filter((i) => i.id !== target.id)
      total.value = Math.max(0, total.value - 1)
      rejectedCount.value++
      if (pickedIds.value.has(target.id)) {
        const s = new Set(pickedIds.value)
        s.delete(target.id)
        pickedIds.value = s
      }
      showRejectModal.value = false
      showToastMsg(`已驳回：${target.glyph}`)
    } catch (err) {
      console.error('[ReviewWorkbench] 驳回失败', err)
      ElMessage.error(`驳回失败：${target.glyph}`)
    }
    return
  }

  // 批量模式
  const ids = Array.from(pickedIds.value)
  if (ids.length === 0) {
    showRejectModal.value = false
    return
  }
  try {
    const res = await batchAudit(ids, 'REJECTED', rejectReason.value.trim())
    rejectedCount.value += res.successCount
    if (res.successCount > 0) {
      const failedSet = new Set(res.failedIds)
      pendingList.value = pendingList.value.filter(
        (i) => !ids.includes(i.id) || failedSet.has(i.id)
      )
      total.value = Math.max(0, total.value - res.successCount)
      showToastMsg(
        `批量驳回 ${res.successCount} 项${res.failedCount > 0 ? `，失败 ${res.failedCount} 项` : ''}`
      )
    } else {
      showToastMsg('批量驳回全部失败')
    }
    if (res.successCount > 0) pickedIds.value = new Set()
    showRejectModal.value = false
  } catch (err) {
    console.error('[ReviewWorkbench] 批量驳回失败', err)
    ElMessage.error('批量驳回失败')
  }
}

/* ========================= 批量审核 ========================= */
async function batchApprove() {
  if (pickedIds.value.size === 0) return
  const ids = Array.from(pickedIds.value)
  try {
    const res = await batchAudit(ids, 'APPROVED')
    approvedCount.value += res.successCount
    if (res.successCount > 0) {
      // 移除已通过的项
      const failedSet = new Set(res.failedIds)
      pendingList.value = pendingList.value.filter(
        (i) => !ids.includes(i.id) || failedSet.has(i.id)
      )
      total.value = Math.max(0, total.value - res.successCount)
      showToastMsg(
        `批量通过 ${res.successCount} 项${res.failedCount > 0 ? `，失败 ${res.failedCount} 项` : ''}`
      )
    } else {
      showToastMsg('批量通过全部失败')
    }
    if (res.successCount > 0) pickedIds.value = new Set()
  } catch (err) {
    console.error('[ReviewWorkbench] 批量通过失败', err)
    ElMessage.error('批量通过失败')
  }
}

function batchReject() {
  if (pickedIds.value.size === 0) return
  // 共用驳回弹窗：rejectTarget 留空表示批量模式，confirmReject 会识别分支
  rejectTarget.value = null
  rejectReason.value = ''
  showRejectModal.value = true
}

/* ========================= 分页监听 ========================= */
watch(page, () => {
  pickedIds.value = new Set()
  loadPending()
})

onMounted(loadPending)
</script>

<style scoped>
.review-bench {
  padding: 0;
}

.bench-shell {
  background: var(--bg-card);
  border-radius: 14px;
  padding: 24px;
  transition: background 0.3s ease;
}

/* === 顶栏 === */
.bench-topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 20px;
}

.bench-topbar h2 {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.bench-topbar__actions {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.text-field {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--bg-base);
  border-radius: 8px;
  padding: 0 12px;
}

.text-field__glyph {
  font-size: 14px;
  color: var(--text-dim);
}

.text-field__ctrl {
  width: 180px;
  height: 38px;
  background: transparent;
  border: none;
  outline: none;
  color: var(--text-primary);
  font-size: 14px;
  font-family: inherit;
}

.text-field__ctrl::placeholder {
  color: var(--text-dim);
}

/* select 用 .text-field__ctrl 但需要保留原生下拉箭头 */
.text-field--select .text-field__ctrl {
  appearance: none;
  -webkit-appearance: none;
  -moz-appearance: none;
  cursor: pointer;
  padding-right: 22px;
  background-image:
    linear-gradient(45deg, transparent 50%, var(--text-dim) 50%),
    linear-gradient(135deg, var(--text-dim) 50%, transparent 50%);
  background-position:
    calc(100% - 12px) center,
    calc(100% - 8px) center;
  background-size:
    4px 4px,
    4px 4px;
  background-repeat: no-repeat;
}
.text-field--select .text-field__ctrl option {
  background: var(--bg-card);
  color: var(--text-primary);
}

/* 真实样本图 */
.thumb-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
  border-radius: inherit;
  background: #fff;
}

.pill-btn {
  height: 38px;
  padding: 0 18px;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  background: var(--bg-hover);
  color: var(--text-faint);
}

.pill-btn:hover {
  background: var(--bg-elevated);
}

.pill-btn--go {
  background: var(--accent-green);
  color: var(--text-on-accent);
}
.pill-btn--go:hover {
  background: var(--accent-green-dark);
}

.pill-btn--ok {
  background: var(--accent-green-soft);
  color: var(--accent-green);
}
.pill-btn--ok:hover {
  background: var(--accent-green-soft-hover);
}

.pill-btn--no {
  background: var(--accent-red-soft);
  color: var(--accent-red);
}
.pill-btn--no:hover {
  background: var(--accent-red-soft-hover);
}

.is-disabled {
  opacity: 0.4;
  cursor: not-allowed;
  pointer-events: none;
}

/* === 统计条 === */
.bench-statrow {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.bench-stat {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  background: var(--bg-base);
  border-radius: 10px;
}

.bench-stat__num {
  font-size: 22px;
  font-weight: 700;
  color: var(--accent-amber);
}
.bench-stat__num--ok {
  color: var(--accent-green);
}
.bench-stat__num--no {
  color: var(--accent-red);
}

.bench-stat__lbl {
  font-size: 13px;
  color: var(--text-muted);
}

/* === 审核列表 === */
.review-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 20px;
}

.review-entry {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: var(--bg-base);
  border-radius: 12px;
  transition: all 0.2s;
}

.review-entry:hover {
  background: var(--bg-soft);
}

.review-entry.is-picked {
  outline: 2px solid var(--accent-green);
  outline-offset: -2px;
}

/* 选择框 */
.review-entry__tick {
  cursor: pointer;
  flex-shrink: 0;
}

.review-entry__tick input {
  display: none;
}

.tick-mark {
  display: block;
  width: 20px;
  height: 20px;
  border-radius: 5px;
  background: var(--bg-hover);
  transition: all 0.15s;
}

.review-entry__tick input:checked + .tick-mark {
  background: var(--accent-green);
}

/* 缩略图 */
.review-entry__thumb {
  flex-shrink: 0;
}

.thumb-glyph {
  width: 64px;
  height: 64px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.9);
  text-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
}

/* 内容 */
.review-entry__body {
  flex: 1;
  min-width: 0;
}

.entry-line {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 4px;
}

.entry-line--sub {
  font-size: 12px;
  color: var(--text-dim);
  gap: 16px;
}

.entry-char {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.entry-tag {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
}

.entry-tag--wait {
  background: var(--accent-amber-soft);
  color: var(--accent-amber);
}

.entry-remark {
  color: var(--accent-red);
  font-style: italic;
}

/* 操作按钮 */
.review-entry__ops {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.op-btn {
  height: 34px;
  padding: 0 14px;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.op-btn--pass {
  background: var(--accent-green-soft);
  color: var(--accent-green);
}
.op-btn--pass:hover {
  background: var(--accent-green-soft-hover);
}

.op-btn--fail {
  background: var(--accent-red-soft);
  color: var(--accent-red);
}
.op-btn--fail:hover {
  background: var(--accent-red-soft-hover);
}

/* === 空状态 === */
.review-empty {
  text-align: center;
  padding: 60px 20px;
}
.review-empty__icon {
  font-size: 48px;
  margin-bottom: 12px;
  opacity: 0.5;
}
.review-empty p {
  font-size: 14px;
  color: var(--text-dim);
}

/* === 底部 === */
.bench-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16px;
  border-top: 1px solid var(--border-base);
}

.bench-footer__info {
  font-size: 13px;
  color: var(--text-muted);
}

.bench-footer__pager {
  display: flex;
  gap: 6px;
  align-items: center;
}

.pager-chip {
  min-width: 32px;
  height: 32px;
  border: none;
  border-radius: 6px;
  background: var(--bg-base);
  color: var(--text-muted);
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}

.pager-chip:hover:not(:disabled) {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.pager-chip:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.pager-chip--num {
  font-size: 13px;
}

.pager-chip--on {
  background: var(--accent-green);
  color: var(--text-on-accent);
  font-weight: 600;
}

/* === 驳回弹窗 === */
.reject-mask {
  position: fixed;
  inset: 0;
  background: var(--overlay-mask);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.reject-dialog {
  background: var(--bg-card);
  border-radius: 14px;
  padding: 24px;
  width: 400px;
  max-width: 90vw;
  box-shadow: var(--shadow-md);
}

.reject-dialog h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 16px;
}

.reject-dialog__area {
  width: 100%;
  padding: 12px;
  background: var(--bg-base);
  border: none;
  border-radius: 8px;
  color: var(--text-primary);
  font-size: 14px;
  font-family: inherit;
  resize: vertical;
  outline: none;
  margin-bottom: 16px;
}

.reject-dialog__area::placeholder {
  color: var(--text-dim);
}

.reject-dialog__btns {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* === Toast === */
.bench-toast {
  position: fixed;
  bottom: 40px;
  left: 50%;
  transform: translateX(-50%);
  padding: 10px 28px;
  background: var(--accent-green);
  color: var(--text-on-accent);
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  box-shadow: var(--shadow-lg);
  z-index: 200;
}

/* === 动画 === */
.pop-enter-active,
.pop-leave-active {
  transition: all 0.25s ease;
}
.pop-enter-from,
.pop-leave-to {
  opacity: 0;
  transform: scale(0.95);
}
</style>
