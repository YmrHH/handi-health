import { getScopedStorageSync, setScopedStorageSync } from '@/utils/session.js';
<template>
    <!-- pages/patient/monitor/monitor.vue -->
    <view class="page monitor-page">
        <!-- 顶部 Tab：日常自测 / 医院检查结果 -->
        <view class="tab-bar">
            <view :class="'tab-item ' + (activeTab === 'daily' ? 'active' : '')" data-tab="daily" @tap="onChangeTab">日常自测</view>
            <view :class="'tab-item ' + (activeTab === 'lab' ? 'active' : '')" data-tab="lab" @tap="onChangeTab">医院检查结果</view>
        </view>

        <!-- 日常自测 Tab -->
        <view v-if="activeTab === 'daily'" class="tab-content">
            <!-- 基础信息卡片 - 固定不需要每天填写 -->
            <view class="card basic-info-card">
                <view class="section-title-row">
                    <view class="section-title">基础信息</view>
                    <view class="section-action" @tap="showBasicInfoModal">编辑</view>
                </view>

                <view class="info-summary-row">
                    <view class="info-item">
                        <view class="info-label">身高</view>
                        <view class="info-value">{{basicInfo.heightCm || '--'}} <text class="info-unit">cm</text></view>
                    </view>
                    <view class="info-item">
                        <view class="info-label">基础体重</view>
                        <view class="info-value">{{basicInfo.baseWeight || '--'}} <text class="info-unit">kg</text></view>
                    </view>
                    <view class="info-item">
                        <view class="info-label">BMI</view>
                        <view :class="'info-value ' + basicInfo.bmiClass">{{basicInfo.bmi || '--'}}</view>
                    </view>
                </view>
                
                <view class="info-updated">上次更新: {{basicInfo.lastUpdated || '未设置'}}</view>
            </view>

            <!-- 今日测量卡片 - 每日需要更新 -->
            <view class="card">
                <view class="section-title">今日血压 / 心率</view>
                <view class="realtime-note">实时更新，请输入最新测量的数值</view>

                <view class="field-row">
                    <view class="field-label">血压</view>
                    <view class="field-input">
                        <input type="digit" placeholder="收缩压" placeholder-class="placeholder" :value="form.sbp" data-field="sbp" @input="onInput" />
                        <text class="field-unit">/</text>
                        <input type="digit" placeholder="舒张压" placeholder-class="placeholder" :value="form.dbp" data-field="dbp" @input="onInput" />
                        <text class="field-unit">mmHg</text>
                    </view>
                </view>

                <view class="field-row">
                    <view class="field-label">心率</view>
                    <view class="field-input">
                        <input type="digit" placeholder="请输入心率" placeholder-class="placeholder" :value="form.hr" data-field="hr" @input="onInput" />
                        <text class="field-unit">次/分</text>
                    </view>
                </view>
                
                <view class="last-updated" v-if="form.bpLastUpdated">上次测量: {{form.bpLastUpdated}}</view>
            </view>

            <!-- 删除了"今日体重"卡片 -->

            <view class="card">
                <view class="section-title">今日症状与生活情况</view>

                <view class="field-row">
                    <view class="field-label">睡眠情况</view>
                    <picker mode="selector" :range="sleepOptions" :value="sleepIndex" @change="onSleepChange">
                        <view class="picker-display">
                            {{ sleepOptions[sleepIndex] }}
                        </view>
                    </picker>
                </view>

                <view class="field-row">
                    <view class="field-label">食欲情况</view>
                    <picker mode="selector" :range="appetiteOptions" :value="appetiteIndex" @change="onAppetiteChange">
                        <view class="picker-display">
                            {{ appetiteOptions[appetiteIndex] }}
                        </view>
                    </picker>
                </view>

                <view class="field-row">
                    <view class="field-label">大便情况</view>
                    <picker mode="selector" :range="stoolOptions" :value="stoolIndex" @change="onStoolChange">
                        <view class="picker-display">
                            {{ stoolOptions[stoolIndex] }}
                        </view>
                    </picker>
                </view>

                <view class="field-row">
                    <view class="field-label">今日血氧 (SpO₂) <text class="sync-badge" @tap="syncSpO2">同步血氧</text></view>
                    <view class="field-input">
                        <input type="digit" placeholder="请输入血氧或同步" placeholder-class="placeholder" :value="form.spo2" data-field="spo2" @input="onInput" />
                        <text class="field-unit">%</text>
                    </view>
                </view>
                <view class="last-updated" v-if="form.spo2LastUpdated">上次更新: {{form.spo2LastUpdated}}</view>

                <view class="field-row">
                    <view class="field-label">其他症状</view>
                    <textarea
                        class="textarea"
                        placeholder="如胸闷、气短、下肢水肿等"
                        placeholder-class="placeholder"
                        :value="form.symptomNote"
                        data-field="symptomNote"
                        @input="onInput"></textarea>
                </view>
            </view>

            <view class="card">
                <button class="btn-primary" @tap="onSaveDaily">保存今日记录</button>
            </view>
        </view>

        <!-- 医院检查结果 Tab -->
        <view v-else-if="activeTab === 'lab'" class="tab-content">
            <view class="card" v-for="(item, index) in labs" :key="index">
                <view class="lab-header">
                    <view class="lab-date">{{ item.date }}</view>
                    <view class="lab-hospital">{{ item.hospital }}</view>
                </view>

                <view class="lab-items">
                    <view class="lab-item" v-for="(labItem, index1) in item.items" :key="index1">
                        <view class="lab-name">{{ labItem.name }}</view>

                        <view class="lab-right">
                            <text class="lab-value">{{ labItem.value }}</text>
                            <text class="lab-unit">{{ labItem.unit }}</text>
                            <text :class="'lab-tag ' + labItem.status">
                                {{ labItem.statusText }}
                            </text>
                        </view>
                    </view>
                </view>

                <view class="lab-note" v-if="item.comment">医生提示：{{ item.comment }}</view>
            </view>

            <view v-if="!labs || labs.length === 0" class="empty-lab">暂无医院化验结果记录，可在门诊化验后由医护人员录入。</view>
        </view>
    </view>
    
    <!-- 基础信息编辑弹窗 - 改为使用全屏弹窗，与第二张图片一致 -->
    <view class="popup-container" v-if="showBasicInfoPopup">
        <view class="popup-mask" @tap="closeBasicInfoModal"></view>
        <view class="popup-content">
            <view class="popup-title">编辑基础信息</view>
            
            <view class="popup-form">
                <view class="popup-field">
                    <view class="popup-label">身高 (cm)</view>
                    <input type="digit" class="popup-input" v-model="basicInfoForm.heightCm" placeholder="请输入身高"/>
                </view>
                
                <view class="popup-field">
                    <view class="popup-label">基础体重 (kg)</view>
                    <input type="digit" class="popup-input" v-model="basicInfoForm.baseWeight" placeholder="请输入基础体重"/>
                </view>
            </view>
            
            <view class="popup-actions">
                <button class="popup-btn cancel" @tap="closeBasicInfoModal">取消</button>
                <button class="popup-btn confirm" @tap="saveBasicInfo">确认</button>
            </view>
        </view>
    </view>
