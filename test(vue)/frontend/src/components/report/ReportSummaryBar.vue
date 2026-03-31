<template>
  <div class="card report-summary-bar">
    <div class="summary-left">
      <div class="summary-title">{{ title }}</div>
      <div class="summary-sub">
        <span>数据域：{{ domainLabel }}</span>
        <span class="sep">·</span>
        <span>范围：{{ timeRangeText }}</span>
        <span class="sep">·</span>
        <span>维度：{{ dimensionLabel }}</span>
        <span class="sep">·</span>
        <span>指标：{{ metricLabels }}</span>
      </div>
      <div class="summary-meta">
        <span class="meta-item">聚合自 {{ sourceCount }} 条记录</span>
        <span class="meta-item">生成时间 {{ generatedAtText }}</span>
        <span v-if="capped" class="meta-warn">已截断：仅聚合前 {{ cap }} 条记录</span>
      </div>
    </div>
    <div class="summary-right">
      <span class="tag" v-for="(t, idx) in tags" :key="idx">{{ t }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { DomainSchema } from '@/utils/reportSchema'
import type { ReportTransformResult } from '@/utils/reportTransform'

const props = defineProps<{
  schema: DomainSchema
  result: ReportTransformResult
}>()

const title = computed(() => props.result.config.name || '报表工作台')
const domainLabel = computed(() => props.schema.label)
const timeRangeText = computed(() => {
  if (!props.schema.supportsTimeRange) return '快照型（无时间趋势）'
  const s = props.result.config.startDate || ''
  const e = props.result.config.endDate || ''
  return s && e ? `${s} ~ ${e}` : '—'
})
const dimensionLabel = computed(() => props.schema.dimensions.find((d) => d.key === props.result.config.dimensionKey)?.label || '—')
const metricLabels = computed(() =>
  props.result.config.metricKeys.map((k) => props.schema.metrics.find((m) => m.key === k)?.label || k).join('、')
)
const sourceCount = computed(() => props.result.source.recordCount)
const cap = computed(() => props.result.source.cap)
const capped = computed(() => props.result.source.capped)
const generatedAtText = computed(() => {
  const iso = props.result.generatedAt
  if (!iso) return '—'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  return `${d.toLocaleDateString()} ${d.toLocaleTimeString()}`
})

const tags = computed(() => {
  const cfg = props.result.config
  const g = cfg.dimensionKey === 'time' ? `粒度：${cfg.grain}` : '分组：单维度'
  const top = cfg.topN === 'all' ? 'Top：全部' : `Top：${cfg.topN}`
  const sort =
    cfg.sortBy === 'metricDesc' ? '排序：主指标↓' : cfg.sortBy === 'metricAsc' ? '排序：主指标↑' : '排序：默认'
  const ct = cfg.chartType === 'auto' ? `图表：自动(${props.result.chart.suggestedType})` : `图表：${cfg.chartType}`
  return [g, top, sort, ct]
})
</script>

<style scoped>
.report-summary-bar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.summary-title {
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 6px;
}

.summary-sub {
  font-size: 13px;
  color: #475569;
  margin-bottom: 8px;
}

.sep {
  margin: 0 6px;
  color: #94a3b8;
}

.summary-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 12px;
  color: #64748b;
}

.meta-warn {
  color: #b45309;
}

.summary-right {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.tag {
  font-size: 12px;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(102, 126, 234, 0.08);
  border: 1px solid rgba(102, 126, 234, 0.2);
  color: #4a5568;
  font-weight: 600;
}

@media (max-width: 768px) {
  .report-summary-bar {
    flex-direction: column;
  }
  .summary-right {
    justify-content: flex-start;
  }
}
</style>

