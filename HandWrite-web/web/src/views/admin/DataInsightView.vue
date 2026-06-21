<template>
  <div class="insight-deck">
    <!-- 顶部指标 -->
    <section class="insight-quick">
      <div v-for="q in quickStats" :key="q.label" class="quick-chip">
        <span class="quick-chip__dot" :style="{ background: q.color }"></span>
        <span class="quick-chip__label">{{ q.label }}</span>
        <span class="quick-chip__val">{{ q.value }}</span>
      </div>
    </section>

    <!-- 双列图表 -->
    <section class="insight-pair">
      <div class="chart-panel">
        <header class="chart-panel__head">
          <h3>提交量 vs 活跃用户</h3>
        </header>
        <div class="chart-panel__body">
          <svg viewBox="0 0 500 220" class="multi-line-svg">
            <defs>
              <linearGradient id="areaA" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="var(--accent-green)" stop-opacity="0.3" />
                <stop offset="100%" stop-color="var(--accent-green)" stop-opacity="0" />
              </linearGradient>
              <linearGradient id="areaB" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="var(--accent-blue)" stop-opacity="0.2" />
                <stop offset="100%" stop-color="var(--accent-blue)" stop-opacity="0" />
              </linearGradient>
            </defs>
            <!-- grid lines -->
            <line
              v-for="i in 5"
              :key="'gl' + i"
              :x1="30"
              :x2="480"
              :y1="i * 36"
              :y2="i * 36"
              stroke="var(--border-base)"
            />
            <!-- area A -->
            <path :d="areaA" fill="url(#areaA)" />
            <!-- line A -->
            <path
              :d="pathA"
              fill="none"
              stroke="var(--accent-green)"
              stroke-width="2.5"
              stroke-linecap="round"
            />
            <!-- area B -->
            <path :d="areaB" fill="url(#areaB)" />
            <!-- line B -->
            <path
              :d="pathB"
              fill="none"
              stroke="var(--accent-blue)"
              stroke-width="2.5"
              stroke-linecap="round"
              stroke-dasharray="6 3"
            />
          </svg>
          <div class="chart-panel__legend">
            <span><i style="background: var(--accent-green)"></i>提交量</span>
            <span><i style="background: var(--accent-blue)"></i>活跃用户</span>
          </div>
        </div>
      </div>

      <div class="chart-panel">
        <header class="chart-panel__head">
          <h3>审核状态分布</h3>
        </header>
        <div class="chart-panel__body">
          <div class="ring-box">
            <svg viewBox="0 0 140 140" class="ring-svg">
              <circle
                v-for="(seg, i) in ringSegs"
                :key="i"
                cx="70"
                cy="70"
                r="48"
                fill="none"
                :stroke="seg.color"
                stroke-width="20"
                :stroke-dasharray="seg.dash"
                :stroke-dashoffset="seg.offset"
              />
            </svg>
            <div class="ring-core">
              <span class="ring-core__val">{{ ringTotal }}</span>
              <span class="ring-core__lbl">总量</span>
            </div>
          </div>
          <ul class="ring-key">
            <li v-for="s in statusItems" :key="s.name">
              <i :style="{ background: s.color }"></i>
              <span>{{ s.name }}</span>
              <b>{{ s.count }}</b>
            </li>
          </ul>
        </div>
      </div>
    </section>

    <!-- 全宽进度图 -->
    <section class="insight-wide">
      <div class="chart-panel">
        <header class="chart-panel__head">
          <h3>字符采集进度 TOP 20</h3>
        </header>
        <div class="chart-panel__body chart-panel__body--tall">
          <div class="progress-grid">
            <div v-for="(p, i) in progressData" :key="p.char" class="progress-row">
              <span class="progress-row__char">{{ p.char }}</span>
              <div class="progress-row__bar">
                <div
                  class="progress-row__fill"
                  :style="{
                    width: p.pct + '%',
                    background: barColor(p.pct),
                    transition: 'width 0.8s ease ' + i * 0.04 + 's',
                  }"
                ></div>
              </div>
              <span class="progress-row__pct">{{ p.pct }}%</span>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
/**
 * 数据洞察（管理员）
 *
 * 数据来源（与 HandWrite-client api2.md §控制台 Dashboard 对齐）：
 *   - 顶部 4 指标 → GET /v1/stats/overview   (StatsOverviewVO)
 *   - 折线图       → GET /v1/stats/trend?days=30  (SampleTrendVO)
 *   - 状态环形     → GET /v1/stats/status-distribution
 *   - 进度条       → GET /v1/stats/dict-progress  (DictProgressVO[])
 */
