"use strict";
require("../common/vendor.js");
const utils_session = require("./session.js");
const METRIC_META = {
  bp: {
    key: "bp",
    label: "血压",
    unit: "mmHg",
    // 常见参考范围（用于可视化/标注，不替代医生建议）
    ref: { sbp: [90, 140], dbp: [60, 90] }
  },
  hr: {
    key: "hr",
    label: "心率",
    unit: "次/分",
    ref: { value: [50, 100] }
  },
  glucose: {
    key: "glucose",
    label: "血糖",
    unit: "mmol/L",
    ref: { value: [3.9, 7.8] }
  },
  weight: {
    key: "weight",
    label: "体温",
    unit: "℃",
    // 参考：正常 36.0~37.2；>=37.3 需关注；>=38.0 可能发热
    ref: { value: [36, 37.2] }
  },
  spo2: {
    key: "spo2",
    label: "血氧",
    unit: "%",
    ref: { value: [95, 100] }
  },
  sleep: {
    key: "sleep",
    label: "睡眠",
    unit: "小时",
    ref: { value: [6, 9] }
  }
};
const METRIC_KEYS = Object.keys(METRIC_META);
function pad2(n) {
  const s = String(n);
  return s.length === 1 ? "0" + s : s;
}
function formatDate(d) {
  const dt = d instanceof Date ? d : new Date(d);
  return `${dt.getFullYear()}-${pad2(dt.getMonth() + 1)}-${pad2(dt.getDate())}`;
}
function formatHM(d) {
  const dt = d instanceof Date ? d : new Date(d);
  return `${pad2(dt.getHours())}:${pad2(dt.getMinutes())}`;
}
function safeNum(v) {
  if (v === null || v === void 0)
    return null;
  const s = String(v).trim();
  if (!s || s === "--")
    return null;
  const n = Number(s);
  return Number.isFinite(n) ? n : null;
}
function clamp(n, lo, hi) {
  if (!Number.isFinite(n))
    return n;
  return Math.max(lo, Math.min(hi, n));
}
function parseBp(value) {
  if (!value)
    return { sbp: null, dbp: null };
  const s = String(value).trim();
  const m = s.split(/[\/\-\s]+/).filter(Boolean);
  const sbp = safeNum(m[0]);
  const dbp = safeNum(m[1]);
  return { sbp, dbp };
}
function formatBpDisplay(sbp, dbp) {
  if (!sbp && !dbp)
    return "--";
  if (sbp && dbp)
    return `${sbp}/${dbp}`;
  return `${sbp || "--"}/${dbp || "--"}`;
}
function buildDefaultLatestMetrics() {
  return [
    { key: "bp", label: "血压", value: "--", unit: "mmHg", trend: "ok", trendText: "", hint: "来源：—" },
    { key: "hr", label: "心率", value: "--", unit: "次/分", trend: "ok", trendText: "", hint: "来源：—" },
    { key: "glucose", label: "血糖", value: "--", unit: "mmol/L", trend: "mid", trendText: "关注", hint: "来源：—" },
    { key: "weight", label: "体温", value: "--", unit: "℃", trend: "ok", trendText: "", hint: "来源：—" },
    { key: "spo2", label: "血氧", value: "--", unit: "%", trend: "ok", trendText: "", hint: "来源：—" },
    { key: "sleep", label: "睡眠", value: "--", unit: "小时", trend: "mid", trendText: "偏少", hint: "来源：—" }
  ];
}
function assessMetric(key, valueStr) {
  const v = String(valueStr || "").trim();
  if (!v || v === "--")
    return { trend: "ok", trendText: "" };
  if (key === "bp") {
    const { sbp, dbp } = parseBp(v);
    if (sbp && sbp >= 140 || dbp && dbp >= 90)
      return { trend: "up", trendText: "偏高" };
    if (sbp && sbp < 90 || dbp && dbp < 60)
      return { trend: "down", trendText: "偏低" };
    return { trend: "ok", trendText: "稳定" };
  }
  if (key === "hr") {
    const n = safeNum(v);
    if (n !== null && n > 100)
      return { trend: "up", trendText: "偏快" };
    if (n !== null && n < 50)
      return { trend: "down", trendText: "偏慢" };
    return { trend: "ok", trendText: "稳定" };
  }
  if (key === "glucose") {
    const n = safeNum(v);
    if (n !== null && n >= 11.1)
      return { trend: "up", trendText: "偏高" };
    if (n !== null && n < 3.9)
      return { trend: "down", trendText: "偏低" };
    return { trend: "mid", trendText: "关注" };
  }
  if (key === "weight") {
    const n = safeNum(v);
    if (n !== null && n >= 38)
      return { trend: "up", trendText: "发热" };
    if (n !== null && n >= 37.3)
      return { trend: "mid", trendText: "偏高" };
    if (n !== null && n < 35.5)
      return { trend: "down", trendText: "偏低" };
    return { trend: "ok", trendText: "正常" };
  }
  if (key === "spo2") {
    const n = safeNum(v);
    if (n !== null && n < 90)
      return { trend: "up", trendText: "偏低" };
    if (n !== null && n < 95)
      return { trend: "mid", trendText: "需关注" };
    return { trend: "ok", trendText: "达标" };
  }
  return { trend: "ok", trendText: "" };
}
function updateDailyMetricsLatest(patch, sourceText, options) {
  const opts = options || {};
  const hint = `来源：${sourceText || "—"}`;
  const old = utils_session.getScopedStorageSync("daily_metrics_latest", {}) || {};
  const prevArr = Array.isArray(old.metrics) ? old.metrics : [];
  const normalizedPrev = prevArr.filter((m) => m && m.key !== "steps");
  const base = normalizedPrev.length ? normalizedPrev : buildDefaultLatestMetrics();
  const next = base.filter((m) => METRIC_META[m.key]).map((m) => {
    const v = patch && patch[m.key] != null ? String(patch[m.key]).trim() : "";
    if (!v)
      return m;
    const t = assessMetric(m.key, v);
    return {
      ...m,
      label: METRIC_META[m.key].label,
      unit: METRIC_META[m.key].unit,
      value: v,
      hint,
      trend: t.trend,
      trendText: t.trendText || m.trendText
    };
  });
  const ordered = METRIC_KEYS.map((k) => next.find((x) => x.key === k) || {
    key: k,
    label: METRIC_META[k].label,
    value: "--",
    unit: METRIC_META[k].unit,
    trend: "ok",
    trendText: "",
    hint: "来源：—"
  });
  const now = /* @__PURE__ */ new Date();
  const updatedAt = `${formatDate(now)} ${formatHM(now)}`;
  const payload = {
    ...old,
    metrics: ordered,
    updatedAt
  };
  utils_session.setScopedStorageSync("daily_metrics_latest", payload);
  if (!opts.silent)
    ;
  return payload;
}
function getDailyMetricsLatest() {
  try {
    const latest = utils_session.getScopedStorageSync("daily_metrics_latest", null);
    if (!latest || typeof latest !== "object")
      return null;
    const arr = Array.isArray(latest.metrics) ? latest.metrics : [];
    const hasSteps = arr.some((m) => m && m.key === "steps");
    const hasSpo2 = arr.some((m) => m && m.key === "spo2");
    if (!hasSteps && hasSpo2)
      return latest;
    const patch = {};
    arr.forEach((m) => {
      if (!m || !m.key)
        return;
      if (!METRIC_META[m.key])
        return;
      if (m.key === "steps")
        return;
      const v = (m.value != null ? String(m.value) : "").trim();
      if (v && v !== "--")
        patch[m.key] = v;
    });
    return updateDailyMetricsLatest(patch, "系统整理", { silent: true });
  } catch (e) {
    return null;
  }
}
function emptyTimeseries() {
  const data = {};
  METRIC_KEYS.forEach((k) => {
    data[k] = [];
  });
  return {
    version: 1,
    updatedAt: "",
    data
  };
}
function getTimeseries() {
  const raw = utils_session.getScopedStorageSync("daily_metrics_timeseries", null);
  if (!raw || typeof raw !== "object")
    return emptyTimeseries();
  const out = {
    version: raw.version || 1,
    updatedAt: raw.updatedAt || "",
    data: raw.data && typeof raw.data === "object" ? raw.data : {}
  };
  METRIC_KEYS.forEach((k) => {
    if (!Array.isArray(out.data[k]))
      out.data[k] = [];
  });
  return out;
}
function saveTimeseries(ts) {
  const now = /* @__PURE__ */ new Date();
  const payload = {
    ...ts || emptyTimeseries(),
    updatedAt: `${formatDate(now)} ${formatHM(now)}`
  };
  utils_session.setScopedStorageSync("daily_metrics_timeseries", payload);
  return payload;
}
function upsertByDate(list, dateStr, item) {
  const arr = Array.isArray(list) ? list.slice() : [];
  const idx = arr.findIndex((x) => x && x.date === dateStr);
  if (idx >= 0)
    arr[idx] = { ...arr[idx], ...item, date: dateStr };
  else
    arr.push({ ...item, date: dateStr });
  arr.sort((a, b) => String(a.date).localeCompare(String(b.date)));
  if (arr.length > 420)
    return arr.slice(arr.length - 420);
  return arr;
}
function upsertTimeseriesFromPatch(patch, sourceText, dateStr) {
  const date = dateStr || formatDate(/* @__PURE__ */ new Date());
  const ts = getTimeseries();
  if (patch && patch.bp) {
    const { sbp, dbp } = typeof patch.bp === "string" ? parseBp(patch.bp) : patch.bp;
    if (sbp || dbp) {
      ts.data.bp = upsertByDate(ts.data.bp, date, {
        sbp: sbp ?? null,
        dbp: dbp ?? null,
        source: sourceText || ""
      });
    }
  }
  const singleKeys = ["hr", "glucose", "weight", "spo2", "sleep"];
  singleKeys.forEach((k) => {
    if (!patch || patch[k] == null || patch[k] === "")
      return;
    const n = safeNum(patch[k]);
    if (n === null)
      return;
    ts.data[k] = upsertByDate(ts.data[k], date, { value: n, source: sourceText || "" });
  });
  return saveTimeseries(ts);
}
function lastValueOnOrBefore(list, dateStr) {
  if (!Array.isArray(list) || !list.length)
    return null;
  let last = null;
  for (let i = 0; i < list.length; i++) {
    const it = list[i];
    if (!it || !it.date)
      continue;
    if (String(it.date) <= String(dateStr))
      last = it;
    else
      break;
  }
  return last;
}
function dateAddDays(dateStr, delta) {
  const d = /* @__PURE__ */ new Date(dateStr + "T00:00:00");
  d.setDate(d.getDate() + delta);
  return formatDate(d);
}
function weekLabels(dates) {
  const map = ["日", "一", "二", "三", "四", "五", "六"];
  return dates.map((ds) => {
    try {
      const d = /* @__PURE__ */ new Date(ds + "T00:00:00");
      return "周" + map[d.getDay()];
    } catch (e) {
      return ds.slice(5).replace("-", "/");
    }
  });
}
function monthLabels(dates) {
  return dates.map((ds) => {
    return ds.slice(5).replace("-", "/");
  });
}
function buildLast7Dates(todayStr) {
  const t = todayStr || formatDate(/* @__PURE__ */ new Date());
  const dates = [];
  for (let i = 6; i >= 0; i--)
    dates.push(dateAddDays(t, -i));
  return dates;
}
function buildLast30SampleDates(todayStr) {
  const t = todayStr || formatDate(/* @__PURE__ */ new Date());
  const dates = [];
  for (let i = 27; i >= 0; i -= 3)
    dates.push(dateAddDays(t, -i));
  if (dates[dates.length - 1] !== t)
    dates.push(t);
  return dates.slice(-10);
}
function buildLast12MonthKeys(todayStr) {
  const t = todayStr || formatDate(/* @__PURE__ */ new Date());
  const d = /* @__PURE__ */ new Date(t + "T00:00:00");
  const keys = [];
  for (let i = 11; i >= 0; i--) {
    const x = new Date(d);
    x.setMonth(x.getMonth() - i);
    const y = x.getFullYear();
    const m = pad2(x.getMonth() + 1);
    keys.push(`${y}-${m}`);
  }
  return keys;
}
function monthKeyLabel(keys) {
  const years = Array.from(new Set(keys.map((k) => k.slice(0, 4))));
  const crossYear = years.length > 1;
  return keys.map((k) => {
    const yy = k.slice(2, 4);
    const mm = k.slice(5, 7);
    if (crossYear)
      return `${yy}/${mm}`;
    return `${Number(mm)}月`;
  });
}
function statFromValues(values) {
  const arr = values.filter((x) => x !== null && x !== void 0 && Number.isFinite(x));
  if (!arr.length)
    return { min: null, max: null, avg: null };
  const min = Math.min(...arr);
  const max = Math.max(...arr);
  const avg = arr.reduce((s, x) => s + x, 0) / arr.length;
  return { min, max, avg };
}
function trendDir(values, eps) {
  const arr = values.filter((x) => x !== null && x !== void 0 && Number.isFinite(x));
  if (arr.length < 2)
    return "flat";
  const a = arr[arr.length - 2];
  const b = arr[arr.length - 1];
  const d = b - a;
  const e = eps || 0;
  if (d > e)
    return "up";
  if (d < -e)
    return "down";
  return "flat";
}
function epsFor(key) {
  if (key === "hr")
    return 3;
  if (key === "glucose")
    return 0.3;
  if (key === "weight")
    return 0.1;
  if (key === "spo2")
    return 1;
  if (key === "sleep")
    return 0.2;
  return 0;
}
function getMetricChartData(metricKey, timeRange, options) {
  const key = metricKey || "bp";
  const range = timeRange || "week";
  const opts = options || {};
  const today = opts.todayStr || formatDate(/* @__PURE__ */ new Date());
  const ts = getTimeseries();
  const list = ts.data[key] || [];
  if (key === "bp") {
    let dates2 = [];
    if (range === "week")
      dates2 = buildLast7Dates(today);
    else if (range === "month")
      dates2 = buildLast30SampleDates(today);
    else {
      const monthKeys = buildLast12MonthKeys(today);
      const sbpByMonth = {};
      const dbpByMonth = {};
      monthKeys.forEach((k) => {
        sbpByMonth[k] = [];
        dbpByMonth[k] = [];
      });
      list.forEach((it) => {
        if (!it || !it.date)
          return;
        const mk = String(it.date).slice(0, 7);
        if (!sbpByMonth[mk])
          return;
        if (Number.isFinite(it.sbp))
          sbpByMonth[mk].push(it.sbp);
        if (Number.isFinite(it.dbp))
          dbpByMonth[mk].push(it.dbp);
      });
      const sbp2 = monthKeys.map((k) => {
        const arr = sbpByMonth[k] || [];
        return arr.length ? arr.reduce((s, x) => s + x, 0) / arr.length : null;
      });
      const dbp2 = monthKeys.map((k) => {
        const arr = dbpByMonth[k] || [];
        return arr.length ? arr.reduce((s, x) => s + x, 0) / arr.length : null;
      });
      const labels3 = monthKeyLabel(monthKeys);
      const latestIdx = Math.max(sbp2.lastIndexOf(sbp2.filter((x) => x !== null).slice(-1)[0]), dbp2.lastIndexOf(dbp2.filter((x) => x !== null).slice(-1)[0]));
      const latestSbp2 = latestIdx >= 0 ? sbp2[latestIdx] : null;
      const latestDbp2 = latestIdx >= 0 ? dbp2[latestIdx] : null;
      const statsS2 = statFromValues(sbp2);
      const statsD2 = statFromValues(dbp2);
      return {
        key,
        range,
        labels: labels3,
        series: { sbp: sbp2, dbp: dbp2 },
        latest: { sbp: latestSbp2, dbp: latestDbp2 },
        stats: {
          sbp: statsS2,
          dbp: statsD2,
          // 综合给一个用于展示的平均（收缩/舒张分别计算）
          avgDisplay: formatBpDisplay(statsS2.avg ? Math.round(statsS2.avg) : null, statsD2.avg ? Math.round(statsD2.avg) : null)
        },
        trend: trendDir(sbp2, 5) === "flat" ? trendDir(dbp2, 3) : trendDir(sbp2, 5),
        hasAny: sbp2.some((x) => x !== null) || dbp2.some((x) => x !== null)
      };
    }
    const sbp = [];
    const dbp = [];
    dates2.forEach((ds) => {
      const it = lastValueOnOrBefore(list, ds);
      sbp.push(it && Number.isFinite(it.sbp) ? it.sbp : null);
      dbp.push(it && Number.isFinite(it.dbp) ? it.dbp : null);
    });
    const labels2 = range === "week" ? weekLabels(dates2) : monthLabels(dates2);
    const latestSbp = sbp.filter((x) => x !== null).slice(-1)[0] ?? null;
    const latestDbp = dbp.filter((x) => x !== null).slice(-1)[0] ?? null;
    const statsS = statFromValues(sbp);
    const statsD = statFromValues(dbp);
    return {
      key,
      range,
      labels: labels2,
      series: { sbp, dbp },
      latest: { sbp: latestSbp, dbp: latestDbp },
      stats: {
        sbp: statsS,
        dbp: statsD,
        avgDisplay: formatBpDisplay(statsS.avg ? Math.round(statsS.avg) : null, statsD.avg ? Math.round(statsD.avg) : null)
      },
      trend: trendDir(sbp, 5) === "flat" ? trendDir(dbp, 3) : trendDir(sbp, 5),
      hasAny: sbp.some((x) => x !== null) || dbp.some((x) => x !== null)
    };
  }
  let dates = [];
  if (range === "week")
    dates = buildLast7Dates(today);
  else if (range === "month")
    dates = buildLast30SampleDates(today);
  else {
    const monthKeys = buildLast12MonthKeys(today);
    const byMonth = {};
    monthKeys.forEach((k) => byMonth[k] = []);
    list.forEach((it) => {
      if (!it || !it.date)
        return;
      const mk = String(it.date).slice(0, 7);
      if (!byMonth[mk])
        return;
      if (Number.isFinite(it.value))
        byMonth[mk].push(it.value);
    });
    const values2 = monthKeys.map((k) => {
      const arr = byMonth[k] || [];
      return arr.length ? arr.reduce((s, x) => s + x, 0) / arr.length : null;
    });
    const labels2 = monthKeyLabel(monthKeys);
    const latest2 = values2.filter((x) => x !== null).slice(-1)[0] ?? null;
    const stats2 = statFromValues(values2);
    return {
      key,
      range,
      labels: labels2,
      series: { values: values2 },
      latest: latest2,
      stats: stats2,
      trend: trendDir(values2, epsFor(key)),
      hasAny: values2.some((x) => x !== null)
    };
  }
  const values = dates.map((ds) => {
    const it = lastValueOnOrBefore(list, ds);
    return it && Number.isFinite(it.value) ? it.value : null;
  });
  const labels = range === "week" ? weekLabels(dates) : monthLabels(dates);
  const latest = values.filter((x) => x !== null).slice(-1)[0] ?? null;
  const stats = statFromValues(values);
  return {
    key,
    range,
    labels,
    series: { values },
    latest,
    stats,
    trend: trendDir(values, epsFor(key)),
    hasAny: values.some((x) => x !== null)
  };
}
function percentMap(key, value) {
  const v = Number(value);
  if (!Number.isFinite(v))
    return null;
  if (key === "bp") {
    return clamp((v - 80) / 100 * 100, 0, 100);
  }
  if (key === "hr")
    return clamp((v - 40) / 100 * 100, 0, 100);
  if (key === "glucose")
    return clamp((v - 3) / 12 * 100, 0, 100);
  if (key === "weight")
    return clamp((v - 35) / 5 * 100, 0, 100);
  if (key === "spo2")
    return clamp((v - 80) / 20 * 100, 0, 100);
  if (key === "sleep")
    return clamp((v - 0) / 10 * 100, 0, 100);
  return clamp(v, 0, 100);
}
function getOverviewTrend(metricKey) {
  const key = metricKey || "bp";
  const cd = getMetricChartData(key, "week");
  if (!cd || !cd.hasAny) {
    return {
      key,
      points: [],
      tip: "暂无趋势数据。请先在“体征记录”录入，或在“设备绑定”同步。"
    };
  }
  let vals = [];
  if (key === "bp") {
    vals = cd.series && cd.series.sbp ? cd.series.sbp : [];
  } else {
    vals = cd.series && cd.series.values ? cd.series.values : [];
  }
  const points = vals.map((x) => x === null ? null : percentMap(key, x)).map((x) => x === null ? 0 : Math.round(x));
  const tip = buildSimpleTip(key, cd);
  return { key, points, tip };
}
function buildSimpleTip(metricKey, chartData) {
  const key = metricKey || "bp";
  const cd = chartData || getMetricChartData(key, "week");
  if (!cd || !cd.hasAny)
    return "暂无数据。";
  const meta = METRIC_META[key] || { label: "指标" };
  const trend = cd.trend || "flat";
  if (key === "bp") {
    const s = cd.latest && cd.latest.sbp ? Math.round(cd.latest.sbp) : null;
    const d = cd.latest && cd.latest.dbp ? Math.round(cd.latest.dbp) : null;
    const latestText2 = formatBpDisplay(s, d);
    if (trend === "up")
      return `${meta.label}近一周有上升趋势，最近值 ${latestText2}，建议按医嘱监测并关注波动。`;
    if (trend === "down")
      return `${meta.label}近一周有下降趋势，最近值 ${latestText2}，如伴头晕乏力请及时联系随访人员。`;
    return `${meta.label}近一周总体平稳，最近值 ${latestText2}。`;
  }
  const latest = cd.latest;
  const latestText = latest === null ? "--" : key === "spo2" ? `${Math.round(latest)}%` : String(Number(latest).toFixed(key === "glucose" ? 1 : 0));
  if (trend === "up")
    return `${meta.label}近一周略有上升，最近值 ${latestText}。`;
  if (trend === "down")
    return `${meta.label}近一周略有下降，最近值 ${latestText}。`;
  return `${meta.label}近一周总体平稳，最近值 ${latestText}。`;
}
exports.METRIC_META = METRIC_META;
exports.buildDefaultLatestMetrics = buildDefaultLatestMetrics;
exports.formatBpDisplay = formatBpDisplay;
exports.getDailyMetricsLatest = getDailyMetricsLatest;
exports.getMetricChartData = getMetricChartData;
exports.getOverviewTrend = getOverviewTrend;
exports.parseBp = parseBp;
exports.updateDailyMetricsLatest = updateDailyMetricsLatest;
exports.upsertTimeseriesFromPatch = upsertTimeseriesFromPatch;
