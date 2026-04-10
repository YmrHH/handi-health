// api/consult.js
//
// 在线咨询（医生咨询）对话：前端本地存储兜底实现。
//
// 说明：当前项目未接入后端 IM/会话接口，因此使用 uni.getStorageSync 在同一设备内
// 共享对话记录，实现「患者端医生咨询」与「医护端患者消息」的双向对话。
//
// 后续如果接入后端（WebSocket/IM/REST），可在此文件内替换为 request() 调用，
// 页面层无需改动。

const KEY_PREFIX = 'consult_thread_';

function key(patientId) {
  return KEY_PREFIX + String(patientId || '');
}

export function formatHM(date) {
  const d = date instanceof Date ? date : new Date(date || Date.now());
  const h = String(d.getHours()).padStart(2, '0');
  const m = String(d.getMinutes()).padStart(2, '0');
  return `${h}:${m}`;
}

function uid() {
  return 'c_' + Date.now() + '_' + Math.floor(Math.random() * 100000);
}

function safeArray(v) {
  return Array.isArray(v) ? v : [];
}

function read(patientId) {
  try {
    const rows = uni.getStorageSync(key(patientId));
    return safeArray(rows);
  } catch (e) {
    return [];
  }
}

function write(patientId, rows) {
  try {
    uni.setStorageSync(key(patientId), safeArray(rows));
  } catch (e) {}
}

// 消息结构：
// {
//   id, ts, time,
//   sender: 'patient' | 'doctor',
//   text?, isVoice?, voicePath?, duration?,
//   readByPatient: boolean,
//   readByDoctor: boolean
// }

export function listThread(patientId) {
  const rows = read(patientId);
  return rows.sort((a, b) => (a.ts || 0) - (b.ts || 0));
}

export function appendMessage(patientId, payload) {
  const rows = listThread(patientId);

  const sender = payload && payload.sender;
  if (sender !== 'patient' && sender !== 'doctor') {
    throw { message: 'sender 必须为 patient 或 doctor' };
  }

  const ts = Date.now();
  const msg = {
    id: uid(),
    ts,
    time: payload.time || formatHM(ts),
    sender,
    text: payload.text || '',
    isVoice: !!payload.isVoice,
    voicePath: payload.voicePath || '',
    duration: payload.duration || 0,
    readByPatient: sender === 'patient',
    readByDoctor: sender === 'doctor'
  };

  // 只保留最近 200 条
  const next = [...rows, msg].slice(-200);
  write(patientId, next);
  return msg;
}

export function markRead(patientId, who /* 'patient' | 'doctor' */) {
  const rows = listThread(patientId);
  const next = rows.map((m) => {
    if (who === 'patient') {
      return { ...m, readByPatient: true };
    }
    if (who === 'doctor') {
      return { ...m, readByDoctor: true };
    }
    return m;
  });
  write(patientId, next);
}

export function getUnreadCount(patientId, who) {
  const rows = listThread(patientId);
  if (who === 'patient') {
    return rows.filter((m) => m.sender === 'doctor' && !m.readByPatient).length;
  }
  if (who === 'doctor') {
    return rows.filter((m) => m.sender === 'patient' && !m.readByDoctor).length;
  }
  return 0;
}
