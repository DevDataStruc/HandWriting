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
import BaseCard from '@/components/base/BaseCard.vue'
import StatsCard from '@/components/business/StatsCard.vue'
import { fetchOverview, fetchSampleTrend, fetchStatusDistribution, fetchTopContributors } from '@/api/stats'
import type {
  SampleTrend,
  StatsOverview,
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

const statsCards = ref<{ label: string; value: number; icon: string; variant: string; trend: number }[]>([])

const trendOption = ref<Record<string, unknown>>({})
const statusOption = ref<Record<string, unknown>>({})
const contributorOption = ref<Record<string, unknown>>({})

async function loadAll() {
  try {
    const [overview, trend, status, contributors] = await Promise.all([
      fetchOverview(),
      fetchSampleTrend(30),
      fetchStatusDistribution(),
      fetchTopContributors(10),
    ])
    bindOverview(overview)
    bindTrend(trend)
    bindStatus(status)
    bindContributors(contributors)
  } catch (err) {
    console.warn('dashboard load error', err)
  }
}

function bindOverview(d: StatsOverview) {
  statsCards.value = [
    { label: '累计样本', value: d.totalSamples, icon: 'Document', variant: 'primary', trend: d.growthRate ?? 0 },
    { label: '注册用户', value: d.totalUsers, icon: 'User', variant: 'success', trend: 0 },
    { label: '待审核', value: d.pendingAudits, icon: 'Bell', variant: 'warning', trend: 0 },
    { label: '今日新增', value: d.todaySamples, icon: 'TrendCharts', variant: 'default', trend: 0 },
  ]
}

function bindTrend(d: SampleTrend) {
  trendOption.value = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['样本数', '用户数'], textStyle: { color: '#E2E8F0' } },
    grid: { left: 40, right: 20, top: 40, bottom: 30 },
    xAxis: {
      type: 'category',
      data: d.dates,
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
        data: d.samples,
        itemStyle: { color: '#22C55E' },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
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
        data: d.users,
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
      data: sorted.map((c) => c.nickname || c.username),
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
            x: 0, y: 0, x2: 1, y2: 0,
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
