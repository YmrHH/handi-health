<template>
    <view class="page alerts-page">
        <!-- 待随访 -->
        <view class="section">
            <view class="section-title">待随访</view>
            <block v-for="(item, index) in todoList" :key="index">
                <view class="card">
                    <view class="row-top">
                        <view>
                            <view class="name">{{ item.name }}</view>
                            <view class="sub">{{ item.desc }}</view>
                        </view>
                        <view :class="'risk-tag ' + item.riskLevel">{{ item.riskText }}</view>
                    </view>
                    <view class="row-bottom">
                        <view class="time">计划时间：{{ item.planTime }}</view>
                        <button class="btn-primary" size="mini" :data-id="item.id" @tap="onStartFollowup">开始随访</button>
                    </view>
                </view>
            </block>
        </view>

        <!-- 已超期 -->
        <view class="section">
            <view class="section-title">已超期</view>
            <block v-for="(item, index) in overdueList" :key="index">
                <view class="card">
                    <view class="row-top">
                        <view>
                            <view class="name">{{ item.name }}</view>
                            <view class="sub">
                                {{ item.desc }}
                            </view>
                        </view>
                        <view class="status-overdue">超期 {{ item.overdueDays }} 天</view>
                    </view>
                    <view class="row-bottom">
                        <view class="time">原计划：{{ item.planTime }}</view>
                        <button class="btn-secondary" size="mini" :data-id="item.id" @tap="onReschedule">重新安排</button>
                    </view>
                </view>
            </block>
        </view>

        <!-- 已完成 -->
        <view class="section">
            <view class="section-title">已完成</view>
            <block v-for="(item, index) in doneList" :key="index">
                <view class="card">
                    <view class="row-top">
                        <view>
                            <view class="name">{{ item.name }}</view>
                            <view class="sub">完成时间：{{ item.doneTime }}</view>
                        </view>
                        <view class="status-done">已完成</view>
                    </view>
                    <view class="row-bottom">
                        <view class="time">{{ item.summary }}</view>
                        <button class="btn-outline" size="mini" :data-id="item.id" @tap="onViewDetail">查看 / 修改</button>
                    </view>
                </view>
            </block>
        </view>
    </view>
</template>

<script>
// pages/staff/alerts/alerts.js
import { USE_MOCK_FALLBACK } from '@/config/api.js';
import { getAlerts } from '@/api/staff.js';

export default {
    data() {
        return {
            todoList: [],
            overdueList: [],
            doneList: []
        };
    },
    onLoad() {
        this.loadAlerts();
    },
    methods: {
        loadAlerts() {
            getAlerts()
                .then((res) => {
                    const list = Array.isArray(res) ? res : (res && res.alerts) || [];
                    const normalized = list.map((it) => ({
                        id: it.id,
                        name: it.patientName || it.name,
                        desc: it.aiSummary || it.desc || '',
                        planTime: it.planTime || it.planText || '',
                        riskLevel: it.riskLevel || 'mid',
                        riskText: it.riskText,
                        status: it.status || 'todo',
                        overdue: !!it.overdue
                    }));
                    const todoList = normalized.filter((a) => a.status === 'todo');
                    const doneList = normalized.filter((a) => a.status === 'done');
                    const overdueList = normalized.filter((a) => a.overdue || a.status === 'overdue');
                    this.setData({ todoList, overdueList, doneList });
                })
                .catch(() => {
                    if (USE_MOCK_FALLBACK) return this.loadDemoData();
                    this.setData({ todoList: [], overdueList: [], doneList: [] });
                });
        },

        // 联调阶段兜底 demo
        loadDemoData() {
            this.setData({
                todoList: [
                    {
                        id: 'a1',
                        name: '张阿姨',
                        desc: '近3天血压偏高，体温偏高 0.8℃，夜间憋醒次数增多。',
                        planTime: '今天 09:30',
                        riskLevel: 'high',
                        riskText: '高风险'
                    }
                ],
                overdueList: [
                    {
                        id: 'a2',
                        name: '李叔叔',
                        desc: '上次随访后未按时复查血脂，需要重新安排随访时间。',
                        planTime: '2025-11-25',
                        overdueDays: 5
                    }
                ],
                doneList: [
                    {
                        id: 'a3',
                        name: '王大爷',
                        doneTime: '2025-11-30',
                        summary: '电话随访，病情稳定，按时服药、血压控制良好。'
                    }
                ]
            });
        },

        // 待随访：开始随访
        onStartFollowup(e) {
            const id = e.currentTarget.dataset.id;
            uni.navigateTo({
                url: `/pages/staff/followup/followup?alertId=${id}&mode=new`
            });
        },

        // 已超期：重新安排
        onReschedule(e) {
            const id = e.currentTarget.dataset.id;
            uni.navigateTo({
                url: `/pages/staff/followup/followup?alertId=${id}&mode=reschedule`
            });
        },

        // 已完成：查看 / 修改
        onViewDetail(e) {
            const id = e.currentTarget.dataset.id;
            uni.navigateTo({
                url: `/pages/staff/followup/followup?alertId=${id}&mode=edit`
            });
        }
    }
};
</script>
<style>
@import './alerts.css';
</style>
