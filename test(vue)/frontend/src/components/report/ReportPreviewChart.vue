<template>
  <div class="card report-chart-card">
    <div class="card-title">主图表</div>
    <div v-if="!hasData" class="empty">暂无图表数据</div>
    <div v-else ref="el" class="chart-container"></div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import type { ReportTransformResult } from '@/utils/reportTransform'

const props = defineProps<{
  result: ReportTransformResult
  metricNameMap?: Record<string, string>
}>()

const el = ref<HTMLElement | null>(null)
let chart: echarts.ECharts | null = null

const hasData = computed(() => (props.result?.chart?.categories?.length || 0) > 0)

function buildOption() {
  const chartType = props.result.config.chartType === 'auto' ? props.result.chart.suggestedType : (props.result.config.chartType as any)
  const categories = props.result.chart.categories
  const series = props.result.chart.series.map((s) => ({
    name: (props.metricNameMap && props.metricNameMap[s.name]) ? props.metricNameMap[s.name] : s.name,
    type: chartType === 'horizontalBar' ? 'bar' : chartType,
    data: s.data,
    barMaxWidth: 42
  }))

  const isPie = chartType === 'pie'
  if (isPie) {
    const first = props.result.chart.series[0]
    const data = categories.map((c, idx) => ({ name: c, value: first?.data?.[idx] ?? 0 }))
    return {
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [
        {
          type: 'pie',
          radius: ['45%', '70%'],
          itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
          label: { show: true },
          data
        }
      ]
    }
  }

  const horizontal = chartType === 'horizontalBar'
  return {
    tooltip: { trigger: 'axis' },
    legend: { bottom: 0 },
    grid: { left: '8%', right: '4%', top: '10%', bottom: '18%', containLabel: true },
    xAxis: horizontal
      ? { type: 'value' }
      : {
          type: 'category',
          data: categories,
          axisLabel: { fontSize: 11 },
          axisTick: { alignWithLabel: true },
          boundaryGap: true
        },
    yAxis: horizontal
      ? {
          type: 'category',
          data: categories,
          axisLabel: { fontSize: 11 },
          axisTick: { alignWithLabel: true },
          boundaryGap: true
        }
      : { type: 'value' },
    series: series.map((s) => ({
      ...s,
      ...(horizontal ? { type: 'bar' } : null),
      ...(props.result.chart.series.length === 1 && !horizontal && chartType === 'line' ? { smooth: true } : null)
    }))
  }
}

function render() {
  if (!el.value) return
  if (!hasData.value) return
  if (chart) {
    chart.dispose()
    chart = null
  }
  chart = echarts.init(el.value)
  chart.setOption(buildOption())
}

onMounted(() => {
  render()
})

watch(
  () => props.result,
  () => {
    render()
  },
  { deep: true }
)

onBeforeUnmount(() => {
  if (chart) {
    chart.dispose()
    chart = null
  }
})
</script>

<style scoped>
.report-chart-card {
  margin-bottom: 16px;
}
.chart-container {
  width: 100%;
  height: 320px;
  min-height: 320px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  padding: 8px;
  box-sizing: border-box;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.2);
  overflow: hidden;
}
.empty {
  text-align: center;
  padding: 36px 10px;
  color: #64748b;
  font-size: 14px;
}
</style>

