"use strict";
const common_vendor = require("../common/vendor.js");
const KEY_PREFIX = "consult_thread_";
function key(patientId) {
  return KEY_PREFIX + String(patientId || "");
}
function formatHM(date) {
  const d = date instanceof Date ? date : new Date(date || Date.now());
  const h = String(d.getHours()).padStart(2, "0");
  const m = String(d.getMinutes()).padStart(2, "0");
  return `${h}:${m}`;
}
function uid() {
  return "c_" + Date.now() + "_" + Math.floor(Math.random() * 1e5);
}
function safeArray(v) {
  return Array.isArray(v) ? v : [];
}
function read(patientId) {
  try {
    const rows = common_vendor.index.getStorageSync(key(patientId));
    return safeArray(rows);
  } catch (e) {
    return [];
  }
}
function write(patientId, rows) {
  try {
    common_vendor.index.setStorageSync(key(patientId), safeArray(rows));
  } catch (e) {
  }
}
function listThread(patientId) {
  const rows = read(patientId);
  return rows.sort((a, b) => (a.ts || 0) - (b.ts || 0));
}
function appendMessage(patientId, payload) {
  const rows = listThread(patientId);
  const sender = payload && payload.sender;
  if (sender !== "patient" && sender !== "doctor") {
    throw { message: "sender 必须为 patient 或 doctor" };
  }
  const ts = Date.now();
  const msg = {
    id: uid(),
    ts,
    time: payload.time || formatHM(ts),
    sender,
    text: payload.text || "",
    isVoice: !!payload.isVoice,
    voicePath: payload.voicePath || "",
    duration: payload.duration || 0,
    readByPatient: sender === "patient",
    readByDoctor: sender === "doctor"
  };
  const next = [...rows, msg].slice(-200);
  write(patientId, next);
  return msg;
}
function markRead(patientId, who) {
  const rows = listThread(patientId);
  const next = rows.map((m) => {
    if (who === "patient") {
      return { ...m, readByPatient: true };
    }
    if (who === "doctor") {
      return { ...m, readByDoctor: true };
    }
    return m;
  });
  write(patientId, next);
}
function getUnreadCount(patientId, who) {
  const rows = listThread(patientId);
  if (who === "patient") {
    return rows.filter((m) => m.sender === "doctor" && !m.readByPatient).length;
  }
  if (who === "doctor") {
    return rows.filter((m) => m.sender === "patient" && !m.readByDoctor).length;
  }
  return 0;
}
exports.appendMessage = appendMessage;
exports.formatHM = formatHM;
exports.getUnreadCount = getUnreadCount;
exports.listThread = listThread;
exports.markRead = markRead;
//# sourceMappingURL=../../.sourcemap/mp-weixin/api/consult.js.map
