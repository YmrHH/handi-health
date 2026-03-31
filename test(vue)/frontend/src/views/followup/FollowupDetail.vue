<template>
  <div class="followup-detail-page">
    <div class="page-header">
      <!-- 仅美化，不改布局：标题徽章 + 渐变标题 -->
      <div class="page-title">
        <span class="title-badge" aria-hidden="true">
          <!-- ClipboardCheck / FileText 风格图标（避免引入新依赖） -->
          <svg viewBox="0 0 24 24" fill="none" class="title-badge-icon">
            <path d="M9 4h6a2 2 0 0 1 2 2v15a2 2 0 0 1-2 2H9a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2Z" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M10 3h4a1.5 1.5 0 0 1 1.5 1.5V6H8.5V4.5A1.5 1.5 0 0 1 10 3Z" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M9.5 12l1.8 1.8L15.5 9.6" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </span>
        <span class="page-title-text">随访记录详情</span>
      </div>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="!detail" class="error">加载失败，请返回重试</div>
    <div v-else class="detail-container">
      <!-- 返回按钮 -->
      <div style="margin-bottom: 16px;">
        <RouterLink to="/followup/workbench?tab=completed" class="btn btn-ghost">← 返回进行主动随访</RouterLink>
      </div>

      <!-- 随访概览 -->
      <div class="card">
        <div class="card-header">
          <span class="card-title">随访概览</span>
        </div>
        <div class="card-body">
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">随访编号：</span>
              <span class="info-value main-value">{{ detail.record.id }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">随访时间：</span>
              <span class="info-value">{{ detail.record.followupTime }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">随访方式：</span>
              <span class="tag tag-neutral">{{ detail.record.followupType }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">随访人员：</span>
              <span class="info-value staff-badge">{{ detail.record.staffName }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">随访结果：</span>
              <span class="info-value">{{ detail.record.resultStatus }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">风险等级：</span>
              <span :class="getRiskLevelClass(detail.record.riskLevelCode)">{{ detail.record.riskLevelText || detail.record.riskLevel }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">下次计划时间：</span>
              <span class="info-value">{{ detail.record.nextPlanTime || '——' }}</span>
            </div>
          </div>
          <div class="info-block" style="margin-top: 12px;">
            <div class="info-label">随访内容摘要：</div>
            <div class="info-text">{{ detail.record.contentSummary || '——' }}</div>
          </div>
        </div>
      </div>

      <!-- 患者基本信息 -->
      <div class="card">
        <div class="card-header">
          <span class="card-title">患者基本信息</span>
        </div>
        <div class="card-body">
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">姓名：</span>
              <span class="info-value patient-name">{{ detail.patient.name }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">性别：</span>
              <span class="info-value">{{ detail.patient.gender || '——' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">年龄：</span>
              <span class="info-value">{{ detail.patient.age || '——' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">身份证号：</span>
              <span class="info-value">{{ detail.patient.idCard || '——' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">手机号：</span>
              <span class="info-value">{{ detail.patient.phone || '——' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">主要诊断：</span>
              <span class="info-value">{{ detail.patient.disease || '——' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">中医证型：</span>
              <span class="info-value">{{ detail.patient.syndrome || '——' }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 症状与体征 -->
      <div class="card">
        <div class="card-header">
          <span class="card-title">症状与体征</span>
        </div>
        <div class="card-body">
          <div class="info-block">
            <div class="info-label">症状变化：</div>
            <div class="info-text">{{ detail.record.symptomChange || '——' }}</div>
          </div>
          <div class="info-grid vital-grid">
            <div class="info-item">
              <span class="info-label">收缩压：</span>
              <span class="info-value">{{ detail.record.sbp || '——' }}{{ detail.record.sbp ? ' mmHg' : '' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">舒张压：</span>
              <span class="info-value">{{ detail.record.dbp || '——' }}{{ detail.record.dbp ? ' mmHg' : '' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">心率：</span>
              <span class="info-value">{{ detail.record.heartRate || '——' }}{{ detail.record.heartRate ? ' 次/分' : '' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">体重：</span>
              <span class="info-value">{{ detail.record.weight || '——' }}{{ detail.record.weight ? ' kg' : '' }}</span>
            </div>
          </div>
          <div class="info-footer">测量时间：{{ detail.record.vitalMeasureTime || '未填写' }}</div>
        </div>
      </div>

      <!-- 用药与不良反应 -->
      <div class="card">
        <div class="card-header">
          <span class="card-title">用药与不良反应</span>
        </div>
        <div class="card-body">
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">用药依从性：</span>
              <span class="tag tag-med-adherence">{{ detail.record.medAdherence || '——' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">用药方案概览：</span>
              <span class="info-value">{{ detail.record.medPlanSummary || '——' }}</span>
            </div>
          </div>
          <div class="info-block">
            <div class="info-label">不良反应：</div>
            <div class="info-text">{{ detail.record.adverseReaction || '——' }}</div>
          </div>
        </div>
      </div>

      <!-- 中医体征 -->
      <div class="card">
        <div class="card-header">
          <span class="card-title">中医体征</span>
        </div>
        <div class="card-body tcm-layout">
          <div class="tcm-left">
            <div class="info-label">舌象照片：</div>
            <div class="tcm-thumb-wrapper">
              <img v-if="detail.record.tcmTongueImageUrl" :src="detail.record.tcmTongueImageUrl" class="tongue-thumb" alt="舌象" />
              <span v-else class="tcm-empty">未上传舌象图片</span>
            </div>
          </div>
          <div class="tcm-right">
            <div class="info-grid">
              <div class="info-item">
                <span class="info-label">面色：</span>
                <span class="info-value">{{ detail.record.tcmFace || '——' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">舌体：</span>
                <span class="info-value">{{ detail.record.tcmTongueBody || '——' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">舌苔：</span>
                <span class="info-value">{{ detail.record.tcmTongueCoating || '——' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">脉率：</span>
                <span class="info-value">{{ detail.record.tcmPulseRate || '——' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">脉形：</span>
                <span v-for="pulse in detail.tcmPulseTypeList" :key="pulse" class="tag tag-pulse">{{ pulse }}</span>
                <span v-if="detail.tcmPulseTypeList.length === 0" class="info-value">{{ detail.record.tcmPulseTypes || '——' }}</span>
              </div>
            </div>
            <div class="info-block">
              <div class="info-label">中医评价与建议：</div>
              <div class="info-text">{{ getTcmEvaluationAdvice(detail.record) || '——' }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 化验指标与评估 -->
      <div class="card">
        <div class="card-header">
          <span class="card-title">化验指标与评估</span>
        </div>
        <div class="card-body">
          <table class="table table-lab">
            <thead>
              <tr>
                <th>指标</th>
                <th>本次数值</th>
                <th>单位</th>
                <th>评估结果</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>空腹血糖 FPG</td>
                <td>{{ detail.record.labFpgValue || '——' }}</td>
                <td>mmol/L</td>
                <td>
                  <span :class="getLabStatusClass(detail.record.labFpgStatusCode)">{{ detail.record.labFpgStatus || '——' }}</span>
                </td>
              </tr>
              <tr>
                <td>低密度脂蛋白 LDL-C</td>
                <td>{{ detail.record.labLdlcValue || '——' }}</td>
                <td>mmol/L</td>
                <td>
                  <span :class="getLabStatusClass(detail.record.labLdlcStatusCode)">{{ detail.record.labLdlcStatus || '——' }}</span>
                </td>
              </tr>
              <tr>
                <td>甘油三酯 TG</td>
                <td>{{ detail.record.labTgValue || '——' }}</td>
                <td>mmol/L</td>
                <td>
                  <span :class="getLabStatusClass(detail.record.labTgStatusCode)">{{ detail.record.labTgStatus || '——' }}</span>
                </td>
              </tr>
              <tr>
                <td>同型半胱氨酸 Hcy</td>
                <td>{{ detail.record.labHcyValue || '——' }}</td>
                <td>μmol/L</td>
                <td>
                  <span :class="getLabStatusClass(detail.record.labHcyStatusCode)">{{ detail.record.labHcyStatus || '——' }}</span>
                </td>
              </tr>
              <tr>
                <td>尿酸 UA</td>
                <td>{{ detail.record.labUaValue || '——' }}</td>
                <td>μmol/L</td>
                <td>
                  <span :class="getLabStatusClass(detail.record.labUaStatusCode)">{{ detail.record.labUaStatus || '——' }}</span>
                </td>
              </tr>
            </tbody>
          </table>
          <div class="info-block">
            <div class="info-label">健康建议：</div>
            <div class="info-text">{{ getHealthAdvice(detail.record) || '——' }}</div>
          </div>
        </div>
      </div>

      <!-- 本次随访小结与计划 -->
      <div class="card">
        <div class="card-header">
          <span class="card-title">本次随访小结与计划</span>
        </div>
        <div class="card-body">
          <div class="info-block">
            <div class="info-label">本次随访小结：</div>
            <div class="info-text">{{ detail.record.summary || '——' }}</div>
          </div>
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">风险分层：</span>
              <span :class="getRiskLevelClass(detail.record.riskLevelCode)">{{ detail.record.riskLevelText || detail.record.riskLevel }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">下次随访方式：</span>
              <span class="info-value">{{ detail.record.nextFollowupType || '——' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">下次随访日期：</span>
              <span class="info-value">{{ detail.record.nextFollowupDate || '——' }}</span>
            </div>
          </div>
          <div class="info-block">
            <div class="info-label">下次随访说明：</div>
            <div class="info-text">{{ detail.record.nextFollowupRemark || '——' }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, RouterLink } from 'vue-router'
import { followupApi, type FollowupDetailData } from '@/api/followup'

const route = useRoute()
const loading = ref(false)
const detail = ref<FollowupDetailData | null>(null)

onMounted(() => {
  const id = route.params.id
  if (id) {
    loadDetail(Number(id))
  }
})

async function loadDetail(id: number) {
  loading.value = true
  try {
    const result = await followupApi.getDetail(id)
    if (result.success && result.data) {
      detail.value = result.data
    }
  } catch (error) {
    console.error('加载详情失败:', error)
  } finally {
    loading.value = false
  }
}

function getRiskLevelClass(code: string): string {
  if (code === 'HIGH' || code === 'high') {
    return 'tag tag-risk-high'
  } else if (code === 'MEDIUM' || code === 'mid') {
    return 'tag tag-risk-medium'
  } else {
    return 'tag tag-risk-low'
  }
}

function mergeUniqueText(...values: Array<string | undefined | null>): string {
  const seen = new Set<string>()
  const list: string[] = []
  values.forEach((value) => {
    const text = String(value ?? '').trim()
    if (!text || seen.has(text)) return
    seen.add(text)
    list.push(text)
  })
  return list.join('；')
}

function getTcmEvaluationAdvice(record: any): string {
  return mergeUniqueText(record?.tcmEvaluationAdvice, record?.tcmRemark, record?.tcmConclusion)
}

function getHealthAdvice(record: any): string {
  return mergeUniqueText(record?.healthAdvice, record?.labSummary)
}

function getLabStatusClass(code: string): string {
  if (code === 'HIGH' || code === 'high' || code === 'abnormal') {
    return 'tag tag-lab-high'
  } else if (code === 'NORMAL' || code === 'normal') {
    return 'tag tag-lab-normal'
  } else {
    return 'tag tag-lab-low'
  }
}
</script>

<style scoped>
/* =========================
   仅美化，不改布局（followup/FollowupDetail.vue）
   ========================= */

.followup-detail-page {
  /* 固定页面宽度，避免随浏览器宽度自适应缩放；放大时通过横向滚动查看 */
  width: 1120px;
  min-width: 1120px;
  margin: 0 auto;
  box-sizing: border-box;
  overflow-x: auto;
  overflow-y: visible;
  -webkit-overflow-scrolling: touch;
  background: transparent;
}

.page-header {
  padding: 6px 0 14px 0;
  border-bottom: 1px solid rgba(148, 163, 184, 0.18);
  margin-bottom: 16px;
}

.page-title {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  font-size: 26px;
  font-weight: 800;
  letter-spacing: 0.2px;
  color: #0f172a;
  margin: 0;
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

.detail-container {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 20px 28px;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 40px;
  padding: 0 14px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.22s ease;
  text-decoration: none;
  user-select: none;
}

.btn-ghost {
  background: rgba(255, 255, 255, 0.92);
  border: 2px solid rgba(226, 232, 240, 0.90);
  color: #4a5568;
  backdrop-filter: blur(10px);
}

.btn-ghost:hover:not(:disabled) {
  border-color: rgba(102, 126, 234, 0.55);
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.12);
}

.btn:focus,
.btn:focus-visible {
  outline: none;
}

.card {
  margin-bottom: 16px;
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  border: 1px solid rgba(226, 232, 240, 0.70);
  box-shadow: 0 10px 30px rgba(102,126,234,0.08), 0 4px 12px rgba(15,23,42,0.04);
  position: relative;
  overflow: hidden;
  transform: scale(1);
  transform-origin: center center;
  will-change: transform, box-shadow;
  transition:
    transform 0.28s cubic-bezier(0.22, 1, 0.36, 1),
    box-shadow 0.28s cubic-bezier(0.22, 1, 0.36, 1),
    border-color 0.28s ease;
}

.card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  opacity: 0.38;
  transition: opacity 0.28s ease;
  z-index: 1;
  pointer-events: none;
}

.card:hover {
  box-shadow: 0 14px 36px rgba(102,126,234,0.10), 0 8px 18px rgba(15,23,42,0.06);
  border-color: rgba(203, 213, 225, 0.78);
  transform: scale(1.016);
  z-index: 2;
}

.card:hover::before {
  opacity: 0.94;
}

.card > * {
  position: relative;
  z-index: 2;
}

.card-header {
  padding: 10px 14px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.75);
}

.card-title {
  font-weight: 700;
  font-size: 16px;
  color: #1f2937;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.card-title::before {
  content: '';
  width: 4px;
  height: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 2px;
}

.card-body {
  padding: 12px 14px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 8px 16px;
}

.info-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.info-label {
  font-weight: 600;
  color: #6b7280;
  font-size: 13px;
  min-width: 100px;
}

.info-value {
  color: #1f2937;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.7;
  word-break: break-word;
}

.info-block {
  margin-top: 12px;
  line-height: 1.7;
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(248, 250, 252, 0.7);
  border: 1px solid rgba(226, 232, 240, 0.75);
}

.info-text {
  color: #1f2937;
  font-size: 14px;
  margin-top: 4px;
}

.info-footer {
  margin-top: 8px;
  font-size: 12px;
  color: #94a3b8;
  font-weight: 600;
}

.vital-grid {
  margin-top: 12px;
}

.tcm-layout {
  display: grid;
  grid-template-columns: 200px 1fr;
  gap: 20px;
}

.tcm-left {
  display: flex;
  flex-direction: column;
}

.tcm-thumb-wrapper {
  margin-top: 8px;
}

.tongue-thumb {
  max-width: 100%;
  border-radius: 12px;
  cursor: pointer;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.06);
}

.tcm-empty {
  color: #94a3b8;
  font-size: 12px;
  font-weight: 600;
}

.tcm-right {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.table-lab {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  margin-top: 12px;
}

.table-lab th,
.table-lab td {
  border-bottom: 1px solid rgba(226, 232, 240, 0.55);
  padding: 12px 14px;
  text-align: center;
  font-size: 13.5px;
}

.table-lab th {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.08) 0%, rgba(118, 75, 162, 0.06) 100%);
  font-weight: 700;
  color: #64748b;
  font-size: 12.5px;
}

.tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 6px 12px;
  min-height: 28px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.2px;
  border: 1px solid rgba(226, 232, 240, 0.85);
  box-shadow: 0 6px 14px rgba(15, 23, 42, 0.06);
}

.tag-med-adherence {
  background: linear-gradient(135deg, rgba(219, 234, 254, 0.95) 0%, rgba(191, 219, 254, 0.70) 100%);
  color: #1e3a8a;
  border-color: rgba(96, 165, 250, 0.40);
}

.tag-pulse {
  background: rgba(124, 58, 237, 0.08);
  color: #5b21b6;
  border-color: rgba(124, 58, 237, 0.18);
  margin-right: 4px;
}

.tag-risk-high {
  background: linear-gradient(135deg, rgba(254, 202, 202, 0.95) 0%, rgba(254, 226, 226, 0.85) 100%);
  color: #991b1b;
  border-color: rgba(248, 113, 113, 0.55);
}

.tag-risk-medium {
  background: linear-gradient(135deg, rgba(254, 249, 195, 0.95) 0%, rgba(253, 230, 138, 0.55) 100%);
  color: #92400e;
  border-color: rgba(245, 158, 11, 0.45);
}

.tag-risk-low {
  background: linear-gradient(135deg, rgba(209, 250, 229, 0.92) 0%, rgba(187, 247, 208, 0.68) 100%);
  color: #065f46;
  border-color: rgba(16, 185, 129, 0.38);
}

.tag-lab-high {
  background: linear-gradient(135deg, rgba(254, 202, 202, 0.95) 0%, rgba(254, 226, 226, 0.85) 100%);
  color: #991b1b;
  border-color: rgba(248, 113, 113, 0.55);
}

.tag-lab-normal {
  background: linear-gradient(135deg, rgba(209, 250, 229, 0.92) 0%, rgba(187, 247, 208, 0.68) 100%);
  color: #065f46;
  border-color: rgba(16, 185, 129, 0.38);
}

.tag-lab-low {
  background: linear-gradient(135deg, rgba(219, 234, 254, 0.95) 0%, rgba(191, 219, 254, 0.70) 100%);
  color: #1e3a8a;
  border-color: rgba(96, 165, 250, 0.40);
}

.loading,
.error {
  text-align: center;
  padding: 40px;
  color: #64748b;
}

.error {
  color: #ef4444;
}

/* 顶部主信息层级（不改字段与顺序） */
.patient-name {
  font-size: 18px;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: 0.2px;
}

.main-value {
  font-size: 16px;
  font-weight: 800;
  color: #0f172a;
}

.tag-neutral {
  background: rgba(102, 126, 234, 0.08);
  color: #334155;
  border-color: rgba(102, 126, 234, 0.18);
  text-transform: none;
}

.staff-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  min-height: 28px;
  border-radius: 999px;
  background: rgba(102, 126, 234, 0.08);
  border: 1px solid rgba(102, 126, 234, 0.18);
  color: #334155;
  font-weight: 600;
  line-height: 1.2;
}
</style>
