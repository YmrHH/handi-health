"use strict";
function getRelationNodes(name) {
  if (!this.$unicom)
    throw "this.getRelationNodes()需与p-f-unicom配合使用！";
  return this.$unicom("@" + name);
}
exports.getRelationNodes = getRelationNodes;
