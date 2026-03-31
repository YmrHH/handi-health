"use strict";
function clone(target) {
  return JSON.parse(JSON.stringify(target));
}
exports.clone = clone;
