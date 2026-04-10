<template>
  <div class="board-page page-container">
    <div class="page-content">
      <div class="page-header board-header">
        <div class="title-wrap">
          <div class="title-row">
            <span class="title-badge" aria-hidden="true">
              <svg viewBox="0 0 24 24" fill="none" class="title-badge-icon">
                <path d="M4 19V5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
                <path d="M9 19V11" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
                <path d="M14 19V8" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
                <path d="M19 19V13" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
              </svg>
            </span>
            <div>
              <div class="page-title">AI专题洞察中心</div>
              <div class="page-subtitle">
                聚焦群体风险变化、哈尔滨寒地气候影响、中医辨证特征与干预成效回顾，不承担预警处置、建议下发和随访分派执行。
              </div>
            </div>
          </div>
        </div>
        <div class="header-meta" v-if="board">
          <div class="meta-pill">分析月份：{{ board.latestMonth || '—' }}</div>
          <div class="meta-pill">当前季节：{{ seasonLabel }}</div>
          <div class="meta-pill">专题病种：{{ diseaseInsights.length }} 项</div>
        </div>
      </div>

      <div v-if="loading" class="state-box">AI专题洞察数据加载中...</div>
      <div v-else-if="error" class="state-box state-error">{{ error }}</div>
      <div v-else-if="!board" class="state-box">暂无可展示的 AI 分析数据</div>
      <template v-else>
        <div class="card toolbar-card">
          <div class="toolbar-main">
            <div class="toolbar-group">
              <div class="toolbar-label">专题视角</div>
              <div class="chip-group">
                <button class="chip" type="button" @click="scrollToSection('trend')">风险趋势</button>
                <button class="chip" type="button" @click="scrollToSection('climate')">哈尔滨寒地因素</button>
                <button class="chip" type="button" @click="scrollToSection('tcm')">中医体质与证候</button>
                <button class="chip" type="button" @click="scrollToSection('effect')">干预成效</button>
                <button class="chip" type="button" @click="scrollToSection('disease')">病种专题</button>
              </div>
            </div>
            <div class="toolbar-group">
              <div class="toolbar-label">最近纳入月份</div>
              <div class="month-pills">
                <span v-for="month in recentMonths" :key="month" class="month-pill">{{ month }}</span>
              </div>
            </div>
          </div>
          <div class="toolbar-note">
            本页只做跨患者、跨病种、跨时间、跨因素的专题洞察；具体患者处理请前往患者档案、预警管理、建议下发和随访管理模块。
          </div>
        </div>

        <div class="card-grid cols-4">
          <div v-for="item in insightCards" :key="item.kicker" class="card insight-card">
            <div class="insight-top">
              <div class="insight-kicker-wrap">
                <span class="insight-icon">{{ item.icon }}</span>
                <span class="insight-kicker">{{ item.kicker }}</span>
              </div>
              <span class="insight-badge" :class="item.badgeClass">{{ item.badge }}</span>
            </div>
            <div class="insight-heading">{{ item.heading }}</div>
            <ul class="insight-points">
              <li v-for="point in item.points" :key="point" class="insight-point">{{ point }}</li>
            </ul>
            <div class="insight-foot">{{ item.foot }}</div>
          </div>
        </div>

        <div ref="trendSectionRef" class="card-grid cols-2">
          <div class="card section-card">
            <div class="section-head">
              <div>
                <div class="section-title">风险趋势联动</div>
                <div class="section-subtitle">从高危人数、异常告警和随访量的联动关系观察群体风险波动，而不是处理单条事件。</div>
              </div>
              <div class="section-caption">近 {{ recentMonths.length || 0 }} 个统计月份</div>
            </div>
            <div v-if="!hasTrendData" class="inner-empty">暂无趋势数据</div>
            <div v-else ref="trendChartRef" class="chart-box"></div>
            <div class="analysis-footer">
              <div class="footer-item">
                <span class="footer-label">高危变化</span>
                <span class="footer-value" :class="trendRateClass(board.highRiskChangeRate)">
                  {{ formatRate(board.highRiskChangeRate, true) }}
                </span>
              </div>
              <div class="footer-item">
                <span class="footer-label">告警变化</span>
                <span class="footer-value" :class="trendRateClass(board.alertChangeRate)">
                  {{ formatRate(board.alertChangeRate, true) }}
                </span>
              </div>
              <div class="footer-item">
                <span class="footer-label">本期高危占比</span>
                <span class="footer-value">{{ latestHighRiskRate }}</span>
              </div>
            </div>
          </div>

          <div class="card section-card">
            <div class="section-head">
              <div>
                <div class="section-title">风险驱动因素分析</div>
                <div class="section-subtitle">聚合各病种的加重因素，识别近期风险升高的主要驱动，而不是重复展示患者清单。</div>
              </div>
              <div class="section-caption">Top {{ topFactorCount }}</div>
            </div>
            <div v-if="factorRanking.length === 0" class="inner-empty">暂无病种加重因素数据</div>
            <div v-else ref="factorChartRef" class="chart-box"></div>
            <div class="factor-list">
              <div v-for="factor in factorRanking.slice(0, 4)" :key="factor.name" class="factor-list-item">
                <div>
                  <div class="factor-name">{{ factor.name }}</div>
                  <div class="factor-meta">涉及 {{ factor.patientCount }} 人 · 关联 {{ factor.diseases.join('、') || '综合慢病' }}</div>
                </div>
                <div class="factor-score">{{ factor.score }}</div>
              </div>
            </div>
          </div>
        </div>

        <div ref="climateSectionRef" class="card-grid cols-2">
          <div class="card section-card climate-card-large">
            <div class="section-head">
              <div>
                <div class="section-title">哈尔滨寒地气候专题</div>
                <div class="section-subtitle">
                  结合哈尔滨及北方寒地“冬季长、供暖期长、室内外温差大、冬春交替风干明显”的环境特征，观察慢病波动背景。
                </div>
              </div>
              <div class="season-chip">{{ seasonLabel }}</div>
            </div>

            <div v-if="seasonLoading" class="inner-empty">正在加载季节规则...</div>
            <div class="climate-grid">
              <div v-for="item in harbinClimateCards" :key="item.title" class="climate-item" :class="item.levelClass">
                <div class="climate-icon">{{ item.icon }}</div>
                <div class="climate-body">
                  <div class="climate-title-row">
                    <div class="climate-title">{{ item.title }}</div>
                    <span class="climate-level">{{ item.level }}</span>
                  </div>
                  <div class="climate-desc">{{ item.description }}</div>
                  <div class="climate-disease">重点关注：{{ item.diseaseHint }}</div>
                  <div class="climate-evidence">专题提示：{{ item.evidence }}</div>
                </div>
              </div>
            </div>

            <div class="harbin-footnote">
              <div class="harbin-footnote-title">地域生活方式联动观察</div>
              <ul>
                <li v-for="item in harbinLifestyleNotes" :key="item">{{ item }}</li>
              </ul>
            </div>
          </div>

          <div ref="tcmSectionRef" class="card section-card">
            <div class="section-head">
              <div>
                <div class="section-title">中医体质与证候专题</div>
                <div class="section-subtitle">展示群体层面的体质聚集、证候倾向和高频症状组合，不重复单患者画像页内容。</div>
              </div>
              <div class="section-caption">群体辨证</div>
            </div>
            <div v-if="constitutionRanking.length === 0" class="inner-empty">暂无体质画像数据</div>
            <div v-else ref="constitutionChartRef" class="chart-box chart-short"></div>

            <div class="tcm-summary" v-if="constitutionRanking.length > 0">
              <div class="tcm-summary-card">
                <div class="mini-label">高频体质</div>
                <div class="mini-value">{{ constitutionRanking[0]?.name || '—' }}</div>
                <div class="mini-desc">典型患者 {{ constitutionRanking[0]?.count || 0 }} 人</div>
              </div>
              <div class="tcm-summary-card">
                <div class="mini-label">高频证候关键词</div>
                <div class="mini-value mini-value-compact">{{ syndromeKeywords[0] || '暂无' }}</div>
                <div class="mini-desc">结合体征和主诉聚合得到</div>
              </div>
            </div>

            <div class="tag-cloud">
              <span v-for="tag in syndromeKeywords.slice(0, 10)" :key="tag" class="cloud-tag">{{ tag }}</span>
              <span v-if="syndromeKeywords.length === 0" class="empty-inline">暂无证候标签</span>
            </div>
          </div>
        </div>

        <div ref="effectSectionRef" class="card section-card effect-section">
          <div class="section-head">
            <div>
              <div class="section-title">干预成效回顾</div>
              <div class="section-subtitle">这部分只分析哪类康养/随访干预更有效，不承担建议编辑与发送动作。</div>
            </div>
            <div class="section-caption">近阶段计划效果</div>
          </div>

          <div class="effect-layout">
            <div class="effect-chart-wrap">
              <div v-if="carePlanRanking.length === 0" class="inner-empty">暂无康养计划效果数据</div>
              <div v-else ref="effectChartRef" class="chart-box chart-medium"></div>
            </div>
            <div class="effect-side">
              <div v-for="plan in carePlanRanking.slice(0, 4)" :key="plan.name" class="effect-item">
                <div class="effect-header">
                  <div class="effect-name">{{ plan.name }}</div>
                  <span class="effect-patients">{{ plan.patientCount }} 人</span>
                </div>
                <div class="effect-metrics">
                  <div class="effect-metric">
                    <span>综合效果</span>
                    <strong>{{ plan.effectiveness.toFixed(1) }}</strong>
                  </div>
                  <div class="effect-metric">
                    <span>稳定性提升</span>
                    <strong>{{ plan.stabilityImprovement.toFixed(1) }}%</strong>
                  </div>
                  <div class="effect-metric">
                    <span>平均改善天数</span>
                    <strong>{{ plan.avgImprovementDays.toFixed(1) }} 天</strong>
                  </div>
                </div>
                <div class="effect-remark">适合用于 {{ plan.focusHint }}</div>
              </div>
            </div>
          </div>
        </div>

        <div ref="diseaseSectionRef" class="card section-card disease-section">
          <div class="section-head">
            <div>
              <div class="section-title">病种专题洞察</div>
              <div class="section-subtitle">针对病种做群体专题分析：看变化、看因素、看干预重点，而不是回到患者级操作页面。</div>
            </div>
            <div class="section-caption">专题标签页</div>
          </div>

          <div v-if="diseaseInsights.length === 0" class="inner-empty">暂无病种专题分析数据</div>
          <template v-else>
            <div class="topic-tabs">
              <button
                v-for="item in diseaseInsights"
                :key="item.disease"
                type="button"
                class="topic-tab"
                :class="{ active: item.disease === selectedDisease }"
                @click="selectedDisease = item.disease"
              >
                {{ item.disease }}
              </button>
            </div>

            <div v-if="currentDisease" class="topic-body">
              <div class="topic-overview">
                <div class="topic-stat">
                  <div class="mini-label">纳管患者</div>
                  <div class="mini-value">{{ currentDisease.patientCount }}</div>
                  <div class="mini-desc">该病种专题样本规模</div>
                </div>
                <div class="topic-stat">
                  <div class="mini-label">病情稳定率</div>
                  <div class="mini-value">{{ percent(currentDisease.stableRate) }}</div>
                  <div class="mini-desc">维持稳定管理的人群占比</div>
                </div>
                <div class="topic-stat">
                  <div class="mini-label">恶化率</div>
                  <div class="mini-value text-danger">{{ percent(currentDisease.deteriorationRate) }}</div>
                  <div class="mini-desc">近期需重点解释的波动部分</div>
                </div>
                <div class="topic-stat">
                  <div class="mini-label">改善率</div>
                  <div class="mini-value text-positive">{{ percent(currentDisease.improvementRate) }}</div>
                  <div class="mini-desc">现有干预成效的直接体现</div>
                </div>
              </div>

              <div class="card-grid cols-2 nested-grid">
                <div class="topic-panel">
                  <div class="panel-title">主要加重因素</div>
                  <div class="topic-factor-list">
                    <div
                      v-for="factor in currentDisease.deteriorationFactors.slice(0, 5)"
                      :key="factor.factorName"
                      class="topic-factor-item"
                    >
                      <div>
                        <div class="factor-name">{{ factor.factorName }}</div>
                        <div class="factor-meta">{{ factor.description || '暂无说明' }}</div>
                      </div>
                      <div class="factor-right">
                        <span class="factor-level" :class="`factor-${factor.impactLevel}`">{{ levelTextMap[factor.impactLevel] }}</span>
                        <span class="factor-patients">{{ factor.patientCount }} 人</span>
                      </div>
                    </div>
                    <div v-if="currentDisease.deteriorationFactors.length === 0" class="empty-inline">暂无加重因素</div>
                  </div>

                  <div class="panel-title second-title">AI关注要点</div>
                  <div class="recommendation-list">
                    <div v-for="text in currentDisease.recommendations.slice(0, 4)" :key="text" class="recommendation-item">
                      {{ text }}
                    </div>
                    <div v-if="currentDisease.recommendations.length === 0" class="empty-inline">暂无专题建议</div>
                  </div>
                </div>

                <div class="topic-panel">
                  <div class="panel-title">典型人群画像</div>
                  <div class="profile-list">
                    <div v-for="profile in currentDisease.typicalProfiles.slice(0, 3)" :key="profile.ageRange + profile.constitution" class="profile-card">
                      <div class="profile-line"><strong>{{ profile.ageRange }}</strong> · {{ profile.diseaseStage }} · {{ profile.constitution }}</div>
                      <div class="profile-line">合并症：{{ profile.comorbidities.join('、') || '暂无' }}</div>
                      <div class="profile-line">生活方式：{{ profile.lifestyle.join('、') || '暂无' }}</div>
                      <div class="profile-count">典型患者 {{ profile.typicalCount }} 人</div>
                    </div>
                    <div v-if="currentDisease.typicalProfiles.length === 0" class="empty-inline">暂无典型画像</div>
                  </div>

                  <div class="panel-title second-title">哈尔滨地区适配提示</div>
                  <div class="adapt-list">
                    <div v-for="tip in currentDiseaseHarbinHints" :key="tip" class="adapt-item">{{ tip }}</div>
                  </div>
                </div>
              </div>
            </div>
          </template>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import {
  reportApi,
  type CarePlanEffectiveness,
  type DiseaseAnalysisData,
  type DeteriorationFactor,
  type PatientProfile,
  type ReportBoardData
} from '@/api/report'
import { systemApi, type RuleSeasonFactor } from '@/api/system'

