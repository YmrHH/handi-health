<template>
  <view class="app-page pe">
    <view class="pe-head">
      <text class="pe-title">编辑个人资料</text>
      <text class="pe-sub">用于出院后监测、预警与随访服务</text>
    </view>

    <!-- 头像 -->
    <view class="app-card pe-avatar-card" @tap="chooseAvatar">
      <view class="flex-row items-center justify-between">
        <view class="flex-row items-center">
          <image class="pe-avatar" :src="formData.avatarUrl || defaultAvatar" mode="aspectFill"></image>
          <view class="flex-col">
            <text class="pe-name">{{ formData.name || '未设置姓名' }}</text>
            <text class="pe-tip">点击更换头像</text>
          </view>
        </view>
        <text class="pe-arrow">›</text>
      </view>
    </view>

    <!-- 基本信息 -->
    <view class="app-card pe-card">
      <text class="pe-section">基本信息</text>

      <view class="pe-field flex-row items-center justify-between">
        <text class="pe-label">姓名</text>
        <input
          class="pe-input"
          v-model="formData.name"
          placeholder="请输入姓名"
          placeholder-class="pe-ph"
        />
      </view>

      <view class="pe-line"></view>

      <view class="pe-field flex-row items-center justify-between">
        <text class="pe-label">性别</text>
        <picker :range="genderOptions" :value="genderIndex" @change="onGenderChange">
          <view class="pe-picker flex-row items-center">
            <text class="pe-picker-text">{{ genderOptions[genderIndex] || '请选择' }}</text>
            <text class="pe-arrow">›</text>
          </view>
        </picker>
      </view>

      <view class="pe-line"></view>

      <view class="pe-field flex-row items-center justify-between">
        <text class="pe-label">出生日期</text>
        <picker mode="date" :value="formData.birthday" @change="onBirthChange">
          <view class="pe-picker flex-row items-center">
            <text class="pe-picker-text">{{ formData.birthday || '请选择' }}</text>
            <text class="pe-arrow">›</text>
          </view>
        </picker>
      </view>

      <view class="pe-line"></view>

      <view class="pe-field flex-row items-center justify-between">
        <text class="pe-label">手机号</text>
        <input
          class="pe-input"
          v-model="formData.phone"
          type="number"
          maxlength="11"
          placeholder="用于接收提醒"
          placeholder-class="pe-ph"
        />
      </view>

      <view class="pe-line"></view>

      <view class="pe-field flex-row items-center justify-between">
        <text class="pe-label">身高</text>
        <view class="flex-row items-center">
          <input
            class="pe-input pe-input-num"
            v-model="formData.heightCm"
            type="number"
            maxlength="3"
            placeholder="cm"
            placeholder-class="pe-ph"
          />
          <text class="pe-unit">cm</text>
        </view>
      </view>

      <view class="pe-line"></view>

      <view class="pe-field flex-row items-center justify-between">
        <text class="pe-label">体重</text>
        <view class="flex-row items-center">
          <input
            class="pe-input pe-input-num"
            v-model="formData.weightKg"
            type="digit"
            maxlength="6"
            placeholder="kg"
            placeholder-class="pe-ph"
          />
          <text class="pe-unit">kg</text>
        </view>
      </view>
    </view>

    <!-- 预留底部空间，避免固定按钮遮挡 -->


    <!-- 安全设置：修改密码 -->
    <view class="app-card app-card--glass pe-card">
      <text class="pe-section">安全设置</text>

      <view class="pe-field flex-row items-center justify-between">
        <text class="pe-label">旧密码</text>
        <input type="text" :password="true" v-model="pwdData.oldPassword" placeholder="请输入旧密码" placeholder-class="pe-ph" />
      </view>
      <view class="pe-line"></view>

      <view class="pe-field flex-row items-center justify-between">
        <text class="pe-label">新密码</text>
        <input type="text" :password="true" v-model="pwdData.newPassword" placeholder="请输入新密码（不少于6位）" placeholder-class="pe-ph" />
      </view>
      <view class="pe-line"></view>

      <view class="pe-field flex-row items-center justify-between">
        <text class="pe-label">确认新密码</text>
        <input type="text" :password="true" v-model="pwdData.confirmPassword" placeholder="再次输入新密码" placeholder-class="pe-ph" />
      </view>

      <view class="pe-pwd-actions">
        <view class="btn-secondary pe-pwd-btn" @tap="onChangePassword">修改密码</view>
      </view>
      <text class="pe-pwd-tip">修改成功后，请使用新密码重新登录。</text>
    </view>

    <view class="pe-spacer"></view>

    <!-- 固定保存按钮 -->
    <view class="pe-footer">
      <view class="btn-primary pe-save" hover-class="btn-hover" @tap="saveUserData">
        <text class="pe-save-text">保存</text>
      </view>
</view>
  </view>
</template>

<script>
import { USE_MOCK_FALLBACK } from '@/config/api.js';
import { getUser, setUser, getScopedStorageSync, setScopedStorageSync } from '@/utils/session.js';
import { getProfile, updateProfile } from '@/api/patient.js';
import { changePassword } from '@/api/auth.js';
import { upload } from '@/api/common.js';

