"use strict";
const common_vendor = require("../../../common/vendor.js");
const api_staff = require("../../../api/staff.js");
const _sfc_main = {
  data() {
    return {
      activeFilter: "all",
      summary: {
        total: 0,
        todo: 0
      },
      alerts: [],
      visibleAlerts: []
    };
  },
  onLoad() {
    this.fetchAlerts();
  },
  onShow() {
    this.fetchAlerts();
  },
  methods: {
    fetchAlerts() {
      api_staff.getAlerts({ filter: this.activeFilter }).then((res) => {
        const list = Array.isArray(res) ? res : res && res.alerts || [];
        const total = list.length;
        const todo = list.filter((a) => (a.status || "todo") === "todo").length;
        this.setData(
          {
            alerts: list,
            summary: { total, todo }
          },
          () => {
            this.updateVisibleAlerts();
          }
        );
      }).catch(() => {
        {
          this.setData({ alerts: [], summary: { total: 0, todo: 0 } }, () => this.updateVisibleAlerts());
          return;
        }
      });
    },
    onChangeFilter(e) {
      const filter = e.currentTarget.dataset.filter;
      if (filter === this.activeFilter) {
        return;
      }
      this.setData(
        {
          activeFilter: filter
        },
        () => {
          this.updateVisibleAlerts();
        }
      );
    },
    updateVisibleAlerts() {
      const { alerts, activeFilter } = this;
      let list = alerts;
      if (activeFilter === "bp") {
        list = alerts.filter((a) => a.typeFlags.includes("bp"));
      } else if (activeFilter === "weight") {
        list = alerts.filter((a) => a.typeFlags.includes("weight"));
      } else if (activeFilter === "symptom") {
        list = alerts.filter((a) => a.typeFlags.includes("symptom"));
      }
      this.setData({
        visibleAlerts: list
      });
    },
    onOpenAlert(e) {
      const id = e.currentTarget.dataset.id;
      common_vendor.index.navigateTo({
        url: `/pages/staff/followup/followup?alertId=${id}`
      });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_vendor.t($data.summary.total),
    b: common_vendor.t($data.summary.todo),
    c: common_vendor.n("filter-chip " + ($data.activeFilter === "all" ? "active" : "")),
    d: common_vendor.o((...args) => $options.onChangeFilter && $options.onChangeFilter(...args)),
    e: common_vendor.n("filter-chip " + ($data.activeFilter === "bp" ? "active" : "")),
    f: common_vendor.o((...args) => $options.onChangeFilter && $options.onChangeFilter(...args)),
    g: common_vendor.n("filter-chip " + ($data.activeFilter === "weight" ? "active" : "")),
    h: common_vendor.o((...args) => $options.onChangeFilter && $options.onChangeFilter(...args)),
    i: common_vendor.n("filter-chip " + ($data.activeFilter === "symptom" ? "active" : "")),
    j: common_vendor.o((...args) => $options.onChangeFilter && $options.onChangeFilter(...args)),
    k: common_vendor.f($data.visibleAlerts, (item, index, i0) => {
      return {
        a: common_vendor.t(item.patientName),
        b: common_vendor.t(item.age),
        c: common_vendor.t(item.gender),
        d: common_vendor.t(item.mainDisease),
        e: common_vendor.t(item.riskText),
        f: common_vendor.n("risk-tag " + item.riskLevel),
        g: common_vendor.f(item.reasons, (item2, index1, i1) => {
          return {
            a: common_vendor.t(item2),
            b: index1
          };
        }),
        h: common_vendor.t(item.latestBP || "--"),
        i: common_vendor.t(item.latestWeight || "--"),
        j: common_vendor.t(item.planText),
        k: common_vendor.t(item.nextVisitDate || "待制定"),
        l: common_vendor.t(item.status === "done" ? "已处理" : "待处理"),
        m: common_vendor.n("alert-status " + item.status),
        n: common_vendor.o((...args) => $options.onOpenAlert && $options.onOpenAlert(...args), index),
        o: item.id,
        p: index
      };
    }),
    l: !$data.visibleAlerts || $data.visibleAlerts.length === 0
  }, !$data.visibleAlerts || $data.visibleAlerts.length === 0 ? {} : {});
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../../.sourcemap/mp-weixin/pages/staff/alarm/alarm.js.map