type ImpactLevel = 'high' | 'medium' | 'low'

interface FactorInsight {
  name: string
  patientCount: number
  score: number
  diseases: string[]
  levels: ImpactLevel[]
}

interface ConstitutionInsight {
  name: string
  count: number
}

interface CarePlanInsight {
  name: string
  patientCount: number
  effectiveness: number
  stabilityImprovement: number
  avgImprovementDays: number
  focusHint: string
}

interface DiseaseTopic extends DiseaseAnalysisData {
  topicScore: number
  typicalProfiles: PatientProfile[]
}

const loading = ref(false)
const seasonLoading = ref(false)
const error = ref('')
const board = ref<ReportBoardData | null>(null)
const seasonRules = ref<RuleSeasonFactor[]>([])
const selectedDisease = ref('')

const trendSectionRef = ref<HTMLElement | null>(null)
const climateSectionRef = ref<HTMLElement | null>(null)
const tcmSectionRef = ref<HTMLElement | null>(null)
const effectSectionRef = ref<HTMLElement | null>(null)
const diseaseSectionRef = ref<HTMLElement | null>(null)

const trendChartRef = ref<HTMLElement | null>(null)
const factorChartRef = ref<HTMLElement | null>(null)
const constitutionChartRef = ref<HTMLElement | null>(null)
const effectChartRef = ref<HTMLElement | null>(null)

