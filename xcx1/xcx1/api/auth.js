// api/auth.js
import { request } from '@/utils/request.js';

// 普通账号登录（保留兼容，当前首页已不再使用）
export function login(data) {
  const payload = {
    username: (data && data.username) || '',
    password: (data && data.password) || ''
  };

  return request({
    url: '/auth/login',
    method: 'POST',
    data: payload
  }).then((res) => {
    const raw = res || {};
    const userId = raw.userId ?? raw.id ?? '';
    const user = {
      id: userId,
      name: raw.name || '',
      backendRole: raw.role || '',
      username: payload.username,
      phone: raw.phone || '',
      password: payload.password
    };
    return { token: String(userId || ''), user, raw };
  });
}

// 统一手机号登录（患者 / 医生 / 随访员）
export function loginByPhone(data) {
  const payload = {
    phone: (data && data.phone) || '',
    password: (data && data.password) || ''
  };

  return request({
    url: '/auth/loginByPhone',
    method: 'POST',
    data: payload
  }).then((res) => {
    const raw = res || {};
    const userId = raw.userId ?? raw.id ?? '';
    const user = {
      id: userId,
      name: raw.name || '',
      backendRole: raw.role || '',
      username: raw.username || '',
      phone: raw.phone || payload.phone,
      password: payload.password
    };
    return { token: String(userId || ''), user, raw };
  });
}

export function logout() {
  return request({
    url: '/auth/logout',
    method: 'POST'
  });
}

export function getMe() {
  return request({
    url: '/auth/me',
    method: 'GET'
  });
}

// 修改密码（校验旧密码）
export function changePassword(data) {
  const payload = {
    userId: data && data.userId,
    oldPassword: (data && data.oldPassword) || '',
    newPassword: (data && data.newPassword) || '',
    confirmPassword: (data && data.confirmPassword) || ''
  };

  return request({
    url: '/auth/changePassword',
    method: 'POST',
    data: payload
  });
}