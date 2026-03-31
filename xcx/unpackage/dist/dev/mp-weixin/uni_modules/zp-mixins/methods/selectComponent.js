"use strict";
var selectComponentWeiXin2 = function(selector) {
  var _a;
  console.log(".$scope", this.$scope.selectComponent(selector));
  return ((_a = this.$scope.selectComponent(selector)) == null ? void 0 : _a.data) || void 0;
};
function selectComponent(args) {
  return selectComponentWeiXin2.call(this, args);
}
var selectAllComponentsWeiXin2 = function(selector) {
  var list = this.$scope.selectAllComponents(selector) || [];
  list = list.map((item) => item.data);
  return list;
};
function selectAllComponents(args) {
  return selectAllComponentsWeiXin2.call(this, args);
}
exports.selectAllComponents = selectAllComponents;
exports.selectComponent = selectComponent;
