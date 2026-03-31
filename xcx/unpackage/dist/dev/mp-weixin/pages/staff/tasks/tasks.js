"use strict";
const common_vendor = require("../../../common/vendor.js");
const api_staff = require("../../../api/staff.js");
const StaffTabbar = () => "../../../components/staff-tabbar/staff-tabbar.js";
const _sfc_main = {
  components: { StaffTabbar },
  data() {
    return {
      filters: [
        {
          value: "today",
          label: "今日"
        },
        {
          value: "urgent",
          label: "紧急告警"
        },
        {
          value: "overdue",
          label: "已超期"
        },
        {
          value: "done",
          label: "已完成"
        }
      ],
      activeFilter: "today",
      summary: {
        total: 0,
        done: 0,
        urgent: 0
      },
      tasks: [],
      visibleTasks: []
    };
  },
  onLoad() {
    this.fetchTasksFromServer();
  },
  onShow() {
    this.fetchTasksFromServer();
  },
  methods: {
    onChangeFilter(e) {
      const value = e.currentTarget.dataset.value;
      if (value === this.activeFilter) {
        return;
      }
      this.setData(
        {
          activeFilter: value
        },
        () => {
          this.updateVisibleTasks();
        }
      );
    },
    onOpenTask(e) {
      const id = e.currentTarget.dataset.id;
      const item = (this.visibleTasks || []).find((x) => String(x.id) === String(id)) || {};
      const q = [];
      q.push(`taskId=${encodeURIComponent(id)}`);
      if (item.patientId)
        q.push(`patientId=${encodeURIComponent(item.patientId)}`);
      if (item.patientName)
        q.push(`patientName=${encodeURIComponent(item.patientName)}`);
      if (item.age != null && item.age !== "")
        q.push(`age=${encodeURIComponent(item.age)}`);
      if (item.gender)
        q.push(`gender=${encodeURIComponent(item.gender)}`);
      if (item.typeText)
        q.push(`typeText=${encodeURIComponent(item.typeText)}`);
      if (item.planTime)
        q.push(`planTime=${encodeURIComponent(item.planTime)}`);
      if (item.aiSummary)
        q.push(`aiSummary=${encodeURIComponent(item.aiSummary)}`);
      if (item.riskLevel)
        q.push(`riskLevel=${encodeURIComponent(item.riskLevel)}`);
      if (item.riskText)
        q.push(`riskText=${encodeURIComponent(item.riskText)}`);
      common_vendor.index.navigateTo({
        url: `/pages/staff/followup/followup?${q.join("&")}`
      });
    },
    // ===== 数据相关 ===== //
    fetchTasksFromServer() {
      api_staff.getTasks({ filter: this.activeFilter }).then((res) => {
        const list = Array.isArray(res) ? res : res && res.tasks || [];
        const total = list.length;
        const done = list.filter((t) => t.status === "done").length;
        const urgent = list.filter((t) => t.riskLevel === "high" && t.status !== "done").length;
        this.setData(
          {
            tasks: list,
            summary: { total, done, urgent }
          },
          () => this.updateVisibleTasks()
        );
      }).catch(() => {
        {
          this.setData(
            { tasks: [], summary: { total: 0, done: 0, urgent: 0 } },
            () => this.updateVisibleTasks()
          );
          return;
        }
      });
    },
    updateVisibleTasks() {
      const { tasks, activeFilter } = this;
      let list = tasks;
      if (activeFilter === "urgent") {
        list = tasks.filter((t) => t.riskLevel === "high" && t.status !== "done");
      } else if (activeFilter === "overdue") {
        list = tasks.filter((t) => t.overdue);
      } else if (activeFilter === "done") {
        list = tasks.filter((t) => t.status === "done");
      } else {
        list = tasks;
      }
      this.setData({
        visibleTasks: list
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
  return common_vendor.e({
    a: common_vendor.t($data.summary.total),
    b: common_vendor.t($data.summary.done),
    c: common_vendor.t($data.summary.urgent),
    d: common_vendor.f($data.filters, (item, index, i0) => {
      return {
        a: common_vendor.t(item.label),
        b: item.value,
        c: common_vendor.n("filter-tab " + ($data.activeFilter === item.value ? "active" : "")),
        d: common_vendor.o((...args) => $options.onChangeFilter && $options.onChangeFilter(...args), index),
        e: index
      };
    }),
    e: common_vendor.f($data.visibleTasks, (item, index, i0) => {
      return common_vendor.e({
        a: common_vendor.t(item.patientName || item.name || "未知患者"),
        b: item.age || item.gender
      }, item.age || item.gender ? {
        c: common_vendor.t(item.age || ""),
        d: common_vendor.t(item.gender || "")
      } : {}, {
        e: common_vendor.t(item.typeText || item.type || "随访任务"),
        f: common_vendor.t(item.riskText),
        g: common_vendor.n("task-risk " + item.riskLevel),
        h: common_vendor.t(item.planTime),
        i: common_vendor.t(item.aiSummary || item.desc || ""),
        j: item.fromAlert
      }, item.fromAlert ? {} : {}, {
        k: item.overdue
      }, item.overdue ? {} : {}, {
        l: common_vendor.t(item.statusText || (item.status === "done" ? "已完成" : item.overdue ? "已超期" : "待处理")),
        m: common_vendor.o((...args) => $options.onOpenTask && $options.onOpenTask(...args), index),
        n: item.id,
        o: index
      });
    }),
    f: $data.visibleTasks.length === 0
  }, $data.visibleTasks.length === 0 ? {} : {}, {
    g: common_vendor.p({
      active: "tasks"
    })
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render]]);
wx.createPage(MiniProgramPage);
