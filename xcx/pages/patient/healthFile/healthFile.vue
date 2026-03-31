<template>
  <view class="app-page hf">
    <!-- 顶部：患者信息 + 同步状态 -->
    <view class="hf-top flex-row items-center justify-between">
      <view class="hf-user flex-row items-center">
        <image class="hf-avatar" :src="avatarUrl" mode="aspectFill"></image>
        <view class="hf-name-pill">
          <text class="hf-name">{{ displayName }}</text>
        </view>
        <view class="hf-sync-pill ml-12">
          <text class="hf-sync-dot"></text>
          <text class="hf-sync-text">{{ syncText }}</text>
        </view>
      </view>

      <view class="hf-actions flex-row items-center">
        <view class="hf-icon-btn" @tap="onRefresh">
          <text class="hf-icon">⟳</text>
        </view>
      </view>
    </view>

    <!-- 概览 -->
    <view class="hf-overview app-card app-card--glass">
      <view class="hf-overview-row flex-row items-start justify-between">
        <view class="flex-col">
          <text class="hf-overview-title">健康档案</text>
          <text class="hf-overview-sub">档案由医生端自动导入并持续更新</text>
        </view>

        <view class="hf-risk">
          <text :class="'hf-risk-chip risk-' + risk.level">{{ risk.text }}</text>
          <text class="hf-risk-score">{{ risk.score }}</text>
          <text class="hf-risk-label">健康分</text>
        </view>
      </view>

      <view class="hf-quick flex-row justify-between">
        <view class="hf-quick-item" @tap="goDashboard">
          <text class="hf-quick-title">每日统计</text>
          <text class="hf-quick-desc">趋势与异常</text>
        </view>
        <view class="hf-quick-item" @tap="goRehab">
          <text class="hf-quick-title">随访任务</text>
          <text class="hf-quick-desc">打卡与依从</text>
        </view>
        <view class="hf-quick-item" @tap="goChat">
          <text class="hf-quick-title">联系医生</text>
          <text class="hf-quick-desc">在线咨询</text>
        </view>
      </view>
    </view>

    <!-- 身体数据 -->
    <view class="hf-section flex-row items-center justify-between">
      <text class="app-section-title">身体数据</text>
      <view class="hf-link flex-row items-center" @tap="goDashboard">
        <text class="hf-link-text">查看趋势</text>
        <text class="hf-arrow">›</text>
      </view>
    </view>

    <view class="hf-metrics">
      <view v-for="m in metrics" :key="m.key" class="hf-metric app-card" @tap="goMetric(m.key)">
        <view class="hf-metric-head flex-row items-center justify-between">
          <text class="hf-metric-title">{{ m.label }}</text>
          <text :class="'hf-metric-trend ' + m.trend">{{ m.trendText }}</text>
        </view>

        <view class="hf-metric-val flex-row items-baseline">
          <text class="hf-metric-num">{{ m.value }}</text>
          <text class="hf-metric-unit ml-8">{{ m.unit }}</text>
        </view>

        <text class="hf-metric-hint">{{ m.hint }}</text>
        <view class="hf-mini" :class="'mini-' + m.key"></view>
      </view>
    </view>

    <!-- 随访关联 -->
    <view class="hf-section flex-row items-center justify-between">
      <text class="app-section-title">随访关联</text>
      <view class="hf-link flex-row items-center" @tap="goFollowup">
        <text class="hf-link-text">查看记录</text>
        <text class="hf-arrow">›</text>
      </view>
    </view>

    <view class="hf-follow app-card app-card--glass" @tap="goRehab">
      <view class="hf-follow-row flex-row items-center justify-between">
        <view class="flex-col">
          <text class="hf-follow-title">本周随访任务</text>
          <text class="hf-follow-sub">异常指标将自动触发重点随访</text>
        </view>
        <view class="hf-follow-badge">
          <text class="hf-follow-badge-num">{{ followup.done }}</text>
          <text class="hf-follow-badge-sep">/</text>
          <text class="hf-follow-badge-total">{{ followup.total }}</text>
        </view>
      </view>

      <view class="hf-follow-grid">
        <view class="hf-follow-item">
          <text class="hf-follow-k">下次随访</text>
          <text class="hf-follow-v">{{ followup.next }}</text>
        </view>
        <view class="hf-follow-item">
          <text class="hf-follow-k">上次随访</text>
          <text class="hf-follow-v">{{ followup.last }}</text>
        </view>
        <view class="hf-follow-item">
          <text class="hf-follow-k">依从建议</text>
          <text class="hf-follow-v">{{ followup.advice }}</text>
        </view>
        <view class="hf-follow-item">
          <text class="hf-follow-k">重点指标</text>
          <text class="hf-follow-v">{{ followup.focus }}</text>
        </view>
      </view>
    </view>

    <!-- 病例信息 -->
    <view class="hf-section flex-row items-center justify-between">
      <text class="app-section-title">病例信息</text>
      <view class="hf-link flex-row items-center" @tap="toggleMedical">
        <text class="hf-link-text">{{ showMedicalMore ? '收起' : '展开' }}</text>
        <text class="hf-arrow">{{ showMedicalMore ? '˄' : '˅' }}</text>
      </view>
    </view>

    <view class="hf-med app-card">
      <view class="hf-med-head flex-row items-start justify-between">
        <view class="flex-col">
          <text class="hf-med-title">{{ medical.docType }}</text>
          <text class="hf-med-sub">{{ medical.hospital }} · {{ medical.department }}</text>
        </view>
        <view class="hf-med-time">
          <text class="hf-med-time-k">入院</text>
          <text class="hf-med-time-v">{{ medical.admissionTime }}</text>
        </view>
      </view>

      <view class="hf-med-block">
        <text class="hf-med-k">主诉</text>
        <text class="hf-med-v">{{ medical.complaint }}</text>
      </view>

      <view class="hf-med-block">
        <text class="hf-med-k">诊断</text>
        <text class="hf-med-v">中医：{{ medical.diagnosisTCM }}</text>
        <text class="hf-med-v">西医：{{ medical.diagnosisWM }}</text>
      </view>

      <view class="hf-med-chips">
        <text class="hf-chip" v-for="(h, idx) in medical.histories" :key="idx">{{ h }}</text>
      </view>

      <view v-if="showMedicalMore" class="hf-med-more">
        <view class="hf-med-split"></view>

        <view class="hf-med-block">
          <text class="hf-med-k">生命体征</text>
          <text class="hf-med-v">{{ medical.vitals }}</text>
        </view>

        <view class="hf-med-block">
          <text class="hf-med-k">手术/过敏</text>
          <text class="hf-med-v">{{ medical.surgery }}</text>
          <text class="hf-med-v">{{ medical.allergy }}</text>
        </view>

        <view class="hf-med-block">
          <text class="hf-med-k">检查要点</text>
          <text class="hf-med-v">{{ medical.imaging }}</text>
        </view>
      </view>
    </view>

    <view class="hf-footnote">
      <text class="hf-footnote-text">档案内容仅供本人查看；医生端与小程序数据一致，平台将严格保护隐私安全。</text>
    </view>
  </view>
