<template>
  <div class="board-page">
    <div class="page-header">
      <!-- 仅美化，不改布局：标题徽章 + 渐变标题 -->
      <div class="page-title">
        <span class="title-badge" aria-hidden="true">
          <!-- 简易 BarChart 图标（避免引入新依赖） -->
          <svg viewBox="0 0 24 24" fill="none" class="title-badge-icon">
            <path d="M4 19V5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
            <path d="M8 19V11" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
            <path d="M12 19V8" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
            <path d="M16 19V13" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
            <path d="M20 19V6" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
          </svg>
        </span>
        <span class="page-title-text">AI 分析看板</span>
      </div>
      <div class="page-subtitle">近期告警、随访与高危趋势概览</div>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <template v-else-if="board">
      <!-- 顶部三个统计卡片 -->
      <div class="card-grid cols-3">
        <div class="card">
          <div class="card-title">
            <span class="title-icon">📅</span>
            <span>本月数据统计</span>
          </div>
          <div class="month-stats">
            <div class="month-header">
              <span class="month-label">统计月份</span>
              <span class="month-value">{{ board.latestMonth }}</span>
          </div>
            <div class="stats-grid">
              <div class="stat-card">
                <div class="stat-card-icon">👥</div>
                <div class="stat-card-content">
                  <div class="stat-card-label">新增患者</div>
                  <div class="stat-card-value">{{ board.latestNewPatients }}</div>
                  <div class="stat-card-unit">人</div>
                </div>
              </div>
              <div class="stat-card">
                <div class="stat-card-icon">⚠️</div>
                <div class="stat-card-content">
                  <div class="stat-card-label">异常告警</div>
                  <div class="stat-card-value">{{ board.latestAlerts }}</div>
                  <div class="stat-card-unit">条</div>
                </div>
              </div>
              <div class="stat-card">
                <div class="stat-card-icon">📞</div>
                <div class="stat-card-content">
                  <div class="stat-card-label">随访任务</div>
                  <div class="stat-card-value">{{ board.latestFollowups }}</div>
                  <div class="stat-card-unit">次</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="card">
          <div class="card-title">
            <span class="title-icon">🎯</span>
            <span>患者风险分层</span>
          </div>
          <div class="risk-stats">
            <div class="risk-summary">
              <span class="summary-label">风险患者总数</span>
              <span class="summary-value">{{ board.latestHighRisk + board.latestMidRisk + board.latestLowRisk }}</span>
              <span class="summary-unit">人</span>
          </div>
            <div class="risk-levels">
              <div class="risk-level-item risk-high">
                <div class="risk-level-header">
                  <span class="risk-icon">🔴</span>
                  <span class="risk-label">高危患者</span>
                </div>
                <div class="risk-level-value">
                  <span class="risk-number">{{ board.latestHighRisk }}</span>
                  <span class="risk-unit">人</span>
                </div>
                <div class="risk-level-desc">需要重点关注和及时干预</div>
              </div>
              <div class="risk-level-item risk-medium">
                <div class="risk-level-header">
                  <span class="risk-icon">🟡</span>
                  <span class="risk-label">中危患者</span>
                </div>
                <div class="risk-level-value">
                  <span class="risk-number">{{ board.latestMidRisk }}</span>
                  <span class="risk-unit">人</span>
                </div>
                <div class="risk-level-desc">定期监测和随访管理</div>
              </div>
              <div class="risk-level-item risk-low">
                <div class="risk-level-header">
                  <span class="risk-icon">🟢</span>
                  <span class="risk-label">低危患者</span>
                </div>
                <div class="risk-level-value">
                  <span class="risk-number">{{ board.latestLowRisk }}</span>
                  <span class="risk-unit">人</span>
                </div>
                <div class="risk-level-desc">常规健康管理即可</div>
              </div>
            </div>
          </div>
        </div>

        <div class="card">
          <div class="card-title">AI 模型性能指标</div>
          <div class="model-metrics">
            <div class="metric-item">
              <div class="metric-header">
                <span class="metric-icon">🎯</span>
                <span class="metric-name">风险预测准确率</span>
          </div>
              <div class="metric-value">
                <span class="value-number">{{ (board.latestAuc * 100).toFixed(1) }}</span>
                <span class="value-unit">%</span>
          </div>
              <div class="metric-desc">AI模型对患者风险等级预测的准确程度</div>
          </div>
            
            <div class="metric-item">
              <div class="metric-header">
                <span class="metric-icon">⚡</span>
                <span class="metric-name">告警识别率</span>
              </div>
              <div class="metric-value">
                <span class="value-number">{{ (board.latestF1 * 100).toFixed(1) }}</span>
                <span class="value-unit">%</span>
              </div>
              <div class="metric-desc">系统识别异常情况的综合性能指标</div>
            </div>
            
            <div class="metric-item">
              <div class="metric-header">
                <span class="metric-icon">📊</span>
                <span class="metric-name">告警趋势变化</span>
              </div>
              <div class="metric-value" :class="getTrendClass(board.alertChangeRate)">
                <span class="value-number">{{ Math.round(board.alertChangeRate * 100) }}</span>
                <span class="value-unit">%</span>
                <span class="trend-icon">{{ getTrendIcon(board.alertChangeRate) }}</span>
              </div>
              <div class="metric-desc">相比上月告警数量的变化幅度</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 中间两个趋势图 -->
      <div class="card-grid cols-2" style="margin-top: 16px;">
        <div class="card">
          <div class="card-title">告警 / 随访趋势</div>
          <div ref="alertTrendContainer" class="chart-container"></div>
        </div>
        <div class="card">
          <div class="card-title">高危 / 其他趋势</div>
          <div ref="riskTrendContainer" class="chart-container"></div>
        </div>
      </div>

      <!-- 季节因素分析 -->
      <div class="card" style="margin-top: 16px;">
        <div class="card-title">季节因素影响分析</div>
        <div v-if="loadingSeason" class="loading">加载中...</div>
        <div v-else class="season-factors">
          <div class="season-intro">
            <p>不同季节和气候条件对患者病情的影响权重调整，帮助医生更好地制定随访和干预策略。</p>
          </div>
          <div class="season-grid">
            <div 
              v-for="(rule, index) in seasonRules" 
              :key="index"
              class="season-item"
              :class="getSeasonItemClass(rule.weightDesc)"
            >
              <div class="season-icon">{{ getSeasonIcon(rule.factorName) }}</div>
              <div class="season-content">
                <h4 class="season-name">{{ rule.factorName }}</h4>
                <p class="season-weight">{{ rule.weightDesc }}</p>
                <div class="season-impact">
                  <span class="impact-label">影响程度：</span>
                  <span class="impact-value" :class="getImpactClass(rule.weightDesc)">
                    {{ getImpactLevel(rule.weightDesc) }}
                  </span>
                </div>
                <div class="season-disease-impact">
                  <span class="impact-label">重点影响病种：</span>
                  <span class="impact-disease">
                    {{ getSeasonDiseaseImpact(rule.factorName) }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 病种统计分析 -->
      <div v-if="board.diseaseAnalysis && board.diseaseAnalysis.length > 0" class="disease-analysis-section" style="margin-top: 24px;">
        <div class="section-header">
          <h2 class="section-title">病种统计分析</h2>
          <p class="section-subtitle">各病种康养计划效果与病情变化趋势分析</p>
        </div>

        <!-- 筛选区域 -->
        <div class="disease-filter-section">
          <div class="filter-row">
            <div class="filter-item">
              <label class="filter-label">病种名称</label>
              <input 
                v-model="diseaseFilter.diseaseName" 
                type="text" 
                class="input" 
                placeholder="请输入病种名称"
                @input="handleFilterChange"
              />
            </div>
            <div class="filter-item">
              <label class="filter-label">患者数范围</label>
              <div class="range-inputs">
                <input 
                  v-model.number="diseaseFilter.minPatientCount" 
                  type="number" 
                  class="input input-small" 
                  placeholder="最小值"
                  @input="handleFilterChange"
                />
                <span class="range-separator">-</span>
                <input 
                  v-model.number="diseaseFilter.maxPatientCount" 
                  type="number" 
                  class="input input-small" 
                  placeholder="最大值"
                  @input="handleFilterChange"
                />
              </div>
            </div>
            <div class="filter-item">
              <label class="filter-label">稳定率范围</label>
              <div class="range-inputs">
                <input 
                  v-model.number="diseaseFilter.minStableRate" 
                  type="number" 
                  class="input input-small" 
                  placeholder="最小值(%)"
                  min="0"
                  max="100"
                  @input="handleFilterChange"
                />
                <span class="range-separator">-</span>
                <input 
                  v-model.number="diseaseFilter.maxStableRate" 
                  type="number" 
                  class="input input-small" 
                  placeholder="最大值(%)"
                  min="0"
                  max="100"
                  @input="handleFilterChange"
                />
              </div>
            </div>
            <div class="filter-actions">
              <button class="btn btn-primary" @click="handleFilterChange">
                <span>🔍</span> 筛选
              </button>
              <button class="btn btn-secondary" @click="resetFilter">
                <span>🔄</span> 重置
              </button>
            </div>
          </div>
        </div>

        <!-- 报表视图（概览表格） -->
        <div class="disease-report-card">
          <div class="disease-report-header">
            <div class="report-title">病种与康养方案效果概览报表</div>
            <div class="report-subtitle">
              从病种维度汇总患者规模、病情趋势，以及最具代表性的康养方案与医生建议，便于快速对比与决策。
            </div>
          </div>
          <div class="disease-report-table-wrapper">
            <table class="disease-report-table">
              <thead>
                <tr>
                  <th style="width: 8%;">病种</th>
                  <th style="width: 6%;">患者数</th>
                  <th style="width: 7%;">稳定率</th>
                  <th style="width: 7%;">改善率</th>
                  <th style="width: 7%;">恶化率</th>
                  <th style="width: 18%;">典型患者画像</th>
                  <th style="width: 20%;">方案效果对比</th>
                  <th style="width: 12%;">核心加重因素</th>
                  <th style="width: 15%;">医生建议要点</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="filteredDiseaseData.length === 0">
                  <td colspan="9" class="empty-cell">暂无符合筛选条件的病种数据</td>
                </tr>
                <tr 
                  v-for="row in paginatedDiseaseData" 
                  :key="row.disease"
                >
                  <td class="col-disease">
                    <div class="disease-name-cell">{{ row.disease }}</div>
                  </td>
                  <td class="col-number">
                    {{ row.patientCount }} 人
                  </td>
                  <td class="col-number">
                    {{ Math.round(row.stableRate * 100) }}%
                  </td>
                  <td class="col-number text-success">
                    {{ Math.round(row.improvementRate * 100) }}%
                  </td>
                  <td class="col-number text-danger">
                    {{ Math.round(row.deteriorationRate * 100) }}%
                  </td>
                  <td class="col-profile">
                    <template v-if="getTopProfile(row)">
                      <div class="profile-display">
                        <div class="profile-main">
                          <span class="profile-age">{{ getTopProfile(row)!.ageRange }}</span>
                          <span class="profile-stage">{{ getTopProfile(row)!.diseaseStage }}</span>
                          <span class="profile-constitution">{{ getTopProfile(row)!.constitution }}</span>
                        </div>
                        <div class="profile-details" v-if="(getTopProfile(row)!.comorbidities && getTopProfile(row)!.comorbidities.length > 0) || (getTopProfile(row)!.lifestyle && getTopProfile(row)!.lifestyle.length > 0)">
                          <span v-if="getTopProfile(row)!.comorbidities && getTopProfile(row)!.comorbidities.length > 0" class="profile-tag profile-comorbidity">
                            合并{{ getTopProfile(row)!.comorbidities.join('、') }}
                          </span>
                          <span v-if="getTopProfile(row)!.lifestyle && getTopProfile(row)!.lifestyle.length > 0" class="profile-tag profile-lifestyle">
                            {{ getTopProfile(row)!.lifestyle.join('、') }}
                          </span>
                        </div>
                        <div class="profile-count">
                          约 {{ getTopProfile(row)!.typicalCount }} 人
                        </div>
                      </div>
                    </template>
                    <span v-else class="text-muted">暂无画像数据</span>
                  </td>
                  <td class="col-plan-comparison">
                    <div v-if="getTopPlans(row).length > 0" class="plan-comparison-wrapper">
                      <div class="plan-summary" v-if="getTopPlanName(row)">
                        <div class="plan-summary-title">代表性高效方案</div>
                        <div class="plan-summary-meta">
                          <span class="plan-summary-name">{{ getTopPlanName(row) }}</span>
                          <span class="plan-summary-type" v-if="getTopPlanType(row)">{{ getTopPlanType(row) }}</span>
                        </div>
                        <div class="plan-summary-stats">
                          覆盖约 {{ getTopPlanPatientCount(row) }} 人；平均评分 {{ getTopPlanScore(row) }} 分
                        </div>
                      </div>
                      <div class="plan-comparison-list">
                        <div 
                          v-for="(plan, index) in getTopPlans(row)" 
                          :key="plan.planName"
                          class="plan-comparison-item"
                        >
                          <div class="plan-comp-header">
                            <span class="plan-comp-rank">{{ index + 1 }}</span>
                            <span class="plan-comp-name">{{ plan.planName }}</span>
                            <span class="plan-comp-type">{{ plan.planType }}</span>
                          </div>
                          <div class="plan-comp-stats">
                            <span class="plan-comp-score">效果评分：{{ plan.effectiveness }}分</span>
                            <span class="plan-comp-count">使用人数：{{ plan.patientCount }}人</span>
                          </div>
                          <div class="plan-comp-bar">
                            <div 
                              class="plan-comp-bar-fill" 
                              :style="{ width: `${plan.effectiveness}%` }"
                            ></div>
                          </div>
                        </div>
                      </div>
                    </div>
                    <span v-else class="text-muted">暂无方案数据</span>
                  </td>
                  <td class="col-factor">
                    <div v-if="getTopFactorName(row)">
                      <span class="factor-tag">{{ getTopFactorImpactLabel(row) }}</span>
                      <span class="factor-name">{{ getTopFactorName(row) }}</span>
                    </div>
                    <span v-else class="text-muted">暂无加重因素数据</span>
                  </td>
                  <td class="col-advice">
                    <div v-if="getPrimaryRecommendation(row)">
                      {{ getPrimaryRecommendation(row) }}
                    </div>
                    <span v-else class="text-muted">暂无医生建议</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- 分页 -->
        <div v-if="filteredDiseaseData.length > pageSize" class="disease-pagination">
          <div class="page-info">
            共 {{ filteredDiseaseData.length }} 条记录，第 {{ diseasePageNo }} / {{ diseaseTotalPages }} 页
          </div>
          <div class="page-controls">
            <button 
              class="btn btn-secondary" 
              :disabled="diseasePageNo <= 1"
              @click="goToDiseasePage(diseasePageNo - 1)">
              上一页
            </button>
            <span class="page-numbers">
              <button
                v-for="page in getDiseasePageNumbers()"
                :key="page"
                class="page-btn"
                :class="{ active: page === diseasePageNo, disabled: page === '...' }"
                :disabled="page === '...' || page === diseasePageNo"
                @click="page !== '...' && goToDiseasePage(page)">
                {{ page }}
              </button>
            </span>
            <button 
              class="btn btn-secondary" 
              :disabled="diseasePageNo >= diseaseTotalPages"
              @click="goToDiseasePage(diseasePageNo + 1)">
              下一页
            </button>
          </div>
        </div>
        <div v-else-if="filteredDiseaseData.length === 0" class="empty-message">
          <p>暂无符合条件的病种数据</p>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick, computed } from 'vue'
