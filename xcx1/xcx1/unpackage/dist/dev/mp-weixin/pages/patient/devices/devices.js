"use strict";
const common_vendor = require("../../../common/vendor.js");
const utils_metricsStore = require("../../../utils/metrics-store.js");
const utils_session = require("../../../utils/session.js");
const api_patient = require("../../../api/patient.js");
const ECB02 = {
  serviceId: "0000FFF0-0000-1000-8000-00805F9B34FB",
  notifyCharId: "0000FFF1-0000-1000-8000-00805F9B34FB",
  writeCharId: "0000FFF2-0000-1000-8000-00805F9B34FB",
  mtu: 500
};
const normUuid = (u) => String(u || "").toUpperCase();
function nowHM() {
  const d = /* @__PURE__ */ new Date();
  const pad = (n) => n < 10 ? "0" + n : "" + n;
  return pad(d.getHours()) + ":" + pad(d.getMinutes());
}
function safeJsonParse(txt) {
  try {
    return JSON.parse(txt);
  } catch (e) {
    return null;
  }
}
function parseKeyValue(txt) {
  const out = {};
  const cleaned = (txt || "").trim();
  if (!cleaned)
    return out;
  const pairs = cleaned.split(/[;\n,]/g);
  pairs.forEach((p) => {
    const m = p.split(/[:=]/);
    if (m.length >= 2) {
      const k = (m[0] || "").trim();
      const v = (m.slice(1).join(":") || "").trim();
      if (k)
        out[k] = v;
    }
  });
  return out;
}
function normalizeMetricData(obj) {
  const d = obj || {};
  const out = {};
  if (d.bp)
    out.bp = String(d.bp);
  if (!out.bp && (d.sbp || d.sys || d.bp_sys) && (d.dbp || d.dia || d.bp_dia)) {
    out.bp = `${d.sbp || d.sys || d.bp_sys}/${d.dbp || d.dia || d.bp_dia}`;
  }
  if (d.hr != null)
    out.hr = String(d.hr);
  if (!out.hr && d.heartRate != null)
    out.hr = String(d.heartRate);
  if (d.glucose != null)
    out.glucose = String(d.glucose);
  if (!out.glucose && d.bg != null)
    out.glucose = String(d.bg);
  if (d.weight != null)
    out.weight = String(d.weight);
  if (!out.weight && d.temperature != null)
    out.weight = String(d.temperature);
  if (!out.weight && d.temp != null)
    out.weight = String(d.temp);
  if (!out.weight && d.bodyTemp != null)
    out.weight = String(d.bodyTemp);
  if (!out.weight && d.body_temperature != null)
    out.weight = String(d.body_temperature);
  if (d.spo2 != null)
    out.spo2 = String(d.spo2);
  if (!out.spo2 && d.SpO2 != null)
    out.spo2 = String(d.SpO2);
  if (!out.spo2 && d.oxygen != null)
    out.spo2 = String(d.oxygen);
  if (!out.spo2 && d.o2 != null)
    out.spo2 = String(d.o2);
  if (!out.spo2 && d.bloodOxygen != null)
    out.spo2 = String(d.bloodOxygen);
  if (d.sleep != null)
    out.sleep = String(d.sleep);
  if (!out.sleep && d.sleepHours != null)
    out.sleep = String(d.sleepHours);
  if (d.bp_kpa != null)
    out.bp_kpa = String(d.bp_kpa);
  if (!out.bp_kpa && d.pressure_kpa != null)
    out.bp_kpa = String(d.pressure_kpa);
  if (!out.bp_kpa && d.pressure != null)
    out.bp_kpa = String(d.pressure);
  if (out.weight != null) {
    const n = Number(out.weight);
    if (!Number.isNaN(n))
      out.weight = n.toFixed(1);
  }
  if (out.bp_kpa != null) {
    const n = Number(out.bp_kpa);
    if (!Number.isNaN(n))
      out.bp_kpa = n.toFixed(1);
  }
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
function formatMeasuredAt(d = /* @__PURE__ */ new Date()) {
  const pad = (n) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}
function parseBP(bpStr) {
  const s = String(bpStr || "").trim();
  const m = s.split("/");
  if (m.length !== 2)
    return {};
  const sbp = Number(m[0]);
  const dbp = Number(m[1]);
  return {
    sbp: Number.isFinite(sbp) ? sbp : void 0,
    dbp: Number.isFinite(dbp) ? dbp : void 0
  };
}
function toNum(v) {
  const n = Number(v);
  return Number.isFinite(n) ? n : void 0;
}
function buildDailyMeasurementPayload(normalized, glucoseType) {
  const d = normalized || {};
  const now = /* @__PURE__ */ new Date();
  const { sbp, dbp } = parseBP(d.bp);
  const payload = {
    measuredAt: formatMeasuredAt(now)
  };
  if (sbp !== void 0)
    payload.sbp = sbp;
  if (dbp !== void 0)
    payload.dbp = dbp;
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
  if (glucoseType)
    payload.glucoseType = glucoseType;
  const sleep = toNum(d.sleep);
  if (sleep !== void 0)
    payload.sleep = sleep;
  return payload;
}
const _sfc_main = {
  data() {
    return {
      statusText: "未初始化",
      discovering: false,
      discovered: [],
      boundDevice: { deviceId: "", name: "" },
      connected: false,
      connectedDeviceId: "",
      connectedName: "",
      // 选择到的 notify/write 特征（自动探测）
      serviceId: "",
      notifyCharId: "",
      writeCharId: "",
      // 接收缓冲（应对 20B 分包 / iOS 不支持 MTU）
      rxBuf: "",
      lastSyncMs: 0,
      negotiatedMtu: 23,
      lastPayloadText: "",
      parsed: {},
      parsedSummary: "",
      lastSyncAt: "",
      autoSync: true,
      autoSyncBackend: false,
      lastBackendSyncAt: "",
      // 写指令
      manual: {
        bp: "",
        hr: "",
        glucose: "",
        weight: "",
        spo2: "",
        sleep: ""
      },
      glucoseTypeOptions: ["空腹"],
      glucoseTypeIndex: 0
    };
  },
  onShow() {
    this.loadBound();
    this.lastSyncAt = utils_session.getScopedStorageSync("device_last_sync_at", "");
    this.lastBackendSyncAt = utils_session.getScopedStorageSync("device_last_backend_sync_at", "");
    const as = utils_session.getScopedStorageSync("ble_auto_sync");
    if (typeof as === "boolean")
      this.autoSync = as;
    const bs = utils_session.getScopedStorageSync("ble_auto_sync_backend");
    if (typeof bs === "boolean")
      this.autoSyncBackend = bs;
    this.restoreConnectedState();
    this.bindBleEvents();
  },
  onUnload() {
    this.unbindBleEvents();
    try {
      common_vendor.index.offBluetoothDeviceFound && common_vendor.index.offBluetoothDeviceFound();
    } catch (e) {
    }
    this.stopDiscovery();
  },
  methods: {
    bindBleEvents() {
      this.unbindBleEvents();
      this._onBleLine = (payload) => {
        if (!payload)
          return;
        const line = payload.line || "";
        const parsed = payload.parsed || this.parsePayload(line);
        this.lastPayloadText = line;
        this.parsed = parsed || {};
        this.parsedSummary = this.buildSummary(this.parsed);
        if (payload.deviceId)
          this.connectedDeviceId = payload.deviceId;
        if (this.connectedDeviceId)
          this.connected = true;
        if (this.connected)
          this.statusText = "已连接（等待设备数据）";
        this.lastSyncAt = utils_session.getScopedStorageSync("device_last_sync_at", this.lastSyncAt);
        this.lastBackendSyncAt = utils_session.getScopedStorageSync("device_last_backend_sync_at", this.lastSyncAt);
      };
      this._onBleState = (st) => {
        if (!st)
          return;
        this.connected = !!st.connected;
        if (st.deviceId)
          this.connectedDeviceId = st.deviceId;
        if (this.connected)
          this.statusText = "已连接（等待设备数据）";
        else
          this.statusText = "未连接";
      };
      try {
        common_vendor.index.$on && common_vendor.index.$on("ble_ecb02_line", this._onBleLine);
        common_vendor.index.$on && common_vendor.index.$on("ble_state_change", this._onBleState);
      } catch (e) {
      }
    },
    unbindBleEvents() {
      try {
        if (this._onBleLine) {
          common_vendor.index.$off && common_vendor.index.$off("ble_ecb02_line", this._onBleLine);
          this._onBleLine = null;
        }
        if (this._onBleState) {
          common_vendor.index.$off && common_vendor.index.$off("ble_state_change", this._onBleState);
          this._onBleState = null;
        }
      } catch (e) {
      }
    },
    restoreConnectedState() {
      const storedConnected = utils_session.getScopedStorageSync("ble_connected");
      const storedId = utils_session.getScopedStorageSync("ble_connected_deviceId", "");
      if (storedConnected && storedId) {
        this.connected = true;
        this.connectedDeviceId = storedId;
        this.statusText = "已连接（等待设备数据）";
      }
      try {
        common_vendor.index.getConnectedBluetoothDevices({
          services: [ECB02.serviceId],
          success: (res) => {
            const list = res && res.devices || [];
            if (!list.length)
              return;
            const preferId = this.boundDevice && this.boundDevice.deviceId || this.connectedDeviceId;
            const hit = list.find((d) => d.deviceId === preferId) || list[0];
            if (hit) {
              this.connected = true;
              this.connectedDeviceId = hit.deviceId;
              this.connectedName = hit.name || this.connectedName;
              this.statusText = "已连接（等待设备数据）";
            }
          }
        });
      } catch (e) {
      }
    },
    loadBound() {
      const d = utils_session.getScopedStorageSync("bound_ble_device");
      if (d && typeof d === "object") {
        this.boundDevice = { ...this.boundDevice, ...d };
      }
    },
    initBluetooth() {
      common_vendor.index.openBluetoothAdapter({
        success: () => {
          this.statusText = "蓝牙已开启";
          common_vendor.index.showToast({ title: "蓝牙已开启", icon: "success" });
          try {
            common_vendor.index.onBluetoothDeviceFound((res) => {
              const list = (res.devices || []).map((x) => ({
                deviceId: x.deviceId,
                name: x.name || x.localName || "未知设备",
                RSSI: x.RSSI
              }));
              this.mergeDiscovered(list);
            });
          } catch (e) {
          }
          if (this.boundDevice.deviceId && !this.connected)
            ;
        },
        fail: (err) => {
          this.statusText = "蓝牙初始化失败";
          common_vendor.index.showToast({ title: (err == null ? void 0 : err.errMsg) || "蓝牙不可用", icon: "none" });
        }
      });
    },
    // 生成一组示例数据，用于联调“身体数据”展示（不依赖真实设备）
    seedExample() {
      const sample = {
        bp: "128/82",
        hr: "66",
        glucose: "6.2",
        weight: "36.6",
        spo2: "97",
        sleep: "6.8"
      };
      this.writeToDailyMetrics(sample, "蓝牙设备");
    },
    toggleDiscovery() {
      if (this.discovering)
        return this.stopDiscovery();
      return this.startDiscovery();
    },
    startDiscovery() {
      this.discovered = [];
      const start = (withFilter) => {
        const args = {
          allowDuplicatesKey: false,
          success: () => {
            this.discovering = true;
            this.statusText = "正在搜索设备…";
          },
          fail: (err) => {
            if (withFilter)
              return start(false);
            common_vendor.index.showToast({ title: (err == null ? void 0 : err.errMsg) || "搜索失败", icon: "none" });
          }
        };
        if (withFilter)
          args.services = [ECB02.serviceId];
        common_vendor.index.startBluetoothDevicesDiscovery(args);
      };
      start(true);
    },
    stopDiscovery() {
      if (!this.discovering)
        return;
      common_vendor.index.stopBluetoothDevicesDiscovery({
        complete: () => {
          this.discovering = false;
          if (!this.connected)
            this.statusText = "已停止搜索";
        }
      });
    },
    clearDiscovered() {
      this.discovered = [];
      common_vendor.index.showToast({ title: "已清空", icon: "success" });
    },
    mergeDiscovered(list) {
      const map = {};
      this.discovered.forEach((d) => map[d.deviceId] = d);
      list.forEach((d) => {
        map[d.deviceId] = { ...map[d.deviceId], ...d };
      });
      const arr = Object.values(map);
      arr.sort((a, b) => (b.RSSI || -999) - (a.RSSI || -999));
      this.discovered = arr;
    },
    connectDevice(d) {
      if (!d || !d.deviceId)
        return;
      this.stopDiscovery();
      this.statusText = "连接中…";
      common_vendor.index.createBLEConnection({
        deviceId: d.deviceId,
        success: () => {
          this.connected = true;
          this.connectedDeviceId = d.deviceId;
          this.connectedName = d.name || "设备";
          this.statusText = "已连接：" + this.connectedName;
          this.rxBuf = "";
          this.lastPayloadText = "";
          this.parsed = {};
          this.parsedSummary = "";
          this.lastSyncMs = 0;
          this.boundDevice = { deviceId: d.deviceId, name: this.connectedName };
          utils_session.setScopedStorageSync("bound_ble_device", this.boundDevice);
          this.trySetMtu(d.deviceId);
          this.discoverServicesAndChars(d.deviceId);
        },
        fail: (err) => {
          this.connected = false;
          this.statusText = "连接失败";
          common_vendor.index.showToast({ title: (err == null ? void 0 : err.errMsg) || "连接失败", icon: "none" });
        }
      });
    },
    disconnect() {
      if (!this.connectedDeviceId)
        return;
      common_vendor.index.closeBLEConnection({
        deviceId: this.connectedDeviceId,
        complete: () => {
          this.connected = false;
          this.statusText = "已断开";
          this.connectedDeviceId = "";
          this.serviceId = "";
          this.notifyCharId = "";
          this.writeCharId = "";
        }
      });
    },
    discoverServicesAndChars(deviceId) {
      common_vendor.index.getBLEDeviceServices({
        deviceId,
        success: (res) => {
          const services = res.services || [];
          const svc = services.find((s) => normUuid(s.uuid) === normUuid(ECB02.serviceId)) || services.find((s) => s.isPrimary) || services[0];
          if (!svc) {
            common_vendor.index.showToast({ title: "未找到服务", icon: "none" });
            return;
          }
          this.serviceId = svc.uuid;
          this.getChars(deviceId, svc.uuid);
        },
        fail: (err) => {
          common_vendor.index.showToast({ title: (err == null ? void 0 : err.errMsg) || "获取服务失败", icon: "none" });
        }
      });
    },
    getChars(deviceId, serviceId) {
      common_vendor.index.getBLEDeviceCharacteristics({
        deviceId,
        serviceId,
        success: (res) => {
          const chars = res.characteristics || [];
          const notify = chars.find((c) => normUuid(c.uuid) === normUuid(ECB02.notifyCharId)) || chars.find((c) => c.properties && c.properties.notify) || chars.find((c) => c.properties && c.properties.indicate);
          const write = chars.find((c) => normUuid(c.uuid) === normUuid(ECB02.writeCharId)) || chars.find((c) => c.properties && c.properties.write) || chars.find((c) => c.properties && c.properties.writeNoResponse);
          this.notifyCharId = notify ? notify.uuid : "";
          this.writeCharId = write ? write.uuid : "";
          if (!this.notifyCharId) {
            this.statusText = "已连接（无可用通知特征）";
            common_vendor.index.showToast({ title: "该设备不支持通知上报", icon: "none" });
            return;
          }
          this.enableNotify(deviceId, serviceId, this.notifyCharId);
        },
        fail: (err) => {
          common_vendor.index.showToast({ title: (err == null ? void 0 : err.errMsg) || "获取特征失败", icon: "none" });
        }
      });
    },
    enableNotify(deviceId, serviceId, charId) {
      common_vendor.index.notifyBLECharacteristicValueChange({
        deviceId,
        serviceId,
        characteristicId: charId,
        state: true,
        success: () => {
          this.statusText = "已连接（等待设备数据）";
          common_vendor.index.showToast({ title: "已开启数据接收", icon: "success" });
        },
        fail: (err) => {
          common_vendor.index.showToast({ title: (err == null ? void 0 : err.errMsg) || "开启通知失败", icon: "none" });
        }
      });
    },
    trySetMtu(deviceId) {
      try {
        if (common_vendor.index.setBLEMTU) {
          common_vendor.index.setBLEMTU({
            deviceId,
            mtu: ECB02.mtu,
            success: () => {
              this.negotiatedMtu = ECB02.mtu;
            },
            fail: () => {
            },
            complete: () => {
            }
          });
        }
      } catch (e) {
      }
    },
    handleRxChunk(txt) {
      const chunk = String(txt || "");
      if (!chunk)
        return;
      this.rxBuf += chunk;
      if (this.rxBuf.length > 4096)
        this.rxBuf = this.rxBuf.slice(-2048);
      while (true) {
        let idx = this.rxBuf.indexOf("\r\n");
        let step = 2;
        const idxN = this.rxBuf.indexOf("\n");
        if (idx < 0 && idxN >= 0) {
          idx = idxN;
          step = 1;
        }
        if (idx < 0)
          break;
        const line = this.rxBuf.slice(0, idx).trim();
        this.rxBuf = this.rxBuf.slice(idx + step);
        if (!line)
          continue;
        const parsed = this.parsePayload(line);
        if (parsed && Object.keys(parsed).length) {
          this.lastPayloadText = line;
          this.parsed = parsed;
          this.parsedSummary = this.buildSummary(parsed);
          const now = Date.now();
          if (this.autoSync && now - (this.lastSyncMs || 0) > 5e3) {
            this.lastSyncMs = now;
            this.writeToDailyMetrics(parsed, "蓝牙设备", { silent: true });
          }
        } else {
          this.lastPayloadText = line;
          this.parsed = {};
          this.parsedSummary = "未识别到常用字段";
        }
      }
    },
    parsePayload(txt) {
      const t = (txt || "").trim();
      if (!t)
        return {};
      if (/^AT\+DATA1=/i.test(t)) {
        const parts = t.split(/[=,]/g);
        if (parts.length >= 5) {
          const hr = parts[1];
          const o2 = parts[2];
          const temp = parts[3];
          const bpRaw = parts[4];
          const norm = normalizeMetricData({ hr, o2, temp });
          const bpStr = mapSingleBpToBpString(bpRaw);
          if (bpStr)
            norm.bp = bpStr;
          return norm;
        }
      }
      const j = safeJsonParse(t);
      if (j && typeof j === "object")
        return normalizeMetricData(j);
      const kv = parseKeyValue(t);
      return normalizeMetricData(kv);
    },
    buildSummary(obj) {
      const d = obj || {};
      const parts = [];
      if (d.bp)
        parts.push("血压 " + d.bp);
      if (d.hr)
        parts.push("心率 " + d.hr);
      if (d.glucose)
        parts.push("血糖 " + d.glucose);
      if (d.weight)
        parts.push("体温 " + d.weight);
      if (d.spo2)
        parts.push("血氧 " + d.spo2 + "%");
      if (d.sleep)
        parts.push("睡眠 " + d.sleep);
      return parts.length ? parts.join("，") : "—";
    },
    onAutoSyncChange(e) {
      this.autoSync = !!(e && e.detail && e.detail.value);
      utils_session.setScopedStorageSync("ble_auto_sync", this.autoSync);
      common_vendor.index.showToast({ title: this.autoSync ? "已开启自动同步" : "已关闭自动同步", icon: "none" });
    },
    onAutoSyncBackendChange(e) {
      this.autoSyncBackend = !!(e && e.detail && e.detail.value);
      utils_session.setScopedStorageSync("ble_auto_sync_backend", this.autoSyncBackend);
      common_vendor.index.showToast({ title: this.autoSyncBackend ? "已开启后端同步" : "已关闭后端同步", icon: "none" });
    },
    onGlucoseTypeChange(e) {
      if (e && e.detail && e.detail.value !== void 0) {
        this.glucoseTypeIndex = e.detail.value;
      }
    },
    // 通过写特征发送指令（串行队列 + 20B 分包）
    saveParsedToMetrics() {
      if (!this.parsed || !Object.keys(this.parsed).length) {
        return common_vendor.index.showToast({ title: "暂无可写入的数据", icon: "none" });
      }
      this.writeToDailyMetrics(this.parsed, "蓝牙设备", { syncBackend: true });
    },
    saveManualToMetrics() {
      const data = {
        bp: (this.manual.bp || "").trim(),
        hr: (this.manual.hr || "").trim(),
        glucose: (this.manual.glucose || "").trim(),
        weight: (this.manual.weight || "").trim(),
        spo2: (this.manual.spo2 || "").trim(),
        sleep: (this.manual.sleep || "").trim()
      };
      const hasAny = Object.keys(data).some((k) => data[k]);
      if (!hasAny)
        return common_vendor.index.showToast({ title: "请先填写要补录的数据", icon: "none" });
      if (data.weight) {
        const n = Number(data.weight);
        if (Number.isNaN(n) || n < 30 || n > 45) {
          return common_vendor.index.showToast({ title: "体温请填写 30~45℃ 范围内的数值", icon: "none" });
        }
        data.weight = n.toFixed(1);
      }
      this.writeToDailyMetrics(data, "手动录入", { syncBackend: true });
      this.manual = { bp: "", hr: "", glucose: "", weight: "", spo2: "", sleep: "" };
    },
    writeToDailyMetrics(raw, sourceText, options) {
      const opts = options || {};
      const normalized = normalizeMetricData(raw);
      utils_metricsStore.updateDailyMetricsLatest(
        {
          bp: normalized.bp,
          hr: normalized.hr,
          glucose: normalized.glucose,
          weight: normalized.weight,
          spo2: normalized.spo2,
          sleep: normalized.sleep
        },
        sourceText || "蓝牙设备",
        { silent: true }
      );
      utils_metricsStore.upsertTimeseriesFromPatch(
        {
          bp: normalized.bp,
          hr: normalized.hr,
          glucose: normalized.glucose,
          weight: normalized.weight,
          spo2: normalized.spo2,
          sleep: normalized.sleep
        },
        sourceText || "蓝牙设备"
      );
      const syncAt = nowHM();
      utils_session.setScopedStorageSync("device_last_sync_at", syncAt);
      this.lastSyncAt = syncAt;
      if (opts.syncBackend) {
        let glucoseType = "FASTING";
        const payload = buildDailyMeasurementPayload(normalized, glucoseType);
        const hasAnyBackendField = ["sbp", "dbp", "hr", "temp", "spo2", "glucose", "sleep"].some(
          (k) => payload[k] !== void 0
        );
        if (!hasAnyBackendField) {
          if (!opts.silent) {
            common_vendor.index.showToast({ title: "已保存（本地），无可同步字段", icon: "none" });
          }
          return;
        }
        api_patient.saveDailyMeasurement(payload).then(() => {
          if (!opts.silent)
            common_vendor.index.showToast({ title: "已保存并同步到后端", icon: "success" });
        }).catch(() => {
          if (!opts.silent)
            common_vendor.index.showToast({ title: "已保存（本地），后端同步失败", icon: "none" });
        });
        return;
      }
      if (!opts.silent) {
        common_vendor.index.showToast({ title: "已同步到身体数据", icon: "success" });
      }
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_vendor.t($data.connected ? "已连接" : "未连接"),
    b: common_vendor.n($data.connected ? "chip-ok" : "chip-idle"),
    c: common_vendor.t($data.boundDevice.name || "未绑定"),
    d: common_vendor.t($data.statusText),
    e: common_vendor.t($data.lastSyncAt || "暂无"),
    f: common_vendor.o((...args) => $options.initBluetooth && $options.initBluetooth(...args)),
    g: common_vendor.t($data.discovering ? "停止搜索" : "搜索设备"),
    h: common_vendor.o((...args) => $options.toggleDiscovery && $options.toggleDiscovery(...args)),
    i: common_vendor.o((...args) => $options.disconnect && $options.disconnect(...args)),
    j: common_vendor.n($data.connected ? "" : "dv-btn--disabled"),
    k: common_vendor.o((...args) => $options.seedExample && $options.seedExample(...args)),
    l: common_vendor.o((...args) => $options.clearDiscovered && $options.clearDiscovered(...args)),
    m: !$data.discovered.length
  }, !$data.discovered.length ? {} : {}, {
    n: common_vendor.f($data.discovered, (d, k0, i0) => {
      return {
        a: common_vendor.t(d.name || "未知设备"),
        b: common_vendor.t(d.deviceId),
        c: common_vendor.t(d.RSSI != null ? "RSSI " + d.RSSI : ""),
        d: common_vendor.o(($event) => $options.connectDevice(d), d.deviceId),
        e: d.deviceId
      };
    }),
    o: $data.connected
  }, $data.connected ? {
    p: common_vendor.o((...args) => $options.saveParsedToMetrics && $options.saveParsedToMetrics(...args))
  } : {}, {
    q: $data.connected
  }, $data.connected ? {
    r: common_vendor.t($data.autoSync ? "开" : "关"),
    s: $data.autoSync,
    t: common_vendor.o((...args) => $options.onAutoSyncChange && $options.onAutoSyncChange(...args)),
    v: common_vendor.t($data.autoSyncBackend ? "开" : "关"),
    w: $data.autoSyncBackend,
    x: common_vendor.o((...args) => $options.onAutoSyncBackendChange && $options.onAutoSyncBackendChange(...args)),
    y: common_vendor.t($data.lastBackendSyncAt || "暂无"),
    z: common_vendor.t($data.lastPayloadText || "等待设备上报…"),
    A: common_vendor.t($data.parsedSummary || "—")
  } : {}, {
    B: common_vendor.o((...args) => $options.saveManualToMetrics && $options.saveManualToMetrics(...args)),
    C: $data.manual.bp,
    D: common_vendor.o(($event) => $data.manual.bp = $event.detail.value),
    E: $data.manual.hr,
    F: common_vendor.o(($event) => $data.manual.hr = $event.detail.value),
    G: $data.manual.glucose,
    H: common_vendor.o(($event) => $data.manual.glucose = $event.detail.value),
    I: common_vendor.t($data.glucoseTypeOptions[$data.glucoseTypeIndex]),
    J: $data.glucoseTypeOptions,
    K: $data.glucoseTypeIndex,
    L: common_vendor.o((...args) => $options.onGlucoseTypeChange && $options.onGlucoseTypeChange(...args)),
    M: $data.manual.weight,
    N: common_vendor.o(($event) => $data.manual.weight = $event.detail.value),
    O: $data.manual.spo2,
    P: common_vendor.o(($event) => $data.manual.spo2 = $event.detail.value),
    Q: $data.manual.sleep,
    R: common_vendor.o(($event) => $data.manual.sleep = $event.detail.value)
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../../.sourcemap/mp-weixin/pages/patient/devices/devices.js.map
