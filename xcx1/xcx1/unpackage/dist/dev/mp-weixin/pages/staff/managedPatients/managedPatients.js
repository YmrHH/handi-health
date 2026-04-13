"use strict";
const common_vendor = require("../../../common/vendor.js");
const api_staff = require("../../../api/staff.js");
const api_consult = require("../../../api/consult.js");
const _sfc_main = {
  data() {
    return {
      keyword: "",
      patients: [],
      loading: false
    };
  },
  computed: {
    filteredPatients() {
      const kw = (this.keyword || "").trim();
      if (!kw)
        return this.patients;
      return this.patients.filter((p) => {
        const s = `${p.name || ""}${p.id || ""}`.toLowerCase();
        return s.includes(kw.toLowerCase());
      });
    }
  },
  onLoad() {
    this.fetchPatients();
  },
  methods: {
    unreadDoctor(patientId) {
      try {
        return api_consult.getUnreadCount(patientId, "doctor");
      } catch (e) {
        return 0;
      }
    },
    riskClass(p) {
      const r = p && p.riskLevel || "";
      if (r === "高" || r === "HIGH")
        return "high";
      if (r === "低" || r === "LOW")
        return "low";
      return "mid";
    },
    riskText(p) {
      const r = p && p.riskLevel || "";
      if (r === "高" || r === "HIGH")
        return "高风险";
      if (r === "低" || r === "LOW")
        return "低风险";
      return "中风险";
    },
    onSearch() {
    },
    fetchPatients() {
      this.loading = true;
      api_staff.listPatients({ page: 1, pageSize: 200 }).then((rows) => {
        this.patients = Array.isArray(rows) ? rows : [];
      }).catch((e) => {
        this.patients = [];
        common_vendor.index.showToast({ title: (e == null ? void 0 : e.message) || "加载患者失败", icon: "none" });
      }).finally(() => {
        this.loading = false;
      });
    },
    onOpenData(e) {
      const id = e.currentTarget.dataset.id;
      const name = e.currentTarget.dataset.name;
      common_vendor.index.navigateTo({
        url: `/pages/staff/patientData/patientData?id=${encodeURIComponent(id)}&name=${encodeURIComponent(name || "")}`
      });
    },
    onOpenChat(e) {
      const id = e.currentTarget.dataset.id;
      const name = e.currentTarget.dataset.name;
      common_vendor.index.navigateTo({
        url: `/pages/staff/patientChat/patientChat?id=${encodeURIComponent(id)}&name=${encodeURIComponent(name || "")}`
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
    a: common_vendor.o((...args) => $options.onSearch && $options.onSearch(...args)),
    b: $data.keyword,
    c: common_vendor.o(($event) => $data.keyword = $event.detail.value),
    d: common_vendor.o((...args) => $options.onSearch && $options.onSearch(...args)),
    e: $data.loading
  }, $data.loading ? {} : $options.filteredPatients.length === 0 ? {} : {
    g: common_vendor.f($options.filteredPatients, (p, k0, i0) => {
      return common_vendor.e({
        a: common_vendor.t(p.name || "未知患者"),
        b: p.age
      }, p.age ? {
        c: common_vendor.t(p.age)
      } : {}, {
        d: common_vendor.t($options.riskText(p)),
        e: common_vendor.n($options.riskClass(p)),
        f: common_vendor.t(p.id),
        g: common_vendor.t(p.disease || "—"),
        h: common_vendor.o((...args) => $options.onOpenData && $options.onOpenData(...args), p.id),
        i: p.id,
        j: p.name,
        k: $options.unreadDoctor(p.id) > 0
      }, $options.unreadDoctor(p.id) > 0 ? {
        l: common_vendor.t($options.unreadDoctor(p.id) > 99 ? "99+" : $options.unreadDoctor(p.id))
      } : {}, {
        m: common_vendor.o((...args) => $options.onOpenChat && $options.onOpenChat(...args), p.id),
        n: p.id,
        o: p.name,
        p: p.id
      });
    })
  }, {
    f: $options.filteredPatients.length === 0,
    h: common_vendor.p({
      active: "managed"
    })
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../../.sourcemap/mp-weixin/pages/staff/managedPatients/managedPatients.js.map
