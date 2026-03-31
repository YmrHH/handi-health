"use strict";
const common_vendor = require("../../../common/vendor.js");
const utils_metricsStore = require("../../../utils/metrics-store.js");
const _sfc_main = {
  data() {
    return {
      timeTabs: [
        { key: "week", label: "一周" },
        { key: "month", label: "一月" },
        { key: "year", label: "一年" }
      ],
      metricTabs: [
        { key: "bp", label: "血压" },
        { key: "hr", label: "心率" },
        { key: "glucose", label: "血糖" },
        { key: "weight", label: "体温" },
        { key: "spo2", label: "血氧" },
        { key: "sleep", label: "睡眠" }
      ],
      timeRange: "week",
      metricKey: "bp",
      chart: null,
      canvasId: "trTrendCanvas",
      canvasCssW: 0,
      canvasCssH: 0,
      canvasW: 0,
      canvasH: 0,
      dpr: 1
    };
  },
  computed: {
    currentMetric() {
      return utils_metricsStore.METRIC_META[this.metricKey] || { label: "指标", unit: "" };
    },
    timeRangeLabel() {
      return this.timeRange === "week" ? "近7天" : this.timeRange === "month" ? "近30天" : "近12个月";
    },
    hasData() {
      return !!(this.chart && this.chart.hasAny);
    },
    xLabels() {
      return this.chart && Array.isArray(this.chart.labels) ? this.chart.labels : [];
    },
    summaryValue() {
      if (!this.chart)
        return "--";
      if (!this.hasData)
        return "--";
      if (this.metricKey === "bp") {
        const s = this.chart.latest && this.chart.latest.sbp != null ? Math.round(this.chart.latest.sbp) : null;
        const d = this.chart.latest && this.chart.latest.dbp != null ? Math.round(this.chart.latest.dbp) : null;
        return utils_metricsStore.formatBpDisplay(s, d);
      }
      const v = this.chart.latest;
      if (v === null || v === void 0)
        return "--";
      if (this.metricKey === "glucose")
        return Number(v).toFixed(1);
      if (this.metricKey === "weight" || this.metricKey === "sleep")
        return Number(v).toFixed(1);
      if (this.metricKey === "spo2")
        return String(Math.round(v));
      return String(Math.round(v));
    },
    trendLevel() {
      if (!this.hasData)
        return "mid";
      if (this.metricKey === "bp") {
        const s = this.chart.latest && this.chart.latest.sbp != null ? Number(this.chart.latest.sbp) : null;
        const d = this.chart.latest && this.chart.latest.dbp != null ? Number(this.chart.latest.dbp) : null;
        if (s == null && d == null)
          return "mid";
        if (s != null && (s >= 140 || s < 90) || d != null && (d >= 90 || d < 60))
          return "high";
        if (s != null && s >= 130 || d != null && d >= 85)
          return "mid";
        return "low";
      }
      const v = this.chart.latest;
      if (v === null || v === void 0)
        return "mid";
      const n = Number(v);
      if (this.metricKey === "glucose") {
        if (n >= 11.1 || n < 3.9)
          return "high";
        if (n >= 7.8 || n < 4.4)
          return "mid";
        return "low";
      }
      if (this.metricKey === "hr") {
        if (n > 120 || n < 40)
          return "high";
        if (n > 100 || n < 50)
          return "mid";
        return "low";
      }
      if (this.metricKey === "spo2") {
        if (n < 90)
          return "high";
        if (n < 95)
          return "mid";
        return "low";
      }
      if (this.metricKey === "sleep") {
        if (n < 4)
          return "high";
        if (n < 6)
          return "mid";
        return "low";
      }
      return this.chart.trend === "up" || this.chart.trend === "down" ? "mid" : "low";
    },
    trendText() {
      if (!this.hasData)
        return "待生成";
      const lv = this.trendLevel;
      if (lv === "high")
        return "需要关注";
      if (lv === "mid")
        return "需留意";
      return "达标";
    },
    footnote() {
      if (!this.hasData)
        return this.timeRange === "year" ? "按月汇总" : this.timeRange === "month" ? "近30天采样" : "近7天";
      if (this.metricKey === "bp") {
        const s = this.chart.stats && this.chart.stats.sbp ? this.chart.stats.sbp : { avg: null };
        const d = this.chart.stats && this.chart.stats.dbp ? this.chart.stats.dbp : { avg: null };
        const avg = utils_metricsStore.formatBpDisplay(s.avg ? Math.round(s.avg) : null, d.avg ? Math.round(d.avg) : null);
        return `平均：${avg} · ${this.timeRangeLabel}`;
      }
      const st = this.chart.stats || { avg: null, min: null, max: null };
      const fmt = (x) => {
        if (x === null || x === void 0)
          return "--";
        if (this.metricKey === "glucose")
          return Number(x).toFixed(1);
        if (this.metricKey === "weight" || this.metricKey === "sleep")
          return Number(x).toFixed(1);
        return String(Math.round(x));
      };
      return `平均：${fmt(st.avg)} · 最高：${fmt(st.max)} · 最低：${fmt(st.min)}`;
    },
    weekAdvice() {
      if (!this.hasData)
        return "暂无足够数据，建议先完成本周监测与打卡。";
      const lv = this.trendLevel;
      const dir = this.chart.trend || "flat";
      if (this.metricKey === "bp") {
        if (lv === "high")
          return "血压可能存在异常波动。建议按医嘱规律用药，固定时间测量并记录；如持续异常或不适，请及时联系医生。";
        if (lv === "mid")
          return "血压存在一定波动。建议减少高盐饮食、避免熬夜，持续记录并留意高峰时段。";
        return "血压趋势较稳定。建议继续保持测量习惯与适度运动。";
      }
      if (this.metricKey === "glucose") {
        if (lv === "high")
          return "血糖可能异常。建议回顾饮食/用药与测量时间，按医嘱监测；如持续异常请咨询医生。";
        if (dir === "up")
          return "血糖近一周略有上升。建议减少高糖/高碳水摄入，餐后适量活动并保持监测。";
        return "血糖整体可控。建议保持规律饮食和监测频率。";
      }
      if (this.metricKey === "spo2") {
        if (lv === "high")
          return "血氧偏低需重点关注。建议优先核对测量姿势与手指状态，必要时复测；如持续偏低或伴明显气短胸闷，请及时联系医生。";
        if (lv === "mid")
          return "血氧略偏低。建议避免受凉和剧烈活动，关注呼吸不适并适当增加复测频次。";
        return "血氧达标。建议继续保持规律监测与适度活动。";
      }
      if (this.metricKey === "sleep") {
        if (lv === "high")
          return "近期睡眠明显偏少。建议减少晚间咖啡因摄入，固定入睡时间；如持续影响白天状态可咨询医生。";
        if (lv === "mid")
          return "近期睡眠偏少。建议规律作息、减少屏幕刺激，并尝试放松训练。";
        return "睡眠趋势尚可。建议保持规律作息并适度运动。";
      }
      if (this.metricKey === "hr") {
        if (lv === "high")
          return "心率可能异常。建议静息状态复测并记录；如伴胸闷心悸等不适，请及时联系医生。";
        if (dir === "up")
          return "心率略有上升。建议近期减少熬夜与刺激性饮品，保持放松与规律作息。";
        return "心率总体平稳。建议继续保持规律作息与适度运动。";
      }
      if (lv === "high")
        return "体温偏高需重点关注。建议复测确认，注意休息与补液；如持续发热或伴明显不适，请及时联系医生。";
      if (lv === "mid")
        return "体温略偏高。建议减少劳累，留意是否有受凉/感染迹象，并适当增加复测频次。";
      if (dir === "down")
        return "体温较平时偏低。若伴畏寒、乏力等不适，建议复测并注意保暖。";
      return "体温整体平稳。建议保持每日同一时间测量的习惯。";
    }
  },
  onShow() {
    this.initCanvas();
    this.refresh();
  },
  onLoad(options) {
    try {
      if (options && options.metric && utils_metricsStore.METRIC_META[options.metric]) {
        this.metricKey = options.metric;
      }
      if (options && options.range && ["week", "month", "year"].includes(options.range)) {
        this.timeRange = options.range;
      }
    } catch (e) {
    }
  },
  methods: {
    initCanvas() {
      try {
        const sys = common_vendor.index.getSystemInfoSync();
        const dpr = sys.pixelRatio || 1;
        const pagePad = common_vendor.index.upx2px(24 * 2);
        const cardPad = common_vendor.index.upx2px(24 * 2);
        const cssW = Math.max(260, sys.windowWidth - pagePad - cardPad);
        const cssH = common_vendor.index.upx2px(260);
        this.canvasCssW = Math.floor(cssW);
        this.canvasCssH = Math.floor(cssH);
        this.dpr = dpr;
        this.canvasW = Math.floor(cssW * dpr);
        this.canvasH = Math.floor(cssH * dpr);
      } catch (e) {
        this.canvasCssW = 300;
        this.canvasCssH = 130;
        this.dpr = 1;
        this.canvasW = 300;
        this.canvasH = 130;
      }
    },
    refresh() {
      this.chart = utils_metricsStore.getMetricChartData(this.metricKey, this.timeRange);
      this.$nextTick(() => this.drawChart());
    },
    setTimeRange(k) {
      this.timeRange = k;
      this.refresh();
    },
    setMetric(k) {
      this.metricKey = k;
      this.refresh();
    },
    goMonitor() {
      common_vendor.index.navigateTo({ url: "/pages/patient/monitor/monitor?tab=daily" });
    },
    goDevices() {
      common_vendor.index.navigateTo({ url: "/pages/patient/devices/devices" });
    },
    drawChart() {
      const ctx = common_vendor.index.createCanvasContext(this.canvasId, this);
      const cssW = this.canvasCssW;
      const cssH = this.canvasCssH;
      const dpr = this.dpr || 1;
      ctx.save();
      ctx.scale(dpr, dpr);
      ctx.clearRect(0, 0, cssW, cssH);
      if (!this.hasData || !this.chart) {
        ctx.restore();
        ctx.draw();
        return;
      }
      const labels = this.xLabels;
      const n = labels.length;
      if (!n) {
        ctx.restore();
        ctx.draw();
        return;
      }
      const padL = 12;
      const padR = 12;
      const padT = 10;
      const padB = 28;
      const plotW = cssW - padL - padR;
      const plotH = cssH - padT - padB;
      const xFor = (i) => {
        if (n === 1)
          return padL + plotW / 2;
        return padL + plotW * i / (n - 1);
      };
      const valuesAll = [];
      const seriesList = [];
      if (this.metricKey === "bp") {
        const sbp = this.chart.series && this.chart.series.sbp ? this.chart.series.sbp : [];
        const dbp = this.chart.series && this.chart.series.dbp ? this.chart.series.dbp : [];
        seriesList.push({ key: "sbp", values: sbp, color: "#5B6CFF" });
        seriesList.push({ key: "dbp", values: dbp, color: "#10B981" });
        sbp.forEach((v) => v != null && Number.isFinite(v) && valuesAll.push(v));
        dbp.forEach((v) => v != null && Number.isFinite(v) && valuesAll.push(v));
      } else {
        const arr = this.chart.series && this.chart.series.values ? this.chart.series.values : [];
        seriesList.push({ key: "v", values: arr, color: "#5B6CFF" });
        arr.forEach((v) => v != null && Number.isFinite(v) && valuesAll.push(v));
      }
      if (!valuesAll.length) {
        ctx.restore();
        ctx.draw();
        return;
      }
      let min = Math.min(...valuesAll);
      let max = Math.max(...valuesAll);
      if (min === max) {
        min -= 1;
        max += 1;
      }
      const pad = (max - min) * 0.15;
      min -= pad;
      max += pad;
      const meta = utils_metricsStore.METRIC_META[this.metricKey];
      if (meta && meta.ref) {
        if (this.metricKey === "bp") {
          const r = meta.ref;
          min = Math.min(min, r.sbp[0], r.dbp[0]);
          max = Math.max(max, r.sbp[1], r.dbp[1]);
        } else if (meta.ref.value) {
          min = Math.min(min, meta.ref.value[0]);
          max = Math.max(max, meta.ref.value[1]);
        }
      }
      const yFor = (v) => {
        return padT + (max - v) / (max - min) * plotH;
      };
      if (meta && meta.ref) {
        let low = null;
        let high = null;
        if (this.metricKey === "bp") {
          low = meta.ref.sbp[0];
          high = meta.ref.sbp[1];
        } else if (meta.ref.value) {
          low = meta.ref.value[0];
          high = meta.ref.value[1];
        }
        if (low != null && high != null) {
          const y1 = yFor(high);
          const y2 = yFor(low);
          ctx.setFillStyle("rgba(16,185,129,0.06)");
          ctx.fillRect(padL, y1, plotW, Math.max(0, y2 - y1));
        }
      }
      const drawSeries = (serie) => {
        const arr = serie.values || [];
        ctx.setStrokeStyle(serie.color);
        ctx.setLineWidth(2);
        ctx.beginPath();
        let started = false;
        for (let i = 0; i < n; i++) {
          const v = arr[i];
          if (v == null || !Number.isFinite(v)) {
            started = false;
            continue;
          }
          const x = xFor(i);
          const y = yFor(v);
          if (!started) {
            ctx.moveTo(x, y);
            started = true;
          } else {
            ctx.lineTo(x, y);
          }
        }
        ctx.stroke();
        ctx.setFillStyle(serie.color);
        for (let i = 0; i < n; i++) {
          const v = arr[i];
          if (v == null || !Number.isFinite(v))
            continue;
          const x = xFor(i);
          const y = yFor(v);
          ctx.beginPath();
          ctx.arc(x, y, 2.2, 0, Math.PI * 2);
          ctx.fill();
        }
      };
      seriesList.forEach(drawSeries);
      ctx.restore();
      ctx.draw();
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_vendor.f($data.timeTabs, (t, k0, i0) => {
      return {
        a: common_vendor.t(t.label),
        b: common_vendor.n(t.key === $data.timeRange ? "tr-tab-text--active" : ""),
        c: t.key,
        d: common_vendor.n(t.key === $data.timeRange ? "tr-tab--active" : ""),
        e: common_vendor.o(($event) => $options.setTimeRange(t.key), t.key)
      };
    }),
    b: common_vendor.f($data.metricTabs, (m, k0, i0) => {
      return {
        a: common_vendor.t(m.label),
        b: common_vendor.n(m.key === $data.metricKey ? "tr-metric-text--active" : ""),
        c: m.key,
        d: common_vendor.n(m.key === $data.metricKey ? "tr-metric--active" : ""),
        e: common_vendor.o(($event) => $options.setMetric(m.key), m.key)
      };
    }),
    c: common_vendor.t($options.currentMetric.label),
    d: common_vendor.t($options.timeRangeLabel),
    e: $data.metricKey === "bp"
  }, $data.metricKey === "bp" ? {} : {}, {
    f: common_vendor.t($options.summaryValue),
    g: common_vendor.t($options.currentMetric.unit),
    h: !$options.hasData
  }, !$options.hasData ? {
    i: common_vendor.o((...args) => $options.goMonitor && $options.goMonitor(...args)),
    j: common_vendor.o((...args) => $options.goDevices && $options.goDevices(...args))
  } : {
    k: $data.canvasId,
    l: $data.canvasId,
    m: common_vendor.s("width:" + $data.canvasCssW + "px;height:" + $data.canvasCssH + "px;"),
    n: $data.canvasW,
    o: $data.canvasH
  }, {
    p: $options.hasData
  }, $options.hasData ? {
    q: common_vendor.f($options.xLabels, (l, idx, i0) => {
      return {
        a: common_vendor.t(l),
        b: idx
      };
    })
  } : {}, {
    r: common_vendor.t($options.footnote),
    s: common_vendor.t($options.trendText),
    t: common_vendor.n("chip-" + $options.trendLevel),
    v: $data.timeRange === "week"
  }, $data.timeRange === "week" ? {
    w: common_vendor.t($options.weekAdvice)
  } : {});
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render]]);
wx.createPage(MiniProgramPage);
