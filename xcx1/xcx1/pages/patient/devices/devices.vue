<template>
  <view class="app-page dv">
    <!-- 顶部说明 / 状态 -->
    <view class="app-card app-card--glass dv-head">
      <view class="flex-row items-start justify-between">
        <view class="flex-col">
          <text class="dv-title">设备管理</text>
          <text class="dv-sub">通过蓝牙连接设备，将每日监测数据同步到“身体数据”展示。</text>
        </view>
        <view class="dv-chip" :class="connected ? 'chip-ok' : 'chip-idle'">
          <text class="dv-chip-text">{{ connected ? '已连接' : '未连接' }}</text>
        </view>
      </view>

      <view class="dv-kv">
        <view class="dv-kv-row">
          <text class="dv-k">已绑定设备</text>
          <text class="dv-v">{{ boundDevice.name || '未绑定' }}</text>
        </view>
        <view class="dv-kv-row">
          <text class="dv-k">连接状态</text>
          <text class="dv-v">{{ statusText }}</text>
        </view>
        <view class="dv-kv-row">
          <text class="dv-k">最近同步</text>
          <text class="dv-v">{{ lastSyncAt || '暂无' }}</text>
        </view>
      </view>

      <view class="dv-actions">
        <view class="btn-secondary dv-btn" @tap="initBluetooth">开启蓝牙</view>
        <view class="btn-secondary dv-btn" @tap="toggleDiscovery">{{ discovering ? '停止搜索' : '搜索设备' }}</view>
        <view class="btn-secondary dv-btn" @tap="disconnect" :class="connected ? '' : 'dv-btn--disabled'">断开</view>
        <view class="btn-secondary dv-btn" @tap="seedExample">示例数据</view>
      </view>
      <text class="dv-hint">提示：首次使用请先开启手机蓝牙，并允许小程序使用蓝牙权限。</text>
    </view>

    <!-- 搜索到的设备 -->
    <view class="dv-section flex-row items-center justify-between">
      <text class="app-section-title">附近设备</text>
      <view class="dv-mini-link" @tap="clearDiscovered">
        <text class="dv-mini-link-text">清空</text>
        <text class="dv-arrow">›</text>
      </view>
    </view>

    <view class="app-card dv-list">
      <view v-if="!discovered.length" class="dv-empty">
        <text class="dv-empty-text">未发现设备。点击“搜索设备”，或靠近设备后重试。</text>
      </view>

      <view v-for="d in discovered" :key="d.deviceId" class="dv-item">
        <view class="flex-row items-center justify-between">
          <view class="flex-col">
            <text class="dv-item-name">{{ d.name || '未知设备' }}</text>
            <text class="dv-item-id">{{ d.deviceId }}</text>
          </view>
          <view class="dv-item-right flex-row items-center">
            <text class="dv-rssi">{{ d.RSSI != null ? ('RSSI ' + d.RSSI) : '' }}</text>
            <view class="btn-primary dv-connect" @tap="connectDevice(d)">
              <text class="dv-connect-text">连接</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 数据接收（连接后显示） -->
    <view v-if="connected" class="dv-section flex-row items-center justify-between">
      <text class="app-section-title">数据接收</text>
      <view class="dv-mini-link" @tap="saveParsedToMetrics">
        <text class="dv-mini-link-text">写入身体数据</text>
        <text class="dv-arrow">›</text>
      </view>
    </view>

    <view v-if="connected" class="app-card app-card--glass dv-rx">
      <view class="dv-rx-row">
        <text class="dv-k">自动同步</text>
        <view class="flex-row items-center">
          <text class="dv-v" style="margin-right: 12rpx;">{{ autoSync ? '开' : '关' }}</text>
          <switch :checked="autoSync" @change="onAutoSyncChange"></switch>
        </view>
      </view>
      <view class="dv-rx-row">
        <text class="dv-k">同步后端</text>
        <view class="flex-row items-center">
          <text class="dv-v" style="margin-right: 12rpx;">{{ autoSyncBackend ? '开' : '关' }}</text>
          <switch :checked="autoSyncBackend" @change="onAutoSyncBackendChange"></switch>
        </view>
      </view>
      <view class="dv-rx-row">
        <text class="dv-k">后端同步</text>
        <text class="dv-v">{{ lastBackendSyncAt || '暂无' }}</text>
      </view>
      <view class="dv-rx-row">
        <text class="dv-k">最近一条数据</text>
        <text class="dv-v">{{ lastPayloadText || '等待设备上报…' }}</text>
      </view>
      <view class="dv-rx-row">
        <text class="dv-k">解析结果</text>
        <text class="dv-v">{{ parsedSummary || '—' }}</text>
      </view>
      <text class="dv-hint">说明：已支持 AT+DATA1=hr,o2,temp,bp,\r\n（bp 会映射为血压 mmHg）；同时也会尝试解析 JSON 或 key=value。</text>
    </view>

    <!-- 手动补录 -->
    <view class="dv-section flex-row items-center justify-between">
      <text class="app-section-title">手动补录</text>
      <view class="dv-mini-link" @tap="saveManualToMetrics">
        <text class="dv-mini-link-text">保存到身体数据</text>
        <text class="dv-arrow">›</text>
      </view>
    </view>

    <view class="app-card dv-form">
      <view class="dv-form-row">
        <text class="dv-label">血压</text>
        <input class="dv-input" v-model="manual.bp" placeholder="如 132/85" />
        <text class="dv-unit">mmHg</text>
      </view>

      <view class="dv-form-row">
        <text class="dv-label">心率</text>
        <input class="dv-input" v-model="manual.hr" placeholder="如 68" type="number" />
        <text class="dv-unit">次/分</text>
      </view>

      <view class="dv-form-row">
        <text class="dv-label">血糖</text>
        <input class="dv-input" v-model="manual.glucose" placeholder="如 6.8" type="digit" />
        <text class="dv-unit">mmol/L</text>
      </view>
      <view class="dv-form-row">
        <text class="dv-label">测量类型</text>
        <picker class="dv-picker" mode="selector" :range="glucoseTypeOptions" :value="glucoseTypeIndex" @change="onGlucoseTypeChange">
          <view class="dv-picker-text">{{ glucoseTypeOptions[glucoseTypeIndex] }}</view>
        </picker>
      </view>

      <view class="dv-form-row">
        <text class="dv-label">体温</text>
        <input class="dv-input" v-model="manual.weight" placeholder="如 36.6" type="digit" />
        <text class="dv-unit">℃</text>
      </view>

      <view class="dv-form-row">
        <text class="dv-label">血氧</text>
        <input class="dv-input" v-model="manual.spo2" placeholder="如 97" type="number" />
        <text class="dv-unit">%</text>
      </view>

      <view class="dv-form-row">
        <text class="dv-label">睡眠</text>
        <input class="dv-input" v-model="manual.sleep" placeholder="如 6.4" type="digit" />
        <text class="dv-unit">小时</text>
      </view>

      <view class="dv-form-tip">
        <text class="dv-form-tip-text">无法由设备获取的数据可在此补录，保存后会同步到“我的-身体数据”展示。</text>
      </view>
    </view>
  </view>
