"use strict";
function escape2Html(str) {
  if (!str)
    return str;
  var arrEntities = {
    "lt": "<",
    "gt": ">",
    "nbsp": " ",
    "amp": "&",
    "quot": '"'
  };
  return str.replace(/&(lt|gt|nbsp|amp|quot);/ig, function(all, t) {
    return arrEntities[t];
  });
}
function html2Escape(sHtml) {
  if (!sHtml)
    return sHtml;
  return sHtml.replace(/[<>&"]/g, function(c) {
    return {
      "<": "&lt;",
      ">": "&gt;",
      "&": "&amp;",
      '"': "&quot;"
    }[c];
  });
}
exports.escape2Html = escape2Html;
exports.html2Escape = html2Escape;
//# sourceMappingURL=../../../../.sourcemap/mp-weixin/uni_modules/zp-mixins/methods/escape.js.map