export default {
  data() {
    return {
      defaultAvatar: '/static/assets/avatar.png',
      formData: {
        avatarUrl: '',
        name: '',
        gender: '男',
        birthday: '',
        phone: '',
        heightCm: '',
        weightKg: ''
      },
      genderOptions: ['男', '女'],
      genderIndex: 0,
      pwdData: {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      }
    };
  },

  onLoad() {
    this.loadUserData();
  },

  methods: {
    loadUserData() {
      try {
        const userInfo = uni.getStorageSync('userInfo',null);
        if (userInfo && typeof userInfo === 'object') {
          this.formData = { ...this.formData, ...userInfo };
          if (userInfo.gender) {
            const idx = this.genderOptions.indexOf(userInfo.gender);
            this.genderIndex = idx >= 0 ? idx : 0;
          }
        }
      } catch (e) {}

      // ✅ 后端资料覆盖本地缓存（不阻塞渲染）
      getProfile()
        .then((p) => {
          if (p && typeof p === 'object') {
            this.formData = { ...this.formData, ...p };
            if (p.gender) {
              const idx = this.genderOptions.indexOf(p.gender);
              this.genderIndex = idx >= 0 ? idx : 0;
            }
            try {
              uni.setStorageSync('userInfo', { ...uni.getStorageSync('userInfo'), ...p });
            } catch (e) {}
          }
        })
        .catch(() => {});
    },

    chooseAvatar() {
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        success: (res) => {
          const temp = (res.tempFilePaths && res.tempFilePaths[0]) || '';
          if (temp) this.formData.avatarUrl = temp;
        }
      });
    },

    onGenderChange(e) {
      const idx = Number(e.detail.value || 0);
      this.genderIndex = idx;
      this.formData.gender = this.genderOptions[idx] || '男';
    },

    onBirthChange(e) {
      this.formData.birthday = e.detail.value;
    },

    // 修改密码
    async onChangePassword() {
      const oldPassword = (this.pwdData.oldPassword || '').trim();
      const newPassword = (this.pwdData.newPassword || '').trim();
      const confirmPassword = (this.pwdData.confirmPassword || '').trim();

      if (!oldPassword || !newPassword || !confirmPassword) {
        return uni.showToast({ title: '请填写完整的密码信息', icon: 'none' });
      }
      if (newPassword.length < 6) {
        return uni.showToast({ title: '新密码不少于6位', icon: 'none' });
      }
      if (newPassword !== confirmPassword) {
        return uni.showToast({ title: '两次新密码不一致', icon: 'none' });
      }

      const u = getUser();
      if (!u || !u.id) {
        return uni.showToast({ title: '未获取到登录用户信息', icon: 'none' });
      }

      uni.showLoading({ title: '修改中…', mask: true });
      try {
        await changePassword({
          userId: u.id,
          oldPassword,
          newPassword,
          confirmPassword
        });

        // 更新本地登录态里的明文密码（联调用）
        setUser({ ...u, password: newPassword });

        // 清空输入框
        this.pwdData = { oldPassword: '', newPassword: '', confirmPassword: '' };

        uni.showToast({ title: '密码已修改', icon: 'success' });
      } catch (err) {
        uni.showToast({ title: (err && err.message) || '修改失败', icon: 'none' });
      } finally {
        uni.hideLoading();
      }
    },

    async saveUserData() {
      const payload = {
        avatarUrl: this.formData.avatarUrl,
        name: (this.formData.name || '').trim(),
        gender: this.genderOptions[this.genderIndex] || '男',
        birthday: this.formData.birthday,
        phone: this.formData.phone,
        heightCm: this.formData.heightCm,
        weightKg: this.formData.weightKg
      };

      if (!payload.name) {
        return uni.showToast({ title: '请填写姓名', icon: 'none' });
      }

      uni.showLoading({ title: '保存中…', mask: true });

      try {
        // 如果头像是本地临时文件，先上传
        if (payload.avatarUrl && !/^https?:\/\//i.test(payload.avatarUrl)) {
          const up = await upload(payload.avatarUrl, { bizType: 'avatar' });
          payload.avatarUrl = (up && up.url) || payload.avatarUrl;
        }

        await updateProfile(payload);
        // 后端成功后再写本地缓存
        // 合并写入，避免覆盖 userId/risk 等其它字段
            const prev = getScopedStorageSync('userInfo', {});
            setScopedStorageSync('userInfo', { ...(prev && typeof prev === 'object' ? prev : {}), ...payload });

            // 同步更新登录态里的用户名（首页/我的页会读取）
            const u = getUser();
            if (u && typeof u === 'object') {
              setUser({ ...u, name: payload.name });
            }

            // 通知首页等页面刷新
            try { uni.$emit('userInfoUpdated', payload); } catch (e) {}
        uni.showToast({ title: '已保存', icon: 'success' });
        setTimeout(() => uni.navigateBack(), 300);
      } catch (err) {
        // 联调阶段允许仅保存到本地
        if (USE_MOCK_FALLBACK) {
          // 合并写入，避免覆盖 userId/risk 等其它字段
           const prev = getScopedStorageSync('userInfo', {});
           setScopedStorageSync('userInfo', { ...(prev && typeof prev === 'object' ? prev : {}), ...payload });

            // 同步更新登录态里的用户名（首页/我的页会读取）
            const u = getUser();
            if (u && typeof u === 'object') {
              setUser({ ...u, name: payload.name });
            }

            // 通知首页等页面刷新
            try { uni.$emit('userInfoUpdated', payload); } catch (e) {}
          uni.showToast({ title: '已保存（本地）', icon: 'success' });
          setTimeout(() => uni.navigateBack(), 300);
        } else {
          uni.showToast({ title: (err && err.message) || '保存失败', icon: 'none' });
        }
      } finally {
        uni.hideLoading();
      }
    }
  }
};
</script>

<style src="./profileedit.css"></style>