</template>

<script>
// 说明：此页面提供一个“可跑通”的蓝牙连接与数据接收框架。
// 设备数据协议未知时：先尝试 JSON、key=value、逗号分隔等通用解析方式。

import { updateDailyMetricsLatest, upsertTimeseriesFromPatch, getDailyMetricsLatest } from '@/utils/metrics-store.js';
import { getScopedStorageSync, setScopedStorageSync } from '@/utils/session.js';
import { saveDailyMeasurement } from '@/api/patient.js';

// ECB02（STM32 蓝牙串口透传模块）固定 UUID
const ECB02 = {
  serviceId: '0000FFF0-0000-1000-8000-00805F9B34FB',
  notifyCharId: '0000FFF1-0000-1000-8000-00805F9B34FB',
  writeCharId: '0000FFF2-0000-1000-8000-00805F9B34FB',
  mtu: 500
};

const normUuid = (u) => String(u || '').toUpperCase();

function nowHM() {
  const d = new Date();
  const pad = (n) => (n < 10 ? '0' + n : '' + n);
  return pad(d.getHours()) + ':' + pad(d.getMinutes());
}

function ab2str(buffer) {
  try {
    // TextDecoder 在多数环境可用
    // eslint-disable-next-line no-undef
    if (typeof TextDecoder !== 'undefined') {
      // eslint-disable-next-line no-undef
      return new TextDecoder('utf-8').decode(buffer);
    }
  } catch (e) {}
  // 兜底：手动转换
  const arr = new Uint8Array(buffer);
  let s = '';
  for (let i = 0; i < arr.length; i++) s += String.fromCharCode(arr[i]);
  try {
    // 处理 utf-8
    return decodeURIComponent(escape(s));
  } catch (e) {
    return s;
  }
}

function str2ab(str) {
  const s = String(str || '');
  try {
    // eslint-disable-next-line no-undef
    if (typeof TextEncoder !== 'undefined') {
      // eslint-disable-next-line no-undef
      const u8 = new TextEncoder().encode(s);
      return u8.buffer;
    }
  } catch (e) {}
  const buf = new ArrayBuffer(s.length);
  const view = new Uint8Array(buf);
  for (let i = 0; i < s.length; i++) view[i] = s.charCodeAt(i);
  return buf;
}

