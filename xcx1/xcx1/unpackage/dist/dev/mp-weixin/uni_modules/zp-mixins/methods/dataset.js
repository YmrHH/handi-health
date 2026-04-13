"use strict";
function handleDataset(event, dataSet = {}) {
  if (event && !event.currentTarget) {
    if (dataSet.tagId) {
      event.currentTarget = {
        id: dataSet.tagId
      };
    } else {
      event.currentTarget = {
        dataset: dataSet
      };
    }
  }
}
exports.handleDataset = handleDataset;
//# sourceMappingURL=../../../../.sourcemap/mp-weixin/uni_modules/zp-mixins/methods/dataset.js.map
