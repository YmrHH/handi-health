<template>
  <view class="staff-tabbar">
    <view class="staff-tabbar-inner">
      <view class="tab-item" :class="active === 'tasks' ? 'active' : ''" @tap="goTasks">
        <text class="tab-ico">🗂️</text>
        <text class="tab-text">随访任务</text>
      </view>

      <view class="tab-item" :class="active === 'managed' ? 'active' : ''" @tap="goManaged">
        <text class="tab-ico">🧑‍🤝‍🧑</text>
        <text class="tab-text">在管患者</text>
      </view>

      <view class="tab-item" :class="active === 'me' ? 'active' : ''" @tap="goMe">
        <text class="tab-ico">👤</text>
        <text class="tab-text">我的信息管理</text>
      </view>
    </view>
    <view class="safe-area"></view>
  </view>
</template>

<script>
export default {
  name: 'StaffTabbar',
  props: {
    active: { type: String, default: 'tasks' }
  },
  methods: {
    goTasks() {
      if (this.active === 'tasks') return;
      uni.reLaunch({ url: '/pages/staff/tasks/tasks' });
    },
    goManaged() {
      if (this.active === 'managed') return;
      uni.reLaunch({ url: '/pages/staff/managedPatients/managedPatients' });
    },
    goMe() {
      if (this.active === 'me') return;
      uni.reLaunch({ url: '/pages/staff/profile/profile' });
    }
  }
};
</script>

<style>
.staff-tabbar{
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 999;
}

.staff-tabbar-inner{
  height: 112rpx;
  background: rgba(255,255,255,0.92);
  border-top: 1rpx solid rgba(91,108,255,0.12);
  box-shadow: 0 -12rpx 30rpx rgba(31,42,55,0.06);
  display: flex;
  flex-direction: row;
  justify-content: space-around;
  align-items: center;
}

.tab-item{
  flex: 1;
  height: 112rpx;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: rgba(31,42,55,0.55);
}

.tab-item.active{
  color: rgba(91,108,255,0.95);
}

.tab-ico{
  font-size: 34rpx;
  line-height: 34rpx;
}

.tab-text{
  margin-top: 6rpx;
  font-size: 22rpx;
  font-weight: 750;
}

.safe-area{
  height: env(safe-area-inset-bottom);
  background: rgba(255,255,255,0.92);
}
</style>