</template>

<script>
import { getProfile as apiGetProfile, getHomeSummary, getDailyMeasurementToday, getFollowupAssociation, listFollowups } from '@/api/patient.js';
import { getScopedStorageSync, setScopedStorageSync } from '@/utils/session.js';
import {
  getDailyMetricsLatest,
  buildDefaultLatestMetrics,
  updateDailyMetricsLatest,
  upsertTimeseriesFromPatch
} from '@/utils/metrics-store.js';

function pad2(n) {
  return String(n).padStart(2, '0');
}

function nowHM() {
  const now = new Date();
  return `${pad2(now.getHours())}:${pad2(now.getMinutes())}`;
}

function dateOnly(v) {
  if (!v) return '';
  const s = String(v).trim();
  if (!s) return '';
  if (/^\d{4}-\d{2}-\d{2}$/.test(s)) return s;
  if (/^\d{4}-\d{2}-\d{2}[ T]/.test(s)) return s.slice(0, 10);
  return '';
}

function toNum(v) {
  if (v === null || v === undefined || v === '') return null;
  const n = Number(v);
  return Number.isFinite(n) ? n : null;
}

function hasMetricValue(v) {
  if (v === null || v === undefined) return false;
  const s = String(v).trim();
  return !!s && s !== '--' && s !== '--/--';
}

