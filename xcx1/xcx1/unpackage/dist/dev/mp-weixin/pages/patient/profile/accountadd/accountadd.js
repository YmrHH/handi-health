"use strict";
const common_vendor = require("../../../../common/vendor.js");
const _sfc_main = {
  methods: {
    goBack() {
      const pages = getCurrentPages();
      if (pages && pages.length > 1) {
        common_vendor.index.navigateBack();
        return;
      }
      common_vendor.index.reLaunch({ url: "/pages/patient/profile/profile" });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return {
    a: common_vendor.o((...args) => $options.goBack && $options.goBack(...args))
  };
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render]]);
wx.createPage(MiniProgramPage);
