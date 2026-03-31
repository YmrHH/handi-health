<template>
  <div class="card report-builder-panel">
    <div class="card-title">报表构建器</div>

    <div class="builder-tabs" role="tablist" aria-label="报表构建器分段">
      <button
        v-for="t in tabs"
        :key="t.key"
        type="button"
        class="tab-btn"
        :class="{ active: activeTab === t.key }"
        @click="activeTab = t.key"
      >
        <span class="tab-key">{{ t.key }}</span>
        <span class="tab-label">{{ t.label }}</span>
      </button>
    </div>

    <div v-show="activeTab === 'A'" class="builder-section">
      <div class="section-title">A. 报表主题</div>
      <label class="field-label">报表名称</label>
      <input class="input" v-model="local.name" placeholder="自动生成，也可编辑" />
      <div class="field-help">用于描述当前报表在回答什么问题（首版不做持久化）。</div>
    </div>

    <div v-show="activeTab === 'B'" class="builder-section">
      <div class="section-title">B. 数据域</div>
      <label class="field-label">数据域选择</label>
      <select class="input" v-model="local.domain">
        <option value="followup">随访域（已实现）</option>
        <option value="patient">患者域（已实现）</option>
        <option value="alert">告警域（已实现）</option>
      </select>
    </div>

    <div v-show="activeTab === 'C'" class="builder-section">
      <div class="section-title">C. 时间范围</div>
      <div class="quick-row">
        <button class="btn btn-ghost" type="button" :disabled="!schema.supportsTimeRange" @click="setQuickRange(7)">近7天</button>
        <button class="btn btn-ghost" type="button" :disabled="!schema.supportsTimeRange" @click="setQuickRange(30)">近30天</button>
        <button class="btn btn-ghost" type="button" :disabled="!schema.supportsTimeRange" @click="setQuickRange(90)">近90天</button>
      </div>

      <div class="date-row">
        <div class="date-col">
          <label class="field-label">开始日期</label>
          <input class="input" type="date" v-model="local.startDate" :disabled="!schema.supportsTimeRange" />
        </div>
        <div class="date-col">
          <label class="field-label">结束日期</label>
          <input class="input" type="date" v-model="local.endDate" :disabled="!schema.supportsTimeRange" />
        </div>
      </div>

      <label class="field-label">日期粒度</label>
      <select class="input" v-model="local.grain" :disabled="!schema.supportsTimeRange || !supportsGrain">
        <option value="day">天</option>
        <option value="week">周</option>
        <option value="month">月</option>
      </select>
      <div v-if="!schema.supportsTimeRange" class="field-warn">该数据域当前仅支持快照型报表（无时间趋势）。</div>
    </div>

    <div v-show="activeTab === 'D'" class="builder-section">
      <div class="section-title">D. 维度</div>
      <label class="field-label">分组维度（单选）</label>
      <select class="input" v-model="local.dimensionKey">
        <option v-for="d in schema.dimensions" :key="d.key" :value="d.key">{{ d.label }}</option>
      </select>
    </div>

    <div v-show="activeTab === 'E'" class="builder-section">
      <div class="section-title">E. 指标</div>
      <div class="metric-grid">
        <label v-for="m in schema.metrics" :key="m.key" class="metric-item">
          <input type="checkbox" :value="m.key" v-model="local.metricKeys" />
          <span>{{ m.label }}</span>
        </label>
      </div>
      <div class="field-help">首版建议选择 1–3 个指标；若为空将无法生成报表。</div>
    </div>

    <div v-show="activeTab === 'F'" class="builder-section">
      <div class="section-title">F. 展示偏好</div>
      <label class="field-label">图表类型</label>
      <select class="input" v-model="local.chartType">
        <option value="auto">自动</option>
        <option value="bar">柱状图</option>
        <option value="line">折线图</option>
        <option value="horizontalBar">条形图</option>
        <option value="pie">环图</option>
      </select>

      <label class="field-label">排序方式</label>
      <select class="input" v-model="local.sortBy">
        <option value="default">默认</option>
        <option value="metricDesc">主指标降序</option>
        <option value="metricAsc">主指标升序</option>
      </select>

      <label class="field-label">TopN</label>
      <select class="input" v-model="topNProxy">
        <option value="all">全部</option>
        <option value="5">Top5</option>
        <option value="10">Top10</option>
        <option value="20">Top20</option>
      </select>

      <label class="field-label">显示数据标签</label>
      <div class="toggle-row">
        <input type="checkbox" v-model="local.showLabels" />
        <span class="toggle-text">在图表上显示数值（首版仅用于配置保留）</span>
      </div>
    </div>

    <div class="builder-actions">
      <button class="btn btn-primary" type="button" :disabled="!canGenerate" @click="$emit('generate')">
        生成报表
      </button>
      <button class="btn btn-secondary" type="button" @click="$emit('reset')">重置条件</button>
      <button class="btn btn-ghost" type="button" :disabled="!canExport" @click="$emit('export')">导出当前结果</button>
    </div>

    <div v-if="hint" class="builder-hint">{{ hint }}</div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { DomainSchema, ReportDomain, TimeGrain, ChartType } from '@/utils/reportSchema'

