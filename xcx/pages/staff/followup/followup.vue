<template>
    <!-- pages/staff/followup/followup.wxml -->
    <view class="page fu-page">
        <!-- 患者信息与风险概况 -->
        <view class="card">
            <view class="fu-header">
                <view>
                    <view class="fu-name">{{ safePatientName }}<text v-if="patient.age || patient.gender">（{{ patient.age || '' }}岁{{ patient.gender || '' }}）</text></view>
                    <view class="fu-sub">任务类型：{{ task.typeText }} · 计划时间：{{ task.planTime }}</view>
                    <view class="fu-ai">{{ task.aiSummary }}</view>
                </view>

                <view :class="'risk-tag ' + task.riskLevel">
                    {{ task.riskText }}
                </view>
            </view>
        </view>

        <!-- 一、联系情况 -->
        <view class="card">
            <view class="section-title">一、联系情况</view>

            <view class="fu-field">
                <view class="fu-label">联系方式</view>
                <picker mode="selector" :range="contactModes" :value="contactModeIndex" @change="onContactModeChange">
                    <view class="fu-picker">{{ contactModes[contactModeIndex] }}</view>
                </picker>
            </view>

            <view class="fu-field">
                <view class="fu-label">联系结果</view>
                <picker mode="selector" :range="contactResults" :value="contactResultIndex" @change="onContactResultChange">
                    <view class="fu-picker">{{ contactResults[contactResultIndex] }}</view>
                </picker>
            </view>
        </view>

        <!-- 二、症状与体征 -->
        <view class="card">
            <view class="section-title">二、症状与体征</view>

            <view class="fu-field">
                <view class="fu-label">症状变化</view>
                <textarea class="fu-textarea" placeholder="如：气短无明显加重，夜间憋醒较前减少等" :value="form.symptomChange" @input="onInput" data-field="symptomChange"></textarea>
            </view>

            <view class="fu-field">
                <view class="fu-label">本次测量（可选）</view>
                <view class="fu-input-row">
                    <input class="fu-input" type="digit" placeholder="收缩压" :value="form.sbp" data-field="sbp" @input="onInput" />
                    <view class="fu-unit">mmHg</view>
                    <input class="fu-input" type="digit" placeholder="舒张压" :value="form.dbp" data-field="dbp" @input="onInput" />
                    <view class="fu-unit">mmHg</view>
                </view>

                <view class="fu-input-row" style="margin-top: 10rpx">
                    <input class="fu-input" type="digit" placeholder="心率" :value="form.hr" data-field="hr" @input="onInput" />
                    <view class="fu-unit">次/分</view>
                    <input class="fu-input" type="digit" placeholder="体温(℃)" :value="form.weight" data-field="weight" @input="onInput" />
                    <view class="fu-unit">℃</view>
                </view>
            </view>
        </view>

        <!-- 三、用药与不良反应 -->
        <view class="card">
            <view class="section-title">三、用药与不良反应</view>

            <view class="fu-field">
                <view class="fu-label">服用药品名称</view>
                <input class="fu-input" v-model="form.medicineName" placeholder="如：阿托伐他汀、二甲双胍等（可选）" />
            </view>

            <view class="fu-field">
                <view class="fu-label">用药依从性</view>
                <picker mode="selector" :range="adherenceOptions" :value="adherenceIndex" @change="onAdherenceChange">
                    <view class="fu-picker">{{ adherenceOptions[adherenceIndex] }}</view>
                </picker>
            </view>

            <view class="fu-field">
                <view class="fu-label">不良反应</view>
                <textarea class="fu-textarea" placeholder="如：偶有轻度胃部不适，无明显头晕、乏力等。" :value="form.adverse" @input="onInput" data-field="adverse"></textarea>
            </view>
        </view>

        <!-- 四、中医体征（舌象 + 脉象） -->
        <view class="card">
            <view class="section-title">四、中医体征（医护端填写）</view>

            <view class="fu-field">
                <view class="fu-label">舌象照片（可选）</view>
                <view class="tcm-upload-row">
                    <image v-if="tcm.tongueImage" :src="tcm.tongueImage" mode="aspectFill" class="tongue-img" @tap="onPreviewTongue" />
                    <view v-else class="tongue-placeholder" @tap="onChooseTongue">+ 上传舌象照片</view>
                </view>
                <view class="tcm-tip">建议在自然光下，张口伸舌拍照，后续可用于医生/AI 辨证参考。</view>
            </view>

            <view class="fu-field">
                <view class="fu-label">面色</view>
                <picker mode="selector" :range="faceOptions" :value="faceIndex" @change="onFaceChange">
                    <view class="fu-picker">{{ faceOptions[faceIndex] }}</view>
                </picker>
            </view>

            <view class="fu-field">
                <view class="fu-label">舌体</view>
                <picker mode="selector" :range="tongueBodyOptions" :value="tongueBodyIndex" @change="onTongueBodyChange">
                    <view class="fu-picker">{{ tongueBodyOptions[tongueBodyIndex] }}</view>
                </picker>
            </view>

            <view class="fu-field">
                <view class="fu-label">舌苔</view>
                <picker mode="selector" :range="tongueCoatOptions" :value="tongueCoatIndex" @change="onTongueCoatChange">
                    <view class="fu-picker">{{ tongueCoatOptions[tongueCoatIndex] }}</view>
                </picker>
            </view>

            <view class="fu-field">
                <view class="fu-label">脉象</view>
                <textarea class="fu-textarea" v-model="tcm.pulseDesc" placeholder="如：弦细、滑数、沉弱等（可选）"></textarea>

            </view>

            <view class="fu-field">
                <view class="fu-label">中医评价与建议</view>
                <textarea class="fu-textarea" placeholder="如：舌淡胖、苔白腻，脉弦滑，考虑痰湿夹瘀倾向；建议规律作息、清淡饮食并按时复诊。" :value="tcm.comment" @input="onTcmCommentInput"></textarea>
            </view>
        </view>

        <!-- 五、化验指标与评估 -->
        <view class="card">
            <view class="section-title">五、化验指标与评估</view>

            <view class="fu-field">
                <view class="fu-label">空腹血糖 FPG</view>
                <view class="fu-input-row">
                    <input class="fu-input" type="digit" placeholder="数值" :value="labs.fpg" data-field="fpg" @input="onLabInput" />
                    <view class="fu-unit">mmol/L</view>
                    <picker mode="selector" :range="labStatusOptions" :value="labStatusIndex.fpg" data-key="fpg" @change="onLabStatusChange">
                        <view class="fu-picker-small">
                            {{ labStatusOptions[labStatusIndex.fpg] }}
                        </view>
                    </picker>
                </view>
            </view>

            <view class="fu-field">
                <view class="fu-label">低密度胆固醇 LDL-C</view>
                <view class="fu-input-row">
                    <input class="fu-input" type="digit" placeholder="数值" :value="labs.ldl" data-field="ldl" @input="onLabInput" />
                    <view class="fu-unit">mmol/L</view>
                    <picker mode="selector" :range="labStatusOptions" :value="labStatusIndex.ldl" data-key="ldl" @change="onLabStatusChange">
                        <view class="fu-picker-small">
                            {{ labStatusOptions[labStatusIndex.ldl] }}
                        </view>
                    </picker>
                </view>
            </view>

            <view class="fu-field">
                <view class="fu-label">甘油三酯 TG</view>
                <view class="fu-input-row">
                    <input class="fu-input" type="digit" placeholder="数值" :value="labs.tg" data-field="tg" @input="onLabInput" />
                    <view class="fu-unit">mmol/L</view>
                    <picker mode="selector" :range="labStatusOptions" :value="labStatusIndex.tg" data-key="tg" @change="onLabStatusChange">
                        <view class="fu-picker-small">
                            {{ labStatusOptions[labStatusIndex.tg] }}
                        </view>
                    </picker>
                </view>
            </view>

            <view class="fu-field">
                <view class="fu-label">同型半胱氨酸 Hcy</view>
                <view class="fu-input-row">
                    <input class="fu-input" type="digit" placeholder="数值" :value="labs.hcy" data-field="hcy" @input="onLabInput" />
                    <view class="fu-unit">μmol/L</view>
                    <picker mode="selector" :range="labStatusOptions" :value="labStatusIndex.hcy" data-key="hcy" @change="onLabStatusChange">
                        <view class="fu-picker-small">
                            {{ labStatusOptions[labStatusIndex.hcy] }}
                        </view>
                    </picker>
                </view>
            </view>

            <view class="fu-field">
                <view class="fu-label">尿酸 UA</view>
                <view class="fu-input-row">
                    <input class="fu-input" type="digit" placeholder="数值" :value="labs.ua" data-field="ua" @input="onLabInput" />
                    <view class="fu-unit">μmol/L</view>
                    <picker mode="selector" :range="labStatusOptions" :value="labStatusIndex.ua" data-key="ua" @change="onLabStatusChange">
                        <view class="fu-picker-small">
                            {{ labStatusOptions[labStatusIndex.ua] }}
                        </view>
                    </picker>
                </view>
            </view>

            <view class="fu-field">
                <view class="fu-label">健康建议（可选）</view>
                <textarea class="fu-textarea" placeholder="如：血脂略高，建议控制油脂摄入、规律运动，并于 2~4 周后复查。" :value="labs.comment" @input="onLabInput" data-field="comment"></textarea>
            </view>
        </view>

        <!-- 六、随访小结与计划 -->
        <view class="card">
            <view class="section-title">六、本次随访小结与计划</view>

            <view class="fu-field">
                <view class="fu-label">本次随访小结</view>
                <textarea class="fu-textarea" placeholder="简要写明目前情况评估、建议及是否需门诊/急诊等。" :value="form.summary" @input="onInput" data-field="summary"></textarea>
            </view>

            <view class="fu-field">
                <view class="fu-label">风险分层</view>
                <picker mode="selector" :range="riskOptions" :value="riskIndex" @change="onRiskChange">
                    <view class="fu-picker">
                        {{ riskOptions[riskIndex] }}
                    </view>
                </picker>
            </view>

            <view class="fu-field">
                <view class="fu-label">下一次随访方式</view>
                <picker mode="selector" :range="planOptions" :value="planIndex" @change="onPlanChange">
                    <view class="fu-picker">
                        {{ planOptions[planIndex] }}
                    </view>
                </picker>
            </view>

            <view class="fu-field">
                <view class="fu-label">下一次随访日期</view>
                <picker mode="date" :value="nextVisitDate" :start="today" @change="onNextDateChange">
                    <view class="fu-picker">
                        {{ nextVisitDate || '请选择日期' }}
                    </view>
                </picker>
            </view>
        </view>

        <!-- 底部按钮 -->
        <view class="card">
            <button class="btn-primary" @tap="onSubmit">保存随访记录</button>
        </view>
    </view>
