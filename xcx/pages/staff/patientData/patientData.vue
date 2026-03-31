<template>
  <view class="page pd-page">
    <!-- 患者信息 -->
    <view class="card">
      <view class="pd-header">
        <view>
          <view class="pd-name">
            {{ profile.name || '未知患者' }}
            <text v-if="profile.age || profile.gender">（{{ profile.age || '' }}岁{{ profile.gender || '' }}）</text>
          </view>
          <view class="pd-sub">病种：{{ profile.disease || '—' }} · 风险：{{ profile.riskText || '—' }}</view>
        </view>
        <view :class="'risk-tag ' + (profile.riskLevel || 'mid')">
          {{ profile.riskText || '—' }}
        </view>
      </view>
    </view>

    <!-- 今日健康指标 -->
    <view class="card">
      <view class="section-title">今日健康指标</view>
      <view class="grid">
        <view class="cell">
          <view class="label">收缩压</view>
          <view class="value">{{ today.sbp ?? '--' }} <text class="unit">mmHg</text></view>
        </view>
        <view class="cell">
          <view class="label">舒张压</view>
          <view class="value">{{ today.dbp ?? '--' }} <text class="unit">mmHg</text></view>
        </view>
        <view class="cell">
          <view class="label">心率</view>
          <view class="value">{{ today.hr ?? '--' }} <text class="unit">次/分</text></view>
        </view>
        <view class="cell">
          <view class="label">体重</view>
          <view class="value">{{ today.weight ?? '--' }} <text class="unit">kg</text></view>
        </view>
        <view class="cell">
          <view class="label">体温</view>
          <view class="value">{{ today.temp ?? '--' }} <text class="unit">℃</text></view>
        </view>
        <view class="cell">
          <view class="label">步数</view>
          <view class="value">{{ today.steps ?? '--' }} <text class="unit">步</text></view>
        </view>
      </view>
      <view class="tip">* 数据来自患者端“填写健康指标”。</view>
    </view>

    
    <!-- 健康指标查看（历史） -->
    <view class="card">
      <view class="section-title row-between">
        <text>健康指标查看</text>
        <view class="seg">
          <view class="seg-item" :class="historyLimit===7?'active':''" @tap="setHistoryLimit(7)">7天</view>
          <view class="seg-item" :class="historyLimit===30?'active':''" @tap="setHistoryLimit(30)">30天</view>
          <view class="seg-item" :class="historyLimit===90?'active':''" @tap="setHistoryLimit(90)">90天</view>
        </view>
      </view>

      <view v-if="historyList.length===0" class="empty">暂无历史健康指标</view>
      <view v-else class="history-list">
        <view class="history-item" v-for="(h,i) in historyList" :key="i">
          <view class="history-time">{{ h.time || '—' }}</view>
          <view class="history-metrics">
            <text class="hm">血压 {{ h.sbp || '--' }}/{{ h.dbp || '--' }}</text>
            <text class="hm">心率 {{ h.hr || '--' }}</text>
            <text class="hm">体重 {{ h.weight || '--' }}</text>
            <text class="hm">体温 {{ h.temp || '--' }}</text>
          </view>
        </view>
      </view>
      <view class="tip">* 历史数据来自患者端健康指标上报。</view>
    </view>

    <!-- 随访小结 -->
    <view class="card">
      <view class="section-title">随访小结</view>
      <view v-if="!followupLatest" class="empty">暂无随访记录</view>
      <view v-else>
        <view class="kv">
          <text class="k">随访时间</text>
          <text class="v">{{ followupLatest.followupTimeText || '—' }}</text>
        </view>
        <view class="kv">
          <text class="k">任务类型</text>
          <text class="v">{{ followupLatest.followupTypeText || followupLatest.followupType || '—' }}</text>
        </view>
        <view class="kv">
          <text class="k">症状变化</text>
          <text class="v">{{ followupLatest.symptomChange || '—' }}</text>
        </view>
        <view class="kv">
          <text class="k">随访小结</text>
          <text class="v">{{ followupLatest.contentSummary || '—' }}</text>
        </view>
        <view class="kv">
          <text class="k">医护建议</text>
          <text class="v">{{ followupLatest.advice || '—' }}</text>
        </view>
        <view class="kv">
          <text class="k">下次随访</text>
          <text class="v">{{ followupLatest.nextFollowupText || '—' }}</text>
        </view>
      </view>
      <view class="tip">* 数据来自医护端提交的随访记录。</view>
    </view>

    <!-- 用药与不良反应 -->
    <view class="card">
      <view class="section-title">用药与不良反应</view>
      <view v-if="!followupLatest" class="empty">暂无随访记录</view>
      <view v-else>
        <view class="kv">
          <text class="k">服用药品</text>
          <text class="v">{{ followupLatest.medicineName || followupLatest.medPlanSummary || '—' }}</text>
        </view>
        <view class="kv">
          <text class="k">用药依从性</text>
          <text class="v">{{ followupLatest.medAdherence || '—' }}</text>
        </view>
        <view class="kv">
          <text class="k">不良反应</text>
          <text class="v">{{ followupLatest.adverseReaction || '—' }}</text>
        </view>
      </view>
      <view class="tip">* 若暂未录入用药/不良反应，请在随访表单中补充后再查看。</view>
    </view>


    <!-- 最近康复任务 -->
    <view class="card">
      <view class="section-title">最近康复任务</view>
      <view v-if="rehabTasks.length === 0" class="empty">暂无任务</view>
      <view v-else class="task-list">
        <view v-for="(t, i) in rehabTasks" :key="i" class="task-item">
          <view class="task-title">{{ t.title || '康复任务' }}</view>
          <view class="task-sub">状态：{{ t.statusText || t.status || '—' }} · 日期：{{ t.date || '—' }}</view>
        </view>
      </view>
    </view>

    <!-- 给患者发送建议 -->
    <view class="card">
      <view class="section-title">给患者发送建议</view>
      <textarea class="advice-textarea" v-model="advice" placeholder="输入建议内容，发送后患者端将在消息中心收到（建议尽量具体，例如饮食/运动/复诊时间等）"></textarea>
      <button class="btn" @tap="onSendAdvice">发送建议</button>
      <view class="tip">* 该功能依赖后端接口：POST /api/staff/patients/{id}/advice</view>
    </view>
  </view>
