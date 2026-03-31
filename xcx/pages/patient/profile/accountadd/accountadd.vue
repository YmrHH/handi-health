<template>
  <view class="account-disabled-page">
    <view class="account-disabled-card">
      <view class="account-disabled-icon">ℹ️</view>
      <text class="account-disabled-title">该功能当前未启用</text>
      <text class="account-disabled-desc">
        当前项目已按“同一患者账号可在多台手机登录”的方案运行。
        家属查看或代患者录入健康指标时，可直接登录同一个患者账号；
        本页原有的“添加家属/关联账号”本地模拟逻辑已停用，避免造成数据误解。
      </text>

      <view class="account-disabled-tips">
        <text class="account-disabled-tip">· 另一台手机登录同一个患者账号后，可拉取后端最新体征并显示</text>
        <text class="account-disabled-tip">· 硬件上传或手动录入后，数据会继续按现有逻辑同步到后端</text>
        <text class="account-disabled-tip">· 后续若需要独立家属账号，再结合后端权限体系单独重做</text>
      </view>

      <button class="account-disabled-btn" @tap="goBack">返回上一页</button>
    </view>
  </view>
</template>

<script>
export default {
  methods: {
    goBack() {
      const pages = getCurrentPages();
      if (pages && pages.length > 1) {
        uni.navigateBack();
        return;
      }
      uni.reLaunch({ url: '/pages/patient/profile/profile' });
    }
  }
};
</script>

<style>
.account-disabled-page {
  min-height: 100vh;
  padding: 32rpx;
  background: #f6f8fc;
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
}

.account-disabled-card {
  width: 100%;
  background: #ffffff;
  border-radius: 28rpx;
  padding: 40rpx 32rpx;
  box-shadow: 0 12rpx 40rpx rgba(0, 0, 0, 0.06);
}

.account-disabled-icon {
  font-size: 64rpx;
  line-height: 1;
  text-align: center;
}

.account-disabled-title {
  display: block;
  margin-top: 20rpx;
  text-align: center;
  font-size: 34rpx;
  font-weight: 700;
  color: #1f2937;
}

.account-disabled-desc {
  display: block;
  margin-top: 24rpx;
  font-size: 28rpx;
  line-height: 1.8;
  color: #475467;
}

.account-disabled-tips {
  margin-top: 28rpx;
  padding: 24rpx;
  background: #f8fafc;
  border-radius: 20rpx;
}

.account-disabled-tip {
  display: block;
  font-size: 26rpx;
  line-height: 1.8;
  color: #344054;
}

.account-disabled-btn {
  margin-top: 36rpx;
  background: #5b6cff;
  color: #ffffff;
  border-radius: 999rpx;
  font-size: 30rpx;
  line-height: 88rpx;
  height: 88rpx;
}
</style>
