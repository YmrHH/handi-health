<template>
    <view class="page">
        <view class="section patient-section">
            <view class="section-title">风险分层患者</view>

            <block v-for="(item, index) in patients" :key="index">
                <view class="patient-card">
                    <!-- 顶部：姓名年龄 + 风险标签 -->
                    <view class="patient-card-header">
                        <view class="patient-name-row">
                            <text class="patient-name">{{ item.name }}</text>
                            <text class="patient-age">（{{ item.age }}岁）</text>
                        </view>

                        <view class="patient-risk-wrap">
                            <text class="patient-risk-label">风险：</text>

                            <!-- 高风险 -->
                            <text class="tag tag-risk-high" v-if="item.riskLevel === '高'">{{ item.riskLevel }}风险</text>

                            <!-- 中风险 -->
                            <text class="tag tag-risk-mid" v-else-if="item.riskLevel === '中'">{{ item.riskLevel }}风险</text>

                            <!-- 低风险 -->
                            <text class="tag tag-risk-low" v-else-if="item.riskLevel === '低'">{{ item.riskLevel }}风险</text>

                            <!-- 兜底：其它值（理论上用不到，但防御一下） -->
                            <text class="tag" v-else>{{ item.riskLevel }}风险</text>
                        </view>
                    </view>

                    <!-- 病种 -->
                    <view class="patient-info-row">
                        <text class="patient-info-label">病种：</text>
                        <text class="patient-info-value">{{ item.disease }}</text>
                    </view>

                    <!-- 体质 + 证型（保持原来的文案形式） -->
                    <view class="patient-info-row">
                        <text class="patient-info-label">体质：</text>
                        <text class="patient-info-value">{{ item.constitution }} ｜ 证型：{{ item.tcmPattern }}</text>
                    </view>
                </view>

                    <!-- 操作 -->
                    <view class="patient-actions">
                        <button class="mini-btn" @tap.stop="goPatientData(item)">查看数据</button>
                    </view>
            </block>
        </view>
    </view>
</template>

<script>
import { USE_MOCK_FALLBACK } from '@/config/api.js';
import { getPatients } from '@/api/staff.js';

export default {
    data() {
        return {
            patients: []
        };
    },
    onLoad() {
        this.loadPatients();
    },
    methods: {
        goPatientData(item) {
            const id = item.id || '';
            if (!id) {
                uni.showToast({ title: '缺少患者ID', icon: 'none' });
                return;
            }
            const name = encodeURIComponent(item.name || '');
            const age = item.age || '';
            const gender = encodeURIComponent(item.gender || '');
            uni.navigateTo({
                url: `/pages/staff/patientData/patientData?id=${id}&name=${name}&age=${age}&gender=${gender}`
            });
        },

        loadPatients() {
            getPatients()
                .then((res) => {
                    // request() 已将后端 {code,msg,data} 解包为 data
                    // 兼容多种后端返回：[] / {list:[]} / {patients:[]}
                    const list = Array.isArray(res) ? res : (res && (res.list || res.patients)) || [];
                    this.patients = Array.isArray(list) ? list : [];
                })
                .catch(() => {
                    if (!USE_MOCK_FALLBACK) {
                        this.patients = [];
                        return;
                    }
                    // 联调 demo
                    this.patients = [
                        {
                            id: 'P001',
                            name: '张阿姨',
                            age: 72,
                            disease: 'COPD + 高血压',
                            constitution: '痰湿质兼气虚',
                            tcmPattern: '痰浊阻肺证',
                            riskLevel: '中'
                        },
                        {
                            id: 'P002',
                            name: '王大爷',
                            age: 78,
                            disease: '冠心病 + 高血压',
                            constitution: '阳虚质',
                            tcmPattern: '心阳虚证',
                            riskLevel: '高'
                        }
                    ];
                });
        }
    }
};
</script>
<style>
@import './patients.css';
</style>