</template>

<script>
import { USE_MOCK_FALLBACK } from '@/config/api.js';
import { getPatientData, sendPatientAdvice } from '@/api/staff.js';

function mapRiskToUi(v) {
  const s = String(v || '').toLowerCase();
  if (s.includes('high') || s.includes('高')) return { level: 'high', text: '高风险' };
  if (s.includes('low') || s.includes('低')) return { level: 'low', text: '低风险' };
  return { level: 'mid', text: '中风险' };
}

export default {
  data() {
    return {
      patientId: '',
      profile: { name: '', age: '', gender: '', disease: '', riskLevel: 'mid', riskText: '—' },
      today: { sbp: '', dbp: '', hr: '', weight: '', temp: '', steps: '' },
      rehabTasks: [],
      historyLimit: 30,
      historyList: [],
      followupLatest: null,
      advice: ''
    };
  },
  onLoad(query) {
    this.patientId = query.id || query.patientId || query.patient_id || '';
    // 允许从上级页面透传展示信息（即使后端没返回也不会空）
    this.profile.name = (query.name || query.patientName) ? decodeURIComponent(query.name || query.patientName) : '';
    this.profile.age = query.age || '';
    this.profile.gender = query.gender ? decodeURIComponent(query.gender) : '';
    this.loadData();
  },
  methods: {
    formatDateTime(v) {
      if (!v) return '';
      // v 可能是 '2026-01-15 11:22:33' 或 '2026-01-15T11:22:33'
      const s = String(v).replace('T', ' ');
      return s.length > 19 ? s.slice(0, 19) : s;
    },
    setHistoryLimit(n) {
      const v = Number(n) || 30;
      if (this.historyLimit === v) return;
      this.historyLimit = v;
      this.loadData();
    },

    loadData() {
      if (!this.patientId) {
        uni.showToast({ title: '缺少患者ID', icon: 'none' });
        return;
      }
      uni.showLoading({ title: '加载中…', mask: true });
      getPatientData(this.patientId, { historyLimit: this.historyLimit })
        .then((res) => {
          // 兼容：res 可能是 {success,code,data} 也可能直接是 data
          const data = (res && res.data) ? res.data : res;
          const d = data && data.data ? data.data : data;
          const p = (d && (d.profile || d.patient)) ? (d.profile || d.patient) : (d || {});
          const pbi = (p && (p.basicInfo || p.basic_info)) ? (p.basicInfo || p.basic_info) : null;

          const risk = mapRiskToUi(p.riskLevel || p.risk || p.riskText);
          this.profile = {
            name: p.name || this.profile.name,
            age: p.age || this.profile.age,
            gender: p.gender || this.profile.gender,
            disease: p.disease || p.mainDisease || (pbi ? (pbi.ext3 || pbi.mainDisease || pbi.disease) : '') || '—',
            riskLevel: risk.level,
            riskText: p.riskText || risk.text
          };
          // 今日指标：后端返回 today:{rows,latest}，这里优先取 latest，并将字段规整为前端使用的 sbp/dbp/hr/weight/temp
          let latest = (d && d.today) ? d.today.latest : null;
          if (!latest && d && d.today && Array.isArray(d.today.rows) && d.today.rows.length > 0) {
            latest = d.today.rows[0];
          }
          if (!latest) {
            latest = (d && d.latest) ? d.latest : {};
          }
          this.today = {
            sbp: latest.sbp ?? latest.SBP ?? '',
            dbp: latest.dbp ?? latest.DBP ?? '',
            hr: latest.hr ?? latest.heartRate ?? latest.heart_rate ?? latest.heartRate ?? '',
            weight: latest.weight ?? latest.weightKg ?? latest.weight_kg ?? latest.weightKg ?? '',
            temp: latest.temp ?? latest.temperatureC ?? latest.temperature_c ?? '',
            steps: latest.steps ?? latest.stepCount ?? latest.ext1 ?? '' // steps 若后端无字段，可用 ext1 承载
          };
          this.rehabTasks = d.rehabTasks || d.tasks || [];
          // 历史健康指标
          const hist = d.history || d.historyList || [];
          this.historyList = (Array.isArray(hist) ? hist : []).map((h) => {
            const time = this.formatDateTime(h.measuredAt || h.measured_at || h.time || h.date);
            return {
              time,
              sbp: h.sbp ?? '',
              dbp: h.dbp ?? '',
              hr: h.hr ?? h.heartRate ?? h.heart_rate ?? '',
              weight: h.weight ?? h.weightKg ?? h.weight_kg ?? '',
              temp: h.temp ?? h.temperatureC ?? h.temperature_c ?? '',
            };
          });

          // 最新随访记录（用于：随访小结、用药与不良反应）
          const fu = d.followupLatest || d.latestFollowup || (Array.isArray(d.followups) ? d.followups[0] : null) || null;
          if (fu) {
            const t = this.formatDateTime(fu.followupTime || fu.followup_time || fu.createdAt || fu.created_at);
            const nextDate = this.formatDateTime(fu.nextFollowupDate || fu.next_followup_date);
            const nextType = fu.nextFollowupType || fu.next_followup_type || '';
            this.followupLatest = {
              ...fu,
              followupTimeText: t || '—',
              followupTypeText: fu.followupType || fu.followup_type || '',
              advice: fu.advice || fu.ext1 || '',
              medicineName: fu.medicineName || fu.medPlanSummary || fu.med_plan_summary || fu.ext2 || '',
              nextFollowupText: (nextType || nextDate) ? `${nextType || ''}${nextType && nextDate ? ' · ' : ''}${nextDate || ''}` : ''
            };
          } else {
            this.followupLatest = null;
          }
        })
        .catch(() => {
          if (!USE_MOCK_FALLBACK) {
            uni.showToast({ title: '加载失败', icon: 'none' });
            return;
          }
          // 本地兜底 Demo
          this.profile = {
            ...this.profile,
            disease: '高血压',
            riskLevel: 'mid',
            riskText: '中风险'
          };
          this.today = { sbp: '138', dbp: '88', hr: '72', weight: '70', temp: '36.5', steps: '3200' };
          this.rehabTasks = [
            { title: '步行 30 分钟', statusText: '待完成', date: '今天' },
            { title: '低盐饮食打卡', statusText: '已完成', date: '昨天' }
          ];
          this.historyList = [
            { time: '今天', sbp: '138', dbp: '88', hr: '72', weight: '70', temp: '36.5' },
            { time: '昨天', sbp: '140', dbp: '90', hr: '74', weight: '70', temp: '36.6' }
          ];
          this.followupLatest = {
            followupTimeText: '昨天 10:00',
            followupTypeText: '电话随访',
            symptomChange: '气短稍有加重',
            contentSummary: '体重略升，建议控制饮食与按时复查。',
            advice: '低盐饮食 + 每日步行 30 分钟',
            medicineName: '氨氯地平 5mg',
            medAdherence: '规律服药',
            adverseReaction: '无明显不适',
            nextFollowupText: '常规随访 · 下周'
          };
        })
        .finally(() => uni.hideLoading());
    },

    onSendAdvice() {
      const content = (this.advice || '').trim();
      if (!content) {
        uni.showToast({ title: '请填写建议内容', icon: 'none' });
        return;
      }
      uni.showLoading({ title: '发送中…', mask: true });
      sendPatientAdvice(this.patientId, content)
        .then(() => {
          uni.showToast({ title: '已发送', icon: 'success' });
          this.advice = '';
        })
        .catch(() => {
          if (!USE_MOCK_FALLBACK) {
            uni.showToast({ title: '发送失败', icon: 'none' });
            return;
          }
          // Demo：兜底当作成功
          uni.showToast({ title: '已发送（本地演示）', icon: 'success' });
          this.advice = '';
        })
        .finally(() => uni.hideLoading());
    }
  }
};
</script>

