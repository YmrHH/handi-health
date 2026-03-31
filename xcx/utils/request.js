// utils/request.js
//
// 封装 uni.request / uni.uploadFile：
// - 自动拼接 baseURL
// - 自动携带 Authorization
// - 统一处理后端返回结构
//
// 约定后端统一返回：
// {
//   code: 0,
//   msg: 'ok',
//   data: ...
// }
// code != 0 表示业务失败。

import { API_BASE_URL, API_PREFIX, API_TIMEOUT } from '@/config/api.js';
import { getToken, clearSession } from '@/utils/session.js';

function joinUrl(path) {
  const p = path || '';
  if (/^https?:\/\//i.test(p)) return p;
  const base = (API_BASE_URL || '').replace(/\/$/, '');
  const prefix = (API_PREFIX || '').replace(/\/$/, '');
  const rel = p.startsWith('/') ? p : '/' + p;
  return `${base}${prefix}${rel}`;
}

function normalizeError(err) {
  if (!err) return { message: '未知错误' };
  if (typeof err === 'string') return { message: err };
  if (err.message) return { message: err.message, raw: err };
  return { message: '请求失败', raw: err };
}

function handleAuthInvalid() {
  clearSession();
  try {
    uni.showToast({ title: '登录已失效，请重新登录', icon: 'none' });
  } catch (e) {}
  try {
    uni.reLaunch({ url: '/pages/index/index' });
  } catch (e) {}
}

export function request(options) {
  const opt = options || {};
  const method = (opt.method || 'GET').toUpperCase();
  const url = joinUrl(opt.url);
  const header = {
    'Content-Type': 'application/json',
    ...(opt.header || {})
  };
  const token = getToken();
  if (token) header.Authorization = String(token);

  return new Promise((resolve, reject) => {
    uni.request({
      url,
      method,
      data: opt.data || {},
      header,
      timeout: opt.timeout || API_TIMEOUT,
      success: (res) => {
        const status = res && res.statusCode;
        const body = res && res.data;

        if (status === 401) {
          handleAuthInvalid();
          return reject({ message: '未登录或登录已过期', status, body });
        }

        // HTTP 非 2xx
        if (!status || status < 200 || status >= 300) {
          return reject({ message: `HTTP ${status || 'ERR'}`, status, body });
        }

        // 兼容多种返回结构
        if (body && typeof body === 'object') {
          // 推荐结构：{code,msg,data}
          if (Object.prototype.hasOwnProperty.call(body, 'code')) {
            const code = body.code;
            if (code === 0) {
              // 兼容后端：成功时不一定有 data 字段（例如 /auth/login 返回 userId/role/name）
              return resolve(Object.prototype.hasOwnProperty.call(body, 'data') ? body.data : body);
            }
            // 兼容自定义鉴权码：40101 / 40301
            if (code === 401 || code === 403 || code === 40101 || code === 40301) {
              handleAuthInvalid();
            }
            return reject({ message: body.msg || body.message || '请求失败', code, body });
          }

          // 常见结构：{success:boolean,message,data}
          if (Object.prototype.hasOwnProperty.call(body, 'success')) {
            if (body.success) {
              return resolve(Object.prototype.hasOwnProperty.call(body, 'data') ? body.data : body);
            }
            // success=false 但也可能带 code
            const code = body.code;
            if (code === 401 || code === 403 || code === 40101 || code === 40301) {
              handleAuthInvalid();
            }
            return reject({ message: body.message || body.msg || '请求失败', code, body });
          }
        }

        // 无约定结构：直接返回
        resolve(body);
      },
      fail: (err) => reject(normalizeError(err))
    });
  });
}

export function uploadFile(options) {
  const opt = options || {};
  const url = joinUrl(opt.url);
  const token = getToken();

  const header = {
    ...(opt.header || {})
  };
  if (token) header.Authorization = String(token);

  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url,
      filePath: opt.filePath,
      name: opt.name || 'file',
      formData: opt.formData || {},
      header,
      timeout: opt.timeout || API_TIMEOUT,
      success: (res) => {
        const status = res && res.statusCode;
        let body = res && res.data;

        if (status === 401) {
          handleAuthInvalid();
          return reject({ message: '未登录或登录已过期', status });
        }
        if (!status || status < 200 || status >= 300) {
          return reject({ message: `HTTP ${status || 'ERR'}`, status, body });
        }

        // uploadFile 返回的 data 通常是 string，需要 JSON.parse
        if (typeof body === 'string') {
          try {
            body = JSON.parse(body);
          } catch (e) {
            // 如果不是 JSON，直接返回字符串
            return resolve(body);
          }
        }

        if (body && typeof body === 'object' && Object.prototype.hasOwnProperty.call(body, 'code')) {
          const code = body.code;
          if (code === 0) {
            return resolve(Object.prototype.hasOwnProperty.call(body, 'data') ? body.data : body);
          }
          return reject({ message: body.msg || body.message || '上传失败', code, body });
        }
        if (body && typeof body === 'object' && Object.prototype.hasOwnProperty.call(body, 'success')) {
          if (body.success) {
            return resolve(Object.prototype.hasOwnProperty.call(body, 'data') ? body.data : body);
          }
          return reject({ message: body.message || body.msg || '上传失败', body });
        }

        resolve(body);
      },
      fail: (err) => reject(normalizeError(err))
    });
  });
}
