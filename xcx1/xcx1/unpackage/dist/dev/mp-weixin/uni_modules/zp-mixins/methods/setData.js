"use strict";
const uni_modules_zpMixins_utils__set = require("../utils/_set.js");
const variableNameReg = /^([^\x00-\xff]|[a-zA-Z_$])([^\x00-\xff]|[a-zA-Z0-9_$])*$/;
function setData(obj, callback = null) {
  Object.keys(obj).forEach((key) => {
    uni_modules_zpMixins_utils__set._set(this, key, obj[key]);
    if (variableNameReg.test(key) && key.endsWith("Clone")) {
      let propName = key.replace(/Clone$/, "");
      if (this.$options && this.$options.propsData[propName]) {
        this.$emit(`update:${propName}`, obj[key]);
      }
    }
  });
  this.$forceUpdate();
  if (typeof callback == "function")
    this.$nextTick(callback);
}
exports.setData = setData;
//# sourceMappingURL=../../../../.sourcemap/mp-weixin/uni_modules/zp-mixins/methods/setData.js.map
