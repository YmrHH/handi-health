"use strict";
const common_vendor = require("../common/vendor.js");
const config_api = require("../config/api.js");
const utils_session = require("./session.js");
function joinUrl(path) {
  const p = path || "";
  if (/^https?:\/\//i.test(p))
    return p;
  const base = config_api.API_BASE_URL.replace(/\/$/, "");
  const prefix = "".replace(/\/$/, "");
  const rel = p.startsWith("/") ? p : "/" + p;
  return `${base}${prefix}${rel}`;
}
function normalizeError(err) {
  if (!err)
    return { message: "未知错误" };
  if (typeof err === "string")
    return { message: err };
  if (err.message)
    return { message: err.message, raw: err };
  return { message: "请求失败", raw: err };
}
function handleAuthInvalid() {
  utils_session.clearSession();
  try {
    common_vendor.index.showToast({ title: "登录已失效，请重新登录", icon: "none" });
  } catch (e) {
  }
  try {
    common_vendor.index.reLaunch({ url: "/pages/index/index" });
  } catch (e) {
  }
}
function request(options) {
  const opt = options || {};
  const method = (opt.method || "GET").toUpperCase();
  const url = joinUrl(opt.url);
  const header = {
    "Content-Type": "application/json",
    ...opt.header || {}
  };
  const token = utils_session.getToken();
  if (token)
    header.Authorization = String(token);
  return new Promise((resolve, reject) => {
    common_vendor.index.request({
      url,
      method,
      data: opt.data || {},
      header,
      timeout: opt.timeout || config_api.API_TIMEOUT,
      success: (res) => {
        const status = res && res.statusCode;
        const body = res && res.data;
        if (status === 401) {
          handleAuthInvalid();
          return reject({ message: "未登录或登录已过期", status, body });
        }
        if (!status || status < 200 || status >= 300) {
          return reject({ message: `HTTP ${status || "ERR"}`, status, body });
        }
        if (body && typeof body === "object") {
          if (Object.prototype.hasOwnProperty.call(body, "code")) {
            const code = body.code;
            if (code === 0) {
              return resolve(Object.prototype.hasOwnProperty.call(body, "data") ? body.data : body);
            }
            if (code === 401 || code === 403 || code === 40101 || code === 40301) {
              handleAuthInvalid();
            }
            return reject({ message: body.msg || body.message || "请求失败", code, body });
          }
          if (Object.prototype.hasOwnProperty.call(body, "success")) {
            if (body.success) {
              return resolve(Object.prototype.hasOwnProperty.call(body, "data") ? body.data : body);
            }
            const code = body.code;
            if (code === 401 || code === 403 || code === 40101 || code === 40301) {
              handleAuthInvalid();
            }
            return reject({ message: body.message || body.msg || "请求失败", code, body });
          }
        }
        resolve(body);
      },
      fail: (err) => reject(normalizeError(err))
    });
  });
}
function uploadFile(options) {
  const opt = options || {};
  const url = joinUrl(opt.url);
  const token = utils_session.getToken();
  const header = {
    ...opt.header || {}
  };
  if (token)
    header.Authorization = String(token);
  return new Promise((resolve, reject) => {
    common_vendor.index.uploadFile({
      url,
      filePath: opt.filePath,
      name: opt.name || "file",
      formData: opt.formData || {},
      header,
      timeout: opt.timeout || config_api.API_TIMEOUT,
      success: (res) => {
        const status = res && res.statusCode;
        let body = res && res.data;
        if (status === 401) {
          handleAuthInvalid();
          return reject({ message: "未登录或登录已过期", status });
        }
        if (!status || status < 200 || status >= 300) {
          return reject({ message: `HTTP ${status || "ERR"}`, status, body });
        }
        if (typeof body === "string") {
          try {
            body = JSON.parse(body);
          } catch (e) {
            return resolve(body);
          }
        }
        if (body && typeof body === "object" && Object.prototype.hasOwnProperty.call(body, "code")) {
          const code = body.code;
          if (code === 0) {
            return resolve(Object.prototype.hasOwnProperty.call(body, "data") ? body.data : body);
          }
          return reject({ message: body.msg || body.message || "上传失败", code, body });
        }
        if (body && typeof body === "object" && Object.prototype.hasOwnProperty.call(body, "success")) {
          if (body.success) {
            return resolve(Object.prototype.hasOwnProperty.call(body, "data") ? body.data : body);
          }
          return reject({ message: body.message || body.msg || "上传失败", body });
        }
        resolve(body);
      },
      fail: (err) => reject(normalizeError(err))
    });
  });
}
exports.request = request;
exports.uploadFile = uploadFile;
