<template>
  <div class="home-service-page">
    <div class="page-header">
      <!-- 仅美化，不改布局：标题徽章 + 渐变标题 -->
      <div class="page-title">
        <span class="title-badge" aria-hidden="true">
          <!-- ClipboardCheck / House 风格图标（避免引入新依赖） -->
          <svg viewBox="0 0 24 24" fill="none" class="title-badge-icon">
            <path d="M9 4h6" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
            <path d="M9 4a2 2 0 0 0-2 2v1h10V6a2 2 0 0 0-2-2" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/>
            <path d="M7 7H6a2 2 0 0 0-2 2v11a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-1" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/>
            <path d="M9 16l2 2 4-5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </span>
        <span class="page-title-text">随访任务指派</span>
      </div>
      <div class="page-subtitle">随访任务调度与人员指派管理</div>
    </div>

    <div class="card">
      <div class="card-header">
        <span class="card-title">服务任务列表</span>
        <button class="btn btn-primary" @click="openGenerateDialog">生成随访任务清单</button>
      </div>
      <div v-if="loading" class="loading">加载中...</div>
      <table v-else class="table">
        <thead>
          <tr>
            <th>患者姓名</th>
            <th>服务地址</th>
            <th>服务类型</th>
            <th>计划日期</th>
            <th>执行团队</th>
            <th>随访人员</th>
            <th>状态</th>
            <th>备注</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="taskList.length === 0">
            <td colspan="9" style="text-align: center; color: #999;">暂无数据</td>
          </tr>
          <tr v-for="task in taskList" :key="task.taskId">
            <td>{{ task.patientName }}</td>
            <td>{{ task.address }}</td>
            <td>{{ task.serviceType }}</td>
            <td>{{ task.planDate }}</td>
            <td>{{ task.assignee }}</td>
            <td>
              <span v-if="task.staffName" class="staff-name">{{ task.staffName }}</span>
              <span v-else class="no-staff">未指派</span>
            </td>
            <td>
              <span class="status-badge">{{ task.statusText }}</span>
            </td>
            <td>{{ task.remark || '——' }}</td>
            <td>
              <button class="btn btn-sm btn-primary" @click="openAssignDialog(task)">指派随访人员</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 指派随访人员弹窗 -->
    <Teleport to="body">
      <div v-if="showAssignDialog" class="modal-overlay" @click="closeAssignDialog">
        <div class="modal-content" @click.stop>
          <div class="modal-header">
            <h3>指派随访人员</h3>
            <button class="modal-close" @click="closeAssignDialog">×</button>
          </div>
          <div class="modal-body">
            <div class="form-group">
              <label>任务信息：</label>
              <div class="task-info">
                <div>患者：{{ currentTask?.patientName }}</div>
                <div>服务类型：{{ currentTask?.serviceType }}</div>
                <div>计划日期：{{ currentTask?.planDate }}</div>
              </div>
            </div>
            <div class="form-group">
              <label>选择随访人员：</label>
              <select v-model="selectedStaffId" class="input">
                <option value="">请选择随访人员</option>
                <option v-for="staff in staffList" :key="staff.id" :value="staff.id">
                  {{ staff.name }}（{{ staff.department || '——' }}）
                </option>
              </select>
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn btn-ghost" @click="closeAssignDialog">取消</button>
            <button class="btn btn-primary" @click="handleAssign" :disabled="!selectedStaffId || assigning">
              {{ assigning ? '指派中...' : '确认指派' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 生成随访任务清单弹窗 -->
    <Teleport to="body">
      <div v-if="showGenerateDialog" class="modal-overlay" @click="closeGenerateDialog">
        <div class="modal-content" @click.stop>
          <div class="modal-header">
            <h3>生成随访任务</h3>
            <button class="modal-close" @click="closeGenerateDialog">×</button>
          </div>
        <div class="modal-body">
          <div class="form-group">
            <label>选择患者：*</label>
            <div class="patient-search">
              <input 
                v-model="patientSearchKeyword" 
                type="text" 
                class="input" 
                placeholder="输入患者姓名搜索"
                @input="searchPatients"
              />
              <div v-if="patientSearchResults.length > 0" class="search-results">
                <div 
                  v-for="patient in patientSearchResults" 
                  :key="patient.patientId"
                  class="search-result-item"
                  :class="{ selected: generateForm.patientId === patient.patientId }"
                  @click="selectPatient(patient)"
                >
                  <div class="patient-name">{{ patient.name }}</div>
                  <div class="patient-info">
                    {{ patient.age }}岁 | {{ patient.disease || '未知疾病' }} | {{ patient.responsibleDoctor || '未分配医生' }}
                  </div>
                </div>
              </div>
            </div>
            <div v-if="selectedPatient" class="selected-patient">
              <div class="patient-card">
                <div class="patient-name">{{ selectedPatient.name }}</div>
                <div class="patient-details">
                  <span>年龄：{{ selectedPatient.age }}岁</span>
                  <span>疾病：{{ selectedPatient.disease || '未知' }}</span>
                  <span>证型：{{ selectedPatient.syndrome || '未知' }}</span>
                  <span>责任医生：{{ selectedPatient.responsibleDoctor || '未分配' }}</span>
                </div>
              </div>
            </div>
          </div>
          
          <div class="form-group">
            <label>服务类型：*</label>
            <select v-model="generateForm.serviceType" class="input">
              <option value="">请选择服务类型</option>
              <option value="健康评估">健康评估</option>
              <option value="用药指导">用药指导</option>
              <option value="康复训练">康复训练</option>
              <option value="心理疏导">心理疏导</option>
              <option value="生活指导">生活指导</option>
              <option value="复查提醒">复查提醒</option>
            </select>
          </div>
          
          <div class="form-group">
            <label>计划日期：*</label>
            <input v-model="generateForm.planDate" type="date" class="input" />
          </div>
          
          <div class="form-group">
            <label>任务优先级：</label>
            <select v-model="generateForm.priority" class="input">
              <option value="HIGH">高优先级</option>
              <option value="MEDIUM">中优先级</option>
              <option value="LOW">低优先级</option>
            </select>
          </div>
          
          <div class="form-group">
            <label>备注：</label>
            <textarea 
              v-model="generateForm.remark" 
              class="input" 
              rows="3" 
              placeholder="可选：添加任务备注信息"
            ></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-ghost" @click="closeGenerateDialog">取消</button>
          <button class="btn btn-primary" @click="handleGenerateTasks" :disabled="generating || !isGenerateFormValid">
            {{ generating ? '生成中...' : '生成任务' }}
          </button>
        </div>
      </div>
    </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { interventionApi, type HomeServiceTask, type GenerateFollowupTasksRequest } from '@/api/intervention'
import { followupApi } from '@/api/followup'
import { patientApi, type PatientSummaryRow } from '@/api/patient'

const loading = ref(false)
const taskList = ref<HomeServiceTask[]>([])
const showAssignDialog = ref(false)
const showGenerateDialog = ref(false)
const currentTask = ref<HomeServiceTask | null>(null)
const selectedStaffId = ref<number | string>('')
const staffList = ref<Array<{ id: number | string; name: string; department?: string }>>([])
const assigning = ref(false)
const generating = ref(false)

// 患者搜索相关
const patientSearchKeyword = ref('')
const patientSearchResults = ref<PatientSummaryRow[]>([])
const selectedPatient = ref<PatientSummaryRow | null>(null)

// 生成任务表单
const generateForm = ref({
  patientId: 0,
  serviceType: '',
  planDate: '',
  priority: 'MEDIUM' as 'HIGH' | 'MEDIUM' | 'LOW',
  remark: ''
})

// 表单验证
const isGenerateFormValid = computed(() => {
  return generateForm.value.patientId > 0 && 
         generateForm.value.serviceType && 
         generateForm.value.planDate
})

onMounted(() => {
  loadData()
  loadStaffList()
  initializeGenerateForm()
})

function initializeGenerateForm() {
  const tomorrow = new Date()
  tomorrow.setDate(tomorrow.getDate() + 1)
  
  generateForm.value.patientId = 0
  generateForm.value.serviceType = ''
  generateForm.value.planDate = tomorrow.toISOString().split('T')[0]
  generateForm.value.priority = 'MEDIUM'
  generateForm.value.remark = ''
  
  // 重置患者选择
  selectedPatient.value = null
  patientSearchKeyword.value = ''
  patientSearchResults.value = []
}

async function loadData() {
  loading.value = true
  try {
    const result = await interventionApi.getHomeService()
    if (result.success && result.data) {
      taskList.value = result.data
    }
  } catch (error) {
    console.error('加载数据失败:', error)
  } finally {
    loading.value = false
  }
}

async function loadStaffList() {
  try {
    const result = await followupApi.getStaffList({ pageNo: 1 })
    if (result.success && result.data) {
      staffList.value = result.data.rows || []
    }
  } catch (error) {
    console.error('加载随访人员列表失败:', error)
  }
}

function openAssignDialog(task: HomeServiceTask) {
  currentTask.value = task
  selectedStaffId.value = task.staffId || ''
  showAssignDialog.value = true
}

function closeAssignDialog() {
  showAssignDialog.value = false
  currentTask.value = null
  selectedStaffId.value = ''
}

function openGenerateDialog() {
  initializeGenerateForm()
  showGenerateDialog.value = true
}

function closeGenerateDialog() {
  showGenerateDialog.value = false
  initializeGenerateForm()
}

// 患者搜索功能
async function searchPatients() {
  if (!patientSearchKeyword.value.trim()) {
    patientSearchResults.value = []
    return
  }
  
  try {
    const result = await patientApi.getPatientSummary({
      q: patientSearchKeyword.value.trim(),
      pageSize: 10
    })
    
    if (result.success && result.data) {
      patientSearchResults.value = result.data.rows
    }
  } catch (error) {
    console.error('搜索患者失败:', error)
  }
}

// 选择患者
function selectPatient(patient: PatientSummaryRow) {
  selectedPatient.value = patient
  generateForm.value.patientId = patient.patientId
  patientSearchResults.value = []
  patientSearchKeyword.value = patient.name
}

async function handleAssign() {
  if (!currentTask.value || !selectedStaffId.value) return
  
  assigning.value = true
  try {
    const result = await interventionApi.assignStaff({
      taskId: currentTask.value.taskId,
      staffId: Number(selectedStaffId.value)
    })
    
    if (result.success) {
      // 更新本地数据
      const task = taskList.value.find(t => t.taskId === currentTask.value!.taskId)
      if (task) {
        const selectedStaff = staffList.value.find(s => s.id === selectedStaffId.value)
        if (selectedStaff) {
          task.staffId = Number(selectedStaffId.value)
          task.staffName = selectedStaff.name
        }
      }
      closeAssignDialog()
      alert('指派成功！')
    } else {
      alert(result.message || '指派失败')
    }
  } catch (error: any) {
    console.error('指派失败:', error)
    alert(error.message || '指派失败')
  } finally {
    assigning.value = false
  }
}

async function handleGenerateTasks() {
  if (!isGenerateFormValid.value) return
  
  generating.value = true
  try {
    const request: GenerateFollowupTasksRequest = {
      patientId: generateForm.value.patientId,
      serviceType: generateForm.value.serviceType,
      planDate: generateForm.value.planDate,
      priority: generateForm.value.priority,
      remark: generateForm.value.remark || undefined
    }
    
    const result = await interventionApi.generateFollowupTasks(request)
    
    if (result.success && result.data) {
      const task = (result.data as any)?.task
      if (!task) {
        alert(result.message || '生成任务失败')
        return
      }

      // 填充患者姓名
      task.patientName = task.patientName || selectedPatient.value?.name || '未知患者'

      // 将新生成的任务添加到任务列表顶部
      taskList.value.unshift(task)

      closeGenerateDialog()
      alert(`成功为患者 ${selectedPatient.value?.name || task.patientName} 生成随访任务！\n任务已添加到列表顶部。`)
    } else {
      alert(result.message || '生成任务失败')
    }
  } catch (error: any) {
    console.error('生成任务失败:', error)
    alert(error.message || '生成任务失败')
  } finally {
    generating.value = false
  }
}
</script>

<style scoped>
/* =========================
   仅美化，不改布局（intervention/HomeService.vue）
   ========================= */
.home-service-page {
  /* 仅美化，不改布局（intervention/HomeService.vue）：修复大卡 hover 遮挡裁切 */
  width: 100%;
  max-width: 1120px;
  min-width: 0;
  margin: 0 auto;
  box-sizing: border-box;
  overflow: visible;
  overflow-y: visible;
  padding-inline: 12px;
  background: transparent;
}

/* 任务列表外层父容器：避免 hover 溢出被裁切 */
.home-service-page > .card {
  overflow: visible;
}

/* 大宽卡 hover：收紧到 1.004 ~ 1.006，只做阴影/高光/边框轻提亮 */
.home-service-page .card:hover {
  transform: scale(1.005);
  box-shadow: 0 18px 40px rgba(102, 126, 234, 0.14), 0 8px 20px rgba(15, 23, 42, 0.06);
  border-color: rgba(102, 126, 234, 0.22);
}

.home-service-page .card:hover::before {
  opacity: 0.95;
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
.input:focus-visible,
textarea:focus-visible,
button:focus-visible,
.modal-close:focus-visible {
  outline: none;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.7);
}

.card-header .card-title {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 0;
  position: relative;
  padding-left: 12px;
}

.card-header .card-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 16px;
  border-radius: 999px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
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

.btn-sm {
  padding: 8px 16px;
  font-size: 12px;
  font-weight: 600;
  border-radius: 10px;
  transition: all 0.2s ease;
}

.staff-name {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.2px;
  color: #4c51bf;
  background: rgba(102, 126, 234, 0.12);
  border: 1px solid rgba(102, 126, 234, 0.25);
}

.no-staff {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.2px;
  color: #64748b;
  background: rgba(148, 163, 184, 0.14);
  border: 1px solid rgba(148, 163, 184, 0.26);
}

.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.2px;
  color: #334155;
  background: rgba(148, 163, 184, 0.14);
  border: 1px solid rgba(148, 163, 184, 0.26);
}

/* 弹窗样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(15, 23, 42, 0.55);
  backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.modal-content {
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(14px);
  border-radius: 16px;
  width: 90%;
  max-width: 700px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 
    0 24px 60px rgba(15, 23, 42, 0.22),
    0 0 0 1px rgba(255, 255, 255, 0.35);
  border: 1px solid rgba(226, 232, 240, 0.7);
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from { 
    opacity: 0;
    transform: translateY(20px) scale(0.95);
  }
  to { 
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 28px;
  border-bottom: 2px solid rgba(226, 232, 240, 0.3);
}

.modal-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #2d3748;
}

.modal-close {
  background: none;
  border: none;
  font-size: 28px;
  color: #a0aec0;
  cursor: pointer;
  padding: 0;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.modal-close:hover {
  background: rgba(226, 232, 240, 0.2);
  color: #718096;
  transform: scale(1.1);
}

.modal-body {
  padding: 28px;
}

.form-group {
  margin-bottom: 24px;
}

.form-group label {
  display: block;
  margin-bottom: 10px;
  font-weight: 600;
  color: #6b7280;
  font-size: 14px;
}

.task-info {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  padding: 16px 20px;
  border-radius: 12px;
  font-size: 14px;
  color: #4a5568;
  border-left: 4px solid #667eea;
}

.task-info div {
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.task-info div:last-child {
  margin-bottom: 0;
}

.task-info div::before {
  content: '•';
  color: #667eea;
  font-weight: bold;
}

.date-range span {
  color: #718096;
  font-size: 14px;
  font-weight: 500;
}

.patient-search {
  position: relative;
}

.search-results {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border: 2px solid #e2e8f0;
  border-top: none;
  border-radius: 0 0 8px 8px;
  max-height: 240px;
  overflow-y: auto;
  z-index: 10;
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
}

.search-result-item {
  padding: 16px 20px;
  cursor: pointer;
  border-bottom: 1px solid rgba(226, 232, 240, 0.5);
  transition: all 0.2s ease;
}

.search-result-item:hover {
  background: rgba(102, 126, 234, 0.05);
}

.search-result-item.selected {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
  border-left: 4px solid #667eea;
}

.search-result-item:last-child {
  border-bottom: none;
}

.search-result-item .patient-name {
  font-weight: 600;
  color: #2d3748;
  margin-bottom: 6px;
  font-size: 15px;
}

.search-result-item .patient-info {
  font-size: 13px;
  color: #718096;
  font-weight: 500;
}

.selected-patient {
  margin-top: 16px;
}

.patient-card {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
  border: 2px solid rgba(102, 126, 234, 0.2);
  border-radius: 12px;
  padding: 20px;
  transition: all 0.2s ease;
}

.patient-card:hover {
  box-shadow: 0 8px 25px rgba(102, 126, 234, 0.2);
}

.patient-card .patient-name {
  font-weight: 700;
  color: #667eea;
  margin-bottom: 12px;
  font-size: 18px;
}

.patient-details {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.patient-details span {
  font-size: 13px;
  color: #4c51bf;
  background: rgba(102, 126, 234, 0.15);
  padding: 6px 12px;
  border-radius: 16px;
  font-weight: 600;
  border: 1px solid rgba(102, 126, 234, 0.2);
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  padding: 24px 28px;
  border-top: 2px solid rgba(226, 232, 240, 0.3);
  background: rgba(248, 250, 252, 0.5);
  border-radius: 0 0 16px 16px;
}

.input {
  width: 100%;
  padding: 12px 16px;
  font-size: 14px;
  border-radius: 8px;
  border: 2px solid #e2e8f0;
  background: rgba(255, 255, 255, 0.9);
  font-weight: 500;
  transition: all 0.2s ease;
}

.input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.12);
  background: #ffffff;
}

.input::placeholder {
  color: #a0aec0;
  font-weight: 400;
}

/* 表格行悬停效果增强 */
.table thead th {
  background: linear-gradient(180deg, rgba(102, 126, 234, 0.10) 0%, rgba(118, 75, 162, 0.06) 100%);
  color: #64748b;
  font-size: 12.5px;
  font-weight: 800;
  letter-spacing: 0.3px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.85);
}

.table td {
  color: #1f2937;
  border-bottom: 1px solid rgba(226, 232, 240, 0.70);
}

.table tbody tr {
  transition: background 0.22s ease;
}

.table tbody tr:hover {
  background: rgba(102, 126, 234, 0.08);
}

/* 空状态美化 */
.table tbody tr td[colspan] {
  text-align: center;
  color: #a0aec0;
  font-style: italic;
  padding: 60px;
  background: rgba(247, 250, 252, 0.5);
  font-size: 16px;
  font-weight: 500;
}

.table tbody tr td[colspan]::before {
  content: '📋';
  display: block;
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}
</style>