import * as echarts from 'echarts'
import { reportApi, type ReportBoardData, type DiseaseAnalysisData } from '@/api/report'
import { systemApi, type RuleSeasonFactor } from '@/api/system'

const loading = ref(false)
const board = ref<ReportBoardData | null>(null)

// 病种筛选和分页
const diseaseFilter = ref({
  diseaseName: '',
  minPatientCount: null as number | null,
  maxPatientCount: null as number | null,
  minStableRate: null as number | null,
  maxStableRate: null as number | null
})

const diseasePageNo = ref(1)
const pageSize = 3 // 每页显示3个病种

// 筛选后的病种数据
const filteredDiseaseData = computed(() => {
  if (!board.value?.diseaseAnalysis) return []
  
  let filtered = [...board.value.diseaseAnalysis]
  
  // 病种名称筛选
  if (diseaseFilter.value.diseaseName) {
    const keyword = diseaseFilter.value.diseaseName.toLowerCase()
    filtered = filtered.filter(item => 
      item.disease.toLowerCase().includes(keyword)
    )
  }
  
  // 患者数范围筛选
  if (diseaseFilter.value.minPatientCount !== null) {
    filtered = filtered.filter(item => 
      item.patientCount >= diseaseFilter.value.minPatientCount!
    )
  }
  if (diseaseFilter.value.maxPatientCount !== null) {
    filtered = filtered.filter(item => 
      item.patientCount <= diseaseFilter.value.maxPatientCount!
    )
  }
  
  // 稳定率范围筛选（转换为百分比）
  if (diseaseFilter.value.minStableRate !== null) {
    filtered = filtered.filter(item => 
      item.stableRate * 100 >= diseaseFilter.value.minStableRate!
    )
  }
  if (diseaseFilter.value.maxStableRate !== null) {
    filtered = filtered.filter(item => 
      item.stableRate * 100 <= diseaseFilter.value.maxStableRate!
    )
  }
  
  return filtered
})

