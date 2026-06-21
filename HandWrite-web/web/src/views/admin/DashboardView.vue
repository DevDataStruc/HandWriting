<template>
  <div class="admin-dashboard">
    <div class="stat-row">
      <StatsCard
        v-for="s in statsCards"
        :key="s.label"
        :label="s.label"
        :value="s.value"
        :icon="s.icon"
        :variant="s.variant"
        :trend="s.trend"
        trend-label="较昨日"
      />
    </div>

    <div class="chart-row">
      <BaseCard title="样本增长趋势" subtitle="近 30 天" class="chart-card">
        <VChart :option="trendOption" autoresize class="chart-canvas" />
      </BaseCard>
      <BaseCard title="样本状态分布" class="chart-card chart-card--small">
        <VChart :option="statusOption" autoresize class="chart-canvas" />
      </BaseCard>
    </div>

    <div class="chart-row">
      <BaseCard title="贡献者排行 TOP 10" class="chart-card chart-card--wide">
        <VChart :option="contributorOption" autoresize class="chart-canvas" />
      </BaseCard>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart, BarChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  DataZoomComponent,
} from 'echarts/components'
import VChart from 'vue-echarts'
import { ElMessage } from 'element-plus'
import BaseCard from '@/components/base/BaseCard.vue'
import StatsCard from '@/components/business/StatsCard.vue'
import {
  fetchOverview,
  fetchSampleTrend,
  fetchStatusDistribution,
  fetchTopContributors,
} from '@/api/stats'
import type {
  SampleTrendVO,
  StatsOverviewVO,
  StatusDistribution,
  TopContributor,
} from '@/api/contracts/stats'

use([
  CanvasRenderer,
  LineChart,
  PieChart,
  BarChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  DataZoomComponent,
])

/**
 * 与后端 DashboardController 一一对应：
 *  - GET /v1/stats/overview              -> StatsOverviewVO
 *  - GET /v1/stats/trend?days=30         -> SampleTrendVO
 *  - GET /v1/stats/status-distribution   -> StatusDistribution[]
 *  - GET /v1/stats/top-contributors?limit=10 -> TopContributor[]
 *
 * 字段映射（后端 VO -> 前端展示）：
 *  totalSamples  -> 累计样本
 *  totalUsers    -> 注册用户
 *  pendingAudits -> 待审核
 *  todaySamples  -> 今日新增
 *  growthRate    -> 较昨日增长率（百分比）
 */
interface StatCard {
  label: string
  value: number
  icon: string
  variant: 'primary' | 'success' | 'warning' | 'danger' | 'default'
  trend: number
}

const statsCards = ref<StatCard[]>([])
const trendOption = ref<Record<string, unknown>>({})
const statusOption = ref<Record<string, unknown>>({})
const contributorOption = ref<Record<string, unknown>>({})

const loading = ref(false)
const loadedAt = ref<string>('')

async function loadAll() {
  loading.value = true
  // allSettled：单个接口失败不影响其他数据加载
  const [overviewRes, trendRes, statusRes, contributorRes] = await Promise.allSettled([
    fetchOverview(),
    fetchSampleTrend(30),
    fetchStatusDistribution(),
    fetchTopContributors(10),
  ])

  if (overviewRes.status === 'fulfilled') {
    bindOverview(overviewRes.value)
  } else {
    console.error('[Dashboard] overview 加载失败', overviewRes.reason)
  }
  if (trendRes.status === 'fulfilled') {
    bindTrend(trendRes.value)
  } else {
    console.error('[Dashboard] trend 加载失败', trendRes.reason)
  }
  if (statusRes.status === 'fulfilled') {
    bindStatus(statusRes.value)
  } else {
    console.error('[Dashboard] status 加载失败', statusRes.reason)
  }
  if (contributorRes.status === 'fulfilled') {
    bindContributors(contributorRes.value)
  } else {
    console.error('[Dashboard] contributors 加载失败', contributorRes.reason)
  }

  const failed = [overviewRes, trendRes, statusRes, contributorRes].filter(
    (r) => r.status === 'rejected'
  ).length
  const ok = 4 - failed
  if (failed > 0) {
    ElMessage.warning(`控制台数据加载 ${ok}/4 成功，${failed} 项失败`)
  }
  loadedAt.value = new Date().toLocaleTimeString()
  loading.value = false
}

