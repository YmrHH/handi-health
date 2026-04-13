"use strict";
const utils_request = require("../utils/request.js");
function loginByPhone(data) {
  const payload = {
    phone: data && data.phone || "",
    password: data && data.password || ""
  };
  return utils_request.request({
    url: "/auth/loginByPhone",
    method: "POST",
    data: payload
  }).then((res) => {
    const raw = res || {};
    const userId = raw.userId ?? raw.id ?? "";
    const user = {
      id: userId,
      name: raw.name || "",
      backendRole: raw.role || "",
      username: raw.username || "",
      phone: raw.phone || payload.phone,
      password: payload.password
    };
    return { token: String(userId || ""), user, raw };
  });
}
function logout() {
  return utils_request.request({
    url: "/auth/logout",
    method: "POST"
  });
}
function changePassword(data) {
  const payload = {
    userId: data && data.userId,
    oldPassword: data && data.oldPassword || "",
    newPassword: data && data.newPassword || "",
    confirmPassword: data && data.confirmPassword || ""
  };
  return utils_request.request({
    url: "/auth/changePassword",
    method: "POST",
    data: payload
  });
}
exports.changePassword = changePassword;
exports.loginByPhone = loginByPhone;
exports.logout = logout;
//# sourceMappingURL=../../.sourcemap/mp-weixin/api/auth.js.map
