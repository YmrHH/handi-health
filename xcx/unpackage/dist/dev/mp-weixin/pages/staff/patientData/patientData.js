"use strict";
const common_vendor = require("../../../common/vendor.js");
const api_staff = require("../../../api/staff.js");
function mapRiskToUi(v) {
  const s = String(v || "").toLowerCase();
  if (s.includes("high") || s.includes("高"))
    return { level: "high", text: "高风险" };
  if (s.includes("low") || s.includes("低"))
    return { level: "low", text: "低风险" };
  return { level: "mid", text: "中风险" };
}
const _sfc_main = {
  data() {
    return {
      patientId: "",
      profile: { name: "", age: "", gender: "", disease: "", riskLevel: "mid", riskText: "—" },
      today: { sbp: "", dbp: "", hr: "", weight: "", temp: "", steps: "" },
      rehabTasks: [],
      historyLimit: 30,
      historyList: [],
      followupLatest: null,
      advice: ""
    };
  },
  onLoad(query) {
    this.patientId = query.id || query.patientId || query.patient_id || "";
    this.profile.name = query.name || query.patientName ? decodeURIComponent(query.name || query.patientName) : "";
    this.profile.age = query.age || "";
    this.profile.gender = query.gender ? decodeURIComponent(query.gender) : "";
    this.loadData();
  },
  methods: {
    formatDateTime(v) {
      if (!v)
        return "";
      const s = String(v).replace("T", " ");
      return s.length > 19 ? s.slice(0, 19) : s;
    },
    setHistoryLimit(n) {
      const v = Number(n) || 30;
      if (this.historyLimit === v)
        return;
      this.historyLimit = v;
      this.loadData();
    },
    loadData() {
      if (!this.patientId) {
        common_vendor.index.showToast({ title: "缺少患者ID", icon: "none" });
        return;
      }
      common_vendor.index.showLoading({ title: "加载中…", mask: true });
      api_staff.getPatientData(this.patientId, { historyLimit: this.historyLimit }).then((res) => {
        const data = res && res.data ? res.data : res;
        const d = data && data.data ? data.data : data;
        const p = d && (d.profile || d.patient) ? d.profile || d.patient : d || {};
        const pbi = p && (p.basicInfo || p.basic_info) ? p.basicInfo || p.basic_info : null;
        const risk = mapRiskToUi(p.riskLevel || p.risk || p.riskText);
        this.profile = {
          name: p.name || this.profile.name,
          age: p.age || this.profile.age,
          gender: p.gender || this.profile.gender,
          disease: p.disease || p.mainDisease || (pbi ? pbi.ext3 || pbi.mainDisease || pbi.disease : "") || "—",
          riskLevel: risk.level,
          riskText: p.riskText || risk.text
        };
        let latest = d && d.today ? d.today.latest : null;
        if (!latest && d && d.today && Array.isArray(d.today.rows) && d.today.rows.length > 0) {
          latest = d.today.rows[0];
        }
        if (!latest) {
          latest = d && d.latest ? d.latest : {};
        }
        this.today = {
          sbp: latest.sbp ?? latest.SBP ?? "",
          dbp: latest.dbp ?? latest.DBP ?? "",
          hr: latest.hr ?? latest.heartRate ?? latest.heart_rate ?? latest.heartRate ?? "",
          weight: latest.weight ?? latest.weightKg ?? latest.weight_kg ?? latest.weightKg ?? "",
          temp: latest.temp ?? latest.temperatureC ?? latest.temperature_c ?? "",
          steps: latest.steps ?? latest.stepCount ?? latest.ext1 ?? ""
          // steps 若后端无字段，可用 ext1 承载
        };
        this.rehabTasks = d.rehabTasks || d.tasks || [];
        const hist = d.history || d.historyList || [];
        this.historyList = (Array.isArray(hist) ? hist : []).map((h) => {
          const time = this.formatDateTime(h.measuredAt || h.measured_at || h.time || h.date);
          return {
            time,
            sbp: h.sbp ?? "",
            dbp: h.dbp ?? "",
            hr: h.hr ?? h.heartRate ?? h.heart_rate ?? "",
            weight: h.weight ?? h.weightKg ?? h.weight_kg ?? "",
            temp: h.temp ?? h.temperatureC ?? h.temperature_c ?? ""
          };
        });
        const fu = d.followupLatest || d.latestFollowup || (Array.isArray(d.followups) ? d.followups[0] : null) || null;
        if (fu) {
          const t = this.formatDateTime(fu.followupTime || fu.followup_time || fu.createdAt || fu.created_at);
          const nextDate = this.formatDateTime(fu.nextFollowupDate || fu.next_followup_date);
          const nextType = fu.nextFollowupType || fu.next_followup_type || "";
          this.followupLatest = {
            ...fu,
            followupTimeText: t || "—",
            followupTypeText: fu.followupType || fu.followup_type || "",
            advice: fu.advice || fu.ext1 || "",
            medicineName: fu.medicineName || fu.medPlanSummary || fu.med_plan_summary || fu.ext2 || "",
            nextFollowupText: nextType || nextDate ? `${nextType || ""}${nextType && nextDate ? " · " : ""}${nextDate || ""}` : ""
          };
        } else {
          this.followupLatest = null;
        }
      }).catch(() => {
        {
          common_vendor.index.showToast({ title: "加载失败", icon: "none" });
          return;
        }
      }).finally(() => common_vendor.index.hideLoading());
    },
    onSendAdvice() {
      const content = (this.advice || "").trim();
      if (!content) {
        common_vendor.index.showToast({ title: "请填写建议内容", icon: "none" });
        return;
      }
      common_vendor.index.showLoading({ title: "发送中…", mask: true });
      api_staff.sendPatientAdvice(this.patientId, content).then(() => {
        common_vendor.index.showToast({ title: "已发送", icon: "success" });
        this.advice = "";
      }).catch(() => {
        {
          common_vendor.index.showToast({ title: "发送失败", icon: "none" });
          return;
        }
      }).finally(() => common_vendor.index.hideLoading());
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_vendor.t($data.profile.name || "未知患者"),
    b: $data.profile.age || $data.profile.gender
  }, $data.profile.age || $data.profile.gender ? {
    c: common_vendor.t($data.profile.age || ""),
    d: common_vendor.t($data.profile.gender || "")
  } : {}, {
    e: common_vendor.t($data.profile.disease || "—"),
    f: common_vendor.t($data.profile.riskText || "—"),
    g: common_vendor.t($data.profile.riskText || "—"),
    h: common_vendor.n("risk-tag " + ($data.profile.riskLevel || "mid")),
    i: common_vendor.t($data.today.sbp ?? "--"),
    j: common_vendor.t($data.today.dbp ?? "--"),
    k: common_vendor.t($data.today.hr ?? "--"),
    l: common_vendor.t($data.today.weight ?? "--"),
    m: common_vendor.t($data.today.temp ?? "--"),
    n: common_vendor.t($data.today.steps ?? "--"),
    o: common_vendor.n($data.historyLimit === 7 ? "active" : ""),
    p: common_vendor.o(($event) => $options.setHistoryLimit(7)),
    q: common_vendor.n($data.historyLimit === 30 ? "active" : ""),
    r: common_vendor.o(($event) => $options.setHistoryLimit(30)),
    s: common_vendor.n($data.historyLimit === 90 ? "active" : ""),
    t: common_vendor.o(($event) => $options.setHistoryLimit(90)),
    v: $data.historyList.length === 0
  }, $data.historyList.length === 0 ? {} : {
    w: common_vendor.f($data.historyList, (h, i, i0) => {
      return {
        a: common_vendor.t(h.time || "—"),
        b: common_vendor.t(h.sbp || "--"),
        c: common_vendor.t(h.dbp || "--"),
        d: common_vendor.t(h.hr || "--"),
        e: common_vendor.t(h.weight || "--"),
        f: common_vendor.t(h.temp || "--"),
        g: i
      };
    })
  }, {
    x: !$data.followupLatest
  }, !$data.followupLatest ? {} : {
    y: common_vendor.t($data.followupLatest.followupTimeText || "—"),
    z: common_vendor.t($data.followupLatest.followupTypeText || $data.followupLatest.followupType || "—"),
    A: common_vendor.t($data.followupLatest.symptomChange || "—"),
    B: common_vendor.t($data.followupLatest.contentSummary || "—"),
    C: common_vendor.t($data.followupLatest.advice || "—"),
    D: common_vendor.t($data.followupLatest.nextFollowupText || "—")
  }, {
    E: !$data.followupLatest
  }, !$data.followupLatest ? {} : {
    F: common_vendor.t($data.followupLatest.medicineName || $data.followupLatest.medPlanSummary || "—"),
    G: common_vendor.t($data.followupLatest.medAdherence || "—"),
    H: common_vendor.t($data.followupLatest.adverseReaction || "—")
  }, {
    I: $data.rehabTasks.length === 0
  }, $data.rehabTasks.length === 0 ? {} : {
    J: common_vendor.f($data.rehabTasks, (t, i, i0) => {
      return {
        a: common_vendor.t(t.title || "康复任务"),
        b: common_vendor.t(t.statusText || t.status || "—"),
        c: common_vendor.t(t.date || "—"),
        d: i
      };
    })
  }, {
    K: $data.advice,
    L: common_vendor.o(($event) => $data.advice = $event.detail.value),
    M: common_vendor.o((...args) => $options.onSendAdvice && $options.onSendAdvice(...args))
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render]]);
wx.createPage(MiniProgramPage);
