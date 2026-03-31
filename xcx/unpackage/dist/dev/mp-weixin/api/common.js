"use strict";
const utils_request = require("../utils/request.js");
function upload(filePath, formData) {
  return utils_request.uploadFile({
    url: "/files/upload",
    filePath,
    name: "file",
    formData: formData || {}
  });
}
exports.upload = upload;