let trendChart: echarts.ECharts | null = null
let factorChart: echarts.ECharts | null = null
let constitutionChart: echarts.ECharts | null = null
let effectChart: echarts.ECharts | null = null

const levelTextMap: Record<ImpactLevel, string> = {
  high: '高影响',
  medium: '中影响',
  low: '低影响'
}

const impactWeightMap: Record<ImpactLevel, number> = {
  high: 3,
  medium: 2,
  low: 1
}

const diseaseInsights = computed<DiseaseTopic[]>(() => {
  const list = Array.isArray(board.value?.diseaseAnalysis) ? board.value?.diseaseAnalysis || [] : []
  return list
    .map((item) => ({
      ...item,
      topicScore: item.patientCount * (item.deteriorationRate * 1.5 + (1 - item.stableRate) * 0.8 + item.improvementRate * 0.2),
      typicalProfiles: Array.isArray(item.typicalProfiles) ? item.typicalProfiles : []
    }))
    .sort((a, b) => b.topicScore - a.topicScore)
})

const currentDisease = computed(() => {
  return diseaseInsights.value.find((item) => item.disease === selectedDisease.value) || diseaseInsights.value[0] || null
})

const recentMonths = computed(() => {
  const months = board.value?.months || []
  return months.slice(Math.max(0, months.length - 6))
})

const seasonLabel = computed(() => getSeasonLabel(inferSeason(board.value?.latestMonth || '')))

const latestRiskTotal = computed(() => {
  const b = board.value
  if (!b) return 0
  return (b.latestHighRisk || 0) + (b.latestMidRisk || 0) + (b.latestLowRisk || 0)
})

const latestHighRiskRate = computed(() => {
  const total = latestRiskTotal.value
  if (!total || !board.value) return '0%'
  return percent((board.value.latestHighRisk || 0) / total)
})

const hasTrendData = computed(() => {
  return Boolean((board.value?.months || []).length && ((board.value?.highArr || []).length || (board.value?.alertsArr || []).length))
})

const factorRanking = computed<FactorInsight[]>(() => {
  const map = new Map<string, FactorInsight>()
  for (const disease of diseaseInsights.value) {
    for (const factor of disease.deteriorationFactors || []) {
      const key = factor.factorName || '未命名因素'
      const existed = map.get(key)
      const addedScore = (impactWeightMap[factor.impactLevel] || 1) * Math.max(factor.patientCount || 0, 1)
      if (existed) {
        existed.patientCount += factor.patientCount || 0
        existed.score += addedScore
        if (!existed.diseases.includes(disease.disease)) existed.diseases.push(disease.disease)
        existed.levels.push(factor.impactLevel)
      } else {
        map.set(key, {
          name: key,
          patientCount: factor.patientCount || 0,
          score: addedScore,
          diseases: disease.disease ? [disease.disease] : [],
          levels: [factor.impactLevel]
        })
      }
    }
  }
  return Array.from(map.values()).sort((a, b) => b.score - a.score)
})

const topFactorCount = computed(() => Math.min(6, factorRanking.value.length))

const constitutionRanking = computed<ConstitutionInsight[]>(() => {
  const map = new Map<string, number>()
  for (const disease of diseaseInsights.value) {
    for (const profile of disease.typicalProfiles || []) {
      const key = profile.constitution || '未标注体质'
      map.set(key, (map.get(key) || 0) + (profile.typicalCount || 0))
    }
  }
  return Array.from(map.entries())
    .map(([name, count]) => ({ name, count }))
    .sort((a, b) => b.count - a.count)
})

const syndromeKeywords = computed<string[]>(() => {
  const bag = new Map<string, number>()
  const pushKeyword = (text: string | undefined, weight = 1) => {
    if (!text) return
    text
      .split(/[、，,；;\s]+/)
      .map((item) => item.trim())
      .filter(Boolean)
      .forEach((item) => {
        bag.set(item, (bag.get(item) || 0) + weight)
      })
  }

  for (const disease of diseaseInsights.value) {
    for (const profile of disease.typicalProfiles || []) {
      pushKeyword(profile.constitution, Math.max(profile.typicalCount || 1, 1))
      profile.comorbidities.forEach((item) => pushKeyword(item, Math.max((profile.typicalCount || 1) / 2, 1)))
      profile.lifestyle.forEach((item) => pushKeyword(item, Math.max((profile.typicalCount || 1) / 3, 1)))
    }
    for (const factor of disease.deteriorationFactors || []) {
      pushKeyword(factor.factorName, factor.patientCount || 1)
    }
  }

  return Array.from(bag.entries())
    .sort((a, b) => b[1] - a[1])
    .map(([name]) => name)
    .slice(0, 12)
})

const carePlanRanking = computed<CarePlanInsight[]>(() => {
  const map = new Map<string, {
    patientCount: number
    effectiveness: number
    stability: number
    improvementDays: number
    types: Set<string>
  }>()

  for (const disease of diseaseInsights.value) {
    for (const plan of disease.carePlanEffectiveness || []) {
      const key = plan.planType || plan.planName || '综合康养计划'
      const existed = map.get(key)
      if (existed) {
        existed.patientCount += plan.patientCount || 0
        existed.effectiveness += (plan.effectiveness || 0) * (plan.patientCount || 1)
        existed.stability += (plan.stabilityImprovement || 0) * (plan.patientCount || 1)
        existed.improvementDays += (plan.avgImprovementDays || 0) * (plan.patientCount || 1)
        existed.types.add(plan.planName || key)
      } else {
        map.set(key, {
          patientCount: plan.patientCount || 0,
          effectiveness: (plan.effectiveness || 0) * (plan.patientCount || 1),
          stability: (plan.stabilityImprovement || 0) * (plan.patientCount || 1),
          improvementDays: (plan.avgImprovementDays || 0) * (plan.patientCount || 1),
          types: new Set([plan.planName || key])
        })
      }
    }
  }

  return Array.from(map.entries())
    .map(([name, item]) => {
      const base = Math.max(item.patientCount || 1, 1)
      return {
        name,
        patientCount: item.patientCount,
        effectiveness: item.effectiveness / base,
        stabilityImprovement: item.stability / base,
        avgImprovementDays: item.improvementDays / base,
        focusHint: getPlanFocusHint(name)
      }
    })
    .sort((a, b) => b.effectiveness - a.effectiveness)
})

