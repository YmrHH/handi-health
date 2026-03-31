"use strict";
const common_vendor = require("../../../common/vendor.js");
const api_patient = require("../../../api/patient.js");
const utils_session = require("../../../utils/session.js");
const _sfc_main = {
  data() {
    return {
      patientName: "张阿姨",
      todayText: "",
      todayDateStr: "",
      riskLevel: "mid",
      riskText: "中风险",
      // 今日任务：现在只统计两项：自测 + 康养指标
      dailyDone: false,
      surveyDone: false,
      todoDoneCount: 0,
      // 下次化验信息
      nextLab: {
        date: "2025-12-20",
        daysLeft: 0,
        overdue: false
      },
      nextLabText: "",
      latestAdvice: null,
      // 是否安排今天康养指标填报（后端未来可控制）
      hasTodaySurvey: true
    };
  },
  computed: {
    adviceCard() {
      const advice = this.latestAdvice;
      if (advice && advice.content) {
        const metaParts = [];
        if (advice.doctor)
          metaParts.push(advice.doctor);
        if (advice.time)
          metaParts.push(String(advice.time).replace("T", " ").slice(0, 16));
        return {
          title: advice.title || "医生最新建议",
          badgeText: "最新消息",
          badgeClass: "normal",
          text: this.buildAdvicePreview(advice.content),
          meta: metaParts.join(" · ")
        };
      }
      return {
        title: "下次医院化验",
        badgeText: this.nextLab.overdue ? "已超期" : "剩余 " + this.nextLab.daysLeft + " 天",
        badgeClass: this.nextLab.overdue ? "overdue" : "normal",
        text: this.nextLabText,
        meta: ""
      };
    }
  },
  onLoad() {
    this.loadLocalUserProfile();
    this.initToday();
    this.updateNextLab();
    this.checkDailyStatus();
    this.checkTodaySurvey();
    this.fetchHomeSummary();
  },
  onShow() {
    this.loadLocalUserProfile();
    this.initToday();
    this.checkDailyStatus();
    this.checkTodaySurvey();
    this.fetchHomeSummary();
  },
  methods: {
    // 从本地缓存/登录态读取用户信息，让首页顶部随个人资料编辑实时刷新
    loadLocalUserProfile() {
      try {
        const userInfo = utils_session.getScopedStorageSync("userInfo", null);
        if (userInfo && typeof userInfo === "object") {
          if (userInfo.name)
            this.patientName = userInfo.name;
          const r0 = userInfo.riskLevel || userInfo.risk_level || userInfo.risk || "";
          if (r0) {
            const r = String(r0).toLowerCase();
            const map = {
              "低": { level: "low", text: "低风险" },
              "中": { level: "mid", text: "中风险" },
              "高": { level: "high", text: "高风险" },
              "low": { level: "low", text: "低风险" },
              "mid": { level: "mid", text: "中风险" },
              "high": { level: "high", text: "高风险" }
            };
            const m = map[r0] || map[r];
            if (m) {
              this.riskLevel = m.level;
              this.riskText = m.text;
            }
          }
        }
        const latestAdvice = utils_session.getScopedStorageSync("doctor_advice_latest", null);
        if (latestAdvice && typeof latestAdvice === "object" && latestAdvice.content) {
          this.latestAdvice = { ...latestAdvice };
        }
        const u = utils_session.getUser();
        if (u && u.name) {
          this.patientName = u.name;
        }
      } catch (e) {
      }
    },
    fetchHomeSummary() {
      api_patient.getHomeSummary().then((res) => {
        const data = res || {};
        if (data.patientName)
          this.patientName = data.patientName;
        if (data.risk) {
          this.riskLevel = data.risk.level || this.riskLevel;
          this.riskText = data.risk.text || this.riskText;
        } else {
          if (data.riskLevel)
            this.riskLevel = data.riskLevel;
          if (data.riskText)
            this.riskText = data.riskText;
        }
        if (typeof data.dailyDone === "boolean")
          this.dailyDone = this.dailyDone || data.dailyDone;
        if (typeof data.surveyDone === "boolean")
          this.surveyDone = this.surveyDone || data.surveyDone;
        if (data.dailyDoneDate) {
          utils_session.setScopedStorageSync("daily_measure_date", String(data.dailyDoneDate));
        }
        if (data.surveyDoneDate) {
          utils_session.setScopedStorageSync("tcm_survey_date", String(data.surveyDoneDate));
        }
        if (typeof data.hasTodaySurvey === "boolean")
          this.hasTodaySurvey = data.hasTodaySurvey;
        if (data.nextLab && data.nextLab.date) {
          this.nextLab = { ...this.nextLab, ...data.nextLab };
          this.updateNextLab();
        }
        if (data.nextFollowupTime && !data.nextLab) {
          const nextDate = String(data.nextFollowupTime).replace("T", " ").slice(0, 10);
          if (nextDate) {
            this.nextLab = { ...this.nextLab, date: nextDate };
            this.updateNextLab();
          }
        }
        if (data.advice && typeof data.advice === "object" && data.advice.content) {
          this.latestAdvice = { ...data.advice };
          utils_session.setScopedStorageSync("doctor_advice_latest", this.latestAdvice);
        }
        this.updateTodoCount();
      }).catch(() => {
      });
    },
    initToday() {
      const date = /* @__PURE__ */ new Date();
      const y = date.getFullYear();
      const m = date.getMonth() + 1;
      const d = date.getDate();
      const weekdayList = ["日", "一", "二", "三", "四", "五", "六"];
      const w = weekdayList[date.getDay()];
      const dateStr = y + "-" + (m < 10 ? "0" + m : m) + "-" + (d < 10 ? "0" + d : d);
      this.todayText = `${y}年${m}月${d}日 周${w}`;
      this.todayDateStr = dateStr;
    },
    // 下次化验
    updateNextLab() {
      const labDateStr = this.nextLab.date;
      if (!labDateStr) {
        this.nextLabText = "暂无计划，可在门诊时由医生为您安排化验时间。";
        return;
      }
      const today = /* @__PURE__ */ new Date();
      const target = new Date(labDateStr.replace(/-/g, "/"));
      const diffMs = target.getTime() - today.setHours(0, 0, 0, 0);
      const diffDays = Math.floor(diffMs / (86400 * 1e3));
      let overdue = false;
      let text = "";
      if (diffDays < 0) {
        overdue = true;
        text = `原定 ${labDateStr} 需要在医院化验血糖/血脂等项目，请尽快安排就诊或体检。`;
      } else if (diffDays === 0) {
        text = `今天（${labDateStr}）是您约定的化验日，请记得前往医院完成检查。`;
      } else {
        text = `预计在 ${labDateStr} 到医院化验血糖、血脂、尿酸等项目。`;
      }
      this.nextLab.daysLeft = diffDays < 0 ? 0 : diffDays;
      this.nextLab.overdue = overdue;
      this.nextLabText = text;
    },
    // 今日自测是否完成：可以先用本地存储，后面改成后端返回
    checkDailyStatus() {
      const today = this.todayDateStr;
      const d = utils_session.getScopedStorageSync("daily_measure_date", "");
      this.dailyDone = d === today;
      this.updateTodoCount();
    },
    // 今日康养指标是否完成
    checkTodaySurvey() {
      const today = this.todayDateStr;
      const d = utils_session.getScopedStorageSync("tcm_survey_date", "");
      this.surveyDone = d === today;
      this.updateTodoCount();
      this.hasTodaySurvey = true;
    },
    updateTodoCount() {
      let count = 0;
      if (this.dailyDone) {
        count++;
      }
      if (this.surveyDone) {
        count++;
      }
      this.todoDoneCount = count;
    },
    buildAdvicePreview(content) {
      const raw = String(content || "").replace(/\r/g, "\n");
      const lines = raw.split(/\n+/).map((line) => String(line || "").replace(/^[\s\-•·*]+/, "").trim()).filter(Boolean);
      const summary = (lines.length ? lines.join(" ") : raw.replace(/\s+/g, " ").trim()).replace(/\s+/g, " ").trim();
      if (!summary)
        return "暂无最新建议，请继续保持规律监测与康养指标填报。";
      if (summary.length <= 48)
        return summary;
      return summary.slice(0, 48).replace(/[，、；：,.。\s]+$/g, "") + "…";
    },
    onOpenAdviceCard() {
      if (this.latestAdvice && this.latestAdvice.content) {
        common_vendor.index.navigateTo({ url: "/pages/patient/message/message" });
      }
    },
    // ===== 导航 ===== //
    onGoMonitor() {
      common_vendor.index.navigateTo({
        url: "/pages/patient/monitor/monitor?from=home"
      });
    },
    onGoSurvey() {
      common_vendor.index.navigateTo({
        url: "/pages/patient/survey/survey",
        success() {
          console.log("navigateTo survey success");
        },
        fail(err) {
          console.error("navigateTo survey fail:", err);
        }
      });
    },
    onGoTongue() {
      common_vendor.index.navigateTo({
        url: "/pages/patient/tongue/tongue"
      });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: $data.patientName
  }, $data.patientName ? {
    b: common_vendor.t($data.patientName)
  } : {}, {
    c: common_vendor.t($data.todayText),
    d: common_vendor.t($data.riskText),
    e: common_vendor.n("risk-pill " + $data.riskLevel),
    f: common_vendor.t($data.todoDoneCount),
    g: common_vendor.t($data.dailyDone ? "已完成" : "未完成"),
    h: common_vendor.n("pill-status " + ($data.dailyDone ? "done" : "todo")),
    i: common_vendor.t($data.surveyDone ? "已完成" : "未完成"),
    j: common_vendor.n("pill-status " + ($data.surveyDone ? "done" : "todo")),
    k: common_vendor.o((...args) => $options.onGoMonitor && $options.onGoMonitor(...args)),
    l: common_vendor.o((...args) => $options.onGoSurvey && $options.onGoSurvey(...args)),
    m: common_vendor.t($options.adviceCard.title),
    n: common_vendor.t($options.adviceCard.badgeText),
    o: common_vendor.n("lab-badge " + $options.adviceCard.badgeClass),
    p: common_vendor.t($options.adviceCard.text),
    q: $options.adviceCard.meta
  }, $options.adviceCard.meta ? {
    r: common_vendor.t($options.adviceCard.meta)
  } : {}, {
    s: common_vendor.n($data.latestAdvice && $data.latestAdvice.content ? "lab-tip-card--advice" : ""),
    t: common_vendor.o((...args) => $options.onOpenAdviceCard && $options.onOpenAdviceCard(...args)),
    v: common_vendor.o((...args) => $options.onGoMonitor && $options.onGoMonitor(...args)),
    w: common_vendor.o((...args) => $options.onGoTongue && $options.onGoTongue(...args))
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render]]);
wx.createPage(MiniProgramPage);
