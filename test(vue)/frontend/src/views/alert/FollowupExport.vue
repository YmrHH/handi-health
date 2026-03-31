<template>
  <div class="followup-export-page">
    <div class="page-header">
      <div class="page-title">导出随访数据</div>
      <div class="page-subtitle">导出随访数据为CSV文件</div>
    </div>

    <div class="card">
      <div class="card-title">导出筛选条件</div>
      <form @submit.prevent="handleExport" class="export-form">
        <div class="form-row">
          <label>随访状态：</label>
          <select v-model="filters.status" class="input">
            <option value="">全部状态</option>
            <option value="PENDING">待随访</option>
            <option value="DONE">已就诊</option>
            <option value="CANCELLED">已取消</option>
          </select>
        </div>

        <div class="form-row">
          <label>责任医生：</label>
          <input v-model="filters.doctor" class="input" type="text" placeholder="医生姓名" />
        </div>

        <div class="form-row">
          <label>开始日期：</label>
          <input v-model="filters.startDate" class="input" type="date" />
        </div>

        <div class="form-row">
          <label>结束日期：</label>
          <input v-model="filters.endDate" class="input" type="date" />
        </div>

        <div class="form-row">
          <label>关键词：</label>
          <input v-model="filters.q" class="input" type="text" placeholder="患者姓名/手机号/身份证号" />
        </div>

        <div class="form-actions">
          <button type="submit" class="btn btn-primary" :disabled="exporting">
            {{ exporting ? '导出中...' : '导出CSV' }}
          </button>
          <RouterLink to="/alert/followup" class="btn btn-ghost">返回</RouterLink>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import { followupApi } from '@/api/followup'

const exporting = ref(false)
const filters = ref({
  status: '',
  doctor: '',
  startDate: '',
  endDate: '',
  q: ''
})

async function handleExport() {
  exporting.value = true
  try {
    await followupApi.exportData(filters.value)
    // 导出会直接下载文件，不需要额外处理
  } catch (error: any) {
    alert('导出失败: ' + (error.message || '请检查网络连接'))
  } finally {
    exporting.value = false
  }
}
</script>

<style scoped>
.followup-export-page {
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
}

.export-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.form-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 0;
  border-bottom: 1px solid rgba(226, 232, 240, 0.3);
}

.form-row:last-of-type {
  border-bottom: none;
}

.form-row label {
  min-width: 120px;
  font-size: 14px;
  color: #2d3748;
  font-weight: 600;
  display: flex;
  align-items: center;
}

.form-row label::before {
  content: '';
  width: 4px;
  height: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 2px;
  margin-right: 8px;
}

.form-row .input {
  flex: 1;
  max-width: 320px;
  padding: 12px 16px;
  border: 2px solid #e2e8f0;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.95);
  transition: all 0.2s ease;
  font-size: 14px;
  font-weight: 500;
}

.form-row .input:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
  background: #ffffff;
}

.form-row select.input {
  cursor: pointer;
}

.form-actions {
  display: flex;
  gap: 16px;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 2px solid rgba(226, 232, 240, 0.3);
}

.btn {
  padding: 12px 24px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  border: 2px solid transparent;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.5s;
}

.btn:hover::before {
  left: 100%;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
  box-shadow: 0 4px 14px 0 rgba(102, 126, 234, 0.39);
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px 0 rgba(102, 126, 234, 0.5);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
  box-shadow: 0 4px 14px 0 rgba(102, 126, 234, 0.2);
}

.btn-ghost {
  background: rgba(255, 255, 255, 0.9);
  border-color: #e2e8f0;
  color: #4a5568;
  backdrop-filter: blur(10px);
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.btn-ghost:hover {
  background: #ffffff;
  border-color: #cbd5e0;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .followup-export-page {
    padding: 12px;
  }
  
  .export-form {
    padding: 20px;
  }
  
  .form-row {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
  }
  
  .form-row label {
    min-width: auto;
  }
  
  .form-row .input {
    max-width: none;
  }
  
  .form-actions {
    flex-direction: column;
  }
}
</style>

