"use strict";
function getRelationNodes(name) {
  if (!this.$unicom)
    throw "this.getRelationNodes()需与p-f-unicom配合使用！";
  return this.$unicom("@" + name);
}
exports.getRelationNodes = getRelationNodes;
//# sourceMappingURL=../../../../.sourcemap/mp-weixin/uni_modules/zp-mixins/methods/relation.js.map
