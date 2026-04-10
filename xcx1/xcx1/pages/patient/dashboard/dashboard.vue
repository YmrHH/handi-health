<template>
  <view class="app-page fa">
    <view class="fa-head app-card app-card--glass">
      <view class="flex-row items-start justify-between">
        <view class="flex-col">
          <text class="fa-title">随访关联</text>
          <text class="fa-sub">将身体数据趋势与随访/康养任务关联分析；异常指标会触发重点提醒</text>
        </view>
        <view class="fa-chip">{{ rangeText }}</view>
      </view>

      <view class="fa-sum flex-row justify-between">
        <view class="fa-sum-item">
          <text class="fa-sum-k">本周任务</text>
          <text class="fa-sum-v">{{ rehab.done }}/{{ rehab.total }}</text>
        </view>
        <view class="fa-sum-item">
          <text class="fa-sum-k">异常次数</text>
          <text class="fa-sum-v">{{ abnormalCount }}</text>
        </view>
        <view class="fa-sum-item">
          <text class="fa-sum-k">下次随访</text>
          <text class="fa-sum-v">{{ rehab.next }}</text>
        </view>
      </view>
    </view>

    <view class="fa-section flex-row items-center justify-between">
      <text class="app-section-title">趋势概览</text>
      <view class="fa-link flex-row items-center" @tap="refreshMock">
        <text class="fa-link-text">刷新</text>
        <text class="fa-arrow">›</text>
      </view>
    </view>

    <view class="fa-trends">
      <view class="fa-trend app-card" v-for="m in metrics" :key="m.key">
        <view class="flex-row items-center justify-between">
          <text class="fa-trend-title">{{ m.label }}</text>
          <text :class="'fa-trend-chip ' + m.level">{{ m.levelText }}</text>
        </view>
        <view class="fa-trend-val flex-row items-baseline">
          <text class="fa-trend-num">{{ m.value }}</text>
          <text class="fa-trend-unit ml-8">{{ m.unit }}</text>
        </view>
        <text class="fa-trend-hint">{{ m.hint }}</text>
        <view class="fa-spark"></view>
      </view>
    </view>

    <view class="fa-section">
      <text class="app-section-title">关联提示</text>
    </view>

    <view class="fa-tips app-card">
      <view class="fa-tip" v-for="(t, idx) in tips" :key="idx">
        <text class="fa-tip-dot"></text>
        <text class="fa-tip-text">{{ t }}</text>
      </view>
      <view class="btn-secondary fa-tip-btn" @tap="goRehab">进入康养任务</view>
    </view>

    <view class="fa-section flex-row items-center justify-between">
      <text class="app-section-title">最近随访</text>
      <view class="fa-link flex-row items-center" @tap="goFollowup">
        <text class="fa-link-text">查看记录</text>
        <text class="fa-arrow">›</text>
      </view>
    </view>

    <view class="fa-list app-card">
      <view class="fa-row" v-for="r in records" :key="r.id">
        <view class="flex-row items-center justify-between">
          <text class="fa-row-title">{{ r.title }}</text>
          <text class="fa-row-time">{{ r.time }}</text>
        </view>
        <text class="fa-row-sub">{{ r.note }}</text>
      </view>
    </view>

    <view class="fa-footnote">
      <text class="fa-footnote-text">提示：趋势分析为辅助信息，最终建议以医生端审核结果为准。</text>
    </view>
  </view>
</template>

