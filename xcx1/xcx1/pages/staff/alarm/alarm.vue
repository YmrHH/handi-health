<template>
    <!-- pages/staff/alerts/alerts.wxml -->
    <view class="page alerts-page">
        <view class="card header-card">
            <view class="header-title">今日告警任务</view>
            <view class="header-sub">共 {{ summary.total }} 条，待处理 {{ summary.todo }} 条</view>

            <view class="filter-row">
                <view :class="'filter-chip ' + (activeFilter === 'all' ? 'active' : '')" data-filter="all" @tap="onChangeFilter">全部</view>
                <view :class="'filter-chip ' + (activeFilter === 'bp' ? 'active' : '')" data-filter="bp" @tap="onChangeFilter">血压异常</view>
                <view :class="'filter-chip ' + (activeFilter === 'weight' ? 'active' : '')" data-filter="weight" @tap="onChangeFilter">体温异常</view>
                <view :class="'filter-chip ' + (activeFilter === 'symptom' ? 'active' : '')" data-filter="symptom" @tap="onChangeFilter">症状加重</view>
            </view>
        </view>

        <view class="card alert-card" @tap="onOpenAlert" :data-id="item.id" v-for="(item, index) in visibleAlerts" :key="index">
            <view class="alert-top">
                <view>
                    <view class="alert-name">{{ item.patientName }}（{{ item.age }}岁{{ item.gender }}）</view>
                    <view class="alert-main-disease">
                        {{ item.mainDisease }}
                    </view>
                </view>
                <view :class="'risk-tag ' + item.riskLevel">
                    {{ item.riskText }}
                </view>
            </view>

            <view class="alert-middle">
                <view class="alert-reason">
                    告警原因：
                    <text class="reason-chip" v-for="(item, index1) in item.reasons" :key="index1">
                        {{ item }}
                    </text>
                </view>
                <view class="alert-latest">最近血压 {{ item.latestBP || '--' }} · 体温 {{ item.latestWeight || '--' }}℃</view>
            </view>

            <view class="alert-bottom">
                <view class="alert-plan">当前计划：{{ item.planText }} ｜ 下次随访：{{ item.nextVisitDate || '待制定' }}</view>
                <view :class="'alert-status ' + item.status">
                    {{ item.status === 'done' ? '已处理' : '待处理' }}
                </view>
            </view>
        </view>

        <view v-if="!visibleAlerts || visibleAlerts.length === 0" class="empty">当前筛选条件下暂无告警任务。</view>
    </view>
</template>

<script>
// pages/staff/alerts/alerts.js
import { USE_MOCK_FALLBACK } from '@/config/api.js';
import { getAlerts } from '@/api/staff.js';

export default {
    data() {
        return {
            activeFilter: 'all',
            summary: {
                total: 0,
                todo: 0
            },
            alerts: [],
            visibleAlerts: []
        };
    },
    onLoad() {
        this.fetchAlerts();
    },
    onShow() {
        this.fetchAlerts();
    },
    methods: {
        fetchAlerts() {
            // ✅ 优先从后端获取
            getAlerts({ filter: this.activeFilter })
                .then((res) => {
                    const list = Array.isArray(res) ? res : (res && res.alerts) || [];
                    const total = list.length;
                    const todo = list.filter((a) => (a.status || 'todo') === 'todo').length;
                    this.setData(
                        {
                            alerts: list,
                            summary: { total, todo }
                        },
                        () => {
                            this.updateVisibleAlerts();
                        }
                    );
                })
                .catch(() => {
                    if (!USE_MOCK_FALLBACK) {
                        this.setData({ alerts: [], summary: { total: 0, todo: 0 } }, () => this.updateVisibleAlerts());
                        return;
                    }

                    // 联调阶段兜底 Demo 数据
                    const alerts = [
                {
                    id: 'a1',
                    patientId: 'p1',
                    patientName: '张阿姨',
                    age: 68,
                    gender: '女',
                    mainDisease: '慢性心衰 + 高血压',
                    reasons: ['血压偏高', '体温3天偏高', '夜间憋醒增加'],
                    typeFlags: ['bp', 'weight', 'symptom'],
                    latestBP: '162/96',
                    latestWeight: '37.8',
                    riskLevel: 'high',
                    riskText: '高风险',
                    status: 'todo',
                    planText: '需尽快电话评估',
                    nextVisitDate: ''
                },
                {
                    id: 'a2',
                    patientId: 'p2',
                    patientName: '李先生',
                    age: 72,
                    gender: '男',
                    mainDisease: '冠心病 + 糖尿病',
                    reasons: ['夜间胸闷', '睡眠差'],
                    typeFlags: ['symptom'],
                    latestBP: '132/78',
                    latestWeight: '36.7',
                    riskLevel: 'mid',
                    riskText: '中风险',
                    status: 'todo',
                    planText: '安排一周内门诊复查',
                    nextVisitDate: '2025-12-15'
                },
                {
                    id: 'a3',
                    patientId: 'p3',
                    patientName: '王大爷',
                    age: 75,
                    gender: '男',
                    mainDisease: '高血压',
                    reasons: ['血压偏高'],
                    typeFlags: ['bp'],
                    latestBP: '158/90',
                    latestWeight: '36.5',
                    riskLevel: 'mid',
                    riskText: '中风险',
                    status: 'done',
                    planText: '已电话随访，三天后复测',
                    nextVisitDate: '2025-12-10'
                }
            ];
                    const total = alerts.length;
                    const todo = alerts.filter((a) => a.status === 'todo').length;
                    this.setData(
                        {
                            alerts,
                            summary: { total, todo }
                        },
                        () => {
                            this.updateVisibleAlerts();
                        }
                    );
                });
        },

        onChangeFilter(e) {
            const filter = e.currentTarget.dataset.filter;
            if (filter === this.activeFilter) {
                return;
            }
            this.setData(
                {
                    activeFilter: filter
                },
                () => {
                    this.updateVisibleAlerts();
                }
            );
        },

        updateVisibleAlerts() {
            const { alerts, activeFilter } = this;
            let list = alerts;
            if (activeFilter === 'bp') {
                list = alerts.filter((a) => a.typeFlags.includes('bp'));
            } else if (activeFilter === 'weight') {
                list = alerts.filter((a) => a.typeFlags.includes('weight'));
            } else if (activeFilter === 'symptom') {
                list = alerts.filter((a) => a.typeFlags.includes('symptom'));
            }
            this.setData({
                visibleAlerts: list
            });
        },

        onOpenAlert(e) {
            const id = e.currentTarget.dataset.id;
            uni.navigateTo({
                url: `/pages/staff/followup/followup?alertId=${id}`
            });
        }
    }
};
</script>
<style>
@import './alarm.css';
</style>
