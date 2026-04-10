"use strict";
const common_vendor = require("../../common/vendor.js");
const _sfc_main = {
  name: "StaffTabbar",
  props: {
    active: { type: String, default: "tasks" }
  },
  methods: {
    goTasks() {
      if (this.active === "tasks")
        return;
      common_vendor.index.reLaunch({ url: "/pages/staff/tasks/tasks" });
    },
    goManaged() {
      if (this.active === "managed")
        return;
      common_vendor.index.reLaunch({ url: "/pages/staff/managedPatients/managedPatients" });
    },
    goMe() {
      if (this.active === "me")
        return;
      common_vendor.index.reLaunch({ url: "/pages/staff/profile/profile" });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return {
    a: common_vendor.n($props.active === "tasks" ? "active" : ""),
    b: common_vendor.o((...args) => $options.goTasks && $options.goTasks(...args)),
    c: common_vendor.n($props.active === "managed" ? "active" : ""),
    d: common_vendor.o((...args) => $options.goManaged && $options.goManaged(...args)),
    e: common_vendor.n($props.active === "me" ? "active" : ""),
    f: common_vendor.o((...args) => $options.goMe && $options.goMe(...args))
  };
}
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render]]);
wx.createComponent(Component);
