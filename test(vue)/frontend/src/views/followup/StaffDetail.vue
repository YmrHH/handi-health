<template>
  <div class="staff-detail-page">
    <div class="page-header">
      <!-- 仅美化，不改布局：标题徽章 + 渐变标题 -->
      <div class="page-title">
        <span class="title-badge" aria-hidden="true">
          <!-- UserCog / UserCheck 风格图标（避免引入新依赖） -->
          <svg viewBox="0 0 24 24" fill="none" class="title-badge-icon">
            <path d="M16 21v-1a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v1" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M9 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M16.5 13.5l.7-1.2 1.4.3.3 1.4 1.2.7-.7 1.2.7 1.2-1.2.7-.3 1.4-1.4.3-.7 1.2-1.2-.7-1.2.7-.7-1.2-1.4-.3-.3-1.4-1.2-.7.7-1.2-.7-1.2 1.2-.7.3-1.4 1.4-.3.7-1.2 1.2.7 1.2-.7Z" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M16.5 16.2a1.4 1.4 0 1 0 0-2.8 1.4 1.4 0 0 0 0 2.8Z" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </span>
        <span class="page-title-text">随访人员详情</span>
      </div>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="!detail" class="error">加载失败，请返回重试</div>
    <div v-else class="detail-container">
      <!-- 返回按钮 -->
      <div style="margin-bottom: 16px;">
        <RouterLink to="/followup/staff" class="btn btn-ghost">← 返回随访人员列表</RouterLink>
      </div>

      <!-- 基本信息 -->
      <div class="card">
        <div class="card-header">
          <span class="card-title">基本信息</span>
        </div>
        <div class="card-body">
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">姓名：</span>
              <span class="info-value staff-name">{{ detail.name }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">工号：</span>
              <span class="info-value">{{ detail.staffId || '——' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">部门：</span>
              <span class="info-value">{{ detail.department || '——' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">岗位：</span>
              <span class="info-value">{{ detail.position || '——' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">联系电话：</span>
              <span class="info-value">{{ detail.phone || '——' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">邮箱：</span>
              <span class="info-email-wrap">
                <span v-if="detail.email" class="info-value">{{ detail.email }}</span>
                <button type="button" class="btn btn-ghost btn-mini" @click="editEmail">
                  {{ detail.email ? '编辑' : '填写' }}
                </button>
              </span>
            </div>
            <div class="info-item">
              <span class="info-label">状态：</span>
              <span class="status-wrap">
                <span :class="getStatusClass(detail.status)">{{ detail.status || '在职' }}</span>
                <button
                  v-if="(detail.status || '在职') === '在职'"
                  type="button"
                  class="btn btn-danger btn-mini"
                  :disabled="changingStatus"
                  @click="markResigned"
                >
                  {{ changingStatus ? '处理中...' : '离职' }}
                </button>
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 工作统计 -->
      <div class="card">
        <div class="card-header">
          <span class="card-title">工作统计</span>
        </div>
        <div class="card-body">
          <div class="stats-grid">
            <div class="stat-card">
              <div class="stat-label">负责患者数</div>
              <div class="stat-value primary">{{ detail.patientCount || 0 }}</div>
              <div class="stat-unit">人</div>
            </div>
            <div class="stat-card">
              <div class="stat-label">随访总次数</div>
              <div class="stat-value success">{{ detail.totalFollowupCount || 0 }}</div>
              <div class="stat-unit">次</div>
            </div>
            <div class="stat-card">
              <div class="stat-label">本月随访</div>
              <div class="stat-value warning">{{ detail.monthFollowupCount || 0 }}</div>
              <div class="stat-unit">次</div>
            </div>
            <div class="stat-card">
              <div class="stat-label">已完成随访</div>
              <div class="stat-value">{{ detail.completedCount || 0 }}</div>
              <div class="stat-unit">次</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 负责的患者列表 -->
      <div class="card">
        <div class="card-header">
          <span class="card-title">负责的患者列表</span>
          <span class="card-subtitle">共 {{ detail.patients?.length || 0 }} 人</span>
        </div>
        <div class="card-body">
          <div v-if="!detail.patients || detail.patients.length === 0" class="empty">暂无负责的患者</div>
          <table v-else class="table">
            <thead>
              <tr>
                <th>序号</th>
                <th>患者姓名</th>
                <th>病案号</th>
                <th>风险等级</th>
                <th>最后随访时间</th>
                <th>随访次数</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(patient, index) in detail.patients" :key="patient.patientId">
                <td>{{ index + 1 }}</td>
                <td>{{ patient.patientName }}</td>
                <td>{{ formatCaseId(patient.patientId) }}</td>
                <td>
                  <span :class="getRiskLevelClass(patient.riskLevel)">{{ patient.riskLevel || '——' }}</span>
                </td>
                <td>{{ patient.lastFollowupTime || '——' }}</td>
                <td>{{ patient.followupCount || 0 }}</td>
                <td>
                  <RouterLink :to="`/patient/detail/${patient.patientId}`" class="link-primary">查看详情</RouterLink>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 最近随访记录 -->
      <div class="card">
        <div class="card-header">
          <span class="card-title">最近随访记录</span>
          <span class="card-subtitle">最近 10 条记录</span>
        </div>
        <div class="card-body">
          <div v-if="!detail.recentFollowups || detail.recentFollowups.length === 0" class="empty">暂无随访记录</div>
          <table v-else class="table">
            <thead>
              <tr>
                <th>序号</th>
                <th>随访时间</th>
                <th>患者姓名</th>
                <th>随访方式</th>
                <th>随访结果</th>
                <th>风险等级</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(record, index) in detail.recentFollowups" :key="record.id">
                <td>{{ index + 1 }}</td>
                <td>{{ formatDate(record.followupTime) }}</td>
                <td>{{ record.patientName }}</td>
                <td>{{ record.followupType }}</td>
                <td>{{ record.resultStatus }}</td>
                <td>
                  <span :class="getRiskLevelClass(record.riskLevel)">{{ record.riskLevel || '——' }}</span>
                </td>
                <td>
                  <RouterLink :to="`/followup/detail/${record.id}`" class="link-primary">查看详情</RouterLink>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, RouterLink } from 'vue-router'
import { followupApi, type StaffDetailData } from '@/api/followup'

const route = useRoute()
const loading = ref(false)
const detail = ref<StaffDetailData | null>(null)
const changingStatus = ref(false)

onMounted(() => {
  const id = route.params.id
  if (id) {
    loadDetail(String(id))
  }
})

async function loadDetail(id: string) {
  loading.value = true
  try {
    const result = await followupApi.getStaffDetail(id)
    if (result.success && result.data) {
      detail.value = result.data
    }
  } catch (error) {
    console.error('加载详情失败:', error)
  } finally {
    loading.value = false
  }
}

async function markResigned() {
  if (!detail.value) return
  const id = detail.value.id
  if (id == null) return
  const ok = window.confirm('确认将该随访人员状态变更为“离职”吗？')
  if (!ok) return

  changingStatus.value = true
  try {
    const res = await followupApi.updateStaffStatus({ id, status: 0 })
    if (!res.success) {
      window.alert(res.message || '更新失败')
      return
    }
    window.alert('已更新为离职')
    await loadDetail(String(id))
  } catch (e: any) {
    window.alert(e?.message || '更新失败')
  } finally {
    changingStatus.value = false
  }
}

function editEmail() {
  if (!detail.value) return
  const current = detail.value.email == null ? '' : String(detail.value.email)
  const input = window.prompt('请输入邮箱', current)
  if (input === null) return
  const next = input.trim()
  detail.value.email = next ? next : undefined
}

function formatCaseId(id?: number | string | null) {
  if (id === undefined || id === null) return '--'
  const num = Number(id)
  if (Number.isNaN(num)) return String(id)
  return `1${String(Math.floor(num)).padStart(3, '0')}`
}

function formatDate(dateStr?: string | null): string {
  if (!dateStr) return '——'
  const str = String(dateStr).trim()
  if (str.includes(' ')) {
    return str.split(' ')[0]
  }
  return str
}

function getStatusClass(status: string): string {
  if (status === '在职') {
    return 'badge badge-success'
  } else {
    return 'badge badge-danger'
  }
}

function getRiskLevelClass(level: string): string {
  if (level?.includes('高')) {
    return 'badge badge-danger'
  } else if (level?.includes('中')) {
    return 'badge badge-warning'
  } else {
    return 'badge badge-success'
  }
}
</script>

<style scoped>
/* =========================
   仅美化，不改布局（followup/StaffDetail.vue）
   ========================= */

.staff-detail-page {
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

.info-email-wrap {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.btn-mini {
  padding: 4px 10px;
  font-size: 12px;
  line-height: 1.2;
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

.btn-danger {
  background: rgba(254, 226, 226, 0.9);
  border: 2px solid rgba(248, 113, 113, 0.45);
  color: #991b1b;
}

.btn-danger:hover:not(:disabled) {
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.12);
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
  display: flex;
  justify-content: space-between;
  align-items: center;
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

.card-subtitle {
  font-size: 13px;
  color: #94a3b8;
  font-weight: 600;
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
}

.staff-name {
  font-size: 18px;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: 0.2px;
}

.status-wrap {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  text-align: center;
  padding: 16px;
  background: rgba(248, 250, 252, 0.72);
  border-radius: 16px;
  border: 1px solid rgba(226, 232, 240, 0.75);
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.03);
}

.stat-label {
  font-size: 12px;
  color: #64748b;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 4px;
}

.stat-value.primary {
  color: #667eea;
}

.stat-value.success {
  color: #10b981;
}

.stat-value.warning {
  color: #f59e0b;
}

.stat-unit {
  font-size: 12px;
  color: #94a3b8;
}

.badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 6px 12px;
  min-height: 28px;
  border-radius: 999px;
  font-size: 12.5px;
  font-weight: 700;
  letter-spacing: 0.2px;
  border: 1px solid rgba(226, 232, 240, 0.85);
  box-shadow: 0 6px 14px rgba(15, 23, 42, 0.06);
}

.badge-success {
  background: linear-gradient(135deg, rgba(209, 250, 229, 0.92) 0%, rgba(187, 247, 208, 0.68) 100%);
  color: #065f46;
  border-color: rgba(16, 185, 129, 0.38);
}

.badge-warning {
  background: linear-gradient(135deg, rgba(254, 249, 195, 0.95) 0%, rgba(253, 230, 138, 0.55) 100%);
  color: #92400e;
  border-color: rgba(245, 158, 11, 0.45);
}

.badge-danger {
  background: linear-gradient(135deg, rgba(254, 202, 202, 0.95) 0%, rgba(254, 226, 226, 0.85) 100%);
  color: #991b1b;
  border-color: rgba(248, 113, 113, 0.55);
}

.table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
}

.table thead th {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.08) 0%, rgba(118, 75, 162, 0.06) 100%);
  font-size: 12.5px;
  font-weight: 700;
  color: #64748b;
  padding: 14px 16px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.85);
  letter-spacing: 0.2px;
  white-space: nowrap;
}

.table tbody td {
  font-size: 13.5px;
  color: #1f2937;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.45);
  white-space: nowrap;
}

.table tbody tr {
  transition: background 0.22s ease;
}

.table tbody tr:hover {
  background: rgba(102, 126, 234, 0.05);
}

.empty {
  text-align: center;
  padding: 40px;
  color: #94a3b8;
  font-weight: 600;
  border-radius: 16px;
  background: rgba(248, 250, 252, 0.7);
  border: 1px dashed rgba(203, 213, 225, 0.85);
}

.link-primary {
  color: #667eea;
  text-decoration: none;
  font-weight: 700;
}

.link-primary:hover {
  text-decoration: underline;
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
</style>