const harbinClimateCards = computed(() => {
  const season = inferSeason(board.value?.latestMonth || '')
  const climatePressure = getClimatePressure(board.value?.alertChangeRate || 0, board.value?.highRiskChangeRate || 0)

  const pickRuleWeight = (...keywords: string[]) => {
    const found = seasonRules.value.find((item) => keywords.some((key) => item.factorName.includes(key)))
    return found?.weightDesc || '结合当前专题走势综合研判'
  }

  const common = {
    winter: [
      {
        icon: '❄️',
        title: '严寒暴露与晨间波动',
        description: '哈尔滨冬季长、低温持续时间长，晨起外出与早高峰时段更容易诱发血压上冲、胸闷和头晕等波动。',
        diseaseHint: '高血压、冠心病、脑卒中后管理人群',
        evidence: pickRuleWeight('冬', '低温'),
        level: climatePressure,
        levelClass: getClimateLevelClass(climatePressure)
      },
      {
        icon: '🏠',
        title: '供暖期室内干燥',
        description: '北方供暖期室内空气偏干，睡眠质量、咽喉不适、夜间口干和晨起不适更容易影响慢病稳定性。',
        diseaseHint: '糖尿病、慢阻肺合并慢病、失眠和阴虚倾向人群',
        evidence: pickRuleWeight('干燥', '湿度'),
        level: '中等关注',
        levelClass: 'level-medium'
      },
      {
        icon: '🧊',
        title: '室内外温差与活动减少',
        description: '室内外温差大、冰雪天气增多时，患者户外活动减少、运动依从性下降，体重管理和血糖稳定更易受影响。',
        diseaseHint: '糖尿病、肥胖合并慢病、高血脂人群',
        evidence: '结合哈尔滨寒地出行与活动场景进行专题判断',
        level: '中等关注',
        levelClass: 'level-medium'
      }
    ],
    spring: [
      {
        icon: '🌬️',
        title: '冬春交替风干刺激',
        description: '哈尔滨春季升温快但波动大，风大且空气偏干，容易带来症状反复和体感不适。',
        diseaseHint: '高血压、呼吸系统慢病合并患者',
        evidence: pickRuleWeight('春', '干燥'),
        level: climatePressure,
        levelClass: getClimateLevelClass(climatePressure)
      },
      {
        icon: '↕️',
        title: '昼夜温差偏大',
        description: '晨晚温差大时，患者晨间自测与晚间主诉可能出现明显分化，需要关注日内波动。',
        diseaseHint: '高血压、冠心病、脑卒中后管理人群',
        evidence: '需结合晨起与晚间监测数据进行解读',
        level: '中等关注',
        levelClass: 'level-medium'
      },
      {
        icon: '🚶',
        title: '活动恢复不稳定',
        description: '北方春季开始增加外出与活动，但强度恢复不均匀，部分患者存在阶段性过量活动或依从性波动。',
        diseaseHint: '冠心病、康复期患者、老年慢病人群',
        evidence: '结合随访和康养计划成效进行观察',
        level: '一般关注',
        levelClass: 'level-low'
      }
    ],
    summer: [
      {
        icon: '🌦️',
        title: '短夏湿热与补水不足',
        description: '哈尔滨夏季相对较短，但降雨和闷热阶段会影响睡眠、血压及血糖波动，老年患者要关注补水与休息。',
        diseaseHint: '高血压、糖尿病及老年慢病人群',
        evidence: pickRuleWeight('夏', '湿'),
        level: climatePressure,
        levelClass: getClimateLevelClass(climatePressure)
      },
      {
        icon: '😴',
        title: '作息波动带来的依从性变化',
        description: '高温和出汗增多容易打乱作息节律，影响服药与饮食稳定。',
        diseaseHint: '糖尿病、心血管疾病长期用药人群',
        evidence: '与依从性、睡眠和用药记录联动观察',
        level: '中等关注',
        levelClass: 'level-medium'
      },
      {
        icon: '🍉',
        title: '饮食结构变化',
        description: '冷饮、水果和外出聚餐增多时，血糖和体重管理更容易失衡。',
        diseaseHint: '糖尿病、肥胖合并慢病人群',
        evidence: '建议结合饮食相关问卷得分解读',
        level: '一般关注',
        levelClass: 'level-low'
      }
    ],
    autumn: [
      {
        icon: '🍂',
        title: '入秋降温启动阶段',
        description: '哈尔滨秋季转凉较快，慢病患者在季节切换初期更容易出现血压、睡眠和情绪波动。',
        diseaseHint: '高血压、冠心病、焦虑失眠倾向人群',
        evidence: pickRuleWeight('秋', '低温'),
        level: climatePressure,
        levelClass: getClimateLevelClass(climatePressure)
      },
      {
        icon: '🌡️',
        title: '早晚温差加大',
        description: '晨晚体感差异扩大后，晨间监测数值与白天症状感受可能明显不同，需关注测量时段。',
        diseaseHint: '高血压、脑卒中后管理人群',
        evidence: '适合与晨间监测专题联动分析',
        level: '中等关注',
        levelClass: 'level-medium'
      },
      {
        icon: '🥘',
        title: '入冬前饮食偏厚重',
        description: '气温下降后，北方地区饮食更偏高热量和重口味，体重、血脂和血糖管理压力会提前显现。',
        diseaseHint: '糖尿病、高脂血症、肥胖合并慢病人群',
        evidence: '结合体重、饮食问卷和血脂趋势综合判断',
        level: '一般关注',
        levelClass: 'level-low'
      }
    ]
  }

  return common[season]
})

const harbinLifestyleNotes = computed(() => {
  const disease = currentDisease.value?.disease || '当前重点病种'
  return [
    `哈尔滨及周边北方寒地冬季户外活动受限更明显，${disease} 人群的运动依从性下降往往会先体现在体重、睡眠和晨间指标波动上。`,
    '供暖期室内外温差大且空气偏干，建议在专题分析时特别关注晨间血压、夜间睡眠、口干和呼吸道不适等联动信号。',
    '北方寒地场景下饮食偏咸、偏热量补充的阶段性特征较明显，若体重、血糖、血脂与问卷得分同步波动，可优先从饮食和作息专题解释。'
  ]
})