// 分页后的病种数据
const paginatedDiseaseData = computed(() => {
  const start = (diseasePageNo.value - 1) * pageSize
  const end = start + pageSize
  return filteredDiseaseData.value.slice(start, end)
})

// 报表视图辅助函数（代表性方案 / 因素 / 建议）
function getTopPlan(d: DiseaseAnalysisData | undefined) {
  if (!d || !d.carePlanEffectiveness || d.carePlanEffectiveness.length === 0) return null
  return d.carePlanEffectiveness[0]
}

// 获取前N个最有效的方案（默认3个）
function getTopPlans(d: DiseaseAnalysisData, count: number = 3) {
  if (!d || !d.carePlanEffectiveness || d.carePlanEffectiveness.length === 0) return []
  // 方案已经按效果评分排序，直接取前N个
  return d.carePlanEffectiveness.slice(0, count)
}

function getTopPlanName(d: DiseaseAnalysisData): string {
  const plan = getTopPlan(d)
  return plan ? plan.planName : ''
}

function getTopPlanType(d: DiseaseAnalysisData): string {
  const plan = getTopPlan(d)
  return plan ? plan.planType : ''
}

function getTopPlanPatientCount(d: DiseaseAnalysisData): number {
  const plan = getTopPlan(d)
  return plan ? plan.patientCount : 0
}

function getTopPlanScore(d: DiseaseAnalysisData): number {
  const plan = getTopPlan(d)
  return plan ? plan.effectiveness : 0
}

function getTopFactor(d: DiseaseAnalysisData | undefined) {
  if (!d || !d.deteriorationFactors || d.deteriorationFactors.length === 0) return null
  return d.deteriorationFactors[0]
}

function getTopFactorName(d: DiseaseAnalysisData): string {
  const factor = getTopFactor(d)
  return factor ? factor.factorName : ''
}

function getTopFactorImpactLabel(d: DiseaseAnalysisData): string {
  const factor = getTopFactor(d)
  if (!factor) return ''
  if (factor.impactLevel === 'high') return '高影响'
  if (factor.impactLevel === 'medium') return '中影响'
  return '低影响'
}

function getPrimaryRecommendation(d: DiseaseAnalysisData): string {
  if (!d || !d.recommendations || d.recommendations.length === 0) return ''
  return d.recommendations[0]
}

// 获取典型患者画像（返回最常见的画像）
function getTopProfile(d: DiseaseAnalysisData) {
  if (!d || !d.typicalProfiles || d.typicalProfiles.length === 0) return null
  return d.typicalProfiles[0]
}

// 总页数
const diseaseTotalPages = computed(() => {
  return Math.ceil(filteredDiseaseData.value.length / pageSize)
})

// 筛选变化处理
function handleFilterChange() {
  diseasePageNo.value = 1 // 重置到第一页
}

// 重置筛选
function resetFilter() {
  diseaseFilter.value = {
    diseaseName: '',
    minPatientCount: null,
    maxPatientCount: null,
    minStableRate: null,
    maxStableRate: null
  }
  diseasePageNo.value = 1
}

// 跳转到指定页
function goToDiseasePage(page: number | string) {
  if (typeof page === 'string') return
  if (page >= 1 && page <= diseaseTotalPages.value) {
    diseasePageNo.value = page
    // 滚动到病种分析区域顶部
    nextTick(() => {
      const section = document.querySelector('.disease-analysis-section')
      if (section) {
        section.scrollIntoView({ behavior: 'smooth', block: 'start' })
      }
    })
  }
}

// 获取分页数字列表
function getDiseasePageNumbers(): (number | string)[] {
  const current = diseasePageNo.value
  const total = diseaseTotalPages.value
  const pages: (number | string)[] = []

  if (total <= 7) {
    for (let i = 1; i <= total; i++) {
      pages.push(i)
    }
  } else {
    if (current <= 4) {
      for (let i = 1; i <= 5; i++) {
        pages.push(i)
      }
      pages.push('...')
      pages.push(total)
    } else if (current >= total - 3) {
      pages.push(1)
      pages.push('...')
      for (let i = total - 4; i <= total; i++) {
        pages.push(i)
      }
    } else {
      pages.push(1)
      pages.push('...')
      for (let i = current - 1; i <= current + 1; i++) {
        pages.push(i)
      }
      pages.push('...')
      pages.push(total)
    }
  }

  return pages
}

let alertChart: echarts.ECharts | null = null
let riskChart: echarts.ECharts | null = null

const alertTrendContainer = ref<HTMLElement | null>(null)
const riskTrendContainer = ref<HTMLElement | null>(null)

// 季节因素数据
const loadingSeason = ref(false)
const seasonRules = ref<RuleSeasonFactor[]>([])

onMounted(async () => {
  await loadData()
  await loadSeasonRules()
  // 移除resize监听，确保图表不随浏览器缩放自适应变化
})

onUnmounted(() => {
  if (alertChart) {
    alertChart.dispose()
    alertChart = null
  }
  if (riskChart) {
    riskChart.dispose()
    riskChart = null
  }
})

watch(() => board.value, async () => {
  if (board.value) {
    // 等待 DOM 更新完成后再初始化图表
    await nextTick()
    initCharts()
  }
}, { deep: true })

// 生成典型患者画像
function generateTypicalProfiles(disease: string) {
  const ageRanges = ['45-55岁', '50-65岁', '55-70岁', '60-75岁']
  const diseaseStages = ['早期', '中期', '晚期']
  const constitutions = ['气虚质', '痰湿质', '阴虚质', '阳虚质', '血瘀质']
  const comorbiditiesMap: Record<string, string[][]> = {
    '高血压': [['高血脂'], ['糖尿病'], ['高血脂', '糖尿病'], []],
    '糖尿病': [['高血压'], ['高血脂'], ['高血压', '高血脂'], ['高血压', '高血脂', '冠心病']],
    '冠心病': [['高血压'], ['高血脂'], ['高血压', '高血脂'], ['高血压', '高血脂', '糖尿病']],
    '脑卒中': [['高血压'], ['高血压', '高血脂'], ['高血压', '糖尿病'], ['高血压', '高血脂', '糖尿病']],
    '慢性肾病': [['高血压'], ['糖尿病'], ['高血压', '糖尿病'], ['高血压', '高血脂', '糖尿病']]
  }
  const lifestyleOptions = [
    ['久坐', '高盐饮食'],
    ['久坐', '缺乏运动'],
    ['高盐饮食', '缺乏运动'],
    ['久坐', '高盐饮食', '缺乏运动'],
    ['吸烟', '饮酒'],
    ['高脂饮食', '缺乏运动']
  ]
  
  const profiles = []
  const baseCount = Math.floor(Math.random() * 30) + 20
  
  // 生成2-3个典型画像
  const profileCount = Math.floor(Math.random() * 2) + 2
  for (let i = 0; i < profileCount; i++) {
    const comorbidities = comorbiditiesMap[disease]?.[i % comorbiditiesMap[disease].length] || []
    const lifestyle = lifestyleOptions[i % lifestyleOptions.length]
    
    profiles.push({
      ageRange: ageRanges[i % ageRanges.length],
      diseaseStage: diseaseStages[i % diseaseStages.length],
      constitution: constitutions[i % constitutions.length],
      comorbidities: comorbidities,
      lifestyle: lifestyle,
      typicalCount: Math.floor(baseCount * (0.6 - i * 0.15))
    })
  }
  
  // 按典型患者数量排序
  return profiles.sort((a, b) => b.typicalCount - a.typicalCount)
}

