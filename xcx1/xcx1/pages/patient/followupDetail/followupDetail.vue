<template>
  <view class="app-page fd">
    <!-- 顶部：上次随访概览 -->
    <view class="app-card app-card--glass fd-head">
      <view class="flex-row items-start justify-between">
        <view class="flex-col">
          <text class="fd-title">随访详情</text>
          <text class="fd-sub">查看历史随访记录，并可主动申请随访</text>
        </view>
        <view class="fd-chip" :class="'chip-' + lastFollowup.level">
          <text class="fd-chip-text">{{ lastFollowup.levelText }}</text>
        </view>
      </view>

      <view class="fd-kv">
        <view class="fd-kv-row">
          <text class="fd-k">上次随访时间</text>
          <text class="fd-v">{{ lastFollowup.time || '暂无记录' }}</text>
        </view>
        <view class="fd-kv-row">
          <text class="fd-k">随访结论</text>
          <text class="fd-v">{{ lastFollowup.result || '暂无' }}</text>
        </view>
        <view class="fd-kv-row">
          <text class="fd-k">记录摘要</text>
          <text class="fd-v">{{ lastFollowup.summary || '暂无' }}</text>
        </view>
      </view>
    </view>

    <!-- 随访记录列表 -->
    <view class="fd-section flex-row items-center justify-between">
      <text class="app-section-title">随访记录</text>
      <view class="fd-link flex-row items-center" @tap="seedMockIfEmpty">
        <text class="fd-link-text">刷新</text>
        <text class="fd-arrow">›</text>
      </view>
    </view>

    <view class="app-card fd-list">
      <view v-if="records.length === 0" class="fd-empty">
        <text class="fd-empty-text">暂无随访记录</text>
      </view>

      <view v-for="r in records" :key="r.id" class="fd-item" @tap="openRecord(r)">
        <view class="flex-row items-center justify-between">
          <text class="fd-item-time">{{ r.time }}</text>
          <view class="fd-badge" :class="'badge-' + r.level">
            <text class="fd-badge-text">{{ r.levelText }}</text>
          </view>
        </view>
        <text class="fd-item-result">{{ r.result }}</text>
        <text class="fd-item-summary">{{ r.summary }}</text>

        <view class="fd-item-foot flex-row items-center justify-between">
          <text class="fd-item-doctor">随访人员：{{ r.doctor || '医生/护士' }}</text>
          <text class="fd-item-more">查看详情</text>
        </view>
      </view>
    </view>

    <!-- 主动申请随访 -->
    <view class="fd-section flex-row items-center justify-between">
      <text class="app-section-title">主动申请随访</text>
      <view v-if="lastApply" class="fd-lastapply">
        <text class="fd-lastapply-text">最近申请：{{ lastApply.time }}（{{ lastApply.statusText }}）</text>
      </view>
    </view>

    <view class="app-card app-card--glass fd-apply">
      <view class="fd-form-row">
        <text class="fd-label">紧急程度</text>
        <picker :range="urgencyOptions" :value="urgencyIndex" @change="onUrgencyChange">
          <view class="fd-picker">
            <text class="fd-picker-text">{{ urgencyOptions[urgencyIndex] }}</text>
            <text class="fd-arrow">›</text>
          </view>
        </picker>
      </view>

      <view class="fd-form-row">
        <text class="fd-label">希望随访日期</text>
        <picker mode="date" :value="preferDate" @change="onDateChange">
          <view class="fd-picker">
            <text class="fd-picker-text">{{ preferDate || '请选择' }}</text>
            <text class="fd-arrow">›</text>
          </view>
        </picker>
      </view>

      <view class="fd-form-row">
        <text class="fd-label">希望联系时间</text>
        <picker mode="time" :value="preferTime" @change="onTimeChange">
          <view class="fd-picker">
            <text class="fd-picker-text">{{ preferTime || '请选择' }}</text>
            <text class="fd-arrow">›</text>
          </view>
        </picker>
      </view>

      <view class="fd-form-block">
        <text class="fd-label">我的当前情况（必填）</text>
        <!-- 注意：textarea 不能使用自闭合标签，否则在部分 uni-app/小程序编译链下会导致页面空白 -->
        <textarea
          class="fd-textarea"
          v-model="form.symptom"
          placeholder="例如：近两天头晕、胸闷，血压偏高，睡眠变差…"
          maxlength="300"
        ></textarea>
        <text class="fd-hint">{{ form.symptom.length }}/300</text>
      </view>

      <view class="fd-form-block">
        <text class="fd-label">我希望随访解决的问题（必填）</text>
        <textarea
          class="fd-textarea"
          v-model="form.request"
          placeholder="例如：想咨询用药调整、指标异常原因、康养任务如何安排…"
          maxlength="300"
        ></textarea>
        <text class="fd-hint">{{ form.request.length }}/300</text>
      </view>

      <view class="fd-submit" @tap="submitApply">
        <text class="fd-submit-text">提交申请</text>
      </view>

      <view class="fd-privacy">
        <text class="fd-privacy-text">提交后将进入待处理状态，医生端审核后安排随访。</text>
      </view>
    </view>
  </view>
