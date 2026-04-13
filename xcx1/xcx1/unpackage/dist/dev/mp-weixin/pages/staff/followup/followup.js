"use strict";
const common_vendor = require("../../../common/vendor.js");
const api_common = require("../../../api/common.js");
const api_staff = require("../../../api/staff.js");
const _sfc_main = {
  data() {
    return {
      alertId: "",
      taskId: "",
      today: "",
      nextVisitDate: "",
      mode: "new",
      patient: {
        id: "",
        name: "",
        age: "",
        gender: ""
      },
      task: {
        id: "",
        typeText: "",
        planTime: "",
        aiSummary: "",
        riskLevel: "mid",
        riskText: "中风险"
      },
      contactModes: ["电话", "视频", "上门面谈"],
      contactModeIndex: 0,
      contactResults: ["已联系到本人", "联系到家属", "无人接听", "停机/错号"],
      contactResultIndex: 0,
      adherenceOptions: ["规律服药", "偶尔漏服", "经常漏服/自行停药"],
      adherenceIndex: 0,
      riskOptions: ["低风险", "中风险", "高风险"],
      riskIndex: 1,
      planOptions: ["常规随访", "1周内电话随访", "安排门诊复诊", "需急诊或住院评估"],
      planIndex: 0,
      form: {
        symptomChange: "",
        sbp: "",
        dbp: "",
        hr: "",
        weight: "",
        medicineName: "",
        adverse: "",
        summary: ""
      },
      // 化验
      labs: {
        fpg: "",
        ldl: "",
        tg: "",
        hcy: "",
        ua: "",
        comment: ""
      },
      labStatusOptions: ["未评估", "正常", "偏高", "偏低"],
      labStatusIndex: {
        fpg: 0,
        ldl: 0,
        tg: 0,
        hcy: 0,
        ua: 0
      },
      // 中医体征
      tcm: {
        tongueImage: "",
        tongueFileId: "",
        tongueUrl: "",
        face: 0,
        tongueBody: 0,
        tongueCoat: 0,
        pulseDesc: "",
        summary: 1,
        comment: ""
      },
      faceOptions: ["未评估", "正常", "面色白", "萎黄", "潮红", "暗滞"],
      faceIndex: 0,
      tongueBodyOptions: ["未评估", "正常", "淡胖", "瘦薄", "紫暗"],
      tongueBodyIndex: 0,
      tongueCoatOptions: ["未评估", "薄白", "厚腻", "黄苔", "少苔/无苔"],
      tongueCoatIndex: 0,
      tcmSummaryOptions: ["未评估", "整体平稳", "偏气虚/阳虚", "痰湿偏重", "血瘀明显", "其他"],
      tcmSummaryIndex: 0
    };
  },
  computed: {
    safePatientName() {
      const raw = this.patient && this.patient.name != null ? String(this.patient.name) : "";
      const n = raw.trim();
      if (!n)
        return "未知患者";
      const cleaned = n.replace(/null|undefined/gi, "").replace(/[#\s]+$/g, "").trim();
      if (!cleaned)
        return "未知患者";
      if (cleaned === "患者" || cleaned === "患者#" || cleaned.toLowerCase() === "patient" || cleaned.toLowerCase() === "patient#")
        return "未知患者";
      return cleaned;
    }
  },
  onLoad(options) {
    const { alertId, taskId, mode } = options || {};
    const today = this.formatDate(/* @__PURE__ */ new Date());
    const pageMode = mode || "new";
    const presetPatient = {
      id: options.patientId || options.pid || "",
      name: (() => {
        const n = options.patientName || options.name || "";
        return n && n !== "null" && n !== "undefined" ? n : "";
      })(),
      age: options.age || "",
      gender: options.gender || ""
    };
    const presetTask = {
      typeText: options.typeText || "",
      planTime: options.planTime || "",
      aiSummary: options.aiSummary || "",
      riskLevel: options.riskLevel || "",
      riskText: options.riskText || ""
    };
    this.setData({
      alertId: alertId || "",
      taskId: taskId || "",
      mode: pageMode,
      today,
      patient: { ...this.patient, ...presetPatient },
      task: { ...this.task, ...presetTask }
    });
    let title = "填写随访记录";
    if (pageMode === "reschedule") {
      title = "重新安排随访";
    } else if (pageMode === "edit") {
      title = "随访记录详情";
    }
    common_vendor.index.setNavigationBarTitle({
      title
    });
    this.loadDetail();
  },
  methods: {
    formatDate(d) {
      const y = d.getFullYear();
      const m = ("0" + (d.getMonth() + 1)).slice(-2);
      const day = ("0" + d.getDate()).slice(-2);
      return `${y}-${m}-${day}`;
    },
    // 任务/告警详情：优先后端
    loadDetail() {
      const { taskId, alertId } = this;
      let p;
      if (taskId) {
        p = api_staff.getTaskDetail(taskId);
      } else if (alertId) {
        p = api_staff.getAlertDetail(alertId);
      } else {
        p = Promise.reject(new Error("缺少 taskId/alertId"));
      }
      p.then((res) => {
        const data = res || {};
        const patient = data.patient || {};
        const task = data.task || {};
        if (patient && (patient.name === "null" || patient.name === "undefined"))
          patient.name = "";
        this.setData({
          patient: { ...this.patient, ...patient },
          task: { ...this.task, ...task },
          // 允许后端回传关联 id
          taskId: this.taskId || task.id || data.taskId || "",
          alertId: this.alertId || data.alertId || ""
        });
        const record = data.record || data.followupRecord;
        if (record && typeof record === "object") {
          this.hydrateFromRecord(record);
        }
        const tid = this.taskId || task.id || data.taskId || "";
        if ((!record || typeof record !== "object") && tid) {
          api_staff.getFollowupByTaskId(tid).then((rr) => {
            const rdata = rr && rr.data !== void 0 ? rr.data : rr;
            if (rdata && typeof rdata === "object") {
              this.hydrateFromRecord(rdata);
            }
          }).catch(() => {
          });
        }
      }).catch(() => {
        common_vendor.index.showToast({ title: "加载任务详情失败", icon: "none" });
      });
    },
    hydrateFromRecord(record) {
      try {
        if (record.form) {
          this.form = { ...this.form, ...record.form };
        }
        if (record.labs) {
          this.labs = { ...this.labs, ...record.labs };
        }
        if (record.nextVisitDate) {
          this.nextVisitDate = record.nextVisitDate;
        }
        if (record.contactMode) {
          const idx = this.contactModes.indexOf(record.contactMode);
          if (idx >= 0)
            this.contactModeIndex = idx;
        }
        if (record.contactResult) {
          const idx = this.contactResults.indexOf(record.contactResult);
          if (idx >= 0)
            this.contactResultIndex = idx;
        }
        if (record.adherence) {
          const idx = this.adherenceOptions.indexOf(record.adherence);
          if (idx >= 0)
            this.adherenceIndex = idx;
        }
        if (record.riskLevelText) {
          const idx = this.riskOptions.indexOf(record.riskLevelText);
          if (idx >= 0)
            this.riskIndex = idx;
        }
        if (record.nextPlan) {
          const idx = this.planOptions.indexOf(record.nextPlan);
          if (idx >= 0)
            this.planIndex = idx;
        }
        if (record.tcm) {
          const t = record.tcm;
          this.tcm = { ...this.tcm, ...t };
          if (t.tongueUrl && !t.tongueImage)
            this.tcm.tongueImage = t.tongueUrl;
        }
        const tcmEvaluationAdvice = record.tcmEvaluationAdvice || [record.tcmRemark, record.tcmConclusion].filter((item, index, arr) => item && arr.indexOf(item) === index).join("；");
        if (tcmEvaluationAdvice) {
          this.tcm = {
            ...this.tcm,
            comment: tcmEvaluationAdvice
          };
        }
        const healthAdvice = record.healthAdvice || record.labSummary;
        if (healthAdvice) {
          this.labs = {
            ...this.labs,
            comment: healthAdvice
          };
        }
      } catch (e) {
      }
    },
    // ===== 普通表单输入 ===== //
    onInput(e) {
      const field = e.currentTarget.dataset.field;
      const value = e.detail.value;
      this.setData({
        form: {
          ...this.form,
          [field]: value
        }
      });
    },
    onLabInput(e) {
      const field = e.currentTarget.dataset.field;
      const value = e.detail.value;
      this.setData({
        labs: {
          ...this.labs,
          [field]: value
        }
      });
    },
    onContactModeChange(e) {
      this.setData({
        contactModeIndex: Number(e.detail.value)
      });
    },
    onContactResultChange(e) {
      this.setData({
        contactResultIndex: Number(e.detail.value)
      });
    },
    onAdherenceChange(e) {
      this.setData({
        adherenceIndex: Number(e.detail.value)
      });
    },
    onRiskChange(e) {
      this.setData({
        riskIndex: Number(e.detail.value)
      });
    },
    onPlanChange(e) {
      this.setData({
        planIndex: Number(e.detail.value)
      });
    },
    onNextDateChange(e) {
      this.setData({
        nextVisitDate: e.detail.value
      });
    },
    onLabStatusChange(e) {
      const key = e.currentTarget.dataset.key;
      const index = Number(e.detail.value);
      this.setData({
        labStatusIndex: {
          ...this.labStatusIndex,
          [key]: index
        }
      });
    },
    // ===== 中医体征相关 ===== //
    onChooseTongue() {
      common_vendor.index.chooseImage({
        count: 1,
        sizeType: ["compressed"],
        success: (res) => {
          const path = res.tempFilePaths[0];
          this.setData({
            tcm: {
              ...this.tcm,
              tongueImage: path,
              tongueFileId: "",
              tongueUrl: ""
            }
          });
          common_vendor.index.showLoading({ title: "上传中…", mask: true });
          api_common.upload(path, {
            bizType: "tongue",
            patientId: this.patient.id || "",
            taskId: this.taskId || this.task.id || "",
            alertId: this.alertId || ""
          }).then((data) => {
            const fileId = data && (data.fileId || data.id) || "";
            const url = data && data.url || "";
            this.setData({
              tcm: {
                ...this.tcm,
                tongueFileId: fileId,
                tongueUrl: url,
                // 若返回 url，则用 url 作为展示源，避免重启后丢失
                tongueImage: url || this.tcm.tongueImage
              }
            });
          }).catch(() => {
            {
              common_vendor.index.showToast({ title: "舌象上传失败", icon: "none" });
            }
          }).finally(() => {
            common_vendor.index.hideLoading();
          });
        }
      });
    },
    onPreviewTongue() {
      common_vendor.index.previewImage({
        urls: [this.tcm.tongueImage]
      });
    },
    onFaceChange(e) {
      const index = Number(e.detail.value);
      this.setData({
        faceIndex: index,
        tcm: {
          ...this.tcm,
          face: index
        }
      });
    },
    onTongueBodyChange(e) {
      const index = Number(e.detail.value);
      this.setData({
        tongueBodyIndex: index,
        tcm: {
          ...this.tcm,
          tongueBody: index
        }
      });
    },
    onTongueCoatChange(e) {
      const index = Number(e.detail.value);
      this.setData({
        tongueCoatIndex: index,
        tcm: {
          ...this.tcm,
          tongueCoat: index
        }
      });
    },
    onTcmSummaryChange(e) {
      const index = Number(e.detail.value);
      this.setData({
        tcmSummaryIndex: index,
        tcm: {
          ...this.tcm,
          summary: index
        }
      });
    },
    onTcmCommentInput(e) {
      const value = e.detail.value;
      this.setData({
        tcm: {
          ...this.tcm,
          comment: value
        }
      });
    },
    // ===== 提交随访记录 ===== //
    onSubmit() {
      const payload = {
        alertId: this.alertId || "",
        taskId: this.taskId || this.task.id || "",
        patientId: this.patient.id || "",
        mode: this.mode,
        contactMode: this.contactModes[this.contactModeIndex],
        contactResult: this.contactResults[this.contactResultIndex],
        adherence: this.adherenceOptions[this.adherenceIndex],
        riskLevel: this.riskOptions[this.riskIndex],
        nextPlan: this.planOptions[this.planIndex],
        nextVisitDate: this.nextVisitDate,
        form: this.form,
        labs: {
          ...this.labs,
          status: {
            fpg: this.labStatusOptions[this.labStatusIndex.fpg],
            ldl: this.labStatusOptions[this.labStatusIndex.ldl],
            tg: this.labStatusOptions[this.labStatusIndex.tg],
            hcy: this.labStatusOptions[this.labStatusIndex.hcy],
            ua: this.labStatusOptions[this.labStatusIndex.ua]
          }
        },
        tcm: {
          tongueFileId: this.tcm.tongueFileId,
          tongueUrl: this.tcm.tongueUrl,
          tongueImage: this.tcm.tongueImage,
          faceIndex: this.faceIndex,
          tongueBodyIndex: this.tongueBodyIndex,
          tongueCoatIndex: this.tongueCoatIndex,
          pulseDesc: this.tcm.pulseDesc,
          summaryIndex: this.tcmSummaryIndex,
          comment: this.tcm.comment
        }
      };
      const riskText = payload.riskLevel || "";
      let riskEnum = "MID";
      if (String(riskText).includes("高"))
        riskEnum = "HIGH";
      else if (String(riskText).includes("低"))
        riskEnum = "LOW";
      payload.riskLevelText = riskText;
      payload.riskLevel = riskEnum;
      payload.followupType = payload.contactMode;
      payload.resultStatus = "COMPLETED";
      payload.mainSymptoms = payload.form.symptomChange;
      payload.summary = payload.form.summary;
      payload.healthAdvice = payload.labs && payload.labs.comment ? payload.labs.comment : "";
      payload.labSummary = payload.healthAdvice;
      payload.tcmEvaluationAdvice = payload.tcm && payload.tcm.comment ? payload.tcm.comment : "";
      payload.tcmConclusion = payload.tcmEvaluationAdvice;
      payload.tcmRemark = payload.tcmEvaluationAdvice;
      payload.advice = payload.healthAdvice || payload.tcmEvaluationAdvice || "";
      payload.nextFollowupDate = payload.nextVisitDate;
      payload.nextFollowupPlan = payload.nextPlan;
      if (!payload.form.summary && !payload.form.symptomChange) {
        common_vendor.index.showToast({
          title: "请至少填写症状变化或随访小结",
          icon: "none"
        });
        return;
      }
      common_vendor.index.showLoading({ title: "保存中…", mask: true });
      api_staff.submitFollowup(payload).then(() => {
        common_vendor.index.showToast({ title: "已保存", icon: "success" });
        setTimeout(() => common_vendor.index.navigateBack(), 600);
      }).catch((err) => {
        common_vendor.index.showToast({
          title: err && err.message || "保存失败",
          icon: "none"
        });
      }).finally(() => {
        common_vendor.index.hideLoading();
      });
    },
    // ===== Demo：兜底（无后端时） ===== //
    fetchTaskDetail(alertId) {
      let demo;
      if (alertId === "a1") {
        demo = {
          patient: {
            id: "p1",
            name: "张阿姨",
            age: 68,
            gender: "女"
          },
          task: {
            id: "task1",
            typeText: "电话随访",
            planTime: "今天 09:30",
            aiSummary: "近3天血压偏高，体温偏高，夜间憋醒次数增多。",
            riskLevel: "high",
            riskText: "高风险"
          }
        };
      } else {
        demo = {
          patient: {
            id: "unknown",
            name: "未知患者",
            age: "",
            gender: ""
          },
          task: {
            id: this.taskId || "unknown",
            typeText: "电话随访",
            planTime: "",
            aiSummary: "",
            riskLevel: "mid",
            riskText: "中风险"
          }
        };
      }
      this.setData({
        patient: demo.patient,
        task: demo.task
      });
    },
    fetchTaskDetailByTaskId(taskId) {
      let demo;
      if (taskId === "task1") {
        demo = {
          patient: { id: "p1", name: "张阿姨", age: 68, gender: "女" },
          task: { id: "task1", typeText: "电话随访", planTime: "今天 09:30", aiSummary: "近3天血压波动偏大，夜间憋醒2次。", riskLevel: "high", riskText: "高风险" }
        };
      } else if (taskId === "task2") {
        demo = {
          patient: { id: "p2", name: "李先生", age: 72, gender: "男" },
          task: { id: "task2", typeText: "上门随访", planTime: "今天 14:00", aiSummary: "体温略偏高，步行完成度较低。", riskLevel: "mid", riskText: "中风险" }
        };
      } else {
        demo = {
          patient: { id: "unknown", name: "未知患者", age: "", gender: "" },
          task: { id: taskId || "unknown", typeText: "随访任务", planTime: "", aiSummary: "", riskLevel: "mid", riskText: "中风险" }
        };
      }
      this.setData({ patient: demo.patient, task: demo.task });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_vendor.t($options.safePatientName),
    b: $data.patient.age || $data.patient.gender
  }, $data.patient.age || $data.patient.gender ? {
    c: common_vendor.t($data.patient.age || ""),
    d: common_vendor.t($data.patient.gender || "")
  } : {}, {
    e: common_vendor.t($data.task.typeText),
    f: common_vendor.t($data.task.planTime),
    g: common_vendor.t($data.task.aiSummary),
    h: common_vendor.t($data.task.riskText),
    i: common_vendor.n("risk-tag " + $data.task.riskLevel),
    j: common_vendor.t($data.contactModes[$data.contactModeIndex]),
    k: $data.contactModes,
    l: $data.contactModeIndex,
    m: common_vendor.o((...args) => $options.onContactModeChange && $options.onContactModeChange(...args)),
    n: common_vendor.t($data.contactResults[$data.contactResultIndex]),
    o: $data.contactResults,
    p: $data.contactResultIndex,
    q: common_vendor.o((...args) => $options.onContactResultChange && $options.onContactResultChange(...args)),
    r: $data.form.symptomChange,
    s: common_vendor.o((...args) => $options.onInput && $options.onInput(...args)),
    t: $data.form.sbp,
    v: common_vendor.o((...args) => $options.onInput && $options.onInput(...args)),
    w: $data.form.dbp,
    x: common_vendor.o((...args) => $options.onInput && $options.onInput(...args)),
    y: $data.form.hr,
    z: common_vendor.o((...args) => $options.onInput && $options.onInput(...args)),
    A: $data.form.weight,
    B: common_vendor.o((...args) => $options.onInput && $options.onInput(...args)),
    C: $data.form.medicineName,
    D: common_vendor.o(($event) => $data.form.medicineName = $event.detail.value),
    E: common_vendor.t($data.adherenceOptions[$data.adherenceIndex]),
    F: $data.adherenceOptions,
    G: $data.adherenceIndex,
    H: common_vendor.o((...args) => $options.onAdherenceChange && $options.onAdherenceChange(...args)),
    I: $data.form.adverse,
    J: common_vendor.o((...args) => $options.onInput && $options.onInput(...args)),
    K: $data.tcm.tongueImage
  }, $data.tcm.tongueImage ? {
    L: $data.tcm.tongueImage,
    M: common_vendor.o((...args) => $options.onPreviewTongue && $options.onPreviewTongue(...args))
  } : {
    N: common_vendor.o((...args) => $options.onChooseTongue && $options.onChooseTongue(...args))
  }, {
    O: common_vendor.t($data.faceOptions[$data.faceIndex]),
    P: $data.faceOptions,
    Q: $data.faceIndex,
    R: common_vendor.o((...args) => $options.onFaceChange && $options.onFaceChange(...args)),
    S: common_vendor.t($data.tongueBodyOptions[$data.tongueBodyIndex]),
    T: $data.tongueBodyOptions,
    U: $data.tongueBodyIndex,
    V: common_vendor.o((...args) => $options.onTongueBodyChange && $options.onTongueBodyChange(...args)),
    W: common_vendor.t($data.tongueCoatOptions[$data.tongueCoatIndex]),
    X: $data.tongueCoatOptions,
    Y: $data.tongueCoatIndex,
    Z: common_vendor.o((...args) => $options.onTongueCoatChange && $options.onTongueCoatChange(...args)),
    aa: $data.tcm.pulseDesc,
    ab: common_vendor.o(($event) => $data.tcm.pulseDesc = $event.detail.value),
    ac: $data.tcm.comment,
    ad: common_vendor.o((...args) => $options.onTcmCommentInput && $options.onTcmCommentInput(...args)),
    ae: $data.labs.fpg,
    af: common_vendor.o((...args) => $options.onLabInput && $options.onLabInput(...args)),
    ag: common_vendor.t($data.labStatusOptions[$data.labStatusIndex.fpg]),
    ah: $data.labStatusOptions,
    ai: $data.labStatusIndex.fpg,
    aj: common_vendor.o((...args) => $options.onLabStatusChange && $options.onLabStatusChange(...args)),
    ak: $data.labs.ldl,
    al: common_vendor.o((...args) => $options.onLabInput && $options.onLabInput(...args)),
    am: common_vendor.t($data.labStatusOptions[$data.labStatusIndex.ldl]),
    an: $data.labStatusOptions,
    ao: $data.labStatusIndex.ldl,
    ap: common_vendor.o((...args) => $options.onLabStatusChange && $options.onLabStatusChange(...args)),
    aq: $data.labs.tg,
    ar: common_vendor.o((...args) => $options.onLabInput && $options.onLabInput(...args)),
    as: common_vendor.t($data.labStatusOptions[$data.labStatusIndex.tg]),
    at: $data.labStatusOptions,
    av: $data.labStatusIndex.tg,
    aw: common_vendor.o((...args) => $options.onLabStatusChange && $options.onLabStatusChange(...args)),
    ax: $data.labs.hcy,
    ay: common_vendor.o((...args) => $options.onLabInput && $options.onLabInput(...args)),
    az: common_vendor.t($data.labStatusOptions[$data.labStatusIndex.hcy]),
    aA: $data.labStatusOptions,
    aB: $data.labStatusIndex.hcy,
    aC: common_vendor.o((...args) => $options.onLabStatusChange && $options.onLabStatusChange(...args)),
    aD: $data.labs.ua,
    aE: common_vendor.o((...args) => $options.onLabInput && $options.onLabInput(...args)),
    aF: common_vendor.t($data.labStatusOptions[$data.labStatusIndex.ua]),
    aG: $data.labStatusOptions,
    aH: $data.labStatusIndex.ua,
    aI: common_vendor.o((...args) => $options.onLabStatusChange && $options.onLabStatusChange(...args)),
    aJ: $data.labs.comment,
    aK: common_vendor.o((...args) => $options.onLabInput && $options.onLabInput(...args)),
    aL: $data.form.summary,
    aM: common_vendor.o((...args) => $options.onInput && $options.onInput(...args)),
    aN: common_vendor.t($data.riskOptions[$data.riskIndex]),
    aO: $data.riskOptions,
    aP: $data.riskIndex,
    aQ: common_vendor.o((...args) => $options.onRiskChange && $options.onRiskChange(...args)),
    aR: common_vendor.t($data.planOptions[$data.planIndex]),
    aS: $data.planOptions,
    aT: $data.planIndex,
    aU: common_vendor.o((...args) => $options.onPlanChange && $options.onPlanChange(...args)),
    aV: common_vendor.t($data.nextVisitDate || "请选择日期"),
    aW: $data.nextVisitDate,
    aX: $data.today,
    aY: common_vendor.o((...args) => $options.onNextDateChange && $options.onNextDateChange(...args)),
    aZ: common_vendor.o((...args) => $options.onSubmit && $options.onSubmit(...args))
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../../.sourcemap/mp-weixin/pages/staff/followup/followup.js.map
