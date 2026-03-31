
<template>
    <view class="page survey-page">
        <!-- 顶部提示 -->
        <view class="card survey-header">
            <view class="survey-title">今日康养指标</view>
            <view class="survey-sub">
                通过几个简单问题，记录您今天的症状、睡眠、用药、运动与饮食等康养指标情况。
                填写约 2–3 分钟，由您或家属代填即可。
            </view>

            <view class="survey-progress">
                <view class="survey-progress-bar">
                    <view class="survey-progress-inner" :style="{ width: progressPct + '%' }"></view>
                </view>
                <view class="survey-step">第 {{ currentStep + 1 }} / {{ steps.length }} 步</view>
            </view>
        </view>

        <!-- 当前步的问题列表 -->
        <view class="card" v-if="steps && steps.length">
            <view class="q-card" v-for="(item, index) in steps[currentStep].questions" :key="item.field">
                <view class="q-title">
                    {{ index + 1 }}. {{ item.title }}
                    <text v-if="item.required !== false" class="q-required">*</text>
                </view>
                <view v-if="item.desc" class="q-desc">{{ item.desc }}</view>

                <!-- 单选选项 -->
                <view class="q-options" v-if="isSingle(item)">
                    <view
                        :class="'q-option ' + (answers[item.field] === opt.value ? 'active' : '')"
                        :data-field="item.field"
                        :data-value="opt.value"
                        @tap="onSelectOption"
                        v-for="(opt, optIndex) in item.options"
                        :key="optIndex"
                    >
                        <view class="q-option-text">{{ opt.label }}</view>
                    </view>
                </view>

                <!-- 星级评分（1-5） -->
                <view v-else-if="item.type === 'star'" class="star-wrap">
                    <view class="star-row">
                        <text
                            v-for="n in (item.max || 5)"
                            :key="n"
                            :class="'star ' + (getNumAnswer(item.field) >= n ? 'on' : '')"
                            :data-field="item.field"
                            :data-value="n"
                            @tap="onSetStar"
                        >★</text>
                        <text class="star-score">{{ (getNumAnswer(item.field) || 0) }}/{{ item.max || 5 }}</text>
                    </view>
                    <view v-if="item.labels && item.labels.length" class="star-labels">
                        <text class="star-label-left">{{ item.labels[0] }}</text>
                        <text class="star-label-right">{{ item.labels[item.labels.length - 1] }}</text>
                    </view>
                </view>

                <!-- 分值评分（滑块） -->
                <view v-else-if="item.type === 'score'" class="score-wrap">
                    <view class="score-head">
                        <text class="score-val">{{ displayScore(item) }}</text>
                        <text v-if="item.unit" class="score-unit">{{ item.unit }}</text>
                    </view>
                    <slider
                        class="score-slider"
                        :min="item.min !== undefined ? item.min : 0"
                        :max="item.max !== undefined ? item.max : 10"
                        :step="item.step !== undefined ? item.step : 1"
                        :value="displayScore(item)"
                        :data-field="item.field"
                        @change="onSliderChange"
                    />
                    <view class="score-range">
                        <text class="score-left">{{ minLabel(item) }}</text>
                        <text class="score-right">{{ maxLabel(item) }}</text>
                    </view>
                </view>

                <!-- 文本补充（选填/必填） -->
                <view v-else-if="item.type === 'text'" class="text-wrap">
                    <textarea
                        class="q-textarea"
                        :placeholder="item.placeholder || '请输入…'"
                        :value="(answers[item.field] || '')"
                        :maxlength="item.maxlength || 200"
                        auto-height
                        :data-field="item.field"
                        @input="onTextInput"></textarea>
                    <view class="text-count">{{ ((answers[item.field] || '').length) }}/{{ item.maxlength || 200 }}</view>
                </view>
            </view>
        </view>

        <view class="card" v-else>
            <view class="empty">加载中…</view>
        </view>

        <!-- 底部操作区 -->
        <view class="card bottom-card">
            <view class="bottom-row">
                <button class="btn-secondary" @tap="onPrev" :disabled="currentStep === 0">上一步</button>

                <button class="btn-primary" v-if="currentStep < steps.length - 1" @tap="onNext">下一步</button>

                <button class="btn-primary" v-else-if="currentStep === steps.length - 1" @tap="onSubmit">提交指标</button>
            </view>

            <view class="bottom-tip">
                本问卷仅用于慢病随访与中医健康管理，不作为诊断依据。
                若出现胸痛、明显呼吸困难、意识不清等严重不适，请立即就医或拨打急救电话。
            </view>
        </view>
    </view>
