<template>
  <div class="followup-import-page">
    <div class="page-header">
      <div class="page-title">导入随访数据</div>
      <div class="page-subtitle">粘贴 CSV 文本进行导入</div>
    </div>

    <div class="card">
      <div class="card-title">粘贴 CSV 文本进行导入</div>
      <div class="card-subtitle">
        格式与导出文件相同：姓名,随访日期,身份证号,手机号,症状备注,随访人员,系统初筛,随访状态,随访时间
      </div>
      <form @submit.prevent="handleImport">
        <textarea
          v-model="csvData"
          rows="12"
          class="csv-textarea"
          placeholder="请粘贴CSV格式的数据..."
        ></textarea>
        <div class="form-actions">
          <button type="submit" class="btn btn-primary" :disabled="loading || !csvData.trim()">
            {{ loading ? '导入中...' : '导入' }}
          </button>
          <RouterLink to="/alert/followup" class="btn btn-ghost">返回随访列表</RouterLink>
        </div>
        <div v-if="message" :class="['import-message', messageType]">
          {{ message }}
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import { followupApi } from '@/api/followup'

const loading = ref(false)
const csvData = ref('')
const message = ref('')
const messageType = ref<'success' | 'error'>('success')

async function handleImport() {
  if (!csvData.value.trim()) {
    message.value = '请输入CSV数据'
    messageType.value = 'error'
    return
  }

  loading.value = true
  message.value = ''
  
  try {
    const result = await followupApi.importData(csvData.value)
    if (result.success) {
      message.value = result.message || '导入成功'
      messageType.value = 'success'
      csvData.value = '' // 清空输入
    } else {
      message.value = result.message || '导入失败'
      messageType.value = 'error'
    }
  } catch (error: any) {
    message.value = error.message || '导入失败，请检查网络连接'
    messageType.value = 'error'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.followup-import-page {
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

.card-subtitle {
  font-size: 13px;
  color: #718096;
  margin-bottom: 20px;
  padding: 12px 16px;
  background: rgba(102, 126, 234, 0.05);
  border-radius: 8px;
  border-left: 4px solid #667eea;
  font-weight: 500;
}

.csv-textarea {
  width: 100%;
  padding: 16px 20px;
  font-size: 13px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  resize: vertical;
  min-height: 240px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  transition: all 0.2s ease;
  line-height: 1.5;
  color: #2d3748;
}

.csv-textarea:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
  background: #ffffff;
  outline: none;
}

.csv-textarea::placeholder {
  color: #a0aec0;
  font-style: italic;
}

.form-actions {
  display: flex;
  gap: 16px;
  margin-top: 24px;
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

.import-message {
  margin-top: 20px;
  padding: 16px 20px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 500;
  backdrop-filter: blur(10px);
  border: 2px solid;
  position: relative;
  overflow: hidden;
}

.import-message::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  opacity: 0.8;
}

.import-message.success {
  background: rgba(198, 246, 213, 0.9);
  color: #2f855a;
  border-color: #68d391;
}

.import-message.success::before {
  background: linear-gradient(90deg, #48bb78, #38a169);
}

.import-message.error {
  background: rgba(254, 226, 226, 0.9);
  color: #c53030;
  border-color: #fc8181;
}

.import-message.error::before {
  background: linear-gradient(90deg, #f56565, #e53e3e);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .followup-import-page {
    padding: 12px;
  }
  
  .csv-textarea {
    min-height: 200px;
    font-size: 12px;
  }
  
  .form-actions {
    flex-direction: column;
  }
  
  .card-subtitle {
    font-size: 12px;
    padding: 10px 14px;
  }
}
</style>

