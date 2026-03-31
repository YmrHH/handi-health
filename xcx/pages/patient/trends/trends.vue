<template>
  <view class="app-page tr">
    <view class="tr-head app-card app-card--glass">
      <text class="tr-title">趋势与建议</text>
      <text class="tr-sub">真实数据写入后自动绘制趋势图（周 / 月 / 年）</text>
    </view>

    <!-- 时间维度 -->
    <view class="tr-tabs app-card">
      <view
        v-for="t in timeTabs"
        :key="t.key"
        class="tr-tab"
        :class="t.key === timeRange ? 'tr-tab--active' : ''"
        @tap="setTimeRange(t.key)"
      >
        <text class="tr-tab-text" :class="t.key === timeRange ? 'tr-tab-text--active' : ''">{{ t.label }}</text>
      </view>
    </view>

    <!-- 指标选择 -->
    <scroll-view scroll-x class="tr-metrics">
      <view class="tr-metrics-row">
        <view
          v-for="m in metricTabs"
          :key="m.key"
          class="tr-metric"
          :class="m.key === metricKey ? 'tr-metric--active' : ''"
          @tap="setMetric(m.key)"
        >
          <text class="tr-metric-text" :class="m.key === metricKey ? 'tr-metric-text--active' : ''">{{ m.label }}</text>
        </view>
      </view>
    </scroll-view>

    <!-- 概览 + 图表 -->
    <view class="tr-summary app-card app-card--glass">
      <view class="flex-row items-start justify-between">
        <view class="flex-col">
          <text class="tr-sum-title">{{ currentMetric.label }} · {{ timeRangeLabel }}</text>
          <text class="tr-sum-sub">数据来源：设备绑定/手动录入/日常自测；趋势仅用于展示与随访管理。</text>

          <view v-if="metricKey === 'bp'" class="tr-legend flex-row items-center">
            <view class="tr-legend-item flex-row items-center">
              <view class="tr-legend-dot dot-sbp"></view>
              <text class="tr-legend-text">收缩压</text>
            </view>
            <view class="tr-legend-item flex-row items-center ml-12">
              <view class="tr-legend-dot dot-dbp"></view>
              <text class="tr-legend-text">舒张压</text>
            </view>
          </view>
        </view>

        <view class="tr-sum-right">
          <text class="tr-sum-val">{{ summaryValue }}</text>
          <text class="tr-sum-unit">{{ currentMetric.unit }}</text>
        </view>
      </view>

      <view class="tr-chart">
        <view class="tr-grid"></view>

        <!-- 无数据态 -->
        <view v-if="!hasData" class="tr-empty">
          <text class="tr-empty-title">暂无数据</text>
          <text class="tr-empty-sub">请先在「体征记录」录入，或在「设备绑定」同步数据。</text>
          <view class="tr-empty-actions flex-row">
            <view class="btn-secondary tr-empty-btn" @tap="goMonitor">去记录</view>
            <view class="btn-secondary tr-empty-btn ml-12" @tap="goDevices">去同步</view>
          </view>
        </view>

        <!-- 有数据：绘图 Canvas -->
        <canvas
          v-else
          class="tr-canvas"
          :canvas-id="canvasId"
          :id="canvasId"
          :style="'width:' + canvasCssW + 'px;height:' + canvasCssH + 'px;'"
          :width="canvasW"
          :height="canvasH"
        />

        <view v-if="hasData" class="tr-xlabels">
          <text v-for="(l, idx) in xLabels" :key="idx" class="tr-xlabel">{{ l }}</text>
        </view>
      </view>

      <view class="tr-foot flex-row items-center justify-between">
        <text class="tr-foot-left">{{ footnote }}</text>
        <view class="tr-chip" :class="'chip-' + trendLevel">{{ trendText }}</view>
      </view>
    </view>

    <!-- 周趋势：系统小建议 -->
    <view v-if="timeRange === 'week'" class="tr-advice app-card">
      <text class="tr-adv-title">系统小建议</text>
      <text class="tr-adv-text">{{ weekAdvice }}</text>
      <view class="tr-adv-tip">
        <text class="tr-adv-tip-text">提示：建议仅供参考；如出现明显不适或指标持续异常，请及时联系医生。</text>
      </view>
    </view>

    <view class="tr-note">
      <text class="tr-note-text">说明：一月趋势展示近30天（每3天采样1点）；一年趋势按月汇总展示。</text>
    </view>
  </view>
