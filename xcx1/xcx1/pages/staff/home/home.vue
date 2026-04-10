<template>
    <!-- pages/patient/home/home.wxml -->
    <view class="app-page staff-page home-page">
        <!-- 顶部问候 + 风险 -->
        <view class="card home-header">
            <view>
                <view class="hello-text">你好，{{ patientName || '患者朋友' }}</view>
                <view class="hello-sub">今天是 {{ todayText }}，请按提示完成监测和康养任务。</view>
            </view>
            <view :class="'risk-pill ' + riskLevel">
                {{ riskText }}
            </view>
        </view>

        <!-- 今日待办：血压 / 体温 / 症状打卡 / 康养任务 -->
        <view class="card todo-card">
            <view class="card-title-row">
                <view class="card-title">今日待办</view>
                <view class="card-sub">已完成 {{ todoDoneCount }} / 4 项</view>
            </view>

            <view class="todo-item" @tap="onGoMonitor">
                <view class="todo-left">
                    <view class="todo-icon">🩺</view>
                    <view>
                        <view class="todo-title">血压测量</view>
                        <view class="todo-desc">请按医嘱在早晨/晚上各测一次</view>
                    </view>
                </view>
                <view :class="'todo-status ' + (todo.bp.done ? 'done' : 'todo')">
                    {{ todo.bp.done ? '已完成' : '去测量' }}
                </view>
            </view>

            <view class="todo-item" @tap="onGoMonitor">
                <view class="todo-left">
                    <view class="todo-icon">⚖️</view>
                    <view>
                        <view class="todo-title">体温登记</view>
                        <view class="todo-desc">晨起空腹称重，自动计算 BMI</view>
                    </view>
                </view>
                <view :class="'todo-status ' + (todo.weight.done ? 'done' : 'todo')">
                    {{ todo.weight.done ? '已完成' : '去记录' }}
                </view>
            </view>

            <view class="todo-item" @tap="onGoSymptom">
                <view class="todo-left">
                    <view class="todo-icon">📝</view>
                    <view>
                        <view class="todo-title">症状打卡</view>
                        <view class="todo-desc">胸闷气短、睡眠、乏力等简单打分</view>
                    </view>
                </view>
                <view :class="'todo-status ' + (todo.symptom.done ? 'done' : 'todo')">
                    {{ todo.symptom.done ? '已完成' : '去打卡' }}
                </view>
            </view>

            <view class="todo-item" @tap="onGoRehab">
                <view class="todo-left">
                    <view class="todo-icon">🏃</view>
                    <view>
                        <view class="todo-title">康养任务</view>
                        <view class="todo-desc">散步、呼吸操、泡脚等日常康养</view>
                    </view>
                </view>
                <view :class="'todo-status ' + (todo.rehab.done ? 'done' : 'todo')">
                    {{ todo.rehab.done ? '已完成' : '去完成' }}
                </view>
            </view>
        </view>

        <!-- 下次医院化验日期提示 -->
        <view class="card lab-tip-card">
            <view class="lab-tip-row">
                <view class="lab-tip-main">
                    <view class="lab-tip-title">下次医院化验</view>
                    <view class="lab-tip-text">
                        {{ nextLabText }}
                    </view>
                </view>
                <view :class="'lab-badge ' + (nextLab.overdue ? 'overdue' : 'normal')">
                    {{ nextLab.overdue ? '已超期' : '剩余 ' + nextLab.daysLeft + ' 天' }}
                </view>
            </view>
        </view>

        <!-- 监测模块入口 -->
        <view class="card quick-card">
            <view class="card-title-row">
                <view class="card-title">监测与数据</view>
                <view class="card-link" @tap="onGoMonitor">全部</view>
            </view>
            <view class="quick-row">
                <view class="quick-item" @tap="onGoMonitor">
                    <view class="quick-icon">📊</view>
                    <view class="quick-text">日常自测</view>
                </view>
                <view class="quick-item" @tap="onGoMonitorLab">
                    <view class="quick-icon">🧪</view>
                    <view class="quick-text">医院检查结果</view>
                </view>
            </view>
        </view>

        <!-- 消息/随访提醒可以保持你原来的实现，这里略过 -->
      <staff-tabbar active="tasks"></staff-tabbar>
    </view>
</template>

<script>
// pages/patient/home/home.js
import StaffTabbar from '@/components/staff-tabbar/staff-tabbar.vue';

export default {
    components: { StaffTabbar },
    data() {
        return {
            patientName: '张阿姨',
            todayText: '',
            riskLevel: 'mid',
            // high | mid | low
            riskText: '中风险',
            // 今日待办状态（实际应由后端返回）
            todo: {
                bp: {
                    done: false
                },
                weight: {
                    done: false
                },
                symptom: {
                    done: false
                },
                rehab: {
                    done: false
                }
            },
            todoDoneCount: 0,
            // 下次化验信息
            nextLab: {
                date: '2025-12-20',
                daysLeft: 0,
                overdue: false
            },
            nextLabText: ''
        };
    },
    onLoad() {
        this.initToday();
        this.updateTodoCount();
        this.updateNextLab();
    },
    methods: {
        initToday() {
            const date = new Date();
            const y = date.getFullYear();
            const m = date.getMonth() + 1;
            const d = date.getDate();
            const weekdayList = ['日', '一', '二', '三', '四', '五', '六'];
            const w = weekdayList[date.getDay()];
            this.setData({
                todayText: `${y}年${m}月${d}日 周${w}`
            });
        },

        updateTodoCount() {
            const { todo } = this;
            let count = 0;
            Object.keys(todo).forEach((key) => {
                if (todo[key].done) {
                    count++;
                }
            });
            this.setData({
                todoDoneCount: count
            });
        },

        // 计算距离下次化验还有几天 / 是否超期
        updateNextLab() {
            const labDateStr = this.nextLab.date; // 格式：YYYY-MM-DD
            if (!labDateStr) {
                this.setData({
                    nextLabText: '暂无计划，可在门诊时由医生为您安排化验时间。'
                });
                return;
            }
            const today = new Date();
            const target = new Date(labDateStr.replace(/-/g, '/'));
            const diffMs = target.getTime() - today.setHours(0, 0, 0, 0);
            const diffDays = Math.floor(diffMs / (86400 * 1000));
            let overdue = false;
            let text = '';
            if (diffDays < 0) {
                overdue = true;
                text = `原定 ${labDateStr} 需要在医院化验血糖/血脂等项目，请尽快安排就诊或体检。`;
            } else if (diffDays === 0) {
                text = `今天（${labDateStr}）是您约定的化验日，请记得前往医院完成检查。`;
            } else {
                text = `预计在 ${labDateStr} 到医院化验血糖、血脂、尿酸等项目。`;
            }
            this.setData({
                nextLab: {
                    ...this.nextLab,
                    daysLeft: diffDays < 0 ? 0 : diffDays,
                    overdue
                },
                nextLabText: text
            });
        },

        // ===== 导航 ===== //
        onGoMonitor() {
            uni.navigateTo({
                url: '/pages/patient/monitor/monitor'
            });
        },

        onGoMonitorLab() {
            uni.navigateTo({
                url: '/pages/patient/monitor/monitor?tab=lab'
            });
        },

        onGoSymptom() {
            // 你可以有单独的症状打卡页，也可以直接用 monitor 页的日常自测
            uni.navigateTo({
                url: '/pages/patient/monitor/monitor?tab=daily&focus=symptom'
            });
        },

        onGoRehab() {
            uni.navigateTo({
                url: '/pages/patient/rehab/rehab'
            });
        }
    }
};
</script>
<style>
@import './home.css';
</style>
