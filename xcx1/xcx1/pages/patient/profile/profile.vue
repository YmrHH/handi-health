<template>
  <view class="app-page my">
    <!-- 顶部：用户信息（点击进入编辑资料） -->
    <view class="my-top flex-row items-center justify-between">
      <view class="my-user flex-row items-center" @tap="goEditProfile">
        <image class="my-avatar" :src="avatarUrl" mode="aspectFill"></image>

        <view class="my-name-pill">
          <text class="my-name">{{ displayName }}</text>
        </view>

        <view class="my-sync-pill ml-12">
          <text class="my-sync-dot"></text>
          <text class="my-sync-text">{{ syncText }}</text>
        </view>
      </view>

      <view class="my-actions flex-row items-center">
        <view class="my-icon-btn" @tap="onRefresh" aria-label="刷新">
          <text class="my-icon">⟳</text>
        </view>
      </view>
    </view>

    <!-- 顶部：医生健康建议（替换“我的健康档案”） -->
    <view class="my-overview app-card app-card--glass">
      <view class="my-overview-row flex-row items-start justify-between">
        <view class="flex-col">
          <text class="my-overview-title">医生健康建议</text>
          <text class="my-overview-sub">{{ advice.title }} · {{ advice.time }}</text>
        </view>

        <view class="my-risk">
          <text :class="'my-risk-chip risk-' + risk.level">{{ risk.text }}</text>
          <text class="my-risk-score">{{ risk.score }}</text>
          <text class="my-risk-label">健康分</text>
        </view>
      </view>

      <view class="my-advice-preview" @tap="goAdviceDetail">
        <text class="my-advice-preview-content">{{ buildAdvicePreview(advice.content) }}</text>
        <view class="my-advice-preview-meta flex-row items-center justify-between">
          <text class="my-advice-preview-doctor">{{ advice.doctor }}</text>
          <text class="my-advice-preview-link">查看详情 ›</text>
        </view>
      </view>

      <!-- 快捷入口 -->
      <view class="my-quick flex-row justify-between">
        <view class="my-quick-item" @tap="goDevices">
          <text class="my-quick-title">设备绑定</text>
          <text class="my-quick-desc">连接手环/血压计</text>
        </view>
        <view class="my-quick-item" @tap="goRehab">
          <text class="my-quick-title">康养任务</text>
          <text class="my-quick-desc">计划与完成情况</text>
        </view>
        <view class="my-quick-item" @tap="goMessage">
          <text class="my-quick-title">消息提醒</text>
          <text class="my-quick-desc">随访/用药提醒</text>
        </view>
      </view>
    </view>

    <!-- 身体数据 -->
    <view class="my-section flex-row items-center justify-between">
      <text class="app-section-title">身体数据</text>
    </view>

    <!-- 趋势与建议（近7天） -->
    <view class="my-trend app-card" @tap="goTrendDetail">
      <view class="my-trend-head flex-row items-center justify-between">
        <text class="my-trend-title">趋势与建议（近7天）</text>
        <view class="my-trend-btn btn-secondary" @tap.stop="goTrendDetail">进入详情</view>
      </view>

      <view class="my-trend-chart">
        <view
          v-for="(p, idx) in trendPoints"
          :key="idx"
          class="my-trend-bar"
          :style="{ height: p + '%' }"
        ></view>
      </view>

      <text class="my-trend-tip">{{ trendTip }}</text>
    </view>

    <!-- 指标卡片 -->
    <view class="my-metrics">
      <view
        v-for="m in metrics"
        :key="m.key"
        class="my-metric app-card"
      >
        <view class="my-metric-head flex-row items-center justify-between">
          <text class="my-metric-title">{{ m.label }}</text>
          <text :class="'my-metric-trend ' + m.trend">{{ m.trendText }}</text>
        </view>

        <view class="my-metric-val flex-row items-baseline">
          <text class="my-metric-num">{{ m.value }}</text>
          <text class="my-metric-unit ml-8">{{ m.unit }}</text>
        </view>

        <text class="my-metric-hint">{{ m.hint }}</text>
        <view class="my-mini" :class="'mini-' + m.key"></view>
      </view>
    </view>

    <!-- 随访关联（替换原下方的康养任务情况模块） -->
    <view class="my-section flex-row items-center justify-between">
      <text class="app-section-title">随访关联</text>
      <view class="my-link flex-row items-center" @tap="goFollowupDetail">
        <text class="my-link-text">查看详情</text>
        <text class="my-arrow">›</text>
      </view>
    </view>

    <view class="my-follow app-card app-card--glass" @tap="goFollowupDetail">
      <view class="my-follow-row flex-row items-center justify-between">
        <view class="flex-col">
          <text class="my-follow-title">本周随访重点</text>
          <text class="my-follow-sub">基于设备数据与康养执行情况生成随访优先级</text>
        </view>

        <view class="my-follow-badge">
          <text class="my-follow-badge-num">{{ followup.done }}</text>
          <text class="my-follow-badge-sep">/</text>
          <text class="my-follow-badge-total">{{ followup.total }}</text>
        </view>
      </view>

      <view class="my-follow-grid">
        <view class="my-follow-item">
          <text class="my-follow-k">下次随访</text>
          <text class="my-follow-v">{{ followup.next }}</text>
        </view>
        <view class="my-follow-item">
          <text class="my-follow-k">上次随访</text>
          <text class="my-follow-v">{{ followup.last }}</text>
        </view>
        <view class="my-follow-item">
          <text class="my-follow-k">重点指标</text>
          <text class="my-follow-v">{{ followup.focus }}</text>
        </view>
        <view class="my-follow-item">
          <text class="my-follow-k">建议</text>
          <text class="my-follow-v">{{ followup.advice }}</text>
        </view>
      </view>
    </view>

    
    <view class="my-footnote flex-row items-center">
      <text class="my-footnote-text">个人资料用于出院后监测、预警与随访服务；平台将严格保护您的隐私安全。</text>
    </view>

    <view class="my-logout app-card">
      <view class="my-logout-btn" @tap="onLogout">退出登录</view>
    </view>
  </view>
