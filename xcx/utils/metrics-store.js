// utils/metrics-store.js
//
// 目的：
// - 统一管理患者端「身体数据」的本地存储结构（最新值 + 趋势序列）。
// - 当真实数据写入（设备同步 / 手动录入 / 日常自测）后，趋势页可立即绘图。
//
// 存储键约定：
// - daily_metrics_latest：用于“我的/健康档案/随访关联”等页面展示“最新值”。
// - daily_metrics_timeseries：用于“趋势与建议”页面绘制周/月/年趋势。

// ------------------------------
// 元信息
// ------------------------------
import { getScopedStorageSync, setScopedStorageSync } from '@/utils/session.js';
export const METRIC_META = {
  bp: {
    key: 'bp',
    label: '血压',
    unit: 'mmHg',
    // 常见参考范围（用于可视化/标注，不替代医生建议）
    ref: { sbp: [90, 140], dbp: [60, 90] }
  },
  hr: {
    key: 'hr',
    label: '心率',
    unit: '次/分',
    ref: { value: [50, 100] }
  },
  glucose: {
    key: 'glucose',
    label: '血糖',
    unit: 'mmol/L',
    ref: { value: [3.9, 7.8] }
  },
  weight: {
    key: 'weight',
    label: '体温',
    unit: '℃',
    // 参考：正常 36.0~37.2；>=37.3 需关注；>=38.0 可能发热
    ref: { value: [36.0, 37.2] }
  },
  spo2: {
    key: 'spo2',
    label: '血氧',
    unit: '%',
    ref: { value: [95, 100] }
  },
  sleep: {
    key: 'sleep',
    label: '睡眠',
    unit: '小时',
    ref: { value: [6, 9] }
  }
};

export const METRIC_KEYS = Object.keys(METRIC_META);

// ------------------------------
// 基础工具
// ------------------------------

function pad2(n) {
  const s = String(n);
  return s.length === 1 ? '0' + s : s;
}

export function formatDate(d) {
  const dt = d instanceof Date ? d : new Date(d);
  return `${dt.getFullYear()}-${pad2(dt.getMonth() + 1)}-${pad2(dt.getDate())}`;
}

export function formatHM(d) {
  const dt = d instanceof Date ? d : new Date(d);
  return `${pad2(dt.getHours())}:${pad2(dt.getMinutes())}`;
}

function safeNum(v) {
  if (v === null || v === undefined) return null;
  const s = String(v).trim();
  if (!s || s === '--') return null;
  const n = Number(s);
  return Number.isFinite(n) ? n : null;
}

function clamp(n, lo, hi) {
  if (!Number.isFinite(n)) return n;
  return Math.max(lo, Math.min(hi, n));
}

export function parseBp(value) {
  // 支持："132/85"、"132-85"、"132 85"
  if (!value) return { sbp: null, dbp: null };
  const s = String(value).trim();
  const m = s.split(/[\/\-\s]+/).filter(Boolean);
  const sbp = safeNum(m[0]);
  const dbp = safeNum(m[1]);
  return { sbp, dbp };
}

export function formatBpDisplay(sbp, dbp) {
  if (!sbp && !dbp) return '--';
  if (sbp && dbp) return `${sbp}/${dbp}`;
  return `${sbp || '--'}/${dbp || '--'}`;
}

function getStorageSync(key, fallback) {
  try {
    const v = uni.getStorageSync(key);
    return v === undefined || v === null || v === '' ? fallback : v;
  } catch (e) {
    return fallback;
  }
}

function setStorageSync(key, value) {
  try {
    uni.setStorageSync(key, value);
  } catch (e) {}
}

// ------------------------------
// 最新值：daily_metrics_latest
// ------------------------------

