"use strict";
const common_vendor = require("../../../common/vendor.js");
const api_patient = require("../../../api/patient.js");
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
      avatarUrl: "/static/assets/avatar-me.png",
      displayName: "用*A",
      syncText: "已同步 · 刚刚",
      showMedicalMore: false,
      risk: { level: "mid", text: "中风险", score: 78 },
      metrics: utils_metricsStore.buildDefaultLatestMetrics(),
      followup: {
        done: 3,
        total: 5,
        next: "",
        last: "2026-01-02",
        advice: "规律服药/低盐饮食",
        focus: "血压、血糖"
      },
      medical: {
        hospital: "医生端导入",
        department: "—",
        docType: "病例摘要",
        admissionTime: "—",
        complaint: "—",
        diagnosisTCM: "—",
        diagnosisWM: "—",
        histories: [],
        vitals: "—",
        surgery: "—",
        allergy: "—",
        imaging: "—"
      }
    };
  },
  onShow() {
    this.loadFromStorage();
    this.fetchRemote();
  },
  methods: {
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
      const advice = associationSummary.advice || associationData.advice || (Array.isArray(associationData.tips) && associationData.tips.length ? associationData.tips[0] : "") || followupList[0] && (followupList[0].advice || followupList[0].contentSummary || followupList[0].summary) || "";
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
          const oldProfile = utils_session.getScopedStorageSync("patient_profile", {}) || {};
          utils_session.setScopedStorageSync("patient_profile", {
            ...oldProfile && typeof oldProfile === "object" ? oldProfile : {},
            displayName: this.displayName,
            avatarUrl: this.avatarUrl,
            risk: p1 && p1.risk ? { ...this.risk, ...p1.risk } : this.risk
          });
        }
        if (p1 && typeof p1 === "object") {
          if (p1.risk) {
            this.risk = { ...this.risk, ...p1.risk };
            const oldProfile = utils_session.getScopedStorageSync("patient_profile", {}) || {};
            utils_session.setScopedStorageSync("patient_profile", {
              ...oldProfile && typeof oldProfile === "object" ? oldProfile : {},
              displayName: this.displayName,
              avatarUrl: this.avatarUrl,
              risk: this.risk
            });
          }
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
        const todayRecord = this.normalizeMeasurementRecord(p2);
        const homeLatestRecord = this.normalizeMeasurementRecord(p1 && p1.latestMeasurement ? p1.latestMeasurement : null);
        const backfilled = this.applyMeasurementToLocal(todayRecord, "后端最新体征") || this.applyMeasurementToLocal(homeLatestRecord, "首页最新体征");
        if (!backfilled && p1 && Array.isArray(p1.metrics) && p1.metrics.length) {
          this.mergeHomeMetricsToLocal(p1.metrics, "后端汇总");
        }
        this.loadFromStorage();
      }).catch(() => {
      });
    },
    loadFromStorage() {
      try {
        const profile = utils_session.getScopedStorageSync("patient_profile", null);
        if (profile && typeof profile === "object") {
          this.displayName = profile.displayName || profile.name || this.displayName;
          this.avatarUrl = profile.avatarUrl || this.avatarUrl;
          if (profile.risk)
            this.risk = { ...this.risk, ...profile.risk };
          if (profile.medical)
            this.medical = { ...this.medical, ...profile.medical };
        }
        const userInfo = utils_session.getScopedStorageSync("userInfo", null);
        if (userInfo && typeof userInfo === "object") {
          this.displayName = userInfo.name || userInfo.displayName || this.displayName;
          this.avatarUrl = userInfo.avatarUrl || this.avatarUrl;
        }
        const latest = utils_metricsStore.getDailyMetricsLatest();
        if (latest && typeof latest === "object" && Array.isArray(latest.metrics) && latest.metrics.length) {
          this.metrics = latest.metrics;
        }
        const fu = utils_session.getScopedStorageSync("followup_summary", null);
        if (fu && typeof fu === "object") {
          this.followup = { ...this.followup, ...fu };
        }
        const syncAt = utils_session.getScopedStorageSync("profile_sync_at", "");
        if (syncAt)
          this.syncText = "已同步 · " + syncAt;
      } catch (e) {
      }
    },
    onRefresh() {
      this.fetchRemote();
      common_vendor.index.showToast({ title: "已刷新", icon: "success" });
    },
    toggleMedical() {
      this.showMedicalMore = !this.showMedicalMore;
    },
    goMetric(key) {
      if (key === "bp")
        return common_vendor.index.navigateTo({ url: "/pages/patient/bp/bp" });
      if (key === "hr")
        return common_vendor.index.navigateTo({ url: "/pages/patient/hr/hr" });
      if (key === "weight")
        return common_vendor.index.navigateTo({ url: "/pages/patient/weight/weight" });
      if (key === "spo2")
        return common_vendor.index.navigateTo({ url: "/pages/patient/spo2/spo2" });
      return this.goDashboard();
    },
    goDashboard() {
      common_vendor.index.navigateTo({ url: "/pages/patient/dashboard/dashboard" });
    },
    goRehab() {
      common_vendor.index.navigateTo({ url: "/pages/patient/rehab/rehab" });
    },
    goFollowup() {
      common_vendor.index.navigateTo({ url: "/pages/patient/followup/followup" });
    },
    goChat() {
      common_vendor.index.switchTab({ url: "/pages/patient/chat/chat" });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: $data.avatarUrl,
    b: common_vendor.t($data.displayName),
    c: common_vendor.t($data.syncText),
    d: common_vendor.o((...args) => $options.onRefresh && $options.onRefresh(...args)),
    e: common_vendor.t($data.risk.text),
    f: common_vendor.n("hf-risk-chip risk-" + $data.risk.level),
    g: common_vendor.t($data.risk.score),
    h: common_vendor.o((...args) => $options.goDashboard && $options.goDashboard(...args)),
    i: common_vendor.o((...args) => $options.goRehab && $options.goRehab(...args)),
    j: common_vendor.o((...args) => $options.goChat && $options.goChat(...args)),
    k: common_vendor.o((...args) => $options.goDashboard && $options.goDashboard(...args)),
    l: common_vendor.f($data.metrics, (m, k0, i0) => {
      return {
        a: common_vendor.t(m.label),
        b: common_vendor.t(m.trendText),
        c: common_vendor.n("hf-metric-trend " + m.trend),
        d: common_vendor.t(m.value),
        e: common_vendor.t(m.unit),
        f: common_vendor.t(m.hint),
        g: common_vendor.n("mini-" + m.key),
        h: m.key,
        i: common_vendor.o(($event) => $options.goMetric(m.key), m.key)
      };
    }),
    m: common_vendor.o((...args) => $options.goFollowup && $options.goFollowup(...args)),
    n: common_vendor.t($data.followup.done),
    o: common_vendor.t($data.followup.total),
    p: common_vendor.t($data.followup.next),
    q: common_vendor.t($data.followup.last),
    r: common_vendor.t($data.followup.advice),
    s: common_vendor.t($data.followup.focus),
    t: common_vendor.o((...args) => $options.goRehab && $options.goRehab(...args)),
    v: common_vendor.t($data.showMedicalMore ? "收起" : "展开"),
    w: common_vendor.t($data.showMedicalMore ? "˄" : "˅"),
    x: common_vendor.o((...args) => $options.toggleMedical && $options.toggleMedical(...args)),
    y: common_vendor.t($data.medical.docType),
    z: common_vendor.t($data.medical.hospital),
    A: common_vendor.t($data.medical.department),
    B: common_vendor.t($data.medical.admissionTime),
    C: common_vendor.t($data.medical.complaint),
    D: common_vendor.t($data.medical.diagnosisTCM),
    E: common_vendor.t($data.medical.diagnosisWM),
    F: common_vendor.f($data.medical.histories, (h, idx, i0) => {
      return {
        a: common_vendor.t(h),
        b: idx
      };
    }),
    G: $data.showMedicalMore
  }, $data.showMedicalMore ? {
    H: common_vendor.t($data.medical.vitals),
    I: common_vendor.t($data.medical.surgery),
    J: common_vendor.t($data.medical.allergy),
    K: common_vendor.t($data.medical.imaging)
  } : {});
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../../.sourcemap/mp-weixin/pages/patient/healthFile/healthFile.js.map
