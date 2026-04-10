"use strict";
const common_vendor = require("../../../common/vendor.js");
const utils_metricsStore = require("../../../utils/metrics-store.js");
const api_patient = require("../../../api/patient.js");
const _sfc_main = {
  data() {
    return {
      rangeText: "近7天",
      metrics: [
        { key: "bp", label: "血压", value: "132/85", unit: "mmHg", level: "mid", levelText: "关注", hint: "与低盐饮食任务强关联" },
        { key: "hr", label: "心率", value: "68", unit: "次/分", level: "ok", levelText: "稳定", hint: "与睡眠时长/情绪相关" },
        { key: "glucose", label: "血糖", value: "6.8", unit: "mmol/L", level: "mid", levelText: "关注", hint: "与用药打卡/饮食记录相关" },
        { key: "weight", label: "体温", value: "36.6", unit: "℃", level: "ok", levelText: "正常", hint: "建议每日同一时间测量" }
      ],
      rehab: { done: 3, total: 5, next: "" },
      association: null,
      associationLoading: false,
      associationError: "",
      tips: [
        "若血压连续 3 天偏高，系统会自动增加“血压复测”随访任务。",
        "康养任务完成率 < 60% 时，将触发“依从性提醒”。",
        "医生端可针对异常指标下发“医生健康建议”，小程序端在“我的”页展示。"
      ],
      records: [
        { id: 1, title: "随访：血压复测", time: "2026-01-02", note: "建议晚餐低盐，连续监测 3 天" },
        { id: 2, title: "随访：用药依从", time: "2025-12-28", note: "按时服药，记录餐后血糖" }
      ]
    };
  },
  computed: {
    abnormalCount() {
      return this.metrics.filter((m) => m.level === "high" || m.level === "mid").length;
    }
  },
  onShow() {
    this.loadFromStorage();
    this.loadAssociation();
  },
  methods: {
    mapFollowupStatus(v) {
      const s = String(v || "").toUpperCase();
      const map = { ASSIGNED: "待随访", IN_PROGRESS: "随访中", COMPLETED: "已随访", DONE: "已随访", FINISHED: "已随访" };
      return map[s] || String(v || "");
    },
    mapResultStatus(v) {
      const s = String(v || "").toUpperCase();
      const map = { ASSIGNED: "待随访", IN_PROGRESS: "随访中", COMPLETED: "已随访", DONE: "已随访", FINISHED: "已随访" };
      return map[s] || String(v || "");
    },
    formatDateText(v) {
      if (!v)
        return "";
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
    async loadAssociation() {
      var _a, _b;
      this.associationLoading = true;
      this.associationError = "";
      try {
        const res = await api_patient.getFollowupAssociation();
        const data = res && res.data !== void 0 ? res.data : res;
        this.association = data || null;
        const pendingTasks = Array.isArray(data == null ? void 0 : data.pendingTasks) ? data.pendingTasks : Array.isArray(data == null ? void 0 : data.tasks) ? data.tasks : [];
        const recs = (Array.isArray(data == null ? void 0 : data.recentRecords) ? data.recentRecords : Array.isArray(data == null ? void 0 : data.records) ? data.records : null) || ((data == null ? void 0 : data.lastFollowup) ? [data.lastFollowup] : []);
        const latestRecord = this.pickLatestFollowupRecord(recs);
        const earliestPendingTask = this.pickEarliestPendingTask(pendingTasks);
        let next = data && (((_a = data.nextFollowup) == null ? void 0 : _a.planTime) || ((_b = data.nextFollowup) == null ? void 0 : _b.dueAt) || data.nextDueAt || data.nextDue || data.nextFollowupTime) || earliestPendingTask && (earliestPendingTask.planTime || earliestPendingTask.dueAt || earliestPendingTask.plan_time) || latestRecord && (latestRecord.nextFollowupDate || latestRecord.nextPlanTime || latestRecord.nextFollowupTime) || "";
        const last = (data == null ? void 0 : data.lastFollowup) && (data.lastFollowup.followupTime || data.lastFollowup.createdAt || data.lastFollowup.time) || latestRecord && (latestRecord.followupTime || latestRecord.createdAt || latestRecord.time) || "";
        const nextTs = this.parseDateMs(next);
        const lastTs = this.parseDateMs(last);
        if (Number.isFinite(nextTs) && Number.isFinite(lastTs) && nextTs < lastTs) {
          const latestRecordNext = latestRecord && (latestRecord.nextFollowupDate || latestRecord.nextPlanTime || latestRecord.nextFollowupTime || "");
          const latestRecordNextTs = this.parseDateMs(latestRecordNext);
          next = Number.isFinite(latestRecordNextTs) && latestRecordNextTs >= lastTs ? latestRecordNext : "";
        }
        this.rehab.next = next ? this.formatDateText(next) : "";
        if (Array.isArray(recs) && recs.length) {
          this.records = recs.slice().sort((a, b) => {
            const ta = this.parseDateMs(a && (a.followupTime || a.measuredAt || a.createdAt || a.time));
            const tb = this.parseDateMs(b && (b.followupTime || b.measuredAt || b.createdAt || b.time));
            if (!Number.isFinite(ta) && !Number.isFinite(tb))
              return 0;
            if (!Number.isFinite(ta))
              return 1;
            if (!Number.isFinite(tb))
              return -1;
            return tb - ta;
          }).slice(0, 5).map((r) => ({
            id: r.id || r.recordId || r.taskId || r.relatedTaskId,
            title: this.mapResultStatus(r.title || r.conclusion || r.resultStatus || r.result) || "随访记录",
            time: String(r.followupTime || r.measuredAt || r.createdAt || r.time || "").replace("T", " ").slice(0, 16),
            note: r.contentSummary || r.summary || r.symptomChange || r.note || ""
          }));
        }
        const tips = Array.isArray(data == null ? void 0 : data.tips) && data.tips || null;
        if (tips && tips.length)
          this.tips = tips;
      } catch (e) {
        this.associationError = e && e.message ? e.message : "加载失败";
      } finally {
        this.associationLoading = false;
      }
    },
    loadFromStorage() {
      try {
        const latest = utils_metricsStore.getDailyMetricsLatest();
        if (latest && typeof latest === "object" && Array.isArray(latest.metrics)) {
          this.metrics = latest.metrics.slice(0, 4).map((m) => ({
            key: m.key,
            label: m.label,
            value: m.value,
            unit: m.unit,
            level: m.trend === "up" ? "mid" : m.trend === "down" ? "ok" : "ok",
            levelText: m.trendText || "稳定",
            hint: m.hint || "与康养任务关联分析"
          }));
        }
        const rs = common_vendor.index.getStorageSync("followup_summary") || common_vendor.index.getStorageSync("rehab_summary");
        if (rs && typeof rs === "object") {
          this.rehab = { ...this.rehab, ...rs };
        }
        const tips = common_vendor.index.getStorageSync("followup_tips");
        if (Array.isArray(tips) && tips.length)
          this.tips = tips;
        const rec = common_vendor.index.getStorageSync("followup_records");
        if (Array.isArray(rec) && rec.length)
          this.records = rec;
      } catch (e) {
      }
    },
    refreshMock() {
      this.loadFromStorage();
      this.loadAssociation();
      common_vendor.index.showToast({ title: "已刷新", icon: "success" });
    },
    goRehab() {
      common_vendor.index.navigateTo({ url: "/pages/patient/rehab/rehab" });
    },
    goFollowup() {
      common_vendor.index.navigateTo({ url: "/pages/patient/followupDetail/followupDetail" });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return {
    a: common_vendor.t($data.rangeText),
    b: common_vendor.t($data.rehab.done),
    c: common_vendor.t($data.rehab.total),
    d: common_vendor.t($options.abnormalCount),
    e: common_vendor.t($data.rehab.next),
    f: common_vendor.o((...args) => $options.refreshMock && $options.refreshMock(...args)),
    g: common_vendor.f($data.metrics, (m, k0, i0) => {
      return {
        a: common_vendor.t(m.label),
        b: common_vendor.t(m.levelText),
        c: common_vendor.n("fa-trend-chip " + m.level),
        d: common_vendor.t(m.value),
        e: common_vendor.t(m.unit),
        f: common_vendor.t(m.hint),
        g: m.key
      };
    }),
    h: common_vendor.f($data.tips, (t, idx, i0) => {
      return {
        a: common_vendor.t(t),
        b: idx
      };
    }),
    i: common_vendor.o((...args) => $options.goRehab && $options.goRehab(...args)),
    j: common_vendor.o((...args) => $options.goFollowup && $options.goFollowup(...args)),
    k: common_vendor.f($data.records, (r, k0, i0) => {
      return {
        a: common_vendor.t(r.title),
        b: common_vendor.t(r.time),
        c: common_vendor.t(r.note),
        d: r.id
      };
    })
  };
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render]]);
wx.createPage(MiniProgramPage);
