"use strict";
const common_vendor = require("../../../common/vendor.js");
const api_patient = require("../../../api/patient.js");
const _sfc_main = {
  data() {
    return {
      records: [],
      applies: [],
      lastFollowup: {
        time: "",
        result: "",
        summary: "",
        level: "mid",
        levelText: "一般"
      },
      urgencyOptions: ["普通", "较急", "紧急"],
      urgencyIndex: 0,
      preferDate: "",
      preferTime: "",
      form: {
        symptom: "",
        request: ""
      }
    };
  },
  computed: {
    lastApply() {
      if (!this.applies.length)
        return null;
      return this.applies[0];
    }
  },
  onShow() {
    this.loadAll();
  },
  methods: {
    mapResultStatus(v) {
      const s = String(v || "").toUpperCase();
      const map = { ASSIGNED: "待随访", IN_PROGRESS: "随访中", COMPLETED: "已随访", DONE: "已随访", FINISHED: "已随访" };
      return map[s] || String(v || "");
    },
    mapRisk(v) {
      const raw = String(v || "");
      const s = raw.toUpperCase();
      if (s.includes("HIGH") || raw.includes("高"))
        return { level: "high", text: "高风险" };
      if (s.includes("LOW") || raw.includes("低"))
        return { level: "low", text: "稳定" };
      if (s.includes("MID") || s.includes("MED") || raw.includes("中"))
        return { level: "mid", text: "一般" };
      return { level: "mid", text: "一般" };
    },
    fmtTime(t) {
      return String(t || "").replace("T", " ").slice(0, 16);
    },
    cleanText(v) {
      const s = String(v == null ? "" : v).replace(/\s+/g, " ").trim();
      return s;
    },
    resolveStaffName(row) {
      if (!row || typeof row !== "object")
        return "医生/护士";
      const candidates = [
        row.followupStaffName,
        row.staffName,
        row.doctorName,
        row.followupStaff,
        row.staff,
        row.doctor
      ];
      for (let i = 0; i < candidates.length; i += 1) {
        const text = this.cleanText(candidates[i]);
        if (text)
          return text;
      }
      return "医生/护士";
    },
    mapFollowupStatus(v) {
      const s = String(v || "").toUpperCase();
      const map = {
        ASSIGNED: "待随访",
        IN_PROGRESS: "随访中",
        COMPLETED: "已随访",
        DONE: "已随访",
        FINISHED: "已随访"
      };
      return map[s] || String(v || "");
    },
    async loadAll() {
      try {
        const recRaw = await api_patient.listFollowups();
        const recList = Array.isArray(recRaw) ? recRaw : recRaw && Array.isArray(recRaw.list) ? recRaw.list : recRaw && Array.isArray(recRaw.records) ? recRaw.records : [];
        this.records = (recList || []).map((r) => {
          const risk = this.mapRisk(r.riskLevel || r.level);
          const time = this.fmtTime(r.followupTime || r.measuredAt || r.createdAt || r.time);
          return {
            id: r.id || r.recordId,
            relatedTaskId: r.relatedTaskId || r.related_task_id || r.taskId,
            time,
            level: risk.level,
            levelText: risk.text,
            result: this.mapResultStatus(r.resultStatus || r.result || r.title) || "随访记录",
            summary: r.contentSummary || r.summary || "",
            doctor: this.resolveStaffName(r),
            staffName: this.resolveStaffName(r)
          };
        });
        if (this.records.length) {
          const r0 = this.records[0];
          this.lastFollowup = {
            time: r0.time,
            result: r0.result,
            summary: r0.summary,
            level: r0.level || "mid",
            levelText: r0.levelText || "一般"
          };
        } else {
          this.lastFollowup = { time: "", result: "", summary: "", level: "mid", levelText: "一般" };
        }
        const appRaw = await api_patient.listFollowupApplications();
        const appList = Array.isArray(appRaw) ? appRaw : appRaw && Array.isArray(appRaw.list) ? appRaw.list : appRaw && Array.isArray(appRaw.records) ? appRaw.records : [];
        const mapStatusText = (v) => {
          const s = String(v || "").toLowerCase();
          if (s.includes("approve") || s.includes("pass"))
            return "已通过";
          if (s.includes("reject") || s.includes("deny"))
            return "已拒绝";
          if (s.includes("arrange") || s.includes("schedule"))
            return "已安排";
          return "待处理";
        };
        const parseApplyFields = (a) => {
          const out = { urgency: "普通", preferDate: "", preferTime: "", symptom: "", request: "" };
          out.urgency = a.urgency || a.urgencyLevel || a.levelText || out.urgency;
          out.preferDate = a.preferDate || a.expectDate || a.date || out.preferDate;
          out.preferTime = a.preferTime || a.expectTime || a.timeSlot || out.preferTime;
          out.symptom = a.currentSituation || a.symptom || out.symptom;
          out.request = a.request || a.ask || out.request;
          if (a.ext1 || a.ext2 || a.ext3 || a.ext4 || a.ext5) {
            out.urgency = a.ext1 || out.urgency;
            out.preferDate = a.ext2 || out.preferDate;
            out.preferTime = a.ext3 || out.preferTime;
            out.symptom = a.ext4 || out.symptom;
            out.request = a.ext5 || out.request;
          }
          const pt = a.preferredTime || a.preferred_time;
          if (pt && (!out.preferDate || !out.preferTime)) {
            const s = String(pt).replace("T", " ");
            if (!out.preferDate)
              out.preferDate = s.slice(0, 10);
            if (!out.preferTime)
              out.preferTime = s.slice(11, 16);
          }
          const ar = a.applyReason || a.apply_reason;
          if (ar && (!out.symptom || !out.request || !out.urgency)) {
            const str = String(ar).trim();
            if (str.startsWith("{") && str.endsWith("}")) {
              try {
                const j = JSON.parse(str);
                out.urgency = j.urgency || out.urgency;
                out.preferDate = j.preferDate || out.preferDate;
                out.preferTime = j.preferTime || out.preferTime;
                out.symptom = j.currentSituation || j.symptom || out.symptom;
                out.request = j.request || out.request;
              } catch (e) {
              }
            }
            const mUrg = str.match(/【紧急程度】([^；;]+)/);
            const mSym = str.match(/【当前情况】([^；;]+)/);
            const mReq = str.match(/【诉求】([^；;]+)/);
            if (mUrg && !out.urgency)
              out.urgency = mUrg[1].trim();
            if (mSym && !out.symptom)
              out.symptom = mSym[1].trim();
            if (mReq && !out.request)
              out.request = mReq[1].trim();
          }
          return out;
        };
        this.applies = (appList || []).map((a) => {
          const time = this.fmtTime(a.createdAt || a.time || a.applyTime);
          const f = parseApplyFields(a);
          return {
            id: a.id || a.appId,
            time,
            urgency: f.urgency,
            preferDate: f.preferDate,
            preferTime: f.preferTime,
            symptom: f.symptom,
            request: f.request,
            status: a.status || "pending",
            statusText: a.statusText || mapStatusText(a.status)
          };
        });
      } catch (e) {
      }
    },
    async seedMockIfEmpty() {
      await this.loadAll();
      common_vendor.index.showToast({ title: "已刷新", icon: "success" });
    },
    async openRecord(r) {
      try {
        const detail = await api_patient.getFollowupDetail(r.id);
        const d = detail && typeof detail === "object" ? detail : {};
        const merged = { ...r, ...d };
        const staffName = this.resolveStaffName(merged);
        const risk = this.mapRisk(merged.riskLevel || merged.level || "");
        const resultText = this.mapResultStatus(merged.resultStatus || merged.result || "");
        const summaryText = this.cleanText(merged.contentSummary || merged.summary || "");
        const symptomText = this.cleanText(merged.symptomChange || merged.mainSymptoms || "");
        const nextFollowupText = this.fmtTime(merged.nextFollowupDate || merged.nextPlanTime || "");
        const adviceText = this.cleanText(merged.ext1 || merged.advice || "");
        const relatedTaskId = merged.relatedTaskId || merged.related_task_id || merged.taskId || "";
        const content = [
          `时间：${this.fmtTime(merged.followupTime || merged.measuredAt || merged.createdAt || r.time) || "暂无"}`,
          `随访人员：${staffName}`,
          `风险：${risk.text || r.levelText || "一般"}`,
          `结论：${resultText || "暂无"}`,
          `摘要：${summaryText || "暂无"}`,
          symptomText ? `症状变化：${symptomText}` : "",
          nextFollowupText ? `下次随访：${nextFollowupText}` : "",
          adviceText ? `随访建议：${adviceText}` : "",
          relatedTaskId ? `任务ID：${relatedTaskId}` : ""
        ].filter(Boolean).join("\n");
        common_vendor.index.showModal({
          title: "随访记录详情",
          content,
          showCancel: false,
          confirmText: "知道了"
        });
      } catch (e) {
        const content = [
          `时间：${r.time || "暂无"}`,
          `随访人员：${this.resolveStaffName(r)}`,
          `结论：${r.result || "暂无"}`,
          `摘要：${r.summary || "暂无"}`
        ].join("\n");
        common_vendor.index.showModal({ title: "随访记录详情", content, showCancel: false, confirmText: "知道了" });
      }
    },
    onUrgencyChange(e) {
      this.urgencyIndex = Number(e.detail.value || 0);
    },
    onDateChange(e) {
      this.preferDate = e.detail.value;
    },
    onTimeChange(e) {
      this.preferTime = e.detail.value;
    },
    submitApply() {
      const symptom = (this.form.symptom || "").trim();
      const request = (this.form.request || "").trim();
      if (!symptom)
        return common_vendor.index.showToast({ title: "请填写当前情况", icon: "none" });
      if (!request)
        return common_vendor.index.showToast({ title: "请填写随访诉求", icon: "none" });
      const urgencyText = this.urgencyOptions[this.urgencyIndex] || "普通";
      const preferredTime = this.preferDate && this.preferTime ? `${this.preferDate} ${this.preferTime}:00` : "";
      const payload = {
        // 新版字段：用于前端展示/后端扩展字段
        urgency: urgencyText,
        preferDate: this.preferDate,
        preferTime: this.preferTime,
        currentSituation: symptom,
        request,
        // 兼容后端当前字段（applyReason/contactPhone/preferredTime）
        applyReason: `【紧急程度】${urgencyText}；【当前情况】${symptom}；【诉求】${request}`,
        contactPhone: "",
        preferredTime
      };
      common_vendor.index.showModal({
        title: "确认提交",
        content: "提交后将进入待处理状态，医生端审核后安排随访。",
        confirmText: "提交",
        cancelText: "再想想",
        success: async (res) => {
          if (!res.confirm)
            return;
          try {
            await api_patient.applyFollowup(payload);
            this.form.symptom = "";
            this.form.request = "";
            this.preferDate = "";
            this.preferTime = "";
            this.urgencyIndex = 0;
            await this.loadAll();
            common_vendor.index.showToast({ title: "已提交", icon: "success" });
          } catch (e) {
            common_vendor.index.showToast({ title: "提交失败，请稍后重试", icon: "none" });
          }
        }
      });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_vendor.t($data.lastFollowup.levelText),
    b: common_vendor.n("chip-" + $data.lastFollowup.level),
    c: common_vendor.t($data.lastFollowup.time || "暂无记录"),
    d: common_vendor.t($data.lastFollowup.result || "暂无"),
    e: common_vendor.t($data.lastFollowup.summary || "暂无"),
    f: common_vendor.o((...args) => $options.seedMockIfEmpty && $options.seedMockIfEmpty(...args)),
    g: $data.records.length === 0
  }, $data.records.length === 0 ? {} : {}, {
    h: common_vendor.f($data.records, (r, k0, i0) => {
      return {
        a: common_vendor.t(r.time),
        b: common_vendor.t(r.levelText),
        c: common_vendor.n("badge-" + r.level),
        d: common_vendor.t(r.result),
        e: common_vendor.t(r.summary),
        f: common_vendor.t(r.doctor || "医生/护士"),
        g: r.id,
        h: common_vendor.o(($event) => $options.openRecord(r), r.id)
      };
    }),
    i: $options.lastApply
  }, $options.lastApply ? {
    j: common_vendor.t($options.lastApply.time),
    k: common_vendor.t($options.lastApply.statusText)
  } : {}, {
    l: common_vendor.t($data.urgencyOptions[$data.urgencyIndex]),
    m: $data.urgencyOptions,
    n: $data.urgencyIndex,
    o: common_vendor.o((...args) => $options.onUrgencyChange && $options.onUrgencyChange(...args)),
    p: common_vendor.t($data.preferDate || "请选择"),
    q: $data.preferDate,
    r: common_vendor.o((...args) => $options.onDateChange && $options.onDateChange(...args)),
    s: common_vendor.t($data.preferTime || "请选择"),
    t: $data.preferTime,
    v: common_vendor.o((...args) => $options.onTimeChange && $options.onTimeChange(...args)),
    w: $data.form.symptom,
    x: common_vendor.o(($event) => $data.form.symptom = $event.detail.value),
    y: common_vendor.t($data.form.symptom.length),
    z: $data.form.request,
    A: common_vendor.o(($event) => $data.form.request = $event.detail.value),
    B: common_vendor.t($data.form.request.length),
    C: common_vendor.o((...args) => $options.submitApply && $options.submitApply(...args))
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render]]);
wx.createPage(MiniProgramPage);
