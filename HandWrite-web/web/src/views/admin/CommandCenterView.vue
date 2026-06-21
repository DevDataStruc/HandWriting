<template>
  <div class="ops-hub">
    <!-- 指标卡片行 -->
    <section class="kpi-strip">
      <div v-for="m in metrics" :key="m.tag" class="kpi-tile" :class="'kpi-tile--' + m.tone">
        <div class="kpi-tile__badge">
          <SvgIcon :icon-name="m.icon" :size="22" />
        </div>
        <div class="kpi-tile__body">
          <span class="kpi-tile__caption">{{ m.tag }}</span>
          <span class="kpi-tile__figure">{{ m.num }}</span>
          <span
            v-if="m.delta !== 0"
            class="kpi-tile__delta"
            :class="m.delta > 0 ? 'is-up' : 'is-down'"
          >
            {{ m.delta > 0 ? '+' : '' }}{{ m.delta }}%
          </span>
        </div>
      </div>
    </section>

    <!-- 图表行 1 -->
    <section class="viz-duo">
      <div class="viz-frame">
        <header class="viz-frame__head">
          <h3>数据增长走势</h3>
          <span class="viz-frame__sub">近 30 天</span>
        </header>
        <div class="viz-frame__canvas">
          <svg viewBox="0 0 600 200" class="line-svg">
            <defs>
              <linearGradient id="areaG1" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="var(--accent-green)" stop-opacity="0.35" />
                <stop offset="100%" stop-color="var(--accent-green)" stop-opacity="0" />
              </linearGradient>
            </defs>
            <!-- grid -->
            <line
              v-for="i in 4"
              :key="'g' + i"
              :x1="40"
              :x2="580"
              :y1="i * 40"
              :y2="i * 40"
              stroke="var(--border-base)"
              stroke-width="1"
            />
            <!-- area -->
            <path :d="areaPath" fill="url(#areaG1)" />
            <!-- line -->
            <path
              :d="linePath"
              fill="none"
              stroke="var(--accent-green)"
              stroke-width="2.5"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
            <!-- dots -->
            <circle
              v-for="(p, i) in linePoints"
              :key="i"
              :cx="p.x"
              :cy="p.y"
              r="3.5"
              fill="var(--accent-green)"
              class="line-dot"
            />
          </svg>
          <div class="viz-legend">
            <span class="viz-legend__item"
              ><i style="background: var(--accent-green)"></i>提交量</span
            >
            <span class="viz-legend__item"
              ><i style="background: var(--accent-blue)"></i>活跃用户</span
            >
          </div>
        </div>
      </div>

      <div class="viz-frame viz-frame--compact">
        <header class="viz-frame__head">
          <h3>状态占比</h3>
        </header>
        <div class="viz-frame__canvas">
          <div class="donut-wrap">
            <svg viewBox="0 0 160 160" class="donut-svg">
              <circle
                v-for="(seg, i) in donutSegments"
                :key="i"
                cx="80"
                cy="80"
                r="56"
                fill="none"
                :stroke="seg.color"
                stroke-width="22"
                :stroke-dasharray="seg.dash"
                :stroke-dashoffset="seg.offset"
                :style="{ transition: 'stroke-dasharray 0.6s ease' }"
              />
            </svg>
            <div class="donut-center">
              <span class="donut-center__val">{{ totalStatus }}</span>
              <span class="donut-center__lbl">总计</span>
            </div>
          </div>
          <ul class="donut-key">
            <li v-for="s in statusData" :key="s.name">
              <i :style="{ background: s.color }"></i>
              <span>{{ s.name }}</span>
              <b>{{ s.count }}</b>
            </li>
          </ul>
        </div>
      </div>
    </section>

    <!-- 图表行 2 -->
    <section class="viz-full">
      <div class="viz-frame">
        <header class="viz-frame__head">
          <h3>活跃贡献者 TOP 10</h3>
        </header>
        <div class="viz-frame__canvas">
          <div class="hbar-list">
            <div v-for="(c, i) in topContributors" :key="c.name" class="hbar-row">
              <span class="hbar-row__rank">{{ i + 1 }}</span>
              <span class="hbar-row__name">{{ c.name }}</span>
              <div class="hbar-row__track">
                <div
                  class="hbar-row__fill"
                  :style="{ width: c.pct + '%', transition: 'width 0.8s ease ' + i * 0.06 + 's' }"
                ></div>
              </div>
              <span class="hbar-row__val">{{ c.count }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
/**
 * 指挥中心（管理员首页）
 *
 * 数据来源（与 HandWrite-client api2.md §控制台 Dashboard 对齐）：
 *   - 顶部 4 卡片  → GET /v1/stats/overview        StatsOverviewVO
 *   - 数据增长走势 → GET /v1/stats/trend?days=30  SampleTrendVO
 *   - 状态占比     → GET /v1/stats/status-distribution
 *   - 贡献者 TOP   → GET /v1/stats/top-contributors?limit=10
 */
import { ref, computed, onMounted } from 'vue'
import {
  fetchOverview,
  fetchSampleTrend,
  fetchStatusDistribution,
  fetchTopContributors,
} from '@/api/stats'
import type {
  StatsOverviewVO,
  SampleTrendVO,
  StatusDistribution,
  TopContributor,
} from '@/api/contracts/stats'

interface MetricTile {
  tag: string
  num: string
  icon: string
  tone: 'teal' | 'blue' | 'amber' | 'green'
  delta: number
}

/* ========================= 4 个 KPI 卡片 ========================= */
const metrics = ref<MetricTile[]>([])

function formatNumber(n: number): string {
  if (n == null) return '0'
  return n.toLocaleString('en-US')
}

function buildMetrics(vo: StatsOverviewVO | null) {
  const safe: StatsOverviewVO = vo ?? {
    totalSamples: 0,
    totalUsers: 0,
    totalChars: 0,
    todaySamples: 0,
    todayUsers: 0,
    pendingAudits: 0,
    approvedSamples: 0,
    rejectedSamples: 0,
    growthRate: 0,
  }
  const g = Number(safe.growthRate ?? 0)
  metrics.value = [
    {
      tag: '总收录量',
      num: formatNumber(safe.totalSamples),
      icon: 'package',
      tone: 'teal',
      delta: g,
    },
    { tag: '注册用户', num: formatNumber(safe.totalUsers), icon: 'users', tone: 'blue', delta: 0 },
    { tag: '待处理', num: formatNumber(safe.pendingAudits), icon: 'bell', tone: 'amber', delta: 0 },
    {
      tag: '今日新增',
      num: formatNumber(safe.todaySamples),
      icon: 'trend-up',
      tone: 'green',
      delta: 0,
    },
  ]
}

/* ========================= 折线图（趋势） ========================= */
const rawSamples = ref<number[]>([])

const linePoints = computed(() => {
  const arr = rawSamples.value
  if (arr.length === 0) return []
  const max = Math.max(...arr, 1)
  return arr.map((v, i) => ({
    x: 40 + (i / Math.max(arr.length - 1, 1)) * 540,
    y: 180 - (v / max) * 160,
  }))
})

const linePath = computed(() => {
  return linePoints.value.map((p, i) => `${i === 0 ? 'M' : 'L'}${p.x},${p.y}`).join(' ')
})

const areaPath = computed(() => {
  const pts = linePoints.value
  if (!pts.length) return ''
  let d = `M${pts[0].x},${pts[0].y}`
  for (let i = 1; i < pts.length; i++) d += ` L${pts[i].x},${pts[i].y}`
  d += ` L${pts[pts.length - 1].x},180 L${pts[0].x},180 Z`
  return d
})

function applyTrend(vo: SampleTrendVO | null) {
  const arr = vo?.samples ?? []
  // 若后端返回空数组，则回退到全 0 占位（与原 mock 行为一致）
  rawSamples.value = arr.length ? arr : []
}

/* ========================= 状态占比（环形图） ========================= */
interface StatusDatum {
  name: string
  count: number
  color: string
}

/** 状态名（后端 status 字符串）→ 颜色映射 */
const STATUS_COLOR_MAP: Record<string, { name: string; color: string }> = {
  APPROVED: { name: '已通过', color: '#22C55E' },
  PENDING: { name: '待审核', color: '#F59E0B' },
  REJECTED: { name: '已驳回', color: '#EF4444' },
  DRAFT: { name: '草稿', color: '#38BDF8' },
}

const statusData = ref<StatusDatum[]>([])

const totalStatus = computed(() => statusData.value.reduce((s, d) => s + d.count, 0))

const donutSegments = computed(() => {
  const total = totalStatus.value
  const circ = 2 * Math.PI * 56
  let acc = 0
  return statusData.value.map((d) => {
    const frac = total > 0 ? d.count / total : 0
    const dash = `${frac * circ} ${circ}`
    const offset = -acc * circ
    acc += frac
    return { ...d, dash, offset }
  })
})

function applyStatus(list: StatusDistribution[]) {
  statusData.value = list.map((s) => {
    const m = STATUS_COLOR_MAP[(s.status || '').toUpperCase()]
    return {
      name: m?.name ?? s.status,
      count: Number(s.count ?? 0),
      color: m?.color ?? '#94A3B8',
    }
  })
}

/* ========================= 贡献者 TOP 10 ========================= */
interface ContributorRow {
  name: string
  count: number
  pct: number
}

const topContributors = ref<ContributorRow[]>([])

function applyContributors(list: TopContributor[]) {
  if (list.length === 0) {
    topContributors.value = []
    return
  }
  const max = Math.max(...list.map((c) => Number(c.sampleCount ?? 0)), 1)
  topContributors.value = list.map((c) => {
    const name = c.nickname || c.username || `user#${c.userId}`
    const count = Number(c.sampleCount ?? 0)
    return {
      name,
      count,
      pct: Math.round((count / max) * 100),
    }
  })
}

/* ========================= 数据加载 ========================= */
const loading = ref(false)
const loadedAt = ref<number>(0)

async function loadAll() {
  loading.value = true
  try {
    const [overviewRes, trendRes, statusRes, contributorRes] = await Promise.allSettled([
      fetchOverview(),
      fetchSampleTrend(30),
      fetchStatusDistribution(),
      fetchTopContributors(10),
    ])

    if (overviewRes.status === 'fulfilled') buildMetrics(overviewRes.value)
    else buildMetrics(null)

    if (trendRes.status === 'fulfilled') applyTrend(trendRes.value)
    else applyTrend(null)

    if (statusRes.status === 'fulfilled') applyStatus(statusRes.value)
    else applyStatus([])

    if (contributorRes.status === 'fulfilled') applyContributors(contributorRes.value)
    else applyContributors([])

    const failed = [overviewRes, trendRes, statusRes, contributorRes].filter(
      (r) => r.status === 'rejected'
    ).length
    if (failed > 0) {
      console.warn(`[CommandCenter] ${failed}/4 接口加载失败，已用占位数据`)
    }
    loadedAt.value = Date.now()
  } finally {
    loading.value = false
  }
}

onMounted(loadAll)
</script>

<style scoped>
.ops-hub {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* === 指标卡片 === */
.kpi-strip {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.kpi-tile {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px;
  background: var(--bg-card);
  border-radius: 14px;
  transition:
    transform 0.2s,
    box-shadow 0.2s;
}
.kpi-tile:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.kpi-tile__badge {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.kpi-tile--teal .kpi-tile__badge {
  background: var(--accent-teal-soft);
  color: var(--accent-teal);
}
.kpi-tile--blue .kpi-tile__badge {
  background: var(--accent-blue-soft);
  color: var(--accent-blue);
}
.kpi-tile--amber .kpi-tile__badge {
  background: var(--accent-amber-soft);
  color: var(--accent-amber);
}
.kpi-tile--green .kpi-tile__badge {
  background: var(--accent-green-soft);
  color: var(--accent-green);
}

.kpi-tile__body {
  display: flex;
  flex-direction: column;
}
.kpi-tile__caption {
  font-size: 13px;
  color: var(--text-muted);
}
.kpi-tile__figure {
  font-size: 26px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.3;
}
.kpi-tile__delta {
  font-size: 12px;
  font-weight: 600;
  margin-top: 2px;
}
.is-up {
  color: var(--accent-green);
}
.is-down {
  color: var(--accent-red);
}

/* === 图表框架 === */
.viz-duo {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
}
@media (max-width: 900px) {
  .viz-duo {
    grid-template-columns: 1fr;
  }
}

.viz-frame {
  background: var(--bg-card);
  border-radius: 14px;
  padding: 20px;
  min-height: 340px;
  transition: background 0.3s ease;
}
.viz-frame--compact {
  min-height: 340px;
}

.viz-frame__head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 16px;
}
.viz-frame__head h3 {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}
.viz-frame__sub {
  font-size: 12px;
  color: var(--text-dim);
}

.viz-frame__canvas {
  position: relative;
  height: 280px;
}

/* === 折线图 === */
.line-svg {
  width: 100%;
  height: 100%;
}
.line-dot {
  opacity: 0;
  transition: opacity 0.2s;
}
.line-svg:hover .line-dot {
  opacity: 1;
}

.viz-legend {
  display: flex;
  gap: 16px;
  margin-top: 8px;
}
.viz-legend__item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-muted);
}
.viz-legend__item i {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

/* === 环形图 === */
.donut-wrap {
  position: relative;
  width: 160px;
  height: 160px;
  margin: 20px auto;
}
.donut-svg {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}
.donut-center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.donut-center__val {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
}
.donut-center__lbl {
  font-size: 11px;
  color: var(--text-dim);
}

.donut-key {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 16px;
}
.donut-key li {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-faint);
}
.donut-key li i {
  width: 10px;
  height: 10px;
  border-radius: 3px;
  flex-shrink: 0;
}
.donut-key li b {
  margin-left: auto;
  color: var(--text-primary);
  font-weight: 600;
}

/* === 横向条形 === */
.viz-full {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
}

.hbar-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.hbar-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.hbar-row__rank {
  width: 22px;
  height: 22px;
  border-radius: 6px;
  background: var(--bg-hover);
  color: var(--text-muted);
  font-size: 11px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.hbar-row:nth-child(-n + 3) .hbar-row__rank {
  background: var(--accent-green-soft-hover);
  color: var(--accent-green);
}
.hbar-row__name {
  width: 72px;
  font-size: 13px;
  color: var(--text-regular);
  flex-shrink: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.hbar-row__track {
  flex: 1;
  height: 18px;
  background: var(--bg-base);
  border-radius: 4px;
  overflow: hidden;
}
.hbar-row__fill {
  height: 100%;
  background: linear-gradient(90deg, var(--accent-green), var(--accent-green-dark));
  border-radius: 4px;
}
.hbar-row__val {
  width: 36px;
  text-align: right;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-muted);
  flex-shrink: 0;
}
</style>
