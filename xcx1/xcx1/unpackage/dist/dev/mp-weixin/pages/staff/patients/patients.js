"use strict";
const common_vendor = require("../../../common/vendor.js");
const api_staff = require("../../../api/staff.js");
const _sfc_main = {
  data() {
    return {
      patients: []
    };
  },
  onLoad() {
    this.loadPatients();
  },
  methods: {
    goPatientData(item) {
      const id = item.id || "";
      if (!id) {
        common_vendor.index.showToast({ title: "缺少患者ID", icon: "none" });
        return;
      }
      const name = encodeURIComponent(item.name || "");
      const age = item.age || "";
      const gender = encodeURIComponent(item.gender || "");
      common_vendor.index.navigateTo({
        url: `/pages/staff/patientData/patientData?id=${id}&name=${name}&age=${age}&gender=${gender}`
      });
    },
    loadPatients() {
      api_staff.getPatients().then((res) => {
        const list = Array.isArray(res) ? res : res && (res.list || res.patients) || [];
        this.patients = Array.isArray(list) ? list : [];
      }).catch(() => {
        {
          this.patients = [];
          return;
        }
      });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return {
    a: common_vendor.f($data.patients, (item, index, i0) => {
      return common_vendor.e({
        a: common_vendor.t(item.name),
        b: common_vendor.t(item.age),
        c: item.riskLevel === "高"
      }, item.riskLevel === "高" ? {
        d: common_vendor.t(item.riskLevel)
      } : item.riskLevel === "中" ? {
        f: common_vendor.t(item.riskLevel)
      } : item.riskLevel === "低" ? {
        h: common_vendor.t(item.riskLevel)
      } : {
        i: common_vendor.t(item.riskLevel)
      }, {
        e: item.riskLevel === "中",
        g: item.riskLevel === "低",
        j: common_vendor.t(item.disease),
        k: common_vendor.t(item.constitution),
        l: common_vendor.t(item.tcmPattern),
        m: common_vendor.o(($event) => $options.goPatientData(item), index),
        n: index
      });
    })
  };
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../../.sourcemap/mp-weixin/pages/staff/patients/patients.js.map