<style>
.pd-page { padding: 24rpx; }
.card { background: #fff; border-radius: 20rpx; padding: 24rpx; margin-bottom: 20rpx; box-shadow: 0 10rpx 24rpx rgba(0,0,0,0.06); }
.pd-header { display:flex; justify-content:space-between; align-items:flex-start; gap: 20rpx; }
.pd-name { font-size: 36rpx; font-weight: 700; color:#111; }
.pd-sub { margin-top: 8rpx; font-size: 26rpx; color:#666; }
.section-title { font-size: 30rpx; font-weight: 700; margin-bottom: 16rpx; color:#111; }
.grid { display:flex; flex-wrap:wrap; gap: 16rpx; }
.cell { width: calc(50% - 8rpx); background:#F6F7FF; border-radius: 16rpx; padding: 16rpx; }
.label { font-size: 24rpx; color:#666; }
.value { margin-top: 6rpx; font-size: 30rpx; font-weight: 700; color:#111; }
.unit { font-size: 22rpx; color:#666; margin-left: 6rpx; }
.risk-tag { padding: 10rpx 16rpx; border-radius: 999rpx; font-size: 24rpx; border: 2rpx solid #F2D5A6; color:#B36B00; background:#FFF6E8; white-space:nowrap; }
.risk-tag.high { border-color:#F9C2C2; color:#B00000; background:#FFF0F0; }
.risk-tag.low { border-color:#BDE6C7; color:#0B6B2B; background:#F0FFF5; }
.task-item { padding: 16rpx 0; border-bottom: 1rpx solid #F1F1F5; }
.task-item:last-child { border-bottom: none; }
.task-title { font-size: 28rpx; font-weight: 600; color:#222; }
.task-sub { margin-top: 6rpx; font-size: 24rpx; color:#666; }
.empty { font-size: 26rpx; color:#999; padding: 10rpx 0; }
.advice-textarea { width:100%; min-height: 180rpx; padding: 16rpx; border-radius: 16rpx; background:#F6F7FF; font-size: 26rpx; box-sizing:border-box; }
.btn { margin-top: 16rpx; background:#2E6BFF; color:#fff; border-radius: 999rpx; }
.tip { margin-top: 12rpx; font-size: 22rpx; color:#999; }

.row-between{ display:flex; flex-direction:row; align-items:center; justify-content:space-between; }
.seg{ display:flex; flex-direction:row; gap: 12rpx; }
.seg-item{ padding: 10rpx 18rpx; border-radius: 999rpx; background: #f2f4ff; color:#4b5bff; font-size: 24rpx; }
.seg-item.active{ background:#4b5bff; color:#fff; }
.kv{ display:flex; flex-direction:row; justify-content:space-between; gap: 18rpx; padding: 12rpx 0; border-bottom: 1rpx solid #f3f4f6; }
.k{ color:#6b7280; font-size: 26rpx; width: 180rpx; flex-shrink:0; }
.v{ color:#111827; font-size: 26rpx; flex:1; text-align:right; }
.history-list{ margin-top: 10rpx; }
.history-item{ padding: 14rpx 0; border-bottom: 1rpx solid #f3f4f6; }
.history-time{ color:#374151; font-size: 26rpx; margin-bottom: 6rpx; }
.history-metrics{ display:flex; flex-direction:row; flex-wrap:wrap; gap: 14rpx; }
.hm{ font-size: 24rpx; color:#6b7280; }
</style>
