"use strict";
const uni_modules_zpMixins_lifecycle_pageLifetimes = require("./lifecycle/pageLifetimes.js");
const uni_modules_zpMixins_methods_clone = require("./methods/clone.js");
const uni_modules_zpMixins_methods_dataset = require("./methods/dataset.js");
const uni_modules_zpMixins_methods_escape = require("./methods/escape.js");
const uni_modules_zpMixins_methods_event = require("./methods/event.js");
const uni_modules_zpMixins_methods_getTabBar = require("./methods/getTabBar.js");
const uni_modules_zpMixins_methods_relation = require("./methods/relation.js");
const uni_modules_zpMixins_methods_selectComponent = require("./methods/selectComponent.js");
const uni_modules_zpMixins_methods_setData = require("./methods/setData.js");
const zpMixins = {
  ...uni_modules_zpMixins_lifecycle_pageLifetimes.pageLifetimes,
  methods: {
    clone: uni_modules_zpMixins_methods_clone.clone,
    handleDataset: uni_modules_zpMixins_methods_dataset.handleDataset,
    escape2Html: uni_modules_zpMixins_methods_escape.escape2Html,
    html2Escape: uni_modules_zpMixins_methods_escape.html2Escape,
    parseEventDynamicCode: uni_modules_zpMixins_methods_event.parseEventDynamicCode,
    getTabBar: uni_modules_zpMixins_methods_getTabBar.getTabBar,
    getRelationNodes: uni_modules_zpMixins_methods_relation.getRelationNodes,
    zpSelectComponent: uni_modules_zpMixins_methods_selectComponent.selectComponent,
    zpSelectAllComponents: uni_modules_zpMixins_methods_selectComponent.selectAllComponents,
    setData: uni_modules_zpMixins_methods_setData.setData
  }
};
exports.zpMixins = zpMixins;
//# sourceMappingURL=../../../.sourcemap/mp-weixin/uni_modules/zp-mixins/index.js.map
