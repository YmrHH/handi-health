"use strict";
const common_vendor = require("../../../common/vendor.js");
const api_patient = require("../../../api/patient.js");
const api_auth = require("../../../api/auth.js");
const utils_session = require("../../../utils/session.js");
const utils_metricsStore = require("../../../utils/metrics-store.js");
function pad2(n) {
  return String(n).padStart(2, "0");
}
function nowHM() {
  const now = /* @__PURE__ */ new Date();
  return `${pad2(now.getHours())}:${pad2(now.getMinutes())}`;
}
function dateOnly(v) {
  if (!v)
    return "";
  const s = String(v).trim();
  if (!s)
    return "";
  if (/^\d{4}-\d{2}-\d{2}$/.test(s))
    return s;
  if (/^\d{4}-\d{2}-\d{2}[ T]/.test(s))
    return s.slice(0, 10);
  return "";
}
function toNum(v) {
  if (v === null || v === void 0 || v === "")
    return null;
  const n = Number(v);
  return Number.isFinite(n) ? n : null;
}
function hasMetricValue(v) {
  if (v === null || v === void 0)
    return false;
  const s = String(v).trim();
  return !!s && s !== "--" && s !== "--/--";
}
const _sfc_main = {
  data() {
    return {
      avatarUrl: "/static/assets/avatar.png",
      displayName: "用*A",
      syncText: "医生端已同步 · 刚刚",
      risk: { level: "mid", text: "中风险", score: 78 },
      // 身体指标（常看）
      metrics: utils_metricsStore.buildDefaultLatestMetrics(),
      // 趋势（近7天，0~100% 映射，用于简单可视化）
      trendPoints: [35, 42, 40, 55, 60, 58, 66],
      trendTip: "血压近7天呈上升趋势，建议今日低盐饮食、按时服药，并关注晨起血压。",
      // 随访关联（原“数据页”作为子页面承载更详细趋势）
      followup: {
        done: 2,
        total: 3,
        next: "",
        last: "2026-01-02",
        focus: "血压、血糖",
        advice: "若连续2天偏高，请在消息中确认随访"
      },
      // 医生建议（由医生 Web 端审核后下发）
      advice: {
        title: "本周重点建议",
        doctor: "责任医生",
        time: "刚刚",
        content: "建议持续监测血压与血糖；若出现胸闷、头晕或血压持续升高，请及时在小程序确认随访并联系医生。"
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
      common_vendor.index.$on && common_vendor.index.$on("metrics_latest_updated", this._onMetricsUpdated);
    } catch (e) {
    }
  },
  onHide() {
    try {
      if (this._onMetricsUpdated)
        common_vendor.index.$off && common_vendor.index.$off("metrics_latest_updated", this._onMetricsUpdated);
      this._onMetricsUpdated = null;
    } catch (e) {
    }
  },
  onUnload() {
    try {
      if (this._onMetricsUpdated)
        common_vendor.index.$off && common_vendor.index.$off("metrics_latest_updated", this._onMetricsUpdated);
      this._onMetricsUpdated = null;
    } catch (e) {
    }
  },
  methods: {
    buildAdvicePreview(content) {
      const raw = String(content || "").replace(/\r/g, "\n");
      const lines = raw.split(/\n+/).map((line) => String(line || "").replace(/^[\s\-•·*]+/, "").trim()).filter(Boolean);
      const summary = (lines.length ? lines.join("  ") : raw.replace(/\s+/g, " ").trim()).replace(/\s+/g, " ").trim();
      if (!summary)
        return "暂无最新医生建议，请继续保持规律监测与康养任务。";
      if (summary.length <= 48)
        return summary;
      return summary.slice(0, 48).replace(/[，、；：,.。\s]+$/g, "") + "…";
    },
    normalizeMeasurementRecord(record) {
      if (!record || typeof record !== "object")
        return null;
      let src = record;
      if (Array.isArray(record.rows)) {
        src = record.rows[0] || null;
      } else if (record.latestMeasurement && typeof record.latestMeasurement === "object") {
        src = record.latestMeasurement;
      }
      if (!src || typeof src !== "object")
        return null;
      const normalized = { ...src };
      const hr = normalized.hr ?? normalized.heartRate;
      const temp = normalized.temp ?? normalized.temperatureC;
      const weight = normalized.weight ?? normalized.weightKg;
      const glucose = normalized.glucose ?? normalized.bloodGlucose;
      const sleep = normalized.sleep ?? normalized.sleepHours;
      if (hr !== void 0 && hr !== null)
        normalized.hr = hr;
      if (temp !== void 0 && temp !== null)
        normalized.temp = temp;
      if (weight !== void 0 && weight !== null)
        normalized.weight = weight;
      if (glucose !== void 0 && glucose !== null)
        normalized.glucose = glucose;
      if (sleep !== void 0 && sleep !== null)
        normalized.sleep = sleep;
      return normalized;
    },
    hasAnyMeasurement(record) {
      const src = this.normalizeMeasurementRecord(record);
      if (!src)
        return false;
      const keys = ["sbp", "dbp", "hr", "temp", "spo2", "glucose", "sleep"];
      return keys.some((k) => src[k] !== void 0 && src[k] !== null && src[k] !== "");
    },
    buildPatchFromMeasurement(record) {
      const src = record || {};
      const patch = {};
      const sbp = toNum(src.sbp);
      const dbp = toNum(src.dbp);
      if (sbp !== null || dbp !== null) {
        patch.bp = `${sbp !== null ? Math.round(sbp) : "--"}/${dbp !== null ? Math.round(dbp) : "--"}`;
      }
      const hr = toNum(src.hr);
      if (hr !== null)
        patch.hr = String(Math.round(hr));
      const temp = toNum(src.temp);
      if (temp !== null)
        patch.weight = Number(temp).toFixed(1);
      const spo2 = toNum(src.spo2);
      if (spo2 !== null)
        patch.spo2 = String(Math.round(spo2));
      const glucose = toNum(src.glucose);
      if (glucose !== null)
        patch.glucose = Number(glucose).toFixed(1);
      const sleep = toNum(src.sleep);
      if (sleep !== null)
        patch.sleep = Number(sleep).toFixed(1);
      return patch;
    },
    applyMeasurementToLocal(record, sourceText) {
      const normalized = this.normalizeMeasurementRecord(record);
      if (!this.hasAnyMeasurement(normalized))
        return false;
      const patch = this.buildPatchFromMeasurement(normalized);
      const hasPatch = Object.keys(patch).some((k) => hasMetricValue(patch[k]));
      if (!hasPatch)
        return false;
      const source = sourceText || "后端同步";
      utils_metricsStore.updateDailyMetricsLatest(patch, source, { silent: true });
      utils_metricsStore.upsertTimeseriesFromPatch(patch, source, dateOnly(normalized.recordDate || normalized.measuredAt) || void 0);
      utils_session.setScopedStorageSync("daily_measure_today", normalized || {});
      utils_session.setScopedStorageSync("daily_measure_date", dateOnly(normalized.recordDate || normalized.measuredAt) || "");
      utils_session.setScopedStorageSync("profile_sync_at", nowHM());
      return true;
    },
    mergeHomeMetricsToLocal(metrics, sourceText) {
      if (!Array.isArray(metrics) || !metrics.length)
        return false;
      const base = utils_metricsStore.buildDefaultLatestMetrics();
      const baseMap = {};
      base.forEach((item) => {
        baseMap[item.key] = { ...item };
      });
      metrics.forEach((item) => {
        if (!item || !item.key || !baseMap[item.key])
          return;
        baseMap[item.key] = {
          ...baseMap[item.key],
          ...item,
          hint: item.hint || `来源：${sourceText || "后端汇总"}`
        };
      });
      const merged = base.map((item) => baseMap[item.key] || item);
      utils_session.setScopedStorageSync("daily_metrics_latest", {
        metrics: merged,
        updatedAt: (/* @__PURE__ */ new Date()).toISOString()
      });
      return true;
    },
    formatDateText(v) {
      if (!v)
        return "";
      const d = dateOnly(v);
      if (d)
        return d;
      return String(v).replace("T", " ").slice(0, 10);
    },
    parseDateMs(v) {
      if (!v)
        return NaN;
      const s = String(v).trim();
      if (!s)
        return NaN;
      const normalized = s.replace(/\./g, "-").replace("T", " ").replace(/\//g, "-");
      const ts = Date.parse(normalized.replace(" ", "T"));
      return Number.isFinite(ts) ? ts : NaN;
    },
    pickLatestFollowupRecord(list) {
      const rows = Array.isArray(list) ? list : [];
      let best = null;
      let bestTs = NaN;
      rows.forEach((item) => {
        const ts = this.parseDateMs(item && (item.followupTime || item.createdAt || item.measuredAt || item.time));
        if (!Number.isFinite(ts))
          return;
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
        if (!Number.isFinite(ts))
          return;
        if (!Number.isFinite(bestTs) || ts < bestTs) {
          best = item;
          bestTs = ts;
        }
      });
      return best;
    },
    deriveFocusFromMetrics(metrics) {
      const list = (Array.isArray(metrics) ? metrics : []).filter((item) => {
        const trend = String(item && item.trend || "").toLowerCase();
        const text = String(item && item.trendText || "");
        const hint = String(item && item.hint || "");
        return trend === "up" || text.includes("偏高") || text.includes("关注") || text.includes("异常") || hint.includes("异常");
      }).map((item) => item && item.label).filter(Boolean);
      return list.slice(0, 2).join("、");
    },
    normalizeFollowupSummary(homeSummary, associationRes, followupRes) {
      var _a;
      const result = {};
      const home = homeSummary && typeof homeSummary === "object" ? homeSummary : {};
      if (home.followup && typeof home.followup === "object") {
        Object.assign(result, home.followup);
      }
      const association = associationRes && associationRes.data !== void 0 ? associationRes.data : associationRes;
      const associationData = association && typeof association === "object" ? association : {};
      const associationSummary = associationData.summary && typeof associationData.summary === "object" ? associationData.summary : {};
      const pendingTasks = Array.isArray(associationData.pendingTasks) ? associationData.pendingTasks : Array.isArray(associationData.tasks) ? associationData.tasks : [];
      const recentRecords = Array.isArray(associationData.recentRecords) ? associationData.recentRecords : Array.isArray(associationData.records) ? associationData.records : [];
      const followupList = Array.isArray(followupRes) ? followupRes : followupRes && Array.isArray(followupRes.list) ? followupRes.list : followupRes && Array.isArray(followupRes.records) ? followupRes.records : [];
      const latestRecord = this.pickLatestFollowupRecord([
        ...Array.isArray(recentRecords) ? recentRecords : [],
        ...followupList
      ]);
      const earliestPendingTask = this.pickEarliestPendingTask(pendingTasks);
      let next = associationSummary.next || associationData.nextDueAt || associationData.nextDue || associationData.nextFollowupTime || associationData.nextFollowup && (associationData.nextFollowup.planTime || associationData.nextFollowup.dueAt) || earliestPendingTask && (earliestPendingTask.planTime || earliestPendingTask.dueAt || earliestPendingTask.plan_time) || latestRecord && (latestRecord.nextFollowupDate || latestRecord.nextPlanTime || latestRecord.nextFollowupTime) || "";
      const last = associationSummary.last || associationData.lastFollowup && (associationData.lastFollowup.followupTime || associationData.lastFollowup.createdAt || associationData.lastFollowup.time) || latestRecord && (latestRecord.followupTime || latestRecord.createdAt || latestRecord.time) || "";
      const nextTs = this.parseDateMs(next);
      const lastTs = this.parseDateMs(last);
      if (Number.isFinite(nextTs) && Number.isFinite(lastTs) && nextTs < lastTs) {
        const latestRecordNext = latestRecord && (latestRecord.nextFollowupDate || latestRecord.nextPlanTime || latestRecord.nextFollowupTime || "");
        const latestRecordNextTs = this.parseDateMs(latestRecordNext);
        next = Number.isFinite(latestRecordNextTs) && latestRecordNextTs >= lastTs ? latestRecordNext : "";
      }
      const focus = associationSummary.focus || associationData.focus || associationData.focusMetricText || (Array.isArray(associationData.focusMetrics) ? associationData.focusMetrics.map((item) => item && (item.label || item.name || item.metricName || item.metricKey)).filter(Boolean).join("、") : "") || this.deriveFocusFromMetrics(this.metrics);
      const advice = associationSummary.advice || associationData.advice || (Array.isArray(associationData.tips) && associationData.tips.length ? associationData.tips[0] : "") || followupList[0] && (followupList[0].advice || followupList[0].contentSummary || followupList[0].summary) || ((_a = home.advice) == null ? void 0 : _a.content) || "";
      const explicitDone = Number(associationSummary.done ?? associationData.done ?? associationData.completedCount);
      const explicitTotal = Number(associationSummary.total ?? associationData.total ?? associationData.taskCount ?? associationData.allCount);
      const doneFromRecords = followupList.filter((item) => {
        const s = String(item && (item.resultStatus || item.status || item.taskStatus) || "").toUpperCase();
        return s === "COMPLETED" || s === "DONE" || s === "FINISHED";
      }).length;
      if (Number.isFinite(explicitDone))
        result.done = explicitDone;
      else if (doneFromRecords > 0)
        result.done = doneFromRecords;
      if (Number.isFinite(explicitTotal))
        result.total = explicitTotal;
      else if (pendingTasks.length || followupList.length)
        result.total = pendingTasks.length + Math.max(doneFromRecords, followupList.length);
      result.next = next ? this.formatDateText(next) : "";
      if (last)
        result.last = this.formatDateText(last);
      if (focus)
        result.focus = String(focus);
      if (advice)
        result.advice = String(advice);
      return Object.keys(result).length ? result : null;
    },
    buildFollowupRecordCache(associationRes, followupRes) {
      const association = associationRes && associationRes.data !== void 0 ? associationRes.data : associationRes;
      const associationData = association && typeof association === "object" ? association : {};
      const fromAssoc = Array.isArray(associationData.recentRecords) ? associationData.recentRecords : Array.isArray(associationData.records) ? associationData.records : [];
      const fromList = Array.isArray(followupRes) ? followupRes : followupRes && Array.isArray(followupRes.list) ? followupRes.list : followupRes && Array.isArray(followupRes.records) ? followupRes.records : [];
      const source = fromList.length ? fromList : fromAssoc;
      return source.slice(0, 5).map((r, index) => ({
        id: r.id || r.recordId || r.taskId || r.relatedTaskId || index,
        title: String(r.title || r.conclusion || r.resultStatus || r.result || "随访记录"),
        time: String(r.followupTime || r.measuredAt || r.createdAt || r.time || "").replace("T", " ").slice(0, 16),
        note: r.contentSummary || r.summary || r.symptomChange || r.note || ""
      }));
    },
    fetchRemote() {
      Promise.allSettled([
        api_patient.getProfile(),
        api_patient.getHomeSummary(),
        api_patient.getDailyMeasurementToday(),
        api_patient.getFollowupAssociation(),
        api_patient.listFollowups({ pageNum: 1, pageSize: 5 })
      ]).then((results) => {
        const p0 = results[0] && results[0].status === "fulfilled" ? results[0].value : null;
        const p1 = results[1] && results[1].status === "fulfilled" ? results[1].value : null;
        const p2 = results[2] && results[2].status === "fulfilled" ? results[2].value : null;
        const p3 = results[3] && results[3].status === "fulfilled" ? results[3].value : null;
        const p4 = results[4] && results[4].status === "fulfilled" ? results[4].value : null;
        if (p0 && typeof p0 === "object") {
          this.displayName = p0.name || p0.displayName || this.displayName;
          this.avatarUrl = p0.avatarUrl || this.avatarUrl;
          try {
            const prev = utils_session.getScopedStorageSync("userInfo", {});
            utils_session.setScopedStorageSync("userInfo", {
              ...prev && typeof prev === "object" ? prev : {},
              ...p0
            });
          } catch (e) {
          }
        }
        if (p1 && typeof p1 === "object") {
          if (p1.risk)
            this.risk = { ...this.risk, ...p1.risk };
          if (p1.advice) {
            this.advice = { ...this.advice, ...p1.advice };
            utils_session.setScopedStorageSync("doctor_advice_latest", this.advice);
          }
          if (Array.isArray(p1.trendPoints))
            this.trendPoints = p1.trendPoints;
          if (p1.trendTip)
            this.trendTip = p1.trendTip;
        }
        const followupSummary = this.normalizeFollowupSummary(p1, p3, p4);
        if (followupSummary) {
          this.followup = { ...this.followup, ...followupSummary };
          utils_session.setScopedStorageSync("followup_summary", this.followup);
          utils_session.setScopedStorageSync("rehab_summary", {
            done: this.followup.done,
            total: this.followup.total,
            next: this.followup.next
          });
        }
        const followupRecords = this.buildFollowupRecordCache(p3, p4);
        if (followupRecords.length) {
          utils_session.setScopedStorageSync("followup_records", followupRecords);
        }
        const associationData = p3 && p3.data !== void 0 ? p3.data : p3;
        if (associationData && Array.isArray(associationData.tips) && associationData.tips.length) {
          utils_session.setScopedStorageSync("followup_tips", associationData.tips);
        }
        const todayRecord = this.normalizeMeasurementRecord(p2);
        const homeLatestRecord = this.normalizeMeasurementRecord(p1 && p1.latestMeasurement ? p1.latestMeasurement : null);
        const backfilled = this.applyMeasurementToLocal(todayRecord, "后端最新体征") || this.applyMeasurementToLocal(homeLatestRecord, "首页最新体征");
        if (!backfilled && p1 && Array.isArray(p1.metrics) && p1.metrics.length) {
          this.mergeHomeMetricsToLocal(p1.metrics, "后端汇总");
        }
        this.loadFromStorage();
        this.refreshTrendOverview();
      }).catch(() => {
      });
    },
    refreshTrendOverview() {
      try {
        const ov = utils_metricsStore.getOverviewTrend("bp");
        const pts = ov && Array.isArray(ov.points) ? ov.points : [];
        const nonZeroCount = pts.filter((n) => Number(n) > 0).length;
        if (nonZeroCount >= 2) {
          this.trendPoints = pts;
          if (ov && ov.tip)
            this.trendTip = ov.tip;
        }
      } catch (e) {
      }
    },
    loadFromStorage() {
      try {
        const userInfo = utils_session.getScopedStorageSync("userInfo", null);
        if (userInfo && typeof userInfo === "object") {
          this.displayName = userInfo.name || userInfo.displayName || this.displayName;
          this.avatarUrl = userInfo.avatarUrl || this.avatarUrl;
        }
        const syncAt = utils_session.getScopedStorageSync("profile_sync_at", "");
        if (syncAt) {
          this.syncText = "医生端已同步 · " + syncAt;
        }
        const advice = utils_session.getScopedStorageSync("doctor_advice_latest", null);
        if (advice && typeof advice === "object") {
          this.advice = { ...this.advice, ...advice };
        }
        const latest = utils_metricsStore.getDailyMetricsLatest();
        if (latest && typeof latest === "object" && Array.isArray(latest.metrics) && latest.metrics.length) {
          this.metrics = latest.metrics;
        }
        const fu = utils_session.getScopedStorageSync("followup_summary", null) || utils_session.getScopedStorageSync("rehab_summary", null);
        if (fu && typeof fu === "object") {
          this.followup = { ...this.followup, ...fu };
        }
      } catch (e) {
      }
    },
    onRefresh() {
      this.fetchRemote();
      common_vendor.index.showToast({ title: "已刷新", icon: "success" });
    },
    goEditProfile() {
      common_vendor.index.navigateTo({ url: "/pages/patient/profile/profileedit/profileedit" });
    },
    goDevices() {
      common_vendor.index.navigateTo({ url: "/pages/patient/devices/devices" });
    },
    goRehab() {
      common_vendor.index.navigateTo({ url: "/pages/patient/rehab/rehab" });
    },
    goMessage() {
      common_vendor.index.navigateTo({ url: "/pages/patient/message/message" });
    },
    goTrendDetail() {
      common_vendor.index.navigateTo({ url: "/pages/patient/trends/trends" });
    },
    goFollowupDetail() {
      common_vendor.index.navigateTo({ url: "/pages/patient/followupDetail/followupDetail" });
    },
    goAdviceDetail() {
      this.goMessage();
    },
    goPrivacy() {
      common_vendor.index.navigateTo({ url: "/pages/patient/privacy/privacy" });
    },
    goFeedback() {
      common_vendor.index.navigateTo({ url: "/pages/patient/feedback/feedback" });
    },
    goHelp() {
      common_vendor.index.navigateTo({ url: "/pages/patient/help/help" });
    },
    onLogout() {
      common_vendor.index.showModal({
        title: "退出登录",
        content: "将清除本机缓存并返回角色选择页面。是否继续？",
        success: (res) => {
          if (res.confirm) {
            api_auth.logout().catch(() => {
            });
            utils_session.clearSession();
            common_vendor.index.reLaunch({ url: "/pages/index/index" });
          }
        }
      });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return {
    a: $data.avatarUrl,
    b: common_vendor.t($data.displayName),
    c: common_vendor.t($data.syncText),
    d: common_vendor.o((...args) => $options.goEditProfile && $options.goEditProfile(...args)),
    e: common_vendor.o((...args) => $options.onRefresh && $options.onRefresh(...args)),
    f: common_vendor.t($data.advice.title),
    g: common_vendor.t($data.advice.time),
    h: common_vendor.t($data.risk.text),
    i: common_vendor.n("my-risk-chip risk-" + $data.risk.level),
    j: common_vendor.t($data.risk.score),
    k: common_vendor.t($options.buildAdvicePreview($data.advice.content)),
    l: common_vendor.t($data.advice.doctor),
    m: common_vendor.o((...args) => $options.goAdviceDetail && $options.goAdviceDetail(...args)),
    n: common_vendor.o((...args) => $options.goDevices && $options.goDevices(...args)),
    o: common_vendor.o((...args) => $options.goRehab && $options.goRehab(...args)),
    p: common_vendor.o((...args) => $options.goMessage && $options.goMessage(...args)),
    q: common_vendor.o((...args) => $options.goTrendDetail && $options.goTrendDetail(...args)),
    r: common_vendor.f($data.trendPoints, (p, idx, i0) => {
      return {
        a: idx,
        b: p + "%"
      };
    }),
    s: common_vendor.t($data.trendTip),
    t: common_vendor.o((...args) => $options.goTrendDetail && $options.goTrendDetail(...args)),
    v: common_vendor.f($data.metrics, (m, k0, i0) => {
      return {
        a: common_vendor.t(m.label),
        b: common_vendor.t(m.trendText),
        c: common_vendor.n("my-metric-trend " + m.trend),
        d: common_vendor.t(m.value),
        e: common_vendor.t(m.unit),
        f: common_vendor.t(m.hint),
        g: common_vendor.n("mini-" + m.key),
        h: m.key
      };
    }),
    w: common_vendor.o((...args) => $options.goFollowupDetail && $options.goFollowupDetail(...args)),
    x: common_vendor.t($data.followup.done),
    y: common_vendor.t($data.followup.total),
    z: common_vendor.t($data.followup.next),
    A: common_vendor.t($data.followup.last),
    B: common_vendor.t($data.followup.focus),
    C: common_vendor.t($data.followup.advice),
    D: common_vendor.o((...args) => $options.goFollowupDetail && $options.goFollowupDetail(...args)),
    E: common_vendor.o((...args) => $options.onLogout && $options.onLogout(...args))
  };
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render]]);
wx.createPage(MiniProgramPage);