const currentDiseaseHarbinHints = computed(() => {
  const disease = currentDisease.value?.disease || '慢病'
  if (disease.includes('高血压')) {
    return [
      '哈尔滨冬季晨间寒冷刺激更容易诱发血压上冲，专题分析时优先看晨起与夜间监测差值。',
      '若患者近期户外活动显著减少，可联动观察体重、睡眠和情绪评分对血压稳定性的影响。',
      '供暖期室内干燥和睡眠质量下降，往往会成为高血压患者头晕、心悸和晨间不适的重要背景因素。'
    ]
  }
  if (disease.includes('糖尿病')) {
    return [
      '冬季活动减少和高热量饮食叠加时，哈尔滨地区糖尿病患者更容易出现空腹血糖和体重的同步波动。',
      '供暖环境造成的口干、饮水节律变化，需要与血糖变化一起看，避免只从单一体征解释。',
      '节庆聚餐和季节性饮食变化在北方地区更明显，专题复盘时建议优先关联饮食问卷和康养计划成效。'
    ]
  }
  if (disease.includes('冠心病') || disease.includes('脑卒中')) {
    return [
      '寒冷暴露和早晚温差是哈尔滨北方地区心脑血管慢病的重要背景因素，专题分析时优先看天气转换期波动。',
      '冰雪天气导致外出减少后，恢复期患者容易出现活动量断崖式下降，应与随访完成度一起观察。',
      '如果近期睡眠差、胸闷或头晕主诉同步升高，可优先从寒冷刺激、供暖干燥和依从性三方面解释。'
    ]
  }
  return [
    `哈尔滨寒地场景下，${disease} 的专题分析应优先考虑低温、温差、供暖干燥和冬季活动减少的共同作用。`,
    '若体征波动与问卷症状在冬春交替阶段同时加重，通常比单一时间点异常更具有解释价值。',
    '北方饮食与季节性活动节律变化较大，建议在病种专题中联合查看饮食、睡眠和康养计划效果。'
  ]
})

const symptomKeywords = computed(() => {
  const commonSymptoms = ['头晕', '乏力', '失眠', '胸闷', '心悸', '口干', '畏寒', '头痛', '气短', '咳嗽', '水肿', '夜尿', '胸痛', '眩晕']
  const bag = new Map<string, number>()
  const collect = (text?: string, weight = 1) => {
    if (!text) return
    for (const symptom of commonSymptoms) {
      if (text.includes(symptom)) {
        bag.set(symptom, (bag.get(symptom) || 0) + weight)
      }
    }
  }

  for (const disease of diseaseInsights.value) {
    for (const factor of disease.deteriorationFactors || []) {
      collect(factor.factorName, Math.max(factor.patientCount || 1, 1))
      collect(factor.description, Math.max((factor.patientCount || 1) / 2, 1))
    }
    for (const recommendation of disease.recommendations || []) {
      collect(recommendation, 1)
    }
  }

  const fallback = ['头晕', '乏力', '失眠', '胸闷']
  const merged = Array.from(bag.entries())
    .sort((a, b) => b[1] - a[1])
    .map(([name]) => name)

  return Array.from(new Set([...merged, ...fallback])).slice(0, 6)
})

const insightCards = computed(() => {
  const focusDisease = diseaseInsights.value[0]
  const secondDisease = diseaseInsights.value[1]
  const topFactor = factorRanking.value[0]
  const topConstitution = constitutionRanking.value[0]
  const secondConstitution = constitutionRanking.value[1]
  const symptoms = symptomKeywords.value

  const focusDiseasePoints = Array.from(new Set([
    focusDisease ? getDiseaseFocusSentence(focusDisease, 0) : '',
    secondDisease ? getDiseaseFocusSentence(secondDisease, 1) : ''
  ].filter(Boolean).concat([
    '高血压风险波动最明显',
    '糖尿病近期血糖离散度上升'
  ]))).slice(0, 2)

  const riskDriverPoints = Array.from(new Set(
    factorRanking.value
      .slice(0, 8)
      .map((item) => normalizeDriverPoint(item.name))
      .filter(Boolean)
      .concat(['晨间血压异常', '睡眠下降', '用药依从性不足', '症状评分上升'])
  )).slice(0, 4)

  const climatePoints = [
    focusDisease?.disease?.includes('高血压')
      ? '低温阶段高血压患者异常率上升'
      : `低温阶段${focusDisease?.disease || '重点慢病'}波动更明显`,
    '昼夜温差较大时晨起不适更集中'
  ]

  const constitutionText = [topConstitution?.name, secondConstitution?.name].filter(Boolean).slice(0, 2).join('、') || '气虚质、痰湿质'
  const symptomText = symptoms.slice(0, 4).join('、') || '头晕、乏力、失眠、胸闷'

  return [
    {
      icon: '🩺',
      kicker: '结论卡 01',
      badge: focusDisease ? '病种聚焦' : '默认示例',
      badgeClass: 'badge-warn',
      heading: '本周期重点关注病种',
      points: focusDiseasePoints,
      foot: focusDisease
        ? `按恶化率、纳管规模与波动强度综合判断，当前最值得持续跟踪的是 ${focusDisease.disease}。`
        : '当前病种数据不足时，先展示默认关注病种示例。'
    },
    {
      icon: '📈',
      kicker: '结论卡 02',
      badge: topFactor ? '驱动聚焦' : '默认示例',
      badgeClass: 'badge-danger',
      heading: '主要风险驱动因素',
      points: riskDriverPoints,
      foot: topFactor
        ? `以上因素由近阶段加重因素聚合归纳得到，可作为解释群体风险抬升的优先线索。`
        : '当前驱动因素不足时，先展示医生最常用的解释型示例。'
    },
    {
      icon: '❄️',
      kicker: '结论卡 03',
      badge: '哈尔滨寒地',
      badgeClass: 'badge-cold',
      heading: '寒地影响结论',
      points: climatePoints,
      foot: `结合哈尔滨及北方地区低温持续、供暖期长、室内外温差大的特征进行专题提示。`
    },
    {
      icon: '🌿',
      kicker: '结论卡 04',
      badge: topConstitution ? '辨证聚焦' : '默认示例',
      badgeClass: 'badge-positive',
      heading: '中医辨证提示',
      points: [
        `${constitutionText}患者近期波动更多`,
        `${symptomText}为高频组合症状`
      ],
      foot: topConstitution
        ? '结合群体体质分布、证候关键词与高频症状聚合得出，用于辅助医生做群体辨证判断。'
        : '当前辨证数据不足时，先展示常见体质与症状组合示例。'
    }
  ]
})

watch(
  diseaseInsights,
  (list) => {
    if (!list.length) {
      selectedDisease.value = ''
      return
    }
    if (!selectedDisease.value || !list.some((item) => item.disease === selectedDisease.value)) {
      selectedDisease.value = list[0].disease
    }
  },
  { immediate: true }
)

watch(
  [board, seasonRules, selectedDisease],
  async () => {
    await nextTick()
    initCharts()
  },
  { deep: true }
)

onMounted(async () => {
  await Promise.all([loadBoard(), loadSeasonRules()])
  await nextTick()
  initCharts()
  window.addEventListener('resize', resizeCharts)
})

onUnmounted(() => {
  disposeCharts()
  window.removeEventListener('resize', resizeCharts)
})

async function loadBoard() {
  loading.value = true
  error.value = ''
  try {
    const result = await reportApi.getBoard()
    if (result.success && result.data) {
      board.value = result.data
    } else {
      board.value = null
      error.value = result.message || '加载 AI 专题数据失败'
    }
  } catch (err: any) {
    board.value = null
    error.value = err?.message || '加载 AI 专题数据失败'
  } finally {
    loading.value = false
  }
}