</template>

<script>
import { getProfile as apiGetProfile, getHomeSummary, getDailyMeasurementToday, getFollowupAssociation, listFollowups } from '@/api/patient.js';
import { logout as apiLogout } from '@/api/auth.js';
import { clearSession, getScopedStorageSync, setScopedStorageSync } from '@/utils/session.js';
import {
  getOverviewTrend,
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
      avatarUrl: '/static/assets/avatar.png',
      displayName: '用*A',
      syncText: '医生端已同步 · 刚刚',

      risk: { level: 'mid', text: '中风险', score: 78 },

      // 身体指标（常看）
      metrics: buildDefaultLatestMetrics(),

      // 趋势（近7天，0~100% 映射，用于简单可视化）
      trendPoints: [35, 42, 40, 55, 60, 58, 66],
      trendTip: '血压近7天呈上升趋势，建议今日低盐饮食、按时服药，并关注晨起血压。',

      // 随访关联（原“数据页”作为子页面承载更详细趋势）
      followup: {
        done: 2,
        total: 3,
        next: '',
        last: '2026-01-02',
        focus: '血压、血糖',
        advice: '若连续2天偏高，请在消息中确认随访'
      },

      // 医生建议（由医生 Web 端审核后下发）
      advice: {
        title: '本周重点建议',
        doctor: '责任医生',
        time: '刚刚',
        content: '建议持续监测血压与血糖；若出现胸闷、头晕或血压持续升高，请及时在小程序确认随访并联系医生。'
      }
    };
  },

  onShow() {
    this.loadFromStorage();
    this.refreshTrendOverview();
    this.fetchRemote();

    this._onMetricsUpdated = () => {
      this.loadFromStorage();
      this.refreshTrendOverview();
    };
    try {
      uni.$on && uni.$on('metrics_latest_updated', this._onMetricsUpdated);
    } catch (e) {}
  },

  onHide() {
    try {
      if (this._onMetricsUpdated) uni.$off && uni.$off('metrics_latest_updated', this._onMetricsUpdated);
      this._onMetricsUpdated = null;
    } catch (e) {}
  },

  onUnload() {
    try {
      if (this._onMetricsUpdated) uni.$off && uni.$off('metrics_latest_updated', this._onMetricsUpdated);
      this._onMetricsUpdated = null;
    } catch (e) {}
  },

  methods: {
    buildAdvicePreview(content) {
      const raw = String(content || '').replace(/\r/g, '\n');
      const lines = raw
        .split(/\n+/)
        .map((line) => String(line || '').replace(/^[\s\-•·*]+/, '').trim())
        .filter(Boolean);
      const summary = (lines.length ? lines.join('  ') : raw.replace(/\s+/g, ' ').trim())
        .replace(/\s+/g, ' ')
        .trim();
      if (!summary) return '暂无最新医生建议，请继续保持规律监测与康养任务。';
      if (summary.length <= 48) return summary;
      return summary.slice(0, 48).replace(/[，、；：,.。\s]+$/g, '') + '…';
    },

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
        || home.advice?.content
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

    buildFollowupRecordCache(associationRes, followupRes) {
      const association = associationRes && associationRes.data !== undefined ? associationRes.data : associationRes;
      const associationData = association && typeof association === 'object' ? association : {};
      const fromAssoc = Array.isArray(associationData.recentRecords)
        ? associationData.recentRecords
        : (Array.isArray(associationData.records) ? associationData.records : []);
      const fromList = Array.isArray(followupRes)
        ? followupRes
        : (followupRes && Array.isArray(followupRes.list)
          ? followupRes.list
          : (followupRes && Array.isArray(followupRes.records) ? followupRes.records : []));
      const source = fromList.length ? fromList : fromAssoc;
      return source.slice(0, 5).map((r, index) => ({
        id: r.id || r.recordId || r.taskId || r.relatedTaskId || index,
        title: String(r.title || r.conclusion || r.resultStatus || r.result || '随访记录'),
        time: String(r.followupTime || r.measuredAt || r.createdAt || r.time || '').replace('T', ' ').slice(0, 16),
        note: r.contentSummary || r.summary || r.symptomChange || r.note || ''
      }));
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
            try {
              const prev = getScopedStorageSync('userInfo', {});
              setScopedStorageSync('userInfo', {
                ...(prev && typeof prev === 'object' ? prev : {}),
                ...p0
              });
            } catch (e) {}
          }

          if (p1 && typeof p1 === 'object') {
            if (p1.risk) this.risk = { ...this.risk, ...p1.risk };
            if (p1.advice) {
              this.advice = { ...this.advice, ...p1.advice };
              setScopedStorageSync('doctor_advice_latest', this.advice);
            }
            if (Array.isArray(p1.trendPoints)) this.trendPoints = p1.trendPoints;
            if (p1.trendTip) this.trendTip = p1.trendTip;
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

          const followupRecords = this.buildFollowupRecordCache(p3, p4);
          if (followupRecords.length) {
            setScopedStorageSync('followup_records', followupRecords);
          }

          const associationData = p3 && p3.data !== undefined ? p3.data : p3;
          if (associationData && Array.isArray(associationData.tips) && associationData.tips.length) {
            setScopedStorageSync('followup_tips', associationData.tips);
          }

          const todayRecord = this.normalizeMeasurementRecord(p2);
          const homeLatestRecord = this.normalizeMeasurementRecord(p1 && p1.latestMeasurement ? p1.latestMeasurement : null);
          const backfilled = this.applyMeasurementToLocal(todayRecord, '后端最新体征')
            || this.applyMeasurementToLocal(homeLatestRecord, '首页最新体征');
          if (!backfilled && p1 && Array.isArray(p1.metrics) && p1.metrics.length) {
            this.mergeHomeMetricsToLocal(p1.metrics, '后端汇总');
          }

          this.loadFromStorage();
          this.refreshTrendOverview();
        })
        .catch(() => {
          // ignore
        });
    },

    refreshTrendOverview() {
      try {
        const ov = getOverviewTrend('bp');
        const pts = ov && Array.isArray(ov.points) ? ov.points : [];
        const nonZeroCount = pts.filter((n) => Number(n) > 0).length;
        if (nonZeroCount >= 2) {
          this.trendPoints = pts;
          if (ov && ov.tip) this.trendTip = ov.tip;
        }
      } catch (e) {}
    },

    loadFromStorage() {
      try {
        const userInfo = getScopedStorageSync('userInfo', null);
        if (userInfo && typeof userInfo === 'object') {
          this.displayName = userInfo.name || userInfo.displayName || this.displayName;
          this.avatarUrl = userInfo.avatarUrl || this.avatarUrl;
        }

        const syncAt = getScopedStorageSync('profile_sync_at', '');
        if (syncAt) {
          this.syncText = '医生端已同步 · ' + syncAt;
        }

        const advice = getScopedStorageSync('doctor_advice_latest', null);
        if (advice && typeof advice === 'object') {
          this.advice = { ...this.advice, ...advice };
        }

        const latest = getDailyMetricsLatest();
        if (latest && typeof latest === 'object' && Array.isArray(latest.metrics) && latest.metrics.length) {
          this.metrics = latest.metrics;
        }

        const fu = getScopedStorageSync('followup_summary', null) || getScopedStorageSync('rehab_summary', null);
        if (fu && typeof fu === 'object') {
          this.followup = { ...this.followup, ...fu };
        }
      } catch (e) {}
    },

    onRefresh() {
      this.fetchRemote();
      uni.showToast({ title: '已刷新', icon: 'success' });
    },

    goEditProfile() {
      uni.navigateTo({ url: '/pages/patient/profile/profileedit/profileedit' });
    },

    goDevices() {
      uni.navigateTo({ url: '/pages/patient/devices/devices' });
    },

    goRehab() {
      uni.navigateTo({ url: '/pages/patient/rehab/rehab' });
    },

    goMessage() {
      uni.navigateTo({ url: '/pages/patient/message/message' });
    },

    goTrendDetail() {
      uni.navigateTo({ url: '/pages/patient/trends/trends' });
    },

    goFollowupDetail() {
      uni.navigateTo({ url: '/pages/patient/followupDetail/followupDetail' });
    },

    goAdviceDetail() {
      this.goMessage();
    },

    goPrivacy() {
      uni.navigateTo({ url: '/pages/patient/privacy/privacy' });
    },

    goFeedback() {
      uni.navigateTo({ url: '/pages/patient/feedback/feedback' });
    },

    goHelp() {
      uni.navigateTo({ url: '/pages/patient/help/help' });
    },

    onLogout() {
      uni.showModal({
        title: '退出登录',
        content: '将清除本机缓存并返回角色选择页面。是否继续？',
        success: (res) => {
          if (res.confirm) {
            apiLogout().catch(() => {});
            clearSession();
            uni.reLaunch({ url: '/pages/index/index' });
          }
        }
      });
    }
  }
};
</script>

<style src="./profile.css"></style>
