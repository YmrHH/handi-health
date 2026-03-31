<template>
  <div class="pending-followup-detail-page">
    <div class="page-header">
      <div class="page-title">待随访详情</div>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="!detail" class="error">加载失败，请返回重试</div>
    <div v-else class="detail-container">
      <!-- 返回按钮 -->
      <div style="margin-bottom: 16px;">
        <RouterLink to="/followup/workbench?tab=pending" class="btn btn-ghost">← 返回进行主动随访</RouterLink>
      </div>

      <!-- 患者基本信息 -->
      <div class="card">
        <div class="card-header">
          <span class="card-title">患者信息</span>
        </div>
        <div class="card-body">
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">患者姓名：</span>
              <span class="info-value">{{ detail.name }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">病案号：</span>
              <span class="info-value">{{ detail.patientIdNumber || detail.patientId }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">风险等级：</span>
              <span :class="getRiskLevelClass(detail.riskLevel)">{{ detail.riskLevel || '——' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">责任医生：</span>
              <span class="info-value">{{ detail.doctor }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">病种：</span>
              <span class="info-value">{{ detail.disease || '——' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">手机号：</span>
              <span class="info-value">{{ detail.phone || '——' }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 随访任务信息（可编辑） -->
      <div class="card">
        <div class="card-header">
          <span class="card-title">随访任务信息</span>
        </div>
        <div class="card-body">
          <div class="form-group">
            <label>随访方式 <span class="required">*</span></label>
            <select v-model="formData.followupType" class="input">
              <option value="">请选择随访方式</option>
              <option value="电话随访">电话随访</option>
              <option value="视频随访">视频随访</option>
              <option value="上门随访">上门随访</option>
              <option value="门诊随访">门诊随访</option>
            </select>
          </div>

          <div class="form-group">
            <label>计划随访时间 <span class="required">*</span></label>
            <input v-model="formData.planDate" type="date" class="input" />
          </div>

          <div class="form-group">
            <label>随访人员</label>
            <select v-model="formData.staffId" class="input">
              <option value="">指派人员（科室）</option>
              <option v-for="staff in staffList" :key="staff.id" :value="staff.id">
                {{ staff.name }}（{{ staff.department || '——' }}）
              </option>
            </select>
          </div>

          <div class="form-group">
            <label>随访清单</label>
            <div class="checklist-container">
              <label v-for="item in followupChecklist" :key="item.id" class="checkbox-label">
                <input 
                  type="checkbox" 
                  :value="item.id"
                  v-model="formData.checklist"
                  class="checkbox-input"
                />
                <span>{{ item.label }}</span>
              </label>
            </div>
          </div>

          <div class="form-group">
            <label>随访内容/备注</label>
            <textarea 
              v-model="formData.content" 
              class="input" 
              rows="5" 
              placeholder="请输入随访的具体内容、注意事项等..."
            ></textarea>
          </div>

          <div class="form-actions">
            <button class="btn btn-ghost" @click="handleCancel">取消</button>
            <button 
              class="btn btn-primary" 
              @click="handleSave"
              :disabled="!isFormValid || submitting">
              {{ submitting ? '保存中...' : '保存' }}
            </button>
            <button 
              class="btn btn-success" 
              @click="handleResend"
              :disabled="!isFormValid || submitting">
              {{ submitting ? '下发中...' : '重新下发' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, RouterLink, useRouter } from 'vue-router'
import request from '@/api/request'
import { followupApi, type StaffListRow } from '@/api/followup'
import type { FollowupWorkbenchRow } from '@/api/alert'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const detail = ref<FollowupWorkbenchRow | null>(null)

// 随访人员列表
const staffList = ref<Array<{ id: number | string; name: string; department?: string }>>([])

// 随访清单选项
const followupChecklist = ref([
  { id: 'blood_pressure', label: '血压监测' },
  { id: 'blood_sugar', label: '血糖监测' },
  { id: 'medication', label: '用药情况' },
  { id: 'diet', label: '饮食控制' },
  { id: 'exercise', label: '运动情况' },
  { id: 'symptoms', label: '症状询问' },
  { id: 'life_quality', label: '生活质量评估' }
])

// 表单数据
const formData = ref({
  followupType: '',
  planDate: '',
  staffId: '' as string | number,
  checklist: [] as string[],
  content: ''
})

// 表单验证
const isFormValid = computed(() => {
  return formData.value.followupType && formData.value.planDate
})

function parseChecklist(v: any): string[] | null {
  if (v == null) return null
  if (Array.isArray(v)) return v as string[]
  const s = String(v).trim()
  if (!s) return null
  try {
    const parsed = JSON.parse(s)
    if (Array.isArray(parsed)) {
      return parsed.map(x => String(x))
    }
  } catch (e) {
    // ignore
  }
  const cleaned = s.replace(/^[\[]|[\]]$/g, '')
  const arr = cleaned
    .split(',')
    .map(x => x.trim())
    .filter(Boolean)
  return arr.length ? arr : null
}

function normalizeTaskStatus(raw?: any): 'PENDING' | 'DONE' | 'CANCELLED' | '' {
  if (raw == null) return ''
  const s = String(raw).trim().toUpperCase()
  if (!s) return ''
  if (s === 'PENDING') return 'PENDING'
  if (s === 'ASSIGNED' || s === 'IN_PROGRESS' || s === 'TODO') return 'PENDING'
  if (s === 'DONE' || s === 'COMPLETED' || s === 'FINISHED' || s === 'SUCCESS') return 'DONE'
  if (s === 'CANCELLED' || s === 'CANCELED') return 'CANCELLED'
  return ''
}

onMounted(() => {
  loadDetail()
  loadStaffList()
})

async function loadDetail() {
  loading.value = true
  try {
    const patientId = Number(route.params.id)
    
    if (!patientId || isNaN(patientId)) {
      throw new Error('无效的患者ID')
    }

    // 使用随访工作台任务接口（/api/followup/tasks）查找该 patientId 对应的待随访任务
    const result = await followupApi.getWorkbenchTasks({
      page: 1,
      pageSize: 200
    })
    const payload = result.success ? (result.data as any) : null
    const rows = (payload?.rows || []) as any[]

    const candidates = Array.isArray(rows) ? rows : []
    const pendingLike = candidates.filter(r => normalizeTaskStatus(r?.status) === 'PENDING')
    let patientData = pendingLike.find(r => Number(r.patientId ?? r.patient_id) === patientId)
    if (!patientData) {
      // 兜底：不按状态过滤，避免历史数据状态不一致导致找不到
      patientData = candidates.find(r => Number(r.patientId ?? r.patient_id) === patientId)
    }
    if (!patientData) {
      throw new Error('未找到患者信息')
    }

    const normalized: FollowupWorkbenchRow = {
      taskId: patientData.taskId ?? patientData.task_id ?? patientData.id,
      patientId: Number(patientData.patientId ?? patientData.patient_id) || patientId,
      patientIdNumber: patientData.patientIdNumber ?? patientData.patient_id ?? patientId,
      name: patientData.patientName ?? patientData.patient_name ?? patientData.name ?? '',
      phone: patientData.phone ?? patientData.patientPhone ?? '',
      idCard: patientData.idCard ?? '',
      disease: patientData.disease ?? patientData.mainDisease ?? '',
      doctor: patientData.doctor ?? patientData.doctorName ?? '',
      followupType: patientData.followMethod ?? patientData.triggerType ?? patientData.followupType ?? '',
      visitDate: '',
      visitTime: '',
      status: normalizeTaskStatus(patientData.status) || 'PENDING',
      statusText: '待随访',
      riskLevel: patientData.riskLevel ?? patientData.risk_level ?? '',
      followupRecordId: patientData.followupRecordId
    } as any

    ;(normalized as any).planDate = patientData.planDate ?? patientData.ext1 ?? ''
    ;(normalized as any).checklist = patientData.checklist ?? patientData.ext2 ?? ''
    ;(normalized as any).followupContent = patientData.followupContent ?? patientData.description ?? ''
    ;(normalized as any).staffId = patientData.taskStaffId ?? patientData.follow_up_user_id ?? patientData.staffId ?? ''

    detail.value = normalized
    
    // 初始化表单数据（如果有已保存的数据，则加载；否则使用默认值）
    const tomorrow = new Date()
    tomorrow.setDate(tomorrow.getDate() + 1)
    
    // 从患者数据中获取已有的随访任务信息
    const existingData = normalized as any
    formData.value = {
      followupType: existingData.followupType || '电话随访',
      planDate: existingData.planDate || tomorrow.toISOString().split('T')[0],
      staffId: existingData.staffId || '',
      checklist: parseChecklist(existingData.checklist) || ['blood_pressure', 'blood_sugar'],
      content: existingData.followupContent || existingData.content || '请关注患者血压和血糖情况，询问用药依从性。'
    }
  } catch (error: any) {
    console.error('加载详情失败:', error)
    alert('加载失败：' + (error.message || '未知错误'))
    router.push('/followup/workbench?tab=pending')
  } finally {
    loading.value = false
  }
}

// 加载随访人员列表
async function loadStaffList() {
  try {
    const result = await followupApi.getStaffList({ pageNo: 1 })
    if (result.success && result.data) {
      const rows: StaffListRow[] = result.data.rows || []
      staffList.value = rows
        .filter(row => row.id || row.staffId)
        .map(row => ({
          id: row.id || row.staffId || 0,
          name: row.name,
          department: row.department
        }))
    }
  } catch (error) {
    console.error('加载随访人员列表失败:', error)
  }
}

function getRiskLevelClass(level?: string | null): string {
  if (!level) return 'badge'
  const text = level.toUpperCase()
  if (text.includes('HIGH') || text.includes('高')) return 'badge badge-danger'
  if (text.includes('MED') || text.includes('中')) return 'badge badge-warning'
  if (text.includes('LOW') || text.includes('低')) return 'badge badge-success'
  return 'badge'
}

// 取消操作
function handleCancel() {
  router.push('/followup/workbench?tab=pending')
}

// 保存
async function handleSave() {
  if (!isFormValid.value) return
  
  submitting.value = true
  try {
    if (!detail.value) {
      throw new Error('缺少患者信息')
    }

    await request.post('/api/alert/followup/task/update', {
      patientId: detail.value.patientId,
      followupType: formData.value.followupType,
      planDate: formData.value.planDate,
      staffId: formData.value.staffId || null,
      checklist: formData.value.checklist,
      content: formData.value.content
    })
    
    alert('保存成功！')
    router.push('/followup/workbench?tab=pending&refresh=true')
  } catch (error: any) {
    console.error('保存失败:', error)
    alert('保存失败：' + (error.message || '未知错误'))
  } finally {
    submitting.value = false
  }
}

// 重新下发
async function handleResend() {
  if (!isFormValid.value) return
  
  const confirmed = confirm(
    `确定要重新下发此随访任务吗？\n\n` +
    `患者：${detail.value?.name}\n` +
    `随访方式：${formData.value.followupType}\n` +
    `计划时间：${formData.value.planDate}\n\n` +
    `任务将重新下发给随访人员。`
  )
  
  if (!confirmed) return
  
  submitting.value = true
  try {
    if (!detail.value) {
      throw new Error('缺少患者信息')
    }

    await request.post('/api/alert/followup/task/resend', {
      patientId: detail.value.patientId,
      followupType: formData.value.followupType,
      planDate: formData.value.planDate,
      staffId: formData.value.staffId || null,
      checklist: formData.value.checklist,
      content: formData.value.content
    })
    
    const staffName = formData.value.staffId 
      ? staffList.value.find(s => s.id === formData.value.staffId)?.name || '未指派'
      : '未指派'
    
    alert(
      `随访任务已重新下发！\n\n` +
      `患者：${detail.value?.name}\n` +
      `随访人员：${staffName}\n` +
      `随访方式：${formData.value.followupType}\n` +
      `计划时间：${formData.value.planDate}`
    )
    
    router.push('/followup/workbench?tab=pending&refresh=true')
  } catch (error: any) {
    console.error('下发失败:', error)
    alert('下发失败：' + (error.message || '未知错误'))
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.pending-followup-detail-page {
  /* 固定页面宽度，避免随浏览器宽度自适应缩放；放大时通过横向滚动查看 */
  width: 1120px;
  min-width: 1120px;
  margin: 0 auto;
  background: transparent;
  padding: 20px;
  box-sizing: border-box;
  overflow-x: auto;
  overflow-y: visible;
  -webkit-overflow-scrolling: touch;
  -webkit-overflow-scrolling: touch;
}

.detail-container {
  max-width: 1200px;
  margin: 0 auto;
}

.card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  margin-bottom: 20px;
}

.card-header {
  padding: 20px 24px;
  border-bottom: 2px solid rgba(226, 232, 240, 0.3);
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
}

.card-title {
  font-size: 18px;
  font-weight: 700;
  color: #2d3748;
}

.card-body {
  padding: 24px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.info-item {
  padding: 12px 16px;
  background: rgba(247, 250, 252, 0.5);
  border-radius: 8px;
  border-left: 4px solid #667eea;
}

.info-label {
  color: #4a5568;
  font-size: 14px;
  font-weight: 600;
  margin-right: 8px;
}

.info-value {
  color: #2d3748;
  font-size: 14px;
  font-weight: 500;
}

.badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.badge-danger {
  background: linear-gradient(135deg, #fed7d7 0%, #feb2b2 100%);
  color: #c53030;
  border: 1px solid #fc8181;
}

.badge-warning {
  background: linear-gradient(135deg, #fefcbf 0%, #faf089 100%);
  color: #d69e2e;
  border: 1px solid #f6e05e;
}

.badge-success {
  background: linear-gradient(135deg, #c6f6d5 0%, #9ae6b4 100%);
  color: #2f855a;
  border: 1px solid #68d391;
}

.loading {
  text-align: center;
  padding: 60px 20px;
  color: #718096;
  font-size: 16px;
  font-weight: 500;
}

.error {
  text-align: center;
  padding: 60px 20px;
  color: #e53e3e;
  font-size: 16px;
  font-weight: 500;
}

/* 表单样式 */
.form-group {
  margin-bottom: 24px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #2d3748;
}

.required {
  color: #e53e3e;
  margin-left: 4px;
}

.form-group .input {
  width: 100%;
  padding: 12px 16px;
  border: 2px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
  background: rgba(255, 255, 255, 0.9);
  box-sizing: border-box;
}

.form-group .input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
  background: #ffffff;
}

.form-group select.input {
  cursor: pointer;
}

.form-group textarea.input {
  resize: vertical;
  min-height: 120px;
  font-family: inherit;
}

.checklist-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  background: rgba(247, 250, 252, 0.5);
  border-radius: 8px;
  border: 2px solid #e2e8f0;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 8px;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.checkbox-label:hover {
  background: rgba(102, 126, 234, 0.05);
}

.checkbox-input {
  width: 18px;
  height: 18px;
  cursor: pointer;
  accent-color: #667eea;
}

.checkbox-label span {
  font-size: 14px;
  color: #2d3748;
  font-weight: 500;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 2px solid rgba(226, 232, 240, 0.3);
}

.btn {
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
}

.btn-ghost {
  background: rgba(255, 255, 255, 0.9);
  color: #4a5568;
  border: 2px solid #e2e8f0;
}

.btn-ghost:hover {
  background: #ffffff;
  border-color: #cbd5e0;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
  box-shadow: 0 4px 14px 0 rgba(102, 126, 234, 0.39);
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.45);
}

.btn-success {
  background: linear-gradient(135deg, #48bb78 0%, #38a169 100%);
  color: #ffffff;
  box-shadow: 0 4px 14px 0 rgba(72, 187, 120, 0.39);
}

.btn-success:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(72, 187, 120, 0.45);
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}
</style>

