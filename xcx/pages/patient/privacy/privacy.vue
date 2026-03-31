<template>
  <view class="app-page pv">
    <view class="app-card app-card--glass pv-head">
      <text class="pv-title">隐私与授权</text>
      <text class="pv-sub">用于出院随访监测与预警服务。你可在此管理授权与本地数据。</text>
    </view>

    <!-- 授权管理 -->
    <view class="pv-section">
      <text class="app-section-title">授权管理</text>
    </view>
    <view class="app-card pv-card">
      <view class="pv-row">
        <view class="pv-left">
          <text class="pv-name">蓝牙</text>
          <text class="pv-desc">用于连接监测设备并接收每日数据</text>
        </view>
        <text class="pv-status">{{ auth.bluetooth }}</text>
      </view>
      <view class="pv-divider"></view>

      <view class="pv-row">
        <view class="pv-left">
          <text class="pv-name">消息通知</text>
          <text class="pv-desc">用于接收医生建议与系统提醒</text>
        </view>
        <text class="pv-status">{{ auth.notify }}</text>
      </view>
      <view class="pv-divider"></view>

      <view class="pv-row">
        <view class="pv-left">
          <text class="pv-name">相册/相机</text>
          <text class="pv-desc">用于意见反馈上传截图（可选）</text>
        </view>
        <text class="pv-status">{{ auth.album }}</text>
      </view>

      <view class="pv-actions">
        <view class="btn-secondary pv-btn" @tap="openSettings">
          <text class="pv-btn-text">去系统授权设置</text>
        </view>
        <view class="btn-secondary pv-btn" @tap="refreshAuth">
          <text class="pv-btn-text">刷新状态</text>
        </view>
      </view>
    </view>

    <!-- 数据与安全 -->
    <view class="pv-section">
      <text class="app-section-title">数据与安全</text>
    </view>
    <view class="app-card app-card--glass pv-card">
      <view class="pv-bullets">
        <view class="pv-bullet">
          <text class="pv-dot">•</text>
          <text class="pv-text">出院基础档案由医生端导入；本端仅展示与补充。</text>
        </view>
        <view class="pv-bullet">
          <text class="pv-dot">•</text>
          <text class="pv-text">身体数据来源：蓝牙设备每日上报 + 手动补录（部分指标无法自动获取）。</text>
        </view>
        <view class="pv-bullet">
          <text class="pv-dot">•</text>
          <text class="pv-text">数据用于趋势分析、随访关联与风险提醒；医生建议由医生端审核后下发。</text>
        </view>
        <view class="pv-bullet">
          <text class="pv-dot">•</text>
          <text class="pv-text">本地缓存仅用于提升体验；如需更换设备，可清除本地缓存重新登录。</text>
        </view>
      </view>

      <view class="pv-actions pv-actions-single">
        <view class="btn-primary pv-danger" @tap="clearLocal">
          <text class="pv-danger-text">清除本地缓存</text>
        </view>
      </view>

      <view class="pv-note">
        <text class="pv-note-text">提示：清除后将移除本地显示的姓名头像、趋势缓存、消息已读状态等；不会影响医生端已存档的记录。</text>
      </view>
    </view>

    <!-- 隐私政策（简版） -->
    <view class="pv-section">
      <text class="app-section-title">隐私说明（简版）</text>
    </view>
    <view class="app-card pv-card">
      <text class="pv-policy">
        我们仅在提供出院随访监测与预警服务所必需的范围内处理你的数据。你可随时在本页管理授权与清除本地缓存。若你需要更详细的隐私政策或数据导出/更正，请通过「帮助与客服」联系工作人员。
      </text>
    </view>
  </view>
</template>

<script>
import { clearCurrentUserBusinessCache } from '@/utils/session.js';

export default {
  data() {
    return {
      auth: {
        bluetooth: '未检测',
        notify: '未检测',
        album: '未检测'
      }
    };
  },

  onShow() {
    this.refreshAuth();
  },

  methods: {
    refreshAuth() {
      this.checkBluetooth();
      this.checkSettings();
    },

    checkBluetooth() {
      uni.openBluetoothAdapter({
        success: () => {
          this.auth.bluetooth = '可用/已开启';
          uni.closeBluetoothAdapter({});
        },
        fail: () => {
          this.auth.bluetooth = '未开启/未授权';
        }
      });
    },

    checkSettings() {
      uni.getSetting({
        success: (res) => {
          const authSetting = (res && res.authSetting) || {};
          const camera = authSetting['scope.camera'];
          const album = authSetting['scope.writePhotosAlbum'];
          this.auth.album = (camera === true || album === true) ? '已授权' : '未授权';
          this.auth.notify = '建议开启（在消息中心）';
        },
        fail: () => {
          this.auth.album = '未检测';
          this.auth.notify = '未检测';
        }
      });
    },

    openSettings() {
      uni.openSetting({
        success: () => {},
        fail: () => {
          uni.showToast({ title: '无法打开设置', icon: 'none' });
        }
      });
    },

    clearLocal() {
      uni.showModal({
        title: '清除本地缓存',
        content: '将清除当前账号在本机上的本地展示数据与状态（不影响后端存档）。是否继续？',
        confirmText: '清除',
        cancelText: '取消',
        success: (res) => {
          if (!res.confirm) return;

          try {
            clearCurrentUserBusinessCache();
          } catch (e) {
            console.error('清除当前账号本地缓存失败', e);
          }

          uni.showToast({ title: '已清除', icon: 'success' });
          this.refreshAuth();
        }
      });
    }
  }
};
</script>

<style src="./privacy.css"></style>