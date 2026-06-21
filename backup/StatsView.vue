<template>
  <div class="stats-view">
    <div class="chart-row">
      <BaseCard title="样本 vs 用户" class="chart-card">
        <VChart :option="lineOption" autoresize class="chart-canvas" />
      </BaseCard>
      <BaseCard title="状态分布" class="chart-card">
        <VChart :option="pieOption" autoresize class="chart-canvas" />
      </BaseCard>
    </div>
    <BaseCard title="字符采集进度 TOP 20" class="chart-card-wide">
      <VChart :option="progressOption" autoresize class="chart-canvas chart-canvas--tall" />
    </BaseCard>
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
} from 'echarts/components'
import VChart from 'vue-echarts'
import BaseCard from '@/components/base/BaseCard.vue'
import { fetchSampleTrend, fetchStatusDistribution, fetchDictProgress } from '@/api/stats'
import type { DictProgress, SampleTrendVO, StatusDistribution } from '@/api/contracts/stats'

use([
  CanvasRenderer,
  LineChart,
  PieChart,
  BarChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
])

const lineOption = ref<Record<string, unknown>>({})
const pieOption = ref<Record<string, unknown>>({})
const progressOption = ref<Record<string, unknown>>({})

async function loadAll() {
  try {
    const [trend, status, progress] = await Promise.all([
      fetchSampleTrend(30),
      fetchStatusDistribution(),
      fetchDictProgress(),
    ])
    bindLine(trend)
    bindPie(status)
    bindProgress(progress)
  } catch (err) {
    console.warn('stats load error', err)
  }
}

function bindLine(d: SampleTrendVO) {
  lineOption.value = {
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

function bindPie(d: StatusDistribution[]) {
  pieOption.value = {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, textStyle: { color: '#E2E8F0' } },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        itemStyle: { borderRadius: 6, borderColor: '#0F172A', borderWidth: 2 },
        data: d.map((x) => ({ name: x.status, value: x.count })),
        color: ['#22C55E', '#F59E0B', '#EF4444'],
      },
    ],
  }
}

function bindProgress(d: DictProgress[]) {
  const top = d.slice(0, 20)
  progressOption.value = {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 60, right: 30, top: 20, bottom: 30 },
    xAxis: {
      type: 'value',
      axisLine: { lineStyle: { color: '#334155' } },
      axisLabel: { color: '#94A3B8' },
      splitLine: { lineStyle: { color: '#1E293B' } },
    },
    yAxis: {
      type: 'category',
      data: top.map((p) => p.char),
      axisLine: { lineStyle: { color: '#334155' } },
      axisLabel: { color: '#E2E8F0', fontFamily: 'PingFang SC, Microsoft YaHei', fontSize: 16 },
    },
    series: [
      {
        name: '采集进度',
        type: 'bar',
        data: top.map((p) => ({
          value: p.progress,
          itemStyle: {
            color: p.progress >= 100 ? '#22C55E' : p.progress >= 60 ? '#38BDF8' : '#F59E0B',
            borderRadius: [0, 6, 6, 0],
          },
        })),
        barWidth: 16,
        label: { show: true, position: 'right', color: '#94A3B8', formatter: '{c}%' },
      },
    ],
  }
}

onMounted(() => {
  loadAll()
})
</script>

<style lang="scss" scoped>
.stats-view {
  @include flex-column;
  gap: $spacing-md;
}
.chart-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: $spacing-md;
  @include responsive(md) {
    grid-template-columns: 1fr;
  }
}
.chart-card {
  min-height: 360px;
}
.chart-card-wide {
  min-height: 480px;
}
.chart-canvas {
  height: 320px;
  width: 100%;
  &--tall {
    height: 440px;
  }
}
</style>
