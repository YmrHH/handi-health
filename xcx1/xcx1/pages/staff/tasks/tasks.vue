<template>
    <!-- pages/staff/tasks/tasks.wxml -->
    <view class="app-page staff-page">
        <!-- 顶部概览 -->
        <view class="app-card app-card--glass staff-card">
            <view class="staff-header">
                <view>
                    <view class="staff-title">今日随访任务</view>
                    <view class="staff-sub">共 {{ summary.total }} 条，已完成 {{ summary.done }} 条， 紧急告警 {{ summary.urgent }} 条。</view>
                </view>
            </view>

            <!-- 筛选 Tabs -->
            <view class="filter-tabs">
                <text
                    :data-value="item.value"
                    :class="'filter-tab ' + (activeFilter === item.value ? 'active' : '')"
                    @tap="onChangeFilter"
                    v-for="(item, index) in filters"
                    :key="index"
                >
                    {{ item.label }}
                </text>
            </view>
        </view>

        <!-- 任务列表 -->
        <view class="app-card staff-card">
            <block v-for="(item, index) in visibleTasks" :key="index">
                <view class="task-card app-card" @tap="onOpenTask" :data-id="item.id">
                    <view class="task-top">
                        <view>
                            <view class="task-patient">
                                {{ item.patientName || item.name || '未知患者' }}
                                <text v-if="item.age || item.gender" class="task-age">· {{ item.age || '' }}岁 · {{ item.gender || '' }}</text>
                            </view>
                            <view class="task-type">{{ item.typeText || item.type || '随访任务' }}</view>
                        </view>

                        <view :class="'task-risk ' + item.riskLevel">
                            {{ item.riskText }}
                        </view>
                    </view>

                    <view class="task-middle">
                        <view class="task-time">计划时间：{{ item.planTime }}</view>
                        <view class="task-ai">{{ item.aiSummary || item.desc || '' }}</view>
                    </view>

                    <view class="task-bottom">
                        <view class="task-tags">
                            <text v-if="item.fromAlert" class="tag tag-alert">告警触发</text>
                            <text v-if="item.overdue" class="tag tag-overdue">已超期</text>
                        </view>
                        <view class="task-status-text">{{ item.statusText || (item.status === 'done' ? '已完成' : (item.overdue ? '已超期' : '待处理')) }}</view>
                    </view>
                </view>
            </block>

            <view v-if="visibleTasks.length === 0" class="empty-tip">当前筛选条件下暂无任务。</view>
        </view>
      <staff-tabbar active="tasks"></staff-tabbar>
    </view>
</template>

<script>
// pages/staff/tasks/tasks.js
import StaffTabbar from '@/components/staff-tabbar/staff-tabbar.vue';
import { USE_MOCK_FALLBACK } from '@/config/api.js';
import { getTasks } from '@/api/staff.js';

