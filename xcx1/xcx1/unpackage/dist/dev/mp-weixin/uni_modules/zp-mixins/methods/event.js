"use strict";
function parseEventDynamicCode(e, exp) {
  if (typeof this[exp] === "function") {
    this[exp](e);
  }
}
exports.parseEventDynamicCode = parseEventDynamicCode;
//# sourceMappingURL=../../../../.sourcemap/mp-weixin/uni_modules/zp-mixins/methods/event.js.map
