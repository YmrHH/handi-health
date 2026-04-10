<template>
    <view class="page tongue-page">
        <view class="card">
            <view class="title">舌象拍照说明</view>
            <view class="tip-list">
                <view class="tip-item">1. 在光线明亮的地方，尽量用自然光。</view>
                <view class="tip-item">2. 去掉口红、口香糖等，张口自然伸舌。</view>
                <view class="tip-item">3. 拍照时手机离口部约 10～15 cm。</view>
            </view>
        </view>

        <view class="card">
            <view class="title">舌象照片</view>

            <view class="preview-wrap">
                <image v-if="imagePath" :src="imagePath" mode="aspectFill" class="preview-img" @tap="onPreview" />
                <view v-else class="preview-placeholder" @tap="onTakePhoto">
                    <view class="camera-icon">📷</view>
                    <view class="camera-text">点击上传舌象</view>
                </view>
            </view>

            <view v-if="imagePath" class="btn-row">
                <button class="btn-secondary" @tap="onRetake">重拍</button>
                <button class="btn-primary" :loading="uploading" :disabled="uploaded || uploading" @tap="onUpload">
                    {{ uploaded ? '已上传' : uploading ? '上传中...' : '上传舌象' }}
                </button>
            </view>

            <view v-if="uploaded" class="upload-tip">上传时间：{{ uploadedAt }}，如舌象有明显变化可重新拍摄上传。</view>
        </view>
    </view>
</template>

<script>
import { USE_MOCK_FALLBACK, API_BASE_URL } from '@/config/api.js';
import { upload } from '@/api/common.js';
import { getUser } from '@/utils/session.js';

export default {
    data() {
        return {
            imagePath: '',
            uploading: false,
            uploaded: false,
            uploadedAt: '',
            patientId: ''
        };
    },

    onLoad() {
        const user = getUser();
        this.patientId = (user && (user.patientId || user.id)) || '';
        this.restoreLastTongue();
    },

    onShow() {
        this.restoreLastTongue();
    },

    methods: {
        normalizeUrl(u) {
            const s = String(u || '').trim();
            if (!s) return '';
            if (/^https?:\/\//i.test(s)) return s;
            if (/^(wxfile|file):\/\//i.test(s)) return s;
            if (/^data:image\//i.test(s)) return s;

            const base = String(API_BASE_URL || '').replace(/\/$/, '');
            if (!base) return s;

            const rel = s.startsWith('/') ? s : '/' + s;
            return base + rel;
        },

        restoreLastTongue() {
            const last = uni.getStorageSync('tcm_tongue_last');
            if (!last || !(last.imagePath || last.url)) return;

            const raw = last.url || last.imagePath;
            const src = this.normalizeUrl(raw) || raw;

            this.setData({
                imagePath: src,
                uploaded: !!last.uploadedAt,
                uploadedAt: last.uploadedAt || ''
            });
        },

        onTakePhoto() {
            uni.chooseImage({
                count: 1,
                sizeType: ['compressed'],
                sourceType: ['camera'],
                success: (res) => {
                    const path = res.tempFilePaths[0];
                    this.setData({
                        imagePath: path,
                        uploaded: false,
                        uploadedAt: ''
                    });
                }
            });
        },

        onRetake() {
            this.onTakePhoto();
        },

        onPreview() {
            if (!this.imagePath) return;
            uni.previewImage({
                urls: [this.imagePath]
            });
        },

        onUpload() {
            if (!this.imagePath) {
                uni.showToast({
                    title: '请先拍摄舌象',
                    icon: 'none'
                });
                return;
            }

            this.setData({ uploading: true });

            upload(this.imagePath, {
                bizType: 'tongue',
                patientId: this.patientId,
                takenAt: new Date().toISOString()
            })
                .then((data) => {
                    const now = this.formatNow();
                    const rawUrl = (data && data.url) || '';
                    const url = this.normalizeUrl(rawUrl);
                    const fileId = (data && (data.fileId || data.id)) || '';

                    const showSrc = url || this.imagePath;

                    this.setData({
                        uploading: false,
                        uploaded: true,
                        uploadedAt: now,
                        imagePath: showSrc
                    });

                    uni.setStorageSync('tcm_tongue_last', {
                        imagePath: showSrc,
                        url,
                        uploadedAt: now,
                        fileId
                    });

                    uni.showToast({
                        title: '上传成功',
                        icon: 'success'
                    });
                })
                .catch((err) => {
                    this.setData({ uploading: false });
                    uni.showToast({
                        title: USE_MOCK_FALLBACK ? '上传失败（可先离线保存）' : ((err && err.message) || '上传失败'),
                        icon: 'none'
                    });
                });
        },

        formatNow() {
            const d = new Date();
            const y = d.getFullYear();
            const m = ('0' + (d.getMonth() + 1)).slice(-2);
            const day = ('0' + d.getDate()).slice(-2);
            const h = ('0' + d.getHours()).slice(-2);
            const mi = ('0' + d.getMinutes()).slice(-2);
            return `${y}-${m}-${day} ${h}:${mi}`;
        }
    }
};
</script>

<style>
@import './tongue.css';
</style>