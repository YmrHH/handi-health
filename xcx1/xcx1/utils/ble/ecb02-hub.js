// utils/ble/ecb02-hub.js
// 常驻 BLE 数据接收/解析/节流同步（解决：切换页面后不再接收、回到设备页显示未连接的问题）
//
// 设计目标：
// - BLE notify 接收监听只注册一次（App 全局），不绑定在某个页面实例上
// - 解析 ECB02 文本协议：AT+DATA1=hr,o2,temp,bp,\r\n（bp 将映射到血压 mmHg）
// - 节流写入本地 “身体数据” 与（可选）节流同步后端

import { updateDailyMetricsLatest, upsertTimeseriesFromPatch, getDailyMetricsLatest } from '@/utils/metrics-store.js';
import { getScopedStorageSync, setScopedStorageSync } from '@/utils/session.js';
import { saveDailyMeasurement } from '@/api/patient.js';

// ECB02（STM32 BLE 串口透传）固定 UUID
export const ECB02 = {
  serviceId: '0000FFF0-0000-1000-8000-00805F9B34FB',
  notifyCharId: '0000FFF1-0000-1000-8000-00805F9B34FB',
  writeCharId: '0000FFF2-0000-1000-8000-00805F9B34FB',
  mtu: 500
};

let _inited = false;
let _rxBuf = '';
let _lastLocalSyncMs = 0;
let _lastBackendSyncMs = 0;

// 轻量状态（供页面恢复展示）
const _state = {
  connected: false,
  deviceId: '',
  name: '',
  lastLine: '',
  lastParsed: null
};

function nowHM() {
  const d = new Date();
  const pad = (n) => (n < 10 ? '0' + n : '' + n);
  return pad(d.getHours()) + ':' + pad(d.getMinutes());
}

function ab2str(buffer) {
  try {
    // eslint-disable-next-line no-undef
    if (typeof TextDecoder !== 'undefined') {
      // eslint-disable-next-line no-undef
      return new TextDecoder('utf-8').decode(buffer);
    }
  } catch (e) {}
  const arr = new Uint8Array(buffer);
  let s = '';
  for (let i = 0; i < arr.length; i++) s += String.fromCharCode(arr[i]);
  try {
    return decodeURIComponent(escape(s));
  } catch (e) {
    return s;
  }
}

function toNum(v) {
  const n = Number(v);
  return Number.isFinite(n) ? n : undefined;
}

function formatMeasuredAt(d = new Date()) {
  const pad = (n) => String(n).padStart(2, '0');
  return (
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ` +
    `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  );
}

// 解析一行 AT+DATA1=hr,o2,temp,bp_kpa,
function parseEcb02Line(line) {
  const t = String(line || '').trim();
  if (!t) return null;
  if (!t.startsWith('AT+DATA1=')) return null;

  const parts = t.split(/[=,]/); // ['AT+DATA1','hr','o2','temp','bp','']
  if (parts.length < 5) return null;

  const hr = parts[1];
  const spo2 = parts[2];
  const temp = parts[3];
  const bpRaw = parts[4];

  const out = {};
  if (hr != null && String(hr).trim() !== '') out.hr = String(hr).trim();
  if (spo2 != null && String(spo2).trim() !== '') out.spo2 = String(spo2).trim();
  if (temp != null && String(temp).trim() !== '') {
    const n = Number(temp);
    out.weight = Number.isFinite(n) ? n.toFixed(1) : String(temp).trim(); // 项目里 weight 表示体温
  }
    // bp 为单值，映射到血压（mmHg）字段 bp（尽量只更新一边）
  const bpStr = mapSingleBpToBpString(bpRaw);
  if (bpStr) out.bp = bpStr;

  return out;
}


function mapSingleBpToBpString(bpRaw) {
  const v = String(bpRaw ?? '').trim();
  if (!v) return '';
  let n = Number(v);
  if (!Number.isFinite(n) || n <= 0) return '';
  // 兼容：可能是 mmHg(125.4) 或 kPa(12.3)
  let mmHg = n < 45 ? n * 7.50062 : n;
  mmHg = Math.round(mmHg);

  // 读取历史血压，尽量做到只更新一边
  const latest = getDailyMetricsLatest() || {};
  const arr = Array.isArray(latest.metrics) ? latest.metrics : [];
  const bpItem = arr.find((m) => m && m.key === 'bp');
  const prev = String(bpItem && bpItem.value ? bpItem.value : '').trim();
  let prevSbp = null;
  let prevDbp = null;
  if (prev && prev !== '--') {
    const p = prev.split(/[\/\-\s]+/).filter(Boolean);
    const s1 = Number(p[0]);
    const s2 = Number(p[1]);
    prevSbp = Number.isFinite(s1) ? Math.round(s1) : null;
    prevDbp = Number.isFinite(s2) ? Math.round(s2) : null;
  }

  const isSbp = mmHg >= 100;
  const sbp = isSbp ? mmHg : prevSbp;
  const dbp = isSbp ? prevDbp : mmHg;
  const sbpStr = sbp != null ? String(sbp) : '--';
  const dbpStr = dbp != null ? String(dbp) : '--';
  return `${sbpStr}/${dbpStr}`;
}

