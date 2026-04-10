<template>
  <view class="app-page fb">
    <view class="app-card app-card--glass fb-head">
      <text class="fb-title">意见与反馈</text>
      <text class="fb-sub">用于改进出院随访监测体验。可描述问题、提出建议，并可选上传截图。</text>
    </view>

    <view class="fb-section">
      <text class="app-section-title">提交反馈</text>
    </view>

    <view class="app-card fb-card">
      <view class="fb-row">
        <text class="fb-label">反馈类型</text>
        <picker :range="typeOptions" :value="typeIndex" @change="onTypeChange">
          <view class="fb-picker">
            <text class="fb-picker-text">{{ typeOptions[typeIndex] }}</text>
            <text class="fb-arrow">›</text>
          </view>
        </picker>
      </view>

      <view class="fb-row fb-row-top">
        <text class="fb-label">问题描述（必填）</text>
      </view>
      <textarea class="fb-textarea" v-model="form.content" maxlength="500" placeholder="请描述你遇到的问题或建议（例如：蓝牙连接失败、数据不同步、随访申请流程建议…）"></textarea>
      <text class="fb-hint">{{ form.content.length }}/500</text>

      <view class="fb-row fb-row-top">
        <text class="fb-label">联系方式（选填）</text>
      </view>
      <input class="fb-input" v-model="form.contact" placeholder="手机号/微信号（便于我们回访）" />

      <view class="fb-row fb-row-top fb-row-between">
        <text class="fb-label">截图（选填）</text>
        <text class="fb-mini">{{ images.length }}/3</text>
      </view>

      <view class="fb-images">
        <view v-for="(img, idx) in images" :key="idx" class="fb-imgbox">
          <image class="fb-img" :src="img" mode="aspectFill" @tap="preview(idx)"></image>
          <view class="fb-del" @tap.stop="removeImg(idx)">
            <text class="fb-del-text">×</text>
          </view>
        </view>
        <view v-if="images.length < 3" class="fb-add" @tap="chooseImages">
          <text class="fb-add-text">+</text>
          <text class="fb-add-sub">添加</text>
        </view>
      </view>

      <view class="btn-primary fb-submit" @tap="submit">
        <text class="fb-submit-text">提交反馈</text>
      </view>

      <view class="fb-note">
        <text class="fb-note-text">说明：反馈内容将用于产品改进；如遇紧急健康问题，请优先联系医生或拨打急救电话。</text>
      </view>
    </view>

    <view class="fb-section">
      <text class="app-section-title">最近反馈</text>
    </view>
    <view class="app-card app-card--glass fb-card">
      <view v-if="recent.length === 0" class="fb-empty">
        <text class="fb-empty-text">暂无反馈记录</text>
      </view>
      <view v-for="item in recent" :key="item.id" class="fb-item">
        <view class="flex-row items-center justify-between">
          <text class="fb-item-type">{{ item.type }}</text>
          <text class="fb-item-time">{{ item.time }}</text>
        </view>
        <text class="fb-item-content">{{ item.content }}</text>
      </view>
    </view>
  </view>
</template>

<script>
import { getScopedStorageSync, setScopedStorageSync } from '@/utils/session.js';
import { USE_MOCK_FALLBACK } from '@/config/api.js';
import { listFeedback, submitFeedback } from '@/api/patient.js';
import { upload } from '@/api/common.js';

function nowStr() {
  const d = new Date();
  const p = (n) => (n < 10 ? '0' + n : '' + n);
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`;
}
function uid() {
  return 'fb_' + Date.now() + '_' + Math.floor(Math.random() * 10000);
}

export default {
  data() {
    return {
      typeOptions: ['功能建议', '数据问题', '蓝牙连接', '随访相关', '其它'],
      typeIndex: 0,
      form: {
        content: '',
        contact: ''
      },
      images: [],
      recent: []
    };
  },

  onShow() {
    this.loadRecent();
  },

  methods: {
    loadRecent() {
      // 本地缓存先渲染
     const arr = getScopedStorageSync('feedback_list', []);
      const list = Array.isArray(arr) ? arr : [];
      this.recent = list.slice(0, 5);

      // ✅ 后端列表覆盖（联调阶段失败不阻塞）
      listFeedback()
        .then((res) => {
          const remote = Array.isArray(res) ? res : (res && res.list) || [];
          if (remote && remote.length) {
            this.recent = remote.slice(0, 5);
          }
        })
        .catch(() => {});
    },

    onTypeChange(e) {
      this.typeIndex = Number(e.detail.value || 0);
    },

    chooseImages() {
      uni.chooseImage({
        count: 3 - this.images.length,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: (res) => {
          const temp = (res && res.tempFilePaths) || [];
          this.images = this.images.concat(temp).slice(0, 3);
        }
      });
    },

    preview(idx) {
      uni.previewImage({
        current: this.images[idx],
        urls: this.images
      });
    },

    removeImg(idx) {
      this.images.splice(idx, 1);
    },

    submit() {
      const content = (this.form.content || '').trim();
      if (!content) return uni.showToast({ title: '请填写问题描述', icon: 'none' });

      const payload = {
        id: uid(),
        time: nowStr(),
        type: this.typeOptions[this.typeIndex],
        content,
        contact: (this.form.contact || '').trim(),
        images: this.images.slice()
      };

      uni.showModal({
        title: '确认提交',
        content: '我们将用于改进体验，必要时可能联系你核实问题。',
        confirmText: '提交',
        cancelText: '取消',
        success: (r) => {
          if (!r.confirm) return;

          uni.showLoading({ title: '提交中…', mask: true });
          (async () => {
            try {
              // 上传图片（最多3张）
              const imageUrls = [];
              for (const p of this.images) {
                if (!p) continue;
                if (/^https?:\/\//i.test(p)) {
                  imageUrls.push(p);
                  continue;
                }
                const up = await upload(p, { bizType: 'feedback' });
                const url = (up && up.url) || '';
                if (url) imageUrls.push(url);
              }

              // ✅ 提交到后端
              const remotePayload = {
                type: payload.type,
                content: payload.content,
                contact: payload.contact,
                imageUrls
              };
              const ret = await submitFeedback(remotePayload);

              // 将后端返回的 id/time 覆盖本地记录（若有）
              const stored = {
                ...payload,
                id: (ret && ret.id) || payload.id,
                time: (ret && ret.time) || payload.time,
                images: imageUrls
              };

              const arr = getScopedStorageSync('feedback_list', []);
              const list = Array.isArray(arr) ? arr : [];
              list.unshift(stored);
              setScopedStorageSync('feedback_list', list);

              this.form.content = '';
              this.form.contact = '';
              this.images = [];
              this.loadRecent();

              uni.showToast({ title: '已提交', icon: 'success' });
            } catch (err) {
              if (USE_MOCK_FALLBACK) {
                const arr = getScopedStorageSync('feedback_list', []);
                const list = Array.isArray(arr) ? arr : [];
                list.unshift(payload);
                setScopedStorageSync('feedback_list', list);

                this.form.content = '';
                this.form.contact = '';
                this.images = [];
                this.loadRecent();

                uni.showToast({ title: '已提交（本地）', icon: 'success' });
              } else {
                uni.showToast({ title: (err && err.message) || '提交失败', icon: 'none' });
              }
            } finally {
              uni.hideLoading();
            }
          })();
        }
      });
    }
  }
};
</script>

<style src="./feedback.css"></style>
