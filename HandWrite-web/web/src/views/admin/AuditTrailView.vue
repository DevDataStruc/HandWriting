<template>
  <div class="trail-log">
    <div class="trail-shell">
      <!-- 标题 + 过滤 -->
      <header class="trail-head">
        <h2>审核日志</h2>
        <div class="trail-head__filters">
          <div class="filter-picker">
            <select v-model="statusFilter" class="filter-picker__ctrl">
              <option value="">全部状态</option>
              <option value="approved">通过</option>
              <option value="rejected">驳回</option>
              <option value="pending">待审核</option>
            </select>
          </div>
          <div class="filter-field">
            <span class="filter-field__ico">&#128269;</span>
            <input v-model="keyword" class="filter-field__ctrl" placeholder="搜索字符/提交人..." />
          </div>
          <button class="filter-go" @click="handleQuery">查询</button>
        </div>
      </header>

      <!-- 日志表 -->
      <div class="trail-table-wrap">
        <table class="trail-table">
          <thead>
            <tr>
              <th class="trail-table__th">字符</th>
              <th class="trail-table__th">缩略</th>
              <th class="trail-table__th">提交人</th>
              <th class="trail-table__th">状态</th>
              <th class="trail-table__th">审核人</th>
              <th class="trail-table__th">审核时间</th>
              <th class="trail-table__th">原因</th>
              <th class="trail-table__th">提交时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in filteredRows" :key="row.id" class="trail-table__tr">
              <td class="trail-table__td">
                <span class="glyph-label">{{ row.glyph }}</span>
              </td>
              <td class="trail-table__td">
                <div class="thumb-box" :style="{ background: row.bg }">
                  <span>{{ row.glyph }}</span>
                </div>
              </td>
              <td class="trail-table__td">
                <span class="submitter">@{{ row.submitter }}</span>
              </td>
              <td class="trail-table__td">
                <span class="status-chip" :class="'status-chip--' + row.statusKey">
                  {{ row.statusLabel }}
                </span>
              </td>
              <td class="trail-table__td">
                <span class="auditor">{{ row.auditor ? '@' + row.auditor : '—' }}</span>
              </td>
              <td class="trail-table__td">
                <span class="time-cell">{{ row.auditTime || '—' }}</span>
              </td>
              <td class="trail-table__td">
                <span v-if="row.reason" class="reason-text">{{ row.reason }}</span>
                <span v-else class="reason-none">—</span>
              </td>
              <td class="trail-table__td">
                <span class="time-cell">{{ row.submitTime }}</span>
              </td>
            </tr>
          </tbody>
        </table>

        <div v-if="filteredRows.length === 0" class="trail-empty">
          <div class="trail-empty__ico">&#128196;</div>
          <p>暂无日志记录</p>
        </div>
      </div>

      <!-- 底部分页 -->
      <footer class="trail-foot">
        <span class="trail-foot__count">共 {{ filteredRows.length }} 条记录</span>
        <div class="trail-foot__pager">
          <button class="foot-chip" :disabled="page <= 1" @click="page--">&laquo;</button>
          <span
            v-for="p in totalPages"
            :key="p"
            class="foot-chip foot-chip--num"
            :class="{ 'foot-chip--on': p === page }"
            @click="page = p"
            >{{ p }}</span
          >
          <button class="foot-chip" :disabled="page >= totalPages" @click="page++">&raquo;</button>
        </div>
      </footer>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 审核日志（管理员）
 *
 * 数据来源（与 HandWrite-client api2.md §审核 对齐）：
 *   - 审核历史 → GET /v1/audit/history?status=&pageNum=&pageSize=
 *     返回 PageResult<SampleVO>，status 为整数（0 待审核 / 1 通过 / 2 驳回）
 *
 * 字段映射说明：
 *   - glyph       ← char
 *   - bg          ← 基于 charId 的稳定渐变
 *   - submitter   ← "user#" + userId（后端未返回 username）
 *   - auditor     ← "user#" + auditedBy（如有）
 *   - statusKey   ← 0/1/2 → "pending"/"approved"/"rejected"（与 CSS 类名一致）
 *   - statusLabel ← 同上 → 中文标签
 *   - submitTime  ← createTime 格式化 "YYYY-MM-DD HH:mm"
 *   - auditTime   ← auditedTime 格式化
 */