</template>

<script>
import { METRIC_META, getMetricChartData, formatBpDisplay } from '@/utils/metrics-store.js';

export default {
  data() {
    return {
      timeTabs: [
        { key: 'week', label: '一周' },
        { key: 'month', label: '一月' },
        { key: 'year', label: '一年' }
      ],
      metricTabs: [
        { key: 'bp', label: '血压' },
        { key: 'hr', label: '心率' },
        { key: 'glucose', label: '血糖' },
        { key: 'weight', label: '体温' },
        { key: 'spo2', label: '血氧' },
        { key: 'sleep', label: '睡眠' }
      ],
      timeRange: 'week',
      metricKey: 'bp',

      chart: null,

      canvasId: 'trTrendCanvas',
      canvasCssW: 0,
      canvasCssH: 0,
      canvasW: 0,
      canvasH: 0,
      dpr: 1
    };
  },

  computed: {
    currentMetric() {
      return METRIC_META[this.metricKey] || { label: '指标', unit: '' };
    },

    timeRangeLabel() {
      return this.timeRange === 'week' ? '近7天' : this.timeRange === 'month' ? '近30天' : '近12个月';
    },

    hasData() {
      return !!(this.chart && this.chart.hasAny);
    },

    xLabels() {
      return (this.chart && Array.isArray(this.chart.labels)) ? this.chart.labels : [];
    },

    summaryValue() {
      if (!this.chart) return '--';
      if (!this.hasData) return '--';

      if (this.metricKey === 'bp') {
        const s = this.chart.latest && this.chart.latest.sbp != null ? Math.round(this.chart.latest.sbp) : null;
        const d = this.chart.latest && this.chart.latest.dbp != null ? Math.round(this.chart.latest.dbp) : null;
        return formatBpDisplay(s, d);
      }

      const v = this.chart.latest;
      if (v === null || v === undefined) return '--';
      if (this.metricKey === 'glucose') return Number(v).toFixed(1);
      if (this.metricKey === 'weight' || this.metricKey === 'sleep') return Number(v).toFixed(1);
      if (this.metricKey === 'spo2') return String(Math.round(v));
      return String(Math.round(v));
    },

    trendLevel() {
      // green=low(达标), orange=mid(需关注), red=high(异常)
      if (!this.hasData) return 'mid';

      if (this.metricKey === 'bp') {
        const s = this.chart.latest && this.chart.latest.sbp != null ? Number(this.chart.latest.sbp) : null;
        const d = this.chart.latest && this.chart.latest.dbp != null ? Number(this.chart.latest.dbp) : null;
        if (s == null && d == null) return 'mid';
        if ((s != null && (s >= 140 || s < 90)) || (d != null && (d >= 90 || d < 60))) return 'high';
        if ((s != null && s >= 130) || (d != null && d >= 85)) return 'mid';
        return 'low';
      }

      const v = this.chart.latest;
      if (v === null || v === undefined) return 'mid';
      const n = Number(v);

      if (this.metricKey === 'glucose') {
        if (n >= 11.1 || n < 3.9) return 'high';
        if (n >= 7.8 || n < 4.4) return 'mid';
        return 'low';
      }
      if (this.metricKey === 'hr') {
        if (n > 120 || n < 40) return 'high';
        if (n > 100 || n < 50) return 'mid';
        return 'low';
      }
      if (this.metricKey === 'spo2') {
        if (n < 90) return 'high';
        if (n < 95) return 'mid';
        return 'low';
      }
      if (this.metricKey === 'sleep') {
        if (n < 4) return 'high';
        if (n < 6) return 'mid';
        return 'low';
      }

      // weight：默认不判“异常”，只提示趋势
      return this.chart.trend === 'up' || this.chart.trend === 'down' ? 'mid' : 'low';
    },

    trendText() {
      if (!this.hasData) return '待生成';
      const lv = this.trendLevel;
      if (lv === 'high') return '需要关注';
      if (lv === 'mid') return '需留意';
      return '达标';
    },

    footnote() {
      if (!this.hasData) return this.timeRange === 'year' ? '按月汇总' : this.timeRange === 'month' ? '近30天采样' : '近7天';

      if (this.metricKey === 'bp') {
        const s = this.chart.stats && this.chart.stats.sbp ? this.chart.stats.sbp : { avg: null };
        const d = this.chart.stats && this.chart.stats.dbp ? this.chart.stats.dbp : { avg: null };
        const avg = formatBpDisplay(s.avg ? Math.round(s.avg) : null, d.avg ? Math.round(d.avg) : null);
        return `平均：${avg} · ${this.timeRangeLabel}`;
      }

      const st = this.chart.stats || { avg: null, min: null, max: null };
      const fmt = (x) => {
        if (x === null || x === undefined) return '--';
        if (this.metricKey === 'glucose') return Number(x).toFixed(1);
        if (this.metricKey === 'weight' || this.metricKey === 'sleep') return Number(x).toFixed(1);
        return String(Math.round(x));
      };
      return `平均：${fmt(st.avg)} · 最高：${fmt(st.max)} · 最低：${fmt(st.min)}`;
    },

    weekAdvice() {
      if (!this.hasData) return '暂无足够数据，建议先完成本周监测与打卡。';

      const lv = this.trendLevel;
      const dir = this.chart.trend || 'flat';

      if (this.metricKey === 'bp') {
        if (lv === 'high') return '血压可能存在异常波动。建议按医嘱规律用药，固定时间测量并记录；如持续异常或不适，请及时联系医生。';
        if (lv === 'mid') return '血压存在一定波动。建议减少高盐饮食、避免熬夜，持续记录并留意高峰时段。';
        return '血压趋势较稳定。建议继续保持测量习惯与适度运动。';
      }
      if (this.metricKey === 'glucose') {
        if (lv === 'high') return '血糖可能异常。建议回顾饮食/用药与测量时间，按医嘱监测；如持续异常请咨询医生。';
        if (dir === 'up') return '血糖近一周略有上升。建议减少高糖/高碳水摄入，餐后适量活动并保持监测。';
        return '血糖整体可控。建议保持规律饮食和监测频率。';
      }
      if (this.metricKey === 'spo2') {
        if (lv === 'high') return '血氧偏低需重点关注。建议优先核对测量姿势与手指状态，必要时复测；如持续偏低或伴明显气短胸闷，请及时联系医生。';
        if (lv === 'mid') return '血氧略偏低。建议避免受凉和剧烈活动，关注呼吸不适并适当增加复测频次。';
        return '血氧达标。建议继续保持规律监测与适度活动。';
      }
      if (this.metricKey === 'sleep') {
        if (lv === 'high') return '近期睡眠明显偏少。建议减少晚间咖啡因摄入，固定入睡时间；如持续影响白天状态可咨询医生。';
        if (lv === 'mid') return '近期睡眠偏少。建议规律作息、减少屏幕刺激，并尝试放松训练。';
        return '睡眠趋势尚可。建议保持规律作息并适度运动。';
      }
      if (this.metricKey === 'hr') {
        if (lv === 'high') return '心率可能异常。建议静息状态复测并记录；如伴胸闷心悸等不适，请及时联系医生。';
        if (dir === 'up') return '心率略有上升。建议近期减少熬夜与刺激性饮品，保持放松与规律作息。';
        return '心率总体平稳。建议继续保持规律作息与适度运动。';
      }
      // 体温（沿用 key=weight）
      if (lv === 'high') return '体温偏高需重点关注。建议复测确认，注意休息与补液；如持续发热或伴明显不适，请及时联系医生。';
      if (lv === 'mid') return '体温略偏高。建议减少劳累，留意是否有受凉/感染迹象，并适当增加复测频次。';
      if (dir === 'down') return '体温较平时偏低。若伴畏寒、乏力等不适，建议复测并注意保暖。';
      return '体温整体平稳。建议保持每日同一时间测量的习惯。';
    }
  },

  onShow() {
    this.initCanvas();
    this.refresh();
  },

  onLoad(options) {
    try {
      if (options && options.metric && METRIC_META[options.metric]) {
        this.metricKey = options.metric;
      }
      if (options && options.range && ['week', 'month', 'year'].includes(options.range)) {
        this.timeRange = options.range;
      }
    } catch (e) {}
  },

  methods: {
    initCanvas() {
      try {
        const sys = uni.getSystemInfoSync();
        const dpr = sys.pixelRatio || 1;
        const pagePad = uni.upx2px(24 * 2);
        const cardPad = uni.upx2px(24 * 2);
        const cssW = Math.max(260, sys.windowWidth - pagePad - cardPad);
        const cssH = uni.upx2px(260);
        this.canvasCssW = Math.floor(cssW);
        this.canvasCssH = Math.floor(cssH);
        this.dpr = dpr;
        this.canvasW = Math.floor(cssW * dpr);
        this.canvasH = Math.floor(cssH * dpr);
      } catch (e) {
        // 兜底
        this.canvasCssW = 300;
        this.canvasCssH = 130;
        this.dpr = 1;
        this.canvasW = 300;
        this.canvasH = 130;
      }
    },

    refresh() {
      this.chart = getMetricChartData(this.metricKey, this.timeRange);
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
      uni.navigateTo({ url: '/pages/patient/monitor/monitor?tab=daily' });
    },

    goDevices() {
      uni.navigateTo({ url: '/pages/patient/devices/devices' });
    },

    drawChart() {
      const ctx = uni.createCanvasContext(this.canvasId, this);
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
        if (n === 1) return padL + plotW / 2;
        return padL + (plotW * i) / (n - 1);
      };

      // 收集数值以确定 Y 轴范围
      const valuesAll = [];
      const seriesList = [];

      if (this.metricKey === 'bp') {
        const sbp = (this.chart.series && this.chart.series.sbp) ? this.chart.series.sbp : [];
        const dbp = (this.chart.series && this.chart.series.dbp) ? this.chart.series.dbp : [];
        seriesList.push({ key: 'sbp', values: sbp, color: '#5B6CFF' });
        seriesList.push({ key: 'dbp', values: dbp, color: '#10B981' });
        sbp.forEach((v) => v != null && Number.isFinite(v) && valuesAll.push(v));
        dbp.forEach((v) => v != null && Number.isFinite(v) && valuesAll.push(v));
      } else {
        const arr = (this.chart.series && this.chart.series.values) ? this.chart.series.values : [];
        seriesList.push({ key: 'v', values: arr, color: '#5B6CFF' });
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

      // 将参考范围纳入可视区
      const meta = METRIC_META[this.metricKey];
      if (meta && meta.ref) {
        if (this.metricKey === 'bp') {
          const r = meta.ref;
          min = Math.min(min, r.sbp[0], r.dbp[0]);
          max = Math.max(max, r.sbp[1], r.dbp[1]);
        } else if (meta.ref.value) {
          min = Math.min(min, meta.ref.value[0]);
          max = Math.max(max, meta.ref.value[1]);
        }
      }

      const yFor = (v) => {
        return padT + ((max - v) / (max - min)) * plotH;
      };

      // 参考区间底色（轻提示）
      if (meta && meta.ref) {
        let low = null;
        let high = null;
        if (this.metricKey === 'bp') {
          // 仅按收缩压参考区间做淡底色
          low = meta.ref.sbp[0];
          high = meta.ref.sbp[1];
        } else if (meta.ref.value) {
          low = meta.ref.value[0];
          high = meta.ref.value[1];
        }
        if (low != null && high != null) {
          const y1 = yFor(high);
          const y2 = yFor(low);
          ctx.setFillStyle('rgba(16,185,129,0.06)');
          ctx.fillRect(padL, y1, plotW, Math.max(0, y2 - y1));
        }
      }

      // 画线 + 点
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

        // 点
        ctx.setFillStyle(serie.color);
        for (let i = 0; i < n; i++) {
          const v = arr[i];
          if (v == null || !Number.isFinite(v)) continue;
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
</script>

<style src="./trends.css"></style>
