<template>
  <view class="app-page staff-page">
    <!-- 顶部：随访人员信息 -->
    <view class="app-card app-card--glass staff-head">
      <view class="flex-row items-center justify-between">
        <view class="flex-row items-center">
          <view class="staff-avatar">
            <text class="staff-avatar-text">{{ avatarText }}</text>
          </view>
          <view class="flex-col ml-12">
            <text class="staff-name">{{ form.name || '随访人员' }}</text>
            <text class="staff-subline">{{ form.role || '随访医生/护士' }} · {{ form.department || '科室未设置' }}</text>
          </view>
        </view>

        <view class="staff-chip">
          <text class="staff-chip-dot"></text>
          <text class="staff-chip-text">信息可编辑</text>
        </view>
      </view>

      <view class="staff-tip">
        <text class="staff-tip-text">建议完善联系方式与科室信息，便于任务派发与跨科室协作。</text>
      </view>
    </view>

    <!-- 我的信息管理 -->
    <view class="app-card staff-form">
      <text class="app-section-title">我的信息管理</text>

      <view class="kv">
        <text class="k">姓名</text>
        <input class="v-input" v-model="form.name" placeholder="请输入姓名" />
      </view>

      <view class="kv">
        <text class="k">工号</text>
        <input class="v-input" v-model="form.staffNo" placeholder="例如：A1023" />
      </view>

      <view class="kv">
        <text class="k">科室</text>
        <input class="v-input" v-model="form.department" placeholder="例如：心内科" />
      </view>

      <view class="kv">
        <text class="k">岗位</text>
        <input class="v-input" v-model="form.role" placeholder="例如：随访护士/责任医生" />
      </view>

      <view class="kv">
        <text class="k">手机号</text>
        <input class="v-input" v-model="form.phone" placeholder="用于随访联络" />
      </view>

      <view class="kv">
        <text class="k">负责片区</text>
        <input class="v-input" v-model="form.area" placeholder="例如：新城区·北片区" />
      </view>

      <view class="staff-actions">
        <view class="btn-secondary staff-btn" @tap="onReset">重置</view>
        <!-- 与“重置”保持一致的可见样式，避免保存按钮不显眼/不显示 -->
        <view class="btn-secondary staff-btn" @tap="onSave">保存</view>
      </view>
      <text class="staff-save-hint">{{ saveHint }}</text>
    </view>

    <!-- 设置与支持 -->
    <view class="app-card staff-settings">
      <text class="app-section-title">设置与支持</text>

      <view class="row" @tap="onClearCache">
        <view class="flex-row items-center">
          <text class="row-ico">🧹</text>
          <text class="row-text">清除本地缓存</text>
        </view>
        <text class="row-arrow">›</text>
      </view>

      <view class="line"></view>

      <view class="row" @tap="onContact">
        <view class="flex-row items-center">
          <text class="row-ico">☎️</text>
          <text class="row-text">联系管理员/技术支持</text>
        </view>
        <text class="row-arrow">›</text>
      </view>
    </view>

    <!-- 退出登录 -->
    <view class="app-card staff-logout">
      <view class="btn-danger" @tap="onLogout">退出登录</view>
    </view>

    <staff-tabbar active="me"></staff-tabbar>
  </view>
</template>

<script>
import StaffTabbar from '@/components/staff-tabbar/staff-tabbar.vue';
import { USE_MOCK_FALLBACK } from '@/config/api.js';
import { getStaffProfile, updateStaffProfile } from '@/api/staff.js';
import { logout as apiLogout } from '@/api/auth.js';
import { clearSession } from '@/utils/session.js';

export default {
  components: { StaffTabbar },
  data() {
    return {
      form: {
        name: '',
        staffNo: '',
        department: '',
        role: '',
        phone: '',
        area: ''
      },
      saveHint: '未保存'
    };
  },

  computed: {
    avatarText() {
      const n = (this.form.name || '').trim();
      return n ? n[0] : '随';
    }
  },

  onShow() {
    this.load();
  },

  methods: {
    load() {
      try {
        const v = uni.getStorageSync('staff_profile');
        if (v && typeof v === 'object') {
          this.form = { ...this.form, ...v };
          this.saveHint = '已从本地加载';
        }
      } catch (e) {}

      // ✅ 后端资料覆盖本地（联调阶段失败不阻塞）
      getStaffProfile()
        .then((res) => {
          if (res && typeof res === 'object') {
            this.form = { ...this.form, ...res };
            this.saveHint = '已从后端加载';
          }
        })
        .catch(() => {});
    },

    onSave() {
      uni.showLoading({ title: '保存中…', mask: true });
      updateStaffProfile(this.form)
        .then(() => {
          try {
            uni.setStorageSync('staff_profile', this.form);
          } catch (e) {}
          const now = new Date();
          const pad = (x) => (x < 10 ? '0' + x : '' + x);
          const t = pad(now.getHours()) + ':' + pad(now.getMinutes());
          this.saveHint = '已保存 · ' + t;
          uni.showToast({ title: '已保存', icon: 'success' });
        })
        .catch((err) => {
          if (USE_MOCK_FALLBACK) {
            try {
              uni.setStorageSync('staff_profile', this.form);
            } catch (e) {}
            uni.showToast({ title: '已保存（本地）', icon: 'success' });
          } else {
            uni.showToast({ title: (err && err.message) || '保存失败', icon: 'none' });
          }
        })
        .finally(() => uni.hideLoading());
    },

    onReset() {
      uni.showModal({
        title: '重置填写内容',
        content: '将清空当前页面已填写信息（不影响已保存的本地信息）。',
        success: (res) => {
          if (res.confirm) {
            this.form = { name: '', staffNo: '', department: '', role: '', phone: '', area: '' };
            this.saveHint = '已重置（未保存）';
          }
        }
      });
    },

    onClearCache() {
      uni.showModal({
        title: '清除本地缓存',
        content: '将清除本机保存的随访人员信息与页面缓存，是否继续？',
        success: (res) => {
          if (res.confirm) {
            try {
              uni.removeStorageSync('staff_profile');
              this.form = { name: '', staffNo: '', department: '', role: '', phone: '', area: '' };
              this.saveHint = '已清除缓存';
              uni.showToast({ title: '已清除', icon: 'success' });
            } catch (e) {
              uni.showToast({ title: '清除失败', icon: 'none' });
            }
          }
        }
      });
    },

    onContact() {
      uni.showModal({
        title: '联系支持',
        content: '如需技术支持，请联系管理员或在工作群反馈问题（示例）。',
        showCancel: false
      });
    },

    onLogout() {
      uni.showModal({
        title: '退出登录',
        content: '将返回角色选择页面。是否继续？',
        success: (res) => {
          if (res.confirm) {
            apiLogout().catch(() => {});
            clearSession();
            uni.reLaunch({ url: '/pages/index/index' });
          }
        }
      });
    }
  }
};
</script>

<style src="./profile.css"></style>
