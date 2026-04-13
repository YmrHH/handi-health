"use strict";
const common_vendor = require("../../../common/vendor.js");
const StaffTabbar = () => "../../../components/staff-tabbar/staff-tabbar.js";
const _sfc_main = {
  components: { StaffTabbar },
  data() {
    return {
      patientName: "张阿姨",
      todayText: "",
      riskLevel: "mid",
      // high | mid | low
      riskText: "中风险",
      // 今日待办状态（实际应由后端返回）
      todo: {
        bp: {
          done: false
        },
        weight: {
          done: false
        },
        symptom: {
          done: false
        },
        rehab: {
          done: false
        }
      },
      todoDoneCount: 0,
      // 下次化验信息
      nextLab: {
        date: "2025-12-20",
        daysLeft: 0,
        overdue: false
      },
      nextLabText: ""
    };
  },
  onLoad() {
    this.initToday();
    this.updateTodoCount();
    this.updateNextLab();
  },
  methods: {
    initToday() {
      const date = /* @__PURE__ */ new Date();
      const y = date.getFullYear();
      const m = date.getMonth() + 1;
      const d = date.getDate();
      const weekdayList = ["日", "一", "二", "三", "四", "五", "六"];
      const w = weekdayList[date.getDay()];
      this.setData({
        todayText: `${y}年${m}月${d}日 周${w}`
      });
    },
    updateTodoCount() {
      const { todo } = this;
      let count = 0;
      Object.keys(todo).forEach((key) => {
        if (todo[key].done) {
          count++;
        }
      });
      this.setData({
        todoDoneCount: count
      });
    },
    // 计算距离下次化验还有几天 / 是否超期
    updateNextLab() {
      const labDateStr = this.nextLab.date;
      if (!labDateStr) {
        this.setData({
          nextLabText: "暂无计划，可在门诊时由医生为您安排化验时间。"
        });
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
      this.setData({
        nextLab: {
          ...this.nextLab,
          daysLeft: diffDays < 0 ? 0 : diffDays,
          overdue
        },
        nextLabText: text
      });
    },
    // ===== 导航 ===== //
    onGoMonitor() {
      common_vendor.index.navigateTo({
        url: "/pages/patient/monitor/monitor"
      });
    },
    onGoMonitorLab() {
      common_vendor.index.navigateTo({
        url: "/pages/patient/monitor/monitor?tab=lab"
      });
    },
    onGoSymptom() {
      common_vendor.index.navigateTo({
        url: "/pages/patient/monitor/monitor?tab=daily&focus=symptom"
      });
    },
    onGoRehab() {
      common_vendor.index.navigateTo({
        url: "/pages/patient/rehab/rehab"
      });
    }
  }
};
if (!Array) {
  const _easycom_staff_tabbar2 = common_vendor.resolveComponent("staff-tabbar");
  _easycom_staff_tabbar2();
}
const _easycom_staff_tabbar = () => "../../../components/staff-tabbar/staff-tabbar.js";
if (!Math) {
  _easycom_staff_tabbar();
}
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return {
    a: common_vendor.t($data.patientName || "患者朋友"),
    b: common_vendor.t($data.todayText),
    c: common_vendor.t($data.riskText),
    d: common_vendor.n("risk-pill " + $data.riskLevel),
    e: common_vendor.t($data.todoDoneCount),
    f: common_vendor.t($data.todo.bp.done ? "已完成" : "去测量"),
    g: common_vendor.n("todo-status " + ($data.todo.bp.done ? "done" : "todo")),
    h: common_vendor.o((...args) => $options.onGoMonitor && $options.onGoMonitor(...args)),
    i: common_vendor.t($data.todo.weight.done ? "已完成" : "去记录"),
    j: common_vendor.n("todo-status " + ($data.todo.weight.done ? "done" : "todo")),
    k: common_vendor.o((...args) => $options.onGoMonitor && $options.onGoMonitor(...args)),
    l: common_vendor.t($data.todo.symptom.done ? "已完成" : "去打卡"),
    m: common_vendor.n("todo-status " + ($data.todo.symptom.done ? "done" : "todo")),
    n: common_vendor.o((...args) => $options.onGoSymptom && $options.onGoSymptom(...args)),
    o: common_vendor.t($data.todo.rehab.done ? "已完成" : "去完成"),
    p: common_vendor.n("todo-status " + ($data.todo.rehab.done ? "done" : "todo")),
    q: common_vendor.o((...args) => $options.onGoRehab && $options.onGoRehab(...args)),
    r: common_vendor.t($data.nextLabText),
    s: common_vendor.t($data.nextLab.overdue ? "已超期" : "剩余 " + $data.nextLab.daysLeft + " 天"),
    t: common_vendor.n("lab-badge " + ($data.nextLab.overdue ? "overdue" : "normal")),
    v: common_vendor.o((...args) => $options.onGoMonitor && $options.onGoMonitor(...args)),
    w: common_vendor.o((...args) => $options.onGoMonitor && $options.onGoMonitor(...args)),
    x: common_vendor.o((...args) => $options.onGoMonitorLab && $options.onGoMonitorLab(...args)),
    y: common_vendor.p({
      active: "tasks"
    })
  };
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../../.sourcemap/mp-weixin/pages/staff/home/home.js.map
