"use strict";
const common_vendor = require("../../../common/vendor.js");
const api_patient = require("../../../api/patient.js");
const utils_session = require("../../../utils/session.js");
const _sfc_main = {
  data() {
    return {
      currentStep: 0,
      steps: [],
      answers: {},
      todayDateStr: ""
    };
  },
  computed: {
    progressPct() {
      if (!this.steps || !this.steps.length)
        return 0;
      return Math.round((this.currentStep + 1) / this.steps.length * 100);
    }
  },
  onLoad() {
    this.initToday();
    this.initSteps();
  },
  methods: {
    initToday() {
      const d = /* @__PURE__ */ new Date();
      const y = d.getFullYear();
      const m = ("0" + (d.getMonth() + 1)).slice(-2);
      const day = ("0" + d.getDate()).slice(-2);
      const dateStr = `${y}-${m}-${day}`;
      this.setData({
        todayDateStr: dateStr
      });
    },
    // 判定是否为单选题
    isSingle(item) {
      return item && Array.isArray(item.options);
    },
    // 取数值型答案
    getNumAnswer(field) {
      const v = this.answers[field];
      return typeof v === "number" && !isNaN(v) ? v : void 0;
    },
    displayScore(item) {
      const v = this.getNumAnswer(item.field);
      if (v !== void 0)
        return v;
      if (typeof item.default === "number")
        return item.default;
      return item.min !== void 0 ? item.min : 0;
    },
    minLabel(item) {
      if (item.minLabel !== void 0 && item.minLabel !== null)
        return item.minLabel;
      return item.min !== void 0 ? item.min : 0;
    },
    maxLabel(item) {
      if (item.maxLabel !== void 0 && item.maxLabel !== null)
        return item.maxLabel;
      return item.max !== void 0 ? item.max : 10;
    },
    // 生成问卷步骤（根据慢性病类型拼装更贴合的问题）
    buildSteps(disease) {
      const diseaseLabelMap = {
        HTN: "高血压",
        DM: "糖尿病",
        HF: "冠心病/心衰",
        COPD: "慢阻肺/哮喘",
        OTHER: "其他/不确定"
      };
      const base = [
        // 第 1 步：慢性病类型 + 今日总体状态
        {
          step: 1,
          questions: [
            {
              field: "chronic_disease",
              title: "您目前主要慢性病类型是？",
              desc: "用于生成更贴合的每日问卷（可随时修改）。",
              options: [
                { value: "HTN", label: "高血压" },
                { value: "DM", label: "糖尿病" },
                { value: "HF", label: "冠心病/心衰" },
                { value: "COPD", label: "慢阻肺/哮喘" },
                { value: "OTHER", label: "其他/不确定" }
              ]
            },
            {
              field: "overall_star",
              type: "star",
              max: 5,
              title: "今天整体感觉如何？",
              desc: "1 星最差，5 星最好。",
              labels: ["很差", "很好"]
            },
            {
              field: "fatigue_score",
              type: "score",
              min: 0,
              max: 10,
              step: 1,
              title: "今天乏力程度（0–10 分）",
              desc: "0 分无乏力，10 分非常疲惫。",
              minLabel: "无",
              maxLabel: "很重"
            }
          ]
        },
        // 第 2 步：核心症状（适用于多数慢性病）
        {
          step: 2,
          questions: [
            {
              field: "dyspnea_score",
              type: "score",
              min: 0,
              max: 10,
              step: 1,
              title: "气短/呼吸困难程度（0–10 分）",
              desc: "0 分无气短，10 分休息也很难呼吸。",
              minLabel: "无",
              maxLabel: "很重"
            },
            {
              field: "chest_tightness",
              title: "是否有胸闷或胸部不适？",
              options: [
                { value: 0, label: "没有" },
                { value: 1, label: "偶尔有" },
                { value: 2, label: "经常有" },
                { value: 3, label: "明显加重/影响活动" }
              ]
            },
            {
              field: "palpitation_score",
              type: "score",
              min: 0,
              max: 10,
              step: 1,
              title: "心慌/心悸程度（0–10 分）",
              desc: "如无此症状可保持 0 分。",
              minLabel: "无",
              maxLabel: "很重",
              required: false,
              default: 0
            },
            {
              field: "edema",
              title: "下肢或脚踝是否有浮肿？",
              options: [
                { value: 0, label: "没有" },
                { value: 1, label: "下午有轻微" },
                { value: 2, label: "明显压痕" },
                { value: 3, label: "持续较重浮肿" }
              ]
            }
          ]
        },
        // 第 3 步：睡眠 / 情绪 / 饮食
        {
          step: 3,
          questions: [
            {
              field: "sleep_star",
              type: "star",
              max: 5,
              title: "昨晚睡眠质量（1–5 星）",
              labels: ["很差", "很好"]
            },
            {
              field: "night_wake",
              title: "昨晚夜间醒来次数？",
              options: [
                { value: 0, label: "0–1 次" },
                { value: 1, label: "2–3 次" },
                { value: 2, label: "4 次以上" },
                { value: 3, label: "几乎整夜睡不安稳" }
              ]
            },
            {
              field: "mood_star",
              type: "star",
              max: 5,
              title: "今天情绪状态（1–5 星）",
              desc: "1 星很差，5 星很好。",
              labels: ["很差", "很好"]
            },
            {
              field: "appetite_star",
              type: "star",
              max: 5,
              title: "今天食欲情况（1–5 星）",
              labels: ["很差", "很好"]
            },
            {
              field: "stool",
              title: "最近一周大便情况？",
              options: [
                { value: 0, label: "每天一次、成形" },
                { value: 1, label: "偏稀或次数偏多" },
                { value: 2, label: "偏干或两天一次以上" },
                { value: 3, label: "需用药或非常不规律" }
              ]
            }
          ]
        }
      ];
      const commonMgmt = [
        {
          field: "med_adherence_star",
          type: "star",
          max: 5,
          title: "今天用药是否按时（1–5 星）",
          desc: "1 星经常漏服，5 星完全按时。",
          labels: ["常漏服", "很按时"]
        },
        {
          field: "diet_adherence_star",
          type: "star",
          max: 5,
          title: "今天饮食控制情况（1–5 星）",
          desc: "综合低盐/控糖/少油/规律三餐等。",
          labels: ["未控制", "控制很好"]
        },
        {
          field: "exercise_minutes",
          type: "score",
          min: 0,
          max: 120,
          step: 10,
          unit: "分钟",
          title: "今天运动/步行时长（选填）",
          desc: "如未运动可保持 0 分。",
          required: false,
          default: 0,
          minLabel: "0",
          maxLabel: "120"
        }
      ];
      const diseaseQs = [];
      if (disease === "HTN") {
        diseaseQs.push(
          {
            field: "dizzy_score",
            type: "score",
            min: 0,
            max: 10,
            step: 1,
            title: "头晕/头痛程度（0–10 分，选填）",
            desc: "如无此症状可保持 0 分。",
            required: false,
            default: 0,
            minLabel: "无",
            maxLabel: "很重"
          },
          {
            field: "bp_measure",
            title: "今天是否测量血压？",
            options: [
              { value: 0, label: "未测" },
              { value: 1, label: "测过一次" },
              { value: 2, label: "早晚各一次" },
              { value: 3, label: "多次/不确定" }
            ]
          },
          {
            field: "salt_intake",
            title: "今天饮食咸淡情况？",
            options: [
              { value: 0, label: "偏淡/低盐" },
              { value: 1, label: "正常" },
              { value: 2, label: "偏咸" },
              { value: 3, label: "很咸/重口" }
            ]
          }
        );
      } else if (disease === "DM") {
        diseaseQs.push(
          {
            field: "glucose_measure",
            title: "今天是否测量血糖？",
            options: [
              { value: 0, label: "未测" },
              { value: 1, label: "测过一次" },
              { value: 2, label: "餐前/餐后都测" },
              { value: 3, label: "多次/不确定" }
            ]
          },
          {
            field: "hypo_sign",
            title: "是否出现疑似低血糖表现？",
            desc: "如出汗、心慌、手抖、头晕等。",
            options: [
              { value: 0, label: "没有" },
              { value: 1, label: "轻微，休息可缓解" },
              { value: 2, label: "明显，需要进食缓解" },
              { value: 3, label: "严重，考虑就医" }
            ]
          },
          {
            field: "sugar_intake_star",
            type: "star",
            max: 5,
            title: "今天控糖情况（1–5 星）",
            desc: "少甜饮/少精制碳水/规律进餐。",
            labels: ["未控制", "控制很好"]
          }
        );
      } else if (disease === "HF") {
        diseaseQs.push(
          {
            field: "orthopnea",
            title: "今晚睡觉是否需要垫高枕头/半卧？",
            options: [
              { value: 0, label: "不需要" },
              { value: 1, label: "需要垫高 1 个枕头" },
              { value: 2, label: "需要半卧/多枕" },
              { value: 3, label: "坐起才能缓解" }
            ]
          },
          {
            field: "nocturia",
            title: "夜间起夜（小便）次数？",
            options: [
              { value: 0, label: "0–1 次" },
              { value: 1, label: "2 次" },
              { value: 2, label: "3 次" },
              { value: 3, label: "4 次以上" }
            ]
          },
          {
            field: "chest_pain_score",
            type: "score",
            min: 0,
            max: 10,
            step: 1,
            title: "胸痛/压榨感程度（0–10 分，选填）",
            desc: "如无此症状可保持 0 分。",
            required: false,
            default: 0,
            minLabel: "无",
            maxLabel: "很重"
          }
        );
      } else if (disease === "COPD") {
        diseaseQs.push(
          {
            field: "cough_score",
            type: "score",
            min: 0,
            max: 10,
            step: 1,
            title: "咳嗽频率/程度（0–10 分）",
            desc: "0 分无咳嗽，10 分频繁剧烈。",
            minLabel: "无",
            maxLabel: "很重"
          },
          {
            field: "sputum",
            title: "痰的情况？",
            options: [
              { value: 0, label: "无/很少" },
              { value: 1, label: "白色/清稀" },
              { value: 2, label: "黄绿/增多" },
              { value: 3, label: "带血/明显增多" }
            ]
          },
          {
            field: "wheeze",
            title: "是否出现喘鸣/哮喘样发作？",
            options: [
              { value: 0, label: "没有" },
              { value: 1, label: "轻微，活动时有" },
              { value: 2, label: "明显，影响活动" },
              { value: 3, label: "休息时也有/考虑就医" }
            ]
          },
          {
            field: "inhaler_star",
            type: "star",
            max: 5,
            title: "今天吸入剂/雾化是否规范（1–5 星）",
            labels: ["不规范", "很规范"]
          }
        );
      } else {
        diseaseQs.push({
          field: "other_note",
          type: "text",
          title: "今天是否有其他特别不适？（选填）",
          placeholder: "例如：头晕、胸闷、咳嗽加重、血压/血糖波动、夜间憋醒等…",
          required: false,
          maxlength: 200
        });
      }
      const lastStep = {
        step: 4,
        questions: [
          ...commonMgmt,
          ...diseaseQs,
          {
            field: "extra_note",
            type: "text",
            title: `补充说明（选填）`,
            desc: `如今天有就医、换药、特殊事件等可简单记录。`,
            placeholder: `可不填`,
            required: false,
            maxlength: 200
          }
        ]
      };
      const steps = [...base, lastStep];
      steps[3].title = `慢病管理（${diseaseLabelMap[disease] || "慢性病"}）`;
      return steps;
    },
    initSteps() {
      const lastDisease = utils_session.getScopedStorageSync("survey_chronic_disease", "HTN") || "HTN";
      const steps = this.buildSteps(lastDisease);
      const answers = { ...this.answers || {} };
      if (!answers.chronic_disease)
        answers.chronic_disease = lastDisease;
      if (answers.overall_star === void 0)
        answers.overall_star = 3;
      if (answers.sleep_star === void 0)
        answers.sleep_star = 3;
      if (answers.mood_star === void 0)
        answers.mood_star = 3;
      if (answers.appetite_star === void 0)
        answers.appetite_star = 3;
      if (answers.med_adherence_star === void 0)
        answers.med_adherence_star = 4;
      if (answers.diet_adherence_star === void 0)
        answers.diet_adherence_star = 4;
      if (answers.fatigue_score === void 0)
        answers.fatigue_score = 0;
      if (answers.dyspnea_score === void 0)
        answers.dyspnea_score = 0;
      this.setData({
        steps,
        answers
      });
    },
    // dataset value 兼容 number/string
    normalizeDatasetValue(raw) {
      if (typeof raw === "number")
        return raw;
      if (raw === null || raw === void 0)
        return raw;
      const s = String(raw);
      if (/^-?\d+(\.\d+)?$/.test(s))
        return Number(s);
      return raw;
    },
    onSelectOption(e) {
      const field = e.currentTarget.dataset.field;
      const raw = e.currentTarget.dataset.value;
      const value = this.normalizeDatasetValue(raw);
      const nextAnswers = {
        ...this.answers,
        [field]: value
      };
      if (field === "chronic_disease") {
        utils_session.setScopedStorageSync("survey_chronic_disease", value);
        const steps = this.buildSteps(value);
        this.setData({
          answers: nextAnswers,
          steps
        });
        return;
      }
      this.setData({
        answers: nextAnswers
      });
    },
    onSetStar(e) {
      const field = e.currentTarget.dataset.field;
      const raw = e.currentTarget.dataset.value;
      const value = this.normalizeDatasetValue(raw);
      this.setData({
        answers: {
          ...this.answers,
          [field]: value
        }
      });
    },
    onSliderChange(e) {
      const field = e.currentTarget.dataset.field;
      const value = Number(e.detail.value);
      this.setData({
        answers: {
          ...this.answers,
          [field]: value
        }
      });
    },
    onTextInput(e) {
      const field = e.currentTarget.dataset.field;
      const value = e.detail.value;
      this.setData({
        answers: {
          ...this.answers,
          [field]: value
        }
      });
    },
    // 校验当前步是否全部作答（仅校验 required !== false 的题）
    validateCurrentStep() {
      const stepObj = this.steps[this.currentStep];
      if (!stepObj)
        return true;
      const missing = (stepObj.questions || []).filter((q) => {
        if (q.required === false)
          return false;
        const v = this.answers[q.field];
        if (q.type === "text")
          return !v || String(v).trim().length === 0;
        return v === void 0;
      });
      if (missing.length > 0) {
        common_vendor.index.showToast({
          title: "请先完成本步所有必填问题",
          icon: "none"
        });
        return false;
      }
      return true;
    },
    onPrev() {
      if (this.currentStep === 0)
        return;
      this.setData({
        currentStep: this.currentStep - 1
      });
    },
    onNext() {
      if (!this.validateCurrentStep())
        return;
      if (this.currentStep < this.steps.length - 1) {
        this.setData({
          currentStep: this.currentStep + 1
        });
      }
    },
    buildRiskSummary(answers) {
      const signals = [];
      const dysp = Number(answers.dyspnea_score || 0);
      const edema = Number(answers.edema || 0);
      const chestT = Number(answers.chest_tightness || 0);
      const sleep = Number(answers.sleep_star || 3);
      const med = Number(answers.med_adherence_star || 3);
      if (dysp >= 7)
        signals.push("气短较重");
      if (edema >= 2)
        signals.push("浮肿明显");
      if (chestT >= 2)
        signals.push("胸闷较多");
      if (sleep <= 2)
        signals.push("睡眠较差");
      if (med <= 2)
        signals.push("可能漏服用药");
      if (answers.chronic_disease === "HF") {
        const orth = Number(answers.orthopnea || 0);
        const chestPain = Number(answers.chest_pain_score || 0);
        if (orth >= 2)
          signals.push("夜间需半卧/坐起");
        if (chestPain >= 7)
          signals.push("胸痛较重");
      }
      if (answers.chronic_disease === "COPD") {
        const wheeze = Number(answers.wheeze || 0);
        const sputum = Number(answers.sputum || 0);
        if (wheeze >= 2)
          signals.push("喘鸣明显");
        if (sputum >= 2)
          signals.push("痰色/量异常");
      }
      if (answers.chronic_disease === "DM") {
        const hypo = Number(answers.hypo_sign || 0);
        if (hypo >= 2)
          signals.push("疑似低血糖");
      }
      let level = "low";
      if (signals.length >= 3 || dysp >= 8 || edema >= 3)
        level = "high";
      else if (signals.length >= 1)
        level = "mid";
      return { level, signals };
    },
    onSubmit() {
      for (let i = 0; i < this.steps.length; i++) {
        const step = this.steps[i];
        const missing = (step.questions || []).filter((q) => {
          if (q.required === false)
            return false;
          const v = this.answers[q.field];
          if (q.type === "text")
            return !v || String(v).trim().length === 0;
          return v === void 0;
        });
        if (missing.length > 0) {
          common_vendor.index.showToast({ title: "还有未完成的必填问题", icon: "none" });
          this.setData({ currentStep: i });
          return;
        }
      }
      const answers = this.answers;
      const risk = this.buildRiskSummary(answers);
      const fatigueScore = Number(answers.fatigue_score || 0);
      const breathScore = Number(answers.dyspnea_score || 0);
      const sleepRisk = 5 - Number(answers.sleep_star || 3);
      const medRisk = 5 - Number(answers.med_adherence_star || 3);
      const payload = {
        date: this.todayDateStr,
        answers,
        features: {
          fatigueScore,
          breathScore,
          sleepRisk,
          medRisk
        },
        risk
      };
      utils_session.setScopedStorageSync("tcm_survey_today", payload);
      utils_session.setScopedStorageSync("tcm_survey_date", this.todayDateStr);
      api_patient.submitTcmSurvey(payload).then(() => {
        this.onSubmitDone(risk);
      }).catch(() => {
        {
          common_vendor.index.showToast({ title: "提交失败", icon: "none" });
        }
      });
    },
    onSubmitDone(risk, localOnly = false) {
      const base = localOnly ? "已提交（本地）" : "已提交";
      if (risk && risk.level === "high" && risk.signals && risk.signals.length) {
        common_vendor.index.showModal({
          title: base,
          content: `今日需重点关注：${risk.signals.slice(0, 4).join("、")}。如不适加重，请及时联系医生或就医。`,
          showCancel: false,
          success: () => {
            common_vendor.index.navigateBack();
          }
        });
      } else {
        common_vendor.index.showToast({ title: base, icon: "success" });
        setTimeout(() => common_vendor.index.navigateBack(), 600);
      }
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: $options.progressPct + "%",
    b: common_vendor.t($data.currentStep + 1),
    c: common_vendor.t($data.steps.length),
    d: $data.steps && $data.steps.length
  }, $data.steps && $data.steps.length ? {
    e: common_vendor.f($data.steps[$data.currentStep].questions, (item, index, i0) => {
      return common_vendor.e({
        a: common_vendor.t(index + 1),
        b: common_vendor.t(item.title),
        c: item.required !== false
      }, item.required !== false ? {} : {}, {
        d: item.desc
      }, item.desc ? {
        e: common_vendor.t(item.desc)
      } : {}, {
        f: $options.isSingle(item)
      }, $options.isSingle(item) ? {
        g: common_vendor.f(item.options, (opt, optIndex, i1) => {
          return {
            a: common_vendor.t(opt.label),
            b: common_vendor.n("q-option " + ($data.answers[item.field] === opt.value ? "active" : "")),
            c: opt.value,
            d: common_vendor.o((...args) => $options.onSelectOption && $options.onSelectOption(...args), optIndex),
            e: optIndex
          };
        }),
        h: item.field
      } : item.type === "star" ? common_vendor.e({
        j: common_vendor.f(item.max || 5, (n, k1, i1) => {
          return {
            a: n,
            b: common_vendor.n("star " + ($options.getNumAnswer(item.field) >= n ? "on" : "")),
            c: n,
            d: common_vendor.o((...args) => $options.onSetStar && $options.onSetStar(...args), n)
          };
        }),
        k: item.field,
        l: common_vendor.t($options.getNumAnswer(item.field) || 0),
        m: common_vendor.t(item.max || 5),
        n: item.labels && item.labels.length
      }, item.labels && item.labels.length ? {
        o: common_vendor.t(item.labels[0]),
        p: common_vendor.t(item.labels[item.labels.length - 1])
      } : {}) : item.type === "score" ? common_vendor.e({
        r: common_vendor.t($options.displayScore(item)),
        s: item.unit
      }, item.unit ? {
        t: common_vendor.t(item.unit)
      } : {}, {
        v: item.min !== void 0 ? item.min : 0,
        w: item.max !== void 0 ? item.max : 10,
        x: item.step !== void 0 ? item.step : 1,
        y: $options.displayScore(item),
        z: item.field,
        A: common_vendor.o((...args) => $options.onSliderChange && $options.onSliderChange(...args), item.field),
        B: common_vendor.t($options.minLabel(item)),
        C: common_vendor.t($options.maxLabel(item))
      }) : item.type === "text" ? {
        E: item.placeholder || "请输入…",
        F: $data.answers[item.field] || "",
        G: item.maxlength || 200,
        H: item.field,
        I: common_vendor.o((...args) => $options.onTextInput && $options.onTextInput(...args), item.field),
        J: common_vendor.t(($data.answers[item.field] || "").length),
        K: common_vendor.t(item.maxlength || 200)
      } : {}, {
        i: item.type === "star",
        q: item.type === "score",
        D: item.type === "text",
        L: item.field
      });
    })
  } : {}, {
    f: common_vendor.o((...args) => $options.onPrev && $options.onPrev(...args)),
    g: $data.currentStep === 0,
    h: $data.currentStep < $data.steps.length - 1
  }, $data.currentStep < $data.steps.length - 1 ? {
    i: common_vendor.o((...args) => $options.onNext && $options.onNext(...args))
  } : $data.currentStep === $data.steps.length - 1 ? {
    k: common_vendor.o((...args) => $options.onSubmit && $options.onSubmit(...args))
  } : {}, {
    j: $data.currentStep === $data.steps.length - 1
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../../.sourcemap/mp-weixin/pages/patient/survey/survey.js.map