export function buildDefaultLatestMetrics() {
  return [
    { key: 'bp', label: '血压', value: '--', unit: 'mmHg', trend: 'ok', trendText: '', hint: '来源：—' },
    { key: 'hr', label: '心率', value: '--', unit: '次/分', trend: 'ok', trendText: '', hint: '来源：—' },
    { key: 'glucose', label: '血糖', value: '--', unit: 'mmol/L', trend: 'mid', trendText: '关注', hint: '来源：—' },
    { key: 'weight', label: '体温', value: '--', unit: '℃', trend: 'ok', trendText: '', hint: '来源：—' },
    { key: 'spo2', label: '血氧', value: '--', unit: '%', trend: 'ok', trendText: '', hint: '来源：—' },
    { key: 'sleep', label: '睡眠', value: '--', unit: '小时', trend: 'mid', trendText: '偏少', hint: '来源：—' }
  ];
}

export function assessMetric(key, valueStr) {
  // 仅用于 UI 标签展示（非诊断）
  const v = String(valueStr || '').trim();
  if (!v || v === '--') return { trend: 'ok', trendText: '' };

  if (key === 'bp') {
    const { sbp, dbp } = parseBp(v);
    if ((sbp && sbp >= 140) || (dbp && dbp >= 90)) return { trend: 'up', trendText: '偏高' };
    if ((sbp && sbp < 90) || (dbp && dbp < 60)) return { trend: 'down', trendText: '偏低' };
    return { trend: 'ok', trendText: '稳定' };
  }
  if (key === 'hr') {
    const n = safeNum(v);
    if (n !== null && n > 100) return { trend: 'up', trendText: '偏快' };
    if (n !== null && n < 50) return { trend: 'down', trendText: '偏慢' };
    return { trend: 'ok', trendText: '稳定' };
  }
  if (key === 'glucose') {
    const n = safeNum(v);
    if (n !== null && n >= 11.1) return { trend: 'up', trendText: '偏高' };
    if (n !== null && n < 3.9) return { trend: 'down', trendText: '偏低' };
    return { trend: 'mid', trendText: '关注' };
  }
  if (key === 'weight') {
    // 体温（沿用 key=weight，兼容后端/旧版本字段）
    const n = safeNum(v);
    if (n !== null && n >= 38.0) return { trend: 'up', trendText: '发热' };
    if (n !== null && n >= 37.3) return { trend: 'mid', trendText: '偏高' };
    if (n !== null && n < 35.5) return { trend: 'down', trendText: '偏低' };
    return { trend: 'ok', trendText: '正常' };
  }

  if (key === 'spo2') {
    const n = safeNum(v);
    if (n !== null && n < 90) return { trend: 'up', trendText: '偏低' };
    if (n !== null && n < 95) return { trend: 'mid', trendText: '需关注' };
    return { trend: 'ok', trendText: '达标' };
  }
  // sleep 等仅做弱提示
  return { trend: 'ok', trendText: '' };
}

