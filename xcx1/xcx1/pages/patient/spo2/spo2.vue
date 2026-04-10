<template>
  <view class="app-page sp">
    <view class="sp-head app-card app-card--glass">
      <text class="sp-title">血氧饱和度（SpO₂）</text>
      <text class="sp-sub">用于观察呼吸与循环状态。数据写入后将自动展示趋势。</text>
    </view>

    <view class="sp-main app-card">
      <view class="sp-main-row flex-row items-center justify-between">
        <view class="flex-col">
          <text class="sp-main-label">当前血氧</text>
          <text class="sp-main-hint">最近一次测量值</text>
        </view>

        <view class="sp-main-right">
          <text class="sp-val">{{ currentValue }}</text>
          <text class="sp-unit">%</text>
          <view class="sp-chip" :class="'chip-' + statusLevel">{{ statusText }}</view>
        </view>
      </view>
    </view>

    <view class="sp-trend app-card app-card--glass">
      <view class="sp-trend-head flex-row items-center justify-between">
        <text class="sp-trend-title">近7天趋势</text>
        <view class="btn-secondary sp-trend-btn" @tap="goTrends">查看详情</view>
      </view>

      <view v-if="!hasData" class="sp-empty">
        <text class="sp-empty-title">暂无数据</text>
        <text class="sp-empty-sub">请先在「体征记录」录入血氧，或在「设备管理」同步数据。</text>
        <view class="sp-empty-actions flex-row">
          <view class="btn-secondary sp-empty-btn" @tap="goMonitor">去记录</view>
          <view class="btn-secondary sp-empty-btn ml-12" @tap="goDevices">去同步</view>
        </view>
      </view>

      <view v-else class="sp-bars">
        <view v-for="(p, idx) in chartPercents" :key="idx" class="sp-bar">
          <view class="sp-bar-inner" :style="{ height: p + '%' }"></view>
          <text class="sp-bar-x">{{ xLabels[idx] }}</text>
        </view>
      </view>

      <view v-if="hasData" class="sp-stats">
        <text class="sp-stat">平均：{{ avgValue }}%</text>
        <text class="sp-stat">最高：{{ maxValue }}%</text>
        <text class="sp-stat">最低：{{ minValue }}%</text>
      </view>
    </view>

    <view class="sp-tips app-card">
      <text class="sp-tips-title">测量要点</text>
      <view class="sp-tip">1. 测量前保持手指温暖、放松静止 30 秒左右。</view>
      <view class="sp-tip">2. 指夹式设备请确保夹紧且指甲无厚涂指甲油。</view>
      <view class="sp-tip">3. 若连续多次偏低或伴明显不适，请及时联系医生。</view>
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
      return String(Math.round(this.chart.latest));
    },

    statusLevel() {
      // green=low, orange=mid, red=high
      if (!this.hasData) return 'mid';
      const n = Number(this.chart.latest);
      if (n < 90) return 'high';
      if (n < 95) return 'mid';
      return 'low';
    },

    statusText() {
      if (!this.hasData) return '待生成';
      const lv = this.statusLevel;
      if (lv === 'high') return '需关注';
      if (lv === 'mid') return '略偏低';
      return '达标';
    },

    chartPercents() {
      if (!this.hasData) return [];
      const arr = this.chart.series && Array.isArray(this.chart.series.values) ? this.chart.series.values : [];
      // 映射区间：85~100 => 0~100（保证可视化），并做最小可见高度 5%
      const lo = 85;
      const hi = 100;
      return arr.map((v) => {
        if (v == null) return 0;
        const p = ((Number(v) - lo) / (hi - lo)) * 100;
        return Math.max(5, Math.min(100, p));
      });
    },

    avgValue() {
      if (!this.hasData || !this.chart.stats) return '--';
      return Number(this.chart.stats.avg).toFixed(0);
    },
    maxValue() {
      if (!this.hasData || !this.chart.stats) return '--';
      return Number(this.chart.stats.max).toFixed(0);
    },
    minValue() {
      if (!this.hasData || !this.chart.stats) return '--';
      return Number(this.chart.stats.min).toFixed(0);
    }
  },

  onShow() {
    this.load();
  },

  methods: {
    load() {
      this.chart = getMetricChartData('spo2', 'week');
    },

    goTrends() {
      uni.navigateTo({ url: '/pages/patient/trends/trends?metric=spo2&range=week' });
    },

    goMonitor() {
      uni.navigateTo({ url: '/pages/patient/monitor/monitor?tab=daily' });
    },

    goDevices() {
      uni.navigateTo({ url: '/pages/patient/devices/devices' });
    }
  }
};
</script>

<style src="./spo2.css"></style>