function bindOverview(d: StatsOverviewVO) {
  statsCards.value = [
    {
      label: '累计样本',
      value: d.totalSamples,
      icon: 'Document',
      variant: 'primary',
      trend: d.growthRate ?? 0,
    },
    {
      label: '注册用户',
      value: d.totalUsers ?? 0,
      icon: 'User',
      variant: 'success',
      trend: 0,
    },
    {
      label: '待审核',
      value: d.pendingAudits ?? 0,
      icon: 'Bell',
      variant: 'warning',
      trend: 0,
    },
    {
      label: '今日新增',
      value: d.todaySamples ?? 0,
      icon: 'TrendCharts',
      variant: 'default',
      trend: 0,
    },
  ]
}

function bindTrend(d: SampleTrendVO) {
  trendOption.value = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['样本数', '用户数'], textStyle: { color: '#E2E8F0' } },
    grid: { left: 40, right: 20, top: 40, bottom: 30 },
    xAxis: {
      type: 'category',
      data: d.dates ?? [],
      axisLine: { lineStyle: { color: '#334155' } },
      axisLabel: { color: '#94A3B8' },
    },
    yAxis: {
      type: 'value',
      axisLine: { lineStyle: { color: '#334155' } },
      axisLabel: { color: '#94A3B8' },
      splitLine: { lineStyle: { color: '#1E293B' } },
    },
    series: [
      {
        name: '样本数',
        type: 'line',
        smooth: true,
        data: d.samples ?? [],
        itemStyle: { color: '#22C55E' },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(34, 197, 94, 0.4)' },
              { offset: 1, color: 'rgba(34, 197, 94, 0)' },
            ],
          },
        },
      },
      {
        name: '用户数',
        type: 'line',
        smooth: true,
        data: d.users ?? [],
        itemStyle: { color: '#38BDF8' },
      },
    ],
  }
}

function bindStatus(d: StatusDistribution[]) {
  statusOption.value = {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, textStyle: { color: '#E2E8F0' } },
    series: [
      {
        name: '状态',
        type: 'pie',
        radius: ['45%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 6, borderColor: '#0F172A', borderWidth: 2 },
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 16, color: '#F8FAFC' } },
        data: d.map((x) => ({ name: x.status, value: x.count })),
        color: ['#22C55E', '#F59E0B', '#EF4444', '#3B82F6'],
      },
    ],
  }
}

function bindContributors(d: TopContributor[]) {
  // 横向条形图：sampleCount 从小到大（ECharts yAxis category 渲染顺序自下而上）
  const sorted = [...d].sort((a, b) => a.sampleCount - b.sampleCount)
  contributorOption.value = {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 100, right: 30, top: 20, bottom: 30 },
    xAxis: {
      type: 'value',
      axisLine: { lineStyle: { color: '#334155' } },
      axisLabel: { color: '#94A3B8' },
      splitLine: { lineStyle: { color: '#1E293B' } },
    },
    yAxis: {
      type: 'category',
      data: sorted.map((c) => c.nickname || c.username || `user#${c.userId}`),
      axisLine: { lineStyle: { color: '#334155' } },
      axisLabel: { color: '#E2E8F0' },
    },
    series: [
      {
        name: '样本数',
        type: 'bar',
        data: sorted.map((c) => c.sampleCount),
        itemStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 1,
            y2: 0,
            colorStops: [
              { offset: 0, color: '#22C55E' },
              { offset: 1, color: '#4ADE80' },
            ],
          },
          borderRadius: [0, 6, 6, 0],
        },
        barWidth: 18,
      },
    ],
  }
}

onMounted(() => {
  loadAll()
})
</script>

<style lang="scss" scoped>
.admin-dashboard {
  @include flex-column;
  gap: $spacing-md;
}

.stat-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: $spacing-md;
}

.chart-row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: $spacing-md;

  @include responsive(md) {
    grid-template-columns: 1fr;
  }
}

.chart-card {
  min-height: 360px;

  &--wide {
    grid-column: 1 / -1;
  }
}

.chart-canvas {
  height: 320px;
  width: 100%;
}
</style>
