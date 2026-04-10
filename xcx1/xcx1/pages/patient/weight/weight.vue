<template>
  <view class="app-page tp">
    <view class="tp-head app-card app-card--glass">
      <text class="tp-title">体温（℃）</text>
      <text class="tp-sub">用于观察发热/感染等风险信号。数据写入后将自动展示趋势。</text>
    </view>

    <view class="tp-main app-card">
      <view class="tp-main-row flex-row items-center justify-between">
        <view class="flex-col">
          <text class="tp-main-label">当前体温</text>
          <text class="tp-main-hint">最近一次测量值</text>
        </view>

        <view class="tp-main-right">
          <text class="tp-val">{{ currentValue }}</text>
          <text class="tp-unit">℃</text>
          <view class="tp-chip" :class="'chip-' + statusLevel">{{ statusText }}</view>
        </view>
      </view>
    </view>

    <view class="tp-trend app-card app-card--glass">
      <view class="tp-trend-head flex-row items-center justify-between">
        <text class="tp-trend-title">近7天趋势</text>
        <view class="btn-secondary tp-trend-btn" @tap="goTrends">查看详情</view>
      </view>

      <view v-if="!hasData" class="tp-empty">
        <text class="tp-empty-title">暂无数据</text>
        <text class="tp-empty-sub">请先在「设备管理」手动补录体温，或后续对接体温设备同步。</text>
        <view class="tp-empty-actions flex-row">
          <view class="btn-secondary tp-empty-btn" @tap="goDevices">去补录</view>
          <view class="btn-secondary tp-empty-btn ml-12" @tap="goTrends">查看趋势</view>
        </view>
      </view>

      <view v-else class="tp-bars">
        <view v-for="(p, idx) in chartPercents" :key="idx" class="tp-bar">
          <view class="tp-bar-inner" :style="{ height: p + '%' }"></view>
          <text class="tp-bar-x">{{ xLabels[idx] }}</text>
        </view>
      </view>

      <view v-if="hasData" class="tp-stats">
        <text class="tp-stat">平均：{{ avgValue }}℃</text>
        <text class="tp-stat">最高：{{ maxValue }}℃</text>
        <text class="tp-stat">最低：{{ minValue }}℃</text>
      </view>
    </view>

    <view class="tp-tips app-card">
      <text class="tp-tips-title">测量要点</text>
      <view class="tp-tip">1. 建议每日同一时间测量（如早起、晚睡前），避免运动后立即测量。</view>
      <view class="tp-tip">2. 若体温 ≥ 37.3℃ 建议复测确认；≥ 38℃ 或伴明显不适请及时联系医生。</view>
      <view class="tp-tip">3. 体温偏低（低于 35.5℃）且伴畏寒、乏力等不适，同样建议复测并注意保暖。</view>
    </view>
  </view>
</template>

<script>
import { getMetricChartData } from '@/utils/metrics-store.js';

export default {
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
      if (!this.hasData) return '--';
      return Number(this.chart.latest).toFixed(1);
    },

    statusLevel() {
      if (!this.hasData) return 'mid';
      const n = Number(this.chart.latest);
      if (Number.isNaN(n)) return 'mid';
      if (n >= 38 || n < 35.5) return 'high';
      if (n >= 37.3) return 'mid';
      return 'low';
    },

    statusText() {
      if (!this.hasData) return '待生成';
      const n = Number(this.chart.latest);
      if (Number.isNaN(n)) return '待生成';
      if (n < 35.5) return '偏低';
      const lv = this.statusLevel;
      if (lv === 'high') return '发热';
      if (lv === 'mid') return '偏高';
      return '正常';
    },

    chartPercents() {
      if (!this.hasData) return [];
      const arr = this.chart.series && Array.isArray(this.chart.series.values) ? this.chart.series.values : [];
      // 映射区间：35~40 => 0~100，并做最小可见高度 5%
      const lo = 35;
      const hi = 40;
      return arr.map((v) => {
        if (v == null) return 0;
        const p = ((Number(v) - lo) / (hi - lo)) * 100;
        return Math.max(5, Math.min(100, p));
      });
    },

    avgValue() {
      if (!this.hasData || !this.chart.stats) return '--';
      return Number(this.chart.stats.avg).toFixed(1);
    },
    maxValue() {
      if (!this.hasData || !this.chart.stats) return '--';
      return Number(this.chart.stats.max).toFixed(1);
    },
    minValue() {
      if (!this.hasData || !this.chart.stats) return '--';
      return Number(this.chart.stats.min).toFixed(1);
    }
  },

  onShow() {
    this.load();
  },

  methods: {
    load() {
      // 说明：这里沿用 key=weight 作为“体温”字段，保证与当前后端/旧版本字段兼容
      this.chart = getMetricChartData('weight', 'week');
    },

    goTrends() {
      uni.navigateTo({ url: '/pages/patient/trends/trends?metric=weight&range=week' });
    },

    goDevices() {
      uni.navigateTo({ url: '/pages/patient/devices/devices' });
    }
  }
};
</script>

<style src="./weight.css"></style>