async function loadSeasonRules() {
  seasonLoading.value = true
  try {
    const result = await systemApi.getRules()
    if (result.success && result.data?.seasonRules) {
      seasonRules.value = result.data.seasonRules
    } else {
      seasonRules.value = []
    }
  } catch {
    seasonRules.value = []
  } finally {
    seasonLoading.value = false
  }
}

function scrollToSection(target: 'trend' | 'climate' | 'tcm' | 'effect' | 'disease') {
  const map = {
    trend: trendSectionRef.value,
    climate: climateSectionRef.value,
    tcm: tcmSectionRef.value,
    effect: effectSectionRef.value,
    disease: diseaseSectionRef.value
  }
  map[target]?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function initCharts() {
  renderTrendChart()
  renderFactorChart()
  renderConstitutionChart()
  renderEffectChart()
}

function resizeCharts() {
  trendChart?.resize()
  factorChart?.resize()
  constitutionChart?.resize()
  effectChart?.resize()
}

function disposeCharts() {
  trendChart?.dispose()
  factorChart?.dispose()
  constitutionChart?.dispose()
  effectChart?.dispose()
  trendChart = null
  factorChart = null
  constitutionChart = null
  effectChart = null
}

function renderTrendChart() {
  if (!trendChartRef.value || !board.value || !hasTrendData.value) {
    trendChart?.dispose()
    trendChart = null
    return
  }
  trendChart?.dispose()
  trendChart = echarts.init(trendChartRef.value)
  const months = recentMonths.value
  const allMonths = board.value.months || []
  const startIndex = Math.max(allMonths.length - months.length, 0)
  const high = (board.value.highArr || []).slice(startIndex)
  const other = (board.value.otherArr || []).slice(startIndex)
  const alerts = (board.value.alertsArr || []).slice(startIndex)
  const followups = (board.value.followArr || []).slice(startIndex)

  trendChart.setOption({
    color: ['#4456d9', '#90cdf4', '#f97316', '#0ea5e9'],
    tooltip: sharedTooltip(),
    legend: {
      bottom: 0,
      textStyle: { color: '#64748b', fontSize: 12 },
      data: ['高危', '其他风险', '异常告警', '随访量']
    },
    grid: { left: 36, right: 24, top: 24, bottom: 48, containLabel: true },
    xAxis: {
      type: 'category',
      data: months,
      axisLine: { lineStyle: { color: 'rgba(148,163,184,0.45)' } },
      axisLabel: { color: '#94a3b8' }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLine: { show: false },
      splitLine: { lineStyle: { color: 'rgba(148,163,184,0.15)' } },
      axisLabel: { color: '#94a3b8' }
    },
    series: [
      {
        name: '高危',
        type: 'line',
        smooth: 0.35,
        symbolSize: 8,
        data: high,
        lineStyle: { width: 3 },
        areaStyle: { color: 'rgba(68,86,217,0.12)' }
      },
      {
        name: '其他风险',
        type: 'line',
        smooth: 0.35,
        symbolSize: 7,
        data: other,
        lineStyle: { width: 2.5 },
        areaStyle: { color: 'rgba(144,205,244,0.10)' }
      },
      {
        name: '异常告警',
        type: 'bar',
        data: alerts,
        barMaxWidth: 26,
        itemStyle: { borderRadius: [8, 8, 4, 4] }
      },
      {
        name: '随访量',
        type: 'line',
        smooth: 0.25,
        symbolSize: 6,
        data: followups,
        lineStyle: { width: 2.5, type: 'dashed' }
      }
    ]
  })
}

function renderFactorChart() {
  if (!factorChartRef.value || factorRanking.value.length === 0) {
    factorChart?.dispose()
    factorChart = null
    return
  }
  factorChart?.dispose()
  factorChart = echarts.init(factorChartRef.value)
  const list = factorRanking.value.slice(0, topFactorCount.value).reverse()
  factorChart.setOption({
    tooltip: sharedTooltip(),
    grid: { left: 96, right: 20, top: 18, bottom: 18 },
    xAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: 'rgba(148,163,184,0.15)' } },
      axisLabel: { color: '#94a3b8' }
    },
    yAxis: {
      type: 'category',
      data: list.map((item) => item.name),
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#475569', width: 80, overflow: 'truncate' }
    },
    series: [
      {
        type: 'bar',
        data: list.map((item) => item.score),
        barWidth: 18,
        itemStyle: {
          borderRadius: [0, 10, 10, 0],
          color: new echarts.graphic.LinearGradient(1, 0, 0, 0, [
            { offset: 0, color: '#f97316' },
            { offset: 1, color: '#fb7185' }
          ])
        }
      }
    ]
  })
}

function renderConstitutionChart() {
  if (!constitutionChartRef.value || constitutionRanking.value.length === 0) {
    constitutionChart?.dispose()
    constitutionChart = null
    return
  }
  constitutionChart?.dispose()
  constitutionChart = echarts.init(constitutionChartRef.value)
  const list = constitutionRanking.value.slice(0, 6)
  constitutionChart.setOption({
    tooltip: sharedTooltip(),
    legend: {
      bottom: 0,
      textStyle: { color: '#64748b' }
    },
    series: [
      {
        type: 'pie',
        radius: ['42%', '68%'],
        center: ['50%', '42%'],
        avoidLabelOverlap: true,
        itemStyle: {
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          color: '#475569',
          formatter: '{b}\n{d}%'
        },
        data: list.map((item) => ({ name: item.name, value: item.count }))
      }
    ]
  })
}

function renderEffectChart() {
  if (!effectChartRef.value || carePlanRanking.value.length === 0) {
    effectChart?.dispose()
    effectChart = null
    return
  }
  effectChart?.dispose()
  effectChart = echarts.init(effectChartRef.value)
  const list = carePlanRanking.value.slice(0, 5).reverse()
  effectChart.setOption({
    tooltip: sharedTooltip(),
    grid: { left: 108, right: 28, top: 18, bottom: 18 },
    xAxis: {
      type: 'value',
      max: 100,
      splitLine: { lineStyle: { color: 'rgba(148,163,184,0.15)' } },
      axisLabel: { color: '#94a3b8' }
    },
    yAxis: {
      type: 'category',
      data: list.map((item) => item.name),
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#475569', width: 90, overflow: 'truncate' }
    },
    series: [
      {
        name: '综合效果',
        type: 'bar',
        data: list.map((item) => Number(item.effectiveness.toFixed(1))),
        barWidth: 18,
        itemStyle: {
          borderRadius: [0, 10, 10, 0],
          color: new echarts.graphic.LinearGradient(1, 0, 0, 0, [
            { offset: 0, color: '#22c55e' },
            { offset: 1, color: '#14b8a6' }
          ])
        }
      }
    ]
  })
}

function sharedTooltip() {
  return {
    trigger: 'axis',
    axisPointer: { type: 'shadow' },
    backgroundColor: 'rgba(15, 23, 42, 0.92)',
    borderColor: 'rgba(255,255,255,0.08)',
    borderWidth: 1,
    textStyle: { color: '#e2e8f0', fontSize: 12 },
    extraCssText: 'border-radius: 12px; box-shadow: 0 12px 28px rgba(15,23,42,0.28);'
  }
}

function percent(value?: number | null) {
  if (value == null || Number.isNaN(Number(value))) return '0%'
  return `${Math.round(Number(value) * 100)}%`
}

