<template>
    <!-- pages/patient/rehab/rehab.wxml -->
    <view class="page">
        <!-- 顶部概览 -->
        <view class="section">
            <view class="rehab-header">
                <view>
                    <view class="rehab-title">今日康养任务</view>
                    <view class="rehab-sub">已完成 {{ summary.done }} / {{ summary.total }} 项，系统会结合您今日上报数据生成运动与饮食康养任务。</view>
                </view>
                <view class="rehab-score">
                    <view class="rehab-score-num">{{ summary.streak }}</view>
                    <view class="rehab-score-label">连续打卡天数</view>
                </view>
            </view>

            <!-- 筛选 tabs -->
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
        <view class="section">
            <view v-if="summary.overdue > 0" class="overdue-banner">今日有 {{ summary.overdue }} 项康养任务已超时，请优先完成。</view>
            <block v-for="(item, index) in visibleTasks" :key="index">
                <view class="task-card">
                    <view class="task-main">
                        <view class="task-left">
                            <view class="task-icon">{{ item.icon }}</view>
                            <view>
                                <view class="task-title">{{ item.title }}</view>
                                <view class="task-meta">{{ item.category }} · 约 {{ item.duration }} 分钟</view>
                                <view class="task-desc">{{ item.desc }}</view>
                                <view class="task-deadline" :class="item.overdue ? 'task-deadline overdue' : 'task-deadline'">{{ item.deadlineText || ('建议在 ' + (item.dueTime || '20:00') + ' 前完成') }}</view>
                            </view>
                        </view>

                        <view class="task-status">
                            <view :class="'status-tag ' + (item.status === 'done' ? 'status-done' : (item.overdue ? 'status-overdue' : 'status-todo'))">
                                {{ item.status === 'done' ? '已完成' : (item.overdue ? '已超时' : '待完成') }}
                            </view>
                        </view>
                    </view>

                    <!-- 难度反馈 -->
                    <view class="task-footer">
                        <view class="difficulty-label">难度感觉：</view>
                        <view class="difficulty-buttons">
                            <button size="mini" :class="'diff-btn ' + (item.difficulty === 'hard' ? 'active' : '')" :data-id="item.id" data-level="hard" @tap="onSetDifficulty">
                                偏难
                            </button>
                            <button size="mini" :class="'diff-btn ' + (item.difficulty === 'just' ? 'active' : '')" :data-id="item.id" data-level="just" @tap="onSetDifficulty">
                                正好
                            </button>
                            <button size="mini" :class="'diff-btn ' + (item.difficulty === 'easy' ? 'active' : '')" :data-id="item.id" data-level="easy" @tap="onSetDifficulty">
                                偏轻
                            </button>
                        </view>

                        <button size="mini" :class="'task-btn ' + (item.status === 'done' ? 'task-btn-done' : '')" :data-id="item.id" @tap="onToggleDone">
                            {{ item.status === 'done' ? '修改状态' : '标记完成' }}
                        </button>
                    </view>
                </view>
            </block>

            <view v-if="visibleTasks.length === 0" class="empty-tip">今日暂无任务。后续将根据医生建议为您定制康养计划。</view>
        </view>

        <!-- 底部提示 -->
        <view class="section">
            <view class="tip-blue">温馨提示：康养任务仅为健康管理建议，不能替代医生面对面诊疗。 如出现胸闷、气短、明显乏力等不适，请及时就医或联系随访人员。</view>
        </view>
    </view>
</template>

<script>
// pages/patient/rehab/rehab.js
import { USE_MOCK_FALLBACK } from '@/config/api.js';
import { getRehabTasks, updateRehabTaskStatus, updateRehabTaskDifficulty } from '@/api/patient.js';
import { setScopedStorageSync } from '@/utils/session.js';

