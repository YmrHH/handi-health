<template>
    <!-- pages/patient/home/home.wxml -->
    <view class="page home-page">
        <!-- 顶部渐变背景区域 + 头部卡片 + 今日任务 -->
        <view class="home-top-wrapper">
            <view class="home-top-bg"></view>

            <view class="home-top-content">
                <!-- 顶部问候 + 风险 -->
                <view class="card home-header">
                    <view class="home-header-left">
                        <view class="hello-text">
                            你好，
                            <text v-if="patientName">{{ patientName }}</text>
                            <text v-else>患者朋友</text>
                        </view>
                        <view class="hello-sub">今天是 {{ todayText }}，请按提示完成今日监测和康养指标。</view>
                    </view>
                    <view class="home-header-right">
                        <view :class="'risk-pill ' + riskLevel">
                            {{ riskText }}
                        </view>
                    </view>
                </view>

                <!-- 今日任务 -->
                <view class="card task-card">
                    <view class="card-title-row">
                        <view class="card-title">今日任务</view>
                        <view class="card-sub">已完成 {{ todoDoneCount }} / 2 项</view>
                    </view>

                    <!-- 今日自测状态 -->
                    <view class="task-row">
                        <view class="task-left">
                            <view class="task-title">今日自测</view>
                            <view class="task-desc">血压、体温、症状等基础监测，建议每天完成 1 次。</view>
                        </view>
                        <view :class="'pill-status ' + (dailyDone ? 'done' : 'todo')">
                            {{ dailyDone ? '已完成' : '未完成' }}
                        </view>
                    </view>

                    <!-- 今日康养指标状态 -->
                    <view class="task-row">
                        <view class="task-left">
                            <view class="task-title">今日康养指标</view>
                            <view class="task-desc">4 步康养指标填报，记录症状、睡眠、用药、运动与饮食等情况。</view>
                        </view>
                        <view :class="'pill-status ' + (surveyDone ? 'done' : 'todo')">
                            {{ surveyDone ? '已完成' : '未完成' }}
                        </view>
                    </view>

                    <!-- 底部两个主按钮 -->
                    <view class="task-actions">
                        <button class="btn-outline" @tap="onGoMonitor" hover-class="btn-hover">开始今日自测</button>
                        <!-- 与“开始今日自测”保持一致的样式（同款描边胶囊按钮） -->
                        <button class="btn-outline" @tap="onGoSurvey" hover-class="btn-hover">填写康养指标</button>
                    </view>
                </view>
            </view>
        </view>

        <!-- 底部内容区域：下次化验 + 监测入口 -->
        <view class="home-bottom">
            <!-- 医生最新建议 / 下次医院化验 -->
            <view class="card lab-tip-card" :class="latestAdvice && latestAdvice.content ? 'lab-tip-card--advice' : ''" @tap="onOpenAdviceCard">
                <view class="card-title-row">
                    <view class="card-title">{{ adviceCard.title }}</view>
                    <view :class="'lab-badge ' + adviceCard.badgeClass">
                        {{ adviceCard.badgeText }}
                    </view>
                </view>
                <view class="lab-tip-text">
                    {{ adviceCard.text }}
                </view>
                <view v-if="adviceCard.meta" class="lab-tip-meta">
                    {{ adviceCard.meta }}
                </view>
            </view>

            <!-- 监测与数据 - 只保留舌象采集 -->
            <view class="card quick-card">
                <view class="card-title-row">
                    <view class="card-title">监测与数据</view>
                    <view class="card-link" @tap="onGoMonitor">全部</view>
                </view>
                <view class="quick-row center">
                    <!-- 只保留舌象采集，居中放置 -->
                    <view class="quick-item tongue-only" @tap="onGoTongue" hover-class="quick-item-hover">
                        <view class="quick-icon">👅</view>
                        <view class="quick-text">舌象采集</view>
                    </view>
                </view>
            </view>
        </view>
    </view>
</template>

<style>
/* pages/patient/home/home.vue
   统一患者端首页视觉：与“我的”页同风格（紫蓝渐变 + 玻璃卡片）
*/

.page {
  background: transparent;
  min-height: 100vh;
}

/* 顶部渐变区 */
.home-top-wrapper { position: relative; }

.home-top-bg {
  position: absolute;
  top: 0; left: 0;
  width: 100%;
  height: 190rpx;
  background: linear-gradient(180deg, var(--c-primary) 0%, var(--c-primary-2) 100%);
  opacity: 0.95;
}

.home-top-content {
  position: relative;
  z-index: 1;
  padding: 26rpx 0 0;
}

