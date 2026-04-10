<template>
  <view class="app-page">
    <view class="app-card page-card">
      <text class="page-title">随访记录</text>
      <text class="page-desc">展示历史随访记录与随访结果。</text>
    </view>
  </view>
</template>

<script>
export default {
  onShow() {
    uni.redirectTo({ url: '/pages/patient/followupDetail/followupDetail' });
  }
};
</script>

<style src="./followup.css"></style>