export default {
  data() {
    return {
      avatarUrl: '/static/assets/avatar-me.png',
      displayName: '用*A',
      syncText: '已同步 · 刚刚',
      showMedicalMore: false,

      risk: { level: 'mid', text: '中风险', score: 78 },

      metrics: buildDefaultLatestMetrics(),

      followup: {
        done: 3,
        total: 5,
        next: '',
        last: '2026-01-02',
        advice: '规律服药/低盐饮食',
        focus: '血压、血糖'
      },

      medical: {
        hospital: '医生端导入',
        department: '—',
        docType: '病例摘要',
        admissionTime: '—',
        complaint: '—',
        diagnosisTCM: '—',
        diagnosisWM: '—',
        histories: [],
        vitals: '—',
        surgery: '—',
        allergy: '—',
        imaging: '—'
      }
    };
  },

  onShow() {
    this.loadFromStorage();
    this.fetchRemote();
  },

  methods: {
    normalizeMeasurementRecord(record) {
      if (!record || typeof record !== 'object') return null;

      let src = record;
      if (Array.isArray(record.rows)) {
        src = record.rows[0] || null;
      } else if (record.latestMeasurement && typeof record.latestMeasurement === 'object') {
        src = record.latestMeasurement;
      }
      if (!src || typeof src !== 'object') return null;

      const normalized = { ...src };
      const hr = normalized.hr ?? normalized.heartRate;
      const temp = normalized.temp ?? normalized.temperatureC;
      const weight = normalized.weight ?? normalized.weightKg;
      const glucose = normalized.glucose ?? normalized.bloodGlucose;
      const sleep = normalized.sleep ?? normalized.sleepHours;

      if (hr !== undefined && hr !== null) normalized.hr = hr;
      if (temp !== undefined && temp !== null) normalized.temp = temp;
      if (weight !== undefined && weight !== null) normalized.weight = weight;
      if (glucose !== undefined && glucose !== null) normalized.glucose = glucose;
      if (sleep !== undefined && sleep !== null) normalized.sleep = sleep;
      return normalized;
    },

    hasAnyMeasurement(record) {
      const src = this.normalizeMeasurementRecord(record);
      if (!src) return false;
      const keys = ['sbp', 'dbp', 'hr', 'temp', 'spo2', 'glucose', 'sleep'];
      return keys.some((k) => src[k] !== undefined && src[k] !== null && src[k] !== '');
    },

    buildPatchFromMeasurement(record) {
      const src = record || {};
      const patch = {};

      const sbp = toNum(src.sbp);
      const dbp = toNum(src.dbp);
      if (sbp !== null || dbp !== null) {
        patch.bp = `${sbp !== null ? Math.round(sbp) : '--'}/${dbp !== null ? Math.round(dbp) : '--'}`;
      }

      const hr = toNum(src.hr);
      if (hr !== null) patch.hr = String(Math.round(hr));

      const temp = toNum(src.temp);
      if (temp !== null) patch.weight = Number(temp).toFixed(1);

      const spo2 = toNum(src.spo2);
      if (spo2 !== null) patch.spo2 = String(Math.round(spo2));

      const glucose = toNum(src.glucose);
      if (glucose !== null) patch.glucose = Number(glucose).toFixed(1);

      const sleep = toNum(src.sleep);
      if (sleep !== null) patch.sleep = Number(sleep).toFixed(1);

      return patch;
    },

    applyMeasurementToLocal(record, sourceText) {
      const normalized = this.normalizeMeasurementRecord(record);
      if (!this.hasAnyMeasurement(normalized)) return false;
      const patch = this.buildPatchFromMeasurement(normalized);
      const hasPatch = Object.keys(patch).some((k) => hasMetricValue(patch[k]));
      if (!hasPatch) return false;

      const source = sourceText || '后端同步';
      updateDailyMetricsLatest(patch, source, { silent: true });
      upsertTimeseriesFromPatch(patch, source, dateOnly(normalized.recordDate || normalized.measuredAt) || undefined);
      setScopedStorageSync('daily_measure_today', normalized || {});
      setScopedStorageSync('daily_measure_date', dateOnly(normalized.recordDate || normalized.measuredAt) || '');
      setScopedStorageSync('profile_sync_at', nowHM());
      return true;
    },

    mergeHomeMetricsToLocal(metrics, sourceText) {
      if (!Array.isArray(metrics) || !metrics.length) return false;
      const base = buildDefaultLatestMetrics();
      const baseMap = {};
      base.forEach((item) => {
        baseMap[item.key] = { ...item };
      });

      metrics.forEach((item) => {
        if (!item || !item.key || !baseMap[item.key]) return;
        baseMap[item.key] = {
          ...baseMap[item.key],
          ...item,
          hint: item.hint || `来源：${sourceText || '后端汇总'}`
        };
      });

      const merged = base.map((item) => baseMap[item.key] || item);
      setScopedStorageSync('daily_metrics_latest', {
        metrics: merged,
        updatedAt: new Date().toISOString()
      });
      return true;
    },

    formatDateText(v) {
      if (!v) return '';
      const d = dateOnly(v);
      if (d) return d;
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

    deriveFocusFromMetrics(metrics) {
      const list = (Array.isArray(metrics) ? metrics : [])
        .filter((item) => {
          const trend = String(item && item.trend || '').toLowerCase();
          const text = String(item && item.trendText || '');
          const hint = String(item && item.hint || '');
          return trend === 'up' || text.includes('偏高') || text.includes('关注') || text.includes('异常') || hint.includes('异常');
        })
        .map((item) => item && item.label)
        .filter(Boolean);
      return list.slice(0, 2).join('、');
    },

    normalizeFollowupSummary(homeSummary, associationRes, followupRes) {
      const result = {};
      const home = homeSummary && typeof homeSummary === 'object' ? homeSummary : {};
      if (home.followup && typeof home.followup === 'object') {
        Object.assign(result, home.followup);
      }

      const association = associationRes && associationRes.data !== undefined ? associationRes.data : associationRes;
      const associationData = association && typeof association === 'object' ? association : {};
      const associationSummary = associationData.summary && typeof associationData.summary === 'object' ? associationData.summary : {};
      const pendingTasks = Array.isArray(associationData.pendingTasks)
        ? associationData.pendingTasks
        : (Array.isArray(associationData.tasks) ? associationData.tasks : []);
      const recentRecords = Array.isArray(associationData.recentRecords)
        ? associationData.recentRecords
        : (Array.isArray(associationData.records) ? associationData.records : []);

      const followupList = Array.isArray(followupRes)
        ? followupRes
        : (followupRes && Array.isArray(followupRes.list)
          ? followupRes.list
          : (followupRes && Array.isArray(followupRes.records) ? followupRes.records : []));

      const latestRecord = this.pickLatestFollowupRecord([
        ...(Array.isArray(recentRecords) ? recentRecords : []),
        ...followupList
      ]);
      const earliestPendingTask = this.pickEarliestPendingTask(pendingTasks);

      let next = associationSummary.next
        || associationData.nextDueAt
        || associationData.nextDue
        || associationData.nextFollowupTime
        || (associationData.nextFollowup && (associationData.nextFollowup.planTime || associationData.nextFollowup.dueAt))
        || (earliestPendingTask && (earliestPendingTask.planTime || earliestPendingTask.dueAt || earliestPendingTask.plan_time))
        || (latestRecord && (latestRecord.nextFollowupDate || latestRecord.nextPlanTime || latestRecord.nextFollowupTime))
        || '';
      const last = associationSummary.last
        || (associationData.lastFollowup && (associationData.lastFollowup.followupTime || associationData.lastFollowup.createdAt || associationData.lastFollowup.time))
        || (latestRecord && (latestRecord.followupTime || latestRecord.createdAt || latestRecord.time))
        || '';

      const nextTs = this.parseDateMs(next);
      const lastTs = this.parseDateMs(last);
      if (Number.isFinite(nextTs) && Number.isFinite(lastTs) && nextTs < lastTs) {
        const latestRecordNext = latestRecord && (latestRecord.nextFollowupDate || latestRecord.nextPlanTime || latestRecord.nextFollowupTime || '');
        const latestRecordNextTs = this.parseDateMs(latestRecordNext);
        next = (Number.isFinite(latestRecordNextTs) && latestRecordNextTs >= lastTs) ? latestRecordNext : '';
      }
      const focus = associationSummary.focus
        || associationData.focus
        || associationData.focusMetricText
        || (Array.isArray(associationData.focusMetrics)
          ? associationData.focusMetrics
            .map((item) => item && (item.label || item.name || item.metricName || item.metricKey))
            .filter(Boolean)
            .join('、')
          : '')
        || this.deriveFocusFromMetrics(this.metrics);
      const advice = associationSummary.advice
        || associationData.advice
        || (Array.isArray(associationData.tips) && associationData.tips.length ? associationData.tips[0] : '')
        || (followupList[0] && (followupList[0].advice || followupList[0].contentSummary || followupList[0].summary))
        || '';

      const explicitDone = Number(associationSummary.done ?? associationData.done ?? associationData.completedCount);
      const explicitTotal = Number(associationSummary.total ?? associationData.total ?? associationData.taskCount ?? associationData.allCount);
      const doneFromRecords = followupList.filter((item) => {
        const s = String(item && (item.resultStatus || item.status || item.taskStatus) || '').toUpperCase();
        return s === 'COMPLETED' || s === 'DONE' || s === 'FINISHED';
      }).length;

      if (Number.isFinite(explicitDone)) result.done = explicitDone;
      else if (doneFromRecords > 0) result.done = doneFromRecords;

      if (Number.isFinite(explicitTotal)) result.total = explicitTotal;
      else if (pendingTasks.length || followupList.length) result.total = pendingTasks.length + Math.max(doneFromRecords, followupList.length);

      result.next = next ? this.formatDateText(next) : '';
      if (last) result.last = this.formatDateText(last);
      if (focus) result.focus = String(focus);
      if (advice) result.advice = String(advice);

      return Object.keys(result).length ? result : null;
    },

    fetchRemote() {
      Promise.allSettled([
        apiGetProfile(),
        getHomeSummary(),
        getDailyMeasurementToday(),
        getFollowupAssociation(),
        listFollowups({ pageNum: 1, pageSize: 5 })
      ])
        .then((results) => {
          const p0 = results[0] && results[0].status === 'fulfilled' ? results[0].value : null;
          const p1 = results[1] && results[1].status === 'fulfilled' ? results[1].value : null;
          const p2 = results[2] && results[2].status === 'fulfilled' ? results[2].value : null;
          const p3 = results[3] && results[3].status === 'fulfilled' ? results[3].value : null;
          const p4 = results[4] && results[4].status === 'fulfilled' ? results[4].value : null;

          if (p0 && typeof p0 === 'object') {
            this.displayName = p0.name || p0.displayName || this.displayName;
            this.avatarUrl = p0.avatarUrl || this.avatarUrl;

            const oldProfile = getScopedStorageSync('patient_profile', {}) || {};
            setScopedStorageSync('patient_profile', {
              ...(oldProfile && typeof oldProfile === 'object' ? oldProfile : {}),
              displayName: this.displayName,
              avatarUrl: this.avatarUrl,
              risk: p1 && p1.risk ? { ...this.risk, ...p1.risk } : this.risk
            });
          }

          if (p1 && typeof p1 === 'object') {
            if (p1.risk) {
              this.risk = { ...this.risk, ...p1.risk };
              const oldProfile = getScopedStorageSync('patient_profile', {}) || {};
              setScopedStorageSync('patient_profile', {
                ...(oldProfile && typeof oldProfile === 'object' ? oldProfile : {}),
                displayName: this.displayName,
                avatarUrl: this.avatarUrl,
                risk: this.risk
              });
            }
          }

          const followupSummary = this.normalizeFollowupSummary(p1, p3, p4);
          if (followupSummary) {
            this.followup = { ...this.followup, ...followupSummary };
            setScopedStorageSync('followup_summary', this.followup);
            setScopedStorageSync('rehab_summary', {
              done: this.followup.done,
              total: this.followup.total,
              next: this.followup.next
            });
          }

          const todayRecord = this.normalizeMeasurementRecord(p2);
          const homeLatestRecord = this.normalizeMeasurementRecord(p1 && p1.latestMeasurement ? p1.latestMeasurement : null);
          const backfilled = this.applyMeasurementToLocal(todayRecord, '后端最新体征')
            || this.applyMeasurementToLocal(homeLatestRecord, '首页最新体征');
          if (!backfilled && p1 && Array.isArray(p1.metrics) && p1.metrics.length) {
            this.mergeHomeMetricsToLocal(p1.metrics, '后端汇总');
          }

          this.loadFromStorage();
        })
        .catch(() => {});
    },

    loadFromStorage() {
      try {
        const profile = getScopedStorageSync('patient_profile', null);
        if (profile && typeof profile === 'object') {
          this.displayName = profile.displayName || profile.name || this.displayName;
          this.avatarUrl = profile.avatarUrl || this.avatarUrl;
          if (profile.risk) this.risk = { ...this.risk, ...profile.risk };
          if (profile.medical) this.medical = { ...this.medical, ...profile.medical };
        }

        const userInfo = getScopedStorageSync('userInfo', null);
        if (userInfo && typeof userInfo === 'object') {
          this.displayName = userInfo.name || userInfo.displayName || this.displayName;
          this.avatarUrl = userInfo.avatarUrl || this.avatarUrl;
        }

        const latest = getDailyMetricsLatest();
        if (latest && typeof latest === 'object' && Array.isArray(latest.metrics) && latest.metrics.length) {
          this.metrics = latest.metrics;
        }

        const fu = getScopedStorageSync('followup_summary', null);
        if (fu && typeof fu === 'object') {
          this.followup = { ...this.followup, ...fu };
        }

        const syncAt = getScopedStorageSync('profile_sync_at', '');
        if (syncAt) this.syncText = '已同步 · ' + syncAt;
      } catch (e) {}
    },

    onRefresh() {
      this.fetchRemote();
      uni.showToast({ title: '已刷新', icon: 'success' });
    },

    toggleMedical() {
      this.showMedicalMore = !this.showMedicalMore;
    },

    goMetric(key) {
      if (key === 'bp') return uni.navigateTo({ url: '/pages/patient/bp/bp' });
      if (key === 'hr') return uni.navigateTo({ url: '/pages/patient/hr/hr' });
      if (key === 'weight') return uni.navigateTo({ url: '/pages/patient/weight/weight' });
      if (key === 'spo2') return uni.navigateTo({ url: '/pages/patient/spo2/spo2' });
      return this.goDashboard();
    },

    goDashboard() {
      uni.navigateTo({ url: '/pages/patient/dashboard/dashboard' });
    },

    goRehab() {
      uni.navigateTo({ url: '/pages/patient/rehab/rehab' });
    },

    goFollowup() {
      uni.navigateTo({ url: '/pages/patient/followup/followup' });
    },

    goChat() {
      uni.switchTab({ url: '/pages/patient/chat/chat' });
    }
  }
};
</script>

<style src="./healthFile.css"></style>