// 生成模拟的病种分析数据
function generateMockDiseaseAnalysis(): DiseaseAnalysisData[] {
  return []
}

async function loadData() {
  loading.value = true
  try {
    const result = await reportApi.getBoard()
    if (result.success && result.data) {
      board.value = result.data
      // 如果没有病种分析数据，生成模拟数据
      if (!board.value.diseaseAnalysis || board.value.diseaseAnalysis.length === 0) {
        board.value.diseaseAnalysis = []
      }
    }
  } catch (error) {
    console.error('加载看板数据失败:', error)
  } finally {
    loading.value = false
  }
}

function initCharts() {
  if (!board.value) return

  // 告警/随访趋势图
  if (alertTrendContainer.value) {
    // 如果图表已存在，先销毁
    if (alertChart) {
      alertChart.dispose()
      alertChart = null
    }
    const monthsAll = board.value.months && board.value.months.length > 0 ? board.value.months : ['暂无数据']
    const alertsAll = board.value.alertsArr && board.value.alertsArr.length > 0 ? board.value.alertsArr : [0]
    const followAll = board.value.followArr && board.value.followArr.length > 0 ? board.value.followArr : [0]
    const len1 = monthsAll.length
    const start1 = len1 > 3 ? len1 - 3 : 0
    const months = monthsAll.slice(start1)
    const alertsArr = alertsAll.slice(start1)
    const followArr = followAll.slice(start1)

    alertChart = echarts.init(alertTrendContainer.value)
    alertChart.setOption({
      // 仅美化，不改布局：图表配色/网格/tooltip 视觉增强
      color: ['#667eea', '#f6ad55'],
      tooltip: {
        trigger: 'axis',
        valueFormatter: (v: any) => String(Math.round(Number(v) || 0)),
        backgroundColor: 'rgba(15, 23, 42, 0.86)',
        borderColor: 'rgba(255, 255, 255, 0.08)',
        borderWidth: 1,
        textStyle: { color: '#e5e7eb', fontSize: 12, lineHeight: 18 },
        padding: [10, 12],
        extraCssText: 'border-radius:12px; box-shadow:0 12px 26px rgba(15,23,42,0.26);'
      },
      legend: { 
        data: ['告警数', '随访数'],
        bottom: 0,
        textStyle: { fontSize: 12, color: '#64748b' }
      },
      grid: {
        left: '10%',
        right: '4%',
        bottom: '15%',
        top: '18%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: months,
        axisLine: { lineStyle: { color: 'rgba(148,163,184,0.55)' } },
        axisTick: { lineStyle: { color: 'rgba(148,163,184,0.35)' } },
        axisLabel: { fontSize: 11, rotate: 0, color: '#94a3b8' }
      },
      yAxis: { 
        type: 'value', 
        name: '次数',
        nameLocation: 'end',
        nameRotate: 0,
        nameGap: 15,
        nameTextStyle: { fontSize: 12, padding: [0, 0, 0, 0], color: '#94a3b8' },
        minInterval: 1,
        axisLine: { lineStyle: { color: 'rgba(148,163,184,0.55)' } },
        axisTick: { lineStyle: { color: 'rgba(148,163,184,0.35)' } },
        splitLine: { lineStyle: { color: 'rgba(148,163,184,0.18)' } },
        axisLabel: { fontSize: 11, formatter: (v: any) => String(Math.round(Number(v) || 0)), color: '#94a3b8' }
      },
      series: [
        {
          name: '告警数',
          type: 'bar',
          data: alertsArr,
          barMaxWidth: 34,
          itemStyle: { borderRadius: [10, 10, 6, 6] }
        },
        {
          name: '随访数',
          type: 'line',
          smooth: 0.35,
          data: followArr,
          symbol: 'circle',
          symbolSize: 7,
          lineStyle: { width: 3, color: '#764ba2' },
          itemStyle: { color: '#764ba2' },
          areaStyle: { color: 'rgba(118, 75, 162, 0.12)' }
        }
      ]
    })
  }

  // 高危/其他趋势图
  if (riskTrendContainer.value) {
    // 如果图表已存在，先销毁
    if (riskChart) {
      riskChart.dispose()
      riskChart = null
    }
    const monthsAll2 = board.value.months && board.value.months.length > 0 ? board.value.months : ['暂无数据']
    const highAll = board.value.highArr && board.value.highArr.length > 0 ? board.value.highArr : [0]
    const otherAll = board.value.otherArr && board.value.otherArr.length > 0 ? board.value.otherArr : [0]
    const len2 = monthsAll2.length
    const start2 = len2 > 3 ? len2 - 3 : 0
    const months2 = monthsAll2.slice(start2)
    const highArr = highAll.slice(start2)
    const otherArr = otherAll.slice(start2)

    riskChart = echarts.init(riskTrendContainer.value)
    riskChart.setOption({
      // 仅美化，不改布局：图表配色/网格/tooltip 视觉增强
      color: ['#667eea', '#90cdf4'],
      tooltip: {
        trigger: 'axis',
        valueFormatter: (v: any) => String(Math.round(Number(v) || 0)),
        backgroundColor: 'rgba(15, 23, 42, 0.86)',
        borderColor: 'rgba(255, 255, 255, 0.08)',
        borderWidth: 1,
        textStyle: { color: '#e5e7eb', fontSize: 12, lineHeight: 18 },
        padding: [10, 12],
        extraCssText: 'border-radius:12px; box-shadow:0 12px 26px rgba(15,23,42,0.26);'
      },
      legend: { 
        data: ['高危', '其他'],
        bottom: 0,
        textStyle: { fontSize: 12, color: '#64748b' }
      },
      grid: {
        left: '10%',
        right: '4%',
        bottom: '15%',
        top: '18%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: months2,
        axisLine: { lineStyle: { color: 'rgba(148,163,184,0.55)' } },
        axisTick: { lineStyle: { color: 'rgba(148,163,184,0.35)' } },
        axisLabel: { fontSize: 11, rotate: 0, color: '#94a3b8' }
      },
      yAxis: { 
        type: 'value', 
        name: '人数',
        nameLocation: 'end',
        nameRotate: 0,
        nameGap: 15,
        nameTextStyle: { fontSize: 12, padding: [0, 0, 0, 0], color: '#94a3b8' },
        minInterval: 1,
        axisLine: { lineStyle: { color: 'rgba(148,163,184,0.55)' } },
        axisTick: { lineStyle: { color: 'rgba(148,163,184,0.35)' } },
        splitLine: { lineStyle: { color: 'rgba(148,163,184,0.18)' } },
        axisLabel: { fontSize: 11, formatter: (v: any) => String(Math.round(Number(v) || 0)), color: '#94a3b8' }
      },
      series: [
        {
          name: '高危',
          type: 'line',
          smooth: 0.35,
          data: highArr,
          symbol: 'circle',
          symbolSize: 7,
          lineStyle: { width: 3, color: '#667eea' },
          itemStyle: { color: '#667eea' },
          areaStyle: { color: 'rgba(102, 126, 234, 0.12)' }
        },
        {
          name: '其他',
          type: 'line',
          smooth: 0.35,
          data: otherArr,
          symbol: 'circle',
          symbolSize: 7,
          lineStyle: { width: 3, color: '#90cdf4' },
          itemStyle: { color: '#90cdf4' },
          areaStyle: { color: 'rgba(144, 205, 244, 0.12)' }
        }
      ]
    })
  }

}