<script>
import { getDailyMetricsLatest } from '@/utils/metrics-store.js';
import { getFollowupAssociation } from '@/api/patient.js';
export default {
  data() {
    return {
      rangeText: '近7天',
      metrics: [
        { key: 'bp', label: '血压', value: '132/85', unit: 'mmHg', level: 'mid', levelText: '关注', hint: '与低盐饮食任务强关联' },
        { key: 'hr', label: '心率', value: '68', unit: '次/分', level: 'ok', levelText: '稳定', hint: '与睡眠时长/情绪相关' },
        { key: 'glucose', label: '血糖', value: '6.8', unit: 'mmol/L', level: 'mid', levelText: '关注', hint: '与用药打卡/饮食记录相关' },
        { key: 'weight', label: '体温', value: '36.6', unit: '℃', level: 'ok', levelText: '正常', hint: '建议每日同一时间测量' }
      ],
      rehab: { done: 3, total: 5, next: '' },
      association: null,
      associationLoading: false,
      associationError: '',

      tips: [
        '若血压连续 3 天偏高，系统会自动增加“血压复测”随访任务。',
        '康养任务完成率 < 60% 时，将触发“依从性提醒”。',
        '医生端可针对异常指标下发“医生健康建议”，小程序端在“我的”页展示。'
      ],
      records: [
        { id: 1, title: '随访：血压复测', time: '2026-01-02', note: '建议晚餐低盐，连续监测 3 天' },
        { id: 2, title: '随访：用药依从', time: '2025-12-28', note: '按时服药，记录餐后血糖' }
      ]
    };
  },

  computed: {
    abnormalCount() {
      return this.metrics.filter(m => m.level === 'high' || m.level === 'mid').length;
    }
  },

  onShow() {
    this.loadFromStorage();
    this.loadAssociation();
  },

  methods: {
    mapFollowupStatus(v) {
      const s = String(v || '').toUpperCase();
      const map = { ASSIGNED: '待随访', IN_PROGRESS: '随访中', COMPLETED: '已随访', DONE: '已随访', FINISHED: '已随访' };
      return map[s] || String(v || '');
    },
    mapResultStatus(v) {
      const s = String(v || '').toUpperCase();
      const map = { ASSIGNED: '待随访', IN_PROGRESS: '随访中', COMPLETED: '已随访', DONE: '已随访', FINISHED: '已随访' };
      return map[s] || String(v || '');
    },
    formatDateText(v) {
      if (!v) return '';
      return String(v).replace('T', ' ').slice(0, 10);
    },
    parseDateMs(v) {
      if (!v) return NaN;
      const s = String(v).trim();
      if (!s) return NaN;
      const normalized = s.replace(/\./g, '-').replace('T', ' ').replace(/\//g, '-');
      const ts = Date.parse(normalized.replace(' ', 'T'));
      return Number.isFinite(ts) ? ts : NaN;
    },
    pickLatestFollowupRecord(list) {
      const rows = Array.isArray(list) ? list : [];
      let best = null;
      let bestTs = NaN;
      rows.forEach((item) => {
        const ts = this.parseDateMs(item && (item.followupTime || item.createdAt || item.measuredAt || item.time));
        if (!Number.isFinite(ts)) return;
        if (!Number.isFinite(bestTs) || ts > bestTs) {
          best = item;
          bestTs = ts;
        }
      });
      return best;
    },
    pickEarliestPendingTask(list) {
      const rows = Array.isArray(list) ? list : [];
      let best = null;
      let bestTs = NaN;
      rows.forEach((item) => {
        const ts = this.parseDateMs(item && (item.planTime || item.dueAt || item.plan_time || item.nextFollowupDate || item.nextPlanTime || item.nextFollowupTime));
        if (!Number.isFinite(ts)) return;
        if (!Number.isFinite(bestTs) || ts < bestTs) {
          best = item;
          bestTs = ts;
        }
      });
      return best;
    },
    async loadAssociation() {
      this.associationLoading = true;
      this.associationError = '';
      try {
        const res = await getFollowupAssociation();
        const data = (res && res.data !== undefined) ? res.data : res;
        this.association = data || null;

        const pendingTasks = Array.isArray(data?.pendingTasks)
          ? data.pendingTasks
          : (Array.isArray(data?.tasks) ? data.tasks : []);
        const recs = (Array.isArray(data?.recentRecords) ? data.recentRecords : (Array.isArray(data?.records) ? data.records : null))
          || (data?.lastFollowup ? [data.lastFollowup] : []);
        const latestRecord = this.pickLatestFollowupRecord(recs);
        const earliestPendingTask = this.pickEarliestPendingTask(pendingTasks);

        let next = (data && (data.nextFollowup?.planTime || data.nextFollowup?.dueAt || data.nextDueAt || data.nextDue || data.nextFollowupTime))
          || (earliestPendingTask && (earliestPendingTask.planTime || earliestPendingTask.dueAt || earliestPendingTask.plan_time))
          || (latestRecord && (latestRecord.nextFollowupDate || latestRecord.nextPlanTime || latestRecord.nextFollowupTime))
          || '';
        const last = (data?.lastFollowup && (data.lastFollowup.followupTime || data.lastFollowup.createdAt || data.lastFollowup.time))
          || (latestRecord && (latestRecord.followupTime || latestRecord.createdAt || latestRecord.time))
          || '';
        const nextTs = this.parseDateMs(next);
        const lastTs = this.parseDateMs(last);
        if (Number.isFinite(nextTs) && Number.isFinite(lastTs) && nextTs < lastTs) {
          const latestRecordNext = latestRecord && (latestRecord.nextFollowupDate || latestRecord.nextPlanTime || latestRecord.nextFollowupTime || '');
          const latestRecordNextTs = this.parseDateMs(latestRecordNext);
          next = (Number.isFinite(latestRecordNextTs) && latestRecordNextTs >= lastTs) ? latestRecordNext : '';
        }
        this.rehab.next = next ? this.formatDateText(next) : '';

        if (Array.isArray(recs) && recs.length) {
          this.records = recs
            .slice()
            .sort((a, b) => {
              const ta = this.parseDateMs(a && (a.followupTime || a.measuredAt || a.createdAt || a.time));
              const tb = this.parseDateMs(b && (b.followupTime || b.measuredAt || b.createdAt || b.time));
              if (!Number.isFinite(ta) && !Number.isFinite(tb)) return 0;
              if (!Number.isFinite(ta)) return 1;
              if (!Number.isFinite(tb)) return -1;
              return tb - ta;
            })
            .slice(0, 5)
            .map((r) => ({
              id: r.id || r.recordId || r.taskId || r.relatedTaskId,
              title: this.mapResultStatus(r.title || r.conclusion || r.resultStatus || r.result) || '随访记录',
              time: String(r.followupTime || r.measuredAt || r.createdAt || r.time || '').replace('T', ' ').slice(0, 16),
              note: r.contentSummary || r.summary || r.symptomChange || r.note || ''
            }));
        }

        const tips = (Array.isArray(data?.tips) && data.tips) || null;
        if (tips && tips.length) this.tips = tips;
      } catch (e) {
        this.associationError = (e && e.message) ? e.message : '加载失败';
        // 保持原 mock，不阻塞页面
      } finally {
        this.associationLoading = false;
      }
    },

    loadFromStorage() {
      try {
        const latest = getDailyMetricsLatest();
        if (latest && typeof latest === 'object' && Array.isArray(latest.metrics)) {
          // 仅取前 4 项用于趋势概览
          this.metrics = latest.metrics.slice(0, 4).map(m => ({
            key: m.key,
            label: m.label,
            value: m.value,
            unit: m.unit,
            level: m.trend === 'up' ? 'mid' : (m.trend === 'down' ? 'ok' : 'ok'),
            levelText: m.trendText || '稳定',
            hint: m.hint || '与康养任务关联分析'
          }));
        }

        const rs = uni.getStorageSync('followup_summary') || uni.getStorageSync('rehab_summary');
        if (rs && typeof rs === 'object') {
          this.rehab = { ...this.rehab, ...rs };
        }

        const tips = uni.getStorageSync('followup_tips');
        if (Array.isArray(tips) && tips.length) this.tips = tips;

        const rec = uni.getStorageSync('followup_records');
        if (Array.isArray(rec) && rec.length) this.records = rec;
      } catch (e) {}
    },

    refreshMock() {
      this.loadFromStorage();
      this.loadAssociation();
      uni.showToast({ title: '已刷新', icon: 'success' });
    },

    goRehab() {
      uni.navigateTo({ url: '/pages/patient/rehab/rehab' });
    },

    goFollowup() {
      uni.navigateTo({ url: '/pages/patient/followupDetail/followupDetail' });
    }
  }
};
</script>

<style src="./dashboard.css"></style>