export function updateDailyMetricsLatest(patch, sourceText, options) {
  const opts = options || {};
  const hint = `来源：${sourceText || '—'}`;

  const old = getScopedStorageSync('daily_metrics_latest', {}) || {};
  const prevArr = Array.isArray(old.metrics) ? old.metrics : [];

  // 兼容：如果历史里还存在 steps，则替换为 spo2（不做值迁移）
  const normalizedPrev = prevArr.filter((m) => m && m.key !== 'steps');
  const base = normalizedPrev.length ? normalizedPrev : buildDefaultLatestMetrics();

  const next = base
    .filter((m) => METRIC_META[m.key])
    .map((m) => {
      const v = patch && patch[m.key] != null ? String(patch[m.key]).trim() : '';
      if (!v) return m;
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

  // 确保 key 顺序固定
  const ordered = METRIC_KEYS.map((k) => next.find((x) => x.key === k) || {
    key: k,
    label: METRIC_META[k].label,
    value: '--',
    unit: METRIC_META[k].unit,
    trend: 'ok',
    trendText: '',
    hint: '来源：—'
  });

  const now = new Date();
  const updatedAt = `${formatDate(now)} ${formatHM(now)}`;
 const payload = {
   ...old,
   metrics: ordered,
   updatedAt
 };
 setScopedStorageSync('daily_metrics_latest', payload);
 
 if (!opts.silent) {
   // 非强依赖：调用方可自行 Toast
 }
 
 return payload;
}

// 读取 daily_metrics_latest，并做一次性迁移/纠偏：
// - 旧版本仍包含 steps 指标时：自动移除。
// - 旧版本缺少 spo2 指标时：自动补齐为“--”。
// - 保障各页面“步数 → 血氧”替换一致，避免患者看到旧字段。
//
// 注意：必要时会回写 storage（写入不会改变业务含义，仅修正结构）。
export function getDailyMetricsLatest() {
  try {
   const latest = getScopedStorageSync('daily_metrics_latest', null);
    if (!latest || typeof latest !== 'object') return null;

    const arr = Array.isArray(latest.metrics) ? latest.metrics : [];
    const hasSteps = arr.some((m) => m && m.key === 'steps');
    const hasSpo2 = arr.some((m) => m && m.key === 'spo2');

    if (!hasSteps && hasSpo2) return latest;

    // 仅保留系统支持的 key 值，回写成新结构
    const patch = {};
    arr.forEach((m) => {
      if (!m || !m.key) return;
      if (!METRIC_META[m.key]) return;
      if (m.key === 'steps') return;
      const v = (m.value != null ? String(m.value) : '').trim();
      if (v && v !== '--') patch[m.key] = v;
    });

    return updateDailyMetricsLatest(patch, '系统整理', { silent: true });
  } catch (e) {
    return null;
  }
}

// ------------------------------
// 趋势：daily_metrics_timeseries
// ------------------------------

function emptyTimeseries() {
  const data = {};
  METRIC_KEYS.forEach((k) => {
    data[k] = [];
  });
  return {
    version: 1,
    updatedAt: '',
    data
  };
}

export function getTimeseries() {
 const raw = getScopedStorageSync('daily_metrics_timeseries', null);
  if (!raw || typeof raw !== 'object') return emptyTimeseries();
  const out = {
    version: raw.version || 1,
    updatedAt: raw.updatedAt || '',
    data: raw.data && typeof raw.data === 'object' ? raw.data : {}
  };
  METRIC_KEYS.forEach((k) => {
    if (!Array.isArray(out.data[k])) out.data[k] = [];
  });
  return out;
}

export function saveTimeseries(ts) {
  const now = new Date();
  const payload = {
    ...(ts || emptyTimeseries()),
    updatedAt: `${formatDate(now)} ${formatHM(now)}`
  };
  setScopedStorageSync('daily_metrics_timeseries', payload);
  return payload;
}

function upsertByDate(list, dateStr, item) {
  const arr = Array.isArray(list) ? list.slice() : [];
  const idx = arr.findIndex((x) => x && x.date === dateStr);
  if (idx >= 0) arr[idx] = { ...arr[idx], ...item, date: dateStr };
  else arr.push({ ...item, date: dateStr });
  arr.sort((a, b) => String(a.date).localeCompare(String(b.date)));
  // 控制长度：最多保留 420 条（约 14 个月）
  if (arr.length > 420) return arr.slice(arr.length - 420);
  return arr;
}

export function upsertTimeseriesFromPatch(patch, sourceText, dateStr) {
  const date = dateStr || formatDate(new Date());
  const ts = getTimeseries();

  // 血压：允许传入 "132/85" 或 {sbp,dbp}
  if (patch && patch.bp) {
    const { sbp, dbp } = typeof patch.bp === 'string' ? parseBp(patch.bp) : patch.bp;
    if (sbp || dbp) {
      ts.data.bp = upsertByDate(ts.data.bp, date, {
        sbp: sbp ?? null,
        dbp: dbp ?? null,
        source: sourceText || ''
      });
    }
  }

  const singleKeys = ['hr', 'glucose', 'weight', 'spo2', 'sleep'];
  singleKeys.forEach((k) => {
    if (!patch || patch[k] == null || patch[k] === '') return;
    const n = safeNum(patch[k]);
    if (n === null) return;
    ts.data[k] = upsertByDate(ts.data[k], date, { value: n, source: sourceText || '' });
  });

  return saveTimeseries(ts);
}

// ------------------------------
// 趋势提取（周 / 月 / 年）
// ------------------------------

function lastValueOnOrBefore(list, dateStr) {
  // list 需按 date 升序
  if (!Array.isArray(list) || !list.length) return null;
  let last = null;
  for (let i = 0; i < list.length; i++) {
    const it = list[i];
    if (!it || !it.date) continue;
    if (String(it.date) <= String(dateStr)) last = it;
    else break;
  }
  return last;
}

function dateAddDays(dateStr, delta) {
  const d = new Date(dateStr + 'T00:00:00');
  d.setDate(d.getDate() + delta);
  return formatDate(d);
}

function weekLabels(dates) {
  // 以“周一/二...”展示；如果无法计算则退化为 MM/DD
  const map = ['日', '一', '二', '三', '四', '五', '六'];
  return dates.map((ds) => {
    try {
      const d = new Date(ds + 'T00:00:00');
      return '周' + map[d.getDay()];
    } catch (e) {
      return ds.slice(5).replace('-', '/');
    }
  });
}

function monthLabels(dates) {
  return dates.map((ds) => {
    // MM/DD
    return ds.slice(5).replace('-', '/');
  });
}

function buildLast7Dates(todayStr) {
  const t = todayStr || formatDate(new Date());
  const dates = [];
  for (let i = 6; i >= 0; i--) dates.push(dateAddDays(t, -i));
  return dates;
}

function buildLast30SampleDates(todayStr) {
  // 10 个点：从 -27 天到今天，步长 3 天
  const t = todayStr || formatDate(new Date());
  const dates = [];
  for (let i = 27; i >= 0; i -= 3) dates.push(dateAddDays(t, -i));
  if (dates[dates.length - 1] !== t) dates.push(t);
  // 保障长度 10
  return dates.slice(-10);
}

function buildLast12MonthKeys(todayStr) {
  const t = todayStr || formatDate(new Date());
  const d = new Date(t + 'T00:00:00');
  const keys = [];
  // 从 11 个月前到本月
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
  // 若跨年，则显示 YY/MM；否则显示 M月
  const years = Array.from(new Set(keys.map((k) => k.slice(0, 4))));
  const crossYear = years.length > 1;
  return keys.map((k) => {
    const yy = k.slice(2, 4);
    const mm = k.slice(5, 7);
    if (crossYear) return `${yy}/${mm}`;
    return `${Number(mm)}月`;
  });
}

function statFromValues(values) {
  const arr = values.filter((x) => x !== null && x !== undefined && Number.isFinite(x));
  if (!arr.length) return { min: null, max: null, avg: null };
  const min = Math.min(...arr);
  const max = Math.max(...arr);
  const avg = arr.reduce((s, x) => s + x, 0) / arr.length;
  return { min, max, avg };
}

function trendDir(values, eps) {
  const arr = values.filter((x) => x !== null && x !== undefined && Number.isFinite(x));
  if (arr.length < 2) return 'flat';
  const a = arr[arr.length - 2];
  const b = arr[arr.length - 1];
  const d = b - a;
  const e = eps || 0;
  if (d > e) return 'up';
  if (d < -e) return 'down';
  return 'flat';
}

function epsFor(key) {
  if (key === 'hr') return 3;
  if (key === 'glucose') return 0.3;
  if (key === 'weight') return 0.1; // 体温变化一般以0.1℃为敏感
  if (key === 'spo2') return 1;
  if (key === 'sleep') return 0.2;
  return 0;
}

export function getMetricChartData(metricKey, timeRange, options) {
  const key = metricKey || 'bp';
  const range = timeRange || 'week';
  const opts = options || {};
  const today = opts.todayStr || formatDate(new Date());

  const ts = getTimeseries();
  const list = ts.data[key] || [];

  if (key === 'bp') {
    // bp list item: {date, sbp, dbp}
    let dates = [];
    if (range === 'week') dates = buildLast7Dates(today);
    else if (range === 'month') dates = buildLast30SampleDates(today);
    else {
      const monthKeys = buildLast12MonthKeys(today);
      // 按月聚合
      const sbpByMonth = {};
      const dbpByMonth = {};
      monthKeys.forEach((k) => {
        sbpByMonth[k] = [];
        dbpByMonth[k] = [];
      });
      list.forEach((it) => {
        if (!it || !it.date) return;
        const mk = String(it.date).slice(0, 7);
        if (!sbpByMonth[mk]) return;
        if (Number.isFinite(it.sbp)) sbpByMonth[mk].push(it.sbp);
        if (Number.isFinite(it.dbp)) dbpByMonth[mk].push(it.dbp);
      });
      const sbp = monthKeys.map((k) => {
        const arr = sbpByMonth[k] || [];
        return arr.length ? arr.reduce((s, x) => s + x, 0) / arr.length : null;
      });
      const dbp = monthKeys.map((k) => {
        const arr = dbpByMonth[k] || [];
        return arr.length ? arr.reduce((s, x) => s + x, 0) / arr.length : null;
      });
      const labels = monthKeyLabel(monthKeys);
      const latestIdx = Math.max(sbp.lastIndexOf(sbp.filter((x) => x !== null).slice(-1)[0]), dbp.lastIndexOf(dbp.filter((x) => x !== null).slice(-1)[0]));
      const latestSbp = latestIdx >= 0 ? sbp[latestIdx] : null;
      const latestDbp = latestIdx >= 0 ? dbp[latestIdx] : null;
      const statsS = statFromValues(sbp);
      const statsD = statFromValues(dbp);
      return {
        key,
        range,
        labels,
        series: { sbp, dbp },
        latest: { sbp: latestSbp, dbp: latestDbp },
        stats: {
          sbp: statsS,
          dbp: statsD,
          // 综合给一个用于展示的平均（收缩/舒张分别计算）
          avgDisplay: formatBpDisplay(statsS.avg ? Math.round(statsS.avg) : null, statsD.avg ? Math.round(statsD.avg) : null)
        },
        trend: trendDir(sbp, 5) === 'flat' ? trendDir(dbp, 3) : trendDir(sbp, 5),
        hasAny: sbp.some((x) => x !== null) || dbp.some((x) => x !== null)
      };
    }

    // week/month：按日/采样日期取“当日或此前最近值”
    const sbp = [];
    const dbp = [];
    dates.forEach((ds) => {
      const it = lastValueOnOrBefore(list, ds);
      sbp.push(it && Number.isFinite(it.sbp) ? it.sbp : null);
      dbp.push(it && Number.isFinite(it.dbp) ? it.dbp : null);
    });

    const labels = range === 'week' ? weekLabels(dates) : monthLabels(dates);
    const latestSbp = sbp.filter((x) => x !== null).slice(-1)[0] ?? null;
    const latestDbp = dbp.filter((x) => x !== null).slice(-1)[0] ?? null;
    const statsS = statFromValues(sbp);
    const statsD = statFromValues(dbp);
    return {
      key,
      range,
      labels,
      series: { sbp, dbp },
      latest: { sbp: latestSbp, dbp: latestDbp },
      stats: {
        sbp: statsS,
        dbp: statsD,
        avgDisplay: formatBpDisplay(statsS.avg ? Math.round(statsS.avg) : null, statsD.avg ? Math.round(statsD.avg) : null)
      },
      trend: trendDir(sbp, 5) === 'flat' ? trendDir(dbp, 3) : trendDir(sbp, 5),
      hasAny: sbp.some((x) => x !== null) || dbp.some((x) => x !== null)
    };
  }

  // 其它单值指标
  let dates = [];
  if (range === 'week') dates = buildLast7Dates(today);
  else if (range === 'month') dates = buildLast30SampleDates(today);
  else {
    const monthKeys = buildLast12MonthKeys(today);
    const byMonth = {};
    monthKeys.forEach((k) => (byMonth[k] = []));
    list.forEach((it) => {
      if (!it || !it.date) return;
      const mk = String(it.date).slice(0, 7);
      if (!byMonth[mk]) return;
      if (Number.isFinite(it.value)) byMonth[mk].push(it.value);
    });
    const values = monthKeys.map((k) => {
      const arr = byMonth[k] || [];
      return arr.length ? arr.reduce((s, x) => s + x, 0) / arr.length : null;
    });
    const labels = monthKeyLabel(monthKeys);
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

  const values = dates.map((ds) => {
    const it = lastValueOnOrBefore(list, ds);
    return it && Number.isFinite(it.value) ? it.value : null;
  });

  const labels = range === 'week' ? weekLabels(dates) : monthLabels(dates);
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

// ------------------------------
// 轻量趋势概览（用于“我的-趋势与建议”小图）
// ------------------------------

function percentMap(key, value) {
  // 将不同指标映射到 0~100，用于“柱状小趋势”
  const v = Number(value);
  if (!Number.isFinite(v)) return null;
  if (key === 'bp') {
    // 以收缩压 80~180 映射
    return clamp(((v - 80) / 100) * 100, 0, 100);
  }
  if (key === 'hr') return clamp(((v - 40) / 100) * 100, 0, 100);
  if (key === 'glucose') return clamp(((v - 3) / 12) * 100, 0, 100);
  if (key === 'weight') return clamp(((v - 35) / 5) * 100, 0, 100); // 体温：35~40℃映射到0~100
  if (key === 'spo2') return clamp(((v - 80) / 20) * 100, 0, 100);
  if (key === 'sleep') return clamp(((v - 0) / 10) * 100, 0, 100);
  return clamp(v, 0, 100);
}

export function getOverviewTrend(metricKey) {
  const key = metricKey || 'bp';
  const cd = getMetricChartData(key, 'week');
  if (!cd || !cd.hasAny) {
    return {
      key,
      points: [],
      tip: '暂无趋势数据。请先在“体征记录”录入，或在“设备绑定”同步。'
    };
  }

  let vals = [];
  if (key === 'bp') {
    vals = (cd.series && cd.series.sbp) ? cd.series.sbp : [];
  } else {
    vals = (cd.series && cd.series.values) ? cd.series.values : [];
  }
  const points = vals
    .map((x) => (x === null ? null : percentMap(key, x)))
    .map((x) => (x === null ? 0 : Math.round(x)));

  const tip = buildSimpleTip(key, cd);
  return { key, points, tip };
}

export function buildSimpleTip(metricKey, chartData) {
  const key = metricKey || 'bp';
  const cd = chartData || getMetricChartData(key, 'week');
  if (!cd || !cd.hasAny) return '暂无数据。';

  const meta = METRIC_META[key] || { label: '指标' };
  const trend = cd.trend || 'flat';

  if (key === 'bp') {
    const s = cd.latest && cd.latest.sbp ? Math.round(cd.latest.sbp) : null;
    const d = cd.latest && cd.latest.dbp ? Math.round(cd.latest.dbp) : null;
    const latestText = formatBpDisplay(s, d);
    if (trend === 'up') return `${meta.label}近一周有上升趋势，最近值 ${latestText}，建议按医嘱监测并关注波动。`;
    if (trend === 'down') return `${meta.label}近一周有下降趋势，最近值 ${latestText}，如伴头晕乏力请及时联系随访人员。`;
    return `${meta.label}近一周总体平稳，最近值 ${latestText}。`;
  }

  const latest = cd.latest;
  const latestText = latest === null ? '--' : (key === 'spo2' ? `${Math.round(latest)}%` : String(Number(latest).toFixed(key === 'glucose' ? 1 : 0)));
  if (trend === 'up') return `${meta.label}近一周略有上升，最近值 ${latestText}。`;
  if (trend === 'down') return `${meta.label}近一周略有下降，最近值 ${latestText}。`;
  return `${meta.label}近一周总体平稳，最近值 ${latestText}。`;
}