// 加载季节因素数据
async function loadSeasonRules() {
  loadingSeason.value = true
  try {
    const result = await systemApi.getRules()
    if (result.success && result.data && result.data.seasonRules) {
      seasonRules.value = result.data.seasonRules
    } else {
      // 如果没有数据，生成模拟数据
      seasonRules.value = []
    }
  } catch (error) {
    console.error('加载季节因素数据失败:', error)
    // 生成模拟数据作为后备
    seasonRules.value = []
  } finally {
    loadingSeason.value = false
  }
}

// 生成模拟季节因素数据
function generateMockSeasonRules(): RuleSeasonFactor[] {
  return []
}

// 获取季节图标
function getSeasonIcon(factorName: string): string {
  if (factorName.includes('春')) return '🌸'
  if (factorName.includes('夏')) return '☀️'
  if (factorName.includes('秋')) return '🍂'
  if (factorName.includes('冬')) return '❄️'
  if (factorName.includes('高温')) return '🔥'
  if (factorName.includes('低温')) return '🧊'
  if (factorName.includes('湿度') || factorName.includes('湿')) return '💧'
  if (factorName.includes('干燥')) return '🌵'
  return '🌍'
}

// 获取季节项样式类
function getSeasonItemClass(weightDesc: string): string {
  const weight = parseFloat(weightDesc.match(/[\d.]+/)?.[0] || '0')
  if (weight >= 15) return 'season-high'
  if (weight >= 10) return 'season-medium'
  return 'season-low'
}

// 根据季节 / 天气描述，给出重点受影响的病种说明
function getSeasonDiseaseImpact(factorName: string): string {
  // 冷天气、冬季：突出心脑血管疾病恶化风险
  if (factorName.includes('冬') || factorName.includes('低温')) {
    return '心脑血管疾病（如冠心病、脑卒中）患者在天气变冷时更易出现血压波动和病情恶化，应加强保暖与血压监测'
  }
  // 高温 / 夏季：心血管负担、脱水相关
  if (factorName.includes('夏') || factorName.includes('高温')) {
    return '心血管疾病患者及老年慢病人群在高温天气下心脏负担加重，易出现胸闷、心悸等不适，应注意补水与避免暴晒'
  }
  // 潮湿：风湿、呼吸系统
  if (factorName.includes('湿') || factorName.includes('湿度')) {
    return '风湿免疫性疾病及慢性呼吸系统疾病（如慢阻肺）患者在潮湿环境下关节疼痛和呼吸道症状更易加重'
  }
  // 干燥：呼吸道疾病
  if (factorName.includes('干燥')) {
    return '慢性支气管炎、哮喘等呼吸道疾病患者在空气干燥时咳嗽、气喘等症状容易加重，应注意室内加湿与护肺'
  }
  // 春秋等其他季节：给出通用慢病提示
  if (factorName.includes('春') || factorName.includes('秋')) {
    return '高血压、糖尿病等慢性病患者在季节交替时易出现血压、血糖波动，应加强随访与用药依从性管理'
  }
  // 默认通用说明
  return '高血压、糖尿病、冠心病等慢性病患者在该类气候变化下更易出现病情波动，应结合季节因素动态调整随访与康养方案'
}

// 获取影响程度
function getImpactLevel(weightDesc: string): string {
  const weight = parseFloat(weightDesc.match(/[\d.]+/)?.[0] || '0')
  if (weight >= 15) return '高'
  if (weight >= 10) return '中'
  return '低'
}

// 获取影响程度样式类
function getImpactClass(weightDesc: string): string {
  const weight = parseFloat(weightDesc.match(/[\d.]+/)?.[0] || '0')
  if (weight >= 15) return 'impact-high'
  if (weight >= 10) return 'impact-medium'
  return 'impact-low'
}

// 获取趋势样式类
function getTrendClass(changeRate: number): string {
  if (changeRate > 0) return 'trend-up'
  if (changeRate < 0) return 'trend-down'
  return 'trend-stable'
}

// 获取趋势图标
function getTrendIcon(changeRate: number): string {
  if (changeRate > 0) return '↑'
  if (changeRate < 0) return '↓'
  return '→'
}
</script>

<style scoped>
.board-page {
  /* 固定页面宽度，避免随浏览器宽度自适应缩放；放大时通过横向滚动查看
     宽度取 1120px，和数据总览页面保持一致 */
  width: 1120px;
  min-width: 1120px;
  margin: 0 auto;
  /* 背景色透明，继承MainLayout的背景色，避免右侧出现颜色不一致 */
  background: transparent;
  padding: 20px;
  box-sizing: border-box;
  overflow-x: auto;
  overflow-y: visible;
  -webkit-overflow-scrolling: touch;
}

/* 缩小AI分析看板页面间距，让内容更紧凑 */
.board-page .page-header {
  margin-bottom: 20px;
}

/* 仅美化，不改布局：标题区视觉升级（不改变标题区位置与结构） */
.page-header {
  padding-bottom: 14px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.18);
}

.page-title {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  font-size: 26px;
  font-weight: 800;
  letter-spacing: 0.2px;
  color: #0f172a;
  margin: 0 0 6px 0;
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
  color: #6b7280;
  margin: 0;
}

.loading {
  text-align: center;
  padding: 60px 20px;
  color: #718096;
  font-size: 16px;
  font-weight: 500;
}

.loading::before {
  content: '';
  display: inline-block;
  width: 20px;
  height: 20px;
  border: 3px solid #e2e8f0;
  border-top: 3px solid #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-right: 12px;
  vertical-align: middle;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.stat-item {
  margin-bottom: 16px;
  padding: 8px 0;
  border-bottom: 1px solid rgba(226, 232, 240, 0.3);
}

.stat-item:last-child {
  border-bottom: none;
}

.stat-label {
  color: #4a5568;
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
  margin-bottom: 4px;
}

.stat-label::before {
  content: '';
  width: 3px;
  height: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 2px;
  margin-right: 8px;
}

.stat-value {
  font-weight: 700;
  font-size: 18px;
  color: #2d3748;
}

/* 模型指标样式 */
.model-metrics {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.metric-item {
  padding: 16px;
  background: rgba(247, 250, 252, 0.6);
  border-radius: 12px;
  border-left: 4px solid #667eea;
}

.metric-item:hover {
  background: rgba(247, 250, 252, 0.9);
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.1);
}

.metric-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.metric-icon {
  font-size: 20px;
  line-height: 1;
}

.metric-name {
  font-size: 14px;
  font-weight: 600;
  color: #4a5568;
}

.metric-value {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin-bottom: 8px;
}

/* 仅美化，不改布局：趋势变化率做成 badge（不改 DOM 结构） */
.metric-value.trend-up,
.metric-value.trend-down,
.metric-value.trend-stable {
  padding: 8px 12px;
  border-radius: 999px;
  display: inline-flex;
  align-items: baseline;
  width: fit-content;
  border: 1px solid rgba(226, 232, 240, 0.8);
}

.metric-value.trend-up {
  background: rgba(34, 197, 94, 0.12);
  border-color: rgba(34, 197, 94, 0.22);
  color: #166534;
}

.metric-value.trend-down {
  background: rgba(239, 68, 68, 0.12);
  border-color: rgba(239, 68, 68, 0.22);
  color: #991b1b;
}

.metric-value.trend-stable {
  background: rgba(148, 163, 184, 0.14);
  border-color: rgba(148, 163, 184, 0.22);
  color: #334155;
}

.value-number {
  font-size: 28px;
  font-weight: 700;
  color: #2d3748;
  line-height: 1;
}

.value-unit {
  font-size: 16px;
  font-weight: 600;
  color: #718096;
}

.trend-icon {
  font-size: 18px;
  font-weight: 700;
  margin-left: 4px;
}

.trend-up {
  color: #e53e3e;
}

.trend-up .trend-icon {
  color: #e53e3e;
}

.trend-down {
  color: #2f855a;
}

.trend-down .trend-icon {
  color: #2f855a;
}

.trend-stable {
  color: #718096;
}

.trend-stable .trend-icon {
  color: #718096;
}

.metric-desc {
  font-size: 12px;
  color: #718096;
  line-height: 1.5;
  margin: 0;
}

/* 本月数据统计样式 */
.month-stats {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.month-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(102, 126, 234, 0.05) 100%);
  border-radius: 10px;
  border-left: 4px solid #667eea;
}