/* 卡片（与全局 app-card 一致的玻璃质感） */
.card {
  background: rgba(255, 255, 255, 0.92);
  border: 1rpx solid rgba(255, 255, 255, 0.95);
  border-radius: var(--r-card);
  box-shadow: var(--shadow-card);
  padding: 26rpx;
  margin-bottom: 18rpx;
}

/* 头部卡片 */
.home-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.hello-text {
  font-size: 36rpx;
  font-weight: 900;
  color: var(--c-text);
  margin-bottom: 10rpx;
}

.hello-sub {
  font-size: 26rpx;
  line-height: 38rpx;
  color: rgba(31,42,55,0.58);
  font-weight: 650;
}

.risk-pill {
  font-size: 22rpx;
  padding: 8rpx 14rpx;
  border-radius: 999rpx;
  text-align: center;
  border: 1rpx solid rgba(91,108,255,0.14);
  background: rgba(91,108,255,0.08);
  color: rgba(91,108,255,0.92);
  font-weight: 800;
}

.risk-pill.low  { background: rgba(16,185,129,0.14); color: #065F46; border-color: rgba(16,185,129,0.18); }
.risk-pill.mid  { background: rgba(245,158,11,0.14); color: #92400E; border-color: rgba(245,158,11,0.18); }
.risk-pill.high { background: rgba(255,99,132,0.14); color: #B42318; border-color: rgba(255,99,132,0.18); }

/* 卡片标题行 */
.card-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14rpx;
}

.card-title {
  font-size: 32rpx;
  font-weight: 900;
  color: var(--c-text);
}

.card-sub {
  font-size: 24rpx;
  color: rgba(31,42,55,0.55);
  font-weight: 700;
}

.card-link {
  font-size: 26rpx;
  color: rgba(91,108,255,0.92);
  font-weight: 750;
}

/* 任务列表 */
.task-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16rpx 0;
  border-bottom: 1rpx solid rgba(31,42,55,0.06);
}

.task-row:last-child { border-bottom: none; }

.task-left { flex: 1; }

.task-title {
  font-size: 30rpx;
  color: var(--c-text);
  font-weight: 850;
  margin-bottom: 8rpx;
}

.task-desc {
  font-size: 24rpx;
  color: rgba(31,42,55,0.55);
  font-weight: 650;
  line-height: 32rpx;
}

.pill-status {
  font-size: 24rpx;
  padding: 6rpx 12rpx;
  border-radius: 999rpx;
  text-align: center;
  min-width: 90rpx;
  font-weight: 800;
  border: 1rpx solid rgba(91,108,255,0.14);
  background: rgba(255,255,255,0.72);
  color: rgba(31,42,55,0.66);
}

.pill-status.done {
  background: rgba(16,185,129,0.14);
  color: #065F46;
  border-color: rgba(16,185,129,0.18);
}

.pill-status.todo {
  background: rgba(245,158,11,0.14);
  color: #92400E;
  border-color: rgba(245,158,11,0.18);
}

/* 任务按钮 */
.task-actions {
  display: flex;
  justify-content: space-between;
  margin-top: 20rpx;
}

button.btn-outline, button.btn-fill {
  width: 48%;
  height: 84rpx;
  border-radius: 999rpx;
  font-size: 32rpx;
  line-height: 84rpx;
  text-align: center;
  font-weight: 800;
}

button.btn-outline {
  background: rgba(255,255,255,0.95);
  color: rgba(91,108,255,0.95);
  border: 1rpx solid rgba(91,108,255,0.18);
}

button.btn-fill {
  background: linear-gradient(135deg, var(--c-primary) 0%, var(--c-primary-2) 100%);
  color: #ffffff;
  border: none;
  box-shadow: var(--shadow-soft);
}

.btn-hover { opacity: 0.88; }

/* 底部区域 */
.home-bottom { padding: 0 0 20rpx; }

/* 实验室检查提示 */
.lab-badge {
  font-size: 22rpx;
  padding: 6rpx 12rpx;
  border-radius: 999rpx;
  text-align: center;
  font-weight: 800;
  border: 1rpx solid rgba(91,108,255,0.14);
  background: rgba(91,108,255,0.08);
  color: rgba(91,108,255,0.92);
}

.lab-badge.normal { background: rgba(91,108,255,0.12); }
.lab-badge.overdue { background: rgba(255,99,132,0.14); color: #B42318; border-color: rgba(255,99,132,0.18); }

.lab-tip-text {
  font-size: 28rpx;
  color: rgba(31,42,55,0.76);
  line-height: 42rpx;
  font-weight: 700;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  text-overflow: ellipsis;
  word-break: break-all;
}

.lab-tip-card--advice .lab-tip-text {
  min-height: 84rpx;
}


.lab-tip-meta {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: rgba(31,42,55,0.50);
  line-height: 34rpx;
  font-weight: 650;
}

.lab-tip-card--advice {
  cursor: pointer;
}


/* 快捷入口 */
.quick-row { display: flex; justify-content: space-between; }
.quick-row.center { justify-content: center; }

.quick-item {
  width: 33.33%;
  height: 160rpx;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  border-radius: 22rpx;
  background: rgba(255,255,255,0.65);
  border: 1rpx solid rgba(255,255,255,0.90);
}

.tongue-only { width: 240rpx; }

.quick-item-hover { background-color: rgba(91,108,255,0.06); }

.quick-icon { font-size: 52rpx; margin-bottom: 10rpx; }

.quick-text { font-size: 28rpx; color: rgba(31,42,55,0.78); font-weight: 800; }
</style>


<script>
// pages/patient/home/home.js
import { USE_MOCK_FALLBACK } from '@/config/api.js';
import { getHomeSummary } from '@/api/patient.js';
import { getUser, getScopedStorageSync, setScopedStorageSync } from '@/utils/session.js';

export default {
    data() {
        return {
            patientName: '张阿姨',
            todayText: '',
            todayDateStr: '',
            riskLevel: 'mid',
            riskText: '中风险',
            // 今日任务：现在只统计两项：自测 + 康养指标
            dailyDone: false,
            surveyDone: false,
            todoDoneCount: 0,
            // 下次化验信息
            nextLab: {
                date: '2025-12-20',
                daysLeft: 0,
                overdue: false
            },
            nextLabText: '',
            latestAdvice: null,
            // 是否安排今天康养指标填报（后端未来可控制）
            hasTodaySurvey: true
        };
    },
    computed: {
        adviceCard() {
            const advice = this.latestAdvice;
            if (advice && advice.content) {
                const metaParts = [];
                if (advice.doctor) metaParts.push(advice.doctor);
                if (advice.time) metaParts.push(String(advice.time).replace('T', ' ').slice(0, 16));
                return {
                    title: advice.title || '医生最新建议',
                    badgeText: '最新消息',
                    badgeClass: 'normal',
                    text: this.buildAdvicePreview(advice.content),
                    meta: metaParts.join(' · ')
                };
            }
            return {
                title: '下次医院化验',
                badgeText: this.nextLab.overdue ? '已超期' : ('剩余 ' + this.nextLab.daysLeft + ' 天'),
                badgeClass: this.nextLab.overdue ? 'overdue' : 'normal',
                text: this.nextLabText,
                meta: ''
            };
        }
    },
    onLoad() {
        this.loadLocalUserProfile();
        this.initToday();
        this.updateNextLab();
        this.checkDailyStatus();
        this.checkTodaySurvey();
        this.fetchHomeSummary();
    },
    onShow() {
        // 从其他页面返回时刷新状态
        this.loadLocalUserProfile();
        this.initToday();
        this.checkDailyStatus();
        this.checkTodaySurvey();
        this.fetchHomeSummary();
    },
    methods: {
        // 从本地缓存/登录态读取用户信息，让首页顶部随个人资料编辑实时刷新
        loadLocalUserProfile() {
            try {
                const userInfo = getScopedStorageSync('userInfo', null);
                if (userInfo && typeof userInfo === 'object') {
                    if (userInfo.name) this.patientName = userInfo.name;

                    const r0 = userInfo.riskLevel || userInfo.risk_level || userInfo.risk || '';
                    if (r0) {
                        const r = String(r0).toLowerCase();
                        const map = {
                            '低': { level: 'low', text: '低风险' },
                            '中': { level: 'mid', text: '中风险' },
                            '高': { level: 'high', text: '高风险' },
                            'low': { level: 'low', text: '低风险' },
                            'mid': { level: 'mid', text: '中风险' },
                            'high': { level: 'high', text: '高风险' }
                        };
                        const m = map[r0] || map[r];
                        if (m) {
                            this.riskLevel = m.level;
                            this.riskText = m.text;
                        }
                    }
                }

                const latestAdvice = getScopedStorageSync('doctor_advice_latest', null);
                if (latestAdvice && typeof latestAdvice === 'object' && latestAdvice.content) {
                    this.latestAdvice = { ...latestAdvice };
                }

                const u = getUser();
                if (u && u.name) {
                    this.patientName = u.name;
                }
            } catch (e) {}
        },

        fetchHomeSummary() {
            getHomeSummary()
                .then((res) => {
                    const data = res || {};

                    if (data.patientName) this.patientName = data.patientName;
                    if (data.risk) {
                        this.riskLevel = data.risk.level || this.riskLevel;
                        this.riskText = data.risk.text || this.riskText;
                    } else {
                        // 兼容直接字段
                        if (data.riskLevel) this.riskLevel = data.riskLevel;
                        if (data.riskText) this.riskText = data.riskText;
                    }

                    if (typeof data.dailyDone === 'boolean') this.dailyDone = this.dailyDone || data.dailyDone;
                    if (typeof data.surveyDone === 'boolean') this.surveyDone = this.surveyDone || data.surveyDone;
                    if (data.dailyDoneDate) {
                        setScopedStorageSync('daily_measure_date', String(data.dailyDoneDate));
                    }
                    if (data.surveyDoneDate) {
                        setScopedStorageSync('tcm_survey_date', String(data.surveyDoneDate));
                    }
                    if (typeof data.hasTodaySurvey === 'boolean') this.hasTodaySurvey = data.hasTodaySurvey;

                    if (data.nextLab && data.nextLab.date) {
                        this.nextLab = { ...this.nextLab, ...data.nextLab };
                        this.updateNextLab();
                    }
                    if (data.nextFollowupTime && !data.nextLab) {
                        const nextDate = String(data.nextFollowupTime).replace('T', ' ').slice(0, 10);
                        if (nextDate) {
                            this.nextLab = { ...this.nextLab, date: nextDate };
                            this.updateNextLab();
                        }
                    }
                    if (data.advice && typeof data.advice === 'object' && data.advice.content) {
                        this.latestAdvice = { ...data.advice };
                        setScopedStorageSync('doctor_advice_latest', this.latestAdvice);
                    }

                    this.updateTodoCount();
                })
                .catch(() => {
                    // ignore
                    if (!USE_MOCK_FALLBACK) {
                        // 不强提示，避免影响首页体验
                    }
                });
        },
        initToday() {
            const date = new Date();
            const y = date.getFullYear();
            const m = date.getMonth() + 1;
            const d = date.getDate();
            const weekdayList = ['日', '一', '二', '三', '四', '五', '六'];
            const w = weekdayList[date.getDay()];
            const dateStr = y + '-' + (m < 10 ? '0' + m : m) + '-' + (d < 10 ? '0' + d : d);
            this.todayText = `${y}年${m}月${d}日 周${w}`;
            this.todayDateStr = dateStr;
        },

        // 下次化验
        updateNextLab() {
            const labDateStr = this.nextLab.date;
            if (!labDateStr) {
                this.nextLabText = '暂无计划，可在门诊时由医生为您安排化验时间。';
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
            
            this.nextLab.daysLeft = diffDays < 0 ? 0 : diffDays;
            this.nextLab.overdue = overdue;
            this.nextLabText = text;
        },

        // 今日自测是否完成：可以先用本地存储，后面改成后端返回
        checkDailyStatus() {
            const today = this.todayDateStr;
            const d = getScopedStorageSync('daily_measure_date', '');
            this.dailyDone = d === today;
            this.updateTodoCount();
        },

        // 今日康养指标是否完成
        checkTodaySurvey() {
            const today = this.todayDateStr;
            const d = getScopedStorageSync('tcm_survey_date', '');
            this.surveyDone = d === today;
            this.updateTodoCount();
        
            this.hasTodaySurvey = true;
        },

        updateTodoCount() {
            let count = 0;
            if (this.dailyDone) {
                count++;
            }
            if (this.surveyDone) {
                count++;
            }
            this.todoDoneCount = count;
        },

        buildAdvicePreview(content) {
            const raw = String(content || '').replace(/\r/g, '\n');
            const lines = raw
                .split(/\n+/)
                .map((line) => String(line || '').replace(/^[\s\-•·*]+/, '').trim())
                .filter(Boolean);
            const summary = (lines.length ? lines.join(' ') : raw.replace(/\s+/g, ' ').trim())
                .replace(/\s+/g, ' ')
                .trim();
            if (!summary) return '暂无最新建议，请继续保持规律监测与康养指标填报。';
            if (summary.length <= 48) return summary;
            return summary.slice(0, 48).replace(/[，、；：,.。\s]+$/g, '') + '…';
        },

        onOpenAdviceCard() {
            if (this.latestAdvice && this.latestAdvice.content) {
                uni.navigateTo({ url: '/pages/patient/message/message' });
            }
        },

        // ===== 导航 ===== //
        onGoMonitor() {
            uni.navigateTo({
                url: '/pages/patient/monitor/monitor?from=home'
            });
        },

        onGoSurvey() {
            uni.navigateTo({
                url: '/pages/patient/survey/survey',
                success() {
                    console.log('navigateTo survey success');
                },
                fail(err) {
                    console.error('navigateTo survey fail:', err);
                }
            });
        },

        onGoTongue() {
            uni.navigateTo({
                url: '/pages/patient/tongue/tongue'
            });
        }
    }
};
</script>
