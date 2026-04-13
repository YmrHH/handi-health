"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_metricsStore = require("../metrics-store.js");
const utils_session = require("../session.js");
const api_patient = require("../../api/patient.js");
let _inited = false;
let _rxBuf = "";
let _lastLocalSyncMs = 0;
let _lastBackendSyncMs = 0;
const _state = {
  connected: false,
  deviceId: "",
  name: "",
  lastLine: "",
  lastParsed: null
};
function nowHM() {
  const d = /* @__PURE__ */ new Date();
  const pad = (n) => n < 10 ? "0" + n : "" + n;
  return pad(d.getHours()) + ":" + pad(d.getMinutes());
}
function ab2str(buffer) {
  try {
    if (typeof TextDecoder !== "undefined") {
      return new TextDecoder("utf-8").decode(buffer);
    }
  } catch (e) {
  }
  const arr = new Uint8Array(buffer);
  let s = "";
  for (let i = 0; i < arr.length; i++)
    s += String.fromCharCode(arr[i]);
  try {
    return decodeURIComponent(escape(s));
  } catch (e) {
    return s;
  }
}
function toNum(v) {
  const n = Number(v);
  return Number.isFinite(n) ? n : void 0;
}
function formatMeasuredAt(d = /* @__PURE__ */ new Date()) {
  const pad = (n) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}