.month-label {
  font-size: 14px;
  color: #718096;
  font-weight: 500;
}

.month-value {
  font-size: 18px;
  font-weight: 700;
  color: #2d3748;
}

.stats-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: rgba(247, 250, 252, 0.6);
  border-radius: 12px;
  border: 1px solid rgba(226, 232, 240, 0.5);
}

.stat-card:hover {
  background: rgba(247, 250, 252, 0.9);
  border-color: #667eea;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.15);
}

.stat-card-icon {
  font-size: 32px;
  line-height: 1;
  flex-shrink: 0;
}

.stat-card-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-card-label {
  /* 仅美化，不改布局：统计卡标题层级 */
  font-size: 13px;
  color: #6b7280;
  font-weight: 600;
  letter-spacing: 0.2px;
}

.stat-card-value {
  /* 仅美化，不改布局：统计卡数字强调 */
  font-size: 30px;
  font-weight: 800;
  color: #0f172a;
  line-height: 1;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.stat-card-unit {
  font-size: 12px;
  color: #a0aec0;
  font-weight: 500;
}

/* 患者风险分层样式 */
.risk-stats {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.risk-summary {
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding: 16px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(102, 126, 234, 0.05) 100%);
  border-radius: 10px;
  border-left: 4px solid #667eea;
}

.summary-label {
  font-size: 14px;
  color: #718096;
  font-weight: 500;
}

.summary-value {
  font-size: 28px;
  font-weight: 700;
  color: #2d3748;
  line-height: 1;
}

.summary-unit {
  font-size: 14px;
  color: #a0aec0;
  font-weight: 500;
}

.risk-levels {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.risk-level-item {
  padding: 16px;
  border-radius: 12px;
  border-left: 4px solid;
  transition: box-shadow 0.22s ease, border-color 0.22s ease, background 0.22s ease;
}

.risk-level-item:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.risk-high {
  background: rgba(254, 215, 215, 0.3);
  border-left-color: #e53e3e;
}

.risk-medium {
  background: rgba(254, 252, 191, 0.3);
  border-left-color: #d69e2e;
}

.risk-low {
  background: rgba(198, 246, 213, 0.3);
  border-left-color: #2f855a;
}

.risk-level-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.risk-icon {
  font-size: 18px;
  line-height: 1;
}

.risk-label {
  font-size: 14px;
  font-weight: 600;
  color: #4a5568;
}

.risk-level-value {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin-bottom: 8px;
}

.risk-number {
  font-size: 28px;
  font-weight: 700;
  color: #2d3748;
  line-height: 1;
}

.risk-unit {
  font-size: 14px;
  color: #718096;
  font-weight: 500;
}

.risk-level-desc {
  font-size: 12px;
  color: #718096;
  line-height: 1.5;
  margin: 0;
}

.text-warning {
  color: #d69e2e;
  background: linear-gradient(135deg, #fefcbf 0%, #faf089 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.text-danger {
  color: #c53030; /* 比之前更深的红色，提高可读性 */
}
.text-success {
  color: #276749; /* 比之前更深的绿色，提高可读性 */
}

.chart-container {
  width: 100%;
  height: 240px;
  min-height: 240px;
  /* 仅美化，不改布局：趋势图容器统一毛玻璃风格 */
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(10px);
  border-radius: 14px;
  padding: 10px;
  box-sizing: border-box;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
  border: 1px solid rgba(226, 232, 240, 0.9);
  overflow: hidden;
}

/* 增强卡片样式 */
.card {
  /* 仅美化，不改布局：统一卡片毛玻璃 + 阴影 */
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 24px 28px;
  box-shadow: 
    0 10px 30px rgba(102,126,234,0.08),
    0 4px 12px rgba(15,23,42,0.04);
  border: 1px solid rgba(226, 232, 240, 0.85);
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease;
  position: relative;
  overflow: hidden;
  margin-bottom: 20px;
}

.card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  opacity: 0.7;
}

.card:hover {
  transform: translateY(-2px);
  box-shadow: 
    0 14px 34px rgba(102,126,234,0.12),
    0 6px 16px rgba(15,23,42,0.06);
  border-color: rgba(102, 126, 234, 0.30);
}

/* 仍保留原有高光线，不再做显隐动画，避免“尺寸感”变化 */

.card-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 20px;
  color: #2d3748;
  display: flex;
  align-items: center;
  gap: 10px;
}

.card-title::before {
  content: '';
  width: 4px;
  height: 18px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 2px;
}

.title-icon {
  font-size: 20px;
  line-height: 1;
  flex-shrink: 0;
}

/* 网格布局增强 */
.card-grid {
  display: grid;
  grid-gap: 16px;
  margin-bottom: 24px;
}

/* AI分析看板页面卡片网格固定布局，不自适应折行或单列 */
.board-page .card-grid.cols-3 {
  grid-template-columns: repeat(3, 1fr) !important;
  grid-gap: 16px !important;
}

.board-page .card-grid.cols-2 {
  grid-template-columns: repeat(2, 1fr) !important;
  grid-gap: 16px !important;
}

.card-grid .card {
  margin-bottom: 0;
}

/* 季节因素样式 */
.season-factors {
  padding: 8px 0;
}

.season-intro {
  margin-bottom: 20px;
  padding: 12px 16px;
  background: rgba(102, 126, 234, 0.05);
  border-radius: 8px;
  border-left: 3px solid #667eea;
}

.season-intro p {
  margin: 0;
  font-size: 14px;
  color: #475569;
  line-height: 1.7;
}

.season-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.season-item {
  /* 仅美化，不改布局：季节因素卡片统一为毛玻璃 16px */
  background: rgba(255, 255, 255, 0.92);
  border-radius: 16px;
  padding: 20px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease;
  display: flex;
  align-items: flex-start;
  gap: 16px;
  position: relative;
  overflow: hidden;
}

.season-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 4px;
  height: 100%;
  background: #cbd5e0;
  transition: all 0.3s ease;
}

.season-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.08);
  border-color: rgba(102, 126, 234, 0.28);
}

.season-item:hover::before {
  background: #667eea;
}

.season-high {
  border-left-color: #e53e3e;
}

.season-high::before {
  background: #e53e3e;
}

.season-medium {
  border-left-color: #d69e2e;
}

.season-medium::before {
  background: #d69e2e;
}

.season-low {
  border-left-color: #48bb78;
}

.season-low::before {
  background: #48bb78;
}

.season-icon {
  font-size: 32px;
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(102, 126, 234, 0.1);
  border-radius: 12px;
}

.season-content {
  flex: 1;
  min-width: 0;
}

.season-name {
  font-size: 16px;
  font-weight: 700;
  color: #2d3748;
  margin: 0 0 8px 0;
}

.season-weight {
  font-size: 14px;
  color: #667eea;
  font-weight: 600;
  margin: 0 0 12px 0;
}

.season-impact {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-top: 8px;
  border-top: 1px solid rgba(226, 232, 240, 0.5);
}

.impact-label {
  font-size: 13px;
  color: #718096;
}

.impact-value {
  font-size: 13px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 12px;
}

.season-disease-impact {
  margin-top: 6px;
  font-size: 13px;
  color: #4b5563;
  line-height: 1.7;
}

.season-disease-impact .impact-label {
  font-size: 13px;
  color: #6b7280;
}

.impact-disease {
  font-weight: 500;
  color: #1f2937;
}

.impact-high {
  background: rgba(245, 101, 101, 0.1);
  color: #c53030;
}