import { ref, computed, onMounted, watch } from 'vue'
import { auditHistory } from '@/api/audit'
import type { SampleVO } from '@/api/contracts/sample'

/* ========================= 类型与常量 ========================= */
type StatusKey = 'approved' | 'rejected' | 'pending' | ''

interface TrailRow {
  id: string
  glyph: string
  bg: string
  submitter: string
  statusKey: Exclude<StatusKey, ''>
  statusLabel: string
  auditor: string
  auditTime: string
  reason: string
  submitTime: string
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

/** 后端数字状态 → 前端字符串 key（与 CSS 类名一致） */
const STATUS_FROM_NUM: Record<number, { key: Exclude<StatusKey, ''>; label: string }> = {
  0: { key: 'pending', label: '待审核' },
  1: { key: 'approved', label: '通过' },
  2: { key: 'rejected', label: '驳回' },
}

/** 前端 select 值 → 后端数字 */
const STATUS_TO_NUM: Record<Exclude<StatusKey, ''>, number> = {
  approved: 1,
  rejected: 2,
  pending: 0,
}

/* ========================= 状态 ========================= */
const keyword = ref('')
const statusFilter = ref<StatusKey>('')
const page = ref(1)
const pageSize = 10

const logRows = ref<TrailRow[]>([])
const total = ref(0)
const loading = ref(false)

/* ========================= 计算属性 ========================= */
const filteredRows = computed(() => logRows.value)

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

/* ========================= 工具函数 ========================= */
function pickGradient(charId: number): string {
  return GRADIENT_PALETTE[Math.abs(charId) % GRADIENT_PALETTE.length]
}

function formatTime(iso: string | undefined): string {
  if (!iso) return ''
  return iso.replace('T', ' ').substring(0, 16)
}

function statusOf(num: number | undefined): { key: Exclude<StatusKey, ''>; label: string } {
  return STATUS_FROM_NUM[num ?? 0] ?? STATUS_FROM_NUM[0]
}

function mapSampleVO(vo: SampleVO): TrailRow {
  const s = statusOf(vo.status)
  return {
    id: String(vo.id),
    glyph: vo.char || '?',
    bg: pickGradient(Number(vo.charId)),
    submitter: `user#${vo.userId}`,
    statusKey: s.key,
    statusLabel: s.label,
    auditor: vo.auditedBy ? `user#${vo.auditedBy}` : '',
    auditTime: formatTime(vo.auditedTime),
    reason: vo.rejectReason || '',
    submitTime: formatTime(vo.createTime),
  }
}

/* ========================= 数据加载 ========================= */
async function loadHistory() {
  loading.value = true
  try {
    const res = await auditHistory({
      status: statusFilter.value ? STATUS_TO_NUM[statusFilter.value] : undefined,
      pageNum: page.value,
      pageSize,
    })
    logRows.value = (res.records ?? []).map(mapSampleVO)
    total.value = Number(res.total ?? 0)
  } catch (err) {
    console.error('[AuditTrail] 加载审核历史失败', err)
    logRows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

/* ========================= 用户操作 ========================= */
function handleQuery() {
  page.value = 1
  loadHistory()
}

/* ========================= 监听状态过滤 / 分页变化 ========================= */
watch(statusFilter, () => {
  page.value = 1
  loadHistory()
})

watch(page, () => {
  loadHistory()
})

onMounted(loadHistory)
</script>

<style scoped>
.trail-log {
  padding: 0;
}

.trail-shell {
  background: var(--bg-card);
  border-radius: 14px;
  padding: 24px;
  transition: background 0.3s ease;
}

/* === 头部 === */
.trail-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 20px;
}

.trail-head h2 {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.trail-head__filters {
  display: flex;
  gap: 10px;
  align-items: center;
}

.filter-picker {
  position: relative;
}

.filter-picker::after {
  content: '';
  position: absolute;
  right: 12px;
  top: 50%;
  width: 7px;
  height: 7px;
  border-right: 2px solid var(--text-muted);
  border-bottom: 2px solid var(--text-muted);
  transform: translateY(-70%) rotate(45deg);
  pointer-events: none;
}

.filter-picker__ctrl {
  height: 38px;
  padding: 0 32px 0 14px;
  background-color: var(--bg-base);
  color: var(--text-faint);
  font-size: 13px;
  border: none;
  outline: none;
  border-radius: 8px;
  cursor: pointer;
  font-family: inherit;
  appearance: none;
  -webkit-appearance: none;
}

.filter-field {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--bg-base);
  border-radius: 8px;
  padding: 0 12px;
}

.filter-field__ico {
  font-size: 14px;
  color: var(--text-dim);
}

.filter-field__ctrl {
  width: 200px;
  height: 38px;
  background: transparent;
  border: none;
  outline: none;
  color: var(--text-primary);
  font-size: 14px;
  font-family: inherit;
}

.filter-field__ctrl::placeholder {
  color: var(--text-dim);
}

.filter-go {
  height: 38px;
  padding: 0 18px;
  background: var(--accent-green);
  color: var(--text-on-accent);
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}

.filter-go:hover {
  background: var(--accent-green-dark);
}

/* === 表格 === */
.trail-table-wrap {
  overflow-x: auto;
  margin-bottom: 20px;
}

.trail-table {
  width: 100%;
  border-collapse: collapse;
}

.trail-table__th {
  text-align: left;
  padding: 12px 14px;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-dim);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border-bottom: 1px solid var(--border-base);
  white-space: nowrap;
}

.trail-table__tr {
  transition: background 0.15s;
}

.trail-table__tr:hover {
  background: var(--hover-overlay);
}

.trail-table__td {
  padding: 14px;
  font-size: 13px;
  color: var(--text-faint);
  border-bottom: 1px solid var(--table-divider);
  vertical-align: middle;
}

.glyph-label {
  font-size: 20px;
  font-weight: 700;
  color: var(--accent-teal);
  font-family: 'PingFang SC', 'Microsoft YaHei', serif;
}

.thumb-box {
  width: 56px;
  height: 42px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.85);
}