export default {
    components: { StaffTabbar },
    data() {
        return {
            filters: [
                {
                    value: 'today',
                    label: '今日'
                },
                {
                    value: 'urgent',
                    label: '紧急告警'
                },
                {
                    value: 'overdue',
                    label: '已超期'
                },
                {
                    value: 'done',
                    label: '已完成'
                }
            ],
            activeFilter: 'today',
            summary: {
                total: 0,
                done: 0,
                urgent: 0
            },
            tasks: [],
            visibleTasks: []
        };
    },
    onLoad() {
        this.fetchTasksFromServer();
    },
    onShow() {
        // 随访后返回可刷新
        this.fetchTasksFromServer();
    },
    methods: {
        onChangeFilter(e) {
            const value = e.currentTarget.dataset.value;
            if (value === this.activeFilter) {
                return;
            }
            this.setData(
                {
                    activeFilter: value
                },
                () => {
                    this.updateVisibleTasks();
                }
            );
        },

        onOpenTask(e) {
            const id = e.currentTarget.dataset.id;
            // 传递关键展示信息，保证详情页顶部可立即显示患者信息（即使详情接口暂不可用）
            const item = (this.visibleTasks || []).find((x) => String(x.id) === String(id)) || {};
            const q = [];
            q.push(`taskId=${encodeURIComponent(id)}`);
            if (item.patientId) q.push(`patientId=${encodeURIComponent(item.patientId)}`);
            if (item.patientName) q.push(`patientName=${encodeURIComponent(item.patientName)}`);
            if (item.age != null && item.age !== '') q.push(`age=${encodeURIComponent(item.age)}`);
            if (item.gender) q.push(`gender=${encodeURIComponent(item.gender)}`);
            if (item.typeText) q.push(`typeText=${encodeURIComponent(item.typeText)}`);
            if (item.planTime) q.push(`planTime=${encodeURIComponent(item.planTime)}`);
            if (item.aiSummary) q.push(`aiSummary=${encodeURIComponent(item.aiSummary)}`);
            if (item.riskLevel) q.push(`riskLevel=${encodeURIComponent(item.riskLevel)}`);
            if (item.riskText) q.push(`riskText=${encodeURIComponent(item.riskText)}`);
            uni.navigateTo({
                url: `/pages/staff/followup/followup?${q.join('&')}`
            });
        },

        // ===== 数据相关 ===== //

        fetchTasksFromServer() {
            // ✅ 优先从后端获取
            getTasks({ filter: this.activeFilter })
                .then((res) => {
                    const list = Array.isArray(res) ? res : (res && res.tasks) || [];
                    const total = list.length;
                    const done = list.filter((t) => t.status === 'done').length;
                    const urgent = list.filter((t) => t.riskLevel === 'high' && t.status !== 'done').length;
                    this.setData(
                        {
                            tasks: list,
                            summary: { total, done, urgent }
                        },
                        () => this.updateVisibleTasks()
                    );
                })
                .catch(() => {
                    // 联调阶段兜底 demo
                    if (!USE_MOCK_FALLBACK) {
                        this.setData(
                            { tasks: [], summary: { total: 0, done: 0, urgent: 0 } },
                            () => this.updateVisibleTasks()
                        );
                        return;
                    }

                    const demoTasks = [
                        {
                            id: 'task1',
                            patientName: '张阿姨',
                            age: 68,
                            gender: '女',
                            type: 'phone',
                            typeText: '电话随访',
                            riskLevel: 'high',
                            riskText: '高风险',
                            planTime: '今天 09:30',
                            aiSummary: '近3天血压波动偏大，夜间憋醒2次。',
                            fromAlert: true,
                            overdue: false,
                            status: 'todo',
                            statusText: '待处理'
                        },
                        {
                            id: 'task2',
                            patientName: '李先生',
                            age: 72,
                            gender: '男',
                            type: 'home',
                            typeText: '上门随访',
                            riskLevel: 'mid',
                            riskText: '中风险',
                            planTime: '今天 14:00',
                            aiSummary: '体温略偏高，步行完成度较低。',
                            fromAlert: false,
                            overdue: false,
                            status: 'todo',
                            statusText: '待处理'
                        },
                        {
                            id: 'task3',
                            patientName: '王大爷',
                            age: 75,
                            gender: '男',
                            type: 'phone',
                            typeText: '电话随访',
                            riskLevel: 'low',
                            riskText: '低风险',
                            planTime: '昨天 10:00',
                            aiSummary: '近一周指标总体平稳。',
                            fromAlert: false,
                            overdue: true,
                            status: 'todo',
                            statusText: '已超期'
                        },
                        {
                            id: 'task4',
                            patientName: '赵女士',
                            age: 64,
                            gender: '女',
                            type: 'clinic',
                            typeText: '门诊复诊提醒',
                            riskLevel: 'mid',
                            riskText: '中风险',
                            planTime: '今天 16:00',
                            aiSummary: '已完成门诊复查，待记录结果。',
                            fromAlert: false,
                            overdue: false,
                            status: 'done',
                            statusText: '已完成'
                        }
                    ];
                    const total = demoTasks.length;
                    const done = demoTasks.filter((t) => t.status === 'done').length;
                    const urgent = demoTasks.filter((t) => t.riskLevel === 'high' && t.status !== 'done').length;
                    this.setData(
                        {
                            tasks: demoTasks,
                            summary: { total, done, urgent }
                        },
                        () => this.updateVisibleTasks()
                    );
                });
        },

        updateVisibleTasks() {
            const { tasks, activeFilter } = this;
            let list = tasks;
            if (activeFilter === 'urgent') {
                list = tasks.filter((t) => t.riskLevel === 'high' && t.status !== 'done');
            } else if (activeFilter === 'overdue') {
                list = tasks.filter((t) => t.overdue);
            } else if (activeFilter === 'done') {
                list = tasks.filter((t) => t.status === 'done');
            } else {
                // today：默认不过滤（接口最好只给今日任务）
                list = tasks;
            }
            this.setData({
                visibleTasks: list
            });
        }
    }
};
</script>
<style>
@import './tasks.css';
</style>
