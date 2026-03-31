<template>
  <div class="layout">
    <div class="col left">
      <ScreenPanel title="干预计划总览" subtitle="未接入干预/服务相关接口">
        <div class="placeholder">当前后端未提供可复用的干预/上门服务聚合接口，大屏不展示模拟数字。</div>
      </ScreenPanel>
      <ScreenPanel title="计划类型占比" subtitle="未接入">
        <div class="placeholder">暂无数据</div>
      </ScreenPanel>
      <ScreenPanel title="热门方案 TOP" subtitle="未接入">
        <div class="placeholder">暂无数据</div>
      </ScreenPanel>
    </div>

    <div class="col center">
      <ScreenPanel title="服务执行路径盘" subtitle="中心主视觉（触达/执行/达标）">
        <div class="center-stage">
          <div class="hub glow-breath">
            <div class="hub-title">服务执行</div>
            <div class="hub-value">—</div>
            <div class="hub-sub">未接入接口</div>
          </div>
          <div class="hub-ring muted-ring"></div>
        </div>
      </ScreenPanel>
      <ScreenPanel title="时间序列趋势" subtitle="未接入">
        <div class="placeholder">暂无数据</div>
      </ScreenPanel>
    </div>

    <div class="col right">
      <ScreenPanel title="上门服务触达" subtitle="未接入">
        <div class="placeholder">暂无数据</div>
      </ScreenPanel>
      <ScreenPanel title="地区/人员分布" subtitle="未接入">
        <div class="placeholder">暂无数据</div>
      </ScreenPanel>
      <ScreenPanel title="最新服务动态" subtitle="事件流（轻量）">
        <EventTicker :items="events" />
      </ScreenPanel>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import ScreenPanel from '../components/ScreenPanel.vue'
import EventTicker from '../components/EventTicker.vue'
const events = ref<Array<{ id: string | number; title: string; time: string }>>([])
</script>

<style scoped>
.layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 2.35fr) minmax(0, 1fr);
  gap: 12px;
}

.col {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
}

.chart {
  height: 210px;
}

.chart-tall {
  height: 340px;
}

.center-stage {
  position: relative;
  height: 300px;
  display: grid;
  place-items: center;
}

.hub {
  position: absolute;
  width: 250px;
  height: 250px;
  border-radius: 999px;
  background: radial-gradient(circle at 50% 35%, rgba(127, 214, 227, 0.30), rgba(255, 255, 255, 0.76) 58%, rgba(140, 188, 227, 0.25));
  border: 1px solid rgba(114, 180, 205, 0.34);
  box-shadow: var(--glow-strong);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
}

.hub-title {
  font-size: 12px;
  letter-spacing: 2px;
  color: var(--t-2);
}

.hub-value {
  margin-top: 8px;
  font-size: 44px;
  font-weight: 900;
  color: var(--c-gold);
}

.hub-sub {
  margin-top: 10px;
  font-size: 12px;
  color: var(--t-3);
}

.hub-ring {
  position: absolute;
  inset: 0;
}

.placeholder {
  padding: 10px 8px;
  color: var(--t-3);
  font-size: 12px;
}

.muted-ring {
  border-radius: 999px;
  border: 1px dashed rgba(114, 180, 205, 0.32);
  margin: 14px;
}
</style>