.impact-medium {
  background: rgba(250, 240, 137, 0.2);
  color: #d69e2e;
}

.impact-low {
  background: rgba(198, 246, 213, 0.2);
  color: #2f855a;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .board-page {
    padding: 12px;
  }
  
  .card-grid.cols-2,
  .card-grid.cols-3 {
    grid-template-columns: 1fr;
  }
  
  .card {
    padding: 20px 24px;
  }
  
  .chart-container {
    height: 250px;
    padding: 12px;
  }
  
  .card-title {
    font-size: 16px;
  }
  
  .stat-value {
    font-size: 16px;
  }

  .season-grid {
    grid-template-columns: 1fr;
  }

  .model-metrics {
    gap: 16px;
  }

  .metric-item {
    padding: 14px;
  }

  .value-number {
    font-size: 24px;
  }

  .metric-desc {
    font-size: 11px;
  }

  .stats-grid {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .stat-card {
    padding: 14px;
  }

  .stat-card-value {
    font-size: 20px;
  }

  .risk-levels {
    gap: 10px;
  }

  .risk-level-item {
    padding: 14px;
  }

  .risk-number {
    font-size: 24px;
  }
}

@media (max-width: 480px) {
  .card {
    padding: 16px 20px;
  }
  
  .chart-container {
    height: 220px;
  }

  .filter-row {
    flex-direction: column;
  }

  .filter-item {
    min-width: 100%;
  }

  .filter-actions {
    width: 100%;
    justify-content: stretch;
  }

  .filter-actions .btn {
    flex: 1;
    justify-content: center;
  }

  .disease-pagination {
    flex-direction: column;
    align-items: stretch;
  }

  .page-controls {
    flex-wrap: wrap;
    justify-content: center;
  }

  .page-numbers {
    flex-wrap: wrap;
    justify-content: center;
  }
}

/* 病种统计分析样式 */
.disease-analysis-section {
  width: 100%;
}

.disease-report-card {
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 20px 24px;
  margin-bottom: 24px;
  box-shadow: 0 4px 10px rgba(15, 23, 42, 0.06);
  border: 1px solid rgba(226, 232, 240, 0.9);
}

.disease-report-header {
  margin-bottom: 16px;
}

.report-title {
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 6px;
}

.report-subtitle {
  font-size: 13px;
  color: #475569;
  line-height: 1.6;
}

.disease-report-table-wrapper {
  width: 100%;
  overflow-x: auto;
}

.disease-report-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
  table-layout: auto;
}

.disease-report-table thead {
  background: #f8fafc;
}

.disease-report-table th,
.disease-report-table td {
  padding: 10px 10px;
  border-bottom: 1px solid #e2e8f0;
  text-align: left;
  vertical-align: middle;
}

.disease-report-table th {
  font-weight: 600;
  color: #1e293b;
  border-top: 1px solid #e2e8f0;
}

.disease-report-table tbody tr:hover {
  background: #f8fafc;
}

/* 仅美化，不改布局：病种分析颜色统一（稳定/改善/恶化） */
.disease-report-table tbody td:nth-child(3) {
  color: #1d4ed8;
  font-weight: 600;
}

.disease-report-table tbody td:nth-child(4) {
  color: #166534;
  font-weight: 600;
}

.disease-report-table tbody td:nth-child(5) {
  color: #991b1b;
  font-weight: 600;
}

.col-disease .disease-name-cell {
  font-weight: 600;
  color: #1e293b;
}

.col-number {
  font-variant-numeric: tabular-nums;
  color: #1f2933;
}

.col-plan .plan-main {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}

.col-plan .plan-name {
  font-weight: 600;
  color: #1e293b;
}

.col-plan .plan-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.08);
  color: #2563eb;
}

.col-plan .plan-meta {
  font-size: 12px;
  color: #334155;
}

.col-factor .factor-tag {
  display: inline-block;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 999px;
  margin-right: 6px;
  background: rgba(248, 250, 252, 0.9);
  border: 1px solid rgba(148, 163, 184, 0.6);
  color: #0f172a;
}

.col-factor .factor-name {
  font-size: 13px;
  color: #1e293b;
  font-weight: 500;
}

.col-advice {
  color: #1e293b;
}

.col-profile {
  font-size: 12px;
}

.col-plan-comparison {
  font-size: 12px;
}

.plan-comparison-wrapper {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.plan-summary {
  padding: 8px 10px;
  border-radius: 6px;
  background: rgba(239, 246, 255, 0.9);
  border: 1px solid rgba(191, 219, 254, 0.9);
}

.plan-summary-title {
  font-size: 11px;
  color: #64748b;
  margin-bottom: 4px;
}

.plan-summary-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 2px;
  flex-wrap: wrap;
}

.plan-summary-name {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
}

.plan-summary-type {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.08);
  color: #2563eb;
}

.plan-summary-stats {
  font-size: 11px;
  color: #475569;
}

.plan-comparison-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.plan-comparison-item {
  background: rgba(247, 250, 252, 0.6);
  border-radius: 6px;
  padding: 8px;
  border-left: 3px solid #667eea;
  transition: all 0.2s ease;
}

.plan-comparison-item:hover {
  background: rgba(247, 250, 252, 0.9);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

.plan-comp-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
  flex-wrap: wrap;
}

.plan-comp-rank {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
  border-radius: 50%;
  font-size: 11px;
  font-weight: 700;
  flex-shrink: 0;
}

.plan-comp-name {
  font-weight: 600;
  color: #1e293b;
  font-size: 12px;
}

.plan-comp-type {
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 4px;
  background: rgba(59, 130, 246, 0.1);
  color: #2563eb;
  font-weight: 500;
}

.plan-comp-stats {
  display: flex;
  gap: 10px;
  margin-bottom: 6px;
  font-size: 11px;
  color: #475569;
  flex-wrap: wrap;
}

.plan-comp-score {
  font-weight: 600;
  color: #1e293b;
}

.plan-comp-count {
  color: #64748b;
}

.plan-comp-bar {
  width: 100%;
  height: 6px;
  background: rgba(226, 232, 240, 0.6);
  border-radius: 3px;
  overflow: hidden;
}

.plan-comp-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  border-radius: 3px;
  transition: width 0.3s ease;
}

.profile-display {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.profile-main {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.profile-age {
  font-weight: 600;
  color: #1e293b;
  padding: 2px 8px;
  background: rgba(102, 126, 234, 0.1);
  border-radius: 4px;
  font-size: 12px;
}

.profile-stage {
  font-weight: 500;
  color: #1e293b;
  padding: 2px 8px;
  background: rgba(72, 187, 120, 0.1);
  border-radius: 4px;
  font-size: 12px;
}

.profile-constitution {
  font-weight: 500;
  color: #1e293b;
  padding: 2px 8px;
  background: rgba(245, 158, 11, 0.1);
  border-radius: 4px;
  font-size: 12px;
}

.profile-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.profile-tag {
  font-size: 11px;
  color: #475569;
  line-height: 1.4;
}

.profile-comorbidity {
  color: #dc2626;
}

.profile-lifestyle {
  color: #64748b;
}

.profile-count {
  font-size: 11px;
  color: #64748b;
  font-weight: 500;
}

.text-muted {
  color: #64748b;
}

.empty-cell {
  text-align: center;
  color: #64748b;
  padding: 24px 8px;
}

.section-header {
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 3px solid rgba(102, 126, 234, 0.2);
}

.section-title {
  font-size: 24px;
  font-weight: 700;
  color: #2d3748;
  margin: 0 0 8px 0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.section-title::before {
  content: '';
  width: 6px;
  height: 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 3px;
}

.section-subtitle {
  font-size: 14px;
  color: #718096;
  margin: 0;
  padding-left: 18px;
}

/* 筛选区域样式 */
.disease-filter-section {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(226, 232, 240, 0.5);
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: flex-end;
}

.filter-item {
  flex: 1;
  min-width: 200px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.filter-label {
  font-size: 14px;
  font-weight: 600;
  color: #4a5568;
}

.range-inputs {
  display: flex;
  align-items: center;
  gap: 8px;
}

.input-small {
  flex: 1;
  min-width: 80px;
  padding: 10px 12px;
  font-size: 13px;
}

.range-separator {
  color: #718096;
  font-weight: 500;
  flex-shrink: 0;
}

.filter-actions {
  display: flex;
  gap: 12px;
  flex-shrink: 0;
}

.filter-actions .btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  font-size: 14px;
  white-space: nowrap;
}

/* 仅美化，不改布局：按钮/输入框统一焦点态，避免默认突兀蓝边 */
.btn {
  border-radius: 12px;
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease, background 0.22s ease;
  outline: none;
}

.btn:focus,
.btn:focus-visible {
  outline: none;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.14);
}

.btn.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  color: #ffffff;
  box-shadow: 0 10px 22px rgba(102, 126, 234, 0.18);
}

.btn.btn-primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 14px 28px rgba(102, 126, 234, 0.24);
}