export default {
    data() {
        return {
            filters: [
                {
                    value: 'all',
                    label: '全部'
                },
                {
                    value: 'todo',
                    label: '待完成'
                },
                {
                    value: 'done',
                    label: '已完成'
                }
            ],
            activeFilter: 'todo',
            summary: {
                total: 0,
                done: 0,
                streak: 3, // 连续打卡天数 demo
                overdue: 0
            },
            tasks: [],
            visibleTasks: []
        };
    },
    onLoad() {
        this.fetchTasksFromServer();
    },
    methods: {
        // 切换筛选
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

        // 标记完成/取消完成
        onToggleDone(e) {
            const id = e.currentTarget.dataset.id;
            const tasks = this.tasks.map((item) => {
                if (item.id === id) {
                    item.status = item.status === 'done' ? 'todo' : 'done';
                }
                return item;
            });
            // 取当前最新状态
            const cur = tasks.find((t) => t.id === id);
            this.setData(
                {
                    tasks
                },
                () => {
                    this.updateSummary();
                    this.updateVisibleTasks();
                    this.saveTaskStatus(id, cur ? cur.status : undefined);
                }
            );
        },

        // 选择难度
        onSetDifficulty(e) {
            const { id, level } = e.currentTarget.dataset;
            const tasks = this.tasks.map((item) => {
                if (item.id === id) {
                    item.difficulty = level;
                }
                return item;
            });
            this.setData(
                {
                    tasks
                },
                () => {
                    this.saveTaskDifficulty(id, level);
                }
            );
        },

        // ===== 数据与统计 ===== //

        fetchTasksFromServer() {
            const now = new Date();
            const pad = (n) => (n < 10 ? '0' + n : '' + n);
            const dateStr = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;

            getRehabTasks({ date: dateStr })
                .then((res) => {
                    const list = this.normalizeTasks(Array.isArray(res) ? res : (res && res.tasks) || []);
                    const streak = (res && res.streak) != null ? res.streak : this.summary.streak;
                    const overdue = list.filter((item) => item.overdue && item.status !== 'done').length;
                    this.setData(
                        {
                            tasks: list,
                            summary: { ...this.summary, streak, overdue }
                        },
                        () => {
                            this.updateSummary();
                            this.updateVisibleTasks();
                            if (overdue > 0) {
                                uni.showToast({ title: `有 ${overdue} 项任务已超时`, icon: 'none' });
                            }
                        }
                    );
                })
                .catch(() => {
                    if (!USE_MOCK_FALLBACK) {
                        this.setData({ tasks: [] }, () => {
                            this.updateSummary();
                            this.updateVisibleTasks();
                        });
                        return;
                    }

                    // 兜底 demo 数据（联调阶段）
                    const demoTasks = [
                {
                    id: 't1',
                    icon: '🚶',
                    title: '缓步步行',
                    category: '运动',
                    duration: 15,
                    desc: '在平地缓步行走，可略微出汗但不气喘。',
                    status: 'todo',
                    difficulty: 'just'
                },
                {
                    id: 't2',
                    icon: '🧘',
                    title: '腹式呼吸',
                    category: '呼吸操',
                    duration: 10,
                    desc: '坐位或仰卧，缓慢深呼吸，以腹部起伏为主。',
                    status: 'todo',
                    difficulty: 'just'
                },
                {
                    id: 't3',
                    icon: '☕',
                    title: '温阳泡脚',
                    category: '足浴',
                    duration: 20,
                    desc: '睡前用温水泡脚，注意水温不宜过高。',
                    status: 'done',
                    difficulty: 'easy'
                }
            ];
                    this.setData(
                        {
                            tasks: this.normalizeTasks(demoTasks),
                            summary: { ...this.summary, overdue: 0 }
                        },
                        () => {
                            this.updateSummary();
                            this.updateVisibleTasks();
                        }
                    );
                });
        },

        updateSummary() {
            const total = this.tasks.length;
            const done = this.tasks.filter((t) => t.status === 'done').length;
            const overdue = this.tasks.filter((t) => t.status !== 'done' && t.overdue).length;
            const nextDue = this.tasks
                .filter((t) => t.status !== 'done' && t.dueTime)
                .map((t) => t.dueTime)
                .sort()[0] || '';
            const summary = {
                ...this.summary,
                total,
                done,
                overdue,
                next: nextDue
            };
            this.setData({ summary });
            setScopedStorageSync('rehab_summary', summary);
            setScopedStorageSync('rehab_tasks_today', {
                date: new Date().toISOString().slice(0, 10),
                tasks: this.tasks,
                summary
            });
        },

        updateVisibleTasks() {
            const { tasks, activeFilter } = this;
            let list = tasks;
            if (activeFilter === 'todo') {
                list = tasks.filter((t) => t.status !== 'done');
            } else if (activeFilter === 'done') {
                list = tasks.filter((t) => t.status === 'done');
            }
            this.setData({
                visibleTasks: list
            });
        },

        normalizeTasks(list) {
            const now = new Date();
            const currentMinutes = now.getHours() * 60 + now.getMinutes();
            return (Array.isArray(list) ? list : []).map((item, index) => {
                const dueText = item.dueTime || item.ext4 || '';
                let dueMinutes = null;
                if (dueText && /^\d{1,2}:\d{2}$/.test(dueText)) {
                    const parts = dueText.split(':');
                    dueMinutes = Number(parts[0]) * 60 + Number(parts[1]);
                }
                const statusRaw = String(item.status || '').toLowerCase();
                const status = statusRaw === 'done' || statusRaw === 'completed' ? 'done' : 'todo';
                const overdue = status !== 'done' && dueMinutes != null && currentMinutes > dueMinutes;
                return {
                    id: item.id != null ? item.id : 'task_' + index,
                    icon: item.icon || item.ext2 || (String(item.title || '').includes('饮食') ? '🥗' : '🚶'),
                    title: item.title || '今日康养任务',
                    category: item.category || item.ext1 || '康养',
                    duration: Number(item.duration || item.ext3 || 15),
                    desc: item.desc || item.description || '请按建议完成今日康养安排。',
                    status,
                    difficulty: item.difficulty || 'just',
                    dueTime: dueText || '20:00',
                    deadlineText: item.deadlineText || (overdue ? '已超过建议完成时间' : `建议在 ${dueText || '20:00'} 前完成`),
                    overdue
                };
            });
        },

        // 保存任务状态
        saveTaskStatus(id, status) {
            if (!id) return;
            if (!status) return;
            updateRehabTaskStatus(id, status).catch(() => {
                // 不打断体验：本地已经变更
                if (!USE_MOCK_FALLBACK) {
                    uni.showToast({ title: '同步任务状态失败', icon: 'none' });
                }
            });
        },

        // 保存难度反馈
        saveTaskDifficulty(id, level) {
            if (!id) return;
            updateRehabTaskDifficulty(id, level).catch(() => {
                if (!USE_MOCK_FALLBACK) {
                    uni.showToast({ title: '同步难度反馈失败', icon: 'none' });
                }
            });
        }
    }
};
</script>
<style>
@import './rehab.css';
</style>
