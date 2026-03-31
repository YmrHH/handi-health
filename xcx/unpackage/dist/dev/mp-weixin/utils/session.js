"use strict";
const common_vendor = require("../common/vendor.js");
const KEY_TOKEN = "token";
const KEY_ROLE = "role";
const KEY_USER = "user";
const USER_BUSINESS_CACHE_KEYS = [
  "userInfo",
  "patient_basic_info",
  "patient_profile",
  "profile_sync_at",
  "doctor_advice_latest",
  "doctor_advice_latest_time",
  "daily_measure_today",
  "daily_measure_date",
  "tcm_survey_today",
  "tcm_survey_date",
  "survey_chronic_disease",
  "daily_metrics_latest",
  "daily_metrics_timeseries",
  "daily_metrics_history",
  "followup_summary",
  "rehab_summary",
  "followup_records",
  "followup_tips",
  "followup_apply_requests",
  "message_doctor_list",
  "message_system_list",
  "feedback_list",
  "linkedAccounts",
  "device_last_sync_at",
  "device_last_backend_sync_at",
  "bound_ble_device",
  "ble_connected",
  "ble_connected_deviceId",
  "ble_auto_sync",
  "ble_auto_sync_backend",
  "tcm_tongue_last"
];
function getToken() {
  try {
    return common_vendor.index.getStorageSync(KEY_TOKEN) || "";
  } catch (e) {
    return "";
  }
}
function setToken(token) {
  try {
    common_vendor.index.setStorageSync(KEY_TOKEN, token || "");
  } catch (e) {
  }
}
function setRole(role) {
  try {
    common_vendor.index.setStorageSync(KEY_ROLE, role || "");
  } catch (e) {
  }
}
function getUser() {
  try {
    const u = common_vendor.index.getStorageSync(KEY_USER);
    return u && typeof u === "object" ? u : null;
  } catch (e) {
    return null;
  }
}
function setUser(user) {
  try {
    common_vendor.index.setStorageSync(KEY_USER, user || null);
  } catch (e) {
  }
}
function getCurrentUserId() {
  try {
    const u = getUser();
    const id = u && (u.id || u.userId || u.patientId || u.uid);
    return id == null ? "" : String(id).trim();
  } catch (e) {
    return "";
  }
}
function userScopedKey(baseKey, userId) {
  const base = String(baseKey || "").trim();
  const uid = String(userId || getCurrentUserId() || "").trim();
  if (!base)
    return "";
  if (!uid)
    return base;
  return `${base}__u_${uid}`;
}
function getScopedStorageSync(baseKey, fallback, userId) {
  const key = userScopedKey(baseKey, userId);
  try {
    const v = common_vendor.index.getStorageSync(key);
    return v === void 0 || v === null || v === "" ? fallback : v;
  } catch (e) {
    return fallback;
  }
}
function setScopedStorageSync(baseKey, value, userId) {
  const key = userScopedKey(baseKey, userId);
  try {
    common_vendor.index.setStorageSync(key, value);
  } catch (e) {
  }
}
function clearCurrentUserBusinessCache(userId) {
  const uid = userId || getCurrentUserId();
  USER_BUSINESS_CACHE_KEYS.forEach((baseKey) => {
    try {
      common_vendor.index.removeStorageSync(userScopedKey(baseKey, uid));
    } catch (e) {
    }
  });
  USER_BUSINESS_CACHE_KEYS.forEach((baseKey) => {
    try {
      common_vendor.index.removeStorageSync(baseKey);
    } catch (e) {
    }
  });
}
function clearSession() {
  try {
    clearCurrentUserBusinessCache();
    common_vendor.index.removeStorageSync(KEY_TOKEN);
    common_vendor.index.removeStorageSync(KEY_USER);
    common_vendor.index.removeStorageSync(KEY_ROLE);
  } catch (e) {
  }
}
exports.clearCurrentUserBusinessCache = clearCurrentUserBusinessCache;
exports.clearSession = clearSession;
exports.getScopedStorageSync = getScopedStorageSync;
exports.getToken = getToken;
exports.getUser = getUser;
exports.setRole = setRole;
exports.setScopedStorageSync = setScopedStorageSync;
exports.setToken = setToken;
exports.setUser = setUser;
