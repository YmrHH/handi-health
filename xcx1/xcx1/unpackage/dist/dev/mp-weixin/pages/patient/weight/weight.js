"use strict";
const common_vendor = require("../../../common/vendor.js");
const utils_metricsStore = require("../../../utils/metrics-store.js");
const _sfc_main = {
  data() {
    return {
      chart: null
    };
  },
  computed: {
    hasData() {
      return !!(this.chart && this.chart.hasAny);
    },
    xLabels() {
      return this.chart && Array.isArray(this.chart.labels) ? this.chart.labels : [];
    },
    currentValue() {
      if (!this.hasData)
        return "--";
      return Number(this.chart.latest).toFixed(1);
    },
    statusLevel() {
      if (!this.hasData)
        return "mid";
      const n = Number(this.chart.latest);
      if (Number.isNaN(n))
        return "mid";
      if (n >= 38 || n < 35.5)
        return "high";
      if (n >= 37.3)
        return "mid";
      return "low";
    },
    statusText() {
      if (!this.hasData)
        return "待生成";
      const n = Number(this.chart.latest);
      if (Number.isNaN(n))
        return "待生成";
      if (n < 35.5)
        return "偏低";
      const lv = this.statusLevel;
      if (lv === "high")
        return "发热";
      if (lv === "mid")
        return "偏高";
      return "正常";
    },
    chartPercents() {
      if (!this.hasData)
        return [];
      const arr = this.chart.series && Array.isArray(this.chart.series.values) ? this.chart.series.values : [];
      const lo = 35;
      const hi = 40;
      return arr.map((v) => {
        if (v == null)
          return 0;
        const p = (Number(v) - lo) / (hi - lo) * 100;
        return Math.max(5, Math.min(100, p));
      });
    },
    avgValue() {
      if (!this.hasData || !this.chart.stats)
        return "--";
      return Number(this.chart.stats.avg).toFixed(1);
    },
    maxValue() {
      if (!this.hasData || !this.chart.stats)
        return "--";
      return Number(this.chart.stats.max).toFixed(1);
    },
    minValue() {
      if (!this.hasData || !this.chart.stats)
        return "--";
      return Number(this.chart.stats.min).toFixed(1);
    }
  },
  onShow() {
    this.load();
  },
  methods: {
    load() {
      this.chart = utils_metricsStore.getMetricChartData("weight", "week");
    },
    goTrends() {
      common_vendor.index.navigateTo({ url: "/pages/patient/trends/trends?metric=weight&range=week" });
    },
    goDevices() {
      common_vendor.index.navigateTo({ url: "/pages/patient/devices/devices" });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_vendor.t($options.currentValue),
    b: common_vendor.t($options.statusText),
    c: common_vendor.n("chip-" + $options.statusLevel),
    d: common_vendor.o((...args) => $options.goTrends && $options.goTrends(...args)),
    e: !$options.hasData
  }, !$options.hasData ? {
    f: common_vendor.o((...args) => $options.goDevices && $options.goDevices(...args)),
    g: common_vendor.o((...args) => $options.goTrends && $options.goTrends(...args))
  } : {
    h: common_vendor.f($options.chartPercents, (p, idx, i0) => {
      return {
        a: p + "%",
        b: common_vendor.t($options.xLabels[idx]),
        c: idx
      };
    })
  }, {
    i: $options.hasData
  }, $options.hasData ? {
    j: common_vendor.t($options.avgValue),
    k: common_vendor.t($options.maxValue),
    l: common_vendor.t($options.minValue)
  } : {});
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render]]);
wx.createPage(MiniProgramPage);