function buildDailyMeasurementPayload(normalized) {
  const d = normalized || {};
  const payload = {
    measuredAt: formatMeasuredAt(new Date())
  };

  const hr = toNum(d.hr);
  if (hr !== undefined) payload.hr = hr;

  const temp = toNum(d.weight); // weight=体温
  if (temp !== undefined) payload.temp = temp;

  const spo2 = toNum(d.spo2);
  if (spo2 !== undefined) payload.spo2 = spo2;

  const glucose = toNum(d.glucose);
  if (glucose !== undefined) payload.glucose = glucose;

  const sleep = toNum(d.sleep);
  if (sleep !== undefined) payload.sleep = sleep;

  // 血压：允许 d.bp 为 \"132/85\"；只写入存在的 sbp/dbp
  if (d.bp) {
    const seg = String(d.bp).trim().split(/[\/\-\s]+/).filter(Boolean);
    const sbp = toNum(seg[0]);
    const dbp = toNum(seg[1]);
    if (sbp !== undefined) payload.sbp = sbp;
    if (dbp !== undefined) payload.dbp = dbp;
  }
  return payload;
}

function shouldAutoSyncLocal() {
  const v = getScopedStorageSync('ble_auto_sync');
  // 默认 true
  if (typeof v === 'boolean') return v;
  return true;
}

function shouldAutoSyncBackend() {
  const v = getScopedStorageSync('ble_auto_sync_backend');
  // 默认 false（避免无意刷接口）
  if (typeof v === 'boolean') return v;
  return false;
}

function throttleWriteLocal(parsed) {
  const now = Date.now();
  if (now - _lastLocalSyncMs < 5000) return; // 5s 节流
  _lastLocalSyncMs = now;

  updateDailyMetricsLatest(
    {
      hr: parsed.hr,
      spo2: parsed.spo2,
      weight: parsed.weight,
      bp: parsed.bp
    },
    '蓝牙设备',
    { silent: true }
  );

  upsertTimeseriesFromPatch(
    {
      hr: parsed.hr,
      spo2: parsed.spo2,
      weight: parsed.weight,
      bp: parsed.bp
    },
    '蓝牙设备'
  );

  const syncAt = nowHM();
  setScopedStorageSync('device_last_sync_at', syncAt);
  uni.$emit && uni.$emit('metrics_latest_updated', { at: syncAt }); // 让“我的/身体数据”等页面实时刷新
}

function throttleSyncBackend(parsed) {
  const now = Date.now();
  if (now - _lastBackendSyncMs < 15000) return; // 15s 节流
  _lastBackendSyncMs = now;

  const payload = buildDailyMeasurementPayload(parsed);
  const hasAny = ['sbp', 'dbp', 'hr', 'temp', 'spo2', 'glucose', 'sleep'].some(
    (k) => payload[k] !== undefined
  );
  if (!hasAny) return;

  // 静默同步，失败不打断 UI（可在 vConsole 里看日志）
  saveDailyMeasurement(payload)
    .then(() => {
      setScopedStorageSync('device_last_backend_sync_at', nowHM());
    })
    .catch(() => {});
}

// 把 chunk 按行尾 \r\n 或 \n 重组，再解析
function feedChunk(txt, deviceId) {
  _rxBuf += String(txt || '');
  let idx;

  // 兼容：有的设备可能只带 \n
  while ((idx = _rxBuf.indexOf('\r\n')) >= 0) {
    const line = _rxBuf.slice(0, idx).trim();
    _rxBuf = _rxBuf.slice(idx + 2);
    if (line) handleLine(line, deviceId);
  }

  // 如果没有 \r\n，但出现了 \n，也处理
  while ((idx = _rxBuf.indexOf('\n')) >= 0) {
    const line = _rxBuf.slice(0, idx).trim();
    _rxBuf = _rxBuf.slice(idx + 1);
    if (line) handleLine(line, deviceId);
  }

  // 防止异常情况下缓冲无限增长
  if (_rxBuf.length > 4096) _rxBuf = _rxBuf.slice(-1024);
}

function handleLine(line, deviceId) {
  const parsed = parseEcb02Line(line);
  _state.lastLine = line;
  _state.lastParsed = parsed;

  if (deviceId && !_state.deviceId) _state.deviceId = deviceId;
  if (parsed) {
    _state.connected = true;
    setScopedStorageSync('ble_connected_deviceId', _state.deviceId || deviceId || '');
    setScopedStorageSync('ble_connected', true);
  }

  // 广播给页面（设备页实时显示、其他页可选择监听）
  try {
    uni.$emit && uni.$emit('ble_ecb02_line', { line, parsed, deviceId });
  } catch (e) {}

  if (!parsed) return;

  // 自动同步（本地/后端）
  if (shouldAutoSyncLocal()) throttleWriteLocal(parsed);
  if (shouldAutoSyncBackend()) throttleSyncBackend(parsed);
}

export function getBleHubState() {
  return { ..._state };
}

export function initEcb02BleHub() {
  if (_inited) return;
  _inited = true;

  // 连接状态变化
  try {
    uni.onBLEConnectionStateChange &&
      uni.onBLEConnectionStateChange((res) => {
        if (!res) return;
        _state.deviceId = res.deviceId || _state.deviceId;
        _state.connected = !!res.connected;
        setScopedStorageSync('ble_connected_deviceId', _state.deviceId || '');
        setScopedStorageSync('ble_connected', _state.connected);

        try {
          uni.$emit && uni.$emit('ble_state_change', { ..._state });
        } catch (e) {}
      });
  } catch (e) {}

  // notify 数据接收（只注册一次）
  try {
    uni.onBLECharacteristicValueChange &&
      uni.onBLECharacteristicValueChange((res) => {
        if (!res || !res.value) return;
        const deviceId = res.deviceId || '';
        const txt = ab2str(res.value);
        feedChunk(txt, deviceId);
      });
  } catch (e) {}
}