function sleepMs(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function safeJsonParse(txt) {
  try {
    return JSON.parse(txt);
  } catch (e) {
    return null;
  }
}

function parseKeyValue(txt) {
  // 支持：bp=132/85;hr=68;glucose=6.8
  // 或：bp:132/85,hr:68
  const out = {};
  const cleaned = (txt || '').trim();
  if (!cleaned) return out;
  const pairs = cleaned.split(/[;\n,]/g);
  pairs.forEach((p) => {
    const m = p.split(/[:=]/);
    if (m.length >= 2) {
      const k = (m[0] || '').trim();
      const v = (m.slice(1).join(':') || '').trim();
      if (k) out[k] = v;
    }
  });
  return out;
}

function normalizeMetricData(obj) {
  // 将各种可能字段名归一
  const d = obj || {};
  const out = {};

  // 血压
  if (d.bp) out.bp = String(d.bp);
  if (!out.bp && (d.sbp || d.sys || d.bp_sys) && (d.dbp || d.dia || d.bp_dia)) {
    out.bp = `${d.sbp || d.sys || d.bp_sys}/${d.dbp || d.dia || d.bp_dia}`;
  }

  // 心率
  if (d.hr != null) out.hr = String(d.hr);
  if (!out.hr && d.heartRate != null) out.hr = String(d.heartRate);

  // 血糖
  if (d.glucose != null) out.glucose = String(d.glucose);
  if (!out.glucose && d.bg != null) out.glucose = String(d.bg);

  // 体温（沿用字段 weight）
  if (d.weight != null) out.weight = String(d.weight);
  // 常见体温字段别名
  if (!out.weight && d.temperature != null) out.weight = String(d.temperature);
  if (!out.weight && d.temp != null) out.weight = String(d.temp);
  if (!out.weight && d.bodyTemp != null) out.weight = String(d.bodyTemp);
  if (!out.weight && d.body_temperature != null) out.weight = String(d.body_temperature);

  // 血氧 SpO2
  if (d.spo2 != null) out.spo2 = String(d.spo2);
  if (!out.spo2 && d.SpO2 != null) out.spo2 = String(d.SpO2);
  if (!out.spo2 && d.oxygen != null) out.spo2 = String(d.oxygen);
  if (!out.spo2 && d.o2 != null) out.spo2 = String(d.o2);
  if (!out.spo2 && d.bloodOxygen != null) out.spo2 = String(d.bloodOxygen);

  // 睡眠
  if (d.sleep != null) out.sleep = String(d.sleep);
  if (!out.sleep && d.sleepHours != null) out.sleep = String(d.sleepHours);

  // 压力值（kPa）——来自设备 AT+DATA1 第4项，不等同于标准血压
  if (d.bp_kpa != null) out.bp_kpa = String(d.bp_kpa);
  if (!out.bp_kpa && d.pressure_kpa != null) out.bp_kpa = String(d.pressure_kpa);
  if (!out.bp_kpa && d.pressure != null) out.bp_kpa = String(d.pressure);

  // 数值格式化：体温保留1位小数
  if (out.weight != null) {
    const n = Number(out.weight);
    if (!Number.isNaN(n)) out.weight = n.toFixed(1);
  }

  // 压力保留1位小数
  if (out.bp_kpa != null) {
    const n = Number(out.bp_kpa);
    if (!Number.isNaN(n)) out.bp_kpa = n.toFixed(1);
  }

  return out;
}


function mapSingleBpToBpString(bpRaw) {
  const v = String(bpRaw ?? '').trim();
  if (!v) return '';
  let n = Number(v);
  if (!Number.isFinite(n) || n <= 0) return '';
  // 兼容：有些设备实际发的是 mmHg（如 125.4），有些可能是 kPa（如 12.3）
  // 经验阈值：<45 认为是 kPa，换算到 mmHg；否则视为 mmHg
  let mmHg = n < 45 ? n * 7.50062 : n;
  mmHg = Math.round(mmHg);

  // 读取历史血压，尽量做到“只更新一边，另一边保持不变”
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

  // 更像收缩压还是舒张压：>=100 视为收缩压，否则视为舒张压
  const isSbp = mmHg >= 100;
  const sbp = isSbp ? mmHg : prevSbp;
  const dbp = isSbp ? prevDbp : mmHg;
  const sbpStr = sbp != null ? String(sbp) : '--';
  const dbpStr = dbp != null ? String(dbp) : '--';
  return `${sbpStr}/${dbpStr}`;
}

function trendFor(key, valueStr) {
  // 非严格，仅用于展示标签
  const v = String(valueStr || '').trim();
  if (!v || v === '--') return { trend: 'ok', trendText: '' };
  if (key === 'bp') {
    const m = v.split('/');
    const sbp = Number(m[0]);
    const dbp = Number(m[1]);
    if ((sbp && sbp >= 140) || (dbp && dbp >= 90)) return { trend: 'up', trendText: '偏高' };
    if ((sbp && sbp < 90) || (dbp && dbp < 60)) return { trend: 'down', trendText: '偏低' };
    return { trend: 'ok', trendText: '稳定' };
  }
  if (key === 'hr') {
    const n = Number(v);
    if (n && n > 100) return { trend: 'up', trendText: '偏快' };
    if (n && n < 50) return { trend: 'down', trendText: '偏慢' };
    return { trend: 'ok', trendText: '稳定' };
  }
  if (key === 'glucose') {
    const n = Number(v);
    if (n && n >= 11.1) return { trend: 'up', trendText: '偏高' };
    if (n && n < 3.9) return { trend: 'down', trendText: '偏低' };
    return { trend: 'mid', trendText: '关注' };
  }
  if (key === 'weight') {
    const n = Number(v);
    if (Number.isNaN(n)) return { trend: 'ok', trendText: '' };
    if (n >= 38 || n < 35.5) return { trend: 'up', trendText: n < 35.5 ? '偏低' : '发热' };
    if (n >= 37.3) return { trend: 'mid', trendText: '偏高' };
    return { trend: 'ok', trendText: '正常' };
  }
  if (key === 'spo2') {
    const n = Number(v);
    if (n && n < 90) return { trend: 'up', trendText: '偏低' };
    if (n && n < 95) return { trend: 'mid', trendText: '需关注' };
    return { trend: 'ok', trendText: '达标' };
  }
  return { trend: 'ok', trendText: '' };
}

function formatMeasuredAt(d = new Date()) {
  const pad = (n) => String(n).padStart(2, '0');
  return (
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ` +
    `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  );
}

function parseBP(bpStr) {
  const s = String(bpStr || '').trim();
  const m = s.split('/');
  if (m.length !== 2) return {};
  const sbp = Number(m[0]);
  const dbp = Number(m[1]);
  return {
    sbp: Number.isFinite(sbp) ? sbp : undefined,
    dbp: Number.isFinite(dbp) ? dbp : undefined
  };
}

function toNum(v) {
  const n = Number(v);
  return Number.isFinite(n) ? n : undefined;
}

function buildDailyMeasurementPayload(normalized, glucoseType) {
  const d = normalized || {};
  const now = new Date();
  const { sbp, dbp } = parseBP(d.bp);

  const payload = {
    measuredAt: formatMeasuredAt(now)
  };

  if (sbp !== undefined) payload.sbp = sbp;
  if (dbp !== undefined) payload.dbp = dbp;

  const hr = toNum(d.hr);
  if (hr !== undefined) payload.hr = hr;

  // 当前项目中 key=weight 表示“体温”
  const temp = toNum(d.weight);
  if (temp !== undefined) payload.temp = temp;

  const spo2 = toNum(d.spo2);
  if (spo2 !== undefined) payload.spo2 = spo2;

  const glucose = toNum(d.glucose);
  if (glucose !== undefined) payload.glucose = glucose;
  
  // 添加血糖测量类型
  if (glucoseType) payload.glucoseType = glucoseType;

  const sleep = toNum(d.sleep);
  if (sleep !== undefined) payload.sleep = sleep;

  return payload;
}

export default {
  data() {
    return {
      statusText: '未初始化',
      discovering: false,
      discovered: [],

      boundDevice: { deviceId: '', name: '' },
      connected: false,
      connectedDeviceId: '',
      connectedName: '',

      // 选择到的 notify/write 特征（自动探测）
      serviceId: '',
      notifyCharId: '',
      writeCharId: '',

      // 接收缓冲（应对 20B 分包 / iOS 不支持 MTU）
      rxBuf: '',
      lastSyncMs: 0,
      negotiatedMtu: 23,

      lastPayloadText: '',
      parsed: {},
      parsedSummary: '',
      lastSyncAt: '',

      autoSync: true,
      autoSyncBackend: false,
      lastBackendSyncAt: '',

      // 写指令

      manual: {
        bp: '',
        hr: '',
        glucose: '',
        weight: '',
        spo2: '',
        sleep: ''
      },
      glucoseTypeOptions: ['空腹'],
      glucoseTypeIndex: 0
    };
  },

  onShow() {
    this.loadBound();
    this.lastSyncAt = getScopedStorageSync('device_last_sync_at','');
    this.lastBackendSyncAt = getScopedStorageSync('device_last_backend_sync_at','');

    const as = getScopedStorageSync('ble_auto_sync');
    if (typeof as === 'boolean') this.autoSync = as;
    const bs = getScopedStorageSync('ble_auto_sync_backend');
    if (typeof bs === 'boolean') this.autoSyncBackend = bs;

    this.restoreConnectedState();
    this.bindBleEvents();
  },

  onUnload() {
    this.unbindBleEvents();
    try {
      uni.offBluetoothDeviceFound && uni.offBluetoothDeviceFound();
    } catch (e) {}
    this.stopDiscovery();
    // 不强制断开（便于调试），但可按需开启
    // this.disconnect();
  },

  methods: {
    bindBleEvents() {
      // 设备页只订阅全局事件，用于展示；BLE 接收监听在 App 侧常驻
      this.unbindBleEvents();

      this._onBleLine = (payload) => {
        if (!payload) return;
        const line = payload.line || '';
        const parsed = payload.parsed || this.parsePayload(line);

        this.lastPayloadText = line;
        this.parsed = parsed || {};
        this.parsedSummary = this.buildSummary(this.parsed);

        if (payload.deviceId) this.connectedDeviceId = payload.deviceId;
        if (this.connectedDeviceId) this.connected = true;
        if (this.connected) this.statusText = '已连接（等待设备数据）';

        // 同步时间从 storage 里读（全局 hub 负责写）
        this.lastSyncAt = getScopedStorageSync('device_last_sync_at',this.lastSyncAt);
        this.lastBackendSyncAt = getScopedStorageSync('device_last_backend_sync_at',this.lastSyncAt);
      };

      this._onBleState = (st) => {
        if (!st) return;
        this.connected = !!st.connected;
        if (st.deviceId) this.connectedDeviceId = st.deviceId;
        if (this.connected) this.statusText = '已连接（等待设备数据）';
        else this.statusText = '未连接';
      };

      try {
        uni.$on && uni.$on('ble_ecb02_line', this._onBleLine);
        uni.$on && uni.$on('ble_state_change', this._onBleState);
      } catch (e) {}
    },

    unbindBleEvents() {
      try {
        if (this._onBleLine) {
          uni.$off && uni.$off('ble_ecb02_line', this._onBleLine);
          this._onBleLine = null;
        }
        if (this._onBleState) {
          uni.$off && uni.$off('ble_state_change', this._onBleState);
          this._onBleState = null;
        }
      } catch (e) {}
    },

    restoreConnectedState() {
      // 优先从 storage 恢复 UI（快速）
      const storedConnected = getScopedStorageSync('ble_connected');
      const storedId = getScopedStorageSync('ble_connected_deviceId','');
      if (storedConnected && storedId) {
        this.connected = true;
        this.connectedDeviceId = storedId;
        this.statusText = '已连接（等待设备数据）';
      }

      // 再尝试从系统查询，确保状态准确（失败也不影响）
      // #ifdef MP-WEIXIN
      try {
        uni.getConnectedBluetoothDevices({
          services: [ECB02.serviceId],
          success: (res) => {
            const list = (res && res.devices) || [];
            if (!list.length) return;
            const preferId = (this.boundDevice && this.boundDevice.deviceId) || this.connectedDeviceId;
            const hit = list.find((d) => d.deviceId === preferId) || list[0];
            if (hit) {
              this.connected = true;
              this.connectedDeviceId = hit.deviceId;
              this.connectedName = hit.name || this.connectedName;
              this.statusText = '已连接（等待设备数据）';
            }
          }
        });
      } catch (e) {}
      // #endif
    },

    loadBound() {
      const d = getScopedStorageSync('bound_ble_device');
      if (d && typeof d === 'object') {
        this.boundDevice = { ...this.boundDevice, ...d };
      }
    },

    initBluetooth() {
      // #ifdef MP-WEIXIN
      uni.openBluetoothAdapter({
        success: () => {
          this.statusText = '蓝牙已开启';
          uni.showToast({ title: '蓝牙已开启', icon: 'success' });

          // 监听发现设备
          try {
            uni.onBluetoothDeviceFound((res) => {
              const list = (res.devices || []).map((x) => ({
                deviceId: x.deviceId,
                name: x.name || x.localName || '未知设备',
                RSSI: x.RSSI
              }));
              this.mergeDiscovered(list);
            });
          } catch (e) {}

          // 自动尝试连接已绑定设备（如果用户希望）
          if (this.boundDevice.deviceId && !this.connected) {
            // 不自动连接，以免打扰；可按需改为自动
          }
        },
        fail: (err) => {
          this.statusText = '蓝牙初始化失败';
          uni.showToast({ title: err?.errMsg || '蓝牙不可用', icon: 'none' });
        }
      });
      // #endif
      // #ifndef MP-WEIXIN
      uni.showToast({ title: '仅微信小程序支持蓝牙功能', icon: 'none' });
      // #endif
    },

    // 生成一组示例数据，用于联调“身体数据”展示（不依赖真实设备）
    seedExample() {
      const sample = {
        bp: '128/82',
        hr: '66',
        glucose: '6.2',
        weight: '36.6',
        spo2: '97',
        sleep: '6.8'
      };
      this.writeToDailyMetrics(sample, '蓝牙设备');
    },

    toggleDiscovery() {
      if (this.discovering) return this.stopDiscovery();
      return this.startDiscovery();
    },

    startDiscovery() {
      // #ifdef MP-WEIXIN
      this.discovered = [];
      const start = (withFilter) => {
        const args = {
          allowDuplicatesKey: false,
          success: () => {
            this.discovering = true;
            this.statusText = '正在搜索设备…';
          },
          fail: (err) => {
            // 某些设备广播不带 service uuid，或低版本基础库不支持 services 过滤：失败则退化为全量扫描
            if (withFilter) return start(false);
            uni.showToast({ title: err?.errMsg || '搜索失败', icon: 'none' });
          }
        };
        if (withFilter) args.services = [ECB02.serviceId];
        uni.startBluetoothDevicesDiscovery(args);
      };
      start(true);
      // #endif
    },

    stopDiscovery() {
      // #ifdef MP-WEIXIN
      if (!this.discovering) return;
      uni.stopBluetoothDevicesDiscovery({
        complete: () => {
          this.discovering = false;
          if (!this.connected) this.statusText = '已停止搜索';
        }
      });
      // #endif
    },

    clearDiscovered() {
      this.discovered = [];
      uni.showToast({ title: '已清空', icon: 'success' });
    },

    mergeDiscovered(list) {
      const map = {};
      this.discovered.forEach((d) => (map[d.deviceId] = d));
      list.forEach((d) => {
        map[d.deviceId] = { ...map[d.deviceId], ...d };
      });
      // 转数组并按 RSSI 排序
      const arr = Object.values(map);
      arr.sort((a, b) => (b.RSSI || -999) - (a.RSSI || -999));
      this.discovered = arr;
    },

    connectDevice(d) {
      if (!d || !d.deviceId) return;
      this.stopDiscovery();
      this.statusText = '连接中…';

      // #ifdef MP-WEIXIN
      uni.createBLEConnection({
        deviceId: d.deviceId,
        success: () => {
          this.connected = true;
          this.connectedDeviceId = d.deviceId;
          this.connectedName = d.name || '设备';
          this.statusText = '已连接：' + this.connectedName;

          // 重置接收/解析状态
          this.rxBuf = '';
          this.lastPayloadText = '';
          this.parsed = {};
          this.parsedSummary = '';
          this.lastSyncMs = 0;

          // 绑定
          this.boundDevice = { deviceId: d.deviceId, name: this.connectedName };
          setScopedStorageSync('bound_ble_device', this.boundDevice);

          // Android 尝试协商 MTU（iOS 不支持，失败也没关系）
          this.trySetMtu(d.deviceId);

          // 获取服务/特征并开启通知
          this.discoverServicesAndChars(d.deviceId);
        },
        fail: (err) => {
          this.connected = false;
          this.statusText = '连接失败';
          uni.showToast({ title: err?.errMsg || '连接失败', icon: 'none' });
        }
      });
      // #endif
    },

    disconnect() {
      if (!this.connectedDeviceId) return;
      // #ifdef MP-WEIXIN
      uni.closeBLEConnection({
        deviceId: this.connectedDeviceId,
        complete: () => {
          this.connected = false;
          this.statusText = '已断开';
          this.connectedDeviceId = '';
          this.serviceId = '';
          this.notifyCharId = '';
          this.writeCharId = '';
        }
      });
      // #endif
    },

    discoverServicesAndChars(deviceId) {
      // #ifdef MP-WEIXIN
      uni.getBLEDeviceServices({
        deviceId,
        success: (res) => {
          const services = res.services || [];
          // 优先对齐 App 的固定服务 UUID（FFF0），否则退化为 primary
          const svc =
            services.find((s) => normUuid(s.uuid) === normUuid(ECB02.serviceId)) ||
            services.find((s) => s.isPrimary) ||
            services[0];
          if (!svc) {
            uni.showToast({ title: '未找到服务', icon: 'none' });
            return;
          }
          this.serviceId = svc.uuid;
          this.getChars(deviceId, svc.uuid);
        },
        fail: (err) => {
          uni.showToast({ title: err?.errMsg || '获取服务失败', icon: 'none' });
        }
      });
      // #endif
    },

    getChars(deviceId, serviceId) {
      // #ifdef MP-WEIXIN
      uni.getBLEDeviceCharacteristics({
        deviceId,
        serviceId,
        success: (res) => {
          const chars = res.characteristics || [];

          // 优先对齐 App 的固定特征 UUID（FFF1/FFF2），否则退化为 properties 推断
          const notify =
            chars.find((c) => normUuid(c.uuid) === normUuid(ECB02.notifyCharId)) ||
            chars.find((c) => c.properties && c.properties.notify) ||
            chars.find((c) => c.properties && c.properties.indicate);
          const write =
            chars.find((c) => normUuid(c.uuid) === normUuid(ECB02.writeCharId)) ||
            chars.find((c) => c.properties && c.properties.write) ||
            chars.find((c) => c.properties && c.properties.writeNoResponse);

          this.notifyCharId = notify ? notify.uuid : '';
          this.writeCharId = write ? write.uuid : '';

          if (!this.notifyCharId) {
            this.statusText = '已连接（无可用通知特征）';
            uni.showToast({ title: '该设备不支持通知上报', icon: 'none' });
            return;
          }

          this.enableNotify(deviceId, serviceId, this.notifyCharId);
        },
        fail: (err) => {
          uni.showToast({ title: err?.errMsg || '获取特征失败', icon: 'none' });
        }
      });
      // #endif
    },

    enableNotify(deviceId, serviceId, charId) {
      // #ifdef MP-WEIXIN
      // 数据接收监听已迁移到 App 全局（utils/ble/ecb02-hub.js），这里不再重复注册

      uni.notifyBLECharacteristicValueChange({
        deviceId,
        serviceId,
        characteristicId: charId,
        state: true,
        success: () => {
          this.statusText = '已连接（等待设备数据）';
          uni.showToast({ title: '已开启数据接收', icon: 'success' });
        },
        fail: (err) => {
          uni.showToast({ title: err?.errMsg || '开启通知失败', icon: 'none' });
        }
      });
      // #endif
    },

    trySetMtu(deviceId) {
      // #ifdef MP-WEIXIN
      // 仅 Android 5.1+ 支持；iOS 不支持。失败也没关系（
      // 仍可通过 \r\n 重组处理分包）。
      try {
        if (uni.setBLEMTU) {
          uni.setBLEMTU({
            deviceId,
            mtu: ECB02.mtu,
            success: () => {
              this.negotiatedMtu = ECB02.mtu;
            },
            fail: () => {},
            complete: () => {}
          });
        }
      } catch (e) {}
      // #endif
    },

    handleRxChunk(txt) {
      const chunk = String(txt || '');
      if (!chunk) return;

      // 拼接缓冲
      this.rxBuf += chunk;
      // 防止极端情况下无限增长
      if (this.rxBuf.length > 4096) this.rxBuf = this.rxBuf.slice(-2048);

      // 同时兼容 \r\n / \n
      while (true) {
        let idx = this.rxBuf.indexOf('\r\n');
        let step = 2;
        const idxN = this.rxBuf.indexOf('\n');
        if (idx < 0 && idxN >= 0) {
          idx = idxN;
          step = 1;
        }
        if (idx < 0) break;

        const line = this.rxBuf.slice(0, idx).trim();
        this.rxBuf = this.rxBuf.slice(idx + step);

        if (!line) continue;
        const parsed = this.parsePayload(line);
        if (parsed && Object.keys(parsed).length) {
          this.lastPayloadText = line;
          this.parsed = parsed;
          this.parsedSummary = this.buildSummary(parsed);

          // 节流：避免 150ms/次 的上报把本地写入刷爆
          const now = Date.now();
          if (this.autoSync && now - (this.lastSyncMs || 0) > 5000) {
            this.lastSyncMs = now;
            this.writeToDailyMetrics(parsed, '蓝牙设备', { silent: true });
          }
        } else {
          // 即便未识别，也记录最后一条文本，便于联调
          this.lastPayloadText = line;
          this.parsed = {};
          this.parsedSummary = '未识别到常用字段';
        }
      }
    },

    parsePayload(txt) {
      const t = (txt || '').trim();
      if (!t) return {};

      // 0) 设备协议：AT+DATA1=<heartrate>,<o2>,<temp>,<bp>,
      if (/^AT\+DATA1=/i.test(t)) {
        const parts = t.split(/[=,]/g); // ['AT+DATA1','hr','o2','temp','bp','']
        if (parts.length >= 5) {
          const hr = parts[1];
          const o2 = parts[2];
          const temp = parts[3];
          const bpRaw = parts[4];
          const norm = normalizeMetricData({ hr, o2, temp });
          const bpStr = mapSingleBpToBpString(bpRaw);
          if (bpStr) norm.bp = bpStr;
          return norm;
        }
      }

      // 1) JSON
      const j = safeJsonParse(t);
      if (j && typeof j === 'object') return normalizeMetricData(j);
      // 2) key=value
      const kv = parseKeyValue(t);
      return normalizeMetricData(kv);
    },

    buildSummary(obj) {
      const d = obj || {};
      const parts = [];
      if (d.bp) parts.push('血压 ' + d.bp);
      if (d.hr) parts.push('心率 ' + d.hr);
      if (d.glucose) parts.push('血糖 ' + d.glucose);
      if (d.weight) parts.push('体温 ' + d.weight);
      if (d.spo2) parts.push('血氧 ' + d.spo2 + '%');
      if (d.sleep) parts.push('睡眠 ' + d.sleep);
      return parts.length ? parts.join('，') : '—';
    },

    onAutoSyncChange(e) {
      this.autoSync = !!(e && e.detail && e.detail.value);
      setScopedStorageSync('ble_auto_sync', this.autoSync);
      uni.showToast({ title: this.autoSync ? '已开启自动同步' : '已关闭自动同步', icon: 'none' });
    },

    onAutoSyncBackendChange(e) {
      this.autoSyncBackend = !!(e && e.detail && e.detail.value);
      setScopedStorageSync('ble_auto_sync_backend', this.autoSyncBackend);
      uni.showToast({ title: this.autoSyncBackend ? '已开启后端同步' : '已关闭后端同步', icon: 'none' });
    },

    onGlucoseTypeChange(e) {
      if (e && e.detail && e.detail.value !== undefined) {
        this.glucoseTypeIndex = e.detail.value;
      }
    },



    // 通过写特征发送指令（串行队列 + 20B 分包）


    saveParsedToMetrics() {
      if (!this.parsed || !Object.keys(this.parsed).length) {
        return uni.showToast({ title: '暂无可写入的数据', icon: 'none' });
      }
      this.writeToDailyMetrics(this.parsed, '蓝牙设备', { syncBackend: true });
    },

    saveManualToMetrics() {
      const data = {
        bp: (this.manual.bp || '').trim(),
        hr: (this.manual.hr || '').trim(),
        glucose: (this.manual.glucose || '').trim(),
        weight: (this.manual.weight || '').trim(),
        spo2: (this.manual.spo2 || '').trim(),
        sleep: (this.manual.sleep || '').trim()
      };

      // 至少一个字段
      const hasAny = Object.keys(data).some((k) => data[k]);
      if (!hasAny) return uni.showToast({ title: '请先填写要补录的数据', icon: 'none' });

      // 体温校验（30~45℃）
      if (data.weight) {
        const n = Number(data.weight);
        if (Number.isNaN(n) || n < 30 || n > 45) {
          return uni.showToast({ title: '体温请填写 30~45℃ 范围内的数值', icon: 'none' });
        }
        data.weight = n.toFixed(1);
      }

      this.writeToDailyMetrics(data, '手动录入', { syncBackend: true });

      // 清空输入
      this.manual = { bp: '', hr: '', glucose: '', weight: '', spo2: '', sleep: '' };
    },

    writeToDailyMetrics(raw, sourceText, options) {
      const opts = options || {};
      const normalized = normalizeMetricData(raw);
      // 1) 写入“最新值”
      updateDailyMetricsLatest(
        {
          bp: normalized.bp,
          hr: normalized.hr,
          glucose: normalized.glucose,
          weight: normalized.weight,
          spo2: normalized.spo2,
          sleep: normalized.sleep
        },
        sourceText || '蓝牙设备',
        { silent: true }
      );

      // 2) 写入“趋势序列”（用于趋势页绘图）
      upsertTimeseriesFromPatch(
        {
          bp: normalized.bp,
          hr: normalized.hr,
          glucose: normalized.glucose,
          weight: normalized.weight,
          spo2: normalized.spo2,
          sleep: normalized.sleep
        },
        sourceText || '蓝牙设备'
      );

      const syncAt = nowHM();
     setScopedStorageSync('device_last_sync_at', syncAt);
      this.lastSyncAt = syncAt;

      // —— 同步到后端（仅在用户点击“写入/保存”时触发）
      if (opts.syncBackend) {
        // 获取血糖测量类型
        let glucoseType = 'FASTING'; // 固定为空腹血糖
        
        const payload = buildDailyMeasurementPayload(normalized, glucoseType);
        const hasAnyBackendField = ['sbp', 'dbp', 'hr', 'temp', 'spo2', 'glucose', 'sleep'].some(
          (k) => payload[k] !== undefined
        );

        if (!hasAnyBackendField) {
          if (!opts.silent) {
            uni.showToast({ title: '已保存（本地），无可同步字段', icon: 'none' });
          }
          return;
        }

        saveDailyMeasurement(payload)
          .then(() => {
            if (!opts.silent) uni.showToast({ title: '已保存并同步到后端', icon: 'success' });
          })
          .catch(() => {
            if (!opts.silent) uni.showToast({ title: '已保存（本地），后端同步失败', icon: 'none' });
          });

        return;
      }

      if (!opts.silent) {
        uni.showToast({ title: '已同步到身体数据', icon: 'success' });
      }
    }
  }
};
</script>

<style src="./devices.css"></style>