.btn.btn-secondary {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(148, 163, 184, 0.38);
  color: #334155;
}

.btn.btn-secondary:hover:not(:disabled) {
  border-color: rgba(102, 126, 234, 0.40);
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.06);
  transform: translateY(-1px);
}

.input:focus {
  outline: none;
  border-color: rgba(102, 126, 234, 0.55);
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.14);
}

/* 分页样式 */
.disease-pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  margin-top: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(226, 232, 240, 0.5);
  flex-wrap: wrap;
  gap: 16px;
}

.page-info {
  font-size: 14px;
  color: #718096;
  font-weight: 500;
}

.page-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-numbers {
  display: flex;
  gap: 4px;
}

.page-btn {
  min-width: 36px;
  height: 36px;
  padding: 0 12px;
  border: 1px solid rgba(226, 232, 240, 0.8);
  background: #ffffff;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #4a5568;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.page-btn:hover:not(:disabled):not(.disabled) {
  background: rgba(102, 126, 234, 0.1);
  border-color: #667eea;
  color: #667eea;
}

.page-btn.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-color: #667eea;
  color: #ffffff;
  font-weight: 600;
}

.page-btn.disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.page-btn:disabled {
  cursor: not-allowed;
  opacity: 0.5;
  background: #f7fafc;
}

.empty-message {
  text-align: center;
  padding: 60px 20px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  margin-top: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(226, 232, 240, 0.5);
}

.empty-message p {
  font-size: 16px;
  color: #718096;
  margin: 0;
}

.disease-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 28px;
  margin-bottom: 24px;
  box-shadow: 
    0 4px 6px -1px rgba(0, 0, 0, 0.1),
    0 2px 4px -1px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.2);
  transition: all 0.3s ease;
}

.disease-card:hover {
  transform: translateY(-2px);
  box-shadow: 
    0 10px 25px -3px rgba(0, 0, 0, 0.1),
    0 4px 6px -2px rgba(0, 0, 0, 0.05);
}

.disease-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 2px solid rgba(226, 232, 240, 0.5);
}

.disease-name {
  font-size: 20px;
  font-weight: 700;
  color: #2d3748;
  margin: 0;
}

.disease-stats {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.stat-badge {
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  background: rgba(102, 126, 234, 0.1);
  color: #667eea;
  border: 1px solid rgba(102, 126, 234, 0.2);
}

.stat-success {
  background: rgba(72, 187, 120, 0.1);
  color: #2f855a;
  border-color: rgba(72, 187, 120, 0.2);
}

.stat-info {
  background: rgba(66, 153, 225, 0.1);
  color: #2c5282;
  border-color: rgba(66, 153, 225, 0.2);
}

.stat-danger {
  background: rgba(245, 101, 101, 0.1);
  color: #c53030;
  border-color: rgba(245, 101, 101, 0.2);
}

.disease-card-content {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.analysis-block {
  background: rgba(247, 250, 252, 0.5);
  border-radius: 12px;
  padding: 20px;
  border-left: 4px solid #667eea;
}

.block-title {
  font-size: 16px;
  font-weight: 700;
  color: #2d3748;
  margin: 0 0 16px 0;
}

/* 康养计划效果样式 */
.care-plan-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.care-plan-item {
  background: #ffffff;
  border-radius: 10px;
  padding: 16px;
  border: 1px solid rgba(226, 232, 240, 0.5);
  transition: all 0.2s ease;
}

.care-plan-item:hover {
  border-color: #667eea;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.1);
}

.plan-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.plan-name {
  font-size: 15px;
  font-weight: 600;
  color: #2d3748;
}

.plan-type {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 12px;
  background: rgba(102, 126, 234, 0.1);
  color: #667eea;
}

.plan-stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-bottom: 12px;
}

.plan-stat {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-label {
  font-size: 13px;
  color: #718096;
}

.stat-value {
  font-size: 14px;
  font-weight: 600;
  color: #2d3748;
}

.stat-highlight {
  color: #667eea;
  font-size: 15px;
}

.effectiveness-bar {
  width: 100%;
  height: 8px;
  background: rgba(226, 232, 240, 0.5);
  border-radius: 4px;
  overflow: hidden;
  margin-top: 8px;
}

.effectiveness-fill {
  height: 100%;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  border-radius: 4px;
  transition: width 0.3s ease;
}

/* 病情加重因素样式 */
.factor-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.factor-item {
  background: #ffffff;
  border-radius: 10px;
  padding: 14px;
  border-left: 4px solid #cbd5e0;
  transition: box-shadow 0.22s ease, border-color 0.22s ease, background 0.22s ease;
}

.factor-item:hover {
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.08);
}

.factor-high {
  border-left-color: #e53e3e;
  background: rgba(245, 101, 101, 0.05);
}

.factor-medium {
  border-left-color: #d69e2e;
  background: rgba(250, 240, 137, 0.05);
}

.factor-low {
  border-left-color: #48bb78;
  background: rgba(198, 246, 213, 0.05);
}

.factor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.factor-name {
  font-size: 14px;
  font-weight: 600;
  color: #2d3748;
}

.factor-badge {
  font-size: 11px;
  padding: 4px 10px;
  border-radius: 12px;
  font-weight: 600;
}

.badge-high {
  background: rgba(245, 101, 101, 0.2);
  color: #c53030;
}

.badge-medium {
  background: rgba(250, 240, 137, 0.3);
  color: #d69e2e;
}

.badge-low {
  background: rgba(198, 246, 213, 0.3);
  color: #2f855a;
}

.factor-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.factor-description {
  font-size: 13px;
  color: #4a5568;
  margin: 0;
  flex: 1;
}

.factor-count {
  font-size: 12px;
  color: #718096;
  margin-left: 12px;
}

/* 医生建议样式 */
.recommendations-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.recommendation-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  background: #ffffff;
  border-radius: 8px;
  border-left: 3px solid #48bb78;
}

.rec-icon {
  flex-shrink: 0;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(72, 187, 120, 0.1);
  color: #2f855a;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 700;
}

.rec-text {
  font-size: 14px;
  color: #2d3748;
  line-height: 1.6;
  flex: 1;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .disease-report-table-wrapper {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }

  .disease-report-table {
    min-width: 960px;
    font-size: 12px;
  }

  .disease-report-table th,
  .disease-report-table td {
    padding: 8px 6px;
  }

  .plan-comparison-list {
    gap: 8px;
  }

  .plan-comparison-item {
    padding: 6px;
  }

  .plan-comp-header {
    gap: 4px;
  }

  .plan-comp-stats {
    flex-direction: column;
    gap: 4px;
  }

  .profile-main {
    gap: 4px;
  }

  .filter-row {
    flex-direction: column;
  }

  .filter-item {
    min-width: 100%;
  }

  .filter-actions {
    width: 100%;
  }

  .filter-actions .btn {
    flex: 1;
  }

  .disease-pagination {
    flex-direction: column;
    gap: 16px;
  }

  .page-controls {
    width: 100%;
    justify-content: center;
  }

  .disease-card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .disease-stats {
    width: 100%;
  }

  .plan-stats {
    grid-template-columns: 1fr;
  }

  .factor-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .factor-count {
    margin-left: 0;
  }
}
</style>
