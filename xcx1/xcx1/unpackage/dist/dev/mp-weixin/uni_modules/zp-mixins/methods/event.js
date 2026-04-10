"use strict";
function parseEventDynamicCode(e, exp) {
  if (typeof this[exp] === "function") {
    this[exp](e);
  }
}
exports.parseEventDynamicCode = parseEventDynamicCode;
