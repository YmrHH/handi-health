<template>
  <div class="page-container report-workbench-page">
    <div class="page-header">
      <!-- 仅美化，不改布局：标题徽章 + 渐变标题 -->
      <div class="page-title">
        <span class="title-badge" aria-hidden="true">
          <!-- FileSpreadsheet / BarChart 风格图标（避免引入新依赖） -->
          <svg viewBox="0 0 24 24" fill="none" class="title-badge-icon">
            <path d="M8 3h6l4 4v14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2Z" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/>
            <path d="M14 3v5h5" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/>
            <path d="M9 12v6M12 14v4M15 11v7" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
          </svg>
        </span>
        <span class="page-title-text">报表工作台</span>
      </div>
      <div class="page-subtitle">选择数据域、维度、指标与时间范围，生成真实可导出的业务报表</div>
      <div class="header-actions">
        <span class="header-meta" v-if="lastGeneratedAt">最近生成：{{ lastGeneratedAt }}</span>
        <button class="btn btn-ghost" type="button" @click="reset">重置配置</button>
      </div>
    </div>

    <div class="page-content">
      <div class="report-workbench-layout">
        <aside class="report-builder-column">
          <ReportBuilderPanel
            :schema="schema"
            v-model="config"
            :can-generate="canGenerate && !loading"
            :can-export="!!transform && !loading"
            :hint="builderHint"
            @generate="generate"
            @reset="reset"
            @export="exportCsv"
          />
        </aside>

        <section class="report-preview-column">
          <div v-if="loading" class="loading">生成中...</div>

          <ReportEmptyState
            v-else-if="error"
            title="生成失败"
            :message="error"
            :tips="['请检查相关后端接口是否可用', '确认条件下存在数据记录', '如数据量很大，建议缩小范围后再试']"
          />

          <ReportEmptyState
            v-else-if="!transform"
            title="还没有生成报表"
            message="请在左侧选择数据域、时间、维度与指标，然后点击“生成报表”。"
            :tips="['无数据会明确提示，不会造假', '图表/表格/洞察/导出都来自同一份聚合结果']"
          />

          <ReportEmptyState
            v-else-if="transform && (!transform.rows || transform.rows.length === 0)"
            title="当前条件下无数据"
            message="已完成查询与聚合，但未得到任何分组结果。"
            :tips="['可尝试扩大范围或切换维度/指标', '如后端返回空列表，这里会明确展示无数据']"
          />

          <template v-else>
            <ReportSummaryBar :schema="schema" :result="transform" />
            <ReportMetricCards :metrics="transform.metrics" />
            <ReportInsightList :insights="insights" />
            <ReportPreviewChart :result="transform" :metric-name-map="metricNameMap" />
            <ReportResultTable :columns="tableColumns" :rows="transform.rows" :page-size="tablePageSize" />
            <ReportExportToolbar :can-export="!!transform" @export-csv="exportCsv" />
          </template>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import ReportBuilderPanel from '@/components/report/ReportBuilderPanel.vue'
import ReportSummaryBar from '@/components/report/ReportSummaryBar.vue'
import ReportInsightList from '@/components/report/ReportInsightList.vue'
import ReportPreviewChart from '@/components/report/ReportPreviewChart.vue'
import ReportMetricCards from '@/components/report/ReportMetricCards.vue'
import ReportResultTable from '@/components/report/ReportResultTable.vue'
import ReportExportToolbar from '@/components/report/ReportExportToolbar.vue'
import ReportEmptyState from '@/components/report/ReportEmptyState.vue'
import { useReportWorkbench } from '@/composables/useReportWorkbench'
import { buildInsights } from '@/utils/reportInsights'
import { buildCsv, downloadTextFile } from '@/utils/reportExport'

const { schema, config, loading, error, transform, lastGeneratedAt, canGenerate, generate, reset } = useReportWorkbench()

const metricNameMap = computed(() => {
  const map: Record<string, string> = {}
  for (const m of schema.value.metrics) map[m.key] = m.label
  return map
})

const tableColumns = computed(() => {
  if (!transform.value) return []
  const cfg = transform.value.config
  const dimLabel = schema.value.dimensions.find((d) => d.key === cfg.dimensionKey)?.label || '分组'
  return [
    { key: 'label', label: dimLabel },
    ...cfg.metricKeys.map((k) => {
      const def = schema.value.metrics.find((m) => m.key === k)
      return { key: k, label: def?.label || k, format: def?.format as any }
    })
  ]
})

const insights = computed(() => (transform.value ? buildInsights(transform.value, metricNameMap.value) : []))

const tablePageSize = computed(() => {
  // 固定：每页 5 条数据
  return 5
})

const builderHint = computed(() => {
  if (!config.value.metricKeys || config.value.metricKeys.length === 0) return '请至少选择 1 个指标。'
  if (schema.value.supportsTimeRange && (!config.value.startDate || !config.value.endDate)) return '请补全时间范围。'
  return null
})

function exportCsv() {
  if (!transform.value) return
  const csv = buildCsv(transform.value)
  const safeName = (transform.value.config.name || 'report').replace(/[\\/:*?"<>|]/g, '_')
  downloadTextFile(`${safeName}.csv`, csv, 'text/csv;charset=utf-8')
}
</script>

<style scoped>
.report-workbench-page {
  width: 100%;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
  padding: 6px 0 14px 0;
  border-bottom: 1px solid rgba(148, 163, 184, 0.18);
  flex-wrap: wrap;
  gap: 10px 16px;
}

.page-title {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  font-size: 26px;
  font-weight: 800;
  letter-spacing: 0.2px;
  color: #0f172a;
}

.page-title-text {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.title-badge {
  width: 40px;
  height: 40px;
  border-radius: 14px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10px 20px rgba(102, 126, 234, 0.22);
  flex-shrink: 0;
}

.title-badge-icon {
  width: 22px;
  height: 22px;
}

.page-subtitle {
  font-size: 14px;
  color: #94a3b8;
  margin-top: 2px;
}

.btn:focus,
.btn:focus-visible,
button:focus-visible {
  outline: none;
}

.header-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-meta {
  font-size: 12px;
  color: #64748b;
}

.report-workbench-layout {
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.report-builder-column,
.report-preview-column {
  min-width: 0;
}

@media (max-width: 768px) {
  .report-workbench-layout {
    grid-template-columns: 1fr;
  }
  .header-actions {
    width: 100%;
    justify-content: flex-start;
    flex-wrap: wrap;
  }
}
</style>