const props = defineProps<{
  schema: DomainSchema
  modelValue: {
    domain: ReportDomain
    name: string
    startDate: string
    endDate: string
    grain: TimeGrain
    dimensionKey: string
    metricKeys: string[]
    chartType: ChartType
    sortBy: 'default' | 'metricDesc' | 'metricAsc'
    topN: number | 'all'
    showLabels: boolean
  }
  canGenerate: boolean
  canExport: boolean
  hint?: string | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', v: any): void
  (e: 'generate'): void
  (e: 'reset'): void
  (e: 'export'): void
}>()

const local = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const tabs = [
  { key: 'A', label: '主题' },
  { key: 'B', label: '数据域' },
  { key: 'C', label: '时间' },
  { key: 'D', label: '维度' },
  { key: 'E', label: '指标' },
  { key: 'F', label: '偏好' }
] as const

const activeTab = ref<(typeof tabs)[number]['key']>('A')

watch(
  () => local.value.domain,
  () => {
    // 切换域后，优先跳到维度/指标，减少用户来回滚动
    activeTab.value = 'D'
  }
)

const supportsGrain = computed(() => {
  const dim = props.schema.dimensions.find((d) => d.key === local.value.dimensionKey)
  return dim?.kind === 'time' && dim?.supportsGrain
})

const topNProxy = computed({
  get: () => (local.value.topN === 'all' ? 'all' : String(local.value.topN)),
  set: (v: string) => {
    emit('update:modelValue', {
      ...local.value,
      topN: v === 'all' ? 'all' : Number(v)
    })
  }
})

function isoDate(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function setQuickRange(days: number) {
  const end = new Date()
  const start = new Date()
  start.setDate(end.getDate() - Math.max(1, days - 1))
  emit('update:modelValue', {
    ...local.value,
    startDate: isoDate(start),
    endDate: isoDate(end)
  })
}
</script>

<style scoped>
.report-builder-panel {
  position: sticky;
  top: 12px;
}

.builder-tabs {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 8px;
  margin: 10px 0 14px;
  padding: 10px;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(226, 232, 240, 0.7);
  border-radius: 12px;
  position: sticky;
  top: 58px;
  z-index: 2;
}

.tab-btn {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  padding: 10px 6px;
  border-radius: 10px;
  border: 1px solid rgba(226, 232, 240, 0.8);
  background: rgba(247, 250, 252, 0.65);
  cursor: pointer;
  transition: all 0.2s ease;
}

.tab-btn:hover {
  border-color: rgba(102, 126, 234, 0.5);
  background: rgba(102, 126, 234, 0.06);
}

.tab-btn.active {
  border-color: rgba(102, 126, 234, 0.8);
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.14) 0%, rgba(118, 75, 162, 0.10) 100%);
}

.tab-key {
  font-size: 12px;
  font-weight: 800;
  color: #334155;
}

.tab-label {
  font-size: 11px;
  color: #64748b;
  font-weight: 600;
}

.builder-section {
  margin-bottom: 16px;
  padding: 12px 12px;
  background: rgba(247, 250, 252, 0.6);
  border-radius: 12px;
  border: 1px solid rgba(226, 232, 240, 0.6);
}

.section-title {
  font-size: 13px;
  font-weight: 700;
  color: #334155;
  margin-bottom: 10px;
}

.field-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  margin: 10px 0 6px;
}

.field-help {
  margin-top: 6px;
  font-size: 12px;
  color: #64748b;
}

.field-warn {
  margin-top: 8px;
  font-size: 12px;
  color: #b45309;
  background: rgba(254, 243, 199, 0.5);
  border: 1px solid rgba(251, 191, 36, 0.4);
  padding: 8px 10px;
  border-radius: 10px;
}

.quick-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.quick-row .btn {
  padding: 10px 14px;
}

.date-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.metric-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.metric-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 10px;
  border: 1px solid rgba(226, 232, 240, 0.7);
  background: rgba(255, 255, 255, 0.75);
  font-size: 13px;
  color: #334155;
}

.toggle-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: rgba(255, 255, 255, 0.75);
  border-radius: 10px;
  border: 1px solid rgba(226, 232, 240, 0.7);
}

.toggle-text {
  font-size: 13px;
  color: #475569;
}

.builder-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 10px;
}

.builder-hint {
  margin-top: 12px;
  font-size: 12px;
  color: #64748b;
}

@media (max-width: 768px) {
  .report-builder-panel {
    position: static;
  }
  .builder-tabs {
    position: static;
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
  .date-row {
    grid-template-columns: 1fr;
  }
}
</style>

