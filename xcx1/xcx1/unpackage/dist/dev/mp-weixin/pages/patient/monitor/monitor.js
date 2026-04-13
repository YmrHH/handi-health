"use strict";
const common_vendor = require("../../../common/vendor.js");
const utils_metricsStore = require("../../../utils/metrics-store.js");
const api_patient = require("../../../api/patient.js");
const _sfc_main = {
  data() {
    return {
      activeTab: "daily",
      // daily | lab
      // 控制基础信息弹窗显示
      showBasicInfoPopup: false,
      // 基础信息 - 不需要每天填写
      basicInfo: {
        heightCm: "",
        baseWeight: "",
        bmi: "",
        bmiClass: "normal",
        lastUpdated: ""
      },
      // 编辑基础信息时使用的表单
      basicInfoForm: {
        heightCm: "",
        baseWeight: ""
      },
      // 日常自测表单 - 每天都要填写的部分 (删除了weight相关字段)
      form: {
        sbp: "",
        dbp: "",
        hr: "",
        bpLastUpdated: "",
        spo2: "",
        spo2LastUpdated: "",
        symptomNote: ""
      },
      sleepOptions: ["睡得很好", "一般", "较差"],
      sleepIndex: 1,
      appetiteOptions: ["食欲好", "一般", "较差"],
      appetiteIndex: 0,
      stoolOptions: ["每日一次、成形", "偏稀", "偏干", "几天一次"],
      stoolIndex: 0,
      // 医院化验结果 demo
      labs: []
    };
  },
  onLoad(options) {
    if (options && options.tab) {
      this.activeTab = options.tab;
    }
    this.loadBasicInfo();
    this.loadDaily();
    this.loadLabs();
  },
  methods: {
    onChangeTab(e) {
      const tab = e.currentTarget.dataset.tab;
      this.activeTab = tab;
    },
    onInput(e) {
      const field = e.currentTarget.dataset.field;
      const value = e.detail.value;
      this.form = {
        ...this.form,
        [field]: value
      };
    },
    onSleepChange(e) {
      this.sleepIndex = Number(e.detail.value);
    },
    onAppetiteChange(e) {
      this.appetiteIndex = Number(e.detail.value);
    },
    onStoolChange(e) {
      this.stoolIndex = Number(e.detail.value);
    },
    // 加载基础信息：优先后端，其次本地缓存
    loadBasicInfo() {
      api_patient.getBasicInfo().then((data) => {
        if (data) {
          this.basicInfo = {
            ...this.basicInfo,
            ...data
          };
          try {
            setScopedStorageSync("patient_basic_info", this.basicInfo);
          } catch (e) {
          }
        }
      }).catch(() => {
        try {
          const cached = getScopedStorageSync("patient_basic_info", null);
          if (cached)
            this.basicInfo = { ...this.basicInfo, ...cached };
        } catch (e) {
        }
      });
    },
    // 显示基础信息编辑弹窗
    showBasicInfoModal() {
      this.basicInfoForm = {
        heightCm: this.basicInfo.heightCm,
        baseWeight: this.basicInfo.baseWeight
      };
      this.showBasicInfoPopup = true;
    },
    // 关闭基础信息编辑弹窗
    closeBasicInfoModal() {
      this.showBasicInfoPopup = false;
    },
    // 保存基础信息
    saveBasicInfo() {
      const height = Number(this.basicInfoForm.heightCm);
      const weight = Number(this.basicInfoForm.baseWeight);
      if (!height || !weight) {
        common_vendor.index.showToast({
          title: "请填写完整信息",
          icon: "none"
        });
        return;
      }
      const h = height / 100;
      const bmi = weight / (h * h);
      const bmiValue = bmi.toFixed(1);
      let bmiClass = "";
      let bmiText = "";
      if (bmi < 18.5) {
        bmiClass = "thin";
        bmiText = "偏瘦";
      } else if (bmi < 24) {
        bmiClass = "normal";
        bmiText = "正常";
      } else if (bmi < 28) {
        bmiClass = "overweight";
        bmiText = "超重";
      } else {
        bmiClass = "obese";
        bmiText = "肥胖";
      }
      const now = /* @__PURE__ */ new Date();
      const dateStr = `${now.getFullYear()}-${(now.getMonth() + 1).toString().padStart(2, "0")}-${now.getDate().toString().padStart(2, "0")}`;
      const updatedInfo = {
        heightCm: this.basicInfoForm.heightCm,
        baseWeight: this.basicInfoForm.baseWeight,
        bmi: bmiValue,
        bmiClass,
        bmiText,
        lastUpdated: dateStr
      };
      this.basicInfo = updatedInfo;
      try {
        setScopedStorageSync("patient_basic_info", updatedInfo);
      } catch (e) {
      }
      api_patient.updateBasicInfo(updatedInfo).catch(() => {
        {
          common_vendor.index.showToast({ title: "基础信息同步失败", icon: "none" });
        }
      });
      this.closeBasicInfoModal();
      common_vendor.index.showToast({
        title: "基础信息已更新",
        icon: "success"
      });
    },
    // 同步血氧（SpO₂）
    // 说明：微信小程序没有通用系统级 SpO₂ 读取接口；
    // 实际项目中一般通过蓝牙设备/厂商 SDK 获取。
    syncSpO2() {
      common_vendor.index.showLoading({
        title: "同步中..."
      });
      setTimeout(() => {
        const randomSpO2 = Math.floor(Math.random() * 7) + 93;
        this.form.spo2 = String(randomSpO2);
        const now = /* @__PURE__ */ new Date();
        this.form.spo2LastUpdated = `${now.getHours().toString().padStart(2, "0")}:${now.getMinutes().toString().padStart(2, "0")}`;
        common_vendor.index.hideLoading();
        common_vendor.index.showToast({
          title: "血氧已同步",
          icon: "success"
        });
      }, 1e3);
    },
    // 保存日常自测
    onSaveDaily() {
      const f = this.form;
      if (!f.sbp || !f.dbp) {
        common_vendor.index.showToast({
          title: "请至少填写血压",
          icon: "none"
        });
        return;
      }
      const now = /* @__PURE__ */ new Date();
      const timeStr = `${now.getHours().toString().padStart(2, "0")}:${now.getMinutes().toString().padStart(2, "0")}`;
      const dateStr = `${now.getFullYear()}-${(now.getMonth() + 1).toString().padStart(2, "0")}-${now.getDate().toString().padStart(2, "0")}`;
      this.form.bpLastUpdated = timeStr;
      if (f.spo2 && !f.spo2LastUpdated) {
        this.form.spo2LastUpdated = timeStr;
      }
      const payload = {
        ...f,
        sleep: this.sleepOptions[this.sleepIndex],
        appetite: this.appetiteOptions[this.appetiteIndex],
        stool: this.stoolOptions[this.stoolIndex],
        recordDate: dateStr
      };
      try {
        setScopedStorageSync("daily_measure_today", payload);
        setScopedStorageSync("daily_measure_date", dateStr);
      } catch (e) {
      }
      const bpText = `${f.sbp}/${f.dbp}`;
      utils_metricsStore.updateDailyMetricsLatest(
        { bp: bpText, hr: f.hr, spo2: f.spo2 },
        "日常自测",
        { silent: true }
      );
      utils_metricsStore.upsertTimeseriesFromPatch(
        { bp: bpText, hr: f.hr, spo2: f.spo2 },
        "日常自测",
        dateStr
      );
      api_patient.saveDailyMeasurement(payload).then(() => {
        common_vendor.index.showToast({ title: "已保存", icon: "success" });
      }).catch(() => {
        common_vendor.index.showToast({ title: "保存失败", icon: "none" });
      });
    },
    // 加载今日测量：优先后端，其次本地
    loadDaily() {
      const today = /* @__PURE__ */ new Date();
      const dateStr = `${today.getFullYear()}-${(today.getMonth() + 1).toString().padStart(2, "0")}-${today.getDate().toString().padStart(2, "0")}`;
      api_patient.getDailyMeasurementToday({ date: dateStr }).then((data) => {
        const row = Array.isArray(data && data.rows) ? data.rows[0] || null : data;
        if (!row)
          return;
        this.fillDailyFormFromData(row);
        try {
          setScopedStorageSync("daily_measure_today", row);
          setScopedStorageSync("daily_measure_date", dateStr);
        } catch (e) {
        }
        const sbp = row && row.sbp != null ? String(row.sbp) : "";
        const dbp = row && row.dbp != null ? String(row.dbp) : "";
        const bpText = sbp || dbp ? `${sbp || "--"}/${dbp || "--"}` : "";
        utils_metricsStore.updateDailyMetricsLatest(
          { bp: bpText, hr: row.hr, spo2: row.spo2 },
          "日常自测",
          { silent: true }
        );
        utils_metricsStore.upsertTimeseriesFromPatch(
          { bp: bpText, hr: row.hr, spo2: row.spo2 },
          "日常自测",
          dateStr
        );
      }).catch(() => {
        try {
          const cached = getScopedStorageSync("daily_measure_today", {}) || {};
          this.fillDailyFormFromData(cached);
        } catch (e) {
        }
      });
    },
    fillDailyFormFromData(data) {
      const d = data || {};
      this.form = {
        sbp: d.sbp || "",
        dbp: d.dbp || "",
        hr: d.hr || "",
        bpLastUpdated: d.bpLastUpdated || "",
        spo2: d.spo2 || "",
        spo2LastUpdated: d.spo2LastUpdated || "",
        symptomNote: d.symptomNote || ""
      };
      this.sleepIndex = this.sleepOptions.indexOf(d.sleep) >= 0 ? this.sleepOptions.indexOf(d.sleep) : 1;
      this.appetiteIndex = this.appetiteOptions.indexOf(d.appetite) >= 0 ? this.appetiteOptions.indexOf(d.appetite) : 0;
      this.stoolIndex = this.stoolOptions.indexOf(d.stool) >= 0 ? this.stoolOptions.indexOf(d.stool) : 0;
    },
    // 加载医院化验结果：优先后端
    loadLabs() {
      api_patient.getLabs().then((list) => {
        this.labs = Array.isArray(list) ? list : list && list.labs || [];
      }).catch(() => {
        {
          this.labs = [];
        }
      });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_vendor.n("tab-item " + ($data.activeTab === "daily" ? "active" : "")),
    b: common_vendor.o((...args) => $options.onChangeTab && $options.onChangeTab(...args)),
    c: common_vendor.n("tab-item " + ($data.activeTab === "lab" ? "active" : "")),
    d: common_vendor.o((...args) => $options.onChangeTab && $options.onChangeTab(...args)),
    e: $data.activeTab === "daily"
  }, $data.activeTab === "daily" ? common_vendor.e({
    f: common_vendor.o((...args) => $options.showBasicInfoModal && $options.showBasicInfoModal(...args)),
    g: common_vendor.t($data.basicInfo.heightCm || "--"),
    h: common_vendor.t($data.basicInfo.baseWeight || "--"),
    i: common_vendor.t($data.basicInfo.bmi || "--"),
    j: common_vendor.n("info-value " + $data.basicInfo.bmiClass),
    k: common_vendor.t($data.basicInfo.lastUpdated || "未设置"),
    l: $data.form.sbp,
    m: common_vendor.o((...args) => $options.onInput && $options.onInput(...args)),
    n: $data.form.dbp,
    o: common_vendor.o((...args) => $options.onInput && $options.onInput(...args)),
    p: $data.form.hr,
    q: common_vendor.o((...args) => $options.onInput && $options.onInput(...args)),
    r: $data.form.bpLastUpdated
  }, $data.form.bpLastUpdated ? {
    s: common_vendor.t($data.form.bpLastUpdated)
  } : {}, {
    t: common_vendor.t($data.sleepOptions[$data.sleepIndex]),
    v: $data.sleepOptions,
    w: $data.sleepIndex,
    x: common_vendor.o((...args) => $options.onSleepChange && $options.onSleepChange(...args)),
    y: common_vendor.t($data.appetiteOptions[$data.appetiteIndex]),
    z: $data.appetiteOptions,
    A: $data.appetiteIndex,
    B: common_vendor.o((...args) => $options.onAppetiteChange && $options.onAppetiteChange(...args)),
    C: common_vendor.t($data.stoolOptions[$data.stoolIndex]),
    D: $data.stoolOptions,
    E: $data.stoolIndex,
    F: common_vendor.o((...args) => $options.onStoolChange && $options.onStoolChange(...args)),
    G: common_vendor.o((...args) => $options.syncSpO2 && $options.syncSpO2(...args)),
    H: $data.form.spo2,
    I: common_vendor.o((...args) => $options.onInput && $options.onInput(...args)),
    J: $data.form.spo2LastUpdated
  }, $data.form.spo2LastUpdated ? {
    K: common_vendor.t($data.form.spo2LastUpdated)
  } : {}, {
    L: $data.form.symptomNote,
    M: common_vendor.o((...args) => $options.onInput && $options.onInput(...args)),
    N: common_vendor.o((...args) => $options.onSaveDaily && $options.onSaveDaily(...args))
  }) : $data.activeTab === "lab" ? common_vendor.e({
    P: common_vendor.f($data.labs, (item, index, i0) => {
      return common_vendor.e({
        a: common_vendor.t(item.date),
        b: common_vendor.t(item.hospital),
        c: common_vendor.f(item.items, (labItem, index1, i1) => {
          return {
            a: common_vendor.t(labItem.name),
            b: common_vendor.t(labItem.value),
            c: common_vendor.t(labItem.unit),
            d: common_vendor.t(labItem.statusText),
            e: common_vendor.n("lab-tag " + labItem.status),
            f: index1
          };
        }),
        d: item.comment
      }, item.comment ? {
        e: common_vendor.t(item.comment)
      } : {}, {
        f: index
      });
    }),
    Q: !$data.labs || $data.labs.length === 0
  }, !$data.labs || $data.labs.length === 0 ? {} : {}) : {}, {
    O: $data.activeTab === "lab",
    R: $data.showBasicInfoPopup
  }, $data.showBasicInfoPopup ? {
    S: common_vendor.o((...args) => $options.closeBasicInfoModal && $options.closeBasicInfoModal(...args)),
    T: $data.basicInfoForm.heightCm,
    U: common_vendor.o(($event) => $data.basicInfoForm.heightCm = $event.detail.value),
    V: $data.basicInfoForm.baseWeight,
    W: common_vendor.o(($event) => $data.basicInfoForm.baseWeight = $event.detail.value),
    X: common_vendor.o((...args) => $options.closeBasicInfoModal && $options.closeBasicInfoModal(...args)),
    Y: common_vendor.o((...args) => $options.saveBasicInfo && $options.saveBasicInfo(...args))
  } : {});
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../../.sourcemap/mp-weixin/pages/patient/monitor/monitor.js.map
