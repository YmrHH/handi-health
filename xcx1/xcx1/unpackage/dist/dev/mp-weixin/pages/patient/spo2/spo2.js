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
      return String(Math.round(this.chart.latest));
    },
    statusLevel() {
      if (!this.hasData)
        return "mid";
      const n = Number(this.chart.latest);
      if (n < 90)
        return "high";
      if (n < 95)
        return "mid";
      return "low";
    },
    statusText() {
      if (!this.hasData)
        return "待生成";
      const lv = this.statusLevel;
      if (lv === "high")
        return "需关注";
      if (lv === "mid")
        return "略偏低";
      return "达标";
    },
    chartPercents() {
      if (!this.hasData)
        return [];
      const arr = this.chart.series && Array.isArray(this.chart.series.values) ? this.chart.series.values : [];
      const lo = 85;
      const hi = 100;
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
      return Number(this.chart.stats.avg).toFixed(0);
    },
    maxValue() {
      if (!this.hasData || !this.chart.stats)
        return "--";
      return Number(this.chart.stats.max).toFixed(0);
    },
    minValue() {
      if (!this.hasData || !this.chart.stats)
        return "--";
      return Number(this.chart.stats.min).toFixed(0);
    }
  },
  onShow() {
    this.load();
  },
  methods: {
    load() {
      this.chart = utils_metricsStore.getMetricChartData("spo2", "week");
    },
    goTrends() {
      common_vendor.index.navigateTo({ url: "/pages/patient/trends/trends?metric=spo2&range=week" });
    },
    goMonitor() {
      common_vendor.index.navigateTo({ url: "/pages/patient/monitor/monitor?tab=daily" });
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
    f: common_vendor.o((...args) => $options.goMonitor && $options.goMonitor(...args)),
    g: common_vendor.o((...args) => $options.goDevices && $options.goDevices(...args))
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
//# sourceMappingURL=../../../../.sourcemap/mp-weixin/pages/patient/spo2/spo2.js.map