.submitter {
  color: var(--text-muted);
}

.auditor {
  color: var(--text-muted);
}

.status-chip {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;
}

.status-chip--approved {
  background: var(--accent-green-soft);
  color: var(--accent-green);
}

.status-chip--rejected {
  background: var(--accent-red-soft);
  color: var(--accent-red);
}

.status-chip--pending {
  background: var(--accent-amber-soft);
  color: var(--accent-amber);
}

.time-cell {
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-size: 12px;
  color: var(--text-muted);
  white-space: nowrap;
}

.reason-text {
  color: var(--accent-red);
  font-size: 12px;
}

.reason-none {
  color: var(--bg-elevated);
}

/* === 空状态 === */
.trail-empty {
  text-align: center;
  padding: 60px 20px;
}
.trail-empty__ico {
  font-size: 48px;
  margin-bottom: 12px;
  opacity: 0.5;
}
.trail-empty p {
  font-size: 14px;
  color: var(--text-dim);
}

/* === 底部 === */
.trail-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16px;
  border-top: 1px solid var(--border-base);
}

.trail-foot__count {
  font-size: 13px;
  color: var(--text-muted);
}

.trail-foot__pager {
  display: flex;
  gap: 6px;
  align-items: center;
}

.foot-chip {
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

.foot-chip:hover:not(:disabled) {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.foot-chip:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.foot-chip--num {
  font-size: 13px;
}

.foot-chip--on {
  background: var(--accent-green);
  color: var(--text-on-accent);
  font-weight: 600;
}
</style>