function formatRate(value?: number | null, withSign = false) {
  const num = Number(value || 0)
  const pct = `${Math.round(Math.abs(num) * 100)}%`
  if (!withSign) return pct
  if (num > 0) return `+${pct}`
  if (num < 0) return `-${pct}`
  return '0%'
}

function trendRateClass(value?: number | null) {
  const num = Number(value || 0)
  if (num > 0) return 'text-danger'
  if (num < 0) return 'text-positive'
  return 'text-neutral'
}

function inferSeason(monthText: string): 'spring' | 'summer' | 'autumn' | 'winter' {
  const match = monthText.match(/(\d{1,2})/) || monthText.match(/-(\d{1,2})/) || monthText.match(/\/(\d{1,2})/)
  const month = Number(match?.[1])
  if ([3, 4, 5].includes(month)) return 'spring'
  if ([6, 7, 8].includes(month)) return 'summer'
  if ([9, 10, 11].includes(month)) return 'autumn'
  return 'winter'
}

function getSeasonLabel(season: 'spring' | 'summer' | 'autumn' | 'winter') {
  const map = {
    spring: '春季 / 冬春交替期',
    summer: '夏季 / 短夏波动期',
    autumn: '秋季 / 入冷过渡期',
    winter: '冬季 / 北方严寒期'
  }
  return map[season]
}

function getClimatePressure(alertRate: number, highRiskRate: number) {
  const score = Math.abs(alertRate || 0) * 100 + Math.abs(highRiskRate || 0) * 100
  if (score >= 25) return '高关注'
  if (score >= 10) return '中等关注'
  return '一般关注'
}

function getClimateLevelClass(level: string) {
  if (level.includes('高')) return 'level-high'
  if (level.includes('中')) return 'level-medium'
  return 'level-low'
}

function getPlanFocusHint(name: string) {
  if (name.includes('饮食')) return '饮食控制、体重管理和血糖波动人群'
  if (name.includes('运动')) return '活动不足、恢复期和体重上升人群'
  if (name.includes('用药')) return '依从性不足和长期用药管理人群'
  if (name.includes('中医')) return '体质偏颇、症状持续波动人群'
  return '综合慢病稳定管理人群'
}

function getDiseaseFocusSentence(disease: DiseaseAnalysisData, index = 0) {
  const name = disease?.disease || '综合慢病'
  const factorNames = (disease?.deteriorationFactors || []).map((item) => item.factorName).join('、')
  if (name.includes('高血压')) {
    return index === 0 ? '高血压风险波动最明显' : '高血压晨间血压波动更集中'
  }
  if (name.includes('糖尿病')) {
    if (factorNames.includes('血糖') || factorNames.includes('饮食')) return '糖尿病近期血糖离散度上升'
    return '糖尿病近期饮食与监测稳定性下降'
  }
  if (name.includes('冠心病')) return '冠心病近期晨晚症状波动更值得关注'
  if (name.includes('脑卒中')) return '脑卒中后管理人群近期恢复稳定性下降'
  return `${name}近期群体波动较为明显`
}

function normalizeDriverPoint(text: string) {
  if (!text) return ''
  if (text.includes('血压')) return '晨间血压异常'
  if (text.includes('睡眠')) return '睡眠下降'
  if (text.includes('用药') || text.includes('依从')) return '用药依从性不足'
  if (text.includes('症状') || text.includes('问卷') || text.includes('头晕') || text.includes('胸闷') || text.includes('乏力')) return '症状评分上升'
  if (text.includes('血糖')) return '血糖波动增大'
  if (text.includes('饮食')) return '饮食控制波动'
  if (text.includes('运动')) return '运动依从性下降'
  return text
}
</script>

<style scoped>
.board-page {
  padding-bottom: 28px;
}

.board-header {
  justify-content: space-between;
  align-items: flex-start;
}

.title-row {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}

.title-badge {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.14), rgba(59, 130, 246, 0.2));
  color: #4f46e5;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-shadow: inset 0 0 0 1px rgba(99, 102, 241, 0.16);
}

.title-badge-icon {
  width: 22px;
  height: 22px;
}

.header-meta {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.meta-pill,
.month-pill,
.chip,
.season-chip,
.insight-tag,
.cloud-tag,
.topic-tab,
.climate-level,
.factor-level,
.insight-badge {
  border-radius: 999px;
}

.meta-pill {
  padding: 8px 12px;
  font-size: 12px;
  color: #475569;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(148, 163, 184, 0.16);
}

.state-box {
  background: rgba(255, 255, 255, 0.94);
  border-radius: 16px;
  padding: 40px 20px;
  text-align: center;
  color: #64748b;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);
}

.state-error {
  color: #b91c1c;
}

.toolbar-card {
  margin-bottom: 20px;
}

.toolbar-main {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  flex-wrap: wrap;
}

.toolbar-group {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  flex-direction: column;
}

.toolbar-label {
  font-size: 13px;
  color: #475569;
  font-weight: 600;
}

.toolbar-note {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px dashed rgba(148, 163, 184, 0.22);
  color: #64748b;
  font-size: 13px;
}

.chip-group,
.month-pills,
.topic-tabs,
.insight-tags,
.tag-cloud {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.chip {
  border: 1px solid rgba(99, 102, 241, 0.16);
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.08), rgba(59, 130, 246, 0.08));
  color: #4338ca;
  padding: 8px 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.chip:hover,
.topic-tab:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 22px rgba(79, 70, 229, 0.12);
}

.month-pill {
  padding: 7px 12px;
  background: rgba(241, 245, 249, 0.92);
  color: #475569;
  font-size: 12px;
  border: 1px solid rgba(148, 163, 184, 0.14);
}

.insight-card {
  min-height: 224px;
}

.insight-top {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}

.insight-kicker-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.insight-icon {
  font-size: 18px;
}

.insight-kicker {
  font-size: 13px;
  font-weight: 700;
  color: #475569;
}

.insight-badge {
  padding: 6px 10px;
  font-size: 12px;
  font-weight: 700;
}

.badge-warn {
  color: #b45309;
  background: rgba(251, 191, 36, 0.18);
}

.badge-danger {
  color: #b91c1c;
  background: rgba(248, 113, 113, 0.16);
}

.badge-cold {
  color: #1d4ed8;
  background: rgba(96, 165, 250, 0.16);
}

.badge-positive {
  color: #047857;
  background: rgba(52, 211, 153, 0.16);
}

.insight-heading {
  margin-top: 18px;
  font-size: 19px;
  line-height: 1.45;
  font-weight: 800;
  color: #0f172a;
}

.insight-points {
  margin: 14px 0 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 10px;
}

.insight-point {
  position: relative;
  padding-left: 18px;
  font-size: 14px;
  line-height: 1.75;
  color: #334155;
}

.insight-point::before {
  content: '';
  position: absolute;
  left: 0;
  top: 9px;
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.9), rgba(59, 130, 246, 0.75));
  box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.08);
}

.insight-foot {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed rgba(148, 163, 184, 0.18);
  font-size: 12px;
  color: #64748b;
  line-height: 1.7;
}

.insight-tags {
  margin-top: 14px;
}

.insight-tag,
.cloud-tag {
  padding: 6px 10px;
  font-size: 12px;
  background: rgba(241, 245, 249, 0.96);
  color: #475569;
  border: 1px solid rgba(148, 163, 184, 0.14);
}

