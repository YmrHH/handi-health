<template>
    <!-- pages/index/index.wxml -->
    <view class="page login-page">
        <!-- 顶部小标题：角色选择 -->
        <view class="page-title">角色选择</view>

        <!-- 顶部蓝色渐变卡片 -->
        <view class="hero-card">
            <view class="hero-title-main">寒地慢性病</view>
            <view class="hero-title-main">中医医疗系统</view>
            <view class="hero-sub">专业的慢病管理与健康监测服务平台</view>
            <view class="hero-tip">请选择登录端口</view>
        </view>

        <!-- 端口选择卡片 -->
        <view class="card role-card">
            <view class="role-row">
                <view :class="'role-item ' + (role === 'patient' ? 'active' : '')" data-role="patient" @tap="onSelectRole">
                    <view class="role-name">患者 / 家属端</view>
                    <view class="role-desc">上传监测数据 完成康养任务</view>
                </view>

                <view :class="'role-item ' + (role === 'staff' ? 'active' : '')" data-role="staff" @tap="onSelectRole">
                    <view class="role-name">医护端</view>
                    <view class="role-desc">处理随访任务 记录随访结果</view>
                </view>
            </view>

            <!-- 当前选择提示 -->
            <view class="role-tip" v-if="!role">请先在上方选择一个端口</view>
            <view class="role-tip role-selected" v-else-if="role === 'patient'">当前端口：患者 / 家属端</view>
            <view class="role-tip role-selected" v-else-if="role === 'staff'">当前端口：医护端</view>
        </view>

        <!-- 中间大白卡：未选择时做占位，选择后显示手机号密码登录 -->
        <view class="card form-card">
            <!-- 未选择角色时：提示卡片 -->
            <view v-if="!role" class="form-placeholder">
                <view class="form-placeholder-icon"></view>
                <view class="form-placeholder-title">请先在上一步选择您的身份</view>
                <view class="form-placeholder-sub">选择后即可输入手机号和密码</view>
            </view>

            <!-- 已选择角色时：手机号密码表单 -->
            <view v-else>
                <view class="form-title">手机号密码登录</view>

                <!-- 手机号 -->
                <view class="field">
                    <view class="field-label">手机号</view>
                    <view class="field-input">
                        <input
                            type="number"
                            maxlength="11"
                            placeholder="请输入手机号"
                            placeholder-class="input-placeholder"
                            :value="form.phone"
                            @input="onInput"
                            data-field="phone"
                        />
                    </view>
                </view>

                <!-- 密码 -->
                <view class="field">
                    <view class="field-label">登录密码</view>
                    <view class="field-input">
                        <input
                            type="password"
                            placeholder="请输入密码"
                            placeholder-class="input-placeholder"
                            :value="form.password"
                            @input="onInput"
                            data-field="password"
                        />
                    </view>
                </view>

                <!-- 登录按钮 -->
                <button class="btn-primary login-btn" @tap="onLogin">登录</button>
            </view>
        </view>

        <!-- 底部说明 -->
        <view class="tip-blue bottom-tip">本小程序仅用于慢性病随访与健康管理提示，不能替代医生面对面诊疗。 如出现严重不适，请立即就医或拨打急救电话。</view>
    </view>
</template>

<script>
// pages/index/index.js
import { loginByPhone } from '@/api/auth.js';
import { setRole, setToken, setUser } from '@/utils/session.js';

export default {
    data() {
        return {
            // 端口：patient 患者端 / staff 医护端 / '' 未选择
            role: '',
            form: {
                phone: '',
                password: ''
            }
        };
    },
    methods: {
        // 选择端口
        onSelectRole(e) {
            const role = e.currentTarget.dataset.role;
            this.role = role;
        },

        // 输入手机号 / 密码
        onInput(e) {
            const field = e.currentTarget.dataset.field;
            const value = e.detail.value;
            this.form = {
                ...this.form,
                [field]: value
            };
        },

        // 点击登录
        onLogin() {
            const { role, form } = this;

            if (!role) {
                uni.showToast({
                    title: '请先选择端口',
                    icon: 'none'
                });
                return;
            }

            const phone = (form.phone || '').trim();
            const password = (form.password || '').trim();

            if (!/^1\d{10}$/.test(phone)) {
                uni.showToast({
                    title: '请输入正确手机号',
                    icon: 'none'
                });
                return;
            }

            if (!password) {
                uni.showToast({
                    title: '请输入密码',
                    icon: 'none'
                });
                return;
            }

            uni.showLoading({ title: '登录中…', mask: true });

            loginByPhone({
                phone,
                password
            })
                .then((res) => {
                    const token = (res && res.token) || '';
                    const user = (res && res.user) || null;

                    const backendRole = String((user && (user.backendRole || user.role)) || '')
                        .trim()
                        .toUpperCase();

                    const okPatient = role === 'patient' && backendRole === 'PATIENT';
                    const okStaff = role === 'staff' && (backendRole === 'DOCTOR' || backendRole === 'FOLLOW_UP');

                    if (!(okPatient || okStaff)) {
                        throw { message: `该手机号账号角色为 ${backendRole || '未知'}，与当前选择端口不匹配` };
                    }

                    setRole(role);
                    setToken(token);
                    if (user) setUser(user);

                    uni.showToast({ title: '登录成功', icon: 'success' });

                    const url = role === 'patient'
                        ? '/pages/patient/home/home'
                        : '/pages/staff/tasks/tasks';

                    setTimeout(() => {
                        uni.reLaunch({ url });
                    }, 200);
                })
                .catch((err) => {
                    console.error('loginByPhone failed:', err);
                    uni.showToast({
                        title: (err && err.message) || '手机号或密码错误',
                        icon: 'none'
                    });
                })
                .finally(() => {
                    uni.hideLoading();
                });
        }
    }
};
</script>
<style>
@import "./index.css";
</style>