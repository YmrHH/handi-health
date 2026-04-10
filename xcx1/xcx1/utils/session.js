// utils/session.js
//
// 统一管理登录态（token / role / user）与“按用户隔离”的本地缓存键。

const KEY_TOKEN = 'token';
const KEY_ROLE = 'role';
const KEY_USER = 'user';

const USER_BUSINESS_CACHE_KEYS = [
  'userInfo',
  'patient_basic_info',
  'patient_profile',
  'profile_sync_at',
  'doctor_advice_latest',
  'doctor_advice_latest_time',
  'daily_measure_today',
  'daily_measure_date',
  'tcm_survey_today',
  'tcm_survey_date',
  'survey_chronic_disease',
  'daily_metrics_latest',
  'daily_metrics_timeseries',
  'daily_metrics_history',
  'followup_summary',
  'rehab_summary',
  'followup_records',
  'followup_tips',
  'followup_apply_requests',
  'message_doctor_list',
  'message_system_list',
  'feedback_list',
  'linkedAccounts',
  'device_last_sync_at',
  'device_last_backend_sync_at',
  'bound_ble_device',
  'ble_connected',
  'ble_connected_deviceId',
  'ble_auto_sync',
  'ble_auto_sync_backend',
  'tcm_tongue_last',
  'ai_chat_thread'
];

export function getToken() {
  try {
    return uni.getStorageSync(KEY_TOKEN) || '';
  } catch (e) {
    return '';
  }
}

export function setToken(token) {
  try {
    uni.setStorageSync(KEY_TOKEN, token || '');
  } catch (e) {}
}

export function getRole() {
  try {
    return uni.getStorageSync(KEY_ROLE) || '';
  } catch (e) {
    return '';
  }
}

export function setRole(role) {
  try {
    uni.setStorageSync(KEY_ROLE, role || '');
  } catch (e) {}
}

export function getUser() {
  try {
    const u = uni.getStorageSync(KEY_USER);
    return u && typeof u === 'object' ? u : null;
  } catch (e) {
    return null;
  }
}

export function setUser(user) {
  try {
    uni.setStorageSync(KEY_USER, user || null);
  } catch (e) {}
}

export function getCurrentUserId() {
  try {
    const u = getUser();
    const id = u && (u.id || u.userId || u.patientId || u.uid);
    return id == null ? '' : String(id).trim();
  } catch (e) {
    return '';
  }
}

export function userScopedKey(baseKey, userId) {
  const base = String(baseKey || '').trim();
  const uid = String(userId || getCurrentUserId() || '').trim();
  if (!base) return '';
  if (!uid) return base;
  return `${base}__u_${uid}`;
}

export function getScopedStorageSync(baseKey, fallback, userId) {
  const key = userScopedKey(baseKey, userId);
  try {
    const v = uni.getStorageSync(key);
    return v === undefined || v === null || v === '' ? fallback : v;
  } catch (e) {
    return fallback;
  }
}

export function setScopedStorageSync(baseKey, value, userId) {
  const key = userScopedKey(baseKey, userId);
  try {
    uni.setStorageSync(key, value);
  } catch (e) {}
}

export function removeScopedStorageSync(baseKey, userId) {
  const key = userScopedKey(baseKey, userId);
  try {
    uni.removeStorageSync(key);
  } catch (e) {}
}

export function getCurrentUserBusinessCacheKeys() {
  return USER_BUSINESS_CACHE_KEYS.slice();
}

export function clearCurrentUserBusinessCache(userId) {
  const uid = userId || getCurrentUserId();
  USER_BUSINESS_CACHE_KEYS.forEach((baseKey) => {
    try {
      uni.removeStorageSync(userScopedKey(baseKey, uid));
    } catch (e) {}
  });

  // 兼容清理旧版未分用户的遗留缓存
  USER_BUSINESS_CACHE_KEYS.forEach((baseKey) => {
    try {
      uni.removeStorageSync(baseKey);
    } catch (e) {}
  });
}

export function clearSession() {
  try {
    clearCurrentUserBusinessCache();
    uni.removeStorageSync(KEY_TOKEN);
    uni.removeStorageSync(KEY_USER);
    uni.removeStorageSync(KEY_ROLE);
  } catch (e) {}
}