function parseEcb02Line(line) {
  const t = String(line || "").trim();
  if (!t)
    return null;
  if (!t.startsWith("AT+DATA1="))
    return null;
  const parts = t.split(/[=,]/);
  if (parts.length < 5)
    return null;
  const hr = parts[1];
  const spo2 = parts[2];
  const temp = parts[3];
  const bpRaw = parts[4];
  const out = {};
  if (hr != null && String(hr).trim() !== "")
    out.hr = String(hr).trim();
  if (spo2 != null && String(spo2).trim() !== "")
    out.spo2 = String(spo2).trim();
  if (temp != null && String(temp).trim() !== "") {
    const n = Number(temp);
    out.weight = Number.isFinite(n) ? n.toFixed(1) : String(temp).trim();
  }
  const bpStr = mapSingleBpToBpString(bpRaw);
  if (bpStr)
    out.bp = bpStr;
  return out;
}
function mapSingleBpToBpString(bpRaw) {
  const v = String(bpRaw ?? "").trim();
  if (!v)
    return "";
  let n = Number(v);
  if (!Number.isFinite(n) || n <= 0)
    return "";
  let mmHg = n < 45 ? n * 7.50062 : n;
  mmHg = Math.round(mmHg);
  const latest = utils_metricsStore.getDailyMetricsLatest() || {};
  const arr = Array.isArray(latest.metrics) ? latest.metrics : [];
  const bpItem = arr.find((m) => m && m.key === "bp");
  const prev = String(bpItem && bpItem.value ? bpItem.value : "").trim();
  let prevSbp = null;
  let prevDbp = null;
  if (prev && prev !== "--") {
    const p = prev.split(/[\/\-\s]+/).filter(Boolean);
    const s1 = Number(p[0]);
    const s2 = Number(p[1]);
    prevSbp = Number.isFinite(s1) ? Math.round(s1) : null;
    prevDbp = Number.isFinite(s2) ? Math.round(s2) : null;
  }
  const isSbp = mmHg >= 100;
  const sbp = isSbp ? mmHg : prevSbp;
  const dbp = isSbp ? prevDbp : mmHg;
  const sbpStr = sbp != null ? String(sbp) : "--";
  const dbpStr = dbp != null ? String(dbp) : "--";
  return `${sbpStr}/${dbpStr}`;
}
function buildDailyMeasurementPayload(normalized) {
  const d = normalized || {};
  const payload = {
    measuredAt: formatMeasuredAt(/* @__PURE__ */ new Date())
  };
  const hr = toNum(d.hr);
  if (hr !== void 0)
    payload.hr = hr;
  const temp = toNum(d.weight);
  if (temp !== void 0)
    payload.temp = temp;
  const spo2 = toNum(d.spo2);
  if (spo2 !== void 0)
    payload.spo2 = spo2;
  const glucose = toNum(d.glucose);
  if (glucose !== void 0)
    payload.glucose = glucose;
  const sleep = toNum(d.sleep);
  if (sleep !== void 0)
    payload.sleep = sleep;
  if (d.bp) {
    const seg = String(d.bp).trim().split(/[\/\-\s]+/).filter(Boolean);
    const sbp = toNum(seg[0]);
    const dbp = toNum(seg[1]);
    if (sbp !== void 0)
      payload.sbp = sbp;
    if (dbp !== void 0)
      payload.dbp = dbp;
  }
  return payload;
}
function shouldAutoSyncLocal() {
  const v = utils_session.getScopedStorageSync("ble_auto_sync");
  if (typeof v === "boolean")
    return v;
  return true;
}
function shouldAutoSyncBackend() {
  const v = utils_session.getScopedStorageSync("ble_auto_sync_backend");
  if (typeof v === "boolean")
    return v;
  return false;
}
function throttleWriteLocal(parsed) {
  const now = Date.now();
  if (now - _lastLocalSyncMs < 5e3)
    return;
  _lastLocalSyncMs = now;
  utils_metricsStore.updateDailyMetricsLatest(
    {
      hr: parsed.hr,
      spo2: parsed.spo2,
      weight: parsed.weight,
      bp: parsed.bp
    },
    "蓝牙设备",
    { silent: true }
  );
  utils_metricsStore.upsertTimeseriesFromPatch(
    {
      hr: parsed.hr,
      spo2: parsed.spo2,
      weight: parsed.weight,
      bp: parsed.bp
    },
    "蓝牙设备"
  );
  const syncAt = nowHM();
  utils_session.setScopedStorageSync("device_last_sync_at", syncAt);
  common_vendor.index.$emit && common_vendor.index.$emit("metrics_latest_updated", { at: syncAt });
}
function throttleSyncBackend(parsed) {
  const now = Date.now();
  if (now - _lastBackendSyncMs < 15e3)
    return;
  _lastBackendSyncMs = now;
  const payload = buildDailyMeasurementPayload(parsed);
  const hasAny = ["sbp", "dbp", "hr", "temp", "spo2", "glucose", "sleep"].some(
    (k) => payload[k] !== void 0
  );
  if (!hasAny)
    return;
  api_patient.saveDailyMeasurement(payload).then(() => {
    utils_session.setScopedStorageSync("device_last_backend_sync_at", nowHM());
  }).catch(() => {
  });
}
function feedChunk(txt, deviceId) {
  _rxBuf += String(txt || "");
  let idx;
  while ((idx = _rxBuf.indexOf("\r\n")) >= 0) {
    const line = _rxBuf.slice(0, idx).trim();
    _rxBuf = _rxBuf.slice(idx + 2);
    if (line)
      handleLine(line, deviceId);
  }
  while ((idx = _rxBuf.indexOf("\n")) >= 0) {
    const line = _rxBuf.slice(0, idx).trim();
    _rxBuf = _rxBuf.slice(idx + 1);
    if (line)
      handleLine(line, deviceId);
  }
  if (_rxBuf.length > 4096)
    _rxBuf = _rxBuf.slice(-1024);
}
function handleLine(line, deviceId) {
  const parsed = parseEcb02Line(line);
  _state.lastLine = line;
  _state.lastParsed = parsed;
  if (deviceId && !_state.deviceId)
    _state.deviceId = deviceId;
  if (parsed) {
    _state.connected = true;
    utils_session.setScopedStorageSync("ble_connected_deviceId", _state.deviceId || deviceId || "");
    utils_session.setScopedStorageSync("ble_connected", true);
  }
  try {
    common_vendor.index.$emit && common_vendor.index.$emit("ble_ecb02_line", { line, parsed, deviceId });
  } catch (e) {
  }
  if (!parsed)
    return;
  if (shouldAutoSyncLocal())
    throttleWriteLocal(parsed);
  if (shouldAutoSyncBackend())
    throttleSyncBackend(parsed);
}
function initEcb02BleHub() {
  if (_inited)
    return;
  _inited = true;
  try {
    common_vendor.index.onBLEConnectionStateChange && common_vendor.index.onBLEConnectionStateChange((res) => {
      if (!res)
        return;
      _state.deviceId = res.deviceId || _state.deviceId;
      _state.connected = !!res.connected;
      utils_session.setScopedStorageSync("ble_connected_deviceId", _state.deviceId || "");
      utils_session.setScopedStorageSync("ble_connected", _state.connected);
      try {
        common_vendor.index.$emit && common_vendor.index.$emit("ble_state_change", { ..._state });
      } catch (e) {
      }
    });
  } catch (e) {
  }
  try {
    common_vendor.index.onBLECharacteristicValueChange && common_vendor.index.onBLECharacteristicValueChange((res) => {
      if (!res || !res.value)
        return;
      const deviceId = res.deviceId || "";
      const txt = ab2str(res.value);
      feedChunk(txt, deviceId);
    });
  } catch (e) {
  }
}
exports.initEcb02BleHub = initEcb02BleHub;
//# sourceMappingURL=../../../.sourcemap/mp-weixin/utils/ble/ecb02-hub.js.map