</template>

<script>
// pages/staff/followup/followup.js
import { USE_MOCK_FALLBACK } from '@/config/api.js';
import { upload } from '@/api/common.js';
import { getAlertDetail, getTaskDetail, submitFollowup, getFollowupByTaskId } from '@/api/staff.js';

export default {
    data() {
        return {
            alertId: '',
            taskId: '',
            today: '',
            nextVisitDate: '',
            mode: 'new',
            patient: {
                id: '',
                name: '',
                age: '',
                gender: ''
            },
            task: {
                id: '',
                typeText: '',
                planTime: '',
                aiSummary: '',
                riskLevel: 'mid',
                riskText: '中风险'
            },
            contactModes: ['电话', '视频', '上门面谈'],
            contactModeIndex: 0,
            contactResults: ['已联系到本人', '联系到家属', '无人接听', '停机/错号'],
            contactResultIndex: 0,
            adherenceOptions: ['规律服药', '偶尔漏服', '经常漏服/自行停药'],
            adherenceIndex: 0,
            riskOptions: ['低风险', '中风险', '高风险'],
            riskIndex: 1,
            planOptions: ['常规随访', '1周内电话随访', '安排门诊复诊', '需急诊或住院评估'],
            planIndex: 0,
            form: {
                symptomChange: '',
                sbp: '',
                dbp: '',
                hr: '',
                weight: '',
                medicineName: '',
                adverse: '',
                summary: ''
            },
            // 化验
            labs: {
                fpg: '',
                ldl: '',
                tg: '',
                hcy: '',
                ua: '',
                comment: ''
            },
            labStatusOptions: ['未评估', '正常', '偏高', '偏低'],
            labStatusIndex: {
                fpg: 0,
                ldl: 0,
                tg: 0,
                hcy: 0,
                ua: 0
            },
            // 中医体征
            tcm: {
                tongueImage: '',
                tongueFileId: '',
                tongueUrl: '',
                face: 0,
                tongueBody: 0,
                tongueCoat: 0,
                pulseDesc: '',
                summary: 1,
                comment: ''
            },
            faceOptions: ['未评估', '正常', '面色白', '萎黄', '潮红', '暗滞'],
            faceIndex: 0,
            tongueBodyOptions: ['未评估', '正常', '淡胖', '瘦薄', '紫暗'],
            tongueBodyIndex: 0,
            tongueCoatOptions: ['未评估', '薄白', '厚腻', '黄苔', '少苔/无苔'],
            tongueCoatIndex: 0,
            tcmSummaryOptions: ['未评估', '整体平稳', '偏气虚/阳虚', '痰湿偏重', '血瘀明显', '其他'],
            tcmSummaryIndex: 0
        };
    },

    computed: {
        safePatientName() {
            const raw = this.patient && this.patient.name != null ? String(this.patient.name) : '';
            const n = raw.trim();
            if (!n) return '未知患者';
            // 兼容：null/undefined/患者#null/患者#undefined/患者#/仅符号
            const cleaned = n.replace(/null|undefined/gi, '').replace(/[#\s]+$/g, '').trim();
            if (!cleaned) return '未知患者';
            if (cleaned === '患者' || cleaned === '患者#' || cleaned.toLowerCase() === 'patient' || cleaned.toLowerCase() === 'patient#') return '未知患者';
            return cleaned;
        }
    },

    onLoad(options) {
        const { alertId, taskId, mode } = options || {};
        const today = this.formatDate(new Date());
        const pageMode = mode || 'new';
        // 允许从任务列表页透传患者/任务展示信息（避免详情接口未就绪时出现“未知患者”）
        const presetPatient = {
            id: options.patientId || options.pid || '',
            name: (() => { const n = options.patientName || options.name || ''; return (n && n !== 'null' && n !== 'undefined') ? n : ''; })(),
            age: options.age || '',
            gender: options.gender || ''
        };
        const presetTask = {
            typeText: options.typeText || '',
            planTime: options.planTime || '',
            aiSummary: options.aiSummary || '',
            riskLevel: options.riskLevel || '',
            riskText: options.riskText || ''
        };

        this.setData({
            alertId: alertId || '',
            taskId: taskId || '',
            mode: pageMode,
            today,
            patient: { ...this.patient, ...presetPatient },
            task: { ...this.task, ...presetTask }
        });

        // 根据模式设置标题
        let title = '填写随访记录';
        if (pageMode === 'reschedule') {
            title = '重新安排随访';
        } else if (pageMode === 'edit') {
            title = '随访记录详情';
        }
        uni.setNavigationBarTitle({
            title
        });

        // ✅ 优先请求后端加载任务/告警详情，失败时才使用 demo
        this.loadDetail();
    },
    methods: {
        formatDate(d) {
            const y = d.getFullYear();
            const m = ('0' + (d.getMonth() + 1)).slice(-2);
            const day = ('0' + d.getDate()).slice(-2);
            return `${y}-${m}-${day}`;
        },

        // 任务/告警详情：优先后端
        loadDetail() {
            const { taskId, alertId } = this;

            let p;
            if (taskId) {
                p = getTaskDetail(taskId);
            } else if (alertId) {
                p = getAlertDetail(alertId);
            } else {
                p = Promise.reject(new Error('缺少 taskId/alertId'));
            }

            p.then((res) => {
                const data = res || {};
                const patient = data.patient || {};
                const task = data.task || {};
                // 兜底：后端可能返回字符串 'null'
                if (patient && (patient.name === 'null' || patient.name === 'undefined')) patient.name = '';

                this.setData({
                    patient: { ...this.patient, ...patient },
                    task: { ...this.task, ...task },
                    // 允许后端回传关联 id
                    taskId: this.taskId || task.id || data.taskId || '',
                    alertId: this.alertId || data.alertId || ''
                });

                // 若后端返回历史随访记录（编辑/查看详情）可在这里回填
                const record = data.record || data.followupRecord;
                if (record && typeof record === 'object') {
                    this.hydrateFromRecord(record);
                }
                // 若任务详情未带 record，则尝试按 taskId 拉取（用于“退出再进仍可回显”）
                const tid = this.taskId || task.id || data.taskId || '';
                if ((!record || typeof record !== 'object') && tid) {
                    getFollowupByTaskId(tid).then((rr) => {
                        const rdata = (rr && rr.data !== undefined) ? rr.data : rr;
                        if (rdata && typeof rdata === 'object') {
                            this.hydrateFromRecord(rdata);
                        }
                    }).catch(() => {});
                }
            }).catch(() => {
                if (USE_MOCK_FALLBACK) {
                    // fallback demo
                    if (alertId) return this.fetchTaskDetail(alertId);
                    if (taskId) return this.fetchTaskDetailByTaskId(taskId);
                }
                uni.showToast({ title: '加载任务详情失败', icon: 'none' });
            });
        },

        hydrateFromRecord(record) {
            // 兼容后端字段命名差异，尽量只覆盖已定义字段
            try {
                if (record.form) {
                    this.form = { ...this.form, ...record.form };
                }
                if (record.labs) {
                    this.labs = { ...this.labs, ...record.labs };
                }
                if (record.nextVisitDate) {
                    this.nextVisitDate = record.nextVisitDate;
                }
                if (record.contactMode) {
                    const idx = this.contactModes.indexOf(record.contactMode);
                    if (idx >= 0) this.contactModeIndex = idx;
                }
                if (record.contactResult) {
                    const idx = this.contactResults.indexOf(record.contactResult);
                    if (idx >= 0) this.contactResultIndex = idx;
                }
                if (record.adherence) {
                    const idx = this.adherenceOptions.indexOf(record.adherence);
                    if (idx >= 0) this.adherenceIndex = idx;
                }
                if (record.riskLevelText) {
                    const idx = this.riskOptions.indexOf(record.riskLevelText);
                    if (idx >= 0) this.riskIndex = idx;
                }
                if (record.nextPlan) {
                    const idx = this.planOptions.indexOf(record.nextPlan);
                    if (idx >= 0) this.planIndex = idx;
                }

                if (record.tcm) {
                    const t = record.tcm;
                    this.tcm = { ...this.tcm, ...t };
                    // 若后端返回舌象 url/fileId
                    if (t.tongueUrl && !t.tongueImage) this.tcm.tongueImage = t.tongueUrl;
                }
                const tcmEvaluationAdvice = record.tcmEvaluationAdvice || [record.tcmRemark, record.tcmConclusion].filter((item, index, arr) => item && arr.indexOf(item) === index).join('；');
                if (tcmEvaluationAdvice) {
                    this.tcm = {
                        ...this.tcm,
                        comment: tcmEvaluationAdvice
                    };
                }
                const healthAdvice = record.healthAdvice || record.labSummary;
                if (healthAdvice) {
                    this.labs = {
                        ...this.labs,
                        comment: healthAdvice
                    };
                }
            } catch (e) {}
        },

        // ===== 普通表单输入 ===== //
        onInput(e) {
            const field = e.currentTarget.dataset.field;
            const value = e.detail.value;
            this.setData({
                form: {
                    ...this.form,
                    [field]: value
                }
            });
        },

        onLabInput(e) {
            const field = e.currentTarget.dataset.field;
            const value = e.detail.value;
            this.setData({
                labs: {
                    ...this.labs,
                    [field]: value
                }
            });
        },

        onContactModeChange(e) {
            this.setData({
                contactModeIndex: Number(e.detail.value)
            });
        },

        onContactResultChange(e) {
            this.setData({
                contactResultIndex: Number(e.detail.value)
            });
        },

        onAdherenceChange(e) {
            this.setData({
                adherenceIndex: Number(e.detail.value)
            });
        },

        onRiskChange(e) {
            this.setData({
                riskIndex: Number(e.detail.value)
            });
        },

        onPlanChange(e) {
            this.setData({
                planIndex: Number(e.detail.value)
            });
        },

        onNextDateChange(e) {
            this.setData({
                nextVisitDate: e.detail.value
            });
        },

        onLabStatusChange(e) {
            const key = e.currentTarget.dataset.key;
            const index = Number(e.detail.value);
            this.setData({
                labStatusIndex: {
                    ...this.labStatusIndex,
                    [key]: index
                }
            });
        },

        // ===== 中医体征相关 ===== //

        onChooseTongue() {
            uni.chooseImage({
                count: 1,
                sizeType: ['compressed'],
                success: (res) => {
                    const path = res.tempFilePaths[0];
                    // 先本地展示
                    this.setData({
                        tcm: {
                            ...this.tcm,
                            tongueImage: path,
                            tongueFileId: '',
                            tongueUrl: ''
                        }
                    });

                    // ✅ 自动上传到后端（可选）
                    uni.showLoading({ title: '上传中…', mask: true });
                    upload(path, {
                        bizType: 'tongue',
                        patientId: this.patient.id || '',
                        taskId: this.taskId || this.task.id || '',
                        alertId: this.alertId || ''
                    })
                        .then((data) => {
                            const fileId = (data && (data.fileId || data.id)) || '';
                            const url = (data && data.url) || '';
                            this.setData({
                                tcm: {
                                    ...this.tcm,
                                    tongueFileId: fileId,
                                    tongueUrl: url,
                                    // 若返回 url，则用 url 作为展示源，避免重启后丢失
                                    tongueImage: url || this.tcm.tongueImage
                                }
                            });
                        })
                        .catch(() => {
                            if (!USE_MOCK_FALLBACK) {
                                uni.showToast({ title: '舌象上传失败', icon: 'none' });
                            }
                        })
                        .finally(() => {
                            uni.hideLoading();
                        });
                }
            });
        },

        onPreviewTongue() {
            uni.previewImage({
                urls: [this.tcm.tongueImage]
            });
        },

        onFaceChange(e) {
            const index = Number(e.detail.value);
            this.setData({
                faceIndex: index,
                tcm: {
                    ...this.tcm,
                    face: index
                }
            });
        },

        onTongueBodyChange(e) {
            const index = Number(e.detail.value);
            this.setData({
                tongueBodyIndex: index,
                tcm: {
                    ...this.tcm,
                    tongueBody: index
                }
            });
        },

        onTongueCoatChange(e) {
            const index = Number(e.detail.value);
            this.setData({
                tongueCoatIndex: index,
                tcm: {
                    ...this.tcm,
                    tongueCoat: index
                }
            });
        },

        onTcmSummaryChange(e) {
            const index = Number(e.detail.value);
            this.setData({
                tcmSummaryIndex: index,
                tcm: {
                    ...this.tcm,
                    summary: index
                }
            });
        },

        onTcmCommentInput(e) {
            const value = e.detail.value;
            this.setData({
                tcm: {
                    ...this.tcm,
                    comment: value
                }
            });
        },

        // ===== 提交随访记录 ===== //

        onSubmit() {
            const payload = {
                alertId: this.alertId || '',
                taskId: this.taskId || this.task.id || '',
                patientId: this.patient.id || '',
                mode: this.mode,
                contactMode: this.contactModes[this.contactModeIndex],
                contactResult: this.contactResults[this.contactResultIndex],
                adherence: this.adherenceOptions[this.adherenceIndex],
                riskLevel: this.riskOptions[this.riskIndex],
                nextPlan: this.planOptions[this.planIndex],
                nextVisitDate: this.nextVisitDate,
                form: this.form,
                labs: {
                    ...this.labs,
                    status: {
                        fpg: this.labStatusOptions[this.labStatusIndex.fpg],
                        ldl: this.labStatusOptions[this.labStatusIndex.ldl],
                        tg: this.labStatusOptions[this.labStatusIndex.tg],
                        hcy: this.labStatusOptions[this.labStatusIndex.hcy],
                        ua: this.labStatusOptions[this.labStatusIndex.ua]
                    }
                },
                tcm: {
                    tongueFileId: this.tcm.tongueFileId,
                    tongueUrl: this.tcm.tongueUrl,
                    tongueImage: this.tcm.tongueImage,
                    faceIndex: this.faceIndex,
                    tongueBodyIndex: this.tongueBodyIndex,
                    tongueCoatIndex: this.tongueCoatIndex,
                    pulseDesc: this.tcm.pulseDesc,
                    summaryIndex: this.tcmSummaryIndex,
                    comment: this.tcm.comment
                }
            };
            // ===== 兼容后端当前字段（顶层字段名/枚举）=====
            // 1) 风险等级：前端中文 → 后端枚举 HIGH/MID/LOW（患者端也能正确映射）
            const riskText = payload.riskLevel || '';
            let riskEnum = 'MID';
            if (String(riskText).includes('高')) riskEnum = 'HIGH';
            else if (String(riskText).includes('低')) riskEnum = 'LOW';
            payload.riskLevelText = riskText;
            payload.riskLevel = riskEnum;

            // 2) 顶层字段：兼容当前后端 StaffController 只读的键名
            payload.followupType = payload.contactMode;         // 目前后端用 followupType
            payload.resultStatus = 'COMPLETED';                // 不传则后端默认 COMPLETED，这里显式写入
            payload.mainSymptoms = payload.form.symptomChange;  // 后端用 mainSymptoms → symptom_change
            payload.summary = payload.form.summary;             // 后端用 summary → content_summary
            payload.healthAdvice = (payload.labs && payload.labs.comment) ? payload.labs.comment : '';
            payload.labSummary = payload.healthAdvice;
            payload.tcmEvaluationAdvice = (payload.tcm && payload.tcm.comment) ? payload.tcm.comment : '';
            payload.tcmConclusion = payload.tcmEvaluationAdvice;
            payload.tcmRemark = payload.tcmEvaluationAdvice;
            payload.advice = payload.healthAdvice || payload.tcmEvaluationAdvice || ''; // 后端 ext1

            // 3) 兼容后端尚未接入的字段：保留原结构（后端升级后可完整落库）
            payload.nextFollowupDate = payload.nextVisitDate;
            payload.nextFollowupPlan = payload.nextPlan;

            if (!payload.form.summary && !payload.form.symptomChange) {
                uni.showToast({
                    title: '请至少填写症状变化或随访小结',
                    icon: 'none'
                });
                return;
            }

            uni.showLoading({ title: '保存中…', mask: true });

           submitFollowup(payload)
               .then(() => {
                   uni.showToast({ title: '已保存', icon: 'success' });
                   setTimeout(() => uni.navigateBack(), 600);
               })
                .catch((err) => {
                    uni.showToast({
                        title: USE_MOCK_FALLBACK ? '已保存（本地模拟）' : ((err && err.message) || '保存失败'),
                        icon: USE_MOCK_FALLBACK ? 'success' : 'none'
                    });
                    if (USE_MOCK_FALLBACK) setTimeout(() => uni.navigateBack(), 600);
                })
                .finally(() => {
                    uni.hideLoading();
                });
        },

        // ===== Demo：兜底（无后端时） ===== //

        fetchTaskDetail(alertId) {
            // 实际情况应根据 alertId 请求后端，这里用与 alerts.js 对应的 demo
            let demo;
            if (alertId === 'a1') {
                demo = {
                    patient: {
                        id: 'p1',
                        name: '张阿姨',
                        age: 68,
                        gender: '女'
                    },
                    task: {
                        id: 'task1',
                        typeText: '电话随访',
                        planTime: '今天 09:30',
                        aiSummary: '近3天血压偏高，体温偏高，夜间憋醒次数增多。',
                        riskLevel: 'high',
                        riskText: '高风险'
                    }
                };
            } else {
                demo = {
                    patient: {
                        id: 'unknown',
                        name: '未知患者',
                        age: '',
                        gender: ''
                    },
                    task: {
                        id: this.taskId || 'unknown',
                        typeText: '电话随访',
                        planTime: '',
                        aiSummary: '',
                        riskLevel: 'mid',
                        riskText: '中风险'
                    }
                };
            }
            this.setData({
                patient: demo.patient,
                task: demo.task
            });

        },

        fetchTaskDetailByTaskId(taskId) {
            // 与 tasks.vue 的 demo 对齐
            let demo;
            if (taskId === 'task1') {
                demo = {
                    patient: { id: 'p1', name: '张阿姨', age: 68, gender: '女' },
                    task: { id: 'task1', typeText: '电话随访', planTime: '今天 09:30', aiSummary: '近3天血压波动偏大，夜间憋醒2次。', riskLevel: 'high', riskText: '高风险' }
                };
            } else if (taskId === 'task2') {
                demo = {
                    patient: { id: 'p2', name: '李先生', age: 72, gender: '男' },
                    task: { id: 'task2', typeText: '上门随访', planTime: '今天 14:00', aiSummary: '体温略偏高，步行完成度较低。', riskLevel: 'mid', riskText: '中风险' }
                };
            } else {
                demo = {
                    patient: { id: 'unknown', name: '未知患者', age: '', gender: '' },
                    task: { id: taskId || 'unknown', typeText: '随访任务', planTime: '', aiSummary: '', riskLevel: 'mid', riskText: '中风险' }
                };
            }
            this.setData({ patient: demo.patient, task: demo.task });
        }
    }
};
</script>
<style>
@import './followup.css';
</style>