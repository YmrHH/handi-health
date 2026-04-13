"use strict";
const common_vendor = require("../../../common/vendor.js");
const api_staff = require("../../../api/staff.js");
const _sfc_main = {
  data() {
    return {
      todoList: [],
      overdueList: [],
      doneList: []
    };
  },
  onLoad() {
    this.loadAlerts();
  },
  methods: {
    loadAlerts() {
      api_staff.getAlerts().then((res) => {
        const list = Array.isArray(res) ? res : res && res.alerts || [];
        const normalized = list.map((it) => ({
          id: it.id,
          name: it.patientName || it.name,
          desc: it.aiSummary || it.desc || "",
          planTime: it.planTime || it.planText || "",
          riskLevel: it.riskLevel || "mid",
          riskText: it.riskText,
          status: it.status || "todo",
          overdue: !!it.overdue
        }));
        const todoList = normalized.filter((a) => a.status === "todo");
        const doneList = normalized.filter((a) => a.status === "done");
        const overdueList = normalized.filter((a) => a.overdue || a.status === "overdue");
        this.setData({ todoList, overdueList, doneList });
      }).catch(() => {
        this.setData({ todoList: [], overdueList: [], doneList: [] });
      });
    },
    // 联调阶段兜底 demo
    loadDemoData() {
      this.setData({
        todoList: [
          {
            id: "a1",
            name: "张阿姨",
            desc: "近3天血压偏高，体温偏高 0.8℃，夜间憋醒次数增多。",
            planTime: "今天 09:30",
            riskLevel: "high",
            riskText: "高风险"
          }
        ],
        overdueList: [
          {
            id: "a2",
            name: "李叔叔",
            desc: "上次随访后未按时复查血脂，需要重新安排随访时间。",
            planTime: "2025-11-25",
            overdueDays: 5
          }
        ],
        doneList: [
          {
            id: "a3",
            name: "王大爷",
            doneTime: "2025-11-30",
            summary: "电话随访，病情稳定，按时服药、血压控制良好。"
          }
        ]
      });
    },
    // 待随访：开始随访
    onStartFollowup(e) {
      const id = e.currentTarget.dataset.id;
      common_vendor.index.navigateTo({
        url: `/pages/staff/followup/followup?alertId=${id}&mode=new`
      });
    },
    // 已超期：重新安排
    onReschedule(e) {
      const id = e.currentTarget.dataset.id;
      common_vendor.index.navigateTo({
        url: `/pages/staff/followup/followup?alertId=${id}&mode=reschedule`
      });
    },
    // 已完成：查看 / 修改
    onViewDetail(e) {
      const id = e.currentTarget.dataset.id;
      common_vendor.index.navigateTo({
        url: `/pages/staff/followup/followup?alertId=${id}&mode=edit`
      });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return {
    a: common_vendor.f($data.todoList, (item, index, i0) => {
      return {
        a: common_vendor.t(item.name),
        b: common_vendor.t(item.desc),
        c: common_vendor.t(item.riskText),
        d: common_vendor.n("risk-tag " + item.riskLevel),
        e: common_vendor.t(item.planTime),
        f: item.id,
        g: common_vendor.o((...args) => $options.onStartFollowup && $options.onStartFollowup(...args), index),
        h: index
      };
    }),
    b: common_vendor.f($data.overdueList, (item, index, i0) => {
      return {
        a: common_vendor.t(item.name),
        b: common_vendor.t(item.desc),
        c: common_vendor.t(item.overdueDays),
        d: common_vendor.t(item.planTime),
        e: item.id,
        f: common_vendor.o((...args) => $options.onReschedule && $options.onReschedule(...args), index),
        g: index
      };
    }),
    c: common_vendor.f($data.doneList, (item, index, i0) => {
      return {
        a: common_vendor.t(item.name),
        b: common_vendor.t(item.doneTime),
        c: common_vendor.t(item.summary),
        d: item.id,
        e: common_vendor.o((...args) => $options.onViewDetail && $options.onViewDetail(...args), index),
        f: index
      };
    })
  };
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../../.sourcemap/mp-weixin/pages/staff/alerts/alerts.js.map