</template>

<script>
import { USE_MOCK_FALLBACK } from '@/config/api.js';
import { submitTcmSurvey } from '@/api/patient.js';
import { getScopedStorageSync, setScopedStorageSync } from '@/utils/session.js';
export default {
    data() {
        return {
            currentStep: 0,
            steps: [],
            answers: {},
            todayDateStr: ''
        };
    },
    computed: {
        progressPct() {
            if (!this.steps || !this.steps.length) return 0;
            return Math.round(((this.currentStep + 1) / this.steps.length) * 100);
        }
    },
    onLoad() {
        this.initToday();
        this.initSteps();
    },
    methods: {
        initToday() {
            const d = new Date();
            const y = d.getFullYear();
            const m = ('0' + (d.getMonth() + 1)).slice(-2);
            const day = ('0' + d.getDate()).slice(-2);
            const dateStr = `${y}-${m}-${day}`;
            this.setData({
                todayDateStr: dateStr
            });
        },

        // 判定是否为单选题
        isSingle(item) {
            return item && Array.isArray(item.options);
        },

        // 取数值型答案
        getNumAnswer(field) {
            const v = this.answers[field];
            return typeof v === 'number' && !isNaN(v) ? v : undefined;
        },

        displayScore(item) {
            const v = this.getNumAnswer(item.field);
            if (v !== undefined) return v;
            if (typeof item.default === 'number') return item.default;
            return item.min !== undefined ? item.min : 0;
        },

        minLabel(item) {
            if (item.minLabel !== undefined && item.minLabel !== null) return item.minLabel;
            return item.min !== undefined ? item.min : 0;
        },

        maxLabel(item) {
            if (item.maxLabel !== undefined && item.maxLabel !== null) return item.maxLabel;
            return item.max !== undefined ? item.max : 10;
        },

        // 生成问卷步骤（根据慢性病类型拼装更贴合的问题）
        buildSteps(disease) {
            const diseaseLabelMap = {
                HTN: '高血压',
                DM: '糖尿病',
                HF: '冠心病/心衰',
                COPD: '慢阻肺/哮喘',
                OTHER: '其他/不确定'
            };

            const base = [
                // 第 1 步：慢性病类型 + 今日总体状态
                {
                    step: 1,
                    questions: [
                        {
                            field: 'chronic_disease',
                            title: '您目前主要慢性病类型是？',
                            desc: '用于生成更贴合的每日问卷（可随时修改）。',
                            options: [
                                { value: 'HTN', label: '高血压' },
                                { value: 'DM', label: '糖尿病' },
                                { value: 'HF', label: '冠心病/心衰' },
                                { value: 'COPD', label: '慢阻肺/哮喘' },
                                { value: 'OTHER', label: '其他/不确定' }
                            ]
                        },
                        {
                            field: 'overall_star',
                            type: 'star',
                            max: 5,
                            title: '今天整体感觉如何？',
                            desc: '1 星最差，5 星最好。',
                            labels: ['很差', '很好']
                        },
                        {
                            field: 'fatigue_score',
                            type: 'score',
                            min: 0,
                            max: 10,
                            step: 1,
                            title: '今天乏力程度（0–10 分）',
                            desc: '0 分无乏力，10 分非常疲惫。',
                            minLabel: '无',
                            maxLabel: '很重'
                        }
                    ]
                },

                // 第 2 步：核心症状（适用于多数慢性病）
                {
                    step: 2,
                    questions: [
                        {
                            field: 'dyspnea_score',
                            type: 'score',
                            min: 0,
                            max: 10,
                            step: 1,
                            title: '气短/呼吸困难程度（0–10 分）',
                            desc: '0 分无气短，10 分休息也很难呼吸。',
                            minLabel: '无',
                            maxLabel: '很重'
                        },
                        {
                            field: 'chest_tightness',
                            title: '是否有胸闷或胸部不适？',
                            options: [
                                { value: 0, label: '没有' },
                                { value: 1, label: '偶尔有' },
                                { value: 2, label: '经常有' },
                                { value: 3, label: '明显加重/影响活动' }
                            ]
                        },
                        {
                            field: 'palpitation_score',
                            type: 'score',
                            min: 0,
                            max: 10,
                            step: 1,
                            title: '心慌/心悸程度（0–10 分）',
                            desc: '如无此症状可保持 0 分。',
                            minLabel: '无',
                            maxLabel: '很重',
                            required: false,
                            default: 0
                        },
                        {
                            field: 'edema',
                            title: '下肢或脚踝是否有浮肿？',
                            options: [
                                { value: 0, label: '没有' },
                                { value: 1, label: '下午有轻微' },
                                { value: 2, label: '明显压痕' },
                                { value: 3, label: '持续较重浮肿' }
                            ]
                        }
                    ]
                },

                // 第 3 步：睡眠 / 情绪 / 饮食
                {
                    step: 3,
                    questions: [
                        {
                            field: 'sleep_star',
                            type: 'star',
                            max: 5,
                            title: '昨晚睡眠质量（1–5 星）',
                            labels: ['很差', '很好']
                        },
                        {
                            field: 'night_wake',
                            title: '昨晚夜间醒来次数？',
                            options: [
                                { value: 0, label: '0–1 次' },
                                { value: 1, label: '2–3 次' },
                                { value: 2, label: '4 次以上' },
                                { value: 3, label: '几乎整夜睡不安稳' }
                            ]
                        },
                        {
                            field: 'mood_star',
                            type: 'star',
                            max: 5,
                            title: '今天情绪状态（1–5 星）',
                            desc: '1 星很差，5 星很好。',
                            labels: ['很差', '很好']
                        },
                        {
                            field: 'appetite_star',
                            type: 'star',
                            max: 5,
                            title: '今天食欲情况（1–5 星）',
                            labels: ['很差', '很好']
                        },
                        {
                            field: 'stool',
                            title: '最近一周大便情况？',
                            options: [
                                { value: 0, label: '每天一次、成形' },
                                { value: 1, label: '偏稀或次数偏多' },
                                { value: 2, label: '偏干或两天一次以上' },
                                { value: 3, label: '需用药或非常不规律' }
                            ]
                        }
                    ]
                }
            ];

            // 第 4 步：慢病管理（通用 + 分病种加问）
            const commonMgmt = [
                {
                    field: 'med_adherence_star',
                    type: 'star',
                    max: 5,
                    title: '今天用药是否按时（1–5 星）',
                    desc: '1 星经常漏服，5 星完全按时。',
                    labels: ['常漏服', '很按时']
                },
                {
                    field: 'diet_adherence_star',
                    type: 'star',
                    max: 5,
                    title: '今天饮食控制情况（1–5 星）',
                    desc: '综合低盐/控糖/少油/规律三餐等。',
                    labels: ['未控制', '控制很好']
                },
                {
                    field: 'exercise_minutes',
                    type: 'score',
                    min: 0,
                    max: 120,
                    step: 10,
                    unit: '分钟',
                    title: '今天运动/步行时长（选填）',
                    desc: '如未运动可保持 0 分。',
                    required: false,
                    default: 0,
                    minLabel: '0',
                    maxLabel: '120'
                }
            ];

            const diseaseQs = [];
            if (disease === 'HTN') {
                diseaseQs.push(
                    {
                        field: 'dizzy_score',
                        type: 'score',
                        min: 0,
                        max: 10,
                        step: 1,
                        title: '头晕/头痛程度（0–10 分，选填）',
                        desc: '如无此症状可保持 0 分。',
                        required: false,
                        default: 0,
                        minLabel: '无',
                        maxLabel: '很重'
                    },
                    {
                        field: 'bp_measure',
                        title: '今天是否测量血压？',
                        options: [
                            { value: 0, label: '未测' },
                            { value: 1, label: '测过一次' },
                            { value: 2, label: '早晚各一次' },
                            { value: 3, label: '多次/不确定' }
                        ]
                    },
                    {
                        field: 'salt_intake',
                        title: '今天饮食咸淡情况？',
                        options: [
                            { value: 0, label: '偏淡/低盐' },
                            { value: 1, label: '正常' },
                            { value: 2, label: '偏咸' },
                            { value: 3, label: '很咸/重口' }
                        ]
                    }
                );
            } else if (disease === 'DM') {
                diseaseQs.push(
                    {
                        field: 'glucose_measure',
                        title: '今天是否测量血糖？',
                        options: [
                            { value: 0, label: '未测' },
                            { value: 1, label: '测过一次' },
                            { value: 2, label: '餐前/餐后都测' },
                            { value: 3, label: '多次/不确定' }
                        ]
                    },
                    {
                        field: 'hypo_sign',
                        title: '是否出现疑似低血糖表现？',
                        desc: '如出汗、心慌、手抖、头晕等。',
                        options: [
                            { value: 0, label: '没有' },
                            { value: 1, label: '轻微，休息可缓解' },
                            { value: 2, label: '明显，需要进食缓解' },
                            { value: 3, label: '严重，考虑就医' }
                        ]
                    },
                    {
                        field: 'sugar_intake_star',
                        type: 'star',
                        max: 5,
                        title: '今天控糖情况（1–5 星）',
                        desc: '少甜饮/少精制碳水/规律进餐。',
                        labels: ['未控制', '控制很好']
                    }
                );
            } else if (disease === 'HF') {
                diseaseQs.push(
                    {
                        field: 'orthopnea',
                        title: '今晚睡觉是否需要垫高枕头/半卧？',
                        options: [
                            { value: 0, label: '不需要' },
                            { value: 1, label: '需要垫高 1 个枕头' },
                            { value: 2, label: '需要半卧/多枕' },
                            { value: 3, label: '坐起才能缓解' }
                        ]
                    },
                    {
                        field: 'nocturia',
                        title: '夜间起夜（小便）次数？',
                        options: [
                            { value: 0, label: '0–1 次' },
                            { value: 1, label: '2 次' },
                            { value: 2, label: '3 次' },
                            { value: 3, label: '4 次以上' }
                        ]
                    },
                    {
                        field: 'chest_pain_score',
                        type: 'score',
                        min: 0,
                        max: 10,
                        step: 1,
                        title: '胸痛/压榨感程度（0–10 分，选填）',
                        desc: '如无此症状可保持 0 分。',
                        required: false,
                        default: 0,
                        minLabel: '无',
                        maxLabel: '很重'
                    }
                );
            } else if (disease === 'COPD') {
                diseaseQs.push(
                    {
                        field: 'cough_score',
                        type: 'score',
                        min: 0,
                        max: 10,
                        step: 1,
                        title: '咳嗽频率/程度（0–10 分）',
                        desc: '0 分无咳嗽，10 分频繁剧烈。',
                        minLabel: '无',
                        maxLabel: '很重'
                    },
                    {
                        field: 'sputum',
                        title: '痰的情况？',
                        options: [
                            { value: 0, label: '无/很少' },
                            { value: 1, label: '白色/清稀' },
                            { value: 2, label: '黄绿/增多' },
                            { value: 3, label: '带血/明显增多' }
                        ]
                    },
                    {
                        field: 'wheeze',
                        title: '是否出现喘鸣/哮喘样发作？',
                        options: [
                            { value: 0, label: '没有' },
                            { value: 1, label: '轻微，活动时有' },
                            { value: 2, label: '明显，影响活动' },
                            { value: 3, label: '休息时也有/考虑就医' }
                        ]
                    },
                    {
                        field: 'inhaler_star',
                        type: 'star',
                        max: 5,
                        title: '今天吸入剂/雾化是否规范（1–5 星）',
                        labels: ['不规范', '很规范']
                    }
                );
            } else {
                diseaseQs.push({
                    field: 'other_note',
                    type: 'text',
                    title: '今天是否有其他特别不适？（选填）',
                    placeholder: '例如：头晕、胸闷、咳嗽加重、血压/血糖波动、夜间憋醒等…',
                    required: false,
                    maxlength: 200
                });
            }

            const lastStep = {
                step: 4,
                questions: [
                    ...commonMgmt,
                    ...diseaseQs,
                    {
                        field: 'extra_note',
                        type: 'text',
                        title: `补充说明（选填）`,
                        desc: `如今天有就医、换药、特殊事件等可简单记录。`,
                        placeholder: `可不填`,
                        required: false,
                        maxlength: 200
                    }
                ]
            };

            const steps = [...base, lastStep];

            // 在第 4 步顶部加一句提示（通过 desc 呈现）
            steps[3].title = `慢病管理（${diseaseLabelMap[disease] || '慢性病'}）`;

            return steps;
        },

        initSteps() {
            // 读取上次选择的慢性病类型，默认高血压
          const lastDisease = getScopedStorageSync('survey_chronic_disease', 'HTN') || 'HTN';
            const steps = this.buildSteps(lastDisease);

            // 初始化 answers，保证第一题有默认值，避免用户无需选择时校验失败
            const answers = { ...(this.answers || {}) };
            if (!answers.chronic_disease) answers.chronic_disease = lastDisease;
            if (answers.overall_star === undefined) answers.overall_star = 3;
            if (answers.sleep_star === undefined) answers.sleep_star = 3;
            if (answers.mood_star === undefined) answers.mood_star = 3;
            if (answers.appetite_star === undefined) answers.appetite_star = 3;
            if (answers.med_adherence_star === undefined) answers.med_adherence_star = 4;
            if (answers.diet_adherence_star === undefined) answers.diet_adherence_star = 4;
            if (answers.fatigue_score === undefined) answers.fatigue_score = 0;
            if (answers.dyspnea_score === undefined) answers.dyspnea_score = 0;

            this.setData({
                steps,
                answers
            });
        },

        // dataset value 兼容 number/string
        normalizeDatasetValue(raw) {
            if (typeof raw === 'number') return raw;
            if (raw === null || raw === undefined) return raw;
            const s = String(raw);
            if (/^-?\d+(\.\d+)?$/.test(s)) return Number(s);
            return raw;
        },

        onSelectOption(e) {
            const field = e.currentTarget.dataset.field;
            const raw = e.currentTarget.dataset.value;
            const value = this.normalizeDatasetValue(raw);

            const nextAnswers = {
                ...this.answers,
                [field]: value
            };

            // 如果选择了慢性病类型，动态拼装第 4 步问题
            if (field === 'chronic_disease') {
               setScopedStorageSync('survey_chronic_disease', value);
                const steps = this.buildSteps(value);
                this.setData({
                    answers: nextAnswers,
                    steps
                });
                return;
            }

            this.setData({
                answers: nextAnswers
            });
        },

        onSetStar(e) {
            const field = e.currentTarget.dataset.field;
            const raw = e.currentTarget.dataset.value;
            const value = this.normalizeDatasetValue(raw);
            this.setData({
                answers: {
                    ...this.answers,
                    [field]: value
                }
            });
        },

        onSliderChange(e) {
            const field = e.currentTarget.dataset.field;
            const value = Number(e.detail.value);
            this.setData({
                answers: {
                    ...this.answers,
                    [field]: value
                }
            });
        },

        onTextInput(e) {
            const field = e.currentTarget.dataset.field;
            const value = e.detail.value;
            this.setData({
                answers: {
                    ...this.answers,
                    [field]: value
                }
            });
        },

        // 校验当前步是否全部作答（仅校验 required !== false 的题）
        validateCurrentStep() {
            const stepObj = this.steps[this.currentStep];
            if (!stepObj) return true;

            const missing = (stepObj.questions || []).filter((q) => {
                if (q.required === false) return false;
                const v = this.answers[q.field];
                if (q.type === 'text') return !v || String(v).trim().length === 0;
                return v === undefined;
            });

            if (missing.length > 0) {
                uni.showToast({
                    title: '请先完成本步所有必填问题',
                    icon: 'none'
                });
                return false;
            }
            return true;
        },

        onPrev() {
            if (this.currentStep === 0) return;
            this.setData({
                currentStep: this.currentStep - 1
            });
        },

        onNext() {
            if (!this.validateCurrentStep()) return;
            if (this.currentStep < this.steps.length - 1) {
                this.setData({
                    currentStep: this.currentStep + 1
                });
            }
        },

        buildRiskSummary(answers) {
            const signals = [];
            const dysp = Number(answers.dyspnea_score || 0);
            const edema = Number(answers.edema || 0);
            const chestT = Number(answers.chest_tightness || 0);
            const sleep = Number(answers.sleep_star || 3);
            const med = Number(answers.med_adherence_star || 3);

            if (dysp >= 7) signals.push('气短较重');
            if (edema >= 2) signals.push('浮肿明显');
            if (chestT >= 2) signals.push('胸闷较多');
            if (sleep <= 2) signals.push('睡眠较差');
            if (med <= 2) signals.push('可能漏服用药');

            // 分病种提示
            if (answers.chronic_disease === 'HF') {
                const orth = Number(answers.orthopnea || 0);
                const chestPain = Number(answers.chest_pain_score || 0);
                if (orth >= 2) signals.push('夜间需半卧/坐起');
                if (chestPain >= 7) signals.push('胸痛较重');
            }
            if (answers.chronic_disease === 'COPD') {
                const wheeze = Number(answers.wheeze || 0);
                const sputum = Number(answers.sputum || 0);
                if (wheeze >= 2) signals.push('喘鸣明显');
                if (sputum >= 2) signals.push('痰色/量异常');
            }
            if (answers.chronic_disease === 'DM') {
                const hypo = Number(answers.hypo_sign || 0);
                if (hypo >= 2) signals.push('疑似低血糖');
            }

            let level = 'low';
            if (signals.length >= 3 || dysp >= 8 || edema >= 3) level = 'high';
            else if (signals.length >= 1) level = 'mid';

            return { level, signals };
        },

        onSubmit() {
            // 提交前校验所有必填题目
            for (let i = 0; i < this.steps.length; i++) {
                const step = this.steps[i];
                const missing = (step.questions || []).filter((q) => {
                    if (q.required === false) return false;
                    const v = this.answers[q.field];
                    if (q.type === 'text') return !v || String(v).trim().length === 0;
                    return v === undefined;
                });
                if (missing.length > 0) {
                    uni.showToast({ title: '还有未完成的必填问题', icon: 'none' });
                    this.setData({ currentStep: i });
                    return;
                }
            }

            const answers = this.answers;
            const risk = this.buildRiskSummary(answers);

            // 结构化派生特征（供后端/AI）
            const fatigueScore = Number(answers.fatigue_score || 0); // 0-10
            const breathScore = Number(answers.dyspnea_score || 0); // 0-10
            const sleepRisk = 5 - Number(answers.sleep_star || 3); // 0-4（越大越差）
            const medRisk = 5 - Number(answers.med_adherence_star || 3); // 0-4

            const payload = {
                date: this.todayDateStr,
                answers,
                features: {
                    fatigueScore,
                    breathScore,
                    sleepRisk,
                    medRisk
                },
                risk
            };

            // 存本地（供首页判断今日是否已填 + 调试）：
           setScopedStorageSync('tcm_survey_today', payload);
           setScopedStorageSync('tcm_survey_date', this.todayDateStr);

            // ✅ 同步到后端
            submitTcmSurvey(payload)
                .then(() => {
                    this.onSubmitDone(risk);
                })
                .catch(() => {
                    if (USE_MOCK_FALLBACK) {
                        this.onSubmitDone(risk, true);
                    } else {
                        uni.showToast({ title: '提交失败', icon: 'none' });
                    }
                });
        },

        onSubmitDone(risk, localOnly = false) {
            const base = localOnly ? '已提交（本地）' : '已提交';

            if (risk && risk.level === 'high' && risk.signals && risk.signals.length) {
                uni.showModal({
                    title: base,
                    content: `今日需重点关注：${risk.signals.slice(0, 4).join('、')}。如不适加重，请及时联系医生或就医。`,
                    showCancel: false,
                    success: () => {
                        uni.navigateBack();
                    }
                });
            } else {
                uni.showToast({ title: base, icon: 'success' });
                setTimeout(() => uni.navigateBack(), 600);
            }
        }
    }
};
</script>

<style>
@import './survey.css';
</style>