</template>

<script>
import { listFollowups, getFollowupDetail, applyFollowup, listFollowupApplications } from '@/api/patient.js';

export default {
  data() {
    return {
      records: [],
      applies: [],
      lastFollowup: {
        time: '',
        result: '',
        summary: '',
        level: 'mid',
        levelText: '一般'
      },

      urgencyOptions: ['普通', '较急', '紧急'],
      urgencyIndex: 0,
      preferDate: '',
      preferTime: '',
      form: {
        symptom: '',
        request: ''
      }
    };
  },

  computed: {
    lastApply() {
      if (!this.applies.length) return null;
      return this.applies[0];
    }
  },

  onShow() {
    this.loadAll();
  },

  methods: {
    mapResultStatus(v) {
      const s = String(v || '').toUpperCase();
      const map = { ASSIGNED: '待随访', IN_PROGRESS: '随访中', COMPLETED: '已随访', DONE: '已随访', FINISHED: '已随访' };
      return map[s] || String(v || '');
    },

    mapRisk(v) {
      const raw = String(v || '');
      const s = raw.toUpperCase();
      // 枚举：HIGH/MID/LOW；兼容中文：高/中/低风险
      if (s.includes('HIGH') || raw.includes('高')) return { level: 'high', text: '高风险' };
      if (s.includes('LOW') || raw.includes('低')) return { level: 'low', text: '稳定' };
      if (s.includes('MID') || s.includes('MED') || raw.includes('中')) return { level: 'mid', text: '一般' };
      return { level: 'mid', text: '一般' };
    },

    fmtTime(t) {
      return String(t || '').replace('T', ' ').slice(0, 16);
    },


    cleanText(v) {
      const s = String(v == null ? '' : v).replace(/\s+/g, ' ').trim();
      return s;
    },

    resolveStaffName(row) {
      if (!row || typeof row !== 'object') return '医生/护士';
      const candidates = [
        row.followupStaffName,
        row.staffName,
        row.doctorName,
        row.followupStaff,
        row.staff,
        row.doctor
      ];
      for (let i = 0; i < candidates.length; i += 1) {
        const text = this.cleanText(candidates[i]);
        if (text) return text;
      }
      return '医生/护士';
    },

    mapFollowupStatus(v) {
      const s = String(v || '').toUpperCase();
      const map = {
        ASSIGNED: '待随访',
        IN_PROGRESS: '随访中',
        COMPLETED: '已随访',
        DONE: '已随访',
        FINISHED: '已随访'
      };
      return map[s] || String(v || '');
    },

    async loadAll() {
      try {
        // 1) 随访记录
        const recRaw = await listFollowups();
        const recList = Array.isArray(recRaw)
          ? recRaw
          : recRaw && Array.isArray(recRaw.list)
            ? recRaw.list
            : recRaw && Array.isArray(recRaw.records)
              ? recRaw.records
              : [];

        this.records = (recList || []).map((r) => {
          const risk = this.mapRisk(r.riskLevel || r.level);
          const time = this.fmtTime(r.followupTime || r.measuredAt || r.createdAt || r.time);
          return {
            id: r.id || r.recordId,
            relatedTaskId: r.relatedTaskId || r.related_task_id || r.taskId,
            time,
            level: risk.level,
            levelText: risk.text,
            result: this.mapResultStatus(r.resultStatus || r.result || r.title) || '随访记录',
            summary: r.contentSummary || r.summary || '',
            doctor: this.resolveStaffName(r),
            staffName: this.resolveStaffName(r)
          };
        });

        if (this.records.length) {
          const r0 = this.records[0];
          this.lastFollowup = {
            time: r0.time,
            result: r0.result,
            summary: r0.summary,
            level: r0.level || 'mid',
            levelText: r0.levelText || '一般'
          };
        } else {
          this.lastFollowup = { time: '', result: '', summary: '', level: 'mid', levelText: '一般' };
        }

        // 2) 主动申请随访列表
        const appRaw = await listFollowupApplications();
        const appList = Array.isArray(appRaw)
          ? appRaw
          : appRaw && Array.isArray(appRaw.list)
            ? appRaw.list
            : appRaw && Array.isArray(appRaw.records)
              ? appRaw.records
              : [];

        const mapStatusText = (v) => {
          const s = String(v || '').toLowerCase();
          if (s.includes('approve') || s.includes('pass')) return '已通过';
          if (s.includes('reject') || s.includes('deny')) return '已拒绝';
          if (s.includes('arrange') || s.includes('schedule')) return '已安排';
          return '待处理';
        };

        const parseApplyFields = (a) => {
          const out = { urgency: '普通', preferDate: '', preferTime: '', symptom: '', request: '' };

          // 1) 直接字段（若后端已扩展/前端旧字段存在）
          out.urgency = a.urgency || a.urgencyLevel || a.levelText || out.urgency;
          out.preferDate = a.preferDate || a.expectDate || a.date || out.preferDate;
          out.preferTime = a.preferTime || a.expectTime || a.timeSlot || out.preferTime;
          out.symptom = a.currentSituation || a.symptom || out.symptom;
          out.request = a.request || a.ask || out.request;

          // 2) ext 扩展字段（推荐后端返回 ext1~ext5）
          if (a.ext1 || a.ext2 || a.ext3 || a.ext4 || a.ext5) {
            out.urgency = a.ext1 || out.urgency;
            out.preferDate = a.ext2 || out.preferDate;
            out.preferTime = a.ext3 || out.preferTime;
            out.symptom = a.ext4 || out.symptom;
            out.request = a.ext5 || out.request;
          }

          // 3) preferredTime（后端当前字段）：拆出日期/时间
          const pt = a.preferredTime || a.preferred_time;
          if (pt && (!out.preferDate || !out.preferTime)) {
            const s = String(pt).replace('T', ' ');
            if (!out.preferDate) out.preferDate = s.slice(0, 10);
            if (!out.preferTime) out.preferTime = s.slice(11, 16);
          }

          // 4) applyReason 兼容（后端当前字段）：支持 JSON 或 “【】” 文案
          const ar = a.applyReason || a.apply_reason;
          if (ar && (!out.symptom || !out.request || !out.urgency)) {
            const str = String(ar).trim();
            // JSON
            if (str.startsWith('{') && str.endsWith('}')) {
              try {
                const j = JSON.parse(str);
                out.urgency = j.urgency || out.urgency;
                out.preferDate = j.preferDate || out.preferDate;
                out.preferTime = j.preferTime || out.preferTime;
                out.symptom = j.currentSituation || j.symptom || out.symptom;
                out.request = j.request || out.request;
              } catch (e) {}
            }
            // “【紧急程度】xx；【当前情况】xx；【诉求】xx”
            const mUrg = str.match(/【紧急程度】([^；;]+)/);
            const mSym = str.match(/【当前情况】([^；;]+)/);
            const mReq = str.match(/【诉求】([^；;]+)/);
            if (mUrg && !out.urgency) out.urgency = mUrg[1].trim();
            if (mSym && !out.symptom) out.symptom = mSym[1].trim();
            if (mReq && !out.request) out.request = mReq[1].trim();
          }

          return out;
        };

        this.applies = (appList || []).map((a) => {
          const time = this.fmtTime(a.createdAt || a.time || a.applyTime);
          const f = parseApplyFields(a);
          return {
            id: a.id || a.appId,
            time,
            urgency: f.urgency,
            preferDate: f.preferDate,
            preferTime: f.preferTime,
            symptom: f.symptom,
            request: f.request,
            status: a.status || 'pending',
            statusText: a.statusText || mapStatusText(a.status)
          };
        });
      } catch (e) {
        // 静默失败：避免阻塞页面
      }
    },

    async seedMockIfEmpty() {
      // 兼容旧按钮：改为“从后端刷新”
      await this.loadAll();
      uni.showToast({ title: '已刷新', icon: 'success' });
    },

    

    async openRecord(r) {
      try {
        const detail = await getFollowupDetail(r.id);
        const d = detail && typeof detail === 'object' ? detail : {};
        const merged = { ...r, ...d };
        const staffName = this.resolveStaffName(merged);
        const risk = this.mapRisk(merged.riskLevel || merged.level || '');
        const resultText = this.mapResultStatus(merged.resultStatus || merged.result || '');
        const summaryText = this.cleanText(merged.contentSummary || merged.summary || '');
        const symptomText = this.cleanText(merged.symptomChange || merged.mainSymptoms || '');
        const nextFollowupText = this.fmtTime(merged.nextFollowupDate || merged.nextPlanTime || '');
        const adviceText = this.cleanText(merged.ext1 || merged.advice || '');
        const relatedTaskId = merged.relatedTaskId || merged.related_task_id || merged.taskId || '';

        const content = [
          `时间：${this.fmtTime(merged.followupTime || merged.measuredAt || merged.createdAt || r.time) || '暂无'}`,
          `随访人员：${staffName}`,
          `风险：${risk.text || r.levelText || '一般'}`,
          `结论：${resultText || '暂无'}`,
          `摘要：${summaryText || '暂无'}`,
          symptomText ? `症状变化：${symptomText}` : '',
          nextFollowupText ? `下次随访：${nextFollowupText}` : '',
          adviceText ? `随访建议：${adviceText}` : '',
          relatedTaskId ? `任务ID：${relatedTaskId}` : ''
        ]
          .filter(Boolean)
          .join('\n');

        uni.showModal({
          title: '随访记录详情',
          content,
          showCancel: false,
          confirmText: '知道了'
        });
      } catch (e) {
        const content = [
          `时间：${r.time || '暂无'}`,
          `随访人员：${this.resolveStaffName(r)}`,
          `结论：${r.result || '暂无'}`,
          `摘要：${r.summary || '暂无'}`
        ].join('\n');

        uni.showModal({ title: '随访记录详情', content, showCancel: false, confirmText: '知道了' });
      }
    },

    onUrgencyChange(e) {
      this.urgencyIndex = Number(e.detail.value || 0);
    },
    onDateChange(e) {
      this.preferDate = e.detail.value;
    },
    onTimeChange(e) {
      this.preferTime = e.detail.value;
    },

    submitApply() {
      const symptom = (this.form.symptom || '').trim();
      const request = (this.form.request || '').trim();

      if (!symptom) return uni.showToast({ title: '请填写当前情况', icon: 'none' });
      if (!request) return uni.showToast({ title: '请填写随访诉求', icon: 'none' });

      const urgencyText = this.urgencyOptions[this.urgencyIndex] || '普通';
      const preferredTime = (this.preferDate && this.preferTime) ? `${this.preferDate} ${this.preferTime}:00` : '';
      const payload = {
        // 新版字段：用于前端展示/后端扩展字段
        urgency: urgencyText,
        preferDate: this.preferDate,
        preferTime: this.preferTime,
        currentSituation: symptom,
        request,

        // 兼容后端当前字段（applyReason/contactPhone/preferredTime）
        applyReason: `【紧急程度】${urgencyText}；【当前情况】${symptom}；【诉求】${request}`,
        contactPhone: '',
        preferredTime
      };

      uni.showModal({
        title: '确认提交',
        content: '提交后将进入待处理状态，医生端审核后安排随访。',
        confirmText: '提交',
        cancelText: '再想想',
        success: async (res) => {
          if (!res.confirm) return;

          try {
            await applyFollowup(payload);

            this.form.symptom = '';
            this.form.request = '';
            this.preferDate = '';
            this.preferTime = '';
            this.urgencyIndex = 0;

            await this.loadAll();
            uni.showToast({ title: '已提交', icon: 'success' });
          } catch (e) {
            uni.showToast({ title: '提交失败，请稍后重试', icon: 'none' });
          }
        }
      });
    }
  }
};
</script>


<style src="./followupDetail.css"></style>
