"use strict";
const common_vendor = require("../../../common/vendor.js");
function getTabBar() {
  return {
    setData(obj) {
      var _a, _b, _c, _d;
      if (typeof ((_b = (_a = this.$mp) == null ? void 0 : _a.page) == null ? void 0 : _b.getTabBar) === "function" && ((_d = (_c = this.$mp) == null ? void 0 : _c.page) == null ? void 0 : _d.getTabBar())) {
        this.$mp.page.getTabBar().setData(obj);
      } else {
        common_vendor.index.__f__("log", "at uni_modules/zp-mixins/methods/getTabBar.js:15", "当前平台不支持getTabBar()，已稍作处理，详细请参见相关文档。");
      }
    }
  };
}
exports.getTabBar = getTabBar;
//# sourceMappingURL=../../../../.sourcemap/mp-weixin/uni_modules/zp-mixins/methods/getTabBar.js.map