</template>

<script>
// pages/patient/monitor/monitor.js
import { updateDailyMetricsLatest, upsertTimeseriesFromPatch } from '@/utils/metrics-store.js';
import { USE_MOCK_FALLBACK } from '@/config/api.js';
import {
    getBasicInfo,
    updateBasicInfo,
    getDailyMeasurementToday,
    saveDailyMeasurement,
    getLabs
} from '@/api/patient.js';

export default {
    data() {
        return {
            activeTab: 'daily',
            // daily | lab

            // 控制基础信息弹窗显示
            showBasicInfoPopup: false,

            // 基础信息 - 不需要每天填写
            basicInfo: {
                heightCm: '',
                baseWeight: '',
                bmi: '',
                bmiClass: 'normal',
                lastUpdated: ''
            },
            
            // 编辑基础信息时使用的表单
            basicInfoForm: {
                heightCm: '',
                baseWeight: ''
            },

            // 日常自测表单 - 每天都要填写的部分 (删除了weight相关字段)
            form: {
                sbp: '',
                dbp: '',
                hr: '',
                bpLastUpdated: '',
                spo2: '',
                spo2LastUpdated: '',
                symptomNote: ''
            },
            
            sleepOptions: ['睡得很好', '一般', '较差'],
            sleepIndex: 1,
            appetiteOptions: ['食欲好', '一般', '较差'],
            appetiteIndex: 0,
            stoolOptions: ['每日一次、成形', '偏稀', '偏干', '几天一次'],
            stoolIndex: 0,
            
            // 医院化验结果 demo
            labs: []
        };
    },
    onLoad(options) {
        if (options && options.tab) {
            this.activeTab = options.tab;
        }
        
        // 加载基础信息
        this.loadBasicInfo();

        // 加载今日测量数据
        this.loadDaily();

        // 加载医院化验结果
        this.loadLabs();
    },
    methods: {
        onChangeTab(e) {
            const tab = e.currentTarget.dataset.tab;
            this.activeTab = tab;
        },

        onInput(e) {
            const field = e.currentTarget.dataset.field;
            const value = e.detail.value;
            this.form = {
                ...this.form,
                [field]: value
            };
        },

        onSleepChange(e) {
            this.sleepIndex = Number(e.detail.value);
        },

        onAppetiteChange(e) {
            this.appetiteIndex = Number(e.detail.value);
        },

        onStoolChange(e) {
            this.stoolIndex = Number(e.detail.value);
        },
        
        // 加载基础信息：优先后端，其次本地缓存
        loadBasicInfo() {
            getBasicInfo()
                .then((data) => {
                    if (data) {
                        this.basicInfo = {
                            ...this.basicInfo,
                            ...data
                        };
                        try {
                           setScopedStorageSync('patient_basic_info', this.basicInfo);
                        } catch (e) {}
                    }
                })
                .catch(() => {
                    // 后端不可用时兜底本地
                    try {
                        const cached = getScopedStorageSync('patient_basic_info', null);
                        if (cached) this.basicInfo = { ...this.basicInfo, ...cached };
                    } catch (e) {}
                });
        },
        
        // 显示基础信息编辑弹窗
        showBasicInfoModal() {
            // 复制当前值到表单
            this.basicInfoForm = {
                heightCm: this.basicInfo.heightCm,
                baseWeight: this.basicInfo.baseWeight
            };
            
            // 显示弹窗
            this.showBasicInfoPopup = true;
        },
        
        // 关闭基础信息编辑弹窗
        closeBasicInfoModal() {
            this.showBasicInfoPopup = false;
        },
        
        // 保存基础信息
        saveBasicInfo() {
            const height = Number(this.basicInfoForm.heightCm);
            const weight = Number(this.basicInfoForm.baseWeight);
            
            if (!height || !weight) {
                uni.showToast({
                    title: '请填写完整信息',
                    icon: 'none'
                });
                return;
            }
            
            // 计算BMI
            const h = height / 100;
            const bmi = weight / (h * h);
            const bmiValue = bmi.toFixed(1);
            
            // 确定BMI分类
            let bmiClass = '';
            let bmiText = '';
            if (bmi < 18.5) {
                bmiClass = 'thin';
                bmiText = '偏瘦';
            } else if (bmi < 24) {
                bmiClass = 'normal';
                bmiText = '正常';
            } else if (bmi < 28) {
                bmiClass = 'overweight';
                bmiText = '超重';
            } else {
                bmiClass = 'obese';
                bmiText = '肥胖';
            }
            
            // 获取当前时间
            const now = new Date();
            const dateStr = `${now.getFullYear()}-${(now.getMonth()+1).toString().padStart(2,'0')}-${now.getDate().toString().padStart(2,'0')}`;
            
            // 更新基础信息
            const updatedInfo = {
                heightCm: this.basicInfoForm.heightCm,
                baseWeight: this.basicInfoForm.baseWeight,
                bmi: bmiValue,
                bmiClass,
                bmiText,
                lastUpdated: dateStr
            };
            
            this.basicInfo = updatedInfo;
            
            // 先更新 UI + 本地缓存
            try {
                setScopedStorageSync('patient_basic_info', updatedInfo);
            } catch (e) {}

            // 同步到后端
            updateBasicInfo(updatedInfo).catch(() => {
                // 联调阶段允许失败不影响本地使用
                if (!USE_MOCK_FALLBACK) {
                    uni.showToast({ title: '基础信息同步失败', icon: 'none' });
                }
            });
            
            // 关闭弹窗
            this.closeBasicInfoModal();
            
            uni.showToast({
                title: '基础信息已更新',
                icon: 'success'
            });
        },

        // 同步血氧（SpO₂）
        // 说明：微信小程序没有通用系统级 SpO₂ 读取接口；
        // 实际项目中一般通过蓝牙设备/厂商 SDK 获取。
        syncSpO2() {
            uni.showLoading({
                title: '同步中...'
            });
            
            setTimeout(() => {
                // 模拟获取到的血氧 (93-99 的随机数)
                const randomSpO2 = Math.floor(Math.random() * 7) + 93;
                
                // 更新血氧
                this.form.spo2 = String(randomSpO2);
                
                // 记录更新时间
                const now = new Date();
                this.form.spo2LastUpdated = `${now.getHours().toString().padStart(2,'0')}:${now.getMinutes().toString().padStart(2,'0')}`;
                
                uni.hideLoading();
                
                uni.showToast({
                    title: '血氧已同步',
                    icon: 'success'
                });
            }, 1000);
            
            // 实际应用中：在此对接蓝牙设备回调/厂商 SDK。
        },

        // 保存日常自测
        onSaveDaily() {
            const f = this.form;
            if (!f.sbp || !f.dbp) {
                uni.showToast({
                    title: '请至少填写血压',
                    icon: 'none'
                });
                return;
            }
            
            // 获取当前时间
            const now = new Date();
            const timeStr = `${now.getHours().toString().padStart(2,'0')}:${now.getMinutes().toString().padStart(2,'0')}`;
            const dateStr = `${now.getFullYear()}-${(now.getMonth()+1).toString().padStart(2,'0')}-${now.getDate().toString().padStart(2,'0')}`;
            
            // 更新最后测量时间
            this.form.bpLastUpdated = timeStr;
            if (f.spo2 && !f.spo2LastUpdated) {
                this.form.spo2LastUpdated = timeStr;
            }
            
            // 准备要保存的数据
            const payload = {
                ...f,
                sleep: this.sleepOptions[this.sleepIndex],
                appetite: this.appetiteOptions[this.appetiteIndex],
                stool: this.stoolOptions[this.stoolIndex],
                recordDate: dateStr
            };

            // 保存到本地存储（作为缓存/离线兜底）
            try {
               setScopedStorageSync('daily_measure_today', payload);
               setScopedStorageSync('daily_measure_date', dateStr);
            } catch (e) {}

            // 同步到“身体数据/趋势”展示：写入最新值 + 趋势序列
            const bpText = `${f.sbp}/${f.dbp}`;
            updateDailyMetricsLatest(
                { bp: bpText, hr: f.hr, spo2: f.spo2 },
                '日常自测',
                { silent: true }
            );
            upsertTimeseriesFromPatch(
                { bp: bpText, hr: f.hr, spo2: f.spo2 },
                '日常自测',
                dateStr
            );

            // ✅ 同步到后端
            saveDailyMeasurement(payload)
                .then(() => {
                    uni.showToast({ title: '已保存', icon: 'success' });
                })
                .catch(() => {
                    // 后端失败时：联调阶段保持本地可用
                    uni.showToast({ title: USE_MOCK_FALLBACK ? '已保存（本地）' : '保存失败', icon: USE_MOCK_FALLBACK ? 'success' : 'none' });
                });
        },

        // 加载今日测量：优先后端，其次本地
        loadDaily() {
            const today = new Date();
            const dateStr = `${today.getFullYear()}-${(today.getMonth() + 1).toString().padStart(2, '0')}-${today.getDate().toString().padStart(2, '0')}`;
        
            getDailyMeasurementToday({ date: dateStr })
                .then((data) => {
                    const row = Array.isArray(data && data.rows) ? (data.rows[0] || null) : data;
                    if (!row) return;
        
                    this.fillDailyFormFromData(row);
                    try {
                        setScopedStorageSync('daily_measure_today', row);
                        setScopedStorageSync('daily_measure_date', dateStr);
                    } catch (e) {}
        
                    const sbp = row && row.sbp != null ? String(row.sbp) : '';
                    const dbp = row && row.dbp != null ? String(row.dbp) : '';
                    const bpText = sbp || dbp ? `${sbp || '--'}/${dbp || '--'}` : '';
        
                    updateDailyMetricsLatest(
                        { bp: bpText, hr: row.hr, spo2: row.spo2 },
                        '日常自测',
                        { silent: true }
                    );
        
                    upsertTimeseriesFromPatch(
                        { bp: bpText, hr: row.hr, spo2: row.spo2 },
                        '日常自测',
                        dateStr
                    );
                })
                .catch(() => {
                    try {
                        const cached = getScopedStorageSync('daily_measure_today', {}) || {};
                        this.fillDailyFormFromData(cached);
                    } catch (e) {}
                });
        },

        fillDailyFormFromData(data) {
            const d = data || {};
            this.form = {
                sbp: d.sbp || '',
                dbp: d.dbp || '',
                hr: d.hr || '',
                bpLastUpdated: d.bpLastUpdated || '',
                spo2: d.spo2 || '',
                spo2LastUpdated: d.spo2LastUpdated || '',
                symptomNote: d.symptomNote || ''
            };
            this.sleepIndex = this.sleepOptions.indexOf(d.sleep) >= 0 ? this.sleepOptions.indexOf(d.sleep) : 1;
            this.appetiteIndex = this.appetiteOptions.indexOf(d.appetite) >= 0 ? this.appetiteOptions.indexOf(d.appetite) : 0;
            this.stoolIndex = this.stoolOptions.indexOf(d.stool) >= 0 ? this.stoolOptions.indexOf(d.stool) : 0;
        },

        // 加载医院化验结果：优先后端
        loadLabs() {
            getLabs()
                .then((list) => {
                    this.labs = Array.isArray(list) ? list : (list && list.labs) || [];
                })
                .catch(() => {
                    // fallback demo
                    if (USE_MOCK_FALLBACK) {
                        this.labs = [
                            {
                                id: 'lab1',
                                date: '2025-11-20',
                                hospital: '××医院',
                                comment: '血脂略高，注意低脂饮食，按时复查。',
                                items: [
                                    { name: '空腹血糖', value: '6.3', unit: 'mmol/L', status: 'high', statusText: '偏高' },
                                    { name: '甘油三酯', value: '2.1', unit: 'mmol/L', status: 'high', statusText: '偏高' },
                                    { name: '同型半胱氨酸', value: '18', unit: 'μmol/L', status: 'high', statusText: '偏高' },
                                    { name: '尿酸', value: '430', unit: 'μmol/L', status: 'normal', statusText: '正常' }
                                ]
                            }
                        ];
                    } else {
                        this.labs = [];
                    }
                });
        }
    }
};
</script>

<style>
/* 引入外部样式文件 */
@import './monitor.css';
</style>