import { ref, computed, onMounted } from 'vue'
import {
  fetchOverview,
  fetchSampleTrend,
  fetchStatusDistribution,
  fetchDictProgress,
} from '@/api/stats'
import type {
  DictProgress,
  SampleTrendVO,
  StatsOverviewVO,
  StatusDistribution,
} from '@/api/contracts/stats'

/* ========================= 类型 ========================= */
interface QuickChip {
  label: string
  value: string
  color: string
}

interface StatusDatum {
  name: string
  count: number
  color: string
}

interface ProgressRow {
  char: string
  pct: number
}

/* ========================= 顶部 4 指标 ========================= */
const quickStats = ref<QuickChip[]>([])

function fmtPct(n: number): string {
  if (!isFinite(n) || n < 0) return '0.0%'
  return n.toFixed(1) + '%'
}

function fmtInt(n: number): string {
  return (n ?? 0).toLocaleString('en-US')
}

function buildQuickStats(vo: StatsOverviewVO) {
  const total = Number(vo.totalSamples ?? 0)
  const approved = Number(vo.approvedSamples ?? 0)
  const passRate = total > 0 ? (approved / total) * 100 : 0

  quickStats.value = [
    { label: '总样本', value: fmtInt(vo.totalSamples), color: '#22C55E' },
    { label: '总用户', value: fmtInt(vo.totalUsers), color: '#38BDF8' },
    { label: '通过率', value: fmtPct(passRate), color: '#22C55E' },
    { label: '今日新增', value: fmtInt(vo.todaySamples), color: '#F59E0B' },
  ]
}

/* ========================= 状态环形图 ========================= */
const STATUS_COLOR_MAP: Record<string, { name: string; color: string }> = {
  APPROVED: { name: '已通过', color: '#22C55E' },
  PENDING: { name: '待审核', color: '#F59E0B' },
  REJECTED: { name: '已驳回', color: '#EF4444' },
  DRAFT: { name: '草稿', color: '#38BDF8' },
}

const statusItems = ref<StatusDatum[]>([])

const ringTotal = computed(() => statusItems.value.reduce((s, d) => s + d.count, 0))

const ringSegs = computed(() => {
  const total = ringTotal.value
  const circ = 2 * Math.PI * 48
  let acc = 0
  return statusItems.value.map((d) => {
    const frac = total > 0 ? d.count / total : 0
    const dash = `${frac * circ} ${circ}`
    const offset = -acc * circ
    acc += frac
    return { ...d, dash, offset }
  })
})

function applyStatus(list: StatusDistribution[]) {
  statusItems.value = list.map((s) => {
    const m = STATUS_COLOR_MAP[(s.status || '').toUpperCase()]
    return {
      name: m?.name ?? s.status,
      count: Number(s.count ?? 0),
      color: m?.color ?? '#94A3B8',
    }
  })
}

/* ========================= 折线图（提交量 vs 活跃用户） ========================= */
const samplesA = ref<number[]>([])
const samplesB = ref<number[]>([])

const maxA = computed(() => Math.max(...samplesA.value, ...samplesB.value, 1))

function buildPath(data: number[], maxVal: number): string {
  const pts = data.map((v, i) => ({
    x: 30 + (i / Math.max(data.length - 1, 1)) * 450,
    y: 200 - (v / maxVal) * 180,
  }))
  return pts.map((p, i) => `${i === 0 ? 'M' : 'L'}${p.x},${p.y}`).join(' ')
}

function buildArea(data: number[], maxVal: number): string {
  if (data.length === 0) return ''
  const pts = data.map((v, i) => ({
    x: 30 + (i / Math.max(data.length - 1, 1)) * 450,
    y: 200 - (v / maxVal) * 180,
  }))
  let d = `M${pts[0].x},${pts[0].y}`
  for (let i = 1; i < pts.length; i++) d += ` L${pts[i].x},${pts[i].y}`
  d += ` L${pts[pts.length - 1].x},200 L${pts[0].x},200 Z`
  return d
}

const pathA = computed(() => buildPath(samplesA.value, maxA.value))
const areaA = computed(() => buildArea(samplesA.value, maxA.value))
const pathB = computed(() => buildPath(samplesB.value, maxA.value))
const areaB = computed(() => buildArea(samplesB.value, maxA.value))

function applyTrend(vo: SampleTrendVO | null) {
  const a = vo?.samples ?? []
  const b = vo?.users ?? []
  samplesA.value = a
  samplesB.value = b
}

/* ========================= 字符采集进度 TOP 20 ========================= */
const progressData = ref<ProgressRow[]>([])

function applyDictProgress(list: DictProgress[]) {
  progressData.value = list.map((p) => ({
    char: p.char || (p as unknown as { charValue?: string }).charValue || '?',
    pct: Number(p.progress ?? 0),
  }))
}

