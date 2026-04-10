"use strict";
const utils_request = require("../utils/request.js");
function chatWithAI(data) {
  return utils_request.request({
    url: "/api/ai/chat",
    method: "POST",
    data: data || {}
  });
}
exports.chatWithAI = chatWithAI;