.section-card {
  overflow: visible;
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 16px;
}

.section-title {
  font-size: 20px;
  font-weight: 800;
  color: #0f172a;
}

.section-subtitle {
  margin-top: 6px;
  font-size: 13px;
  color: #64748b;
  line-height: 1.7;
}

.section-caption,
.season-chip {
  white-space: nowrap;
  padding: 7px 12px;
  font-size: 12px;
  color: #4338ca;
  background: rgba(224, 231, 255, 0.66);
  border: 1px solid rgba(99, 102, 241, 0.14);
}

.chart-box {
  width: 100%;
  height: 320px;
}

.chart-short {
  height: 280px;
}

.chart-medium {
  height: 300px;
}

.inner-empty,
.empty-inline {
  color: #94a3b8;
  font-size: 13px;
}

.inner-empty {
  min-height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px dashed rgba(148, 163, 184, 0.26);
  border-radius: 16px;
  background: rgba(248, 250, 252, 0.72);
}

.analysis-footer {
  margin-top: 10px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.footer-item,
.tcm-summary-card,
.topic-stat {
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(248, 250, 252, 0.92);
  border: 1px solid rgba(148, 163, 184, 0.12);
}

.footer-label,
.mini-label {
  font-size: 12px;
  color: #64748b;
}

.footer-value,
.mini-value {
  margin-top: 6px;
  font-size: 22px;
  font-weight: 800;
  color: #0f172a;
}

.mini-desc {
  margin-top: 8px;
  font-size: 12px;
  color: #64748b;
  line-height: 1.6;
}

.mini-value-compact {
  font-size: 18px;
}

.text-danger {
  color: #b91c1c;
}

.text-positive {
  color: #047857;
}

.text-neutral {
  color: #475569;
}

.factor-list {
  margin-top: 12px;
  display: grid;
  gap: 10px;
}

.factor-list-item,
.topic-factor-item,
.effect-item,
.recommendation-item,
.adapt-item,
.profile-card,
.climate-item {
  background: rgba(248, 250, 252, 0.95);
  border: 1px solid rgba(148, 163, 184, 0.12);
  border-radius: 16px;
}

.factor-list-item {
  padding: 12px 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.factor-name {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.factor-meta,
.profile-line,
.climate-desc,
.climate-disease,
.climate-evidence,
.effect-remark {
  margin-top: 6px;
  font-size: 12px;
  color: #64748b;
  line-height: 1.7;
}

.factor-score {
  min-width: 48px;
  text-align: right;
  font-size: 20px;
  font-weight: 800;
  color: #ea580c;
}

.climate-grid {
  display: grid;
  gap: 14px;
}

.climate-item {
  padding: 16px;
  display: flex;
  gap: 14px;
  align-items: flex-start;
}

.climate-icon {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.82);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
  box-shadow: inset 0 0 0 1px rgba(148, 163, 184, 0.12);
}

.climate-body {
  flex: 1;
}

.climate-title-row {
  display: flex;
  gap: 10px;
  align-items: center;
  justify-content: space-between;
}

.climate-title {
  font-size: 15px;
  font-weight: 800;
  color: #0f172a;
}

.climate-level {
  padding: 6px 10px;
  font-size: 12px;
  font-weight: 700;
}

.level-high {
  background: rgba(248, 113, 113, 0.14);
}

.level-high .climate-level,
.factor-high {
  background: rgba(248, 113, 113, 0.16);
  color: #b91c1c;
}

.level-medium {
  background: rgba(251, 191, 36, 0.12);
}

.level-medium .climate-level,
.factor-medium {
  background: rgba(251, 191, 36, 0.18);
  color: #b45309;
}

.level-low {
  background: rgba(96, 165, 250, 0.10);
}

.level-low .climate-level,
.factor-low {
  background: rgba(96, 165, 250, 0.16);
  color: #1d4ed8;
}

.harbin-footnote {
  margin-top: 16px;
  padding: 16px 18px;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(239, 246, 255, 0.92), rgba(248, 250, 252, 0.92));
  border: 1px solid rgba(96, 165, 250, 0.16);
}

.harbin-footnote-title,
.panel-title {
  font-size: 15px;
  font-weight: 800;
  color: #0f172a;
}

.harbin-footnote ul {
  margin: 10px 0 0;
  padding-left: 18px;
  color: #475569;
}

.harbin-footnote li + li {
  margin-top: 8px;
}

.tcm-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 12px;
}

.tag-cloud {
  margin-top: 14px;
}

.effect-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(300px, 0.95fr);
  gap: 18px;
}

.effect-side {
  display: grid;
  gap: 12px;
}

.effect-item {
  padding: 16px;
}

.effect-header,
.effect-metrics,
.topic-factor-item,
.profile-card {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.effect-name {
  font-size: 15px;
  font-weight: 800;
  color: #0f172a;
}

.effect-patients {
  color: #475569;
  font-size: 12px;
}

.effect-metrics {
  margin-top: 12px;
}

.effect-metric {
  flex: 1;
  min-width: 0;
}

.effect-metric span {
  display: block;
  font-size: 12px;
  color: #64748b;
}

.effect-metric strong {
  display: block;
  margin-top: 6px;
  font-size: 18px;
  color: #0f172a;
}

.topic-tabs {
  margin-bottom: 16px;
}

.topic-tab {
  border: 1px solid rgba(148, 163, 184, 0.16);
  background: rgba(248, 250, 252, 0.94);
  color: #475569;
  padding: 9px 16px;
  cursor: pointer;
  font-weight: 700;
}

.topic-tab.active {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.14), rgba(59, 130, 246, 0.14));
  color: #3730a3;
  border-color: rgba(99, 102, 241, 0.24);
}

.topic-overview {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.topic-body {
  display: grid;
  gap: 16px;
}

.nested-grid {
  margin-top: 0;
  padding-left: 0;
  padding-right: 0;
}

.topic-panel {
  padding: 18px;
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.7);
  border: 1px solid rgba(148, 163, 184, 0.12);
}

.topic-factor-list,
.recommendation-list,
.profile-list,
.adapt-list {
  display: grid;
  gap: 10px;
}

.topic-factor-list {
  margin-top: 12px;
}

.topic-factor-item {
  padding: 12px 14px;
  align-items: center;
}

.factor-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.factor-level {
  padding: 6px 10px;
  font-size: 12px;
  font-weight: 700;
}

.factor-patients,
.profile-count {
  font-size: 12px;
  color: #64748b;
}

.second-title {
  margin-top: 18px;
}

.recommendation-item,
.adapt-item,
.profile-card {
  padding: 12px 14px;
  line-height: 1.75;
  color: #475569;
}

.profile-card {
  display: block;
}

@media (max-width: 1280px) {
  .card-grid.cols-4 {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .effect-layout {
    grid-template-columns: 1fr;
  }

  .topic-overview {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 980px) {
  .card-grid.cols-2,
  .card-grid.cols-3,
  .card-grid.cols-4,
  .topic-overview,
  .analysis-footer,
  .tcm-summary {
    grid-template-columns: 1fr;
  }

  .board-header,
  .section-head,
  .toolbar-main,
  .effect-header,
  .effect-metrics,
  .topic-factor-item {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-meta {
    justify-content: flex-start;
  }
}
</style>