function barColor(pct: number): string {
  if (pct >= 100) return '#22C55E'
  if (pct >= 60) return '#38BDF8'
  return '#F59E0B'
}

/* ========================= 数据加载 ========================= */
const loading = ref(false)

async function loadAll() {
  loading.value = true
  try {
    const [overviewRes, trendRes, statusRes, dictRes] = await Promise.allSettled([
      fetchOverview(),
      fetchSampleTrend(30),
      fetchStatusDistribution(),
      fetchDictProgress(),
    ])

    if (overviewRes.status === 'fulfilled') {
      buildQuickStats(overviewRes.value)
    } else {
      // 兜底：使用 0 值
      buildQuickStats({
        totalSamples: 0,
        totalUsers: 0,
        totalChars: 0,
        todaySamples: 0,
        todayUsers: 0,
        pendingAudits: 0,
        approvedSamples: 0,
        rejectedSamples: 0,
        growthRate: 0,
      })
    }

    if (trendRes.status === 'fulfilled') applyTrend(trendRes.value)
    else applyTrend(null)

    if (statusRes.status === 'fulfilled') applyStatus(statusRes.value)
    else applyStatus([])

    if (dictRes.status === 'fulfilled') applyDictProgress(dictRes.value)
    else applyDictProgress([])

    const failed = [overviewRes, trendRes, statusRes, dictRes].filter(
      (r) => r.status === 'rejected'
    ).length
    if (failed > 0) {
      console.warn(`[DataInsight] ${failed}/4 接口加载失败，已用占位数据`)
    }
  } finally {
    loading.value = false
  }
}

onMounted(loadAll)
</script>

<style scoped>
.insight-deck {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* === 快速指标 === */
.insight-quick {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.quick-chip {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 20px;
  background: var(--bg-card);
  border-radius: 10px;
  flex: 1;
  min-width: 180px;
  transition: background 0.3s ease;
}

.quick-chip__dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.quick-chip__label {
  font-size: 13px;
  color: var(--text-muted);
}

.quick-chip__val {
  margin-left: auto;
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
}

/* === 图表面板 === */
.insight-pair {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
@media (max-width: 900px) {
  .insight-pair {
    grid-template-columns: 1fr;
  }
}

.chart-panel {
  background: var(--bg-card);
  border-radius: 14px;
  padding: 20px;
  transition: background 0.3s ease;
}

.chart-panel__head h3 {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 16px;
}

.chart-panel__body {
  height: 260px;
  position: relative;
}

.chart-panel__body--tall {
  height: 440px;
  overflow-y: auto;
}

.chart-panel__legend {
  display: flex;
  gap: 16px;
  margin-top: 8px;
}
.chart-panel__legend span {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-muted);
}
.chart-panel__legend i {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

/* === 折线图 === */
.multi-line-svg {
  width: 100%;
  height: 100%;
}

/* === 环形图 === */
.ring-box {
  position: relative;
  width: 140px;
  height: 140px;
  margin: 10px auto;
}
.ring-svg {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}
.ring-core {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.ring-core__val {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
}
.ring-core__lbl {
  font-size: 11px;
  color: var(--text-dim);
}

.ring-key {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 16px;
}
.ring-key li {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-faint);
}
.ring-key li i {
  width: 10px;
  height: 10px;
  border-radius: 3px;
  flex-shrink: 0;
}
.ring-key li b {
  margin-left: auto;
  color: var(--text-primary);
  font-weight: 600;
}

/* === 进度条 === */
.progress-grid {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.progress-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.progress-row__char {
  width: 32px;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-regular);
  text-align: center;
  flex-shrink: 0;
  font-family: 'PingFang SC', 'Microsoft YaHei', serif;
}

.progress-row__bar {
  flex: 1;
  height: 16px;
  background: var(--bg-base);
  border-radius: 4px;
  overflow: hidden;
}

.progress-row__fill {
  height: 100%;
  border-radius: 4px;
}

.progress-row__pct {
  width: 42px;
  text-align: right;
  font-size: 12px;
  color: var(--text-muted);
  flex-shrink: 0;
}

/* === 全宽 === */
.insight-wide {
  display: grid;
  grid-template-columns: 1fr;
}

/* 滚动条 */
.chart-panel__body--tall::-webkit-scrollbar {
  width: 6px;
}
.chart-panel__body--tall::-webkit-scrollbar-track {
  background: transparent;
}
.chart-panel__body--tall::-webkit-scrollbar-thumb {
  background: var(--scrollbar-thumb);
  border-radius: 3px;
}
</style>
